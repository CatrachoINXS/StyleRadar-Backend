package edu.dosw.proyecto.style_radar.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagenCatalogoResponseDTO {

    private Long id;
    private Long itemCatalogoId;
    private String contentType;
}
