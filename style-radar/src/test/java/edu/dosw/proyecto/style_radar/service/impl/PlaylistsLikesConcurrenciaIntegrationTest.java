package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.PlaylistRepository;
import edu.dosw.proyecto.style_radar.service.IPlaylistService;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;
import jakarta.persistence.EntityManager;

@SpringBootTest @ActiveProfiles("test")
class PlaylistsLikesConcurrenciaIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired IPlaylistService servicio;
    @Autowired PlaylistRepository repository;
    @MockitoSpyBean UsuarioValidator usuarioValidator;
    Long a, b, playlistId;
    <T> T tx(Supplier<T> trabajo) { return new TransactionTemplate(transactions).execute(s -> trabajo.get()); }
    @BeforeEach void preparar() {
        tx(() -> {
            var ua = usuario(); var ub = usuario(); a = ua.getId(); b = ub.getId();
            var p = PlaylistEntity.builder().usuario(ua).nombre("Concurrente").visibilidad(VisibilidadPlaylist.PUBLICA)
                    .fechaCreacion(Instant.parse("2026-10-01T12:00:00Z")).build();
            em.persist(p); playlistId = p.getId(); return null;
        });
    }
    UsuarioEntity usuario() {
        var u = UsuarioEntity.builder().nombre("Comprador").email(UUID.randomUUID() + "@example.co")
                .fechaRegistro(Instant.parse("2026-10-01T12:00:00Z")).build(); em.persist(u); return u;
    }
    @AfterEach void limpiar() {
        reset(usuarioValidator);
        tx(() -> { em.remove(em.find(PlaylistEntity.class, playlistId));
            em.remove(em.find(UsuarioEntity.class, a)); em.remove(em.find(UsuarioEntity.class, b)); return null; });
    }
    void esperar(CountDownLatch latch) {
        try { if (!latch.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de coordinación"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
    void simultaneas(Long primerUsuario, Long segundoUsuario) throws Exception {
        var retenida = new CountDownLatch(1); var intenta = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        // Interceptar un método concreto, no el método abstracto del proxy Spring Data.
        doAnswer(i -> { i.callRealMethod();
            if (Thread.currentThread().getName().equals("rf72-segunda")) intenta.countDown();
            return null; }).when(usuarioValidator).validarExiste(anyLong());
        var pool = Executors.newFixedThreadPool(2);
        try {
            var primero = pool.submit(() -> {
                try { return tx(() -> {
                    var p = servicio.alternarLike(primerUsuario, playlistId); em.flush(); retenida.countDown(); esperar(liberar); return p;
                }); } catch (RuntimeException e) { retenida.countDown(); throw e; }
            });
            assertThat(retenida.await(15, TimeUnit.SECONDS)).isTrue();
            if (primero.isDone()) primero.get(1, TimeUnit.SECONDS);
            var segundo = pool.submit(() -> { Thread.currentThread().setName("rf72-segunda");
                return servicio.alternarLike(segundoUsuario, playlistId); });
            assertThat(intenta.await(15, TimeUnit.SECONDS)).isTrue(); assertThat(segundo.isDone()).isFalse();
            liberar.countDown(); primero.get(25, TimeUnit.SECONDS); segundo.get(25, TimeUnit.SECONDS);
        } finally { liberar.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(25, TimeUnit.SECONDS)).isTrue(); reset(usuarioValidator); }
    }
    @Test void dosUsuariosAgreganLikesSinPerderActualizaciones() throws Exception {
        simultaneas(a, b); comprobar(Set.of(a, b));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void retirarMientrasOtroAgregaConservaColeccion(boolean retirarPrimero) throws Exception {
        servicio.alternarLike(a, playlistId);
        simultaneas(retirarPrimero ? a : b, retirarPrimero ? b : a); comprobar(Set.of(b));
    }
    @Test void dosAlternanciasDelMismoUsuarioNoDuplican() throws Exception {
        simultaneas(a, a); comprobar(Set.of());
    }
    void comprobar(Set<Long> esperados) {
        var likes = tx(() -> new HashSet<>(repository.findById(playlistId).orElseThrow().getLikesUsuarios()));
        assertThat(likes).isEqualTo(esperados);
        long filas = tx(() -> ((Number) em.createNativeQuery("select count(*) from playlist_likes where playlist_id=:id")
                .setParameter("id", playlistId).getSingleResult()).longValue());
        assertThat(filas).isEqualTo(esperados.size());
        assertThat(servicio.obtenerPopulares(0, 100).getContent()).filteredOn(p -> p.getId().equals(playlistId))
                .singleElement().satisfies(p -> assertThat(p.getLikesCount()).isEqualTo(esperados.size()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void errorTecnicoDespuesDeFlushRevierteLikeAgregadoORetirado(boolean retirar) {
        if (retirar) servicio.alternarLike(a, playlistId);
        assertThatThrownBy(() -> tx(() -> {
            servicio.alternarLike(a, playlistId); em.flush();
            throw new DataAccessResourceFailureException("Fallo técnico posterior a flush fixture");
        })).isInstanceOf(DataAccessResourceFailureException.class);
        comprobar(retirar ? Set.of(a) : Set.of());
        servicio.alternarLike(b, playlistId); comprobar(retirar ? Set.of(a, b) : Set.of(b));
    }
}
