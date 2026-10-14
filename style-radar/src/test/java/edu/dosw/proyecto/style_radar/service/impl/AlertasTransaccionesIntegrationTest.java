package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.*;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;

@SpringBootTest @ActiveProfiles("test")
class AlertasTransaccionesIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired IAlertaService service;
    @Autowired ICatalogoService catalogo;
    @Autowired IInventarioService inventario;
    @Autowired IUsuarioService perfiles;
    @Autowired AlertaRepository alertas;
    @Autowired DisponibilidadItemRepository disponibilidad;
    @Autowired ItemCatalogoRepository items;
    @Autowired BusquedaGuardadaRepository busquedas;
    @MockitoSpyBean NotificacionRepository notificaciones;
    Long usuarioId;
    Long itemId;
    Long prendaId;
    Long busquedaId;
    String nit;

    private <T> T tx(Supplier<T> tarea) { return new TransactionTemplate(transactions).execute(status -> tarea.get()); }

    @BeforeEach
    void datosComprometidosEnTransaccionIndependiente() {
        nit = "tx-" + UUID.randomUUID();
        tx(() -> {
            var u = UsuarioEntity.builder().nombre("Comprador").email(nit + "@example.co").fechaRegistro(Instant.now()).build();
            em.persist(u); usuarioId = u.getId();
            em.persist(new AlmacenEntity(nit, "Radar", "Catalogo", "123", "store@example.co", new ArrayList<>()));
            var b = BusquedaGuardadaEntity.builder().nombre("Privada").usuario(u).marca("Radar").fechaCreacion(Instant.now()).build();
            em.persist(b); busquedaId = b.getId();
            var i = catalogo.publicar(nit, new Prenda(null, "Camisa", "Algodon", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL), 100.0);
            itemId = i.getId(); prendaId = i.getPrenda().getId();
            inventario.registrarTallas(nit, itemId, Set.of(Talla.M, Talla.L));
            return null;
        });
    }

    @AfterEach
    void limpiarSoloFixturesPropias() {
        reset(notificaciones);
        tx(() -> {
            em.createQuery("delete NotificacionEntity n where n.usuario.id = :id").setParameter("id", usuarioId).executeUpdate();
            em.createQuery("delete AlertaEntity a where a.usuario.id = :id").setParameter("id", usuarioId).executeUpdate();
            em.createQuery("delete DisponibilidadItemEntity d where d.id = :id").setParameter("id", itemId).executeUpdate();
            em.createQuery("delete BusquedaGuardadaEntity b where b.usuario.id = :id").setParameter("id", usuarioId).executeUpdate();
            var almacen = em.find(AlmacenEntity.class, nit); if (almacen != null) em.remove(almacen);
            em.flush();
            em.remove(em.find(PrendaEntity.class, prendaId));
            em.remove(em.find(UsuarioEntity.class, usuarioId));
            return null;
        });
    }

    @Test
    void alertaYNotificacionSeConservanEntreTransaccionesYContextos() {
        // Arrange
        var a = service.crear(usuarioId, null, itemId);
        // Act
        inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 5);
        // Assert: cada llamada abre y cierra su transacción sin Open Session in View.
        assertThat(service.consultar(usuarioId)).containsExactly(a);
        assertThat(service.notificaciones(usuarioId, 0, 20)).singleElement().satisfies(n -> {
            assertThat(n.alertaId()).isEqualTo(a.id()); assertThat(n.itemCatalogoId()).isEqualTo(itemId);
        });
        tx(() -> {
            var n = notificaciones.findAll().stream().filter(x -> x.getUsuario().getId().equals(usuarioId)).findFirst().orElseThrow();
            assertThat(Hibernate.isInitialized(n.getUsuario())).isFalse();
            assertThat(Hibernate.isInitialized(n.getAlerta())).isFalse();
            return null;
        });
    }

    @Test
    void errorTecnicoDeNotificacionRevierteStockEstadoCicloYPermiteReintento() {
        // Arrange
        service.crear(usuarioId, null, itemId);
        doThrow(new DataAccessResourceFailureException("fallo tecnico simulado")).when(notificaciones).saveAndFlush(any(NotificacionEntity.class));
        // Act & Assert
        assertThatThrownBy(() -> inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 5))
                .isInstanceOf(DataAccessResourceFailureException.class);
        reset(notificaciones);
        tx(() -> {
            var i = items.findById(itemId).orElseThrow();
            assertThat(i.getInventario()).extracting(InventarioTallaEntity::getUnidades).containsOnly(0);
            assertThat(i.getEstado()).isEqualTo(EstadoItem.AGOTADA);
            var d = disponibilidad.findById(itemId).orElseThrow();
            assertThat(d.isPrimeraDisponibilidad()).isFalse(); assertThat(d.getCiclo()).isZero();
            return null;
        });
        assertThat(service.notificaciones(usuarioId, 0, 20)).isEmpty();
        inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 5);
        assertThat(service.notificaciones(usuarioId, 0, 20)).hasSize(1);
    }

    @Test
    void rollbackPosteriorAEventoNoDejaNotificacionesFalsasNiCambiosDeStock() {
        // Arrange
        service.crear(usuarioId, null, itemId);
        // Act & Assert
        assertThatThrownBy(() -> tx(() -> {
            inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 3);
            assertThat(service.notificaciones(usuarioId, 0, 20)).hasSize(1);
            throw new IllegalStateException("fallo posterior");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(service.notificaciones(usuarioId, 0, 20)).isEmpty();
        tx(() -> {
            assertThat(disponibilidad.findById(itemId).orElseThrow().getCiclo()).isZero();
            assertThat(items.findById(itemId).orElseThrow().getInventario()).extracting(InventarioTallaEntity::getUnidades).containsOnly(0);
            return null;
        });
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void creacionConcurrenteSoloPermiteUnaAlertaActiva(boolean busqueda) throws Exception {
        // Arrange / Act
        var resultados = paralelo(() -> crear(busqueda), () -> crear(busqueda));
        // Assert
        assertThat(resultados).containsExactlyInAnyOrder("creada", "conflicto");
        assertThat(service.consultar(usuarioId)).hasSize(1);
    }

    private String crear(boolean busqueda) {
        try { service.crear(usuarioId, busqueda ? busquedaId : null, busqueda ? null : itemId); return "creada"; }
        catch (DataIntegrityViolationException e) { return "conflicto"; }
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void reposicionConcurrenteEnMismaODistintaTallaNoDuplica(boolean misma) throws Exception {
        // Arrange
        service.crear(usuarioId, null, itemId);
        // Act
        paralelo(() -> { inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 2); return "ok"; },
                () -> { inventario.actualizarDisponibilidad(nit, itemId, misma ? Talla.M : Talla.L, 3); return "ok"; });
        // Assert
        assertThat(service.notificaciones(usuarioId, 0, 20)).hasSize(1);
        assertThat(tx(() -> disponibilidad.findById(itemId).orElseThrow().getCiclo())).isEqualTo(1);
    }

    @Test
    void nuevaCoincidenciaConcurrenteEsUnicaPorAlertaEItem() throws Exception {
        // Arrange: búsqueda antes de una nueva publicación creada en otra transacción.
        service.crear(usuarioId, busquedaId, null);
        Long nuevaId = catalogo.publicar(nit, new Prenda(null, "Nueva", "Algodon", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL), 100.0).getId();
        Long nuevaPrenda = tx(() -> items.findById(nuevaId).orElseThrow().getPrenda().getId());
        inventario.registrarTallas(nit, nuevaId, Set.of(Talla.M, Talla.L));
        try {
            // Act
            paralelo(() -> { inventario.actualizarDisponibilidad(nit, nuevaId, Talla.M, 2); return "ok"; },
                    () -> { inventario.actualizarDisponibilidad(nit, nuevaId, Talla.L, 3); return "ok"; });
            // Assert
            assertThat(service.notificaciones(usuarioId, 0, 20)).singleElement()
                    .extracting(Notificacion::tipoEvento).isEqualTo(TipoEventoNotificacion.NUEVA_COINCIDENCIA);
        } finally {
            tx(() -> { catalogo.retirar(nit, nuevaId); em.remove(em.find(PrendaEntity.class, nuevaPrenda)); return null; });
        }
    }

    @Test
    void coincidenciaTardiaConcurrenteEsUnicaSinDuplicarReposicionTotal() throws Exception {
        // Arrange: búsqueda activa antes de publicar, con primera disponibilidad en otra talla.
        var busqueda = configurarAlertaDeportivaM();
        Long nuevaId = publicarDeportiva();
        var seguimiento = service.crear(usuarioId, null, nuevaId);
        try {
            inventario.actualizarDisponibilidad(nit, nuevaId, Talla.L, 5);
            assertThat(service.notificaciones(usuarioId, 0, 20)).singleElement()
                    .extracting(Notificacion::alertaId).isEqualTo(seguimiento.id());
            // Act: dos solicitudes de M compiten en transacciones independientes.
            paralelo(() -> { inventario.actualizarDisponibilidad(nit, nuevaId, Talla.M, 3); return "ok"; },
                    () -> { inventario.actualizarDisponibilidad(nit, nuevaId, Talla.M, 8); return "ok"; });
            // Assert
            var eventos = service.notificaciones(usuarioId, 0, 20).getContent();
            assertThat(eventos).hasSize(2);
            assertThat(eventos).filteredOn(n -> n.tipoEvento() == TipoEventoNotificacion.NUEVA_COINCIDENCIA)
                    .singleElement().extracting(Notificacion::alertaId).isEqualTo(busqueda.id());
            assertThat(eventos).filteredOn(n -> n.tipoEvento() == TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA)
                    .singleElement().extracting(Notificacion::alertaId).isEqualTo(seguimiento.id());
            assertThat(tx(() -> disponibilidad.findById(nuevaId).orElseThrow().getCiclo())).isEqualTo(1);
        } finally { retirarNueva(nuevaId); }
    }

    @Test
    void falloDeCoincidenciaTardiaRevierteSoloLaActualizacionActualYPermiteReintento() {
        // Arrange
        configurarAlertaDeportivaM(); Long nuevaId = publicarDeportiva();
        var seguimiento = service.crear(usuarioId, null, nuevaId);
        try {
            inventario.actualizarDisponibilidad(nit, nuevaId, Talla.L, 5);
            doThrow(new DataAccessResourceFailureException("fallo tecnico simulado"))
                    .when(notificaciones).saveAndFlush(any(NotificacionEntity.class));
            // Act & Assert
            assertThatThrownBy(() -> inventario.actualizarDisponibilidad(nit, nuevaId, Talla.M, 3))
                    .isInstanceOf(DataAccessResourceFailureException.class);
            reset(notificaciones);
            tx(() -> {
                var i = items.findById(nuevaId).orElseThrow();
                assertThat(i.getInventario()).filteredOn(v -> v.getTalla() == Talla.M).singleElement()
                        .extracting(InventarioTallaEntity::getUnidades).isEqualTo(0);
                assertThat(i.getInventario()).filteredOn(v -> v.getTalla() == Talla.L).singleElement()
                        .extracting(InventarioTallaEntity::getUnidades).isEqualTo(5);
                assertThat(disponibilidad.findById(nuevaId).orElseThrow().getCiclo()).isEqualTo(1);
                return null;
            });
            assertThat(service.notificaciones(usuarioId, 0, 20)).singleElement()
                    .extracting(Notificacion::alertaId).isEqualTo(seguimiento.id());
            inventario.actualizarDisponibilidad(nit, nuevaId, Talla.M, 3);
            assertThat(service.notificaciones(usuarioId, 0, 20)).hasSize(2);
        } finally { reset(notificaciones); retirarNueva(nuevaId); }
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void variosHistorialesNullPermitenSoloUnaNuevaActivaConcurrente(boolean busqueda) throws Exception {
        // Arrange: dos filas históricas del mismo usuario y objetivo con activa_unica NULL.
        var primera = service.crear(usuarioId, busqueda ? busquedaId : null, busqueda ? null : itemId);
        service.desactivar(usuarioId, primera.id(), false);
        var segunda = service.crear(usuarioId, busqueda ? busquedaId : null, busqueda ? null : itemId);
        service.desactivar(usuarioId, segunda.id(), false);
        // Act
        var resultados = paralelo(() -> crear(busqueda), () -> crear(busqueda));
        // Assert
        assertThat(resultados).containsExactlyInAnyOrder("creada", "conflicto");
        var historial = service.consultar(usuarioId);
        assertThat(historial).hasSize(3);
        assertThat(historial).filteredOn(Alerta::activa).hasSize(1);
        assertThat(historial).filteredOn(a -> !a.activa()).hasSize(2);
        tx(() -> {
            var entidades = alertas.findByUsuario_IdOrderByFechaCreacionDescIdDesc(usuarioId);
            assertThat(entidades).filteredOn(a -> !a.isActiva()).extracting(AlertaEntity::getActivaUnica).containsOnlyNulls();
            return null;
        });
    }

    private Alerta configurarAlertaDeportivaM() {
        tx(() -> {
            var b = busquedas.findById(busquedaId).orElseThrow();
            b.setTalla(Talla.M); b.setEstilo(Estilo.DEPORTIVO);
            return null;
        });
        return service.crear(usuarioId, busquedaId, null);
    }

    private Long publicarDeportiva() {
        Long id = catalogo.publicar(nit, new Prenda(null, "Deportiva", "Algodon", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.DEPORTIVO), 100.0).getId();
        inventario.registrarTallas(nit, id, Set.of(Talla.M, Talla.L));
        return id;
    }

    private void retirarNueva(Long id) {
        tx(() -> {
            Long prenda = items.findById(id).orElseThrow().getPrenda().getId();
            catalogo.retirar(nit, id); em.remove(em.find(PrendaEntity.class, prenda));
            return null;
        });
    }

    @Test
    void restriccionPersistenteImpideDuplicadoInclusoSinServicio() {
        // Arrange
        service.crear(usuarioId, null, itemId);
        // Act & Assert
        assertThatThrownBy(() -> tx(() -> {
            var a = new AlertaEntity(); a.setUsuario(em.getReference(UsuarioEntity.class, usuarioId));
            a.setTipoObjetivo(TipoObjetivoAlerta.ITEM_CATALOGO); a.setIdentificadorObjetivo(itemId); a.setFechaCreacion(Instant.now());
            alertas.saveAndFlush(a); return null;
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(service.consultar(usuarioId)).hasSize(1);
    }

    @Test
    void restriccionPersistenteImpideNotificacionDuplicada() {
        // Arrange
        var a = service.crear(usuarioId, null, itemId); inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 3);
        // Act & Assert
        assertThatThrownBy(() -> tx(() -> {
            var n = new NotificacionEntity(); n.setUsuario(em.getReference(UsuarioEntity.class, usuarioId));
            n.setAlerta(em.getReference(AlertaEntity.class, a.id())); n.setItemCatalogoId(itemId);
            n.setTipoEvento(TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA); n.setCiclo(1);
            n.setFechaCreacion(Instant.now()); n.setMensaje("duplicado"); notificaciones.saveAndFlush(n); return null;
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(service.notificaciones(usuarioId, 0, 20)).hasSize(1);
    }

    @ParameterizedTest @ValueSource(strings = {"objetivo", "actividad", "ciclo", "fk"})
    void restriccionesSqlProtegenIntegridad(String regla) {
        // Arrange
        var a = service.crear(usuarioId, null, itemId); inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 3);
        // Act & Assert
        assertThatThrownBy(() -> tx(() -> {
            String sql = switch (regla) {
                case "objetivo" -> "update alertas set identificador_objetivo = 0 where id = :id";
                case "actividad" -> "update alertas set activa_unica = null where id = :id";
                case "ciclo" -> "update notificaciones set ciclo = 0 where alerta_id = :id";
                case "fk" -> "delete from alertas where id = :id";
                default -> throw new AssertionError(regla);
            };
            em.createNativeQuery(sql).setParameter("id", a.id()).executeUpdate(); return null;
        })).hasRootCauseInstanceOf(org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException.class);
        assertThat(service.notificaciones(usuarioId, 0, 20)).hasSize(1);
    }

    @Test
    void indicesYTablasExistenEnH2() {
        // Act & Assert
        tx(() -> {
            var indices = em.createNativeQuery("select index_name from information_schema.indexes where table_name in ('alertas','notificaciones')", String.class).getResultList();
            assertThat(indices).contains("ix_alerta_usuario_fecha", "ix_alerta_objetivo", "ix_notificacion_usuario_fecha");
            assertThat(em.createNativeQuery("select count(*) from disponibilidad_items").getSingleResult()).isNotNull();
            return null;
        });
    }

    private <T> List<T> paralelo(Supplier<T> primera, Supplier<T> segunda) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var preparadas = new CountDownLatch(2); var inicio = new CountDownLatch(1);
        try {
            Callable<T> a = () -> { preparadas.countDown(); if (!inicio.await(10, TimeUnit.SECONDS)) throw new TimeoutException(); return primera.get(); };
            Callable<T> b = () -> { preparadas.countDown(); if (!inicio.await(10, TimeUnit.SECONDS)) throw new TimeoutException(); return segunda.get(); };
            var f1 = pool.submit(a); var f2 = pool.submit(b);
            assertThat(preparadas.await(10, TimeUnit.SECONDS)).isTrue(); inicio.countDown();
            return List.of(f1.get(20, TimeUnit.SECONDS), f2.get(20, TimeUnit.SECONDS));
        } finally { inicio.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS); }
    }
}
