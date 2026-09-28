package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogoEntity, Long> {

    @EntityGraph(attributePaths = { "almacen", "prenda", "inventario" })
    List<ItemCatalogoEntity> findByAlmacen_Nit(String nit);

    @EntityGraph(attributePaths = { "almacen", "prenda", "inventario" })
    Optional<ItemCatalogoEntity> findByIdAndAlmacen_Nit(Long id, String nit);
}
