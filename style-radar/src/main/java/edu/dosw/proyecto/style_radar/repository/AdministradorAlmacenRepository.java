package edu.dosw.proyecto.style_radar.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.dosw.proyecto.style_radar.model.entity.AdministradorAlmacenEntity;

public interface AdministradorAlmacenRepository extends JpaRepository<AdministradorAlmacenEntity, Long> {
    boolean existsByUsuario_IdAndAlmacen_Nit(Long usuarioId, String nit);
    Optional<AdministradorAlmacenEntity> findByUsuario_IdAndAlmacen_Nit(Long usuarioId, String nit);
}
