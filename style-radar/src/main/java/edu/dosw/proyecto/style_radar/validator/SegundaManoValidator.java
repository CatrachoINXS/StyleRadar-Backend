package edu.dosw.proyecto.style_radar.validator;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.repository.PublicacionSegundaManoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SegundaManoValidator {

    private final PublicacionSegundaManoRepository publicacionRepository;

    public void validarPrecio(Double precio) {
        if (precio == null || precio < 0) {
            throw new ReglaDeNegocioException("El precio de la prenda usada debe ser mayor o igual a 0");
        }
    }

    public PublicacionSegundaManoEntity validarYObtenerDeUsuario(Long publicacionId, Long usuarioId) {
        PublicacionSegundaManoEntity publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una publicación con ID: " + publicacionId));

        if (!publicacion.getUsuario().getId().equals(usuarioId)) {
            throw new ReglaDeNegocioException("La publicación no pertenece al usuario especificado");
        }

        return publicacion;
    }

    public void validarEditable(PublicacionSegundaManoEntity publicacion) {
        if (publicacion.getEstado() == EstadoPublicacion.VENDIDA) {
            throw new ReglaDeNegocioException("No se puede editar una publicación que ya ha sido vendida");
        }
        if (publicacion.getEstado() == EstadoPublicacion.RETIRADA) {
            throw new ReglaDeNegocioException("No se puede editar una publicación que ha sido retirada");
        }
    }

    public void validarTransicionAVendida(PublicacionSegundaManoEntity publicacion) {
        if (publicacion.getEstado() == EstadoPublicacion.VENDIDA) {
            throw new ReglaDeNegocioException("La publicación ya se encuentra marcada como vendida");
        }
        if (publicacion.getEstado() == EstadoPublicacion.RETIRADA) {
            throw new ReglaDeNegocioException("No se puede marcar como vendida una publicación retirada");
        }
    }

    public void validarTransicionARetirada(PublicacionSegundaManoEntity publicacion) {
        if (publicacion.getEstado() == EstadoPublicacion.RETIRADA) {
            throw new ReglaDeNegocioException("La publicación ya se encuentra retirada");
        }
    }
}
