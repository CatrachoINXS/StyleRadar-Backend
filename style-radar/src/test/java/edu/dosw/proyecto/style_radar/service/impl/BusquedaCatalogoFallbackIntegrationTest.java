package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.OrdenCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.service.IBusquedaCatalogoService;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BusquedaCatalogoFallbackIntegrationTest {

    private static final Instant FECHA = Instant.parse("2026-09-01T10:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private IBusquedaCatalogoService service;

    private AlmacenEntity almacen;

    @BeforeEach
    void setUp() {
        almacen = persistirAlmacen("NIT-BASE", 0.0, 0.0, 3.0);
    }

    @Test
    void sinCoincidenciaDirectaDebeRetornarSimilarConScorePositivo() {
        ItemCatalogoEntity similar = persistirItem("Camiseta urbana", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(criteria("camiseta negra"), 0, 20);

        assertThat(ids(resultado)).containsExactly(similar.getId());
    }

    @Test
    void masSimilarDebeAparecerAntes() {
        ItemCatalogoEntity menosSimilar = persistirItem("Camiseta urbana", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        ItemCatalogoEntity masSimilar = persistirItem("Camiseta urbana", "Elegante", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(criteria("camiseta negra elegante"), 0, 20);

        assertThat(ids(resultado)).containsExactly(masSimilar.getId(), menosSimilar.getId());
    }

    @Test
    void scoreEmpatadoDebeDesempatarPorItemIdAscendente() {
        ItemCatalogoEntity primero = persistirItem("Camiseta uno", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        ItemCatalogoEntity segundo = persistirItem("Camiseta dos", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Verde", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(criteria("camiseta negra"), 0, 20);

        assertThat(ids(resultado)).containsExactly(primero.getId(), segundo.getId());
    }

    @Test
    void scoreCeroNoDebeAparecer() {
        persistirItem("Falda formal", "Lino", TipoPrenda.INFERIOR,
                "Radar", "Blanca", Estilo.FORMAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(criteria("camiseta negra"), 0, 20);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    @Test
    void tipoDebeConservarseEnFallback() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta superior", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        persistirItem("Camiseta calzado", "Textil", TipoPrenda.CALZADO,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta negra").tipo(TipoPrenda.SUPERIOR).build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(esperado.getId());
    }

    @Test
    void colorDebeConservarseEnFallback() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta urbana", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        persistirItem("Camiseta urbana", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta elegante").color("NEGRA").build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(esperado.getId());
    }

    @Test
    void tallaDebeExigirRegistroConStockPositivoEnFallback() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta disponible", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 2, almacen);
        persistirItem("Camiseta sin stock M", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 0, almacen);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta negra").talla(Talla.M).build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(esperado.getId());
    }

    @Test
    void rangoDePrecioDebeConservarseEnFallback() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta rango", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 75000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        persistirItem("Camiseta costosa", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 120000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta negra").precioMin(50000.0).precioMax(100000.0).build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(esperado.getId());
    }

    @Test
    void marcaDebeConservarseEnFallback() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta urbana", "Algodón", TipoPrenda.SUPERIOR,
                "Nike", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        persistirItem("Camiseta urbana", "Algodón", TipoPrenda.SUPERIOR,
                "Adidas", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta negra").marca("NIKE").build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(esperado.getId());
    }

    @Test
    void estiloDebeConservarseEnFallback() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta urbana", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        persistirItem("Camiseta formal", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.FORMAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta negra").estilo(Estilo.CASUAL).build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(esperado.getId());
    }

    @Test
    void agotadaNoDebeAparecerEnFallback() {
        persistirItem("Camiseta agotada", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.AGOTADA, Talla.M, 0, almacen);

        Page<ItemCatalogo> resultado = buscar(criteria("camiseta negra elegante"), 0, 20);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    @Test
    void debePaginarDespuesDeOrdenarTodosLosScores() {
        ItemCatalogoEntity cuarto = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        ItemCatalogoEntity primero = persistirItem("Camiseta", "Casual", TipoPrenda.SUPERIOR,
                "Nike", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        ItemCatalogoEntity tercero = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);
        ItemCatalogoEntity segundo = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Nike", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, almacen);

        Page<ItemCatalogo> resultado = buscar(criteria("camiseta negra nike casual"), 0, 2);

        assertThat(ids(resultado)).containsExactly(primero.getId(), segundo.getId());
        assertThat(resultado.getTotalElements()).isEqualTo(4);
        assertThat(ids(resultado)).doesNotContain(tercero.getId(), cuarto.getId());
    }

    @Test
    void distanciaDebeDesempatarSinSuperarLaSimilitud() {
        AlmacenEntity lejano = persistirAlmacen("NIT-LEJANO", 0.0, 10.0, 3.0);
        AlmacenEntity cercano = persistirAlmacen("NIT-CERCANO", 0.0, 1.0, 3.0);
        AlmacenEntity medio = persistirAlmacen("NIT-MEDIO", 0.0, 2.0, 3.0);
        AlmacenEntity sinCoordenadas = persistirAlmacen("NIT-SIN-COORD", null, null, 3.0);
        ItemCatalogoEntity scoreMayor = persistirItem("Camiseta", "Elegante", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, lejano);
        ItemCatalogoEntity empateCercano = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, cercano);
        ItemCatalogoEntity empateMedio = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, medio);
        ItemCatalogoEntity empateSinCoordenadas = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, sinCoordenadas);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta negra elegante").orden(OrdenCatalogo.DISTANCIA)
                .latitudUsuario(0.0).longitudUsuario(0.0).build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(
                scoreMayor.getId(), empateCercano.getId(), empateMedio.getId(), empateSinCoordenadas.getId());
    }

    @Test
    void reputacionDebeDesempatarSinSuperarLaSimilitud() {
        AlmacenEntity reputacionBaja = persistirAlmacen("NIT-BAJA", 0.0, 0.0, 1.0);
        AlmacenEntity reputacionAlta = persistirAlmacen("NIT-ALTA", 0.0, 0.0, 5.0);
        AlmacenEntity reputacionMedia = persistirAlmacen("NIT-MEDIA", 0.0, 0.0, 4.0);
        AlmacenEntity sinReputacion = persistirAlmacen("NIT-SIN-REP", 0.0, 0.0, null);
        ItemCatalogoEntity scoreMayor = persistirItem("Camiseta", "Elegante", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, reputacionBaja);
        ItemCatalogoEntity empateAlta = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, reputacionAlta);
        ItemCatalogoEntity empateMedia = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, reputacionMedia);
        ItemCatalogoEntity empateSinReputacion = persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negra", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 3, sinReputacion);

        Page<ItemCatalogo> resultado = buscar(BusquedaCatalogoCriteria.builder()
                .q("camiseta negra elegante").orden(OrdenCatalogo.REPUTACION).build(), 0, 20);

        assertThat(ids(resultado)).containsExactly(
                scoreMayor.getId(), empateAlta.getId(), empateMedia.getId(), empateSinReputacion.getId());
    }

    private BusquedaCatalogoCriteria criteria(String q) {
        return BusquedaCatalogoCriteria.builder().q(q).build();
    }

    private Page<ItemCatalogo> buscar(BusquedaCatalogoCriteria criteria, int page, int size) {
        entityManager.flush();
        entityManager.clear();
        return service.buscar(criteria, page, size);
    }

    private List<Long> ids(Page<ItemCatalogo> page) {
        return page.getContent().stream().map(ItemCatalogo::getId).toList();
    }

    private AlmacenEntity persistirAlmacen(String nit, Double latitud, Double longitud, Double reputacion) {
        AlmacenEntity nuevo = new AlmacenEntity(
                nit, "Almacén " + nit, "Moda", "3000000000", nit + "@radar.co",
                latitud, longitud, reputacion, new ArrayList<>());
        entityManager.persist(nuevo);
        return nuevo;
    }

    private ItemCatalogoEntity persistirItem(
            String nombre,
            String descripcion,
            TipoPrenda tipo,
            String marca,
            String color,
            Estilo estilo,
            Double precio,
            EstadoItem estado,
            Talla talla,
            int unidades,
            AlmacenEntity almacenItem) {
        PrendaEntity prenda = new PrendaEntity(null, nombre, descripcion, tipo, marca, color, estilo);
        entityManager.persist(prenda);
        ItemCatalogoEntity item = new ItemCatalogoEntity(
                null, precio, estado, FECHA, almacenItem, prenda, new ArrayList<>(), new ArrayList<>());
        entityManager.persist(item);
        InventarioTallaEntity inventario = new InventarioTallaEntity(null, talla, unidades, item);
        item.getInventario().add(inventario);
        entityManager.persist(inventario);
        return item;
    }
}
