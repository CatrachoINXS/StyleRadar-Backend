package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionSegundaMano {
    private Long id;
    private Long usuarioId;
    private Prenda prenda;
    private Talla talla;
    private EstadoConservacion estadoConservacion;
    private Double precio;
    private EstadoPublicacion estado;
    private Instant fechaPublicacion;
    @Builder.Default
    private List<String> fotos = new ArrayList<>();
}
