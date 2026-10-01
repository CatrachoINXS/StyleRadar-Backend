package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;

public interface PlaylistRepository extends JpaRepository<PlaylistEntity, Long> {

    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    List<PlaylistEntity> findByVisibilidadOrderByFechaCreacionDesc(VisibilidadPlaylist visibilidad);

    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    List<PlaylistEntity> findByUsuario_IdOrderByFechaCreacionDesc(Long usuarioId);

    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    Optional<PlaylistEntity> findByIdAndUsuario_Id(Long id, Long usuarioId);

    @Override
    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    Optional<PlaylistEntity> findById(Long id);
}
