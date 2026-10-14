package edu.dosw.proyecto.style_radar.service;

import java.util.List;
import org.springframework.data.domain.Page;
import edu.dosw.proyecto.style_radar.model.domain.*;

public interface IAlertaService {
    Alerta crear(Long usuarioId, Long busquedaGuardadaId, Long itemCatalogoId);
    List<Alerta> consultar(Long usuarioId);
    Alerta desactivar(Long usuarioId, Long alertaId, Boolean activa);
    Page<Notificacion> notificaciones(Long usuarioId, int page, int size);
}
