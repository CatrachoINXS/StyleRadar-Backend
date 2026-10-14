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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.CambioEstadoCuentaRequestDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.*;
import jakarta.persistence.EntityManager;

@SpringBootTest @ActiveProfiles("test")
class GestionEstadoCuentaEventosConcurrentesIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired GestionEstadoCuentaService gestion;
    @Autowired IAlertaService alertas;
    @Autowired IInventarioService inventario;
    @MockitoSpyBean BloqueoCuentaService bloqueo;
    Long adminId, compradorId, itemId, prendaId;
    String nit;
    UsuarioPrincipal admin;
    <T> T tx(Supplier<T> tarea) { return new TransactionTemplate(transactions).execute(s -> tarea.get()); }

    @BeforeEach void prepararComprometido() {
        // Arrange
        nit = "rf66-evento-" + UUID.randomUUID();
        tx(() -> {
            var a = UsuarioEntity.builder().nombre("Admin").email(nit + "-admin@example.co")
                    .fechaRegistro(Instant.now()).roles(Set.of(Rol.ADMIN_STYLERADAR)).build();
            em.persist(a); adminId = a.getId(); admin = new UsuarioPrincipal(a, "");
            var u = UsuarioEntity.builder().nombre("Comprador").email(nit + "@example.co").fechaRegistro(Instant.now()).build();
            em.persist(u); compradorId = u.getId();
            var almacen = new AlmacenEntity(nit, "Radar", "Catálogo", "123", "fixture@example.co", new ArrayList<>()); em.persist(almacen);
            var p = new PrendaEntity(null, "Camisa", "Algodón", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL);
            em.persist(p); prendaId = p.getId();
            var item = new ItemCatalogoEntity(null, 100.0, EstadoItem.AGOTADA, Instant.now(), almacen, p, new ArrayList<>(), new ArrayList<>());
            em.persist(item); itemId = item.getId(); em.persist(new InventarioTallaEntity(null, Talla.M, 0, item)); return null;
        });
        alertas.crear(compradorId, null, itemId);
    }
    @AfterEach void limpiarFixtures() {
        reset(bloqueo);
        tx(() -> {
            em.createQuery("delete NotificacionEntity n where n.usuario.id = :id").setParameter("id", compradorId).executeUpdate();
            em.createQuery("delete AlertaEntity a where a.usuario.id = :id").setParameter("id", compradorId).executeUpdate();
            em.createQuery("delete DisponibilidadItemEntity d where d.id = :id").setParameter("id", itemId).executeUpdate();
            em.remove(em.find(ItemCatalogoEntity.class, itemId)); em.flush(); em.remove(em.find(PrendaEntity.class, prendaId));
            em.remove(em.find(AlmacenEntity.class, nit)); em.remove(em.find(UsuarioEntity.class, compradorId));
            em.remove(em.find(UsuarioEntity.class, adminId)); return null;
        });
    }
    void esperar(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de coordinación"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
    void cambiar(EstadoCuenta estado) {
        var r = new CambioEstadoCuentaRequestDTO(); r.setEstado(estado); gestion.cambiar(compradorId, admin, r);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void eventoYCambioDeCuentaSeSerializanBajoElMismoLock(boolean eventoPrimero) throws Exception {
        // Arrange: la primera transacción retiene el lock hasta que la segunda intenta tomarlo.
        var primeraBloqueada = new CountDownLatch(1); var segundaIntenta = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        tx(() -> { doAnswer(i -> {
            boolean primera = Thread.currentThread().getName().equals("rf66-primera");
            if (!primera) segundaIntenta.countDown();
            Object u = i.callRealMethod();
            if (primera) { primeraBloqueada.countDown(); esperar(liberar); }
            return u;
        }).when(bloqueo).bloquear(anyCollection()); return null; });
        var pool = Executors.newFixedThreadPool(2);
        try {
            // Act
            var uno = pool.submit(() -> {
                Thread.currentThread().setName("rf66-primera");
                if (eventoPrimero) inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 3); else cambiar(EstadoCuenta.INACTIVA);
            });
            assertThat(primeraBloqueada.await(10, TimeUnit.SECONDS)).isTrue();
            var dos = pool.submit(() -> {
                Thread.currentThread().setName("rf66-segunda");
                if (eventoPrimero) cambiar(EstadoCuenta.INACTIVA); else inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 3);
            });
            assertThat(segundaIntenta.await(10, TimeUnit.SECONDS)).isTrue(); liberar.countDown();
            uno.get(20, TimeUnit.SECONDS); dos.get(20, TimeUnit.SECONDS); reset(bloqueo);
            // Assert: un evento anterior puede conservarse; después del cambio no se genera.
            assertThat(alertas.notificaciones(compradorId, 0, 20)).hasSize(eventoPrimero ? 1 : 0);
            assertThat(gestion.consultarUno(compradorId, admin).estadoCuenta()).isEqualTo(EstadoCuenta.INACTIVA);
            assertThat(alertas.consultar(compradorId)).hasSize(1);
            cambiar(EstadoCuenta.ACTIVA);
            assertThat(alertas.notificaciones(compradorId, 0, 20)).hasSize(eventoPrimero ? 1 : 0);
            inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 0);
            inventario.actualizarDisponibilidad(nit, itemId, Talla.M, 4);
            assertThat(alertas.notificaciones(compradorId, 0, 20)).hasSize(eventoPrimero ? 2 : 1);
        } finally { liberar.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue(); }
    }
}
