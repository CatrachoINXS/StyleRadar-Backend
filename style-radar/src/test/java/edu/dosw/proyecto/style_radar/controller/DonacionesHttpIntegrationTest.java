package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class DonacionesHttpIntegrationTest extends SeguridadFixtures {

    static final String FRANJA = "{\"puntoEntrega\":\"Portería principal\","
            + "\"franjaInicio\":\"2099-01-10T14:00:00Z\",\"franjaFin\":\"2099-01-10T16:00:00Z\"}";

    @Autowired ObjectMapper json;
    UsuarioEntity admin;
    UsuarioEntity donante;
    UsuarioEntity representante;
    String tAdmin;
    String tDonante;
    String tRepresentante;

    @BeforeEach
    void preparar() {
        admin = usuarioConRol(Rol.ADMIN_STYLERADAR);
        donante = usuarioConRol(Rol.COMPRADOR);
        representante = usuarioConRol(Rol.COMPRADOR);
        tAdmin = bearer(admin);
        tDonante = bearer(donante);
        tRepresentante = bearer(representante);
    }

    String fundacionJson(String nombre, Long representanteId, String categorias) {
        return "{\"nombre\":\"" + nombre + "\",\"direccion\":\"Calle 1 # 2-3\",\"email\":\"f@example.co\","
                + "\"latitud\":4.7,\"longitud\":-74.04,\"categoriasAceptadas\":" + categorias
                + ",\"representanteId\":" + representanteId + "}";
    }

    long registrarFundacion(String nombre, String categorias) throws Exception {
        return id(mvc.perform(post("/api/v1/admin/fundaciones").header("Authorization", tAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(fundacionJson(nombre, representante.getId(), categorias)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    long publicarParaDonar(String token) throws Exception {
        long id = id(mvc.perform(post("/api/v1/segunda-mano").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"prenda\":{\"nombre\":\"Chaqueta\",\"descripcion\":\"Abrigada\","
                        + "\"tipo\":\"SUPERIOR\",\"marca\":\"Radar\",\"color\":\"Negro\",\"estilo\":\"CASUAL\"},"
                        + "\"talla\":\"M\",\"estadoConservacion\":\"COMO_NUEVO\",\"precio\":0}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(patch("/api/v1/usuarios/me/segunda-mano/" + id + "/donacion").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_DONACION"));
        return id;
    }

    MockHttpServletRequestBuilder solicitar(String token, long publicacionId, long fundacionId) {
        return post("/api/v1/usuarios/me/donaciones").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicacionId\":" + publicacionId + ",\"fundacionId\":" + fundacionId + ",\"mensaje\":\"Para el invierno\"}");
    }

    long id(String body) {
        return json.readTree(body).get("id").asLong();
    }

    @Test
    void flujoCompletoDeDonacionDesdeRegistroHastaRecepcion() throws Exception {
        long fundacionId = registrarFundacion("Abrigo para Todos", "[\"SUPERIOR\",\"INFERIOR\"]");
        long publicacionId = publicarParaDonar(tDonante);

        mvc.perform(get("/api/v1/fundaciones").param("latitud", "4.71").param("longitud", "-74.05").param("tipo", "SUPERIOR")
                        .header("Authorization", tDonante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fundacionId))
                .andExpect(jsonPath("$[0].distanciaKm").isNumber());

        long solicitudId = id(mvc.perform(solicitar(tDonante, publicacionId, fundacionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.fundacionNombre").value("Abrigo para Todos"))
                .andExpect(jsonPath("$.prenda.nombre").value("Chaqueta"))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/v1/fundaciones/mi-fundacion/solicitudes").param("estado", "PENDIENTE")
                        .header("Authorization", tRepresentante))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(solicitudId));

        mvc.perform(patch("/api/v1/fundaciones/mi-fundacion/solicitudes/" + solicitudId + "/decision")
                        .header("Authorization", tRepresentante).contentType(MediaType.APPLICATION_JSON).content("{\"aceptar\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACEPTADA"));

        mvc.perform(patch("/api/v1/donaciones/" + solicitudId + "/recogida").header("Authorization", tDonante)
                        .contentType(MediaType.APPLICATION_JSON).content(FRANJA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COORDINADA"))
                .andExpect(jsonPath("$.puntoEntrega").value("Portería principal"));

        mvc.perform(patch("/api/v1/fundaciones/mi-fundacion/solicitudes/" + solicitudId + "/recepcion")
                        .header("Authorization", tRepresentante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.fechaRecepcion").exists());

        mvc.perform(get("/api/v1/segunda-mano/" + publicacionId).header("Authorization", tDonante))
                .andExpect(jsonPath("$.estado").value("DONADA"));
        mvc.perform(get("/api/v1/usuarios/me/donaciones").header("Authorization", tDonante))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value("RECIBIDA"));
    }

    @Test
    void segundaSolicitudEnCursoSobreLaMismaPrenda409() throws Exception {
        long fundacionId = registrarFundacion("Fundación Única", "[\"SUPERIOR\"]");
        long publicacionId = publicarParaDonar(tDonante);
        mvc.perform(solicitar(tDonante, publicacionId, fundacionId)).andExpect(status().isCreated());
        mvc.perform(solicitar(tDonante, publicacionId, fundacionId)).andExpect(status().isConflict());
    }

    @Test
    void rechazoSinMotivo400YConMotivoLiberaLaPrenda() throws Exception {
        long fundacionId = registrarFundacion("Fundación Rechazo", "[\"SUPERIOR\"]");
        long publicacionId = publicarParaDonar(tDonante);
        long solicitudId = id(mvc.perform(solicitar(tDonante, publicacionId, fundacionId))
                .andReturn().getResponse().getContentAsString());
        String ruta = "/api/v1/fundaciones/mi-fundacion/solicitudes/" + solicitudId + "/decision";

        mvc.perform(patch(ruta).header("Authorization", tRepresentante).contentType(MediaType.APPLICATION_JSON)
                .content("{\"aceptar\":false}")).andExpect(status().isBadRequest());
        mvc.perform(patch(ruta).header("Authorization", tRepresentante).contentType(MediaType.APPLICATION_JSON)
                .content("{\"aceptar\":false,\"motivo\":\"Sin espacio\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADA"));
        mvc.perform(patch(ruta).header("Authorization", tRepresentante).contentType(MediaType.APPLICATION_JSON)
                .content("{\"aceptar\":true}")).andExpect(status().isUnprocessableContent());
        // Rechazada: la prenda puede ofrecerse de nuevo.
        mvc.perform(solicitar(tDonante, publicacionId, fundacionId)).andExpect(status().isCreated());
    }

    @Test
    void fundacionDesactivadaOQueNoAceptaElTipo422() throws Exception {
        long soloCalzado = registrarFundacion("Solo Calzado", "[\"CALZADO\"]");
        long publicacionId = publicarParaDonar(tDonante);
        mvc.perform(solicitar(tDonante, publicacionId, soloCalzado)).andExpect(status().isUnprocessableContent());

        mvc.perform(patch("/api/v1/admin/fundaciones/" + soloCalzado + "/desactivar").header("Authorization", tAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
        mvc.perform(patch("/api/v1/admin/fundaciones/" + soloCalzado + "/desactivar").header("Authorization", tAdmin))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/v1/fundaciones").param("latitud", "4.7").param("longitud", "-74")
                .header("Authorization", tDonante)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void prendaNoMarcadaParaDonacion422YPrendaAjena422() throws Exception {
        long fundacionId = registrarFundacion("Fundación Validaciones", "[\"SUPERIOR\"]");
        long ajena = publicarParaDonar(bearer(Rol.COMPRADOR));
        mvc.perform(solicitar(tDonante, ajena, fundacionId)).andExpect(status().isUnprocessableContent());
        mvc.perform(solicitar(tDonante, Long.MAX_VALUE, fundacionId)).andExpect(status().isNotFound());
    }

    @Test
    void terceroNoPuedeCoordinarRecogida404YFranjaInvertida422() throws Exception {
        long fundacionId = registrarFundacion("Fundación Coordinación", "[\"SUPERIOR\"]");
        long solicitudId = id(mvc.perform(solicitar(tDonante, publicarParaDonar(tDonante), fundacionId))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(patch("/api/v1/fundaciones/mi-fundacion/solicitudes/" + solicitudId + "/decision")
                .header("Authorization", tRepresentante).contentType(MediaType.APPLICATION_JSON).content("{\"aceptar\":true}"));

        mvc.perform(patch("/api/v1/donaciones/" + solicitudId + "/recogida").header("Authorization", bearer(Rol.COMPRADOR))
                .contentType(MediaType.APPLICATION_JSON).content(FRANJA)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/donaciones/" + solicitudId + "/recogida").header("Authorization", tRepresentante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntoEntrega\":\"Sede\","
                        + "\"franjaInicio\":\"2099-01-10T16:00:00Z\",\"franjaFin\":\"2099-01-10T14:00:00Z\"}"))
                .andExpect(status().isUnprocessableContent());
        // La recepción exige COORDINADA.
        mvc.perform(patch("/api/v1/fundaciones/mi-fundacion/solicitudes/" + solicitudId + "/recepcion")
                .header("Authorization", tRepresentante)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void adminRegistraEditaYConflictos409() throws Exception {
        long id = registrarFundacion("Fundación Original", "[\"SUPERIOR\"]");
        mvc.perform(post("/api/v1/admin/fundaciones").header("Authorization", tAdmin).contentType(MediaType.APPLICATION_JSON)
                .content(fundacionJson("fundación original", usuarioConRol(Rol.COMPRADOR).getId(), "[\"SUPERIOR\"]")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/admin/fundaciones").header("Authorization", tAdmin).contentType(MediaType.APPLICATION_JSON)
                .content(fundacionJson("Otra", representante.getId(), "[\"SUPERIOR\"]")))
                .andExpect(status().isConflict());

        UsuarioEntity nuevo = usuarioConRol(Rol.COMPRADOR);
        mvc.perform(put("/api/v1/admin/fundaciones/" + id).header("Authorization", tAdmin).contentType(MediaType.APPLICATION_JSON)
                .content(fundacionJson("Fundación Renombrada", nuevo.getId(), "[\"CALZADO\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Fundación Renombrada"))
                .andExpect(jsonPath("$.representanteId").value(nuevo.getId()));
        releer();
        assertThat(usuarios.findById(representante.getId()).orElseThrow().getRoles()).doesNotContain(Rol.REPRESENTANTE_FUNDACION);
        assertThat(usuarios.findById(nuevo.getId()).orElseThrow().getRoles()).contains(Rol.REPRESENTANTE_FUNDACION);
        mvc.perform(put("/api/v1/admin/fundaciones/" + Long.MAX_VALUE).header("Authorization", tAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(fundacionJson("X", nuevo.getId(), "[\"CALZADO\"]")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/fundaciones").header("Authorization", tAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nombre\":\"\"}", "{\"nombre\":\"A\",\"direccion\":\"B\",\"email\":\"no-email\","
            + "\"latitud\":100,\"longitud\":0,\"categoriasAceptadas\":[],\"representanteId\":1}"})
    void fundacionInvalida400(String body) throws Exception {
        mvc.perform(post("/api/v1/admin/fundaciones").header("Authorization", tAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void consultaCercanasSinCoordenadas400() throws Exception {
        mvc.perform(get("/api/v1/fundaciones").header("Authorization", tDonante)).andExpect(status().isBadRequest());
    }

    @Test
    void sinJwt401() throws Exception {
        mvc.perform(get("/api/v1/fundaciones").param("latitud", "4").param("longitud", "-74")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/fundaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios/me/donaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/fundaciones/mi-fundacion/solicitudes")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/donaciones/1/recogida").contentType(MediaType.APPLICATION_JSON).content(FRANJA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolIncorrecto403() throws Exception {
        mvc.perform(get("/api/v1/admin/fundaciones").header("Authorization", tDonante)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/fundaciones").header("Authorization", tDonante).contentType(MediaType.APPLICATION_JSON)
                .content(fundacionJson("Intrusa", donante.getId(), "[\"SUPERIOR\"]"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/fundaciones/mi-fundacion/solicitudes").header("Authorization", tDonante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios/me/donaciones").header("Authorization", bearer(Rol.REPRESENTANTE_FUNDACION)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/fundaciones").param("latitud", "4").param("longitud", "-74").header("Authorization", tAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    void representanteSinFundacion404() throws Exception {
        mvc.perform(get("/api/v1/fundaciones/mi-fundacion/solicitudes").header("Authorization", bearer(Rol.REPRESENTANTE_FUNDACION)))
                .andExpect(status().isNotFound());
    }
}
