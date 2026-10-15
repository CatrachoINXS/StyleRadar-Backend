package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaSegundaManoRequestDTO {

    @Pattern(regexp = "(?s).*\\S.*", message = "q no puede estar vacío")
    private String q;

    private TipoPrenda tipo;

    @Pattern(regexp = "(?s).*\\S.*", message = "color no puede estar vacío")
    private String color;

    private Talla talla;

    private EstadoConservacion estadoConservacion;

    @DecimalMin(value = "0.0", inclusive = true, message = "precioMin debe ser mayor o igual a 0")
    private Double precioMin;

    @DecimalMin(value = "0.0", inclusive = true, message = "precioMax debe ser mayor o igual a 0")
    private Double precioMax;

    @Pattern(regexp = "(?s).*\\S.*", message = "marca no puede estar vacía")
    private String marca;

    private Estilo estilo;

    @Min(value = 0, message = "page debe ser mayor o igual a 0")
    @NotNull(message = "page es obligatorio cuando se proporciona")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "size debe ser mayor a 0")
    @Max(value = 100, message = "size no puede ser mayor a 100")
    @NotNull(message = "size es obligatorio cuando se proporciona")
    @Builder.Default
    private Integer size = 20;

    @AssertTrue(message = "precioMin no puede ser mayor que precioMax")
    public boolean isRangoPrecioValido() {
        return precioMin == null || precioMax == null || precioMin <= precioMax;
    }
}
