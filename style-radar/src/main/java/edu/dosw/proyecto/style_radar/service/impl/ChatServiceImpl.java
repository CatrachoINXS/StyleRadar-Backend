package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.mapper.ChatEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.Conversacion;
import edu.dosw.proyecto.style_radar.model.domain.Mensaje;
import edu.dosw.proyecto.style_radar.model.entity.ConversacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.MensajeEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.repository.ConversacionRepository;
import edu.dosw.proyecto.style_radar.repository.MensajeRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.IChatService;
import edu.dosw.proyecto.style_radar.validator.ChatValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Chat sin tiempo real (Sprint 3): el cliente consulta los mensajes; WebSockets llegan en el Corte 3. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements IChatService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChatEntityMapper chatEntityMapper;
    private final ChatValidator chatValidator;
    private final Clock clock;

    @Override
    @Transactional
    public Conversacion iniciar(Long compradorId, Long publicacionId) {
        log.info("Usuario ID: {} inicia chat sobre la publicación ID: {}", compradorId, publicacionId);
        PublicacionSegundaManoEntity publicacion = chatValidator.validarPublicacionParaChat(publicacionId, compradorId);
        ConversacionEntity conversacion = conversacionRepository
                .findByPublicacion_IdAndComprador_Id(publicacionId, compradorId)
                .orElseGet(() -> {
                    ConversacionEntity nueva = conversacionRepository.save(ConversacionEntity.builder()
                            .publicacion(publicacion)
                            .comprador(usuarioRepository.getReferenceById(compradorId))
                            .fechaCreacion(clock.instant())
                            .build());
                    log.info("Conversación ID: {} creada", nueva.getId());
                    return nueva;
                });
        return chatEntityMapper.toDomain(conversacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversacion> consultarMisConversaciones(Long usuarioId) {
        log.info("Consultando conversaciones del usuario ID: {}", usuarioId);
        List<Conversacion> conversaciones = conversacionRepository.findDeParticipante(usuarioId).stream()
                .map(chatEntityMapper::toDomain)
                .toList();
        log.info("Conversaciones del usuario ID {}: {}", usuarioId, conversaciones.size());
        return conversaciones;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mensaje> consultarMensajes(Long usuarioId, Long conversacionId) {
        log.info("Usuario ID: {} consulta mensajes de la conversación ID: {}", usuarioId, conversacionId);
        chatValidator.validarYObtenerDeParticipante(conversacionId, usuarioId);
        return mensajeRepository.findByConversacion_IdOrderByFechaEnvioAscIdAsc(conversacionId).stream()
                .map(chatEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Mensaje enviarMensaje(Long usuarioId, Long conversacionId, String texto) {
        log.info("Usuario ID: {} envía mensaje en la conversación ID: {}", usuarioId, conversacionId);
        ConversacionEntity conversacion = chatValidator.validarYObtenerDeParticipante(conversacionId, usuarioId);
        chatValidator.validarAbierta(conversacion);
        MensajeEntity mensaje = mensajeRepository.save(MensajeEntity.builder()
                .conversacion(conversacion)
                .autor(usuarioRepository.getReferenceById(usuarioId))
                .texto(texto.trim())
                .fechaEnvio(clock.instant())
                .build());
        log.info("Mensaje ID: {} enviado en la conversación ID: {}", mensaje.getId(), conversacionId);
        return chatEntityMapper.toDomain(mensaje);
    }
}
