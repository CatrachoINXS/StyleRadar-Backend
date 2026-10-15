package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PublicacionSegundaManoResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Segunda Mano", description = "Publicación, búsqueda, venta y donación de prendas usadas")
public interface SegundaManoApi {

    @Operation(summary = "Publicar prenda de segunda mano",
            description = "ROLE_COMPRADOR. El vendedor es el usuario autenticado; publica con estado de conservación y precio.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prenda de segunda mano publicada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio no cumplida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/segunda-mano")
    ResponseEntity<PublicacionSegundaManoResponseDTO> publicar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearPublicacionSegundaManoRequestDTO request);

    @Operation(summary = "Consultar publicación por ID", description = "Cualquier usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación consultada exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/segunda-mano/{id}")
    ResponseEntity<PublicacionSegundaManoResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Buscar prendas de otros usuarios",
            description = "Cualquier usuario autenticado. Solo publicaciones DISPONIBLES, paginadas y filtrables.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsqueda realizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/segunda-mano")
    ResponseEntity<PageResponseDTO<PublicacionSegundaManoResponseDTO>> buscar(
            @Valid @ParameterObject @ModelAttribute BusquedaSegundaManoRequestDTO request);

    @Operation(summary = "Consultar mis publicaciones", description = "ROLE_COMPRADOR. Incluye todos los estados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicaciones consultadas exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/usuarios/me/segunda-mano")
    ResponseEntity<List<PublicacionSegundaManoResponseDTO>> obtenerMisPublicaciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Editar mi publicación", description = "ROLE_COMPRADOR. Solo el propietario; no aplica a vendidas, retiradas o donadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación editada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "422", description = "No es el propietario o el estado no permite editar"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PutMapping("/api/v1/usuarios/me/segunda-mano/{id}")
    ResponseEntity<PublicacionSegundaManoResponseDTO> editar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EditarPublicacionSegundaManoRequestDTO request);

    @Operation(summary = "Marcar mi publicación como vendida", description = "ROLE_COMPRADOR. Solo el propietario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación marcada como vendida"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "422", description = "No es el propietario o transición inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PatchMapping("/api/v1/usuarios/me/segunda-mano/{id}/vender")
    ResponseEntity<PublicacionSegundaManoResponseDTO> marcarComoVendida(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);

    @Operation(summary = "Marcar mi prenda para donación (RF-42)",
            description = "ROLE_COMPRADOR. Solo el propietario y solo desde DISPONIBLE. La prenda deja de ofrecerse en venta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación destinada a donación"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "422", description = "No es el propietario o la publicación no está disponible"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PatchMapping("/api/v1/usuarios/me/segunda-mano/{id}/donacion")
    ResponseEntity<PublicacionSegundaManoResponseDTO> marcarParaDonacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);

    @Operation(summary = "Retirar mi publicación", description = "ROLE_COMPRADOR. Solo el propietario.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicación retirada exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "422", description = "No es el propietario o transición inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @DeleteMapping("/api/v1/usuarios/me/segunda-mano/{id}")
    ResponseEntity<Void> retirar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);
}
