package edu.dosw.proyecto.style_radar.config;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class AjusteEsquemaTest {

    @Test
    void runReemplazaLaRestriccionConTodosLosEstadosDePublicacion() {
        // Arrange
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AjusteEsquema ajuste = new AjusteEsquema(jdbc);

        // Act
        ajuste.run(new DefaultApplicationArguments());

        // Assert
        InOrder orden = inOrder(jdbc);
        orden.verify(jdbc).execute("ALTER TABLE publicaciones_segunda_mano DROP CONSTRAINT IF EXISTS "
                + "publicaciones_segunda_mano_estado_check");
        orden.verify(jdbc).execute("ALTER TABLE publicaciones_segunda_mano ADD CONSTRAINT "
                + "publicaciones_segunda_mano_estado_check CHECK (estado IN "
                + "('DISPONIBLE', 'VENDIDA', 'RETIRADA', 'EN_DONACION', 'DONADA'))");
    }
}
