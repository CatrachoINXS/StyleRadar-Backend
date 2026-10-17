package edu.dosw.proyecto.style_radar.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;
import edu.dosw.proyecto.style_radar.repository.FundacionRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.PlanSuscripcionRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "styleradar.demo.enabled=true",
        "styleradar.demo.password=demo-test-123" })
@ActiveProfiles("test")
class DatosDemoTest {

    @Autowired
    private DatosDemo datosDemo;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private CredencialRepository credenciales;
    @Autowired
    private AlmacenRepository almacenes;
    @Autowired
    private ItemCatalogoRepository items;
    @Autowired
    private PlanSuscripcionRepository planes;
    @Autowired
    private FundacionRepository fundaciones;

    @Test
    void shouldLoadDemoDataOnStartup() {
        // Assert
        assertThat(almacenes.count()).isEqualTo(3);
        assertThat(items.count()).isEqualTo(10);
        assertThat(planes.count()).isEqualTo(3);
        assertThat(fundaciones.count()).isEqualTo(1);
        assertThat(usuarios.findByEmail("admin@styleradar.co")).isPresent();
        assertThat(usuarios.findByEmail("comprador@styleradar.co")).isPresent();
        assertThat(usuarios.findByEmail("comprador2@styleradar.co")).isPresent();
        assertThat(usuarios.findByEmail("almacen@styleradar.co")).isPresent();
        assertThat(usuarios.findByEmail("fundacion@styleradar.co")).isPresent();
        assertThat(credenciales.count()).isEqualTo(5);
    }

    @Test
    void shouldNotDuplicateDataWhenRunAgain() {
        // Act
        datosDemo.run(new DefaultApplicationArguments());

        // Assert
        assertThat(almacenes.count()).isEqualTo(3);
        assertThat(items.count()).isEqualTo(10);
        assertThat(usuarios.count()).isEqualTo(5);
    }

    @Test
    void shouldSkipWhenPasswordIsMissing() {
        // Arrange
        ReflectionTestUtils.setField(datosDemo, "password", " ");
        try {
            // Act
            datosDemo.run(new DefaultApplicationArguments());

            // Assert
            assertThat(usuarios.count()).isEqualTo(5);
        } finally {
            ReflectionTestUtils.setField(datosDemo, "password", "demo-test-123");
        }
    }

    @Test
    void shouldGeneratePngImage() {
        // Act
        byte[] imagen = DatosDemo.imagen(Color.RED);

        // Assert
        assertThat(imagen).isNotEmpty();
        assertThat(imagen[1]).isEqualTo((byte) 'P');
    }
}
