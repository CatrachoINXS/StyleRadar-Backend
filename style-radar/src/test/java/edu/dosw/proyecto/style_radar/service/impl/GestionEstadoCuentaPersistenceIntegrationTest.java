package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.CambioEstadoCuentaRequestDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import jakarta.persistence.EntityManager;

@SpringBootTest @ActiveProfiles("test")
class GestionEstadoCuentaPersistenceIntegrationTest {
    static final String HASH = new BCryptPasswordEncoder(4).encode("fixture password");
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired GestionEstadoCuentaService service;
    @Autowired CredencialRepository credenciales;
    @MockitoSpyBean UsuarioRepository usuarios;
    @MockitoSpyBean edu.dosw.proyecto.style_radar.service.BloqueoCuentaService bloqueo;
    UsuarioPrincipal adminA, adminB;
    Long aId, bId, objetivoId;
    List<Long> propias;

    <T> T tx(Supplier<T> tarea) { return new TransactionTemplate(transactions).execute(s -> tarea.get()); }

    UsuarioEntity crear(Rol rol, EstadoCuenta estado, boolean credencial) {
        var u = UsuarioEntity.builder().nombre("RF66").email(UUID.randomUUID() + "@example.co")
                .fechaRegistro(Instant.parse("2026-10-01T12:00:00Z")).roles(new HashSet<>(Set.of(rol)))
                .estadoCuenta(estado).telefono("123").preferenciasEstilo(new HashSet<>(Set.of(Estilo.FORMAL)))
                .tallasHabitualesPorTipo(new HashSet<>()).build();
        em.persist(u); propias.add(u.getId()); if (credencial) em.persist(new CredencialEntity(u, HASH)); return u;
    }
    @BeforeEach void prepararDatosComprometidos() {
        // Arrange: cada servicio se ejecuta en su propia transacción y contexto.
        propias = new ArrayList<>();
        tx(() -> {
            var a = crear(Rol.ADMIN_STYLERADAR, EstadoCuenta.ACTIVA, true); aId = a.getId(); adminA = new UsuarioPrincipal(a, "");
            var b = crear(Rol.ADMIN_STYLERADAR, EstadoCuenta.ACTIVA, true); bId = b.getId(); adminB = new UsuarioPrincipal(b, "");
            objetivoId = crear(Rol.COMPRADOR, EstadoCuenta.ACTIVA, true).getId(); return null;
        });
    }
    @AfterEach void limpiarSoloDatosPropios() {
        reset(usuarios, bloqueo);
        tx(() -> {
            for (Long id : propias) {
                credenciales.findById(id).ifPresent(em::remove); em.flush();
                var u = em.find(UsuarioEntity.class, id); if (u != null) em.remove(u);
            }
            return null;
        });
    }
    CambioEstadoCuentaRequestDTO request(EstadoCuenta estado, String motivo) {
        var r = new CambioEstadoCuentaRequestDTO(); r.setEstado(estado); r.setMotivo(motivo); return r;
    }

    @ParameterizedTest @EnumSource(EstadoCuenta.class)
    void estadoStringYMetadatosPersistenSinAlterarDatosAnteriores(EstadoCuenta estado) {
        // Arrange
        tx(() -> { em.find(UsuarioEntity.class, objetivoId).setEstadoCuenta(EstadoCuenta.INACTIVA); return null; });
        // Act
        var respuesta = service.cambiar(objetivoId, adminA, request(estado, "  Motivo  "));
        // Assert
        tx(() -> {
            var u = usuarios.findById(objetivoId).orElseThrow();
            assertThat(em.createNativeQuery("select estado_cuenta from usuarios where id = :id", String.class)
                    .setParameter("id", objetivoId).getSingleResult()).isEqualTo(estado.name());
            assertThat(u.getFechaCambioEstado()).isEqualTo(respuesta.fechaCambioEstado());
            assertThat(u.getAdministradorCambioEstadoId()).isEqualTo(respuesta.administradorCambioEstadoId());
            assertThat(u.getMotivoCambioEstado()).isEqualTo(respuesta.motivoCambioEstado());
            assertThat(u.getTelefono()).isEqualTo("123"); assertThat(u.getNombre()).isEqualTo("RF66");
            assertThat(u.getFechaRegistro()).isEqualTo(Instant.parse("2026-10-01T12:00:00Z"));
            assertThat(u.getRoles()).containsExactly(Rol.COMPRADOR); assertThat(u.getPreferenciasEstilo()).containsExactly(Estilo.FORMAL);
            assertThat(u.getTallasHabitualesPorTipo()).isEmpty();
            assertThat(credenciales.findById(objetivoId).orElseThrow().getPasswordHash()).isEqualTo(HASH); return null;
        });
    }

    @Test void nullHistoricoYMetadatosNullableIdempotenciaYTransicion() {
        // Arrange
        tx(() -> { em.find(UsuarioEntity.class, objetivoId).setEstadoCuenta(null); return null; });
        // Act
        var legado = service.cambiar(objetivoId, adminA, request(EstadoCuenta.ACTIVA, null));
        // Assert
        assertThat(legado.estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); assertThat(legado.fechaCambioEstado()).isNull();
        tx(() -> { var u = em.find(UsuarioEntity.class, objetivoId); assertThat(u.getEstadoCuenta()).isNull();
            assertThat(u.getAdministradorCambioEstadoId()).isNull(); assertThat(u.getMotivoCambioEstado()).isNull(); return null; });
        var primera = service.cambiar(objetivoId, adminA, request(EstadoCuenta.BLOQUEADA, "Original"));
        var repetida = service.cambiar(objetivoId, adminB, request(EstadoCuenta.BLOQUEADA, "Otro motivo"));
        assertThat(repetida).isEqualTo(primera);
        assertThat(service.consultarUno(objetivoId, adminA)).isEqualTo(primera);
    }

    @Test void filtrosCountOrdenYPaginacionEnBDIncluyenNullComoActiva() {
        // Arrange
        tx(() -> {
            em.find(UsuarioEntity.class, objetivoId).setEstadoCuenta(null);
            crear(Rol.COMPRADOR, EstadoCuenta.INACTIVA, false); crear(Rol.ADMIN_ALMACEN, EstadoCuenta.BLOQUEADA, false); return null;
        });
        // Act / Assert
        var primera = service.consultar(null, 0, 2, adminA);
        var segunda = service.consultar(null, 1, 2, adminA);
        assertThat(primera.getTotalElements()).isEqualTo(5); assertThat(primera.getTotalPages()).isEqualTo(3);
        assertThat(primera).extracting(r -> r.id()).containsExactly(aId, bId);
        assertThat(segunda).extracting(r -> r.id()).containsExactly(objetivoId, propias.get(3));
        assertThat(service.consultar(EstadoCuenta.ACTIVA, 0, 100, adminA)).hasSize(3);
        assertThat(service.consultar(EstadoCuenta.INACTIVA, 0, 100, adminA)).hasSize(1);
        assertThat(service.consultar(EstadoCuenta.BLOQUEADA, 0, 100, adminA)).hasSize(1);
        assertThat(service.consultar(null, Integer.MAX_VALUE, 100, adminA)).isEmpty();
        tx(() -> { assertThat(usuarios.countAdministracion(EstadoCuenta.ACTIVA)).isEqualTo(3);
            assertThat(em.find(UsuarioEntity.class, objetivoId).getEstadoCuenta()).isNull(); return null; });
    }

    @Test void rolesEnLoteSinPaginacionDeFetchJoinNiNMasUno() {
        // Arrange
        tx(() -> { for (int i = 0; i < 15; i++) crear(Rol.ADMIN_ALMACEN, EstadoCuenta.ACTIVA, false); return null; });
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean anterior = stats.isStatisticsEnabled(); stats.setStatisticsEnabled(true); stats.clear();
        try {
            // Act
            var p = service.consultar(null, 0, 10, adminA);
            // Assert: actor + IDs + count + carga de roles, independiente de cantidad de usuarios.
            assertThat(p).hasSize(10); assertThat(p.getTotalElements()).isEqualTo(18);
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(4);
            assertThat(p.getContent().get(3).roles()).containsExactly(Rol.ADMIN_ALMACEN);
        } finally { stats.setStatisticsEnabled(anterior); }
    }

    @Test void relacionesLazyYCargaAdministrativaLimitadaARoles() {
        // Act / Assert
        tx(() -> {
            var u = em.find(UsuarioEntity.class, objetivoId);
            assertThat(Hibernate.isInitialized(u.getRoles())).isFalse();
            assertThat(Hibernate.isInitialized(u.getPreferenciasEstilo())).isFalse();
            assertThat(Hibernate.isInitialized(u.getTallasHabitualesPorTipo())).isFalse();
            assertThat(Hibernate.isInitialized(u.getBusquedasGuardadas())).isFalse(); em.clear();
            u = usuarios.findAdministracionById(objetivoId).orElseThrow();
            assertThat(Hibernate.isInitialized(u.getRoles())).isTrue();
            assertThat(Hibernate.isInitialized(u.getPreferenciasEstilo())).isFalse(); return null;
        });
    }

    @Test void administradorSinCredencialNoCuentaComoRecuperable() {
        // Arrange: actor posee un principal previo, pero ya no puede iniciar un login normal.
        tx(() -> { em.remove(em.find(CredencialEntity.class, aId)); crear(Rol.ADMIN_STYLERADAR, null, false); return null; });
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(bId, adminA, request(EstadoCuenta.BLOQUEADA, "Motivo")))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(service.consultarUno(bId, adminA).estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
    }

    @Test void administradorNullConCredencialCuentaComoOperativo() {
        // Arrange
        tx(() -> { em.find(UsuarioEntity.class, aId).setEstadoCuenta(null); return null; });
        // Act / Assert
        assertThat(service.cambiar(bId, adminA, request(EstadoCuenta.INACTIVA, null)).estadoCuenta()).isEqualTo(EstadoCuenta.INACTIVA);
        assertThat(service.consultarUno(aId, adminA).estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
    }

    List<String> paralelo(Supplier<String> uno, Supplier<String> dos) throws Exception {
        var pool = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            var a = pool.submit(() -> { inicio.await(); return uno.get(); });
            var b = pool.submit(() -> { inicio.await(); return dos.get(); });
            inicio.countDown(); return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally { inicio.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue(); }
    }
    String decidir(Long id, UsuarioPrincipal actor, EstadoCuenta estado) {
        try { service.cambiar(id, actor, request(estado, "Decisión " + estado)); return "ok"; }
        catch (BadCredentialsException e) { return "actor-no-activo"; }
        catch (ReglaDeNegocioException e) { return "regla"; }
    }

    @Test void dosCambiosConcurrentesMismaCuentaConMetadatosDeUnaSecuenciaValida() throws Exception {
        // Act
        assertThat(paralelo(() -> decidir(objetivoId, adminA, EstadoCuenta.BLOQUEADA),
                () -> decidir(objetivoId, adminB, EstadoCuenta.INACTIVA))).containsOnly("ok");
        // Assert
        var r = service.consultarUno(objetivoId, adminA);
        assertThat(r.estadoCuenta()).isIn(EstadoCuenta.BLOQUEADA, EstadoCuenta.INACTIVA);
        assertThat(r.motivoCambioEstado()).isEqualTo("Decisión " + r.estadoCuenta());
        assertThat(r.administradorCambioEstadoId()).isEqualTo(r.estadoCuenta() == EstadoCuenta.BLOQUEADA ? aId : bId);
        assertThat(r.fechaCambioEstado()).isNotNull();
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void dosAdministradoresNoPuedenDeshabilitarseMutuamenteNiDejarConteoCero(EstadoCuenta estado) throws Exception {
        // Arrange: fuerza que las dos consultas iniciales vean los mismos administradores.
        var ambosLeidos = new CyclicBarrier(2);
        tx(() -> {
            doAnswer(i -> { ambosLeidos.await(10, TimeUnit.SECONDS); return i.callRealMethod(); })
                    .when(bloqueo).bloquear(anyCollection()); return null;
        });
        // Act
        var r = paralelo(() -> decidir(bId, adminA, estado), () -> decidir(aId, adminB, estado)); reset(usuarios, bloqueo);
        // Assert
        assertThat(r).containsExactlyInAnyOrder("ok", "actor-no-activo");
        tx(() -> {
            long activos = propias.stream().map(id -> em.find(UsuarioEntity.class, id))
                    .filter(u -> u.getRoles().contains(Rol.ADMIN_STYLERADAR))
                    .filter(u -> u.getEstadoCuenta() == null || u.getEstadoCuenta() == EstadoCuenta.ACTIVA)
                    .filter(u -> credenciales.existsById(u.getId())).count();
            assertThat(activos).isEqualTo(1); return null;
        });
    }

    @Test void cambioConcurrenteActorSeRevalidaAunqueYaEsteEnContextoJPA() throws Exception {
        // Arrange: B lee su estado antes de que A lo suspenda.
        var actorLeido = new CountDownLatch(1); var suspendido = new CountDownLatch(1);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var intento = pool.submit(() -> tx(() -> {
                assertThat(em.find(UsuarioEntity.class, bId).getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
                actorLeido.countDown(); esperar(suspendido);
                return decidir(objetivoId, adminB, EstadoCuenta.BLOQUEADA);
            }));
            assertThat(actorLeido.await(10, TimeUnit.SECONDS)).isTrue();
            // Act
            service.cambiar(bId, adminA, request(EstadoCuenta.INACTIVA, null)); suspendido.countDown();
            // Assert: la excepción del servicio marca rollback también en la transacción exterior.
            assertThatThrownBy(() -> intento.get(20, TimeUnit.SECONDS)).hasCauseInstanceOf(
                    org.springframework.transaction.UnexpectedRollbackException.class);
            assertThat(service.consultarUno(objetivoId, adminA).estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        } finally { suspendido.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue(); }
    }
    void esperar(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de coordinación"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }

    @Test void falloPersistenciaRevierteEstadoYTodosLosMetadatos() {
        // Arrange
        doThrow(new DataAccessResourceFailureException("fallo simulado")).when(usuarios).flush();
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(objetivoId, adminA, request(EstadoCuenta.BLOQUEADA, "Motivo")))
                .isInstanceOf(DataAccessResourceFailureException.class); reset(usuarios);
        var r = service.consultarUno(objetivoId, adminA);
        assertThat(r.estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); assertThat(r.fechaCambioEstado()).isNull();
        assertThat(r.administradorCambioEstadoId()).isNull(); assertThat(r.motivoCambioEstado()).isNull();
    }
    @Test void rollbackPosteriorAlFlushNoDejaDecisionParcial() {
        // Act / Assert
        assertThatThrownBy(() -> tx(() -> {
            service.cambiar(objetivoId, adminA, request(EstadoCuenta.BLOQUEADA, "Motivo"));
            assertThat(usuarios.findDatosActuales(objetivoId).orElseThrow().fechaCambioEstado()).isNotNull();
            throw new IllegalStateException("fallo posterior");
        })).isInstanceOf(IllegalStateException.class);
        var r = service.consultarUno(objetivoId, adminA);
        assertThat(r.estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); assertThat(r.fechaCambioEstado()).isNull();
        assertThat(r.administradorCambioEstadoId()).isNull(); assertThat(r.motivoCambioEstado()).isNull();
    }
    @Test void actualizacionConcurrentePreferenciasNoSobrescribeEstadoNiTrazabilidad() throws Exception {
        // Arrange
        var leido = new CountDownLatch(1); var cambiado = new CountDownLatch(1); var pool = Executors.newSingleThreadExecutor();
        try {
            var f = pool.submit(() -> tx(() -> {
                var u = usuarios.findById(objetivoId).orElseThrow(); leido.countDown(); esperar(cambiado);
                u.setTelefono("456"); return null;
            }));
            assertThat(leido.await(10, TimeUnit.SECONDS)).isTrue();
            // Act
            service.cambiar(objetivoId, adminA, request(EstadoCuenta.BLOQUEADA, "Motivo")); cambiado.countDown(); f.get(20, TimeUnit.SECONDS);
            // Assert: DynamicUpdate evita que una escritura ajena reemplace el estado leído antes.
            var r = service.consultarUno(objetivoId, adminA);
            assertThat(r.estadoCuenta()).isEqualTo(EstadoCuenta.BLOQUEADA); assertThat(r.motivoCambioEstado()).isEqualTo("Motivo");
            tx(() -> { assertThat(em.find(UsuarioEntity.class, objetivoId).getTelefono()).isEqualTo("456"); return null; });
        } finally { cambiado.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue(); }
    }

    @Test void motivoExcesivoViolaRestriccionDeEsquemaYHaceRollback() {
        // Act / Assert: comprobación real de varchar(1000) sin pasar por Bean Validation.
        assertThatThrownBy(() -> tx(() -> {
            usuarios.actualizarEstado(objetivoId, EstadoCuenta.BLOQUEADA, Instant.now(), aId, "x".repeat(1001));
            return null;
        })).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        var r = service.consultarUno(objetivoId, adminA);
        assertThat(r.estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); assertThat(r.fechaCambioEstado()).isNull();
        assertThat(r.administradorCambioEstadoId()).isNull(); assertThat(r.motivoCambioEstado()).isNull();
    }

    @Test void decisionUsaEstadoActualAunqueEntidadObjetivoEsteObsoleta() {
        // Arrange / Act: otra transacción decide después de la lectura inicial.
        tx(() -> {
            var previo = em.find(UsuarioEntity.class, objetivoId);
            var independiente = new TransactionTemplate(transactions);
            independiente.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            independiente.execute(s -> service.cambiar(objetivoId, adminA, request(EstadoCuenta.BLOQUEADA, "Primera")));
            assertThat(previo.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
            // Assert: incluso solicitar ACTIVA, igual al snapshot obsoleto, persiste la transición real.
            var r = service.cambiar(objetivoId, adminB, request(EstadoCuenta.ACTIVA, "Segunda"));
            assertThat(r.estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); assertThat(r.motivoCambioEstado()).isEqualTo("Segunda");
            return null;
        });
        assertThat(service.consultarUno(objetivoId, adminA).motivoCambioEstado()).isEqualTo("Segunda");
    }
}
