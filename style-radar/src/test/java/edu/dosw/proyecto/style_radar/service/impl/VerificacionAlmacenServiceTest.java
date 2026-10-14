package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.access.AccessDeniedException;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;

class VerificacionAlmacenServiceTest {
    AlmacenRepository almacenes;
    VerificacionAlmacenService service;
    AlmacenEntity almacen;
    UsuarioPrincipal admin;
    static final Instant FECHA = Instant.parse("2026-10-09T17:00:00.123456789Z");

    @BeforeEach void preparar() {
        // Arrange
        almacenes = mock(AlmacenRepository.class);
        service = new VerificacionAlmacenService(almacenes, Clock.fixed(FECHA, ZoneOffset.UTC));
        almacen = new AlmacenEntity("NIT", "Radar", "Datos", "123", "contacto@example.co", new ArrayList<>());
        almacen.setEstadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE);
        when(almacenes.findForUpdate("NIT")).thenReturn(Optional.of(almacen));
        var u = new UsuarioEntity(); u.setId(7L); u.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        admin = new UsuarioPrincipal(u, "fixture");
    }

    DecisionVerificacionRequestDTO request(DecisionVerificacionAlmacen d, String motivo) {
        var r = new DecisionVerificacionRequestDTO(); r.setDecision(d); r.setMotivo(motivo); return r;
    }

    @ParameterizedTest @EnumSource(DecisionVerificacionAlmacen.class)
    void fechaServidorEIdentidadReal(DecisionVerificacionAlmacen d) {
        // Act
        var r = service.decidir("NIT", admin, request(d, " Datos revisados "));
        // Assert
        assertThat(r.fechaDecisionVerificacion()).isEqualTo(FECHA.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(r.administradorDecisionId()).isEqualTo(7L);
        assertThat(r.motivoVerificacion()).isEqualTo("Datos revisados");
        assertThat(r.estadoVerificacion().name()).isEqualTo(d.name());
        verify(almacenes).findForUpdate("NIT"); verify(almacenes).flush();
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", " "})
    void aprobacionPermiteOmitirMotivo(String motivo) {
        // Act / Assert
        assertThat(service.decidir("NIT", admin, request(DecisionVerificacionAlmacen.APROBADO, motivo)).motivoVerificacion()).isNull();
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", " "})
    void rechazoExigeMotivo(String motivo) {
        // Act / Assert
        assertThatThrownBy(() -> service.decidir("NIT", admin, request(DecisionVerificacionAlmacen.RECHAZADO, motivo)))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(almacenes, never()).flush();
    }

    @ParameterizedTest @NullSource @EnumSource(value = EstadoVerificacionAlmacen.class, names = {"APROBADO", "RECHAZADO"})
    void soloPendientesAdmitenAmbasDecisiones(EstadoVerificacionAlmacen actual) {
        // Arrange
        almacen.setEstadoVerificacion(actual);
        // Act / Assert
        for (var d : DecisionVerificacionAlmacen.values())
            assertThatThrownBy(() -> service.decidir("NIT", admin, request(d, "Revisado"))).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(almacen.getEstadoVerificacion()).isEqualTo(actual); verify(almacenes, never()).flush();
    }

    @ParameterizedTest @NullSource @EnumSource(value = EstadoVerificacionAlmacen.class, names = {"APROBADO", "RECHAZADO"})
    void revisionExplicitaConservaUltimaDecision(EstadoVerificacionAlmacen actual) {
        // Arrange
        almacen.setEstadoVerificacion(actual); almacen.setFechaDecisionVerificacion(FECHA);
        almacen.setAdministradorDecisionId(99L); almacen.setMotivoVerificacion("Anterior");
        // Act
        var r = service.solicitarRevision("NIT", admin);
        // Assert
        assertThat(r.estadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE);
        assertThat(r.fechaDecisionVerificacion()).isEqualTo(FECHA);
        assertThat(r.administradorDecisionId()).isEqualTo(99L); assertThat(r.motivoVerificacion()).isEqualTo("Anterior");
    }

    @Test void revisionRepetidaEsErrorDeNegocio() {
        // Act / Assert
        assertThatThrownBy(() -> service.solicitarRevision("NIT", admin)).isInstanceOf(ReglaDeNegocioException.class);
        verify(almacenes, never()).flush();
    }

    @ParameterizedTest @NullSource @EnumSource(value = Rol.class, names = {"COMPRADOR", "ADMIN_ALMACEN", "REPRESENTANTE_FUNDACION"})
    void servicioDefiendeLecturasYEscrituras(Rol rol) {
        // Arrange
        var u = new UsuarioEntity(); u.setId(8L); u.setRoles(rol == null ? Set.of() : Set.of(rol));
        var p = rol == null ? null : new UsuarioPrincipal(u, "fixture");
        // Act / Assert
        assertThatThrownBy(() -> service.decidir("NIT", p, request(DecisionVerificacionAlmacen.APROBADO, null))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.solicitarRevision("NIT", p)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.consultar(null, 0, 20, p)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.consultarUno("NIT", p)).isInstanceOf(AccessDeniedException.class);
        verify(almacenes, never()).flush();
    }

    @Test void requestsInvalidosNoMutan() {
        // Act / Assert
        for (var r : new DecisionVerificacionRequestDTO[]{null, request(null, null), request(DecisionVerificacionAlmacen.APROBADO, "x".repeat(1001))})
            assertThatThrownBy(() -> service.decidir("NIT", admin, r)).isInstanceOf(ReglaDeNegocioException.class);
        verify(almacenes, never()).flush();
    }

    @ParameterizedTest @EnumSource(DecisionVerificacionAlmacen.class)
    void motivoDeMilCaracteresEsAdmitido(DecisionVerificacionAlmacen d) {
        // Arrange
        String motivo = "x".repeat(1000);
        // Act / Assert
        assertThat(service.decidir("NIT", admin, request(d, motivo)).motivoVerificacion()).isEqualTo(motivo);
    }

    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,101"})
    void paginacionInvalida(int page, int size) {
        // Act / Assert
        assertThatThrownBy(() -> service.consultar(null, page, size, admin)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test void inexistenteNoSeCrea() {
        // Arrange
        when(almacenes.findForUpdate("missing")).thenReturn(Optional.empty());
        when(almacenes.findRevision("missing")).thenReturn(Optional.empty());
        // Act / Assert
        assertThatThrownBy(() -> service.solicitarRevision("missing", admin)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.decidir("missing", admin, request(DecisionVerificacionAlmacen.APROBADO, null))).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.consultarUno("missing", admin)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(almacenes, never()).save(any());
    }
}
