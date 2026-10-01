package edu.dosw.proyecto.style_radar.model.dto.request;

import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPreferenciasEstiloRequestDTO {

    @NotNull(message = "El conjunto de preferencias de estilo es obligatorio")
    private Set<Estilo> preferencias;
}
