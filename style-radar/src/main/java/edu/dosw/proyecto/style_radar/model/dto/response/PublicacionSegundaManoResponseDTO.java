package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionSegundaManoResponseDTO {
    private Long id;
    private Long usuarioId;
    private PrendaResponseDTO prenda;
    private Talla talla;
    private EstadoConservacion estadoConservacion;
    private Double precio;
    private EstadoPublicacion estado;
    private Instant fechaPublicacion;
    private List<String> fotos;
}
