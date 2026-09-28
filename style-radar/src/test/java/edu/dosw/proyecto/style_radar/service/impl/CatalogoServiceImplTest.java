package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.PrendaEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    private static final String NIT = "900123456";

    @Mock
    private AlmacenRepository almacenRepository;

    @Mock
    private PrendaRepository prendaRepository;

    @Mock
    private ItemCatalogoRepository itemCatalogoRepository;

    @Mock
    private PrendaEntityMapper prendaEntityMapper;

    @Mock
    private ItemCatalogoEntityMapper itemCatalogoEntityMapper;

    @InjectMocks
    private CatalogoServiceImpl catalogoService;

    @Test
    void obtenerCatalogoShouldReturnItemsWhenStoreExists() {
        // Arrange
        ItemCatalogoEntity entity = itemEntity(10L);
        ItemCatalogo domain = itemDomain(10L);
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByAlmacen_Nit(NIT)).thenReturn(List.of(entity));
        when(itemCatalogoEntityMapper.toDomain(entity)).thenReturn(domain);

        // Act
        List<ItemCatalogo> result = catalogoService.obtenerCatalogo(NIT);

        // Assert
        assertThat(result).containsExactly(domain);
        verify(itemCatalogoRepository).findByAlmacen_Nit(NIT);
        verify(itemCatalogoEntityMapper).toDomain(entity);
    }

    @Test
    void obtenerCatalogoShouldReturnEmptyListWhenStoreHasNoItems() {
        // Arrange
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByAlmacen_Nit(NIT)).thenReturn(List.of());

        // Act
        List<ItemCatalogo> result = catalogoService.obtenerCatalogo(NIT);

        // Assert
        assertThat(result).isEmpty();
        verify(itemCatalogoRepository).findByAlmacen_Nit(NIT);
        verify(itemCatalogoEntityMapper, never()).toDomain(any());
    }

    @Test
    void obtenerCatalogoShouldThrowWhenStoreDoesNotExist() {
        // Arrange
        when(almacenRepository.existsById(NIT)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> catalogoService.obtenerCatalogo(NIT))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(itemCatalogoRepository, never()).findByAlmacen_Nit(any());
    }

    @Test
    void publicarShouldSaveGarmentAndCatalogItem() {
        // Arrange
        AlmacenEntity almacen = almacenEntity();
        Prenda prenda = prendaDomain(null);
        PrendaEntity prendaEntity = prendaEntity(null);
        PrendaEntity prendaGuardada = prendaEntity(5L);
        Prenda prendaGuardadaDomain = prendaDomain(5L);
        ItemCatalogoEntity itemSinGuardar = itemEntity(null);
        ItemCatalogo resultadoEsperado = itemDomain(10L);

        when(almacenRepository.findById(NIT)).thenReturn(Optional.of(almacen));
        when(prendaEntityMapper.toEntity(prenda)).thenReturn(prendaEntity);
        when(prendaRepository.save(prendaEntity)).thenReturn(prendaGuardada);
        when(prendaEntityMapper.toDomain(prendaGuardada)).thenReturn(prendaGuardadaDomain);
        when(itemCatalogoEntityMapper.toEntity(any(ItemCatalogo.class))).thenReturn(itemSinGuardar);
        when(itemCatalogoRepository.save(itemSinGuardar)).thenAnswer(invocation -> {
            itemSinGuardar.setId(10L);
            return itemSinGuardar;
        });
        when(itemCatalogoEntityMapper.toDomain(itemSinGuardar)).thenReturn(resultadoEsperado);

        // Act
        ItemCatalogo result = catalogoService.publicar(NIT, prenda, 89000.0);

        // Assert
        assertThat(result).isEqualTo(resultadoEsperado);
        assertThat(itemSinGuardar.getAlmacen()).isSameAs(almacen);
        assertThat(itemSinGuardar.getPrenda()).isSameAs(prendaGuardada);
        ArgumentCaptor<ItemCatalogo> itemCaptor = ArgumentCaptor.forClass(ItemCatalogo.class);
        verify(itemCatalogoEntityMapper).toEntity(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStock()).isZero();
        assertThat(itemCaptor.getValue().getEstado()).isEqualTo(EstadoItem.AGOTADA);
        assertThat(itemCaptor.getValue().getTallasDisponibles()).isEmpty();
        assertThat(itemCaptor.getValue().getInventario()).isEmpty();
        verify(prendaRepository, times(1)).save(prendaEntity);
        verify(itemCatalogoRepository, times(1)).save(itemSinGuardar);
    }

    @Test
    void publicarShouldNotSaveAnythingWhenStoreDoesNotExist() {
        // Arrange
        Prenda prenda = prendaDomain(null);
        when(almacenRepository.findById(NIT)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogoService.publicar(NIT, prenda, 89000.0))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(prendaRepository, never()).save(any());
        verify(itemCatalogoRepository, never()).save(any());
    }

    @Test
    void actualizarShouldUpdateGarmentAndPriceForStoreItem() {
        // Arrange
        ItemCatalogoEntity item = itemEntity(10L);
        Prenda cambios = new Prenda(null, "Chaqueta", "Chaqueta formal", TipoPrenda.SUPERIOR,
                "Radar", "Azul", Estilo.FORMAL);
        PrendaEntity cambiosEntity = new PrendaEntity(null, "Chaqueta", "Chaqueta formal",
                TipoPrenda.SUPERIOR, "Radar", "Azul", Estilo.FORMAL);
        ItemCatalogo resultadoEsperado = itemDomain(10L);

        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(10L, NIT)).thenReturn(Optional.of(item));
        when(prendaEntityMapper.toEntity(cambios)).thenReturn(cambiosEntity);
        when(prendaRepository.save(cambiosEntity)).thenReturn(cambiosEntity);
        when(itemCatalogoRepository.save(item)).thenReturn(item);
        when(itemCatalogoEntityMapper.toDomain(item)).thenReturn(resultadoEsperado);

        // Act
        ItemCatalogo result = catalogoService.actualizar(NIT, 10L, cambios, 125000.0);

        // Assert
        assertThat(result).isEqualTo(resultadoEsperado);
        assertThat(cambiosEntity.getId()).isEqualTo(5L);
        assertThat(item.getPrecio()).isEqualTo(125000.0);
        assertThat(item.getPrenda()).isSameAs(cambiosEntity);
        assertThat(item.getInventario()).isEmpty();
        assertThat(item.getEstado()).isEqualTo(EstadoItem.AGOTADA);
        verify(prendaRepository).save(cambiosEntity);
        verify(itemCatalogoRepository).save(item);
    }

    @Test
    void actualizarShouldThrowWhenItemIsNotInStore() {
        // Arrange
        Prenda cambios = prendaDomain(null);
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(10L, NIT)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogoService.actualizar(NIT, 10L, cambios, 125000.0))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(prendaRepository, never()).save(any());
        verify(itemCatalogoRepository, never()).save(any());
    }

    @Test
    void retirarShouldDeleteStoreItem() {
        // Arrange
        ItemCatalogoEntity item = itemEntity(10L);
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(10L, NIT)).thenReturn(Optional.of(item));

        // Act
        catalogoService.retirar(NIT, 10L);

        // Assert
        verify(itemCatalogoRepository).delete(item);
        verify(prendaRepository, never()).delete(any());
    }

    @Test
    void retirarShouldThrowWhenItemDoesNotExist() {
        // Arrange
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(10L, NIT)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogoService.retirar(NIT, 10L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(itemCatalogoRepository, never()).delete(any());
        verify(prendaRepository, never()).delete(any());
    }

    private AlmacenEntity almacenEntity() {
        return new AlmacenEntity(NIT, "StyleRadar", "Moda", "3000000000", "contacto@styleradar.co",
                new ArrayList<>());
    }

    private PrendaEntity prendaEntity(Long id) {
        return new PrendaEntity(id, "Camiseta", "Camiseta de algodón", TipoPrenda.SUPERIOR,
                "StyleRadar", "Negro", Estilo.CASUAL);
    }

    private Prenda prendaDomain(Long id) {
        return new Prenda(id, "Camiseta", "Camiseta de algodón", TipoPrenda.SUPERIOR,
                "StyleRadar", "Negro", Estilo.CASUAL);
    }

    private ItemCatalogoEntity itemEntity(Long id) {
        return new ItemCatalogoEntity(id, 89000.0, EstadoItem.AGOTADA,
                almacenEntity(), prendaEntity(5L), new ArrayList<>(), new ArrayList<>());
    }

    private ItemCatalogo itemDomain(Long id) {
        return new ItemCatalogo(id, 89000.0, EstadoItem.AGOTADA,
                prendaDomain(5L), NIT, new ArrayList<>());
    }
}
