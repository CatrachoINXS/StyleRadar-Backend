package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumenAlmacenRequestDTO {
    @Min(1)
    @Max(20)
    private int limite = 5;
}
