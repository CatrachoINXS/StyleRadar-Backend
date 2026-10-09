package edu.dosw.proyecto.style_radar.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = { "roles" })
    @Query("select u from UsuarioEntity u where lower(trim(u.email)) = :email")
    List<UsuarioEntity> findAllByEmailNormalizado(@Param("email") String email);

    @Query("select (count(u) > 0) from UsuarioEntity u where lower(trim(u.email)) = :email")
    boolean existsByEmailNormalizado(@Param("email") String email);

    @EntityGraph(attributePaths = { "preferenciasEstilo", "tallasHabituales", "roles" })
    Optional<UsuarioEntity> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = { "preferenciasEstilo", "tallasHabituales", "roles" })
    Optional<UsuarioEntity> findById(Long id);
}
