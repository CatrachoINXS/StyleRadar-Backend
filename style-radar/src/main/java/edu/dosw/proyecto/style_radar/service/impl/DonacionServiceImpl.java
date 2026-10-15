package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.mapper.SolicitudDonacionEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.domain.SolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.SolicitudDonacionEntity;
import edu.dosw.proyecto.style_radar.repository.SolicitudDonacionRepository;
import edu.dosw.proyecto.style_radar.service.IDonacionService;
import edu.dosw.proyecto.style_radar.validator.DonacionValidator;
import edu.dosw.proyecto.style_radar.validator.FundacionValidator;
import edu.dosw.proyecto.style_radar.validator.SegundaManoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonacionServiceImpl implements IDonacionService {

    private final SolicitudDonacionRepository solicitudRepository;
    private final SolicitudDonacionEntityMapper solicitudEntityMapper;
    private final DonacionValidator donacionValidator;
    private final FundacionValidator fundacionValidator;
    private final SegundaManoValidator segundaManoValidator;
    private final Clock clock;

    @Override
    @Transactional
    public SolicitudDonacion enviarSolicitud(Long donanteId, Long publicacionId, Long fundacionId, String mensaje) {
        log.info("Usuario ID: {} envía solicitud de donación de la publicación ID: {} a la fundación ID: {}",
                donanteId, publicacionId, fundacionId);
        PublicacionSegundaManoEntity publicacion = segundaManoValidator.validarYObtenerDeUsuario(publicacionId, donanteId);
        donacionValidator.validarPublicacionEnDonacion(publicacion);
        FundacionEntity fundacion = fundacionValidator.validarYObtener(fundacionId);
        fundacionValidator.validarActiva(fundacion);
        donacionValidator.validarFundacionAceptaPrenda(fundacion, publicacion);
        donacionValidator.validarSinSolicitudEnCurso(publicacionId);

        SolicitudDonacionEntity solicitud = SolicitudDonacionEntity.builder()
                .publicacion(publicacion)
                .donante(publicacion.getUsuario())
                .fundacion(fundacion)
                .mensaje(mensaje)
                .estado(EstadoSolicitudDonacion.PENDIENTE)
                .fechaSolicitud(clock.instant())
                .build();
        SolicitudDonacionEntity guardada = solicitudRepository.save(solicitud);
        log.info("Solicitud de donación ID: {} creada en estado PENDIENTE", guardada.getId());
        return solicitudEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudDonacion> consultarMisDonaciones(Long donanteId) {
        log.info("Consultando solicitudes de donación del usuario ID: {}", donanteId);
        List<SolicitudDonacion> solicitudes = solicitudRepository.findByDonante_IdOrderByFechaSolicitudDescIdDesc(donanteId)
                .stream()
                .map(solicitudEntityMapper::toDomain)
                .toList();
        log.info("Solicitudes de donación del usuario ID {}: {}", donanteId, solicitudes.size());
        return solicitudes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudDonacion> consultarSolicitudesDeFundacion(Long representanteId, EstadoSolicitudDonacion estado) {
        FundacionEntity fundacion = fundacionValidator.validarYObtenerDeRepresentante(representanteId);
        log.info("Consultando solicitudes de la fundación ID: {} con estado: {}", fundacion.getId(), estado);
        List<SolicitudDonacionEntity> entidades = estado == null
                ? solicitudRepository.findByFundacion_IdOrderByFechaSolicitudDescIdDesc(fundacion.getId())
                : solicitudRepository.findByFundacion_IdAndEstadoOrderByFechaSolicitudDescIdDesc(fundacion.getId(), estado);
        List<SolicitudDonacion> solicitudes = entidades.stream().map(solicitudEntityMapper::toDomain).toList();
        log.info("Solicitudes encontradas para la fundación ID {}: {}", fundacion.getId(), solicitudes.size());
        return solicitudes;
    }

    @Override
    @Transactional
    public SolicitudDonacion decidir(Long representanteId, Long solicitudId, boolean aceptar, String motivo) {
        FundacionEntity fundacion = fundacionValidator.validarYObtenerDeRepresentante(representanteId);
        log.info("Fundación ID: {} decide solicitud ID: {}. Aceptar: {}", fundacion.getId(), solicitudId, aceptar);
        SolicitudDonacionEntity solicitud = donacionValidator.validarYObtenerDeFundacion(solicitudId, fundacion.getId());
        donacionValidator.validarEstado(solicitud, EstadoSolicitudDonacion.PENDIENTE, "decidir");

        solicitud.setEstado(aceptar ? EstadoSolicitudDonacion.ACEPTADA : EstadoSolicitudDonacion.RECHAZADA);
        solicitud.setMotivoRechazo(aceptar ? null : motivo.trim());
        solicitud.setFechaDecision(clock.instant());
        // Al rechazar, la prenda sigue EN_DONACION y el donante puede elegir otra fundación.
        SolicitudDonacionEntity guardada = solicitudRepository.save(solicitud);
        log.info("Solicitud ID: {} quedó en estado {}", solicitudId, guardada.getEstado());
        return solicitudEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public SolicitudDonacion coordinarRecogida(
            Long usuarioId, Long solicitudId, String puntoEntrega, Instant inicio, Instant fin) {
        log.info("Usuario ID: {} coordina la recogida de la solicitud ID: {}", usuarioId, solicitudId);
        SolicitudDonacionEntity solicitud = donacionValidator.validarYObtenerDeParticipante(solicitudId, usuarioId);
        donacionValidator.validarCoordinable(solicitud);
        donacionValidator.validarFranja(inicio, fin);

        solicitud.setPuntoEntrega(puntoEntrega.trim());
        solicitud.setFranjaInicio(inicio);
        solicitud.setFranjaFin(fin);
        solicitud.setEstado(EstadoSolicitudDonacion.COORDINADA);
        SolicitudDonacionEntity guardada = solicitudRepository.save(solicitud);
        log.info("Recogida de la solicitud ID: {} coordinada", solicitudId);
        return solicitudEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public SolicitudDonacion confirmarRecepcion(Long representanteId, Long solicitudId) {
        FundacionEntity fundacion = fundacionValidator.validarYObtenerDeRepresentante(representanteId);
        log.info("Fundación ID: {} confirma recepción de la solicitud ID: {}", fundacion.getId(), solicitudId);
        SolicitudDonacionEntity solicitud = donacionValidator.validarYObtenerDeFundacion(solicitudId, fundacion.getId());
        donacionValidator.validarEstado(solicitud, EstadoSolicitudDonacion.COORDINADA, "confirmar la recepción de");

        solicitud.setEstado(EstadoSolicitudDonacion.RECIBIDA);
        solicitud.setFechaRecepcion(clock.instant());
        solicitud.getPublicacion().setEstado(EstadoPublicacion.DONADA);
        SolicitudDonacionEntity guardada = solicitudRepository.save(solicitud);
        log.info("Donación de la solicitud ID: {} recibida; publicación ID: {} marcada DONADA",
                solicitudId, solicitud.getPublicacion().getId());
        return solicitudEntityMapper.toDomain(guardada);
    }
}
