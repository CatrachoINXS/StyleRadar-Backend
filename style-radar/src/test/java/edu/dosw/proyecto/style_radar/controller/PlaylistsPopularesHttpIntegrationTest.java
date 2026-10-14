package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.support.PlaylistsPopularesFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @ActiveProfiles("test") @Transactional
class PlaylistsPopularesHttpIntegrationTest extends PlaylistsPopularesFixtures {
    static final String RUTA = "/api/v1/playlists/populares";
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    @Value("${styleradar.security.jwt.secret-base64}") String secret;
    MockMvc mvc;
    @BeforeEach void seguridadReal() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    String token() {
        credenciales.crearCredencial(usuario.getId(), "popular fixture password"); releer(); return jwt.generate(usuario.getId());
    }
    @Test void sinJwtEs401() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
    @ParameterizedTest @ValueSource(strings = {"invalido", "expirado"})
    void jwtInvalidoOExpiradoEs401(String tipo) throws Exception {
        token(); String t = tipo.equals("invalido") ? "a.b.c" : new JwtUtil(secret, Duration.ofHours(1),
                Clock.offset(Clock.systemUTC(), Duration.ofHours(-2))).generate(usuario.getId());
        mvc.perform(get(RUTA).header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void cuentaSuspendidaDespuesDeEmitirTokenEs401(EstadoCuenta estado) throws Exception {
        String t = token(); em.find(UsuarioEntity.class, usuario.getId()).setEstadoCuenta(estado); releer();
        mvc.perform(get(RUTA).header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
    }
    @Test void compradorActivoRutaLiteralDtoYPrivacidad() throws Exception {
        var privada = playlist(VisibilidadPlaylist.PRIVADA, 1000, 100);
        var publica = playlist(VisibilidadPlaylist.PUBLICA, 10, 0); String t = token();
        var body = mvc.perform(get(RUTA).param("usuarioConsultaId", usuario.getId().toString())
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(publica.getId()))
                .andExpect(jsonPath("$.content[0].likes").value(10)).andExpect(jsonPath("$.content[0].prendas").isEmpty())
                .andExpect(jsonPath("$.content[0].totalPrendas").value(0))
                .andExpect(jsonPath("$.content[0].likesUsuarios").doesNotExist())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true)).andExpect(jsonPath("$.last").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(privada.getNombre(), "likesUsuarios", "roles", "email");
        // La ruta numérica anterior conserva su contrato y no se reemplaza por la literal.
        mvc.perform(get("/api/v1/playlists/" + publica.getId()).header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(publica.getId()));
    }
    @ParameterizedTest @CsvSource({"page,-1", "page,texto", "page,2147483648", "size,0", "size,-1", "size,101", "size,texto"})
    void paginacionInvalidaEs400(String parametro, String valor) throws Exception {
        String t = token();
        mvc.perform(get(RUTA).param(parametro, valor).header("Authorization", "Bearer " + t))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }
    @ParameterizedTest @ValueSource(strings = {"page", "size"})
    void parametrosVaciosEs400(String parametro) throws Exception {
        String t = token(); mvc.perform(get(RUTA).param(parametro, "").header("Authorization", "Bearer " + t))
                .andExpect(status().isBadRequest());
    }
    @ParameterizedTest @CsvSource({"0,1,1", "0,2,2", "1,2,1", "0,100,3", "10,20,0", "2147483647,100,0"})
    void paginacionHttpYOverflowConCountCorrecto(int page, int size, int cantidad) throws Exception {
        for (int n = 0; n < 3; n++) playlist(VisibilidadPlaylist.PUBLICA, n, 0);
        playlist(VisibilidadPlaylist.PRIVADA, 100, 0); String t = token();
        var b = json.readTree(mvc.perform(get(RUTA).param("page", String.valueOf(page)).param("size", String.valueOf(size))
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size)).andReturn().getResponse().getContentAsString());
        assertThat(b.get("content").size()).isEqualTo(cantidad);
    }
    @Test void sinPublicasEs200Vacio() throws Exception {
        playlist(VisibilidadPlaylist.PRIVADA, 100, 0); String t = token();
        mvc.perform(get(RUTA).header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void falsificarIdentidadEs403SinModificarLikes() throws Exception {
        var otra = usuario("otra@example.co"); var p = playlist(VisibilidadPlaylist.PUBLICA, 0, 0); String t = token();
        mvc.perform(post("/api/v1/playlists/" + p.getId() + "/like").header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":" + otra.getId() + "}"))
                .andExpect(status().isForbidden());
        releer(); assertThat(repository.findById(p.getId()).orElseThrow().getLikesUsuarios()).isEmpty();
        assertThat(servicio.obtenerPopulares(0, 20).getContent().getFirst().getLikesCount()).isZero();
    }
    @Test void identidadPropiaAlternaSinDuplicadosYActualizaRanking() throws Exception {
        var a = playlist(VisibilidadPlaylist.PUBLICA, 0, 0); var b = playlist(VisibilidadPlaylist.PUBLICA, 0, 10); String t = token();
        for (int esperado : new int[]{1, 0, 1}) {
            mvc.perform(post("/api/v1/playlists/" + a.getId() + "/like").header("Authorization", "Bearer " + t)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":" + usuario.getId() + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(esperado)); releer();
            mvc.perform(get(RUTA).header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(esperado == 1 ? a.getId() : b.getId()));
        }
        assertThat(repository.findById(a.getId()).orElseThrow().getLikesUsuarios()).containsExactly(usuario.getId());
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void filtraRF61RF62SinCambiarLikesNiAsociaciones(boolean todasOcultas) throws Exception {
        var p = playlist(VisibilidadPlaylist.PUBLICA, 10, 0);
        var visible = prenda(EstadoModeracion.APROBADA, EstadoVerificacionAlmacen.APROBADO);
        var rechazadas = List.of(prenda(EstadoModeracion.RECHAZADA, EstadoVerificacionAlmacen.APROBADO),
                prenda(EstadoModeracion.APROBADA, EstadoVerificacionAlmacen.RECHAZADO),
                prenda(EstadoModeracion.PENDIENTE, EstadoVerificacionAlmacen.APROBADO),
                prenda(EstadoModeracion.APROBADA, EstadoVerificacionAlmacen.PENDIENTE));
        p.getPrendas().addAll(rechazadas); if (!todasOcultas) p.getPrendas().add(visible); String t = token();
        var b = json.readTree(mvc.perform(get(RUTA).header("Authorization", "Bearer " + t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likes").value(10))
                .andExpect(jsonPath("$.content[0].totalPrendas").value(todasOcultas ? 0 : 1))
                .andReturn().getResponse().getContentAsString());
        assertThat(b.get("content").get(0).get("prendas").size()).isEqualTo(todasOcultas ? 0 : 1);
        if (!todasOcultas) assertThat(b.get("content").get(0).get("prendas").get(0).get("id").asLong()).isEqualTo(visible.getId());
        assertThat(repository.findById(p.getId()).orElseThrow().getPrendas()).hasSize(todasOcultas ? 4 : 5);
        servicio.editar(usuario.getId(), p.getId(), "Editada", "Desc", VisibilidadPlaylist.PUBLICA, Estilo.CASUAL);
        servicio.retirarPrenda(usuario.getId(), p.getId(), rechazadas.getFirst().getId()); releer();
        assertThat(repository.findById(p.getId()).orElseThrow().getPrendas()).hasSize(todasOcultas ? 3 : 4);
        assertThat(servicio.obtenerPopulares(0, 20).getContent().getFirst().getLikesCount()).isEqualTo(10);
    }
    @Test void swaggerDocumentaRutaBearerParametrosRespuestaYErrores() throws Exception {
        var api = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        var op = api.get("paths").get(RUTA).get("get");
        assertThat(op.get("security").get(0).has("bearerAuth")).isTrue();
        for (String codigo : List.of("200", "400", "401", "500")) assertThat(op.get("responses").has(codigo)).isTrue();
        assertThat(op.get("parameters").toString()).contains("page", "size", "100", "20").doesNotContain("usuarioConsultaId");
        assertThat(op.get("description").asText()).contains("PUBLICA", "DESC", "ASC", "cero likes", "PageResponseDTO");
        assertThat(op.get("responses").get("200").toString()).contains("PageResponseDTOPlaylistResponseDTO");
        assertThat(api.get("paths").has("/api/v1/playlists/{id}")).isTrue();
        assertThat(api.get("paths").get("/api/v1/playlists/{id}/like").get("post").get("responses").has("403")).isTrue();
    }
}
