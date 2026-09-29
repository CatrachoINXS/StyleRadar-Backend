package edu.dosw.proyecto.style_radar.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.dosw.proyecto.style_radar.model.domain.Prenda;

class SimilaridadPrendaCalculatorTest {

    private SimilaridadPrendaCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SimilaridadPrendaCalculator();
    }

    @Test
    void todosLosTokensPresentesDebeRetornarUno() {
        assertThat(calculator.calcular("camiseta negra", prenda("Camiseta básica", "Negra")))
                .isEqualTo(1.0);
    }

    @Test
    void unoDeDosTokensDebeRetornarMedio() {
        assertThat(calculator.calcular("camiseta negra", prenda("Camiseta básica", "Azul")))
                .isEqualTo(0.5);
    }

    @Test
    void ningunTokenDebeRetornarCero() {
        assertThat(calculator.calcular("pantalón blanco", prenda("Camiseta básica", "Negra")))
                .isZero();
    }

    @Test
    void debeIgnorarMayusculasYMinusculas() {
        assertThat(calculator.calcular("CAMISETA", prenda("camiseta", "Negra")))
                .isEqualTo(1.0);
    }

    @Test
    void debeIgnorarTildesYDiacriticos() {
        assertThat(calculator.calcular("Básica", prenda("basica", "Negra")))
                .isEqualTo(1.0);
    }

    @Test
    void debeIgnorarSignosDePuntuacion() {
        assertThat(calculator.calcular("camiseta, negra!", prenda("Camiseta", "Negra")))
                .isEqualTo(1.0);
    }

    @Test
    void tokensRepetidosNoDebenInflarElScore() {
        assertThat(calculator.calcular("camiseta camiseta negra", prenda("Camiseta", "Azul")))
                .isEqualTo(0.5);
    }

    @Test
    void camposNulosYConsultaSinTokensDebenRetornarCero() {
        assertThat(calculator.calcular("---", new Prenda())).isZero();
        assertThat(calculator.calcular("camiseta", null)).isZero();
    }

    private Prenda prenda(String nombre, String color) {
        return new Prenda(1L, nombre, "Prenda casual para hombre", null, "Marca X", color, null);
    }
}
