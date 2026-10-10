package edu.dosw.proyecto.style_radar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;

public interface PrendaRepository extends JpaRepository<PrendaEntity, Long> {
    // Definiciones sin publicación siguen visibles. Si tiene publicaciones, exige al menos una visible.
    String VISIBLE = "(not exists (select i.id from ItemCatalogoEntity i where i.prenda = p) "
            + "or exists (select i.id from ItemCatalogoEntity i where i.prenda = p and "
            + edu.dosw.proyecto.style_radar.service.VisibilidadModeracion.JPQL + "))";

    @org.springframework.data.jpa.repository.Query("select p from PrendaEntity p where " + VISIBLE + " order by p.id")
    java.util.List<PrendaEntity> findPublicas();

    @org.springframework.data.jpa.repository.Query("select p.id from PrendaEntity p where p.id in :ids and not " + VISIBLE)
    java.util.List<Long> findIdsOcultas(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}
