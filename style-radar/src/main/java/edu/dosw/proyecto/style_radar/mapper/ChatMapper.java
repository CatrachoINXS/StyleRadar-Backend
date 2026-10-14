package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;

import edu.dosw.proyecto.style_radar.model.domain.Conversacion;
import edu.dosw.proyecto.style_radar.model.domain.Mensaje;
import edu.dosw.proyecto.style_radar.model.dto.response.ConversacionResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.MensajeResponseDTO;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    ConversacionResponseDTO toResponse(Conversacion conversacion);

    MensajeResponseDTO toResponse(Mensaje mensaje);
}
