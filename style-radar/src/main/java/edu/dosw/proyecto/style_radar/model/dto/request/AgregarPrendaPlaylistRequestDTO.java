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
public class AgregarPrendaPlaylistRequestDTO {

    @NotNull(message = "El ID de la prenda es obligatorio")
    private Long prendaId;
}
