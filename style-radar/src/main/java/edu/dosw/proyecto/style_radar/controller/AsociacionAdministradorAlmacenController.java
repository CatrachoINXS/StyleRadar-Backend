package edu.dosw.proyecto.style_radar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.AsociacionAdministradorAlmacenApi;
import edu.dosw.proyecto.style_radar.model.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.impl.AsociacionAdministradorAlmacenService;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/admin/almacenes") @RequiredArgsConstructor
public class AsociacionAdministradorAlmacenController implements AsociacionAdministradorAlmacenApi {
    private final AsociacionAdministradorAlmacenService service;
    @Override public ResponseEntity<AdministradorAlmacenResponseDTO> asociar(@PathVariable String nit, @PathVariable Long usuarioId,
            @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(service.asociar(nit, usuarioId, actor));
    }
}
