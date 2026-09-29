package edu.dosw.proyecto.style_radar.service;

import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;

public interface IInventarioService {

    ItemCatalogo registrarTallas(String nit, Long itemId, Set<Talla> tallas);

    ItemCatalogo actualizarDisponibilidad(String nit, Long itemId, Talla talla, Integer unidades);
}
