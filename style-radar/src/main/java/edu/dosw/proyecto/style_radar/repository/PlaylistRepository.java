package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;

public interface PlaylistRepository extends JpaRepository<PlaylistEntity, Long> {

    @Query(value = "select p.id from PlaylistEntity p left join p.likesUsuarios l "
            + "where p.visibilidad = edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist.PUBLICA "
            + "group by p.id, p.fechaCreacion order by count(distinct l) desc, p.fechaCreacion desc, p.id asc",
            countQuery = "select count(p) from PlaylistEntity p "
            + "where p.visibilidad = edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist.PUBLICA")
    Page<Long> findIdsPopulares(Pageable pageable);

    long countByVisibilidad(VisibilidadPlaylist visibilidad);

    // Una colección por consulta: no multiplica prendas por likes ni pagina un fetch join.
    @Query("select distinct p from PlaylistEntity p join fetch p.usuario left join fetch p.prendas "
            + "where p.id in :ids and p.visibilidad = edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist.PUBLICA")
    List<PlaylistEntity> findDetallesPopulares(@Param("ids") List<Long> ids);

    @Query("select distinct p from PlaylistEntity p left join fetch p.likesUsuarios where p.id in :ids")
    List<PlaylistEntity> cargarLikesPorIds(@Param("ids") List<Long> ids);

    // El lock bloquea la fila raíz, sin outer joins de colecciones.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlaylistEntity p where p.id = :id")
    Optional<PlaylistEntity> findForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    List<PlaylistEntity> findByVisibilidadOrderByFechaCreacionDesc(VisibilidadPlaylist visibilidad);

    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    List<PlaylistEntity> findByUsuario_IdOrderByFechaCreacionDesc(Long usuarioId);

    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    Optional<PlaylistEntity> findByIdAndUsuario_Id(Long id, Long usuarioId);

    List<PlaylistEntity> findByGuardadosUsuariosContainsOrderByFechaCreacionDesc(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = { "usuario", "prendas", "likesUsuarios" })
    Optional<PlaylistEntity> findById(Long id);
}
