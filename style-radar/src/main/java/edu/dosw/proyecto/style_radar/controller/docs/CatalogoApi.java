package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.dto.request.CatalogoItemRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@Tag(name = "Catálogo", description = "Operaciones del catálogo de un almacén")
public interface CatalogoApi {

    @Operation(summary = "Consultar catálogo", description = "Retorna los ítems publicados por el almacén.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo consultado correctamente"),
            @ApiResponse(responseCode = "404", description = "Almacén no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    ResponseEntity<List<CatalogoItemResponseDTO>> obtenerCatalogo(@PathVariable String nit);

    @Operation(summary = "Publicar prenda", description = "Crea una prenda y la publica en el catálogo del almacén.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prenda publicada correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Almacén no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping
    ResponseEntity<CatalogoItemResponseDTO> publicar(
            @PathVariable String nit,
            @Valid @RequestBody CatalogoItemRequestDTO request);

    @Operation(summary = "Editar prenda del catálogo", description = "Actualiza los datos básicos y el precio de un ítem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ítem actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Almacén o ítem no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping("/{itemId}")
    ResponseEntity<CatalogoItemResponseDTO> actualizar(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @Valid @RequestBody CatalogoItemRequestDTO request);

    @Operation(summary = "Retirar prenda", description = "Elimina el ítem del catálogo sin eliminar la prenda asociada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ítem retirado correctamente"),
            @ApiResponse(responseCode = "404", description = "Almacén o ítem no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/{itemId}")
    ResponseEntity<Void> retirar(@PathVariable String nit, @PathVariable Long itemId);
}
