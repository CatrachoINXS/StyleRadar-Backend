package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.support.PersonalizacionFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersonalizacionHttpIntegrationTest extends PersonalizacionFixtures {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    @Value("${styleradar.security.jwt.secret-base64}") String secret;
    private MockMvc mvc;

    @BeforeEach
    void seguridadReal() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String login() throws Exception {
        credenciales.crearCredencial(usuario.getId(), "personalizacion fixture password");
        releer();
        var respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"comprador@example.co\",\"password\":\"personalizacion fixture password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("accessToken").asText();
    }

    @ParameterizedTest
    @ValueSource(strings = {"feed", "recomendaciones"})
    void sinTokenDevuelve401(String ruta) throws Exception {
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/" + ruta)).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer")).andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @CsvSource({"feed,invalido", "recomendaciones,invalido", "feed,expirado", "recomendaciones,expirado"})
    void jwtInvalidoOExpiradoNoDaAcceso(String ruta, String tipo) throws Exception {
        // Arrange
        String token = tipo.equals("invalido") ? "a.b.c" : new JwtUtil(secret, Duration.ofHours(1),
                Clock.offset(Clock.systemUTC(), Duration.ofHours(-2))).generate(usuario.getId());
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"feed", "recomendaciones"})
    void jwtEmitidoPorLoginDa200ConDtoDeCatalogo(String ruta) throws Exception {
        // Arrange
        var i = item("Camisa", Estilo.FORMAL, Talla.M, 2, 0);
        String token = login();
        // Act
        var respuesta = mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].itemId").value(i.getId()))
                .andExpect(jsonPath("$.content[0].prendaId").value(i.getPrenda().getId()))
                .andExpect(jsonPath("$.content[0].almacenNit").value(almacen.getNit()))
                .andExpect(jsonPath("$.content[0].nombre").value("Camisa"))
                .andExpect(jsonPath("$.content[0].tipo").value("SUPERIOR"))
                .andExpect(jsonPath("$.content[0].marca").value("Radar"))
                .andExpect(jsonPath("$.content[0].color").value("Negro"))
                .andExpect(jsonPath("$.content[0].estilo").value("FORMAL"))
                .andExpect(jsonPath("$.content[0].precio").value(100.0))
                .andExpect(jsonPath("$.content[0].stock").value(2))
                .andExpect(jsonPath("$.content[0].estado").value("ULTIMAS_UNIDADES"))
                .andExpect(jsonPath("$.content[0].fechaPublicacion").exists())
                .andExpect(jsonPath("$.content[0].tallasDisponibles[0]").value("M")).andReturn();
        // Assert
        assertThat(respuesta.getResponse().getContentAsString()).doesNotContain("password", "email", "busquedasGuardadas");
        assertThat(respuesta.getRequest().getSession(false)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"feed", "recomendaciones"})
    void cuentaBloqueadaDespuesDeEmitirJwtEs401(String ruta) throws Exception {
        // Arrange
        String token = login();
        usuarios.findById(usuario.getId()).orElseThrow().setEstadoCuenta(EstadoCuenta.BLOQUEADA);
        releer();
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"feed", "recomendaciones"})
    void idEnQueryOBodyNoPuedeElegirOtroPerfil(String ruta) throws Exception {
        // Arrange
        var otro = usuario("otro@example.co", Set.of(Estilo.CASUAL), Set.of(Talla.L));
        var propio = item("Propia", Estilo.FORMAL, Talla.M, 4, 0);
        item("Otro perfil", Estilo.CASUAL, Talla.L, 4, 10);
        String token = login();
        // Act & Assert: parámetros desconocidos no son identidad ni participan en el ranking.
        mvc.perform(get("/api/v1/usuarios/me/" + ruta).param("usuarioId", otro.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":" + otro.getId() + "}")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].itemId").value(propio.getId()));
    }

    @Test
    void busquedasAjenasNoInfluyenEnHttpYPropiasSi() throws Exception {
        // Arrange
        perfil(Set.of(), Set.of());
        var otro = usuario("otro@example.co", Set.of(), Set.of());
        var ajena = item("Ajena", Estilo.CASUAL, Talla.L, 4, 0);
        var propia = item("Propia", Estilo.CASUAL, Talla.L, 4, 10);
        var general = item("General", Estilo.CASUAL, Talla.L, 4, 20);
        busqueda(otro, b -> b.setQuery("Ajena"));
        busqueda(usuario, b -> b.setQuery("Propia"));
        String token = login();
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/feed").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].itemId").value(propia.getId()))
                .andExpect(jsonPath("$.content[1].itemId").value(general.getId()))
                .andExpect(jsonPath("$.content[2].itemId").value(ajena.getId()));
        mvc.perform(get("/api/v1/usuarios/me/recomendaciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generalSinPreferencias").value(true))
                .andExpect(jsonPath("$.content[0].itemId").value(general.getId()));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "page,texto", "page,2147483648", "size,0", "size,101", "size,texto"})
    void paginacionInvalidaEs400(String parametro, String valor) throws Exception {
        // Arrange
        String token = login();
        // Act & Assert
        for (String ruta : new String[]{"feed", "recomendaciones"}) {
            mvc.perform(get("/api/v1/usuarios/me/" + ruta).param(parametro, valor).header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "size"})
    void parametrosVaciosSon400(String parametro) throws Exception {
        // Arrange
        String token = login();
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/feed").param(parametro, "").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/usuarios/me/recomendaciones").param(parametro, "").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginaFueraDeRangoEs200VacioConCountReal() throws Exception {
        // Arrange
        item("Una", Estilo.FORMAL, Talla.M, 4, 0);
        String token = login();
        // Act & Assert
        for (String ruta : new String[]{"feed", "recomendaciones"}) {
            mvc.perform(get("/api/v1/usuarios/me/" + ruta).param("page", "2147483647").param("size", "100")
                    .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Test
    void perfilConfiguradoSinCoincidenciasEs200YNoGeneral() throws Exception {
        // Arrange
        item("Otra", Estilo.CASUAL, Talla.L, 4, 0);
        String token = login();
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/recomendaciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.generalSinPreferencias").value(false));
    }

    @Test
    void usuarioSinCredencialesNoPuedeAutenticarseNiObtenerRecomendaciones() throws Exception {
        // Arrange
        releer();
        // Act & Assert
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"comprador@example.co\",\"password\":\"password inexistente\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios/me/recomendaciones")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/prendas", "/api/v1/catalogo/buscar", "/api/v1/almacenes",
            "/api/v1/almacenes/personalizacion/catalogo", "/api/v1/almacenes/personalizacion/catalogo/resumen",
            "/api/v1/almacenes/personalizacion/catalogo/novedades"})
    void getPublicosExistentesSiguenSinJwt(String ruta) throws Exception {
        // Arrange
        releer();
        // Act & Assert
        mvc.perform(get(ruta)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/usuarios/1", "/api/v1/usuarios/1/busquedas-guardadas", "/api/v1/playlists"})
    void rutasPrivadasAnterioresSiguenProtegidas(String ruta) throws Exception {
        // Act & Assert
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentaBearerSinParametroDeIdentidadYErrores() throws Exception {
        // Act
        var documento = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        // Assert
        for (String ruta : new String[]{"feed", "recomendaciones"}) {
            assertThat(documento.get("paths").has("/api/v1/usuarios/{id}/" + ruta)).isFalse();
            var operacion = documento.get("paths").get("/api/v1/usuarios/me/" + ruta).get("get");
            assertThat(operacion.get("security").get(0).has("bearerAuth")).isTrue();
            for (String codigo : new String[]{"200", "400", "401", "404", "500"}) {
                assertThat(operacion.get("responses").has(codigo)).isTrue();
            }
            assertThat(operacion.get("parameters").toString()).contains("page", "size").doesNotContain("usuarioId", "principal");
        }
    }
}
