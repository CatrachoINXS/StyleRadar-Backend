package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;

@Mapper(componentModel = "spring", uses = { PrendaEntityMapper.class, InventarioTallaEntityMapper.class })
public interface ItemCatalogoEntityMapper {

    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "imagenes", ignore = true)
    ItemCatalogoEntity toEntity(ItemCatalogo itemCatalogo);

    @Mapping(target = "almacenNit", source = "almacen.nit")
    @Mapping(target = "tallasDisponibles", ignore = true)
    ItemCatalogo toDomain(ItemCatalogoEntity itemCatalogoEntity);

    @AfterMapping
    default void enlazarInventario(@MappingTarget ItemCatalogoEntity itemCatalogoEntity) {
        itemCatalogoEntity.getInventario().forEach(inventario -> inventario.setItemCatalogo(itemCatalogoEntity));
    }
}
