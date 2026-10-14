package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;

public record AdministradorAlmacenResponseDTO(Long id, Long usuarioId, String almacenNit,
        Long asignadoPorId, Instant fechaAsignacion) { }
