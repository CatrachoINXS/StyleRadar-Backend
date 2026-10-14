package edu.dosw.proyecto.style_radar.controller;

import java.util.List;
import edu.dosw.proyecto.style_radar.security.IdentidadAutenticada;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.model.dto.request.PlaylistPopularesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.PlaylistApi;
import edu.dosw.proyecto.style_radar.mapper.PlaylistMapper;
import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.dto.request.AgregarPrendaPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.InteractuarPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PlaylistResponseDTO;
import edu.dosw.proyecto.style_radar.service.IPlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PlaylistController implements PlaylistApi {

    private final IPlaylistService playlistService;
    private final PlaylistMapper playlistMapper;
    private final IdentidadAutenticada identidad;

    @Override
    public ResponseEntity<PlaylistResponseDTO> crear(@Valid @RequestBody CrearPlaylistRequestDTO request) {
        identidad.exigirUsuario(request.getUsuarioId());
        Playlist playlist = playlistService.crear(
                request.getUsuarioId(),
                request.getNombre(),
                request.getDescripcion(),
                request.getVisibilidad(),
                request.getEstilo());
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistMapper.toResponse(playlist));
    }

    @Override
    public ResponseEntity<List<PlaylistResponseDTO>> obtenerPublicas() {
        List<PlaylistResponseDTO> playlists = playlistService.obtenerPublicas().stream()
                .map(playlistMapper::toResponse)
                .toList();
        return ResponseEntity.ok(playlists);
    }

    @Override
    public ResponseEntity<PageResponseDTO<PlaylistResponseDTO>> obtenerPopulares(
            @Valid @ModelAttribute PlaylistPopularesRequestDTO request) {
        var page = playlistService.obtenerPopulares(request.getPage(), request.getSize()).map(playlistMapper::toResponse);
        return ResponseEntity.ok(new PageResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast()));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> obtenerPorId(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioConsultaId) {
        if (usuarioConsultaId != null) identidad.exigirUsuario(usuarioConsultaId);
        Playlist playlist = playlistService.obtenerPorId(id, identidad.usuarioId());
        return ResponseEntity.ok(playlistMapper.toResponse(playlist));
    }

    @Override
    public ResponseEntity<List<PlaylistResponseDTO>> obtenerPorUsuario(@PathVariable Long usuarioId) {
        identidad.exigirUsuario(usuarioId);
        List<PlaylistResponseDTO> playlists = playlistService.obtenerPorUsuario(usuarioId).stream()
                .map(playlistMapper::toResponse)
                .toList();
        return ResponseEntity.ok(playlists);
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> editar(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @Valid @RequestBody EditarPlaylistRequestDTO request) {
        identidad.exigirUsuario(usuarioId);
        Playlist editada = playlistService.editar(
                usuarioId,
                id,
                request.getNombre(),
                request.getDescripcion(),
                request.getVisibilidad(),
                request.getEstilo());
        return ResponseEntity.ok(playlistMapper.toResponse(editada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> agregarPrenda(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @Valid @RequestBody AgregarPrendaPlaylistRequestDTO request) {
        identidad.exigirUsuario(usuarioId);
        Playlist actualizada = playlistService.agregarPrenda(usuarioId, id, request.getPrendaId());
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> retirarPrenda(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @PathVariable Long prendaId) {
        identidad.exigirUsuario(usuarioId);
        Playlist actualizada = playlistService.retirarPrenda(usuarioId, id, prendaId);
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> alternarLike(
            @PathVariable Long id,
            @Valid @RequestBody InteractuarPlaylistRequestDTO request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal == null || !principal.getUsuarioId().equals(request.getUsuarioId())) {
            throw new AccessDeniedException("Acceso denegado");
        }
        Playlist actualizada = playlistService.alternarLike(principal.getUsuarioId(), id);
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<Void> eliminar(
            @PathVariable Long usuarioId,
            @PathVariable Long id) {
        identidad.exigirUsuario(usuarioId);
        playlistService.eliminar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
