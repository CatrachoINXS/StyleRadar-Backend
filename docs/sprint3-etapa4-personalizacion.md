# Sprint 3 — Etapa 4: personalización de feed y recomendaciones

Alcance exclusivo: SR-RF-05 y SR-RF-06, conservando sus descripciones originales. No hay ML, APIs de IA, aleatoriedad, entrenamiento, actividad inventada, notificaciones ni nuevos historiales.

## Base y auditoría

- Rama inicial: `feature/s3-daniel-functional-security`.
- HEAD inicial y final: `a7a869ec4f931acc2976b7f98f814007fcf6f836`, `feat(stores): implement geolocation and discovery APIs`.
- `git status --short` inicial: sin salida, working tree limpio.
- Base reportada: 477 pruebas, cero failures/errors/skipped, LINE 93,89 %, BRANCH 74,10 %.
- Se inspeccionaron Usuario, UsuarioEntity, UsuarioRepository, UsuarioServiceImpl, Estilo, Talla, TipoPrenda, BusquedaGuardada y su entidad/repositorio, Prenda y su entidad, ItemCatalogo y su entidad/repositorio, InventarioTalla y su entidad, BusquedaCatalogoCriteria, ItemCatalogoSpecifications, BusquedaCatalogoServiceImpl, mappers de catálogo, DTOs de catálogo/página, EstadoItemCalculator, UsuarioPrincipal, JwtAuthFilter, SecurityConfig, SwaggerConfig, GlobalExceptionHandler y sus pruebas existentes.
- El retiro actual es borrado físico mediante `CatalogoServiceImpl.retirar`. No existe un estado adicional RETIRADA y no se introduce uno.

## RF05: feed personalizado

El feed incluye todos los ítems publicados que tengan alguna fila de InventarioTalla con unidades > 0. No filtra por compatibilidad de perfil: la utiliza para priorizar.

Para cada ítem se evalúan dos señales de perfil: coincidencia con alguno de los estilos preferidos y existencia de stock positivo en alguna talla habitual. Una dimensión vacía es una señal falsa, nunca una coincidencia positiva.

Orden lexicográfico aprobado, aplicado antes de paginar:

1. Coincidencia simultánea de estilo y talla.
2. Coincidencia de una sola dimensión.
3. Ninguna coincidencia.
4. Dentro de cada grupo, primero coincidencia exacta con al menos una búsqueda guardada significativa propia.
5. Dentro de cada subgrupo, `fechaPublicacion DESC`, `itemId ASC`.

Los valores ordinales internos del CASE representan grupos de esta política; no son un score de afinidad ni ponderaciones.

Sin estilos y tallas, se sigue priorizando actividad guardada significativa. Sin ninguna señal, el resultado es un feed general disponible reciente. No se actualizan preferencias ni se genera error por perfil vacío.

## Búsquedas guardadas como actividad

Se leen exclusivamente con `BusquedaGuardadaRepository.findByUsuario_IdOrderByFechaCreacionDesc(usuarioId)`, usando el ID del principal autenticado. RF06 no consulta este repositorio.

La conversión a criterios elimina textos nulos, vacíos o compuestos solo de espacios; los demás textos se recortan como en la búsqueda del Sprint 2. Una búsqueda es significativa si tiene al menos un criterio efectivo entre query, tipo, color, talla, precioMin, precioMax, marca y estilo. El nombre y la fecha de creación no son criterios. Un precio mínimo de cero sí es un criterio efectivo.

`ItemCatalogoSpecifications.coincidenciaExacta` comparte las reglas estructuradas y de texto de los filtros existentes. Todos los criterios de una misma búsqueda se combinan con AND; las distintas búsquedas se combinan con OR. Marca y color usan igualdad sin distinguir mayúsculas; query usa el LIKE parcial existente con escape de `%`, `_` y barra inversa; precios usan límites inclusivos; tipo y estilo son exactos; talla requiere unidades > 0.

No se invoca BusquedaCatalogoServiceImpl ni su fallback de similitud RF16. No se reutiliza el estado persistido como sustituto de inventario real: una publicación marcada AGOTADA con stock positivo puede aparecer con estado efectivo recalculado.

`tallaDisponible` se expresa ahora mediante EXISTS, conservando la misma semántica pública del Sprint 2 y evitando joins de colección que multipliquen filas. `conCriterios` y `conCriteriosSinTexto` conservan el filtro de estado que tenían; el comportamiento general de búsqueda no cambia.

## RF06: recomendaciones compatibles

| Perfil persistido | Selección |
| --- | --- |
| Estilos y tallas | Algún estilo preferido AND alguna talla habitual con unidades > 0 |
| Solo estilos | Algún estilo preferido y disponibilidad real en cualquier talla |
| Solo tallas | Alguna talla habitual con unidades > 0 |
| Sin estilos ni tallas | Selección general disponible reciente, `generalSinPreferencias=true` |
| Perfil configurado sin coincidencias | Página vacía, `generalSinPreferencias=false` |

Las coincidencias y el fallback se ordenan por `fechaPublicacion DESC`, `itemId ASC`. No se rellenan resultados incompatibles ni se infieren preferencias de color o marca. RF05 y RF06 tienen diferencia observable: el primero conserva prendas incompatibles en posiciones posteriores y utiliza actividad; el segundo las excluye y no utiliza actividad.

## Contrato y arquitectura

- `GET /api/v1/usuarios/me/feed`: `PageResponseDTO<CatalogoItemResponseDTO>`.
- `GET /api/v1/usuarios/me/recomendaciones`: `RecomendacionesResponseDTO`, que extiende la página existente y agrega solamente `generalSinPreferencias`.
- Ambos admiten solamente paginación como parámetros documentados: page=0 y size=20 por defecto; page >= 0, 1 <= size <= 100. Parámetros mal tipados, vacíos o fuera de rango producen 400 mediante la validación y los manejadores existentes.
- CatalogoItemResponseDTO conserva itemId, prendaId, almacenNit, nombre, descripción, tipo, marca, color, estilo, precio, stock, estado, fechaPublicacion y tallasDisponibles. No se exponen entidades ni credenciales ni se crean imágenes o precios.
- Controller -> IPersonalizacionService/PersonalizacionServiceImpl -> repositorios. No hay acceso a Repository desde el Controller ni RequestDTO HTTP en el Service.
- Se reutilizan ItemCatalogoEntityMapper, BusquedaCatalogoMapper para convertir páginas y CatalogoItemMapper para el contenido. No se crea otro mapper de catálogo.
- ResultadoRecomendaciones transporta la página de dominio y la indicación de fallback; no depende de JPA, Spring MVC ni SecurityContext.
- No se crean validators vacíos: las restricciones de paginación están en PersonalizacionRequestDTO.

## Consultas, orden y paginación

ItemCatalogoRepository incorpora el fragmento PersonalizacionCatalogoRepository, implementado con Criteria JPA en PersonalizacionCatalogoRepositoryImpl.

1. Count de todos los candidatos elegibles, con los mismos predicados de selección.
2. Proyección de IDs con CASE para RF05 o recencia para RF06; LIMIT/OFFSET se aplican en DB después del orden completo.
3. `findByIdIn` con el EntityGraph existente (`almacen`, `prenda`, `inventario`) carga únicamente los detalles de esa página.
4. El servicio reconstruye el orden según la proyección de IDs, porque IN no garantiza orden.

EXISTS sobre InventarioTalla evita duplicados por tallas o búsquedas múltiples; el count no necesita multiplicar filas. Una página fuera de rango conserva el total real y no ejecuta el fetch de detalles. Se comprueba el offset largo antes de convertir al int de JPA, incluyendo page=Integer.MAX_VALUE con size=100.

Los métodos de aplicación tienen transacciones de solo lectura con REPEATABLE_READ para que las consultas de una petición compartan la misma vista en PostgreSQL. Peticiones sucesivas con paginación por offset pueden observar publicaciones o retiros concurrentes; no se introduce un cursor ni una instantánea persistida entre peticiones. Si faltara inesperadamente un detalle seleccionado, se propaga un error; no se oculta como resultado vacío.

No se procesa el conjunto completo de candidatos en memoria ni se introduce un límite oculto. El orden en memoria solo restaura los IDs de la página ya seleccionada. No hay SQL nativo ni infraestructura adicional; las consultas se verifican en H2 en modo PostgreSQL, sin ejecutar cambios contra PostgreSQL real.

Las pruebas con 25 candidatos, dos tallas por artículo y búsquedas múltiples comprueban 5 consultas para RF05 y 4 para RF06 en la aplicación (perfil, actividad solo en feed, count, IDs y detalles), sin N+1 y sin escritura de estados. HTTP agrega la lectura de autenticación JWT existente. Se activa en estas pruebas el rechazo de paginación con fetch de colecciones y se accede al resultado tras limpiar el EntityManager.

## Seguridad, errores y logging

PersonalizacionController resuelve `usuarioId` exclusivamente desde `@AuthenticationPrincipal UsuarioPrincipal.getUsuarioId()`. No hay parámetro ni body de identidad y no se registra una ruta `/{id}/feed` o `/{id}/recomendaciones`. Un parámetro desconocido usuarioId o un body GET no seleccionan otro usuario ni cambian el perfil utilizado.

SecurityConfig conserva `anyRequest().authenticated()`; ninguna ruta nueva está en permitAll. JwtAuthFilter permanece intacto y relee identidad/estado/roles: JWT ausente, inválido, expirado o cuenta bloqueada producen 401. Una cuenta sin credenciales no puede obtener un token mediante login ni acceder sin JWT. No se añade una matriz global de roles ni @PreAuthorize del Líder Técnico.

Un usuario ausente durante autenticación es 401; un recurso ausente durante una consulta de aplicación autenticada válida es 404. El test de este último caso simula la desaparición en la capa de aplicación después de pasar los filtros reales. Perfiles vacíos y páginas vacías son 200. Los errores de DB se propagan al GlobalExceptionHandler y devuelven 500 genérico sin stacktrace en el frontend. Se mantienen los manejadores existentes 400/404/409/422/500 y 401/403 de seguridad.

SLF4J registra usuarioId, tipo de consulta, page, size, cantidad y total; la ausencia de usuario se registra como advertencia y los fallos se gestionan por el manejador existente. No se registra contenido completo de búsquedas privadas, JWT, Authorization, hashes, contraseñas ni datos personales innecesarios; no hay logging por prenda.

## Pruebas y validación final

- PersonalizacionServiceImplTest: orden de fetch, cálculo efectivo sin escritura, cada criterio significativo y descarte de vacíos, count fuera de rango, usuario inexistente, errores de DB y detalle faltante.
- PersonalizacionPersistenceIntegrationTest: prioridades RF05, todos los filtros AND, actividad propia/ajena/vacía, texto normalizado y comodines literales, ausencia de similitud, perfiles parciales/vacíos, disponibilidad, retiro, múltiples estilos/tallas, RF06 estricto, orden, count, paginación global, duplicados y consultas/LAZY.
- PersonalizacionHttpIntegrationTest: filtros reales de SecurityConfig/JwtAuthFilter, JWT obtenido por login, expiración, cuenta bloqueada, identidad no seleccionable, privacidad, validación de page/size, contrato DTO, fallback explícito, usuario sin credenciales, GET públicos previos, rutas privadas previas y Swagger.
- PersonalizacionErroresHttpTest: filtros reales con mock de aplicación exclusivamente para 404/500 posteriores a la autenticación y 401 de identidad ausente.
- Fixtures persistidas por test con rollback; Arrange / Act / Assert. No se eliminan, modifican ni deshabilitan pruebas existentes.

Validación final ejecutada desde `style-radar` el 9 de octubre de 2026:

| Verificación | Resultado |
| --- | --- |
| `mvn clean test` | BUILD SUCCESS, 574 pruebas |
| `mvn clean verify` | BUILD SUCCESS, 574 pruebas; verificación de cobertura aprobada |
| Pruebas anteriores | 477 conservadas y aprobadas sin modificar sus archivos |
| Pruebas nuevas | 97: 15 unitarias, 42 de persistencia y 40 HTTP con filtros reales |
| Failures / errors / skipped | 0 / 0 / 0 en ambas ejecuciones finales |
| JaCoCo LINE BUNDLE | 94,21 %: 1725 líneas cubiertas, 106 no cubiertas |
| JaCoCo BRANCH BUNDLE | 76,28 %: 418 ramas cubiertas, 130 no cubiertas |
| Umbral LINE | 85 % conservado, sin exclusiones nuevas ni cambios al POM |

Fuente de métricas: XML final de `target/surefire-reports/TEST-*.xml` y contadores BUNDLE de `target/site/jacoco/jacoco.xml`.

La primera selección HTTP detectó una expectativa incorrecta del test nuevo sobre rutas inexistentes (404 frente al 500 del manejador global existente). Se verificó su causa y se comprobó la ausencia de rutas alternativas en OpenAPI, conservando los tests funcionales de identidad, privacidad y 404 de consultas válidas. Después se ejecutó la regresión completa; una simplificación final de la consulta y su aislamiento motivó repetir `clean test` y luego `clean verify`, ambos exitosos.

## Inventario de archivos

Archivos creados (las rutas Java son relativas a `style-radar/src/`):

- `main/java/edu/dosw/proyecto/style_radar/controller/PersonalizacionController.java`
- `main/java/edu/dosw/proyecto/style_radar/controller/docs/PersonalizacionApi.java`
- `main/java/edu/dosw/proyecto/style_radar/model/domain/ResultadoRecomendaciones.java`
- `main/java/edu/dosw/proyecto/style_radar/model/dto/request/PersonalizacionRequestDTO.java`
- `main/java/edu/dosw/proyecto/style_radar/model/dto/response/RecomendacionesResponseDTO.java`
- `main/java/edu/dosw/proyecto/style_radar/repository/PersonalizacionCatalogoRepository.java`
- `main/java/edu/dosw/proyecto/style_radar/repository/PersonalizacionCatalogoRepositoryImpl.java`
- `main/java/edu/dosw/proyecto/style_radar/service/IPersonalizacionService.java`
- `main/java/edu/dosw/proyecto/style_radar/service/impl/PersonalizacionServiceImpl.java`
- `test/java/edu/dosw/proyecto/style_radar/controller/PersonalizacionErroresHttpTest.java`
- `test/java/edu/dosw/proyecto/style_radar/controller/PersonalizacionHttpIntegrationTest.java`
- `test/java/edu/dosw/proyecto/style_radar/service/impl/PersonalizacionPersistenceIntegrationTest.java`
- `test/java/edu/dosw/proyecto/style_radar/service/impl/PersonalizacionServiceImplTest.java`
- `test/java/edu/dosw/proyecto/style_radar/support/PersonalizacionFixtures.java`
- `docs/sprint3-etapa4-personalizacion.md` (desde la raíz del repositorio).

Archivos existentes modificados: `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/ItemCatalogoRepository.java` y `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/specification/ItemCatalogoSpecifications.java`.

Archivos eliminados: ninguno. Total: 15 creados, 2 modificados, 0 eliminados. Los nuevos permanecen sin seguimiento y los cambios existentes sin staging.

`git diff --check`: sin salida, código 0. `git diff --stat`: 2 archivos existentes modificados, 17 inserciones y 8 eliminaciones; Git no incluye en ese resumen los 15 archivos nuevos sin seguimiento. `git status --short`: únicamente las dos modificaciones y los archivos nuevos descritos arriba. Rama y HEAD conservados.

Listo para commit: **SÍ**, RF05/RF06 implementados, contratos y seguridad verificados, 477 pruebas previas compatibles, regresión completa y umbral JaCoCo aprobados. El commit no se ejecuta; el working tree queda con los cambios de esta etapa para revisión.

## Límites y decisiones de integración

La política determinista y el fallback están aprobados para esta etapa; no se inventan decisiones de negocio adicionales. La visibilidad actual deriva de existencia de la publicación e inventario positivo.

El modelo conserva `Set<Talla>` global por usuario, sin asociación con TipoPrenda. No se reimplementa RF04 ni se inventa una relación talla/tipo. El equipo deberá decidir si refina RF04 para tallas por tipo de prenda.

RF61 (moderación) y RF62 (verificación de almacenes) todavía no aportan estados aprobados. Cuando se implementen, deberán incorporar sus políticas de visibilidad en la selección compartida de feed/recomendaciones. RF67/RF68 y los demás RF pendientes no se implementan aquí.

Riesgos: el tamaño del predicado de actividad crece con las búsquedas significativas del usuario; a gran escala se debe medir su plan de ejecución e índices antes de decidir mejoras. La paginación por offset no fija una vista entre peticiones. La compatibilidad PostgreSQL se apoya en JPA portable y H2 modo PostgreSQL, sin prueba sobre la DB real en esta etapa. El comportamiento global existente para rutas HTTP inexistentes devuelve 500; no se altera dentro de RF05/RF06, y la ausencia de rutas alternativas se verifica en OpenAPI.

Posibles conflictos con el Líder Técnico: cambios futuros en ItemCatalogoRepository/ItemCatalogoSpecifications o en la matriz de roles y visibilidad pueden requerir coordinación. Esta etapa no modifica SecurityConfig, JwtAuthFilter ni las políticas globales del Líder.

Al finalizar el Sprint, actualizar manualmente los diagramas de componentes/clases y secuencias para mostrar PersonalizacionController, IPersonalizacionService, el fragmento de repositorio, consultas por IDs, principal JWT y metadata de fallback. Los diagramas no se modifican ni se generan aquí.

README, POM, Dockerfile, docker-compose.yml, .dockerignore, .env.example, .github/workflows y configuraciones Docker/CI/CD/Azure permanecen intactos. No se realiza commit, push, merge, rebase, reset, cherry-pick ni PR; no se continúa con Etapa 5.
