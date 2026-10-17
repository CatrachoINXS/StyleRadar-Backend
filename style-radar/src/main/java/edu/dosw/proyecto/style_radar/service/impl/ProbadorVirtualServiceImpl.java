package edu.dosw.proyecto.style_radar.service.impl;

import java.awt.image.BufferedImage;
import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.FotoProbador;
import edu.dosw.proyecto.style_radar.model.entity.FotoProbadorEntity;
import edu.dosw.proyecto.style_radar.model.entity.ImagenCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.FotoProbadorRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.ComposicionProbador;
import edu.dosw.proyecto.style_radar.service.IProbadorVirtualService;
import edu.dosw.proyecto.style_radar.service.ProbadorIA;
import edu.dosw.proyecto.style_radar.validator.ProbadorValidator;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProbadorVirtualServiceImpl implements IProbadorVirtualService {

    private final FotoProbadorRepository fotoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProbadorValidator probadorValidator;
    private final UsuarioValidator usuarioValidator;
    private final ComposicionProbador composicion;
    private final ProbadorIA probadorIA;
    private final Clock clock;

    @Override
    @Transactional
    public FotoProbador cargarFoto(Long usuarioId, String contentType, byte[] datos) {
        log.info("Cargando foto del probador para el usuario ID: {}", usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        BufferedImage imagen = composicion.leer(datos);
        probadorValidator.validarFoto(contentType, datos, imagen);

        FotoProbadorEntity foto = fotoRepository.findByUsuario_Id(usuarioId)
                .orElseGet(() -> FotoProbadorEntity.builder().usuario(usuarioRepository.getReferenceById(usuarioId)).build());
        foto.setContentType(contentType);
        foto.setDatos(datos);
        foto.setAncho(imagen.getWidth());
        foto.setAlto(imagen.getHeight());
        foto.setFechaCarga(clock.instant());
        FotoProbadorEntity guardada = fotoRepository.save(foto);
        log.info("Foto del probador guardada para el usuario ID: {} ({}x{})", usuarioId, imagen.getWidth(), imagen.getHeight());
        return toDomain(guardada, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public FotoProbador obtenerFoto(Long usuarioId) {
        log.info("Consultando foto del probador del usuario ID: {}", usuarioId);
        return toDomain(probadorValidator.obtenerFoto(usuarioId), usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] probar(Long usuarioId, Long itemCatalogoId) {
        log.info("Usuario ID: {} prueba la prenda del catálogo ID: {}", usuarioId, itemCatalogoId);
        long inicio = System.nanoTime();
        FotoProbadorEntity foto = probadorValidator.obtenerFoto(usuarioId);
        ItemCatalogoEntity item = probadorValidator.obtenerPrendaCompatible(itemCatalogoId);

        ImagenCatalogoEntity imagenPrenda = item.getImagenes().get(0);
        BufferedImage prenda = composicion.leer(imagenPrenda.getDatos());
        if (prenda == null) {
            throw new ReglaDeNegocioException("La fotografía de la prenda no se puede procesar");
        }
        byte[] resultado = probadorIA.probar(foto.getDatos(), foto.getContentType(), imagenPrenda.getDatos(),
                        imagenPrenda.getContentType(), item.getPrenda().getTipo())
                .orElseGet(() -> composicion.componer(composicion.leer(foto.getDatos()), prenda,
                        item.getPrenda().getTipo()));
        log.info("Prueba virtual generada para usuario ID: {} e item ID: {} en {} ms",
                usuarioId, itemCatalogoId, (System.nanoTime() - inicio) / 1_000_000);
        return resultado;
    }

    @Override
    @Transactional
    public void eliminarFoto(Long usuarioId) {
        log.info("Eliminando foto del probador del usuario ID: {}", usuarioId);
        fotoRepository.delete(probadorValidator.obtenerFoto(usuarioId));
        log.info("Foto del probador eliminada para el usuario ID: {}", usuarioId);
    }

    private FotoProbador toDomain(FotoProbadorEntity entity, Long usuarioId) {
        return FotoProbador.builder().usuarioId(usuarioId).contentType(entity.getContentType())
                .ancho(entity.getAncho()).alto(entity.getAlto()).fechaCarga(entity.getFechaCarga()).build();
    }
}
