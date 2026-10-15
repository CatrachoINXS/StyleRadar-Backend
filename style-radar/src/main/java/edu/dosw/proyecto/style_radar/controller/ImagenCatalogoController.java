package edu.dosw.proyecto.style_radar.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.proyecto.style_radar.controller.docs.ImagenCatalogoApi;
import edu.dosw.proyecto.style_radar.mapper.ImagenCatalogoMapper;
import edu.dosw.proyecto.style_radar.model.domain.ImagenCatalogo;
import edu.dosw.proyecto.style_radar.model.dto.response.ImagenCatalogoResponseDTO;
import edu.dosw.proyecto.style_radar.service.IImagenCatalogoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/almacenes/{nit}/catalogo")
@RequiredArgsConstructor
public class ImagenCatalogoController implements ImagenCatalogoApi {

    private final IImagenCatalogoService imagenCatalogoService;
    private final ImagenCatalogoMapper imagenCatalogoMapper;

    @Override
    public ResponseEntity<ImagenCatalogoResponseDTO> registrar(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @RequestPart("archivo") MultipartFile archivo) {
        try {
            ImagenCatalogo imagen = imagenCatalogoService.registrar(
                    nit, itemId, archivo.getContentType(), archivo.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(imagenCatalogoMapper.toResponse(imagen));
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible procesar la fotografía recibida", exception);
        }
    }
}
