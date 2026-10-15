package edu.dosw.proyecto.style_radar.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.PlaylistRepository;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;

@ExtendWith(MockitoExtension.class)
class PlaylistValidatorTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PrendaRepository prendaRepository;

    @InjectMocks
    private PlaylistValidator validator;

    @Test
    void validarYObtenerShouldThrowWhenNotFound() {
        when(playlistRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validarYObtener(5L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe una playlist con ID: 5");
    }

    @Test
    void validarYObtenerDeUsuarioShouldThrowWhenMismatch() {
        UsuarioEntity owner = UsuarioEntity.builder().id(2L).build();
        PlaylistEntity playlist = PlaylistEntity.builder().id(5L).usuario(owner).build();
        when(playlistRepository.findById(5L)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> validator.validarYObtenerDeUsuario(5L, 1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("La playlist no pertenece al usuario especificado");
    }

    @Test
    void validarYObtenerDeUsuarioShouldReturnEntityWhenValid() {
        UsuarioEntity owner = UsuarioEntity.builder().id(1L).build();
        PlaylistEntity playlist = PlaylistEntity.builder().id(5L).usuario(owner).build();
        when(playlistRepository.findById(5L)).thenReturn(Optional.of(playlist));

        PlaylistEntity result = validator.validarYObtenerDeUsuario(5L, 1L);
        assertThat(result).isSameAs(playlist);
    }

    @Test
    void validarAccesoLecturaShouldThrowWhenPrivateAndDifferentUser() {
        UsuarioEntity owner = UsuarioEntity.builder().id(1L).build();
        PlaylistEntity playlist = PlaylistEntity.builder()
                .visibilidad(VisibilidadPlaylist.PRIVADA)
                .usuario(owner)
                .build();

        assertThatThrownBy(() -> validator.validarAccesoLectura(playlist, null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("No tiene permisos");

        assertThatThrownBy(() -> validator.validarAccesoLectura(playlist, 2L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("No tiene permisos");
    }

    @Test
    void validarAccesoLecturaShouldPassWhenPublicOrOwner() {
        UsuarioEntity owner = UsuarioEntity.builder().id(1L).build();
        PlaylistEntity privatePlaylist = PlaylistEntity.builder()
                .visibilidad(VisibilidadPlaylist.PRIVADA)
                .usuario(owner)
                .build();
        PlaylistEntity publicPlaylist = PlaylistEntity.builder()
                .visibilidad(VisibilidadPlaylist.PUBLICA)
                .usuario(owner)
                .build();

        assertThatCode(() -> validator.validarAccesoLectura(privatePlaylist, 1L)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarAccesoLectura(publicPlaylist, null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarAccesoLectura(publicPlaylist, 2L)).doesNotThrowAnyException();
    }

    @Test
    void validarPrendaExisteShouldThrowWhenNullOrNotExists() {
        when(prendaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> validator.validarPrendaExiste(null))
                .isInstanceOf(RecursoNoEncontradoException.class);

        assertThatThrownBy(() -> validator.validarPrendaExiste(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe una prenda con ID: 99");
    }

    @Test
    void validarPrendaExisteShouldPassWhenExists() {
        when(prendaRepository.existsById(10L)).thenReturn(true);
        assertThatCode(() -> validator.validarPrendaExiste(10L)).doesNotThrowAnyException();
    }
}
