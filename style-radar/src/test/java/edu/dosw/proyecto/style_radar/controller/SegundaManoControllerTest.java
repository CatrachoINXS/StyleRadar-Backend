package edu.dosw.proyecto.style_radar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import edu.dosw.proyecto.style_radar.exception.GlobalExceptionHandler;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.PrendaMapper;
import edu.dosw.proyecto.style_radar.mapper.PublicacionSegundaManoMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.PrendaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PublicacionSegundaManoResponseDTO;
import edu.dosw.proyecto.style_radar.service.ISegundaManoService;

@ExtendWith(MockitoExtension.class)
class SegundaManoControllerTest {

    @Mock
    private ISegundaManoService segundaManoService;

    @Mock
    private PublicacionSegundaManoMapper publicacionMapper;

    @Mock
    private PrendaMapper prendaMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SegundaManoController(segundaManoService, publicacionMapper, prendaMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void publicarShouldReturnCreatedWhenValid() throws Exception {
        String requestBody = """
                {
                  "usuarioId": 1,
                  "prenda": {
                    "nombre": "Chaqueta Cuero",
                    "descripcion": "Vintage",
                    "tipo": "SUPERIOR",
                    "marca": "Zara",
                    "color": "Negro",
                    "estilo": "URBANO"
                  },
                  "talla": "M",
                  "estadoConservacion": "BUEN_ESTADO",
                  "precio": 80000,
                  "fotos": ["http://foto.jpg"]
                }
                """;

        Prenda prenda = new Prenda(1L, "Chaqueta Cuero", "Vintage", TipoPrenda.SUPERIOR, "Zara", "Negro", Estilo.URBANO);
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).build();
        PublicacionSegundaManoResponseDTO response = PublicacionSegundaManoResponseDTO.builder().id(10L).precio(80000.0).build();

        when(prendaMapper.toDomain(any(PrendaRequestDTO.class))).thenReturn(prenda);
        when(segundaManoService.publicar(eq(1L), eq(prenda), eq(Talla.M), eq(EstadoConservacion.BUEN_ESTADO), eq(80000.0), any())).thenReturn(domain);
        when(publicacionMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(post("/api/v1/segunda-mano")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.precio").value(80000.0));
    }

    @Test
    void obtenerPorIdShouldReturnOkWhenFound() throws Exception {
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).build();
        PublicacionSegundaManoResponseDTO response = PublicacionSegundaManoResponseDTO.builder().id(10L).build();

        when(segundaManoService.obtenerPorId(10L)).thenReturn(domain);
        when(publicacionMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get("/api/v1/segunda-mano/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void buscarShouldReturnPage() throws Exception {
        BusquedaSegundaManoCriteria criteria = BusquedaSegundaManoCriteria.builder().q("cuero").build();
        Page<PublicacionSegundaMano> pagedDomain = new PageImpl<>(List.of());
        PageResponseDTO<PublicacionSegundaManoResponseDTO> pageResponse = new PageResponseDTO<>(List.of(), 0, 20, 0, 0, true, true);

        when(publicacionMapper.toCriteria(any(BusquedaSegundaManoRequestDTO.class))).thenReturn(criteria);
        when(segundaManoService.buscar(eq(criteria), eq(0), eq(20))).thenReturn(pagedDomain);
        when(publicacionMapper.toPageResponse(pagedDomain)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/segunda-mano?q=cuero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void obtenerPorUsuarioShouldReturnList() throws Exception {
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).build();
        PublicacionSegundaManoResponseDTO response = PublicacionSegundaManoResponseDTO.builder().id(10L).build();

        when(segundaManoService.obtenerPorUsuario(1L)).thenReturn(List.of(domain));
        when(publicacionMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get("/api/v1/usuarios/1/segunda-mano"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void editarShouldReturnUpdated() throws Exception {
        String requestBody = """
                {
                  "prenda": {
                    "nombre": "Chaqueta Cuero Mod",
                    "descripcion": "Vintage",
                    "tipo": "SUPERIOR",
                    "marca": "Zara",
                    "color": "Negro",
                    "estilo": "URBANO"
                  },
                  "talla": "L",
                  "estadoConservacion": "COMO_NUEVO",
                  "precio": 90000,
                  "fotos": []
                }
                """;

        Prenda prenda = new Prenda(1L, "Chaqueta Cuero Mod", "Vintage", TipoPrenda.SUPERIOR, "Zara", "Negro", Estilo.URBANO);
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).build();
        PublicacionSegundaManoResponseDTO response = PublicacionSegundaManoResponseDTO.builder().id(10L).precio(90000.0).build();

        when(prendaMapper.toDomain(any(PrendaRequestDTO.class))).thenReturn(prenda);
        when(segundaManoService.editar(eq(1L), eq(10L), eq(prenda), eq(Talla.L), eq(EstadoConservacion.COMO_NUEVO), eq(90000.0), any())).thenReturn(domain);
        when(publicacionMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(put("/api/v1/usuarios/1/segunda-mano/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void marcarComoVendidaShouldReturnOk() throws Exception {
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).estado(EstadoPublicacion.VENDIDA).build();
        PublicacionSegundaManoResponseDTO response = PublicacionSegundaManoResponseDTO.builder().id(10L).estado(EstadoPublicacion.VENDIDA).build();

        when(segundaManoService.marcarComoVendida(1L, 10L)).thenReturn(domain);
        when(publicacionMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/usuarios/1/segunda-mano/10/vender"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VENDIDA"));
    }

    @Test
    void retirarShouldReturnNoContent() throws Exception {
        doNothing().when(segundaManoService).retirar(1L, 10L);

        mockMvc.perform(delete("/api/v1/usuarios/1/segunda-mano/10"))
                .andExpect(status().isNoContent());

        verify(segundaManoService).retirar(1L, 10L);
    }
}
