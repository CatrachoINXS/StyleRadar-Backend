package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.OrdenCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaCatalogoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;

@ExtendWith(MockitoExtension.class)
class BusquedaCatalogoMapperTest {

    @Mock
    private CatalogoItemMapper catalogoItemMapper;

    @InjectMocks
    private BusquedaCatalogoMapper mapper;

    @Test
    void shouldNormalizeTextAndMapEverySearchCriterion() {
        // Arrange
        BusquedaCatalogoRequestDTO request = new BusquedaCatalogoRequestDTO(
                "  camiseta  ", TipoPrenda.SUPERIOR, "  Negro ", Talla.M,
                10000.0, 90000.0, "  StyleRadar  ", Estilo.CASUAL,
                OrdenCatalogo.DISTANCIA, 4.711, -74.0721, 2, 25);

        // Act
        BusquedaCatalogoCriteria result = mapper.toCriteria(request);

        // Assert
        assertThat(result.getQ()).isEqualTo("camiseta");
        assertThat(result.getTipo()).isEqualTo(TipoPrenda.SUPERIOR);
        assertThat(result.getColor()).isEqualTo("Negro");
        assertThat(result.getTalla()).isEqualTo(Talla.M);
        assertThat(result.getPrecioMin()).isEqualTo(10000.0);
        assertThat(result.getPrecioMax()).isEqualTo(90000.0);
        assertThat(result.getMarca()).isEqualTo("StyleRadar");
        assertThat(result.getEstilo()).isEqualTo(Estilo.CASUAL);
        assertThat(result.getOrden()).isEqualTo(OrdenCatalogo.DISTANCIA);
        assertThat(result.getLatitudUsuario()).isEqualTo(4.711);
        assertThat(result.getLongitudUsuario()).isEqualTo(-74.0721);
    }

    @Test
    void shouldPreserveNullOptionalText() {
        // Arrange
        BusquedaCatalogoRequestDTO request = new BusquedaCatalogoRequestDTO();

        // Act
        BusquedaCatalogoCriteria result = mapper.toCriteria(request);

        // Assert
        assertThat(result.getQ()).isNull();
        assertThat(result.getColor()).isNull();
        assertThat(result.getMarca()).isNull();
    }

    @Test
    void shouldMapContentAndPaginationMetadata() {
        // Arrange
        ItemCatalogo item = new ItemCatalogo();
        CatalogoItemResponseDTO mappedItem = CatalogoItemResponseDTO.builder().itemId(10L).build();
        Page<ItemCatalogo> page = new PageImpl<>(List.of(item), PageRequest.of(1, 2), 5);
        when(catalogoItemMapper.toResponse(item)).thenReturn(mappedItem);

        // Act
        PageResponseDTO<CatalogoItemResponseDTO> result = mapper.toResponse(page);

        // Assert
        assertThat(result.getContent()).containsExactly(mappedItem);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
        verify(catalogoItemMapper).toResponse(item);
    }
}
