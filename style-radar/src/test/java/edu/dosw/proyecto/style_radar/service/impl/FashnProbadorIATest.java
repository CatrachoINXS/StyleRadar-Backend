package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;

class FashnProbadorIATest {

    static final String URL = "https://ia.test/v1";
    static final byte[] FOTO = {1, 2, 3};
    static final byte[] PRENDA = {4, 5, 6};

    RestClient.Builder builder;
    MockRestServiceServer server;
    FashnProbadorIA adaptador;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl(URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adaptador = new FashnProbadorIA(builder, "clave-test", Duration.ZERO, 2);
    }

    @Test
    void probarDevuelveLaImagenGeneradaCuandoElServicioCompleta() {
        // Arrange
        byte[] generada = {9, 8, 7};
        server.expect(requestTo(URL + "/run")).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer clave-test"))
                .andExpect(jsonPath("$.model_name").value("tryon-v1.6"))
                .andExpect(jsonPath("$.inputs.category").value("tops"))
                .andRespond(withSuccess("{\"id\":\"abc\",\"error\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL + "/status/abc"))
                .andRespond(withSuccess("{\"id\":\"abc\",\"status\":\"processing\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL + "/status/abc"))
                .andRespond(withSuccess("{\"id\":\"abc\",\"status\":\"completed\",\"output\":[\"data:image/png;base64,"
                        + Base64.getEncoder().encodeToString(generada) + "\"]}", MediaType.APPLICATION_JSON));

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).contains(generada);
        server.verify();
    }

    @Test
    void probarDevuelveVacioSinClaveConfigurada() {
        // Arrange
        FashnProbadorIA sinClave = new FashnProbadorIA(builder, " ", Duration.ZERO, 2);

        // Act
        Optional<byte[]> resultado = sinClave.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void probarDevuelveVacioCuandoElServicioFalla() {
        // Arrange
        server.expect(requestTo(URL + "/run")).andRespond(withServerError());

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.INFERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void probarDevuelveVacioCuandoLaEjecucionTerminaConError() {
        // Arrange
        server.expect(requestTo(URL + "/run"))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL + "/status/abc"))
                .andRespond(withSuccess("{\"status\":\"failed\",\"error\":\"imagen inválida\"}", MediaType.APPLICATION_JSON));

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.VESTIDO);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void probarDevuelveVacioCuandoNoTerminaATiempo() {
        // Arrange
        server.expect(requestTo(URL + "/run"))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL + "/status/abc"))
                .andRespond(withSuccess("{\"status\":\"processing\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL + "/status/abc"))
                .andRespond(withSuccess("{\"status\":\"in_queue\"}", MediaType.APPLICATION_JSON));

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void probarDevuelveVacioSinIdentificadorDeEjecucion() {
        // Arrange
        server.expect(requestTo(URL + "/run"))
                .andRespond(withSuccess("{\"error\":\"sin créditos\"}", MediaType.APPLICATION_JSON));

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void categoriaTraduceElTipoDePrenda() {
        // Act & Assert
        assertThat(FashnProbadorIA.categoria(TipoPrenda.SUPERIOR)).isEqualTo("tops");
        assertThat(FashnProbadorIA.categoria(TipoPrenda.INFERIOR)).isEqualTo("bottoms");
        assertThat(FashnProbadorIA.categoria(TipoPrenda.VESTIDO)).isEqualTo("one-pieces");
        assertThat(FashnProbadorIA.categoria(TipoPrenda.CALZADO)).isEqualTo("auto");
    }
}
