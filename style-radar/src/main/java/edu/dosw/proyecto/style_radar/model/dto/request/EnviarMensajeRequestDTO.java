package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnviarMensajeRequestDTO {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 1000, message = "El mensaje no puede superar 1000 caracteres")
    private String texto;
}
