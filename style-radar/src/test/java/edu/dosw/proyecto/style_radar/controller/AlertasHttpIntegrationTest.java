package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.support.AlertasFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @ActiveProfiles("test") @Transactional
class AlertasHttpIntegrationTest extends AlertasFixtures {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    @Value("${styleradar.security.jwt.secret-base64}") String secret;
    private MockMvc mvc;
    private static final String BASE = "/api/v1/usuarios/me";

    @BeforeEach
    void filtrosReales() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String token() {
        credenciales.crearCredencial(usuario.getId(), "alertas fixture password"); releer();
        return jwt.generate(usuario.getId());
    }

    private MockHttpServletRequestBuilder ruta(String operacion) {
        return switch (operacion) {
            case "crear" -> post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON).content("{\"itemCatalogoId\":1}");
            case "desactivar" -> patch(BASE + "/alertas/1").contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}");
            case "consultar" -> get(BASE + "/alertas");
            case "notificaciones" -> get(BASE + "/notificaciones");
            default -> throw new AssertionError(operacion);
        };
    }

    @ParameterizedTest @ValueSource(strings = {"crear", "desactivar", "consultar", "notificaciones"})
    void sinJwtEs401(String operacion) throws Exception {
        // Act & Assert
        mvc.perform(ruta(operacion)).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @ParameterizedTest @CsvSource({"crear,invalido", "desactivar,invalido", "consultar,invalido", "notificaciones,invalido",
            "crear,expirado", "desactivar,expirado", "consultar,expirado", "notificaciones,expirado"})
    void tokenInvalidoOExpiradoEs401(String operacion, String tipo) throws Exception {
        // Arrange
        String t = tipo.equals("invalido") ? "a.b.c" : new JwtUtil(secret, Duration.ofHours(1),
                Clock.offset(Clock.systemUTC(), Duration.ofHours(-2))).generate(usuario.getId());
        // Act & Assert
        mvc.perform(ruta(operacion).header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"crear", "desactivar", "consultar", "notificaciones"})
    void cuentaBloqueadaConJwtEmitidoEs401(String operacion) throws Exception {
        // Arrange
        String t = token(); usuarios.findById(usuario.getId()).orElseThrow().setEstadoCuenta(EstadoCuenta.BLOQUEADA); releer();
        // Act & Assert
        mvc.perform(ruta(operacion).header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"busquedaGuardadaId\":1,\"itemCatalogoId\":2}",
            "{\"itemCatalogoId\":0}", "{\"itemCatalogoId\":-1}", "{\"itemCatalogoId\":\"abc\"}", "{", "null"})
    void requestCreacionInvalidoEs400(String body) throws Exception {
        // Arrange
        String t = token();
        // Act & Assert
        mvc.perform(post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + t)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"activa\":true}", "{\"activa\":null}", "{\"activa\":\"abc\"}"})
    void noPermiteReactivarNiDesactivacionMalformada(String body) throws Exception {
        // Arrange
        var a = alertaBusqueda(b -> b.setMarca("Radar")); String t = token();
        // Act & Assert
        mvc.perform(patch(BASE + "/alertas/" + a.id()).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + t)).andExpect(status().isBadRequest());
    }

    @Test
    void crearBusquedaPropiaConsultarYDesactivarConDtos() throws Exception {
        // Arrange
        var b = busqueda(usuario, x -> x.setMarca("Radar")); String t = token();
        // Act
        var respuesta = mvc.perform(post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"busquedaGuardadaId\":" + b.getId() + "}").header("Authorization", "Bearer " + t))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.tipoObjetivo").value("BUSQUEDA_GUARDADA"))
                .andExpect(jsonPath("$.identificadorObjetivo").value(b.getId())).andReturn().getResponse().getContentAsString();
        Long id = json.readTree(respuesta).get("id").asLong();
        // Assert
        mvc.perform(get(BASE + "/alertas").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].usuario").doesNotExist());
        var desactivada = mvc.perform(patch(BASE + "/alertas/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activa\":false}").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false)).andExpect(jsonPath("$.fechaDesactivacion").exists())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(patch(BASE + "/alertas/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activa\":false}").header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(content().json(desactivada));
        assertThat(alertas.findById(id).orElseThrow().getUsuario().getId()).isEqualTo(usuario.getId());
    }

    @Test
    void crearItemAgotadoDa201() throws Exception {
        // Arrange
        Long id = publicar(); String t = token();
        // Act & Assert
        mvc.perform(post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON).content("{\"itemCatalogoId\":" + id + "}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoObjetivo").value("ITEM_CATALOGO"));
    }

    @ParameterizedTest @ValueSource(strings = {"busquedaGuardadaId", "itemCatalogoId"})
    void objetivoInexistenteEs404(String objetivo) throws Exception {
        // Arrange
        String t = token();
        // Act & Assert
        mvc.perform(post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON).content("{\"" + objetivo + "\":9223372036854775807}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isNotFound());
    }

    @Test
    void busquedaAjenaEs404SinRevelarPropietario() throws Exception {
        // Arrange
        var b = busqueda(usuario("otro@example.co"), x -> x.setMarca("Radar")); String t = token();
        // Act & Assert
        mvc.perform(post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON).content("{\"busquedaGuardadaId\":" + b.getId() + "}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Busqueda no accesible"));
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void reglaDeNegocioVaciaODisponibleDa422(boolean vacia) throws Exception {
        // Arrange
        Long id = vacia ? busqueda(usuario, b -> {}).getId() : publicar();
        if (!vacia) stock(id, 3);
        String t = token();
        // Act & Assert
        mvc.perform(post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"" + (vacia ? "busquedaGuardadaId" : "itemCatalogoId") + "\":" + id + "}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void duplicadoActivoEs409() throws Exception {
        // Arrange
        Long id = publicar(); service.crear(usuario.getId(), null, id); String t = token();
        // Act & Assert
        mvc.perform(post(BASE + "/alertas").contentType(MediaType.APPLICATION_JSON).content("{\"itemCatalogoId\":" + id + "}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isConflict());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void desactivarAjenaOInexistenteEs404(boolean ajena) throws Exception {
        // Arrange
        Long id = ajena ? service.crear(usuario("otro@example.co").getId(), null, publicar()).id() : Long.MAX_VALUE;
        String t = token();
        // Act & Assert
        mvc.perform(patch(BASE + "/alertas/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}")
                .header("Authorization", "Bearer " + t)).andExpect(status().isNotFound());
    }

    @Test
    void identidadSoloDelJwtYSinAccesoAColeccionesAjenasPorParametros() throws Exception {
        // Arrange
        var otro = usuario("otro@example.co"); Long id = publicar();
        var propia = service.crear(usuario.getId(), null, id); service.crear(otro.getId(), null, id); stock(id, 3);
        String t = token();
        // Act & Assert
        mvc.perform(get(BASE + "/alertas").param("usuarioId", otro.getId().toString()).header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(propia.id()));
        mvc.perform(get(BASE + "/notificaciones").param("usuarioId", otro.getId().toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":" + otro.getId() + "}").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].alertaId").value(propia.id())).andExpect(jsonPath("$.size").value(20));
    }

    @ParameterizedTest @CsvSource({"page,-1", "page,texto", "page,2147483648", "size,0", "size,101", "size,texto"})
    void paginacionInvalidaEs400(String parametro, String valor) throws Exception {
        // Arrange
        String t = token();
        // Act & Assert
        mvc.perform(get(BASE + "/notificaciones").param(parametro, valor).header("Authorization", "Bearer " + t))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest @ValueSource(strings = {"page", "size"})
    void paginacionVaciaEs400(String parametro) throws Exception {
        // Arrange
        String t = token();
        // Act & Assert
        mvc.perform(get(BASE + "/notificaciones").param(parametro, "").header("Authorization", "Bearer " + t))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void eliminarBusquedaMantieneContrato204ConOSinAlerta(boolean conAlerta) throws Exception {
        // Arrange
        var b = busqueda(usuario, x -> x.setMarca("Radar"));
        if (conAlerta) service.crear(usuario.getId(), b.getId(), null);
        String t = token();
        // Act & Assert
        mvc.perform(delete("/api/v1/usuarios/" + usuario.getId() + "/busquedas-guardadas/" + b.getId())
                .header("Authorization", "Bearer " + t)).andExpect(status().isNoContent());
        assertThat(busquedas.existsById(b.getId())).isFalse();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void retirarItemMantieneContrato204ConOSinAlerta(boolean conAlerta) throws Exception {
        // Arrange
        Long id = publicar(); if (conAlerta) service.crear(usuario.getId(), null, id);
        String t = token();
        // Act & Assert
        mvc.perform(delete("/api/v1/almacenes/alertas/catalogo/" + id).header("Authorization", "Bearer " + t))
                .andExpect(status().isNoContent());
        assertThat(items.existsById(id)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings = {"/api/v1/prendas", "/api/v1/catalogo/buscar", "/api/v1/almacenes",
            "/api/v1/almacenes/alertas/catalogo", "/api/v1/almacenes/alertas/catalogo/resumen"})
    void rutasPublicasConservanAccesoSinJwt(String ruta) throws Exception {
        // Act & Assert
        mvc.perform(get(ruta)).andExpect(status().isOk());
    }

    @ParameterizedTest @ValueSource(strings = {"/api/v1/almacenes/alertas/catalogo", "/api/v1/playlists", "/api/v1/prendas"})
    void escriturasExistentesSiguenProtegidas(String ruta) throws Exception {
        // Act & Assert
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerIncluyeTodasLasOperacionesYBearer() throws Exception {
        // Act
        var doc = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        // Assert
        for (String[] par : new String[][]{{"/alertas", "post"}, {"/alertas", "get"}, {"/alertas/{id}", "patch"}, {"/notificaciones", "get"}}) {
            var op = doc.get("paths").get(BASE + par[0]).get(par[1]);
            assertThat(op.get("security").get(0).has("bearerAuth")).isTrue();
            assertThat(op.get("responses").has("401")).isTrue();
            assertThat(op.get("responses").has("500")).isTrue();
            if (op.has("parameters")) assertThat(op.get("parameters").toString()).doesNotContain("usuarioId", "principal");
        }
        assertThat(doc.get("paths").get(BASE + "/alertas").get("post").get("responses").has("409")).isTrue();
    }
}
