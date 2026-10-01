package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Playlist;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist;
import edu.dosw.proyecto.style_radar.model.entity.PlaylistEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

@SpringBootTest
@ActiveProfiles("test")
class PlaylistEntityMapperTest {

    @Autowired
    private PlaylistEntityMapper mapper;

    @Test
    void shouldMapDomainToEntity() {
        Prenda prenda = new Prenda(1L, "Camisa Blanca", "Lino", TipoPrenda.SUPERIOR, "Arturo Calle", "Blanco", Estilo.FORMAL);
        Playlist domain = Playlist.builder()
                .id(100L)
                .usuarioId(2L)
                .nombre("Outfits de Verano")
                .descripcion("Looks frescos")
                .visibilidad(VisibilidadPlaylist.PUBLICA)
                .estilo(Estilo.FORMAL)
                .fechaCreacion(Instant.parse("2026-09-01T10:00:00Z"))
                .prendas(List.of(prenda))
                .likesUsuarios(Set.of(1L))
                .build();

        PlaylistEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(100L);
        assertThat(entity.getNombre()).isEqualTo("Outfits de Verano");
        assertThat(entity.getPrendas().getFirst().getNombre()).isEqualTo("Camisa Blanca");
    }

    @Test
    void shouldMapEntityToDomain() {
        UsuarioEntity usuario = UsuarioEntity.builder().id(2L).build();
        PrendaEntity prenda = new PrendaEntity(1L, "Camisa Blanca", "Lino", TipoPrenda.SUPERIOR, "Arturo Calle", "Blanco", Estilo.FORMAL);
        PlaylistEntity entity = PlaylistEntity.builder()
                .id(100L)
                .usuario(usuario)
                .nombre("Outfits de Verano")
                .descripcion("Looks frescos")
                .visibilidad(VisibilidadPlaylist.PUBLICA)
                .estilo(Estilo.FORMAL)
                .fechaCreacion(Instant.parse("2026-09-01T10:00:00Z"))
                .prendas(List.of(prenda))
                .likesUsuarios(Set.of(1L))
                .build();

        Playlist domain = mapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(100L);
        assertThat(domain.getUsuarioId()).isEqualTo(2L);
        assertThat(domain.getLikesCount()).isEqualTo(1);
    }
}
