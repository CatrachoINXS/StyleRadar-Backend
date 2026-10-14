package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import jakarta.persistence.EntityManager;

@SpringBootTest @ActiveProfiles("test")
class VerificacionTransaccionesIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired VerificacionAlmacenService service;
    @Autowired edu.dosw.proyecto.style_radar.service.IInventarioService inventario;
    @Autowired edu.dosw.proyecto.style_radar.service.IAlertaService alertasService;
    @MockitoSpyBean AlmacenRepository almacenes;
    Long itemId, prendaId;
    String nit;
    UsuarioPrincipal adminA, adminB;

    <T> T tx(Supplier<T> task) { return new TransactionTemplate(transactions).execute(status -> task.get()); }

    UsuarioPrincipal admin(long id) {
        var u = new UsuarioEntity(); u.setId(id); u.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        return new UsuarioPrincipal(u, "fixture");
    }

    @BeforeEach void datosComprometidos() {
        nit = "moderacion-tx-" + UUID.randomUUID(); adminA = admin(111L); adminB = admin(222L);
        tx(() -> {
            var a = new AlmacenEntity(nit, "Radar", "Catálogo", "123", "fixture@example.co", new ArrayList<>()); em.persist(a);
            var p = new PrendaEntity(null, "Camisa", "Algodón", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL); em.persist(p); prendaId = p.getId();
            var i = new ItemCatalogoEntity(null, 100.0, EstadoItem.DISPONIBLE, Instant.now(), a, p, new ArrayList<>(), new ArrayList<>());
            a.setEstadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE); em.persist(i); itemId = i.getId();
            em.persist(new InventarioTallaEntity(null, Talla.M, 5, i));
            em.persist(new ImagenCatalogoEntity(null, "image/png", new byte[]{1, 2, 3}, i)); return null;
        });
    }

    @AfterEach void limpiarSoloFixtures() {
        reset(almacenes);
        tx(() -> { em.remove(em.find(AlmacenEntity.class, nit)); em.flush(); em.remove(em.find(PrendaEntity.class, prendaId)); return null; });
    }

    DecisionVerificacionRequestDTO decision(DecisionVerificacionAlmacen d) {
        var r = new DecisionVerificacionRequestDTO(); r.setDecision(d); r.setMotivo("Decisión " + d); return r;
    }

    String decidir(UsuarioPrincipal admin, DecisionVerificacionAlmacen d) {
        try { service.decidir(nit, admin, decision(d)); return d.name(); }
        catch (ReglaDeNegocioException e) { return "transicion-invalida"; }
    }

    @Test void decisionesConcurrentesSoloPermitenUnGanadorConMetadatosConsistentes() throws Exception {
        // Arrange
        var pool = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            // Act
            var a = pool.submit(() -> { inicio.await(); return decidir(adminA, DecisionVerificacionAlmacen.APROBADO); });
            var b = pool.submit(() -> { inicio.await(); return decidir(adminB, DecisionVerificacionAlmacen.RECHAZADO); });
            inicio.countDown(); var resultados = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            // Assert
            assertThat(resultados).contains("transicion-invalida");
            assertThat(resultados.stream().filter(x -> !x.equals("transicion-invalida"))).hasSize(1);
            tx(() -> {
                var i = almacenes.findById(nit).orElseThrow(); var ganador = i.getEstadoVerificacion();
                assertThat(resultados).contains(ganador.name());
                assertThat(i.getAdministradorDecisionId()).isEqualTo(ganador == EstadoVerificacionAlmacen.APROBADO ? 111L : 222L);
                assertThat(i.getMotivoVerificacion()).isEqualTo("Decisión " + ganador);
                assertThat(i.getFechaDecisionVerificacion()).isNotNull(); assertThat(i.getNit()).isEqualTo(nit);
                assertThat(i.getItemsCatalogo().getFirst().getInventario()).singleElement().extracting(InventarioTallaEntity::getUnidades).isEqualTo(5);
                return null;
            });
        } finally { pool.shutdownNow(); }
    }

    @ParameterizedTest @EnumSource(DecisionVerificacionAlmacen.class)
    void falloDePersistenciaRevierteDecisionYMetadatosYPermiteReintentar(DecisionVerificacionAlmacen d) {
        // Arrange
        doThrow(new DataAccessResourceFailureException("fallo técnico simulado")).when(almacenes).flush();
        // Act / Assert
        assertThatThrownBy(() -> service.decidir(nit, adminA, decision(d))).isInstanceOf(DataAccessResourceFailureException.class);
        reset(almacenes);
        tx(() -> {
            var i = almacenes.findById(nit).orElseThrow();
            assertThat(i.getEstadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE);
            assertThat(i.getAdministradorDecisionId()).isNull(); assertThat(i.getFechaDecisionVerificacion()).isNull();
            assertThat(i.getMotivoVerificacion()).isNull(); assertThat(i.getItemsCatalogo().getFirst().getImagenes()).hasSize(1);
            assertThat(i.getItemsCatalogo().getFirst().getInventario()).singleElement().extracting(InventarioTallaEntity::getUnidades).isEqualTo(5);
            return null;
        });
        assertThat(service.decidir(nit, adminA, decision(d)).estadoVerificacion().name()).isEqualTo(d.name());
    }

    @Test void falloAlSolicitarRevisionConservaDecisionAnterior() {
        // Arrange
        var anterior = service.decidir(nit, adminA, decision(DecisionVerificacionAlmacen.RECHAZADO));
        doThrow(new DataAccessResourceFailureException("fallo simulado")).when(almacenes).flush();
        // Act / Assert
        assertThatThrownBy(() -> service.solicitarRevision(nit, adminB)).isInstanceOf(DataAccessResourceFailureException.class);
        reset(almacenes);
        tx(() -> {
            var i = almacenes.findById(nit).orElseThrow();
            assertThat(i.getEstadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.RECHAZADO);
            assertThat(i.getFechaDecisionVerificacion()).isEqualTo(anterior.fechaDecisionVerificacion());
            assertThat(i.getAdministradorDecisionId()).isEqualTo(111L); return null;
        });
    }

    @Test void rollbackPosteriorALaDecisionRevierteInclusoDespuesDelFlush() {
        // Act / Assert
        assertThatThrownBy(() -> tx(() -> {
            service.decidir(nit, adminA, decision(DecisionVerificacionAlmacen.APROBADO));
            throw new IllegalStateException("fallo posterior");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(service.consultar(EstadoVerificacionAlmacen.PENDIENTE, 0, 100, adminA).getContent()).extracting(VerificacionAlmacenResponseDTO::nit).contains(nit);
        tx(() -> { var i = almacenes.findById(nit).orElseThrow(); assertThat(i.getAdministradorDecisionId()).isNull(); return null; });
    }

    Long prepararSeguimiento(EstadoVerificacionAlmacen estado) {
        return tx(() -> {
            em.find(AlmacenEntity.class, nit).setEstadoVerificacion(estado);
            var i = em.find(ItemCatalogoEntity.class, itemId); i.getInventario().getFirst().setUnidades(0); i.setEstado(EstadoItem.AGOTADA);
            var u = UsuarioEntity.builder().nombre("Comprador").email(nit + "@example.co").fechaRegistro(Instant.now()).build(); em.persist(u);
            var a = new AlertaEntity(); a.setUsuario(u); a.setTipoObjetivo(TipoObjetivoAlerta.ITEM_CATALOGO);
            a.setIdentificadorObjetivo(itemId); a.setFechaCreacion(Instant.now()); em.persist(a); return u.getId();
        });
    }

    void limpiarSeguimiento(Long usuarioId) {
        tx(() -> {
            em.createQuery("delete NotificacionEntity n where n.usuario.id = :id").setParameter("id", usuarioId).executeUpdate();
            em.createQuery("delete AlertaEntity a where a.usuario.id = :id").setParameter("id", usuarioId).executeUpdate();
            em.createQuery("delete DisponibilidadItemEntity d where d.id = :id").setParameter("id", itemId).executeUpdate();
            em.remove(em.find(UsuarioEntity.class, usuarioId)); return null;
        });
    }

    @Test void columnaMotivoRestringeLongitudYRevierteCambiosDirectos() {
        // Act / Assert: base H2 aislada, nunca producción.
        assertThatThrownBy(() -> tx(() -> {
            var a = em.find(AlmacenEntity.class, nit); a.setMotivoVerificacion("x".repeat(1001));
            a.setEstadoVerificacion(EstadoVerificacionAlmacen.APROBADO); em.flush(); return null;
        })).hasRootCauseInstanceOf(org.h2.jdbc.JdbcSQLDataException.class);
        tx(() -> { var a = em.find(AlmacenEntity.class, nit); assertThat(a.getMotivoVerificacion()).isNull();
            assertThat(a.getEstadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE); return null; });
    }

    void esperar(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("No se alcanzó el punto de sincronización"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void decisionQueOcultaSerializaStockYReleeEstadoAunqueEstuvieraCargado(boolean reabrir) throws Exception {
        // Arrange: dos transacciones reales y un contexto de stock con estado previo cargado.
        Long usuarioId = prepararSeguimiento(reabrir ? EstadoVerificacionAlmacen.APROBADO : EstadoVerificacionAlmacen.PENDIENTE);
        var pool = Executors.newFixedThreadPool(2);
        var cargado = new CountDownLatch(1); var decidido = new CountDownLatch(1);
        var stockIniciado = new CountDownLatch(1); var liberarDecision = new CountDownLatch(1);
        try {
            var stock = pool.submit(() -> tx(() -> {
                em.find(AlmacenEntity.class, nit); cargado.countDown(); esperar(decidido); stockIniciado.countDown();
                inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 5); return "stock";
            }));
            esperar(cargado);
            var decision = pool.submit(() -> tx(() -> {
                if (reabrir) service.solicitarRevision(nit, adminA);
                else service.decidir(nit, adminA, decision(DecisionVerificacionAlmacen.RECHAZADO));
                decidido.countDown(); esperar(liberarDecision); return "decision";
            }));
            // Act / Assert: stock espera a que se confirme la decisión.
            esperar(stockIniciado);
            assertThatThrownBy(() -> stock.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            liberarDecision.countDown(); assertThat(decision.get(20, TimeUnit.SECONDS)).isEqualTo("decision");
            assertThat(stock.get(20, TimeUnit.SECONDS)).isEqualTo("stock");
            assertThat(alertasService.notificaciones(usuarioId, 0, 20)).isEmpty();
            tx(() -> { assertThat(em.find(ItemCatalogoEntity.class, itemId).getInventario().getFirst().getUnidades()).isEqualTo(5); return null; });
        } finally {
            decidido.countDown(); liberarDecision.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS); limpiarSeguimiento(usuarioId);
        }
    }

    @Test void stockConfirmadoAntesDeReabrirConservaSuNotificacionHistorica() throws Exception {
        // Arrange
        Long usuarioId = prepararSeguimiento(EstadoVerificacionAlmacen.APROBADO);
        var pool = Executors.newFixedThreadPool(2); var notificado = new CountDownLatch(1);
        var iniciarRevision = new CountDownLatch(1); var liberarStock = new CountDownLatch(1);
        try {
            var stock = pool.submit(() -> tx(() -> {
                inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 5);
                notificado.countDown(); esperar(liberarStock); return "stock";
            }));
            esperar(notificado);
            var revision = pool.submit(() -> { iniciarRevision.countDown(); return service.solicitarRevision(nit, adminB); });
            // Act / Assert
            esperar(iniciarRevision); assertThatThrownBy(() -> revision.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            liberarStock.countDown(); assertThat(stock.get(20, TimeUnit.SECONDS)).isEqualTo("stock");
            assertThat(revision.get(20, TimeUnit.SECONDS).estadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE);
            assertThat(alertasService.notificaciones(usuarioId, 0, 20)).hasSize(1);
        } finally {
            liberarStock.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS); limpiarSeguimiento(usuarioId);
        }
    }
}


