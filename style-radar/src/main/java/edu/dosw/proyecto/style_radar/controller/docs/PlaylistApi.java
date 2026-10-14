package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.model.dto.request.PlaylistPopularesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import edu.dosw.proyecto.style_radar.model.dto.request.AgregarPrendaPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.InteractuarPlaylistRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PlaylistResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Playlists", description = "Operaciones de creación, curaduría de colecciones de moda, prendas e interacción social")
public interface PlaylistApi {

    @Operation(summary = "Crear playlist", description = "Crea una nueva playlist con visibilidad pública o privada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Playlist creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/api/v1/playlists")
    ResponseEntity<PlaylistResponseDTO> crear(@Valid @RequestBody CrearPlaylistRequestDTO request);

    @Operation(summary = "Consultar playlists públicas", description = "Lista todas las playlists públicas de la comunidad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlists públicas consultadas exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
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
    @GetMapping("/api/v1/playlists/populares")
    ResponseEntity<PageResponseDTO<PlaylistResponseDTO>> obtenerPopulares(
            @Valid @ModelAttribute @ParameterObject PlaylistPopularesRequestDTO request);

    @Operation(summary = "Consultar playlist por ID", description = "Obtiene los detalles y prendas de una playlist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlist consultada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "Acceso no autorizado a playlist privada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/api/v1/playlists/{id}")
    ResponseEntity<PlaylistResponseDTO> obtenerPorId(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioConsultaId);

    @Operation(summary = "Consultar playlists de un usuario", description = "Lista todas las playlists creadas por un usuario específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlists del usuario consultadas"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/api/v1/usuarios/{usuarioId}/playlists")
    ResponseEntity<List<PlaylistResponseDTO>> obtenerPorUsuario(@PathVariable Long usuarioId);

    @Operation(summary = "Editar playlist", description = "Actualiza nombre, descripción, visibilidad o estilo de la playlist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlist actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario o playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "La playlist no pertenece al usuario"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping("/api/v1/usuarios/{usuarioId}/playlists/{id}")
    ResponseEntity<PlaylistResponseDTO> editar(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @Valid @RequestBody EditarPlaylistRequestDTO request);

    @Operation(summary = "Agregar prenda a playlist", description = "Añade una prenda existente a una playlist del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenda agregada a la playlist"),
            @ApiResponse(responseCode = "404", description = "Usuario, playlist o prenda no encontrada"),
            @ApiResponse(responseCode = "422", description = "La prenda ya está en la playlist o permiso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/api/v1/usuarios/{usuarioId}/playlists/{id}/prendas")
    ResponseEntity<PlaylistResponseDTO> agregarPrenda(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @Valid @RequestBody AgregarPrendaPlaylistRequestDTO request);

    @Operation(summary = "Retirar prenda de playlist", description = "Elimina una prenda de la playlist del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenda retirada de la playlist"),
            @ApiResponse(responseCode = "404", description = "Usuario o playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "La prenda no pertenece a la playlist o permiso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/api/v1/usuarios/{usuarioId}/playlists/{id}/prendas/{prendaId}")
    ResponseEntity<PlaylistResponseDTO> retirarPrenda(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @PathVariable Long prendaId);

    @Operation(summary = "Interactuar con playlist (Dar o quitar Like)", description = "Permite a un usuario dar o quitar like a una playlist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interacción registrada exitosamente"),
            @ApiResponse(responseCode = "403", description = "usuarioId no corresponde al principal JWT"),
            @ApiResponse(responseCode = "404", description = "Usuario o playlist no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/api/v1/playlists/{id}/like")
    ResponseEntity<PlaylistResponseDTO> alternarLike(
            @PathVariable Long id,
            @Valid @RequestBody InteractuarPlaylistRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Eliminar playlist", description = "Elimina una playlist del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Playlist eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario o playlist no encontrada"),
            @ApiResponse(responseCode = "422", description = "La playlist no pertenece al usuario"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/api/v1/usuarios/{usuarioId}/playlists/{id}")
    ResponseEntity<Void> eliminar(
            @PathVariable Long usuarioId,
            @PathVariable Long id);
}
