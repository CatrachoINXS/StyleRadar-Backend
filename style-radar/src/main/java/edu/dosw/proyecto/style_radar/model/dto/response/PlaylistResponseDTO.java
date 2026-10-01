package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistResponseDTO {
    private Long id;
    private Long usuarioId;
    private String nombre;
    private String descripcion;
    private VisibilidadPlaylist visibilidad;
    private Estilo estilo;
    private Instant fechaCreacion;
    private List<PrendaResponseDTO> prendas;
    private int likes;
    private int totalPrendas;
}
