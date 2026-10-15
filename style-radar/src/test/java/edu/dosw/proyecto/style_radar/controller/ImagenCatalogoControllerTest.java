package edu.dosw.proyecto.style_radar.controller;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.dosw.proyecto.style_radar.exception.GlobalExceptionHandler;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.ImagenCatalogoMapper;
import edu.dosw.proyecto.style_radar.model.domain.ImagenCatalogo;
import edu.dosw.proyecto.style_radar.model.dto.response.ImagenCatalogoResponseDTO;
import edu.dosw.proyecto.style_radar.service.IImagenCatalogoService;

@ExtendWith(MockitoExtension.class)
class ImagenCatalogoControllerTest {

    private static final String NIT = "900123456";
    private static final String PATH = "/api/v1/almacenes/" + NIT + "/catalogo/10/imagenes";

    @Mock
    private IImagenCatalogoService imagenService;

    @Mock
    private ImagenCatalogoMapper imagenMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ImagenCatalogoController(imagenService, imagenMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registrarShouldReturnCreatedForValidImage() throws Exception {
        // Arrange
        byte[] datos = { 1, 2, 3 };
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.png", "image/png", datos);
        ImagenCatalogo imagen = new ImagenCatalogo(20L, "image/png", datos, 10L);
        ImagenCatalogoResponseDTO response = new ImagenCatalogoResponseDTO(20L, 10L, "image/png");
        when(imagenService.registrar(eq(NIT), eq(10L), eq("image/png"), any(byte[].class))).thenReturn(imagen);
        when(imagenMapper.toResponse(imagen)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(multipart(PATH).file(archivo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20L))
                .andExpect(jsonPath("$.itemCatalogoId").value(10L))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.datos").doesNotExist());
    }

    @Test
    void registrarShouldReturnUnprocessableEntityForEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.png", "image/png", new byte[0]);
        when(imagenService.registrar(eq(NIT), eq(10L), eq("image/png"), any(byte[].class)))
                .thenThrow(new ReglaDeNegocioException("Archivo vacío"));

        // Act & Assert
        mockMvc.perform(multipart(PATH).file(archivo))
                .andExpect(status().isUnprocessableEntity());
        verify(imagenService).registrar(eq(NIT), eq(10L), eq("image/png"), any(byte[].class));
    }

    @Test
    void registrarShouldReturnUnprocessableEntityForNonImageFile() throws Exception {
        // Arrange
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "documento.pdf", "application/pdf", new byte[] { 1 });
        when(imagenService.registrar(eq(NIT), eq(10L), eq("application/pdf"), any(byte[].class)))
                .thenThrow(new ReglaDeNegocioException("No es una imagen"));

        // Act & Assert
        mockMvc.perform(multipart(PATH).file(archivo))
                .andExpect(status().isUnprocessableEntity());
        verify(imagenService).registrar(eq(NIT), eq(10L), eq("application/pdf"), any(byte[].class));
    }

    @Test
    void registrarShouldConvertFileReadFailureToTechnicalException() throws Exception {
        // Arrange
        MultipartFile archivo = org.mockito.Mockito.mock(MultipartFile.class);
        when(archivo.getBytes()).thenThrow(new IOException("disk detail"));

        // Act & Assert
        assertThatThrownBy(() -> new ImagenCatalogoController(imagenService, imagenMapper)
                .registrar(NIT, 10L, archivo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No fue posible procesar la fotografía recibida")
                .hasCauseInstanceOf(IOException.class);
    }
}
