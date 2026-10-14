package edu.dosw.proyecto.style_radar.service;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.entity.BusquedaGuardadaEntity;

/** Misma normalización para actividad del feed y alertas exactas. */
public final class CriteriosBusquedaGuardada {
    private CriteriosBusquedaGuardada() { }

    public static BusquedaCatalogoCriteria criterios(BusquedaGuardadaEntity b) {
        return BusquedaCatalogoCriteria.builder().q(texto(b.getQuery())).tipo(b.getTipo())
                .color(texto(b.getColor())).talla(b.getTalla()).precioMin(b.getPrecioMin())
                .precioMax(b.getPrecioMax()).marca(texto(b.getMarca())).estilo(b.getEstilo()).build();
    }

    public static boolean significativa(BusquedaCatalogoCriteria c) {
        return c.getQ() != null || c.getTipo() != null || c.getColor() != null || c.getTalla() != null
                || c.getPrecioMin() != null || c.getPrecioMax() != null || c.getMarca() != null || c.getEstilo() != null;
    }

    private static String texto(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
