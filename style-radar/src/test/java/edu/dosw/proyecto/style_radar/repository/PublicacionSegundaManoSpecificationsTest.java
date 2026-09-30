package edu.dosw.proyecto.style_radar.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.specification.PublicacionSegundaManoSpecifications;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PublicacionSegundaManoSpecificationsTest {

    private static final Instant FECHA = Instant.parse("2026-09-01T10:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PublicacionSegundaManoRepository repository;

    private UsuarioEntity usuario;

    @BeforeEach
    void setUp() {
        usuario = UsuarioEntity.builder()
                .nombre("Usuario Test")
                .email("test@styleradar.co")
                .telefono("3000000000")
                .fechaRegistro(FECHA)
                .preferenciasEstilo(new HashSet<>())
                .tallasHabituales(new HashSet<>())
                .busquedasGuardadas(new ArrayList<>())
                .build();
        entityManager.persist(usuario);
    }

    @Test
    void qShouldFindPartialMatchAndExcludeSoldOrRetired() {
        persistirPublicacion("Chaqueta Cuero", "Vintage 80s", TipoPrenda.SUPERIOR, "Zara", "Negro", Estilo.URBANO,
                Talla.L, EstadoConservacion.BUEN_ESTADO, 120000.0, EstadoPublicacion.DISPONIBLE);
        persistirPublicacion("Chaqueta Cuero", "Vendida", TipoPrenda.SUPERIOR, "Zara", "Negro", Estilo.URBANO,
                Talla.L, EstadoConservacion.BUEN_ESTADO, 120000.0, EstadoPublicacion.VENDIDA);

        Page<PublicacionSegundaManoEntity> result = repository.findAll(
                PublicacionSegundaManoSpecifications.conCriterios(BusquedaSegundaManoCriteria.builder().q("cuero").build()),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getEstado()).isEqualTo(EstadoPublicacion.DISPONIBLE);
    }

    @Test
    void shouldFilterByTipoTallaEstadoConservacionAndPriceRange() {
        persistirPublicacion("Jean Slim", "Poco uso", TipoPrenda.INFERIOR, "Levis", "Azul", Estilo.URBANO,
                Talla.M, EstadoConservacion.COMO_NUEVO, 85000.0, EstadoPublicacion.DISPONIBLE);
        persistirPublicacion("Vestido Fiesta", "Seda", TipoPrenda.VESTIDO, "Mango", "Rojo", Estilo.FORMAL,
                Talla.S, EstadoConservacion.NUEVO_CON_ETIQUETA, 200000.0, EstadoPublicacion.DISPONIBLE);

        BusquedaSegundaManoCriteria criteria = BusquedaSegundaManoCriteria.builder()
                .tipo(TipoPrenda.INFERIOR)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precioMin(50000.0)
                .precioMax(100000.0)
                .color("Azul")
                .marca("Levis")
                .estilo(Estilo.URBANO)
                .build();

        Page<PublicacionSegundaManoEntity> result = repository.findAll(
                PublicacionSegundaManoSpecifications.conCriterios(criteria),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getPrenda().getNombre()).isEqualTo("Jean Slim");
    }

    private void persistirPublicacion(
            String nombre,
            String descripcion,
            TipoPrenda tipo,
            String marca,
            String color,
            Estilo estilo,
            Talla talla,
            EstadoConservacion conservacion,
            Double precio,
            EstadoPublicacion estado) {
        PrendaEntity prenda = new PrendaEntity(null, nombre, descripcion, tipo, marca, color, estilo);
        entityManager.persist(prenda);

        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .usuario(usuario)
                .prenda(prenda)
                .talla(talla)
                .estadoConservacion(conservacion)
                .precio(precio)
                .estado(estado)
                .fechaPublicacion(FECHA)
                .fotos(List.of("http://foto.jpg"))
                .build();
        entityManager.persist(entity);
    }
}
