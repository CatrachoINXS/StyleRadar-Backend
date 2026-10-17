package edu.dosw.proyecto.style_radar.service;

import java.util.Optional;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;

/**
 * Puerto hacia el servicio externo de prueba virtual con IA (patrón Adapter).
 * Devuelve vacío cuando el servicio no está configurado o no responde, para usar la composición local.
 */
public interface ProbadorIA {

    Optional<byte[]> probar(byte[] foto, String fotoContentType, byte[] prenda, String prendaContentType, TipoPrenda tipo);
}
