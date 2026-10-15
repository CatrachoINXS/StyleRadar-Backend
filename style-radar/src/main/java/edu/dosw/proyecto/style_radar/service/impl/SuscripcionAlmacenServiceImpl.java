package edu.dosw.proyecto.style_radar.service.impl;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.SuscripcionAlmacenMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.SuscripcionAlmacenEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j @Transactional(readOnly = true)
public class SuscripcionAlmacenServiceImpl implements ISuscripcionAlmacenService {
    private final AlmacenRepository almacenes;
    private final PlanSuscripcionRepository planes;
    private final SuscripcionAlmacenRepository suscripciones;
    private final AutorizacionSuscripcionService autorizacion;
    private final SuscripcionAlmacenMapper mapper;
    private final Clock clock;

    @Override public List<PlanSuscripcion> planes() {
        return planes.findByActivoTrueOrderByIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED)
    public SuscripcionAlmacen crear(String nit, Long planId, UsuarioPrincipal actor) {
        autorizacion.autorizar(nit, actor, true);
        if (planId == null || planId < 1) throw new SolicitudSuscripcionInvalidaException("planId inválido");
        var almacen = almacenes.findForUpdate(nit).orElseThrow(this::noExiste);
        if (almacen.getEstadoVerificacion() != EstadoVerificacionAlmacen.APROBADO)
            throw new ReglaDeNegocioException("La suscripción requiere un almacén APROBADO por RF62");
        var planEntity = planes.findById(planId).orElseThrow(() -> new RecursoNoEncontradoException("Plan inexistente"));
        var plan = mapper.toDomain(planEntity);
        if (!plan.activo()) throw new ReglaDeNegocioException("El plan no está habilitado");
        // Leer Clock tras adquirir los locks: la espera no debe usar un instante obsoleto.
        var ahora = ahora();
        var fin = plan.finDesde(ahora);
        var anterior = suscripciones.findByAlmacenActivoNit(nit);
        if (anterior.isPresent()) {
            var s = anterior.get();
            if (s.getFinPeriodo().isAfter(ahora)) throw new SuscripcionActivaDuplicadaException();
            s.finalizar();
            // Liberar la clave única antes del INSERT; todo se revierte si falla la nueva activación.
            suscripciones.flush();
        }
        var nueva = new SuscripcionAlmacenEntity();
        nueva.setAlmacen(almacen); nueva.setPlan(planEntity); nueva.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        nueva.setAlmacenActivoNit(nit); nueva.setFechaCreacion(ahora); nueva.setInicioPeriodo(ahora); nueva.setFinPeriodo(fin);
        nueva.setRenovacionHabilitada(true);
        suscripciones.saveAndFlush(nueva);
        log.info("Suscripción nit={} usuarioId={} suscripcionId={} operación=crear resultado=aplicado-en-transaccion",
                nit, actor.getUsuarioId(), nueva.getId());
        return mapper.toDomain(nueva);
    }

    @Override public SuscripcionAlmacen vigente(String nit, UsuarioPrincipal actor) {
        autorizacion.autorizar(nit, actor, false);
        return mapper.toDomain(suscripciones.findVigente(nit, ahora()).orElseThrow(this::noExiste));
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED)
    public SuscripcionAlmacen cancelarRenovacion(String nit, Long id, Boolean renovacion, UsuarioPrincipal actor) {
        autorizacion.autorizar(nit, actor, true);
        if (!Boolean.FALSE.equals(renovacion) || id == null || id < 1)
            throw new SolicitudSuscripcionInvalidaException("Solo se permite cancelar una renovación existente");
        almacenes.findForUpdate(nit).orElseThrow(this::noExiste);
        var s = suscripciones.findByIdAndAlmacen_Nit(id, nit).orElseThrow(this::noExiste);
        var ahora = ahora();
        if (!mapper.toDomain(s).vigente(ahora)) throw noExiste();
        boolean repetida = !s.isRenovacionHabilitada();
        s.cancelarRenovacion(ahora);
        suscripciones.flush();
        log.info("Suscripción nit={} usuarioId={} suscripcionId={} operación=cancelar-renovacion resultado={}",
                nit, actor.getUsuarioId(), id, repetida ? "idempotente" : "aplicado-en-transaccion");
        return mapper.toDomain(s);
    }

    @Override public Page<SuscripcionAlmacen> historial(String nit, int page, int size, UsuarioPrincipal actor) {
        autorizacion.autorizar(nit, actor, false);
        if (page < 0 || size < 1 || size > 100) throw new SolicitudSuscripcionInvalidaException("Paginación inválida");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id")));
        if (pageable.getOffset() > Integer.MAX_VALUE)
            return new PageImpl<>(List.of(), pageable, suscripciones.countByAlmacen_Nit(nit));
        return suscripciones.findByAlmacen_Nit(nit, pageable).map(mapper::toDomain);
    }

    private Instant ahora() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private RecursoNoEncontradoException noExiste() { return new RecursoNoEncontradoException("Suscripción o almacén no disponible"); }
}
