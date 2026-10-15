package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarPreferenciasEstiloRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarTallasHabitualesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.GuardarBusquedaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.RegistrarUsuarioRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.BusquedaGuardadaResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.UsuarioResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Usuarios", description = "Registro, perfil, preferencias y búsquedas guardadas del usuario autenticado")
public interface UsuarioApi {

    @Operation(summary = "Registrar usuario", description = "Público. Crea un nuevo usuario en la plataforma.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "422", description = "El email ya está en uso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping
    ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistrarUsuarioRequestDTO request);

    @Operation(summary = "Consultar mi perfil", description = "ROLE_COMPRADOR. Retorna el perfil del usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil consultado exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/me")
    ResponseEntity<UsuarioResponseDTO> obtenerPerfil(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Consultar usuario por ID", description = "ROLE_ADMIN_STYLERADAR. Retorna el perfil de cualquier usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario consultado exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('ADMIN_STYLERADAR')")
    @GetMapping("/{id}")
    ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Configurar mis preferencias de estilo", description = "ROLE_COMPRADOR. Reemplaza las preferencias de estilo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias de estilo actualizadas"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PutMapping("/me/preferencias-estilo")
    ResponseEntity<UsuarioResponseDTO> actualizarPreferenciasEstilo(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ActualizarPreferenciasEstiloRequestDTO request);

    @Operation(summary = "Configurar mis tallas habituales", description = "ROLE_COMPRADOR. Reemplaza las tallas habituales.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tallas habituales actualizadas"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PutMapping("/me/tallas-habituales")
    ResponseEntity<UsuarioResponseDTO> actualizarTallasHabituales(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ActualizarTallasHabitualesRequestDTO request);

    @Operation(summary = "Guardar búsqueda en mi perfil", description = "ROLE_COMPRADOR. Almacena texto, filtros y ubicación de una búsqueda.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Búsqueda guardada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/me/busquedas-guardadas")
    ResponseEntity<BusquedaGuardadaResponseDTO> guardarBusqueda(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody GuardarBusquedaRequestDTO request);

    @Operation(summary = "Consultar mis búsquedas guardadas", description = "ROLE_COMPRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsquedas guardadas consultadas"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/me/busquedas-guardadas")
    ResponseEntity<List<BusquedaGuardadaResponseDTO>> obtenerBusquedasGuardadas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Eliminar mi búsqueda guardada", description = "ROLE_COMPRADOR. Solo búsquedas propias.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Búsqueda guardada eliminada"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Búsqueda no encontrada o ajena"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @DeleteMapping("/me/busquedas-guardadas/{busquedaId}")
    ResponseEntity<Void> eliminarBusquedaGuardada(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long busquedaId);
}
