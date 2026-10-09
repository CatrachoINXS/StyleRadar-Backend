package edu.dosw.proyecto.style_radar.security;

import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

public final class UsuarioPrincipal extends User {
    private final Long usuarioId;
    private final Set<Rol> roles;

    public UsuarioPrincipal(UsuarioEntity usuario, String hash) {
        super(usuario.getId().toString(), hash,
                usuario.getEstadoCuenta() == null || usuario.getEstadoCuenta() == EstadoCuenta.ACTIVA,
                true, true, usuario.getEstadoCuenta() != EstadoCuenta.BLOQUEADA,
                effectiveRoles(usuario).stream().map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name())).toList());
        this.usuarioId = usuario.getId();
        this.roles = effectiveRoles(usuario);
    }

    private static Set<Rol> effectiveRoles(UsuarioEntity usuario) {
        return usuario.getRoles() == null || usuario.getRoles().isEmpty()
                ? Set.of(Rol.COMPRADOR) : Set.copyOf(usuario.getRoles());
    }

    public Long getUsuarioId() { return usuarioId; }
    public Set<Rol> getRoles() { return roles; }
}
