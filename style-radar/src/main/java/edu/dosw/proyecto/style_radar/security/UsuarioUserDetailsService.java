package edu.dosw.proyecto.style_radar.security;

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    private final CredencialRepository credenciales;

    @Override
    @Transactional(readOnly = true)
    public UsuarioPrincipal loadUserByUsername(String email) {
        var matches = usuarios.findAllByEmailNormalizado(EmailNormalizer.normalize(email));
        // No elegir siquiera una coincidencia exacta si existe ambigüedad histórica.
        if (matches.size() != 1) {
            throw new UsernameNotFoundException("Autenticación inválida");
        }
        var usuario = matches.getFirst();
        var credencial = credenciales.findById(usuario.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Autenticación inválida"));
        return new UsuarioPrincipal(usuario, credencial.getPasswordHash());
    }

    @Transactional(readOnly = true)
    public UsuarioPrincipal loadActiveById(Long id) {
        var usuario = usuarios.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Autenticación inválida"));
        // El token no requiere releer el hash; identidad, estado y roles se consultan una vez.
        var principal = new UsuarioPrincipal(usuario, "");
        new AccountStatusUserDetailsChecker().check(principal);
        return principal;
    }
}
