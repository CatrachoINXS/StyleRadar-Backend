package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import edu.dosw.proyecto.style_radar.model.domain.TipoEventoNotificacion;

public record NotificacionResponseDTO(Long id, TipoEventoNotificacion tipoEvento, Long alertaId,
        Long itemCatalogoId, String mensaje, Instant fechaCreacion) { }
