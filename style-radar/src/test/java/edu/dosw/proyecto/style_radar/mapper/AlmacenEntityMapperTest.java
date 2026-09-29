package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.Almacen;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;

class AlmacenEntityMapperTest {

    private final AlmacenEntityMapper mapper = Mappers.getMapper(AlmacenEntityMapper.class);

    @Test
    void shouldMapLocationAndReputationToEntity() {
        Almacen almacen = new Almacen(
                "900123456",
                "StyleRadar",
                "Moda",
                "3000000000",
                "contacto@styleradar.co",
                4.7110,
                -74.0721,
                4.8);

        AlmacenEntity entity = mapper.toEntity(almacen);

        assertThat(entity.getLatitud()).isEqualTo(4.7110);
        assertThat(entity.getLongitud()).isEqualTo(-74.0721);
        assertThat(entity.getReputacion()).isEqualTo(4.8);
    }

    @Test
    void shouldMapNullableLocationAndReputationToDomain() {
        AlmacenEntity entity = new AlmacenEntity();
        entity.setNit("900123456");

        Almacen almacen = mapper.toDomain(entity);

        assertThat(almacen.getLatitud()).isNull();
        assertThat(almacen.getLongitud()).isNull();
        assertThat(almacen.getReputacion()).isNull();
    }
}
