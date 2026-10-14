package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionDonacionRequestDTO {

    @NotNull(message = "Debe indicar si acepta la solicitud")
    private Boolean aceptar;

    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    private String motivo;

    @AssertTrue(message = "El motivo es obligatorio al rechazar una solicitud")
    public boolean isMotivoPresenteSiRechaza() {
        return !Boolean.FALSE.equals(aceptar) || (motivo != null && !motivo.isBlank());
    }
}
