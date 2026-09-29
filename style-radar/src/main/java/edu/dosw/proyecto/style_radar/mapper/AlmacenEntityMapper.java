package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Almacen;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;

@Mapper(componentModel = "spring")
public interface AlmacenEntityMapper {

    @Mapping(target = "itemsCatalogo", ignore = true)
    @Mapping(target = "latitud", source = "latitud")
    @Mapping(target = "longitud", source = "longitud")
    @Mapping(target = "reputacion", source = "reputacion")
    AlmacenEntity toEntity(Almacen almacen);

    @Mapping(target = "latitud", source = "latitud")
    @Mapping(target = "longitud", source = "longitud")
    @Mapping(target = "reputacion", source = "reputacion")
    Almacen toDomain(AlmacenEntity almacenEntity);
}
