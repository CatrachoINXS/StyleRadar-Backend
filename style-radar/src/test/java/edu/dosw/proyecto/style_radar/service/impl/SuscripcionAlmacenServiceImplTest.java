package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.SuscripcionAlmacenMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.AutorizacionSuscripcionService;

@ExtendWith(MockitoExtension.class)
class SuscripcionAlmacenServiceImplTest {
    static final Instant NOW = Instant.parse("2024-01-31T10:20:30.123456789Z");
    @Mock AlmacenRepository almacenes;
    @Mock PlanSuscripcionRepository planes;
    @Mock SuscripcionAlmacenRepository suscripciones;
    @Mock AutorizacionSuscripcionService autorizacion;
    SuscripcionAlmacenServiceImpl service;
    UsuarioPrincipal actor;
    AlmacenEntity almacen;
    PlanSuscripcionEntity plan;

    @BeforeEach void preparar() {
        actor = new UsuarioPrincipal(UsuarioEntity.builder().id(7L).roles(Set.of(Rol.ADMIN_ALMACEN)).build(), "");
        almacen = new AlmacenEntity(); almacen.setNit("nit"); almacen.setEstadoVerificacion(EstadoVerificacionAlmacen.APROBADO);
        plan = new PlanSuscripcionEntity(); plan.setId(2L); plan.setNombre("Fixture sin precio"); plan.setActivo(true);
        plan.setPeriodo(1); plan.setUnidadDuracion(UnidadPeriodoSuscripcion.MESES_CALENDARIO);
        service = new SuscripcionAlmacenServiceImpl(almacenes, planes, suscripciones, autorizacion,
                new SuscripcionAlmacenMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }
    void valido() {
        when(almacenes.findForUpdate("nit")).thenReturn(Optional.of(almacen));
        when(planes.findById(2L)).thenReturn(Optional.of(plan));
    }
    SuscripcionAlmacenEntity activa() {
        var s = new SuscripcionAlmacenEntity(); s.setId(8L); s.setAlmacen(almacen); s.setPlan(plan);
        s.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA); s.setAlmacenActivoNit("nit");
        s.setFechaCreacion(NOW.minusSeconds(1)); s.setInicioPeriodo(NOW.minusSeconds(1));
        s.setFinPeriodo(NOW.plusSeconds(100)); s.setRenovacionHabilitada(true); return s;
    }
    void cancelable(SuscripcionAlmacenEntity s) {
        when(almacenes.findForUpdate("nit")).thenReturn(Optional.of(almacen));
        when(suscripciones.findByIdAndAlmacen_Nit(8L, "nit")).thenReturn(Optional.of(s));
    }
    @Test void creaConClockMicrosYMesCalendarioSinDatosMonetarios() {
        valido(); when(suscripciones.saveAndFlush(any())).thenAnswer(i -> { var s = (SuscripcionAlmacenEntity)i.getArgument(0); s.setId(8L); return s; });
        var resultado = service.crear("nit", 2L, actor);
        assertThat(resultado.id()).isEqualTo(8L); assertThat(resultado.estadoSuscripcion()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(resultado.inicioPeriodo()).isEqualTo(Instant.parse("2024-01-31T10:20:30.123456Z"));
        assertThat(resultado.fechaCreacion()).isEqualTo(resultado.inicioPeriodo());
        assertThat(resultado.finPeriodo()).isEqualTo(Instant.parse("2024-02-29T10:20:30.123456Z"));
        assertThat(resultado.renovacionHabilitada()).isTrue(); assertThat(resultado.fechaCancelacionRenovacion()).isNull();
        assertThat(resultado.almacenNit()).isEqualTo("nit"); assertThat(resultado.planId()).isEqualTo(2L);
        assertThat(Arrays.stream(SuscripcionAlmacen.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("precio", "fechaPago", "transaccion", "factura");
        verify(autorizacion).autorizar("nit", actor, true);
    }
    @ParameterizedTest @NullSource @EnumSource(value=EstadoVerificacionAlmacen.class, names={"PENDIENTE", "RECHAZADO"})
    void soloAprobadoPermiteActivar(EstadoVerificacionAlmacen estado) {
        almacen.setEstadoVerificacion(estado); when(almacenes.findForUpdate("nit")).thenReturn(Optional.of(almacen));
        assertThatThrownBy(() -> service.crear("nit", 2L, actor)).isInstanceOf(ReglaDeNegocioException.class);
        verifyNoInteractions(planes, suscripciones);
        if (estado == null) assertThat(edu.dosw.proyecto.style_radar.service.VisibilidadAlmacen.visible(null)).isTrue();
    }
    @Test void almacenInexistente() {
        assertThatThrownBy(() -> service.crear("nit", 2L, actor)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void planInexistente() {
        when(almacenes.findForUpdate("nit")).thenReturn(Optional.of(almacen));
        assertThatThrownBy(() -> service.crear("nit", 2L, actor)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void planInactivo() {
        valido(); plan.setActivo(false);
        assertThatThrownBy(() -> service.crear("nit", 2L, actor)).isInstanceOf(ReglaDeNegocioException.class);
    }
    @ParameterizedTest @NullSource @ValueSource(ints={0,-1})
    void duracionInvalida(Integer meses) {
        valido(); plan.setPeriodo(meses);
        assertThatThrownBy(() -> service.crear("nit", 2L, actor)).isInstanceOf(ReglaDeNegocioException.class);
        verify(suscripciones, never()).saveAndFlush(any());
    }
    @Test void unidadInvalida() {
        valido(); plan.setUnidadDuracion(null);
        assertThatThrownBy(() -> service.crear("nit", 2L, actor)).isInstanceOf(ReglaDeNegocioException.class);
    }
    @ParameterizedTest @ValueSource(longs={2,3})
    void duplicadoIndependienteDelPlan(Long nuevoPlan) {
        when(almacenes.findForUpdate("nit")).thenReturn(Optional.of(almacen));
        when(planes.findById(nuevoPlan)).thenReturn(Optional.of(plan));
        when(suscripciones.findByAlmacenActivoNit("nit")).thenReturn(Optional.of(activa()));
        assertThatThrownBy(() -> service.crear("nit", nuevoPlan, actor)).isInstanceOf(SuscripcionActivaDuplicadaException.class);
        verify(suscripciones, never()).saveAndFlush(any());
    }
    @Test void vencidaLiberaClaveAntesDeCrearSinSobrescribirHistoria() {
        valido(); var anterior = activa(); anterior.setFinPeriodo(NOW.minusSeconds(1));
        when(suscripciones.findByAlmacenActivoNit("nit")).thenReturn(Optional.of(anterior));
        service.crear("nit", 2L, actor);
        assertThat(anterior.getEstadoSuscripcion()).isEqualTo(EstadoSuscripcion.FINALIZADA);
        assertThat(anterior.getAlmacenActivoNit()).isNull();
        var orden = inOrder(suscripciones); orden.verify(suscripciones).findByAlmacenActivoNit("nit");
        orden.verify(suscripciones).flush(); orden.verify(suscripciones).saveAndFlush(argThat(s -> s != anterior));
    }
    @Test void cancelacionConservaContratoYEsIdempotente() {
        var s = activa(); cancelable(s); var antes = new SuscripcionAlmacenMapper().toDomain(s);
        var uno = service.cancelarRenovacion("nit", 8L, false, actor);
        var dos = service.cancelarRenovacion("nit", 8L, false, actor);
        assertThat(uno).isEqualTo(dos); assertThat(uno.renovacionHabilitada()).isFalse();
        assertThat(uno.fechaCancelacionRenovacion()).isEqualTo(NOW.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(uno.estadoSuscripcion()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(uno.inicioPeriodo()).isEqualTo(antes.inicioPeriodo()); assertThat(uno.finPeriodo()).isEqualTo(antes.finPeriodo());
        assertThat(uno.planId()).isEqualTo(antes.planId()); assertThat(uno.almacenNit()).isEqualTo(antes.almacenNit());
        assertThat(uno.fechaCreacion()).isEqualTo(antes.fechaCreacion()); verify(suscripciones, never()).delete(any());
    }
    @Test void sinSuscripcionNoCancela() {
        when(almacenes.findForUpdate("nit")).thenReturn(Optional.of(almacen));
        assertThatThrownBy(() -> service.cancelarRenovacion("nit", 8L, false, actor)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @ParameterizedTest @ValueSource(strings={"vencida", "futura", "finalizada"})
    void noVigenteNoCancela(String caso) {
        var s = activa();
        if (caso.equals("vencida")) s.setFinPeriodo(NOW.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        if (caso.equals("futura")) s.setInicioPeriodo(NOW.plusSeconds(1));
        if (caso.equals("finalizada")) s.finalizar(); cancelable(s);
        assertThatThrownBy(() -> service.cancelarRenovacion("nit", 8L, false, actor)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(s.isRenovacionHabilitada()).isTrue();
    }
    @ParameterizedTest @NullSource @ValueSource(booleans={true})
    void noReactivar(Boolean valor) {
        assertThatThrownBy(() -> service.cancelarRenovacion("nit", 8L, valor, actor)).isInstanceOf(SolicitudSuscripcionInvalidaException.class);
        verifyNoInteractions(almacenes, suscripciones);
    }
    @ParameterizedTest @NullSource @ValueSource(longs={0,-1})
    void idPlanInvalido(Long id) {
        assertThatThrownBy(() -> service.crear("nit", id, actor)).isInstanceOf(SolicitudSuscripcionInvalidaException.class);
    }
    @ParameterizedTest @NullSource @ValueSource(longs={0,-1})
    void idCancelacionInvalido(Long id) {
        assertThatThrownBy(() -> service.cancelarRenovacion("nit", id, false, actor)).isInstanceOf(SolicitudSuscripcionInvalidaException.class);
    }
    @Test void consultaVigenteUsaClock() {
        when(suscripciones.findVigente("nit", NOW.truncatedTo(java.time.temporal.ChronoUnit.MICROS))).thenReturn(Optional.of(activa()));
        assertThat(service.vigente("nit", actor).id()).isEqualTo(8L); verify(autorizacion).autorizar("nit", actor, false);
    }
    @Test void consultaAusente404() { assertThatThrownBy(() -> service.vigente("nit", actor)).isInstanceOf(RecursoNoEncontradoException.class); }
    @Test void listaPlanesSinSembrar() {
        when(planes.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(plan));
        assertThat(service.planes()).extracting(PlanSuscripcion::id).containsExactly(2L); verify(planes, never()).save(any());
    }
    @Test void paginaOrdenadaEnBD() {
        when(suscripciones.findByAlmacen_Nit(eq("nit"), any())).thenAnswer(i -> new PageImpl<>(List.of(activa()), i.getArgument(1), 1));
        assertThat(service.historial("nit", 0, 20, actor)).hasSize(1);
        verify(suscripciones).findByAlmacen_Nit(eq("nit"), argThat(p -> p.getSort().equals(
                Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id")))));
    }
    @Test void offsetExtremoNoDesbordaJPA() {
        when(suscripciones.countByAlmacen_Nit("nit")).thenReturn(5L);
        var p = service.historial("nit", Integer.MAX_VALUE, 100, actor);
        assertThat(p).isEmpty(); assertThat(p.getTotalElements()).isEqualTo(5); verify(suscripciones, never()).findByAlmacen_Nit(any(), any());
    }
    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,101"})
    void paginacionInvalida(int page, int size) {
        assertThatThrownBy(() -> service.historial("nit", page, size, actor)).isInstanceOf(SolicitudSuscripcionInvalidaException.class);
    }
}
