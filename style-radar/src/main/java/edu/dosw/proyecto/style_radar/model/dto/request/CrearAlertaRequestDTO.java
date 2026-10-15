package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@Schema(description = "Exactamente un objetivo: busquedaGuardadaId propia o itemCatalogoId agotado. Sin propietario ni fechas.")
public class CrearAlertaRequestDTO {
    @Positive
    private Long busquedaGuardadaId;
    @Positive
    private Long itemCatalogoId;

    @JsonIgnore @Schema(hidden = true)
    @AssertTrue(message = "Debe indicar exactamente un objetivo")
    public boolean isObjetivoExclusivo() {
        return (busquedaGuardadaId == null) != (itemCatalogoId == null);
    }
}
