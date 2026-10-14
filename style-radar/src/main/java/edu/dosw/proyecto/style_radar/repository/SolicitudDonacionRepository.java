package edu.dosw.proyecto.style_radar.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.entity.SolicitudDonacionEntity;

public interface SolicitudDonacionRepository extends JpaRepository<SolicitudDonacionEntity, Long> {

    boolean existsByPublicacion_IdAndEstadoIn(Long publicacionId, Collection<EstadoSolicitudDonacion> estados);

    @EntityGraph(attributePaths = { "publicacion", "publicacion.prenda", "fundacion" })
    List<SolicitudDonacionEntity> findByDonante_IdOrderByFechaSolicitudDescIdDesc(Long donanteId);

    @EntityGraph(attributePaths = { "publicacion", "publicacion.prenda", "fundacion" })
    List<SolicitudDonacionEntity> findByFundacion_IdOrderByFechaSolicitudDescIdDesc(Long fundacionId);

    @EntityGraph(attributePaths = { "publicacion", "publicacion.prenda", "fundacion" })
    List<SolicitudDonacionEntity> findByFundacion_IdAndEstadoOrderByFechaSolicitudDescIdDesc(
            Long fundacionId, EstadoSolicitudDonacion estado);

    Optional<SolicitudDonacionEntity> findByIdAndFundacion_Id(Long id, Long fundacionId);
}
