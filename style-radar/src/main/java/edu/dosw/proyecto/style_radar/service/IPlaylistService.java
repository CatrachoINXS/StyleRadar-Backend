package edu.dosw.proyecto.style_radar.service;

import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;

public interface IPlaylistService {

    Playlist crear(
            Long usuarioId,
            String nombre,
            String descripcion,
            VisibilidadPlaylist visibilidad,
            Estilo estilo);

    Playlist obtenerPorId(Long id, Long usuarioConsultaId);

    List<Playlist> obtenerPublicas();

    List<Playlist> obtenerPorUsuario(Long usuarioId);

    Playlist editar(
            Long usuarioId,
            Long playlistId,
            String nombre,
            String descripcion,
            VisibilidadPlaylist visibilidad,
            Estilo estilo);

    Playlist agregarPrenda(Long usuarioId, Long playlistId, Long prendaId);

    Playlist retirarPrenda(Long usuarioId, Long playlistId, Long prendaId);

    Playlist alternarLike(Long usuarioId, Long playlistId);

    void eliminar(Long usuarioId, Long playlistId);
}
