package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.PlaylistEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.validator.*;

@ExtendWith(MockitoExtension.class)
class PlaylistsPopularesServiceTest {
    @Mock PlaylistRepository repository;
    @Mock PrendaRepository prendas;
    @Mock UsuarioRepository usuarios;
    @Mock ItemCatalogoRepository items;
    @Mock PublicacionSegundaManoRepository publicaciones;
    @Mock PlaylistEntityMapper mapper;
    @Mock UsuarioValidator usuarioValidator;
    PlaylistServiceImpl service;
    @BeforeEach void preparar() {
        service = new PlaylistServiceImpl(repository, prendas, usuarios, items, publicaciones, mapper,
                new PlaylistValidator(repository, prendas), usuarioValidator, Clock.systemUTC());
    }
    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,-1", "0,101"})
    void rechazaAntesDeConsultar(int page, int size) {
        assertThatThrownBy(() -> service.obtenerPopulares(page, size)).isInstanceOf(SolicitudPlaylistInvalidaException.class);
        verifyNoInteractions(repository, mapper, prendas);
    }
    @Test void reconstruyeOrdenDeIdsYFiltraPrendasEnUnLote() {
        var pageable = PageRequest.of(0, 2);
        when(repository.findIdsPopulares(pageable)).thenReturn(new PageImpl<>(List.of(2L, 1L), pageable, 4));
        var uno = PlaylistEntity.builder().id(1L).build(); var dos = PlaylistEntity.builder().id(2L).build();
        when(repository.findDetallesPopulares(List.of(2L, 1L))).thenReturn(List.of(uno, dos));
        var visible = new Prenda(); visible.setId(5L); var oculta = new Prenda(); oculta.setId(6L);
        var a = Playlist.builder().id(1L).prendas(List.of(oculta)).likesUsuarios(Set.of(9L)).build();
        var b = Playlist.builder().id(2L).prendas(List.of(visible, oculta)).likesUsuarios(Set.of(8L, 9L)).build();
        when(mapper.toDomain(uno)).thenReturn(a); when(mapper.toDomain(dos)).thenReturn(b);
        when(prendas.findIdsOcultas(List.of(5L, 6L))).thenReturn(List.of(6L));
        var resultado = service.obtenerPopulares(0, 2);
        assertThat(resultado.getContent()).containsExactly(b, a); assertThat(resultado.getTotalElements()).isEqualTo(4);
        assertThat(b.getPrendas()).containsExactly(visible); assertThat(a.getPrendas()).isEmpty();
        assertThat(b.getLikesCount()).isEqualTo(2); assertThat(a.getLikesCount()).isEqualTo(1);
        verify(repository).cargarLikesPorIds(List.of(2L, 1L)); verify(prendas, times(1)).findIdsOcultas(anyList());
        verify(repository, never()).findAll(); verify(repository, never()).findById(anyLong());
    }
    @Test void paginaVaciaNoCargaDetalles() {
        var pageable = PageRequest.of(10, 20);
        when(repository.findIdsPopulares(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 4));
        var resultado = service.obtenerPopulares(10, 20);
        assertThat(resultado.getContent()).isEmpty(); assertThat(resultado.getTotalElements()).isEqualTo(4);
        verify(repository, never()).findDetallesPopulares(anyList()); verify(repository, never()).cargarLikesPorIds(anyList());
        verifyNoInteractions(mapper, prendas);
    }
    @Test void overflowConsultaSoloCountPublico() {
        when(repository.countByVisibilidad(VisibilidadPlaylist.PUBLICA)).thenReturn(4L);
        var resultado = service.obtenerPopulares(Integer.MAX_VALUE, 100);
        assertThat(resultado.getContent()).isEmpty(); assertThat(resultado.getTotalElements()).isEqualTo(4);
        verify(repository, never()).findIdsPopulares(any()); verifyNoInteractions(mapper, prendas);
    }
    @Test void errorTecnicoNoSeConvierteEnPaginaVacia() {
        when(repository.findIdsPopulares(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture"));
        assertThatThrownBy(() -> service.obtenerPopulares(0, 20)).isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
    }
    @Test void likeInexistenteNoGuarda() {
        when(repository.findForUpdate(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.alternarLike(1L, 99L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).save(any());
    }
    @Test void likeInicializaColeccionNulaBajoLock() {
        var entity = PlaylistEntity.builder().id(10L).likesUsuarios(null).build();
        when(repository.findForUpdate(10L)).thenReturn(Optional.of(entity)); when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(Playlist.builder().id(10L).likesUsuarios(Set.of(1L)).build());
        assertThat(service.alternarLike(1L, 10L).getLikesCount()).isEqualTo(1);
        assertThat(entity.getLikesUsuarios()).containsExactly(1L);
    }
}
