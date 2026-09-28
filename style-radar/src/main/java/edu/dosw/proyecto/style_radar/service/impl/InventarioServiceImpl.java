package edu.dosw.proyecto.style_radar.service.impl;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;
import edu.dosw.proyecto.style_radar.service.IInventarioService;
import edu.dosw.proyecto.style_radar.validator.InventarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioServiceImpl implements IInventarioService {

    private final AlmacenRepository almacenRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final ItemCatalogoEntityMapper itemCatalogoEntityMapper;
    private final InventarioValidator inventarioValidator;
    private final EstadoItemCalculator estadoItemCalculator;

    @Override
    @Transactional
    public ItemCatalogo registrarTallas(String nit, Long itemId, Set<Talla> tallas) {
        ItemCatalogoEntity item = obtenerItemDelAlmacen(nit, itemId);
        inventarioValidator.validarTallasEliminables(item.getInventario(), tallas);

        item.getInventario().removeIf(inventario ->
                !tallas.contains(inventario.getTalla()) && inventario.getUnidades() == 0);

        Set<Talla> tallasRegistradas = item.getInventario().stream()
                .map(InventarioTallaEntity::getTalla)
                .collect(java.util.stream.Collectors.toSet());
        tallas.stream()
                .filter(talla -> !tallasRegistradas.contains(talla))
                .map(talla -> new InventarioTallaEntity(null, talla, 0, item))
                .forEach(item.getInventario()::add);

        actualizarEstado(item);
        return itemCatalogoEntityMapper.toDomain(itemCatalogoRepository.save(item));
    }

    @Override
    @Transactional
    public ItemCatalogo actualizarDisponibilidad(String nit, Long itemId, Talla talla, Integer unidades) {
        ItemCatalogoEntity item = obtenerItemDelAlmacen(nit, itemId);
        InventarioTallaEntity inventario = inventarioValidator.obtenerTallaRegistrada(item.getInventario(), talla);
        inventario.setUnidades(unidades);
        actualizarEstado(item);
        return itemCatalogoEntityMapper.toDomain(itemCatalogoRepository.save(item));
    }

    private ItemCatalogoEntity obtenerItemDelAlmacen(String nit, Long itemId) {
        if (!almacenRepository.existsById(nit)) {
            throw new RecursoNoEncontradoException("No existe un almacén con NIT " + nit);
        }
        return itemCatalogoRepository.findByIdAndAlmacen_Nit(itemId, nit)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el item " + itemId + " en el catálogo del almacén con NIT " + nit));
    }

    private void actualizarEstado(ItemCatalogoEntity item) {
        EstadoItem estadoAnterior = item.getEstado();
        ItemCatalogo itemDominio = itemCatalogoEntityMapper.toDomain(item);
        EstadoItem estadoActual = estadoItemCalculator.calcular(itemDominio);
        item.setEstado(estadoActual);
        if (estadoAnterior != estadoActual) {
            log.info("Estado del item {} actualizado de {} a {}", item.getId(), estadoAnterior, estadoActual);
        }
    }
}
