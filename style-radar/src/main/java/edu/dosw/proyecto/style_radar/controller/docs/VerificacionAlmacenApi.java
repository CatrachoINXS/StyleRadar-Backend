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

@Tag(name = "Verificación de almacenes", description = "RF62. Revisión de datos disponibles; requiere ROLE_ADMIN_STYLERADAR")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({@ApiResponse(responseCode = "200", description = "Operación correcta"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o cuenta bloqueada"),
        @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR"),
        @ApiResponse(responseCode = "404", description = "Almacén inexistente"),
        @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia"),
        @ApiResponse(responseCode = "422", description = "Transición inválida o revisión repetida"),
        @ApiResponse(responseCode = "500", description = "Fallo técnico")})
public interface VerificacionAlmacenApi {
    @Operation(summary = "Consultar almacenes para verificación", description = "ROLE_ADMIN_STYLERADAR. "
            + "estado opcional; sin filtro incluye NULL legado sin aprobación. Filtros muestran estados reales. "
            + "page=0, size=20; máximo 100. Orden nombreComercial ASC, nit ASC; incluye almacenes sin coordenadas.")
    @GetMapping("/verificacion")
    ResponseEntity<PageResponseDTO<VerificacionAlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute VerificacionConsultaRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Revisar datos de un almacén", description = "ROLE_ADMIN_STYLERADAR. "
            + "Datos existentes, contacto y última decisión; no realiza validación legal externa.")
    @GetMapping("/{nit}/verificacion")
    ResponseEntity<VerificacionAlmacenResponseDTO> consultarUno(@PathVariable String nit,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Aprobar o rechazar vinculación pendiente", description = "ROLE_ADMIN_STYLERADAR. "
            + "Solo PENDIENTE. Identidad y fecha del servidor. Rechazo exige motivo no vacío de máximo 1000 caracteres; "
            + "aprobación permite omitirlo. No aprueba publicaciones ni genera notificaciones retroactivas.")
    @PatchMapping("/{nit}/verificacion")
    ResponseEntity<VerificacionAlmacenResponseDTO> decidir(@PathVariable String nit,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "APROBADO", value = "{\"decision\":\"APROBADO\",\"motivo\":\"Datos revisados\"}"),
                    @ExampleObject(name = "RECHAZADO", value = "{\"decision\":\"RECHAZADO\",\"motivo\":\"Información insuficiente\"}")}))
            @Valid @RequestBody DecisionVerificacionRequestDTO request);

    @Operation(summary = "Iniciar o reabrir revisión", description = "ROLE_ADMIN_STYLERADAR. "
            + "NULL, APROBADO o RECHAZADO pasan a PENDIENTE; PENDIENTE repetido devuelve 422. Sin body. "
            + "Conserva metadatos de la última decisión hasta finalizar la nueva evaluación.")
    @PostMapping("/{nit}/verificacion/solicitar-revision")
    ResponseEntity<VerificacionAlmacenResponseDTO> solicitarRevision(@PathVariable String nit,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);
}
