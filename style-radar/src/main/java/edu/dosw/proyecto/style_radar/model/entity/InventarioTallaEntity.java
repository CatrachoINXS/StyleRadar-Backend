package edu.dosw.proyecto.style_radar.model.entity;

import edu.dosw.proyecto.style_radar.model.domain.Talla;
import jakarta.persistence.CheckConstraint;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "inventario_tallas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventario_item_talla",
                columnNames = { "item_catalogo_id", "talla" }),
        check = @CheckConstraint(name = "ck_inventario_unidades_no_negativas", constraint = "unidades >= 0"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventarioTallaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Talla talla;

    @Column(nullable = false)
    private Integer unidades;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_catalogo_id", nullable = false)
    private ItemCatalogoEntity itemCatalogo;
}
