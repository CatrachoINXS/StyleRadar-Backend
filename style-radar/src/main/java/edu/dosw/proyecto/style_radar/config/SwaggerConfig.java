package edu.dosw.proyecto.style_radar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import java.util.List;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI styleRadarOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .info(new Info()
                        .title("StyleRadar API")
                        .description("API REST para la gestión de StyleRadar.")
                        .version("v1"));
    }

    @Bean
    OpenApiCustomizer authenticationDocumentation() {
        Set<String> publicPosts = Set.of("/api/v1/auth/login", "/api/v1/auth/registro", "/api/v1/usuarios");
        Set<String> publicGets = Set.of("/api/v1/prendas", "/api/v1/catalogo/buscar",
                "/api/v1/almacenes/{nit}/catalogo");
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean publicRoute = method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST && publicPosts.contains(path)
                    || method == io.swagger.v3.oas.models.PathItem.HttpMethod.GET && publicGets.contains(path);
            operation.setSecurity(publicRoute ? List.of()
                    : List.of(new SecurityRequirement().addList("bearerAuth")));
        }));
    }
}
