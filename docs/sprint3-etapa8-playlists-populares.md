# Sprint 3 — Etapa 8: RF72, playlists populares

## Alcance y verificación inicial

Únicamente RF72 y las correcciones mínimas de identidad, concurrencia e integridad de likes necesarias para proteger su ranking. Rama inicial `feature/s3-daniel-functional-security`, HEAD `0e430bd4dee0c33d81574cde767bcc3b876b27a1`, commit `feat(subscriptions): implement RF67-RF68 store subscriptions`, working tree limpio. No se hizo checkout, pull, merge ni rebase.

No se encontró un documento oficial de requisitos con el texto de RF72 dentro del workspace, incluidos archivos ocultos fuera de `.git` y artefactos `target`. Los documentos de etapas anteriores mencionan RF72 como pendiente, sin definir otra política. La política especificada por el usuario es provisional: likes activos únicos DESC, fecha de creación DESC, ID ASC. No se incorporan visitas, períodos, compartidos, recomendaciones ni criterios adicionales.

Se auditaron Playlist, PlaylistEntity, PlaylistRepository, IPlaylistService, PlaylistServiceImpl, PlaylistController, PlaylistApi, PlaylistMapper, PlaylistEntityMapper, PlaylistResponseDTO, InteractuarPlaylistRequestDTO, VisibilidadPlaylist, PlaylistValidator, PrendaEntity/Repository/Mapper, UsuarioEntity/Repository, UsuarioPrincipal, JwtAuthFilter, SecurityConfig, GlobalExceptionHandler, VisibilidadModeracion, VisibilidadAlmacen, PageResponseDTO, SwaggerConfig y TimeConfig/Clock. También se revisaron las pruebas existentes de controller, servicio, mappers, validator, HTTP/JWT y visibilidad RF61/RF62. No existía una prueba dedicada de PlaylistRepository; esta etapa agrega integración JPA real para sus consultas.

## Endpoint y contratos

`GET /api/v1/playlists/populares?page=0&size=20`, autenticado mediante Bearer JWT. `page >= 0`, `1 <= size <= 100`; valores predeterminados 0 y 20. PlaylistPopularesRequestDTO aplica Bean Validation mediante `@Valid @ModelAttribute`, siguiendo el patrón actual. PlaylistValidator vuelve a validar en el servicio; SolicitudPlaylistInvalidaException se traduce a 400 mediante el handler existente. Tipos no numéricos, vacíos o fuera de int se rechazan con 400. Un offset mayor que Integer.MAX_VALUE devuelve página vacía y count real, sin enviar ese offset a JPA.

Respuesta `PageResponseDTO<PlaylistResponseDTO>`: content, page, size, totalElements, totalPages, first y last. PlaylistResponseDTO, Playlist y ambos mappers se conservan intactos. La respuesta incluye los campos previos id, usuarioId, nombre, descripcion, visibilidad, estilo, fechaCreacion, prendas, likes y totalPrendas. Nunca serializa likesUsuarios. La ruta literal y la ruta numérica `GET /api/v1/playlists/{id}` tienen pruebas HTTP explícitas. Swagger documenta Bearer, política, límites, defaults, DTO y respuestas 200/400/401/500. Solo se añade 403 a la documentación de likes por la corrección de identidad.

## Popularidad, privacidad y actualización

Solo PUBLICA participa en contenido, count y paginación, incluso si el consumidor es el propietario de una privada. No existe parámetro de identidad del ranking; usuarioConsultaId no participa. Una privada con 1000 likes no influye en la posición de una pública con 1 like.

Las públicas con cero likes son elegibles y aparecen después de las que tienen likes. Las públicas sin prendas, o sin prendas visibles, también participan: prendas=[], totalPrendas=0 y cantidad real de likes. No hay mínimo de popularidad ni requisito de contenido. Sin públicas, se devuelve una página vacía con 200.

No hay caché ni puntuación separada. Tras confirmar una transacción que agrega/quita likes, elimina una playlist o cambia su visibilidad, la siguiente consulta refleja el nuevo estado. PUBLICA→PRIVADA desaparece; PRIVADA→PUBLICA regresa conservando sus likes. Cada respuesta utiliza un snapshot consistente con REPEATABLE_READ: no mezcla el orden de una consulta con los likes de otro instante. Una consulta que ya estaba en curso puede devolver su snapshot anterior; esta es la semántica transaccional, sin caché de ranking.

## Consulta y rendimiento

IPlaylistService y PlaylistServiceImpl agregan `Page<Playlist> obtenerPopulares(int page, int size)`. Controller convierte dominio mediante PlaylistMapper; no contiene SQL ni entidades HTTP.

Fase A, PlaylistRepository.findIdsPopulares(Pageable):

```sql
select p.id from PlaylistEntity p left join p.likesUsuarios l
where p.visibilidad = edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist.PUBLICA
group by p.id, p.fechaCreacion
order by count(distinct l) desc, p.fechaCreacion desc, p.id asc
```

JPQL ejecutado por JPA/Hibernate, con LIMIT/OFFSET en la base de datos; LEFT JOIN conserva los cero likes. `count(distinct l)` cuenta usuarios únicos y tolera duplicados históricos para ordenar. La consulta count independiente es:

```sql
select count(p) from PlaylistEntity p
where p.visibilidad = edu.dosw.proyecto.style_radar.model.domain.VisibilidadPlaylist.PUBLICA
```

No cuenta filas de playlist_likes. Cuatro públicas con cien likes en una arrojan totalElements=4. El count derivado por visibilidad solo se usa para el camino de overflow.

Fase B: findDetallesPopulares carga únicamente los IDs de la página con usuario y prendas; vuelve a exigir PUBLICA. cargarLikesPorIds inicializa likes en otro lote dentro del mismo contexto de persistencia. Cada consulta carga una sola colección; no existe producto cartesiano prendas×likes ni fetch de colecciones sobre la consulta paginada. Un mapa por ID reconstruye el orden de fase A, ya que IN no garantiza orden. prendasVisibles filtra el dominio en un lote adicional. La prueba con 25 playlists de autores diferentes, 30 prendas por playlist y 100–124 likes verifica cinco consultas para una página completa de 20, count incluido; el número de consultas no crece por playlist, autor o prenda.

No se usa findAll para clasificar, no se carga el universo de playlists en memoria, ni se añaden Redis, tareas programadas o dependencias. La fase B queda limitada a size<=100; la cantidad de prendas/likes de cada playlist puede ser grande y sigue siendo un coste real de materialización. PostgreSQL QA debe medir cardinalidades y EXPLAIN ANALYZE; un ranking exacto por agregación sigue requiriendo procesar los likes de las públicas.

## RF61/RF62 y asociaciones

Se reutiliza prendasVisibles y PrendaRepository.findIdsOcultas, con las políticas actuales VisibilidadModeracion y VisibilidadAlmacen. Una definición sin publicaciones sigue visible; una prenda con publicaciones exige al menos una publicación elegible por ambas políticas. No se agrega una regla nueva de stock ni cambia la compatibilidad NULL del legado.

La prueba HTTP construye prendas con publicación aprobada, rechazada y pendiente; almacenes aprobados, rechazados y pendientes. Solo la prenda elegible aparece. Otra variante contiene únicamente prendas ocultas y conserva la playlist, sus diez likes y totalPrendas=0. El filtrado modifica solamente la proyección de dominio: las relaciones JPA permanecen, y las operaciones actuales de edición/retiro siguen funcionando. totalPrendas se calcula por el mapper sobre las prendas visibles realmente devueltas.

## Seguridad e identidad de likes

SecurityConfig y JwtAuthFilter permanecen intactos. Populares hereda anyRequest().authenticated(), como GET /api/v1/playlists. La cadena real rechaza con 401 JWT ausente/inválido/expirado y cuentas INACTIVA/BLOQUEADA; un comprador ACTIVA recibe 200. No se modifica ningún rol, permiso del Líder Técnico ni secreto JWT.

La vulnerabilidad auditada era que POST /api/v1/playlists/{id}/like utilizaba directamente request.usuarioId. Se conserva InteractuarPlaylistRequestDTO y se compara su usuarioId con UsuarioPrincipal.getUsuarioId(). Si difiere, 403 genérico antes de invocar el servicio; la identidad enviada al servicio procede del principal. Un principal ausente también se rechaza; normalmente la FilterChain ya evita ese caso con 401.

Se conserva la alternancia: si no hay like del usuario se agrega; si existe se retira. La popularidad cuenta únicamente likes activos, no una métrica acumulativa. No hay endpoint nuevo de likes ni tabla/entidad nueva.

## Concurrencia e integridad persistente

alternarLike obtiene PlaylistEntity con findForUpdate, PESSIMISTIC_WRITE sobre la fila raíz y sin fetch joins. El lock dura toda la transacción; la colección se lee y modifica después de adquirirlo. Dos usuarios no sobrescriben el Set a partir de lecturas obsoletas. El método de lock está separado de los métodos de lectura y no cambia los validadores de acceso existentes.

PlaylistEntity conserva su modelo, con un cambio mínimo en el mapping de playlist_likes: UNIQUE(playlist_id,usuario_id), nombre uk_playlist_like_usuario, y usuario_id no nullable. No se introduce contador. El mapping anterior no declaraba UNIQUE explícito; la prueba H2 comprueba que una inserción nativa del mismo par se rechaza. COUNT DISTINCT y el Set aseguran que el número mostrado coincide con usuarios únicos persistidos.

No se ejecuta ninguna migración ni conexión sobre PostgreSQL. Antes de desplegar/aplicar la restricción en QA se deben buscar pares duplicados y valores NULL, planificar su depuración y confirmar la restricción real. No se han eliminado datos históricos. El ddl-auto configurado actualmente puede intentar cambios al arrancar un entorno: QA debe controlar esa aplicación y no asumir que el cambio de anotación depura o migra datos existentes correctamente.

Las pruebas usan transacciones independientes con fixtures confirmados, CountDownLatch y Futures con límites de tiempo, sin Thread.sleep. Cubren dos usuarios agregando, retirada mientras otro agrega en ambos órdenes, dos alternancias del mismo usuario, ausencia de filas duplicadas, ranking después de confirmar, y rollback posterior a flush al agregar o retirar. Una operación posterior al rollback comprueba liberación de locks y conservación de integridad.

## Índices y PostgreSQL QA

El UNIQUE compuesto crea un índice útil con playlist_id como primera columna. No se agrega un índice redundante a playlist_likes ni se ejecuta DDL real. Evaluar en QA un índice de playlists(visibilidad,fecha_creacion,id) según plan/cardinalidad; el agregado de likes DESC no se resuelve por completo con un índice ordinario. Verificar el nombre físico de columnas, constraint, tamaño de la tabla, distribución de likes y coste de count y ranking.

Pendiente QA: ejecutar las consultas JPQL con la versión PostgreSQL objetivo; revisar EXPLAIN ANALYZE/BUFFERS, locks/timeouts/deadlocks, snapshot REPEATABLE_READ, contención al alternar likes, errores serializables que requieren reintento, y cargas grandes. H2 en modo PostgreSQL no demuestra todos los comportamientos del motor PostgreSQL. El handler existente traduce conflictos concurrentes a 409; no se añade reintento automático.

## Compatibilidad y hallazgos para el Líder Técnico

Los endpoints previos de crear, listar públicas, obtener por ID/usuario, editar, agregar/retirar prendas y eliminar conservan sus contratos y comportamiento. Solo likes cambia identidad y protección de escritura, autorizado para RF72. El fixture de su prueba anterior se ajusta para incluir principal autenticado; la prueba de servicio utiliza el método de lock correspondiente. No se eliminan pruebas ni cambian assertions ajenas.

Hallazgos existentes fuera del alcance: otras operaciones confían en usuarioId de ruta/body y la lectura privada por ID acepta usuarioConsultaId del cliente; consultar por usuario devuelve sus playlists sin una identidad JWT contrastada. alternarLike también conserva la política previa de acceso/visibilidad del objeto sobre el que se interactúa. Deben revisarse separadamente con el Líder Técnico: RF72 no reescribe esos permisos ni abre una ruta alternativa del ranking. Las ediciones/eliminaciones existentes no pasan por el nuevo método explícito de lock de likes; QA debe evaluar su interacción concurrente con likes. No se implementa un rediseño de todo el módulo.

Logging nuevo con SLF4J: operación, page, size y totalElements, una entrada por consulta. No se registran JWT, headers, credenciales, listas de usuarios que dieron like ni datos privados. No se modifica Clock; el ranking ordena por la fecha persistida y no inventa fechas o ventanas.

No se modifican Dockerfile, docker-compose.yml, .dockerignore, .env.example, workflows, Azure, Docker Hub, CI/CD, dependencias, README o diagramas. No se implementa ningún otro RF. Los diagramas quedan para la actualización posterior autorizada. No hay commit, push ni PR y no se inicia Etapa 9.

## Validación final

Resultados del 14 de octubre de 2026, ejecutados desde `style-radar`:

| Verificación | Resultado |
| --- | --- |
| mvn clean test | BUILD SUCCESS; 1340 tests; failures=0, errors=0, skipped=0; 2 min 57 s |
| mvn clean verify | BUILD SUCCESS; 1340 tests; failures=0, errors=0, skipped=0; 3 min |
| Regresión anterior | Se conservan las 1278 pruebas; pasan todas |
| Pruebas nuevas | 62 |
| JaCoCo BUNDLE LINE | 2363 cubiertas, 97 no cubiertas: 96,0569 % (96,06 %) |
| JaCoCo BUNDLE BRANCH | 776 cubiertas, 142 no cubiertas: 84,5316 % (84,53 %) |
| Umbral LINE | 85 % original, sin cambios ni exclusiones; All coverage checks have been met |

Las cifras se extraen de los XML de Surefire y de los counters directos BUNDLE de target/site/jacoco/jacoco.xml, iguales en ambas ejecuciones completas. Distribución de las 62 nuevas: 10 unitarias de servicio, 2 unitarias de controller (403 sin principal y 500 genérico), 27 HTTP con FilterChain real, 17 de ranking/repositorio/paginación JPA y 6 de concurrencia/rollback con transacciones reales. Las dos unitarias de controller no se presentan como evidencia de seguridad JWT; esa evidencia proviene de las 27 HTTP.

Se cubren 10/5/0 likes, desempates, cero/una pública, privadas fuera del count, cuatro públicas/cien likes, páginas consecutivas sin duplicados, size 1/2/20/100, page 0/1/fuera de rango, tipos inválidos y overflow, cambios de likes/visibilidad/eliminación, unicidad persistente, prendas ocultas/ninguna visible, compatibilidad de edición/retiro, rutas literal/numérica, Swagger y rendimiento. La regresión incluye RF61/RF62/RF66, suscripciones, catálogo, prendas, mappers, validators, HTTP/JWT y OpenApiSmokeTest.

Durante las ejecuciones específicas se corrigieron un error de tipo en el resultado nativo de un fixture y una coordinación de concurrencia que intentaba invocar el método abstracto de un proxy Spring Data como método real. La coordinación ahora intercepta UsuarioValidator, manteniendo las operaciones y locks reales. Los seis casos concurrentes pasaron después de la corrección y en ambas regresiones completas. No se eliminaron casos ni relajaron assertions para solucionar esos fallos.

Control final: git diff --check sin errores; ocho archivos nuevos, diez modificados y cero eliminados. HEAD y rama siguen iguales a la verificación inicial. No se hace commit, push ni PR. Listo para commit de esta etapa: SÍ; validación PostgreSQL, aplicación controlada del constraint e integración de hallazgos externos quedan como QA posterior, sin afirmar validación del motor PostgreSQL.

## Inventario de archivos

Prefijo de producción: `style-radar/src/main/java/edu/dosw/proyecto/style_radar/`.
Prefijo de pruebas: `style-radar/src/test/java/edu/dosw/proyecto/style_radar/`.

| Estado | Prefijo | Archivo |
| --- | --- | --- |
| Creado | workspace | docs/sprint3-etapa8-playlists-populares.md |
| Creado | producción | exception/SolicitudPlaylistInvalidaException.java |
| Creado | producción | model/dto/request/PlaylistPopularesRequestDTO.java |
| Creado | pruebas | controller/PlaylistsPopularesHttpIntegrationTest.java |
| Creado | pruebas | service/impl/PlaylistsLikesConcurrenciaIntegrationTest.java |
| Creado | pruebas | service/impl/PlaylistsPopularesJpaIntegrationTest.java |
| Creado | pruebas | service/impl/PlaylistsPopularesServiceTest.java |
| Creado | pruebas | support/PlaylistsPopularesFixtures.java |
| Modificado | producción | controller/PlaylistController.java |
| Modificado | producción | controller/docs/PlaylistApi.java |
| Modificado | producción | exception/GlobalExceptionHandler.java |
| Modificado | producción | model/entity/PlaylistEntity.java |
| Modificado | producción | repository/PlaylistRepository.java |
| Modificado | producción | service/IPlaylistService.java |
| Modificado | producción | service/impl/PlaylistServiceImpl.java |
| Modificado | producción | validator/PlaylistValidator.java |
| Modificado | pruebas | controller/PlaylistControllerTest.java |
| Modificado | pruebas | service/impl/PlaylistServiceImplTest.java |

No se elimina ningún archivo. GlobalExceptionHandler es el archivo compartido transversal modificado; el resto pertenece al módulo de playlists y sus pruebas. Su nueva traducción a 400 está limitada a SolicitudPlaylistInvalidaException. SecurityConfig, JwtAuthFilter, PageResponseDTO, SwaggerConfig, Clock, las políticas RF61/RF62 y los repositorios de prendas/usuarios no se modifican.
