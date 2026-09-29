package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.OrdenCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaCatalogoRequestDTO {

    @Pattern(regexp = "(?s).*\\S.*", message = "q no puede estar vacío")
    private String q;

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

    private OrdenCatalogo orden;

    @DecimalMin(value = "-90.0", message = "latitudUsuario debe ser mayor o igual a -90")
    @DecimalMax(value = "90.0", message = "latitudUsuario debe ser menor o igual a 90")
    private Double latitudUsuario;

    @DecimalMin(value = "-180.0", message = "longitudUsuario debe ser mayor o igual a -180")
    @DecimalMax(value = "180.0", message = "longitudUsuario debe ser menor o igual a 180")
    private Double longitudUsuario;

    @Min(value = 0, message = "page debe ser mayor o igual a 0")
    @NotNull(message = "page es obligatorio cuando se proporciona")
    private Integer page = 0;

    @Min(value = 1, message = "size debe ser mayor a 0")
    @Max(value = 100, message = "size no puede ser mayor a 100")
    @NotNull(message = "size es obligatorio cuando se proporciona")
    private Integer size = 20;

    @AssertTrue(message = "precioMin no puede ser mayor que precioMax")
    public boolean isRangoPrecioValido() {
        return precioMin == null || precioMax == null || precioMin <= precioMax;
    }

    @AssertTrue(message = "latitudUsuario y longitudUsuario deben proporcionarse juntas; son obligatorias para DISTANCIA")
    public boolean isCoordenadasValidas() {
        boolean tieneLatitud = latitudUsuario != null;
        boolean tieneLongitud = longitudUsuario != null;
        if (tieneLatitud != tieneLongitud) {
            return false;
        }
        return orden != OrdenCatalogo.DISTANCIA || tieneLatitud;
    }
}
