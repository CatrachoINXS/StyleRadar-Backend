package edu.dosw.proyecto.style_radar.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.dosw.proyecto.style_radar.model.entity.NotificacionEntity;

public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {
    long countByUsuario_Id(Long usuarioId);
    Page<NotificacionEntity> findByUsuario_IdOrderByFechaCreacionDescIdDesc(Long usuarioId, Pageable pageable);
}
