package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion;

/** Proyección administrativa sin entidad de usuario, credenciales ni datos de contacto. */
public record ModeracionItemResponseDTO(Long itemId, String almacenNit, PrendaResponseDTO prenda,
        Double precio, EstadoModeracion estadoModeracion, Instant fechaPublicacion,
        Instant fechaDecisionModeracion, Long administradorDecisionId, String motivoModeracion) { }
