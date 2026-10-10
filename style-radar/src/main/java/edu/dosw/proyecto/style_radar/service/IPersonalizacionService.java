package edu.dosw.proyecto.style_radar.service;

import org.springframework.data.domain.Page;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.ResultadoRecomendaciones;

public interface IPersonalizacionService {
    Page<ItemCatalogo> obtenerFeed(Long usuarioId, int page, int size);
    ResultadoRecomendaciones obtenerRecomendaciones(Long usuarioId, int page, int size);
}
