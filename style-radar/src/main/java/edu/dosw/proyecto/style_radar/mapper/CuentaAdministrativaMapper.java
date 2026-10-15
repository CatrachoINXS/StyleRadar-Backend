package edu.dosw.proyecto.style_radar.mapper;

import java.util.Set;
import org.springframework.stereotype.Component;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.dto.response.CuentaAdministrativaResponseDTO;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;

@Component
public class CuentaAdministrativaMapper {
    public CuentaAdministrativaResponseDTO toResponse(UsuarioEntity u) {
        return new CuentaAdministrativaResponseDTO(u.getId(), u.getNombre(), u.getEmail(),
                u.getRoles() == null || u.getRoles().isEmpty() ? Set.of(Rol.COMPRADOR) : Set.copyOf(u.getRoles()),
                u.getEstadoCuenta() == null ? EstadoCuenta.ACTIVA : u.getEstadoCuenta(),
                u.getFechaRegistro(), u.getFechaCambioEstado(), u.getAdministradorCambioEstadoId(), u.getMotivoCambioEstado());
    }
}
