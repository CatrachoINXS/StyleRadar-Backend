package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.validator.PasswordValido;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroAuthRequestDTO extends RegistrarUsuarioRequestDTO {
    @PasswordValido
    private String password;
}
