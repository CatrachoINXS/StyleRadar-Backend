package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;

import edu.dosw.proyecto.style_radar.model.domain.ImagenCatalogo;
import edu.dosw.proyecto.style_radar.model.dto.response.ImagenCatalogoResponseDTO;

@Mapper(componentModel = "spring")
public interface ImagenCatalogoMapper {

    ImagenCatalogoResponseDTO toResponse(ImagenCatalogo imagen);
}
