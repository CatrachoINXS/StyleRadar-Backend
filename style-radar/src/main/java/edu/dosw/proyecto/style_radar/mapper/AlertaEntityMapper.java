package edu.dosw.proyecto.style_radar.mapper;

import org.springframework.stereotype.Component;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;

@Component
public class AlertaEntityMapper {
    public Alerta toDomain(AlertaEntity a) {
        return new Alerta(a.getId(), a.getUsuario().getId(), a.getTipoObjetivo(), a.getIdentificadorObjetivo(),
                a.isActiva(), a.getFechaCreacion(), a.getFechaDesactivacion());
    }

    public Notificacion toDomain(NotificacionEntity n) {
        return new Notificacion(n.getId(), n.getTipoEvento(), n.getAlerta().getId(),
                n.getItemCatalogoId(), n.getMensaje(), n.getFechaCreacion());
    }
}
