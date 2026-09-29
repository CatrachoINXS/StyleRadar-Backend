package edu.dosw.proyecto.style_radar.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.specification.ItemCatalogoSpecifications;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;
import edu.dosw.proyecto.style_radar.service.IBusquedaCatalogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BusquedaCatalogoServiceImpl implements IBusquedaCatalogoService {

    private final ItemCatalogoRepository itemCatalogoRepository;
    private final ItemCatalogoEntityMapper itemCatalogoEntityMapper;
    private final EstadoItemCalculator estadoItemCalculator;

    @Override
    public Page<ItemCatalogo> buscar(BusquedaCatalogoCriteria criteria, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<ItemCatalogo> resultado = itemCatalogoRepository
                .findAll(ItemCatalogoSpecifications.conCriterios(criteria), pageable)
                .map(itemCatalogoEntityMapper::toDomain)
                .map(this::aplicarEstadoEfectivo);

        log.info("Búsqueda global de catálogo procesada. Página: {}, tamaño: {}, resultados: {}, total: {}",
                page, size, resultado.getNumberOfElements(), resultado.getTotalElements());
        return resultado;
    }

    private ItemCatalogo aplicarEstadoEfectivo(ItemCatalogo item) {
        item.setEstado(estadoItemCalculator.calcular(item));
        return item;
    }
}
