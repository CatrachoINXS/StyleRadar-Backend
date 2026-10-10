package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import edu.dosw.proyecto.style_radar.mapper.BusquedaGuardadaEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.UsuarioEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.entity.BusquedaGuardadaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.BusquedaGuardadaRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-09-08T10:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BusquedaGuardadaRepository busquedaGuardadaRepository;

    @Mock
    private UsuarioEntityMapper usuarioEntityMapper;

    @Mock
    private BusquedaGuardadaEntityMapper busquedaGuardadaEntityMapper;

    @Mock
    private UsuarioValidator usuarioValidator;

    private UsuarioServiceImpl usuarioService;

    @Mock private edu.dosw.proyecto.style_radar.service.DetectorDisponibilidad detector;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        usuarioService = new UsuarioServiceImpl(
                usuarioRepository,
                busquedaGuardadaRepository,
                usuarioEntityMapper,
                busquedaGuardadaEntityMapper,
                usuarioValidator,
                clock, detector);
    }

    @Test
    void registrarUsuarioShouldSaveAndReturnUser() {
        Usuario domain = Usuario.builder().nombre("Maria").email("maria@example.com").build();
        UsuarioEntity entity = UsuarioEntity.builder().nombre("Maria").email("maria@example.com").build();
        UsuarioEntity savedEntity = UsuarioEntity.builder().id(1L).nombre("Maria").email("maria@example.com").fechaRegistro(AHORA).build();
        Usuario savedDomain = Usuario.builder().id(1L).nombre("Maria").email("maria@example.com").fechaRegistro(AHORA).build();

        doNothing().when(usuarioValidator).validarEmailUnico("maria@example.com");
        when(usuarioEntityMapper.toEntity(domain)).thenReturn(entity);
        when(usuarioRepository.save(entity)).thenReturn(savedEntity);
        when(usuarioEntityMapper.toDomain(savedEntity)).thenReturn(savedDomain);

        Usuario result = usuarioService.registrarUsuario(domain);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(usuarioValidator).validarEmailUnico("maria@example.com");
        verify(usuarioRepository).save(entity);
    }

    @Test
    void registrarUsuarioShouldPropagateExceptionWhenEmailExists() {
        Usuario domain = Usuario.builder().nombre("Maria").email("maria@example.com").build();
        doThrow(new ReglaDeNegocioException("Email en uso")).when(usuarioValidator).validarEmailUnico("maria@example.com");

        assertThatThrownBy(() -> usuarioService.registrarUsuario(domain))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Email en uso");
    }

    @Test
    void obtenerPorIdShouldReturnUserWhenExists() {
        UsuarioEntity entity = UsuarioEntity.builder().id(1L).nombre("Maria").email("maria@example.com").build();
        Usuario domain = Usuario.builder().id(1L).nombre("Maria").email("maria@example.com").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(usuarioEntityMapper.toDomain(entity)).thenReturn(domain);

        Usuario result = usuarioService.obtenerPorId(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void obtenerPorIdShouldThrowWhenNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe un usuario con ID: 99");
    }

    @Test
    void actualizarPreferenciasEstiloShouldUpdateAndReturnUser() {
        UsuarioEntity entity = UsuarioEntity.builder().id(1L).build();
        UsuarioEntity savedEntity = UsuarioEntity.builder().id(1L).preferenciasEstilo(Set.of(Estilo.CASUAL)).build();
        Usuario savedDomain = Usuario.builder().id(1L).preferenciasEstilo(Set.of(Estilo.CASUAL)).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(usuarioRepository.save(entity)).thenReturn(savedEntity);
        when(usuarioEntityMapper.toDomain(savedEntity)).thenReturn(savedDomain);

        Usuario result = usuarioService.actualizarPreferenciasEstilo(1L, Set.of(Estilo.CASUAL));

        assertThat(result.getPreferenciasEstilo()).containsExactly(Estilo.CASUAL);
        verify(usuarioRepository).save(entity);
    }

    @Test
    void actualizarTallasHabitualesShouldUpdateAndReturnUser() {
        UsuarioEntity entity = UsuarioEntity.builder().id(1L).build();
        UsuarioEntity savedEntity = UsuarioEntity.builder().id(1L).tallasHabituales(Set.of(Talla.M, Talla.L)).build();
        Usuario savedDomain = Usuario.builder().id(1L).tallasHabituales(Set.of(Talla.M, Talla.L)).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(usuarioRepository.save(entity)).thenReturn(savedEntity);
        when(usuarioEntityMapper.toDomain(savedEntity)).thenReturn(savedDomain);

        Usuario result = usuarioService.actualizarTallasHabituales(1L, Set.of(Talla.M, Talla.L));

        assertThat(result.getTallasHabituales()).containsExactlyInAnyOrder(Talla.M, Talla.L);
        verify(usuarioRepository).save(entity);
    }

    @Test
    void guardarBusquedaShouldSaveAndReturnBusqueda() {
        UsuarioEntity usuario = UsuarioEntity.builder().id(1L).build();
        BusquedaGuardada domain = BusquedaGuardada.builder().nombre("Camisas").tipo(TipoPrenda.SUPERIOR).build();
        BusquedaGuardadaEntity entity = BusquedaGuardadaEntity.builder().nombre("Camisas").build();
        BusquedaGuardadaEntity savedEntity = BusquedaGuardadaEntity.builder().id(5L).nombre("Camisas").usuario(usuario).fechaCreacion(AHORA).build();
        BusquedaGuardada savedDomain = BusquedaGuardada.builder().id(5L).nombre("Camisas").usuarioId(1L).fechaCreacion(AHORA).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(busquedaGuardadaEntityMapper.toEntity(domain)).thenReturn(entity);
        when(busquedaGuardadaRepository.save(entity)).thenReturn(savedEntity);
        when(busquedaGuardadaEntityMapper.toDomain(savedEntity)).thenReturn(savedDomain);

        BusquedaGuardada result = usuarioService.guardarBusqueda(1L, domain);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getUsuarioId()).isEqualTo(1L);
        verify(busquedaGuardadaRepository).save(entity);
    }

    @Test
    void obtenerBusquedasGuardadasShouldReturnList() {
        BusquedaGuardadaEntity entity = BusquedaGuardadaEntity.builder().id(5L).build();
        BusquedaGuardada domain = BusquedaGuardada.builder().id(5L).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(busquedaGuardadaRepository.findByUsuario_IdOrderByFechaCreacionDesc(1L)).thenReturn(List.of(entity));
        when(busquedaGuardadaEntityMapper.toDomain(entity)).thenReturn(domain);

        List<BusquedaGuardada> result = usuarioService.obtenerBusquedasGuardadas(1L);

        assertThat(result).containsExactly(domain);
        verify(usuarioValidator).validarExiste(1L);
    }

    @Test
    void eliminarBusquedaGuardadaShouldDeleteWhenFound() {
        BusquedaGuardadaEntity entity = BusquedaGuardadaEntity.builder().id(5L).build();

        doNothing().when(usuarioValidator).validarExiste(1L);
        when(busquedaGuardadaRepository.findByIdAndUsuario_Id(5L, 1L)).thenReturn(Optional.of(entity));

        usuarioService.eliminarBusquedaGuardada(1L, 5L);

        verify(busquedaGuardadaRepository).delete(entity);
    }

    @Test
    void eliminarBusquedaGuardadaShouldThrowWhenNotFound() {
        doNothing().when(usuarioValidator).validarExiste(1L);
        when(busquedaGuardadaRepository.findByIdAndUsuario_Id(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.eliminarBusquedaGuardada(1L, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe la búsqueda guardada con ID: 5");
    }
}
