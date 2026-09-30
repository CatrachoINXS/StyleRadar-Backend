package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private Instant fechaRegistro;
    private Set<Estilo> preferenciasEstilo;
    private Set<Talla> tallasHabituales;
}
