package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardarBusquedaRequestDTO {

    @NotBlank(message = "El nombre de la búsqueda guardada es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private String nombre;

    @Pattern(regexp = "(?s).*\\S.*", message = "query no puede estar vacío")
    private String query;

    private TipoPrenda tipo;

    @Pattern(regexp = "(?s).*\\S.*", message = "color no puede estar vacío")
    private String color;

    private Talla talla;

    @DecimalMin(value = "0.0", inclusive = true, message = "precioMin debe ser mayor o igual a 0")
    private Double precioMin;

    @DecimalMin(value = "0.0", inclusive = true, message = "precioMax debe ser mayor o igual a 0")
    private Double precioMax;

    @Pattern(regexp = "(?s).*\\S.*", message = "marca no puede estar vacía")
    private String marca;

    private Estilo estilo;

    @AssertTrue(message = "precioMin no puede ser mayor que precioMax")
    public boolean isRangoPrecioValido() {
        return precioMin == null || precioMax == null || precioMin <= precioMax;
    }
}
