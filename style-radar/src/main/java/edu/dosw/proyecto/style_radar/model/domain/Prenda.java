package edu.dosw.proyecto.style_radar.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
public class Prenda {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private String marca;
    private String color;
}
