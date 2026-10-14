package edu.dosw.proyecto.style_radar.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Identidad HTTP ya validada por JwtAuthFilter; los IDs del cliente no otorgan autoridad. */
@Component
public class IdentidadAutenticada {
    public Long usuarioId() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new AccessDeniedException("Acceso denegado");
        }
        return principal.getUsuarioId();
    }

    public void exigirUsuario(Long usuarioId) {
        if (!usuarioId().equals(usuarioId)) throw new AccessDeniedException("Acceso denegado");
    }
}
