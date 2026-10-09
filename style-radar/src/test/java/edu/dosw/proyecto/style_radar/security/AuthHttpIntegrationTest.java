package edu.dosw.proyecto.style_radar.security;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.*;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Import({AuthHttpIntegrationTest.TestSecurity.class, AuthHttpIntegrationTest.TestEndpoints.class})
@Transactional
class AuthHttpIntegrationTest {
    private static final String PASSWORD = "HTTP fixture password";
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired CredencialRepository credenciales;
    @Autowired ICredencialService credentials;
    @Autowired IAuthService auth;
    @Autowired EntityManager em;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper json;
    @Value("${styleradar.security.jwt.secret-base64}") String secret;
    private MockMvc mvc;

    @BeforeEach
    void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    private UsuarioEntity fixture(String email, Rol rol, EstadoCuenta state, boolean password) {
        var user = usuarios.saveAndFlush(UsuarioEntity.builder().nombre("HTTP fixture").email(email)
                .fechaRegistro(Instant.now()).roles(new HashSet<>(Set.of(rol))).estadoCuenta(state).build());
        if (password) credentials.crearCredencial(user.getId(), PASSWORD);
        em.flush();
        em.clear();
        return user;
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String token(UsuarioEntity user) throws Exception {
        return json.readTree(login(user.getEmail(), PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void loginSinAutenticacionPreviaFuncionaParaLosCuatroActores(Rol rol) throws Exception {
        // Arrange
        var user = fixture("actor@ejemplo.co", rol, EstadoCuenta.ACTIVA, true);
        // Act
        var response = login("ACTOR@ejemplo.co", PASSWORD)
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuarioId").value(user.getId()))
                .andExpect(jsonPath("$.roles[0]").value(rol.name()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse();
        // Assert
        assertThat(response.getContentAsString()).doesNotContain(PASSWORD, "passwordHash", "$2");
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    @Test
    void passwordIncorrectoYCorreoInexistenteTienenElMismoErrorGenerico() throws Exception {
        fixture("bad@ejemplo.co", Rol.COMPRADOR, EstadoCuenta.ACTIVA, true);
        login("bad@ejemplo.co", "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.UNAUTHORIZED_MESSAGE))
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
        login("missing@ejemplo.co", "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void cuentaNoActivaNoPuedeIniciarSesion(EstadoCuenta state) throws Exception {
        fixture("state@ejemplo.co", Rol.COMPRADOR, state, true);
        login("state@ejemplo.co", PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
    }

    @Test
    void historicoSinCredencialNoPuedeAutenticarseNiReclamarse() throws Exception {
        var user = fixture("Historical@ejemplo.co", Rol.COMPRADOR, null, false);
        login(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registration("historical@ejemplo.co"))).andExpect(status().isUnprocessableContent());
        assertThat(credenciales.existsById(user.getId())).isFalse();
        assertThat(usuarios.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("Historical@ejemplo.co");
    }

    @Test
    void historicoConCredencialRealObtieneCompradorYActivaSinActualizarDatos() throws Exception {
        var user = fixture("Old@ejemplo.co", Rol.COMPRADOR, null, true);
        var persisted = usuarios.findById(user.getId()).orElseThrow();
        persisted.getRoles().clear();
        usuarios.saveAndFlush(persisted);
        em.clear();
        String accessToken = token(user);
        mvc.perform(get("/test-security/authorities").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("ROLE_COMPRADOR"));
        assertThat(usuarios.findById(user.getId()).orElseThrow().getRoles()).isEmpty();
    }

    @Test
    void ambiguosHistoricosRechazadosSinSeleccionarUnaCuenta() throws Exception {
        fixture("Duplicate@ejemplo.co", Rol.COMPRADOR, EstadoCuenta.ACTIVA, true);
        fixture("duplicate@ejemplo.co", Rol.ADMIN_STYLERADAR, EstadoCuenta.ACTIVA, true);
        login("Duplicate@ejemplo.co", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void endpointRealProtegidoSinTokenProduce401ConFormatoExistente() throws Exception {
        mvc.perform(get("/api/v1/usuarios/1")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/usuarios/1"))
                .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.validationErrors").isMap());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalid", "Bearer", "Bearer ", "Bearer a.b.c", "Basic dXNlcjpwYXNz"})
    void autenticacionInvalidaNoDaAcceso(String header) throws Exception {
        mvc.perform(get("/api/v1/usuarios/1").header("Authorization", header))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoProduce401() throws Exception {
        var user = fixture("expired@ejemplo.co", Rol.COMPRADOR, EstadoCuenta.ACTIVA, true);
        var past = new JwtUtil(secret, Duration.ofHours(1), Clock.offset(Clock.systemUTC(), Duration.ofHours(-2)));
        mvc.perform(get("/api/v1/usuarios/{id}", user.getId()).header("Authorization", "Bearer " + past.generate(user.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoDaAccesoAlContratoRealSinSesion() throws Exception {
        var user = fixture("valid@ejemplo.co", Rol.COMPRADOR, EstadoCuenta.ACTIVA, true);
        var result = mvc.perform(get("/api/v1/usuarios/{id}", user.getId()).header("Authorization", "Bearer " + token(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void tokenEmitidoSeRechazaCuandoCambiaElEstadoActual(EstadoCuenta state) throws Exception {
        // Arrange
        var user = fixture("changed@ejemplo.co", Rol.COMPRADOR, EstadoCuenta.ACTIVA, true);
        String token = token(user);
        var changed = usuarios.findById(user.getId()).orElseThrow();
        changed.setEstadoCuenta(state);
        usuarios.saveAndFlush(changed);
        em.clear();
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/{id}", user.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenEmitidoConsultaRolesActualesYNoClaimsManipulados() throws Exception {
        // Arrange
        var user = fixture("roles@ejemplo.co", Rol.COMPRADOR, EstadoCuenta.ACTIVA, true);
        String token = token(user);
        mvc.perform(get("/test-security/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acceso denegado"));
        var changed = usuarios.findById(user.getId()).orElseThrow();
        changed.setRoles(new HashSet<>(Set.of(Rol.ADMIN_STYLERADAR)));
        usuarios.saveAndFlush(changed);
        em.clear();
        // Act & Assert: el mismo token utiliza el rol nuevo.
        mvc.perform(get("/test-security/authorities").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("ROLE_ADMIN_STYLERADAR"));
        mvc.perform(get("/test-security/admin").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        changed = usuarios.findById(user.getId()).orElseThrow();
        changed.setRoles(new HashSet<>(Set.of(Rol.COMPRADOR)));
        usuarios.saveAndFlush(changed);
        em.clear();
        String forgedRoles = Jwts.builder().subject(user.getId().toString()).issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60))).claim("roles", List.of("ADMIN_STYLERADAR"))
                .signWith(new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256"), Jwts.SIG.HS256).compact();
        mvc.perform(get("/test-security/admin").header("Authorization", "Bearer " + forgedRoles))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioEliminadoNoPuedeUsarTokenAnterior() throws Exception {
        var user = fixture("deleted@ejemplo.co", Rol.COMPRADOR, EstadoCuenta.ACTIVA, true);
        String token = token(user);
        credenciales.deleteById(user.getId());
        usuarios.deleteById(user.getId());
        em.flush();
        em.clear();
        mvc.perform(get("/api/v1/usuarios/{id}", user.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroPublicoCreaSoloCompradorActivoConHashYLoginDisponible() throws Exception {
        // Act
        String response = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Nuevo comprador","email":"New@Ejemplo.co","telefono":"+57123456789",
                         "password":"HTTP fixture password","roles":["ADMIN_STYLERADAR"],"estadoCuenta":"BLOQUEADA"}
                        """)).andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("new@ejemplo.co"))
                .andReturn().getResponse().getContentAsString();
        em.flush();
        em.clear();
        // Assert
        var user = usuarios.findByEmail("new@ejemplo.co").orElseThrow();
        assertThat(user.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(user.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(credenciales.findById(user.getId()).orElseThrow().getPasswordHash()).startsWith("$2b$");
        assertThat(response).doesNotContain("password", "passwordHash", PASSWORD);
        login("NEW@ejemplo.co", PASSWORD).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registration("new@ejemplo.co"))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void servicioIgnoraRolesPrivilegiadosYEstadoDeEntrada() {
        var result = auth.registrarComprador(Usuario.builder().nombre("New").email("service@ejemplo.co")
                .roles(Set.of(Rol.ADMIN_ALMACEN, Rol.ADMIN_STYLERADAR, Rol.REPRESENTANTE_FUNDACION))
                .estadoCuenta(EstadoCuenta.BLOQUEADA).build(), PASSWORD);
        assertThat(result.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(result.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
    }

    @Test
    void registroLegadoPermanecePublicoYCreaCuentaSinCredencial() throws Exception {
        mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Legacy\",\"email\":\"legacy@ejemplo.co\"}")).andExpect(status().isCreated());
        var user = usuarios.findByEmail("legacy@ejemplo.co").orElseThrow();
        assertThat(credenciales.existsById(user.getId())).isFalse();
        login(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"email\":\"bad\",\"password\":\"ok\"}",
        "{\"email\":\"test@ejemplo.co\"}",
        "{\"email\":\"test@ejemplo.co\",\"password\":\"\"}"
    })
    void loginRequestInvalidoEs400(String request) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordExcede72BytesOContieneNulEs400() throws Exception {
        login("test@ejemplo.co", "á".repeat(37)).andExpect(status().isBadRequest());
        login("test@ejemplo.co", "pass\0word").andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registration("bad@ejemplo.co").replace(PASSWORD, "á".repeat(37))))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.existsByEmail("bad@ejemplo.co")).isFalse();
    }

    @Test
    void registroReutilizaValidacionesExistentes() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"A","email":"bad","telefono":"abc","password":"ok"}
                        """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.nombre").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.telefono").exists());
    }

    @Test
    void swaggerPublicoGeneraBearerAuthYEndpointsReales() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/auth/registro'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/registro'].post").exists());
    }

    @Test
    void consultasPublicasExplicitasYGetPrivadosProtegidos() throws Exception {
        mvc.perform(get("/api/v1/prendas")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/usuarios/1/busquedas-guardadas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/playlists")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/almacenes/123/catalogo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoNoDejaContextoParaLaSiguienteSolicitud() throws Exception {
        mvc.perform(get("/test-security/authorities").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/test-security/authorities")).andExpect(status().isUnauthorized());
    }

    private String registration(String email) {
        return json.writeValueAsString(Map.of("nombre", "Nuevo comprador", "email", email, "password", PASSWORD));
    }

    // Exclusivo de tests: demuestra 403 sin establecer permisos de negocio en producción.
    @TestConfiguration
    static class TestSecurity {
        @Bean @Order(1)
        SecurityFilterChain testChain(HttpSecurity http, JwtAuthFilter jwt, SecurityErrorHandler errors) throws Exception {
            return http.securityMatcher("/test-security/**")
                    .csrf(csrf -> csrf.disable()).httpBasic(basic -> basic.disable()).formLogin(form -> form.disable())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requestCache(c -> c.disable())
                    .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                    .authorizeHttpRequests(a -> a.requestMatchers("/test-security/admin").hasAuthority("ROLE_ADMIN_STYLERADAR")
                            .anyRequest().authenticated())
                    .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
        }
    }

    @RestController
    static class TestEndpoints {
        @GetMapping("/test-security/authorities")
        List<String> authorities(Authentication authentication) {
            return authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        }
        @GetMapping("/test-security/admin")
        String admin() { return "authorized"; }
    }
}
