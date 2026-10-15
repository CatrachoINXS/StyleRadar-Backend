package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.dto.request.GuardarBusquedaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.BusquedaGuardadaResponseDTO;

class BusquedaGuardadaMapperTest {

    private final BusquedaGuardadaMapper mapper = Mappers.getMapper(BusquedaGuardadaMapper.class);

    @Test
    void shouldMapRequestToDomain() {
        GuardarBusquedaRequestDTO request = GuardarBusquedaRequestDTO.builder()
                .nombre("Camisetas Negras")
                .query("camiseta")
                .tipo(TipoPrenda.SUPERIOR)
                .color("negro")
                .talla(Talla.M)
                .precioMin(10000.0)
                .precioMax(50000.0)
                .marca("Zara")
                .estilo(Estilo.CASUAL)
                .build();

        BusquedaGuardada domain = mapper.toDomain(request);

        assertThat(domain).isNotNull();
        assertThat(domain.getNombre()).isEqualTo("Camisetas Negras");
        assertThat(domain.getQuery()).isEqualTo("camiseta");
        assertThat(domain.getTipo()).isEqualTo(TipoPrenda.SUPERIOR);
        assertThat(domain.getColor()).isEqualTo("negro");
        assertThat(domain.getTalla()).isEqualTo(Talla.M);
        assertThat(domain.getPrecioMin()).isEqualTo(10000.0);
        assertThat(domain.getPrecioMax()).isEqualTo(50000.0);
        assertThat(domain.getMarca()).isEqualTo("Zara");
        assertThat(domain.getEstilo()).isEqualTo(Estilo.CASUAL);
    }

    @Test
    void shouldMapDomainToResponse() {
        BusquedaGuardada domain = BusquedaGuardada.builder()
                .id(10L)
                .nombre("Camisetas Negras")
                .query("camiseta")
                .tipo(TipoPrenda.SUPERIOR)
                .color("negro")
                .talla(Talla.M)
                .precioMin(10000.0)
                .precioMax(50000.0)
                .marca("Zara")
                .estilo(Estilo.CASUAL)
                .fechaCreacion(Instant.parse("2026-09-01T12:00:00Z"))
                .usuarioId(1L)
                .build();

        BusquedaGuardadaResponseDTO response = mapper.toResponse(domain);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNombre()).isEqualTo("Camisetas Negras");
        assertThat(response.getQuery()).isEqualTo("camiseta");
        assertThat(response.getFechaCreacion()).isEqualTo(Instant.parse("2026-09-01T12:00:00Z"));
    }
}
