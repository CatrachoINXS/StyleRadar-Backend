package edu.dosw.proyecto.style_radar.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarStockRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.TallasDisponiblesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Inventario", description = "Registro de tallas y disponibilidad por talla")
public interface InventarioApi {

    @Operation(summary = "Registrar tallas", description = "Reemplaza el conjunto de tallas registradas del item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tallas registradas correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Almacén o item no encontrado"),
            @ApiResponse(responseCode = "422", description = "No se puede retirar una talla con unidades")
    })
    @PutMapping("/{itemId}/tallas")
    ResponseEntity<CatalogoItemResponseDTO> registrarTallas(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @Valid @RequestBody TallasDisponiblesRequestDTO request);

    @Operation(summary = "Actualizar disponibilidad", description = "Define las unidades finales de una talla registrada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud o talla inválida"),
            @ApiResponse(responseCode = "404", description = "Almacén o item no encontrado"),
            @ApiResponse(responseCode = "422", description = "Talla no registrada")
    })
    @PatchMapping("/{itemId}/inventario/{talla}")
    ResponseEntity<CatalogoItemResponseDTO> actualizarDisponibilidad(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @PathVariable Talla talla,
            @Valid @RequestBody ActualizarStockRequestDTO request);
}
