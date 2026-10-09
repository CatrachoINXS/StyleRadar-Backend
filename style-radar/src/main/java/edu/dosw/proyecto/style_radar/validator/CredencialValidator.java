package edu.dosw.proyecto.style_radar.validator;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CredencialValidator {

    private final CredencialRepository credencialRepository;

    public void validarUsuarioId(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new ReglaDeNegocioException("El identificador de usuario no es válido");
        }
    }

    public void validarPassword(String password) {
        // BCrypt admite hasta 72 bytes. No recortar ni normalizar el secreto.
        if (password == null || password.isBlank() || password.indexOf('\0') >= 0
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ReglaDeNegocioException("La contraseña no cumple los límites admitidos");
        }
    }

    public void validarNoExiste(Long usuarioId) {
        if (credencialRepository.existsById(usuarioId)) {
            throw new ReglaDeNegocioException("El usuario ya tiene una credencial registrada");
        }
    }
}
