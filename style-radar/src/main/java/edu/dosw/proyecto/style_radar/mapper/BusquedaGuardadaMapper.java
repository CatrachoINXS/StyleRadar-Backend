package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.dto.request.GuardarBusquedaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.BusquedaGuardadaResponseDTO;

@Mapper(componentModel = "spring")
public interface BusquedaGuardadaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    BusquedaGuardada toDomain(GuardarBusquedaRequestDTO request);

    BusquedaGuardadaResponseDTO toResponse(BusquedaGuardada domain);
}
