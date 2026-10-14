package edu.dosw.proyecto.style_radar.service;

import java.util.List;
import org.springframework.data.domain.Page;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;

public interface ISuscripcionAlmacenService {
    List<PlanSuscripcion> planes();
    SuscripcionAlmacen crear(String nit, Long planId, UsuarioPrincipal actor);
    SuscripcionAlmacen vigente(String nit, UsuarioPrincipal actor);
    SuscripcionAlmacen cancelarRenovacion(String nit, Long id, Boolean renovacion, UsuarioPrincipal actor);
    Page<SuscripcionAlmacen> historial(String nit, int page, int size, UsuarioPrincipal actor);
}
