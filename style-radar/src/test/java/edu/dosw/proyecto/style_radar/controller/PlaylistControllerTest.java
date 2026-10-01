package edu.dosw.proyecto.style_radar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import edu.dosw.proyecto.style_radar.exception.GlobalExceptionHandler;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.PlaylistMapper;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.dto.response.PlaylistResponseDTO;
import edu.dosw.proyecto.style_radar.service.IPlaylistService;

@ExtendWith(MockitoExtension.class)
class PlaylistControllerTest {

    @Mock
    private IPlaylistService playlistService;

    @Mock
    private PlaylistMapper playlistMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaylistController(playlistService, playlistMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void crearShouldReturnCreatedWhenValid() throws Exception {
        String requestBody = """
                {
                  "usuarioId": 1,
                  "nombre": "Looks de Fiesta",
                  "descripcion": "Colección elegante",
                  "visibilidad": "PUBLICA",
                  "estilo": "FORMAL"
                }
                """;
        Playlist domain = Playlist.builder().id(10L).nombre("Looks de Fiesta").build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).nombre("Looks de Fiesta").build();

        when(playlistService.crear(eq(1L), eq("Looks de Fiesta"), eq("Colección elegante"), eq(VisibilidadPlaylist.PUBLICA), eq(Estilo.FORMAL))).thenReturn(domain);
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(post("/api/v1/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.nombre").value("Looks de Fiesta"));
    }

    @Test
    void obtenerPublicasShouldReturnList() throws Exception {
        Playlist domain = Playlist.builder().id(10L).build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).build();

        when(playlistService.obtenerPublicas()).thenReturn(List.of(domain));
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get("/api/v1/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void obtenerPorIdShouldReturnOkWhenFound() throws Exception {
        Playlist domain = Playlist.builder().id(10L).build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).build();

        when(playlistService.obtenerPorId(10L, null)).thenReturn(domain);
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get("/api/v1/playlists/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void obtenerPorUsuarioShouldReturnList() throws Exception {
        Playlist domain = Playlist.builder().id(10L).build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).build();

        when(playlistService.obtenerPorUsuario(1L)).thenReturn(List.of(domain));
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get("/api/v1/usuarios/1/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void editarShouldReturnOk() throws Exception {
        String requestBody = """
                {
                  "nombre": "Looks de Noche",
                  "descripcion": "Nueva descripcion",
                  "visibilidad": "PRIVADA",
                  "estilo": "FORMAL"
                }
                """;
        Playlist domain = Playlist.builder().id(10L).nombre("Looks de Noche").build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).nombre("Looks de Noche").build();

        when(playlistService.editar(eq(1L), eq(10L), eq("Looks de Noche"), eq("Nueva descripcion"), eq(VisibilidadPlaylist.PRIVADA), eq(Estilo.FORMAL))).thenReturn(domain);
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(put("/api/v1/usuarios/1/playlists/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void agregarPrendaShouldReturnOk() throws Exception {
        String requestBody = """
                {
                  "prendaId": 5
                }
                """;
        Playlist domain = Playlist.builder().id(10L).build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).build();

        when(playlistService.agregarPrenda(1L, 10L, 5L)).thenReturn(domain);
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(post("/api/v1/usuarios/1/playlists/10/prendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void retirarPrendaShouldReturnOk() throws Exception {
        Playlist domain = Playlist.builder().id(10L).build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).build();

        when(playlistService.retirarPrenda(1L, 10L, 5L)).thenReturn(domain);
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/usuarios/1/playlists/10/prendas/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void alternarLikeShouldReturnOk() throws Exception {
        String requestBody = """
                {
                  "usuarioId": 2
                }
                """;
        Playlist domain = Playlist.builder().id(10L).build();
        PlaylistResponseDTO response = PlaylistResponseDTO.builder().id(10L).likes(1).build();

        when(playlistService.alternarLike(2L, 10L)).thenReturn(domain);
        when(playlistMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(post("/api/v1/playlists/10/like")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1));
    }

    @Test
    void eliminarShouldReturnNoContent() throws Exception {
        doNothing().when(playlistService).eliminar(1L, 10L);

        mockMvc.perform(delete("/api/v1/usuarios/1/playlists/10"))
                .andExpect(status().isNoContent());

        verify(playlistService).eliminar(1L, 10L);
    }
}
