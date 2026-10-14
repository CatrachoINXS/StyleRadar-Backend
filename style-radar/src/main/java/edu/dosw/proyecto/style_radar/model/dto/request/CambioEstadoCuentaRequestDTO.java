package edu.dosw.proyecto.style_radar.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CambioEstadoCuentaRequestDTO {
    @NotNull
    private EstadoCuenta estado;
    @Size(max = 1000)
    private String motivo;

    @JsonIgnore @Schema(hidden = true)
    @AssertTrue(message = "El bloqueo exige un motivo no vacío")
    public boolean isMotivoValido() {
        return estado != EstadoCuenta.BLOQUEADA || (motivo != null && !motivo.isBlank());
    }

    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo no permitido en el cambio de estado de cuenta");
    }
}
