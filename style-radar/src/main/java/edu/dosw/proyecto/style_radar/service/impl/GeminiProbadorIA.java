package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.service.ProbadorIA;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeminiProbadorIA implements ProbadorIA {

    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(30);

    private final RestClient client;
    private final String apiKey;
    private final String modelo;

    @Autowired
    public GeminiProbadorIA(
            @Value("${styleradar.probador.ia.url:https://generativelanguage.googleapis.com/v1beta}") String url,
            @Value("${styleradar.probador.ia.api-key:}") String apiKey,
            @Value("${styleradar.probador.ia.modelo:gemini-3.1-flash-lite-image}") String modelo) {
        this(RestClient.builder().baseUrl(url).requestFactory(fabrica()), apiKey, modelo);
    }

    GeminiProbadorIA(RestClient.Builder builder, String apiKey, String modelo) {
        this.client = builder.build();
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public Optional<byte[]> probar(byte[] foto, String fotoContentType, byte[] prenda, String prendaContentType,
            TipoPrenda tipo) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<?, ?> respuesta = client.post().uri("/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "contents", List.of(Map.of("parts", List.of(
                                    Map.of("text", instruccion(tipo)),
                                    imagen(fotoContentType, foto),
                                    imagen(prendaContentType, prenda)))),
                            "generationConfig", Map.of("responseModalities", List.of("IMAGE"))))
                    .retrieve().body(Map.class);
            Optional<byte[]> resultado = extraerImagen(respuesta);
            if (resultado.isPresent()) {
                log.info("Prueba virtual generada con Gemini ({})", modelo);
            } else {
                log.warn("Gemini no devolvió una imagen para la prueba virtual");
            }
            return resultado;
        } catch (RuntimeException e) {
            log.warn("Error al llamar a Gemini para la prueba virtual: {}", e.getMessage());
            return Optional.empty();
        }
    }

    static String instruccion(TipoPrenda tipo) {
        String prenda = switch (tipo) {
            case SUPERIOR -> "upper-body garment";
            case INFERIOR -> "lower-body garment";
            case VESTIDO -> "dress";
            default -> "garment";
        };
        return "Virtual try-on: dress the person in the first image with the " + prenda
                + " shown in the second image. Keep the person's face, body, pose and background unchanged "
                + "and keep the garment's color, pattern and design. Return only the edited photo.";
    }

    private static Map<String, Object> imagen(String contentType, byte[] datos) {
        return Map.of("inline_data", Map.of("mime_type", contentType,
                "data", Base64.getEncoder().encodeToString(datos)));
    }

    private static Optional<byte[]> extraerImagen(Map<?, ?> respuesta) {
        if (respuesta == null || !(respuesta.get("candidates") instanceof List<?> candidatos)) {
            return Optional.empty();
        }
        for (Object candidato : candidatos) {
            if (candidato instanceof Map<?, ?> c && c.get("content") instanceof Map<?, ?> contenido
                    && contenido.get("parts") instanceof List<?> partes) {
                for (Object parte : partes) {
                    if (parte instanceof Map<?, ?> p && p.get("inlineData") instanceof Map<?, ?> datos
                            && datos.get("data") instanceof String base64) {
                        return Optional.of(Base64.getDecoder().decode(base64));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(TIEMPO_MAXIMO);
        return fabrica;
    }
}
