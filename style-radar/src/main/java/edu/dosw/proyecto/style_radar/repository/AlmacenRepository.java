package edu.dosw.proyecto.style_radar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import edu.dosw.proyecto.style_radar.model.domain.CategoriaAlmacen;

import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;

public interface AlmacenRepository extends JpaRepository<AlmacenEntity, String> {

    // MEMBER OF usa una subconsulta y no multiplica filas ni altera el count.
    @Query("""
            select a.nit from AlmacenEntity a
            where a.latitud between -90 and 90 and a.longitud between -180 and 180
            and (:categoria is null or :categoria member of a.categorias)
            order by a.nombreComercial asc, a.nit asc
            """)
    Page<String> findGeolocalizables(@Param("categoria") CategoriaAlmacen categoria, Pageable pageable);

    // Carga la colección solo después de paginar los identificadores en BD.
    @EntityGraph(attributePaths = "categorias")
    List<AlmacenEntity> findByNitIn(List<String> nits);
}
