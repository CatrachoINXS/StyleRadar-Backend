package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.*;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.AlertaEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.IAlertaService;
import edu.dosw.proyecto.style_radar.validator.AlertaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class AlertaServiceImpl implements IAlertaService {
    private final AlertaRepository alertas;
    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;
    private final BusquedaGuardadaRepository busquedas;
    private final ItemCatalogoRepository items;
    private final ItemCatalogoEntityMapper itemsMapper;
    private final AlertaEntityMapper mapper;
    private final AlertaValidator validator;
    private final Clock clock;

    @Override @Transactional
    public Alerta crear(Long usuarioId, Long busquedaId, Long itemId) {
        validator.objetivo(busquedaId, itemId);
        var tipo = busquedaId != null ? TipoObjetivoAlerta.BUSQUEDA_GUARDADA : TipoObjetivoAlerta.ITEM_CATALOGO;
        Long objetivoId = busquedaId != null ? busquedaId : itemId;
        // El objetivo bloqueado serializa creación contra eliminación y stock.
        if (busquedaId != null) {
            validator.busqueda(busquedas.findByIdAndUsuario_Id(busquedaId, usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Busqueda no accesible")));
        } else {
            var item = items.findForUpdate(itemId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Item no disponible para seguimiento"));
            validator.agotado(itemsMapper.toDomain(item).getStock());
        }
        if (alertas.existsByUsuario_IdAndTipoObjetivoAndIdentificadorObjetivoAndActivaTrue(usuarioId, tipo, objetivoId)) {
            log.warn("Alerta activa duplicada usuarioId={} tipo={} objetivoId={}", usuarioId, tipo, objetivoId);
            throw new DataIntegrityViolationException("Ya existe una alerta activa para este objetivo");
        }
        var alerta = new AlertaEntity();
        alerta.setUsuario(usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no accesible")));
        alerta.setTipoObjetivo(tipo);
        alerta.setIdentificadorObjetivo(objetivoId);
        alerta.setFechaCreacion(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        var resultado = mapper.toDomain(alertas.saveAndFlush(alerta));
        log.info("Alerta creada alertaId={} usuarioId={} tipo={}", resultado.id(), usuarioId, tipo);
        return resultado;
    }

    @Override @Transactional(readOnly = true)
    public List<Alerta> consultar(Long usuarioId) {
        return alertas.findByUsuario_IdOrderByFechaCreacionDescIdDesc(usuarioId).stream().map(mapper::toDomain).toList();
    }

    @Override @Transactional
    public Alerta desactivar(Long usuarioId, Long alertaId, Boolean activa) {
        validator.desactivacion(activa);
        var alerta = alertas.findForUpdate(alertaId)
                .filter(a -> a.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta no accesible"));
        if (alerta.isActiva()) {
            alerta.desactivar(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            log.info("Alerta desactivada alertaId={} usuarioId={}", alertaId, usuarioId);
        }
        return mapper.toDomain(alerta);
    }

    @Override @Transactional(readOnly = true)
    public Page<Notificacion> notificaciones(Long usuarioId, int page, int size) {
        validator.paginacion(page, size);
        var pageable = PageRequest.of(page, size);
        long total = notificaciones.countByUsuario_Id(usuarioId);
        if (pageable.getOffset() >= total) return new PageImpl<>(List.of(), pageable, total);
        return notificaciones.findByUsuario_IdOrderByFechaCreacionDescIdDesc(usuarioId, pageable).map(mapper::toDomain);
    }
}
