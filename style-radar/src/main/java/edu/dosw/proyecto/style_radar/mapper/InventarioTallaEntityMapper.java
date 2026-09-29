package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.InventarioTalla;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;

@Mapper(componentModel = "spring")
public interface InventarioTallaEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "itemCatalogo", ignore = true)
    InventarioTallaEntity toEntity(InventarioTalla inventario);

    InventarioTalla toDomain(InventarioTallaEntity inventario);
}
