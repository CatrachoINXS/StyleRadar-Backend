package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.ImagenCatalogo;
import edu.dosw.proyecto.style_radar.model.dto.response.ImagenCatalogoResponseDTO;

class ImagenCatalogoMapperTest {

    private final ImagenCatalogoMapper mapper = Mappers.getMapper(ImagenCatalogoMapper.class);

    @Test
    void shouldExposeImageMetadataWithoutBinaryContent() {
        // Arrange
        ImagenCatalogo imagen = new ImagenCatalogo(20L, "image/png", new byte[] { 1, 2, 3 }, 10L);

        // Act
        ImagenCatalogoResponseDTO result = mapper.toResponse(imagen);

        // Assert
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getItemCatalogoId()).isEqualTo(10L);
        assertThat(result.getContentType()).isEqualTo("image/png");
        assertThat(ImagenCatalogoResponseDTO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("datos");
    }
}
