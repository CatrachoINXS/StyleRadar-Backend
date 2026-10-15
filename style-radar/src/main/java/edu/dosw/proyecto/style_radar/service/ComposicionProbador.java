package edu.dosw.proyecto.style_radar.service;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;

/**
 * RF-25: composición aproximada (guía visual, no ajuste real) superponiendo la foto de la prenda
 * sobre la foto frontal del usuario, centrada y a la altura del torso o de las piernas.
 */
@Component
public class ComposicionProbador {

    /** Lado máximo de la foto base; acota memoria y tiempo (RNF-05: < 30 s). */
    static final int LADO_MAXIMO = 1600;
    static final double ANCHO_RELATIVO_PRENDA = 0.55;
    static final double ALTURA_TORSO = 0.22;
    static final double ALTURA_PIERNAS = 0.50;
    static final float OPACIDAD_PRENDA = 0.9f;

    /** @return la imagen decodificada, o null si los bytes no son una imagen soportada. */
    public BufferedImage leer(byte[] datos) {
        try {
            return datos == null ? null : ImageIO.read(new ByteArrayInputStream(datos));
        } catch (IOException exception) {
            return null;
        }
    }

    public byte[] componer(BufferedImage foto, BufferedImage prenda, TipoPrenda tipo) {
        BufferedImage base = acotar(foto);
        int anchoPrenda = (int) Math.round(base.getWidth() * ANCHO_RELATIVO_PRENDA);
        int altoPrenda = Math.max(1, (int) Math.round((double) prenda.getHeight() * anchoPrenda / prenda.getWidth()));
        int x = (base.getWidth() - anchoPrenda) / 2;
        int y = (int) Math.round(base.getHeight() * (tipo == TipoPrenda.INFERIOR ? ALTURA_PIERNAS : ALTURA_TORSO));

        BufferedImage resultado = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resultado.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(base, 0, 0, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, OPACIDAD_PRENDA));
            g.drawImage(prenda, x, y, anchoPrenda, altoPrenda, null);
        } finally {
            g.dispose();
        }
        return png(resultado);
    }

    private BufferedImage acotar(BufferedImage foto) {
        int lado = Math.max(foto.getWidth(), foto.getHeight());
        if (lado <= LADO_MAXIMO) {
            return foto;
        }
        double factor = (double) LADO_MAXIMO / lado;
        int ancho = (int) Math.round(foto.getWidth() * factor);
        int alto = (int) Math.round(foto.getHeight() * factor);
        BufferedImage reducida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = reducida.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(foto, 0, 0, ancho, alto, null);
        } finally {
            g.dispose();
        }
        return reducida;
    }

    private byte[] png(BufferedImage imagen) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("No fue posible codificar el resultado del probador", exception);
        }
    }
}
