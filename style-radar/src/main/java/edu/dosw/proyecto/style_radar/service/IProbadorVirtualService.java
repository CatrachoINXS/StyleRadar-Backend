package edu.dosw.proyecto.style_radar.service;

import edu.dosw.proyecto.style_radar.model.domain.FotoProbador;

public interface IProbadorVirtualService {

    FotoProbador cargarFoto(Long usuarioId, String contentType, byte[] datos);

    FotoProbador obtenerFoto(Long usuarioId);

    /** @return la composición en PNG. */
    byte[] probar(Long usuarioId, Long itemCatalogoId);

    void eliminarFoto(Long usuarioId);
}
