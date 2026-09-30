package edu.dosw.proyecto.style_radar.service;

import java.util.List;

import org.springframework.data.domain.Page;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.domain.PublicacionSegundaMano;
import edu.dosw.proyecto.style_radar.model.domain.Talla;

public interface ISegundaManoService {

    PublicacionSegundaMano publicar(
            Long usuarioId,
            Prenda prenda,
            Talla talla,
            EstadoConservacion estadoConservacion,
            Double precio,
            List<String> fotos);

    PublicacionSegundaMano obtenerPorId(Long id);

    Page<PublicacionSegundaMano> buscar(BusquedaSegundaManoCriteria criteria, int page, int size);

    List<PublicacionSegundaMano> obtenerPorUsuario(Long usuarioId);

    PublicacionSegundaMano editar(
            Long usuarioId,
            Long publicacionId,
            Prenda prenda,
            Talla talla,
            EstadoConservacion estadoConservacion,
            Double precio,
            List<String> fotos);

    PublicacionSegundaMano marcarComoVendida(Long usuarioId, Long publicacionId);

    void retirar(Long usuarioId, Long publicacionId);
}
