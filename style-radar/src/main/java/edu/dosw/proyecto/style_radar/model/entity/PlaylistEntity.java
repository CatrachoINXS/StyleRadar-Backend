package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "playlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisibilidadPlaylist visibilidad;

    @Enumerated(EnumType.STRING)
    private Estilo estilo;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "playlist_prendas",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "prenda_id")
    )
    @Builder.Default
    private List<PrendaEntity> prendas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "playlist_likes", joinColumns = @JoinColumn(name = "playlist_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_playlist_like_usuario",
                    columnNames = { "playlist_id", "usuario_id" }))
    @Column(name = "usuario_id", nullable = false)
    @Builder.Default
    private Set<Long> likesUsuarios = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    // Un solo lote por página de playlists: evita N+1 al mapear listados.
    @BatchSize(size = 100)
    @CollectionTable(name = "playlist_guardados", joinColumns = @JoinColumn(name = "playlist_id"))
    @Column(name = "usuario_id")
    @Builder.Default
    private Set<Long> guardadosUsuarios = new HashSet<>();
}
