package edu.dosw.proyecto.style_radar.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.model.domain.CategoriaAlmacen;
import edu.dosw.proyecto.style_radar.model.domain.EstadoItem;
import edu.dosw.proyecto.style_radar.model.domain.EstadoVerificacionAlmacen;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.domain.UnidadPeriodoSuscripcion;
import edu.dosw.proyecto.style_radar.model.entity.AdministradorAlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.AlmacenEntity;
import edu.dosw.proyecto.style_radar.model.entity.CredencialEntity;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.ImagenCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;
import edu.dosw.proyecto.style_radar.model.entity.ItemCatalogoEntity;
import edu.dosw.proyecto.style_radar.model.entity.PlanSuscripcionEntity;
import edu.dosw.proyecto.style_radar.model.entity.PrendaEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.AdministradorAlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.AlmacenRepository;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;
import edu.dosw.proyecto.style_radar.repository.FundacionRepository;
import edu.dosw.proyecto.style_radar.repository.ItemCatalogoRepository;
import edu.dosw.proyecto.style_radar.repository.PlanSuscripcionRepository;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "styleradar.demo.enabled", havingValue = "true")
public class DatosDemo implements ApplicationRunner {

    static final String NIT_PRINCIPAL = "900123456-1";

    private final UsuarioRepository usuarios;
    private final CredencialRepository credenciales;
    private final AlmacenRepository almacenes;
    private final PrendaRepository prendas;
    private final ItemCatalogoRepository items;
    private final PlanSuscripcionRepository planes;
    private final FundacionRepository fundaciones;
    private final AdministradorAlmacenRepository administradores;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${styleradar.demo.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.warn("Datos de demostración omitidos: falta STYLERADAR_DEMO_PASSWORD");
            return;
        }
        UsuarioEntity admin = usuario("Administrador StyleRadar", "admin@styleradar.co", Rol.ADMIN_STYLERADAR);
        usuario("Laura Gómez", "comprador@styleradar.co", Rol.COMPRADOR);
        usuario("Andrés Pérez", "comprador2@styleradar.co", Rol.COMPRADOR);
        UsuarioEntity adminAlmacen = usuario("Carlos Rojas", "almacen@styleradar.co", Rol.ADMIN_ALMACEN);
        UsuarioEntity representante = usuario("Marta Díaz", "fundacion@styleradar.co", Rol.REPRESENTANTE_FUNDACION);

        if (almacenes.existsById(NIT_PRINCIPAL)) {
            actualizarPrendas();
            return;
        }
        AlmacenEntity norte = almacen(NIT_PRINCIPAL, "Urbano Norte", "Moda urbana y casual en Usaquén",
                4.6946, -74.0310, 4.7, Set.of(CategoriaAlmacen.CASUAL, CategoriaAlmacen.DEPORTIVA));
        almacen("900234567-2", "Clásico Chapinero", "Ropa formal para oficina y eventos",
                4.6486, -74.0628, 4.4, Set.of(CategoriaAlmacen.FORMAL));
        almacen("900345678-3", "Retro Candelaria", "Prendas vintage y accesorios",
                4.5970, -74.0726, 4.2, Set.of(CategoriaAlmacen.VINTAGE, CategoriaAlmacen.ACCESORIOS));

        for (PrendaDemo p : PRENDAS) {
            item(almacenes.getReferenceById(p.nit()), p);
        }

        plan("Plan mensual", 1);
        plan("Plan trimestral", 3);
        plan("Plan anual", 12);

        AdministradorAlmacenEntity asociacion = new AdministradorAlmacenEntity();
        asociacion.setUsuario(adminAlmacen);
        asociacion.setAlmacen(norte);
        asociacion.setAsignadoPor(admin);
        asociacion.setFechaAsignacion(clock.instant());
        administradores.save(asociacion);

        if (!fundaciones.existsByRepresentante_Id(representante.getId())) {
            fundaciones.save(FundacionEntity.builder()
                    .nombre("Fundación Abrigo Bogotá")
                    .descripcion("Recibe ropa en buen estado para familias de Ciudad Bolívar")
                    .direccion("Calle 70 Sur # 18-20, Bogotá")
                    .telefono("6017654321")
                    .email("contacto@abrigobogota.org")
                    .latitud(4.5560).longitud(-74.1540)
                    .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.INFERIOR, TipoPrenda.CALZADO))
                    .representante(representante)
                    .activa(true)
                    .fechaRegistro(clock.instant())
                    .build());
        }
        log.info("Datos de demostración cargados: 3 almacenes, 10 prendas, 3 planes y 1 fundación");
    }

    private UsuarioEntity usuario(String nombre, String email, Rol rol) {
        return usuarios.findByEmail(email).orElseGet(() -> {
            UsuarioEntity usuario = usuarios.save(UsuarioEntity.builder()
                    .nombre(nombre)
                    .email(email)
                    .telefono("3001234567")
                    .fechaRegistro(clock.instant())
                    .roles(new HashSet<>(Set.of(rol)))
                    .build());
            credenciales.save(new CredencialEntity(usuario, passwordEncoder.encode(password)));
            return usuario;
        });
    }

    private AlmacenEntity almacen(String nit, String nombre, String descripcion, double latitud, double longitud,
            double reputacion, Set<CategoriaAlmacen> categorias) {
        AlmacenEntity almacen = new AlmacenEntity(nit, nombre, descripcion, "6013456789",
                "contacto@" + nombre.toLowerCase().replace(" ", "") + ".co", latitud, longitud, reputacion,
                new ArrayList<>(), new HashSet<>(categorias));
        almacen.setEstadoVerificacion(EstadoVerificacionAlmacen.APROBADO);
        almacen.setFechaDecisionVerificacion(clock.instant());
        return almacenes.save(almacen);
    }

    private void item(AlmacenEntity almacen, PrendaDemo p) {
        PrendaEntity prenda = prendas.save(new PrendaEntity(null, p.nombre(), p.descripcion(), p.tipo(), p.marca(),
                p.color(), p.estilo()));
        int stock = p.tallas().values().stream().mapToInt(Integer::intValue).sum();
        Instant publicacion = clock.instant().minus(Duration.ofDays(10));
        ItemCatalogoEntity item = new ItemCatalogoEntity(null, p.precio(),
                stock <= 3 ? EstadoItem.ULTIMAS_UNIDADES : EstadoItem.DISPONIBLE, publicacion, almacen, prenda,
                new ArrayList<>(), new ArrayList<>());
        p.tallas().forEach((talla, unidades) -> item.getInventario()
                .add(new InventarioTallaEntity(null, talla, unidades, item)));
        item.getImagenes().add(new ImagenCatalogoEntity(null, "image/jpeg", foto(p.foto()), item));
        items.save(item);
    }

    private void actualizarPrendas() {
        int actualizadas = 0;
        for (PrendaDemo p : PRENDAS) {
            for (ItemCatalogoEntity item : almacenes.getReferenceById(p.nit()).getItemsCatalogo()) {
                PrendaEntity prenda = item.getPrenda();
                boolean conFoto = !item.getImagenes().isEmpty()
                        && "image/jpeg".equals(item.getImagenes().get(0).getContentType());
                boolean esDemo = prenda.getNombre().equals(p.nombre()) || prenda.getNombre().equals(p.nombreAnterior());
                boolean actualizada = prenda.getNombre().equals(p.nombre()) && conFoto;
                if (!esDemo || actualizada) {
                    continue;
                }
                prenda.setNombre(p.nombre());
                prenda.setDescripcion(p.descripcion());
                prenda.setTipo(p.tipo());
                prenda.setMarca(p.marca());
                prenda.setColor(p.color());
                prenda.setEstilo(p.estilo());
                item.getImagenes().clear();
                item.getImagenes().add(new ImagenCatalogoEntity(null, "image/jpeg", foto(p.foto()), item));
                actualizadas++;
            }
        }
        log.info("Datos de demostración ya cargados; prendas actualizadas: {}", actualizadas);
    }

    private void plan(String nombre, int meses) {
        PlanSuscripcionEntity plan = new PlanSuscripcionEntity();
        plan.setNombre(nombre);
        plan.setPeriodo(meses);
        plan.setUnidadDuracion(UnidadPeriodoSuscripcion.MESES_CALENDARIO);
        plan.setActivo(true);
        planes.save(plan);
    }

    static byte[] foto(String nombre) {
        try (InputStream entrada = new ClassPathResource("demo/" + nombre + ".jpg").getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record PrendaDemo(String nit, String nombre, String nombreAnterior, String descripcion, TipoPrenda tipo,
            String marca, String color, Estilo estilo, double precio, Map<Talla, Integer> tallas, String foto) {
    }

    static final List<PrendaDemo> PRENDAS = List.of(
            new PrendaDemo(NIT_PRINCIPAL, "Camiseta blanca de algodón", "Camiseta básica blanca",
                    "Camiseta de algodón de cuello redondo", TipoPrenda.SUPERIOR, "Basicos Co", "Blanco",
                    Estilo.CASUAL, 39900, Map.of(Talla.S, 5, Talla.M, 8, Talla.L, 6), "camiseta-blanca-de-algodon"),
            new PrendaDemo(NIT_PRINCIPAL, "Jean ancho azul", "Jean slim azul", "Jean de tiro alto y bota ancha",
                    TipoPrenda.INFERIOR, "Denim BOG", "Azul", Estilo.URBANO, 119900, Map.of(Talla.M, 4, Talla.L, 3),
                    "jean-ancho-azul"),
            new PrendaDemo(NIT_PRINCIPAL, "Tenis running grises", "Tenis running negros",
                    "Tenis livianos para correr", TipoPrenda.CALZADO, "Ruta", "Gris", Estilo.DEPORTIVO, 189900,
                    Map.of(Talla.M, 2, Talla.L, 1), "tenis-running-grises"),
            new PrendaDemo(NIT_PRINCIPAL, "Hoodie gris jaspeado", "Buzo con capota gris",
                    "Buzo con capota en algodón perchado", TipoPrenda.SUPERIOR, "Basicos Co", "Gris", Estilo.URBANO,
                    99900, Map.of(Talla.S, 3, Talla.M, 6, Talla.XL, 4), "hoodie-gris-jaspeado"),
            new PrendaDemo("900234567-2", "Camisa negra de popelina", "Camisa formal celeste",
                    "Camisa de popelina manga larga", TipoPrenda.SUPERIOR, "Sastre", "Negro", Estilo.FORMAL, 89900,
                    Map.of(Talla.S, 4, Talla.M, 5, Talla.L, 5), "camisa-negra-de-popelina"),
            new PrendaDemo("900234567-2", "Pantalón negro recto", "Pantalón de vestir negro",
                    "Pantalón de vestir de corte recto", TipoPrenda.INFERIOR, "Sastre", "Negro", Estilo.FORMAL,
                    129900, Map.of(Talla.M, 6, Talla.L, 4), "pantalon-negro-recto"),
            new PrendaDemo("900234567-2", "Vestido largo negro", "Vestido midi rojo", "Vestido largo de noche",
                    TipoPrenda.VESTIDO, "Aura", "Negro", Estilo.FORMAL, 159900,
                    Map.of(Talla.XS, 2, Talla.S, 3, Talla.M, 2), "vestido-largo-negro"),
            new PrendaDemo("900345678-3", "Chaqueta de cuero negra", "Chaqueta de jean vintage",
                    "Chaqueta de cuero estilo motociclista", TipoPrenda.SUPERIOR, "Retro", "Negro", Estilo.CASUAL,
                    149900, Map.of(Talla.M, 2, Talla.L, 2), "chaqueta-de-cuero-negra"),
            new PrendaDemo("900345678-3", "Bolso hobo café", "Bolso de cuero café", "Bolso de gamuza para el hombro",
                    TipoPrenda.ACCESORIO, "Retro", "Café", Estilo.CASUAL, 79900, Map.of(Talla.M, 7),
                    "bolso-hobo-cafe"),
            new PrendaDemo("900345678-3", "Gorra deportiva negra", "Gorra deportiva verde",
                    "Gorra de algodón con correa ajustable", TipoPrenda.ACCESORIO, "Ruta", "Negro", Estilo.DEPORTIVO,
                    45900, Map.of(Talla.M, 9), "gorra-deportiva-negra"));
}
