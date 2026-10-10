package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Set;
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
class ModeracionHttpIntegrationTest extends AlertasFixtures {
    static final String BASE = "/api/v1/admin/catalogo";
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    MockMvc mvc;
    Long id;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        id = publicar();
    }

    String token(Rol rol) {
        usuarios.findById(usuario.getId()).orElseThrow().setRoles(Set.of(rol));
        credenciales.crearCredencial(usuario.getId(), "moderacion fixture password");
        releer();
        return jwt.generate(usuario.getId());
    }

    MockHttpServletRequestBuilder ruta(String operacion) {
        return switch (operacion) {
            case "listar" -> get(BASE + "/moderacion");
            case "revisar" -> post(BASE + "/" + id + "/moderacion/solicitar-revision");
            case "decidir" -> decision(id, "{\"decision\":\"APROBADA\"}");
            default -> throw new AssertionError(operacion);
        };
    }

    MockHttpServletRequestBuilder decision(Long itemId, String body) {
        return patch(BASE + "/" + itemId + "/moderacion").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @ParameterizedTest @ValueSource(strings = {"listar", "revisar", "decidir"})
    void sinJwt401(String op) throws Exception {
        mvc.perform(ruta(op)).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @ParameterizedTest @CsvSource({"listar,COMPRADOR", "listar,ADMIN_ALMACEN", "listar,REPRESENTANTE_FUNDACION",
            "revisar,COMPRADOR", "revisar,ADMIN_ALMACEN", "revisar,REPRESENTANTE_FUNDACION",
            "decidir,COMPRADOR", "decidir,ADMIN_ALMACEN", "decidir,REPRESENTANTE_FUNDACION"})
    void rolAjeno403(String op, Rol rol) throws Exception {
        mvc.perform(ruta(op).header("Authorization", "Bearer " + token(rol))).andExpect(status().isForbidden());
        assertThat(items.findById(id).orElseThrow().getEstadoModeracion()).isEqualTo(EstadoModeracion.NO_REQUERIDA);
    }

    @ParameterizedTest @ValueSource(strings = {"listar", "revisar", "decidir"})
    void jwtManipulado401(String op) throws Exception {
        String t = token(Rol.ADMIN_STYLERADAR);
        mvc.perform(ruta(op).header("Authorization", "Bearer " + t.substring(0, t.lastIndexOf('.') + 1) + "AAAA"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"listar", "revisar", "decidir"})
    void cuentaBloqueada401(String op) throws Exception {
        String t = token(Rol.ADMIN_STYLERADAR);
        usuarios.findById(usuario.getId()).orElseThrow().setEstadoCuenta(EstadoCuenta.BLOQUEADA); releer();
        mvc.perform(ruta(op).header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @EnumSource(DecisionModeracion.class)
    void adminRevisaDecideYConsulta(DecisionModeracion d) throws Exception {
        String t = token(Rol.ADMIN_STYLERADAR);
        mvc.perform(ruta("revisar").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoModeracion").value("PENDIENTE"));
        mvc.perform(ruta("listar").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].itemId").value(id))
                .andExpect(jsonPath("$.content[0].prenda.nombre").value("Camisa especial"));
        mvc.perform(decision(id, "{\"decision\":\"" + d + "\",\"motivo\":\" Información revisada \"}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoModeracion").value(d.name()))
                .andExpect(jsonPath("$.administradorDecisionId").value(usuario.getId()))
                .andExpect(jsonPath("$.fechaDecisionModeracion").exists())
                .andExpect(jsonPath("$.motivoModeracion").value("Información revisada"))
                .andExpect(jsonPath("$.precio").value(100.0)).andExpect(jsonPath("$.usuario").doesNotExist());
        mvc.perform(ruta("listar").header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(BASE + "/moderacion").param("estado", d.name()).header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "null", "{", "{\"decision\":null}",
            "{\"decision\":\"PENDIENTE\"}", "{\"decision\":\"NO_REQUERIDA\"}", "{\"decision\":\"OTRA\"}",
            "{\"decision\":\"RECHAZADA\"}", "{\"decision\":\"RECHAZADA\",\"motivo\":\"   \"}",
            "{\"decision\":\"APROBADA\",\"administradorId\":999}",
            "{\"decision\":\"APROBADA\",\"administradorDecisionId\":999}",
            "{\"decision\":\"APROBADA\",\"estadoModeracion\":\"APROBADA\"}"})
    void requestInvalido400(String body) throws Exception {
        String t = token(Rol.ADMIN_STYLERADAR);
        mvc.perform(decision(id, body).header("Authorization", "Bearer " + t)).andExpect(status().isBadRequest());
    }

    @Test void motivoExcesivo400() throws Exception {
        mvc.perform(decision(id, "{\"decision\":\"APROBADA\",\"motivo\":\"" + "x".repeat(1001) + "\"}")
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest @CsvSource({"page,-1", "page,abc", "size,0", "size,101", "size,", "estado,OTRO", "estado,"})
    void consultaInvalida400(String campo, String valor) throws Exception {
        mvc.perform(get(BASE + "/moderacion").param(campo, valor == null ? "" : valor)
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest @EnumSource(DecisionModeracion.class)
    void itemInexistente404(DecisionModeracion d) throws Exception {
        mvc.perform(decision(Long.MAX_VALUE, "{\"decision\":\"" + d + "\",\"motivo\":\"Revisada\"}")
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isNotFound());
    }

    @Test void solicitudInexistente404() throws Exception {
        mvc.perform(post(BASE + "/" + Long.MAX_VALUE + "/moderacion/solicitar-revision")
                .header("Authorization", "Bearer " + token(Rol.ADMIN_STYLERADAR))).andExpect(status().isNotFound());
    }

    @Test void compradorNoApruebaMedianteEdicionExistente() throws Exception {
        items.findById(id).orElseThrow().setEstadoModeracion(EstadoModeracion.PENDIENTE);
        String t = token(Rol.COMPRADOR);
        mvc.perform(put("/api/v1/almacenes/alertas/catalogo/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Editada\",\"descripcion\":\"Descripción\",\"tipo\":\"SUPERIOR\","
                        + "\"marca\":\"Radar\",\"color\":\"Negro\",\"estilo\":\"FORMAL\",\"precio\":100,"
                        + "\"estadoModeracion\":\"APROBADA\",\"decision\":\"APROBADA\",\"administradorId\":999}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        releer();
        var item = items.findById(id).orElseThrow();
        assertThat(item.getEstadoModeracion()).isEqualTo(EstadoModeracion.PENDIENTE);
        assertThat(item.getAdministradorDecisionId()).isNull();
    }

    @Test void transicionesInvalidas422YAprobacionSinMotivo() throws Exception {
        String t = token(Rol.ADMIN_STYLERADAR);
        mvc.perform(ruta("decidir").header("Authorization", "Bearer " + t)).andExpect(status().isUnprocessableContent());
        mvc.perform(ruta("revisar").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(ruta("revisar").header("Authorization", "Bearer " + t)).andExpect(status().isUnprocessableContent());
        mvc.perform(ruta("decidir").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(ruta("decidir").header("Authorization", "Bearer " + t)).andExpect(status().isUnprocessableContent());
    }

    @Test void swaggerDocumentaRolRutasBearerYEjemplos() throws Exception {
        var doc = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        for (String[] par : new String[][]{{"/moderacion", "get"}, {"/{itemId}/moderacion", "patch"},
                {"/{itemId}/moderacion/solicitar-revision", "post"}}) {
            var op = doc.get("paths").get(BASE + par[0]).get(par[1]);
            assertThat(op.get("security").get(0).has("bearerAuth")).isTrue();
            assertThat(op.get("description").asText()).contains("ROLE_ADMIN_STYLERADAR");
            for (String code : new String[]{"200", "400", "401", "403", "404", "409", "422", "500"})
                assertThat(op.get("responses").has(code)).isTrue();
        }
        var ejemplos = doc.get("paths").get(BASE + "/{itemId}/moderacion").get("patch").get("requestBody");
        assertThat(ejemplos.toString()).contains("APROBADA", "RECHAZADA");
    }
}
