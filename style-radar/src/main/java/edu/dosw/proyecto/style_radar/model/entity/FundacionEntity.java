package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fundaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private String direccion;

    private String telefono;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fundacion_categorias", joinColumns = @JoinColumn(name = "fundacion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_prenda")
    @Builder.Default
    private Set<TipoPrenda> categoriasAceptadas = new HashSet<>();

    // Un representante administra una sola fundación.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "representante_id", nullable = false, unique = true)
    private UsuarioEntity representante;

    @Column(nullable = false)
    private boolean activa;

    @Column(nullable = false, updatable = false)
    private Instant fechaRegistro;
}
