package edu.dosw.proyecto.style_radar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.CredencialEntity;

public interface CredencialRepository extends JpaRepository<CredencialEntity, Long> {
}
