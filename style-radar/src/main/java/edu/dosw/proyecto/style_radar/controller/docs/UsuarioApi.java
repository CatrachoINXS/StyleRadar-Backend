package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Usuarios", description = "Operaciones de registro, perfil, preferencias y búsquedas guardadas de usuarios")
public interface UsuarioApi {

    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en la plataforma.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "422", description = "El email ya está en uso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping
    ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistrarUsuarioRequestDTO request);

    @Operation(summary = "Consultar usuario por ID", description = "Retorna el perfil y configuración de un usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario consultado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}")
    ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Configurar preferencias de estilo", description = "Actualiza las preferencias de estilo favoritas de un usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias de estilo actualizadas"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping("/{id}/preferencias-estilo")
    ResponseEntity<UsuarioResponseDTO> actualizarPreferenciasEstilo(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPreferenciasEstiloRequestDTO request);

    @Operation(summary = "Configurar tallas habituales", description = "Actualiza las tallas habituales de un usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tallas habituales actualizadas"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping("/{id}/tallas-habituales")
    ResponseEntity<UsuarioResponseDTO> actualizarTallasHabituales(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarTallasHabitualesRequestDTO request);

    @Operation(summary = "Guardar búsqueda en el perfil", description = "Almacena los filtros o consulta de búsqueda frecuente del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Búsqueda guardada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/{id}/busquedas-guardadas")
    ResponseEntity<BusquedaGuardadaResponseDTO> guardarBusqueda(
            @PathVariable Long id,
            @Valid @RequestBody GuardarBusquedaRequestDTO request);

    @Operation(summary = "Consultar búsquedas guardadas", description = "Retorna el historial de búsquedas guardadas de un usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsquedas guardadas consultadas"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}/busquedas-guardadas")
    ResponseEntity<List<BusquedaGuardadaResponseDTO>> obtenerBusquedasGuardadas(@PathVariable Long id);

    @Operation(summary = "Eliminar búsqueda guardada", description = "Elimina una búsqueda guardada del perfil del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Búsqueda guardada eliminada"),
            @ApiResponse(responseCode = "404", description = "Usuario o búsqueda no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/{id}/busquedas-guardadas/{busquedaId}")
    ResponseEntity<Void> eliminarBusquedaGuardada(
            @PathVariable Long id,
            @PathVariable Long busquedaId);
}
