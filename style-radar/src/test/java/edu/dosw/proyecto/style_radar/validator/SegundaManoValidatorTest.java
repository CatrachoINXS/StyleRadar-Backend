package edu.dosw.proyecto.style_radar.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.PublicacionSegundaManoRepository;

@ExtendWith(MockitoExtension.class)
class SegundaManoValidatorTest {

    @Mock
    private PublicacionSegundaManoRepository publicacionRepository;

    @InjectMocks
    private SegundaManoValidator validator;

    @Test
    void validarPrecioShouldThrowWhenNullOrNegative() {
        assertThatThrownBy(() -> validator.validarPrecio(null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("mayor o igual a 0");

        assertThatThrownBy(() -> validator.validarPrecio(-5.0))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("mayor o igual a 0");
    }

    @Test
    void validarPrecioShouldPassWhenValid() {
        assertThatCode(() -> validator.validarPrecio(0.0)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarPrecio(25000.0)).doesNotThrowAnyException();
    }

    @Test
    void validarYObtenerDeUsuarioShouldThrowWhenNotFound() {
        when(publicacionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validarYObtenerDeUsuario(10L, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No existe una publicación con ID: 10");
    }

    @Test
    void validarYObtenerDeUsuarioShouldThrowWhenUserMismatch() {
        UsuarioEntity owner = UsuarioEntity.builder().id(2L).build();
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .id(10L)
                .usuario(owner)
                .build();
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> validator.validarYObtenerDeUsuario(10L, 1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no pertenece al usuario");
    }

    @Test
    void validarYObtenerDeUsuarioShouldReturnEntityWhenValid() {
        UsuarioEntity owner = UsuarioEntity.builder().id(1L).build();
        PublicacionSegundaManoEntity entity = PublicacionSegundaManoEntity.builder()
                .id(10L)
                .usuario(owner)
                .build();
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(entity));

        PublicacionSegundaManoEntity result = validator.validarYObtenerDeUsuario(10L, 1L);
        assertThat(result).isSameAs(entity);
    }

    @Test
    void validarEditableShouldThrowWhenSoldOrRetired() {
        PublicacionSegundaManoEntity sold = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.VENDIDA)
                .build();
        PublicacionSegundaManoEntity retired = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.RETIRADA)
                .build();

        assertThatThrownBy(() -> validator.validarEditable(sold))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ya ha sido vendida");

        assertThatThrownBy(() -> validator.validarEditable(retired))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ha sido retirada");
    }

    @Test
    void validarEditableShouldPassWhenAvailable() {
        PublicacionSegundaManoEntity available = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();
        assertThatCode(() -> validator.validarEditable(available)).doesNotThrowAnyException();
    }

    @Test
    void validarTransicionAVendidaShouldThrowWhenAlreadySoldOrRetired() {
        PublicacionSegundaManoEntity sold = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.VENDIDA)
                .build();
        PublicacionSegundaManoEntity retired = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.RETIRADA)
                .build();

        assertThatThrownBy(() -> validator.validarTransicionAVendida(sold))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ya se encuentra marcada como vendida");

        assertThatThrownBy(() -> validator.validarTransicionAVendida(retired))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("No se puede marcar como vendida una publicación retirada");
    }

    @Test
    void validarTransicionAVendidaShouldPassWhenAvailable() {
        PublicacionSegundaManoEntity available = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();
        assertThatCode(() -> validator.validarTransicionAVendida(available)).doesNotThrowAnyException();
    }

    @Test
    void validarTransicionARetiradaShouldThrowWhenAlreadyRetired() {
        PublicacionSegundaManoEntity retired = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.RETIRADA)
                .build();

        assertThatThrownBy(() -> validator.validarTransicionARetirada(retired))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ya se encuentra retirada");
    }

    @Test
    void validarTransicionARetiradaShouldPassWhenAvailable() {
        PublicacionSegundaManoEntity available = PublicacionSegundaManoEntity.builder()
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();
        assertThatCode(() -> validator.validarTransicionARetirada(available)).doesNotThrowAnyException();
    }
}
