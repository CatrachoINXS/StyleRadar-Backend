package edu.dosw.proyecto.style_radar.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.model.entity.CredencialEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.validator.CredencialValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CredencialServiceImpl implements ICredencialService {

    private final CredencialRepository credencialRepository;
    private final UsuarioRepository usuarioRepository;
    private final CredencialValidator credencialValidator;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void crearCredencial(Long usuarioId, String password) {
        credencialValidator.validarUsuarioId(usuarioId);
        credencialValidator.validarPassword(password);
        UsuarioEntity usuario = obtenerUsuario(usuarioId);
        credencialValidator.validarNoExiste(usuarioId);

        // ID inicialmente null: save persiste una entidad nueva; la PK también evita duplicados concurrentes.
        credencialRepository.saveAndFlush(new CredencialEntity(usuario, passwordEncoder.encode(password)));
        log.info("Credencial registrada para el usuario ID {}", usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarPassword(Long usuarioId, String password) {
        credencialValidator.validarUsuarioId(usuarioId);
        credencialValidator.validarPassword(password);
        obtenerUsuario(usuarioId);
        return credencialRepository.findById(usuarioId)
                .map(credencial -> passwordEncoder.matches(password, credencial.getPasswordHash()))
                .orElse(false);
    }

    private UsuarioEntity obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con ID: " + usuarioId));
    }
}
