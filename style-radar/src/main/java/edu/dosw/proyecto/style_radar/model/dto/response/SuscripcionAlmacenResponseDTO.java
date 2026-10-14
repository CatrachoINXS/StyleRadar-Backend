package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import edu.dosw.proyecto.style_radar.model.domain.EstadoSuscripcion;

public record SuscripcionAlmacenResponseDTO(Long id, String almacenNit, Long planId, String nombrePlan,
        EstadoSuscripcion estadoSuscripcion, Instant fechaCreacion, Instant inicioPeriodo,
        Instant finPeriodo, boolean renovacionHabilitada, Instant fechaCancelacionRenovacion) { }
