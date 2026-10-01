package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractuarPlaylistRequestDTO {

    @NotNull(message = "El ID del usuario que interactúa es obligatorio")
    private Long usuarioId;
}
