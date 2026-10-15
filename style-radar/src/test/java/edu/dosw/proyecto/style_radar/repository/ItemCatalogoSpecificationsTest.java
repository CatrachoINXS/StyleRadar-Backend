package edu.dosw.proyecto.style_radar.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.repository.specification.ItemCatalogoSpecifications;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemCatalogoSpecificationsTest {

    private static final Instant FECHA = Instant.parse("2026-09-01T10:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ItemCatalogoRepository repository;

    private AlmacenEntity almacen;

    @BeforeEach
    void setUp() {
        almacen = new AlmacenEntity("900123456", "Radar", "Moda", "3000000000",
                "catalogo@radar.co", new ArrayList<>());
        entityManager.persist(almacen);
    }

    @Test
    void qShouldFindPartialNameMatch() {
        ItemCatalogoEntity camiseta = persistirItem("Camiseta básica", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(BusquedaCatalogoCriteria.builder().q("cam").build(), 0, 20);

        assertThat(result.getContent()).extracting(ItemCatalogoEntity::getId).containsExactly(camiseta.getId());
    }

    @Test
    void qShouldBeCaseInsensitive() {
        persistirItem("Camiseta básica", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(BusquedaCatalogoCriteria.builder().q("CAMISETA").build(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldFilterByTipo() {
        persistirItem("Tenis", "Running", TipoPrenda.CALZADO,
                "Adidas", "Blanco", Estilo.DEPORTIVO, 150000.0, EstadoItem.DISPONIBLE, Talla.M, 4);
        persistirItem("Camisa", "Formal", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.FORMAL, 90000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().tipo(TipoPrenda.CALZADO).build(), 0, 20);

        assertThat(result.getContent()).singleElement()
                .extracting(item -> item.getPrenda().getTipo()).isEqualTo(TipoPrenda.CALZADO);
    }

    @Test
    void shouldFilterColorCaseInsensitively() {
        persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().color("NEGRO").build(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldIncludeRequestedSizeWithPositiveUnits() {
        persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 2);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().talla(Talla.M).build(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldExcludeRequestedSizeWithZeroUnits() {
        persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 0);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().talla(Talla.M).build(), 0, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldIncludeMinimumPriceBoundary() {
        persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 50000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().precioMin(50000.0).build(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldIncludeMaximumPriceBoundary() {
        persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 100000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().precioMax(100000.0).build(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldFilterBrandCaseInsensitively() {
        persistirItem("Tenis", "Running", TipoPrenda.CALZADO,
                "Adidas", "Blanco", Estilo.DEPORTIVO, 150000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().marca("ADIDAS").build(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldFilterExactStyle() {
        persistirItem("Tenis", "Running", TipoPrenda.CALZADO,
                "Adidas", "Blanco", Estilo.DEPORTIVO, 150000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().estilo(Estilo.DEPORTIVO).build(), 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldCombineAllFiltersWithAnd() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta deportiva", "Entrenamiento", TipoPrenda.SUPERIOR,
                "Adidas", "Negro", Estilo.DEPORTIVO, 120000.0, EstadoItem.DISPONIBLE, Talla.M, 3);
        persistirItem("Camiseta casual", "Algodón", TipoPrenda.SUPERIOR,
                "Adidas", "Negro", Estilo.CASUAL, 120000.0, EstadoItem.DISPONIBLE, Talla.M, 3);

        BusquedaCatalogoCriteria criteria = BusquedaCatalogoCriteria.builder()
                .q("camiseta")
                .tipo(TipoPrenda.SUPERIOR)
                .color("negro")
                .talla(Talla.M)
                .precioMin(100000.0)
                .precioMax(120000.0)
                .marca("adidas")
                .estilo(Estilo.DEPORTIVO)
                .build();

        Page<ItemCatalogoEntity> result = buscar(criteria, 0, 20);

        assertThat(result.getContent()).extracting(ItemCatalogoEntity::getId).containsExactly(esperado.getId());
    }

    @Test
    void specificationSinTextoDebeIgnorarQYConservarLosDemasFiltros() {
        ItemCatalogoEntity esperado = persistirItem("Camiseta deportiva", "Entrenamiento", TipoPrenda.SUPERIOR,
                "Adidas", "Negro", Estilo.DEPORTIVO, 120000.0, EstadoItem.DISPONIBLE, Talla.M, 3);
        persistirItem("Camiseta casual", "Algodón", TipoPrenda.SUPERIOR,
                "Adidas", "Negro", Estilo.CASUAL, 120000.0, EstadoItem.DISPONIBLE, Talla.M, 3);

        BusquedaCatalogoCriteria criteria = BusquedaCatalogoCriteria.builder()
                .q("texto inexistente")
                .tipo(TipoPrenda.SUPERIOR)
                .color("negro")
                .talla(Talla.M)
                .precioMin(100000.0)
                .precioMax(120000.0)
                .marca("adidas")
                .estilo(Estilo.DEPORTIVO)
                .build();

        entityManager.flush();
        entityManager.clear();
        List<ItemCatalogoEntity> result = repository.findAll(
                ItemCatalogoSpecifications.conCriteriosSinTexto(criteria));

        assertThat(result).extracting(ItemCatalogoEntity::getId).containsExactly(esperado.getId());
    }

    @Test
    void shouldExcludeExhaustedItems() {
        persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.AGOTADA, Talla.M, 0);

        Page<ItemCatalogoEntity> result = buscar(BusquedaCatalogoCriteria.builder().build(), 0, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyPageWhenNothingMatches() {
        persistirItem("Camiseta", "Algodón", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(
                BusquedaCatalogoCriteria.builder().marca("Inexistente").build(), 0, 20);

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void shouldRespectPageAndSizeWithStableIdOrder() {
        ItemCatalogoEntity first = persistirItem("Uno", "Prenda", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 4);
        ItemCatalogoEntity second = persistirItem("Dos", "Prenda", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 4);
        ItemCatalogoEntity third = persistirItem("Tres", "Prenda", TipoPrenda.SUPERIOR,
                "Radar", "Negro", Estilo.CASUAL, 80000.0, EstadoItem.DISPONIBLE, Talla.M, 4);

        Page<ItemCatalogoEntity> result = buscar(BusquedaCatalogoCriteria.builder().build(), 1, 1);

        assertThat(result.getContent()).extracting(ItemCatalogoEntity::getId).containsExactly(second.getId());
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(first.getId()).isLessThan(second.getId());
        assertThat(second.getId()).isLessThan(third.getId());
    }

    private Page<ItemCatalogoEntity> buscar(BusquedaCatalogoCriteria criteria, int page, int size) {
        entityManager.flush();
        entityManager.clear();
        return repository.findAll(
                ItemCatalogoSpecifications.conCriterios(criteria),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
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
            int unidades) {
        PrendaEntity prenda = new PrendaEntity(null, nombre, descripcion, tipo, marca, color, estilo);
        entityManager.persist(prenda);
        ItemCatalogoEntity item = new ItemCatalogoEntity(
                null, precio, estado, FECHA, almacen, prenda, new ArrayList<>(), new ArrayList<>());
        entityManager.persist(item);
        InventarioTallaEntity inventario = new InventarioTallaEntity(null, talla, unidades, item);
        item.getInventario().add(inventario);
        entityManager.persist(inventario);
        return item;
    }
}
