package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.BusquedaGuardadaEntity;

public interface BusquedaGuardadaRepository extends JpaRepository<BusquedaGuardadaEntity, Long> {

    List<BusquedaGuardadaEntity> findByUsuario_IdOrderByFechaCreacionDesc(Long usuarioId);

    Optional<BusquedaGuardadaEntity> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
