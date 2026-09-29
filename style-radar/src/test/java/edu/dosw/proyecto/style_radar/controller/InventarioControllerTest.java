package edu.dosw.proyecto.style_radar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.CatalogoItemMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.InventarioTalla;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.service.IInventarioService;

@ExtendWith(MockitoExtension.class)
class InventarioControllerTest {

    private static final String NIT = "900123456";
    private static final String BASE_PATH = "/api/v1/almacenes/" + NIT + "/catalogo/10";

    @Mock
    private IInventarioService inventarioService;

    @Mock
    private CatalogoItemMapper catalogoItemMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InventarioController(inventarioService, catalogoItemMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registrarTallasShouldReturnOk() throws Exception {
        // Arrange
        ItemCatalogo item = item();
        when(inventarioService.registrarTallas(NIT, 10L, java.util.Set.of(Talla.M, Talla.L))).thenReturn(item);
        when(catalogoItemMapper.toResponse(item)).thenReturn(response());

        // Act & Assert
        mockMvc.perform(put(BASE_PATH + "/tallas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tallas\":[\"M\",\"L\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(4));
    }

    @Test
    void actualizarDisponibilidadShouldReturnOk() throws Exception {
        // Arrange
        ItemCatalogo item = item();
        when(inventarioService.actualizarDisponibilidad(NIT, 10L, Talla.M, 4)).thenReturn(item);
        when(catalogoItemMapper.toResponse(item)).thenReturn(response());

        // Act & Assert
        mockMvc.perform(patch(BASE_PATH + "/inventario/M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidades\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void actualizarDisponibilidadShouldReturnBadRequestForNegativeUnits() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(patch(BASE_PATH + "/inventario/M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidades\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.unidades").exists());
        verify(inventarioService, never()).actualizarDisponibilidad(any(), any(), any(), any());
    }

    @Test
    void registrarTallasShouldReturnNotFoundForMissingItem() throws Exception {
        // Arrange
        when(inventarioService.registrarTallas(NIT, 10L, java.util.Set.of(Talla.M)))
                .thenThrow(new RecursoNoEncontradoException("Item no encontrado"));

        // Act & Assert
        mockMvc.perform(put(BASE_PATH + "/tallas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tallas\":[\"M\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarDisponibilidadShouldReturnNotFoundForMissingItem() throws Exception {
        // Arrange
        when(inventarioService.actualizarDisponibilidad(NIT, 10L, Talla.M, 2))
                .thenThrow(new RecursoNoEncontradoException("Item no encontrado"));

        // Act & Assert
        mockMvc.perform(patch(BASE_PATH + "/inventario/M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidades\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarTallasShouldReturnUnprocessableEntityForBusinessRule() throws Exception {
        // Arrange
        when(inventarioService.registrarTallas(NIT, 10L, java.util.Set.of(Talla.M)))
                .thenThrow(new ReglaDeNegocioException("Talla con unidades"));

        // Act & Assert
        mockMvc.perform(put(BASE_PATH + "/tallas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tallas\":[\"M\"]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void actualizarDisponibilidadShouldReturnUnprocessableEntityForUnregisteredSize() throws Exception {
        // Arrange
        when(inventarioService.actualizarDisponibilidad(NIT, 10L, Talla.M, 2))
                .thenThrow(new ReglaDeNegocioException("Talla no registrada"));

        // Act & Assert
        mockMvc.perform(patch(BASE_PATH + "/inventario/M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidades\":2}"))
                .andExpect(status().isUnprocessableEntity());
    }

    private ItemCatalogo item() {
        return new ItemCatalogo(10L, 89000.0, EstadoItem.DISPONIBLE,
                Instant.parse("2026-09-01T10:00:00Z"), null, NIT,
                List.of(new InventarioTalla(Talla.M, 4)));
    }

    private CatalogoItemResponseDTO response() {
        CatalogoItemResponseDTO response = new CatalogoItemResponseDTO();
        response.setItemId(10L);
        response.setStock(4);
        response.setEstado(EstadoItem.DISPONIBLE);
        response.setTallasDisponibles(List.of(Talla.M));
        return response;
    }
}
