package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.FotoProbador;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.FotoProbadorEntity;
import edu.dosw.proyecto.style_radar.model.entity.ImagenCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.FotoProbadorRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.ComposicionProbador;
import edu.dosw.proyecto.style_radar.validator.ProbadorValidator;
import edu.dosw.proyecto.style_radar.validator.UsuarioValidator;

@ExtendWith(MockitoExtension.class)
class ProbadorVirtualServiceImplTest {

    static final Instant AHORA = Instant.parse("2026-10-12T15:00:00Z");

    @Mock FotoProbadorRepository fotoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ProbadorValidator probadorValidator;
    @Mock UsuarioValidator usuarioValidator;
    @Spy ComposicionProbador composicion = new ComposicionProbador();
    ProbadorVirtualServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProbadorVirtualServiceImpl(fotoRepository, usuarioRepository, probadorValidator, usuarioValidator,
                composicion, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    static byte[] png(int ancho, int alto) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    FotoProbadorEntity foto(byte[] datos, int ancho, int alto) {
        return FotoProbadorEntity.builder().id(1L).contentType("image/png").datos(datos).ancho(ancho).alto(alto)
                .fechaCarga(AHORA).build();
    }

    ItemCatalogoEntity item(TipoPrenda tipo, byte[] imagen) {
        PrendaEntity prenda = new PrendaEntity(3L, "Camisa", "Algodón", tipo, "Radar", "Azul", null);
        ItemCatalogoEntity item = new ItemCatalogoEntity(9L, 100.0, null, AHORA, null, prenda, new ArrayList<>(), new ArrayList<>());
        item.getImagenes().add(new ImagenCatalogoEntity(1L, "image/png", imagen, item));
        return item;
    }

    @Test
    void cargarFotoNuevaGuardaDimensionesYFecha() throws Exception {
        // Arrange
        byte[] datos = png(400, 600);
        when(fotoRepository.findByUsuario_Id(5L)).thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(5L)).thenReturn(UsuarioEntity.builder().id(5L).build());
        when(fotoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        FotoProbador cargada = service.cargarFoto(5L, "image/png", datos);

        // Assert
        assertThat(cargada.getAncho()).isEqualTo(400);
        assertThat(cargada.getAlto()).isEqualTo(600);
        assertThat(cargada.getFechaCarga()).isEqualTo(AHORA);
    }

    @Test
    void cargarFotoInvalidaNoGuarda() {
        // Arrange
        doThrow(new ReglaDeNegocioException("formato")).when(probadorValidator).validarFoto(any(), any(), any());

        // Act + Assert
        assertThatThrownBy(() -> service.cargarFoto(5L, "image/gif", new byte[] {1}))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(fotoRepository, never()).save(any());
    }

    @Test
    void probarSinFotoLanzaNoEncontrado() {
        // Arrange
        when(probadorValidator.obtenerFoto(5L)).thenThrow(new RecursoNoEncontradoException("sin foto"));

        // Act + Assert
        assertThatThrownBy(() -> service.probar(5L, 9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void probarComponePngDelTamanoDeLaFoto() throws Exception {
        // Arrange
        when(probadorValidator.obtenerFoto(5L)).thenReturn(foto(png(400, 600), 400, 600));
        when(probadorValidator.obtenerPrendaCompatible(9L)).thenReturn(item(TipoPrenda.SUPERIOR, png(100, 120)));

        // Act
        byte[] resultado = service.probar(5L, 9L);

        // Assert
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(resultado));
        assertThat(imagen.getWidth()).isEqualTo(400);
        assertThat(imagen.getHeight()).isEqualTo(600);
    }

    @Test
    void probarConImagenDePrendaCorruptaLanzaEstadoInvalido() throws Exception {
        // Arrange
        when(probadorValidator.obtenerFoto(5L)).thenReturn(foto(png(400, 600), 400, 600));
        when(probadorValidator.obtenerPrendaCompatible(9L)).thenReturn(item(TipoPrenda.SUPERIOR, new byte[] {1, 2, 3}));

        // Act + Assert
        assertThatThrownBy(() -> service.probar(5L, 9L)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void probarConFotoGrandeLaReduceAlLadoMaximo() throws Exception {
        // Arrange
        when(probadorValidator.obtenerFoto(5L)).thenReturn(foto(png(1200, 2400), 1200, 2400));
        when(probadorValidator.obtenerPrendaCompatible(9L)).thenReturn(item(TipoPrenda.INFERIOR, png(80, 80)));

        // Act
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(service.probar(5L, 9L)));

        // Assert
        assertThat(List.of(imagen.getWidth(), imagen.getHeight())).containsExactly(800, 1600);
    }

    @Test
    void eliminarFotoBorraLaDelUsuario() {
        // Arrange
        FotoProbadorEntity existente = foto(new byte[] {1}, 400, 600);
        when(probadorValidator.obtenerFoto(5L)).thenReturn(existente);

        // Act
        service.eliminarFoto(5L);

        // Assert
        verify(fotoRepository).delete(existente);
    }
}
