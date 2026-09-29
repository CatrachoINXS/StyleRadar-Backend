package edu.dosw.proyecto.style_radar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import edu.dosw.proyecto.style_radar.exception.GlobalExceptionHandler;
import edu.dosw.proyecto.style_radar.mapper.BusquedaCatalogoMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaCatalogoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.service.IBusquedaCatalogoService;

@ExtendWith(MockitoExtension.class)
class BusquedaCatalogoControllerTest {

    private static final String PATH = "/api/v1/catalogo/buscar";

    @Mock
    private IBusquedaCatalogoService service;

    @Mock
    private BusquedaCatalogoMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BusquedaCatalogoController(service, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        BusquedaCatalogoCriteria criteria = BusquedaCatalogoCriteria.builder().build();
        Page<ItemCatalogo> emptyPage = Page.empty(PageRequest.of(0, 20));
        PageResponseDTO<CatalogoItemResponseDTO> response = new PageResponseDTO<>(
                List.of(), 0, 20, 0, 0, true, true);
        lenient().when(mapper.toCriteria(any(BusquedaCatalogoRequestDTO.class))).thenReturn(criteria);
        lenient().when(service.buscar(any(BusquedaCatalogoCriteria.class), anyInt(), anyInt())).thenReturn(emptyPage);
        lenient().when(mapper.toResponse(any())).thenReturn(response);
    }

    @Test
    void withoutFiltersShouldReturnOk() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk());
        verify(service).buscar(any(BusquedaCatalogoCriteria.class), org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20));
    }

    @Test
    void validQShouldReturnOk() throws Exception {
        mockMvc.perform(get(PATH).param("q", "camiseta"))
                .andExpect(status().isOk());
    }

    @Test
    void explicitEmptyQShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("q", ""))
                .andExpect(status().isBadRequest());
        verify(service, never()).buscar(any(), anyInt(), anyInt());
    }

    @Test
    void invalidTipoShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("tipo", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTallaShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("talla", "INVALIDA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeMinimumPriceShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("precioMin", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeMaximumPriceShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("precioMax", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void minimumPriceGreaterThanMaximumShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("precioMin", "100001").param("precioMax", "100000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void explicitEmptyBrandShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("marca", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void explicitEmptyColorShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("color", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativePageShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zeroSizeShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sizeOverMaximumShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(PATH).param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noMatchesShouldReturnOkWithEmptyContent() throws Exception {
        mockMvc.perform(get(PATH).param("marca", "Inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void validResponseShouldContainPaginationMetadata() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
}
