package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class NotificacionesRequestDTO {
    @NotNull @Min(0)
    private Integer page = 0;
    @NotNull @Min(1) @Max(100)
    private Integer size = 20;
}
