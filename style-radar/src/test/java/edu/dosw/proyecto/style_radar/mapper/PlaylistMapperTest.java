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
import edu.dosw.proyecto.style_radar.model.dto.response.PlaylistResponseDTO;

@SpringBootTest
@ActiveProfiles("test")
class PlaylistMapperTest {

    @Autowired
    private PlaylistMapper mapper;

    @Test
    void shouldMapPlaylistToResponse() {
        Prenda prenda = new Prenda(1L, "Camisa Blanca", "Lino", TipoPrenda.SUPERIOR, "Arturo Calle", "Blanco", Estilo.FORMAL);
        Playlist playlist = Playlist.builder()
                .id(100L)
                .usuarioId(2L)
                .nombre("Outfits de Verano")
                .descripcion("Looks frescos")
                .visibilidad(VisibilidadPlaylist.PUBLICA)
                .estilo(Estilo.FORMAL)
                .fechaCreacion(Instant.parse("2026-09-01T10:00:00Z"))
                .prendas(List.of(prenda))
                .likesUsuarios(Set.of(1L, 3L))
                .build();

        PlaylistResponseDTO response = mapper.toResponse(playlist);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getNombre()).isEqualTo("Outfits de Verano");
        assertThat(response.getLikes()).isEqualTo(2);
        assertThat(response.getTotalPrendas()).isEqualTo(1);
        assertThat(response.getPrendas().getFirst().getNombre()).isEqualTo("Camisa Blanca");
    }
}
