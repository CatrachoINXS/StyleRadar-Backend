package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "publicaciones_segunda_mano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionSegundaManoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private PrendaEntity prenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Talla talla;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoConservacion estadoConservacion;

    @Column(nullable = false)
    private Double precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPublicacion estado;

    @Column(nullable = false, updatable = false)
    private Instant fechaPublicacion;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "publicacion_segunda_mano_fotos", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "foto_url")
    @Builder.Default
    private List<String> fotos = new ArrayList<>();
}
