package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import java.util.Set;
import edu.dosw.proyecto.style_radar.model.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;

public record VerificacionAlmacenResponseDTO(String nit, String nombreComercial, String descripcion,
        String telefono, String correoContacto, Double latitud, Double longitud, Double reputacion,
        Set<CategoriaAlmacen> categorias,
        @Schema(description = "NULL indica legado sin aprobación administrativa; no se convierte a APROBADO")
        EstadoVerificacionAlmacen estadoVerificacion,
        boolean legadoSinVerificacion, Instant fechaDecisionVerificacion, Long administradorDecisionId,
        String motivoVerificacion) { }
