package edu.dosw.proyecto.style_radar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;

public interface PrendaRepository extends JpaRepository<PrendaEntity, Long> {
}
