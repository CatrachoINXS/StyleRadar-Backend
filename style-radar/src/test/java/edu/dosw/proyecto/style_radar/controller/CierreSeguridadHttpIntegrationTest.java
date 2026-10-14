package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.support.PlaylistsPopularesFixtures;
import tools.jackson.databind.ObjectMapper;

/** MVC, JWT, servicios y JPA reales. Ningún mock ni cadena de seguridad de prueba. */
@SpringBootTest @ActiveProfiles("test") @Transactional
class CierreSeguridadHttpIntegrationTest extends PlaylistsPopularesFixtures {
    @Autowired WebApplicationContext context;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    @Value("${styleradar.security.jwt.secret-base64}") String secret;
    MockMvc mvc;
    UsuarioEntity otro;
    PlaylistEntity coleccion;
    PrendaEntity prenda;
    BusquedaGuardadaEntity busqueda;
    PublicacionSegundaManoEntity usada;
    String tokenPropio;
    static final String PRENDA = """
            {"nombre":"Camisa","descripcion":"Algodón","tipo":"SUPERIOR",
             "marca":"Radar","color":"Negro","estilo":"FORMAL"}
            """;

    @BeforeEach void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        otro = usuario("intruso-" + UUID.randomUUID() + "@example.co");
        coleccion = playlist(VisibilidadPlaylist.PRIVADA, 0, 0);
        prenda = prenda(EstadoModeracion.APROBADA, EstadoVerificacionAlmacen.APROBADO);
        coleccion.getPrendas().add(prenda);
        busqueda = BusquedaGuardadaEntity.builder().usuario(usuario).nombre("Privada")
                .query("Algodón").fechaCreacion(FECHA).build();
        em.persist(busqueda);
        usada = PublicacionSegundaManoEntity.builder().usuario(usuario).prenda(prenda).talla(Talla.M)
                .estadoConservacion(EstadoConservacion.BUEN_ESTADO).precio(100.0)
                .estado(EstadoPublicacion.DISPONIBLE).fechaPublicacion(FECHA).build();
        em.persist(usada);
        credenciales.crearCredencial(usuario.getId(), "cierre fixture password");
        releer();
        tokenPropio = login();
    }

    String login() throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", usuario.getEmail(), "password", "cierre fixture password"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    String crearPlaylist(Long id) {
        return json.writeValueAsString(Map.of("usuarioId", id, "nombre", "Nueva", "visibilidad", "PUBLICA"));
    }
    String editarPlaylist(String visibilidad) {
        return json.writeValueAsString(Map.of("nombre", "Editada", "descripcion", "Nueva descripción", "visibilidad", visibilidad));
    }
    String publicacion(Long id) {
        return "{\"usuarioId\":" + id + ",\"prenda\":" + PRENDA
                + ",\"talla\":\"M\",\"estadoConservacion\":\"BUEN_ESTADO\",\"precio\":200,\"fotos\":[]}";
    }

    MockHttpServletRequestBuilder operacion(String op) {
        String base = "/api/v1/usuarios/" + usuario.getId();
        String playlist = base + "/playlists/" + coleccion.getId();
        String segunda = base + "/segunda-mano/" + usada.getId();
        return switch (op) {
            case "crear" -> post("/api/v1/playlists").content(crearPlaylist(usuario.getId()));
            case "editar" -> put(playlist).content(editarPlaylist("PUBLICA"));
            case "agregar" -> post(playlist + "/prendas").content("{\"prendaId\":" + prenda.getId() + "}");
            case "retirar" -> delete(playlist + "/prendas/" + prenda.getId());
            case "eliminar" -> delete(playlist);
            case "listar" -> get(base + "/playlists");
            case "consulta" -> get("/api/v1/playlists/" + coleccion.getId()).param("usuarioConsultaId", usuario.getId().toString());
            case "preferencias" -> put(base + "/preferencias-estilo").content("{\"preferencias\":[\"FORMAL\"]}");
            case "tallas" -> put(base + "/tallas-habituales").content("{\"tallas\":[\"M\"]}");
            case "guardarBusqueda" -> post(base + "/busquedas-guardadas").content("{\"nombre\":\"Nueva\",\"query\":\"Camisa\"}");
            case "leerBusquedas" -> get(base + "/busquedas-guardadas");
            case "eliminarBusqueda" -> delete(base + "/busquedas-guardadas/" + busqueda.getId());
            case "publicarUsada" -> post("/api/v1/segunda-mano").content(publicacion(usuario.getId()));
            case "editarUsada" -> put(segunda).content(publicacion(usuario.getId()));
            case "venderUsada" -> patch(segunda + "/vender");
            case "retirarUsada" -> delete(segunda);
            case "like" -> post("/api/v1/playlists/" + coleccion.getId() + "/like")
                    .content("{\"usuarioId\":" + usuario.getId() + "}");
            default -> throw new IllegalArgumentException(op);
        };
    }

    /** Snapshot leído de nuevo desde BD, incluidas asociaciones y conteos para detectar inserts parciales. */
    String estadoPersistido() {
        releer();
        var resultado = new ArrayList<Object>();
        for (String tabla : List.of("usuarios", "usuario_preferencias_estilo", "usuario_tallas_habituales",
                "busquedas_guardadas", "playlists", "playlist_prendas", "playlist_likes",
                "publicaciones_segunda_mano", "prendas")) {
            var filas = em.createNativeQuery("select * from " + tabla).getResultList();
            resultado.add(filas.stream().map(fila -> fila instanceof Object[] columnas
                    ? Arrays.deepToString(columnas) : String.valueOf(fila)).sorted().toList());
        }
        return resultado.toString();
    }

    @ParameterizedTest @ValueSource(strings = {"crear", "editar", "agregar", "retirar", "eliminar", "listar", "consulta",
            "preferencias", "tallas", "guardarBusqueda", "leerBusquedas", "eliminarBusqueda",
            "publicarUsada", "editarUsada", "venderUsada", "retirarUsada", "like"})
    void identidadAjenaEs403SinLeerPrivadosNiModificarDatos(String op) throws Exception {
        String anterior = estadoPersistido();
        mvc.perform(operacion(op).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + new JwtUtil(secret, Duration.ofHours(1), Clock.systemUTC()).generate(otro.getId())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acceso denegado"));
        assertThat(estadoPersistido()).isEqualTo(anterior);
    }

    @ParameterizedTest @ValueSource(strings = {"ausente", "invalido", "manipulado", "expirado", "INACTIVA", "BLOQUEADA"})
    void jwtRechazadoNoPuedeEliminarDatos(String tipo) throws Exception {
        String token = tokenPropio;
        if (tipo.equals("invalido")) token = "a.b.c";
        if (tipo.equals("manipulado")) {
            var partes = token.split("\\.");
            partes[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"sub\":\"" + otro.getId() + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            token = String.join(".", partes);
        }
        if (tipo.equals("expirado")) token = new JwtUtil(secret, Duration.ofHours(1),
                Clock.offset(Clock.systemUTC(), Duration.ofHours(-2))).generate(usuario.getId());
        if (Set.of("INACTIVA", "BLOQUEADA").contains(tipo)) {
            em.find(UsuarioEntity.class, usuario.getId()).setEstadoCuenta(EstadoCuenta.valueOf(tipo));
        }
        String anterior = estadoPersistido();
        var request = operacion("eliminar");
        if (!tipo.equals("ausente")) request.header("Authorization", "Bearer " + token);
        mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.path").value(request.buildRequest(context.getServletContext()).getRequestURI()));
        assertThat(estadoPersistido()).isEqualTo(anterior);
    }

    @Test void likeDeOtroUsuarioNoExponePlaylistPrivadaNiEscribe() throws Exception {
        String anterior = estadoPersistido();
        mvc.perform(post("/api/v1/playlists/{id}/like", coleccion.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":" + otro.getId() + "}")
                .header("Authorization", "Bearer " + new JwtUtil(secret, Duration.ofHours(1), Clock.systemUTC()).generate(otro.getId())))
                .andExpect(status().isUnprocessableContent());
        assertThat(estadoPersistido()).isEqualTo(anterior);
    }

    @Test void identidadJWTResuelveConsultaPrivadaSinParametroCliente() throws Exception {
        mvc.perform(get("/api/v1/playlists/{id}", coleccion.getId()).header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(coleccion.getId()));
        mvc.perform(get("/api/v1/playlists/{id}", coleccion.getId())
                .header("Authorization", "Bearer " + new JwtUtil(secret, Duration.ofHours(1), Clock.systemUTC()).generate(otro.getId())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test void propietarioCambiaVisibilidadYRankingPorHTTPConservandoLikes() throws Exception {
        mvc.perform(operacion("listar").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(coleccion.getId()));
        mvc.perform(operacion("editar").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visibilidad").value("PUBLICA"));
        mvc.perform(operacion("like").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(1));
        mvc.perform(get("/api/v1/playlists/populares").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].likes").value(1));
        mvc.perform(put("/api/v1/usuarios/{u}/playlists/{p}", usuario.getId(), coleccion.getId())
                .contentType(MediaType.APPLICATION_JSON).content(editarPlaylist("PRIVADA"))
                .header("Authorization", "Bearer " + tokenPropio)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/playlists/populares").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        releer();
        assertThat(em.find(PlaylistEntity.class, coleccion.getId()).getLikesUsuarios()).containsExactly(usuario.getId());
    }

    @Test void perfilYBusquedasPropiasConservanContratosHTTP() throws Exception {
        for (String op : List.of("preferencias", "tallas")) {
            mvc.perform(operacion(op).contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + tokenPropio))
                    .andExpect(status().isOk());
        }
        mvc.perform(operacion("guardarBusqueda").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Nueva"));
        mvc.perform(operacion("leerBusquedas").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(operacion("eliminarBusqueda").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isNoContent());
        releer();
        var persistido = em.find(UsuarioEntity.class, usuario.getId());
        assertThat(persistido.getPreferenciasEstilo()).containsExactly(Estilo.FORMAL);
        assertThat(persistido.getTallasHabituales()).containsExactly(Talla.M);
        assertThat(em.find(BusquedaGuardadaEntity.class, busqueda.getId())).isNull();
    }

    @Test void segundaManoPropiaPermitePublicarEditarVenderYRetirar() throws Exception {
        mvc.perform(operacion("publicarUsada").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(usuario.getId()));
        mvc.perform(operacion("editarUsada").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(200));
        mvc.perform(operacion("venderUsada").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("VENDIDA"));
        mvc.perform(operacion("retirarUsada").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isNoContent());
        releer();
        assertThat(em.find(PublicacionSegundaManoEntity.class, usada.getId()).getEstado()).isEqualTo(EstadoPublicacion.RETIRADA);
    }

    @Test void identidadPropiaNoSustituyePropiedadDeLaPlaylist() throws Exception {
        em.find(PlaylistEntity.class, coleccion.getId()).setUsuario(em.find(UsuarioEntity.class, otro.getId()));
        String anterior = estadoPersistido();
        mvc.perform(operacion("eliminar").header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isUnprocessableContent());
        assertThat(estadoPersistido()).isEqualTo(anterior);
    }

    @Test void swaggerExponeNuevosRechazosDeIdentidadYPrivacidad() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/playlists'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/playlists'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/playlists/{id}'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/playlists/{id}/like'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}/busquedas-guardadas'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/segunda-mano'].post.responses['403']").exists());
    }
}
