# Sprint 3 — Etapa 3: geolocalización y descubrimiento

Alcance exclusivo: SR-RF-17, SR-RF-18, SR-RF-19, SR-RF-20 y SR-RF-21. APIs REST de lectura pública; sin frontend, mapas, navegación ni proveedores externos.

## Contratos

| GET | Parámetros | Respuesta |
| --- | --- | --- |
| `/api/v1/almacenes` | `categoria` opcional; `page=0`, `size=20` | `PageResponseDTO<AlmacenResponseDTO>` |
| `/api/v1/almacenes/{nit}/catalogo/resumen` | `limite=5` | Lista de `CatalogoItemResponseDTO` |
| `/api/v1/almacenes/{nit}/catalogo/novedades` | `page=0`, `size=20` | `PageResponseDTO<CatalogoItemResponseDTO>` |
| `/api/v1/almacenes/{nit}/distancia` | `latitudUsuario`, `longitudUsuario`, obligatorios | `DistanciaAlmacenResponseDTO` |

`page >= 0`, `1 <= size <= 100`, `1 <= limite <= 20`. Categorías exactas: `CASUAL`, `FORMAL`, `DEPORTIVA`, `VINTAGE`, `ACCESORIOS`. Parámetros mal formados, categorías desconocidas y coordenadas inválidas producen 400. Las consultas individuales producen 404 si no existe el almacén; distancia produce 422 si sus coordenadas faltan o son inválidas. Los errores técnicos pasan por `GlobalExceptionHandler` (500).

Los marcadores exponen únicamente NIT, nombre comercial, descripción, coordenadas, reputación y categorías. Solo aparecen coordenadas completas, finitas y dentro de los rangos geográficos. Orden: `nombreComercial ASC, nit ASC`. Las categorías vacías no excluyen un almacén geolocalizable cuando no se aplica filtro.

## Decisiones provisionales de negocio

El resumen usa una muestra de publicaciones disponibles más recientes: `fechaPublicacion DESC, itemId ASC`. No existe selección editorial ni propiedad `destacada`. Esta regla no implementa RF72. Reutiliza el DTO y el mapper públicos de catálogo; no devuelve bytes ni inventa URLs de imágenes.

Las novedades usan una ventana móvil UTC de siete días: `[fin - 7 días, fin]`, con ambos extremos inclusivos y `fin = Clock.instant()` leído una sola vez. Filtran la fecha real de publicación y no el estado `NUEVA_PRENDA`: un ítem con `ULTIMAS_UNIDADES` también puede ser novedad. Las fechas futuras quedan fuera.

La distancia reutiliza directamente `DistanciaCalculator.calcularKm`, conserva el valor completo en dominio y presenta dos decimales en kilómetros con `tipoEstimacion=LINEA_RECTA`. No representa una ruta de conducción ni estima tiempos de viaje.

## Persistencia, inventario y consultas

`CategoriaAlmacen` es independiente de `Estilo` y `TipoPrenda`. `Almacen` y `AlmacenEntity` agregan `Set<CategoriaAlmacen>`. JPA usa `@ElementCollection`, `EnumType.STRING` y la tabla `almacen_categorias(almacen_nit, categoria)`, con unicidad por pareja. Se conservan los constructores anteriores.

El listado pagina identificadores en BD y luego carga las categorías en una sola consulta. `MEMBER OF` evita la multiplicación de filas y mantiene el count. Resumen y novedades también paginan identificadores y luego cargan almacén, prenda e inventario con un EntityGraph. No se pagina una colección cargada mediante fetch. Las consultas de página fuera de rango conservan el total.

La visibilidad del catálogo usa `InventarioTalla`: como el esquema existente impide unidades negativas, una subconsulta `EXISTS` sobre tallas con unidades positivas excluye stock cero antes del límite/paginación. No duplica la suma del stock. El estado de respuesta se obtiene de `EstadoItemCalculator`, y stock/tallas del dominio `ItemCatalogo`, conservando los umbrales de Sprint 2. Los retiros existentes eliminan físicamente los ítems, por lo que no aparecen. Ningún GET escribe ni sincroniza estados persistidos.

Un estado persistido puede quedar obsoleto; las lecturas de esta etapa siguen el estado efectivo igual que `CatalogoServiceImpl`. Las pruebas incluyen ambos casos de inconsistencia (stock cero con estado disponible y stock positivo con estado AGOTADA). No se inspeccionó ni modificó PostgreSQL real para determinar si existen registros así.

## Históricos y dependencia de RF62

Latitud y longitud siguen siendo opcionales. Los históricos sin coordenadas válidas permanecen en BD y no se convierten en marcadores. No se asignan coordenadas ni categorías ficticias. La ausencia de filas en `almacen_categorias` representa categorías vacías.

Las categorías existentes deberán ser confirmadas por el flujo de administración del almacén, coordinado con RF62, y cargadas por ese proceso autorizado: una fila por categoría aprobada y NIT existente, usando el nombre textual exacto del enum y sin duplicados. Los fixtures pueden usar `setCategorias(Set.of(...))` o el mapper de persistencia. Esta etapa no crea endpoints administrativos ni realiza una carga sobre PostgreSQL real. El despliegue del esquema aditivo y su carga deben coordinarse posteriormente con infraestructura.

RF62 implementará la verificación formal de aliados. Actualmente no existe ese estado: estas consultas muestran almacenes existentes y geolocalizables sin afirmar que están verificados. Cuando RF62 esté implementado, el listado deberá filtrar por el estado de verificación aprobado, junto con la geolocalización y la categoría. No se implementó RF62.

## Seguridad y verificación

Solo los cuatro GET concretos se agregan a la política pública y a la documentación OpenAPI. Las rutas restantes conservan `anyRequest().authenticated()`. No se habilita un comodín de almacenes; POST/PUT/PATCH/DELETE siguen protegidos. La autorización definitiva por roles queda a cargo del Líder Técnico.

Las pruebas usan JUnit, Mockito, MockMvc con la FilterChain real y H2/JPA. El reloj de las pruebas de novedades es `Clock.fixed`, sin esperas ni dependencia del tiempo real. La integración habilita el fallo de Hibernate ante paginación sobre fetch de colecciones para comprobar que estas consultas no usan esa estrategia.

## Cambios manuales futuros en diagramas

En clases: agregar `CategoriaAlmacen`, la colección de categorías de `Almacen`/`AlmacenEntity`, `DistanciaAlmacen`, los DTOs de consulta/respuesta, `AlmacenMapper`, `AlmacenValidator`, `IAlmacenService`, `AlmacenServiceImpl`, `AlmacenController` y `AlmacenApi`; reflejar las nuevas consultas en los repositorios y la reutilización de inventario, estado y distancia.

En componentes: representar el descubrimiento público y sus conexiones con catálogo/inventario, persistencia y seguridad existente. Más adelante, incorporar el filtro de verificación de RF62 cuando exista su contrato. No se modificaron ni generaron diagramas en esta etapa.
