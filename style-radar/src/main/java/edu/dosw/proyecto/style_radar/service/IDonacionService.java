package edu.dosw.proyecto.style_radar.service;

import java.time.Instant;
import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.domain.SolicitudDonacion;

public interface IDonacionService {

    SolicitudDonacion enviarSolicitud(Long donanteId, Long publicacionId, Long fundacionId, String mensaje);

    List<SolicitudDonacion> consultarMisDonaciones(Long donanteId);

    List<SolicitudDonacion> consultarSolicitudesDeFundacion(Long representanteId, EstadoSolicitudDonacion estado);

    SolicitudDonacion decidir(Long representanteId, Long solicitudId, boolean aceptar, String motivo);

    SolicitudDonacion coordinarRecogida(Long usuarioId, Long solicitudId, String puntoEntrega, Instant inicio, Instant fin);

    SolicitudDonacion confirmarRecepcion(Long representanteId, Long solicitudId);
}
