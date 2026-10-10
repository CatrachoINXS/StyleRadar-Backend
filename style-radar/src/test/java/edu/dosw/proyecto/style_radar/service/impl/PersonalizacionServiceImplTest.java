package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.ItemCatalogoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.EstadoItemCalculator;

@ExtendWith(MockitoExtension.class)
class PersonalizacionServiceImplTest {
    @Mock UsuarioRepository usuarios;
    @Mock BusquedaGuardadaRepository busquedas;
    @Mock ItemCatalogoRepository items;
    @Mock ItemCatalogoEntityMapper mapper;
    private PersonalizacionServiceImpl service;
    private UsuarioEntity usuario;

    @BeforeEach
    void preparar() {
        service = new PersonalizacionServiceImpl(usuarios, busquedas, items, mapper,
                new EstadoItemCalculator(Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC)));
        usuario = UsuarioEntity.builder().id(7L).preferenciasEstilo(Set.of(Estilo.FORMAL)).tallasHabituales(Set.of(Talla.M)).build();
    }

    @Test
    void recuperaDetallesEnOrdenDeIdsYPreservaEstadoPersistido() {
        // Arrange
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(busquedas.findByUsuario_IdOrderByFechaCreacionDesc(7L)).thenReturn(List.of());
        var pagina = PageRequest.of(0, 2);
        when(items.findIdsFeed(Set.of(Estilo.FORMAL), Set.of(Talla.M), List.of(), pagina))
                .thenReturn(new PageImpl<>(List.of(2L, 1L), pagina, 5));
        var uno = new ItemCatalogoEntity(); uno.setId(1L); uno.setEstado(EstadoItem.AGOTADA);
        var dos = new ItemCatalogoEntity(); dos.setId(2L);
        when(items.findByIdIn(List.of(2L, 1L))).thenReturn(List.of(uno, dos));
        when(mapper.toDomain(uno)).thenReturn(dominio(1L, 1));
        when(mapper.toDomain(dos)).thenReturn(dominio(2L, 4));
        // Act
        var resultado = service.obtenerFeed(7L, 0, 2);
        // Assert
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getId).containsExactly(2L, 1L);
        assertThat(resultado.getTotalElements()).isEqualTo(5);
        assertThat(resultado.getContent()).extracting(ItemCatalogo::getEstado)
                .containsExactly(EstadoItem.DISPONIBLE, EstadoItem.ULTIMAS_UNIDADES);
        assertThat(uno.getEstado()).isEqualTo(EstadoItem.AGOTADA);
        verify(items, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"query", "tipo", "color", "talla", "precioMin", "precioMax", "marca", "estilo"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void conservaCadaCriterioEfectivoYDescartaVacios(String filtro) {
        // Arrange
        var b = new BusquedaGuardadaEntity();
        switch (filtro) {
            case "query" -> b.setQuery("  camisa  ");
            case "tipo" -> b.setTipo(TipoPrenda.SUPERIOR);
            case "color" -> b.setColor("  negro  ");
            case "talla" -> b.setTalla(Talla.M);
            case "precioMin" -> b.setPrecioMin(0.0);
            case "precioMax" -> b.setPrecioMax(100.0);
            case "marca" -> b.setMarca("  Radar  ");
            case "estilo" -> b.setEstilo(Estilo.FORMAL);
            default -> throw new AssertionError(filtro);
        }
        var vacia = new BusquedaGuardadaEntity(); vacia.setQuery(" "); vacia.setColor(""); vacia.setMarca("\t");
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(busquedas.findByUsuario_IdOrderByFechaCreacionDesc(7L)).thenReturn(List.of(vacia, b));
        when(items.findIdsFeed(anySet(), anySet(), anyList(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        // Act
        service.obtenerFeed(7L, 0, 20);
        // Assert
        ArgumentCaptor<List<BusquedaCatalogoCriteria>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(items).findIdsFeed(eq(Set.of(Estilo.FORMAL)), eq(Set.of(Talla.M)), captor.capture(), eq(PageRequest.of(0, 20)));
        assertThat(captor.getValue()).hasSize(1);
        var c = captor.getValue().getFirst();
        switch (filtro) {
            case "query" -> assertThat(c.getQ()).isEqualTo("camisa");
            case "tipo" -> assertThat(c.getTipo()).isEqualTo(TipoPrenda.SUPERIOR);
            case "color" -> assertThat(c.getColor()).isEqualTo("negro");
            case "talla" -> assertThat(c.getTalla()).isEqualTo(Talla.M);
            case "precioMin" -> assertThat(c.getPrecioMin()).isZero();
            case "precioMax" -> assertThat(c.getPrecioMax()).isEqualTo(100.0);
            case "marca" -> assertThat(c.getMarca()).isEqualTo("Radar");
            case "estilo" -> assertThat(c.getEstilo()).isEqualTo(Estilo.FORMAL);
            default -> throw new AssertionError(filtro);
        }
        verify(items, never()).findByIdIn(anyList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void paginasVaciasConservanCountSinCargarDetalles(boolean recomendaciones) {
        // Arrange
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        var pagina = new PageImpl<Long>(List.of(), PageRequest.of(2, 20), 25);
        if (recomendaciones) when(items.findIdsRecomendaciones(anySet(), anySet(), any())).thenReturn(pagina);
        else {
            when(busquedas.findByUsuario_IdOrderByFechaCreacionDesc(7L)).thenReturn(List.of());
            when(items.findIdsFeed(anySet(), anySet(), anyList(), any())).thenReturn(pagina);
        }
        // Act
        var resultado = recomendaciones ? service.obtenerRecomendaciones(7L, 2, 20).pagina() : service.obtenerFeed(7L, 2, 20);
        // Assert
        assertThat(resultado).isEmpty();
        assertThat(resultado.getTotalElements()).isEqualTo(25);
        assertThat(resultado.getNumber()).isEqualTo(2);
        verify(items, never()).findByIdIn(anyList());
        if (recomendaciones) verifyNoInteractions(busquedas);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usuarioInexistenteNoConsultaCatalogoNiActividad(boolean recomendaciones) {
        // Arrange
        when(usuarios.findById(7L)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> {
            if (recomendaciones) service.obtenerRecomendaciones(7L, 0, 20);
            else service.obtenerFeed(7L, 0, 20);
        }).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(busquedas, items, mapper);
    }

    @Test
    void falloDePersistenciaSePropagaSinConvertirseEnVacio() {
        // Arrange
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(items.findIdsRecomendaciones(anySet(), anySet(), any())).thenThrow(new DataAccessResourceFailureException("DB"));
        // Act & Assert
        assertThatThrownBy(() -> service.obtenerRecomendaciones(7L, 0, 20)).isInstanceOf(DataAccessResourceFailureException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void detalleEliminadoEntreConsultasNoProduceResultadoSilencioso() {
        // Arrange
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(items.findIdsRecomendaciones(anySet(), anySet(), any()))
                .thenReturn(new PageImpl<>(List.of(1L), PageRequest.of(0, 20), 1));
        when(items.findByIdIn(List.of(1L))).thenReturn(List.of());
        // Act & Assert
        assertThatThrownBy(() -> service.obtenerRecomendaciones(7L, 0, 20)).isInstanceOf(IllegalStateException.class);
    }

    private ItemCatalogo dominio(Long id, int stock) {
        return new ItemCatalogo(id, 100.0, EstadoItem.AGOTADA, Instant.parse("2026-09-01T00:00:00Z"),
                null, "store", List.of(new InventarioTalla(Talla.M, stock)));
    }
}
