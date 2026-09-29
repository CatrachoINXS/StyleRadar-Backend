package edu.dosw.proyecto.style_radar.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.BusquedaCatalogoApi;
import edu.dosw.proyecto.style_radar.mapper.BusquedaCatalogoMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaCatalogoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.service.IBusquedaCatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/catalogo")
@RequiredArgsConstructor
public class BusquedaCatalogoController implements BusquedaCatalogoApi {

    private final IBusquedaCatalogoService busquedaCatalogoService;
    private final BusquedaCatalogoMapper busquedaCatalogoMapper;

    @Override
    public ResponseEntity<PageResponseDTO<CatalogoItemResponseDTO>> buscar(
            @Valid @ModelAttribute BusquedaCatalogoRequestDTO request) {
        BusquedaCatalogoCriteria criteria = busquedaCatalogoMapper.toCriteria(request);
        Page<ItemCatalogo> resultado = busquedaCatalogoService.buscar(criteria, request.getPage(), request.getSize());
        return ResponseEntity.ok(busquedaCatalogoMapper.toResponse(resultado));
    }
}
