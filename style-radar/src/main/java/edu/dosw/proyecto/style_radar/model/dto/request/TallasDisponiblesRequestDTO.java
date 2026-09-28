package edu.dosw.proyecto.style_radar.model.dto.request;

import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.Talla;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TallasDisponiblesRequestDTO {

    @NotEmpty(message = "Debe registrar al menos una talla")
    private Set<@NotNull(message = "La talla no puede ser nula") Talla> tallas;
}
