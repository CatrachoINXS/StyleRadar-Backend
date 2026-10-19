package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.exception.SolicitudPlaylistInvalidaException;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.support.PlaylistsPopularesFixtures;

@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"})
@ActiveProfiles("test") @Transactional
class PlaylistsPopularesJpaIntegrationTest extends PlaylistsPopularesFixtures {
    @Test void ordenDiezCincoCeroExcluyePrivadaYCount() {
        var cero = playlist(VisibilidadPlaylist.PUBLICA, 0, 50);
        var cinco = playlist(VisibilidadPlaylist.PUBLICA, 5, 20);
        var diez = playlist(VisibilidadPlaylist.PUBLICA, 10, 0);
        playlist(VisibilidadPlaylist.PRIVADA, 1000, 100); releer();
        var page = servicio.obtenerPopulares(0, 20);
        assertThat(page.getContent()).extracting(Playlist::getId).containsExactly(diez.getId(), cinco.getId(), cero.getId());
        assertThat(page.getContent()).extracting(Playlist::getLikesCount).containsExactly(10, 5, 0);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test void desempatesFechaDescEIdAscDeterministas() {
        var viejo = playlist(VisibilidadPlaylist.PUBLICA, 5, 0);
        var primero = playlist(VisibilidadPlaylist.PUBLICA, 5, 10);
        var segundo = playlist(VisibilidadPlaylist.PUBLICA, 5, 10); releer();
        assertThat(repository.findIdsPopulares(PageRequest.of(0, 20)).getContent())
                .containsExactly(primero.getId(), segundo.getId(), viejo.getId());
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).extracting(Playlist::getId)
                .containsExactly(primero.getId(), segundo.getId(), viejo.getId());
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 20, 100})
    void cuatroPublicasCienLikesCountIndependienteYPaginasSinDuplicados(int size) {
        var ids = new ArrayList<Long>();
        ids.add(playlist(VisibilidadPlaylist.PUBLICA, 100, 0).getId());
        for (int n = 0; n < 3; n++) ids.add(playlist(VisibilidadPlaylist.PUBLICA, 0, 0).getId());
        playlist(VisibilidadPlaylist.PRIVADA, 1000, 10); releer();
        var encontrados = new ArrayList<Long>();
        for (int page = 0; page < (4 + size - 1) / size; page++) {
            var resultado = servicio.obtenerPopulares(page, size);
            assertThat(resultado.getTotalElements()).isEqualTo(4);
            assertThat(resultado.getSize()).isEqualTo(size);
            resultado.forEach(p -> encontrados.add(p.getId()));
        }
        assertThat(encontrados).containsExactlyElementsOf(ids).doesNotHaveDuplicates();
        var fuera = servicio.obtenerPopulares(10, size);
        assertThat(fuera.getContent()).isEmpty(); assertThat(fuera.getTotalElements()).isEqualTo(4);
    }

    @Test void paginaCeroYUnoSizeDos() {
        var ids = new ArrayList<Long>();
        for (int n = 4; n > 0; n--) ids.add(playlist(VisibilidadPlaylist.PUBLICA, n, 0).getId()); releer();
        var p0 = servicio.obtenerPopulares(0, 2); var p1 = servicio.obtenerPopulares(1, 2);
        assertThat(p0.getContent()).extracting(Playlist::getId).containsExactlyElementsOf(ids.subList(0, 2));
        assertThat(p1.getContent()).extracting(Playlist::getId).containsExactlyElementsOf(ids.subList(2, 4));
        assertThat(p0.isFirst()).isTrue(); assertThat(p0.isLast()).isFalse();
        assertThat(p1.isFirst()).isFalse(); assertThat(p1.isLast()).isTrue();
        assertThat(p0.getTotalPages()).isEqualTo(2);
    }

    @Test void sinPublicasDevuelveVacioAunqueExistanPrivadas() {
        playlist(VisibilidadPlaylist.PRIVADA, 100, 0); releer();
        var p = servicio.obtenerPopulares(0, 20);
        assertThat(p.getContent()).isEmpty(); assertThat(p.getTotalElements()).isZero();
        assertThat(p.isFirst()).isTrue(); assertThat(p.isLast()).isTrue();
    }

    @Test void unaPublicaSinPrendasNiLikesParticipa() {
        var p = playlist(VisibilidadPlaylist.PUBLICA, 0, 0); releer();
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).singleElement().satisfies(x -> {
            assertThat(x.getId()).isEqualTo(p.getId()); assertThat(x.getPrendas()).isEmpty();
            assertThat(x.getTotalPrendas()).isZero(); assertThat(x.getLikesCount()).isZero();
        });
    }

    @Test void overflowOffsetDevuelveVacioConCountReal() {
        playlist(VisibilidadPlaylist.PUBLICA, 100, 0); releer();
        var p = servicio.obtenerPopulares(Integer.MAX_VALUE, 100);
        assertThat(p.getContent()).isEmpty(); assertThat(p.getTotalElements()).isEqualTo(1);
        assertThat(p.getNumber()).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,-1", "0,101"})
    void paginacionInvalidaRechazadaDesdeServicio(int page, int size) {
        assertThatThrownBy(() -> servicio.obtenerPopulares(page, size)).isInstanceOf(SolicitudPlaylistInvalidaException.class);
    }

    @Test void rankingReflejaLikeRetiroVisibilidadYEliminacion() {
        var a = playlist(VisibilidadPlaylist.PUBLICA, 0, 0);
        var b = playlist(VisibilidadPlaylist.PUBLICA, 0, 10); releer();
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).extracting(Playlist::getId).containsExactly(b.getId(), a.getId());
        servicio.alternarLike(usuario.getId(), a.getId()); releer();
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).extracting(Playlist::getId).containsExactly(a.getId(), b.getId());
        servicio.editar(usuario.getId(), a.getId(), "A", "Desc", VisibilidadPlaylist.PRIVADA, Estilo.FORMAL); releer();
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).extracting(Playlist::getId).containsExactly(b.getId());
        assertThat(servicio.obtenerPopulares(0, 20).getTotalElements()).isEqualTo(1);
        servicio.editar(usuario.getId(), a.getId(), "A", "Desc", VisibilidadPlaylist.PUBLICA, Estilo.FORMAL); releer();
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).extracting(Playlist::getId).containsExactly(a.getId(), b.getId());
        assertThat(servicio.obtenerPopulares(0, 20).getContent().getFirst().getLikesCount()).isEqualTo(1);
        servicio.alternarLike(usuario.getId(), a.getId()); releer();
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).extracting(Playlist::getId).containsExactly(b.getId(), a.getId());
        servicio.eliminar(usuario.getId(), b.getId()); releer();
        assertThat(servicio.obtenerPopulares(0, 20).getContent()).extracting(Playlist::getId).containsExactly(a.getId());
        assertThat(servicio.obtenerPopulares(0, 20).getTotalElements()).isEqualTo(1);
    }

    @Test void muchasPrendasYLikesCargaPorLotesSinProductoCartesianoNiNMasUno() {
        var prendas = new ArrayList<PrendaEntity>();
        for (int n = 0; n < 30; n++) {
            var p = new PrendaEntity(null, "Camisa " + n, "Datos", TipoPrenda.SUPERIOR, "Radar", "Azul", Estilo.CASUAL);
            em.persist(p); prendas.add(p);
        }
        for (int n = 0; n < 25; n++) {
            var p = playlist(VisibilidadPlaylist.PUBLICA, 100 + n, n);
            p.setUsuario(usuario("autor-" + n + "@example.co")); p.getPrendas().addAll(prendas);
        }
        releer(); var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
        var page = servicio.obtenerPopulares(0, 20);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getContent()).hasSize(20).allSatisfy(p -> {
            assertThat(p.getTotalPrendas()).isEqualTo(30); assertThat(p.getPrendas()).extracting(Prenda::getId).doesNotHaveDuplicates();
        });
        assertThat(page.getContent()).extracting(Playlist::getLikesCount).containsExactlyElementsOf(
                java.util.stream.IntStream.range(0, 20).map(n -> 124 - n).boxed().toList());
        // 5 consultas de RF72 + 1 lote de guardados (RF-71) + 2 consultas de disponibilidad (catálogo y segunda mano); constante, no crece con el número de playlists.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(8);
    }

    @Test void restriccionPersistenteRechazaMismoParDuplicado() {
        var p = playlist(VisibilidadPlaylist.PUBLICA, 1, 0); releer();
        assertThatThrownBy(() -> em.createNativeQuery("insert into playlist_likes (playlist_id, usuario_id) values (:p, 10000)")
                .setParameter("p", p.getId()).executeUpdate()).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }
}
