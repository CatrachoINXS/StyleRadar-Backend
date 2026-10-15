package edu.dosw.proyecto.style_radar.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;

@ExtendWith(MockitoExtension.class)
class CredencialValidatorTest {

    @Mock
    private CredencialRepository repository;

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = { 0, -1 })
    void debeRechazarIdentificadoresInvalidos(Long usuarioId) {
        // Arrange
        CredencialValidator validator = new CredencialValidator(repository);

        // Act
        // Assert
        assertThatThrownBy(() -> validator.validarUsuarioId(usuarioId))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @ParameterizedTest
    @MethodSource("passwordsInvalidos")
    void debeRechazarSecretosInvalidosSinIncluirlosEnElMensaje(String password) {
        // Arrange
        CredencialValidator validator = new CredencialValidator(repository);

        // Act
        // Assert
        assertThatThrownBy(() -> validator.validarPassword(password))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La contraseña no cumple los límites admitidos");
    }

    @ParameterizedTest
    @MethodSource("passwordsValidos")
    void debeAceptarElLimiteEnBytesSinRecortarElPassword(String password) {
        // Arrange
        CredencialValidator validator = new CredencialValidator(repository);

        // Act
        // Assert
        assertThatCode(() -> validator.validarPassword(password)).doesNotThrowAnyException();
    }

    @Test
    void debeRechazarUnaCredencialExistente() {
        // Arrange
        CredencialValidator validator = new CredencialValidator(repository);
        when(repository.existsById(1L)).thenReturn(true);

        // Act
        // Assert
        assertThatThrownBy(() -> validator.validarNoExiste(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El usuario ya tiene una credencial registrada");
    }

    private static Stream<String> passwordsInvalidos() {
        return Stream.of(null, "", "   ", "secreto\0oculto", "a".repeat(73), "é".repeat(37));
    }

    private static Stream<String> passwordsValidos() {
        return Stream.of("a".repeat(72), "é".repeat(36), " secreto con espacios ");
    }
}
