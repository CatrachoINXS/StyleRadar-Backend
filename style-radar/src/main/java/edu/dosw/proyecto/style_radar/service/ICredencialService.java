package edu.dosw.proyecto.style_radar.service;

/** Capacidad interna; el aprovisionamiento y la autenticación HTTP se definen después. */
public interface ICredencialService {

    void crearCredencial(Long usuarioId, String password);

    boolean verificarPassword(Long usuarioId, String password);
}
