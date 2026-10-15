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
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.SuscripcionAlmacenRepository;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.support.SuscripcionesFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @ActiveProfiles("test") @Transactional
class SuscripcionesHttpIntegrationTest extends SuscripcionesFixtures {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper json;
    @Value("${styleradar.security.jwt.secret-base64}") String secret;
    @MockitoSpyBean Clock clock;
    @MockitoSpyBean SuscripcionAlmacenRepository repositorio;
    MockMvc mvc;
    Long suscripcionId;

    @BeforeEach void preparar() {
        doReturn(AHORA).when(clock).instant(); prepararFixtures();
        suscripcionId = servicio.crear(nit,plan.getId(),principal).id(); releer();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void limpiarSpies() { reset(clock,repositorio); }
    String base(String id) { return "/api/v1/almacenes/" + id + "/suscripciones"; }
    String bodyPlan() { return "{\"planId\":" + plan.getId() + "}"; }
    MockHttpServletRequestBuilder ruta(String op,String id) {
        return switch(op) {
            case "crear" -> post(base(id)).contentType(MediaType.APPLICATION_JSON).content(bodyPlan());
            case "vigente" -> get(base(id) + "/vigente");
            case "historial" -> get(base(id));
            case "cancelar" -> patch(base(id) + "/" + suscripcionId + "/renovacion")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"renovacionHabilitada\":false}");
            default -> throw new AssertionError(op);
        };
    }
    static Stream<Arguments> matriz() {
        return Stream.of("crear","vigente","historial","cancelar").flatMap(op ->
                Stream.of("ausente","manipulado","expirado","COMPRADOR","REPRESENTANTE_FUNDACION","ADMIN_STYLERADAR",
                        "ADMIN_ALMACEN","ajeno","sin-vinculo","BLOQUEADA","INACTIVA").map(tipo -> Arguments.of(op,tipo)));
    }
    @ParameterizedTest @MethodSource("matriz")
    void seguridadJWTRealRolEstadoYPropiedad(String op,String tipo) throws Exception {
        var r = ruta(op,tipo.equals("ajeno") ? ajeno : nit); int codigo = 401;
        if (!tipo.equals("ausente")) {
            String token = jwt.generate(tienda.getId());
            if (tipo.equals("manipulado")) token = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";
            else if (tipo.equals("expirado")) token = new JwtUtil(secret,Duration.ofHours(1),
                    Clock.fixed(AHORA.minusSeconds(7200),ZoneOffset.UTC)).generate(tienda.getId());
            else if (tipo.equals("BLOQUEADA") || tipo.equals("INACTIVA")) {
                em.find(UsuarioEntity.class,tienda.getId()).setEstadoCuenta(EstadoCuenta.valueOf(tipo)); releer();
            } else if (tipo.equals("sin-vinculo")) {
                var u = usuario(Rol.ADMIN_ALMACEN); releer(); token = jwt.generate(u.getId()); codigo = 404;
            } else if (tipo.equals("ajeno")) codigo = 404;
            else {
                em.find(UsuarioEntity.class,tienda.getId()).setRoles(Set.of(Rol.valueOf(tipo))); releer();
                codigo = tipo.equals("ADMIN_ALMACEN") ? (op.equals("crear") ? 409 : 200) : 403;
            }
            r.header("Authorization", "Bearer " + token);
        }
        mvc.perform(r).andExpect(status().is(codigo));
    }
    @Test void crear201ConsultarYCancelar200SinPago() throws Exception {
        // Otro NIT propio sin suscripción, aprovisionado por la operación administrativa real.
        mvc.perform(put("/api/v1/admin/almacenes/" + ajeno + "/administradores/" + tienda.getId())
                .header("Authorization", "Bearer " + jwt.generate(admin.getId()))).andExpect(status().isOk());
        var creado = json.readTree(mvc.perform(ruta("crear",ajeno).header("Authorization","Bearer " + jwt.generate(tienda.getId())))
                .andExpect(status().isCreated()).andExpect(header().string("Location",base(ajeno)+"/vigente"))
                .andExpect(jsonPath("$.estadoSuscripcion").value("ACTIVA")).andReturn().getResponse().getContentAsString());
        assertThat(creado.has("precio")).isFalse(); assertThat(creado.has("fechaPago")).isFalse();
        var id = creado.get("id").asLong();
        mvc.perform(get(base(ajeno)+"/vigente").header("Authorization","Bearer " + jwt.generate(tienda.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        var cancelado = json.readTree(mvc.perform(patch(base(ajeno)+"/"+id+"/renovacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"renovacionHabilitada\":false}").header("Authorization","Bearer " + jwt.generate(tienda.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.renovacionHabilitada").value(false))
                .andExpect(jsonPath("$.estadoSuscripcion").value("ACTIVA")).andReturn().getResponse().getContentAsString());
        assertThat(cancelado.get("finPeriodo")).isEqualTo(creado.get("finPeriodo"));
    }
    @ParameterizedTest @ValueSource(strings={"{}","null","{","{\"planId\":null}","{\"planId\":0}","{\"planId\":-1}","{\"planId\":\"abc\"}",
            "{\"planId\":1,\"usuarioId\":1}","{\"planId\":1,\"precio\":0}","{\"planId\":1,\"fechaInicio\":\"2024-01-01\"}",
            "{\"planId\":1,\"fechaFin\":\"2024-01-01\"}","{\"planId\":1,\"estado\":\"ACTIVA\"}","{\"planId\":1,\"fechaPago\":\"2024-01-01\"}",
            "{\"planId\":1,\"aprobadoPor\":1}","{\"planId\":1,\"renovacionHabilitada\":true}","{\"planId\":1,\"nit\":\"ajeno\"}"})
    void crearRechazaCamposNoPermitidos400(String body) throws Exception {
        mvc.perform(post(base(nit)).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"{}","null","{","{\"renovacionHabilitada\":null}","{\"renovacionHabilitada\":true}",
            "{\"renovacionHabilitada\":false,\"usuarioId\":1}","{\"renovacionHabilitada\":false,\"finPeriodo\":\"2030-01-01\"}",
            "{\"renovacionHabilitada\":false,\"planId\":1}","{\"renovacionHabilitada\":false,\"almacenNit\":\"ajeno\"}"})
    void cancelarSoloFalse400(String body) throws Exception {
        mvc.perform(patch(base(nit)+"/"+suscripcionId+"/renovacion").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"page=-1","size=0","size=101","page=abc","size=abc"})
    void paginacionInvalida400(String query) throws Exception {
        mvc.perform(get(base(nit)+"?"+query).header("Authorization","Bearer " + jwt.generate(tienda.getId())))
                .andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"PENDIENTE","RECHAZADO","NULL"})
    void verificacionDebeSerExplicita422(String estado) throws Exception {
        asociaciones.asociar(ajeno,tienda.getId(),administrador);
        em.find(AlmacenEntity.class,ajeno).setEstadoVerificacion(estado.equals("NULL")?null:EstadoVerificacionAlmacen.valueOf(estado)); releer();
        mvc.perform(ruta("crear",ajeno).header("Authorization","Bearer " + jwt.generate(tienda.getId())))
                .andExpect(status().isUnprocessableContent());
        assertThat(em.find(AlmacenEntity.class,ajeno).getEstadoVerificacion()).isEqualTo(estado.equals("NULL")?null:EstadoVerificacionAlmacen.valueOf(estado));
    }
    @Test void planInexistente404YPlanInactivo422() throws Exception {
        asociaciones.asociar(ajeno,tienda.getId(),administrador); em.find(PlanSuscripcionEntity.class,plan.getId()).setActivo(false); releer();
        mvc.perform(post(base(ajeno)).contentType(MediaType.APPLICATION_JSON).content("{\"planId\":9223372036854775807}")
                .header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isNotFound());
        mvc.perform(ruta("crear",ajeno).header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isUnprocessableContent());
    }
    @Test void vigenteVencidaYCancelacionVencida404() throws Exception {
        var fin = suscripciones.findById(suscripcionId).orElseThrow().getFinPeriodo(); doReturn(fin).when(clock).instant();
        mvc.perform(ruta("vigente",nit).header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isNotFound());
        mvc.perform(ruta("cancelar",nit).header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isNotFound());
        mvc.perform(ruta("historial",nit).header("Authorization","Bearer " + jwt.generate(tienda.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].estadoSuscripcion").value("FINALIZADA"));
    }
    @Test void sinSuscripcionNoCancelaNiConsulta404() throws Exception {
        asociaciones.asociar(ajeno,tienda.getId(),administrador);
        mvc.perform(ruta("vigente",ajeno).header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isNotFound());
        mvc.perform(ruta("cancelar",ajeno).header("Authorization","Bearer " + jwt.generate(tienda.getId()))).andExpect(status().isNotFound());
    }
    @Test void planesPublicosSoloActivosSinPrecios() throws Exception {
        em.find(PlanSuscripcionEntity.class,otroPlan.getId()).setActivo(false); releer();
        var ar = json.readTree(mvc.perform(get("/api/v1/planes-suscripcion")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ar).hasSize(1); assertThat(ar.get(0).get("unidadDuracion").asText()).isEqualTo("MESES_CALENDARIO");
        assertThat(ar.get(0).has("precio")).isFalse(); assertThat(ar.get(0).size()).isEqualTo(4);
    }
    @Test void planesVaciosSinSeed() throws Exception {
        em.find(PlanSuscripcionEntity.class,plan.getId()).setActivo(false);
        em.find(PlanSuscripcionEntity.class,otroPlan.getId()).setActivo(false); releer();
        mvc.perform(get("/api/v1/planes-suscripcion")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @ParameterizedTest @ValueSource(strings={"ausente","ADMIN_ALMACEN","COMPRADOR","REPRESENTANTE_FUNDACION","ADMIN_STYLERADAR","BLOQUEADA","INACTIVA"})
    void aprovisionamientoSoloAdministrativo(String tipo) throws Exception {
        var r = put("/api/v1/admin/almacenes/"+nit+"/administradores/"+otraTienda.getId()); int codigo=401;
        if (!tipo.equals("ausente")) {
            if (tipo.equals("BLOQUEADA") || tipo.equals("INACTIVA")) em.find(UsuarioEntity.class,admin.getId()).setEstadoCuenta(EstadoCuenta.valueOf(tipo));
            else { em.find(UsuarioEntity.class,admin.getId()).setRoles(Set.of(Rol.valueOf(tipo))); codigo=tipo.equals("ADMIN_STYLERADAR")?200:403; }
            releer(); r.header("Authorization","Bearer "+jwt.generate(admin.getId()));
        }
        mvc.perform(r).andExpect(status().is(codigo));
    }
    @Test void apropiacionPorJSONRechazadaNoCreaVinculo() throws Exception {
        var u=usuario(Rol.ADMIN_ALMACEN); releer();
        mvc.perform(post(base(nit)).contentType(MediaType.APPLICATION_JSON).content("{\"planId\":"+plan.getId()+",\"usuarioId\":"+tienda.getId()+"}")
                .header("Authorization","Bearer "+jwt.generate(u.getId()))).andExpect(status().isBadRequest());
        assertThat(vinculos.existsByUsuario_IdAndAlmacen_Nit(u.getId(),nit)).isFalse();
    }
    @Test void conflictoConcurrente409SinSQL() throws Exception {
        doThrow(new CannotAcquireLockException("SQL secreto")).when(repositorio).findByIdAndAlmacen_Nit(suscripcionId,nit);
        var b=mvc.perform(ruta("cancelar",nit).header("Authorization","Bearer "+jwt.generate(tienda.getId())))
                .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();
        assertThat(b).doesNotContain("SQL secreto","stackTrace");
    }
    @Test void falloTecnico500SinSQL() throws Exception {
        doThrow(new DataAccessResourceFailureException("SQL secreto")).when(repositorio).flush();
        var b=mvc.perform(ruta("cancelar",nit).header("Authorization","Bearer "+jwt.generate(tienda.getId())))
                .andExpect(status().isInternalServerError()).andReturn().getResponse().getContentAsString();
        assertThat(b).doesNotContain("SQL secreto","stackTrace");
    }
    @Test void swaggerDocumentaRutasBearerRolesYFronteraComercial() throws Exception {
        var doc=json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String b="/api/v1/almacenes/{nit}/suscripciones";
        for (String[] par:new String[][]{{b,"post"},{b,"get"},{b+"/vigente","get"},{b+"/{id}/renovacion","patch"}}) {
            var op=doc.get("paths").get(par[0]).get(par[1]);
            assertThat(op.get("security").get(0).has("bearerAuth")).isTrue();
            assertThat(op.get("description").asText()).contains("ROLE_ADMIN_ALMACEN","NIT");
            for(String c:List.of("400","401","403","404","409","422","500")) assertThat(op.get("responses").has(c)).isTrue();
        }
        assertThat(doc.get("paths").get(b).get("post").get("responses").has("201")).isTrue();
        assertThat(doc.get("paths").get("/api/v1/planes-suscripcion").get("get").get("security")).isEmpty();
        assertThat(doc.get("paths").get(b).get("post").get("description").asText()).contains("No registra pagos");
        assertThat(doc.get("paths").get("/api/v1/admin/almacenes/{nit}/administradores/{usuarioId}")
                .get("put").get("description").asText()).contains("ROLE_ADMIN_STYLERADAR");
    }
}
