package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaGuardadaResponseDTO {
    private Long id;
    private String nombre;
    private String query;
    private TipoPrenda tipo;
    private String color;
    private Talla talla;
    private Double precioMin;
    private Double precioMax;
    private String marca;
    private Estilo estilo;
    private Instant fechaCreacion;
}
