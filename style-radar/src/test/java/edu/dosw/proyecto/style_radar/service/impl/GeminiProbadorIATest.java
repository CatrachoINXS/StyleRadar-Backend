package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;

class GeminiProbadorIATest {

    static final String URL = "https://gemini.test/v1beta";
    static final String RUTA = URL + "/models/modelo-test:generateContent";
    static final byte[] FOTO = {1, 2, 3};
    static final byte[] PRENDA = {4, 5, 6};

    RestClient.Builder builder;
    MockRestServiceServer server;
    GeminiProbadorIA adaptador;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl(URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adaptador = new GeminiProbadorIA(builder, "clave-test", "modelo-test");
    }

    @Test
    void probarDevuelveLaImagenGeneradaPorGemini() {
        // Arrange
        byte[] generada = {9, 8, 7};
        server.expect(requestTo(RUTA)).andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-test"))
                .andExpect(jsonPath("$.contents[0].parts[1].inline_data.data")
                        .value(Base64.getEncoder().encodeToString(FOTO)))
                .andExpect(jsonPath("$.contents[0].parts[2].inline_data.mime_type").value("image/jpeg"))
                .andExpect(jsonPath("$.generationConfig.responseModalities[0]").value("IMAGE"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Listo\"},"
                        + "{\"inlineData\":{\"mimeType\":\"image/png\",\"data\":\""
                        + Base64.getEncoder().encodeToString(generada) + "\"}}]}}]}", MediaType.APPLICATION_JSON));

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/jpeg", TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).contains(generada);
        server.verify();
    }

    @Test
    void probarDevuelveVacioSinClaveConfigurada() {
        // Arrange
        GeminiProbadorIA sinClave = new GeminiProbadorIA(builder, " ", "modelo-test");

        // Act
        Optional<byte[]> resultado = sinClave.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void probarDevuelveVacioCuandoGeminiFalla() {
        // Arrange
        server.expect(requestTo(RUTA)).andRespond(withServerError());

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.INFERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void probarDevuelveVacioCuandoLaRespuestaNoTraeImagen() {
        // Arrange
        server.expect(requestTo(RUTA)).andRespond(withSuccess(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"No puedo editar esta imagen\"}]}}]}",
                MediaType.APPLICATION_JSON));

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.VESTIDO);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void probarDevuelveVacioSinCandidatos() {
        // Arrange
        server.expect(requestTo(RUTA)).andRespond(withSuccess("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}",
                MediaType.APPLICATION_JSON));

        // Act
        Optional<byte[]> resultado = adaptador.probar(FOTO, "image/png", PRENDA, "image/png", TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void instruccionNombraElTipoDePrenda() {
        // Act & Assert
        assertThat(GeminiProbadorIA.instruccion(TipoPrenda.SUPERIOR)).contains("upper-body garment");
        assertThat(GeminiProbadorIA.instruccion(TipoPrenda.INFERIOR)).contains("lower-body garment");
        assertThat(GeminiProbadorIA.instruccion(TipoPrenda.VESTIDO)).contains("dress");
        assertThat(GeminiProbadorIA.instruccion(TipoPrenda.CALZADO)).contains("the garment shown");
    }

    @Test
    void constructorDeSpringCreaElAdaptadorSinClave() {
        // Arrange
        GeminiProbadorIA desdeConfiguracion = new GeminiProbadorIA(URL, "", "modelo-test");

        // Act
        Optional<byte[]> resultado = desdeConfiguracion.probar(FOTO, "image/png", PRENDA, "image/png",
                TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).isEmpty();
    }
}
