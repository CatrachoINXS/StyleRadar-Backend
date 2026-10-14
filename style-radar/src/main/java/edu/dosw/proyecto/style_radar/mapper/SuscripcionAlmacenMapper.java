package edu.dosw.proyecto.style_radar.mapper;

import java.time.Instant;
import org.springframework.stereotype.Component;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;

@Component
public class SuscripcionAlmacenMapper {
    public PlanSuscripcion toDomain(PlanSuscripcionEntity p) {
        return new PlanSuscripcion(p.getId(), p.getNombre(), p.getPeriodo(), p.getUnidadDuracion(), p.isActivo());
    }
    public SuscripcionAlmacen toDomain(SuscripcionAlmacenEntity s) {
        return new SuscripcionAlmacen(s.getId(), s.getAlmacen().getNit(), s.getPlan().getId(), s.getPlan().getNombre(),
                s.getEstadoSuscripcion(), s.getFechaCreacion(), s.getInicioPeriodo(), s.getFinPeriodo(),
                s.isRenovacionHabilitada(), s.getFechaCancelacionRenovacion());
    }
    public PlanSuscripcionResponseDTO toResponse(PlanSuscripcion p) {
        return new PlanSuscripcionResponseDTO(p.id(), p.nombre(), p.periodo(), p.unidadDuracion());
    }
    public SuscripcionAlmacenResponseDTO toResponse(SuscripcionAlmacen s, Instant ahora) {
        return new SuscripcionAlmacenResponseDTO(s.id(), s.almacenNit(), s.planId(), s.nombrePlan(),
                s.estadoEfectivo(ahora), s.fechaCreacion(), s.inicioPeriodo(), s.finPeriodo(),
                s.renovacionHabilitada(), s.fechaCancelacionRenovacion());
    }
    public AdministradorAlmacenResponseDTO toResponse(AdministradorAlmacenEntity a) {
        return new AdministradorAlmacenResponseDTO(a.getId(), a.getUsuario().getId(), a.getAlmacen().getNit(),
                a.getAsignadoPor().getId(), a.getFechaAsignacion());
    }
}
