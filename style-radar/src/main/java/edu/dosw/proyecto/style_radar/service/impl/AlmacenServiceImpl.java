package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.AlmacenEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.service.*;
import edu.dosw.proyecto.style_radar.validator.AlmacenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlmacenServiceImpl implements IAlmacenService {
    private final AlmacenRepository almacenes;
    private final ItemCatalogoRepository items;
    private final AlmacenEntityMapper almacenMapper;
    private final ItemCatalogoEntityMapper itemMapper;
    private final EstadoItemCalculator estados;
    private final DistanciaCalculator distancias;
    private final AlmacenValidator validator;
    private final Clock clock;

    @Override
    public Page<Almacen> consultar(CategoriaAlmacen categoria, int page, int size) {
        Page<String> ids = almacenes.findGeolocalizables(categoria, PageRequest.of(page, size));
        if (ids.isEmpty()) {
            return ids.map(id -> (Almacen) null);
        }
        Map<String, AlmacenEntity> porNit = almacenes.findByNitIn(ids.getContent()).stream()
                .collect(Collectors.toMap(AlmacenEntity::getNit, Function.identity()));
        log.info("Consulta de almacenes geolocalizables: página {}, total {}", page, ids.getTotalElements());
        return ids.map(id -> almacenMapper.toDomain(porNit.get(id)));
    }

    /** Muestra provisional: publicaciones disponibles más recientes; sin selección editorial. */
    @Override
    public List<ItemCatalogo> resumen(String nit, int limite) {
        validarExistencia(nit);
        return catalogo(nit, null, null, 0, limite).getContent();
    }

    @Override
    public Page<ItemCatalogo> novedades(String nit, int page, int size) {
        validarExistencia(nit);
        Instant fin = clock.instant();
        return catalogo(nit, fin.minus(Duration.ofDays(7)), fin, page, size);
    }

    @Override
    public DistanciaAlmacen distancia(String nit, double latitudUsuario, double longitudUsuario) {
        Almacen almacen = almacenMapper.toDomain(almacenes.findById(nit).orElseThrow(() -> noExiste(nit)));
        validator.validarCoordenadas(almacen);
        return new DistanciaAlmacen(nit, distancias.calcularKm(latitudUsuario, longitudUsuario,
                almacen.getLatitud(), almacen.getLongitud()));
    }

    private Page<ItemCatalogo> catalogo(String nit, Instant inicio, Instant fin, int page, int size) {
        Page<Long> ids = items.findIdsPublicosDelAlmacen(nit, inicio, fin, PageRequest.of(page, size));
        if (ids.isEmpty()) {
            return ids.map(id -> (ItemCatalogo) null);
        }
        Map<Long, ItemCatalogoEntity> porId = items.findByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(ItemCatalogoEntity::getId, Function.identity()));
        log.info("Consulta pública de catálogo de almacén: página {}, total {}", page, ids.getTotalElements());
        return ids.map(id -> {
            ItemCatalogo item = itemMapper.toDomain(porId.get(id));
            item.setEstado(estados.calcular(item));
            return item;
        });
    }

    private void validarExistencia(String nit) {
        if (!almacenes.existsById(nit)) {
            throw noExiste(nit);
        }
    }

    private RecursoNoEncontradoException noExiste(String nit) {
        log.warn("Almacén inexistente: {}", nit);
        return new RecursoNoEncontradoException("No existe un almacén con NIT " + nit);
    }
}
