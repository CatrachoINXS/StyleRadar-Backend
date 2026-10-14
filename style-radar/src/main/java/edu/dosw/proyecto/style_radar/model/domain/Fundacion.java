package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fundacion {
    private Long id;
    private String nombre;
    private String descripcion;
    private String direccion;
    private String telefono;
    private String email;
    private Double latitud;
    private Double longitud;
    @Builder.Default
    private Set<TipoPrenda> categoriasAceptadas = new HashSet<>();
    private Long representanteId;
    private boolean activa;
    private Instant fechaRegistro;
    /** Solo en consultas por cercanía; no se persiste. */
    private Double distanciaKm;
}
