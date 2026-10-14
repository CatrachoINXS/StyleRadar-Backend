package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.*;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.support.AlertasFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @ActiveProfiles("test") @Transactional
class GestionEstadoCuentaHttpIntegrationTest extends AlertasFixtures {
    static final String BASE = "/api/v1/admin/usuarios";
    static final String PASSWORD = "RF66 fixture password";
    static final String HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper json;
    @Value("${styleradar.security.jwt.secret-base64}") String secret;
    @MockitoSpyBean UsuarioRepository repositorio;
    UsuarioEntity admin, otroAdmin;
    MockMvc mvc;

    @BeforeEach void preparar() {
        // Arrange: cuentas persistidas y cadena real de seguridad de producción.
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = usuario("admin-rf66@example.co"); admin.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        otroAdmin = usuario("otro-admin-rf66@example.co"); otroAdmin.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        em.persist(new CredencialEntity(admin, HASH)); em.persist(new CredencialEntity(otroAdmin, HASH));
        em.persist(new CredencialEntity(usuario, HASH)); releer();
    }
    @AfterEach void limpiarSpy() { reset(repositorio); }

    MockHttpServletRequestBuilder cambio(Long id, String body) {
        return patch(BASE + "/" + id + "/estado").contentType(MediaType.APPLICATION_JSON).content(body);
    }
    MockHttpServletRequestBuilder ruta(String op) {
        return switch(op) {
            case "listar" -> get(BASE);
            case "detalle" -> get(BASE + "/" + usuario.getId());
            case "cambiar" -> cambio(usuario.getId(), "{\"estado\":\"INACTIVA\"}");
            default -> throw new AssertionError(op);
        };
    }
    ResultActions login(UsuarioEntity u) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "password", PASSWORD))));
    }
    String loginToken(UsuarioEntity u) throws Exception {
        return json.readTree(login(u).andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("accessToken").asText();
    }
    static Stream<Arguments> matriz() {
        return Stream.of("listar", "detalle", "cambiar").flatMap(op ->
                Stream.of("ausente", "manipulado", "expirado", "COMPRADOR", "ADMIN_ALMACEN", "REPRESENTANTE_FUNDACION",
                        "ADMIN_STYLERADAR", "BLOQUEADA", "INACTIVA").map(tipo -> Arguments.of(op, tipo)));
    }

    @ParameterizedTest @MethodSource("matriz")
    void seguridadEnTresRutasConFiltrosReales(String op, String tipo) throws Exception {
        // Arrange
        var r = ruta(op); int codigo;
        if (tipo.equals("ausente")) codigo = 401;
        else {
            String t = jwt.generate(admin.getId()); codigo = 401;
            if (tipo.equals("manipulado")) t = t.substring(0, t.lastIndexOf('.') + 1) + "AAAA";
            else if (tipo.equals("expirado")) t = new JwtUtil(secret, Duration.ofHours(1),
                    Clock.offset(Clock.systemUTC(), Duration.ofHours(-2))).generate(admin.getId());
            else if (tipo.equals("BLOQUEADA") || tipo.equals("INACTIVA")) {
                usuarios.findById(admin.getId()).orElseThrow().setEstadoCuenta(EstadoCuenta.valueOf(tipo)); releer();
            } else {
                usuarios.findById(admin.getId()).orElseThrow().setRoles(Set.of(Rol.valueOf(tipo))); releer();
                codigo = tipo.equals("ADMIN_STYLERADAR") ? 200 : 403;
            }
            r.header("Authorization", "Bearer " + t);
        }
        // Act / Assert
        mvc.perform(r).andExpect(status().is(codigo));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "null", "{", "{\"estado\":null}", "{\"estado\":\"OTRO\"}",
            "{\"estado\":\"BLOQUEADA\"}", "{\"estado\":\"BLOQUEADA\",\"motivo\":\"   \"}",
            "{\"estado\":\"INACTIVA\",\"administradorId\":999}",
            "{\"estado\":\"INACTIVA\",\"administradorCambioEstadoId\":999}",
            "{\"estado\":\"INACTIVA\",\"fechaCambioEstado\":\"2020-01-01T00:00:00Z\"}",
            "{\"estado\":\"INACTIVA\",\"roles\":[\"ADMIN_STYLERADAR\"]}"})
    void requestInvalidoYCamposInternos400(String body) throws Exception {
        // Act / Assert
        mvc.perform(cambio(usuario.getId(), body).header("Authorization", "Bearer " + jwt.generate(admin.getId())))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest @CsvSource({"page,-1", "page,abc", "page,", "size,0", "size,101", "size,", "estado,OTRO", "estado,"})
    void consultaInvalida400(String campo, String valor) throws Exception {
        // Act / Assert
        mvc.perform(get(BASE).param(campo, valor == null ? "" : valor).header("Authorization", "Bearer " + jwt.generate(admin.getId())))
                .andExpect(status().isBadRequest());
    }
    @Test void motivoExcesivo400() throws Exception {
        // Act / Assert
        mvc.perform(cambio(usuario.getId(), json.writeValueAsString(Map.of("estado", "INACTIVA", "motivo", "x".repeat(1001))))
                .header("Authorization", "Bearer " + jwt.generate(admin.getId()))).andExpect(status().isBadRequest());
    }

    @Test void listadoDetalleYDecisionExponenSoloNueveCampos() throws Exception {
        // Arrange
        usuarios.findById(usuario.getId()).orElseThrow().setEstadoCuenta(null); releer();
        String t = loginToken(admin);
        // Act / Assert
        mvc.perform(get(BASE).header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content[0].id").value(usuario.getId()));
        var legado = json.readTree(mvc.perform(ruta("detalle").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCuenta").value("ACTIVA")).andExpect(jsonPath("$.fechaCambioEstado").isEmpty())
                .andReturn().getResponse().getContentAsString());
        assertThat(legado.size()).isEqualTo(9);
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getEstadoCuenta()).isNull();
        mvc.perform(cambio(usuario.getId(), "{\"estado\":\"BLOQUEADA\",\"motivo\":\"  Revisión  \"}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoCambioEstado").value("Revisión"))
                .andExpect(jsonPath("$.administradorCambioEstadoId").value(admin.getId()))
                .andExpect(jsonPath("$.fechaCambioEstado").exists()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get(BASE).param("estado", "BLOQUEADA").header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/usuarios/" + usuario.getId()).header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.motivoCambioEstado").doesNotExist());
    }

    @ParameterizedTest @EnumSource(EstadoCuenta.class)
    void autocambio422InclusoActivaIdempotente(EstadoCuenta estado) throws Exception {
        // Act / Assert
        mvc.perform(cambio(admin.getId(), json.writeValueAsString(Map.of("estado", estado, "motivo", "Motivo")))
                .header("Authorization", "Bearer " + jwt.generate(admin.getId())))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.message").value(
                        "El administrador no puede modificar el estado de su propia cuenta"));
    }
    @Test void compradorNoPuedeActivarseNiBloquearOtros() throws Exception {
        // Arrange
        String t = loginToken(usuario);
        // Act / Assert
        mvc.perform(cambio(usuario.getId(), "{\"estado\":\"ACTIVA\"}").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        mvc.perform(cambio(otroAdmin.getId(), "{\"estado\":\"BLOQUEADA\",\"motivo\":\"Motivo\"}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }
    @Test void cuentaInexistente404() throws Exception {
        // Arrange
        String t = jwt.generate(admin.getId());
        // Act / Assert
        mvc.perform(get(BASE + "/" + Long.MAX_VALUE).header("Authorization", "Bearer " + t)).andExpect(status().isNotFound());
        mvc.perform(cambio(Long.MAX_VALUE, "{\"estado\":\"INACTIVA\"}").header("Authorization", "Bearer " + t)).andExpect(status().isNotFound());
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void loginJwtRealCambioAdministrativoYReactivacion(EstadoCuenta estado) throws Exception {
        // Arrange: tokens obtenidos por login HTTP con credenciales reales.
        String comprador = loginToken(usuario), administrador = loginToken(admin);
        String[] privadas = {"feed", "recomendaciones", "alertas", "notificaciones"};
        for (String ruta : privadas) mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());
        // Act: suspensión desde RF66.
        mvc.perform(cambio(usuario.getId(), json.writeValueAsString(Map.of("estado", estado, "motivo", "Motivo")))
                .header("Authorization", "Bearer " + administrador)).andExpect(status().isOk()); releer();
        // Assert: mismo JWT rechazado en todos los endpoints privados y login rechazado.
        for (String ruta : privadas) mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + comprador))
                .andExpect(status().isUnauthorized());
        login(usuario).andExpect(status().isUnauthorized());
        // Act / Assert: reactivar no modifica credenciales ni roles; nuevo login da acceso.
        mvc.perform(cambio(usuario.getId(), "{\"estado\":\"ACTIVA\"}").header("Authorization", "Bearer " + administrador))
                .andExpect(status().isOk()); releer();
        String nuevo = loginToken(usuario);
        mvc.perform(get("/api/v1/usuarios/me/alertas").header("Authorization", "Bearer " + nuevo)).andExpect(status().isOk());
        // Comportamiento real: no hay revocación permanente por versión; JWT previo vigente vuelve a funcionar.
        mvc.perform(get("/api/v1/usuarios/me/alertas").header("Authorization", "Bearer " + comprador)).andExpect(status().isOk());
        assertThat(em.find(CredencialEntity.class, usuario.getId()).getPasswordHash()).isEqualTo(HASH);
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getRoles()).containsExactly(Rol.COMPRADOR);
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void administradorSuspendidoPierdeRF61RF62YRF66YRecuperaLogin(EstadoCuenta estado) throws Exception {
        // Arrange
        String t = loginToken(otroAdmin), a = loginToken(admin);
        mvc.perform(cambio(otroAdmin.getId(), json.writeValueAsString(Map.of("estado", estado, "motivo", "Motivo")))
                .header("Authorization", "Bearer " + a)).andExpect(status().isOk()); releer();
        // Act / Assert
        for (String path : List.of(BASE, "/api/v1/admin/catalogo/moderacion", "/api/v1/admin/almacenes/verificacion"))
            mvc.perform(get(path).header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/admin/catalogo/1/moderacion").contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBADA\"}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/admin/almacenes/alertas/verificacion").contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBADO\"}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
        login(otroAdmin).andExpect(status().isUnauthorized());
        mvc.perform(cambio(otroAdmin.getId(), "{\"estado\":\"ACTIVA\"}").header("Authorization", "Bearer " + a)).andExpect(status().isOk());
        releer(); String nuevo = loginToken(otroAdmin);
        mvc.perform(get(BASE).header("Authorization", "Bearer " + nuevo)).andExpect(status().isOk());
    }

    @Test void swaggerDocumentaTresRutasRolBearerYEstados() throws Exception {
        // Act
        var doc = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        // Assert
        for (String[] par : new String[][]{{"", "get"}, {"/{id}", "get"}, {"/{id}/estado", "patch"}}) {
            var op = doc.get("paths").get(BASE + par[0]).get(par[1]);
            assertThat(op.get("security").get(0).has("bearerAuth")).isTrue();
            assertThat(op.get("description").asText()).contains("ROLE_ADMIN_STYLERADAR");
            for (String c : List.of("200", "400", "401", "403", "404", "409", "422", "500")) assertThat(op.get("responses").has(c)).isTrue();
        }
        assertThat(doc.get("paths").get(BASE + "/{id}/estado").get("patch").get("requestBody").toString())
                .contains("ACTIVA", "INACTIVA", "BLOQUEADA");
    }

    @Test void conflicto409SinSql() throws Exception {
        // Arrange
        doThrow(new CannotAcquireLockException("SQL secreto")).when(repositorio).findForUpdate(admin.getId());
        // Act / Assert
        var body = mvc.perform(ruta("cambiar").header("Authorization", "Bearer " + jwt.generate(admin.getId())))
                .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("SQL secreto");
    }
    @Test void persistenciaFallida500Nunca200() throws Exception {
        // Arrange
        doThrow(new DataAccessResourceFailureException("SQL secreto")).when(repositorio).flush();
        // Act / Assert
        var body = mvc.perform(ruta("cambiar").header("Authorization", "Bearer " + jwt.generate(admin.getId())))
                .andExpect(status().isInternalServerError()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("SQL secreto");
    }
}
