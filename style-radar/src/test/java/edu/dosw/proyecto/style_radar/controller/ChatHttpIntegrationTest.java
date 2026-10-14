package edu.dosw.proyecto.style_radar.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.support.SeguridadFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatHttpIntegrationTest extends SeguridadFixtures {

    @Autowired ObjectMapper json;
    UsuarioEntity vendedor;
    UsuarioEntity comprador;
    String tVendedor;
    String tComprador;
    long publicacionId;

    @BeforeEach
    void preparar() throws Exception {
        vendedor = usuarioConRol(Rol.COMPRADOR);
        comprador = usuarioConRol(Rol.COMPRADOR);
        tVendedor = bearer(vendedor);
        tComprador = bearer(comprador);
        publicacionId = id(mvc.perform(post("/api/v1/segunda-mano").header("Authorization", tVendedor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"prenda\":{\"nombre\":\"Jean\",\"descripcion\":\"Clásico\","
                        + "\"tipo\":\"INFERIOR\",\"marca\":\"Radar\",\"color\":\"Azul\",\"estilo\":\"CASUAL\"},"
                        + "\"talla\":\"M\",\"estadoConservacion\":\"BUEN_ESTADO\",\"precio\":50000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    long id(String body) {
        return json.readTree(body).get("id").asLong();
    }

    MockHttpServletRequestBuilder mensaje(String token, long conversacionId, String texto) {
        return post("/api/v1/conversaciones/" + conversacionId + "/mensajes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + texto + "\"}");
    }

    long iniciar() throws Exception {
        return id(mvc.perform(post("/api/v1/segunda-mano/" + publicacionId + "/conversaciones").header("Authorization", tComprador))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void compradorYVendedorCoordinanLaEntregaEnPrivado() throws Exception {
        long conversacionId = iniciar();
        // Idempotente: reintentar devuelve la misma conversación.
        mvc.perform(post("/api/v1/segunda-mano/" + publicacionId + "/conversaciones").header("Authorization", tComprador))
                .andExpect(jsonPath("$.id").value(conversacionId))
                .andExpect(jsonPath("$.vendedorId").value(vendedor.getId()))
                .andExpect(jsonPath("$.prendaNombre").value("Jean"));

        mvc.perform(mensaje(tComprador, conversacionId, "¿Nos vemos el sábado?")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorId").value(comprador.getId()));
        mvc.perform(mensaje(tVendedor, conversacionId, "Sí, en la estación Calle 72")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/conversaciones/" + conversacionId + "/mensajes").header("Authorization", tVendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].texto").value("¿Nos vemos el sábado?"));
        mvc.perform(get("/api/v1/usuarios/me/conversaciones").header("Authorization", tVendedor))
                .andExpect(jsonPath("$[0].id").value(conversacionId));

        // Tras vender se sigue coordinando; al retirar, el chat se cierra.
        mvc.perform(patch("/api/v1/usuarios/me/segunda-mano/" + publicacionId + "/vender").header("Authorization", tVendedor))
                .andExpect(status().isOk());
        mvc.perform(mensaje(tComprador, conversacionId, "Llevo el efectivo")).andExpect(status().isCreated());
    }

    @Test
    void publicacionRetiradaCierraElChat422() throws Exception {
        long conversacionId = iniciar();
        mvc.perform(delete("/api/v1/usuarios/me/segunda-mano/" + publicacionId).header("Authorization", tVendedor))
                .andExpect(status().isNoContent());
        mvc.perform(mensaje(tComprador, conversacionId, "Hola")).andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/v1/segunda-mano/" + publicacionId + "/conversaciones").header("Authorization", bearer(Rol.COMPRADOR)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void terceroNoVeNiEscribe404() throws Exception {
        long conversacionId = iniciar();
        String intruso = bearer(Rol.COMPRADOR);
        mvc.perform(get("/api/v1/conversaciones/" + conversacionId + "/mensajes").header("Authorization", intruso))
                .andExpect(status().isNotFound());
        mvc.perform(mensaje(intruso, conversacionId, "Hola")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/usuarios/me/conversaciones").header("Authorization", intruso))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void reglasDeInicio() throws Exception {
        mvc.perform(post("/api/v1/segunda-mano/" + publicacionId + "/conversaciones").header("Authorization", tVendedor))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/v1/segunda-mano/" + Long.MAX_VALUE + "/conversaciones").header("Authorization", tComprador))
                .andExpect(status().isNotFound());
        long conversacionId = iniciar();
        mvc.perform(mensaje(tComprador, conversacionId, "   ")).andExpect(status().isBadRequest());
        mvc.perform(mensaje(tComprador, conversacionId, "x".repeat(1001))).andExpect(status().isBadRequest());
    }

    @Test
    void sinJwt401YRolIncorrecto403() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me/conversaciones")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/segunda-mano/" + publicacionId + "/conversaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios/me/conversaciones").header("Authorization", bearer(Rol.ADMIN_ALMACEN)))
                .andExpect(status().isForbidden());
    }
}
