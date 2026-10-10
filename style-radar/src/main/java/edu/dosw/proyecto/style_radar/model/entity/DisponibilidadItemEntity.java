package edu.dosw.proyecto.style_radar.model.entity;

import jakarta.persistence.*;
import lombok.*;

/** Estado de eventos, independiente de EstadoItem. Protegido por el bloqueo del item. */
@Entity
@Table(name = "disponibilidad_items", check = @CheckConstraint(name = "ck_disponibilidad_ciclo", constraint = "ciclo >= 0"))
@Getter @Setter @NoArgsConstructor
public class DisponibilidadItemEntity {
    @Id
    private Long id;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_catalogo_id", nullable = false)
    private ItemCatalogoEntity item;
    @Column(nullable = false)
    private boolean primeraDisponibilidad;
    @Column(nullable = false)
    private long ciclo;
}
