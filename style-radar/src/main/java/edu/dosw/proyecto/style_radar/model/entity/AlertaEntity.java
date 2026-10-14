package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import edu.dosw.proyecto.style_radar.model.domain.TipoObjetivoAlerta;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alertas", uniqueConstraints = @UniqueConstraint(name = "uk_alerta_activa",
        columnNames = {"usuario_id", "tipo_objetivo", "identificador_objetivo", "activa_unica"}),
        indexes = {@Index(name = "ix_alerta_usuario_fecha", columnList = "usuario_id,fecha_creacion,id"),
                @Index(name = "ix_alerta_objetivo", columnList = "tipo_objetivo,identificador_objetivo,activa")},
        check = {@CheckConstraint(name = "ck_alerta_objetivo", constraint =
                "identificador_objetivo > 0 and tipo_objetivo in ('BUSQUEDA_GUARDADA','ITEM_CATALOGO')"),
                @CheckConstraint(name = "ck_alerta_actividad", constraint =
                "(activa = true and activa_unica = true and activa_unica is not null and fecha_desactivacion is null) or "
                + "(activa = false and activa_unica is null and fecha_desactivacion is not null)")})
@Getter @Setter @NoArgsConstructor
public class AlertaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private UsuarioEntity usuario;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_objetivo", nullable = false, updatable = false)
    private TipoObjetivoAlerta tipoObjetivo;
    // Referencia histórica deliberadamente sin FK al objetivo eliminable.
    @Column(name = "identificador_objetivo", nullable = false, updatable = false)
    private Long identificadorObjetivo;
    @Column(nullable = false)
    private boolean activa = true;
    @Column(name = "activa_unica")
    private Boolean activaUnica = true;
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
    @Column(name = "fecha_desactivacion")
    private Instant fechaDesactivacion;

    public void desactivar(Instant fecha) {
        if (activa) {
            activa = false;
            activaUnica = null;
            fechaDesactivacion = fecha;
        }
    }
}
