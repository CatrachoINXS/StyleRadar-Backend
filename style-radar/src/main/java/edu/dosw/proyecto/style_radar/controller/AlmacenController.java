package edu.dosw.proyecto.style_radar.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.controller.docs.AlmacenApi;
import edu.dosw.proyecto.style_radar.mapper.AlmacenMapper;
import edu.dosw.proyecto.style_radar.mapper.CatalogoItemMapper;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.service.IAlmacenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/almacenes")
@RequiredArgsConstructor
public class AlmacenController implements AlmacenApi {
    private final IAlmacenService service;
    private final AlmacenMapper mapper;
    private final CatalogoItemMapper catalogoMapper;

    @Override
    public ResponseEntity<PageResponseDTO<AlmacenResponseDTO>> consultar(
            @Valid @ModelAttribute ConsultaAlmacenRequestDTO request) {
        return ResponseEntity.ok(mapper.toPageResponse(service.consultar(request.getCategoria(),
                request.getPage(), request.getSize()).map(mapper::toResponse)));
    }

    @Override
    public ResponseEntity<List<CatalogoItemResponseDTO>> resumen(@PathVariable String nit,
            @Valid @ModelAttribute ResumenAlmacenRequestDTO request) {
        return ResponseEntity.ok(service.resumen(nit, request.getLimite()).stream()
                .map(catalogoMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<PageResponseDTO<CatalogoItemResponseDTO>> novedades(@PathVariable String nit,
            @Valid @ModelAttribute PaginacionAlmacenRequestDTO request) {
        return ResponseEntity.ok(mapper.toPageResponse(service.novedades(nit, request.getPage(),
                request.getSize()).map(catalogoMapper::toResponse)));
    }

    @Override
    public ResponseEntity<DistanciaAlmacenResponseDTO> distancia(@PathVariable String nit,
            @Valid @ModelAttribute DistanciaAlmacenRequestDTO request) {
        return ResponseEntity.ok(mapper.toResponse(service.distancia(nit,
                request.getLatitudUsuario(), request.getLongitudUsuario())));
    }
}
