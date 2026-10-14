package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.access.AccessDeniedException;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.*;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionModeracionRequestDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;

class ModeracionCatalogoServiceTest {
    ItemCatalogoRepository items;
    ModeracionCatalogoService service;
    ItemCatalogoEntity item;
    UsuarioPrincipal admin;
    static final Instant FECHA = Instant.parse("2026-10-09T17:00:00.123456789Z");

    @BeforeEach void preparar() {
        items = mock(ItemCatalogoRepository.class);
        service = new ModeracionCatalogoService(items, mock(PrendaEntityMapper.class), mock(PrendaMapper.class), Clock.fixed(FECHA, ZoneOffset.UTC));
        item = new ItemCatalogoEntity(); item.setId(10L); item.setAlmacen(new AlmacenEntity());
        item.setEstadoModeracion(EstadoModeracion.PENDIENTE);
        when(items.findForUpdate(10L)).thenReturn(Optional.of(item));
        var u = new UsuarioEntity(); u.setId(7L); u.setRoles(Set.of(Rol.ADMIN_STYLERADAR)); admin = new UsuarioPrincipal(u, "fixture");
    }

    DecisionModeracionRequestDTO request(DecisionModeracion d, String motivo) {
        var r = new DecisionModeracionRequestDTO(); r.setDecision(d); r.setMotivo(motivo); return r;
    }

    @ParameterizedTest @EnumSource(DecisionModeracion.class)
    void fechaVieneDeClockYAdministradorDelPrincipal(DecisionModeracion d) {
        var r = service.decidir(10L, admin, request(d, " Información revisada "));
        assertThat(r.fechaDecisionModeracion()).isEqualTo(FECHA.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(r.administradorDecisionId()).isEqualTo(7L); assertThat(r.motivoModeracion()).isEqualTo("Información revisada");
        verify(items).findForUpdate(10L); verify(items).flush();
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", " "})
    void aprobacionPermiteOmitirMotivo(String motivo) {
        assertThat(service.decidir(10L, admin, request(DecisionModeracion.APROBADA, motivo)).motivoModeracion()).isNull();
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", " "})
    void rechazoExigeMotivo(String motivo) {
        assertThatThrownBy(() -> service.decidir(10L, admin, request(DecisionModeracion.RECHAZADA, motivo))).isInstanceOf(ReglaDeNegocioException.class);
        verify(items, never()).flush();
    }

    @ParameterizedTest @NullSource @EnumSource(value = EstadoModeracion.class, names = {"NO_REQUERIDA", "APROBADA", "RECHAZADA"})
    void soloPendientesAdmitenDecisiones(EstadoModeracion actual) {
        item.setEstadoModeracion(actual);
        assertThatThrownBy(() -> service.decidir(10L, admin, request(DecisionModeracion.APROBADA, null))).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(item.getEstadoModeracion()).isEqualTo(actual); verify(items, never()).flush();
    }

    @ParameterizedTest @NullSource @EnumSource(value = EstadoModeracion.class, names = {"NO_REQUERIDA", "APROBADA", "RECHAZADA"})
    void revisionExplicitaAdmiteEstadosNoPendientes(EstadoModeracion actual) {
        item.setEstadoModeracion(actual);
        assertThat(service.solicitarRevision(10L, admin).estadoModeracion()).isEqualTo(EstadoModeracion.PENDIENTE);
    }

    @Test void revisionRepetidaEsErrorDeNegocio() {
        assertThatThrownBy(() -> service.solicitarRevision(10L, admin)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @ParameterizedTest @NullSource @EnumSource(value = Rol.class, names = {"COMPRADOR", "ADMIN_ALMACEN", "REPRESENTANTE_FUNDACION"})
    void servicioDefiendeEscriturasContraRolesAjenos(Rol rol) {
        var u = new UsuarioEntity(); u.setId(8L); u.setRoles(rol == null ? Set.of() : Set.of(rol));
        var p = rol == null ? null : new UsuarioPrincipal(u, "fixture");
        assertThatThrownBy(() -> service.decidir(10L, p, request(DecisionModeracion.APROBADA, null))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.solicitarRevision(10L, p)).isInstanceOf(AccessDeniedException.class);
        verify(items, never()).flush();
    }

    @Test void servicioRechazaRequestsInvalidosSinMutar() {
        for (var r : new DecisionModeracionRequestDTO[]{null, request(null, null), request(DecisionModeracion.APROBADA, "x".repeat(1001))})
            assertThatThrownBy(() -> service.decidir(10L, admin, r)).isInstanceOf(ReglaDeNegocioException.class);
        verify(items, never()).flush();
    }

    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,101"})
    void servicioRechazaPaginacionInvalida(int page, int size) {
        assertThatThrownBy(() -> service.consultar(EstadoModeracion.PENDIENTE, page, size)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test void filtroNuloEsInvalido() {
        assertThatThrownBy(() -> service.consultar(null, 0, 20)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
