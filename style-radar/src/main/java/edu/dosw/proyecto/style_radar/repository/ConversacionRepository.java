package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.dosw.proyecto.style_radar.model.entity.ConversacionEntity;

public interface ConversacionRepository extends JpaRepository<ConversacionEntity, Long> {

    Optional<ConversacionEntity> findByPublicacion_IdAndComprador_Id(Long publicacionId, Long compradorId);

    @EntityGraph(attributePaths = { "publicacion", "publicacion.prenda", "publicacion.usuario", "comprador" })
    @Query("select c from ConversacionEntity c where c.comprador.id = :usuarioId or c.publicacion.usuario.id = :usuarioId "
            + "order by c.fechaCreacion desc, c.id desc")
    List<ConversacionEntity> findDeParticipante(@Param("usuarioId") Long usuarioId);
}
