package edu.dosw.proyecto.style_radar.model.dto.request;

import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditarPublicacionSegundaManoRequestDTO {

    @NotNull(message = "Los datos de la prenda son obligatorios")
    @Valid
    private PrendaRequestDTO prenda;

    @NotNull(message = "La talla es obligatoria")
    private Talla talla;

    @NotNull(message = "El estado de conservación es obligatorio")
    private EstadoConservacion estadoConservacion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio debe ser mayor o igual a 0")
    private Double precio;

    private List<String> fotos;
}
