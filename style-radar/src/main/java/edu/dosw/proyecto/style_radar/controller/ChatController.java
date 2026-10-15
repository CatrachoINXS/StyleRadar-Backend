package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.ChatApi;
import edu.dosw.proyecto.style_radar.mapper.ChatMapper;
import edu.dosw.proyecto.style_radar.model.dto.request.EnviarMensajeRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.ConversacionResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.MensajeResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final IChatService chatService;
    private final ChatMapper chatMapper;

    @Override
    public ResponseEntity<ConversacionResponseDTO> iniciar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long publicacionId) {
        return ResponseEntity.ok(chatMapper.toResponse(chatService.iniciar(principal.getUsuarioId(), publicacionId)));
    }

    @Override
    public ResponseEntity<List<ConversacionResponseDTO>> consultarMisConversaciones(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(chatService.consultarMisConversaciones(principal.getUsuarioId()).stream()
                .map(chatMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<List<MensajeResponseDTO>> consultarMensajes(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.consultarMensajes(principal.getUsuarioId(), id).stream()
                .map(chatMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<MensajeResponseDTO> enviarMensaje(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EnviarMensajeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatMapper.toResponse(chatService.enviarMensaje(principal.getUsuarioId(), id, request.getTexto())));
    }
}
