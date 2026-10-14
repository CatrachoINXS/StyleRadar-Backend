package edu.dosw.proyecto.style_radar.model.dto.response;

import edu.dosw.proyecto.style_radar.model.domain.UnidadPeriodoSuscripcion;

public record PlanSuscripcionResponseDTO(Long id, String nombre, Integer periodo,
        UnidadPeriodoSuscripcion unidadDuracion) { }
