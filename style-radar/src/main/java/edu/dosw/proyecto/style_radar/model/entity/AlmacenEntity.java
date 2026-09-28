package edu.dosw.proyecto.style_radar.model.entity;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "almacen", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCatalogoEntity> itemsCatalogo = new ArrayList<>();
}
