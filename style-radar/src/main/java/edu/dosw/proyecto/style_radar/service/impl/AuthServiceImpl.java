package edu.dosw.proyecto.style_radar.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.model.domain.LoginResult;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.security.EmailNormalizer;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.IAuthService;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.service.IUsuarioService;
import edu.dosw.proyecto.style_radar.validator.CredencialValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwt;
    private final IUsuarioService usuarios;
    private final ICredencialService credenciales;
    private final CredencialValidator credencialValidator;

    @Override
    public LoginResult login(String email, String password) {
        try {
            var auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(EmailNormalizer.normalize(email), password));
            var principal = (UsuarioPrincipal) auth.getPrincipal();
            log.info("Sesión iniciada para usuario ID {}", principal.getUsuarioId());
            return new LoginResult(jwt.generate(principal.getUsuarioId()), jwt.expiresIn(),
                    principal.getUsuarioId(), principal.getRoles());
        } catch (InternalAuthenticationServiceException exception) {
            log.error("Error inesperado durante autenticación");
            throw exception;
        } catch (AccountStatusException exception) {
            log.warn("Estado de cuenta no permitido durante autenticación");
            throw exception;
        } catch (AuthenticationException exception) {
            log.warn("Intento de autenticación rechazado");
            throw exception;
        }
    }

    @Override
    @Transactional
    public Usuario registrarComprador(Usuario usuario, String password) {
        credencialValidator.validarPassword(password);
        // El registro existente impone COMPRADOR/ACTIVA y valida unicidad.
        Usuario registrado = usuarios.registrarUsuario(usuario);
        credenciales.crearCredencial(registrado.getId(), password);
        return registrado;
    }
}
