package edu.dosw.proyecto.style_radar.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.InventarioTalla;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;

class EstadoItemCalculatorTest {

    private static final Instant AHORA = Instant.parse("2026-09-08T10:00:00Z");
    private final EstadoItemCalculator calculator = new EstadoItemCalculator(
            Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void stockZeroShouldBeExhausted() {
        assertThat(calculator.calcular(item(0, AHORA))).isEqualTo(EstadoItem.AGOTADA);
    }

    @Test
    void stockOneShouldBeLastUnits() {
        assertThat(calculator.calcular(item(1, AHORA))).isEqualTo(EstadoItem.ULTIMAS_UNIDADES);
    }

    @Test
    void stockThreeShouldBeLastUnits() {
        assertThat(calculator.calcular(item(3, AHORA))).isEqualTo(EstadoItem.ULTIMAS_UNIDADES);
    }

    @Test
    void stockFourPublishedNowShouldBeNew() {
        assertThat(calculator.calcular(item(4, AHORA))).isEqualTo(EstadoItem.NUEVA_PRENDA);
    }

    @Test
    void stockFourOneSecondBeforeSevenDaysShouldBeNew() {
        assertThat(calculator.calcular(item(4, AHORA.minusSeconds(7 * 24 * 60 * 60 - 1))))
                .isEqualTo(EstadoItem.NUEVA_PRENDA);
    }

    @Test
    void stockFourAtExactlySevenDaysShouldBeAvailable() {
        assertThat(calculator.calcular(item(4, AHORA.minusSeconds(7 * 24 * 60 * 60))))
                .isEqualTo(EstadoItem.DISPONIBLE);
    }

    @Test
    void stockFourAfterMoreThanSevenDaysShouldBeAvailable() {
        assertThat(calculator.calcular(item(4, AHORA.minusSeconds(8 * 24 * 60 * 60))))
                .isEqualTo(EstadoItem.DISPONIBLE);
    }

    @Test
    void lowStockShouldTakePrecedenceOverNewArrival() {
        assertThat(calculator.calcular(item(2, AHORA))).isEqualTo(EstadoItem.ULTIMAS_UNIDADES);
    }

    private ItemCatalogo item(int stock, Instant fechaPublicacion) {
        List<InventarioTalla> inventario = stock == 0
                ? List.of()
                : List.of(new InventarioTalla(Talla.M, stock));
        return new ItemCatalogo(null, 89000.0, null, fechaPublicacion, null, "900123456", inventario);
    }
}
