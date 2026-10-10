package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;

public interface PersonalizacionCatalogoRepository {
    Page<Long> findIdsFeed(Set<Estilo> estilos, Set<Talla> tallas,
            List<BusquedaCatalogoCriteria> busquedas, Pageable pageable);
    Page<Long> findIdsRecomendaciones(Set<Estilo> estilos, Set<Talla> tallas, Pageable pageable);
}
