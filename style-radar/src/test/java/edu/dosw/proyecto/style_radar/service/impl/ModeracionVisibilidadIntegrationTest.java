package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionModeracionRequestDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.security.*;
import edu.dosw.proyecto.style_radar.service.*;
import edu.dosw.proyecto.style_radar.support.AlertasFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test") @Transactional
class ModeracionVisibilidadIntegrationTest extends AlertasFixtures {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    @Autowired ModeracionCatalogoService moderacion;
    @Autowired IPlaylistService playlists;
    MockMvc mvc;

    @BeforeEach void prepararHttp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    UsuarioPrincipal admin() {
        var u = usuarios.findById(usuario.getId()).orElseThrow();
        u.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        return new UsuarioPrincipal(u, "fixture");
    }

    DecisionModeracionRequestDTO decision(DecisionModeracion d) {
        var r = new DecisionModeracionRequestDTO(); r.setDecision(d); r.setMotivo("Revisada"); return r;
    }

    Long item(EstadoModeracion estado, int stock) {
        Long id = publicar();
        items.findById(id).orElseThrow().setEstadoModeracion(estado);
        stock(id, stock);
        return id;
    }

    String token() {
        credenciales.crearCredencial(usuario.getId(), "visibility fixture password"); releer();
        return jwt.generate(usuario.getId());
    }

    @ParameterizedTest @ValueSource(strings = {"catalogo", "buscar", "similar", "resumen", "novedades", "feed", "recomendaciones"})
    void todosLosCaminosExcluyenPendientesYRechazadasConCountYPaginaReal(String camino) throws Exception {
        item(EstadoModeracion.PENDIENTE, 5); item(EstadoModeracion.RECHAZADA, 5);
        Long aprobado = item(EstadoModeracion.APROBADA, 5);
        Long ordinario = item(EstadoModeracion.NO_REQUERIDA, 5);
        Long historico = item(null, 5);
        Long agotado = item(EstadoModeracion.APROBADA, 0);
        String t = token();
        String ruta = switch (camino) {
            case "catalogo", "resumen", "novedades" -> "/api/v1/almacenes/alertas/catalogo"
                    + (camino.equals("catalogo") ? "" : "/" + camino);
            case "buscar", "similar" -> "/api/v1/catalogo/buscar";
            default -> "/api/v1/usuarios/me/" + camino;
        };
        var request = get(ruta).header("Authorization", "Bearer " + t);
        if (camino.equals("similar")) request.param("q", "camisa inexistente");
        var body = json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var content = body.isArray() ? body : body.get("content");
        var ids = new ArrayList<Long>(); content.forEach(n -> ids.add(n.get("itemId").asLong()));
        assertThat(ids).containsExactlyInAnyOrder(aprobado, ordinario, historico).doesNotContain(agotado);
        if (!body.isArray()) {
            assertThat(body.get("totalElements").asLong()).isEqualTo(3);
            var pagina = get(ruta).header("Authorization", "Bearer " + t).param("size", "2");
            if (camino.equals("similar")) pagina.param("q", "camisa inexistente");
            var p0 = json.readTree(mvc.perform(pagina).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(p0.get("content").size()).isEqualTo(2);
            assertThat(p0.get("totalElements").asLong()).isEqualTo(3);
            pagina.param("page", "1");
            var p1 = json.readTree(mvc.perform(pagina).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(p1.get("content").size()).isEqualTo(1);
            var orden = new ArrayList<Long>(); p0.get("content").forEach(n -> orden.add(n.get("itemId").asLong()));
            p1.get("content").forEach(n -> orden.add(n.get("itemId").asLong()));
            assertThat(orden).containsExactlyElementsOf(ids);
        }
    }

    @ParameterizedTest @NullSource @EnumSource(EstadoModeracion.class)
    void prendasMantieneDefinicionesYRespetaPublicaciones(EstadoModeracion estado) throws Exception {
        Long id = item(estado, 0);
        Long prendaId = items.findById(id).orElseThrow().getPrenda().getId();
        var definicion = new PrendaEntity(null, "Definición", "Histórica", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL);
        em.persist(definicion); releer();
        var body = json.readTree(mvc.perform(get("/api/v1/prendas")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        var ids = new ArrayList<Long>(); body.forEach(n -> ids.add(n.get("id").asLong()));
        assertThat(ids).contains(definicion.getId());
        if (VisibilidadModeracion.visible(estado)) assertThat(ids).contains(prendaId);
        else assertThat(ids).doesNotContain(prendaId);
    }

    @Test void prendaCompartidaEsVisibleSiTieneAlMenosUnaPublicacionVisible() throws Exception {
        Long oculto = item(EstadoModeracion.RECHAZADA, 3);
        var i = items.findById(oculto).orElseThrow();
        Long prenda = i.getPrenda().getId();
        em.persist(new ItemCatalogoEntity(null, 100.0, EstadoItem.AGOTADA, Instant.now(), almacen,
                i.getPrenda(), new ArrayList<>(), new ArrayList<>())); releer();
        String body = mvc.perform(get("/api/v1/prendas")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get(0).get("id").asLong()).isEqualTo(prenda);
    }

    @ParameterizedTest @EnumSource(value = EstadoModeracion.class, names = {"PENDIENTE", "RECHAZADA"})
    void playlistsNoFiltranContenidoOcultoNiBorranAsociaciones(EstadoModeracion estado) {
        Long oculto = item(estado, 5); Long visible = item(EstadoModeracion.APROBADA, 5);
        var playlist = playlists.crear(usuario.getId(), "Pública", "Fixture", VisibilidadPlaylist.PUBLICA, Estilo.FORMAL);
        Long prendaOculta = items.findById(oculto).orElseThrow().getPrenda().getId();
        Long prendaVisible = items.findById(visible).orElseThrow().getPrenda().getId();
        playlists.agregarPrenda(usuario.getId(), playlist.getId(), prendaOculta);
        playlists.agregarPrenda(usuario.getId(), playlist.getId(), prendaVisible); releer();
        assertThat(playlists.obtenerPublicas().getFirst().getPrendas()).extracting(Prenda::getId).containsExactly(prendaVisible);
        assertThat(playlists.obtenerPorId(playlist.getId(), usuario.getId()).getPrendas()).extracting(Prenda::getId).containsExactly(prendaVisible);
        assertThat(playlists.obtenerPorUsuario(usuario.getId()).getFirst().getPrendas()).extracting(Prenda::getId).containsExactly(prendaVisible);
        assertThat(em.find(PlaylistEntity.class, playlist.getId()).getPrendas()).hasSize(2);
        playlists.retirarPrenda(usuario.getId(), playlist.getId(), prendaOculta);
        assertThat(em.find(PlaylistEntity.class, playlist.getId()).getPrendas()).hasSize(1);
    }

    @ParameterizedTest @NullSource @EnumSource(EstadoModeracion.class)
    void nuevaPublicacionConStockGeneraEventosSoloSiVisible(EstadoModeracion estado) {
        alertaBusqueda(b -> { b.setMarca("Radar"); b.setTalla(Talla.M); });
        Long id = item(estado, 5); releer();
        assertThat(eventos()).hasSize(VisibilidadModeracion.visible(estado) ? 1 : 0);
        stock(id, 0); stock(id, 8); stock(id, Talla.L, 5); releer();
        assertThat(eventos()).hasSize(VisibilidadModeracion.visible(estado) ? 1 : 0);
    }

    @Test void rechazarConservaEventosHistoricosYNoEmiteAlAprobar() {
        var a = alertaBusqueda(b -> b.setMarca("Radar"));
        Long id = item(EstadoModeracion.NO_REQUERIDA, 5); releer();
        assertThat(eventos()).hasSize(1);
        var p = admin(); moderacion.solicitarRevision(id, p); moderacion.decidir(id, p, decision(DecisionModeracion.RECHAZADA));
        stock(id, 0); stock(id, 8); releer();
        assertThat(eventos()).singleElement().extracting(Notificacion::alertaId).isEqualTo(a.id());
        moderacion.solicitarRevision(id, p); moderacion.decidir(id, p, decision(DecisionModeracion.APROBADA)); releer();
        assertThat(eventos()).hasSize(1);
    }

    @Test void aprobarConStockExistenteNoGeneraCoincidenciaRetroactiva() {
        alertaBusqueda(b -> b.setMarca("Radar")); Long id = item(EstadoModeracion.PENDIENTE, 5);
        moderacion.decidir(id, admin(), decision(DecisionModeracion.APROBADA)); releer();
        assertThat(eventos()).isEmpty();
    }

    @Test void seguimientoDeItemOcultoNoGeneraReposicionPublica() {
        Long id = item(EstadoModeracion.NO_REQUERIDA, 0); service.crear(usuario.getId(), null, id);
        moderacion.solicitarRevision(id, admin()); stock(id, 5); releer();
        assertThat(eventos()).isEmpty();
    }

    @ParameterizedTest @EnumSource(DecisionModeracion.class)
    void decisionesConservanPrecioInventarioImagenesEstadoYFecha(DecisionModeracion d) {
        Long id = item(EstadoModeracion.NO_REQUERIDA, 5); releer();
        var i = items.findById(id).orElseThrow();
        var fecha = i.getFechaPublicacion(); var estado = i.getEstado();
        var imagen = new ImagenCatalogoEntity(null, "image/png", new byte[]{1, 2, 3}, i); em.persist(imagen);
        i.getImagenes().add(imagen);
        var p = admin(); moderacion.solicitarRevision(id, p); moderacion.decidir(id, p, decision(d)); releer();
        var persistido = items.findById(id).orElseThrow();
        assertThat(persistido.getPrecio()).isEqualTo(100.0);
        assertThat(persistido.getFechaPublicacion()).isEqualTo(fecha);
        assertThat(persistido.getEstado()).isEqualTo(estado);
        assertThat(persistido.getInventario()).extracting(InventarioTallaEntity::getUnidades).containsExactlyInAnyOrder(5, 0);
        assertThat(persistido.getImagenes()).singleElement().satisfies(x -> assertThat(x.getDatos()).containsExactly(1, 2, 3));
        assertThat(persistido.getAdministradorDecisionId()).isEqualTo(usuario.getId());
        assertThat(persistido.getMotivoModeracion()).isEqualTo("Revisada");
        assertThat(em.createNativeQuery("select estado_moderacion from items_catalogo where id = :id", String.class)
                .setParameter("id", id).getSingleResult()).isEqualTo(d.name());
    }

    @Test void nuevaRevisionConservaUltimaDecisionHastaReemplazarla() {
        Long id = item(EstadoModeracion.PENDIENTE, 0); var p = admin();
        var anterior = moderacion.decidir(id, p, decision(DecisionModeracion.RECHAZADA));
        var pendiente = moderacion.solicitarRevision(id, p);
        assertThat(pendiente.fechaDecisionModeracion()).isEqualTo(anterior.fechaDecisionModeracion());
        assertThat(pendiente.motivoModeracion()).isEqualTo(anterior.motivoModeracion());
        assertThat(moderacion.decidir(id, p, decision(DecisionModeracion.APROBADA)).estadoModeracion()).isEqualTo(EstadoModeracion.APROBADA);
    }

    @Test void escriturasSiguenLocalizandoRechazadasSinRevisarAutomaticamente() {
        Long id = item(EstadoModeracion.PENDIENTE, 0); var p = admin();
        moderacion.decidir(id, p, decision(DecisionModeracion.RECHAZADA));
        catalogo.actualizar(almacen.getNit(), id, new Prenda(null, "Editada", "Datos", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL), 125.0);
        stock(id, 5); releer();
        assertThat(items.findById(id).orElseThrow().getEstadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADA);
        assertThat(catalogo.obtenerCatalogo(almacen.getNit())).isEmpty();
        catalogo.retirar(almacen.getNit(), id); releer();
        assertThat(items.existsById(id)).isFalse();
    }

    @Test void consultaAdminOrdenaPaginaSinNMasUnoYCubreNullHistorico() {
        var ids = new ArrayList<Long>();
        for (int n = 0; n < 25; n++) ids.add(item(n == 0 ? null : EstadoModeracion.NO_REQUERIDA, 0));
        item(EstadoModeracion.PENDIENTE, 0); releer();
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
        var page = moderacion.consultar(EstadoModeracion.NO_REQUERIDA, 0, 20);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getContent()).extracting(x -> x.itemId()).containsExactlyElementsOf(ids.subList(0, 20));
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
        assertThat(moderacion.consultar(EstadoModeracion.NO_REQUERIDA, 1, 20).getContent()).hasSize(5);
        assertThat(moderacion.consultar(EstadoModeracion.NO_REQUERIDA, Integer.MAX_VALUE, 100).getTotalElements()).isEqualTo(25);
        em.clear(); var entity = items.findById(ids.getFirst()).orElseThrow();
        assertThat(Hibernate.isInitialized(entity.getAlmacen())).isFalse();
        assertThat(Hibernate.isInitialized(entity.getPrenda())).isFalse();
        assertThat(Hibernate.isInitialized(entity.getImagenes())).isFalse();
        assertThat(entity.getEstadoModeracion()).isNull();
        assertThat(em.createNativeQuery("select count(*) from information_schema.indexes where index_name = 'idx_item_moderacion_fecha_id'", Long.class)
                .getSingleResult()).isEqualTo(1L);
    }
}
