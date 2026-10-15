package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.dosw.proyecto.style_radar.exception.SuscripcionActivaDuplicadaException;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.AutorizacionSuscripcionService;
import edu.dosw.proyecto.style_radar.support.SuscripcionesFixtures;

/** Fixtures comprometidos; cada llamada al servicio abre una transacción real independiente. */
@SpringBootTest @ActiveProfiles("test")
class SuscripcionesConcurrenciaIntegrationTest extends SuscripcionesFixtures {
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean Clock clock;
    @Autowired AlmacenRepository almacenes;
    @MockitoSpyBean AutorizacionSuscripcionService autorizacion;
    UsuarioPrincipal segundo;
    <T> T tx(Supplier<T> trabajo) { return new TransactionTemplate(transactions).execute(s -> trabajo.get()); }

    @BeforeEach void preparar() {
        doReturn(AHORA).when(clock).instant();
        tx(() -> { prepararFixtures(); asociaciones.asociar(nit,otraTienda.getId(),administrador);
            segundo = new UsuarioPrincipal(em.find(UsuarioEntity.class,otraTienda.getId()), ""); return null; });
    }
    @AfterEach void limpiar() {
        reset(clock,autorizacion);
        tx(() -> {
            em.createQuery("delete SuscripcionAlmacenEntity s where s.almacen.nit in :nits").setParameter("nits",List.of(nit,ajeno)).executeUpdate();
            em.createQuery("delete AdministradorAlmacenEntity a where a.almacen.nit in :nits").setParameter("nits",List.of(nit,ajeno)).executeUpdate();
            em.remove(em.find(AlmacenEntity.class,nit)); em.remove(em.find(AlmacenEntity.class,ajeno));
            em.remove(em.find(PlanSuscripcionEntity.class,plan.getId())); em.remove(em.find(PlanSuscripcionEntity.class,otroPlan.getId()));
            em.remove(em.find(UsuarioEntity.class,tienda.getId())); em.remove(em.find(UsuarioEntity.class,otraTienda.getId()));
            em.remove(em.find(UsuarioEntity.class,admin.getId())); return null;
        });
    }
    void esperar(CountDownLatch latch) {
        try { if (!latch.await(15,TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de coordinación"); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
    /** Retiene el lock del NIT de la primera operación hasta que la segunda intenta tomarlo. */
    List<Object> simultaneas(Supplier<?> primera, Supplier<?> segunda) throws Exception {
        var adquirida = new CountDownLatch(1); var intenta = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        // El proxy MANDATORY también exige una transacción al configurar la interceptación del spy.
        tx(() -> {
            doAnswer(i -> {
                i.callRealMethod();
                if (Thread.currentThread().getName().equals("rf67-segunda")) intenta.countDown();
                return null;
            }).when(autorizacion).autorizar(eq(nit), any(), eq(true));
            return null;
        });
        var pool = Executors.newFixedThreadPool(2);
        try {
            var uno = pool.submit(() -> { Thread.currentThread().setName("rf67-primera");
                return ejecutar(() -> tx(() -> {
                    // Lock real del almacén retenido por la transacción exterior antes de ejecutar la primera operación.
                    almacenes.findForUpdate(nit).orElseThrow(); adquirida.countDown(); esperar(liberar);
                    return primera.get();
                })); });
            assertThat(adquirida.await(15,TimeUnit.SECONDS)).isTrue();
            var dos = pool.submit(() -> { Thread.currentThread().setName("rf67-segunda"); return ejecutar(segunda); });
            assertThat(intenta.await(15,TimeUnit.SECONDS)).isTrue(); liberar.countDown();
            return List.of(uno.get(25,TimeUnit.SECONDS),dos.get(25,TimeUnit.SECONDS));
        } finally { liberar.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(25,TimeUnit.SECONDS)).isTrue(); reset(autorizacion); }
    }
    Object ejecutar(Supplier<?> tarea) {
        try { return tarea.get(); } catch(SuscripcionActivaDuplicadaException e) { return e; }
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void dosActivacionesMismoNitUnaAceptadaInclusoPlanesDiferentes(boolean diferente) throws Exception {
        var resultados = simultaneas(() -> servicio.crear(nit,plan.getId(),principal),
                () -> servicio.crear(nit,diferente?otroPlan.getId():plan.getId(),segundo));
        assertThat(resultados.stream().filter(SuscripcionAlmacen.class::isInstance)).hasSize(1);
        assertThat(resultados.stream().filter(SuscripcionActivaDuplicadaException.class::isInstance)).hasSize(1);
        assertThat(tx(() -> suscripciones.countByAlmacen_Nit(nit))).isEqualTo(1);
        assertThat(servicio.vigente(nit,principal).planId()).isEqualTo(plan.getId());
    }
    @Test void dosCancelacionesMismaFechaPeriodoSinInconsistencia() throws Exception {
        var s = servicio.crear(nit,plan.getId(),principal); doReturn(AHORA.plusSeconds(10)).when(clock).instant();
        var resultados = simultaneas(() -> servicio.cancelarRenovacion(nit,s.id(),false,principal),
                () -> servicio.cancelarRenovacion(nit,s.id(),false,segundo));
        assertThat(resultados.get(0)).isEqualTo(resultados.get(1));
        var vigente = servicio.vigente(nit,principal);
        assertThat(vigente.fechaCancelacionRenovacion()).isEqualTo(AHORA.plusSeconds(10));
        assertThat(vigente.finPeriodo()).isEqualTo(s.finPeriodo()); assertThat(vigente.renovacionHabilitada()).isFalse();
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void activarMientrasCancelaNoCreaSegundaSuscripcion(boolean cancelarPrimero) throws Exception {
        var s = servicio.crear(nit,plan.getId(),principal);
        Supplier<?> cancelar = () -> servicio.cancelarRenovacion(nit,s.id(),false,principal);
        Supplier<?> crear = () -> servicio.crear(nit,otroPlan.getId(),segundo);
        var r = cancelarPrimero ? simultaneas(cancelar,crear) : simultaneas(crear,cancelar);
        assertThat(r.stream().filter(SuscripcionActivaDuplicadaException.class::isInstance)).hasSize(1);
        assertThat(tx(() -> suscripciones.countByAlmacen_Nit(nit))).isEqualTo(1);
        assertThat(servicio.vigente(nit,principal).renovacionHabilitada()).isFalse();
    }
    @Test void vencimientoRealDosActivacionesConservanHistoriaYUnaNuevaActiva() throws Exception {
        var vieja = servicio.crear(nit,plan.getId(),principal); doReturn(vieja.finPeriodo()).when(clock).instant();
        var resultados = simultaneas(() -> servicio.crear(nit,otroPlan.getId(),principal),
                () -> servicio.crear(nit,plan.getId(),segundo));
        assertThat(resultados.stream().filter(SuscripcionAlmacen.class::isInstance)).hasSize(1);
        assertThat(tx(() -> suscripciones.countByAlmacen_Nit(nit))).isEqualTo(2);
        var anterior = tx(() -> suscripciones.findById(vieja.id()).orElseThrow().getEstadoSuscripcion());
        assertThat(anterior).isEqualTo(EstadoSuscripcion.FINALIZADA);
        assertThat(servicio.vigente(nit,principal).inicioPeriodo()).isEqualTo(vieja.finPeriodo());
    }
    @Test void almacenesIndependientesNoSeBloqueanGlobalmente() throws Exception {
        var retenida=new CountDownLatch(1); var liberar=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var primero=pool.submit(() -> tx(() -> {
                var s=servicio.crear(nit,plan.getId(),principal); retenida.countDown(); esperar(liberar); return s;
            }));
            assertThat(retenida.await(15,TimeUnit.SECONDS)).isTrue();
            var independiente=pool.submit(() -> servicio.crear(ajeno,otroPlan.getId(),segundo));
            assertThat(independiente.get(10,TimeUnit.SECONDS).almacenNit()).isEqualTo(ajeno);
            liberar.countDown(); assertThat(primero.get(25,TimeUnit.SECONDS).almacenNit()).isEqualTo(nit);
        } finally { liberar.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(25,TimeUnit.SECONDS)).isTrue(); }
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void falloPosteriorAlInsertRevierteNuevaYFinalizacionAnterior(boolean conVencida) {
        SuscripcionAlmacen previa = conVencida ? servicio.crear(nit,plan.getId(),principal) : null;
        if (conVencida) doReturn(previa.finPeriodo()).when(clock).instant();
        assertThatThrownBy(() -> tx(() -> {
            var creada=servicio.crear(nit,otroPlan.getId(),principal);
            assertThat(em.find(SuscripcionAlmacenEntity.class,creada.id())).isNotNull(); em.flush();
            throw new DataAccessResourceFailureException("Fallo posterior a INSERT fixture");
        })).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(tx(() -> suscripciones.countByAlmacen_Nit(nit))).isEqualTo(conVencida?1:0);
        if (conVencida) {
            assertThat(tx(() -> suscripciones.findById(previa.id()).orElseThrow().getEstadoSuscripcion())).isEqualTo(EstadoSuscripcion.ACTIVA);
            assertThat(tx(() -> suscripciones.findByAlmacenActivoNit(nit).orElseThrow().getId())).isEqualTo(previa.id());
        }
    }
    @Test void falloPosteriorACancelarRevierteIntencionYFecha() {
        var s=servicio.crear(nit,plan.getId(),principal);
        assertThatThrownBy(() -> tx(() -> {
            servicio.cancelarRenovacion(nit,s.id(),false,principal); em.flush();
            throw new DataAccessResourceFailureException("Fallo posterior a UPDATE fixture");
        })).isInstanceOf(DataAccessResourceFailureException.class);
        var actual=servicio.vigente(nit,principal);
        assertThat(actual.renovacionHabilitada()).isTrue(); assertThat(actual.fechaCancelacionRenovacion()).isNull();
        assertThat(actual.finPeriodo()).isEqualTo(s.finPeriodo());
    }
    @Test void falloPosteriorAAsociarRevierteVinculo() {
        tx(() -> { em.createQuery("delete AdministradorAlmacenEntity a where a.usuario.id=:id and a.almacen.nit=:nit")
                .setParameter("id",otraTienda.getId()).setParameter("nit",nit).executeUpdate(); return null; });
        assertThatThrownBy(() -> tx(() -> { asociaciones.asociar(nit,otraTienda.getId(),administrador);
            throw new DataAccessResourceFailureException("Rollback exterior fixture"); })).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(tx(() -> vinculos.existsByUsuario_IdAndAlmacen_Nit(otraTienda.getId(),nit))).isFalse();
    }
}
