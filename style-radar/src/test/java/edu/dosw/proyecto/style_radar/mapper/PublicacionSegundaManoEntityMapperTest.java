package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

@SpringBootTest
@ActiveProfiles("test")
class PublicacionSegundaManoEntityMapperTest {

    @Autowired
    private PublicacionSegundaManoEntityMapper mapper;

    @Test
    void shouldMapDomainToEntity() {
        Prenda prenda = new Prenda(1L, "Pantalón", "Dril", TipoPrenda.INFERIOR, "Studio F", "Beige", Estilo.CASUAL);
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder()
                .id(5L)
                .usuarioId(10L)
                .prenda(prenda)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(45000.0)
                .estado(EstadoPublicacion.DISPONIBLE)
                .fechaPublicacion(Instant.parse("2026-09-01T10:00:00Z"))
                .fotos(List.of("http://foto.jpg"))
                .build();

        PublicacionSegundaManoEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(5L);
        assertThat(entity.getPrecio()).isEqualTo(45000.0);
        assertThat(entity.getEstadoConservacion()).isEqualTo(EstadoConservacion.COMO_NUEVO);
        assertThat(entity.getPrenda().getNombre()).isEqualTo("Pantalón");
    }

    @Test
    void shouldMapEntityToDomain() {
        UsuarioEntity usuario = UsuarioEntity.builder().id(10L).build();
        PrendaEntity prenda = new PrendaEntity(1L, "Pantalón", "Dril", TipoPrenda.INFERIOR, "Studio F", "Beige", Estilo.CASUAL);
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .id(5L)
                .usuario(usuario)
                .prenda(prenda)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(45000.0)
                .estado(EstadoPublicacion.DISPONIBLE)
                .fechaPublicacion(Instant.parse("2026-09-01T10:00:00Z"))
                .fotos(List.of("http://foto.jpg"))
                .build();

        PublicacionSegundaMano domain = mapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(5L);
        assertThat(domain.getUsuarioId()).isEqualTo(10L);
        assertThat(domain.getPrenda().getNombre()).isEqualTo("Pantalón");
    }
}
