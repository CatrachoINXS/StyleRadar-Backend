package edu.dosw.proyecto.style_radar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.ModeracionCatalogoApi;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.impl.ModeracionCatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/admin/catalogo") @RequiredArgsConstructor
public class ModeracionCatalogoController implements ModeracionCatalogoApi {
    private final ModeracionCatalogoService service;

    @Override
    public ResponseEntity<PageResponseDTO<ModeracionItemResponseDTO>> consultar(
            @Valid @ModelAttribute ModeracionConsultaRequestDTO request) {
        var p = service.consultar(request.getEstado(), request.getPage(), request.getSize());
        return ResponseEntity.ok(new PageResponseDTO<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast()));
    }

    @Override
    public ResponseEntity<ModeracionItemResponseDTO> decidir(@PathVariable Long itemId,
            @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody DecisionModeracionRequestDTO request) {
        return ResponseEntity.ok(service.decidir(itemId, principal, request));
    }

    @Override
    public ResponseEntity<ModeracionItemResponseDTO> solicitarRevision(@PathVariable Long itemId,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(service.solicitarRevision(itemId, principal));
    }
}
