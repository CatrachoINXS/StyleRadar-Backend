package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;

@Mapper(componentModel = "spring", uses = PrendaEntityMapper.class)
public interface ItemCatalogoEntityMapper {

    @Mapping(target = "almacen", ignore = true)
    ItemCatalogoEntity toEntity(ItemCatalogo itemCatalogo);

    @Mapping(target = "almacenNit", source = "almacen.nit")
    ItemCatalogo toDomain(ItemCatalogoEntity itemCatalogoEntity);
}
