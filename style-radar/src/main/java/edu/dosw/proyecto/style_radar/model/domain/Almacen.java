package edu.dosw.proyecto.style_radar.model.domain;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Almacen {

    private String nit;
    private String nombreComercial;
    private String descripcion;
    private String telefono;
    private String correoContacto;
    private Double latitud;
    private Double longitud;
    private Double reputacion;

    private Set<CategoriaAlmacen> categorias = new HashSet<>();

    public Almacen(String nit, String nombreComercial, String descripcion, String telefono,
            String correoContacto, Double latitud, Double longitud, Double reputacion) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto,
                latitud, longitud, reputacion, new HashSet<>());
    }

    public Almacen(
            String nit,
            String nombreComercial,
            String descripcion,
            String telefono,
            String correoContacto) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto, null, null, null);
    }
}
