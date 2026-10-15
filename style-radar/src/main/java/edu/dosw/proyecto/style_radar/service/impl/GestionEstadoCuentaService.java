package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.CuentaAdministrativaMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.CambioEstadoCuentaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CuentaAdministrativaResponseDTO;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.*;
import edu.dosw.proyecto.style_radar.service.BloqueoCuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class GestionEstadoCuentaService {
    private final UsuarioRepository usuarios;
    private final CredencialRepository credenciales;
    private final BloqueoCuentaService bloqueo;
    private final CuentaAdministrativaMapper mapper;
    private final Clock clock;
    private final jakarta.validation.Validator validator;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<CuentaAdministrativaResponseDTO> consultar(EstadoCuenta estado, int page, int size, UsuarioPrincipal actor) {
        administrador(actor);
        if (page < 0 || size < 1 || size > 100) throw new ReglaDeNegocioException("Paginación de cuentas inválida");
        var pageable = PageRequest.of(page, size);
        if (pageable.getOffset() > Integer.MAX_VALUE)
            return new PageImpl<>(List.of(), pageable, usuarios.countAdministracion(estado));
        var ids = usuarios.findIdsAdministracion(estado, pageable);
        var entidades = ids.isEmpty() ? List.<UsuarioEntity>of() : usuarios.findAdministracionByIdIn(ids.getContent());
        var porId = entidades.stream().collect(Collectors.toMap(UsuarioEntity::getId, Function.identity()));
        return ids.map(id -> mapper.toResponse(porId.get(id)));
    }

    @Transactional(readOnly = true)
    public CuentaAdministrativaResponseDTO consultarUno(Long id, UsuarioPrincipal actor) {
        administrador(actor);
        return mapper.toResponse(usuarios.findAdministracionById(id).orElseThrow(() -> noExiste(id)));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CuentaAdministrativaResponseDTO cambiar(Long id, UsuarioPrincipal actor, CambioEstadoCuentaRequestDTO request) {
        exigirRol(actor);
        if (actor.getUsuarioId().equals(id))
            throw new ReglaDeNegocioException("El administrador no puede modificar el estado de su propia cuenta");
        if (request == null || request.getEstado() == null || !request.isMotivoValido()
                || (request.getMotivo() != null && request.getMotivo().length() > 1000))
            throw new ReglaDeNegocioException("Estado o motivo de cuenta inválidos");

        // Todas las decisiones RF66 comparten los administradores, incluso para objetivos compradores.
        // Evita el write skew A->B / B->A; el actor se revalida después de obtener los locks.
        var ids = new TreeSet<>(usuarios.findIdsByRol(Rol.ADMIN_STYLERADAR));
        ids.add(actor.getUsuarioId()); ids.add(id);
        var bloqueadas = bloqueo.bloquear(ids);
        validarActor(bloqueadas.containsKey(actor.getUsuarioId()) ? actual(actor.getUsuarioId()) : null);
        if (!bloqueadas.containsKey(id)) throw noExiste(id);
        var datosActuales = actual(id);
        var anterior = datosActuales.estadoCuenta();
        if (anterior == request.getEstado()) {
            // NULL->ACTIVA efectivo tampoco fabrica una decisión ni un backfill implícito.
            log.info("Estado cuenta administradorId={} usuarioObjetivoId={} anterior={} nuevo={} resultado=idempotente",
                    actor.getUsuarioId(), id, anterior, request.getEstado());
            return datosActuales;
        }
        if (request.getEstado() != EstadoCuenta.ACTIVA && datosActuales.roles().contains(Rol.ADMIN_STYLERADAR)
                && bloqueadas.keySet().stream().filter(otroId -> !otroId.equals(id)).map(this::actual).noneMatch(this::operativo))
            throw new ReglaDeNegocioException("Debe permanecer al menos otro administrador operativo con credenciales");

        String motivo = request.getMotivo() == null || request.getMotivo().isBlank() ? null : request.getMotivo().strip();
        // Actualiza solo estas cuatro columnas, incluso si el contexto conserva una entidad obsoleta.
        if (usuarios.actualizarEstado(id, request.getEstado(), clock.instant().truncatedTo(ChronoUnit.MICROS),
                actor.getUsuarioId(), motivo) != 1) throw noExiste(id);
        usuarios.flush();
        log.info("Estado cuenta administradorId={} usuarioObjetivoId={} anterior={} nuevo={} resultado=aplicado-en-transaccion",
                actor.getUsuarioId(), id, anterior, request.getEstado());
        return actual(id);
    }

    private void exigirRol(UsuarioPrincipal actor) {
        if (actor == null || !actor.getRoles().contains(Rol.ADMIN_STYLERADAR))
            throw new AccessDeniedException("Se requiere ROLE_ADMIN_STYLERADAR");
    }

    private void administrador(UsuarioPrincipal actor) {
        exigirRol(actor);
        validarActor(usuarios.findAdministracionById(actor.getUsuarioId()).map(mapper::toResponse).orElse(null));
    }

    private void validarActor(CuentaAdministrativaResponseDTO actor) {
        if (actor == null || actor.estadoCuenta() != EstadoCuenta.ACTIVA)
            throw new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE);
        if (!actor.roles().contains(Rol.ADMIN_STYLERADAR))
            throw new AccessDeniedException("Se requiere ROLE_ADMIN_STYLERADAR");
    }

    private boolean operativo(CuentaAdministrativaResponseDTO u) {
        if (!u.roles().contains(Rol.ADMIN_STYLERADAR) || u.estadoCuenta() != EstadoCuenta.ACTIVA) return false;
        String email = EmailNormalizer.normalize(u.email());
        if (!validator.validateValue(edu.dosw.proyecto.style_radar.model.dto.request.LoginRequestDTO.class, "email", email).isEmpty()) return false;
        // El flujo normal usa BCrypt y rechaza identidades ambiguas por correo normalizado.
        return credenciales.findById(u.id()).filter(c -> c.getPasswordHash() != null
                && c.getPasswordHash().matches("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}")).isPresent()
                && usuarios.findAllByEmailNormalizado(email).size() == 1;
    }

    private CuentaAdministrativaResponseDTO actual(Long id) {
        var u = usuarios.findDatosActuales(id).orElseThrow(() -> noExiste(id));
        var roles = usuarios.findRolesActuales(id);
        return new CuentaAdministrativaResponseDTO(u.id(), u.nombre(), u.email(),
                roles.isEmpty() ? Set.of(Rol.COMPRADOR) : Set.copyOf(roles),
                u.estadoCuenta() == null ? EstadoCuenta.ACTIVA : u.estadoCuenta(), u.fechaRegistro(),
                u.fechaCambioEstado(), u.administradorCambioEstadoId(), u.motivoCambioEstado());
    }
    private RecursoNoEncontradoException noExiste(Long id) { return new RecursoNoEncontradoException("No existe un usuario con ID: " + id); }
}
