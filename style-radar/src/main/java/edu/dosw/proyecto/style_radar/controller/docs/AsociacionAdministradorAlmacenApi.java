package edu.dosw.proyecto.style_radar.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.model.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autorización de suscripciones", description = "Aprovisionamiento mínimo habilitador de RF67/RF68")
@SecurityRequirement(name = "bearerAuth")
public interface AsociacionAdministradorAlmacenApi {
    @Operation(summary = "Asociar administrador autorizado a un NIT existente", description = "Solo ROLE_ADMIN_STYLERADAR "
            + "con cuenta activa. Usuario objetivo existente con ADMIN_ALMACEN y almacén existente. "
            + "Sin body. Idempotente; conserva primera asignación y registra actor/Clock UTC. "
            + "No modifica roles, correo, verificación ni suscripciones. No es registro público de almacenes.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Asociación persistida o ya existente"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido"),
            @ApiResponse(responseCode = "401", description = "JWT inválido o cuenta suspendida"),
            @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR"),
            @ApiResponse(responseCode = "404", description = "Usuario o almacén inexistente"),
            @ApiResponse(responseCode = "409", description = "Conflicto concurrente o de integridad"),
            @ApiResponse(responseCode = "422", description = "Usuario objetivo sin ADMIN_ALMACEN"),
            @ApiResponse(responseCode = "500", description = "Fallo técnico")})
    @PutMapping("/{nit}/administradores/{usuarioId}")
    ResponseEntity<AdministradorAlmacenResponseDTO> asociar(@PathVariable String nit, @PathVariable Long usuarioId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor);
}
