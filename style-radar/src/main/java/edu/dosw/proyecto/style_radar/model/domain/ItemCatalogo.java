package edu.dosw.proyecto.style_radar.model.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCatalogo {

    private Long id;
    private Double precio;
    private byte[] imagen;
    private Integer stock;
    private EstadoItem estado;
    private List<Talla> tallasDisponibles = new ArrayList<>();
    private Prenda prenda;
    private String almacenNit;
}
