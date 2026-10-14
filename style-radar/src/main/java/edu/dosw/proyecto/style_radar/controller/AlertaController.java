package edu.dosw.proyecto.style_radar.controller;

import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.AlertaApi;
import edu.dosw.proyecto.style_radar.mapper.AlertaMapper;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.IAlertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/usuarios/me") @RequiredArgsConstructor
public class AlertaController implements AlertaApi {
    private final IAlertaService service;
    private final AlertaMapper mapper;

    @Override
    public ResponseEntity<AlertaResponseDTO> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearAlertaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(service.crear(principal.getUsuarioId(),
                request.getBusquedaGuardadaId(), request.getItemCatalogoId())));
    }

    @Override
    public ResponseEntity<List<AlertaResponseDTO>> consultar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(service.consultar(principal.getUsuarioId()).stream().map(mapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<AlertaResponseDTO> desactivar(@AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody DesactivarAlertaRequestDTO request) {
        return ResponseEntity.ok(mapper.toResponse(service.desactivar(principal.getUsuarioId(), id, request.getActiva())));
    }

    @Override
    public ResponseEntity<PageResponseDTO<NotificacionResponseDTO>> notificaciones(
            @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @ModelAttribute NotificacionesRequestDTO request) {
        return ResponseEntity.ok(mapper.toResponse(service.notificaciones(principal.getUsuarioId(), request.getPage(), request.getSize())));
    }
}
