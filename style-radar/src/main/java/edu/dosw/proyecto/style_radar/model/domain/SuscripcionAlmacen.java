package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;

public record SuscripcionAlmacen(Long id, String almacenNit, Long planId, String nombrePlan,
        EstadoSuscripcion estadoSuscripcion, Instant fechaCreacion, Instant inicioPeriodo,
        Instant finPeriodo, boolean renovacionHabilitada, Instant fechaCancelacionRenovacion) {
    public boolean vigente(Instant ahora) {
        return estadoSuscripcion == EstadoSuscripcion.ACTIVA
                && !ahora.isBefore(inicioPeriodo) && ahora.isBefore(finPeriodo);
    }

    public EstadoSuscripcion estadoEfectivo(Instant ahora) {
        return vigente(ahora) ? EstadoSuscripcion.ACTIVA : EstadoSuscripcion.FINALIZADA;
    }
}
