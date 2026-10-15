package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.ConflictoException;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.SolicitudDonacionEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoSolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.domain.SolicitudDonacion;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.SolicitudDonacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.SolicitudDonacionRepository;
import edu.dosw.proyecto.style_radar.validator.DonacionValidator;
import edu.dosw.proyecto.style_radar.validator.FundacionValidator;
import edu.dosw.proyecto.style_radar.validator.SegundaManoValidator;

@ExtendWith(MockitoExtension.class)
class DonacionServiceImplTest {

    static final Instant AHORA = Instant.parse("2026-10-12T15:00:00Z");

    @Mock SolicitudDonacionRepository solicitudRepository;
    @Mock SolicitudDonacionEntityMapper solicitudEntityMapper;
    @Mock DonacionValidator donacionValidator;
    @Mock FundacionValidator fundacionValidator;
    @Mock SegundaManoValidator segundaManoValidator;
    DonacionServiceImpl service;

    UsuarioEntity donante;
    UsuarioEntity representante;
    FundacionEntity fundacion;
    PublicacionSegundaManoEntity publicacion;

    @BeforeEach
    void setUp() {
        service = new DonacionServiceImpl(solicitudRepository, solicitudEntityMapper, donacionValidator,
                fundacionValidator, segundaManoValidator, Clock.fixed(AHORA, ZoneOffset.UTC));
        donante = UsuarioEntity.builder().id(1L).build();
        representante = UsuarioEntity.builder().id(2L).build();
        fundacion = FundacionEntity.builder().id(10L).nombre("Abrigo").activa(true)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR)).representante(representante).build();
        PrendaEntity prenda = new PrendaEntity(5L, "Chaqueta", "Abrigada", TipoPrenda.SUPERIOR, "Radar", "Negro", null);
        publicacion = PublicacionSegundaManoEntity.builder().id(20L).usuario(donante).prenda(prenda)
                .estado(EstadoPublicacion.EN_DONACION).build();
        lenient().when(solicitudEntityMapper.toDomain(any())).thenAnswer(inv -> {
            SolicitudDonacionEntity e = inv.getArgument(0);
            return SolicitudDonacion.builder().id(e.getId()).estado(e.getEstado())
                    .motivoRechazo(e.getMotivoRechazo()).puntoEntrega(e.getPuntoEntrega()).build();
        });
        lenient().when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    SolicitudDonacionEntity solicitud(EstadoSolicitudDonacion estado) {
        return SolicitudDonacionEntity.builder().id(30L).publicacion(publicacion).donante(donante)
                .fundacion(fundacion).estado(estado).fechaSolicitud(AHORA).build();
    }

    @Test
    void enviarSolicitudCreaPendienteConFechaDelReloj() {
        // Arrange
        when(segundaManoValidator.validarYObtenerDeUsuario(20L, 1L)).thenReturn(publicacion);
        when(fundacionValidator.validarYObtener(10L)).thenReturn(fundacion);

        // Act
        SolicitudDonacion creada = service.enviarSolicitud(1L, 20L, 10L, "Para el invierno");

        // Assert
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitudDonacion.PENDIENTE);
        verify(solicitudRepository).save(org.mockito.ArgumentMatchers.argThat(s ->
                s.getFechaSolicitud().equals(AHORA) && s.getFundacion() == fundacion && s.getDonante() == donante));
    }

    @Test
    void enviarSolicitudConFundacionInexistenteLanzaNoEncontrado() {
        // Arrange
        when(segundaManoValidator.validarYObtenerDeUsuario(20L, 1L)).thenReturn(publicacion);
        when(fundacionValidator.validarYObtener(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        // Act + Assert
        assertThatThrownBy(() -> service.enviarSolicitud(1L, 20L, 99L, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void enviarSolicitudConOtraEnCursoLanzaConflicto() {
        // Arrange
        when(segundaManoValidator.validarYObtenerDeUsuario(20L, 1L)).thenReturn(publicacion);
        when(fundacionValidator.validarYObtener(10L)).thenReturn(fundacion);
        doThrow(new ConflictoException("en curso")).when(donacionValidator).validarSinSolicitudEnCurso(20L);

        // Act + Assert
        assertThatThrownBy(() -> service.enviarSolicitud(1L, 20L, 10L, null)).isInstanceOf(ConflictoException.class);
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void enviarSolicitudConPrendaNoMarcadaLanzaEstadoInvalido() {
        // Arrange
        publicacion.setEstado(EstadoPublicacion.DISPONIBLE);
        when(segundaManoValidator.validarYObtenerDeUsuario(20L, 1L)).thenReturn(publicacion);
        doThrow(new ReglaDeNegocioException("no marcada")).when(donacionValidator).validarPublicacionEnDonacion(publicacion);

        // Act + Assert
        assertThatThrownBy(() -> service.enviarSolicitud(1L, 20L, 10L, null)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void consultarMisDonacionesSinSolicitudesDevuelveListaVacia() {
        // Arrange
        when(solicitudRepository.findByDonante_IdOrderByFechaSolicitudDescIdDesc(1L)).thenReturn(List.of());

        // Act
        List<SolicitudDonacion> resultado = service.consultarMisDonaciones(1L);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void consultarSolicitudesDeFundacionFiltraPorEstado() {
        // Arrange
        when(fundacionValidator.validarYObtenerDeRepresentante(2L)).thenReturn(fundacion);
        when(solicitudRepository.findByFundacion_IdAndEstadoOrderByFechaSolicitudDescIdDesc(10L, EstadoSolicitudDonacion.PENDIENTE))
                .thenReturn(List.of(solicitud(EstadoSolicitudDonacion.PENDIENTE)));

        // Act
        List<SolicitudDonacion> resultado = service.consultarSolicitudesDeFundacion(2L, EstadoSolicitudDonacion.PENDIENTE);

        // Assert
        assertThat(resultado).extracting(SolicitudDonacion::getEstado).containsExactly(EstadoSolicitudDonacion.PENDIENTE);
        verify(solicitudRepository, never()).findByFundacion_IdOrderByFechaSolicitudDescIdDesc(any());
    }

    @Test
    void decidirAceptaSinMotivoYRegistraFecha() {
        // Arrange
        SolicitudDonacionEntity pendiente = solicitud(EstadoSolicitudDonacion.PENDIENTE);
        when(fundacionValidator.validarYObtenerDeRepresentante(2L)).thenReturn(fundacion);
        when(donacionValidator.validarYObtenerDeFundacion(30L, 10L)).thenReturn(pendiente);

        // Act
        SolicitudDonacion decidida = service.decidir(2L, 30L, true, "ignorado");

        // Assert
        assertThat(decidida.getEstado()).isEqualTo(EstadoSolicitudDonacion.ACEPTADA);
        assertThat(pendiente.getMotivoRechazo()).isNull();
        assertThat(pendiente.getFechaDecision()).isEqualTo(AHORA);
    }

    @Test
    void decidirRechazaGuardandoMotivoYNoTocaLaPublicacion() {
        // Arrange
        SolicitudDonacionEntity pendiente = solicitud(EstadoSolicitudDonacion.PENDIENTE);
        when(fundacionValidator.validarYObtenerDeRepresentante(2L)).thenReturn(fundacion);
        when(donacionValidator.validarYObtenerDeFundacion(30L, 10L)).thenReturn(pendiente);

        // Act
        SolicitudDonacion decidida = service.decidir(2L, 30L, false, "  Sin espacio  ");

        // Assert
        assertThat(decidida.getEstado()).isEqualTo(EstadoSolicitudDonacion.RECHAZADA);
        assertThat(decidida.getMotivoRechazo()).isEqualTo("Sin espacio");
        assertThat(publicacion.getEstado()).isEqualTo(EstadoPublicacion.EN_DONACION);
    }

    @Test
    void decidirSolicitudNoPendienteLanzaEstadoInvalido() {
        // Arrange
        SolicitudDonacionEntity aceptada = solicitud(EstadoSolicitudDonacion.ACEPTADA);
        when(fundacionValidator.validarYObtenerDeRepresentante(2L)).thenReturn(fundacion);
        when(donacionValidator.validarYObtenerDeFundacion(30L, 10L)).thenReturn(aceptada);
        doThrow(new ReglaDeNegocioException("no pendiente")).when(donacionValidator)
                .validarEstado(aceptada, EstadoSolicitudDonacion.PENDIENTE, "decidir");

        // Act + Assert
        assertThatThrownBy(() -> service.decidir(2L, 30L, true, null)).isInstanceOf(ReglaDeNegocioException.class);
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void coordinarRecogidaGuardaPuntoYFranja() {
        // Arrange
        SolicitudDonacionEntity aceptada = solicitud(EstadoSolicitudDonacion.ACEPTADA);
        Instant inicio = AHORA.plusSeconds(3600);
        Instant fin = AHORA.plusSeconds(7200);
        when(donacionValidator.validarYObtenerDeParticipante(30L, 1L)).thenReturn(aceptada);

        // Act
        SolicitudDonacion coordinada = service.coordinarRecogida(1L, 30L, " Portería ", inicio, fin);

        // Assert
        assertThat(coordinada.getEstado()).isEqualTo(EstadoSolicitudDonacion.COORDINADA);
        assertThat(coordinada.getPuntoEntrega()).isEqualTo("Portería");
        assertThat(aceptada.getFranjaInicio()).isEqualTo(inicio);
        assertThat(aceptada.getFranjaFin()).isEqualTo(fin);
    }

    @Test
    void confirmarRecepcionMarcaRecibidaYPublicacionDonada() {
        // Arrange
        SolicitudDonacionEntity coordinada = solicitud(EstadoSolicitudDonacion.COORDINADA);
        when(fundacionValidator.validarYObtenerDeRepresentante(2L)).thenReturn(fundacion);
        when(donacionValidator.validarYObtenerDeFundacion(30L, 10L)).thenReturn(coordinada);

        // Act
        SolicitudDonacion recibida = service.confirmarRecepcion(2L, 30L);

        // Assert
        assertThat(recibida.getEstado()).isEqualTo(EstadoSolicitudDonacion.RECIBIDA);
        assertThat(coordinada.getFechaRecepcion()).isEqualTo(AHORA);
        assertThat(publicacion.getEstado()).isEqualTo(EstadoPublicacion.DONADA);
    }
}
