package edu.dosw.proyecto.style_radar.model.domain;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDonacion {
    private Long id;
    private Long publicacionId;
    private Prenda prenda;
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
