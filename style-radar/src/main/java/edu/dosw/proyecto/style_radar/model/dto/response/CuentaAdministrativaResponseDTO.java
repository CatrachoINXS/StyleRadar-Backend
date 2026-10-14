package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import java.util.Set;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.domain.Rol;

public record CuentaAdministrativaResponseDTO(Long id, String nombre, String email, Set<Rol> roles,
        EstadoCuenta estadoCuenta, Instant fechaRegistro, Instant fechaCambioEstado,
        Long administradorCambioEstadoId, String motivoCambioEstado) {}
