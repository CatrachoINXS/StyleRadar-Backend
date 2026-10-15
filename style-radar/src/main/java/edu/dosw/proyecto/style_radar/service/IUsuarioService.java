package edu.dosw.proyecto.style_radar.service;

import java.util.List;
import java.util.Set;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;

public interface IUsuarioService {

    Usuario registrarUsuario(Usuario usuario);

    Usuario obtenerPorId(Long id);

    Usuario actualizarPreferenciasEstilo(Long id, Set<Estilo> preferencias);

    Usuario actualizarTallasHabituales(Long id, Set<Talla> tallas);

    BusquedaGuardada guardarBusqueda(Long usuarioId, BusquedaGuardada busqueda);

    List<BusquedaGuardada> obtenerBusquedasGuardadas(Long usuarioId);

    void eliminarBusquedaGuardada(Long usuarioId, Long busquedaId);
}
