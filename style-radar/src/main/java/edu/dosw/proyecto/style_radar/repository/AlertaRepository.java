package edu.dosw.proyecto.style_radar.repository;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import edu.dosw.proyecto.style_radar.model.domain.TipoObjetivoAlerta;
import edu.dosw.proyecto.style_radar.model.entity.AlertaEntity;
import jakarta.persistence.LockModeType;

public interface AlertaRepository extends JpaRepository<AlertaEntity, Long> {
    List<AlertaEntity> findByUsuario_IdOrderByFechaCreacionDescIdDesc(Long usuarioId);
    boolean existsByUsuario_IdAndTipoObjetivoAndIdentificadorObjetivoAndActivaTrue(
            Long usuarioId, TipoObjetivoAlerta tipo, Long objetivoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AlertaEntity a where a.id = :id")
    Optional<AlertaEntity> findForUpdate(@Param("id") Long id);

    @Query("select a.id from AlertaEntity a where a.activa = true and a.tipoObjetivo = :tipo "
            + "and a.identificadorObjetivo = :objetivo order by a.id")
    List<Long> findIdsActivasDelObjetivo(@Param("tipo") TipoObjetivoAlerta tipo, @Param("objetivo") Long objetivo);

    @Query("select a.id from AlertaEntity a, BusquedaGuardadaEntity b where a.activa = true "
            + "and a.tipoObjetivo = edu.dosw.proyecto.style_radar.model.domain.TipoObjetivoAlerta.BUSQUEDA_GUARDADA "
            + "and a.identificadorObjetivo = b.id and a.usuario.id = b.usuario.id "
            + "and a.fechaCreacion < :publicacion "
            + "and not exists (select n.id from NotificacionEntity n where n.alerta = a "
            + "and n.itemCatalogoId = :itemId "
            + "and n.tipoEvento = edu.dosw.proyecto.style_radar.model.domain.TipoEventoNotificacion.NUEVA_COINCIDENCIA) "
            + "order by a.id")
    List<Long> findIdsBusquedasCandidatas(@Param("publicacion") Instant publicacion, @Param("itemId") Long itemId);
}
