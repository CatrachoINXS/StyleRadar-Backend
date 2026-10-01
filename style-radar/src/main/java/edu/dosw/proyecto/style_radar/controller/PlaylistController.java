package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

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

    @Override
    public ResponseEntity<PlaylistResponseDTO> crear(@Valid @RequestBody CrearPlaylistRequestDTO request) {
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
    public ResponseEntity<PlaylistResponseDTO> obtenerPorId(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioConsultaId) {
        Playlist playlist = playlistService.obtenerPorId(id, usuarioConsultaId);
        return ResponseEntity.ok(playlistMapper.toResponse(playlist));
    }

    @Override
    public ResponseEntity<List<PlaylistResponseDTO>> obtenerPorUsuario(@PathVariable Long usuarioId) {
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
        Playlist actualizada = playlistService.agregarPrenda(usuarioId, id, request.getPrendaId());
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> retirarPrenda(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @PathVariable Long prendaId) {
        Playlist actualizada = playlistService.retirarPrenda(usuarioId, id, prendaId);
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<PlaylistResponseDTO> alternarLike(
            @PathVariable Long id,
            @Valid @RequestBody InteractuarPlaylistRequestDTO request) {
        Playlist actualizada = playlistService.alternarLike(request.getUsuarioId(), id);
        return ResponseEntity.ok(playlistMapper.toResponse(actualizada));
    }

    @Override
    public ResponseEntity<Void> eliminar(
            @PathVariable Long usuarioId,
            @PathVariable Long id) {
        playlistService.eliminar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
