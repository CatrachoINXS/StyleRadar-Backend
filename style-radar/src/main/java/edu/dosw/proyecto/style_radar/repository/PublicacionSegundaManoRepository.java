package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;

public interface PublicacionSegundaManoRepository extends JpaRepository<PublicacionSegundaManoEntity, Long>,
        JpaSpecificationExecutor<PublicacionSegundaManoEntity> {

    @Override
    @EntityGraph(attributePaths = { "usuario", "prenda", "fotos" })
    Page<PublicacionSegundaManoEntity> findAll(Specification<PublicacionSegundaManoEntity> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "usuario", "prenda", "fotos" })
    List<PublicacionSegundaManoEntity> findByUsuario_IdOrderByFechaPublicacionDesc(Long usuarioId);

    @EntityGraph(attributePaths = { "usuario", "prenda", "fotos" })
    Optional<PublicacionSegundaManoEntity> findByIdAndUsuario_Id(Long id, Long usuarioId);

    @Override
    @EntityGraph(attributePaths = { "usuario", "prenda", "fotos" })
    Optional<PublicacionSegundaManoEntity> findById(Long id);

    /** Indica si existe al menos una publicación con la prenda dada en estado DISPONIBLE. */
    @Query("select count(p) > 0 from PublicacionSegundaManoEntity p where p.prenda.id = :prendaId and p.estado = :estado")
    boolean existsDisponiblePorPrenda(@Param("prendaId") Long prendaId,
            @Param("estado") EstadoPublicacion estado);

    /** IDs de prenda que tienen al menos una publicación disponible en segunda mano. */
    @Query("select distinct p.prenda.id from PublicacionSegundaManoEntity p where p.estado = :estado")
    List<Long> findIdsPrendaDisponible(@Param("estado") EstadoPublicacion estado);
}
