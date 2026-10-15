package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;

@Mapper(componentModel = "spring", uses = { PrendaEntityMapper.class })
public interface PlaylistEntityMapper {

    @Mapping(target = "usuario", ignore = true)
    PlaylistEntity toEntity(Playlist domain);

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "prendas", source = "prendas")
    @Mapping(target = "likesUsuarios", source = "likesUsuarios")
    Playlist toDomain(PlaylistEntity entity);
}
