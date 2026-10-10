package edu.dosw.proyecto.style_radar.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import edu.dosw.proyecto.style_radar.security.JwtAuthFilter;
import edu.dosw.proyecto.style_radar.security.SecurityErrorHandler;
import edu.dosw.proyecto.style_radar.security.UsuarioUserDetailsService;

@Configuration
public class SecurityConfig {
    @Bean
    AuthenticationManager authenticationManager(UsuarioUserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwt,
                                           SecurityErrorHandler errors) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // API REST con Bearer, sin cookies de sesión.
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(handler -> handler.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/catalogo/moderacion")
                            .hasRole("ADMIN_STYLERADAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/catalogo/{itemId}/moderacion")
                            .hasRole("ADMIN_STYLERADAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/catalogo/{itemId}/moderacion/solicitar-revision")
                            .hasRole("ADMIN_STYLERADAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro",
                                "/api/v1/usuarios").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**",
                                "/api/v1/prendas", "/api/v1/catalogo/buscar",
                                "/api/v1/almacenes/{nit}/catalogo",
                                "/api/v1/almacenes",
                                "/api/v1/almacenes/{nit}/catalogo/resumen",
                                "/api/v1/almacenes/{nit}/catalogo/novedades",
                                "/api/v1/almacenes/{nit}/distancia").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Ejecutarlo solamente dentro de Spring Security.
        return registration;
    }
}
