package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;

class PrendaEntityMapperTest {

    private final PrendaEntityMapper mapper = Mappers.getMapper(PrendaEntityMapper.class);

    @Test
    void shouldMapDomainToEntity() {
        // Arrange
        Prenda prenda = new Prenda(
                1L,
                "Camiseta",
                "Camiseta de algodón",
                TipoPrenda.SUPERIOR,
                "StyleRadar",
                "Negro",
                Estilo.CASUAL);

        // Act
        PrendaEntity entity = mapper.toEntity(prenda);

        // Assert
        assertThat(entity).usingRecursiveComparison().isEqualTo(prenda);
    }

    @Test
    void shouldMapEntityToDomain() {
        // Arrange
        PrendaEntity entity = new PrendaEntity(
                2L,
                "Zapatos",
                "Zapatos deportivos",
                TipoPrenda.CALZADO,
                "StyleRadar",
                "Blanco",
                Estilo.DEPORTIVO);

        // Act
        Prenda prenda = mapper.toDomain(entity);

        // Assert
        assertThat(prenda).usingRecursiveComparison().isEqualTo(entity);
    }
}
