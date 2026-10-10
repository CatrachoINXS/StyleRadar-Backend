package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;

public record Alerta(Long id, Long usuarioId, TipoObjetivoAlerta tipoObjetivo, Long identificadorObjetivo,
        boolean activa, Instant fechaCreacion, Instant fechaDesactivacion) { }
