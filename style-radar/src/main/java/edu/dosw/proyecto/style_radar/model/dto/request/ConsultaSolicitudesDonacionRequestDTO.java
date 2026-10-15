package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaSolicitudesDonacionRequestDTO {

    /** Si se envía, filtra por estado; si no, devuelve todas. */
    private EstadoSolicitudDonacion estado;
}
