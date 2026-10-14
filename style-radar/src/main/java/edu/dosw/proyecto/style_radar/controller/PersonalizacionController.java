package edu.dosw.proyecto.style_radar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.dosw.proyecto.style_radar.controller.docs.PersonalizacionApi;
import edu.dosw.proyecto.style_radar.mapper.BusquedaCatalogoMapper;
import edu.dosw.proyecto.style_radar.model.dto.request.PersonalizacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.RecomendacionesResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.IPersonalizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios/me")
@RequiredArgsConstructor
public class PersonalizacionController implements PersonalizacionApi {
    private final IPersonalizacionService personalizacion;
    private final BusquedaCatalogoMapper mapper;

    @Override
    public ResponseEntity<PageResponseDTO<CatalogoItemResponseDTO>> feed(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @ModelAttribute PersonalizacionRequestDTO request) {
        return ResponseEntity.ok(mapper.toResponse(
                personalizacion.obtenerFeed(principal.getUsuarioId(), request.getPage(), request.getSize())));
    }

    @Override
    public ResponseEntity<RecomendacionesResponseDTO> recomendaciones(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @ModelAttribute PersonalizacionRequestDTO request) {
        var resultado = personalizacion.obtenerRecomendaciones(principal.getUsuarioId(), request.getPage(), request.getSize());
        return ResponseEntity.ok(new RecomendacionesResponseDTO(
                mapper.toResponse(resultado.pagina()), resultado.generalSinPreferencias()));
    }
}
