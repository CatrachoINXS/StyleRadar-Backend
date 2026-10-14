package edu.dosw.proyecto.style_radar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.ImagenCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.support.SeguridadFixtures;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProbadorVirtualHttpIntegrationTest extends SeguridadFixtures {

    static final String FOTO = "/api/v1/usuarios/me/probador/foto";
    static final String PRUEBAS = "/api/v1/usuarios/me/probador/pruebas";

    String tComprador;
    AlmacenEntity almacen;

    @BeforeEach
    void preparar() {
        tComprador = bearer(Rol.COMPRADOR);
        almacen = new AlmacenEntity("probador", "Radar", "Catalogo", "123", "store@example.co", new ArrayList<>());
        em.persist(almacen);
    }

    static byte[] imagen(int ancho, int alto, String formato) throws Exception {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, ancho, alto);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, formato, out);
        return out.toByteArray();
    }

    Long item(TipoPrenda tipo, boolean conImagen, EstadoModeracion moderacion) throws Exception {
        PrendaEntity prenda = new PrendaEntity(null, "Camisa", "Algodón", tipo, "Radar", "Azul", Estilo.CASUAL);
        em.persist(prenda);
        ItemCatalogoEntity item = new ItemCatalogoEntity(null, 100.0, EstadoItem.DISPONIBLE, Instant.now(),
                almacen, prenda, new ArrayList<>(), new ArrayList<>());
        item.setEstadoModeracion(moderacion);
        em.persist(item);
        if (conImagen) {
            ImagenCatalogoEntity imagen = new ImagenCatalogoEntity(null, "image/png", imagen(200, 240, "png"), item);
            item.getImagenes().add(imagen);
            em.persist(imagen);
        }
        releer();
        return item.getId();
    }

    org.springframework.test.web.servlet.RequestBuilder subir(String token, String contentType, byte[] datos) {
        return multipart(FOTO).file(new MockMultipartFile("foto", "foto", contentType, datos)).header("Authorization", token);
    }

    MockHttpServletRequestBuilder probar(String token, Long itemId) {
        return post(PRUEBAS).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemCatalogoId\":" + itemId + "}");
    }

    @Test
    void flujoCargarProbarOtraPrendaYEliminar() throws Exception {
        mvc.perform(subir(tComprador, "image/jpeg", imagen(600, 900, "jpg")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ancho").value(600))
                .andExpect(jsonPath("$.alto").value(900));
        mvc.perform(get(FOTO).header("Authorization", tComprador)).andExpect(status().isOk());

        byte[] png = mvc.perform(probar(tComprador, item(TipoPrenda.SUPERIOR, true, EstadoModeracion.NO_REQUERIDA)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage resultado = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(resultado.getWidth()).isEqualTo(600);
        assertThat(resultado.getHeight()).isEqualTo(900);

        // RF-26: seleccionar otra prenda para una nueva prueba.
        mvc.perform(probar(tComprador, item(TipoPrenda.INFERIOR, true, EstadoModeracion.APROBADA))).andExpect(status().isOk());

        mvc.perform(delete(FOTO).header("Authorization", tComprador)).andExpect(status().isNoContent());
        mvc.perform(get(FOTO).header("Authorization", tComprador)).andExpect(status().isNotFound());
        mvc.perform(delete(FOTO).header("Authorization", tComprador)).andExpect(status().isNotFound());
    }

    @Test
    void cargarOtraFotoReemplazaLaAnterior() throws Exception {
        mvc.perform(subir(tComprador, "image/png", imagen(400, 500, "png"))).andExpect(status().isCreated());
        mvc.perform(subir(tComprador, "image/png", imagen(500, 700, "png"))).andExpect(status().isCreated());
        mvc.perform(get(FOTO).header("Authorization", tComprador)).andExpect(jsonPath("$.ancho").value(500));
    }

    @Test
    void fotoInvalida422() throws Exception {
        mvc.perform(subir(tComprador, "image/gif", imagen(400, 500, "gif"))).andExpect(status().isUnprocessableContent());
        mvc.perform(subir(tComprador, "image/png", "no es imagen".getBytes())).andExpect(status().isUnprocessableContent());
        mvc.perform(subir(tComprador, "image/png", imagen(100, 150, "png"))).andExpect(status().isUnprocessableContent());
        mvc.perform(subir(tComprador, "image/png", imagen(900, 600, "png"))).andExpect(status().isUnprocessableContent());
        mvc.perform(subir(tComprador, "image/png", new byte[0])).andExpect(status().isUnprocessableContent());
    }

    @Test
    void sinParteFoto400() throws Exception {
        mvc.perform(multipart(FOTO).header("Authorization", tComprador)).andExpect(status().isBadRequest());
    }

    @Test
    void probarSinFotoCargada404() throws Exception {
        mvc.perform(probar(tComprador, item(TipoPrenda.SUPERIOR, true, null))).andExpect(status().isNotFound());
    }

    @Test
    void prendaIncompatibleSinImagenRechazadaOInexistente() throws Exception {
        mvc.perform(subir(tComprador, "image/png", imagen(400, 600, "png"))).andExpect(status().isCreated());
        mvc.perform(probar(tComprador, item(TipoPrenda.CALZADO, true, null))).andExpect(status().isUnprocessableContent());
        mvc.perform(probar(tComprador, item(TipoPrenda.VESTIDO, false, null))).andExpect(status().isUnprocessableContent());
        mvc.perform(probar(tComprador, item(TipoPrenda.SUPERIOR, true, EstadoModeracion.RECHAZADA))).andExpect(status().isNotFound());
        mvc.perform(probar(tComprador, Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(post(PRUEBAS).header("Authorization", tComprador).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinJwt401YRolIncorrecto403() throws Exception {
        mvc.perform(get(FOTO)).andExpect(status().isUnauthorized());
        mvc.perform(post(PRUEBAS).contentType(MediaType.APPLICATION_JSON).content("{\"itemCatalogoId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(FOTO).header("Authorization", bearer(Rol.ADMIN_ALMACEN))).andExpect(status().isForbidden());
        mvc.perform(delete(FOTO).header("Authorization", bearer(Rol.REPRESENTANTE_FUNDACION))).andExpect(status().isForbidden());
    }
}
