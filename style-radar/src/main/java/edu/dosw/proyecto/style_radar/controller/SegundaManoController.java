package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.SegundaManoApi;
import edu.dosw.proyecto.style_radar.mapper.PrendaMapper;
import edu.dosw.proyecto.style_radar.mapper.PublicacionSegundaManoMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PublicacionSegundaManoResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.ISegundaManoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SegundaManoController implements SegundaManoApi {

    private final ISegundaManoService segundaManoService;
    private final PublicacionSegundaManoMapper publicacionMapper;
    private final PrendaMapper prendaMapper;

    @Override
    public ResponseEntity<PublicacionSegundaManoResponseDTO> publicar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearPublicacionSegundaManoRequestDTO request) {
        Prenda prenda = prendaMapper.toDomain(request.getPrenda());
        PublicacionSegundaMano publicacion = segundaManoService.publicar(
                principal.getUsuarioId(),
                prenda,
                request.getTalla(),
                request.getEstadoConservacion(),
                request.getPrecio(),
                request.getFotos());
        return ResponseEntity.status(HttpStatus.CREATED).body(publicacionMapper.toResponse(publicacion));
    }

    @Override
    public ResponseEntity<PublicacionSegundaManoResponseDTO> obtenerPorId(@PathVariable Long id) {
        PublicacionSegundaMano publicacion = segundaManoService.obtenerPorId(id);
        return ResponseEntity.ok(publicacionMapper.toResponse(publicacion));
    }

    @Override
    public ResponseEntity<PageResponseDTO<PublicacionSegundaManoResponseDTO>> buscar(
            @Valid @ModelAttribute BusquedaSegundaManoRequestDTO request) {
        BusquedaSegundaManoCriteria criteria = publicacionMapper.toCriteria(request);
        Page<PublicacionSegundaMano> resultado = segundaManoService.buscar(criteria, request.getPage(), request.getSize());
        return ResponseEntity.ok(publicacionMapper.toPageResponse(resultado));
    }

    @Override
    public ResponseEntity<List<PublicacionSegundaManoResponseDTO>> obtenerMisPublicaciones(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        List<PublicacionSegundaManoResponseDTO> publicaciones = segundaManoService
                .obtenerPorUsuario(principal.getUsuarioId()).stream()
                .map(publicacionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(publicaciones);
    }

    @Override
    public ResponseEntity<PublicacionSegundaManoResponseDTO> editar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EditarPublicacionSegundaManoRequestDTO request) {
        Prenda prenda = prendaMapper.toDomain(request.getPrenda());
        PublicacionSegundaMano editada = segundaManoService.editar(
                principal.getUsuarioId(),
                id,
                prenda,
                request.getTalla(),
                request.getEstadoConservacion(),
                request.getPrecio(),
                request.getFotos());
        return ResponseEntity.ok(publicacionMapper.toResponse(editada));
    }

    @Override
    public ResponseEntity<PublicacionSegundaManoResponseDTO> marcarComoVendida(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        PublicacionSegundaMano vendida = segundaManoService.marcarComoVendida(principal.getUsuarioId(), id);
        return ResponseEntity.ok(publicacionMapper.toResponse(vendida));
    }

    @Override
    public ResponseEntity<PublicacionSegundaManoResponseDTO> marcarParaDonacion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        PublicacionSegundaMano enDonacion = segundaManoService.marcarParaDonacion(principal.getUsuarioId(), id);
        return ResponseEntity.ok(publicacionMapper.toResponse(enDonacion));
    }

    @Override
    public ResponseEntity<Void> retirar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        segundaManoService.retirar(principal.getUsuarioId(), id);
        return ResponseEntity.noContent().build();
    }
}
