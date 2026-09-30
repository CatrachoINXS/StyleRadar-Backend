package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;

@Mapper(componentModel = "spring", uses = { PrendaEntityMapper.class })
public interface PublicacionSegundaManoEntityMapper {

    @Mapping(target = "usuario", ignore = true)
    PublicacionSegundaManoEntity toEntity(PublicacionSegundaMano domain);

    @Mapping(target = "usuarioId", source = "usuario.id")
    PublicacionSegundaMano toDomain(PublicacionSegundaManoEntity entity);

}


