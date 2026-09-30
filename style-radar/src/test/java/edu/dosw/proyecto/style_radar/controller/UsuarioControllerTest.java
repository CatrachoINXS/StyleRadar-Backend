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

import java.time.Instant;
import java.util.List;
import java.util.Set;

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
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.BusquedaGuardadaMapper;
import edu.dosw.proyecto.style_radar.mapper.UsuarioMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarPreferenciasEstiloRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarTallasHabitualesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.GuardarBusquedaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.RegistrarUsuarioRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.BusquedaGuardadaResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.UsuarioResponseDTO;
import edu.dosw.proyecto.style_radar.service.IUsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private static final String BASE_PATH = "/api/v1/usuarios";

    @Mock
    private IUsuarioService usuarioService;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private BusquedaGuardadaMapper busquedaGuardadaMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UsuarioController(usuarioService, usuarioMapper, busquedaGuardadaMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registrarShouldReturnCreatedWhenValid() throws Exception {
        String requestBody = """
                {
                  "nombre": "Ana Torres",
                  "email": "ana@example.com",
                  "telefono": "3001234567"
                }
                """;
        Usuario domain = Usuario.builder().nombre("Ana Torres").email("ana@example.com").build();
        Usuario registrado = Usuario.builder().id(1L).nombre("Ana Torres").email("ana@example.com").build();
        UsuarioResponseDTO response = UsuarioResponseDTO.builder().id(1L).nombre("Ana Torres").email("ana@example.com").build();

        when(usuarioMapper.toDomain(any(RegistrarUsuarioRequestDTO.class))).thenReturn(domain);
        when(usuarioService.registrarUsuario(domain)).thenReturn(registrado);
        when(usuarioMapper.toResponse(registrado)).thenReturn(response);

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Ana Torres"));
    }

    @Test
    void registrarShouldReturnBadRequestWhenInvalid() throws Exception {
        String invalidRequest = """
                {
                  "nombre": "",
                  "email": "invalido",
                  "telefono": "1"
                }
                """;

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerPorIdShouldReturnUserWhenFound() throws Exception {
        Usuario domain = Usuario.builder().id(1L).nombre("Ana").build();
        UsuarioResponseDTO response = UsuarioResponseDTO.builder().id(1L).nombre("Ana").build();

        when(usuarioService.obtenerPorId(1L)).thenReturn(domain);
        when(usuarioMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get(BASE_PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void obtenerPorIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(usuarioService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

        mockMvc.perform(get(BASE_PATH + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarPreferenciasEstiloShouldReturnOk() throws Exception {
        String requestBody = """
                {
                  "preferencias": ["CASUAL", "URBANO"]
                }
                """;
        Usuario domain = Usuario.builder().id(1L).preferenciasEstilo(Set.of(Estilo.CASUAL, Estilo.URBANO)).build();
        UsuarioResponseDTO response = UsuarioResponseDTO.builder().id(1L).preferenciasEstilo(Set.of(Estilo.CASUAL, Estilo.URBANO)).build();

        when(usuarioService.actualizarPreferenciasEstilo(eq(1L), any())).thenReturn(domain);
        when(usuarioMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(put(BASE_PATH + "/1/preferencias-estilo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void actualizarTallasHabitualesShouldReturnOk() throws Exception {
        String requestBody = """
                {
                  "tallas": ["M", "L"]
                }
                """;
        Usuario domain = Usuario.builder().id(1L).tallasHabituales(Set.of(Talla.M, Talla.L)).build();
        UsuarioResponseDTO response = UsuarioResponseDTO.builder().id(1L).tallasHabituales(Set.of(Talla.M, Talla.L)).build();

        when(usuarioService.actualizarTallasHabituales(eq(1L), any())).thenReturn(domain);
        when(usuarioMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(put(BASE_PATH + "/1/tallas-habituales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void guardarBusquedaShouldReturnCreated() throws Exception {
        String requestBody = """
                {
                  "nombre": "Mis Chaquetas",
                  "query": "chaqueta",
                  "tipo": "SUPERIOR",
                  "color": "Negro",
                  "precioMin": 10000,
                  "precioMax": 90000
                }
                """;
        BusquedaGuardada domain = BusquedaGuardada.builder().nombre("Mis Chaquetas").build();
        BusquedaGuardada guardada = BusquedaGuardada.builder().id(5L).nombre("Mis Chaquetas").build();
        BusquedaGuardadaResponseDTO response = BusquedaGuardadaResponseDTO.builder().id(5L).nombre("Mis Chaquetas").build();

        when(busquedaGuardadaMapper.toDomain(any(GuardarBusquedaRequestDTO.class))).thenReturn(domain);
        when(usuarioService.guardarBusqueda(eq(1L), any())).thenReturn(guardada);
        when(busquedaGuardadaMapper.toResponse(guardada)).thenReturn(response);

        mockMvc.perform(post(BASE_PATH + "/1/busquedas-guardadas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.nombre").value("Mis Chaquetas"));
    }

    @Test
    void obtenerBusquedasGuardadasShouldReturnList() throws Exception {
        BusquedaGuardada domain = BusquedaGuardada.builder().id(5L).nombre("Mis Chaquetas").build();
        BusquedaGuardadaResponseDTO response = BusquedaGuardadaResponseDTO.builder().id(5L).nombre("Mis Chaquetas").build();

        when(usuarioService.obtenerBusquedasGuardadas(1L)).thenReturn(List.of(domain));
        when(busquedaGuardadaMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get(BASE_PATH + "/1/busquedas-guardadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5L));
    }

    @Test
    void eliminarBusquedaGuardadaShouldReturnNoContent() throws Exception {
        doNothing().when(usuarioService).eliminarBusquedaGuardada(1L, 5L);

        mockMvc.perform(delete(BASE_PATH + "/1/busquedas-guardadas/5"))
                .andExpect(status().isNoContent());

        verify(usuarioService).eliminarBusquedaGuardada(1L, 5L);
    }
}
