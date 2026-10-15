package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;

import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDonacionResponseDTO {
    private Long id;
    private Long publicacionId;
    private PrendaResponseDTO prenda;
    private Long donanteId;
    private Long fundacionId;
    private String fundacionNombre;
    private String mensaje;
    private EstadoSolicitudDonacion estado;
    private String motivoRechazo;
    private String puntoEntrega;
    private Instant franjaInicio;
    private Instant franjaFin;
    private Instant fechaSolicitud;
    private Instant fechaDecision;
    private Instant fechaRecepcion;
}
