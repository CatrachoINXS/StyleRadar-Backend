package edu.dosw.proyecto.style_radar.model.entity;

import java.util.ArrayList;
import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCatalogoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double precio;

    @Column(columnDefinition = "bytea")
    private byte[] imagen;

    private Integer stock;

    @Enumerated(EnumType.STRING)
    private EstadoItem estado;

    @ElementCollection
    @CollectionTable(name = "item_catalogo_tallas", joinColumns = @JoinColumn(name = "item_catalogo_id"))
    @Column(name = "talla")
    @Enumerated(EnumType.STRING)
    private List<Talla> tallasDisponibles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_nit", nullable = false)
    private AlmacenEntity almacen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private PrendaEntity prenda;
}
