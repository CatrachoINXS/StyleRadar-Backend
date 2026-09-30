package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.entity.BusquedaGuardadaEntity;

@Mapper(componentModel = "spring")
public interface BusquedaGuardadaEntityMapper {

    @Mapping(target = "usuario", ignore = true)
    BusquedaGuardadaEntity toEntity(BusquedaGuardada domain);

    @Mapping(target = "usuarioId", source = "usuario.id")
    BusquedaGuardada toDomain(BusquedaGuardadaEntity entity);
}
