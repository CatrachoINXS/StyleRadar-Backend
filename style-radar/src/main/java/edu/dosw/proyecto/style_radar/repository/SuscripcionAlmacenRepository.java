package edu.dosw.proyecto.style_radar.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import edu.dosw.proyecto.style_radar.model.entity.SuscripcionAlmacenEntity;

public interface SuscripcionAlmacenRepository extends JpaRepository<SuscripcionAlmacenEntity, Long> {
    @EntityGraph(attributePaths = {"almacen", "plan"})
    Optional<SuscripcionAlmacenEntity> findByAlmacenActivoNit(String nit);

    @EntityGraph(attributePaths = {"almacen", "plan"})
    Optional<SuscripcionAlmacenEntity> findByIdAndAlmacen_Nit(Long id, String nit);

    @EntityGraph(attributePaths = {"almacen", "plan"})
    @Query("select s from SuscripcionAlmacenEntity s where s.almacen.nit = :nit "
            + "and s.estadoSuscripcion = edu.dosw.proyecto.style_radar.model.domain.EstadoSuscripcion.ACTIVA "
            + "and s.inicioPeriodo <= :ahora and s.finPeriodo > :ahora")
    Optional<SuscripcionAlmacenEntity> findVigente(@Param("nit") String nit, @Param("ahora") Instant ahora);

    @EntityGraph(attributePaths = {"almacen", "plan"})
    Page<SuscripcionAlmacenEntity> findByAlmacen_Nit(String nit, Pageable pageable);
    long countByAlmacen_Nit(String nit);
}
