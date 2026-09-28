package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.InventarioTalla;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.dto.request.CatalogoItemRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;

class CatalogoItemMapperTest {

    private final CatalogoItemMapper mapper = Mappers.getMapper(CatalogoItemMapper.class);

    @Test
    void shouldMapRequestToGarmentDomain() {
        // Arrange
        CatalogoItemRequestDTO request = new CatalogoItemRequestDTO(
                "Camiseta",
                "Camiseta de algodón",
                TipoPrenda.SUPERIOR,
                "StyleRadar",
                "Negro",
                Estilo.CASUAL,
                89000.0);

        // Act
        Prenda result = mapper.toPrenda(request);

        // Assert
        assertThat(result.getId()).isNull();
        assertThat(result.getNombre()).isEqualTo(request.getNombre());
        assertThat(result.getDescripcion()).isEqualTo(request.getDescripcion());
        assertThat(result.getTipo()).isEqualTo(request.getTipo());
        assertThat(result.getMarca()).isEqualTo(request.getMarca());
        assertThat(result.getColor()).isEqualTo(request.getColor());
        assertThat(result.getEstilo()).isEqualTo(request.getEstilo());
    }

    @Test
    void shouldMapCatalogItemToFlattenedResponse() {
        // Arrange
        Prenda prenda = new Prenda(5L, "Camiseta", "Camiseta de algodón", TipoPrenda.SUPERIOR,
                "StyleRadar", "Negro", Estilo.CASUAL);
        ItemCatalogo item = new ItemCatalogo(10L, 89000.0, EstadoItem.DISPONIBLE,
                prenda, "900123456", List.of(new InventarioTalla(Talla.S, 0), new InventarioTalla(Talla.M, 4)));

        // Act
        CatalogoItemResponseDTO result = mapper.toResponse(item);

        // Assert
        assertThat(result.getItemId()).isEqualTo(10L);
        assertThat(result.getPrendaId()).isEqualTo(5L);
        assertThat(result.getAlmacenNit()).isEqualTo("900123456");
        assertThat(result.getNombre()).isEqualTo("Camiseta");
        assertThat(result.getPrecio()).isEqualTo(89000.0);
        assertThat(result.getStock()).isEqualTo(4);
        assertThat(result.getEstado()).isEqualTo(EstadoItem.DISPONIBLE);
        assertThat(result.getTallasDisponibles()).containsExactly(Talla.M);
    }
}
