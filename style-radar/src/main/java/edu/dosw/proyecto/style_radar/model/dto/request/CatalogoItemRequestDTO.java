package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoItemRequestDTO {

    @NotBlank(message = "El nombre de la prenda es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String descripcion;

    @NotNull(message = "El tipo de prenda es obligatorio")
    private TipoPrenda tipo;

    @NotBlank(message = "La marca de la prenda es obligatoria")
    @Size(max = 30, message = "Máximo 30 caracteres")
    private String marca;

    @NotBlank(message = "El color es obligatorio")
    @Size(max = 30, message = "Máximo 30 caracteres")
    private String color;

    @NotNull(message = "El estilo es obligatorio")
    private Estilo estilo;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor que cero")
    private Double precio;
}
