package edu.dosw.proyecto.style_radar.support;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.impl.*;
import jakarta.persistence.EntityManager;

public abstract class SuscripcionesFixtures {
    protected static final Instant AHORA = Instant.parse("2024-01-31T14:15:16.123456Z");
    @Autowired protected EntityManager em;
    @Autowired protected SuscripcionAlmacenServiceImpl servicio;
    @Autowired protected AsociacionAdministradorAlmacenService asociaciones;
    @Autowired protected SuscripcionAlmacenRepository suscripciones;
    @Autowired protected PlanSuscripcionRepository planes;
    @Autowired protected AdministradorAlmacenRepository vinculos;
    @Autowired protected UsuarioRepository usuarios;
    protected String nit, ajeno;
    protected UsuarioEntity admin, tienda, otraTienda;
    protected PlanSuscripcionEntity plan, otroPlan;
    protected UsuarioPrincipal principal, administrador;

    protected void prepararFixtures() {
        nit = "rf67-" + UUID.randomUUID(); ajeno = nit + "-ajeno";
        admin = usuario(Rol.ADMIN_STYLERADAR); tienda = usuario(Rol.ADMIN_ALMACEN); otraTienda = usuario(Rol.ADMIN_ALMACEN);
        principal = new UsuarioPrincipal(tienda, ""); administrador = new UsuarioPrincipal(admin, "");
        almacen(nit); almacen(ajeno); plan = plan(1, true); otroPlan = plan(3, true);
        em.flush(); asociaciones.asociar(nit, tienda.getId(), administrador);
        asociaciones.asociar(ajeno, otraTienda.getId(), administrador); releer();
    }
    protected UsuarioEntity usuario(Rol rol) {
        var u = UsuarioEntity.builder().nombre("Fixture").email(UUID.randomUUID() + "@example.co")
                .fechaRegistro(AHORA).roles(Set.of(rol)).estadoCuenta(EstadoCuenta.ACTIVA).build();
        em.persist(u); return u;
    }
    protected AlmacenEntity almacen(String id) {
        var a = new AlmacenEntity(id, "Fixture", "Prueba", "123", "contacto@example.co", new ArrayList<>());
        a.setEstadoVerificacion(EstadoVerificacionAlmacen.APROBADO); em.persist(a); return a;
    }
    protected PlanSuscripcionEntity plan(int meses, boolean activo) {
        var p = new PlanSuscripcionEntity(); p.setNombre("Plan fixture " + meses); p.setPeriodo(meses);
        p.setUnidadDuracion(UnidadPeriodoSuscripcion.MESES_CALENDARIO); p.setActivo(activo); em.persist(p); return p;
    }
    protected SuscripcionAlmacenEntity fila(String idAlmacen, PlanSuscripcionEntity p, Instant inicio, Instant fin,
            EstadoSuscripcion estado) {
        var s = new SuscripcionAlmacenEntity(); s.setAlmacen(em.find(AlmacenEntity.class, idAlmacen)); s.setPlan(p);
        s.setEstadoSuscripcion(estado); s.setAlmacenActivoNit(estado == EstadoSuscripcion.ACTIVA ? idAlmacen : null);
        s.setFechaCreacion(inicio); s.setInicioPeriodo(inicio); s.setFinPeriodo(fin); s.setRenovacionHabilitada(true);
        em.persist(s); return s;
    }
    protected void releer() { em.flush(); em.clear(); }
}
