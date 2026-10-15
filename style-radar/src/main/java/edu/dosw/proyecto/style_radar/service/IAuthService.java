package edu.dosw.proyecto.style_radar.service;

import edu.dosw.proyecto.style_radar.model.domain.LoginResult;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;

public interface IAuthService {
    LoginResult login(String email, String password);
    Usuario registrarComprador(Usuario usuario, String password);
}
