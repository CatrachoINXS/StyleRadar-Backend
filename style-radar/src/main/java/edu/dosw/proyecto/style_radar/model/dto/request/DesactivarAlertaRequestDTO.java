package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DesactivarAlertaRequestDTO {
    @NotNull @AssertFalse(message = "Solo se permite activa=false")
    private Boolean activa;
}
