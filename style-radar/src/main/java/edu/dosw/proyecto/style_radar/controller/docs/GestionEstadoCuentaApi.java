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

@Tag(name = "Estados de cuentas", description = "RF66. Requiere ROLE_ADMIN_STYLERADAR y cuenta activa")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({@ApiResponse(responseCode = "200", description = "Operación correcta"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o cuenta no activa"),
        @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR"),
        @ApiResponse(responseCode = "404", description = "Cuenta inexistente"),
        @ApiResponse(responseCode = "409", description = "Conflicto concurrente; reintente"),
        @ApiResponse(responseCode = "422", description = "Autocambio o pérdida del último administrador operativo"),
        @ApiResponse(responseCode = "500", description = "Error técnico")})
public interface GestionEstadoCuentaApi {
    @GetMapping
    @Operation(summary = "Consultar cuentas", description = "ROLE_ADMIN_STYLERADAR. Filtro opcional ACTIVA, INACTIVA, BLOQUEADA. "
            + "ACTIVA incluye NULL histórico efectivo, sin migrarlo. page=0, size=20, máximo 100; orden id ASC; paginación en BD.")
    ResponseEntity<PageResponseDTO<CuentaAdministrativaResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute CuentasConsultaRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor);

    @GetMapping("/{id}")
    @Operation(summary = "Consultar cuenta y última decisión", description = "ROLE_ADMIN_STYLERADAR. Estado efectivo y última modificación, "
            + "sin credenciales ni datos privados. NULL histórico se muestra ACTIVA; metadatos nulos no representan una decisión.")
    ResponseEntity<CuentaAdministrativaResponseDTO> consultarUno(@PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor);

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar, inactivar o bloquear una cuenta", description = "ROLE_ADMIN_STYLERADAR. "
            + "ACTIVA, INACTIVA, BLOQUEADA; todas las transiciones entre estados distintos. BLOQUEADA exige motivo no vacío; "
            + "máximo 1000 caracteres. Actor y fecha del servidor. No permite autocambio, ni siquiera idempotente. "
            + "Conserva otro administrador operativo. Estado repetido conserva los metadatos; NULL->ACTIVA efectivo no hace backfill. "
            + "JWT previo recibe 401 mientras no esté ACTIVA; podría funcionar otra vez al reactivar si no expiró.")
    ResponseEntity<CuentaAdministrativaResponseDTO> cambiar(@PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "BLOQUEADA", value = "{\"estado\":\"BLOQUEADA\",\"motivo\":\"Incumplimiento de las condiciones\"}"),
                    @ExampleObject(name = "INACTIVA", value = "{\"estado\":\"INACTIVA\"}"),
                    @ExampleObject(name = "ACTIVA", value = "{\"estado\":\"ACTIVA\",\"motivo\":\"Revisión concluida\"}")}))
            @Valid @RequestBody CambioEstadoCuentaRequestDTO request);
}
