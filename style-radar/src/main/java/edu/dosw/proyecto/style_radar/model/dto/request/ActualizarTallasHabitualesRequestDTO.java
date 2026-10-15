package edu.dosw.proyecto.style_radar.model.dto.request;

import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.Talla;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarTallasHabitualesRequestDTO {

    @NotNull(message = "El conjunto de tallas habituales es obligatorio")
    private Set<Talla> tallas;
}
