package edu.dosw.proyecto.style_radar.service;

import org.springframework.stereotype.Component;

@Component
public class DistanciaCalculator {

    static final double RADIO_TERRESTRE_KM = 6371.0088;

    public double calcularKm(
            double latitudOrigen,
            double longitudOrigen,
            double latitudDestino,
            double longitudDestino) {
        double diferenciaLatitud = Math.toRadians(latitudDestino - latitudOrigen);
        double diferenciaLongitud = Math.toRadians(longitudDestino - longitudOrigen);
        double latitudOrigenRadianes = Math.toRadians(latitudOrigen);
        double latitudDestinoRadianes = Math.toRadians(latitudDestino);

        double haversine = Math.pow(Math.sin(diferenciaLatitud / 2), 2)
                + Math.cos(latitudOrigenRadianes)
                * Math.cos(latitudDestinoRadianes)
                * Math.pow(Math.sin(diferenciaLongitud / 2), 2);
        double valorSeguro = Math.max(0.0, Math.min(1.0, haversine));
        double anguloCentral = 2 * Math.asin(Math.sqrt(valorSeguro));

        return RADIO_TERRESTRE_KM * anguloCentral;
    }
}
