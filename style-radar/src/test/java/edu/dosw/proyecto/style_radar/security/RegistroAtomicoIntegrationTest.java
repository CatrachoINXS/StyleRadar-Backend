package edu.dosw.proyecto.style_radar.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.entity.CredencialEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.IAuthService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:auth-rollback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class RegistroAtomicoIntegrationTest {
    @Autowired IAuthService auth;
    @MockitoSpyBean UsuarioRepository usuarios;
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @MockitoSpyBean CredencialRepository credenciales;
    @MockitoSpyBean PasswordEncoder encoder;

    @Test
    void falloAlGuardarCredencialRevierteUsuarioEnTransaccionReal() {
        // Arrange
        long usersBefore = usuarios.count();
        long credentialsBefore = credenciales.count();
        doThrow(new org.springframework.dao.DataIntegrityViolationException("fixture failure"))
                .when(credenciales).saveAndFlush(any(CredencialEntity.class));
        // Act & Assert: sin transacción de test; verifica tras rollback del servicio.
        assertThatThrownBy(() -> auth.registrarComprador(nuevo("save-failure@ejemplo.co"), "test password"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(usuarios.existsByEmail("save-failure@ejemplo.co")).isFalse();
        assertThat(usuarios.count()).isEqualTo(usersBefore);
        assertThat(credenciales.count()).isEqualTo(credentialsBefore);
    }

    @Test
    void falloDelHashRevierteUsuarioEnTransaccionReal() {
        // Arrange
        long before = usuarios.count();
        doThrow(new IllegalStateException("fixture failure")).when(encoder).encode("test password");
        // Act & Assert
        assertThatThrownBy(() -> auth.registrarComprador(nuevo("hash-failure@ejemplo.co"), "test password"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(usuarios.existsByEmail("hash-failure@ejemplo.co")).isFalse();
        assertThat(usuarios.count()).isEqualTo(before);
    }

    @Test
    void registroExitosoConfirmaAmbasFilasYHashReal() {
        // Act
        var result = auth.registrarComprador(nuevo("committed@ejemplo.co"), "test password");
        // Assert
        assertThat(usuarios.existsById(result.getId())).isTrue();
        var hash = credenciales.findById(result.getId()).orElseThrow().getPasswordHash();
        assertThat(encoder.matches("test password", hash)).isTrue();
        assertThat(hash).isNotEqualTo("test password");
    }

    private Usuario nuevo(String email) { return Usuario.builder().nombre("Comprador").email(email).build(); }

    @Test
    void falloPersistenciaEnFiltroResponde500YNo401() throws Exception {
        // Arrange
        var result = auth.registrarComprador(nuevo("filter-failure@ejemplo.co"), "test password");
        String token = jwt.generate(result.getId());
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture unavailable"))
                .when(usuarios).findById(result.getId());
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/{id}", result.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Intente nuevamente más tarde."));
        mvc.perform(get("/api/v1/usuarios/{id}", result.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void falloPersistenciaEnLoginResponde500YNo401() throws Exception {
        // Arrange
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture unavailable"))
                .when(usuarios).findAllByEmailNormalizado("unavailable@ejemplo.co");
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Act & Assert
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unavailable@ejemplo.co\",\"password\":\"test password\"}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Intente nuevamente más tarde."));
    }
}
