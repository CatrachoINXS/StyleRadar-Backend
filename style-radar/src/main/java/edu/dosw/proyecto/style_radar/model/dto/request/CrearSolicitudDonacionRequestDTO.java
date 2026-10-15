package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudDonacionRequestDTO {

    @NotNull(message = "El ID de la publicación marcada para donación es obligatorio")
    private Long publicacionId;

    @NotNull(message = "El ID de la fundación seleccionada es obligatorio")
    private Long fundacionId;

    @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
    private String mensaje;
}
