package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
