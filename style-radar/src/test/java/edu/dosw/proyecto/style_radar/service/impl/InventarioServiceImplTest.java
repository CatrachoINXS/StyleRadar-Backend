package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.InventarioTalla;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;
import edu.dosw.proyecto.style_radar.validator.InventarioValidator;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    private static final String NIT = "900123456";
    private static final Long ITEM_ID = 10L;
    private static final Instant AHORA = Instant.parse("2026-09-08T10:00:00Z");

    @Mock
    private AlmacenRepository almacenRepository;

    @Mock
    private ItemCatalogoRepository itemCatalogoRepository;

    @Mock
    private ItemCatalogoEntityMapper itemCatalogoEntityMapper;

    private InventarioServiceImpl inventarioService;

    @BeforeEach
    void setUp() {
        inventarioService = new InventarioServiceImpl(
                almacenRepository,
                itemCatalogoRepository,
                itemCatalogoEntityMapper,
                new InventarioValidator(),
                new EstadoItemCalculator(Clock.fixed(AHORA, ZoneOffset.UTC)));
    }

    @Test
    void registrarTallasShouldCreateNewEntriesWithZeroUnits() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.AGOTADA);
        prepararItem(item);

        // Act
        inventarioService.registrarTallas(NIT, ITEM_ID, Set.of(Talla.S, Talla.M));

        // Assert
        assertThat(item.getInventario()).hasSize(2);
        assertThat(item.getInventario()).extracting(InventarioTallaEntity::getTalla)
                .containsExactlyInAnyOrder(Talla.S, Talla.M);
        assertThat(item.getInventario()).extracting(InventarioTallaEntity::getUnidades).containsOnly(0);
        assertThat(item.getInventario()).allMatch(entry -> entry.getItemCatalogo() == item);
    }

    @Test
    void registrarTallasShouldPreserveUnitsOfExistingSize() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.DISPONIBLE);
        item.getInventario().add(inventario(item, Talla.M, 7));
        prepararItem(item);

        // Act
        inventarioService.registrarTallas(NIT, ITEM_ID, Set.of(Talla.M, Talla.L));

        // Assert
        assertThat(unidades(item, Talla.M)).isEqualTo(7);
        assertThat(unidades(item, Talla.L)).isZero();
    }

    @Test
    void registrarTallasShouldRemoveAbsentSizeWithZeroUnits() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.AGOTADA);
        item.getInventario().add(inventario(item, Talla.S, 0));
        item.getInventario().add(inventario(item, Talla.M, 0));
        prepararItem(item);

        // Act
        inventarioService.registrarTallas(NIT, ITEM_ID, Set.of(Talla.M));

        // Assert
        assertThat(item.getInventario()).extracting(InventarioTallaEntity::getTalla).containsExactly(Talla.M);
    }

    @Test
    void registrarTallasShouldRejectRemovingSizeWithUnits() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.DISPONIBLE);
        item.getInventario().add(inventario(item, Talla.M, 3));
        item.getInventario().add(inventario(item, Talla.L, 0));
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(ITEM_ID, NIT)).thenReturn(Optional.of(item));

        // Act & Assert
        assertThatThrownBy(() -> inventarioService.registrarTallas(NIT, ITEM_ID, Set.of(Talla.L)))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(item.getInventario()).hasSize(2);
        verify(itemCatalogoRepository, never()).save(any());
    }

    @Test
    void actualizarDisponibilidadShouldUpdateRegisteredSize() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.AGOTADA);
        item.getInventario().add(inventario(item, Talla.M, 0));
        prepararItem(item);

        // Act
        inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.M, 7);

        // Assert
        assertThat(unidades(item, Talla.M)).isEqualTo(7);
        verify(itemCatalogoRepository).save(item);
    }

    @Test
    void itemCatalogoShouldCalculateTotalStockFromInventory() {
        // Arrange
        ItemCatalogo item = domainItem(List.of(
                new InventarioTalla(Talla.S, 0),
                new InventarioTalla(Talla.M, 4),
                new InventarioTalla(Talla.L, 2)));

        // Act
        Integer stock = item.getStock();

        // Assert
        assertThat(stock).isEqualTo(6);
    }

    @Test
    void itemCatalogoShouldExposeOnlySizesWithUnits() {
        // Arrange
        ItemCatalogo item = domainItem(List.of(
                new InventarioTalla(Talla.S, 0),
                new InventarioTalla(Talla.M, 4),
                new InventarioTalla(Talla.L, 2)));

        // Act
        List<Talla> tallasDisponibles = item.getTallasDisponibles();

        // Assert
        assertThat(tallasDisponibles).containsExactly(Talla.M, Talla.L);
    }

    @Test
    void actualizarDisponibilidadShouldChangeExhaustedToLastUnitsWithStockOne() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.AGOTADA);
        item.getInventario().add(inventario(item, Talla.M, 0));
        prepararItem(item);

        // Act
        inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.M, 1);

        // Assert
        assertThat(item.getEstado()).isEqualTo(EstadoItem.ULTIMAS_UNIDADES);
    }

    @Test
    void actualizarDisponibilidadShouldSetLastUnitsWithStockThree() {
        ItemCatalogoEntity item = item(EstadoItem.AGOTADA);
        item.getInventario().add(inventario(item, Talla.M, 0));
        prepararItem(item);

        inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.M, 3);

        assertThat(item.getEstado()).isEqualTo(EstadoItem.ULTIMAS_UNIDADES);
    }

    @Test
    void actualizarDisponibilidadShouldSetNewArrivalWithStockFourWithinSevenDays() {
        ItemCatalogoEntity item = item(EstadoItem.AGOTADA);
        item.getInventario().add(inventario(item, Talla.M, 0));
        prepararItem(item);

        inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.M, 4);

        assertThat(item.getEstado()).isEqualTo(EstadoItem.NUEVA_PRENDA);
    }

    @Test
    void actualizarDisponibilidadShouldSetAvailableWithStockFourAfterSevenDays() {
        ItemCatalogoEntity item = item(EstadoItem.NUEVA_PRENDA);
        item.setFechaPublicacion(AHORA.minusSeconds(8 * 24 * 60 * 60));
        item.getInventario().add(inventario(item, Talla.M, 0));
        prepararItem(item);

        inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.M, 4);

        assertThat(item.getEstado()).isEqualTo(EstadoItem.DISPONIBLE);
    }

    @Test
    void actualizarDisponibilidadShouldChangeStateToExhaustedWhenAllSizesReachZero() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.DISPONIBLE);
        item.getInventario().add(inventario(item, Talla.M, 0));
        item.getInventario().add(inventario(item, Talla.L, 2));
        prepararItem(item);

        // Act
        inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.L, 0);

        // Assert
        assertThat(item.getEstado()).isEqualTo(EstadoItem.AGOTADA);
    }

    @Test
    void actualizarDisponibilidadShouldRejectUnregisteredSize() {
        // Arrange
        ItemCatalogoEntity item = item(EstadoItem.AGOTADA);
        item.getInventario().add(inventario(item, Talla.S, 0));
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(ITEM_ID, NIT)).thenReturn(Optional.of(item));

        // Act & Assert
        assertThatThrownBy(() -> inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.M, 3))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(itemCatalogoRepository, never()).save(any());
    }

    @Test
    void registrarTallasShouldThrowWhenItemDoesNotExist() {
        // Arrange
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(ITEM_ID, NIT)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventarioService.registrarTallas(NIT, ITEM_ID, Set.of(Talla.M)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(itemCatalogoRepository, never()).save(any());
    }

    @Test
    void actualizarDisponibilidadShouldRejectItemFromAnotherStore() {
        // Arrange
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(ITEM_ID, NIT)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventarioService.actualizarDisponibilidad(NIT, ITEM_ID, Talla.M, 2))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(itemCatalogoRepository, never()).save(any());
    }

    private void prepararItem(ItemCatalogoEntity item) {
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(ITEM_ID, NIT)).thenReturn(Optional.of(item));
        when(itemCatalogoRepository.save(item)).thenReturn(item);
        when(itemCatalogoEntityMapper.toDomain(item)).thenAnswer(invocation -> domainItem(item));
    }

    private ItemCatalogoEntity item(EstadoItem estado) {
        ItemCatalogoEntity item = new ItemCatalogoEntity();
        item.setId(ITEM_ID);
        item.setPrecio(89000.0);
        item.setEstado(estado);
        item.setFechaPublicacion(AHORA);
        item.setInventario(new ArrayList<>());
        item.setImagenes(new ArrayList<>());
        return item;
    }

    private InventarioTallaEntity inventario(ItemCatalogoEntity item, Talla talla, int unidades) {
        return new InventarioTallaEntity(null, talla, unidades, item);
    }

    private int unidades(ItemCatalogoEntity item, Talla talla) {
        return item.getInventario().stream()
                .filter(entry -> entry.getTalla() == talla)
                .findFirst()
                .orElseThrow()
                .getUnidades();
    }

    private ItemCatalogo domainItem(List<InventarioTalla> inventario) {
        return new ItemCatalogo(ITEM_ID, 89000.0, EstadoItem.AGOTADA, AHORA, null, NIT, inventario);
    }

    private ItemCatalogo domainItem(ItemCatalogoEntity item) {
        List<InventarioTalla> inventario = item.getInventario().stream()
                .map(entry -> new InventarioTalla(entry.getTalla(), entry.getUnidades()))
                .toList();
        return new ItemCatalogo(item.getId(), item.getPrecio(), item.getEstado(), item.getFechaPublicacion(),
                null, NIT, inventario);
    }
}
