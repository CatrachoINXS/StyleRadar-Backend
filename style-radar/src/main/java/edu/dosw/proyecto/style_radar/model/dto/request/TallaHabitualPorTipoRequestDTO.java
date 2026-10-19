package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TallaHabitualPorTipoRequestDTO {

    @NotNull(message = "El tipo de prenda es obligatorio")
    private TipoPrenda tipoPrenda;

    @NotEmpty(message = "Debe registrar al menos una talla para el tipo de prenda")
    private Set<@NotNull(message = "La talla no puede ser nula") Talla> tallas;
}
