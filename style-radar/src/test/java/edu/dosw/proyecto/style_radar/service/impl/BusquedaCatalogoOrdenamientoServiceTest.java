package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.InventarioTalla;
import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.OrdenCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.service.DistanciaCalculator;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;

@ExtendWith(MockitoExtension.class)
class BusquedaCatalogoOrdenamientoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-09-28T12:00:00Z");

    @Mock
    private ItemCatalogoRepository repository;

    @Mock
    private ItemCatalogoEntityMapper mapper;

    private BusquedaCatalogoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BusquedaCatalogoServiceImpl(
                repository,
                mapper,
                new EstadoItemCalculator(Clock.fixed(AHORA, ZoneOffset.UTC)),
                new DistanciaCalculator());
    }

    @Test
    void distanciaDebeOrdenarTresAlmacenesDelMasCercanoAlMasLejano() {
        ItemCatalogoEntity lejano = entity(1L, 0.0, 3.0, null);
        ItemCatalogoEntity cercano = entity(2L, 0.0, 1.0, null);
        ItemCatalogoEntity medio = entity(3L, 0.0, 2.0, null);
        preparar(List.of(lejano, cercano, medio));

        Page<ItemCatalogo> resultado = service.buscar(criteriaDistancia(), 0, 20);

        assertThat(ids(resultado)).containsExactly(2L, 3L, 1L);
    }

    @Test
    void distanciaDebeDesempatarPorItemIdAscendente() {
        ItemCatalogoEntity segundo = entity(2L, 1.0, 1.0, null);
        ItemCatalogoEntity primero = entity(1L, 1.0, 1.0, null);
        preparar(List.of(segundo, primero));

        Page<ItemCatalogo> resultado = service.buscar(criteriaDistancia(), 0, 20);

        assertThat(ids(resultado)).containsExactly(1L, 2L);
    }

    @Test
    void distanciaDebeUbicarAlmacenSinCoordenadasAlFinal() {
        ItemCatalogoEntity desconocido = entity(1L, null, null, null);
        ItemCatalogoEntity conocido = entity(2L, 0.0, 1.0, null);
        preparar(List.of(desconocido, conocido));

        Page<ItemCatalogo> resultado = service.buscar(criteriaDistancia(), 0, 20);

        assertThat(ids(resultado)).containsExactly(2L, 1L);
    }

    @Test
    void distanciaConTodosSinCoordenadasDebeOrdenarPorItemId() {
        ItemCatalogoEntity tercero = entity(3L, null, null, null);
        ItemCatalogoEntity primero = entity(1L, null, null, null);
        ItemCatalogoEntity segundo = entity(2L, null, null, null);
        preparar(List.of(tercero, primero, segundo));

        Page<ItemCatalogo> resultado = service.buscar(criteriaDistancia(), 0, 20);

        assertThat(ids(resultado)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void distanciaDebeOrdenarLosCandidatosYaFiltradosPorSpecification() {
        ItemCatalogoEntity candidatoFiltrado = entity(7L, 0.0, 1.0, null);
        preparar(List.of(candidatoFiltrado));
        BusquedaCatalogoCriteria criteria = BusquedaCatalogoCriteria.builder()
                .marca("adidas")
                .orden(OrdenCatalogo.DISTANCIA)
                .latitudUsuario(0.0)
                .longitudUsuario(0.0)
                .build();

        Page<ItemCatalogo> resultado = service.buscar(criteria, 0, 20);

        assertThat(ids(resultado)).containsExactly(7L);
        verify(repository).findAll(anySpecification());
    }

    @Test
    void distanciaDebePaginarDespuesDelOrdenGlobalYMapearSoloLaPagina() {
        ItemCatalogoEntity distanciaCinco = entity(50L, 0.0, 5.0, null);
        ItemCatalogoEntity distanciaUno = entity(10L, 0.0, 1.0, null);
        ItemCatalogoEntity distanciaCuatro = entity(40L, 0.0, 4.0, null);
        ItemCatalogoEntity distanciaDos = entity(20L, 0.0, 2.0, null);
        ItemCatalogoEntity distanciaTres = entity(30L, 0.0, 3.0, null);
        preparar(List.of(distanciaCinco, distanciaUno, distanciaCuatro, distanciaDos, distanciaTres));

        Page<ItemCatalogo> resultado = service.buscar(criteriaDistancia(), 0, 2);

        assertThat(ids(resultado)).containsExactly(10L, 20L);
        assertThat(resultado.getTotalElements()).isEqualTo(5);
        assertThat(resultado.getTotalPages()).isEqualTo(3);
        verify(mapper, never()).toDomain(distanciaCinco);
        verify(mapper, never()).toDomain(distanciaCuatro);
        verify(mapper, never()).toDomain(distanciaTres);
    }

    @Test
    void reputacionDebeOrdenarCincoAntesDeCuatroYCuatroAntesDeTres() {
        ItemCatalogoEntity tres = entity(1L, null, null, 3.0);
        ItemCatalogoEntity cinco = entity(2L, null, null, 5.0);
        ItemCatalogoEntity cuatro = entity(3L, null, null, 4.0);
        preparar(List.of(tres, cinco, cuatro));

        Page<ItemCatalogo> resultado = service.buscar(criteriaReputacion(), 0, 20);

        assertThat(ids(resultado)).containsExactly(2L, 3L, 1L);
    }

    @Test
    void reputacionDebeDesempatarPorItemIdAscendente() {
        ItemCatalogoEntity segundo = entity(2L, null, null, 4.0);
        ItemCatalogoEntity primero = entity(1L, null, null, 4.0);
        preparar(List.of(segundo, primero));

        Page<ItemCatalogo> resultado = service.buscar(criteriaReputacion(), 0, 20);

        assertThat(ids(resultado)).containsExactly(1L, 2L);
    }

    @Test
    void reputacionNullDebeQuedarAlFinal() {
        ItemCatalogoEntity desconocida = entity(1L, null, null, null);
        ItemCatalogoEntity conocida = entity(2L, null, null, 1.0);
        preparar(List.of(desconocida, conocida));

        Page<ItemCatalogo> resultado = service.buscar(criteriaReputacion(), 0, 20);

        assertThat(ids(resultado)).containsExactly(2L, 1L);
    }

    @Test
    void reputacionConTodosNullDebeOrdenarPorItemId() {
        ItemCatalogoEntity tercero = entity(3L, null, null, null);
        ItemCatalogoEntity primero = entity(1L, null, null, null);
        ItemCatalogoEntity segundo = entity(2L, null, null, null);
        preparar(List.of(tercero, primero, segundo));

        Page<ItemCatalogo> resultado = service.buscar(criteriaReputacion(), 0, 20);

        assertThat(ids(resultado)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void reputacionDebeOrdenarLosCandidatosYaFiltradosPorSpecification() {
        ItemCatalogoEntity candidatoFiltrado = entity(8L, null, null, 4.5);
        preparar(List.of(candidatoFiltrado));
        BusquedaCatalogoCriteria criteria = BusquedaCatalogoCriteria.builder()
                .estilo(edu.dosw.proyecto.style_radar.model.domain.Estilo.CASUAL)
                .orden(OrdenCatalogo.REPUTACION)
                .build();

        Page<ItemCatalogo> resultado = service.buscar(criteria, 0, 20);

        assertThat(ids(resultado)).containsExactly(8L);
        verify(repository).findAll(anySpecification());
    }

    @Test
    void reputacionDebePaginarDespuesDelOrdenGlobal() {
        ItemCatalogoEntity uno = entity(1L, null, null, 1.0);
        ItemCatalogoEntity cinco = entity(5L, null, null, 5.0);
        ItemCatalogoEntity dos = entity(2L, null, null, 2.0);
        ItemCatalogoEntity cuatro = entity(4L, null, null, 4.0);
        ItemCatalogoEntity tres = entity(3L, null, null, 3.0);
        preparar(List.of(uno, cinco, dos, cuatro, tres));

        Page<ItemCatalogo> resultado = service.buscar(criteriaReputacion(), 0, 2);

        assertThat(ids(resultado)).containsExactly(5L, 4L);
        assertThat(resultado.getTotalElements()).isEqualTo(5);
        assertThat(resultado.getTotalPages()).isEqualTo(3);
    }

    private void preparar(List<ItemCatalogoEntity> entities) {
        when(repository.findAll(anySpecification())).thenReturn(entities);
        for (ItemCatalogoEntity entity : entities) {
            lenient().when(mapper.toDomain(entity)).thenReturn(domain(entity.getId()));
        }
    }

    private ItemCatalogoEntity entity(Long id, Double latitud, Double longitud, Double reputacion) {
        AlmacenEntity almacen = new AlmacenEntity();
        almacen.setNit("NIT-" + id);
        almacen.setLatitud(latitud);
        almacen.setLongitud(longitud);
        almacen.setReputacion(reputacion);
        ItemCatalogoEntity entity = new ItemCatalogoEntity();
        entity.setId(id);
        entity.setAlmacen(almacen);
        return entity;
    }

    private ItemCatalogo domain(Long id) {
        return new ItemCatalogo(
                id,
                90000.0,
                EstadoItem.DISPONIBLE,
                AHORA.minusSeconds(8 * 24 * 60 * 60),
                null,
                "NIT-" + id,
                List.of(new InventarioTalla(Talla.M, 4)));
    }

    private BusquedaCatalogoCriteria criteriaDistancia() {
        return BusquedaCatalogoCriteria.builder()
                .orden(OrdenCatalogo.DISTANCIA)
                .latitudUsuario(0.0)
                .longitudUsuario(0.0)
                .build();
    }

    private BusquedaCatalogoCriteria criteriaReputacion() {
        return BusquedaCatalogoCriteria.builder()
                .orden(OrdenCatalogo.REPUTACION)
                .build();
    }

    private List<Long> ids(Page<ItemCatalogo> page) {
        return page.getContent().stream().map(ItemCatalogo::getId).toList();
    }

    private Specification<ItemCatalogoEntity> anySpecification() {
        return ArgumentMatchers.any();
    }
}
