package edu.dosw.proyecto.style_radar.controller.docs;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.proyecto.style_radar.model.dto.response.ImagenCatalogoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fotografías de catálogo", description = "Registro de fotografías asociadas a un item")
public interface ImagenCatalogoApi {

    @Operation(summary = "Registrar fotografía", description = "Almacena una fotografía para el item indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fotografía registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "Parte multipart ausente o inválida"),
            @ApiResponse(responseCode = "404", description = "Almacén o item no encontrado"),
            @ApiResponse(responseCode = "422", description = "Archivo vacío o tipo de contenido no válido")
    })
    @PostMapping(value = "/{itemId}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ImagenCatalogoResponseDTO> registrar(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @RequestPart("archivo") MultipartFile archivo);
}
