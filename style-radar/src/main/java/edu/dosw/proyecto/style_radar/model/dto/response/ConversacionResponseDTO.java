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
public class ConversacionResponseDTO {
    private Long id;
    private Long publicacionId;
    private String prendaNombre;
    private Long compradorId;
    private Long vendedorId;
    private Instant fechaCreacion;
}
