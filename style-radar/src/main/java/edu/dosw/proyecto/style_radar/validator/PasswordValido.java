package edu.dosw.proyecto.style_radar.validator;

import java.lang.annotation.*;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordRequestValidator.class)
public @interface PasswordValido {
    String message() default "La contraseña no cumple los límites admitidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
