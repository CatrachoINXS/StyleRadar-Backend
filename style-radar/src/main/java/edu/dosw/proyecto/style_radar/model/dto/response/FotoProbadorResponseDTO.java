package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotoProbadorResponseDTO {
    private String contentType;
    private Integer ancho;
    private Integer alto;
    private Instant fechaCarga;
}
