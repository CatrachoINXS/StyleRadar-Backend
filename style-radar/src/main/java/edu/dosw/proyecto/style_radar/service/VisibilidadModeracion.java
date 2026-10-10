package edu.dosw.proyecto.style_radar.service;

import edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import org.springframework.data.jpa.domain.Specification;

/** Una sola política para Criteria, JPQL y eventos internos. No implica stock. */
public final class VisibilidadModeracion {
    private VisibilidadModeracion() { }

    // Alias i compartido por las consultas JPQL públicas.
    public static final String JPQL = "(i.estadoModeracion is null or i.estadoModeracion in "
            + "(edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion.NO_REQUERIDA, "
            + "edu.dosw.proyecto.style_radar.model.domain.EstadoModeracion.APROBADA))";

    public static EstadoModeracion efectiva(EstadoModeracion estado) {
        return estado == null ? EstadoModeracion.NO_REQUERIDA : estado;
    }

    public static boolean visible(EstadoModeracion estado) {
        return efectiva(estado) == EstadoModeracion.NO_REQUERIDA || estado == EstadoModeracion.APROBADA;
    }

    public static Specification<ItemCatalogoEntity> publica() {
        return (root, query, cb) -> cb.or(cb.isNull(root.get("estadoModeracion")),
                root.get("estadoModeracion").in(EstadoModeracion.NO_REQUERIDA, EstadoModeracion.APROBADA));
    }
}
