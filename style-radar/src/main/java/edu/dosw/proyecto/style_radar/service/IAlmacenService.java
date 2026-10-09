package edu.dosw.proyecto.style_radar.service;

import java.util.List;
import org.springframework.data.domain.Page;
import edu.dosw.proyecto.style_radar.model.domain.*;

public interface IAlmacenService {
    Page<Almacen> consultar(CategoriaAlmacen categoria, int page, int size);
    List<ItemCatalogo> resumen(String nit, int limite);
    Page<ItemCatalogo> novedades(String nit, int page, int size);
    DistanciaAlmacen distancia(String nit, double latitudUsuario, double longitudUsuario);
}
