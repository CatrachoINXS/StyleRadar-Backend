package edu.dosw.proyecto.style_radar.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CrearSuscripcionRequestDTO {
    @NotNull @Positive
    private Long planId;
    @JsonAnySetter
    public void rechazarCampo(String nombre, Object valor) {
        throw new IllegalArgumentException("Solo se permite planId");
    }
}
