package edu.dosw.proyecto.style_radar.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import edu.dosw.proyecto.style_radar.exception.RecursoNoEncontradoException;
import edu.dosw.proyecto.style_radar.security.JwtUtil;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.service.IPersonalizacionService;
import edu.dosw.proyecto.style_radar.support.PersonalizacionFixtures;

/** Mock de aplicación para simular errores tras autenticación real; no sustituye los filtros. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersonalizacionErroresHttpTest extends PersonalizacionFixtures {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    @Autowired ICredencialService credenciales;
    @MockitoBean IPersonalizacionService aplicacion;
    private MockMvc mvc;

    @BeforeEach
    void prepararHttp() {
        credenciales.crearCredencial(usuario.getId(), "fixture password");
        releer();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"feed", "recomendaciones"})
    void recursoInexistenteTrasAutenticacionValidaEs404(String ruta) throws Exception {
        // Arrange
        var error = new RecursoNoEncontradoException("No existe el usuario consultado");
        if (ruta.equals("feed")) when(aplicacion.obtenerFeed(usuario.getId(), 0, 20)).thenThrow(error);
        else when(aplicacion.obtenerRecomendaciones(usuario.getId(), 0, 20)).thenThrow(error);
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + jwt.generate(usuario.getId())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe el usuario consultado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"feed", "recomendaciones"})
    void falloDeBaseDeDatosEs500GenericoSinStacktrace(String ruta) throws Exception {
        // Arrange
        var error = new DataAccessResourceFailureException("Detalle privado de conexion");
        if (ruta.equals("feed")) when(aplicacion.obtenerFeed(usuario.getId(), 0, 20)).thenThrow(error);
        else when(aplicacion.obtenerRecomendaciones(usuario.getId(), 0, 20)).thenThrow(error);
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + jwt.generate(usuario.getId())))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Intente nuevamente más tarde."))
                .andExpect(jsonPath("$.trace").doesNotExist()).andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"feed", "recomendaciones"})
    void usuarioAusenteDuranteAutenticacionEs401(String ruta) throws Exception {
        // Arrange: ID sin cuenta; todavía no hay una consulta de aplicación válida.
        String token = jwt.generate(Long.MAX_VALUE);
        // Act & Assert
        mvc.perform(get("/api/v1/usuarios/me/" + ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
