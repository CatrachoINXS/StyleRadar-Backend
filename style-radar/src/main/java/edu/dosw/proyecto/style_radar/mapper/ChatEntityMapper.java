package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Conversacion;
import edu.dosw.proyecto.style_radar.model.domain.Mensaje;
import edu.dosw.proyecto.style_radar.model.entity.ConversacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.MensajeEntity;

@Mapper(componentModel = "spring")
public interface ChatEntityMapper {

    @Mapping(target = "publicacionId", source = "publicacion.id")
    @Mapping(target = "prendaNombre", source = "publicacion.prenda.nombre")
    @Mapping(target = "compradorId", source = "comprador.id")
    @Mapping(target = "vendedorId", source = "publicacion.usuario.id")
    Conversacion toDomain(ConversacionEntity entity);

    @Mapping(target = "conversacionId", source = "conversacion.id")
    @Mapping(target = "autorId", source = "autor.id")
    Mensaje toDomain(MensajeEntity entity);
}
