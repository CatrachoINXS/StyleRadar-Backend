package edu.dosw.proyecto.style_radar.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion;
import jakarta.persistence.Index;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items_catalogo", indexes = @Index(name = "idx_item_moderacion_fecha_id",
        columnList = "estado_moderacion,fechaPublicacion,id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCatalogoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double precio;

    @Enumerated(EnumType.STRING)
    private EstadoItem estado;

    @Column(nullable = false, updatable = false)
    private Instant fechaPublicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_nit", nullable = false)
    private AlmacenEntity almacen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private PrendaEntity prenda;

    @OneToMany(mappedBy = "itemCatalogo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventarioTallaEntity> inventario = new ArrayList<>();

    @OneToMany(mappedBy = "itemCatalogo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ImagenCatalogoEntity> imagenes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_moderacion")
    private EstadoModeracion estadoModeracion = EstadoModeracion.NO_REQUERIDA;

    @Column(name = "fecha_decision_moderacion")
    private Instant fechaDecisionModeracion;

    @Column(name = "administrador_decision_id")
    private Long administradorDecisionId;

    @Column(name = "motivo_moderacion", length = 1000)
    private String motivoModeracion;

    /** Constructor histórico conservado para clientes y fixtures existentes. */
    public ItemCatalogoEntity(Long id, Double precio, EstadoItem estado, Instant fechaPublicacion,
            AlmacenEntity almacen, PrendaEntity prenda, List<InventarioTallaEntity> inventario,
            List<ImagenCatalogoEntity> imagenes) {
        this.id = id;
        this.precio = precio;
        this.estado = estado;
        this.fechaPublicacion = fechaPublicacion;
        this.almacen = almacen;
        this.prenda = prenda;
        this.inventario = inventario;
        this.imagenes = imagenes;
    }
}
