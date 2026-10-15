package edu.dosw.proyecto.style_radar.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioValidatorTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioValidator usuarioValidator;

    @Test
    void validarEmailUnicoShouldThrowExceptionWhenEmailExists() {
        // Arrange
        when(usuarioRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> usuarioValidator.validarEmailUnico("test@example.com"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Ya existe un usuario registrado con el email: test@example.com");
    }

    @Test
    void validarEmailUnicoShouldNotThrowWhenEmailDoesNotExist() {
        // Arrange
        when(usuarioRepository.existsByEmail("test@example.com")).thenReturn(false);

        // Act & Assert
        assertThatCode(() -> usuarioValidator.validarEmailUnico("test@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void validarExisteShouldThrowExceptionWhenUsuarioDoesNotExist() {
        // Arrange
        when(usuarioRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> usuarioValidator.validarExiste(1L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe un usuario con ID: 1");
    }

    @Test
    void validarExisteShouldThrowExceptionWhenIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> usuarioValidator.validarExiste(null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void validarExisteShouldNotThrowWhenUsuarioExists() {
        // Arrange
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertThatCode(() -> usuarioValidator.validarExiste(1L))
                .doesNotThrowAnyException();
    }
}
