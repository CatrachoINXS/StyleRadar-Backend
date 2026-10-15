package edu.dosw.proyecto.style_radar.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class Prenda {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private TipoPrenda tipo;
    private String marca;
    private String color;
    private Estilo estilo;
}
