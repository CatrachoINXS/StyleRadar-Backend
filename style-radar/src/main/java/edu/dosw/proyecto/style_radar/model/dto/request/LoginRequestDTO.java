package edu.dosw.proyecto.style_radar.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import edu.dosw.proyecto.style_radar.validator.PasswordValido;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
    @NotBlank
    @Email
    private String email;
    @PasswordValido
    private String password;
}
