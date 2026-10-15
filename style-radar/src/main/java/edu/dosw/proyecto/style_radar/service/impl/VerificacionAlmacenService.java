package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class VerificacionAlmacenService {
    private final AlmacenRepository almacenes;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Page<VerificacionAlmacenResponseDTO> consultar(EstadoVerificacionAlmacen estado, int page, int size,
            UsuarioPrincipal principal) {
        administrador(principal);
        if (page < 0 || size < 1 || size > 100) {
            throw new ReglaDeNegocioException("Paginación de verificación inválida");
        }
        var pageable = PageRequest.of(page, size);
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            return new PageImpl<>(List.of(), pageable, almacenes.countVerificacion(estado));
        }
        var ids = almacenes.findIdsVerificacion(estado, pageable);
        var entidades = ids.isEmpty() ? List.<AlmacenEntity>of() : almacenes.findByNitIn(ids.getContent());
        var porNit = entidades.stream().collect(Collectors.toMap(AlmacenEntity::getNit, Function.identity()));
        return ids.map(nit -> respuesta(porNit.get(nit)));
    }

    @Transactional(readOnly = true)
    public VerificacionAlmacenResponseDTO consultarUno(String nit, UsuarioPrincipal principal) {
        administrador(principal);
        return respuesta(almacenes.findRevision(nit).orElseThrow(() -> noExiste(nit)));
    }

    @Transactional
    public VerificacionAlmacenResponseDTO solicitarRevision(String nit, UsuarioPrincipal principal) {
        administrador(principal);
        var almacen = almacen(nit);
        var anterior = almacen.getEstadoVerificacion();
        if (anterior == EstadoVerificacionAlmacen.PENDIENTE) {
            throw new ReglaDeNegocioException("El almacén ya está pendiente de revisión");
        }
        almacen.setEstadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE);
        // Conserva la última decisión mientras la nueva evaluación está pendiente.
        almacenes.flush();
        log.info("Verificación nit={} administradorId={} anterior={} nuevo={} operación=solicitar-revision",
                nit, principal.getUsuarioId(), anterior, almacen.getEstadoVerificacion());
        return respuesta(almacen);
    }

    @Transactional
    public VerificacionAlmacenResponseDTO decidir(String nit, UsuarioPrincipal principal, DecisionVerificacionRequestDTO request) {
        administrador(principal);
        if (request == null || request.getDecision() == null || !request.isMotivoValido()
                || (request.getMotivo() != null && request.getMotivo().length() > 1000)) {
            throw new ReglaDeNegocioException("Decisión o motivo de verificación inválidos");
        }
        var almacen = almacen(nit);
        if (almacen.getEstadoVerificacion() != EstadoVerificacionAlmacen.PENDIENTE) {
            throw new ReglaDeNegocioException("La decisión exige un almacén pendiente de revisión");
        }
        almacen.setEstadoVerificacion(EstadoVerificacionAlmacen.valueOf(request.getDecision().name()));
        almacen.setAdministradorDecisionId(principal.getUsuarioId());
        almacen.setFechaDecisionVerificacion(clock.instant().truncatedTo(ChronoUnit.MICROS));
        almacen.setMotivoVerificacion(request.getMotivo() == null || request.getMotivo().isBlank()
                ? null : request.getMotivo().strip());
        almacenes.flush();
        log.info("Verificación nit={} administradorId={} anterior=PENDIENTE nuevo={} operación=decidir",
                nit, principal.getUsuarioId(), almacen.getEstadoVerificacion());
        return respuesta(almacen);
    }

    private AlmacenEntity almacen(String nit) {
        return almacenes.findForUpdate(nit).orElseThrow(() -> noExiste(nit));
    }

    private RecursoNoEncontradoException noExiste(String nit) {
        return new RecursoNoEncontradoException("No existe un almacén con NIT " + nit);
    }

    private void administrador(UsuarioPrincipal principal) {
        if (principal == null || !principal.getRoles().contains(Rol.ADMIN_STYLERADAR)) {
            throw new AccessDeniedException("Se requiere ROLE_ADMIN_STYLERADAR");
        }
    }

    private VerificacionAlmacenResponseDTO respuesta(AlmacenEntity a) {
        return new VerificacionAlmacenResponseDTO(a.getNit(), a.getNombreComercial(), a.getDescripcion(),
                a.getTelefono(), a.getCorreoContacto(), a.getLatitud(), a.getLongitud(), a.getReputacion(),
                Set.copyOf(a.getCategorias()), a.getEstadoVerificacion(), a.getEstadoVerificacion() == null,
                a.getFechaDecisionVerificacion(), a.getAdministradorDecisionId(), a.getMotivoVerificacion());
    }
}
