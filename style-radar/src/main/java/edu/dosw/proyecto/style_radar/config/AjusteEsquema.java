package edu.dosw.proyecto.style_radar.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AjusteEsquema implements ApplicationRunner {

    static final String RESTRICCION = "publicaciones_segunda_mano_estado_check";

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        String estados = Arrays.stream(EstadoPublicacion.values())
                .map(e -> "'" + e.name() + "'")
                .collect(Collectors.joining(", "));
        jdbc.execute("ALTER TABLE publicaciones_segunda_mano DROP CONSTRAINT IF EXISTS " + RESTRICCION);
        jdbc.execute("ALTER TABLE publicaciones_segunda_mano ADD CONSTRAINT " + RESTRICCION
                + " CHECK (estado IN (" + estados + "))");
        log.info("Restricción de estados de publicaciones actualizada: {}", estados);
    }
}
