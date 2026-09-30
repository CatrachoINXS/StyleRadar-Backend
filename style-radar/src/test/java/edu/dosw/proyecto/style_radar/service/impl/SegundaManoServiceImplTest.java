package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.mapper.PrendaEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.PublicacionSegundaManoEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import edu.dosw.proyecto.style_radar.repository.PublicacionSegundaManoRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.validator.SegundaManoValidator;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;

@ExtendWith(MockitoExtension.class)
class SegundaManoServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-09-08T10:00:00Z");

    @Mock
    private PublicacionSegundaManoRepository publicacionRepository;

    @Mock
    private PrendaRepository prendaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PublicacionSegundaManoEntityMapper publicacionEntityMapper;

    @Mock
    private PrendaEntityMapper prendaEntityMapper;

    @Mock
    private SegundaManoValidator segundaManoValidator;

    @Mock
    private UsuarioValidator usuarioValidator;

    private SegundaManoServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new SegundaManoServiceImpl(
                publicacionRepository,
                prendaRepository,
                usuarioRepository,
                publicacionEntityMapper,
                prendaEntityMapper,
                segundaManoValidator,
                usuarioValidator,
                clock);
    }

    @Test
    void publicarShouldSavePrendaAndPublicacion() {
        Prenda prenda = new Prenda(null, "Falda", "Seda", TipoPrenda.INFERIOR, "Zara", "Rojo", Estilo.FORMAL);
        PrendaEntity prendaEntity = new PrendaEntity(null, "Falda", "Seda", TipoPrenda.INFERIOR, "Zara", "Rojo", Estilo.FORMAL);
        PrendaEntity prendaGuardada = new PrendaEntity(1L, "Falda", "Seda", TipoPrenda.INFERIOR, "Zara", "Rojo", Estilo.FORMAL);
        UsuarioEntity usuario = UsuarioEntity.builder().id(2L).build();

        PublicacionSegundaManoEntity guardada = PublicacionSegundaManoEntity.builder()
                .id(10L)
                .usuario(usuario)
                .prenda(prendaGuardada)
                .talla(Talla.S)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(45000.0)
                .estado(EstadoPublicacion.DISPONIBLE)
                .fechaPublicacion(AHORA)
                .build();
        PublicacionSegundaMano domainResponse = PublicacionSegundaMano.builder()
                .id(10L)
                .usuarioId(2L)
                .prenda(prenda)
                .talla(Talla.S)
                .precio(45000.0)
                .build();

        doNothing().when(usuarioValidator).validarExiste(2L);
        doNothing().when(segundaManoValidator).validarPrecio(45000.0);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(prendaEntityMapper.toEntity(prenda)).thenReturn(prendaEntity);
        when(prendaRepository.save(prendaEntity)).thenReturn(prendaGuardada);
        when(publicacionRepository.save(any(PublicacionSegundaManoEntity.class))).thenReturn(guardada);
        when(publicacionEntityMapper.toDomain(guardada)).thenReturn(domainResponse);

        PublicacionSegundaMano result = service.publicar(
                2L, prenda, Talla.S, EstadoConservacion.COMO_NUEVO, 45000.0, List.of("http://foto.jpg"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(prendaRepository).save(prendaEntity);
        verify(publicacionRepository).save(any(PublicacionSegundaManoEntity.class));
    }

    @Test
    void obtenerPorIdShouldReturnWhenExists() {
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder().id(10L).build();
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).build();

        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(publicacionEntityMapper.toDomain(entity)).thenReturn(domain);

        PublicacionSegundaMano result = service.obtenerPorId(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void obtenerPorIdShouldThrowWhenNotFound() {
        when(publicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe publicación de segunda mano con ID: 99");
    }

    @Test
    void buscarShouldReturnPagedResults() {
        BusquedaSegundaManoCriteria criteria = BusquedaSegundaManoCriteria.builder().q("camisa").build();
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder().id(10L).build();
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).build();

        when(publicacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(publicacionEntityMapper.toDomain(entity)).thenReturn(domain);

        Page<PublicacionSegundaMano> result = service.buscar(criteria, 0, 20);

        assertThat(result.getContent()).containsExactly(domain);
    }

    @Test
    void obtenerPorUsuarioShouldReturnList() {
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder().id(10L).build();
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(publicacionRepository.findByUsuario_IdOrderByFechaPublicacionDesc(1L)).thenReturn(List.of(entity));
        when(publicacionEntityMapper.toDomain(entity)).thenReturn(domain);

        List<PublicacionSegundaMano> result = service.obtenerPorUsuario(1L);

        assertThat(result).containsExactly(domain);
    }

    @Test
    void editarShouldUpdatePrendaAndPublicacion() {
        Prenda prenda = new Prenda(1L, "Falda Larga", "Seda", TipoPrenda.INFERIOR, "Zara", "Rojo", Estilo.FORMAL);
        PrendaEntity prendaEntity = new PrendaEntity(1L, "Falda Larga", "Seda", TipoPrenda.INFERIOR, "Zara", "Rojo", Estilo.FORMAL);
        PrendaEntity prendaGuardada = new PrendaEntity(1L, "Falda Larga", "Seda", TipoPrenda.INFERIOR, "Zara", "Rojo", Estilo.FORMAL);
        PrendaEntity prendaOriginal = new PrendaEntity(1L, "Falda", "Seda", TipoPrenda.INFERIOR, "Zara", "Rojo", Estilo.FORMAL);

        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .id(10L)
                .prenda(prendaOriginal)
                .talla(Talla.S)
                .precio(40000.0)
                .build();
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder().id(10L).precio(50000.0).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        doNothing().when(segundaManoValidator).validarPrecio(50000.0);
        when(segundaManoValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);
        doNothing().when(segundaManoValidator).validarEditable(entity);

        when(prendaEntityMapper.toEntity(prenda)).thenReturn(prendaEntity);
        when(prendaRepository.save(prendaEntity)).thenReturn(prendaGuardada);
        when(publicacionRepository.save(entity)).thenReturn(entity);
        when(publicacionEntityMapper.toDomain(entity)).thenReturn(domain);

        PublicacionSegundaMano result = service.editar(
                1L, 10L, prenda, Talla.M, EstadoConservacion.BUEN_ESTADO, 50000.0, List.of("http://foto2.jpg"));

        assertThat(result.getId()).isEqualTo(10L);
        verify(prendaRepository).save(prendaEntity);
        verify(publicacionRepository).save(entity);
    }

    @Test
    void marcarComoVendidaShouldUpdateStatus() {
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .id(10L)
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder()
                .id(10L)
                .estado(EstadoPublicacion.VENDIDA)
                .build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(segundaManoValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);
        doNothing().when(segundaManoValidator).validarTransicionAVendida(entity);
        when(publicacionRepository.save(entity)).thenReturn(entity);
        when(publicacionEntityMapper.toDomain(entity)).thenReturn(domain);

        PublicacionSegundaMano result = service.marcarComoVendida(1L, 10L);

        assertThat(result.getEstado()).isEqualTo(EstadoPublicacion.VENDIDA);
        assertThat(entity.getEstado()).isEqualTo(EstadoPublicacion.VENDIDA);
        verify(publicacionRepository).save(entity);
    }

    @Test
    void retirarShouldUpdateStatusToRetirada() {
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .id(10L)
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(segundaManoValidator.validarYObtenerDeUsuario(10L, 1L)).thenReturn(entity);
        doNothing().when(segundaManoValidator).validarTransicionARetirada(entity);
        when(publicacionRepository.save(entity)).thenReturn(entity);

        service.retirar(1L, 10L);

        assertThat(entity.getEstado()).isEqualTo(EstadoPublicacion.RETIRADA);
        verify(publicacionRepository).save(entity);
    }
}
