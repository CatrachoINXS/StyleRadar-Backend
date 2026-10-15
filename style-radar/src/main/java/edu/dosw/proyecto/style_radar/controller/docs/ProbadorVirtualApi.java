package edu.dosw.proyecto.style_radar.controller.docs;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.proyecto.style_radar.model.dto.request.PruebaVirtualRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.FotoProbadorResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Probador virtual", description = "Composición aproximada de una prenda del catálogo sobre la foto frontal del usuario")
@SecurityRequirement(name = "bearerAuth")
public interface ProbadorVirtualApi {

    @Operation(summary = "Cargar fotografía frontal (RF-23)",
            description = "ROLE_COMPRADOR. JPEG o PNG, máximo 5 MB, mínimo 300x400 px y orientación vertical. Reemplaza la anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fotografía cargada"),
            @ApiResponse(responseCode = "400", description = "Falta la parte multipart 'foto'"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "413", description = "Archivo mayor al límite del servidor"),
            @ApiResponse(responseCode = "422", description = "Formato, tamaño, dimensiones u orientación inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping(value = "/api/v1/usuarios/me/probador/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FotoProbadorResponseDTO> cargarFoto(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestPart("foto") MultipartFile foto);

    @Operation(summary = "Consultar mi fotografía cargada", description = "ROLE_COMPRADOR. Solo metadatos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fotografía cargada"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "No hay fotografía cargada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/usuarios/me/probador/foto")
    ResponseEntity<FotoProbadorResponseDTO> obtenerFoto(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Probar una prenda del catálogo (RF-24, RF-25, RF-26)",
            description = "ROLE_COMPRADOR. Prendas SUPERIOR, INFERIOR o VESTIDO con foto. Devuelve la composición en PNG; "
                    + "para probar otra prenda se vuelve a invocar con otro itemCatalogoId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Composición generada",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Sin fotografía cargada o prenda inexistente"),
            @ApiResponse(responseCode = "422", description = "Prenda incompatible o sin fotografía"),
            @ApiResponse(responseCode = "500", description = "Error interno; el usuario puede reintentar")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping(value = "/api/v1/usuarios/me/probador/pruebas", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> probar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody PruebaVirtualRequestDTO request);

    @Operation(summary = "Eliminar mi fotografía del probador (RF-27)", description = "ROLE_COMPRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fotografía eliminada"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "No hay fotografía cargada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @DeleteMapping("/api/v1/usuarios/me/probador/foto")
    ResponseEntity<Void> eliminarFoto(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);
}
