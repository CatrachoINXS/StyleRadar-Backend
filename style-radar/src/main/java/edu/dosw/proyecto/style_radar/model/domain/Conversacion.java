package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversacion {
    private Long id;
    private Long publicacionId;
    private String prendaNombre;
    private Long compradorId;
    private Long vendedorId;
    private Instant fechaCreacion;
}
