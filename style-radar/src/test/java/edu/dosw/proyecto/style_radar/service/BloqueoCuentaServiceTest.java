package edu.dosw.proyecto.style_radar.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;

class BloqueoCuentaServiceTest {
    @Test void ordenAscendenteSinDuplicadosNiRefreshEnCascada() {
        // Arrange
        var repo = mock(UsuarioRepository.class);
        var u = UsuarioEntity.builder().id(2L).build(); when(repo.findForUpdate(2L)).thenReturn(Optional.of(u));
        var service = new BloqueoCuentaService(repo);
        // Act
        var r = service.bloquear(List.of(3L, 2L, 1L, 2L));
        // Assert
        var orden = inOrder(repo); orden.verify(repo).findForUpdate(1L); orden.verify(repo).findForUpdate(2L);
        orden.verify(repo).findForUpdate(3L);
        assertThat(r).containsOnlyKeys(2L); verify(repo, times(1)).findForUpdate(2L);
    }
    @Test void sinIdsNoConsultaBD() {
        // Arrange
        var repo = mock(UsuarioRepository.class);
        // Act / Assert
        assertThat(new BloqueoCuentaService(repo).bloquear(List.of())).isEmpty(); verifyNoInteractions(repo);
    }
}
