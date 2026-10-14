package edu.dosw.proyecto.style_radar.support;

import java.time.Instant;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;

/** Autenticación para pruebas standalone de controllers (sin filtro JWT). */
public final class PrincipalFixtures {

    private PrincipalFixtures() {
    }

    public static AuthenticationPrincipalArgumentResolver autenticar(Long usuarioId, Rol rol) {
        UsuarioEntity usuario = UsuarioEntity.builder().id(usuarioId).nombre("Test").email("test@example.co")
                .fechaRegistro(Instant.now()).roles(Set.of(rol)).build();
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario, "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return new AuthenticationPrincipalArgumentResolver();
    }

    public static void limpiar() {
        SecurityContextHolder.clearContext();
    }
}
