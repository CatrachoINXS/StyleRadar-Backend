package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.validator.AlmacenValidator;
import jakarta.validation.constraints.AssertTrue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DistanciaAlmacenRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "-90", maximum = "90")
    private Double latitudUsuario;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "-180", maximum = "180")
    private Double longitudUsuario;

    @Schema(hidden = true)
    @AssertTrue(message = "Ambas coordenadas son obligatorias, finitas y deben estar dentro del rango geográfico")
    public boolean isCoordenadasValidas() {
        return AlmacenValidator.coordenadasValidas(latitudUsuario, longitudUsuario);
    }
}
