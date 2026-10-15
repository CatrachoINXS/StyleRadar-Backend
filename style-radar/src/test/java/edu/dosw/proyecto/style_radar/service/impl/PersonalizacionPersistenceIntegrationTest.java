package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.support.PersonalizacionFixtures;

@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"})
@ActiveProfiles("test")
@Transactional
class PersonalizacionPersistenceIntegrationTest extends PersonalizacionFixtures {
    private Page<ItemCatalogo> feed(int page, int size) {
        releer();
        return service.obtenerFeed(usuario.getId(), page, size);
    }

    private ResultadoRecomendaciones recomendaciones(int page, int size) {
        releer();
        return service.obtenerRecomendaciones(usuario.getId(), page, size);
    }

    @Test
    void feedPriorizaAmbasSenalesLuegoUnaLuegoNingunaSinExcluirCatalogo() {
        // Arrange: el orden de inserción y recencia contradicen el de prioridad.
        var ninguno = item("Ninguno", Estilo.CASUAL, Talla.L, 4, 400);
        var talla = item("Talla", Estilo.CASUAL, Talla.M, 4, 300);
        var estilo = item("Estilo", Estilo.FORMAL, Talla.L, 4, 200);
        var ambos = item("Ambos", Estilo.FORMAL, Talla.M, 4, 100);
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId)
                .containsExactly(ambos.getId(), talla.getId(), estilo.getId(), ninguno.getId());
        assertThat(resultado.getTotalElements()).isEqualTo(4);
    }

    @ParameterizedTest
    @CsvSource({"FORMAL,L", "CASUAL,M", "CASUAL,L"})
    void feedActividadPriorizaDentroDeCadaGrupo(Estilo estilo, Talla talla) {
        // Arrange
        var reciente = item("Reciente", estilo, talla, 4, 10);
        var actividad = item("Buscada", estilo, talla, 4, 0);
        busqueda(usuario, b -> b.setQuery("Buscada"));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId)
                .containsExactly(actividad.getId(), reciente.getId());
    }

    @Test
    void feedActividadNoSuperaPrioridadDePerfil() {
        // Arrange
        var actividad = item("Buscada", Estilo.CASUAL, Talla.L, 4, 10);
        var ambos = item("Perfil", Estilo.FORMAL, Talla.M, 4, 0);
        busqueda(usuario, b -> b.setQuery("Buscada"));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(ambos.getId(), actividad.getId());
    }

    @Test
    void feedActividadDentroDelGrupoAmbasSenales() {
        // Arrange
        var reciente = item("Reciente", Estilo.FORMAL, Talla.M, 4, 10);
        var buscada = item("Buscada", Estilo.FORMAL, Talla.M, 4, 0);
        busqueda(usuario, b -> b.setQuery("Buscada"));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(buscada.getId(), reciente.getId());
    }

    @Test
    void busquedaConTodosLosFiltrosExigeAndYNormalizaTexto() {
        // Arrange
        perfil(Set.of(), Set.of());
        var esperado = item("Camisa especial", Estilo.FORMAL, Talla.M, 4, 0);
        var parcial = item("Camisa especial", Estilo.CASUAL, Talla.M, 4, 20);
        var otraTalla = item("Camisa especial", Estilo.FORMAL, Talla.L, 4, 10);
        busqueda(usuario, b -> {
            b.setQuery("  ESPECIAL  "); b.setTipo(TipoPrenda.SUPERIOR); b.setColor("  NEGRO ");
            b.setMarca(" radar "); b.setEstilo(Estilo.FORMAL); b.setTalla(Talla.M);
            b.setPrecioMin(100.0); b.setPrecioMax(100.0);
        });
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId)
                .containsExactly(esperado.getId(), parcial.getId(), otraTalla.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"query", "tipo", "color", "marca", "estilo", "talla", "precioMin", "precioMax"})
    void cadaFiltroGuardadoDebeCumplirse(String filtro) {
        // Arrange
        perfil(Set.of(), Set.of());
        var exacta = item("Buscada", Estilo.FORMAL, Talla.M, 4, 0);
        var parcial = item("Buscada", Estilo.FORMAL, Talla.M, 4, 10);
        switch (filtro) {
            case "query" -> parcial.getPrenda().setNombre("Otra");
            case "tipo" -> parcial.getPrenda().setTipo(TipoPrenda.INFERIOR);
            case "color" -> parcial.getPrenda().setColor("Blanco");
            case "marca" -> parcial.getPrenda().setMarca("Otra");
            case "estilo" -> parcial.getPrenda().setEstilo(Estilo.CASUAL);
            case "talla" -> parcial.getInventario().getFirst().setUnidades(0);
            case "precioMin" -> parcial.setPrecio(99.0);
            case "precioMax" -> parcial.setPrecio(101.0);
            default -> throw new AssertionError(filtro);
        }
        if (filtro.equals("talla")) inventario(parcial, Talla.L, 4);
        busqueda(usuario, b -> {
            b.setQuery("Buscada"); b.setTipo(TipoPrenda.SUPERIOR); b.setColor("Negro"); b.setMarca("Radar");
            b.setEstilo(Estilo.FORMAL); b.setTalla(Talla.M); b.setPrecioMin(100.0); b.setPrecioMax(100.0);
        });
        // Act
        var resultado = feed(0, 20);
        // Assert: la más reciente falla un solo filtro y no recibe prioridad de actividad.
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(exacta.getId(), parcial.getId());
    }

    @Test
    void busquedasVaciasNoAnulanActividadSignificativa() {
        // Arrange
        perfil(Set.of(), Set.of());
        var reciente = item("General", Estilo.CASUAL, Talla.L, 4, 10);
        var buscada = item("Buscada", Estilo.CASUAL, Talla.L, 4, 0);
        busqueda(usuario, b -> {});
        busqueda(usuario, b -> { b.setQuery("  "); b.setColor(""); b.setMarca("\t"); });
        busqueda(usuario, b -> b.setQuery("Buscada"));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(buscada.getId(), reciente.getId());
    }

    @Test
    void soloBusquedasVaciasProducenFeedGeneral() {
        // Arrange
        perfil(Set.of(), Set.of());
        var antiguo = item("Antiguo", Estilo.FORMAL, Talla.M, 4, 0);
        var nuevo = item("Nuevo", Estilo.CASUAL, Talla.L, 4, 10);
        busqueda(usuario, b -> b.setQuery(" "));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(nuevo.getId(), antiguo.getId());
    }

    @Test
    void actividadDeOtroUsuarioNoInfluye() {
        // Arrange
        perfil(Set.of(), Set.of());
        var otro = usuario("otro@example.co", Set.of(), Set.of());
        var buscadaPorOtro = item("Privada", Estilo.FORMAL, Talla.M, 4, 0);
        var reciente = item("General", Estilo.CASUAL, Talla.L, 4, 10);
        busqueda(otro, b -> b.setQuery("Privada"));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(reciente.getId(), buscadaPorOtro.getId());
    }

    @Test
    void textoSinCoincidenciaNoUtilizaSimilitudRf16() {
        // Arrange
        perfil(Set.of(), Set.of());
        var similar = item("Camisa formal", Estilo.FORMAL, Talla.M, 4, 0);
        var reciente = item("General", Estilo.CASUAL, Talla.L, 4, 10);
        busqueda(usuario, b -> b.setQuery("Camisa especial"));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(reciente.getId(), similar.getId());
    }

    @Test
    void textoEscapaComodinesDeLike() {
        // Arrange
        perfil(Set.of(), Set.of());
        var literal = item("100%_real", Estilo.CASUAL, Talla.M, 4, 0);
        var otro = item("100extra", Estilo.CASUAL, Talla.M, 4, 10);
        busqueda(usuario, b -> b.setQuery("%_"));
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(literal.getId(), otro.getId());
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "false,false"})
    void feedResuelvePerfilesParcialesYVacioSinBusqueda(boolean estilos, boolean tallas) {
        // Arrange
        perfil(estilos ? Set.of(Estilo.FORMAL) : Set.of(), tallas ? Set.of(Talla.M) : Set.of());
        var primero = item("Estilo", Estilo.FORMAL, Talla.L, 4, 0);
        var segundo = item("Talla", Estilo.CASUAL, Talla.M, 4, 10);
        // Act
        var resultado = feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactlyElementsOf(
                estilos ? List.of(primero.getId(), segundo.getId()) : List.of(segundo.getId(), primero.getId()));
    }

    @Test
    void feedOrdenaGlobalmenteAntesDePaginarSinDuplicados() {
        // Arrange
        var ultimo = item("Ninguno", Estilo.CASUAL, Talla.L, 4, 30);
        var segundo = item("Una", Estilo.CASUAL, Talla.M, 4, 20);
        var primero = item("Ambas", Estilo.FORMAL, Talla.M, 4, 10);
        inventario(primero, Talla.L, 5);
        busqueda(usuario, b -> b.setEstilo(Estilo.FORMAL));
        busqueda(usuario, b -> b.setTalla(Talla.M));
        // Act
        var p0 = feed(0, 1); var p1 = feed(1, 1); var p2 = feed(2, 1);
        // Assert
        assertThat(List.of(p0.getContent().getFirst().getId(), p1.getContent().getFirst().getId(),
                p2.getContent().getFirst().getId())).containsExactly(primero.getId(), segundo.getId(), ultimo.getId());
        assertThat(p0.getTotalElements()).isEqualTo(3);
        assertThat(p0.getTotalPages()).isEqualTo(3);
        assertThat(feed(3, 1).getContent()).isEmpty();
        assertThat(feed(Integer.MAX_VALUE, 100).getTotalElements()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ordenEstablePorFechaDescEIdAsc(boolean recomendaciones) {
        // Arrange
        var viejo = item("Viejo", Estilo.FORMAL, Talla.M, 4, 0);
        var primero = item("Nuevo 1", Estilo.FORMAL, Talla.M, 4, 10);
        var segundo = item("Nuevo 2", Estilo.FORMAL, Talla.M, 4, 10);
        // Act
        var resultado = recomendaciones ? recomendaciones(0, 20).pagina() : feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(primero.getId(), segundo.getId(), viejo.getId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void disponibilidadRealExcluyeCeroSinInventarioYRetirados(boolean recomendaciones) {
        // Arrange
        item("Cero", Estilo.FORMAL, Talla.M, 0, 40);
        var sinInventario = item("Sin inventario", Estilo.FORMAL, Talla.M, 0, 30);
        sinInventario.getInventario().clear();
        var retirado = item("Retirado", Estilo.FORMAL, Talla.M, 4, 20);
        items.delete(retirado);
        var positivo = item("Positivo", Estilo.FORMAL, Talla.M, 4, 10);
        positivo.setEstado(EstadoItem.AGOTADA);
        // Act
        var resultado = recomendaciones ? recomendaciones(0, 20).pagina() : feed(0, 20);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(positivo.getId());
        assertThat(resultado.getContent().getFirst().getEstado()).isEqualTo(EstadoItem.DISPONIBLE);
        assertThat(items.findById(positivo.getId()).orElseThrow().getEstado()).isEqualTo(EstadoItem.AGOTADA);
    }

    @Test
    void recomendacionesExigenEstiloYTallaYNoRellenanIncompatibles() {
        // Arrange
        item("Solo estilo", Estilo.FORMAL, Talla.L, 4, 40);
        item("Solo talla", Estilo.CASUAL, Talla.M, 4, 30);
        item("Ninguno", Estilo.CASUAL, Talla.L, 4, 20);
        var ambos = item("Ambos", Estilo.FORMAL, Talla.M, 4, 10);
        // Act
        var resultado = recomendaciones(0, 20);
        // Assert
        assertThat(resultado.pagina().getContent()).extracting(ItemCatalogo::getId).containsExactly(ambos.getId());
        assertThat(resultado.generalSinPreferencias()).isFalse();
        assertThat(feed(0, 20).getTotalElements()).isEqualTo(4);
    }

    @Test
    void recomendacionesAceptanCualquieraDeVariosEstilosYTallasSinDuplicar() {
        // Arrange
        perfil(Set.of(Estilo.FORMAL, Estilo.URBANO), Set.of(Talla.M, Talla.S));
        var uno = item("Uno", Estilo.URBANO, Talla.S, 4, 0);
        inventario(uno, Talla.M, 4);
        var dos = item("Dos", Estilo.FORMAL, Talla.M, 4, 10);
        item("Incompatible", Estilo.CASUAL, Talla.S, 4, 20);
        // Act
        var resultado = recomendaciones(0, 20);
        // Assert
        assertThat(resultado.pagina().getContent()).extracting(ItemCatalogo::getId).containsExactly(dos.getId(), uno.getId());
        assertThat(resultado.pagina().getTotalElements()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void tallaHabitualSoloCuentaConUnidadesPositivas(int unidades) {
        // Arrange
        var i = item("Inventario", Estilo.FORMAL, Talla.M, unidades, 0);
        inventario(i, Talla.L, 8);
        // Act
        var resultado = recomendaciones(0, 20);
        // Assert
        assertThat(resultado.pagina().getTotalElements()).isEqualTo(unidades > 0 ? 1 : 0);
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true"})
    void recomendacionesConUnaDimensionExigenSoloEsaDimension(boolean estilos, boolean tallas) {
        // Arrange
        perfil(estilos ? Set.of(Estilo.FORMAL) : Set.of(), tallas ? Set.of(Talla.M) : Set.of());
        var estilo = item("Estilo", Estilo.FORMAL, Talla.L, 4, 0);
        var talla = item("Talla", Estilo.CASUAL, Talla.M, 4, 10);
        // Act
        var resultado = recomendaciones(0, 20);
        // Assert
        assertThat(resultado.pagina().getContent()).extracting(ItemCatalogo::getId)
                .containsExactly(estilos ? estilo.getId() : talla.getId());
        assertThat(resultado.generalSinPreferencias()).isFalse();
    }

    @Test
    void perfilVacioDaSeleccionGeneralExplicitaSinUsarActividad() {
        // Arrange
        perfil(Set.of(), Set.of());
        var viejo = item("Buscada", Estilo.FORMAL, Talla.M, 4, 0);
        var nuevo = item("General", Estilo.CASUAL, Talla.L, 4, 10);
        busqueda(usuario, b -> b.setQuery("Buscada"));
        // Act
        var resultado = recomendaciones(0, 20);
        // Assert
        assertThat(resultado.generalSinPreferencias()).isTrue();
        assertThat(resultado.pagina().getContent()).extracting(ItemCatalogo::getId).containsExactly(nuevo.getId(), viejo.getId());
        assertThat(feed(0, 20).getContent()).extracting(ItemCatalogo::getId).containsExactly(viejo.getId(), nuevo.getId());
    }

    @Test
    void perfilSinCoincidenciasDevuelveVacioSinFallback() {
        // Arrange
        item("Otra", Estilo.CASUAL, Talla.L, 4, 0);
        // Act
        var resultado = recomendaciones(0, 20);
        // Assert
        assertThat(resultado.pagina()).isEmpty();
        assertThat(resultado.pagina().getTotalElements()).isZero();
        assertThat(resultado.generalSinPreferencias()).isFalse();
    }

    @Test
    void recomendacionesPaginanCountRealSinDuplicadosNiRelleno() {
        // Arrange
        var viejo = item("Viejo", Estilo.FORMAL, Talla.M, 4, 0);
        var nuevo = item("Nuevo", Estilo.FORMAL, Talla.M, 4, 10);
        inventario(nuevo, Talla.S, 4);
        item("Incompatible", Estilo.CASUAL, Talla.L, 4, 20);
        // Act
        var p0 = recomendaciones(0, 1).pagina(); var p1 = recomendaciones(1, 1).pagina();
        // Assert
        assertThat(p0.getContent()).extracting(ItemCatalogo::getId).containsExactly(nuevo.getId());
        assertThat(p1.getContent()).extracting(ItemCatalogo::getId).containsExactly(viejo.getId());
        assertThat(p0.getTotalElements()).isEqualTo(2);
        assertThat(p0.getTotalPages()).isEqualTo(2);
        var vacia = recomendaciones(2, 1).pagina();
        assertThat(vacia).isEmpty();
        assertThat(vacia.getTotalElements()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usuarioInexistenteEsErrorDeRecurso(boolean recomendaciones) {
        // Arrange
        releer();
        // Act & Assert
        assertThatThrownBy(() -> {
            if (recomendaciones) service.obtenerRecomendaciones(Long.MAX_VALUE, 0, 20);
            else service.obtenerFeed(Long.MAX_VALUE, 0, 20);
        }).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void coleccionesLazyResueltasConCantidadConstanteDeConsultas(boolean recomendaciones) {
        // Arrange
        for (int n = 0; n < 25; n++) {
            var i = item("Item " + n, Estilo.FORMAL, Talla.M, 4, n);
            inventario(i, Talla.L, 4);
        }
        busqueda(usuario, b -> b.setTalla(Talla.M));
        busqueda(usuario, b -> b.setEstilo(Estilo.FORMAL));
        releer();
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        // Act
        var pagina = recomendaciones ? service.obtenerRecomendaciones(usuario.getId(), 0, 20).pagina()
                : service.obtenerFeed(usuario.getId(), 0, 20);
        em.clear();
        // Assert: perfil + actividad (solo feed) + count + IDs + EntityGraph, sin N+1.
        assertThat(pagina.getTotalElements()).isEqualTo(25);
        assertThat(pagina.getContent()).hasSize(20).allSatisfy(i -> {
            assertThat(i.getStock()).isEqualTo(8);
            assertThat(i.getTallasDisponibles()).containsExactlyInAnyOrder(Talla.M, Talla.L);
            assertThat(i.getPrenda().getNombre()).startsWith("Item");
            assertThat(i.getAlmacenNit()).isEqualTo(almacen.getNit());
        });
        assertThat(stats.getPrepareStatementCount()).isEqualTo(recomendaciones ? 4 : 5);
        assertThat(stats.getEntityUpdateCount()).isZero();
    }
}
