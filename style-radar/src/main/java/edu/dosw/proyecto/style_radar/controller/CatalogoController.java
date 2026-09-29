package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.CatalogoApi;
import edu.dosw.proyecto.style_radar.mapper.CatalogoItemMapper;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.dto.request.CatalogoItemRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.service.ICatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/almacenes/{nit}/catalogo")
@RequiredArgsConstructor
public class CatalogoController implements CatalogoApi {

    private final ICatalogoService catalogoService;
    private final CatalogoItemMapper catalogoItemMapper;

    @Override
    public ResponseEntity<List<CatalogoItemResponseDTO>> obtenerCatalogo(@PathVariable String nit) {
        List<CatalogoItemResponseDTO> catalogo = catalogoService.obtenerCatalogo(nit).stream()
                .map(catalogoItemMapper::toResponse)
                .toList();
        return ResponseEntity.ok(catalogo);
    }

    @Override
    public ResponseEntity<CatalogoItemResponseDTO> publicar(
            @PathVariable String nit,
            @Valid @RequestBody CatalogoItemRequestDTO request) {
        Prenda prenda = catalogoItemMapper.toPrenda(request);
        ItemCatalogo item = catalogoService.publicar(nit, prenda, request.getPrecio());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoItemMapper.toResponse(item));
    }

    @Override
    public ResponseEntity<CatalogoItemResponseDTO> actualizar(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @Valid @RequestBody CatalogoItemRequestDTO request) {
        Prenda prenda = catalogoItemMapper.toPrenda(request);
        ItemCatalogo item = catalogoService.actualizar(nit, itemId, prenda, request.getPrecio());
        return ResponseEntity.ok(catalogoItemMapper.toResponse(item));
    }

    @Override
    public ResponseEntity<Void> retirar(@PathVariable String nit, @PathVariable Long itemId) {
        catalogoService.retirar(nit, itemId);
        return ResponseEntity.noContent().build();
    }
}
