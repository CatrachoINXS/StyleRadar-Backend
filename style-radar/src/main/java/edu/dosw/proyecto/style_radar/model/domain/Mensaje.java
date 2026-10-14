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
public class Mensaje {
    private Long id;
    private Long conversacionId;
    private Long autorId;
    private String texto;
    private Instant fechaEnvio;
}
