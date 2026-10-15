package edu.dosw.proyecto.style_radar.validator;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.ConflictoException;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.FundacionRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FundacionValidator {

    private final FundacionRepository fundacionRepository;
    private final UsuarioRepository usuarioRepository;

    public FundacionEntity validarYObtener(Long fundacionId) {
        return fundacionRepository.findById(fundacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una fundación con ID: " + fundacionId));
    }

    /** Fundación administrada por el representante autenticado. */
    public FundacionEntity validarYObtenerDeRepresentante(Long representanteId) {
        return fundacionRepository.findByRepresentante_Id(representanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no representa ninguna fundación"));
    }

    public void validarNombreUnico(String nombre, Long fundacionId) {
        boolean duplicado = fundacionId == null
                ? fundacionRepository.existsByNombreIgnoreCase(nombre)
                : fundacionRepository.existsByNombreIgnoreCaseAndIdNot(nombre, fundacionId);
        if (duplicado) {
            throw new ConflictoException("Ya existe una fundación con el nombre: " + nombre);
        }
    }

    public UsuarioEntity validarRepresentante(Long representanteId, Long fundacionId) {
        UsuarioEntity representante = usuarioRepository.findById(representanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con ID: " + representanteId));
        if (representante.getEstadoCuenta() != null && representante.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            throw new ReglaDeNegocioException("El representante debe tener una cuenta activa");
        }
        boolean ocupado = fundacionId == null
                ? fundacionRepository.existsByRepresentante_Id(representanteId)
                : fundacionRepository.existsByRepresentante_IdAndIdNot(representanteId, fundacionId);
        if (ocupado) {
            throw new ConflictoException("El usuario ya representa otra fundación");
        }
        return representante;
    }

    public void validarActiva(FundacionEntity fundacion) {
        if (!fundacion.isActiva()) {
            throw new ReglaDeNegocioException("La fundación está desactivada y no recibe nuevas donaciones");
        }
    }
}
