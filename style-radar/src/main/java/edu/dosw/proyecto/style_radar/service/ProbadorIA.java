package edu.dosw.proyecto.style_radar.service;

import java.util.Optional;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;

public interface ProbadorIA {

    Optional<byte[]> probar(byte[] foto, String fotoContentType, byte[] prenda, String prendaContentType, TipoPrenda tipo);
}
