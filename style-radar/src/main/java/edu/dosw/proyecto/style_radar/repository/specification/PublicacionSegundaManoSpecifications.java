package edu.dosw.proyecto.style_radar.repository.specification;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import edu.dosw.proyecto.style_radar.model.domain.BusquedaSegundaManoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.EstadoConservacion;
import edu.dosw.proyecto.style_radar.model.domain.EstadoPublicacion;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.PublicacionSegundaManoEntity;
import jakarta.persistence.criteria.Path;

public final class PublicacionSegundaManoSpecifications {

    private PublicacionSegundaManoSpecifications() {
    }

    public static Specification<PublicacionSegundaManoEntity> conCriterios(BusquedaSegundaManoCriteria criteria) {
        return disponible()
                .and(tipo(criteria.getTipo()))
                .and(talla(criteria.getTalla()))
                .and(estadoConservacion(criteria.getEstadoConservacion()))
                .and(color(criteria.getColor()))
                .and(precioMinimo(criteria.getPrecioMin()))
                .and(precioMaximo(criteria.getPrecioMax()))
                .and(marca(criteria.getMarca()))
                .and(estilo(criteria.getEstilo()))
                .and(textoLibre(criteria.getQ()));
    }

    public static Specification<PublicacionSegundaManoEntity> disponible() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("estado"), EstadoPublicacion.DISPONIBLE);
    }

    public static Specification<PublicacionSegundaManoEntity> textoLibre(String texto) {
        return (root, query, criteriaBuilder) -> {
            if (texto == null || texto.isBlank()) {
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

    public static Specification<PublicacionSegundaManoEntity> tipo(TipoPrenda tipo) {
        return (root, query, criteriaBuilder) -> tipo == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("prenda").get("tipo"), tipo);
    }

    public static Specification<PublicacionSegundaManoEntity> talla(Talla talla) {
        return (root, query, criteriaBuilder) -> talla == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("talla"), talla);
    }

    public static Specification<PublicacionSegundaManoEntity> estadoConservacion(EstadoConservacion estadoConservacion) {
        return (root, query, criteriaBuilder) -> estadoConservacion == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("estadoConservacion"), estadoConservacion);
    }

    public static Specification<PublicacionSegundaManoEntity> color(String color) {
        return (root, query, criteriaBuilder) -> color == null || color.isBlank()
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("prenda").get("color")),
                        color.toLowerCase(Locale.ROOT));
    }

    public static Specification<PublicacionSegundaManoEntity> precioMinimo(Double precioMin) {
        return (root, query, criteriaBuilder) -> precioMin == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThanOrEqualTo(root.get("precio"), precioMin);
    }

    public static Specification<PublicacionSegundaManoEntity> precioMaximo(Double precioMax) {
        return (root, query, criteriaBuilder) -> precioMax == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.lessThanOrEqualTo(root.get("precio"), precioMax);
    }

    public static Specification<PublicacionSegundaManoEntity> marca(String marca) {
        return (root, query, criteriaBuilder) -> marca == null || marca.isBlank()
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("prenda").get("marca")),
                        marca.toLowerCase(Locale.ROOT));
    }

    public static Specification<PublicacionSegundaManoEntity> estilo(Estilo estilo) {
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
