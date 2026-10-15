package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.SuscripcionAlmacenMapper;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.proyecto.style_radar.model.entity.AdministradorAlmacenEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.AutorizacionSuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class AsociacionAdministradorAlmacenService {
    private final AutorizacionSuscripcionService autorizacion;
    private final UsuarioRepository usuarios;
    private final AlmacenRepository almacenes;
    private final AdministradorAlmacenRepository vinculos;
    private final SuscripcionAlmacenMapper mapper;
    private final Clock clock;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AdministradorAlmacenResponseDTO asociar(String nit, Long usuarioId, UsuarioPrincipal actor) {
        if (usuarioId == null || usuarioId < 1)
            throw new SolicitudSuscripcionInvalidaException("Identificador de usuario inválido");
        var bloqueados = autorizacion.bloquear(actor, actor == null ? Set.of(usuarioId)
                : new java.util.HashSet<>(java.util.List.of(actor.getUsuarioId(), usuarioId)), Rol.ADMIN_STYLERADAR);
        var usuario = bloqueados.get(usuarioId);
        if (usuario == null) throw new RecursoNoEncontradoException("Usuario inexistente");
        if (!usuarios.findRolesActuales(usuarioId).contains(Rol.ADMIN_ALMACEN))
            throw new ReglaDeNegocioException("El usuario debe tener el rol ADMIN_ALMACEN");
        var almacen = almacenes.findForUpdate(nit).orElseThrow(() -> new RecursoNoEncontradoException("Almacén inexistente"));
        var existente = vinculos.findByUsuario_IdAndAlmacen_Nit(usuarioId, nit);
        if (existente.isPresent()) return mapper.toResponse(existente.get());
        var vinculo = new AdministradorAlmacenEntity();
        vinculo.setUsuario(usuario); vinculo.setAlmacen(almacen); vinculo.setAsignadoPor(bloqueados.get(actor.getUsuarioId()));
        vinculo.setFechaAsignacion(clock.instant().truncatedTo(ChronoUnit.MICROS));
        vinculos.saveAndFlush(vinculo);
        log.info("Asociación administradorId={} usuarioId={} nit={} resultado=aplicado-en-transaccion",
                actor.getUsuarioId(), usuarioId, nit);
        return mapper.toResponse(vinculo);
    }
}
