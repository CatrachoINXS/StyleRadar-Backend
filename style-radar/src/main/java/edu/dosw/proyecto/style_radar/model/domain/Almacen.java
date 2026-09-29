package edu.dosw.proyecto.style_radar.model.domain;

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

    public Almacen(
            String nit,
            String nombreComercial,
            String descripcion,
            String telefono,
            String correoContacto) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto, null, null, null);
    }
}
