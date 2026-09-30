package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

class UsuarioEntityMapperTest {

    private final UsuarioEntityMapper mapper = Mappers.getMapper(UsuarioEntityMapper.class);

    @Test
    void shouldMapDomainToEntity() {
        Usuario domain = Usuario.builder()
                .id(1L)
                .nombre("Carlos Pérez")
                .email("carlos@example.com")
                .telefono("3101234567")
                .fechaRegistro(Instant.parse("2026-09-01T10:00:00Z"))
                .preferenciasEstilo(Set.of(Estilo.FORMAL))
                .tallasHabituales(Set.of(Talla.L))
                .build();

        UsuarioEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getNombre()).isEqualTo("Carlos Pérez");
        assertThat(entity.getEmail()).isEqualTo("carlos@example.com");
        assertThat(entity.getPreferenciasEstilo()).containsExactly(Estilo.FORMAL);
        assertThat(entity.getTallasHabituales()).containsExactly(Talla.L);
    }

    @Test
    void shouldMapEntityToDomain() {
        UsuarioEntity entity = UsuarioEntity.builder()
                .id(1L)
                .nombre("Carlos Pérez")
                .email("carlos@example.com")
                .telefono("3101234567")
                .fechaRegistro(Instant.parse("2026-09-01T10:00:00Z"))
                .preferenciasEstilo(Set.of(Estilo.FORMAL))
                .tallasHabituales(Set.of(Talla.L))
                .build();

        Usuario domain = mapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getNombre()).isEqualTo("Carlos Pérez");
        assertThat(domain.getEmail()).isEqualTo("carlos@example.com");
    }
}
