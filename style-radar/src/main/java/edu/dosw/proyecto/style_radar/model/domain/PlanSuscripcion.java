package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.DateTimeException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;

/** Configuración confiable; no representa un pago ni condiciones comerciales aprobadas. */
public record PlanSuscripcion(Long id, String nombre, Integer periodo,
        UnidadPeriodoSuscripcion unidadDuracion, boolean activo) {
    public Instant finDesde(Instant inicio) {
        if (periodo == null || periodo < 1 || unidadDuracion != UnidadPeriodoSuscripcion.MESES_CALENDARIO)
            throw new ReglaDeNegocioException("El plan tiene una duración inválida");
        try {
            return inicio.atZone(ZoneOffset.UTC).plusMonths(periodo).toInstant();
        } catch (DateTimeException | ArithmeticException e) {
            throw new ReglaDeNegocioException("El período del plan excede el rango temporal soportado");
        }
    }
}
