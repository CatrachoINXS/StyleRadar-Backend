package edu.dosw.proyecto.style_radar.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaCatalogoCriteria;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.repository.specification.ItemCatalogoSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

/** Proyecta IDs: ningún fetch de colección participa en el LIMIT/OFFSET. */
@RequiredArgsConstructor
public class PersonalizacionCatalogoRepositoryImpl implements PersonalizacionCatalogoRepository {
    private final EntityManager entityManager;

    @Override
    public Page<Long> findIdsFeed(Set<Estilo> estilos, Set<Talla> tallas,
            List<BusquedaCatalogoCriteria> busquedas, Pageable pageable) {
        return consultar(estilos, tallas, busquedas, pageable, false);
    }

    @Override
    public Page<Long> findIdsRecomendaciones(Set<Estilo> estilos, Set<Talla> tallas, Pageable pageable) {
        return consultar(estilos, tallas, List.of(), pageable, true);
    }

    private Page<Long> consultar(Set<Estilo> estilos, Set<Talla> tallas,
            List<BusquedaCatalogoCriteria> busquedas, Pageable pageable, boolean recomendaciones) {
        var cb = entityManager.getCriteriaBuilder();
        var countQuery = cb.createQuery(Long.class);
        var countRoot = countQuery.from(ItemCatalogoEntity.class);
        countQuery.select(cb.count(countRoot)).where(elegible(countRoot, countQuery, cb, estilos, tallas, recomendaciones));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        // También evita desbordar el offset int de JPA para páginas muy grandes válidas.
        if (pageable.getOffset() >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        var query = cb.createQuery(Long.class);
        var root = query.from(ItemCatalogoEntity.class);
        query.select(root.get("id")).where(elegible(root, query, cb, estilos, tallas, recomendaciones));
        var fecha = cb.desc(root.get("fechaPublicacion"));
        var id = cb.asc(root.get("id"));
        if (recomendaciones) {
            query.orderBy(fecha, id);
        } else {
            var estilo = estilo(root, cb, estilos);
            var talla = tallas.isEmpty() ? cb.disjunction() : stock(root, query, cb, tallas);
            // Son categorías ordinales de la política aprobada, no un score de afinidad.
            var grupo = cb.<Integer>selectCase().when(cb.and(estilo, talla), 0)
                    .when(cb.or(estilo, talla), 1).otherwise(2);
            var actividad = cb.<Integer>selectCase().when(actividad(root, query, cb, busquedas), 0).otherwise(1);
            query.orderBy(cb.asc(grupo), cb.asc(actividad), fecha, id);
        }
        List<Long> ids = entityManager.createQuery(query).setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize()).getResultList();
        return new PageImpl<>(ids, pageable, total);
    }

    private Predicate elegible(Root<ItemCatalogoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            Set<Estilo> estilos, Set<Talla> tallas, boolean recomendaciones) {
        Predicate disponible = cb.and(stock(root, query, cb, Set.of()),
                edu.dosw.proyecto.style_radar.service.VisibilidadModeracion.publica().toPredicate(root, query, cb));
        if (!recomendaciones) {
            return disponible;
        }
        return cb.and(disponible,
                estilos.isEmpty() ? cb.conjunction() : estilo(root, cb, estilos),
                tallas.isEmpty() ? cb.conjunction() : stock(root, query, cb, tallas));
    }

    private Predicate estilo(Root<ItemCatalogoEntity> root, CriteriaBuilder cb, Set<Estilo> estilos) {
        return estilos.isEmpty() ? cb.disjunction() : root.get("prenda").get("estilo").in(estilos);
    }

    private Predicate stock(Root<ItemCatalogoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb, Set<Talla> tallas) {
        var subquery = query.subquery(Long.class);
        var inventario = subquery.from(InventarioTallaEntity.class);
        subquery.select(inventario.get("id")).where(cb.and(
                cb.equal(inventario.get("itemCatalogo"), root), cb.greaterThan(inventario.get("unidades"), 0),
                tallas.isEmpty() ? cb.conjunction() : inventario.get("talla").in(tallas)));
        return cb.exists(subquery);
    }

    private Predicate actividad(Root<ItemCatalogoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            List<BusquedaCatalogoCriteria> busquedas) {
        Predicate resultado = cb.disjunction();
        for (var criterios : busquedas) {
            // Los filtros son predicados sin joins de colección ni cambios al SELECT.
            resultado = cb.or(resultado,
                    ItemCatalogoSpecifications.coincidenciaExacta(criterios).toPredicate(root, query, cb));
        }
        return resultado;
    }
}
