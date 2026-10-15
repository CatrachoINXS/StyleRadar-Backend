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
public class Usuario {
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private Instant fechaRegistro;
    @Builder.Default
    private Set<Estilo> preferenciasEstilo = new HashSet<>();
    @Builder.Default
    private Set<Talla> tallasHabituales = new HashSet<>();
    @Builder.Default
    private Set<Rol> roles = new HashSet<>(Set.of(Rol.COMPRADOR));
    @Builder.Default
    private EstadoCuenta estadoCuenta = EstadoCuenta.ACTIVA;
}
