package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.ChatEntityMapper;
import edu.dosw.proyecto.style_radar.mapper.ChatEntityMapperImpl;
import edu.dosw.proyecto.style_radar.model.domain.Conversacion;
import edu.dosw.proyecto.style_radar.model.domain.Mensaje;
import edu.dosw.proyecto.style_radar.model.entity.ConversacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.MensajeEntity;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.ConversacionRepository;
import edu.dosw.proyecto.style_radar.repository.MensajeRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.validator.ChatValidator;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    static final Instant AHORA = Instant.parse("2026-10-12T15:00:00Z");

    @Mock ConversacionRepository conversacionRepository;
    @Mock MensajeRepository mensajeRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ChatValidator chatValidator;
    @Spy ChatEntityMapper chatEntityMapper = new ChatEntityMapperImpl();
    ChatServiceImpl service;

    UsuarioEntity vendedor = UsuarioEntity.builder().id(1L).build();
    UsuarioEntity comprador = UsuarioEntity.builder().id(2L).build();
    PublicacionSegundaManoEntity publicacion = PublicacionSegundaManoEntity.builder().id(10L).usuario(vendedor).build();

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(conversacionRepository, mensajeRepository, usuarioRepository, chatEntityMapper,
                chatValidator, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    ConversacionEntity conversacion() {
        return ConversacionEntity.builder().id(20L).publicacion(publicacion).comprador(comprador).fechaCreacion(AHORA).build();
    }

    @Test
    void iniciarCreaConversacionNueva() {
        // Arrange
        when(chatValidator.validarPublicacionParaChat(10L, 2L)).thenReturn(publicacion);
        when(conversacionRepository.findByPublicacion_IdAndComprador_Id(10L, 2L)).thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(2L)).thenReturn(comprador);
        when(conversacionRepository.save(any())).thenAnswer(inv -> {
            ConversacionEntity c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });

        // Act
        Conversacion iniciada = service.iniciar(2L, 10L);

        // Assert
        assertThat(iniciada.getId()).isEqualTo(20L);
        assertThat(iniciada.getVendedorId()).isEqualTo(1L);
        assertThat(iniciada.getCompradorId()).isEqualTo(2L);
        assertThat(iniciada.getFechaCreacion()).isEqualTo(AHORA);
    }

    @Test
    void iniciarExistenteNoDuplica() {
        // Arrange
        when(chatValidator.validarPublicacionParaChat(10L, 2L)).thenReturn(publicacion);
        when(conversacionRepository.findByPublicacion_IdAndComprador_Id(10L, 2L)).thenReturn(Optional.of(conversacion()));

        // Act
        Conversacion existente = service.iniciar(2L, 10L);

        // Assert
        assertThat(existente.getId()).isEqualTo(20L);
        verify(conversacionRepository, never()).save(any());
    }

    @Test
    void iniciarSobrePublicacionNoDisponibleLanzaEstadoInvalido() {
        // Arrange
        when(chatValidator.validarPublicacionParaChat(10L, 2L)).thenThrow(new ReglaDeNegocioException("no disponible"));

        // Act + Assert
        assertThatThrownBy(() -> service.iniciar(2L, 10L)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void consultarMensajesDeConversacionAjenaLanzaNoEncontrado() {
        // Arrange
        when(chatValidator.validarYObtenerDeParticipante(20L, 99L)).thenThrow(new RecursoNoEncontradoException("ajena"));

        // Act + Assert
        assertThatThrownBy(() -> service.consultarMensajes(99L, 20L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(mensajeRepository, never()).findByConversacion_IdOrderByFechaEnvioAscIdAsc(any());
    }

    @Test
    void consultarMisConversacionesSinConversacionesDevuelveVacio() {
        // Arrange
        when(conversacionRepository.findDeParticipante(2L)).thenReturn(List.of());

        // Act + Assert
        assertThat(service.consultarMisConversaciones(2L)).isEmpty();
    }

    @Test
    void enviarMensajeRecortaTextoYUsaElReloj() {
        // Arrange
        ConversacionEntity conversacion = conversacion();
        when(chatValidator.validarYObtenerDeParticipante(20L, 1L)).thenReturn(conversacion);
        when(usuarioRepository.getReferenceById(1L)).thenReturn(vendedor);
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Mensaje mensaje = service.enviarMensaje(1L, 20L, "  Nos vemos a las 3  ");

        // Assert
        assertThat(mensaje.getTexto()).isEqualTo("Nos vemos a las 3");
        assertThat(mensaje.getAutorId()).isEqualTo(1L);
        assertThat(mensaje.getFechaEnvio()).isEqualTo(AHORA);
    }

    @Test
    void enviarMensajeEnChatCerradoNoGuarda() {
        // Arrange
        ConversacionEntity conversacion = conversacion();
        when(chatValidator.validarYObtenerDeParticipante(20L, 1L)).thenReturn(conversacion);
        doThrow(new ReglaDeNegocioException("cerrada")).when(chatValidator).validarAbierta(conversacion);

        // Act + Assert
        assertThatThrownBy(() -> service.enviarMensaje(1L, 20L, "Hola")).isInstanceOf(ReglaDeNegocioException.class);
        verify(mensajeRepository, never()).save(any(MensajeEntity.class));
    }
}
