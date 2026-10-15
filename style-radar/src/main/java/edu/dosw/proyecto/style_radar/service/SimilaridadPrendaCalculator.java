package edu.dosw.proyecto.style_radar.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.model.domain.Prenda;

@Component
public class SimilaridadPrendaCalculator {

    public double calcular(String consulta, Prenda prenda) {
        Set<String> tokensConsulta = tokenizar(consulta);
        if (tokensConsulta.isEmpty() || prenda == null) {
            return 0.0;
        }

        Set<String> tokensPrenda = new LinkedHashSet<>();
        tokensPrenda.addAll(tokenizar(prenda.getNombre()));
        tokensPrenda.addAll(tokenizar(prenda.getDescripcion()));
        tokensPrenda.addAll(tokenizar(prenda.getMarca()));
        tokensPrenda.addAll(tokenizar(prenda.getColor()));

        long coincidencias = tokensConsulta.stream().filter(tokensPrenda::contains).count();
        return (double) coincidencias / tokensConsulta.size();
    }

    Set<String> tokenizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }

        String normalizado = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}]", " ")
                .trim();
        if (normalizado.isEmpty()) {
            return Set.of();
        }

        return Arrays.stream(normalizado.split("\\s+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
