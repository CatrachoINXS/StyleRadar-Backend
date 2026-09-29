package edu.dosw.proyecto.style_radar.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaCatalogoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BusquedaCatalogoMapper {

    private final CatalogoItemMapper catalogoItemMapper;

    public BusquedaCatalogoCriteria toCriteria(BusquedaCatalogoRequestDTO request) {
        return BusquedaCatalogoCriteria.builder()
                .q(normalizar(request.getQ()))
                .tipo(request.getTipo())
                .color(normalizar(request.getColor()))
                .talla(request.getTalla())
                .precioMin(request.getPrecioMin())
                .precioMax(request.getPrecioMax())
                .marca(normalizar(request.getMarca()))
                .estilo(request.getEstilo())
                .orden(request.getOrden())
                .latitudUsuario(request.getLatitudUsuario())
                .longitudUsuario(request.getLongitudUsuario())
                .build();
    }

    public PageResponseDTO<CatalogoItemResponseDTO> toResponse(Page<ItemCatalogo> page) {
        return new PageResponseDTO<>(
                page.getContent().stream().map(catalogoItemMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private String normalizar(String value) {
        return value == null ? null : value.trim();
    }
}
