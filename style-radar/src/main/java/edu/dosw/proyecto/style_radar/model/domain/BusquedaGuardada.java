package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaGuardada {
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
    private Long usuarioId;
}
