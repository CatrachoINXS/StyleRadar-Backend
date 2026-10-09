package edu.dosw.proyecto.style_radar.security;

import java.io.IOException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final UsuarioUserDetailsService users;
    private final SecurityErrorHandler errors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer", 0, 6)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            if (header.length() < 8 || header.charAt(6) != ' ') {
                throw new BadCredentialsException("Bearer inválido");
            }
            Long id = validatedId(header.substring(7));
            var principal = users.loadActiveById(id);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities()));
            SecurityContextHolder.setContext(context);
        } catch (AccountStatusException exception) {
            SecurityContextHolder.clearContext();
            log.warn("Estado de cuenta no permitido durante autenticación Bearer");
            errors.commence(request, response, new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
            return;
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            log.warn("Autenticación Bearer rechazada");
            errors.commence(request, response, new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
            return;
        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();
            log.error("Error de persistencia durante autenticación Bearer");
            errors.serverError(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private Long validatedId(String token) {
        try {
            return jwt.validateAndGetUserId(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException(SecurityErrorHandler.UNAUTHORIZED_MESSAGE);
        }
    }
}
