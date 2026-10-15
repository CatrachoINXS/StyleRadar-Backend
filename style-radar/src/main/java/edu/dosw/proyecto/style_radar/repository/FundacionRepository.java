package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;

public interface FundacionRepository extends JpaRepository<FundacionEntity, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByRepresentante_Id(Long representanteId);

    boolean existsByRepresentante_IdAndIdNot(Long representanteId, Long id);

    Optional<FundacionEntity> findByRepresentante_Id(Long representanteId);

    List<FundacionEntity> findByActivaTrue();

    List<FundacionEntity> findAllByOrderByNombreAsc();
}
