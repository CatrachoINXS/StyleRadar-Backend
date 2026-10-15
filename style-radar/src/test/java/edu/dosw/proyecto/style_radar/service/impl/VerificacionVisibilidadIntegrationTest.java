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
import edu.dosw.proyecto.style_radar.model.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.security.*;
import edu.dosw.proyecto.style_radar.service.*;
import edu.dosw.proyecto.style_radar.support.AlertasFixtures;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test") @Transactional
class VerificacionVisibilidadIntegrationTest extends AlertasFixtures {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @Autowired ObjectMapper json;
    @Autowired VerificacionAlmacenService verificacion;
    @Autowired AlmacenRepository almacenes;
    @Autowired IPlaylistService playlists;
    MockMvc mvc;

    @BeforeEach void prepararHttp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    UsuarioPrincipal admin() {
        var u = usuarios.findById(usuario.getId()).orElseThrow(); u.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        return new UsuarioPrincipal(u, "fixture");
    }

    DecisionVerificacionRequestDTO decision(DecisionVerificacionAlmacen d) {
        var r = new DecisionVerificacionRequestDTO(); r.setDecision(d); r.setMotivo("Datos revisados"); return r;
    }

    AlmacenEntity tienda(String nit, EstadoVerificacionAlmacen estado) {
        var a = new AlmacenEntity(nit, "Radar", "Datos", "123", "contacto@example.co", 4.0, -74.0, 4.5, new ArrayList<>());
        a.setEstadoVerificacion(estado); em.persist(a); return a;
    }

    Long item(AlmacenEntity a, EstadoModeracion moderacion, int unidades) {
        var p = new PrendaEntity(null, "Camisa especial", "Algodon", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL); em.persist(p);
        var i = new ItemCatalogoEntity(null, 100.0, unidades > 0 ? EstadoItem.DISPONIBLE : EstadoItem.AGOTADA,
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), a, p, new ArrayList<>(), new ArrayList<>());
        i.setEstadoModeracion(moderacion); em.persist(i);
        var v = new InventarioTallaEntity(null, Talla.M, unidades, i); i.getInventario().add(v); em.persist(v); return i.getId();
    }

    String token() {
        credenciales.crearCredencial(usuario.getId(), "verification fixture password"); releer();
        return jwt.generate(usuario.getId());
    }

    @ParameterizedTest @ValueSource(strings = {"buscar", "similar", "distancia", "reputacion", "feed", "recomendaciones"})
    void consultasGlobalesExcluyenTiendasOcultasAntesDeCountYPagina(String camino) throws Exception {
        // Arrange: ocultos primero; tres elegibles, más elementos que el tamaño de página.
        item(tienda("pendiente", EstadoVerificacionAlmacen.PENDIENTE), EstadoModeracion.APROBADA, 5);
        item(tienda("rechazado", EstadoVerificacionAlmacen.RECHAZADO), EstadoModeracion.APROBADA, 5);
        var a = tienda("aprobado", EstadoVerificacionAlmacen.APROBADO);
        Long uno = item(a, EstadoModeracion.APROBADA, 5), dos = item(a, EstadoModeracion.NO_REQUERIDA, 5);
        Long legado = item(almacen, null, 5);
        item(a, EstadoModeracion.RECHAZADA, 5); item(a, EstadoModeracion.APROBADA, 0);
        String t = token();
        String ruta = Set.of("feed", "recomendaciones").contains(camino) ? "/api/v1/usuarios/me/" + camino : "/api/v1/catalogo/buscar";
        var request = get(ruta).header("Authorization", "Bearer " + t).param("size", "2");
        if (camino.equals("similar")) request.param("q", "camisa inexistente");
        if (camino.equals("distancia")) request.param("orden", "DISTANCIA").param("latitudUsuario", "4").param("longitudUsuario", "-74");
        if (camino.equals("reputacion")) request.param("orden", "REPUTACION");
        // Act
        var p0 = json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        request.param("page", "1");
        var p1 = json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        // Assert
        assertThat(p0.get("totalElements").asLong()).isEqualTo(3); assertThat(p1.get("totalElements").asLong()).isEqualTo(3);
        assertThat(p0.get("content").size()).isEqualTo(2); assertThat(p1.get("content").size()).isEqualTo(1);
        var ids = new ArrayList<Long>(); p0.get("content").forEach(n -> ids.add(n.get("itemId").asLong()));
        p1.get("content").forEach(n -> ids.add(n.get("itemId").asLong()));
        assertThat(ids).containsExactlyInAnyOrder(uno, dos, legado);
    }

    @ParameterizedTest @NullSource @EnumSource(EstadoVerificacionAlmacen.class)
    void mapaCategoriaCatalogoResumenNovedadesYDistanciaRespetanEstado(EstadoVerificacionAlmacen estado) throws Exception {
        // Arrange
        var a = em.find(AlmacenEntity.class, almacen.getNit()); a.setEstadoVerificacion(estado);
        a.setLatitud(4.0); a.setLongitud(-74.0); a.getCategorias().add(CategoriaAlmacen.values()[0]);
        Long id = item(a, EstadoModeracion.APROBADA, 5);
        Long otro = item(a, EstadoModeracion.NO_REQUERIDA, 5), tercero = item(a, null, 5);
        item(a, EstadoModeracion.PENDIENTE, 5); item(a, EstadoModeracion.RECHAZADA, 5); item(a, EstadoModeracion.APROBADA, 0);
        releer();
        boolean visible = VisibilidadAlmacen.visible(estado);
        // Act / Assert
        for (boolean categoria : new boolean[]{false, true}) {
            var request = get("/api/v1/almacenes").param("size", "1");
            if (categoria) request.param("categoria", CategoriaAlmacen.values()[0].name());
            var mapa = json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(mapa.get("totalElements").asLong()).isEqualTo(visible ? 1 : 0);
        }
        for (String camino : new String[]{"catalogo", "catalogo/resumen", "catalogo/novedades", "distancia"}) {
            var request = get("/api/v1/almacenes/alertas/" + camino);
            if (camino.equals("distancia")) request.param("latitudUsuario", "4").param("longitudUsuario", "-74");
            var result = mvc.perform(request).andExpect(visible ? status().isOk() : status().isNotFound());
            if (visible && !camino.equals("distancia")) assertThat(result.andReturn().getResponse().getContentAsString()).contains("\"itemId\":" + id);
        }
        if (visible) {
            var ids = new ArrayList<Long>();
            for (int page = 0; page < 2; page++) {
                var b = json.readTree(mvc.perform(get("/api/v1/almacenes/alertas/catalogo/novedades").param("size", "2")
                        .param("page", String.valueOf(page))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
                assertThat(b.get("totalElements").asLong()).isEqualTo(3);
                assertThat(b.get("content").size()).isEqualTo(page == 0 ? 2 : 1);
                b.get("content").forEach(n -> ids.add(n.get("itemId").asLong()));
            }
            assertThat(ids).containsExactlyInAnyOrder(id, otro, tercero);
        }
        assertThat(verificacion.consultarUno("alertas", admin()).estadoVerificacion()).isEqualTo(estado);
    }

    @Test void mapaPaginaAntesDeCategoriasSinDuplicados() throws Exception {
        // Arrange
        for (int n = 0; n < 5; n++) {
            var a = tienda("tienda-" + n, n < 2 ? EstadoVerificacionAlmacen.RECHAZADO : EstadoVerificacionAlmacen.APROBADO);
            a.getCategorias().addAll(Arrays.asList(CategoriaAlmacen.values()));
        }
        releer(); var ids = new ArrayList<String>();
        // Act
        for (int page = 0; page < 2; page++) {
            var b = json.readTree(mvc.perform(get("/api/v1/almacenes").param("categoria", CategoriaAlmacen.values()[0].name())
                    .param("size", "2").param("page", String.valueOf(page))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            // Assert
            assertThat(b.get("totalElements").asLong()).isEqualTo(3);
            assertThat(b.get("content").size()).isEqualTo(page == 0 ? 2 : 1); b.get("content").forEach(n -> ids.add(n.get("nit").asText()));
        }
        assertThat(ids).containsExactly("tienda-2", "tienda-3", "tienda-4");
    }

    @ParameterizedTest @CsvSource({"APROBADO,APROBADA,5,true", "APROBADO,RECHAZADA,5,false", "RECHAZADO,APROBADA,5,false",
            "PENDIENTE,NO_REQUERIDA,5,false", ",,5,true", "APROBADO,APROBADA,0,false"})
    void matrizRF61RF62YStock(EstadoVerificacionAlmacen estado, EstadoModeracion moderacion, int stock, boolean visible) throws Exception {
        // Arrange
        var a = em.find(AlmacenEntity.class, "alertas"); a.setEstadoVerificacion(estado); Long id = item(a, moderacion, stock); releer();
        // Act
        var result = json.readTree(mvc.perform(get("/api/v1/catalogo/buscar")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        // Assert
        assertThat(result.get("totalElements").asLong()).isEqualTo(visible ? 1 : 0);
        assertThat(items.findById(id).orElseThrow().getEstadoModeracion()).isEqualTo(moderacion);
    }

    @ParameterizedTest @NullSource @EnumSource(EstadoVerificacionAlmacen.class)
    void prendasYPlaylistsPreservanDefinicionesYAsociaciones(EstadoVerificacionAlmacen estado) throws Exception {
        // Arrange
        var a = em.find(AlmacenEntity.class, "alertas"); a.setEstadoVerificacion(estado);
        Long id = item(a, EstadoModeracion.APROBADA, 5); Long prenda = items.findById(id).orElseThrow().getPrenda().getId();
        var definicion = new PrendaEntity(null, "Sin publicación", "Datos", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL); em.persist(definicion);
        var lista = playlists.crear(usuario.getId(), "Pública", "Datos", VisibilidadPlaylist.PUBLICA, Estilo.FORMAL);
        playlists.agregarPrenda(usuario.getId(), lista.getId(), prenda); playlists.agregarPrenda(usuario.getId(), lista.getId(), definicion.getId()); releer();
        // Act
        var body = json.readTree(mvc.perform(get("/api/v1/prendas")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var ids = new ArrayList<Long>(); body.forEach(n -> ids.add(n.get("id").asLong()));
        // Assert
        assertThat(ids).contains(definicion.getId());
        if (VisibilidadAlmacen.visible(estado)) assertThat(ids).contains(prenda); else assertThat(ids).doesNotContain(prenda);
        for (var p : List.of(playlists.obtenerPublicas().getFirst(), playlists.obtenerPorId(lista.getId(), usuario.getId()),
                playlists.obtenerPorUsuario(usuario.getId()).getFirst()))
            assertThat(p.getPrendas()).hasSize(VisibilidadAlmacen.visible(estado) ? 2 : 1);
        assertThat(em.find(PlaylistEntity.class, lista.getId()).getPrendas()).hasSize(2);
        // Otra publicación elegible mantiene la prenda compartida visible.
        var otro = tienda("visible", EstadoVerificacionAlmacen.APROBADO);
        em.persist(new ItemCatalogoEntity(null, 100.0, EstadoItem.AGOTADA, Instant.now(), otro, em.find(PrendaEntity.class, prenda), new ArrayList<>(), new ArrayList<>())); releer();
        assertThat(playlists.obtenerPublicas().getFirst().getPrendas()).hasSize(2);
    }

    @ParameterizedTest @NullSource @EnumSource(EstadoVerificacionAlmacen.class)
    void alertasAmbosTiposRespetanVisibilidadYCiclos(EstadoVerificacionAlmacen estado) {
        // Arrange
        em.find(AlmacenEntity.class, "alertas").setEstadoVerificacion(estado);
        alertaBusqueda(b -> b.setMarca("Radar")); Long id = publicar(); service.crear(usuario.getId(), null, id);
        // Act
        stock(id, 5); stock(id, 0); stock(id, 8); releer();
        // Assert
        if (VisibilidadAlmacen.visible(estado)) {
            assertThat(eventos()).extracting(Notificacion::tipoEvento).containsExactlyInAnyOrder(
                    TipoEventoNotificacion.NUEVA_COINCIDENCIA, TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA,
                    TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA);
        } else assertThat(eventos()).isEmpty();
        assertThat(service.consultar(usuario.getId())).hasSize(2);
    }

    @ParameterizedTest @EnumSource(value = EstadoVerificacionAlmacen.class, names = {"PENDIENTE", "RECHAZADO"})
    void coincidenciaTardiaDeTallaNoNotifica(EstadoVerificacionAlmacen estado) {
        // Arrange
        em.find(AlmacenEntity.class, "alertas").setEstadoVerificacion(estado); alertaBusqueda(b -> b.setTalla(Talla.L)); Long id = publicar();
        // Act
        stock(id, 5); stock(id, Talla.L, 3); releer();
        // Assert
        assertThat(eventos()).isEmpty();
    }

    @Test void rechazoConservaHistorialYAprobacionNoNotificaRetroactivamente() {
        // Arrange
        alertaBusqueda(b -> b.setMarca("Radar")); Long id = publicar(); stock(id, 5); var p = admin();
        verificacion.solicitarRevision("alertas", p);
        // Act
        verificacion.decidir("alertas", p, decision(DecisionVerificacionAlmacen.RECHAZADO)); stock(id, 0); stock(id, 8);
        var anterior = verificacion.consultarUno("alertas", p); var pendiente = verificacion.solicitarRevision("alertas", p);
        assertThat(pendiente.fechaDecisionVerificacion()).isEqualTo(anterior.fechaDecisionVerificacion());
        verificacion.decidir("alertas", p, decision(DecisionVerificacionAlmacen.APROBADO)); releer();
        // Assert
        assertThat(eventos()).hasSize(1); assertThat(service.consultar(usuario.getId())).hasSize(1);
    }

    @Test void aprobarPendienteConStockNoGeneraEventosRetroactivos() {
        // Arrange
        em.find(AlmacenEntity.class, "alertas").setEstadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE);
        alertaBusqueda(b -> b.setMarca("Radar")); Long id = publicar(); service.crear(usuario.getId(), null, id); stock(id, 5);
        // Act
        verificacion.decidir("alertas", admin(), decision(DecisionVerificacionAlmacen.APROBADO)); releer();
        // Assert
        assertThat(eventos()).isEmpty(); assertThat(service.consultar(usuario.getId())).hasSize(2);
        assertThat(catalogo.obtenerCatalogo("alertas")).extracting(ItemCatalogo::getId).containsExactly(id);
    }

    @Test void escrituraInternaEnAlmacenRechazadoConservaContenidoYEstado() {
        // Arrange
        em.find(AlmacenEntity.class, "alertas").setEstadoVerificacion(EstadoVerificacionAlmacen.RECHAZADO);
        Long id = publicar();
        // Act
        catalogo.actualizar("alertas", id, new Prenda(null, "Editada", "Datos", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL), 125.0);
        stock(id, 5); releer();
        // Assert
        var i = items.findById(id).orElseThrow(); assertThat(i.getPrecio()).isEqualTo(125.0);
        assertThat(i.getAlmacen().getEstadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.RECHAZADO);
        assertThat(i.getEstadoModeracion()).isEqualTo(EstadoModeracion.NO_REQUERIDA);
        assertThat(verificacion.consultarUno("alertas", admin()).estadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.RECHAZADO);
        assertThatThrownBy(() -> catalogo.obtenerCatalogo("alertas")).isInstanceOf(edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException.class);
    }

    @ParameterizedTest @EnumSource(DecisionVerificacionAlmacen.class)
    void decisionConservaCamposCatalogoModeracionYEnumString(DecisionVerificacionAlmacen d) {
        // Arrange
        var a = em.find(AlmacenEntity.class, "alertas"); a.setLatitud(4.0); a.setLongitud(-74.0); a.setReputacion(4.5);
        a.getCategorias().add(CategoriaAlmacen.values()[0]); Long id = item(a, EstadoModeracion.RECHAZADA, 5);
        var i = items.findById(id).orElseThrow(); var fecha = i.getFechaPublicacion(); var estado = i.getEstado();
        var imagen = new ImagenCatalogoEntity(null, "image/png", new byte[]{1, 2, 3}, i); em.persist(imagen); i.getImagenes().add(imagen);
        var p = admin(); verificacion.solicitarRevision("alertas", p);
        // Act
        var r = verificacion.decidir("alertas", p, decision(d)); releer();
        // Assert
        assertThat(r.nit()).isEqualTo("alertas"); assertThat(r.nombreComercial()).isEqualTo("Radar"); assertThat(r.descripcion()).isEqualTo("Catalogo");
        assertThat(r.telefono()).isEqualTo("123"); assertThat(r.correoContacto()).isEqualTo("store@example.co");
        assertThat(r.latitud()).isEqualTo(4.0); assertThat(r.longitud()).isEqualTo(-74.0); assertThat(r.reputacion()).isEqualTo(4.5);
        assertThat(r.categorias()).containsExactly(CategoriaAlmacen.values()[0]);
        var persistido = items.findById(id).orElseThrow(); assertThat(persistido.getEstadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADA);
        assertThat(persistido.getPrecio()).isEqualTo(100.0); assertThat(persistido.getEstado()).isEqualTo(estado);
        assertThat(persistido.getFechaPublicacion()).isEqualTo(fecha); assertThat(persistido.getInventario()).singleElement().extracting(InventarioTallaEntity::getUnidades).isEqualTo(5);
        assertThat(persistido.getImagenes()).singleElement().satisfies(x -> assertThat(x.getDatos()).containsExactly(1, 2, 3));
        assertThat(em.createNativeQuery("select estado_verificacion from almacenes where nit = 'alertas'", String.class).getSingleResult()).isEqualTo(d.name());
    }

    @Test void administracionPaginaTodosSinGeolocalizacionSinNMasUnoYFiltraEstadosReales() {
        // Arrange
        for (int n = 0; n < 25; n++) {
            var a = tienda(String.format("tienda-%02d", n), n < 3 ? EstadoVerificacionAlmacen.values()[n] : null);
            a.setLatitud(null); a.setLongitud(null); a.getCategorias().addAll(Arrays.asList(CategoriaAlmacen.values()));
        }
        releer(); var p = admin(); em.flush(); em.clear();
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
        // Act
        var page = verificacion.consultar(null, 0, 20, p);
        // Assert
        assertThat(page.getTotalElements()).isEqualTo(26); assertThat(page.getContent()).hasSize(20);
        assertThat(page.getContent()).extracting(x -> x.nit()).startsWith("alertas", "tienda-00", "tienda-01");
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
        assertThat(verificacion.consultar(null, 1, 20, p).getContent()).hasSize(6);
        for (var estado : EstadoVerificacionAlmacen.values())
            assertThat(verificacion.consultar(estado, 0, 20, p).getContent()).singleElement().extracting(x -> x.estadoVerificacion()).isEqualTo(estado);
        assertThat(verificacion.consultar(null, Integer.MAX_VALUE, 100, p).getTotalElements()).isEqualTo(26);
        assertThat(verificacion.consultar(EstadoVerificacionAlmacen.APROBADO, 2, 1, p).getContent()).isEmpty();
        em.clear(); var legado = almacenes.findById("alertas").orElseThrow();
        assertThat(Hibernate.isInitialized(legado.getCategorias())).isFalse(); assertThat(legado.getEstadoVerificacion()).isNull();
        var r = verificacion.consultarUno("alertas", p); assertThat(r.legadoSinVerificacion()).isTrue();
        assertThat(r.administradorDecisionId()).isNull(); assertThat(r.fechaDecisionVerificacion()).isNull();
    }

    @Test void visibleSinCoordenadasConserva422EInexistente404() throws Exception {
        // Arrange
        releer();
        // Act / Assert
        mvc.perform(get("/api/v1/almacenes/alertas/distancia").param("latitudUsuario", "4").param("longitudUsuario", "-74"))
                .andExpect(status().isUnprocessableContent());
        for (String camino : new String[]{"catalogo", "catalogo/resumen", "catalogo/novedades", "distancia"})
            mvc.perform(get("/api/v1/almacenes/missing/" + camino).param("latitudUsuario", "4").param("longitudUsuario", "-74"))
                    .andExpect(status().isNotFound());
    }
}
