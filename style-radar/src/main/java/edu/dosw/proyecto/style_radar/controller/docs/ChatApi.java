package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.dto.request.EnviarMensajeRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.ConversacionResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.MensajeResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Chat segunda mano", description = "Mensajes privados comprador-vendedor (sin tiempo real en el Sprint 3)")
@SecurityRequirement(name = "bearerAuth")
public interface ChatApi {

    @Operation(summary = "Iniciar chat con el vendedor (RF-40)",
            description = "ROLE_COMPRADOR. Publicación DISPONIBLE y ajena. Idempotente: devuelve la conversación existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversación iniciada o existente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Publicación inexistente"),
            @ApiResponse(responseCode = "422", description = "Publicación no disponible o propia"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/segunda-mano/{publicacionId}/conversaciones")
    ResponseEntity<ConversacionResponseDTO> iniciar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long publicacionId);

    @Operation(summary = "Consultar mis conversaciones", description = "ROLE_COMPRADOR. Como comprador o como vendedor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversaciones propias"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/usuarios/me/conversaciones")
    ResponseEntity<List<ConversacionResponseDTO>> consultarMisConversaciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal);

    @Operation(summary = "Consultar mensajes (RF-41)", description = "ROLE_COMPRADOR. Solo participantes; orden cronológico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensajes de la conversación"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Conversación inexistente o ajena"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/conversaciones/{id}/mensajes")
    ResponseEntity<List<MensajeResponseDTO>> consultarMensajes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id);

    @Operation(summary = "Enviar mensaje para coordinar la entrega (RF-41)",
            description = "ROLE_COMPRADOR. Solo participantes; los datos de entrega quedan en el chat privado, no en el anuncio.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensaje enviado"),
            @ApiResponse(responseCode = "400", description = "Mensaje vacío o demasiado largo"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Conversación inexistente o ajena"),
            @ApiResponse(responseCode = "422", description = "La publicación fue retirada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/api/v1/conversaciones/{id}/mensajes")
    ResponseEntity<MensajeResponseDTO> enviarMensaje(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EnviarMensajeRequestDTO request);
}
