package edu.dosw.proyecto.style_radar.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordConfigTest {

    @Test
    void debeUsarElCosteConfiguradoYVerificarConElSaltDelHash() {
        // Arrange
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder(4);
        String password = "clave de prueba";

        // Act
        String hash = encoder.encode(password);

        // Assert
        assertThat(hash).startsWith("$2b$04$");
        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(encoder.matches("otra clave", hash)).isFalse();
    }
}
