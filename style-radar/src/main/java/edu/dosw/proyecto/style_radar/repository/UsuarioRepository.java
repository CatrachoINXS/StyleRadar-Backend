package edu.dosw.proyecto.style_radar.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    @Query(value = "select u.id from UsuarioEntity u where :estado is null or u.estadoCuenta = :estado "
            + "or (:estado = edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta.ACTIVA and u.estadoCuenta is null) order by u.id",
            countQuery = "select count(u) from UsuarioEntity u where :estado is null or u.estadoCuenta = :estado "
            + "or (:estado = edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta.ACTIVA and u.estadoCuenta is null)")
    org.springframework.data.domain.Page<Long> findIdsAdministracion(
            @Param("estado") edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta estado,
            org.springframework.data.domain.Pageable pageable);

    @Query("select count(u) from UsuarioEntity u where :estado is null or u.estadoCuenta = :estado "
            + "or (:estado = edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta.ACTIVA and u.estadoCuenta is null)")
    long countAdministracion(@Param("estado") edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta estado);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from UsuarioEntity u where u.id in :ids")
    List<UsuarioEntity> findAdministracionByIdIn(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from UsuarioEntity u where u.id = :id")
    Optional<UsuarioEntity> findAdministracionById(@Param("id") Long id);

    @Query("select u.id from UsuarioEntity u where :rol member of u.roles order by u.id")
    List<Long> findIdsByRol(@Param("rol") edu.dosw.proyecto.style_radar.model.domain.Rol rol);

    // Sin fetch de colecciones: FOR UPDATE bloquea únicamente la fila de usuarios.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioEntity u where u.id = :id")
    Optional<UsuarioEntity> findForUpdate(@Param("id") Long id);

    // Proyección escalar: consulta valores actuales aunque la entidad esté en el primer nivel de caché.
    @Query("select new edu.dosw.proyecto.style_radar.model.dto.response.CuentaAdministrativaResponseDTO("
            + "u.id, u.nombre, u.email, null, u.estadoCuenta, u.fechaRegistro, u.fechaCambioEstado, "
            + "u.administradorCambioEstadoId, u.motivoCambioEstado) from UsuarioEntity u where u.id = :id")
    Optional<edu.dosw.proyecto.style_radar.model.dto.response.CuentaAdministrativaResponseDTO> findDatosActuales(@Param("id") Long id);

    @Query("select r from UsuarioEntity u join u.roles r where u.id = :id")
    java.util.Set<edu.dosw.proyecto.style_radar.model.domain.Rol> findRolesActuales(@Param("id") Long id);

    @Query("select u.estadoCuenta from UsuarioEntity u where u.id = :id")
    edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta findEstadoActual(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update UsuarioEntity u set u.estadoCuenta = :estado, u.fechaCambioEstado = :fecha, "
            + "u.administradorCambioEstadoId = :actor, u.motivoCambioEstado = :motivo where u.id = :id")
    int actualizarEstado(@Param("id") Long id,
            @Param("estado") edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta estado,
            @Param("fecha") java.time.Instant fecha, @Param("actor") Long actor, @Param("motivo") String motivo);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = { "roles" })
    @Query("select u from UsuarioEntity u where lower(trim(u.email)) = :email")
    List<UsuarioEntity> findAllByEmailNormalizado(@Param("email") String email);

    @Query("select (count(u) > 0) from UsuarioEntity u where lower(trim(u.email)) = :email")
    boolean existsByEmailNormalizado(@Param("email") String email);

    @EntityGraph(attributePaths = { "preferenciasEstilo", "tallasHabitualesPorTipo", "roles" })
    Optional<UsuarioEntity> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = { "preferenciasEstilo", "tallasHabitualesPorTipo", "roles" })
    Optional<UsuarioEntity> findById(Long id);
}
