package edu.dosw.proyecto.style_radar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.ImagenCatalogo;
import edu.dosw.proyecto.style_radar.model.entity.ImagenCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.ImagenCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.service.IImagenCatalogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImagenCatalogoServiceImpl implements IImagenCatalogoService {

    private final AlmacenRepository almacenRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final ImagenCatalogoRepository imagenCatalogoRepository;

    @Override
    @Transactional
    public ImagenCatalogo registrar(String nit, Long itemId, String contentType, byte[] datos) {
        ItemCatalogoEntity item = obtenerItemDelAlmacen(nit, itemId);
        validarImagen(contentType, datos);
        ImagenCatalogoEntity imagen = new ImagenCatalogoEntity(null, contentType, datos, item);
        ImagenCatalogoEntity guardada = imagenCatalogoRepository.save(imagen);
        log.info("Imagen {} registrada para el item {} del almacén con NIT {}", guardada.getId(), itemId, nit);
        return new ImagenCatalogo(guardada.getId(), guardada.getContentType(), guardada.getDatos(), item.getId());
    }

    private void validarImagen(String contentType, byte[] datos) {
        if (datos == null || datos.length == 0) {
            log.warn("Se rechazó una imagen vacía");
            throw new ReglaDeNegocioException("El archivo de imagen no puede estar vacío");
        }
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("Se rechazó un archivo cuyo content type no es de imagen");
            throw new ReglaDeNegocioException("El archivo debe tener un tipo de contenido de imagen");
        }
    }

    private ItemCatalogoEntity obtenerItemDelAlmacen(String nit, Long itemId) {
        if (!almacenRepository.existsById(nit)) {
            log.warn("No existe un almacén con NIT {}", nit);
            throw new RecursoNoEncontradoException("No existe un almacén con NIT " + nit);
        }
        return itemCatalogoRepository.findByIdAndAlmacen_Nit(itemId, nit)
                .orElseThrow(() -> {
                    log.warn("No existe el item {} en el catálogo del almacén con NIT {}", itemId, nit);
                    return new RecursoNoEncontradoException(
                            "No existe el item " + itemId + " en el catálogo del almacén con NIT " + nit);
                });
    }
}
