package edu.dosw.proyecto.style_radar.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenCatalogo {

    private Long id;
    private String contentType;
    private byte[] datos;
    private Long itemCatalogoId;
}
