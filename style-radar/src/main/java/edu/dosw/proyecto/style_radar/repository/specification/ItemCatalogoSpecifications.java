package edu.dosw.proyecto.style_radar.repository.specification;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;

public final class ItemCatalogoSpecifications {

    private ItemCatalogoSpecifications() {
    }

    public static Specification<ItemCatalogoEntity> conCriterios(BusquedaCatalogoCriteria criteria) {
        return conCriteriosSinTexto(criteria)
                .and(textoLibre(criteria.getQ()));
    }

    public static Specification<ItemCatalogoEntity> conCriteriosSinTexto(BusquedaCatalogoCriteria criteria) {
        return noAgotado()
                .and(tipo(criteria.getTipo()))
                .and(color(criteria.getColor()))
                .and(tallaDisponible(criteria.getTalla()))
                .and(precioMinimo(criteria.getPrecioMin()))
                .and(precioMaximo(criteria.getPrecioMax()))
                .and(marca(criteria.getMarca()))
                .and(estilo(criteria.getEstilo()));
    }

    public static Specification<ItemCatalogoEntity> noAgotado() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("estado"), EstadoItem.AGOTADA);
    }

    public static Specification<ItemCatalogoEntity> textoLibre(String texto) {
        return (root, query, criteriaBuilder) -> {
            if (texto == null) {
                return criteriaBuilder.conjunction();
            }
            String patron = "%" + escaparLike(texto.toLowerCase(Locale.ROOT)) + "%";
            Path<String> nombre = root.get("prenda").get("nombre");
            Path<String> descripcion = root.get("prenda").get("descripcion");
            Path<String> marca = root.get("prenda").get("marca");
            Path<String> color = root.get("prenda").get("color");
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(nombre), patron, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(descripcion), patron, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(marca), patron, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(color), patron, '\\'));
        };
    }

    public static Specification<ItemCatalogoEntity> tipo(TipoPrenda tipo) {
        return (root, query, criteriaBuilder) -> tipo == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("prenda").get("tipo"), tipo);
    }

    public static Specification<ItemCatalogoEntity> color(String color) {
        return (root, query, criteriaBuilder) -> color == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("prenda").get("color")),
                        color.toLowerCase(Locale.ROOT));
    }

    public static Specification<ItemCatalogoEntity> tallaDisponible(Talla talla) {
        return (root, query, criteriaBuilder) -> {
            if (talla == null) {
                return criteriaBuilder.conjunction();
            }
            Join<ItemCatalogoEntity, InventarioTallaEntity> inventario = root.join("inventario", JoinType.INNER);
            query.distinct(true);
            return criteriaBuilder.and(
                    criteriaBuilder.equal(inventario.get("talla"), talla),
                    criteriaBuilder.greaterThan(inventario.get("unidades"), 0));
        };
    }

    public static Specification<ItemCatalogoEntity> precioMinimo(Double precioMin) {
        return (root, query, criteriaBuilder) -> precioMin == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThanOrEqualTo(root.get("precio"), precioMin);
    }

    public static Specification<ItemCatalogoEntity> precioMaximo(Double precioMax) {
        return (root, query, criteriaBuilder) -> precioMax == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.lessThanOrEqualTo(root.get("precio"), precioMax);
    }

    public static Specification<ItemCatalogoEntity> marca(String marca) {
        return (root, query, criteriaBuilder) -> marca == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("prenda").get("marca")),
                        marca.toLowerCase(Locale.ROOT));
    }

    public static Specification<ItemCatalogoEntity> estilo(Estilo estilo) {
        return (root, query, criteriaBuilder) -> estilo == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("prenda").get("estilo"), estilo);
    }

    private static String escaparLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
