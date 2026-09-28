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
    private EstadoItem estado;
    private Prenda prenda;
    private String almacenNit;
    private List<InventarioTalla> inventario = new ArrayList<>();

    public Integer getStock() {
        return inventario == null ? 0 : inventario.stream()
                .map(InventarioTalla::getUnidades)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public List<Talla> getTallasDisponibles() {
        if (inventario == null) {
            return List.of();
        }
        return inventario.stream()
                .filter(item -> item.getUnidades() != null && item.getUnidades() > 0)
                .map(InventarioTalla::getTalla)
                .toList();
    }
}
