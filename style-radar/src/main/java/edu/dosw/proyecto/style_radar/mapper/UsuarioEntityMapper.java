package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

@Mapper(componentModel = "spring")
public interface UsuarioEntityMapper {

    @Mapping(target = "busquedasGuardadas", ignore = true)
    UsuarioEntity toEntity(Usuario usuario);

    Usuario toDomain(UsuarioEntity usuarioEntity);
}
