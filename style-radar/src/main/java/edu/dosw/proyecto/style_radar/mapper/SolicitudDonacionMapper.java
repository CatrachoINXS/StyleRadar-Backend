package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;

import edu.dosw.proyecto.style_radar.model.domain.SolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.dto.response.SolicitudDonacionResponseDTO;

@Mapper(componentModel = "spring", uses = { PrendaMapper.class })
public interface SolicitudDonacionMapper {

    SolicitudDonacionResponseDTO toResponse(SolicitudDonacion solicitud);
}
