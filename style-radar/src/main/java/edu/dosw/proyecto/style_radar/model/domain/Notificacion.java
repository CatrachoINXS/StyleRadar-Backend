package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;

public record Notificacion(Long id, TipoEventoNotificacion tipoEvento, Long alertaId,
        Long itemCatalogoId, String mensaje, Instant fechaCreacion) { }
