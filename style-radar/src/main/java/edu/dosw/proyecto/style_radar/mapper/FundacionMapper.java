package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Fundacion;
import edu.dosw.proyecto.style_radar.model.dto.request.FundacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.FundacionResponseDTO;

@Mapper(componentModel = "spring")
public interface FundacionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "distanciaKm", ignore = true)
    Fundacion toDomain(FundacionRequestDTO request);

    FundacionResponseDTO toResponse(Fundacion fundacion);
}
