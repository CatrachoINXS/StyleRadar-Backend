package edu.dosw.proyecto.style_radar.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Suscripciones de almacenes", description = "RF67/RF68. Registro sin confirmación de pagos")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({@ApiResponse(responseCode = "200", description = "Consulta o cancelación correcta"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o campos no permitidos"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido, expirado o cuenta suspendida"),
        @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_ALMACEN"),
        @ApiResponse(responseCode = "404", description = "Recurso inexistente, ajeno o sin suscripción vigente"),
        @ApiResponse(responseCode = "409", description = "Suscripción vigente duplicada o conflicto concurrente"),
        @ApiResponse(responseCode = "422", description = "Almacén no APROBADO, plan inactivo o configuración inválida"),
        @ApiResponse(responseCode = "500", description = "Fallo técnico")})
public interface SuscripcionAlmacenApi {
    @Operation(summary = "Registrar suscripción a un plan configurado", description = "ROLE_ADMIN_ALMACEN con cuenta activa "
            + "y asociación persistida al NIT. Exige RF62 APROBADO explícito; NULL histórico no es elegible. "
            + "Solo recibe planId existente. Período desde Clock UTC; una vigente por NIT. "
            + "No registra pagos ni facturas. La intención de renovación inicia habilitada, sin motor automático.")
    @ApiResponse(responseCode = "201", description = "Suscripción registrada, sin afirmar pago")
    @PostMapping
    ResponseEntity<SuscripcionAlmacenResponseDTO> crear(@PathVariable String nit,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @Valid @RequestBody CrearSuscripcionRequestDTO request);

    @Operation(summary = "Consultar suscripción vigente propia", description = "ROLE_ADMIN_ALMACEN con asociación "
            + "persistida al NIT. Vigencia [inicioPeriodo, finPeriodo) UTC; ausencia o vencimiento devuelve 404.")
    @GetMapping("/vigente")
    ResponseEntity<SuscripcionAlmacenResponseDTO> vigente(@PathVariable String nit,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor);

    @Operation(summary = "Cancelar la próxima renovación", description = "ROLE_ADMIN_ALMACEN con asociación persistida "
            + "al NIT. Solo renovacionHabilitada=false. La suscripción debe pertenecer al NIT y estar vigente. "
            + "Idempotente: conserva la primera fecha de cancelación, estado ACTIVA, plan e inicio/fin del período. "
            + "No cancela anticipadamente ni ejecuta cobros; true devuelve 400.")
    @PatchMapping("/{id}/renovacion")
    ResponseEntity<SuscripcionAlmacenResponseDTO> cancelar(@PathVariable String nit, @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @Valid @RequestBody CancelarRenovacionRequestDTO request);

    @Operation(summary = "Consultar historial propio", description = "ROLE_ADMIN_ALMACEN con asociación persistida "
            + "al NIT. page=0, size=20, máximo 100; fechaCreacion DESC, id DESC en BD. "
            + "Estado efectivo FINALIZADA al vencer, aunque la fila todavía conserve ACTIVA. Conserva historial.")
    @GetMapping
    ResponseEntity<PageResponseDTO<SuscripcionAlmacenResponseDTO>> historial(@PathVariable String nit,
            @Valid @ParameterObject @ModelAttribute SuscripcionesConsultaRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor);
}
