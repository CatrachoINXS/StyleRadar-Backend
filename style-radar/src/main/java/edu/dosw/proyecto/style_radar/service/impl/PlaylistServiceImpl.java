package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.PlaylistEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.PlaylistRepository;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.IPlaylistService;
import edu.dosw.proyecto.style_radar.validator.PlaylistValidator;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistServiceImpl implements IPlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PrendaRepository prendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlaylistEntityMapper playlistEntityMapper;
    private final PlaylistValidator playlistValidator;
    private final UsuarioValidator usuarioValidator;
    private final Clock clock;

    @Override
    @Transactional
    public Playlist crear(
            Long usuarioId,
            String nombre,
            String descripcion,
            VisibilidadPlaylist visibilidad,
            Estilo estilo) {
        log.info("Creando playlist '{}' para el usuario ID: {}", nombre, usuarioId);
        usuarioValidator.validarExiste(usuarioId);

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con ID: " + usuarioId));

        PlaylistEntity entity = PlaylistEntity.builder()
                .usuario(usuario)
                .nombre(nombre)
                .descripcion(descripcion)
                .visibilidad(visibilidad)
                .estilo(estilo)
                .fechaCreacion(clock.instant())
                .prendas(new ArrayList<>())
                .likesUsuarios(new HashSet<>())
                .build();

        PlaylistEntity guardada = playlistRepository.save(entity);
        log.info("Playlist creada exitosamente con ID: {}", guardada.getId());
        return playlistEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Playlist obtenerPorId(Long id, Long usuarioConsultaId) {
        log.info("Consultando playlist ID: {} por usuario ID: {}", id, usuarioConsultaId);
        PlaylistEntity entity = playlistValidator.validarYObtener(id);
        playlistValidator.validarAccesoLectura(entity, usuarioConsultaId);
        return playlistEntityMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Playlist> obtenerPublicas() {
        log.info("Consultando todas las playlists públicas");
        List<Playlist> publicas = playlistRepository
                .findByVisibilidadOrderByFechaCreacionDesc(VisibilidadPlaylist.PUBLICA)
                .stream()
                .map(playlistEntityMapper::toDomain)
                .toList();
        log.info("Playlists públicas obtenidas: {}", publicas.size());
        return publicas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Playlist> obtenerPorUsuario(Long usuarioId) {
        log.info("Consultando playlists del usuario ID: {}", usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        List<Playlist> playlists = playlistRepository
                .findByUsuario_IdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(playlistEntityMapper::toDomain)
                .toList();
        log.info("Playlists obtenidas para usuario ID {}: {}", usuarioId, playlists.size());
        return playlists;
    }

    @Override
    @Transactional
    public Playlist editar(
            Long usuarioId,
            Long playlistId,
            String nombre,
            String descripcion,
            VisibilidadPlaylist visibilidad,
            Estilo estilo) {
        log.info("Editando playlist ID: {} por usuario ID: {}", playlistId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);

        PlaylistEntity entity = playlistValidator.validarYObtenerDeUsuario(playlistId, usuarioId);
        entity.setNombre(nombre);
        entity.setDescripcion(descripcion);
        if (visibilidad != null) {
            entity.setVisibilidad(visibilidad);
        }
        entity.setEstilo(estilo);

        PlaylistEntity guardada = playlistRepository.save(entity);
        log.info("Playlist ID: {} editada exitosamente", playlistId);
        return playlistEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public Playlist agregarPrenda(Long usuarioId, Long playlistId, Long prendaId) {
        log.info("Agregando prenda ID: {} a playlist ID: {} por usuario ID: {}", prendaId, playlistId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        playlistValidator.validarPrendaExiste(prendaId);

        PlaylistEntity playlist = playlistValidator.validarYObtenerDeUsuario(playlistId, usuarioId);
        PrendaEntity prenda = prendaRepository.findById(prendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe prenda con ID: " + prendaId));

        boolean yaExiste = playlist.getPrendas().stream()
                .anyMatch(p -> p.getId().equals(prendaId));
        if (yaExiste) {
            throw new ReglaDeNegocioException("La prenda ya se encuentra en la playlist");
        }

        playlist.getPrendas().add(prenda);
        PlaylistEntity guardada = playlistRepository.save(playlist);
        log.info("Prenda ID: {} agregada a playlist ID: {}", prendaId, playlistId);
        return playlistEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public Playlist retirarPrenda(Long usuarioId, Long playlistId, Long prendaId) {
        log.info("Retirando prenda ID: {} de playlist ID: {} por usuario ID: {}", prendaId, playlistId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);

        PlaylistEntity playlist = playlistValidator.validarYObtenerDeUsuario(playlistId, usuarioId);
        boolean removida = playlist.getPrendas().removeIf(p -> p.getId().equals(prendaId));
        if (!removida) {
            throw new ReglaDeNegocioException("La prenda no pertenece a la playlist");
        }

        PlaylistEntity guardada = playlistRepository.save(playlist);
        log.info("Prenda ID: {} retirada de playlist ID: {}", prendaId, playlistId);
        return playlistEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public Playlist alternarLike(Long usuarioId, Long playlistId) {
        log.info("Alternando like para playlist ID: {} por usuario ID: {}", playlistId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);

        PlaylistEntity playlist = playlistValidator.validarYObtener(playlistId);
        if (playlist.getLikesUsuarios() == null) {
            playlist.setLikesUsuarios(new HashSet<>());
        }

        if (playlist.getLikesUsuarios().contains(usuarioId)) {
            playlist.getLikesUsuarios().remove(usuarioId);
            log.info("Like removido de playlist ID: {} por usuario ID: {}", playlistId, usuarioId);
        } else {
            playlist.getLikesUsuarios().add(usuarioId);
            log.info("Like agregado a playlist ID: {} por usuario ID: {}", playlistId, usuarioId);
        }

        PlaylistEntity guardada = playlistRepository.save(playlist);
        return playlistEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public void eliminar(Long usuarioId, Long playlistId) {
        log.info("Eliminando playlist ID: {} por usuario ID: {}", playlistId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);

        PlaylistEntity playlist = playlistValidator.validarYObtenerDeUsuario(playlistId, usuarioId);
        playlistRepository.delete(playlist);
        log.info("Playlist ID: {} eliminada exitosamente", playlistId);
    }
}
