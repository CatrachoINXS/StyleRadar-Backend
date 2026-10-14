package edu.dosw.proyecto.style_radar.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.PlanSuscripcionApi;
import edu.dosw.proyecto.style_radar.mapper.SuscripcionAlmacenMapper;
import edu.dosw.proyecto.style_radar.model.dto.response.PlanSuscripcionResponseDTO;
import edu.dosw.proyecto.style_radar.service.ISuscripcionAlmacenService;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/planes-suscripcion") @RequiredArgsConstructor
public class PlanSuscripcionController implements PlanSuscripcionApi {
    private final ISuscripcionAlmacenService service;
    private final SuscripcionAlmacenMapper mapper;
    @Override public ResponseEntity<List<PlanSuscripcionResponseDTO>> consultar() {
        return ResponseEntity.ok(service.planes().stream().map(mapper::toResponse).toList());
    }
}
