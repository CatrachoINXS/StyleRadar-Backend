package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.Instant;
import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundacionResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String direccion;
    private String telefono;
    private String email;
    private Double latitud;
    private Double longitud;
    private Set<TipoPrenda> categoriasAceptadas;
    private Long representanteId;
    private boolean activa;
    private Instant fechaRegistro;
    private Double distanciaKm;
}
