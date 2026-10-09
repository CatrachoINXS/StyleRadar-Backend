package edu.dosw.proyecto.style_radar.model.dto.response;

import java.math.BigDecimal;

public record DistanciaAlmacenResponseDTO(String almacenNit, BigDecimal distanciaKm, String tipoEstimacion) {
}
