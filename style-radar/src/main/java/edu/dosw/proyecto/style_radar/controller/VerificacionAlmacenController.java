package edu.dosw.proyecto.style_radar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.VerificacionAlmacenApi;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.impl.VerificacionAlmacenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/admin/almacenes") @RequiredArgsConstructor
public class VerificacionAlmacenController implements VerificacionAlmacenApi {
    private final VerificacionAlmacenService service;

    @InitBinder
    void validarEstado(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(edu.dosw.proyecto.style_radar.model.domain.EstadoVerificacionAlmacen.class,
                new java.beans.PropertyEditorSupport() {
                    @Override public void setAsText(String text) {
                        setValue(edu.dosw.proyecto.style_radar.model.domain.EstadoVerificacionAlmacen.valueOf(text));
                    }
                });
    }

    @Override
    public ResponseEntity<PageResponseDTO<VerificacionAlmacenResponseDTO>> consultar(
            @Valid @ModelAttribute VerificacionConsultaRequestDTO request, @AuthenticationPrincipal UsuarioPrincipal principal) {
        var p = service.consultar(request.getEstado(), request.getPage(), request.getSize(), principal);
        return ResponseEntity.ok(new PageResponseDTO<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast()));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> consultarUno(@PathVariable String nit,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(service.consultarUno(nit, principal));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> decidir(@PathVariable String nit,
            @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody DecisionVerificacionRequestDTO request) {
        return ResponseEntity.ok(service.decidir(nit, principal, request));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> solicitarRevision(@PathVariable String nit,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(service.solicitarRevision(nit, principal));
    }
}
