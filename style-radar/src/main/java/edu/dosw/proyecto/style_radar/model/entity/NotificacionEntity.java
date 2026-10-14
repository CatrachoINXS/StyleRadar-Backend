package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import edu.dosw.proyecto.style_radar.model.domain.TipoEventoNotificacion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notificaciones", uniqueConstraints = @UniqueConstraint(name = "uk_notificacion_evento",
        columnNames = {"alerta_id", "item_catalogo_id", "tipo_evento", "ciclo"}),
        indexes = @Index(name = "ix_notificacion_usuario_fecha", columnList = "usuario_id,fecha_creacion,id"),
        check = @CheckConstraint(name = "ck_notificacion_ciclo", constraint =
        "(tipo_evento = 'NUEVA_COINCIDENCIA' and ciclo = 0) or "
        + "(tipo_evento = 'DISPONIBILIDAD_RESTABLECIDA' and ciclo > 0)"))
@Getter @Setter @NoArgsConstructor
public class NotificacionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private UsuarioEntity usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false, updatable = false)
    private AlertaEntity alerta;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_evento", nullable = false, updatable = false)
    private TipoEventoNotificacion tipoEvento;
    @Column(name = "item_catalogo_id", nullable = false, updatable = false)
    private Long itemCatalogoId;
    @Column(nullable = false, updatable = false)
    private long ciclo;
    @Column(nullable = false, updatable = false, length = 200)
    private String mensaje;
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
}
