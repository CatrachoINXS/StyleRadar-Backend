package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class PlaylistPopularesRequestDTO {
    @Schema(description = "Página desde cero", defaultValue = "0", minimum = "0")
    @Min(0)
    private int page = 0;
    @Schema(description = "Tamaño de página", defaultValue = "20", minimum = "1", maximum = "100")
    @Min(1)
    @Max(100)
    private int size = 20;
}
