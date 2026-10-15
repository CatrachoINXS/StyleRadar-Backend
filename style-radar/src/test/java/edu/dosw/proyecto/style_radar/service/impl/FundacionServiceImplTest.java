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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.ConflictoException;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.FundacionEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.FundacionEntityMapperImpl;
import edu.dosw.proyecto.style_radar.model.domain.Fundacion;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.FundacionRepository;
import edu.dosw.proyecto.style_radar.service.DistanciaCalculator;
import edu.dosw.proyecto.style_radar.validator.FundacionValidator;

@ExtendWith(MockitoExtension.class)
class FundacionServiceImplTest {

    static final Instant AHORA = Instant.parse("2026-10-12T15:00:00Z");

    @Mock FundacionRepository fundacionRepository;
    @Mock FundacionValidator fundacionValidator;
    @Spy FundacionEntityMapper fundacionEntityMapper = new FundacionEntityMapperImpl();
    @Spy DistanciaCalculator distanciaCalculator = new DistanciaCalculator();
    FundacionServiceImpl service;

    UsuarioEntity representante;

    @BeforeEach
    void setUp() {
        service = new FundacionServiceImpl(fundacionRepository, fundacionEntityMapper, fundacionValidator,
                distanciaCalculator, Clock.fixed(AHORA, ZoneOffset.UTC));
        representante = UsuarioEntity.builder().id(7L).roles(new HashSet<>()).build();
    }

    Fundacion datos(String nombre, Long representanteId) {
        return Fundacion.builder().nombre(nombre).direccion("Calle 1").email("f@example.co")
                .latitud(4.7).longitud(-74.0).categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(representanteId).build();
    }

    FundacionEntity entidad(Long id, boolean activa, double lat, double lon, Set<TipoPrenda> categorias) {
        return FundacionEntity.builder().id(id).nombre("F" + id).direccion("Calle").email("f@example.co")
                .latitud(lat).longitud(lon).categoriasAceptadas(new HashSet<>(categorias))
                .representante(representante).activa(activa).fechaRegistro(AHORA).build();
    }

    @Test
    void registrarCreaFundacionActivaYAsignaRolConservandoComprador() {
        // Arrange
        when(fundacionValidator.validarRepresentante(7L, null)).thenReturn(representante);
        when(fundacionRepository.save(any())).thenAnswer(inv -> {
            FundacionEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        // Act
        Fundacion registrada = service.registrar(datos("  Abrigo  ", 7L));

        // Assert
        assertThat(registrada.getId()).isEqualTo(1L);
        assertThat(registrada.getNombre()).isEqualTo("Abrigo");
        assertThat(registrada.isActiva()).isTrue();
        assertThat(registrada.getFechaRegistro()).isEqualTo(AHORA);
        assertThat(registrada.getRepresentanteId()).isEqualTo(7L);
        assertThat(representante.getRoles()).containsExactlyInAnyOrder(Rol.COMPRADOR, Rol.REPRESENTANTE_FUNDACION);
    }

    @Test
    void registrarConNombreDuplicadoLanzaConflicto() {
        // Arrange
        org.mockito.Mockito.doThrow(new ConflictoException("duplicado"))
                .when(fundacionValidator).validarNombreUnico("Abrigo", null);

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(datos("Abrigo", 7L))).isInstanceOf(ConflictoException.class);
        verify(fundacionRepository, never()).save(any());
    }

    @Test
    void editarFundacionInexistenteLanzaNoEncontrado() {
        // Arrange
        when(fundacionValidator.validarYObtener(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        // Act + Assert
        assertThatThrownBy(() -> service.editar(99L, datos("X", 7L))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarCambiaRepresentanteYTrasladaElRol() {
        // Arrange
        representante.getRoles().addAll(Set.of(Rol.COMPRADOR, Rol.REPRESENTANTE_FUNDACION));
        UsuarioEntity nuevo = UsuarioEntity.builder().id(8L).roles(new HashSet<>(Set.of(Rol.COMPRADOR))).build();
        FundacionEntity existente = entidad(1L, true, 4.7, -74.0, Set.of(TipoPrenda.SUPERIOR));
        when(fundacionValidator.validarYObtener(1L)).thenReturn(existente);
        when(fundacionValidator.validarRepresentante(8L, 1L)).thenReturn(nuevo);
        when(fundacionRepository.save(existente)).thenReturn(existente);

        // Act
        Fundacion editada = service.editar(1L, datos("Nuevo nombre", 8L));

        // Assert
        assertThat(editada.getNombre()).isEqualTo("Nuevo nombre");
        assertThat(editada.getRepresentanteId()).isEqualTo(8L);
        assertThat(representante.getRoles()).doesNotContain(Rol.REPRESENTANTE_FUNDACION);
        assertThat(nuevo.getRoles()).contains(Rol.REPRESENTANTE_FUNDACION);
    }

    @Test
    void desactivarFundacionYaInactivaLanzaEstadoInvalido() {
        // Arrange
        FundacionEntity inactiva = entidad(1L, false, 4.7, -74.0, Set.of(TipoPrenda.SUPERIOR));
        when(fundacionValidator.validarYObtener(1L)).thenReturn(inactiva);
        org.mockito.Mockito.doThrow(new ReglaDeNegocioException("ya inactiva")).when(fundacionValidator).validarActiva(inactiva);

        // Act + Assert
        assertThatThrownBy(() -> service.desactivar(1L)).isInstanceOf(ReglaDeNegocioException.class);
        verify(fundacionRepository, never()).save(any());
    }

    @Test
    void desactivarConservaElRegistro() {
        // Arrange
        FundacionEntity activa = entidad(1L, true, 4.7, -74.0, Set.of(TipoPrenda.SUPERIOR));
        when(fundacionValidator.validarYObtener(1L)).thenReturn(activa);
        when(fundacionRepository.save(activa)).thenReturn(activa);

        // Act
        Fundacion desactivada = service.desactivar(1L);

        // Assert
        assertThat(desactivada.isActiva()).isFalse();
        verify(fundacionRepository, never()).delete(any());
    }

    @Test
    void consultarCercanasOrdenaPorDistanciaYFiltraPorTipo() {
        // Arrange
        FundacionEntity lejana = entidad(1L, true, 6.25, -75.56, Set.of(TipoPrenda.SUPERIOR));
        FundacionEntity cercana = entidad(2L, true, 4.70, -74.04, Set.of(TipoPrenda.SUPERIOR));
        FundacionEntity otroTipo = entidad(3L, true, 4.70, -74.04, Set.of(TipoPrenda.CALZADO));
        when(fundacionRepository.findByActivaTrue()).thenReturn(List.of(lejana, cercana, otroTipo));

        // Act
        List<Fundacion> resultado = service.consultarCercanas(4.71, -74.05, TipoPrenda.SUPERIOR);

        // Assert
        assertThat(resultado).extracting(Fundacion::getId).containsExactly(2L, 1L);
        assertThat(resultado.get(0).getDistanciaKm()).isLessThan(resultado.get(1).getDistanciaKm());
    }

    @Test
    void consultarCercanasSinFundacionesActivasDevuelveListaVacia() {
        // Arrange
        when(fundacionRepository.findByActivaTrue()).thenReturn(List.of());

        // Act
        List<Fundacion> resultado = service.consultarCercanas(4.7, -74.0, null);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarTodasDevuelveTambienInactivas() {
        // Arrange
        when(fundacionRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(entidad(1L, true, 4, -74, Set.of(TipoPrenda.SUPERIOR)),
                        entidad(2L, false, 4, -74, Set.of(TipoPrenda.SUPERIOR))));

        // Act
        List<Fundacion> todas = service.listarTodas();

        // Assert
        assertThat(todas).extracting(Fundacion::isActiva).containsExactly(true, false);
    }
}
