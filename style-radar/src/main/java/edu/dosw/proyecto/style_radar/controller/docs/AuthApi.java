package edu.dosw.proyecto.style_radar.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import edu.dosw.proyecto.style_radar.model.dto.request.LoginRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.RegistroAuthRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.LoginResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.UsuarioResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Autenticación")
public interface AuthApi {
    @PostMapping("/login")
    @Operation(summary = "RF02: iniciar sesión con correo y contraseña", security = {},
            responses = {@ApiResponse(responseCode = "200", description = "JWT Bearer; expiresIn en segundos"),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
                    @ApiResponse(responseCode = "401", description = "Autenticación inválida")})
    ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request);

    @PostMapping("/registro")
    @Operation(summary = "Registrar un nuevo comprador con contraseña", security = {},
            description = "Crea COMPRADOR/ACTIVA de forma atómica. No permite reclamar cuentas existentes.",
            responses = {@ApiResponse(responseCode = "201", description = "Comprador creado"),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
                    @ApiResponse(responseCode = "422", description = "Correo duplicado"),
                    @ApiResponse(responseCode = "409", description = "Conflicto concurrente")})
    ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroAuthRequestDTO request);
}
