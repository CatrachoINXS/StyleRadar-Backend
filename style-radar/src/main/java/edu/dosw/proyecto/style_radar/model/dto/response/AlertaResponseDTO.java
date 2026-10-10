package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import edu.dosw.proyecto.style_radar.model.domain.TipoObjetivoAlerta;

public record AlertaResponseDTO(Long id, TipoObjetivoAlerta tipoObjetivo, Long identificadorObjetivo,
        boolean activa, Instant fechaCreacion, Instant fechaDesactivacion) { }
