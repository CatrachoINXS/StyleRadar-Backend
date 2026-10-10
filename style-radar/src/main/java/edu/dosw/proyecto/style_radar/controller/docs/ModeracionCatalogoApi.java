package edu.dosw.proyecto.style_radar.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Moderación de catálogo", description = "RF61. Requiere ROLE_ADMIN_STYLERADAR")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({@ApiResponse(responseCode = "200", description = "Operación correcta"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o cuenta bloqueada"),
        @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR"),
        @ApiResponse(responseCode = "404", description = "Publicación inexistente"),
        @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia"),
        @ApiResponse(responseCode = "422", description = "Transición inválida o revisión repetida"),
        @ApiResponse(responseCode = "500", description = "Fallo técnico")})
public interface ModeracionCatalogoApi {
    @Operation(summary = "Consultar publicaciones para moderación", description = "ROLE_ADMIN_STYLERADAR. "
            + "estado=PENDIENTE, page=0, size=20; máximo 100. Orden fechaPublicacion ASC, itemId ASC. "
            + "NULL histórico se incluye en NO_REQUERIDA.")
    @GetMapping("/moderacion")
    ResponseEntity<PageResponseDTO<ModeracionItemResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute ModeracionConsultaRequestDTO request);

    @Operation(summary = "Aprobar o rechazar publicación pendiente", description = "ROLE_ADMIN_STYLERADAR. "
            + "Identidad y fecha del servidor. Rechazar exige motivo de 1 a 1000 caracteres; aprobación lo permite omitir.")
    @PatchMapping("/{itemId}/moderacion")
    ResponseEntity<ModeracionItemResponseDTO> decidir(@PathVariable Long itemId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "APROBADA", value = "{\"decision\":\"APROBADA\",\"motivo\":\"Información revisada\"}"),
                    @ExampleObject(name = "RECHAZADA", value = "{\"decision\":\"RECHAZADA\",\"motivo\":\"Información incompleta\"}")}))
            @Valid @RequestBody DecisionModeracionRequestDTO request);

    @Operation(summary = "Solicitar revisión manual", description = "ROLE_ADMIN_STYLERADAR. "
            + "Establece PENDIENTE (ejemplo: estadoModeracion=PENDIENTE). Sin body. "
            + "Permite nueva revisión tras decisión; PENDIENTE repetido devuelve 422. Conserva la última decisión.")
    @PostMapping("/{itemId}/moderacion/solicitar-revision")
    ResponseEntity<ModeracionItemResponseDTO> solicitarRevision(@PathVariable Long itemId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);
}
