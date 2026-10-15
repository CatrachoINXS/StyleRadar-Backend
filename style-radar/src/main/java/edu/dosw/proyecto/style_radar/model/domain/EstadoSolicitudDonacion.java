package edu.dosw.proyecto.style_radar.model.domain;

import java.util.Set;

public enum EstadoSolicitudDonacion {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    COORDINADA,
    RECIBIDA;

    /** Estados que bloquean otra solicitud sobre la misma prenda. */
    public static final Set<EstadoSolicitudDonacion> EN_CURSO = Set.of(PENDIENTE, ACEPTADA, COORDINADA);
}
