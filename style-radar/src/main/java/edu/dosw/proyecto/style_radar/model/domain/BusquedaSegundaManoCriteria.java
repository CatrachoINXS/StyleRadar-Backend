package edu.dosw.proyecto.style_radar.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaSegundaManoCriteria {
    private String q;
    private TipoPrenda tipo;
    private String color;
    private Talla talla;
    private EstadoConservacion estadoConservacion;
    private Double precioMin;
    private Double precioMax;
    private String marca;
    private Estilo estilo;
}
