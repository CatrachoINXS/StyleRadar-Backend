package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarTallasHabitualesRequestDTO {

    @NotEmpty(message = "Debe registrar al menos una talla habitual por tipo de prenda")
    private List<@NotNull(message = "La entrada de talla por tipo no puede ser nula") TallaHabitualPorTipoRequestDTO> tallasPorTipo;
}
