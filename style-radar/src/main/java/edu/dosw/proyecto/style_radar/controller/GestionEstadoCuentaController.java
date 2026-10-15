package edu.dosw.proyecto.style_radar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.GestionEstadoCuentaApi;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.impl.GestionEstadoCuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/admin/usuarios") @RequiredArgsConstructor
public class GestionEstadoCuentaController implements GestionEstadoCuentaApi {
    private final GestionEstadoCuentaService service;

    @InitBinder
    void validarEstado(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(EstadoCuenta.class, new java.beans.PropertyEditorSupport() {
            @Override public void setAsText(String text) { setValue(EstadoCuenta.valueOf(text)); }
        });
    }

    @Override
    public ResponseEntity<PageResponseDTO<CuentaAdministrativaResponseDTO>> consultar(
            @Valid @ModelAttribute CuentasConsultaRequestDTO request, @AuthenticationPrincipal UsuarioPrincipal actor) {
        var p = service.consultar(request.getEstado(), request.getPage(), request.getSize(), actor);
        return ResponseEntity.ok(new PageResponseDTO<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast()));
    }

    @Override
    public ResponseEntity<CuentaAdministrativaResponseDTO> consultarUno(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(service.consultarUno(id, actor));
    }

    @Override
    public ResponseEntity<CuentaAdministrativaResponseDTO> cambiar(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal actor, @Valid @RequestBody CambioEstadoCuentaRequestDTO request) {
        return ResponseEntity.ok(service.cambiar(id, actor, request));
    }
}
