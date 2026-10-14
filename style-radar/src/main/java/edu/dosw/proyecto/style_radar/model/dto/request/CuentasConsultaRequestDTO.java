package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CuentasConsultaRequestDTO {
    private EstadoCuenta estado;
    @NotNull @Min(0)
    private Integer page = 0;
    @NotNull @Min(1) @Max(100)
    private Integer size = 20;
}
