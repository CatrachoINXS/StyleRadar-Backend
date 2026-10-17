package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.service.ProbadorIA;
import lombok.extern.slf4j.Slf4j;

/** Adaptador del servicio de prueba virtual de FASHN (modelo tryon-v1.6). */
@Slf4j
@Component
public class FashnProbadorIA implements ProbadorIA {

    private static final String MODELO = "tryon-v1.6";

    private final RestClient client;
    private final String apiKey;
    private final Duration pausa;
    private final int intentos;

    @Autowired
    public FashnProbadorIA(@Value("${styleradar.probador.ia.url:https://api.fashn.ai/v1}") String url,
            @Value("${styleradar.probador.ia.api-key:}") String apiKey) {
        // 14 consultas cada 2 segundos: la respuesta no supera los 30 segundos antes de usar la composición local.
        this(RestClient.builder().baseUrl(url), apiKey, Duration.ofSeconds(2), 14);
    }

    FashnProbadorIA(RestClient.Builder builder, String apiKey, Duration pausa, int intentos) {
        this.client = builder.build();
        this.apiKey = apiKey;
        this.pausa = pausa;
        this.intentos = intentos;
    }

    @Override
    public Optional<byte[]> probar(byte[] foto, String fotoContentType, byte[] prenda, String prendaContentType,
            TipoPrenda tipo) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<?, ?> ejecucion = client.post().uri("/run")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model_name", MODELO, "inputs", Map.of(
                            "model_image", dataUri(fotoContentType, foto),
                            "garment_image", dataUri(prendaContentType, prenda),
                            "category", categoria(tipo),
                            "return_base64", true)))
                    .retrieve().body(Map.class);
            Object id = ejecucion == null ? null : ejecucion.get("id");
            if (id == null) {
                log.warn("El servicio de prueba virtual no devolvió un identificador de ejecución");
                return Optional.empty();
            }
            for (int i = 0; i < intentos; i++) {
                Thread.sleep(pausa);
                Map<?, ?> estado = client.get().uri("/status/{id}", id)
                        .header("Authorization", "Bearer " + apiKey)
                        .retrieve().body(Map.class);
                Object valor = estado == null ? null : estado.get("status");
                if ("completed".equals(valor) && estado.get("output") instanceof List<?> salida && !salida.isEmpty()) {
                    log.info("Prueba virtual generada por el servicio externo, ejecución {}", id);
                    return Optional.of(decodificar(String.valueOf(salida.get(0))));
                }
                if ("failed".equals(valor) || "canceled".equals(valor)) {
                    log.warn("El servicio de prueba virtual terminó con estado {}: {}", valor, estado.get("error"));
                    return Optional.empty();
                }
            }
            log.warn("El servicio de prueba virtual no respondió a tiempo, ejecución {}", id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log.warn("Error al llamar el servicio de prueba virtual: {}", e.getMessage());
        }
        return Optional.empty();
    }

    static String categoria(TipoPrenda tipo) {
        return switch (tipo) {
            case SUPERIOR -> "tops";
            case INFERIOR -> "bottoms";
            case VESTIDO -> "one-pieces";
            default -> "auto";
        };
    }

    private static String dataUri(String contentType, byte[] datos) {
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(datos);
    }

    private static byte[] decodificar(String salida) {
        return Base64.getDecoder().decode(salida.substring(salida.indexOf(',') + 1));
    }
}
