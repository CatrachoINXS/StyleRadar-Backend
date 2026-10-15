package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.SolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.entity.SolicitudDonacionEntity;

@Mapper(componentModel = "spring", uses = { PrendaEntityMapper.class })
public interface SolicitudDonacionEntityMapper {

    @Mapping(target = "publicacionId", source = "publicacion.id")
    @Mapping(target = "prenda", source = "publicacion.prenda")
    @Mapping(target = "donanteId", source = "donante.id")
    @Mapping(target = "fundacionId", source = "fundacion.id")
    @Mapping(target = "fundacionNombre", source = "fundacion.nombre")
    SolicitudDonacion toDomain(SolicitudDonacionEntity entity);
}
