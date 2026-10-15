package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;
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

@Tag(name = "Alertas", description = "Alertas y notificaciones persistentes propias")
@SecurityRequirement(name = "bearerAuth")
public interface AlertaApi {
    @Operation(summary = "Crear alerta de disponibilidad", description = "Exactamente un ID: busquedaGuardadaId propia "
            + "con criterios efectivos o itemCatalogoId existente sin stock. Usuario, actividad y fechas los determina el servidor. "
            + "No genera notificaciones inmediatas. No permite duplicados activos.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Alerta creada"),
            @ApiResponse(responseCode = "400", description = "Objetivo ausente, doble o request invalido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido, expirado o cuenta bloqueada"),
            @ApiResponse(responseCode = "404", description = "Objetivo inexistente o no accesible"),
            @ApiResponse(responseCode = "409", description = "Alerta activa duplicada o conflicto concurrente"),
            @ApiResponse(responseCode = "422", description = "Busqueda vacia o item disponible"),
            @ApiResponse(responseCode = "500", description = "Error tecnico")})
    @PostMapping("/alertas")
    ResponseEntity<AlertaResponseDTO> crear(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearAlertaRequestDTO request);

    @Operation(summary = "Consultar mis alertas", description = "Solo propias; fechaCreacion DESC, id DESC. Incluye historial desactivado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Alertas propias"),
            @ApiResponse(responseCode = "401", description = "Requiere JWT valido y cuenta activa"),
            @ApiResponse(responseCode = "500", description = "Error tecnico")})
    @GetMapping("/alertas")
    ResponseEntity<List<AlertaResponseDTO>> consultar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Desactivar mi alerta", description = "Solo activa=false. Idempotente: conserva fecha previa, objetivo e historial. No admite reactivacion.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Alerta desactivada"),
            @ApiResponse(responseCode = "400", description = "Request invalido o intenta activar"),
            @ApiResponse(responseCode = "401", description = "Requiere JWT valido y cuenta activa"),
            @ApiResponse(responseCode = "404", description = "Alerta inexistente o no accesible"),
            @ApiResponse(responseCode = "409", description = "Conflicto concurrente"),
            @ApiResponse(responseCode = "500", description = "Error tecnico")})
    @PatchMapping("/alertas/{id}")
    ResponseEntity<AlertaResponseDTO> desactivar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody DesactivarAlertaRequestDTO request);

    @Operation(summary = "Consultar mis notificaciones", description = "Solo destinatario autenticado; page=0, size=20. "
            + "page>=0; size 1 a 100. Orden fechaCreacion DESC, id DESC. itemCatalogoId conserva referencia historica incluso tras retiro.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Pagina de notificaciones propias"),
            @ApiResponse(responseCode = "400", description = "Paginacion invalida"),
            @ApiResponse(responseCode = "401", description = "Requiere JWT valido y cuenta activa"),
            @ApiResponse(responseCode = "500", description = "Error tecnico")})
    @GetMapping("/notificaciones")
    ResponseEntity<PageResponseDTO<NotificacionResponseDTO>> notificaciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @ParameterObject @ModelAttribute NotificacionesRequestDTO request);
}
