package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.dto.request.RegistrarUsuarioRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.UsuarioResponseDTO;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "preferenciasEstilo", ignore = true)
    @Mapping(target = "tallasHabituales", ignore = true)
    Usuario toDomain(RegistrarUsuarioRequestDTO request);

    UsuarioResponseDTO toResponse(Usuario usuario);
}
