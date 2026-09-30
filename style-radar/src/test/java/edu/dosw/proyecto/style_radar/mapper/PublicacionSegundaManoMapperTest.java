package edu.dosw.proyecto.style_radar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.CrearPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.EditarPublicacionSegundaManoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.PrendaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PublicacionSegundaManoResponseDTO;

@SpringBootTest
@ActiveProfiles("test")
class PublicacionSegundaManoMapperTest {

    @Autowired
    private PublicacionSegundaManoMapper mapper;

    @Test
    void shouldMapCrearRequestToDomain() {
        PrendaRequestDTO prendaDto = new PrendaRequestDTO("Chaqueta Jean", "Vintage", TipoPrenda.SUPERIOR, "Levis", "Azul", Estilo.CASUAL);
        CrearPublicacionSegundaManoRequestDTO request = CrearPublicacionSegundaManoRequestDTO.builder()
                .usuarioId(1L)
                .prenda(prendaDto)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(65000.0)
                .fotos(List.of("http://foto1.jpg"))
                .build();

        PublicacionSegundaMano domain = mapper.toDomain(request);

        assertThat(domain).isNotNull();
        assertThat(domain.getUsuarioId()).isEqualTo(1L);
        assertThat(domain.getTalla()).isEqualTo(Talla.M);
        assertThat(domain.getEstadoConservacion()).isEqualTo(EstadoConservacion.COMO_NUEVO);
        assertThat(domain.getPrecio()).isEqualTo(65000.0);
        assertThat(domain.getFotos()).containsExactly("http://foto1.jpg");
        assertThat(domain.getPrenda().getNombre()).isEqualTo("Chaqueta Jean");
    }

    @Test
    void shouldMapEditarRequestToDomain() {
        PrendaRequestDTO prendaDto = new PrendaRequestDTO("Chaqueta Jean Modificada", "Vintage editada", TipoPrenda.SUPERIOR, "Levis", "Azul Claro", Estilo.CASUAL);
        EditarPublicacionSegundaManoRequestDTO request = EditarPublicacionSegundaManoRequestDTO.builder()
                .prenda(prendaDto)
                .talla(Talla.L)
                .estadoConservacion(EstadoConservacion.BUEN_ESTADO)
                .precio(70000.0)
                .fotos(List.of("http://foto2.jpg"))
                .build();

        PublicacionSegundaMano domain = mapper.toDomain(request);

        assertThat(domain).isNotNull();
        assertThat(domain.getTalla()).isEqualTo(Talla.L);
        assertThat(domain.getEstadoConservacion()).isEqualTo(EstadoConservacion.BUEN_ESTADO);
        assertThat(domain.getPrecio()).isEqualTo(70000.0);
        assertThat(domain.getFotos()).containsExactly("http://foto2.jpg");
    }

    @Test
    void shouldMapCriteriaAndPageResponse() {
        BusquedaSegundaManoRequestDTO request = BusquedaSegundaManoRequestDTO.builder()
                .q("jean")
                .tipo(TipoPrenda.SUPERIOR)
                .color("Azul")
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.BUEN_ESTADO)
                .precioMin(20000.0)
                .precioMax(100000.0)
                .marca("Levis")
                .estilo(Estilo.CASUAL)
                .build();

        BusquedaSegundaManoCriteria criteria = mapper.toCriteria(request);
        assertThat(criteria.getQ()).isEqualTo("jean");
        assertThat(criteria.getTipo()).isEqualTo(TipoPrenda.SUPERIOR);
        assertThat(criteria.getColor()).isEqualTo("Azul");

        Prenda prenda = new Prenda(1L, "Chaqueta Jean", "Vintage", TipoPrenda.SUPERIOR, "Levis", "Azul", Estilo.CASUAL);
        PublicacionSegundaMano domain = PublicacionSegundaMano.builder()
                .id(10L)
                .usuarioId(1L)
                .prenda(prenda)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.BUEN_ESTADO)
                .precio(60000.0)
                .estado(EstadoPublicacion.DISPONIBLE)
                .fechaPublicacion(Instant.parse("2026-09-01T10:00:00Z"))
                .fotos(List.of("http://foto1.jpg"))
                .build();

        Page<PublicacionSegundaMano> page = new PageImpl<>(List.of(domain), PageRequest.of(0, 10), 1);
        PageResponseDTO<PublicacionSegundaManoResponseDTO> pageResponse = mapper.toPageResponse(page);

        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.getTotalElements()).isEqualTo(1L);
        assertThat(pageResponse.getContent().getFirst().getId()).isEqualTo(10L);
    }
}
