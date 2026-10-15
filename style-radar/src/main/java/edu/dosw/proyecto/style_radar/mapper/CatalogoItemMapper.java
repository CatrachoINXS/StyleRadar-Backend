package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.dto.request.CatalogoItemRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;

@Mapper(componentModel = "spring")
public interface CatalogoItemMapper {

    @Mapping(target = "id", ignore = true)
    Prenda toPrenda(CatalogoItemRequestDTO request);

    @Mapping(target = "itemId", source = "id")
    @Mapping(target = "prendaId", source = "prenda.id")
    @Mapping(target = "nombre", source = "prenda.nombre")
    @Mapping(target = "descripcion", source = "prenda.descripcion")
    @Mapping(target = "tipo", source = "prenda.tipo")
    @Mapping(target = "marca", source = "prenda.marca")
    @Mapping(target = "color", source = "prenda.color")
    @Mapping(target = "estilo", source = "prenda.estilo")
    CatalogoItemResponseDTO toResponse(ItemCatalogo itemCatalogo);
}
