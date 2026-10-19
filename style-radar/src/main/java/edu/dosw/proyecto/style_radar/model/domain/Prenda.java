package edu.dosw.proyecto.style_radar.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class Prenda {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private TipoPrenda tipo;
    private String marca;
    private String color;
    private Estilo estilo;
    private Boolean disponible;

    public Prenda(Long id, String nombre, String descripcion, TipoPrenda tipo,
            String marca, String color, Estilo estilo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.marca = marca;
        this.color = color;
        this.estilo = estilo;
        this.disponible = null;
    }

    public Prenda(Long id, String nombre, String descripcion, TipoPrenda tipo,
            String marca, String color, Estilo estilo, Boolean disponible) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.marca = marca;
        this.color = color;
        this.estilo = estilo;
        this.disponible = disponible;
    }
}
