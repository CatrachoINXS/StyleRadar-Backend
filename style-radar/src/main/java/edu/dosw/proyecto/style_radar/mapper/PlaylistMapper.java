package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.dto.response.PlaylistResponseDTO;

@Mapper(componentModel = "spring", uses = { PrendaMapper.class })
public interface PlaylistMapper {

    @Mapping(target = "likes", expression = "java(playlist.getLikesCount())")
    @Mapping(target = "totalPrendas", expression = "java(playlist.getTotalPrendas())")
    PlaylistResponseDTO toResponse(Playlist playlist);

}

