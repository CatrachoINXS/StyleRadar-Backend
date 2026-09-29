package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;

import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;

@Mapper(componentModel = "spring")
public interface PrendaEntityMapper {

    PrendaEntity toEntity(Prenda prenda);

    Prenda toDomain(PrendaEntity prendaEntity);
}
