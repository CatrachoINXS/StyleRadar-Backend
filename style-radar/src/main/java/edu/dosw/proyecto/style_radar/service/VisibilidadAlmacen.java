package edu.dosw.proyecto.style_radar.service;

import edu.dosw.proyecto.style_radar.model.domain.EstadoVerificacionAlmacen;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** NULL es compatibilidad provisional del legado, nunca una aprobación administrativa. */
public final class VisibilidadAlmacen {
    private VisibilidadAlmacen() { }

    private static final String APROBADO = "edu.dosw.proyecto.style_radar.model.domain.EstadoVerificacionAlmacen.APROBADO";
    public static final String JPQL = "(a.estadoVerificacion is null or a.estadoVerificacion = " + APROBADO + ")";
    public static final String JPQL_ITEM = "(i.almacen.estadoVerificacion is null or i.almacen.estadoVerificacion = " + APROBADO + ")";

    public static boolean visible(EstadoVerificacionAlmacen estado) {
        return estado == null || estado == EstadoVerificacionAlmacen.APROBADO;
    }

    public static Predicate publica(Path<?> almacen, CriteriaBuilder cb) {
        return cb.or(cb.isNull(almacen.get("estadoVerificacion")),
                cb.equal(almacen.get("estadoVerificacion"), EstadoVerificacionAlmacen.APROBADO));
    }

    public static Specification<ItemCatalogoEntity> publicaciones() {
        return (root, query, cb) -> publica(root.get("almacen"), cb);
    }
}
