package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class PrendaRequestDTO {

    @NotBlank(message = "El nombre de la prenda es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String descripcion;

    @NotBlank(message = "La marca de la prenda es obligatoria")
    @Size(max = 30, message = "Máximo 30 caracteres")
    private String marca;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String categoria;

    @NotBlank(message = "El color es obligatorio")
    @Size(max = 30, message = "Máximo 30 caracteres")
    private String color;
    
}
