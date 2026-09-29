package edu.dosw.proyecto.style_radar.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DistanciaCalculatorTest {

    private final DistanciaCalculator calculator = new DistanciaCalculator();

    @Test
    void mismoPuntoDebeTenerDistanciaCero() {
        double distancia = calculator.calcularKm(4.7110, -74.0721, 4.7110, -74.0721);

        assertThat(distancia).isCloseTo(0.0, within(1.0e-9));
    }

    @Test
    void distanciaDebeSerSimetrica() {
        double distanciaAB = calculator.calcularKm(4.7110, -74.0721, 6.2442, -75.5812);
        double distanciaBA = calculator.calcularKm(6.2442, -75.5812, 4.7110, -74.0721);

        assertThat(distanciaAB).isCloseTo(distanciaBA, within(1.0e-9));
    }

    @Test
    void unGradoDeLongitudEnEcuadorDebeSerAproximadamenteCientoOnceKm() {
        double distancia = calculator.calcularKm(0.0, 0.0, 0.0, 1.0);

        assertThat(distancia).isCloseTo(111.2, within(0.2));
    }

    private org.assertj.core.data.Offset<Double> within(double tolerancia) {
        return org.assertj.core.data.Offset.offset(tolerancia);
    }
}
