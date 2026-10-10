package edu.dosw.proyecto.style_radar.validator;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import edu.dosw.proyecto.style_radar.exception.*;

class AlertaValidatorTest {
    private final AlertaValidator validator = new AlertaValidator();

    @ParameterizedTest @CsvSource({"-1,", ",-1", "0,", ",0", "1,2", ","})
    void objetivoInvalidoSeRechazaEnDominioDeServicio(Long busqueda, Long item) {
        // Act & Assert
        assertThatThrownBy(() -> validator.objetivo(busqueda, item)).isInstanceOf(SolicitudAlertaInvalidaException.class);
    }

    @ParameterizedTest @CsvSource({"1,", ",1"})
    void unObjetivoPositivoEsValido(Long busqueda, Long item) {
        // Act & Assert
        assertThatCode(() -> validator.objetivo(busqueda, item)).doesNotThrowAnyException();
    }

    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,101"})
    void paginacionInvalidaNoLlegaAJpa(int page, int size) {
        // Act & Assert
        assertThatThrownBy(() -> validator.paginacion(page, size)).isInstanceOf(SolicitudAlertaInvalidaException.class);
    }

    @Test
    void soloDesactivacionExplicitaPermitida() {
        // Act & Assert
        assertThatThrownBy(() -> validator.desactivacion(null)).isInstanceOf(SolicitudAlertaInvalidaException.class);
        assertThatThrownBy(() -> validator.desactivacion(true)).isInstanceOf(SolicitudAlertaInvalidaException.class);
        assertThatCode(() -> validator.desactivacion(false)).doesNotThrowAnyException();
    }
}
