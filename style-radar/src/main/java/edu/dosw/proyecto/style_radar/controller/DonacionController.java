package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.DonacionApi;
import edu.dosw.proyecto.style_radar.mapper.SolicitudDonacionMapper;
import edu.dosw.proyecto.style_radar.model.domain.SolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.dto.request.ConsultaSolicitudesDonacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CoordinarRecogidaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearSolicitudDonacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionDonacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.SolicitudDonacionResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.IDonacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DonacionController implements DonacionApi {

    private final IDonacionService donacionService;
    private final SolicitudDonacionMapper solicitudMapper;

    @Override
    public ResponseEntity<SolicitudDonacionResponseDTO> enviarSolicitud(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearSolicitudDonacionRequestDTO request) {
        SolicitudDonacion solicitud = donacionService.enviarSolicitud(
                principal.getUsuarioId(), request.getPublicacionId(), request.getFundacionId(), request.getMensaje());
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudMapper.toResponse(solicitud));
    }

    @Override
    public ResponseEntity<List<SolicitudDonacionResponseDTO>> consultarMisDonaciones(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(toResponse(donacionService.consultarMisDonaciones(principal.getUsuarioId())));
    }

    @Override
    public ResponseEntity<List<SolicitudDonacionResponseDTO>> consultarSolicitudesDeFundacion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @ModelAttribute ConsultaSolicitudesDonacionRequestDTO request) {
        return ResponseEntity.ok(toResponse(
                donacionService.consultarSolicitudesDeFundacion(principal.getUsuarioId(), request.getEstado())));
    }

    @Override
    public ResponseEntity<SolicitudDonacionResponseDTO> decidir(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DecisionDonacionRequestDTO request) {
        SolicitudDonacion decidida = donacionService.decidir(
                principal.getUsuarioId(), id, request.getAceptar(), request.getMotivo());
        return ResponseEntity.ok(solicitudMapper.toResponse(decidida));
    }

    @Override
    public ResponseEntity<SolicitudDonacionResponseDTO> coordinarRecogida(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CoordinarRecogidaRequestDTO request) {
        SolicitudDonacion coordinada = donacionService.coordinarRecogida(principal.getUsuarioId(), id,
                request.getPuntoEntrega(), request.getFranjaInicio(), request.getFranjaFin());
        return ResponseEntity.ok(solicitudMapper.toResponse(coordinada));
    }

    @Override
    public ResponseEntity<SolicitudDonacionResponseDTO> confirmarRecepcion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(solicitudMapper.toResponse(donacionService.confirmarRecepcion(principal.getUsuarioId(), id)));
    }

    private List<SolicitudDonacionResponseDTO> toResponse(List<SolicitudDonacion> solicitudes) {
        return solicitudes.stream().map(solicitudMapper::toResponse).toList();
    }
}
