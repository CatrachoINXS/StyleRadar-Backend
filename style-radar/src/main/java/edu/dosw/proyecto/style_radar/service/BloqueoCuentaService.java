package edu.dosw.proyecto.style_radar.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Bloqueos de filas sin joins y en orden estable, compartidos por RF66 y el detector. */
@Service @RequiredArgsConstructor
public class BloqueoCuentaService {
    private final UsuarioRepository usuarios;

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, UsuarioEntity> bloquear(Collection<Long> ids) {
        Map<Long, UsuarioEntity> resultado = new LinkedHashMap<>();
        for (Long id : new TreeSet<>(ids)) {
            usuarios.findForUpdate(id).ifPresent(usuario -> {
                resultado.put(id, usuario);
            });
        }
        return resultado;
    }
}
