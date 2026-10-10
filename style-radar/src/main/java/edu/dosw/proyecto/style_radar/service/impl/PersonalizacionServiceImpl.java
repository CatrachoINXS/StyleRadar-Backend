package edu.dosw.proyecto.style_radar.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.ResultadoRecomendaciones;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.BusquedaGuardadaRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;
import edu.dosw.proyecto.style_radar.service.IPersonalizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import edu.dosw.proyecto.style_radar.service.CriteriosBusquedaGuardada;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class PersonalizacionServiceImpl implements IPersonalizacionService {
    private final UsuarioRepository usuarios;
    private final BusquedaGuardadaRepository busquedas;
    private final ItemCatalogoRepository items;
    private final ItemCatalogoEntityMapper mapper;
    private final EstadoItemCalculator estados;

    @Override
    public Page<ItemCatalogo> obtenerFeed(Long usuarioId, int page, int size) {
        var usuario = usuario(usuarioId);
        var criterios = busquedas.findByUsuario_IdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(CriteriosBusquedaGuardada::criterios).filter(CriteriosBusquedaGuardada::significativa).toList();
        var ids = items.findIdsFeed(usuario.getPreferenciasEstilo(), usuario.getTallasHabituales(), criterios,
                PageRequest.of(page, size));
        var resultado = cargar(ids);
        registrar(usuarioId, "feed", resultado);
        return resultado;
    }

    @Override
    public ResultadoRecomendaciones obtenerRecomendaciones(Long usuarioId, int page, int size) {
        var usuario = usuario(usuarioId);
        boolean general = usuario.getPreferenciasEstilo().isEmpty() && usuario.getTallasHabituales().isEmpty();
        var ids = items.findIdsRecomendaciones(usuario.getPreferenciasEstilo(), usuario.getTallasHabituales(),
                PageRequest.of(page, size));
        var resultado = cargar(ids);
        registrar(usuarioId, "recomendaciones", resultado);
        return new ResultadoRecomendaciones(resultado, general);
    }

    private UsuarioEntity usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> {
            log.warn("Usuario inexistente en personalizacion: {}", id);
            return new RecursoNoEncontradoException("No existe un usuario con ID: " + id);
        });
    }

    private Page<ItemCatalogo> cargar(Page<Long> ids) {
        var entidades = ids.isEmpty() ? List.<ItemCatalogoEntity>of() : items.findByIdIn(ids.getContent());
        var porId = entidades.stream().collect(Collectors.toMap(ItemCatalogoEntity::getId, Function.identity()));
        // IN no garantiza orden. Se conserva el orden global de la proyección paginada.
        return ids.map(id -> {
            var entity = porId.get(id);
            if (entity == null) {
                throw new IllegalStateException("El catalogo cambio durante la consulta de personalizacion");
            }
            var item = mapper.toDomain(entity);
            item.setEstado(estados.calcular(item));
            return item;
        });
    }

    private void registrar(Long usuarioId, String consulta, Page<?> resultado) {
        log.info("Personalizacion usuarioId={} consulta={} page={} size={} resultados={} total={}",
                usuarioId, consulta, resultado.getNumber(), resultado.getSize(),
                resultado.getNumberOfElements(), resultado.getTotalElements());
    }
}
