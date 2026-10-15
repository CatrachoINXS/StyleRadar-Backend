package edu.dosw.proyecto.style_radar.service;

import edu.dosw.proyecto.style_radar.model.domain.ImagenCatalogo;

public interface IImagenCatalogoService {

    ImagenCatalogo registrar(String nit, Long itemId, String contentType, byte[] datos);
}
