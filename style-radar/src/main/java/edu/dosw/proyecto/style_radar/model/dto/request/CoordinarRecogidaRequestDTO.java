package edu.dosw.proyecto.style_radar.model.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
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
public class CoordinarRecogidaRequestDTO {

    @NotBlank(message = "El punto de entrega o recogida es obligatorio")
    @Size(max = 200, message = "El punto de entrega no puede superar 200 caracteres")
    private String puntoEntrega;

    @NotNull(message = "El inicio de la franja es obligatorio")
    @Future(message = "La franja debe estar en el futuro")
    private Instant franjaInicio;

    @NotNull(message = "El fin de la franja es obligatorio")
    @Future(message = "La franja debe estar en el futuro")
    private Instant franjaFin;
}
