package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import jakarta.persistence.Column;
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
@Table(name = "busquedas_guardadas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusquedaGuardadaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String query;

    @Enumerated(EnumType.STRING)
    private TipoPrenda tipo;

    private String color;

    @Enumerated(EnumType.STRING)
    private Talla talla;

    private Double precioMin;

    private Double precioMax;

    private String marca;

    @Enumerated(EnumType.STRING)
    private Estilo estilo;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;
}
