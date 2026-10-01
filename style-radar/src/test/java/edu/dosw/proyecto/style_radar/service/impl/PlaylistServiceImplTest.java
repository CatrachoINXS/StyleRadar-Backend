package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.PlaylistEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.PlaylistRepository;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.validator.PlaylistValidator;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-09-08T10:00:00Z");

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PrendaRepository prendaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlaylistEntityMapper playlistEntityMapper;

    @Mock
    private PlaylistValidator playlistValidator;

    @Mock
    private UsuarioValidator usuarioValidator;

    private PlaylistServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new PlaylistServiceImpl(
                playlistRepository,
                prendaRepository,
                usuarioRepository,
                playlistEntityMapper,
                playlistValidator,
                usuarioValidator,
                clock);
    }

    @Test
    void crearShouldSavePlaylist() {
        UsuarioEntity usuario = UsuarioEntity.builder().id(1L).build();
        PlaylistEntity savedEntity = PlaylistEntity.builder().id(10L).nombre("Mi Playlist").build();
        Playlist domain = Playlist.builder().id(10L).nombre("Mi Playlist").build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(playlistRepository.save(any(PlaylistEntity.class))).thenReturn(savedEntity);
        when(playlistEntityMapper.toDomain(savedEntity)).thenReturn(domain);

        Playlist result = service.crear(1L, "Mi Playlist", "Desc", VisibilidadPlaylist.PUBLICA, Estilo.CASUAL);

        assertThat(result.getId()).isEqualTo(10L);
        verify(playlistRepository).save(any(PlaylistEntity.class));
    }

    @Test
    void obtenerPorIdShouldValidateReadAccess() {
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).build();
        Playlist domain = Playlist.builder().id(10L).build();

        when(playlistValidator.validarYObtener(10L)).thenReturn(entity);
        doNothing().when(playlistValidator).validarAccesoLectura(entity, 1L);
        when(playlistEntityMapper.toDomain(entity)).thenReturn(domain);

        Playlist result = service.obtenerPorId(10L, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        verify(playlistValidator).validarAccesoLectura(entity, 1L);
    }

    @Test
    void obtenerPublicasShouldReturnList() {
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).build();
        Playlist domain = Playlist.builder().id(10L).build();

        when(playlistRepository.findByVisibilidadOrderByFechaCreacionDesc(VisibilidadPlaylist.PUBLICA)).thenReturn(List.of(entity));
        when(playlistEntityMapper.toDomain(entity)).thenReturn(domain);

        List<Playlist> result = service.obtenerPublicas();

        assertThat(result).containsExactly(domain);
    }

    @Test
    void obtenerPorUsuarioShouldReturnList() {
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).build();
        Playlist domain = Playlist.builder().id(10L).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(playlistRepository.findByUsuario_IdOrderByFechaCreacionDesc(1L)).thenReturn(List.of(entity));
        when(playlistEntityMapper.toDomain(entity)).thenReturn(domain);

        List<Playlist> result = service.obtenerPorUsuario(1L);

        assertThat(result).containsExactly(domain);
    }

    @Test
    void editarShouldUpdateAndReturnPlaylist() {
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).nombre("Antiguo").build();
        Playlist domain = Playlist.builder().id(10L).nombre("Nuevo").build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(playlistValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);
        when(playlistRepository.save(entity)).thenReturn(entity);
        when(playlistEntityMapper.toDomain(entity)).thenReturn(domain);

        Playlist result = service.editar(1L, 10L, "Nuevo", "Nueva desc", VisibilidadPlaylist.PRIVADA, Estilo.FORMAL);

        assertThat(result.getNombre()).isEqualTo("Nuevo");
        assertThat(entity.getNombre()).isEqualTo("Nuevo");
        assertThat(entity.getVisibilidad()).isEqualTo(VisibilidadPlaylist.PRIVADA);
    }

    @Test
    void agregarPrendaShouldAddPrendaSuccessfully() {
        PrendaEntity prenda = new PrendaEntity(5L, "Camisa", "Desc", TipoPrenda.SUPERIOR, "Marca", "Color", Estilo.CASUAL);
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).prendas(new ArrayList<>()).build();
        Playlist domain = Playlist.builder().id(10L).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        doNothing().when(playlistValidator).validarPrendaExiste(5L);
        when(playlistValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);
        when(prendaRepository.findById(5L)).thenReturn(Optional.of(prenda));
        when(playlistRepository.save(entity)).thenReturn(entity);
        when(playlistEntityMapper.toDomain(entity)).thenReturn(domain);

        Playlist result = service.agregarPrenda(1L, 10L, 5L);

        assertThat(result).isNotNull();
        assertThat(entity.getPrendas()).contains(prenda);
        verify(playlistRepository).save(entity);
    }

    @Test
    void agregarPrendaShouldThrowWhenAlreadyInPlaylist() {
        PrendaEntity prenda = new PrendaEntity(5L, "Camisa", "Desc", TipoPrenda.SUPERIOR, "Marca", "Color", Estilo.CASUAL);
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).prendas(new ArrayList<>(List.of(prenda))).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        doNothing().when(playlistValidator).validarPrendaExiste(5L);
        when(playlistValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);
        when(prendaRepository.findById(5L)).thenReturn(Optional.of(prenda));

        assertThatThrownBy(() -> service.agregarPrenda(1L, 10L, 5L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("La prenda ya se encuentra en la playlist");
    }

    @Test
    void retirarPrendaShouldRemovePrenda() {
        PrendaEntity prenda = new PrendaEntity(5L, "Camisa", "Desc", TipoPrenda.SUPERIOR, "Marca", "Color", Estilo.CASUAL);
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).prendas(new ArrayList<>(List.of(prenda))).build();
        Playlist domain = Playlist.builder().id(10L).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(playlistValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);
        when(playlistRepository.save(entity)).thenReturn(entity);
        when(playlistEntityMapper.toDomain(entity)).thenReturn(domain);

        Playlist result = service.retirarPrenda(1L, 10L, 5L);

        assertThat(entity.getPrendas()).isEmpty();
        verify(playlistRepository).save(entity);
    }

    @Test
    void retirarPrendaShouldThrowWhenNotInPlaylist() {
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).prendas(new ArrayList<>()).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(playlistValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);

        assertThatThrownBy(() -> service.retirarPrenda(1L, 10L, 5L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("La prenda no pertenece a la playlist");
    }

    @Test
    void alternarLikeShouldAddAndRemoveLike() {
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).likesUsuarios(new HashSet<>()).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(playlistValidator.validarYObtener(10L)).thenReturn(entity);
        when(playlistRepository.save(entity)).thenReturn(entity);
        when(playlistEntityMapper.toDomain(entity)).thenReturn(Playlist.builder().id(10L).build());

        // Add like
        service.alternarLike(1L, 10L);
        assertThat(entity.getLikesUsuarios()).contains(1L);

        // Remove like
        service.alternarLike(1L, 10L);
        assertThat(entity.getLikesUsuarios()).doesNotContain(1L);
    }

    @Test
    void eliminarShouldDeletePlaylist() {
        PlaylistEntity entity = PlaylistEntity.builder().id(10L).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(playlistValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);

        service.eliminar(1L, 10L);

        verify(playlistRepository).delete(entity);
    }
}
