package edu.dosw.proyecto.style_radar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.InventarioApi;
import edu.dosw.proyecto.style_radar.mapper.CatalogoItemMapper;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarStockRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.TallasDisponiblesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.service.IInventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/almacenes/{nit}/catalogo")
@RequiredArgsConstructor
public class InventarioController implements InventarioApi {

    private final IInventarioService inventarioService;
    private final CatalogoItemMapper catalogoItemMapper;

    @Override
    public ResponseEntity<CatalogoItemResponseDTO> registrarTallas(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @Valid @RequestBody TallasDisponiblesRequestDTO request) {
        ItemCatalogo item = inventarioService.registrarTallas(nit, itemId, request.getTallas());
        return ResponseEntity.ok(catalogoItemMapper.toResponse(item));
    }

    @Override
    public ResponseEntity<CatalogoItemResponseDTO> actualizarDisponibilidad(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @PathVariable Talla talla,
            @Valid @RequestBody ActualizarStockRequestDTO request) {
        ItemCatalogo item = inventarioService.actualizarDisponibilidad(nit, itemId, talla, request.getUnidades());
        return ResponseEntity.ok(catalogoItemMapper.toResponse(item));
    }
}
