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
import edu.dosw.proyecto.style_radar.model.entity.UsuarioTallaHabitualEntity;
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
        var tallasPorTipo = tallas.stream()
                .map(t -> {
                    var th = UsuarioTallaHabitualEntity.builder()
                            .tipoPrenda(TipoPrenda.SUPERIOR)
                            .talla(t)
                            .build();
                    th.setUsuario(null); // se asigna por cascade al persistir el usuario
                    return th;
                })
                .collect(java.util.stream.Collectors.toSet());
        var u = UsuarioEntity.builder().nombre("Comprador").email(email).fechaRegistro(FECHA)
                .preferenciasEstilo(new HashSet<>(estilos))
                .tallasHabitualesPorTipo(tallasPorTipo).build();
        tallasPorTipo.forEach(th -> th.setUsuario(u));
        em.persist(u);
        return u;
    }

    protected void perfil(Set<Estilo> estilos, Set<Talla> tallas) {
        usuario.setPreferenciasEstilo(new HashSet<>(estilos));
        if (tallas == null) {
            usuario.getTallasHabitualesPorTipo().clear();
            return;
        }
        Set<Talla> actuales = usuario.getTallasHabitualesPorTipo().stream()
                .map(UsuarioTallaHabitualEntity::getTalla)
                .collect(java.util.stream.Collectors.toSet());
        usuario.getTallasHabitualesPorTipo().removeIf(th -> !tallas.contains(th.getTalla()));
        for (var t : tallas) {
            if (!actuales.contains(t)) {
                var th = new UsuarioTallaHabitualEntity();
                th.setUsuario(usuario);
                th.setTipoPrenda(TipoPrenda.SUPERIOR);
                th.setTalla(t);
                usuario.getTallasHabitualesPorTipo().add(th);
            }
        }
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
