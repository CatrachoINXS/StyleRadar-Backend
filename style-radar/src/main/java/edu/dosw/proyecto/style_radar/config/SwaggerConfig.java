package edu.dosw.proyecto.style_radar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI styleRadarOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StyleRadar API")
                        .description("API REST para la gestión de StyleRadar.")
                        .version("v1"));
    }
}
