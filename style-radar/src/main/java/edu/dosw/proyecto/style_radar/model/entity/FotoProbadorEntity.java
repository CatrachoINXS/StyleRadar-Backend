package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Foto frontal del usuario para el probador virtual: una por usuario, se reemplaza al cargar otra. */
@Entity
@Table(name = "fotos_probador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FotoProbadorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] datos;

    @Column(nullable = false)
    private Integer ancho;

    @Column(nullable = false)
    private Integer alto;

    @Column(nullable = false)
    private Instant fechaCarga;
}
