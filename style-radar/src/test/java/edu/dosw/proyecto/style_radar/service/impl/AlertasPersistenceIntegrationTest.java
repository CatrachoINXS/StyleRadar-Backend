package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.support.AlertasFixtures;
import org.hibernate.Hibernate;

@SpringBootTest @ActiveProfiles("test") @Transactional
class AlertasPersistenceIntegrationTest extends AlertasFixtures {
    @Test
    void creaBusquedaPropiaSinNotificarYPersiste() {
        // Arrange / Act
        var a = alertaBusqueda(b -> b.setMarca("Radar"));
        releer();
        // Assert
        assertThat(service.consultar(usuario.getId())).containsExactly(a);
        assertThat(a.usuarioId()).isEqualTo(usuario.getId());
        assertThat(a.tipoObjetivo()).isEqualTo(TipoObjetivoAlerta.BUSQUEDA_GUARDADA);
        assertThat(a.activa()).isTrue();
        assertThat(a.fechaCreacion()).isNotNull();
        assertThat(eventos()).isEmpty();
    }

    @Test
    void creaItemAgotadoYSoloUnObjetivo() {
        // Arrange
        Long id = publicar();
        // Act
        var a = service.crear(usuario.getId(), null, id);
        releer();
        // Assert
        assertThat(a.tipoObjetivo()).isEqualTo(TipoObjetivoAlerta.ITEM_CATALOGO);
        assertThat(a.identificadorObjetivo()).isEqualTo(id);
        assertThat(eventos()).isEmpty();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void objetivoExclusivoSeValidaEnServicio(boolean ambos) {
        // Act & Assert
        assertThatThrownBy(() -> service.crear(usuario.getId(), ambos ? 1L : null, ambos ? 1L : null))
                .isInstanceOf(SolicitudAlertaInvalidaException.class);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void objetivoInexistenteSeRechaza(boolean busqueda) {
        // Act & Assert
        assertThatThrownBy(() -> service.crear(usuario.getId(), busqueda ? Long.MAX_VALUE : null,
                busqueda ? null : Long.MAX_VALUE)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void busquedaAjenaEsNoAccesible() {
        // Arrange
        var otra = busqueda(usuario("otra@example.co"), b -> b.setMarca("Radar"));
        // Act & Assert
        assertThatThrownBy(() -> service.crear(usuario.getId(), otra.getId(), null))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("Busqueda no accesible");
    }

    @Test
    void busquedaConSoloEspaciosEsVacia() {
        // Arrange
        var b = busqueda(usuario, x -> { x.setQuery("  "); x.setMarca(" "); x.setColor("\t"); });
        // Act & Assert
        assertThatThrownBy(() -> service.crear(usuario.getId(), b.getId(), null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void itemDisponibleNoAceptaAlerta() {
        // Arrange
        Long id = publicar(); stock(id, 1);
        // Act & Assert
        assertThatThrownBy(() -> service.crear(usuario.getId(), null, id)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void duplicadoActivoEsConflicto() {
        // Arrange
        Long id = publicar(); service.crear(usuario.getId(), null, id);
        // Act & Assert
        assertThatThrownBy(() -> service.crear(usuario.getId(), null, id)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void dosObjetivosDiferentesPermitidosYOrdenDeterminista() {
        // Arrange
        Long id = publicar();
        var a = service.crear(usuario.getId(), null, id);
        // Act
        var b = alertaBusqueda(x -> x.setMarca("Radar"));
        // Assert
        assertThat(service.consultar(usuario.getId())).extracting(Alerta::id).containsExactly(b.id(), a.id());
    }

    @Test
    void puedeCrearNuevaAlertaSinBorrarHistorialDesactivado() {
        // Arrange
        Long id = publicar(); var a = service.crear(usuario.getId(), null, id);
        service.desactivar(usuario.getId(), a.id(), false); releer();
        // Act
        var b = service.crear(usuario.getId(), null, id);
        // Assert
        assertThat(b.id()).isNotEqualTo(a.id());
        assertThat(service.consultar(usuario.getId())).hasSize(2);
    }

    @ParameterizedTest @ValueSource(strings = {"query", "tipo", "color", "marca", "estilo", "talla", "precioMin", "precioMax"})
    void cadaFiltroExactoCompatibleProduceEvento(String filtro) {
        // Arrange
        var a = alertaBusqueda(b -> criterio(b, filtro));
        Long id = publicar();
        // Act
        stock(id, 2); releer();
        // Assert
        assertThat(eventos()).singleElement().satisfies(n -> {
            assertThat(n.tipoEvento()).isEqualTo(TipoEventoNotificacion.NUEVA_COINCIDENCIA);
            assertThat(n.alertaId()).isEqualTo(a.id()); assertThat(n.itemCatalogoId()).isEqualTo(id);
        });
    }

    @ParameterizedTest @ValueSource(strings = {"query", "tipo", "color", "marca", "estilo", "talla", "precioMin", "precioMax"})
    void todosLosFiltrosAndRechazanCualquierCoincidenciaParcial(String filtro) {
        // Arrange
        alertaBusqueda(b -> { for (String f : List.of("query", "tipo", "color", "marca", "estilo", "talla", "precioMin", "precioMax")) criterio(b, f); });
        Long id = publicar(); var i = items.findById(id).orElseThrow();
        switch (filtro) {
            case "query" -> i.getPrenda().setNombre("Camisa general");
            case "tipo" -> i.getPrenda().setTipo(TipoPrenda.INFERIOR);
            case "color" -> i.getPrenda().setColor("Blanco");
            case "marca" -> i.getPrenda().setMarca("Otra");
            case "estilo" -> i.getPrenda().setEstilo(Estilo.CASUAL);
            case "precioMin" -> i.setPrecio(99.0);
            case "precioMax" -> i.setPrecio(101.0);
            case "talla" -> { }
            default -> throw new AssertionError(filtro);
        }
        // Act
        stock(id, filtro.equals("talla") ? Talla.L : Talla.M, 2);
        // Assert
        assertThat(eventos()).isEmpty();
    }

    @Test
    void todosLosFiltrosAndCompatiblesYNormalizados() {
        // Arrange
        alertaBusqueda(b -> { for (String f : List.of("query", "tipo", "color", "marca", "estilo", "talla", "precioMin", "precioMax")) criterio(b, f); });
        Long id = publicar();
        // Act
        stock(id, 5);
        // Assert
        assertThat(eventos()).hasSize(1);
    }

    @Test
    void noUsaSimilitudRf16() {
        // Arrange
        alertaBusqueda(b -> b.setQuery("Camisa inexistente")); Long id = publicar();
        // Act
        stock(id, 4);
        // Assert
        assertThat(eventos()).isEmpty();
    }

    @Test
    void busquedaLegacyVaciaNoNotificaAunqueTengaAlerta() {
        // Arrange
        var a = alertaBusqueda(b -> b.setMarca("Radar"));
        busquedas.findById(a.identificadorObjetivo()).orElseThrow().setMarca(" ");
        Long id = publicar();
        // Act
        stock(id, 4);
        // Assert
        assertThat(eventos()).isEmpty();
    }

    @Test
    void publicacionAgotadaNoNotificaHastaPrimeraDisponibilidad() {
        // Arrange
        alertaBusqueda(b -> b.setMarca("Radar")); Long id = publicar();
        assertThat(eventos()).isEmpty();
        // Act
        stock(id, 0); assertThat(eventos()).isEmpty(); stock(id, 2); stock(id, 5); stock(id, 5);
        stock(id, 0); stock(id, 4); releer();
        // Assert
        assertThat(eventos()).hasSize(1);
        assertThat(disponibilidad.findById(id).orElseThrow().getCiclo()).isEqualTo(2);
    }

    @Test
    void publicacionAnteriorAAlertaNoNotificaComoNueva() {
        // Arrange
        Long id = publicar(); alertaBusqueda(b -> b.setMarca("Radar"));
        // Act
        stock(id, 3);
        // Assert
        assertThat(eventos()).isEmpty();
    }

    @Test
    void primeraCoincidenciaEfectivaEnMDespuesDeStockEnLNotificaUnaSolaVez() {
        // Arrange
        var alerta = alertaBusqueda(b -> { b.setTalla(Talla.M); b.setEstilo(Estilo.DEPORTIVO); });
        Long id = publicarDeportiva();
        // Act: L da disponibilidad global, pero no cumple la talla de la búsqueda.
        stock(id, Talla.L, 5); releer();
        assertThat(eventos()).isEmpty();
        stock(id, Talla.M, 3); releer();
        // Assert: la primera coincidencia efectiva pertenece a esta alerta y publicación.
        assertThat(eventos()).singleElement().satisfies(n -> {
            assertThat(n.alertaId()).isEqualTo(alerta.id());
            assertThat(n.itemCatalogoId()).isEqualTo(id);
            assertThat(n.tipoEvento()).isEqualTo(TipoEventoNotificacion.NUEVA_COINCIDENCIA);
        });
        stock(id, Talla.M, 8); stock(id, Talla.M, 8);
        stock(id, Talla.M, 0); stock(id, Talla.M, 3); releer();
        assertThat(eventos()).hasSize(1);
        assertThat(disponibilidad.findById(id).orElseThrow().getCiclo()).isEqualTo(1);
    }

    @Test
    void seguimientoDeItemSoloNotificaPorReposicionTotalAunqueOtraTallaObtengaStockDespues() {
        // Arrange
        Long id = publicarDeportiva(); var alerta = service.crear(usuario.getId(), null, id);
        // Act & Assert
        stock(id, Talla.L, 5); releer();
        assertThat(eventos()).singleElement().satisfies(n -> {
            assertThat(n.alertaId()).isEqualTo(alerta.id());
            assertThat(n.tipoEvento()).isEqualTo(TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA);
        });
        stock(id, Talla.M, 3); stock(id, Talla.M, 8); releer();
        assertThat(eventos()).hasSize(1);
        assertThat(disponibilidad.findById(id).orElseThrow().getCiclo()).isEqualTo(1);
        stock(id, Talla.L, 0); stock(id, Talla.M, 0); stock(id, Talla.M, 3); releer();
        assertThat(eventos()).hasSize(2).allSatisfy(n ->
                assertThat(n.tipoEvento()).isEqualTo(TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA));
        assertThat(notificaciones.findAll()).extracting(NotificacionEntity::getCiclo).containsExactly(1L, 2L);
    }

    @Test
    void publicacionAnteriorNoNotificaAlCoincidirTallaTardiamente() {
        // Arrange
        Long id = publicarDeportiva();
        alertaBusqueda(b -> { b.setTalla(Talla.M); b.setEstilo(Estilo.DEPORTIVO); });
        // Act
        stock(id, Talla.L, 5); stock(id, Talla.M, 3); stock(id, Talla.M, 8); releer();
        // Assert
        assertThat(eventos()).isEmpty();
    }

    @Test
    void cadaAlertaRecibeSuPrimeraCoincidenciaPorTallaIndependientemente() {
        // Arrange
        var m = alertaBusqueda(b -> { b.setTalla(Talla.M); b.setEstilo(Estilo.DEPORTIVO); });
        var l = alertaBusqueda(b -> { b.setTalla(Talla.L); b.setEstilo(Estilo.DEPORTIVO); });
        Long id = publicarDeportiva();
        // Act & Assert
        stock(id, Talla.L, 5); releer();
        assertThat(eventos()).extracting(Notificacion::alertaId).containsExactly(l.id());
        stock(id, Talla.M, 3); stock(id, Talla.M, 8); releer();
        assertThat(eventos()).extracting(Notificacion::alertaId).containsExactly(m.id(), l.id());
    }

    private Long publicarDeportiva() {
        var prenda = new Prenda(null, "Camiseta deportiva", "Algodon", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.DEPORTIVO);
        Long id = catalogo.publicar(almacen.getNit(), prenda, 100.0).getId();
        inventario.registrarTallas(almacen.getNit(), id, Set.of(Talla.M, Talla.L));
        return id;
    }

    @Test
    void reposicionLegacyNuncaEsNuevaPeroNotificaSeguimiento() {
        // Arrange
        alertaBusqueda(b -> b.setMarca("Radar")); Long id = antiguo(0);
        var a = service.crear(usuario.getId(), null, id);
        // Act
        stock(id, 2);
        // Assert
        assertThat(eventos()).singleElement().satisfies(n -> {
            assertThat(n.alertaId()).isEqualTo(a.id());
            assertThat(n.tipoEvento()).isEqualTo(TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA);
        });
    }

    @Test
    void reposicionesSoloEnTransicionesRealesYVariasTallasNoDuplican() {
        // Arrange
        Long id = publicar(); service.crear(usuario.getId(), null, id);
        // Act
        stock(id, 5); stock(id, 8); stock(id, 8); stock(id, Talla.L, 2);
        assertThat(eventos()).hasSize(1);
        stock(id, 0); assertThat(eventos()).hasSize(1); stock(id, Talla.L, 0); stock(id, 3);
        releer();
        // Assert
        assertThat(eventos()).hasSize(2).allSatisfy(n -> assertThat(n.tipoEvento()).isEqualTo(TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA));
        assertThat(notificaciones.findAll()).extracting(NotificacionEntity::getCiclo).containsExactly(1L, 2L);
    }

    @Test
    void dosUsuariosRecibenSoloSusEventosYSinAlertaNada() {
        // Arrange
        var otro = usuario("otro@example.co"); var sin = usuario("sin@example.co");
        Long id = publicar(); var a = service.crear(usuario.getId(), null, id); var b = service.crear(otro.getId(), null, id);
        // Act
        stock(id, 3); releer();
        // Assert
        assertThat(eventos()).extracting(Notificacion::alertaId).containsExactly(a.id());
        assertThat(service.notificaciones(otro.getId(), 0, 20)).extracting(Notificacion::alertaId).containsExactly(b.id());
        assertThat(service.notificaciones(sin.getId(), 0, 20)).isEmpty();
    }

    @Test
    void paginaYOrdenDeNotificacionesSonDeterministasInclusoMismaFecha() {
        // Arrange
        Long id = publicar(); service.crear(usuario.getId(), null, id); stock(id, 3); stock(id, 0); stock(id, 2);
        var ns = notificaciones.findAll();
        em.createQuery("update NotificacionEntity n set n.fechaCreacion = :fecha").setParameter("fecha", ns.getFirst().getFechaCreacion()).executeUpdate();
        releer();
        // Act
        var page = service.notificaciones(usuario.getId(), 0, 1);
        // Assert
        assertThat(page.getContent()).extracting(Notificacion::id).containsExactly(ns.getLast().getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(service.notificaciones(usuario.getId(), 1, 1).getContent()).extracting(Notificacion::id).containsExactly(ns.getFirst().getId());
        assertThat(service.notificaciones(usuario.getId(), Integer.MAX_VALUE, 100)).isEmpty();
    }

    @Test
    void desactivacionIdempotenteConservaBusquedaFechaEHistorialYDetieneEventos() {
        // Arrange
        var a = alertaBusqueda(b -> b.setMarca("Radar")); stock(publicar(), 3);
        // Act
        var d = service.desactivar(usuario.getId(), a.id(), false); releer();
        var repetida = service.desactivar(usuario.getId(), a.id(), false); stock(publicar(), 3); releer();
        // Assert
        assertThat(d.activa()).isFalse(); assertThat(d.fechaDesactivacion()).isNotNull();
        assertThat(repetida.fechaDesactivacion()).isEqualTo(d.fechaDesactivacion());
        assertThat(busquedas.existsById(a.identificadorObjetivo())).isTrue();
        assertThat(eventos()).hasSize(1);
    }

    @Test
    void seguimientoDesactivadoNoNotifica() {
        // Arrange
        Long id = publicar(); var a = service.crear(usuario.getId(), null, id); service.desactivar(usuario.getId(), a.id(), false);
        // Act
        stock(id, 3);
        // Assert
        assertThat(eventos()).isEmpty();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void alertaAjenaOInexistenteNoEsAccesible(boolean ajena) {
        // Arrange
        Long id = ajena ? alertaBusqueda(b -> b.setMarca("Radar")).id() : Long.MAX_VALUE;
        var otro = usuario("otro@example.co");
        // Act & Assert
        assertThatThrownBy(() -> service.desactivar(otro.getId(), id, false)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void eliminarBusquedaConOSinAlertaConservaOtrasAlertas(boolean conAlerta) {
        // Arrange
        var b = busqueda(usuario, x -> x.setMarca("Radar"));
        var a = conAlerta ? service.crear(usuario.getId(), b.getId(), null) : null;
        var otra = alertaBusqueda(x -> x.setColor("Negro"));
        stock(publicar(), 2);
        int anteriores = eventos().size();
        // Act
        perfiles.eliminarBusquedaGuardada(usuario.getId(), b.getId()); releer();
        // Assert
        assertThat(busquedas.existsById(b.getId())).isFalse();
        if (conAlerta) assertThat(alertas.findById(a.id()).orElseThrow().isActiva()).isFalse();
        assertThat(alertas.findById(otra.id()).orElseThrow().isActiva()).isTrue();
        assertThat(eventos()).hasSize(anteriores);
        stock(publicar(), 2);
        assertThat(eventos()).hasSize(anteriores + 1);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void retirarItemConOSinAlertaPreservaHistorialSinCascadas(boolean conAlerta) {
        // Arrange
        var busqueda = alertaBusqueda(b -> b.setMarca("Radar")); Long id = publicar();
        var seguimiento = conAlerta ? service.crear(usuario.getId(), null, id) : null;
        stock(id, 2); int anteriores = eventos().size();
        // Act
        catalogo.retirar(almacen.getNit(), id); releer();
        // Assert
        assertThat(items.existsById(id)).isFalse(); assertThat(disponibilidad.existsById(id)).isFalse();
        assertThat(eventos()).hasSize(anteriores).allSatisfy(n -> assertThat(n.itemCatalogoId()).isEqualTo(id));
        if (conAlerta) assertThat(alertas.findById(seguimiento.id()).orElseThrow().isActiva()).isFalse();
        assertThat(alertas.findById(busqueda.id()).orElseThrow().isActiva()).isTrue();
    }

    @Test
    void relacionesSonLazyYObjetivosSonReferenciasHistoricas() {
        // Arrange
        Long id = publicar(); var a = service.crear(usuario.getId(), null, id); stock(id, 2); releer();
        // Act
        var entidad = alertas.findById(a.id()).orElseThrow(); var n = notificaciones.findAll().getFirst();
        // Assert
        assertThat(Hibernate.isInitialized(entidad.getUsuario())).isFalse();
        assertThat(Hibernate.isInitialized(n.getAlerta())).isTrue(); // ya cargada en este contexto
        assertThat(Hibernate.isInitialized(disponibilidad.findById(id).orElseThrow().getItem())).isFalse();
        assertThat(entidad.getIdentificadorObjetivo()).isEqualTo(id);
    }

    private void criterio(BusquedaGuardadaEntity b, String filtro) {
        switch (filtro) {
            case "query" -> b.setQuery("  ESPECIAL  ");
            case "tipo" -> b.setTipo(TipoPrenda.SUPERIOR);
            case "color" -> b.setColor(" NEGRO ");
            case "marca" -> b.setMarca(" radar ");
            case "estilo" -> b.setEstilo(Estilo.FORMAL);
            case "talla" -> b.setTalla(Talla.M);
            case "precioMin" -> b.setPrecioMin(100.0);
            case "precioMax" -> b.setPrecioMax(100.0);
            default -> throw new AssertionError(filtro);
        }
    }
}
