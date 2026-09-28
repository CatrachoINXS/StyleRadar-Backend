package edu.dosw.proyecto.style_radar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import edu.dosw.proyecto.style_radar.mapper.CatalogoItemMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.dto.request.CatalogoItemRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.service.ICatalogoService;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    private static final String NIT = "900123456";
    private static final String BASE_PATH = "/api/v1/almacenes/" + NIT + "/catalogo";
    private static final String VALID_REQUEST = """
            {
              "nombre": "Camiseta",
              "descripcion": "Camiseta de algodón",
              "tipo": "SUPERIOR",
              "marca": "StyleRadar",
              "color": "Negro",
              "estilo": "CASUAL",
              "precio": 89000
            }
            """;

    @Mock
    private ICatalogoService catalogoService;

    @Mock
    private CatalogoItemMapper catalogoItemMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CatalogoController(catalogoService, catalogoItemMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void publicarShouldReturnCreatedForValidRequest() throws Exception {
        // Arrange
        Prenda prenda = prenda();
        ItemCatalogo item = item();
        CatalogoItemResponseDTO response = response();
        when(catalogoItemMapper.toPrenda(any(CatalogoItemRequestDTO.class))).thenReturn(prenda);
        when(catalogoService.publicar(NIT, prenda, 89000.0)).thenReturn(item);
        when(catalogoItemMapper.toResponse(item)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").value(10L))
                .andExpect(jsonPath("$.prendaId").value(5L));
        verify(catalogoService).publicar(NIT, prenda, 89000.0);
    }

    @Test
    void publicarShouldReturnBadRequestForInvalidRequest() throws Exception {
        // Arrange
        String invalidRequest = """
                {
                  "nombre": "",
                  "descripcion": "",
                  "tipo": null,
                  "marca": "",
                  "color": "",
                  "estilo": null,
                  "precio": 0
                }
                """;

        // Act & Assert
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.nombre").exists())
                .andExpect(jsonPath("$.validationErrors.precio").exists());
        verify(catalogoService, never()).publicar(any(), any(), any());
    }

    @Test
    void obtenerCatalogoShouldReturnOk() throws Exception {
        // Arrange
        ItemCatalogo item = item();
        when(catalogoService.obtenerCatalogo(NIT)).thenReturn(List.of(item));
        when(catalogoItemMapper.toResponse(item)).thenReturn(response());

        // Act & Assert
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(10L));
    }

    @Test
    void actualizarShouldReturnOkForValidRequest() throws Exception {
        // Arrange
        Prenda prenda = prenda();
        ItemCatalogo item = item();
        when(catalogoItemMapper.toPrenda(any(CatalogoItemRequestDTO.class))).thenReturn(prenda);
        when(catalogoService.actualizar(NIT, 10L, prenda, 89000.0)).thenReturn(item);
        when(catalogoItemMapper.toResponse(item)).thenReturn(response());

        // Act & Assert
        mockMvc.perform(put(BASE_PATH + "/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(10L));
        verify(catalogoService).actualizar(NIT, 10L, prenda, 89000.0);
    }

    @Test
    void retirarShouldReturnNoContent() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(delete(BASE_PATH + "/10"))
                .andExpect(status().isNoContent());
        verify(catalogoService).retirar(NIT, 10L);
    }

    @Test
    void obtenerCatalogoShouldReturnNotFound() throws Exception {
        // Arrange
        when(catalogoService.obtenerCatalogo(NIT))
                .thenThrow(new RecursoNoEncontradoException("Almacén no encontrado"));

        // Act & Assert
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void actualizarShouldReturnNotFound() throws Exception {
        // Arrange
        Prenda prenda = prenda();
        when(catalogoItemMapper.toPrenda(any(CatalogoItemRequestDTO.class))).thenReturn(prenda);
        when(catalogoService.actualizar(eq(NIT), eq(10L), eq(prenda), eq(89000.0)))
                .thenThrow(new RecursoNoEncontradoException("Item no encontrado"));

        // Act & Assert
        mockMvc.perform(put(BASE_PATH + "/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void retirarShouldReturnNotFound() throws Exception {
        // Arrange
        org.mockito.Mockito.doThrow(new RecursoNoEncontradoException("Item no encontrado"))
                .when(catalogoService).retirar(NIT, 10L);

        // Act & Assert
        mockMvc.perform(delete(BASE_PATH + "/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private Prenda prenda() {
        return new Prenda(5L, "Camiseta", "Camiseta de algodón", TipoPrenda.SUPERIOR,
                "StyleRadar", "Negro", Estilo.CASUAL);
    }

    private ItemCatalogo item() {
        return new ItemCatalogo(10L, 89000.0, EstadoItem.AGOTADA, prenda(), NIT, new ArrayList<>());
    }

    private CatalogoItemResponseDTO response() {
        return new CatalogoItemResponseDTO(10L, 5L, NIT, "Camiseta", "Camiseta de algodón",
                TipoPrenda.SUPERIOR, "StyleRadar", "Negro", Estilo.CASUAL, 89000.0, 0,
                EstadoItem.AGOTADA, List.of());
    }
}
