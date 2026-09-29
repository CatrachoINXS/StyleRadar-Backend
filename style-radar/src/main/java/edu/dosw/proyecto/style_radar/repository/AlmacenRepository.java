package edu.dosw.proyecto.style_radar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;

public interface AlmacenRepository extends JpaRepository<AlmacenEntity, String> {
}
