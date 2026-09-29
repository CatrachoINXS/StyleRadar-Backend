package edu.dosw.proyecto.style_radar.service;

import org.springframework.data.domain.Page;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;

public interface IBusquedaCatalogoService {

    Page<ItemCatalogo> buscar(BusquedaCatalogoCriteria criteria, int page, int size);
}
