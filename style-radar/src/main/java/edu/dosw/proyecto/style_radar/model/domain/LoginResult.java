package edu.dosw.proyecto.style_radar.model.domain;

import java.util.Set;

public record LoginResult(String accessToken, long expiresIn, Long usuarioId, Set<Rol> roles) {}
