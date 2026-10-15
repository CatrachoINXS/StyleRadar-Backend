package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Metadatos de la foto del probador; los bytes no salen del servicio. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotoProbador {
    private Long usuarioId;
    private String contentType;
    private Integer ancho;
    private Integer alto;
    private Instant fechaCarga;
}
