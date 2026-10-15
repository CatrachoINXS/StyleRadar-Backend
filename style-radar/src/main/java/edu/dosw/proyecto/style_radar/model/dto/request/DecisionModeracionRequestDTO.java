package edu.dosw.proyecto.style_radar.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.dosw.proyecto.style_radar.model.domain.DecisionModeracion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DecisionModeracionRequestDTO {
    @NotNull
    private DecisionModeracion decision;
    @Size(max = 1000)
    private String motivo;

    @JsonIgnore @Schema(hidden = true)
    @AssertTrue(message = "El rechazo exige un motivo no vacío")
    public boolean isMotivoValido() {
        return decision != DecisionModeracion.RECHAZADA || (motivo != null && !motivo.isBlank());
    }

    /** Rechaza identidad, fechas, precio y estados inyectados por el cliente. */
    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo no permitido en la decisión de moderación");
    }
}
