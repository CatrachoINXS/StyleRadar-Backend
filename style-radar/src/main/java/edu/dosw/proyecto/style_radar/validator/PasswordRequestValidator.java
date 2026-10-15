package edu.dosw.proyecto.style_radar.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordRequestValidator implements ConstraintValidator<PasswordValido, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return CredencialValidator.passwordAdmitido(value);
    }
}
