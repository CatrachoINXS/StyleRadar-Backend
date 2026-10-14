package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.PlaylistApi;
import edu.dosw.proyecto.style_radar.mapper.PlaylistMapper;
import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.dto.request.AgregarPrendaPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PlaylistResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.IPlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PlaylistController implements PlaylistApi {

    private final IPlaylistService playlistService;
    private final PlaylistMapper playlistMapper;

    @Override
    public ResponseEntity<PlaylistResponseDTO> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearPlaylistRequestDTO request) {
        Playlist playlist = playlistService.crear(
                principal.getUsuarioId(),
                request.getNombre(),
                request.getDescripcion(),
                request.getVisibilidad(),
                request.getEstilo());
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistMapper.toResponse(playlist));
    }

    @Override
    public ResponseEntity<List<PlaylistResponseDTO>> obtenerPublicas() {
        return ResponseEntity.ok(toResponse(playlistService.obtenerPublicas()));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> obtenerPorId(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        Playlist playlist = playlistService.obtenerPorId(id, principal.getUsuarioId());
        return ResponseEntity.ok(playlistMapper.toResponse(playlist));
    }

    @Override
    public ResponseEntity<List<PlaylistResponseDTO>> obtenerMisPlaylists(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(toResponse(playlistService.obtenerPorUsuario(principal.getUsuarioId())));
    }

    @Override
    public ResponseEntity<List<PlaylistResponseDTO>> obtenerGuardadas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(toResponse(playlistService.obtenerGuardadas(principal.getUsuarioId())));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> editar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EditarPlaylistRequestDTO request) {
        Playlist editada = playlistService.editar(
                principal.getUsuarioId(),
                id,
                request.getNombre(),
                request.getDescripcion(),
                request.getVisibilidad(),
                request.getEstilo());
        return ResponseEntity.ok(playlistMapper.toResponse(editada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> agregarPrenda(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AgregarPrendaPlaylistRequestDTO request) {
        Playlist actualizada = playlistService.agregarPrenda(principal.getUsuarioId(), id, request.getPrendaId());
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> retirarPrenda(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long prendaId) {
        Playlist actualizada = playlistService.retirarPrenda(principal.getUsuarioId(), id, prendaId);
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> alternarLike(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(playlistMapper.toResponse(playlistService.alternarLike(principal.getUsuarioId(), id)));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> alternarGuardado(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(playlistMapper.toResponse(playlistService.alternarGuardado(principal.getUsuarioId(), id)));
    }

    @Override
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        playlistService.eliminar(principal.getUsuarioId(), id);
        return ResponseEntity.noContent().build();
    }

    private List<PlaylistResponseDTO> toResponse(List<Playlist> playlists) {
        return playlists.stream().map(playlistMapper::toResponse).toList();
    }
}
