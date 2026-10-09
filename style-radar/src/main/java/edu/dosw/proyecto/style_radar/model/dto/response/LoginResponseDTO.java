package edu.dosw.proyecto.style_radar.model.dto.response;

import java.util.Set;
import edu.dosw.proyecto.style_radar.model.domain.Rol;

public record LoginResponseDTO(String accessToken, String tokenType, long expiresIn, Long usuarioId, Set<Rol> roles) {}
