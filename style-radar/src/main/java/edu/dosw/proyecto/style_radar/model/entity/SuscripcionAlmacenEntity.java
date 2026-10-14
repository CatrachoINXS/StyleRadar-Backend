package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import edu.dosw.proyecto.style_radar.model.domain.EstadoSuscripcion;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "suscripciones_almacen", uniqueConstraints =
        @UniqueConstraint(name = "uk_suscripcion_almacen_activa", columnNames = "almacen_activo_nit"),
        indexes = @Index(name = "ix_suscripcion_historial", columnList = "almacen_nit, fecha_creacion, id"))
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Check(constraints = "fin_periodo > inicio_periodo and fecha_creacion = inicio_periodo "
        + "and ((estado_suscripcion = 'ACTIVA' and almacen_activo_nit is not null and almacen_activo_nit = almacen_nit) "
        + "or (estado_suscripcion = 'FINALIZADA' and almacen_activo_nit is null)) "
        + "and ((renovacion_habilitada = true and fecha_cancelacion_renovacion is null) "
        + "or (renovacion_habilitada = false and fecha_cancelacion_renovacion is not null))")
public class SuscripcionAlmacenEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_nit", nullable = false, updatable = false)
    private AlmacenEntity almacen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private PlanSuscripcionEntity plan;
    @Enumerated(EnumType.STRING) @Column(name = "estado_suscripcion", nullable = false)
    private EstadoSuscripcion estadoSuscripcion;
    /** NULL para historial; NIT para ACTIVA. UNIQUE admite varios NULL en H2 y PostgreSQL. */
    @Column(name = "almacen_activo_nit")
    private String almacenActivoNit;
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
    @Column(name = "inicio_periodo", nullable = false, updatable = false)
    private Instant inicioPeriodo;
    @Column(name = "fin_periodo", nullable = false, updatable = false)
    private Instant finPeriodo;
    @Column(name = "renovacion_habilitada", nullable = false)
    private boolean renovacionHabilitada;
    @Column(name = "fecha_cancelacion_renovacion")
    private Instant fechaCancelacionRenovacion;

    public void finalizar() {
        estadoSuscripcion = EstadoSuscripcion.FINALIZADA;
        almacenActivoNit = null;
    }

    public void cancelarRenovacion(Instant ahora) {
        if (renovacionHabilitada) {
            renovacionHabilitada = false;
            fechaCancelacionRenovacion = ahora;
        }
    }
}
