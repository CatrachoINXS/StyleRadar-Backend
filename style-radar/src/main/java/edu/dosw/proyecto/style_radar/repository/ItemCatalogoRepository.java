package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.service.VisibilidadModeracion;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogoEntity, Long>,
        JpaSpecificationExecutor<ItemCatalogoEntity>, PersonalizacionCatalogoRepository {

    /** Inventario no negativo: existe disponibilidad si alguna talla tiene unidades positivas. */
    @Query("""
            select i.id from ItemCatalogoEntity i
            where i.almacen.nit = :nit
            and """ + VisibilidadModeracion.JPQL + """

            and exists (select v.id from InventarioTallaEntity v
                        where v.itemCatalogo = i and v.unidades > 0)
            and (:inicio is null or i.fechaPublicacion >= :inicio)
            and (:fin is null or i.fechaPublicacion <= :fin)
            order by i.fechaPublicacion desc, i.id asc
            """)
    Page<Long> findIdsPublicosDelAlmacen(@Param("nit") String nit,
            @Param("inicio") Instant inicio, @Param("fin") Instant fin, Pageable pageable);

    @EntityGraph(attributePaths = { "almacen", "prenda", "inventario" })
    List<ItemCatalogoEntity> findByIdIn(List<Long> ids);

    @Override
    @EntityGraph(attributePaths = { "almacen", "prenda", "inventario" })
    Page<ItemCatalogoEntity> findAll(Specification<ItemCatalogoEntity> specification, Pageable pageable);

    @EntityGraph(attributePaths = { "almacen", "prenda", "inventario" })
    List<ItemCatalogoEntity> findByAlmacen_Nit(String nit);

    @EntityGraph(attributePaths = { "almacen", "prenda", "inventario" })
    @Query("select i from ItemCatalogoEntity i where i.almacen.nit = :nit and " + VisibilidadModeracion.JPQL
            + " order by i.id")
    List<ItemCatalogoEntity> findPublicosByAlmacenNit(@Param("nit") String nit);

    @Query("select i.id from ItemCatalogoEntity i where i.estadoModeracion = :estado "
            + "or (:estado = edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion.NO_REQUERIDA "
            + "and i.estadoModeracion is null) order by i.fechaPublicacion asc, i.id asc")
    Page<Long> findIdsModeracion(@Param("estado") edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion estado,
            Pageable pageable);

    @EntityGraph(attributePaths = { "almacen", "prenda" })
    @Query("select i from ItemCatalogoEntity i where i.id in :ids")
    List<ItemCatalogoEntity> findRevisionByIdIn(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemCatalogoEntity i where i.id = :id and i.almacen.nit = :nit")
    Optional<ItemCatalogoEntity> findByIdAndAlmacen_Nit(Long id, String nit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemCatalogoEntity i where i.id = :id")
    Optional<ItemCatalogoEntity> findForUpdate(@Param("id") Long id);
}
