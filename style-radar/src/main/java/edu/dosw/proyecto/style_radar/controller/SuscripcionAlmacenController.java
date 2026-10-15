package edu.dosw.proyecto.style_radar.controller;

import java.time.Clock;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.SuscripcionAlmacenApi;
import edu.dosw.proyecto.style_radar.mapper.SuscripcionAlmacenMapper;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.ISuscripcionAlmacenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/almacenes/{nit}/suscripciones") @RequiredArgsConstructor
public class SuscripcionAlmacenController implements SuscripcionAlmacenApi {
    private final ISuscripcionAlmacenService service;
    private final SuscripcionAlmacenMapper mapper;
    private final Clock clock;

    @Override public ResponseEntity<SuscripcionAlmacenResponseDTO> crear(@PathVariable String nit,
            @AuthenticationPrincipal UsuarioPrincipal actor, @Valid @RequestBody CrearSuscripcionRequestDTO request) {
        var s = service.crear(nit, request.getPlanId(), actor);
        var location = org.springframework.web.util.UriComponentsBuilder.fromPath("/api/v1/almacenes")
                .pathSegment(nit, "suscripciones", "vigente").build().encode().toUri();
        return ResponseEntity.created(location)
                .body(mapper.toResponse(s, clock.instant()));
    }
    @Override public ResponseEntity<SuscripcionAlmacenResponseDTO> vigente(@PathVariable String nit,
            @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(mapper.toResponse(service.vigente(nit, actor), clock.instant()));
    }
    @Override public ResponseEntity<SuscripcionAlmacenResponseDTO> cancelar(@PathVariable String nit, @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal actor, @Valid @RequestBody CancelarRenovacionRequestDTO request) {
        return ResponseEntity.ok(mapper.toResponse(service.cancelarRenovacion(nit, id, request.getRenovacionHabilitada(), actor),
                clock.instant()));
    }
    @Override public ResponseEntity<PageResponseDTO<SuscripcionAlmacenResponseDTO>> historial(@PathVariable String nit,
            @Valid @ModelAttribute SuscripcionesConsultaRequestDTO request, @AuthenticationPrincipal UsuarioPrincipal actor) {
        var p = service.historial(nit, request.getPage(), request.getSize(), actor);
        var ahora = clock.instant();
        var respuestas = p.map(s -> mapper.toResponse(s, ahora));
        return ResponseEntity.ok(new PageResponseDTO<>(respuestas.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast()));
    }
}
