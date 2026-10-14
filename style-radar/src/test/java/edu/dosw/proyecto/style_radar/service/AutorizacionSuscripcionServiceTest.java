package edu.dosw.proyecto.style_radar.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.model.dto.response.CuentaAdministrativaResponseDTO;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;

@ExtendWith(MockitoExtension.class)
class AutorizacionSuscripcionServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock AdministradorAlmacenRepository vinculos;
    @Mock BloqueoCuentaService bloqueo;
    AutorizacionSuscripcionService servicio;
    UsuarioEntity usuario;
    UsuarioPrincipal actor;
    @BeforeEach void preparar() {
        usuario = UsuarioEntity.builder().id(1L).roles(Set.of(Rol.ADMIN_ALMACEN)).build();
        actor = new UsuarioPrincipal(usuario, ""); servicio = new AutorizacionSuscripcionService(usuarios, vinculos, bloqueo);
    }
    void actual(EstadoCuenta estado) {
        when(usuarios.findDatosActuales(1L)).thenReturn(Optional.of(new CuentaAdministrativaResponseDTO(1L, "Fixture", "f@example.co",
                Set.of(Rol.ADMIN_ALMACEN), estado, null, null, null, null)));
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void vinculadoActivoPermitido(boolean escritura) {
        actual(EstadoCuenta.ACTIVA); when(usuarios.findRolesActuales(1L)).thenReturn(Set.of(Rol.ADMIN_ALMACEN));
        when(vinculos.existsByUsuario_IdAndAlmacen_Nit(1L, "propio")).thenReturn(true);
        if (escritura) when(bloqueo.bloquear(List.of(1L))).thenReturn(Map.of(1L, usuario));
        servicio.autorizar("propio", actor, escritura);
        if (escritura) verify(bloqueo).bloquear(List.of(1L)); else verifyNoInteractions(bloqueo);
    }
    @ParameterizedTest @EnumSource(value=Rol.class, names={"COMPRADOR", "REPRESENTANTE_FUNDACION", "ADMIN_STYLERADAR"})
    void rolIncorrectoNoBasta(Rol rol) {
        usuario.setRoles(Set.of(rol)); var p = new UsuarioPrincipal(usuario, "");
        assertThatThrownBy(() -> servicio.autorizar("propio", p, true)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(usuarios, bloqueo, vinculos);
    }
    @Test void sinPrincipal401() {
        assertThatThrownBy(() -> servicio.autorizar("nit", null, true)).isInstanceOf(BadCredentialsException.class);
    }
    @ParameterizedTest @EnumSource(value=EstadoCuenta.class, names={"INACTIVA", "BLOQUEADA"})
    void cuentaSuspendidaAunquePrincipalAnteriorActivo(EstadoCuenta estado) {
        actual(estado);
        assertThatThrownBy(() -> servicio.autorizar("nit", actor, false)).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(vinculos);
    }
    @Test void sinUsuario401() {
        assertThatThrownBy(() -> servicio.autorizar("nit", actor, false)).isInstanceOf(BadCredentialsException.class);
    }
    @Test void usuarioDesapareceAntesDelLock401() {
        assertThatThrownBy(() -> servicio.autorizar("nit", actor, true)).isInstanceOf(BadCredentialsException.class);
    }
    @Test void rolRetiradoEnBD403() {
        actual(EstadoCuenta.ACTIVA); when(usuarios.findRolesActuales(1L)).thenReturn(Set.of(Rol.COMPRADOR));
        assertThatThrownBy(() -> servicio.autorizar("nit", actor, false)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void nitAjenoOSinVinculo404() {
        actual(EstadoCuenta.ACTIVA); when(usuarios.findRolesActuales(1L)).thenReturn(Set.of(Rol.ADMIN_ALMACEN));
        assertThatThrownBy(() -> servicio.autorizar("ajeno", actor, false)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void nullDeCuentaConservaCompatibilidadRF66() {
        actual(null); when(usuarios.findRolesActuales(1L)).thenReturn(Set.of(Rol.ADMIN_ALMACEN));
        when(vinculos.existsByUsuario_IdAndAlmacen_Nit(1L, "propio")).thenReturn(true);
        servicio.autorizar("propio", actor, false);
    }
}
