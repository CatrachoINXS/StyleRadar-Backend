package edu.dosw.proyecto.style_radar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PublicacionSegundaManoResponseDTO;

@Mapper(componentModel = "spring", uses = { PrendaMapper.class })
public interface PublicacionSegundaManoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaPublicacion", ignore = true)
    PublicacionSegundaMano toDomain(CrearPublicacionSegundaManoRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaPublicacion", ignore = true)
    PublicacionSegundaMano toDomain(EditarPublicacionSegundaManoRequestDTO request);

    BusquedaSegundaManoCriteria toCriteria(BusquedaSegundaManoRequestDTO request);

    PublicacionSegundaManoResponseDTO toResponse(PublicacionSegundaMano domain);

    default edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO<PublicacionSegundaManoResponseDTO> toPageResponse(
            org.springframework.data.domain.Page<PublicacionSegundaMano> page) {
        return new edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

}
