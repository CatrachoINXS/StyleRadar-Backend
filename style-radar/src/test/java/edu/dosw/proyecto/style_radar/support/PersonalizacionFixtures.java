package edu.dosw.proyecto.style_radar.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.service.IPersonalizacionService;
import jakarta.persistence.EntityManager;

/** Fixtures persistidas: cada test hace rollback y relee después de limpiar el contexto. */
public abstract class PersonalizacionFixtures {
    protected static final Instant FECHA = Instant.parse("2026-10-01T12:00:00Z");
    @Autowired protected EntityManager em;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected ItemCatalogoRepository items;
    @Autowired protected IPersonalizacionService service;
    protected UsuarioEntity usuario;
    protected AlmacenEntity almacen;

    @BeforeEach
    void prepararCatalogo() {
        almacen = new AlmacenEntity("personalizacion", "Almacen", "Catalogo", "123", "store@example.co", new ArrayList<>());
        em.persist(almacen);
        usuario = usuario("comprador@example.co", Set.of(Estilo.FORMAL), Set.of(Talla.M));
    }

    protected UsuarioEntity usuario(String email, Set<Estilo> estilos, Set<Talla> tallas) {
        var u = UsuarioEntity.builder().nombre("Comprador").email(email).fechaRegistro(FECHA)
                .preferenciasEstilo(new HashSet<>(estilos)).tallasHabituales(new HashSet<>(tallas)).build();
        em.persist(u);
        return u;
    }

    protected void perfil(Set<Estilo> estilos, Set<Talla> tallas) {
        usuario.setPreferenciasEstilo(new HashSet<>(estilos));
        usuario.setTallasHabituales(new HashSet<>(tallas));
    }

    protected ItemCatalogoEntity item(String nombre, Estilo estilo, Talla talla, int unidades, long recencia) {
        var prenda = new PrendaEntity(null, nombre, "Algodon", TipoPrenda.SUPERIOR, "Radar", "Negro", estilo);
        em.persist(prenda);
        var i = new ItemCatalogoEntity(null, 100.0, EstadoItem.NUEVA_PRENDA, FECHA.plusSeconds(recencia),
                almacen, prenda, new ArrayList<>(), new ArrayList<>());
        em.persist(i);
        inventario(i, talla, unidades);
        return i;
    }

    protected void inventario(ItemCatalogoEntity item, Talla talla, int unidades) {
        var inventario = new InventarioTallaEntity(null, talla, unidades, item);
        item.getInventario().add(inventario);
        em.persist(inventario);
    }

    protected void busqueda(UsuarioEntity propietario, Consumer<BusquedaGuardadaEntity> configurar) {
        var b = BusquedaGuardadaEntity.builder().nombre("Privada").usuario(propietario).fechaCreacion(FECHA).build();
        configurar.accept(b);
        em.persist(b);
    }

    protected void releer() {
        em.flush();
        em.clear();
    }
}
