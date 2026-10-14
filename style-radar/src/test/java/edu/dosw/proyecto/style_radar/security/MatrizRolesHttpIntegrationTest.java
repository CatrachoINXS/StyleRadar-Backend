package edu.dosw.proyecto.style_radar.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.support.SeguridadFixtures;

/**
 * Política global (líder técnico): sin token 401, rol incorrecto 403, rol correcto 200,
 * y el usuario siempre sale del JWT, nunca de la URL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatrizRolesHttpIntegrationTest extends SeguridadFixtures {

    @ParameterizedTest(name = "{0} {1} sin token -> 401")
    @CsvSource({
            "GET,/api/v1/usuarios/me", "PUT,/api/v1/usuarios/me/preferencias-estilo",
            "GET,/api/v1/usuarios/me/busquedas-guardadas", "GET,/api/v1/usuarios/1",
            "POST,/api/v1/segunda-mano", "GET,/api/v1/segunda-mano", "GET,/api/v1/usuarios/me/segunda-mano",
            "PATCH,/api/v1/usuarios/me/segunda-mano/1/vender",
            "POST,/api/v1/playlists", "GET,/api/v1/playlists", "GET,/api/v1/usuarios/me/playlists",
            "POST,/api/v1/playlists/1/like", "POST,/api/v1/playlists/1/guardar",
            "GET,/api/v1/admin/fundaciones", "GET,/api/v1/usuarios/me/probador/foto"})
    void sinToken401(String metodo, String ruta) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), ruta).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @ParameterizedTest(name = "{0} {1} con {2} -> 403")
    @CsvSource({
            "GET,/api/v1/usuarios/me,ADMIN_ALMACEN",
            "GET,/api/v1/usuarios/me/busquedas-guardadas,REPRESENTANTE_FUNDACION",
            "GET,/api/v1/usuarios/1,COMPRADOR",
            "GET,/api/v1/usuarios/me/segunda-mano,ADMIN_STYLERADAR",
            "GET,/api/v1/usuarios/me/playlists,ADMIN_ALMACEN",
            "POST,/api/v1/playlists/1/like,REPRESENTANTE_FUNDACION",
            "GET,/api/v1/admin/fundaciones,COMPRADOR",
            "GET,/api/v1/admin/fundaciones,ADMIN_ALMACEN"})
    void rolIncorrecto403(String metodo, String ruta, Rol rol) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), ruta).header("Authorization", bearer(rol))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acceso denegado"));
    }

    @Test
    void rolCorrecto200YElUsuarioSaleDelToken() throws Exception {
        UsuarioEntity ana = usuarioConRol(Rol.COMPRADOR);
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(ana)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ana.getId()));
        mvc.perform(get("/api/v1/usuarios/" + ana.getId()).header("Authorization", bearer(Rol.ADMIN_STYLERADAR)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/playlists").header("Authorization", bearer(Rol.ADMIN_ALMACEN))).andExpect(status().isOk());
    }

    @Test
    void playlistAjenaNoSePuedeModificarConElTokenPropio() throws Exception {
        String dueno = bearer(Rol.COMPRADOR);
        String otro = bearer(Rol.COMPRADOR);
        String body = mvc.perform(post("/api/v1/playlists").header("Authorization", dueno).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mi estilo\",\"visibilidad\":\"PRIVADA\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(request(HttpMethod.DELETE, "/api/v1/usuarios/me/playlists/" + id).header("Authorization", otro))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/v1/playlists/" + id).header("Authorization", otro)).andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/v1/playlists/" + id + "/guardar").header("Authorization", otro))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/v1/playlists/" + id).header("Authorization", dueno)).andExpect(status().isOk());
    }

    @Test
    void playlistAdmiteMaximo15Prendas() throws Exception {
        String token = bearer(Rol.COMPRADOR);
        String body = mvc.perform(post("/api/v1/playlists").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Llena\",\"visibilidad\":\"PUBLICA\"}")).andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        for (int i = 0; i < 16; i++) {
            var prenda = new edu.dosw.proyecto.style_radar.model.entity.PrendaEntity(null, "P" + i, "D",
                    edu.dosw.proyecto.style_radar.model.domain.TipoPrenda.SUPERIOR, "M", "C", null);
            em.persist(prenda);
            var resultado = mvc.perform(post("/api/v1/usuarios/me/playlists/" + id + "/prendas").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"prendaId\":" + prenda.getId() + "}"));
            resultado.andExpect(i < 15 ? status().isOk() : status().isUnprocessableContent());
        }
        String guardada = mvc.perform(post("/api/v1/playlists/" + id + "/guardar").header("Authorization", bearer(Rol.COMPRADOR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPrendas").value(15))
                .andReturn().getResponse().getContentAsString();
        assertThat(guardada).contains("\"guardados\":1");
    }

    @Test
    void rutaInexistente404SinStackTrace() throws Exception {
        mvc.perform(get("/api/v1/usuarios/1/busquedas-guardadas/5").header("Authorization", bearer(Rol.COMPRADOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La ruta solicitada no existe"));
    }

    @Test
    void corsPermiteElFrontendLocalYRechazaOtrosOrigenes() throws Exception {
        mvc.perform(options("/api/v1/playlists").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(options("/api/v1/playlists").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
