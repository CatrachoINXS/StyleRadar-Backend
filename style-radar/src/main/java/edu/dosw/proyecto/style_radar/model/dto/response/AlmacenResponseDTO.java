package edu.dosw.proyecto.style_radar.model.dto.response;

import java.util.Set;
import edu.dosw.proyecto.style_radar.model.domain.CategoriaAlmacen;

public record AlmacenResponseDTO(String nit, String nombreComercial, String descripcion,
        Double latitud, Double longitud, Double reputacion, Set<CategoriaAlmacen> categorias) {
}
