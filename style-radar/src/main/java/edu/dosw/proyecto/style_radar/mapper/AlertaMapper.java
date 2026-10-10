package edu.dosw.proyecto.style_radar.mapper;

import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;

@Component
public class AlertaMapper {
    public AlertaResponseDTO toResponse(Alerta a) {
        return new AlertaResponseDTO(a.id(), a.tipoObjetivo(), a.identificadorObjetivo(),
                a.activa(), a.fechaCreacion(), a.fechaDesactivacion());
    }

    public PageResponseDTO<NotificacionResponseDTO> toResponse(Page<Notificacion> pagina) {
        var content = pagina.getContent().stream().map(n -> new NotificacionResponseDTO(n.id(), n.tipoEvento(),
                n.alertaId(), n.itemCatalogoId(), n.mensaje(), n.fechaCreacion())).toList();
        return new PageResponseDTO<>(content, pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages(), pagina.isFirst(), pagina.isLast());
    }
}
