package edu.dosw.proyecto.style_radar.support;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.PlaylistRepository;
import edu.dosw.proyecto.style_radar.service.IPlaylistService;
import jakarta.persistence.EntityManager;

public abstract class PlaylistsPopularesFixtures {
    protected static final Instant FECHA = Instant.parse("2026-10-01T12:00:00Z");
    @Autowired protected EntityManager em;
    @Autowired protected IPlaylistService servicio;
    @Autowired protected PlaylistRepository repository;
    protected UsuarioEntity usuario;

    @BeforeEach void prepararPlaylists() {
        usuario = usuario("popular-" + UUID.randomUUID() + "@example.co");
    }

    protected UsuarioEntity usuario(String email) {
        var u = UsuarioEntity.builder().nombre("Comprador").email(email).fechaRegistro(FECHA).build();
        em.persist(u); return u;
    }

    protected PlaylistEntity playlist(VisibilidadPlaylist visibilidad, int likes, long recencia) {
        var p = PlaylistEntity.builder().usuario(usuario).nombre("Playlist " + UUID.randomUUID())
                .descripcion("Fixture").visibilidad(visibilidad).estilo(Estilo.FORMAL)
                .fechaCreacion(FECHA.plusSeconds(recencia))
                .likesUsuarios(LongStream.rangeClosed(10000, 9999 + likes).boxed().collect(Collectors.toCollection(HashSet::new)))
                .build();
        em.persist(p); return p;
    }

    protected PrendaEntity prenda(EstadoModeracion moderacion, EstadoVerificacionAlmacen verificacion) {
        var a = new AlmacenEntity("popular-" + UUID.randomUUID(), "Radar", "Datos", "123", "store@example.co", new ArrayList<>());
        a.setEstadoVerificacion(verificacion); em.persist(a);
        var p = new PrendaEntity(null, "Camisa", "Algodón", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL);
        em.persist(p);
        var i = new ItemCatalogoEntity(null, 100.0, EstadoItem.DISPONIBLE, FECHA, a, p, new ArrayList<>(), new ArrayList<>());
        i.setEstadoModeracion(moderacion); em.persist(i); return p;
    }

    protected void releer() { em.flush(); em.clear(); }
}
