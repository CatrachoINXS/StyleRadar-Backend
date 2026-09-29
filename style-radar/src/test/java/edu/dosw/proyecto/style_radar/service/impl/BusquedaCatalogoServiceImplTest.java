package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.InventarioTalla;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.service.DistanciaCalculator;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;

@ExtendWith(MockitoExtension.class)
class BusquedaCatalogoServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-09-28T12:00:00Z");

    @Mock
    private ItemCatalogoRepository itemCatalogoRepository;

    @Mock
    private ItemCatalogoEntityMapper itemCatalogoEntityMapper;

    private BusquedaCatalogoServiceImpl service;
    private BusquedaCatalogoCriteria criteria;

    @BeforeEach
    void setUp() {
        service = new BusquedaCatalogoServiceImpl(
                itemCatalogoRepository,
                itemCatalogoEntityMapper,
                new EstadoItemCalculator(Clock.fixed(AHORA, ZoneOffset.UTC)),
                new DistanciaCalculator());
        criteria = BusquedaCatalogoCriteria.builder().q("camiseta").build();
    }

    @Test
    void buscarShouldReturnResults() {
        ItemCatalogoEntity entity = new ItemCatalogoEntity();
        ItemCatalogo domain = item(1L, 4, AHORA.minusSeconds(8 * 24 * 60 * 60));
        when(itemCatalogoRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(itemCatalogoEntityMapper.toDomain(entity)).thenReturn(domain);

        Page<ItemCatalogo> result = service.buscar(criteria, 0, 20);

        assertThat(result.getContent()).containsExactly(domain);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void buscarShouldReturnEmptyPage() {
        when(itemCatalogoRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        Page<ItemCatalogo> result = service.buscar(criteria, 0, 20);

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void buscarShouldRespectPaginationAndStableSort() {
        when(itemCatalogoRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(2, 5)));

        Page<ItemCatalogo> result = service.buscar(criteria, 2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemCatalogoRepository).findAll(anySpecification(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").isAscending()).isTrue();
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
    }

    @Test
    void buscarShouldConvertEntitiesToDomain() {
        ItemCatalogoEntity firstEntity = new ItemCatalogoEntity();
        ItemCatalogoEntity secondEntity = new ItemCatalogoEntity();
        ItemCatalogo firstDomain = item(1L, 4, AHORA.minusSeconds(8 * 24 * 60 * 60));
        ItemCatalogo secondDomain = item(2L, 2, AHORA);
        when(itemCatalogoRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(firstEntity, secondEntity)));
        when(itemCatalogoEntityMapper.toDomain(firstEntity)).thenReturn(firstDomain);
        when(itemCatalogoEntityMapper.toDomain(secondEntity)).thenReturn(secondDomain);

        Page<ItemCatalogo> result = service.buscar(criteria, 0, 20);

        assertThat(result.getContent()).containsExactly(firstDomain, secondDomain);
        verify(itemCatalogoEntityMapper).toDomain(firstEntity);
        verify(itemCatalogoEntityMapper).toDomain(secondEntity);
    }

    @Test
    void buscarShouldApplyEffectiveStateBeforeReturning() {
        ItemCatalogoEntity entity = new ItemCatalogoEntity();
        ItemCatalogo domain = item(1L, 4, AHORA.minusSeconds(8 * 24 * 60 * 60));
        domain.setEstado(EstadoItem.NUEVA_PRENDA);
        when(itemCatalogoRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(itemCatalogoEntityMapper.toDomain(entity)).thenReturn(domain);

        ItemCatalogo result = service.buscar(criteria, 0, 20).getContent().getFirst();

        assertThat(result.getEstado()).isEqualTo(EstadoItem.DISPONIBLE);
    }

    private ItemCatalogo item(Long id, int stock, Instant fechaPublicacion) {
        return new ItemCatalogo(
                id,
                90000.0,
                EstadoItem.NUEVA_PRENDA,
                fechaPublicacion,
                null,
                "900123456",
                List.of(new InventarioTalla(Talla.M, stock)));
    }

    private Specification<ItemCatalogoEntity> anySpecification() {
        return ArgumentMatchers.any();
    }
}
