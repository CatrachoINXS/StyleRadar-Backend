package edu.dosw.proyecto.style_radar.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class PrendaResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private String marca;
    private String color;
}