package edu.dosw.proyecto.style_radar.validator;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.ConflictoException;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.SolicitudDonacionEntity;
import edu.dosw.proyecto.style_radar.repository.SolicitudDonacionRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DonacionValidator {

    private final SolicitudDonacionRepository solicitudRepository;
    private final Clock clock;

    public void validarPublicacionEnDonacion(PublicacionSegundaManoEntity publicacion) {
        if (publicacion.getEstado() != EstadoPublicacion.EN_DONACION) {
            throw new ReglaDeNegocioException("La prenda debe estar marcada para donación antes de enviar la solicitud");
        }
    }

    public void validarFundacionAceptaPrenda(FundacionEntity fundacion, PublicacionSegundaManoEntity publicacion) {
        if (!fundacion.getCategoriasAceptadas().contains(publicacion.getPrenda().getTipo())) {
            throw new ReglaDeNegocioException("La fundación no acepta prendas de tipo " + publicacion.getPrenda().getTipo());
        }
    }

    public void validarSinSolicitudEnCurso(Long publicacionId) {
        if (solicitudRepository.existsByPublicacion_IdAndEstadoIn(publicacionId, EstadoSolicitudDonacion.EN_CURSO)) {
            throw new ConflictoException("La prenda ya tiene una solicitud de donación en curso");
        }
    }

    public SolicitudDonacionEntity validarYObtenerDeFundacion(Long solicitudId, Long fundacionId) {
        return solicitudRepository.findByIdAndFundacion_Id(solicitudId, fundacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la solicitud de donación " + solicitudId + " para esta fundación"));
    }

    /** Donante o representante de la fundación destinataria; para cualquier otro la solicitud "no existe". */
    public SolicitudDonacionEntity validarYObtenerDeParticipante(Long solicitudId, Long usuarioId) {
        SolicitudDonacionEntity solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud de donación " + solicitudId));
        boolean esDonante = solicitud.getDonante().getId().equals(usuarioId);
        boolean esRepresentante = solicitud.getFundacion().getRepresentante().getId().equals(usuarioId);
        if (!esDonante && !esRepresentante) {
            throw new RecursoNoEncontradoException("No existe la solicitud de donación " + solicitudId);
        }
        return solicitud;
    }

    public void validarEstado(SolicitudDonacionEntity solicitud, EstadoSolicitudDonacion esperado, String accion) {
        if (solicitud.getEstado() != esperado) {
            throw new ReglaDeNegocioException("No se puede " + accion + " una solicitud en estado " + solicitud.getEstado());
        }
    }

    public void validarCoordinable(SolicitudDonacionEntity solicitud) {
        if (solicitud.getEstado() != EstadoSolicitudDonacion.ACEPTADA
                && solicitud.getEstado() != EstadoSolicitudDonacion.COORDINADA) {
            throw new ReglaDeNegocioException("Solo se coordina la recogida de una solicitud aceptada; estado actual: "
                    + solicitud.getEstado());
        }
    }

    public void validarFranja(Instant inicio, Instant fin) {
        if (!inicio.isAfter(clock.instant())) {
            throw new ReglaDeNegocioException("La franja de recogida debe iniciar en el futuro");
        }
        if (!fin.isAfter(inicio)) {
            throw new ReglaDeNegocioException("El fin de la franja debe ser posterior a su inicio");
        }
    }
}
