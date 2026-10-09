package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.*;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.*;
import edu.dosw.proyecto.style_radar.validator.AlmacenValidator;

@ExtendWith(MockitoExtension.class)
class AlmacenServiceImplTest {
    private static final Instant AHORA = Instant.parse("2026-10-09T12:00:00Z");
    @Mock AlmacenRepository almacenes;
    @Mock ItemCatalogoRepository items;
    @Mock AlmacenEntityMapper almacenMapper;
    @Mock ItemCatalogoEntityMapper itemMapper;
    @Mock DistanciaCalculator distancias;
    private AlmacenServiceImpl service;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new AlmacenServiceImpl(almacenes, items, almacenMapper, itemMapper,
                new EstadoItemCalculator(clock), distancias, new AlmacenValidator(), clock);
    }

    @Test
    void conservaPaginaCategoriaYTotalDelRepositorio() {
        // Arrange
        var page = PageRequest.of(0, 1);
        var entity = new AlmacenEntity();
        entity.setNit("A");
        var domain = new Almacen("A", "Radar", null, null, null);
        when(almacenes.findGeolocalizables(CategoriaAlmacen.CASUAL, page))
                .thenReturn(new PageImpl<>(List.of("A"), page, 5));
        when(almacenes.findByNitIn(List.of("A"))).thenReturn(List.of(entity));
        when(almacenMapper.toDomain(entity)).thenReturn(domain);
        // Act
        var result = service.consultar(CategoriaAlmacen.CASUAL, 0, 1);
        // Assert
        assertThat(result.getContent()).containsExactly(domain);
        assertThat(result.getTotalElements()).isEqualTo(5);
        verify(almacenes).findByNitIn(List.of("A"));
    }

    @Test
    void paginaVaciaNoEjecutaFetchYConservaTotal() {
        // Arrange
        var page = PageRequest.of(8, 1);
        when(almacenes.findGeolocalizables(null, page)).thenReturn(new PageImpl<>(List.of(), page, 3));
        // Act
        var result = service.consultar(null, 8, 1);
        // Assert
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(3);
        verify(almacenes, never()).findByNitIn(anyList());
    }

    @Test
    void resumenCargaSoloIdsSeleccionadosYCalculaEstadoDesdeInventario() {
        // Arrange
        var page = PageRequest.of(0, 5);
        var e1 = new ItemCatalogoEntity(); e1.setId(1L);
        var e2 = new ItemCatalogoEntity(); e2.setId(2L);
        var i1 = item(1L); var i2 = item(2L);
        when(almacenes.existsById("A")).thenReturn(true);
        when(items.findIdsPublicosDelAlmacen("A", null, null, page))
                .thenReturn(new PageImpl<>(List.of(1L, 2L), page, 2));
        when(items.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(e2, e1));
        when(itemMapper.toDomain(e1)).thenReturn(i1);
        when(itemMapper.toDomain(e2)).thenReturn(i2);
        // Act
        var result = service.resumen("A", 5);
        // Assert
        assertThat(result).containsExactly(i1, i2);
        assertThat(result).extracting(ItemCatalogo::getEstado).containsOnly(EstadoItem.ULTIMAS_UNIDADES);
        verify(items, never()).save(any());
    }

    @Test
    void novedadesTomanUnInstanteYEnvíanAmbosLimitesInclusivosAlRepositorio() {
        // Arrange
        var page = PageRequest.of(2, 10);
        Instant inicio = AHORA.minus(Duration.ofDays(7));
        when(almacenes.existsById("A")).thenReturn(true);
        when(items.findIdsPublicosDelAlmacen("A", inicio, AHORA, page))
                .thenReturn(new PageImpl<>(List.of(), page, 15));
        // Act
        var result = service.novedades("A", 2, 10);
        // Assert
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(15);
        verify(items).findIdsPublicosDelAlmacen("A", inicio, AHORA, page);
        verify(items, never()).findByIdIn(anyList());
    }

    @Test
    void resumenDeAlmacenInexistenteNoConsultaItems() {
        // Arrange
        when(almacenes.existsById("missing")).thenReturn(false);
        // Act
        var result = catchThrowable(() -> service.resumen("missing", 5));
        // Assert
        assertThat(result).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(items);
    }

    @Test
    void distanciaReutilizaCalculadorExistenteSinRedondearDominio() {
        // Arrange
        var entity = new AlmacenEntity();
        var almacen = new Almacen("A", "Radar", null, null, null, 4.0, -74.0, null);
        when(almacenes.findById("A")).thenReturn(Optional.of(entity));
        when(almacenMapper.toDomain(entity)).thenReturn(almacen);
        when(distancias.calcularKm(5, -75, 4, -74)).thenReturn(156.123456);
        // Act
        var result = service.distancia("A", 5, -75);
        // Assert
        assertThat(result.almacenNit()).isEqualTo("A");
        assertThat(result.distanciaKm()).isEqualTo(156.123456);
        verify(distancias).calcularKm(5, -75, 4, -74);
    }

    @Test
    void distanciaDeAlmacenInexistenteNoEjecutaCalculador() {
        // Arrange
        when(almacenes.findById("missing")).thenReturn(Optional.empty());
        // Act
        var result = catchThrowable(() -> service.distancia("missing", 0, 0));
        // Assert
        assertThat(result).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(distancias);
    }

    private ItemCatalogo item(long id) {
        return new ItemCatalogo(id, 100.0, EstadoItem.DISPONIBLE, AHORA, null, "A",
                List.of(new InventarioTalla(Talla.M, 2)));
    }
}
