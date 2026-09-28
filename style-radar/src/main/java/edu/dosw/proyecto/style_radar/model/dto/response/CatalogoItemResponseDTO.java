package edu.dosw.proyecto.style_radar.model.dto.response;

import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoItemResponseDTO {

    private Long itemId;
    private Long prendaId;
    private String almacenNit;
    private String nombre;
    private String descripcion;
    private TipoPrenda tipo;
    private String marca;
    private String color;
    private Estilo estilo;
    private Double precio;
    private Integer stock;
    private EstadoItem estado;
    private List<Talla> tallasDisponibles;
}
