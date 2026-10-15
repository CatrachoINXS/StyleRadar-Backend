package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.dto.request.ConsultaSolicitudesDonacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CoordinarRecogidaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearSolicitudDonacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionDonacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.SolicitudDonacionResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Donaciones", description = "Solicitudes de donación entre donantes y fundaciones aliadas")
@SecurityRequirement(name = "bearerAuth")
public interface DonacionApi {

    @Operation(summary = "Seleccionar fundación y enviar solicitud de donación (RF-44, RF-45)",
            description = "ROLE_COMPRADOR. La prenda debe ser propia y estar marcada para donación (RF-42); "
                    + "la fundación debe estar activa y aceptar ese tipo de prenda.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud enviada en estado PENDIENTE"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Publicación o fundación inexistente"),
            @ApiResponse(responseCode = "409", description = "La prenda ya tiene una solicitud en curso"),
            @ApiResponse(responseCode = "422", description = "Prenda ajena o no marcada para donación, fundación inactiva o que no acepta el tipo"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/usuarios/me/donaciones")
    ResponseEntity<SolicitudDonacionResponseDTO> enviarSolicitud(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearSolicitudDonacionRequestDTO request);

    @Operation(summary = "Consultar mis solicitudes de donación", description = "ROLE_COMPRADOR. Más recientes primero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitudes propias"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/usuarios/me/donaciones")
    ResponseEntity<List<SolicitudDonacionResponseDTO>> consultarMisDonaciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Consultar solicitudes de mi fundación (RF-46)",
            description = "ROLE_REPRESENTANTE_FUNDACION. Solo las asignadas a la fundación que representa; filtro opcional por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitudes de la fundación"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "El usuario no representa ninguna fundación"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('REPRESENTANTE_FUNDACION')")
    @GetMapping("/api/v1/fundaciones/mi-fundacion/solicitudes")
    ResponseEntity<List<SolicitudDonacionResponseDTO>> consultarSolicitudesDeFundacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @ParameterObject @ModelAttribute ConsultaSolicitudesDonacionRequestDTO request);

    @Operation(summary = "Aceptar o rechazar solicitud (RF-47)",
            description = "ROLE_REPRESENTANTE_FUNDACION. Solo solicitudes PENDIENTES de su fundación; el rechazo exige motivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud ACEPTADA o RECHAZADA"),
            @ApiResponse(responseCode = "400", description = "Falta la decisión o el motivo de rechazo"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Solicitud inexistente o de otra fundación"),
            @ApiResponse(responseCode = "422", description = "La solicitud no está PENDIENTE"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('REPRESENTANTE_FUNDACION')")
    @PatchMapping("/api/v1/fundaciones/mi-fundacion/solicitudes/{id}/decision")
    ResponseEntity<SolicitudDonacionResponseDTO> decidir(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DecisionDonacionRequestDTO request);

    @Operation(summary = "Coordinar recogida de la donación (RF-48)",
            description = "ROLE_COMPRADOR (donante) o ROLE_REPRESENTANTE_FUNDACION. Solo participantes; "
                    + "solicitud ACEPTADA o COORDINADA (permite reprogramar). Franja futura y fin posterior al inicio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recogida COORDINADA"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Solicitud inexistente o ajena"),
            @ApiResponse(responseCode = "422", description = "Estado no coordinable o franja inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasAnyRole('COMPRADOR', 'REPRESENTANTE_FUNDACION')")
    @PatchMapping("/api/v1/donaciones/{id}/recogida")
    ResponseEntity<SolicitudDonacionResponseDTO> coordinarRecogida(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CoordinarRecogidaRequestDTO request);

    @Operation(summary = "Confirmar recepción de la donación (RF-49)",
            description = "ROLE_REPRESENTANTE_FUNDACION. Solo solicitudes COORDINADAS de su fundación; la prenda queda DONADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donación RECIBIDA"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Solicitud inexistente o de otra fundación"),
            @ApiResponse(responseCode = "422", description = "La solicitud no está COORDINADA"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('REPRESENTANTE_FUNDACION')")
    @PatchMapping("/api/v1/fundaciones/mi-fundacion/solicitudes/{id}/recepcion")
    ResponseEntity<SolicitudDonacionResponseDTO> confirmarRecepcion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);
}
