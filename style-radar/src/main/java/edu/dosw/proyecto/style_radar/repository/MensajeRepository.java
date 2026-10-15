package edu.dosw.proyecto.style_radar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.MensajeEntity;

public interface MensajeRepository extends JpaRepository<MensajeEntity, Long> {

    List<MensajeEntity> findByConversacion_IdOrderByFechaEnvioAscIdAsc(Long conversacionId);
}
