package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Segunda Mano", description = "Operaciones de compra/venta C2C, publicación, búsqueda y gestión de prendas usadas")
public interface SegundaManoApi {

    @Operation(summary = "Publicar prenda de segunda mano", description = "Publica una prenda usada con su estado de conservación y precio.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prenda de segunda mano publicada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio no cumplida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/api/v1/segunda-mano")
    ResponseEntity<PublicacionSegundaManoResponseDTO> publicar(
            @Valid @RequestBody CrearPublicacionSegundaManoRequestDTO request);

    @Operation(summary = "Consultar publicación por ID", description = "Retorna los detalles de una prenda de segunda mano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación consultada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/api/v1/segunda-mano/{id}")
    ResponseEntity<PublicacionSegundaManoResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Buscar prendas de segunda mano", description = "Búsqueda paginada y filtrada de prendas de segunda mano disponibles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsqueda realizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de búsqueda inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/api/v1/segunda-mano")
    ResponseEntity<PageResponseDTO<PublicacionSegundaManoResponseDTO>> buscar(
            @Valid @ModelAttribute BusquedaSegundaManoRequestDTO request);

    @Operation(summary = "Consultar publicaciones de un usuario", description = "Lista todas las publicaciones de segunda mano creadas por un usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicaciones consultadas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/api/v1/usuarios/{usuarioId}/segunda-mano")
    ResponseEntity<List<PublicacionSegundaManoResponseDTO>> obtenerPorUsuario(@PathVariable Long usuarioId);

    @Operation(summary = "Editar publicación de segunda mano", description = "Actualiza los datos de una prenda usada disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario o publicación no encontrada"),
            @ApiResponse(responseCode = "422", description = "No se puede editar si está vendida o retirada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping("/api/v1/usuarios/{usuarioId}/segunda-mano/{id}")
    ResponseEntity<PublicacionSegundaManoResponseDTO> editar(
            @PathVariable Long usuarioId,
            @PathVariable Long id,
            @Valid @RequestBody EditarPublicacionSegundaManoRequestDTO request);

    @Operation(summary = "Marcar prenda como vendida", description = "Actualiza el estado de la publicación a VENDIDA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenda marcada como vendida"),
            @ApiResponse(responseCode = "404", description = "Usuario o publicación no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transición de estado inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PatchMapping("/api/v1/usuarios/{usuarioId}/segunda-mano/{id}/vender")
    ResponseEntity<PublicacionSegundaManoResponseDTO> marcarComoVendida(
            @PathVariable Long usuarioId,
            @PathVariable Long id);

    @Operation(summary = "Retirar publicación de segunda mano", description = "Retira la publicación del catálogo público.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicación retirada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario o publicación no encontrada"),
            @ApiResponse(responseCode = "422", description = "La publicación ya estaba retirada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/api/v1/usuarios/{usuarioId}/segunda-mano/{id}")
    ResponseEntity<Void> retirar(
            @PathVariable Long usuarioId,
            @PathVariable Long id);
}
