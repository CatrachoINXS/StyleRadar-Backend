package edu.dosw.proyecto.style_radar.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiSmokeTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void apiDocsShouldExposeOnlyImplementedCatalogEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/prendas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/catalogo/buscar']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo/{itemId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo/{itemId}/tallas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo/{itemId}/inventario/{talla}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/almacenes/{nit}/catalogo/{itemId}/imagenes']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}/preferencias-estilo']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}/tallas-habituales']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}/busquedas-guardadas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}/busquedas-guardadas/{busquedaId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/segunda-mano']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/segunda-mano/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/segunda-mano']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/segunda-mano/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/segunda-mano/{id}/vender']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/playlists']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/playlists/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/playlists']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/playlists/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/playlists/{id}/prendas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{usuarioId}/playlists/{id}/prendas/{prendaId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/playlists/{id}/like']").exists());
    }
}
