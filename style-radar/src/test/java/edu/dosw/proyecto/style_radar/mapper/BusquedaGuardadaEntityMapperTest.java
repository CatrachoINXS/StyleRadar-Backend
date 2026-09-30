package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.BusquedaGuardadaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

class BusquedaGuardadaEntityMapperTest {

    private final BusquedaGuardadaEntityMapper mapper = Mappers.getMapper(BusquedaGuardadaEntityMapper.class);

    @Test
    void shouldMapDomainToEntity() {
        BusquedaGuardada domain = BusquedaGuardada.builder()
                .id(10L)
                .nombre("Vestidos Elegantes")
                .query("vestido")
                .tipo(TipoPrenda.VESTIDO)
                .color("rojo")
                .talla(Talla.S)
                .precioMin(50000.0)
                .precioMax(150000.0)
                .marca("Mango")
                .estilo(Estilo.FORMAL)
                .fechaCreacion(Instant.parse("2026-09-01T12:00:00Z"))
                .build();

        BusquedaGuardadaEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getNombre()).isEqualTo("Vestidos Elegantes");
        assertThat(entity.getTipo()).isEqualTo(TipoPrenda.VESTIDO);
        assertThat(entity.getColor()).isEqualTo("rojo");
    }

    @Test
    void shouldMapEntityToDomain() {
        UsuarioEntity usuario = UsuarioEntity.builder().id(1L).build();
        BusquedaGuardadaEntity entity = BusquedaGuardadaEntity.builder()
                .id(10L)
                .nombre("Vestidos Elegantes")
                .query("vestido")
                .tipo(TipoPrenda.VESTIDO)
                .color("rojo")
                .talla(Talla.S)
                .precioMin(50000.0)
                .precioMax(150000.0)
                .marca("Mango")
                .estilo(Estilo.FORMAL)
                .fechaCreacion(Instant.parse("2026-09-01T12:00:00Z"))
                .usuario(usuario)
                .build();

        BusquedaGuardada domain = mapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(10L);
        assertThat(domain.getUsuarioId()).isEqualTo(1L);
        assertThat(domain.getNombre()).isEqualTo("Vestidos Elegantes");
    }
}
