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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.mapper.AlmacenEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.IAlmacenService;
import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"
})
@ActiveProfiles("test")
@Import(AlmacenDescubrimientoIntegrationTest.FixedTime.class)
@Transactional
class AlmacenDescubrimientoIntegrationTest {
    private static final Instant AHORA = Instant.parse("2026-10-09T12:00:00Z");
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired AlmacenRepository almacenes;
    @Autowired ItemCatalogoRepository items;
    @Autowired IAlmacenService service;
    @Autowired AlmacenEntityMapper almacenMapper;
    private MockMvc mvc;
    private AlmacenEntity a;
    private AlmacenEntity b;

    @TestConfiguration
    static class FixedTime {
        @Bean @Primary
        Clock discoveryClock() { return Clock.fixed(AHORA, ZoneOffset.UTC); }
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        a = almacen("A", "Radar", 0.0, 0.0, CategoriaAlmacen.CASUAL, CategoriaAlmacen.DEPORTIVA);
        b = almacen("B", "Radar", 4.0, -74.0, CategoriaAlmacen.VINTAGE, CategoriaAlmacen.ACCESORIOS);
    }

    @Test
    void marcadoresExcluyenHistoricosSinCoordenadasYDatosPrivados() throws Exception {
        // Arrange
        almacen("historico", "Histórico", null, null);
        almacen("parcial", "Parcial", 4.0, null);
        // Act
        var result = mvc.perform(get("/api/v1/almacenes"));
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].nit").value("A"))
                .andExpect(jsonPath("$.content[0].nombreComercial").value("Radar"))
                .andExpect(jsonPath("$.content[0].descripcion").value("Moda"))
                .andExpect(jsonPath("$.content[0].latitud").value(0.0))
                .andExpect(jsonPath("$.content[0].longitud").value(0.0))
                .andExpect(jsonPath("$.content[0].reputacion").value(4.5))
                .andExpect(jsonPath("$.content[0].categorias.length()").value(2))
                .andExpect(jsonPath("$.content[0].telefono").doesNotExist())
                .andExpect(jsonPath("$.content[0].correoContacto").doesNotExist())
                .andExpect(jsonPath("$.content[0].itemsCatalogo").doesNotExist())
                .andExpect(jsonPath("$.content[0].verificado").doesNotExist());
        assertThat(almacenes.existsById("historico")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"91,0", "-91,0", "0,181", "0,-181", "NaN,0", "Infinity,0", "0,-Infinity"})
    void coordenadasHistoricasInvalidasNoCreanMarcadores(double lat, double lon) {
        // Arrange
        almacen("invalido", "Inválido", lat, lon);
        em.flush();
        em.clear();
        // Act
        var page = service.consultar(null, 0, 20);
        // Assert
        assertThat(page.getContent()).extracting(Almacen::getNit).containsExactly("A", "B");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(almacenes.existsById("invalido")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"CASUAL,A", "DEPORTIVA,A", "VINTAGE,B", "ACCESORIOS,B", "FORMAL,C"})
    void filtraCategoriasPersistidasSinDuplicados(CategoriaAlmacen categoria, String nit) throws Exception {
        // Arrange
        almacen("C", "Formal", 4.0, -74.0, CategoriaAlmacen.FORMAL);
        em.flush();
        em.clear();
        // Act
        var result = mvc.perform(get("/api/v1/almacenes").param("categoria", categoria.name()).param("size", "1"));
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nit").value(nit));
    }

    @Test
    void categoriaSinResultadosEntregaPaginaVacia() throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes").param("categoria", "FORMAL");
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void categoriasViajanPorMapperYPersistenComoTexto() {
        // Arrange
        var domain = new Almacen("C", "Categorías", "Moda", null, null);
        domain.setCategorias(new HashSet<>(Set.of(CategoriaAlmacen.FORMAL, CategoriaAlmacen.CASUAL)));
        em.persist(almacenMapper.toEntity(domain));
        em.flush();
        em.clear();
        // Act
        var recuperado = almacenMapper.toDomain(almacenes.findById("C").orElseThrow());
        List<?> valores = em.createNativeQuery("select categoria from almacen_categorias where almacen_nit = 'C'")
                .getResultList();
        // Assert
        assertThat(recuperado.getCategorias()).containsExactlyInAnyOrder(CategoriaAlmacen.FORMAL, CategoriaAlmacen.CASUAL);
        assertThat(new HashSet<>(valores)).isEqualTo(Set.of("FORMAL", "CASUAL"));
    }

    @Test
    void filtroConVariosAlmacenesMulticategoriaMantieneCountYPaginasSinDuplicados() {
        // Arrange
        almacen("C", "Alfa", -90.0, -180.0, CategoriaAlmacen.CASUAL, CategoriaAlmacen.FORMAL,
                CategoriaAlmacen.VINTAGE, CategoriaAlmacen.ACCESORIOS);
        em.flush();
        em.clear();
        // Act
        var primera = service.consultar(CategoriaAlmacen.CASUAL, 0, 1);
        var segunda = service.consultar(CategoriaAlmacen.CASUAL, 1, 1);
        // Assert
        assertThat(primera.getContent()).extracting(Almacen::getNit).containsExactly("C");
        assertThat(segunda.getContent()).extracting(Almacen::getNit).containsExactly("A");
        assertThat(primera.getTotalElements()).isEqualTo(2);
        assertThat(segunda.getTotalElements()).isEqualTo(2);
        assertThat(primera.getTotalPages()).isEqualTo(2);
    }

    @Test
    void stockSumadoDeVariasTallasDeterminaVisibilidadYRespuesta() throws Exception {
        // Arrange
        var publicado = item(a, AHORA, 0, EstadoItem.AGOTADA);
        publicado.getInventario().add(new InventarioTallaEntity(null, Talla.S, 2, publicado));
        publicado.getInventario().add(new InventarioTallaEntity(null, Talla.L, 3, publicado));
        em.persist(publicado);
        em.flush();
        em.clear();
        // Act
        var result = mvc.perform(get("/api/v1/almacenes/A/catalogo/novedades"));
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].stock").value(5))
                .andExpect(jsonPath("$.content[0].estado").value("NUEVA_PRENDA"))
                .andExpect(jsonPath("$.content[0].tallasDisponibles.length()").value(2))
                .andExpect(jsonPath("$.content[0].tallasDisponibles").value(org.hamcrest.Matchers.containsInAnyOrder("S", "L")));
    }

    @Test
    void consultasCarganColeccionesEnLoteSinNMasUno() {
        // Arrange
        for (int i = 0; i < 6; i++) {
            almacen("C" + i, "Tienda" + i, 4.0, -74.0, CategoriaAlmacen.FORMAL);
            item(a, AHORA.minusSeconds(i), 4, EstadoItem.NUEVA_PRENDA);
        }
        em.flush();
        em.clear();
        var stats = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            // Act
            var almacenesPage = service.consultar(null, 0, 3);
            long consultasAlmacenes = stats.getPrepareStatementCount();
            em.clear();
            stats.clear();
            var novedadesPage = service.novedades("A", 0, 3);
            long consultasNovedades = stats.getPrepareStatementCount();
            // Assert
            assertThat(almacenesPage.getContent()).hasSize(3);
            assertThat(almacenesPage.getTotalElements()).isEqualTo(8);
            assertThat(consultasAlmacenes).isEqualTo(3); // IDs, count, fetch de categorías.
            assertThat(novedadesPage.getContent()).hasSize(3);
            assertThat(novedadesPage.getTotalElements()).isEqualTo(6);
            assertThat(consultasNovedades).isEqualTo(4); // Existencia, IDs, count, fetch de inventario/prenda.
        } finally {
            stats.setStatisticsEnabled(false);
        }
    }

    @Test
    void tamanoMaximoDePaginaEsAceptado() throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes").param("size", "100");
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void historicoGeolocalizableConCategoriasVaciasSigueDisponibleSinFiltro() {
        // Arrange
        almacen("C", "Antiguo", 4.0, -74.0);
        em.flush();
        em.clear();
        // Act
        var page = service.consultar(null, 0, 20);
        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().getFirst().getCategorias()).isEmpty();
        assertThat(service.consultar(CategoriaAlmacen.CASUAL, 0, 20).getTotalElements()).isEqualTo(1);
    }

    @Test
    void ordenDeterministaYPaginacionEnBD() throws Exception {
        // Arrange
        almacen("C", "Alfa", 90.0, 180.0);
        // Act
        var result = mvc.perform(get("/api/v1/almacenes").param("size", "1").param("page", "1"));
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].nit").value("A"))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false)).andExpect(jsonPath("$.last").value(false));
        assertThat(service.consultar(null, 0, 20).getContent()).extracting(Almacen::getNit)
                .containsExactly("C", "A", "B");
    }

    @Test
    void paginaFueraDeRangoConservaCountCorrecto() throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes").param("size", "1").param("page", "8");
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(8)).andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void sinAlmacenesGeolocalizablesEntregaListaVacia() {
        // Arrange
        a.setLatitud(null);
        b.setLongitud(null);
        // Act
        var page = service.consultar(null, 0, 20);
        // Assert
        assertThat(page).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoria=OTRA", "categoria=casual", "page=-1", "page=abc", "size=0", "size=101", "size=abc"})
    void parametrosDeListadoInvalidosSon400(String query) throws Exception {
        // Arrange
        String[] param = query.split("=");
        // Act
        var result = mvc.perform(get("/api/v1/almacenes").param(param[0], param[1]));
        // Assert
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void resumenUsaCincoRecientesPorDefectoConStockEstadoYTallas() throws Exception {
        // Arrange
        for (int i = 0; i < 7; i++) item(a, AHORA.minusSeconds(i), 2, EstadoItem.ULTIMAS_UNIDADES);
        // Act
        var result = mvc.perform(get("/api/v1/almacenes/A/catalogo/resumen"));
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].itemId").isNumber()).andExpect(jsonPath("$[0].prendaId").isNumber())
                .andExpect(jsonPath("$[0].nombre").value("Camisa"))
                .andExpect(jsonPath("$[0].tipo").value("SUPERIOR")).andExpect(jsonPath("$[0].marca").value("Radar"))
                .andExpect(jsonPath("$[0].color").value("Azul")).andExpect(jsonPath("$[0].estilo").value("CASUAL"))
                .andExpect(jsonPath("$[0].precio").value(100.0)).andExpect(jsonPath("$[0].stock").value(2))
                .andExpect(jsonPath("$[0].estado").value("ULTIMAS_UNIDADES"))
                .andExpect(jsonPath("$[0].tallasDisponibles[0]").value("M"))
                .andExpect(jsonPath("$[0].imagenes").doesNotExist());
    }

    @Test
    void resumenAplicaLimiteVeinteYDesempataPorId() throws Exception {
        // Arrange
        var ids = new ArrayList<Long>();
        for (int i = 0; i < 25; i++) ids.add(item(a, AHORA, 4, EstadoItem.NUEVA_PRENDA).getId());
        // Act
        var result = service.resumen("A", 20);
        // Assert
        assertThat(result).hasSize(20);
        assertThat(result).extracting(ItemCatalogo::getId).containsExactlyElementsOf(ids.subList(0, 20));
        mvc.perform(get("/api/v1/almacenes/A/catalogo/resumen").param("limite", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(20));
    }

    @Test
    void resumenExcluyeStockCeroRetiradosYOtrosAlmacenesAntesDeLimitar() {
        // Arrange
        var disponible = item(a, AHORA.minusSeconds(10), 4, EstadoItem.NUEVA_PRENDA);
        item(a, AHORA, 0, EstadoItem.DISPONIBLE); // Persistido obsoleto: efectivo AGOTADA.
        item(a, AHORA, 0, EstadoItem.AGOTADA);
        var retirado = item(a, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        em.remove(retirado); // El retiro existente elimina físicamente el ítem.
        item(b, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        // Act
        var result = service.resumen("A", 1);
        // Assert
        assertThat(result).extracting(ItemCatalogo::getId).containsExactly(disponible.getId());
    }

    @Test
    void estadoPersistidoAgotadoConStockRealNoOcultaPrendaDisponibleNiEscribe() {
        // Arrange
        var disponible = item(a, AHORA, 2, EstadoItem.AGOTADA);
        em.flush();
        em.clear();
        // Act
        var result = service.resumen("A", 5);
        em.flush();
        em.clear();
        // Assert
        assertThat(result).singleElement().satisfies(i -> {
            assertThat(i.getStock()).isEqualTo(2);
            assertThat(i.getEstado()).isEqualTo(EstadoItem.ULTIMAS_UNIDADES);
        });
        assertThat(items.findById(disponible.getId()).orElseThrow().getEstado()).isEqualTo(EstadoItem.AGOTADA);
    }

    @Test
    void resumenOrdenaPorPublicacionDescendenteLuegoId() {
        // Arrange
        var viejo = item(a, AHORA.minus(Duration.ofDays(8)), 4, EstadoItem.DISPONIBLE);
        var primero = item(a, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        var segundo = item(a, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        // Act
        var result = service.resumen("A", 5);
        // Assert
        assertThat(result).extracting(ItemCatalogo::getId).containsExactly(primero.getId(), segundo.getId(), viejo.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "21", "-1", "abc"})
    void limiteResumenInvalidoEs400(String limite) throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/A/catalogo/resumen").param("limite", limite);
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"resumen", "novedades"})
    void catalogoVacioEs200(String consulta) throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/A/catalogo/" + consulta);
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath(consulta.equals("resumen") ? "$" : "$.content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"catalogo/resumen", "catalogo/novedades", "distancia"})
    void almacenInexistenteEs404(String consulta) throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/missing/" + consulta)
                .param("latitudUsuario", "0").param("longitudUsuario", "0");
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @CsvSource({"0,true", "6,true", "7,true", "8,false", "-1,false"})
    void novedadesUsanVentanaMovilInclusivaConRelojFijo(int dias, boolean visible) {
        // Arrange
        var publicado = item(a, AHORA.minus(Duration.ofDays(dias)), 4, EstadoItem.DISPONIBLE);
        // Act
        var result = service.novedades("A", 0, 20);
        // Assert
        assertThat(result.getTotalElements()).isEqualTo(visible ? 1 : 0);
        if (visible) assertThat(result.getContent()).extracting(ItemCatalogo::getId).containsExactly(publicado.getId());
    }

    @Test
    void novedadesIncluyenFinExactoYExcluyenUnMicrosegundoFueraDeAmbosExtremos() {
        // Arrange
        var fin = item(a, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        item(a, AHORA.plusNanos(1000), 4, EstadoItem.NUEVA_PRENDA);
        item(a, AHORA.minus(Duration.ofDays(7)).minusNanos(1000), 4, EstadoItem.DISPONIBLE);
        // Act
        var result = service.novedades("A", 0, 20);
        // Assert
        assertThat(result.getContent()).extracting(ItemCatalogo::getId).containsExactly(fin.getId());
    }

    @Test
    void novedadesConStockBajoAparecenYAgotadasYOtroAlmacenNoAparecen() throws Exception {
        // Arrange
        var bajo = item(a, AHORA.minus(Duration.ofDays(2)), 2, EstadoItem.ULTIMAS_UNIDADES);
        item(a, AHORA, 0, EstadoItem.NUEVA_PRENDA);
        item(b, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        // Act
        var result = mvc.perform(get("/api/v1/almacenes/A/catalogo/novedades").param("size", "1"));
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].itemId").value(bajo.getId()))
                .andExpect(jsonPath("$.content[0].estado").value("ULTIMAS_UNIDADES"));
    }

    @Test
    void novedadesPaginanTrasFiltrarYConservanTotalFueraDeRango() throws Exception {
        // Arrange
        var uno = item(a, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        var dos = item(a, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        var tres = item(a, AHORA.minusSeconds(1), 4, EstadoItem.NUEVA_PRENDA);
        item(a, AHORA, 0, EstadoItem.NUEVA_PRENDA);
        item(a, AHORA.minus(Duration.ofDays(8)), 4, EstadoItem.DISPONIBLE);
        item(b, AHORA, 4, EstadoItem.NUEVA_PRENDA);
        // Act
        var page = service.novedades("A", 1, 2);
        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(ItemCatalogo::getId).containsExactly(tres.getId());
        assertThat(service.novedades("A", 0, 2).getContent()).extracting(ItemCatalogo::getId)
                .containsExactly(uno.getId(), dos.getId());
        mvc.perform(get("/api/v1/almacenes/A/catalogo/novedades").param("page", "10").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,101", "page,abc"})
    void paginacionNovedadesInvalidaEs400(String parametro, String valor) throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/A/catalogo/novedades").param(parametro, valor);
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isBadRequest());
    }

    @Test
    void distanciaConocidaSePresentaEnKmConDosDecimales() throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/A/distancia").param("latitudUsuario", "0").param("longitudUsuario", "1");
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.almacenNit").value("A"))
                .andExpect(jsonPath("$.distanciaKm").value(111.20))
                .andExpect(jsonPath("$.tipoEstimacion").value("LINEA_RECTA"));
        assertThat(service.distancia("A", 0, 1).distanciaKm()).isBetween(111.19, 111.20);
    }

    @ParameterizedTest
    @CsvSource({"0,0", "90,180", "-90,-180", "90,-180", "-90,180"})
    void coordenadasLimiteDelUsuarioSonValidas(double lat, double lon) throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/A/distancia")
                .param("latitudUsuario", Double.toString(lat)).param("longitudUsuario", Double.toString(lon));
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isOk());
        assertThat(service.distancia("A", lat, lon).distanciaKm()).isFinite().isGreaterThanOrEqualTo(0);
        if (lat == 0 && lon == 0) result.andExpect(jsonPath("$.distanciaKm").value(0));
    }

    @ParameterizedTest
    @CsvSource({"NaN,0", "Infinity,0", "-Infinity,0", "0,NaN", "0,Infinity", "91,0", "-91,0",
        "0,181", "0,-181", "abc,0", "0,abc", "1e309,0"})
    void coordenadasUsuarioInvalidasSon400(String lat, String lon) throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/A/distancia").param("latitudUsuario", lat).param("longitudUsuario", lon);
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"latitudUsuario", "longitudUsuario", "ambas"})
    void coordenadasUsuarioFaltantesSon400(String faltante) throws Exception {
        // Arrange
        var request = get("/api/v1/almacenes/A/distancia");
        if (faltante.equals("latitudUsuario")) request.param("longitudUsuario", "0");
        if (faltante.equals("longitudUsuario")) request.param("latitudUsuario", "0");
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,NULL", "4,NULL", "NULL,-74", "91,0", "NaN,0"}, nullValues = "NULL")
    void almacenSinCoordenadasValidasEs422(Double lat, Double lon) throws Exception {
        // Arrange
        almacen("C", "Histórico", lat, lon);
        // Act
        var result = mvc.perform(get("/api/v1/almacenes/C/distancia")
                .param("latitudUsuario", "0").param("longitudUsuario", "0"));
        // Assert
        result.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("El almacén no dispone de coordenadas geográficas válidas y completas"));
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/v1/almacenes", "PUT,/api/v1/almacenes/A", "PATCH,/api/v1/almacenes/A",
        "DELETE,/api/v1/almacenes/A", "POST,/api/v1/almacenes/A/catalogo",
        "PUT,/api/v1/almacenes/A/catalogo/1", "DELETE,/api/v1/almacenes/A/catalogo/1",
        "POST,/api/v1/almacenes/A/catalogo/resumen", "PUT,/api/v1/almacenes/A/distancia",
        "GET,/api/v1/almacenes/A/administracion", "GET,/api/v1/usuarios/1"})
    void seguridadRealConservaRutasPrivadasYEscriturasProtegidas(String metodo, String ruta) throws Exception {
        // Arrange
        var request = request(HttpMethod.valueOf(metodo), ruta);
        // Act
        var result = mvc.perform(request);
        // Assert
        result.andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void openApiDocumentaSoloLosGetPublicosYErroresAplicables() throws Exception {
        // Arrange
        var request = get("/v3/api-docs");
        // Act
        var result = mvc.perform(request);
        // Assert
        for (String ruta : List.of("/api/v1/almacenes", "/api/v1/almacenes/{nit}/catalogo/resumen",
                "/api/v1/almacenes/{nit}/catalogo/novedades", "/api/v1/almacenes/{nit}/distancia")) {
            result.andExpect(jsonPath("$.paths['" + ruta + "'].get").exists())
                    .andExpect(jsonPath("$.paths['" + ruta + "'].get.security").isEmpty())
                    .andExpect(jsonPath("$.paths['" + ruta + "'].get.responses['200']").exists())
                    .andExpect(jsonPath("$.paths['" + ruta + "'].get.responses['400']").exists())
                    .andExpect(jsonPath("$.paths['" + ruta + "'].get.responses['500']").exists())
                    .andExpect(jsonPath("$.paths['" + ruta + "'].post").doesNotExist());
        }
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/distancia'].get.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo/resumen'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo/resumen'].get.responses['422']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo'].post.security[0].bearerAuth").isArray());
    }

    private AlmacenEntity almacen(String nit, String nombre, Double lat, Double lon, CategoriaAlmacen... categorias) {
        var almacen = new AlmacenEntity(nit, nombre, "Moda", "3000000000", "contacto@ejemplo.co",
                lat, lon, 4.5, new ArrayList<>());
        almacen.setCategorias(new HashSet<>(Arrays.asList(categorias)));
        em.persist(almacen);
        return almacen;
    }

    private ItemCatalogoEntity item(AlmacenEntity almacen, Instant fecha, int stock, EstadoItem estado) {
        var prenda = new PrendaEntity(null, "Camisa", "Algodón", TipoPrenda.SUPERIOR, "Radar", "Azul", Estilo.CASUAL);
        em.persist(prenda);
        var item = new ItemCatalogoEntity();
        item.setAlmacen(almacen);
        item.setPrenda(prenda);
        item.setFechaPublicacion(fecha);
        item.setPrecio(100.0);
        item.setEstado(estado);
        item.getInventario().add(new InventarioTallaEntity(null, Talla.M, stock, item));
        em.persist(item);
        return item;
    }
}
