package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.entity.CredencialEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.validator.CredencialValidator;

@ExtendWith(MockitoExtension.class)
class CredencialServiceImplTest {

    private static final String PASSWORD = "clave interna de prueba";

    @Mock
    private CredencialRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private CredencialServiceImpl service;
    private UsuarioEntity usuario;

    @BeforeEach
    void setUp() {
        service = new CredencialServiceImpl(repository, usuarioRepository,
                new CredencialValidator(repository), encoder);
        usuario = UsuarioEntity.builder().id(7L).build();
    }

    @Test
    void debeGuardarSoloUnHashYLaIdentidadCorrecta() {
        // Arrange
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        ArgumentCaptor<CredencialEntity> captor = ArgumentCaptor.forClass(CredencialEntity.class);

        // Act
        service.crearCredencial(7L, PASSWORD);

        // Assert
        verify(repository).saveAndFlush(captor.capture());
        CredencialEntity credencial = captor.getValue();
        assertThat(credencial.getUsuario()).isSameAs(usuario);
        assertThat(credencial.getPasswordHash()).startsWith("$2a$04$").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, credencial.getPasswordHash())).isTrue();
    }

    @Test
    void debeVerificarLaClaveCorrecta() {
        // Arrange
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(repository.findById(7L))
                .thenReturn(Optional.of(new CredencialEntity(usuario, encoder.encode(PASSWORD))));

        // Act
        boolean resultado = service.verificarPassword(7L, PASSWORD);

        // Assert
        assertThat(resultado).isTrue();
    }

    @Test
    void debeRechazarLaClaveIncorrecta() {
        // Arrange
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(repository.findById(7L))
                .thenReturn(Optional.of(new CredencialEntity(usuario, encoder.encode(PASSWORD))));

        // Act
        boolean resultado = service.verificarPassword(7L, "otra clave");

        // Assert
        assertThat(resultado).isFalse();
    }

    @Test
    void unUsuarioSinCredencialNoVerificaPeroSigueExistiendo() {
        // Arrange
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(repository.findById(7L)).thenReturn(Optional.empty());

        // Act
        boolean resultado = service.verificarPassword(7L, PASSWORD);

        // Assert
        assertThat(resultado).isFalse();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noDebeReemplazarUnaCredencialExistente() {
        // Arrange
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(repository.existsById(7L)).thenReturn(true);

        // Act
        // Assert
        assertThatThrownBy(() -> service.crearCredencial(7L, PASSWORD))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void crearDebeRechazarUnUsuarioInexistente() {
        // Arrange
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        // Act
        // Assert
        assertThatThrownBy(() -> service.crearCredencial(7L, PASSWORD))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void verificarDebeRechazarUnUsuarioInexistente() {
        // Arrange
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        // Act
        // Assert
        assertThatThrownBy(() -> service.verificarPassword(7L, PASSWORD))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).findById(any());
    }

    @Test
    void crearDebeValidarAntesDeConsultarOPersistir() {
        // Arrange
        String password = "secreto".repeat(20);

        // Act
        // Assert
        assertThatThrownBy(() -> service.crearCredencial(7L, password))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La contraseña no cumple los límites admitidos");
        verify(usuarioRepository, never()).findById(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void verificarDebeValidarAntesDeLeerElHash() {
        // Arrange
        String password = " ";

        // Act
        // Assert
        assertThatThrownBy(() -> service.verificarPassword(7L, password))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(repository, never()).findById(any());
    }
}
