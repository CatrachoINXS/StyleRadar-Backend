package edu.dosw.proyecto.style_radar.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CancelarRenovacionRequestDTO {
    @NotNull @AssertFalse(message = "Solo se permite cancelar la renovación")
    private Boolean renovacionHabilitada;
    @JsonAnySetter
    public void rechazarCampo(String nombre, Object valor) {
        throw new IllegalArgumentException("Solo se permite renovacionHabilitada=false");
    }
}
