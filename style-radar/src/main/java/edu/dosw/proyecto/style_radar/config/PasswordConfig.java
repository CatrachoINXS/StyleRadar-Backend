package edu.dosw.proyecto.style_radar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder(@Value("${styleradar.security.bcrypt-strength:10}") int strength) {
        return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2B, strength);
    }
}
