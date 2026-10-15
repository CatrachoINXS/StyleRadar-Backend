package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.ImagenCatalogo;
import edu.dosw.proyecto.style_radar.model.entity.ImagenCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.ImagenCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;

@ExtendWith(MockitoExtension.class)
class ImagenCatalogoServiceImplTest {

    private static final String NIT = "900123456";
    private static final Long ITEM_ID = 10L;

    @Mock
    private AlmacenRepository almacenRepository;

    @Mock
    private ItemCatalogoRepository itemCatalogoRepository;

    @Mock
    private ImagenCatalogoRepository imagenCatalogoRepository;

    @InjectMocks
    private ImagenCatalogoServiceImpl imagenService;

    @Test
    void registrarShouldSaveValidImage() {
        // Arrange
        ItemCatalogoEntity item = item();
        byte[] datos = { 1, 2, 3 };
        prepararItem(item);
        when(imagenCatalogoRepository.save(any(ImagenCatalogoEntity.class))).thenAnswer(invocation -> {
            ImagenCatalogoEntity entity = invocation.getArgument(0);
            entity.setId(20L);
            return entity;
        });

        // Act
        ImagenCatalogo result = imagenService.registrar(NIT, ITEM_ID, "image/png", datos);

        // Assert
        ArgumentCaptor<ImagenCatalogoEntity> captor = ArgumentCaptor.forClass(ImagenCatalogoEntity.class);
        verify(imagenCatalogoRepository).save(captor.capture());
        assertThat(captor.getValue().getItemCatalogo()).isSameAs(item);
        assertThat(captor.getValue().getDatos()).hasSize(3);
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getContentType()).isEqualTo("image/png");
        assertThat(result.getItemCatalogoId()).isEqualTo(ITEM_ID);
    }

    @Test
    void registrarShouldThrowWhenItemDoesNotExist() {
        // Arrange
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(ITEM_ID, NIT)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> imagenService.registrar(NIT, ITEM_ID, "image/png", new byte[] { 1 }))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(imagenCatalogoRepository, never()).save(any());
    }

    @Test
    void registrarShouldRejectEmptyFile() {
        // Arrange
        prepararItem(item());

        // Act & Assert
        assertThatThrownBy(() -> imagenService.registrar(NIT, ITEM_ID, "image/png", new byte[0]))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(imagenCatalogoRepository, never()).save(any());
    }

    @Test
    void registrarShouldRejectNonImageContentType() {
        // Arrange
        prepararItem(item());

        // Act & Assert
        assertThatThrownBy(() -> imagenService.registrar(NIT, ITEM_ID, "application/pdf", new byte[] { 1 }))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(imagenCatalogoRepository, never()).save(any());
    }

    @Test
    void registrarShouldAllowMultipleImagesForSameItem() {
        // Arrange
        ItemCatalogoEntity item = item();
        AtomicLong ids = new AtomicLong(20L);
        prepararItem(item);
        when(imagenCatalogoRepository.save(any(ImagenCatalogoEntity.class))).thenAnswer(invocation -> {
            ImagenCatalogoEntity entity = invocation.getArgument(0);
            entity.setId(ids.getAndIncrement());
            return entity;
        });

        // Act
        ImagenCatalogo first = imagenService.registrar(NIT, ITEM_ID, "image/png", new byte[] { 1 });
        ImagenCatalogo second = imagenService.registrar(NIT, ITEM_ID, "image/jpeg", new byte[] { 2 });

        // Assert
        assertThat(first.getId()).isNotEqualTo(second.getId());
        verify(imagenCatalogoRepository, times(2)).save(any(ImagenCatalogoEntity.class));
    }

    private void prepararItem(ItemCatalogoEntity item) {
        when(almacenRepository.existsById(NIT)).thenReturn(true);
        when(itemCatalogoRepository.findByIdAndAlmacen_Nit(ITEM_ID, NIT)).thenReturn(Optional.of(item));
    }

    private ItemCatalogoEntity item() {
        ItemCatalogoEntity item = new ItemCatalogoEntity();
        item.setId(ITEM_ID);
        return item;
    }
}
