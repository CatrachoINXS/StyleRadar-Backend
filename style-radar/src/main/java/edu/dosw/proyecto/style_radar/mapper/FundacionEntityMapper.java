package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Fundacion;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;

@Mapper(componentModel = "spring")
public interface FundacionEntityMapper {

    @Mapping(target = "representanteId", source = "representante.id")
    @Mapping(target = "distanciaKm", ignore = true)
    Fundacion toDomain(FundacionEntity entity);
}
