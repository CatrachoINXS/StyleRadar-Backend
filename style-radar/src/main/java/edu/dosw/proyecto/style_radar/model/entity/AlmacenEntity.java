package edu.dosw.proyecto.style_radar.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.CategoriaAlmacen;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "almacenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenEntity {

    @Id
    private String nit;

    private String nombreComercial;

    private String descripcion;

    private String telefono;

    private String correoContacto;

    /** Latitud esperada entre -90 y 90; opcional para registros existentes. */
    private Double latitud;

    /** Longitud esperada entre -180 y 180; opcional para registros existentes. */
    private Double longitud;

    /** Reputacion esperada entre 0.0 y 5.0; su calculo pertenece al modulo de almacenes. */
    private Double reputacion;

    @OneToMany(mappedBy = "almacen", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCatalogoEntity> itemsCatalogo = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "almacen_categorias", joinColumns = @JoinColumn(name = "almacen_nit"),
            uniqueConstraints = @UniqueConstraint(name = "uk_almacen_categoria",
                    columnNames = { "almacen_nit", "categoria" }))
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private Set<CategoriaAlmacen> categorias = new HashSet<>();

    public AlmacenEntity(String nit, String nombreComercial, String descripcion, String telefono,
            String correoContacto, Double latitud, Double longitud, Double reputacion,
            List<ItemCatalogoEntity> itemsCatalogo) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto,
                latitud, longitud, reputacion, itemsCatalogo, new HashSet<>());
    }

    public AlmacenEntity(
            String nit,
            String nombreComercial,
            String descripcion,
            String telefono,
            String correoContacto,
            List<ItemCatalogoEntity> itemsCatalogo) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto,
                null, null, null, itemsCatalogo);
    }
}
