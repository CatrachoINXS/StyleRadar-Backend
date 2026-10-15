package edu.dosw.proyecto.style_radar.service;

import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.ItemCatalogo;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;

public interface ICatalogoService {

    List<ItemCatalogo> obtenerCatalogo(String nit);

    ItemCatalogo publicar(String nit, Prenda prenda, Double precio);

    ItemCatalogo actualizar(String nit, Long itemId, Prenda prenda, Double precio);

    void retirar(String nit, Long itemId);
}
