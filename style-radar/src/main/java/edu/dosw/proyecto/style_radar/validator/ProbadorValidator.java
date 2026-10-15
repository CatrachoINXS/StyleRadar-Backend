package edu.dosw.proyecto.style_radar.validator;

import java.awt.image.BufferedImage;
import java.util.Set;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.FotoProbadorEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.FotoProbadorRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.service.VisibilidadModeracion;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProbadorValidator {

    public static final Set<String> FORMATOS = Set.of("image/jpeg", "image/png");
    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    public static final int ANCHO_MINIMO = 300;
    public static final int ALTO_MINIMO = 400;
    public static final Set<TipoPrenda> TIPOS_COMPATIBLES = Set.of(TipoPrenda.SUPERIOR, TipoPrenda.INFERIOR, TipoPrenda.VESTIDO);

    private final FotoProbadorRepository fotoRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;

    /** RF-23: JPEG/PNG, hasta 5 MB, decodificable, mínimo 300x400 y vertical (foto frontal de cuerpo). */
    public void validarFoto(String contentType, byte[] datos, BufferedImage imagen) {
        if (datos == null || datos.length == 0) {
            throw new ReglaDeNegocioException("La fotografía no puede estar vacía");
        }
        if (contentType == null || !FORMATOS.contains(contentType)) {
            throw new ReglaDeNegocioException("La fotografía debe ser JPEG o PNG");
        }
        if (datos.length > TAMANO_MAXIMO_BYTES) {
            throw new ReglaDeNegocioException("La fotografía no puede superar 5 MB");
        }
        if (imagen == null) {
            throw new ReglaDeNegocioException("El archivo no es una imagen válida");
        }
        if (imagen.getWidth() < ANCHO_MINIMO || imagen.getHeight() < ALTO_MINIMO) {
            throw new ReglaDeNegocioException("La fotografía debe medir al menos " + ANCHO_MINIMO + "x" + ALTO_MINIMO + " píxeles");
        }
        if (imagen.getHeight() < imagen.getWidth()) {
            throw new ReglaDeNegocioException("La fotografía frontal debe estar en orientación vertical");
        }
    }

    public FotoProbadorEntity obtenerFoto(Long usuarioId) {
        return fotoRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Primero cargue una fotografía frontal en el probador"));
    }

    /** RF-24: prenda visible del catálogo, de un tipo que se pueda superponer y con al menos una foto. */
    public ItemCatalogoEntity obtenerPrendaCompatible(Long itemCatalogoId) {
        ItemCatalogoEntity item = itemCatalogoRepository.findById(itemCatalogoId)
                .filter(i -> VisibilidadModeracion.visible(i.getEstadoModeracion()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la prenda del catálogo " + itemCatalogoId));
        if (!TIPOS_COMPATIBLES.contains(item.getPrenda().getTipo())) {
            throw new ReglaDeNegocioException("El probador virtual no admite prendas de tipo " + item.getPrenda().getTipo());
        }
        if (item.getImagenes().isEmpty()) {
            throw new ReglaDeNegocioException("La prenda no tiene fotografías para el probador virtual");
        }
        return item;
    }
}
