package edu.dosw.proyecto.style_radar.model.domain;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SuscripcionTemporalTest {
    @ParameterizedTest @CsvSource({
            "2023-01-31T23:59:59Z,1,2023-02-28T23:59:59Z",
            "2024-01-31T23:59:59Z,1,2024-02-29T23:59:59Z",
            "2024-02-29T12:00:00Z,12,2025-02-28T12:00:00Z",
            "2024-08-31T12:00:00Z,6,2025-02-28T12:00:00Z",
            "2024-12-31T12:00:00Z,3,2025-03-31T12:00:00Z"})
    void mesesCalendarioUTCFinesDeMesYBisiesto(String desde, int meses, String hasta) {
        var p = new PlanSuscripcion(1L, "Fixture", meses, UnidadPeriodoSuscripcion.MESES_CALENDARIO, true);
        assertThat(p.finDesde(Instant.parse(desde))).isEqualTo(Instant.parse(hasta));
    }
    @Test void intervaloInicioIncluidoFinExcluidoAunqueRenovacionHabilitada() {
        var inicio = Instant.parse("2024-01-01T00:00:00Z"); var fin = inicio.plusSeconds(100);
        var s = new SuscripcionAlmacen(1L, "nit", 2L, "Fixture", EstadoSuscripcion.ACTIVA, inicio, inicio, fin, true, null);
        assertThat(s.vigente(inicio)).isTrue(); assertThat(s.vigente(fin.minusNanos(1))).isTrue();
        assertThat(s.vigente(inicio.minusNanos(1))).isFalse(); assertThat(s.vigente(fin)).isFalse();
        assertThat(s.estadoEfectivo(fin)).isEqualTo(EstadoSuscripcion.FINALIZADA);
        assertThat(s.estadoEfectivo(inicio)).isEqualTo(EstadoSuscripcion.ACTIVA);
    }
    @Test void rangoTemporalInvalidoRechazado() {
        var p = new PlanSuscripcion(1L, "Fixture", Integer.MAX_VALUE, UnidadPeriodoSuscripcion.MESES_CALENDARIO, true);
        assertThatThrownBy(() -> p.finDesde(Instant.MAX)).isInstanceOf(edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException.class);
    }
}
