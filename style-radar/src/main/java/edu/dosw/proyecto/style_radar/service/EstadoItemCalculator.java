package edu.dosw.proyecto.style_radar.service;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EstadoItemCalculator {

    private static final int UMBRAL_ULTIMAS_UNIDADES = 3;
    private static final Duration PERIODO_NUEVA_PRENDA = Duration.ofDays(7);

    private final Clock clock;

    public EstadoItem calcular(ItemCatalogo item) {
        int stock = item.getStock();
        if (stock == 0) {
            return EstadoItem.AGOTADA;
        }
        if (stock <= UMBRAL_ULTIMAS_UNIDADES) {
            return EstadoItem.ULTIMAS_UNIDADES;
        }
        if (item.getFechaPublicacion().plus(PERIODO_NUEVA_PRENDA).isAfter(clock.instant())) {
            return EstadoItem.NUEVA_PRENDA;
        }
        return EstadoItem.DISPONIBLE;
    }
}
