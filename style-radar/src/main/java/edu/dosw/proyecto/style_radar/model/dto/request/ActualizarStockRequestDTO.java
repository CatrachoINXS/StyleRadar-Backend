package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarStockRequestDTO {

    @NotNull(message = "Las unidades son obligatorias")
    @PositiveOrZero(message = "Las unidades no pueden ser negativas")
    private Integer unidades;
}
