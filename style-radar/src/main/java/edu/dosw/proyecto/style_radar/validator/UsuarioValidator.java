package edu.dosw.proyecto.style_radar.validator;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UsuarioValidator {

    private final UsuarioRepository usuarioRepository;

    public void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaDeNegocioException("Ya existe un usuario registrado con el email: " + email);
        }
    }

    public void validarExiste(Long usuarioId) {
        if (usuarioId == null || !usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("No existe un usuario con ID: " + usuarioId);
        }
    }
}
