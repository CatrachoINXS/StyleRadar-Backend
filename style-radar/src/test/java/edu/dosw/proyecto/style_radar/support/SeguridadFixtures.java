package edu.dosw.proyecto.style_radar.support;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import jakarta.persistence.EntityManager;

/** Usuarios reales con credencial y JWT firmado, para pruebas HTTP con la cadena de seguridad completa. */
public abstract class SeguridadFixtures {
    @Autowired protected WebApplicationContext context;
    @Autowired protected JwtUtil jwt;
    @Autowired protected ICredencialService credenciales;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected EntityManager em;
    protected MockMvc mvc;
    private int secuencia;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    protected UsuarioEntity usuarioConRol(Rol... roles) {
        UsuarioEntity usuario = UsuarioEntity.builder().nombre("Usuario " + secuencia)
                .email("seguridad" + (secuencia++) + "-" + System.nanoTime() + "@example.co")
                .fechaRegistro(Instant.now()).roles(new HashSet<>(Set.of(roles))).build();
        em.persist(usuario);
        credenciales.crearCredencial(usuario.getId(), "fixture password segura");
        return usuario;
    }

    protected String bearer(UsuarioEntity usuario) {
        return "Bearer " + jwt.generate(usuario.getId());
    }

    protected String bearer(Rol rol) {
        return bearer(usuarioConRol(rol));
    }

    protected void releer() {
        em.flush();
        em.clear();
    }
}
