package edu.dosw.proyecto.style_radar.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaCatalogoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.ErrorResponseDTO;

class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/v1/catalogo/buscar";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = request();

    @Test
    void dataIntegrityConflictShouldReturnSafeConflictResponse() {
        // Arrange
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("constraint uk_inventario_item_talla");

        // Act
        ResponseEntity<ErrorResponseDTO> response = handler.handleDataIntegrityViolation(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("uk_inventario_item_talla");
        assertThat(response.getBody().getPath()).isEqualTo(PATH);
    }

    @Test
    void bindingErrorsShouldBeReturnedByFieldWithoutDuplicates() {
        // Arrange
        BindException exception = new BindException(new BusquedaCatalogoRequestDTO(), "request");
        exception.addError(new FieldError("request", "size", "size inválido"));
        exception.addError(new FieldError("request", "size", "otro mensaje"));

        // Act
        ResponseEntity<ErrorResponseDTO> response = handler.handleBindingValidation(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValidationErrors())
                .containsExactlyEntriesOf(java.util.Map.of("size", "size inválido"));
    }

    @Test
    void malformedHttpRequestShouldReturnGenericBadRequest() {
        // Arrange
        Exception exception = new IllegalArgumentException("internal parser detail");

        // Act
        ResponseEntity<ErrorResponseDTO> response = handler.handleInvalidHttpRequest(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("internal parser detail");
    }

    @Test
    void unexpectedErrorShouldReturnSafeInternalServerError() {
        // Arrange
        Exception exception = new IllegalStateException("sensitive internal class and stack detail");

        // Act
        ResponseEntity<ErrorResponseDTO> response = handler.handleUnexpected(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Ocurrió un error interno. Intente nuevamente más tarde.")
                .doesNotContain("sensitive");
        assertThat(response.getBody().getValidationErrors()).isEmpty();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(PATH);
        return request;
    }
}
