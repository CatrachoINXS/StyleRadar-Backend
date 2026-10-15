package edu.dosw.proyecto.style_radar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.dosw.proyecto.style_radar.controller.docs.AuthApi;
import edu.dosw.proyecto.style_radar.mapper.UsuarioMapper;
import edu.dosw.proyecto.style_radar.model.dto.request.LoginRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.RegistroAuthRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.LoginResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.UsuarioResponseDTO;
import edu.dosw.proyecto.style_radar.service.IAuthService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final IAuthService auth;
    private final UsuarioMapper usuarios;

    @Override
    public ResponseEntity<LoginResponseDTO> login(LoginRequestDTO request) {
        var result = auth.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new LoginResponseDTO(result.accessToken(), "Bearer", result.expiresIn(),
                result.usuarioId(), result.roles()));
    }

    @Override
    public ResponseEntity<UsuarioResponseDTO> registrar(RegistroAuthRequestDTO request) {
        var usuario = auth.registrarComprador(usuarios.toDomain(request), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarios.toResponse(usuario));
    }
}
