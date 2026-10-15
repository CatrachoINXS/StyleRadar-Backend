package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.PrendaEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.PrendaMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionModeracionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.ModeracionItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.VisibilidadModeracion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class ModeracionCatalogoService {
    private final ItemCatalogoRepository items;
    private final PrendaEntityMapper prendas;
    private final PrendaMapper respuestas;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Page<ModeracionItemResponseDTO> consultar(EstadoModeracion estado, int page, int size) {
        if (estado == null || page < 0 || size < 1 || size > 100) {
            throw new ReglaDeNegocioException("Filtro o paginación de moderación inválidos");
        }
        var pageable = PageRequest.of(page, size);
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            long total = items.count((root, query, cb) -> estado == EstadoModeracion.NO_REQUERIDA
                    ? cb.or(cb.isNull(root.get("estadoModeracion")), cb.equal(root.get("estadoModeracion"), estado))
                    : cb.equal(root.get("estadoModeracion"), estado));
            return new PageImpl<>(List.of(), pageable, total);
        }
        var ids = items.findIdsModeracion(estado, pageable);
        var entidades = ids.isEmpty() ? List.<ItemCatalogoEntity>of() : items.findRevisionByIdIn(ids.getContent());
        var porId = entidades.stream().collect(Collectors.toMap(ItemCatalogoEntity::getId, Function.identity()));
        return ids.map(id -> respuesta(porId.get(id)));
    }

    @Transactional
    public ModeracionItemResponseDTO solicitarRevision(Long itemId, UsuarioPrincipal principal) {
        administrador(principal);
        var item = item(itemId);
        if (item.getEstadoModeracion() == EstadoModeracion.PENDIENTE) {
            throw new ReglaDeNegocioException("La publicación ya está pendiente de revisión");
        }
        item.setEstadoModeracion(EstadoModeracion.PENDIENTE);
        items.flush();
        log.info("Revisión manual solicitada itemId={} administradorId={}", itemId, principal.getUsuarioId());
        return respuesta(item);
    }

    @Transactional
    public ModeracionItemResponseDTO decidir(Long itemId, UsuarioPrincipal principal, DecisionModeracionRequestDTO request) {
        administrador(principal);
        if (request == null || request.getDecision() == null || !request.isMotivoValido()
                || (request.getMotivo() != null && request.getMotivo().length() > 1000)) {
            throw new ReglaDeNegocioException("Decisión o motivo de moderación inválidos");
        }
        var item = item(itemId);
        if (item.getEstadoModeracion() != EstadoModeracion.PENDIENTE) {
            throw new ReglaDeNegocioException("La decisión exige una publicación pendiente de revisión");
        }
        item.setEstadoModeracion(EstadoModeracion.valueOf(request.getDecision().name()));
        item.setAdministradorDecisionId(principal.getUsuarioId());
        item.setFechaDecisionModeracion(clock.instant().truncatedTo(ChronoUnit.MICROS));
        item.setMotivoModeracion(request.getMotivo() == null || request.getMotivo().isBlank()
                ? null : request.getMotivo().strip());
        // Flush dentro de la transacción: una falla revierte todos los metadatos.
        items.flush();
        log.info("Decisión de moderación itemId={} administradorId={} estado={}",
                itemId, principal.getUsuarioId(), item.getEstadoModeracion());
        return respuesta(item);
    }

    private ItemCatalogoEntity item(Long id) {
        return items.findForUpdate(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la publicación " + id));
    }

    private void administrador(UsuarioPrincipal principal) {
        if (principal == null || !principal.getRoles().contains(Rol.ADMIN_STYLERADAR)) {
            throw new AccessDeniedException("Se requiere ROLE_ADMIN_STYLERADAR");
        }
    }

    private ModeracionItemResponseDTO respuesta(ItemCatalogoEntity item) {
        return new ModeracionItemResponseDTO(item.getId(), item.getAlmacen().getNit(),
                respuestas.toResponse(prendas.toDomain(item.getPrenda())), item.getPrecio(),
                VisibilidadModeracion.efectiva(item.getEstadoModeracion()), item.getFechaPublicacion(),
                item.getFechaDecisionModeracion(), item.getAdministradorDecisionId(), item.getMotivoModeracion());
    }
}
