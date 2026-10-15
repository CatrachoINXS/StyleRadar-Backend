package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Set;
import static org.mockito.Mockito.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.support.AlertasFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @ActiveProfiles("test") @Transactional
class VerificacionHttpIntegrationTest extends AlertasFixtures {
    static final String BASE = "/api/v1/admin/almacenes";
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    @MockitoSpyBean AlmacenRepository almacenes;
    MockMvc mvc;

    @BeforeEach void preparar() {
        // Arrange: fixtures persistidas y FilterChain real.
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void limpiarSpy() { reset(almacenes); }

    String token(Rol rol) {
        usuarios.findById(usuario.getId()).orElseThrow().setRoles(Set.of(rol));
        credenciales.crearCredencial(usuario.getId(), "moderacion fixture password");
        releer();
        return jwt.generate(usuario.getId());
    }

    MockHttpServletRequestBuilder ruta(String operacion) {
        return switch (operacion) {
            case "listar" -> get(BASE + "/verificacion").param("estado", "PENDIENTE");
            case "detalle" -> get(BASE + "/" + almacen.getNit() + "/verificacion");
            case "revisar" -> post(BASE + "/" + almacen.getNit() + "/verificacion/solicitar-revision");
            case "decidir" -> decision(almacen.getNit(), "{\"decision\":\"APROBADO\"}");
            default -> throw new AssertionError(operacion);
        };
    }

    MockHttpServletRequestBuilder decision(String nit, String body) {
        return patch(BASE + "/" + nit + "/verificacion").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @ParameterizedTest @ValueSource(strings = {"listar", "detalle", "revisar", "decidir"})
    void sinJwt401(String op) throws Exception {
        // Act / Assert
        mvc.perform(ruta(op)).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @ParameterizedTest @CsvSource({"listar,COMPRADOR", "listar,ADMIN_ALMACEN", "listar,REPRESENTANTE_FUNDACION",
            "revisar,COMPRADOR", "revisar,ADMIN_ALMACEN", "revisar,REPRESENTANTE_FUNDACION",
            "detalle,COMPRADOR", "detalle,ADMIN_ALMACEN", "detalle,REPRESENTANTE_FUNDACION",
            "decidir,COMPRADOR", "decidir,ADMIN_ALMACEN", "decidir,REPRESENTANTE_FUNDACION"})
    void rolAjeno403(String op, Rol rol) throws Exception {
        // Act / Assert
        mvc.perform(ruta(op).header("Authorization", "Bearer " + token(rol))).andExpect(status().isForbidden());
        assertThat(em.find(edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity.class, almacen.getNit()).getEstadoVerificacion()).isNull();
    }

    @ParameterizedTest @ValueSource(strings = {"listar", "detalle", "revisar", "decidir"})
    void jwtManipulado401(String op) throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        // Act / Assert
        mvc.perform(ruta(op).header("Authorization", "Bearer " + t.substring(0, t.lastIndexOf('.') + 1) + "AAAA"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"listar", "detalle", "revisar", "decidir"})
    void cuentaBloqueada401(String op) throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        usuarios.findById(usuario.getId()).orElseThrow().setEstadoCuenta(EstadoCuenta.BLOQUEADA); releer();
        // Act / Assert
        mvc.perform(ruta(op).header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @EnumSource(DecisionVerificacionAlmacen.class)
    void adminRevisaDecideYConsulta(DecisionVerificacionAlmacen d) throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        // Act / Assert: solicitar, consultar, decidir y comprobar lo persistido vía API.
        mvc.perform(ruta("revisar").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE"));
        mvc.perform(ruta("listar").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].nit").value(almacen.getNit()))
                .andExpect(jsonPath("$.content[0].nombreComercial").value("Radar"));
        mvc.perform(decision(almacen.getNit(), "{\"decision\":\"" + d + "\",\"motivo\":\" Información revisada \"}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value(d.name()))
                .andExpect(jsonPath("$.administradorDecisionId").value(usuario.getId()))
                .andExpect(jsonPath("$.fechaDecisionVerificacion").exists())
                .andExpect(jsonPath("$.motivoVerificacion").value("Información revisada"))
                .andExpect(jsonPath("$.telefono").value("123")).andExpect(jsonPath("$.usuario").doesNotExist());
        mvc.perform(ruta("listar").header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(BASE + "/verificacion").param("estado", d.name()).header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "null", "{", "{\"decision\":null}",
            "{\"decision\":\"PENDIENTE\"}", "{\"decision\":\"NO_REQUERIDA\"}", "{\"decision\":\"OTRA\"}",
            "{\"decision\":\"RECHAZADO\"}", "{\"decision\":\"RECHAZADO\",\"motivo\":\"   \"}",
            "{\"decision\":\"APROBADO\",\"administradorId\":999}",
            "{\"decision\":\"APROBADO\",\"administradorDecisionId\":999}",
            "{\"decision\":\"APROBADO\",\"fechaDecisionVerificacion\":\"2020-01-01T00:00:00Z\"}",
            "{\"decision\":\"APROBADO\",\"estadoVerificacion\":\"APROBADO\"}"})
    void requestInvalido400(String body) throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        // Act / Assert
        mvc.perform(decision(almacen.getNit(), body).header("Authorization", "Bearer " + t)).andExpect(status().isBadRequest());
    }

    @Test void detalleLegadoYNoExistente() throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        // Act / Assert
        mvc.perform(ruta("detalle").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.legadoSinVerificacion").value(true))
                .andExpect(jsonPath("$.administradorDecisionId").isEmpty());
        mvc.perform(get(BASE + "/inexistente/verificacion").header("Authorization", "Bearer " + t))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/verificacion").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test void motivoExcesivo400() throws Exception {
        // Act / Assert
        mvc.perform(decision(almacen.getNit(), "{\"decision\":\"APROBADO\",\"motivo\":\"" + "x".repeat(1001) + "\"}")
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest @CsvSource({"page,-1", "page,abc", "size,0", "size,101", "size,", "estado,OTRO", "estado,"})
    void consultaInvalida400(String campo, String valor) throws Exception {
        // Act / Assert
        mvc.perform(get(BASE + "/verificacion").param(campo, valor == null ? "" : valor)
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest @EnumSource(DecisionVerificacionAlmacen.class)
    void itemInexistente404(DecisionVerificacionAlmacen d) throws Exception {
        // Act / Assert
        mvc.perform(decision("inexistente", "{\"decision\":\"" + d + "\",\"motivo\":\"Revisada\"}")
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isNotFound());
    }

    @Test void solicitudInexistente404() throws Exception {
        // Act / Assert
        mvc.perform(post(BASE + "/inexistente" + "/verificacion/solicitar-revision")
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isNotFound());
    }

    @Test void transicionesInvalidas422YAprobacionSinMotivo() throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        // Act / Assert
        mvc.perform(ruta("decidir").header("Authorization", "Bearer " + t)).andExpect(status().isUnprocessableContent());
        mvc.perform(ruta("revisar").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(ruta("revisar").header("Authorization", "Bearer " + t)).andExpect(status().isUnprocessableContent());
        mvc.perform(ruta("decidir").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(ruta("decidir").header("Authorization", "Bearer " + t)).andExpect(status().isUnprocessableContent());
    }

    @Test void swaggerDocumentaRolRutasBearerYEjemplos() throws Exception {
        // Act
        var doc = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        // Assert
        for (String[] par : new String[][]{{"/verificacion", "get"}, {"/{nit}/verificacion", "get"}, {"/{nit}/verificacion", "patch"},
                {"/{nit}/verificacion/solicitar-revision", "post"}}) {
            var op = doc.get("paths").get(BASE + par[0]).get(par[1]);
            assertThat(op.get("security").get(0).has("bearerAuth")).isTrue();
            assertThat(op.get("description").asText()).contains("ROLE_ADMIN_STYLERADAR");
            for (String code : new String[]{"200", "400", "401", "403", "404", "409", "422", "500"})
                assertThat(op.get("responses").has(code)).isTrue();
        }
        var ejemplos = doc.get("paths").get(BASE + "/{nit}/verificacion").get("patch").get("requestBody");
        assertThat(ejemplos.toString()).contains("APROBADO", "RECHAZADO");
    }

    @ParameterizedTest @ValueSource(strings = {"revisar", "decidir"})
    void conflictoDeBloqueo409SinDetalleSql(String op) throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        doThrow(new CannotAcquireLockException("SQL técnico simulado")).when(almacenes).findForUpdate(almacen.getNit());
        // Act / Assert
        var body = mvc.perform(ruta(op).header("Authorization", "Bearer " + t)).andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("SQL técnico", "stackTrace");
    }

    @ParameterizedTest @ValueSource(strings = {"revisar", "decidir"})
    void persistenciaFallida500Nunca200(String op) throws Exception {
        // Arrange
        String t = token(Rol.ADMIN_STYLERADAR);
        if (op.equals("decidir")) em.find(edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity.class, almacen.getNit())
                .setEstadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE);
        doThrow(new DataAccessResourceFailureException("fallo técnico simulado")).when(almacenes).flush();
        // Act / Assert
        var body = mvc.perform(ruta(op).header("Authorization", "Bearer " + t)).andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("fallo técnico simulado", "stackTrace");
    }
}


