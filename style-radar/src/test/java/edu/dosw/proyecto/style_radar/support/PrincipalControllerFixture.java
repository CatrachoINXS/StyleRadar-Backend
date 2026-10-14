package edu.dosw.proyecto.style_radar.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;

/** Fixture de controller standalone; no constituye evidencia de autenticación JWT. */
public final class PrincipalControllerFixture {
    private PrincipalControllerFixture() {}
    public static void autenticar(Long id) {
        var principal = new UsuarioPrincipal(UsuarioEntity.builder().id(id).build(), "fixture");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
