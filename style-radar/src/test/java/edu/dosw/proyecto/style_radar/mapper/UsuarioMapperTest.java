package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.dto.request.RegistrarUsuarioRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.UsuarioResponseDTO;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = Mappers.getMapper(UsuarioMapper.class);

    @Test
    void shouldMapRequestToDomain() {
        RegistrarUsuarioRequestDTO request = RegistrarUsuarioRequestDTO.builder()
                .nombre("Laura Gómez")
                .email("laura@example.com")
                .telefono("3001234567")
                .build();

        Usuario domain = mapper.toDomain(request);

        assertThat(domain).isNotNull();
        assertThat(domain.getNombre()).isEqualTo("Laura Gómez");
        assertThat(domain.getEmail()).isEqualTo("laura@example.com");
        assertThat(domain.getTelefono()).isEqualTo("3001234567");
    }

    @Test
    void shouldMapDomainToResponse() {
        Usuario domain = Usuario.builder()
                .id(1L)
                .nombre("Laura Gómez")
                .email("laura@example.com")
                .telefono("3001234567")
                .fechaRegistro(Instant.parse("2026-09-01T10:00:00Z"))
                .preferenciasEstilo(Set.of(Estilo.CASUAL, Estilo.URBANO))
                .tallasHabituales(Set.of(Talla.M, Talla.L))
                .build();

        UsuarioResponseDTO response = mapper.toResponse(domain);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("Laura Gómez");
        assertThat(response.getEmail()).isEqualTo("laura@example.com");
        assertThat(response.getPreferenciasEstilo()).containsExactlyInAnyOrder(Estilo.CASUAL, Estilo.URBANO);
        assertThat(response.getTallasHabituales()).containsExactlyInAnyOrder(Talla.M, Talla.L);
    }
}
