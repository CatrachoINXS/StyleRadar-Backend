package edu.dosw.proyecto.style_radar.support;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.*;
import jakarta.persistence.EntityManager;

public abstract class AlertasFixtures {
    @Autowired protected EntityManager em;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected AlertaRepository alertas;
    @Autowired protected NotificacionRepository notificaciones;
    @Autowired protected DisponibilidadItemRepository disponibilidad;
    @Autowired protected BusquedaGuardadaRepository busquedas;
    @Autowired protected ItemCatalogoRepository items;
    @Autowired protected IAlertaService service;
    @Autowired protected ICatalogoService catalogo;
    @Autowired protected IInventarioService inventario;
    @Autowired protected IUsuarioService perfiles;
    @Autowired protected DetectorDisponibilidad detector;
    protected UsuarioEntity usuario;
    protected AlmacenEntity almacen;

    @BeforeEach
    void prepararAlertas() {
        almacen = new AlmacenEntity("alertas", "Radar", "Catalogo", "123", "store@example.co", new ArrayList<>());
        em.persist(almacen);
        usuario = usuario("alertas@example.co");
    }

    protected UsuarioEntity usuario(String email) {
        var u = UsuarioEntity.builder().nombre("Comprador").email(email).fechaRegistro(Instant.now()).build();
        em.persist(u);
        return u;
    }

    protected BusquedaGuardadaEntity busqueda(UsuarioEntity u, Consumer<BusquedaGuardadaEntity> configurar) {
        var b = BusquedaGuardadaEntity.builder().nombre("Privada").usuario(u).fechaCreacion(Instant.now()).build();
        configurar.accept(b);
        em.persist(b);
        return b;
    }

    protected Alerta alertaBusqueda(Consumer<BusquedaGuardadaEntity> configurar) {
        return service.crear(usuario.getId(), busqueda(usuario, configurar).getId(), null);
    }

    protected Long publicar() {
        var p = new Prenda(null, "Camisa especial", "Algodon", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL);
        Long id = catalogo.publicar(almacen.getNit(), p, 100.0).getId();
        inventario.registrarTallas(almacen.getNit(), id, Set.of(Talla.M, Talla.L));
        return id;
    }

    protected Long antiguo(int unidades) {
        var p = new PrendaEntity(null, "Antigua", "Algodon", TipoPrenda.SUPERIOR, "Radar", "Negro", Estilo.FORMAL);
        em.persist(p);
        var i = new ItemCatalogoEntity(null, 100.0, EstadoItem.AGOTADA, Instant.now().minusSeconds(3600),
                almacen, p, new ArrayList<>(), new ArrayList<>());
        em.persist(i);
        var v = new InventarioTallaEntity(null, Talla.M, unidades, i);
        i.getInventario().add(v);
        em.persist(v);
        return i.getId();
    }

    protected void stock(Long id, int unidades) { stock(id, Talla.M, unidades); }
    protected void stock(Long id, Talla talla, int unidades) {
        inventario.actualizarDisponibilidad(almacen.getNit(), id, talla, unidades);
    }
    protected void releer() { em.flush(); em.clear(); }
    protected List<Notificacion> eventos() { return service.notificaciones(usuario.getId(), 0, 100).getContent(); }
}
