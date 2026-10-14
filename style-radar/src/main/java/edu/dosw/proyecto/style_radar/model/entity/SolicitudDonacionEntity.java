package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;

import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
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
@Table(name = "solicitudes_donacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudDonacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private PublicacionSegundaManoEntity publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donante_id", nullable = false)
    private UsuarioEntity donante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fundacion_id", nullable = false)
    private FundacionEntity fundacion;

    @Column(length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudDonacion estado;

    @Column(length = 500)
    private String motivoRechazo;

    @Column(length = 200)
    private String puntoEntrega;

    private Instant franjaInicio;

    private Instant franjaFin;

    @Column(nullable = false, updatable = false)
    private Instant fechaSolicitud;

    private Instant fechaDecision;

    private Instant fechaRecepcion;
}
