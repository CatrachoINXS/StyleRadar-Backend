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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionModeracionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.ModeracionItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import jakarta.persistence.EntityManager;

@SpringBootTest @ActiveProfiles("test")
class ModeracionTransaccionesIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ModeracionCatalogoService service;
    @MockitoSpyBean ItemCatalogoRepository items;
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
            i.setEstadoModeracion(EstadoModeracion.PENDIENTE); em.persist(i); itemId = i.getId();
            em.persist(new InventarioTallaEntity(null, Talla.M, 5, i));
            em.persist(new ImagenCatalogoEntity(null, "image/png", new byte[]{1, 2, 3}, i)); return null;
        });
    }

    @AfterEach void limpiarSoloFixtures() {
        reset(items);
        tx(() -> { em.remove(em.find(AlmacenEntity.class, nit)); em.flush(); em.remove(em.find(PrendaEntity.class, prendaId)); return null; });
    }

    DecisionModeracionRequestDTO decision(DecisionModeracion d) {
        var r = new DecisionModeracionRequestDTO(); r.setDecision(d); r.setMotivo("Decisión " + d); return r;
    }

    String decidir(UsuarioPrincipal admin, DecisionModeracion d) {
        try { service.decidir(itemId, admin, decision(d)); return d.name(); }
        catch (ReglaDeNegocioException e) { return "transicion-invalida"; }
    }

    @Test void decisionesConcurrentesSoloPermitenUnGanadorConMetadatosConsistentes() throws Exception {
        var pool = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            var a = pool.submit(() -> { inicio.await(); return decidir(adminA, DecisionModeracion.APROBADA); });
            var b = pool.submit(() -> { inicio.await(); return decidir(adminB, DecisionModeracion.RECHAZADA); });
            inicio.countDown(); var resultados = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertThat(resultados).contains("transicion-invalida");
            assertThat(resultados.stream().filter(x -> !x.equals("transicion-invalida"))).hasSize(1);
            tx(() -> {
                var i = items.findById(itemId).orElseThrow(); var ganador = i.getEstadoModeracion();
                assertThat(resultados).contains(ganador.name());
                assertThat(i.getAdministradorDecisionId()).isEqualTo(ganador == EstadoModeracion.APROBADA ? 111L : 222L);
                assertThat(i.getMotivoModeracion()).isEqualTo("Decisión " + ganador);
                assertThat(i.getFechaDecisionModeracion()).isNotNull(); assertThat(i.getPrecio()).isEqualTo(100.0);
                assertThat(i.getInventario()).singleElement().extracting(InventarioTallaEntity::getUnidades).isEqualTo(5);
                return null;
            });
        } finally { pool.shutdownNow(); }
    }

    @ParameterizedTest @EnumSource(DecisionModeracion.class)
    void falloDePersistenciaRevierteDecisionYMetadatosYPermiteReintentar(DecisionModeracion d) {
        doThrow(new DataAccessResourceFailureException("fallo técnico simulado")).when(items).flush();
        assertThatThrownBy(() -> service.decidir(itemId, adminA, decision(d))).isInstanceOf(DataAccessResourceFailureException.class);
        reset(items);
        tx(() -> {
            var i = items.findById(itemId).orElseThrow();
            assertThat(i.getEstadoModeracion()).isEqualTo(EstadoModeracion.PENDIENTE);
            assertThat(i.getAdministradorDecisionId()).isNull(); assertThat(i.getFechaDecisionModeracion()).isNull();
            assertThat(i.getMotivoModeracion()).isNull(); assertThat(i.getImagenes()).hasSize(1);
            assertThat(i.getInventario()).singleElement().extracting(InventarioTallaEntity::getUnidades).isEqualTo(5);
            return null;
        });
        assertThat(service.decidir(itemId, adminA, decision(d)).estadoModeracion().name()).isEqualTo(d.name());
    }

    @Test void falloAlSolicitarRevisionConservaDecisionAnterior() {
        var anterior = service.decidir(itemId, adminA, decision(DecisionModeracion.RECHAZADA));
        doThrow(new DataAccessResourceFailureException("fallo simulado")).when(items).flush();
        assertThatThrownBy(() -> service.solicitarRevision(itemId, adminB)).isInstanceOf(DataAccessResourceFailureException.class);
        reset(items);
        tx(() -> {
            var i = items.findById(itemId).orElseThrow();
            assertThat(i.getEstadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADA);
            assertThat(i.getFechaDecisionModeracion()).isEqualTo(anterior.fechaDecisionModeracion());
            assertThat(i.getAdministradorDecisionId()).isEqualTo(111L); return null;
        });
    }

    @Test void rollbackPosteriorALaDecisionRevierteInclusoDespuesDelFlush() {
        assertThatThrownBy(() -> tx(() -> {
            service.decidir(itemId, adminA, decision(DecisionModeracion.APROBADA));
            throw new IllegalStateException("fallo posterior");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(service.consultar(EstadoModeracion.PENDIENTE, 0, 100).getContent()).extracting(ModeracionItemResponseDTO::itemId).contains(itemId);
        tx(() -> { var i = items.findById(itemId).orElseThrow(); assertThat(i.getAdministradorDecisionId()).isNull(); return null; });
    }
}
