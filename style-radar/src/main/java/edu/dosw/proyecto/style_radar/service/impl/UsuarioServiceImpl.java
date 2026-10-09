package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.BusquedaGuardadaEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.UsuarioEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.entity.BusquedaGuardadaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.BusquedaGuardadaRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.IUsuarioService;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BusquedaGuardadaRepository busquedaGuardadaRepository;
    private final UsuarioEntityMapper usuarioEntityMapper;
    private final BusquedaGuardadaEntityMapper busquedaGuardadaEntityMapper;
    private final UsuarioValidator usuarioValidator;
    private final Clock clock;

    @Override
    @Transactional
    public Usuario registrarUsuario(Usuario usuario) {
        log.info("Registrando usuario con email: {}", usuario.getEmail());
        usuarioValidator.validarEmailUnico(usuario.getEmail());

        UsuarioEntity entity = usuarioEntityMapper.toEntity(usuario);
        // Este servicio conserva el registro público; nunca aprovisiona roles privilegiados.
        entity.setRoles(new HashSet<>(Set.of(Rol.COMPRADOR)));
        entity.setEstadoCuenta(EstadoCuenta.ACTIVA);
        entity.setFechaRegistro(clock.instant());
        if (entity.getPreferenciasEstilo() == null) {
            entity.setPreferenciasEstilo(new HashSet<>());
        }
        if (entity.getTallasHabituales() == null) {
            entity.setTallasHabituales(new HashSet<>());
        }

        UsuarioEntity guardado = usuarioRepository.save(entity);
        log.info("Usuario registrado exitosamente con ID: {}", guardado.getId());
        return usuarioEntityMapper.toDomain(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        log.info("Consultando usuario con ID: {}", id);
        UsuarioEntity entity = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No se encontró usuario con ID: {}", id);
                    return new RecursoNoEncontradoException("No existe un usuario con ID: " + id);
                });
        return usuarioEntityMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public Usuario actualizarPreferenciasEstilo(Long id, Set<Estilo> preferencias) {
        log.info("Actualizando preferencias de estilo para el usuario con ID: {}", id);
        UsuarioEntity entity = obtenerEntityPorId(id);
        entity.setPreferenciasEstilo(preferencias != null ? new HashSet<>(preferencias) : new HashSet<>());
        UsuarioEntity actualizado = usuarioRepository.save(entity);
        log.info("Preferencias de estilo actualizadas para el usuario ID: {}", id);
        return usuarioEntityMapper.toDomain(actualizado);
    }

    @Override
    @Transactional
    public Usuario actualizarTallasHabituales(Long id, Set<Talla> tallas) {
        log.info("Actualizando tallas habituales para el usuario con ID: {}", id);
        UsuarioEntity entity = obtenerEntityPorId(id);
        entity.setTallasHabituales(tallas != null ? new HashSet<>(tallas) : new HashSet<>());
        UsuarioEntity actualizado = usuarioRepository.save(entity);
        log.info("Tallas habituales actualizadas para el usuario ID: {}", id);
        return usuarioEntityMapper.toDomain(actualizado);
    }

    @Override
    @Transactional
    public BusquedaGuardada guardarBusqueda(Long usuarioId, BusquedaGuardada busqueda) {
        log.info("Guardando búsqueda '{}' para el usuario con ID: {}", busqueda.getNombre(), usuarioId);
        UsuarioEntity usuario = obtenerEntityPorId(usuarioId);

        BusquedaGuardadaEntity entity = busquedaGuardadaEntityMapper.toEntity(busqueda);
        entity.setUsuario(usuario);
        entity.setFechaCreacion(clock.instant());

        BusquedaGuardadaEntity guardada = busquedaGuardadaRepository.save(entity);
        log.info("Búsqueda guardada exitosamente con ID: {} para el usuario ID: {}", guardada.getId(), usuarioId);
        return busquedaGuardadaEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusquedaGuardada> obtenerBusquedasGuardadas(Long usuarioId) {
        log.info("Consultando búsquedas guardadas del usuario con ID: {}", usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        List<BusquedaGuardada> busquedas = busquedaGuardadaRepository
                .findByUsuario_IdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(busquedaGuardadaEntityMapper::toDomain)
                .toList();
        log.info("Búsquedas guardadas obtenidas para usuario ID {}: {}", usuarioId, busquedas.size());
        return busquedas;
    }

    @Override
    @Transactional
    public void eliminarBusquedaGuardada(Long usuarioId, Long busquedaId) {
        log.info("Eliminando búsqueda guardada ID: {} para usuario ID: {}", busquedaId, usuarioId);
        usuarioValidator.validarExiste(usuarioId);
        BusquedaGuardadaEntity entity = busquedaGuardadaRepository.findByIdAndUsuario_Id(busquedaId, usuarioId)
                .orElseThrow(() -> {
                    log.warn("No existe la búsqueda guardada ID: {} para el usuario ID: {}", busquedaId, usuarioId);
                    return new RecursoNoEncontradoException(
                            "No existe la búsqueda guardada con ID: " + busquedaId + " para el usuario: " + usuarioId);
                });
        busquedaGuardadaRepository.delete(entity);
        log.info("Búsqueda guardada ID: {} eliminada exitosamente", busquedaId);
    }

    private UsuarioEntity obtenerEntityPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No existe usuario con ID: {}", id);
                    return new RecursoNoEncontradoException("No existe un usuario con ID: " + id);
                });
    }
}
