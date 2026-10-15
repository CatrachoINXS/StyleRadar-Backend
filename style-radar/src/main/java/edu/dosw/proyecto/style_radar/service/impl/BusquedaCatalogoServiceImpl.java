package edu.dosw.proyecto.style_radar.service.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.OrdenCatalogo;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.specification.ItemCatalogoSpecifications;
import edu.dosw.proyecto.style_radar.service.DistanciaCalculator;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;
import edu.dosw.proyecto.style_radar.service.IBusquedaCatalogoService;
import edu.dosw.proyecto.style_radar.service.SimilaridadPrendaCalculator;
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
    private final DistanciaCalculator distanciaCalculator;
    private final SimilaridadPrendaCalculator similaridadPrendaCalculator;

    @Override
    public Page<ItemCatalogo> buscar(BusquedaCatalogoCriteria criteria, int page, int size) {
        Page<ItemCatalogo> resultadoDirecto = criteria.getOrden() != null
                ? buscarDirectoConOrdenEspecial(criteria, page, size)
                : buscarDirecto(criteria, page, size);

        if (resultadoDirecto.getTotalElements() > 0 || criteria.getQ() == null) {
            return resultadoDirecto;
        }

        return buscarSimilares(criteria, page, size);
    }

    private Page<ItemCatalogo> buscarDirecto(BusquedaCatalogoCriteria criteria, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<ItemCatalogo> resultado = itemCatalogoRepository
                .findAll(ItemCatalogoSpecifications.conCriterios(criteria), pageable)
                .map(itemCatalogoEntityMapper::toDomain)
                .map(this::aplicarEstadoEfectivo);

        log.info("Búsqueda global de catálogo procesada. Página: {}, tamaño: {}, resultados: {}, total: {}",
                page, size, resultado.getNumberOfElements(), resultado.getTotalElements());
        return resultado;
    }

    /**
     * Trade-off consciente de esta etapa: el orden especial se resuelve en memoria
     * para conservar portabilidad entre PostgreSQL y H2. Debe sustituirse por una
     * estrategia escalable si el catalogo crece a millones de registros.
     */
    private Page<ItemCatalogo> buscarDirectoConOrdenEspecial(
            BusquedaCatalogoCriteria criteria, int page, int size) {
        List<ItemCatalogoEntity> candidatos = itemCatalogoRepository
                .findAll(ItemCatalogoSpecifications.conCriterios(criteria));
        Comparator<ItemCatalogoEntity> comparador = criteria.getOrden() == OrdenCatalogo.DISTANCIA
                ? comparadorDistancia(criteria.getLatitudUsuario(), criteria.getLongitudUsuario())
                : comparadorReputacion();
        List<ItemCatalogoEntity> ordenados = candidatos.stream().sorted(comparador).toList();
        Pageable pageable = PageRequest.of(page, size);
        List<ItemCatalogoEntity> contenido = pagina(ordenados, pageable);

        Page<ItemCatalogo> resultado = new PageImpl<>(contenido, pageable, ordenados.size())
                .map(itemCatalogoEntityMapper::toDomain)
                .map(this::aplicarEstadoEfectivo);

        log.info("Busqueda global de catalogo procesada con orden {}. Resultados encontrados: {}",
                criteria.getOrden(), resultado.getTotalElements());
        return resultado;
    }

    /**
     * Trade-off consciente: RF-16 calcula la similitud en memoria sobre todos los
     * candidatos que ya cumplen los filtros estructurados. Es apropiado para el
     * alcance académico actual, pero requeriría otra estrategia a gran escala.
     */
    private Page<ItemCatalogo> buscarSimilares(
            BusquedaCatalogoCriteria criteria, int page, int size) {
        List<ResultadoSimilar> similares = itemCatalogoRepository
                .findAll(ItemCatalogoSpecifications.conCriteriosSinTexto(criteria))
                .stream()
                .map(entity -> crearResultadoSimilar(entity, criteria.getQ()))
                .filter(resultado -> resultado.score() > 0.0)
                .sorted(comparadorSimilitud(criteria))
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        List<ItemCatalogo> contenido = pagina(similares, pageable).stream()
                .map(ResultadoSimilar::item)
                .map(this::aplicarEstadoEfectivo)
                .toList();

        log.info("No se encontraron coincidencias directas. Se aplica búsqueda de prendas similares. Similares obtenidos: {}",
                similares.size());
        return new PageImpl<>(contenido, pageable, similares.size());
    }

    private ResultadoSimilar crearResultadoSimilar(ItemCatalogoEntity entity, String consulta) {
        ItemCatalogo item = itemCatalogoEntityMapper.toDomain(entity);
        return new ResultadoSimilar(
                entity,
                item,
                similaridadPrendaCalculator.calcular(consulta, item.getPrenda()));
    }

    private Comparator<ResultadoSimilar> comparadorSimilitud(BusquedaCatalogoCriteria criteria) {
        Comparator<ResultadoSimilar> comparador = Comparator
                .comparingDouble(ResultadoSimilar::score)
                .reversed();

        if (criteria.getOrden() == OrdenCatalogo.DISTANCIA) {
            comparador = comparador.thenComparing(
                    resultado -> distanciaDesdeUsuario(
                            resultado.entity(), criteria.getLatitudUsuario(), criteria.getLongitudUsuario()),
                    Comparator.nullsLast(Comparator.naturalOrder()));
        } else if (criteria.getOrden() == OrdenCatalogo.REPUTACION) {
            comparador = comparador.thenComparing(
                    resultado -> resultado.entity().getAlmacen().getReputacion(),
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }

        return comparador.thenComparing(resultado -> resultado.item().getId());
    }

    private Comparator<ItemCatalogoEntity> comparadorDistancia(double latitudUsuario, double longitudUsuario) {
        return Comparator
                .comparing(
                        (ItemCatalogoEntity item) -> distanciaDesdeUsuario(
                                item, latitudUsuario, longitudUsuario),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ItemCatalogoEntity::getId);
    }

    private Comparator<ItemCatalogoEntity> comparadorReputacion() {
        return Comparator
                .comparing(
                        (ItemCatalogoEntity item) -> item.getAlmacen().getReputacion(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ItemCatalogoEntity::getId);
    }

    private Double distanciaDesdeUsuario(
            ItemCatalogoEntity item, double latitudUsuario, double longitudUsuario) {
        AlmacenEntity almacen = item.getAlmacen();
        if (almacen.getLatitud() == null || almacen.getLongitud() == null) {
            return null;
        }
        return distanciaCalculator.calcularKm(
                latitudUsuario,
                longitudUsuario,
                almacen.getLatitud(),
                almacen.getLongitud());
    }

    private <T> List<T> pagina(List<T> ordenados, Pageable pageable) {
        long offset = pageable.getOffset();
        if (offset >= ordenados.size()) {
            return List.of();
        }
        int desde = (int) offset;
        int hasta = Math.min(desde + pageable.getPageSize(), ordenados.size());
        return ordenados.subList(desde, hasta);
    }

    private ItemCatalogo aplicarEstadoEfectivo(ItemCatalogo item) {
        item.setEstado(estadoItemCalculator.calcular(item));
        return item;
    }

    private record ResultadoSimilar(ItemCatalogoEntity entity, ItemCatalogo item, double score) {
    }
}
