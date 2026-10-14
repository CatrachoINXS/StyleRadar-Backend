package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.support.SuscripcionesFixtures;

@SpringBootTest @ActiveProfiles("test") @Transactional
class SuscripcionesPersistenceIntegrationTest extends SuscripcionesFixtures {
    @MockitoSpyBean Clock clock;
    @BeforeEach void preparar() { doReturn(AHORA).when(clock).instant(); prepararFixtures(); }
    @AfterEach void limpiarSpy() { reset(clock); }

    @Test void registroJPATiemposUTCFKsYEnumStringSinPago() {
        var s = servicio.crear(nit, plan.getId(), principal); releer();
        var fila = suscripciones.findById(s.id()).orElseThrow();
        assertThat(fila.getInicioPeriodo()).isEqualTo(AHORA);
        assertThat(fila.getFinPeriodo()).isEqualTo(Instant.parse("2024-02-29T14:15:16.123456Z"));
        assertThat(fila.getFechaCreacion()).isEqualTo(AHORA); assertThat(fila.isRenovacionHabilitada()).isTrue();
        assertThat(fila.getAlmacen().getNit()).isEqualTo(nit); assertThat(fila.getPlan().getId()).isEqualTo(plan.getId());
        assertThat(em.createNativeQuery("select estado_suscripcion from suscripciones_almacen where id = :id", String.class)
                .setParameter("id", s.id()).getSingleResult()).isEqualTo("ACTIVA");
        assertThat(em.createNativeQuery("select unidad_duracion from planes_suscripcion where id = :id", String.class)
                .setParameter("id", plan.getId()).getSingleResult()).isEqualTo("MESES_CALENDARIO");
        assertThat(em.find(AlmacenEntity.class,nit).getEstadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.APROBADO);
    }
    @Test void finalizaAlVencerPermiteNuevoPlanYConservaHistorial() {
        var uno = servicio.crear(nit, plan.getId(), principal); releer();
        doReturn(uno.finPeriodo()).when(clock).instant();
        assertThatThrownBy(() -> servicio.vigente(nit, principal)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(servicio.historial(nit,0,20,principal).getContent().getFirst().estadoEfectivo(uno.finPeriodo()))
                .isEqualTo(EstadoSuscripcion.FINALIZADA);
        var dos = servicio.crear(nit, otroPlan.getId(), principal); releer();
        assertThat(dos.id()).isNotEqualTo(uno.id()); assertThat(dos.inicioPeriodo()).isEqualTo(uno.finPeriodo());
        var anterior = suscripciones.findById(uno.id()).orElseThrow();
        assertThat(anterior.getEstadoSuscripcion()).isEqualTo(EstadoSuscripcion.FINALIZADA);
        assertThat(anterior.getFinPeriodo()).isEqualTo(uno.finPeriodo()); assertThat(anterior.getAlmacenActivoNit()).isNull();
        assertThat(servicio.historial(nit,0,1,principal).getTotalElements()).isEqualTo(2);
        assertThat(servicio.historial(nit,0,1,principal).getContent().getFirst().id()).isEqualTo(dos.id());
        assertThat(servicio.historial(nit,1,1,principal).getContent().getFirst().id()).isEqualTo(uno.id());
    }
    @Test void empateFechaOrdenaIdDescYPaginaSinCargarTodo() {
        var uno = fila(nit, em.find(PlanSuscripcionEntity.class,plan.getId()), AHORA.minusSeconds(100), AHORA.minusSeconds(10), EstadoSuscripcion.FINALIZADA);
        var dos = fila(nit, em.find(PlanSuscripcionEntity.class,plan.getId()), AHORA.minusSeconds(100), AHORA.minusSeconds(10), EstadoSuscripcion.FINALIZADA);
        releer(); var pagina = servicio.historial(nit,0,1,principal);
        assertThat(pagina.getTotalElements()).isEqualTo(2); assertThat(pagina.getContent()).extracting(SuscripcionAlmacen::id).containsExactly(dos.getId());
        assertThat(servicio.historial(nit,1,1,principal).getContent()).extracting(SuscripcionAlmacen::id).containsExactly(uno.getId());
    }
    @Test void cancelacionJPANoModificaPeriodoYNoSobrescribeFecha() {
        var s = servicio.crear(nit,plan.getId(),principal); releer();
        doReturn(AHORA.plusSeconds(10)).when(clock).instant(); var primera = servicio.cancelarRenovacion(nit,s.id(),false,principal); releer();
        doReturn(AHORA.plusSeconds(20)).when(clock).instant(); var segunda = servicio.cancelarRenovacion(nit,s.id(),false,principal); releer();
        assertThat(primera).isEqualTo(segunda); assertThat(segunda.renovacionHabilitada()).isFalse();
        assertThat(segunda.fechaCancelacionRenovacion()).isEqualTo(AHORA.plusSeconds(10));
        assertThat(segunda.estadoSuscripcion()).isEqualTo(EstadoSuscripcion.ACTIVA); assertThat(segunda.finPeriodo()).isEqualTo(s.finPeriodo());
        assertThat(segunda.inicioPeriodo()).isEqualTo(s.inicioPeriodo()); assertThat(segunda.fechaCreacion()).isEqualTo(s.fechaCreacion());
        assertThat(suscripciones.countByAlmacen_Nit(nit)).isEqualTo(1); assertThat(servicio.vigente(nit,principal).id()).isEqualTo(s.id());
    }
    @Test void rechazoPosteriorNoBorraContratoNiPermiteEludirVisibilidad() {
        var s = servicio.crear(nit,plan.getId(),principal); releer();
        var a = em.find(AlmacenEntity.class,nit); a.setEstadoVerificacion(EstadoVerificacionAlmacen.RECHAZADO); releer();
        assertThat(servicio.vigente(nit,principal).finPeriodo()).isEqualTo(s.finPeriodo());
        assertThat(edu.dosw.proyecto.style_radar.service.VisibilidadAlmacen.visible(EstadoVerificacionAlmacen.RECHAZADO)).isFalse();
        assertThat(servicio.cancelarRenovacion(nit,s.id(),false,principal).estadoSuscripcion()).isEqualTo(EstadoSuscripcion.ACTIVA);
    }
    @Test void mismaCuentaPropioPermitidoAjeno404EnCadaOperacion() {
        var s = servicio.crear(nit,plan.getId(),principal); releer();
        assertThatThrownBy(() -> servicio.crear(ajeno,plan.getId(),principal)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.vigente(ajeno,principal)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.historial(ajeno,0,20,principal)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.cancelarRenovacion(ajeno,s.id(),false,principal)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void idDeOtroNitInclusoSiAdministraAmbos404() {
        asociaciones.asociar(ajeno,tienda.getId(),administrador);
        var s = servicio.crear(ajeno,plan.getId(),principal); releer();
        assertThatThrownBy(() -> servicio.cancelarRenovacion(nit,s.id(),false,principal)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void asociacionIdempotenteSinRolesNiCorreoInventados() {
        var uno = asociaciones.asociar(nit,tienda.getId(),administrador); releer();
        doReturn(AHORA.plusSeconds(20)).when(clock).instant(); var dos = asociaciones.asociar(nit,tienda.getId(),administrador);
        assertThat(uno).isEqualTo(dos); assertThat(uno.asignadoPorId()).isEqualTo(admin.getId());
        assertThat(usuarios.findById(tienda.getId()).orElseThrow().getRoles()).containsExactly(Rol.ADMIN_ALMACEN);
        assertThat(em.find(AlmacenEntity.class,nit).getCorreoContacto()).isEqualTo("contacto@example.co");
    }
    @ParameterizedTest @EnumSource(value=Rol.class, names={"COMPRADOR", "REPRESENTANTE_FUNDACION", "ADMIN_STYLERADAR"})
    void noAsociarObjetivosSinAdminAlmacen(Rol rol) {
        var objetivo = usuario(rol); releer();
        assertThatThrownBy(() -> asociaciones.asociar(nit,objetivo.getId(),administrador)).isInstanceOf(ReglaDeNegocioException.class);
    }
    @Test void objetivoInexistente404() {
        assertThatThrownBy(() -> asociaciones.asociar(nit,Long.MAX_VALUE,administrador)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void almacenInexistenteEnAsociacion404() {
        assertThatThrownBy(() -> asociaciones.asociar("inexistente",tienda.getId(),administrador)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void usuarioNoPuedeAutoasignarse() {
        assertThatThrownBy(() -> asociaciones.asociar(ajeno,tienda.getId(),principal)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void compradorNoPuedeAutoasignarse() {
        var u = usuario(Rol.COMPRADOR); var p = new UsuarioPrincipal(u, ""); releer();
        assertThatThrownBy(() -> asociaciones.asociar(nit,u.getId(),p)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void duplicadoPersistenteAunqueOmitaElServicio() {
        servicio.crear(nit,plan.getId(),principal); releer();
        assertThatThrownBy(() -> { fila(nit,em.find(PlanSuscripcionEntity.class,otroPlan.getId()),AHORA,AHORA.plusSeconds(50),EstadoSuscripcion.ACTIVA); em.flush(); })
                .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
    @Test void duplicadoVinculoPersistente() {
        var a = new AdministradorAlmacenEntity(); a.setUsuario(em.find(UsuarioEntity.class,tienda.getId()));
        a.setAlmacen(em.find(AlmacenEntity.class,nit)); a.setAsignadoPor(em.find(UsuarioEntity.class,admin.getId())); a.setFechaAsignacion(AHORA);
        assertThatThrownBy(() -> { em.persist(a); em.flush(); }).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
    @ParameterizedTest @ValueSource(ints={0,-1})
    void planDuracionInvalidaRechazadaPorJPA(int meses) {
        assertThatThrownBy(() -> { plan(meses,true); em.flush(); }).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
    @ParameterizedTest @ValueSource(strings={"plan", "almacen", "periodo", "clave", "cancelacion"})
    void restriccionesSQLNoDependenDeBeanValidation(String caso) {
        var s = servicio.crear(nit,plan.getId(),principal); releer();
        String sql = switch(caso) {
            case "plan" -> "update suscripciones_almacen set plan_id = 9223372036854775807 where id = :id";
            case "almacen" -> "update suscripciones_almacen set almacen_nit = 'no-existe', almacen_activo_nit = 'no-existe' where id = :id";
            case "periodo" -> "update suscripciones_almacen set fin_periodo = inicio_periodo where id = :id";
            case "clave" -> "update suscripciones_almacen set almacen_activo_nit = null where id = :id";
            default -> "update suscripciones_almacen set renovacion_habilitada = false where id = :id";
        };
        assertThatThrownBy(() -> em.createNativeQuery(sql).setParameter("id",s.id()).executeUpdate())
                .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
    @Test void periodoPlanSQLCheck() {
        assertThatThrownBy(() -> em.createNativeQuery("update planes_suscripcion set periodo = 0 where id = :id")
                .setParameter("id",plan.getId()).executeUpdate()).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
    @Test void foreignKeyAsociacionUsuarioSQL() {
        assertThatThrownBy(() -> em.createNativeQuery("update administradores_almacen set usuario_id = 9223372036854775807 where almacen_nit = :nit")
                .setParameter("nit",nit).executeUpdate()).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
}
