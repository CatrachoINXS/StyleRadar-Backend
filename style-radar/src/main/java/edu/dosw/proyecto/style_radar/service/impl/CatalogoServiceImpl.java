package edu.dosw.proyecto.style_radar.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.PrendaEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import edu.dosw.proyecto.style_radar.service.ICatalogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogoServiceImpl implements ICatalogoService {

    private final AlmacenRepository almacenRepository;
    private final PrendaRepository prendaRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final PrendaEntityMapper prendaEntityMapper;
    private final ItemCatalogoEntityMapper itemCatalogoEntityMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ItemCatalogo> obtenerCatalogo(String nit) {
        log.info("Consultando catálogo del almacén con NIT {}", nit);
        validarAlmacenExiste(nit);

        List<ItemCatalogo> catalogo = itemCatalogoRepository.findByAlmacen_Nit(nit).stream()
                .map(itemCatalogoEntityMapper::toDomain)
                .toList();

        log.info("Consulta de catálogo completada para el almacén con NIT {}. Total: {}", nit, catalogo.size());
        return catalogo;
    }

    @Override
    @Transactional
    public ItemCatalogo publicar(String nit, Prenda prenda, Double precio) {
        AlmacenEntity almacen = obtenerAlmacen(nit);
        PrendaEntity prendaGuardada = prendaRepository.save(prendaEntityMapper.toEntity(prenda));

        ItemCatalogo nuevoItem = new ItemCatalogo(
                null,
                precio,
                null,
                0,
                EstadoItem.AGOTADA,
                new ArrayList<>(),
                prendaEntityMapper.toDomain(prendaGuardada),
                nit);
        ItemCatalogoEntity itemEntity = itemCatalogoEntityMapper.toEntity(nuevoItem);
        itemEntity.setAlmacen(almacen);
        itemEntity.setPrenda(prendaGuardada);

        ItemCatalogo resultado = itemCatalogoEntityMapper.toDomain(itemCatalogoRepository.save(itemEntity));
        log.info("Publicación completada para el item {} en el almacén con NIT {}", resultado.getId(), nit);
        return resultado;
    }

    @Override
    @Transactional
    public ItemCatalogo actualizar(String nit, Long itemId, Prenda prenda, Double precio) {
        validarAlmacenExiste(nit);
        ItemCatalogoEntity itemEntity = obtenerItemDelAlmacen(nit, itemId);

        PrendaEntity prendaActualizada = prendaEntityMapper.toEntity(prenda);
        prendaActualizada.setId(itemEntity.getPrenda().getId());
        prendaActualizada = prendaRepository.save(prendaActualizada);

        itemEntity.setPrenda(prendaActualizada);
        itemEntity.setPrecio(precio);

        ItemCatalogo resultado = itemCatalogoEntityMapper.toDomain(itemCatalogoRepository.save(itemEntity));
        log.info("Actualización completada para el item {} del almacén con NIT {}", itemId, nit);
        return resultado;
    }

    @Override
    @Transactional
    public void retirar(String nit, Long itemId) {
        validarAlmacenExiste(nit);
        ItemCatalogoEntity itemEntity = obtenerItemDelAlmacen(nit, itemId);
        itemCatalogoRepository.delete(itemEntity);
        log.info("Retiro completado para el item {} del almacén con NIT {}", itemId, nit);
    }

    private void validarAlmacenExiste(String nit) {
        if (!almacenRepository.existsById(nit)) {
            log.warn("No existe un almacén con NIT {}", nit);
            throw new RecursoNoEncontradoException("No existe un almacén con NIT " + nit);
        }
    }

    private AlmacenEntity obtenerAlmacen(String nit) {
        return almacenRepository.findById(nit)
                .orElseThrow(() -> {
                    log.warn("No existe un almacén con NIT {}", nit);
                    return new RecursoNoEncontradoException("No existe un almacén con NIT " + nit);
                });
    }

    private ItemCatalogoEntity obtenerItemDelAlmacen(String nit, Long itemId) {
        return itemCatalogoRepository.findByIdAndAlmacen_Nit(itemId, nit)
                .orElseThrow(() -> {
                    log.warn("No existe el item {} en el catálogo del almacén con NIT {}", itemId, nit);
                    return new RecursoNoEncontradoException(
                            "No existe el item " + itemId + " en el catálogo del almacén con NIT " + nit);
                });
    }
}
