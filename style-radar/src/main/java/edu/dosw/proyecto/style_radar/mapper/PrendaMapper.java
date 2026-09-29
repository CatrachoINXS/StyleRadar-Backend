package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.dto.request.PrendaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PrendaResponseDTO;

@Mapper(componentModel = "spring")
public interface PrendaMapper {
    
    @Mapping(target = "id", ignore = true)
    Prenda toDomain(PrendaRequestDTO prenda);

    PrendaResponseDTO toResponse(Prenda prenda);
}
