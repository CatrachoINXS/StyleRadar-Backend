package edu.dosw.proyecto.style_radar.service;

import java.util.List;

import edu.dosw.proyecto.style_radar.model.domain.Fundacion;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;

public interface IFundacionService {

    Fundacion registrar(Fundacion fundacion);

    Fundacion editar(Long fundacionId, Fundacion cambios);

    Fundacion desactivar(Long fundacionId);

    List<Fundacion> listarTodas();

    List<Fundacion> consultarCercanas(double latitud, double longitud, TipoPrenda tipo);
}
