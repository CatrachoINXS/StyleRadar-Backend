package edu.dosw.proyecto.style_radar.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.*;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.*;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;

/** Revalida cuenta/roles actuales. En escrituras comparte los locks de RF66 antes del NIT. */
@Service @RequiredArgsConstructor
public class AutorizacionSuscripcionService {
    private final UsuarioRepository usuarios;
    private final AdministradorAlmacenRepository vinculos;
    private final BloqueoCuentaService bloqueo;

    @Transactional(propagation = Propagation.MANDATORY)
    public void autorizar(String nit, UsuarioPrincipal actor, boolean escritura) {
        exigirRol(actor, Rol.ADMIN_ALMACEN);
        if (escritura) bloquear(actor, List.of(actor.getUsuarioId()), Rol.ADMIN_ALMACEN);
        else validarActual(actor.getUsuarioId(), Rol.ADMIN_ALMACEN);
        if (!vinculos.existsByUsuario_IdAndAlmacen_Nit(actor.getUsuarioId(), nit))
            throw new RecursoNoEncontradoException("Almacén no disponible");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, UsuarioEntity> bloquear(UsuarioPrincipal actor, Collection<Long> ids, Rol rol) {
        exigirRol(actor, rol);
        var resultado = bloqueo.bloquear(ids);
        if (!resultado.containsKey(actor.getUsuarioId()))
            throw new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE);
        validarActual(actor.getUsuarioId(), rol);
        return resultado;
    }

    private void exigirRol(UsuarioPrincipal actor, Rol rol) {
        if (actor == null) throw new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE);
        if (!actor.getRoles().contains(rol)) throw new AccessDeniedException("Se requiere ROLE_" + rol);
    }

    private void validarActual(Long id, Rol rol) {
        var actual = usuarios.findDatosActuales(id).orElseThrow(() ->
                new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
        // Misma compatibilidad de cuentas históricas NULL que RF66; no aplica a verificación RF62.
        if (actual.estadoCuenta() != null && actual.estadoCuenta() != EstadoCuenta.ACTIVA)
            throw new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE);
        if (!usuarios.findRolesActuales(id).contains(rol)) throw new AccessDeniedException("Se requiere ROLE_" + rol);
    }
}
