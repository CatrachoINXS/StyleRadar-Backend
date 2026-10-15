package edu.dosw.proyecto.style_radar.service;

import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.Conversacion;
import edu.dosw.proyecto.style_radar.model.domain.Mensaje;

public interface IChatService {

    /** Idempotente: si ya existe la conversación del comprador sobre esa publicación, la devuelve. */
    Conversacion iniciar(Long compradorId, Long publicacionId);

    List<Conversacion> consultarMisConversaciones(Long usuarioId);

    List<Mensaje> consultarMensajes(Long usuarioId, Long conversacionId);

    Mensaje enviarMensaje(Long usuarioId, Long conversacionId, String texto);
}
