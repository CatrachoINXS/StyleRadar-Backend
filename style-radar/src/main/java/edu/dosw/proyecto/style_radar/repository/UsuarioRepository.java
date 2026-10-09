package edu.dosw.proyecto.style_radar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = { "preferenciasEstilo", "tallasHabituales", "roles" })
    Optional<UsuarioEntity> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = { "preferenciasEstilo", "tallasHabituales", "roles" })
    Optional<UsuarioEntity> findById(Long id);
}
