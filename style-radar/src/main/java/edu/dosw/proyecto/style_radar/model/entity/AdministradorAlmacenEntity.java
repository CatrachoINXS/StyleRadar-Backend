package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

/** Solo se aprovisiona por una acción administrativa autenticada, sin cascade de identidad. */
@Entity @Table(name = "administradores_almacen", uniqueConstraints =
        @UniqueConstraint(name = "uk_administrador_almacen", columnNames = {"usuario_id", "almacen_nit"}))
@Getter @Setter @NoArgsConstructor
public class AdministradorAlmacenEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private UsuarioEntity usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_nit", nullable = false, updatable = false)
    private AlmacenEntity almacen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignado_por_id", nullable = false, updatable = false)
    private UsuarioEntity asignadoPor;
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private Instant fechaAsignacion;
}
