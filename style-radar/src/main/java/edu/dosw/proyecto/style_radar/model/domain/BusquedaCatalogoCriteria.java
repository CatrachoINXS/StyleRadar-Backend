package edu.dosw.proyecto.style_radar.model.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BusquedaCatalogoCriteria {

    String q;
    TipoPrenda tipo;
    String color;
    Talla talla;
    Double precioMin;
    Double precioMax;
    String marca;
    Estilo estilo;
    OrdenCatalogo orden;
    Double latitudUsuario;
    Double longitudUsuario;
}
