package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {
    private Long id;
    private Long usuarioId;
    private String nombre;
    private String descripcion;
    private VisibilidadPlaylist visibilidad;
    private Estilo estilo;
    private Instant fechaCreacion;
    @Builder.Default
    private List<Prenda> prendas = new ArrayList<>();
    @Builder.Default
    private Set<Long> likesUsuarios = new HashSet<>();

    public int getLikesCount() {
        return likesUsuarios != null ? likesUsuarios.size() : 0;
    }

    public int getTotalPrendas() {
        return prendas != null ? prendas.size() : 0;
    }
}
