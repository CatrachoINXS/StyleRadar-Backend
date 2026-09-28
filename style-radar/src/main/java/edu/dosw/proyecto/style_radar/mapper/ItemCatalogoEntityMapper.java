package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;

@Mapper(componentModel = "spring")
public interface ItemCatalogoEntityMapper {

    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "prenda", ignore = true)
    ItemCatalogoEntity toEntity(ItemCatalogo itemCatalogo);

    ItemCatalogo toDomain(ItemCatalogoEntity itemCatalogoEntity);
}
