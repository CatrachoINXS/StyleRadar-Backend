package edu.dosw.proyecto.style_radar.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import edu.dosw.proyecto.style_radar.model.domain.Almacen;
import edu.dosw.proyecto.style_radar.model.domain.DistanciaAlmacen;
import edu.dosw.proyecto.style_radar.model.dto.response.AlmacenResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.DistanciaAlmacenResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;

@Mapper(componentModel = "spring")
public interface AlmacenMapper {
    AlmacenResponseDTO toResponse(Almacen almacen);

    default DistanciaAlmacenResponseDTO toResponse(DistanciaAlmacen distancia) {
        return new DistanciaAlmacenResponseDTO(distancia.almacenNit(),
                BigDecimal.valueOf(distancia.distanciaKm()).setScale(2, RoundingMode.HALF_UP), "LINEA_RECTA");
    }

    default <T> PageResponseDTO<T> toPageResponse(Page<T> page) {
        return new PageResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
