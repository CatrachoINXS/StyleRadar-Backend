package edu.dosw.proyecto.style_radar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.FotoProbadorEntity;

public interface FotoProbadorRepository extends JpaRepository<FotoProbadorEntity, Long> {

    Optional<FotoProbadorEntity> findByUsuario_Id(Long usuarioId);
}
