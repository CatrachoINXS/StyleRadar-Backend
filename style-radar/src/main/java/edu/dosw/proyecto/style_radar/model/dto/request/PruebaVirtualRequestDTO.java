package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PruebaVirtualRequestDTO {

    @NotNull(message = "El ID de la prenda del catálogo es obligatorio")
    private Long itemCatalogoId;
}
