package edu.dosw.proyecto.style_radar.validator;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.entity.ConversacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.repository.ConversacionRepository;
import edu.dosw.proyecto.style_radar.repository.PublicacionSegundaManoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatValidator {

    private final PublicacionSegundaManoRepository publicacionRepository;
    private final ConversacionRepository conversacionRepository;

    /** RF-40: solo sobre publicaciones activas (DISPONIBLE) y nunca con uno mismo. */
    public PublicacionSegundaManoEntity validarPublicacionParaChat(Long publicacionId, Long compradorId) {
        PublicacionSegundaManoEntity publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una publicación con ID: " + publicacionId));
        if (publicacion.getEstado() != EstadoPublicacion.DISPONIBLE) {
            throw new ReglaDeNegocioException("Solo se puede iniciar un chat sobre una publicación disponible");
        }
        if (publicacion.getUsuario().getId().equals(compradorId)) {
            throw new ReglaDeNegocioException("No puede iniciar un chat sobre su propia publicación");
        }
        return publicacion;
    }

    /** Solo comprador y vendedor; para cualquier otro la conversación "no existe". */
    public ConversacionEntity validarYObtenerDeParticipante(Long conversacionId, Long usuarioId) {
        ConversacionEntity conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la conversación " + conversacionId));
        boolean participa = conversacion.getComprador().getId().equals(usuarioId)
                || conversacion.getPublicacion().getUsuario().getId().equals(usuarioId);
        if (!participa) {
            throw new RecursoNoEncontradoException("No existe la conversación " + conversacionId);
        }
        return conversacion;
    }

    /** RF-41: tras la venta se sigue coordinando la entrega; una publicación retirada cierra el chat. */
    public void validarAbierta(ConversacionEntity conversacion) {
        if (conversacion.getPublicacion().getEstado() == EstadoPublicacion.RETIRADA) {
            throw new ReglaDeNegocioException("La publicación fue retirada; la conversación está cerrada");
        }
    }
}
