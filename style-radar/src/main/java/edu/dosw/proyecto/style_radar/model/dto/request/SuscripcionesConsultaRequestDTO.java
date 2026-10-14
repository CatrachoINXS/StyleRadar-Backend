package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SuscripcionesConsultaRequestDTO {
    @Min(0)
    private int page = 0;
    @Min(1) @Max(100)
    private int size = 20;
}
