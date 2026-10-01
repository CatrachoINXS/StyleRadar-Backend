package edu.dosw.proyecto.style_radar.validator;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;
import edu.dosw.proyecto.style_radar.repository.PlaylistRepository;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlaylistValidator {

    private final PlaylistRepository playlistRepository;
    private final PrendaRepository prendaRepository;

    public PlaylistEntity validarYObtener(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una playlist con ID: " + playlistId));
    }

    public PlaylistEntity validarYObtenerDeUsuario(Long playlistId, Long usuarioId) {
        PlaylistEntity playlist = validarYObtener(playlistId);
        if (!playlist.getUsuario().getId().equals(usuarioId)) {
            throw new ReglaDeNegocioException("La playlist no pertenece al usuario especificado");
        }
        return playlist;
    }

    public void validarAccesoLectura(PlaylistEntity playlist, Long usuarioConsultaId) {
        if (playlist.getVisibilidad() == VisibilidadPlaylist.PRIVADA) {
            if (usuarioConsultaId == null || !playlist.getUsuario().getId().equals(usuarioConsultaId)) {
                throw new ReglaDeNegocioException("No tiene permisos para consultar esta playlist privada");
            }
        }
    }

    public void validarPrendaExiste(Long prendaId) {
        if (prendaId == null || !prendaRepository.existsById(prendaId)) {
            throw new RecursoNoEncontradoException("No existe una prenda con ID: " + prendaId);
        }
    }
}
