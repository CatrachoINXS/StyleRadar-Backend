package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.proyecto.style_radar.mapper.PrendaEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;

@ExtendWith(MockitoExtension.class)
class PrendaServiceImplTest {

    @Mock
    private PrendaRepository prendaRepository;

    @Mock
    private PrendaEntityMapper prendaEntityMapper;

    @InjectMocks
    private PrendaServiceImpl prendaService;

    @Test
    void obtenerPrendasShouldReturnMappedRepositoryResults() {
        // Arrange
        PrendaEntity entity = new PrendaEntity(
                1L,
                "Camiseta",
                "Camiseta de algodón",
                TipoPrenda.SUPERIOR,
                "StyleRadar",
                "Negro",
                Estilo.CASUAL);
        Prenda domain = new Prenda(
                1L,
                "Camiseta",
                "Camiseta de algodón",
                TipoPrenda.SUPERIOR,
                "StyleRadar",
                "Negro",
                Estilo.CASUAL);
        when(prendaRepository.findAll()).thenReturn(List.of(entity));
        when(prendaEntityMapper.toDomain(entity)).thenReturn(domain);

        // Act
        List<Prenda> result = prendaService.obtenerPrendas();

        // Assert
        assertThat(result).containsExactly(domain);
        verify(prendaRepository).findAll();
        verify(prendaEntityMapper).toDomain(entity);
    }
}
