package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.PrendaEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.PublicacionSegundaManoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import edu.dosw.proyecto.style_radar.repository.PublicacionSegundaManoRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.repository.specification.PublicacionSegundaManoSpecifications;
import edu.dosw.proyecto.style_radar.service.ISegundaManoService;
import edu.dosw.proyecto.style_radar.validator.SegundaManoValidator;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SegundaManoServiceImpl implements ISegundaManoService {

    private final PublicacionSegundaManoRepository publicacionRepository;
    private final PrendaRepository prendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PublicacionSegundaManoEntityMapper publicacionEntityMapper;
    private final PrendaEntityMapper prendaEntityMapper;
    private final SegundaManoValidator segundaManoValidator;
    private final UsuarioValidator usuarioValidator;
    private final Clock clock;

    @Override
    @Transactional
    public PublicacionSegundaMano publicar(
            Long usuarioId,
            Prenda prenda,
            Talla talla,
            EstadoConservacion estadoConservacion,
            Double precio,
            List<String> fotos) {
        log.info("Publicando prenda de segunda mano para el usuario ID: {}", usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        segundaManoValidator.validarPrecio(precio);

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con ID: " + usuarioId));

        PrendaEntity prendaGuardada = prendaRepository.save(prendaEntityMapper.toEntity(prenda));

        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .usuario(usuario)
                .prenda(prendaGuardada)
                .talla(talla)
                .estadoConservacion(estadoConservacion)
                .precio(precio)
                .estado(EstadoPublicacion.DISPONIBLE)
                .fechaPublicacion(clock.instant())
                .fotos(fotos != null ? new ArrayList<>(fotos) : new ArrayList<>())
                .build();

        PublicacionSegundaManoEntity guardada = publicacionRepository.save(entity);
        log.info("Prenda de segunda mano publicada exitosamente con ID: {}", guardada.getId());
        return publicacionEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicacionSegundaMano obtenerPorId(Long id) {
        log.info("Consultando publicación de segunda mano con ID: {}", id);
        PublicacionSegundaManoEntity entity = publicacionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No existe publicación de segunda mano con ID: {}", id);
                    return new RecursoNoEncontradoException("No existe publicación de segunda mano con ID: " + id);
                });
        return publicacionEntityMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicacionSegundaMano> buscar(BusquedaSegundaManoCriteria criteria, int page, int size) {
        log.info("Buscando publicaciones de segunda mano con filtros. Página: {}, Tamaño: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaPublicacion"));
        Page<PublicacionSegundaMano> resultado = publicacionRepository
                .findAll(PublicacionSegundaManoSpecifications.conCriterios(criteria), pageable)
                .map(publicacionEntityMapper::toDomain);
        log.info("Búsqueda de segunda mano completada. Resultados: {}", resultado.getTotalElements());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicacionSegundaMano> obtenerPorUsuario(Long usuarioId) {
        log.info("Consultando publicaciones de segunda mano del usuario ID: {}", usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        List<PublicacionSegundaMano> publicaciones = publicacionRepository
                .findByUsuario_IdOrderByFechaPublicacionDesc(usuarioId)
                .stream()
                .map(publicacionEntityMapper::toDomain)
                .toList();
        log.info("Publicaciones del usuario ID {}: {}", usuarioId, publicaciones.size());
        return publicaciones;
    }

    @Override
    @Transactional
    public PublicacionSegundaMano editar(
            Long usuarioId,
            Long publicacionId,
            Prenda prenda,
            Talla talla,
            EstadoConservacion estadoConservacion,
            Double precio,
            List<String> fotos) {
        log.info("Editando publicación de segunda mano ID: {} para el usuario ID: {}", publicacionId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        segundaManoValidator.validarPrecio(precio);

        PublicacionSegundaManoEntity entity = segundaManoValidator.validarYObtenerDeUsuario(publicacionId, usuarioId);
        segundaManoValidator.validarEditable(entity);

        PrendaEntity prendaActualizada = prendaEntityMapper.toEntity(prenda);
        prendaActualizada.setId(entity.getPrenda().getId());
        prendaActualizada = prendaRepository.save(prendaActualizada);

        entity.setPrenda(prendaActualizada);
        entity.setTalla(talla);
        entity.setEstadoConservacion(estadoConservacion);
        entity.setPrecio(precio);
        if (fotos != null) {
            entity.setFotos(new ArrayList<>(fotos));
        }

        PublicacionSegundaManoEntity guardada = publicacionRepository.save(entity);
        log.info("Publicación de segunda mano ID: {} editada exitosamente", publicacionId);
        return publicacionEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public PublicacionSegundaMano marcarComoVendida(Long usuarioId, Long publicacionId) {
        log.info("Marcando publicación ID: {} como VENDIDA por el usuario ID: {}", publicacionId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);

        PublicacionSegundaManoEntity entity = segundaManoValidator.validarYObtenerDeUsuario(publicacionId, usuarioId);
        segundaManoValidator.validarTransicionAVendida(entity);

        entity.setEstado(EstadoPublicacion.VENDIDA);
        PublicacionSegundaManoEntity guardada = publicacionRepository.save(entity);
        log.info("Publicación ID: {} marcada como VENDIDA exitosamente", publicacionId);
        return publicacionEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public void retirar(Long usuarioId, Long publicacionId) {
        log.info("Retirando publicación ID: {} por el usuario ID: {}", publicacionId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);

        PublicacionSegundaManoEntity entity = segundaManoValidator.validarYObtenerDeUsuario(publicacionId, usuarioId);
        segundaManoValidator.validarTransicionARetirada(entity);

        entity.setEstado(EstadoPublicacion.RETIRADA);
        publicacionRepository.save(entity);
        log.info("Publicación ID: {} retirada exitosamente", publicacionId);
    }
}
