package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.dto.request.AgregarPrendaPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.PlaylistPopularesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PlaylistResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Playlists", description = "Colecciones de moda: creación, curaduría, likes y guardado en el perfil")
public interface PlaylistApi {

    @Operation(summary = "Crear playlist", description = "ROLE_COMPRADOR. El creador es el usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Playlist creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/playlists")
    ResponseEntity<PlaylistResponseDTO> crear(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearPlaylistRequestDTO request);

    @Operation(summary = "Consultar playlists públicas", description = "Cualquier usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlists públicas consultadas exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/playlists")
    ResponseEntity<List<PlaylistResponseDTO>> obtenerPublicas();

    @Operation(summary = "Consultar playlists populares (RF72)", description = "Solo playlists PUBLICA. "
            + "Orden: cantidad de likes únicos DESC, fechaCreacion DESC, ID ASC. Incluye cero likes y playlists "
            + "sin prendas visibles. PageResponseDTO contiene content, page, size, totalElements, totalPages, first y last. "
            + "Requiere autenticación Bearer JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de playlists públicas populares, posiblemente vacía"),
            @ApiResponse(responseCode = "400", description = "page >= 0; size entre 1 y 100; parámetros numéricos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido, expirado o cuenta no activa"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/playlists/populares")
    ResponseEntity<PageResponseDTO<PlaylistResponseDTO>> obtenerPopulares(
            @Valid @ModelAttribute @ParameterObject PlaylistPopularesRequestDTO request);


    @Operation(summary = "Consultar playlist por ID",
            description = "Cualquier usuario autenticado. Una playlist privada solo la ve su creador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlist consultada exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "404", description = "Playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "Playlist privada de otro usuario"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/playlists/{id}")
    ResponseEntity<PlaylistResponseDTO> obtenerPorId(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);

    @Operation(summary = "Consultar mis playlists", description = "ROLE_COMPRADOR. Incluye públicas y privadas propias.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlists consultadas exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/usuarios/me/playlists")
    ResponseEntity<List<PlaylistResponseDTO>> obtenerMisPlaylists(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Consultar playlists guardadas en mi perfil (RF-71)", description = "ROLE_COMPRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlists guardadas consultadas"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/usuarios/me/playlists-guardadas")
    ResponseEntity<List<PlaylistResponseDTO>> obtenerGuardadas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Editar mi playlist", description = "ROLE_COMPRADOR. Solo el creador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlist editada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "La playlist no es del usuario"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PutMapping("/api/v1/usuarios/me/playlists/{id}")
    ResponseEntity<PlaylistResponseDTO> editar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EditarPlaylistRequestDTO request);

    @Operation(summary = "Agregar prenda a mi playlist", description = "ROLE_COMPRADOR. Máximo 15 prendas por playlist (RNF-12).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenda agregada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Playlist o prenda no encontrada"),
            @ApiResponse(responseCode = "422", description = "Prenda repetida, límite de 15 alcanzado o playlist ajena"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/usuarios/me/playlists/{id}/prendas")
    ResponseEntity<PlaylistResponseDTO> agregarPrenda(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AgregarPrendaPlaylistRequestDTO request);

    @Operation(summary = "Retirar prenda de mi playlist", description = "ROLE_COMPRADOR. Solo el creador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenda retirada exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "La prenda no está en la playlist o playlist ajena"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @DeleteMapping("/api/v1/usuarios/me/playlists/{id}/prendas/{prendaId}")
    ResponseEntity<PlaylistResponseDTO> retirarPrenda(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long prendaId);

    @Operation(summary = "Dar o quitar Me gusta", description = "ROLE_COMPRADOR. Alterna el like sobre una playlist visible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Like alternado exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "Playlist privada de otro usuario"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/playlists/{id}/like")
    ResponseEntity<PlaylistResponseDTO> alternarLike(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);

    @Operation(summary = "Guardar o quitar playlist de mi perfil (RF-71)",
            description = "ROLE_COMPRADOR. Alterna el guardado de una playlist visible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guardado alternado exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "Playlist privada de otro usuario"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/playlists/{id}/guardar")
    ResponseEntity<PlaylistResponseDTO> alternarGuardado(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);

    @Operation(summary = "Eliminar mi playlist", description = "ROLE_COMPRADOR. Solo el creador.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Playlist eliminada exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "La playlist no es del usuario"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @DeleteMapping("/api/v1/usuarios/me/playlists/{id}")
    ResponseEntity<Void> eliminar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);
}
