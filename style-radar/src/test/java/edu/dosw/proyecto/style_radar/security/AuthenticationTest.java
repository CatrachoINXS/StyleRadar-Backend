package edu.dosw.proyecto.style_radar.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.*;
import edu.dosw.proyecto.style_radar.service.impl.AuthServiceImpl;
import edu.dosw.proyecto.style_radar.validator.CredencialValidator;

@ExtendWith(MockitoExtension.class)
class AuthenticationTest {
    @Mock UsuarioRepository usuarios;
    @Mock CredencialRepository credenciales;
    @Mock IUsuarioService registro;
    @Mock ICredencialService provision;
    @Mock CredencialValidator validator;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UsuarioUserDetailsService details;
    private AuthServiceImpl auth;
    private UsuarioEntity usuario;
    private JwtUtil jwt;

    @BeforeEach
    void setup() {
        usuario = UsuarioEntity.builder().id(1L).email("test@ejemplo.co").build();
        details = new UsuarioUserDetailsService(usuarios, credenciales);
        var provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        jwt = new JwtUtil(Base64.getEncoder().encodeToString(new byte[32]), Duration.ofHours(1), Clock.systemUTC());
        auth = new AuthServiceImpl(new ProviderManager(provider), jwt, registro, provision, validator);
    }

    private void withCredential() {
        when(usuarios.findAllByEmailNormalizado("test@ejemplo.co")).thenReturn(List.of(usuario));
        when(credenciales.findById(1L)).thenReturn(Optional.of(new CredencialEntity(usuario, encoder.encode("test password"))));
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void loginRealBcryptParaCadaActor(Rol rol) {
        // Arrange
        usuario.setRoles(Set.of(rol));
        withCredential();
        // Act
        var result = auth.login(" Test@Ejemplo.co ", "test password");
        // Assert
        assertThat(result.roles()).containsExactly(rol);
        assertThat(result.usuarioId()).isEqualTo(1L);
        assertThat(jwt.validateAndGetUserId(result.accessToken())).isEqualTo(1L);
        assertThat(result.expiresIn()).isEqualTo(3600);
    }

    @Test
    void passwordIncorrectoEsBadCredentials() {
        // Arrange
        withCredential();
        // Act & Assert
        assertThatThrownBy(() -> auth.login("test@ejemplo.co", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void correoInexistenteEsBadCredentialsSinEnumeracion() {
        assertThatThrownBy(() -> auth.login("missing@ejemplo.co", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(credenciales);
    }

    @Test
    void usuarioSinCredencialNoAutentica() {
        when(usuarios.findAllByEmailNormalizado("test@ejemplo.co")).thenReturn(List.of(usuario));
        assertThatThrownBy(() -> auth.login("test@ejemplo.co", "test password"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void estadoNoPermitidoRechazaLogin(EstadoCuenta estado) {
        // Arrange
        usuario.setEstadoCuenta(estado);
        withCredential();
        // Act & Assert
        assertThatThrownBy(() -> auth.login("test@ejemplo.co", "test password"))
                .isInstanceOf(AccountStatusException.class);
    }

    @Test
    void historicoSoloObtieneCompradorYActivaConCredencialReal() {
        // Arrange
        usuario.setRoles(null);
        usuario.setEstadoCuenta(null);
        withCredential();
        // Act
        var result = auth.login("test@ejemplo.co", "test password");
        // Assert
        assertThat(result.roles()).containsExactly(Rol.COMPRADOR);
        assertThat(usuario.getRoles()).isNull();
        assertThat(usuario.getEstadoCuenta()).isNull();
    }

    @Test
    void principalTieneAuthoritiesActualesYPasswordBorrable() {
        // Arrange
        usuario.setRoles(Set.of(Rol.ADMIN_ALMACEN, Rol.COMPRADOR));
        withCredential();
        // Act
        var principal = details.loadUserByUsername("test@ejemplo.co");
        // Assert
        assertThat(principal.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN_ALMACEN", "ROLE_COMPRADOR");
        assertThat(encoder.matches("test password", principal.getPassword())).isTrue();
        principal.eraseCredentials();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void duplicadosHistoricosNoSeSeleccionanInclusoConCoincidenciaExacta() {
        // Arrange
        when(usuarios.findAllByEmailNormalizado("test@ejemplo.co"))
                .thenReturn(List.of(usuario, UsuarioEntity.builder().id(2L).email("TEST@ejemplo.co").build()));
        // Act & Assert
        assertThatThrownBy(() -> auth.login("test@ejemplo.co", "test password")).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(credenciales);
    }

    @Test
    void bearerConsultaUsuarioUnaVezSinReleerHash() {
        // Arrange
        usuario.setRoles(Set.of(Rol.REPRESENTANTE_FUNDACION));
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        // Act
        var principal = details.loadActiveById(1L);
        // Assert
        assertThat(principal.getRoles()).containsExactly(Rol.REPRESENTANTE_FUNDACION);
        verify(usuarios, times(1)).findById(1L);
        verifyNoInteractions(credenciales);
    }

    @Test
    void bearerUsuarioInexistenteRechazado() {
        assertThatThrownBy(() -> details.loadActiveById(1L)).isInstanceOf(UsernameNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void bearerRechazaEstadoActualNoPermitido(EstadoCuenta estado) {
        usuario.setEstadoCuenta(estado);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> details.loadActiveById(1L)).isInstanceOf(AccountStatusException.class);
    }

    @Test
    void falloPersistenciaDuranteLoginNoSeClasificaComoCredencialInvalida() {
        when(usuarios.findAllByEmailNormalizado("test@ejemplo.co")).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> auth.login("test@ejemplo.co", "test password"))
                .isInstanceOf(InternalAuthenticationServiceException.class);
    }

    @Test
    void registroReutilizaServiciosYValidacionSinDtoHttp() {
        // Arrange
        Usuario input = Usuario.builder().nombre("New").email("new@ejemplo.co").build();
        Usuario saved = Usuario.builder().id(5L).build();
        when(registro.registrarUsuario(input)).thenReturn(saved);
        // Act
        assertThat(auth.registrarComprador(input, "test password")).isSameAs(saved);
        // Assert
        var order = inOrder(validator, registro, provision);
        order.verify(validator).validarPassword("test password");
        order.verify(registro).registrarUsuario(input);
        order.verify(provision).crearCredencial(5L, "test password");
    }
}
