# Sprint 3 — Etapa 6B: verificación de almacenes aliados (SR-RF-62)

Alcance exclusivo: revisión y decisión administrativa sobre los datos existentes de un almacén. No incorpora registro público, verificación documental externa, validación legal del NIT, administración de estados de cuenta ni una plataforma de auditoría. RF61 sigue siendo una decisión independiente por publicación.

Base Git comprobada: `feature/s3-daniel-functional-security`, HEAD `99571c65e6e7c404ba92675c67951ab7aa6f5249`, commit `feat(admin): implement RF61 catalog moderation`, working tree limpio al iniciar. Base funcional reportada: 819 pruebas.

## Modelo y persistencia

`EstadoVerificacionAlmacen`: `PENDIENTE`, `APROBADO`, `RECHAZADO`. `DecisionVerificacionAlmacen` admite únicamente las dos decisiones finales, evitando aceptar `PENDIENTE` como decisión.

Se agregan cuatro columnas nullable a `almacenes`, sin tablas nuevas:

| Campo Java | Columna | Representación |
| --- | --- | --- |
| estadoVerificacion | estado_verificacion | EnumType.STRING |
| fechaDecisionVerificacion | fecha_decision_verificacion | Instant, generado mediante Clock, precisión de microsegundos |
| administradorDecisionId | administrador_decision_id | Long, tomado de UsuarioPrincipal |
| motivoVerificacion | motivo_verificacion | String, máximo 1000 caracteres |

Se conservan NIT como identificador, constructores históricos, contacto, coordenadas, reputación, categorías y relaciones con publicaciones, inventario e imágenes. No se agrega una fecha de registro inexistente. El mapper de dominio no recibe decisiones administrativas.

### Compatibilidad histórica y límite del futuro registro

| Estado persistido | Visibilidad pública | Interpretación administrativa |
| --- | --- | --- |
| NULL | Visible provisionalmente | Legado sin aprobación administrativa |
| PENDIENTE | Oculto | Evaluación abierta |
| APROBADO | Visible | Vinculación aprobada explícitamente |
| RECHAZADO | Oculto | Vinculación rechazada explícitamente |

El DTO administrativo conserva `estadoVerificacion=null` y devuelve `legadoSinVerificacion=true`; no fabrica una aprobación, un administrador ni una fecha. Sin filtro administrativo se incluyen los NULL; los filtros APROBADO/PENDIENTE/RECHAZADO incluyen únicamente esos estados reales.

No hay backfill ni defaults globales que cambien los fixtures o registros históricos. El futuro servicio de alta debe asignar **PENDIENTE explícitamente**, dentro de su transacción. No existe aquí ese servicio ni un endpoint de alta. El modelo actual no tiene fecha/procedencia confiable para distinguir un NULL antiguo de un NULL insertado por otro sistema: quien inserte NULL en el futuro recibirá la misma compatibilidad provisional. Es un límite conocido para la integración final; no debe usarse NULL como alta aprobada. El equipo deberá cerrar esta transición cuando haya un alta controlada y clasificación confiable del legado.

## Transiciones y trazabilidad

| Operación | Estado inicial admitido | Estado final |
| --- | --- | --- |
| Solicitar revisión | NULL, APROBADO, RECHAZADO | PENDIENTE |
| Aprobar | PENDIENTE | APROBADO |
| Rechazar | PENDIENTE | RECHAZADO |

Solicitar revisión en PENDIENTE devuelve 422. Decidir fuera de PENDIENTE devuelve 422; tampoco se permite RECHAZADO→APROBADO directo. Para volver a decidir se exige una solicitud administrativa explícita.

La solicitud conserva fecha, administrador y motivo de la última decisión. Esos metadatos representan una decisión anterior cuando el estado actual es PENDIENTE, y se sustituyen conjuntamente al completar la siguiente evaluación. Se mantiene solamente la última decisión; no existe un historial completo de revisiones. La solicitud no crea almacenes, no modifica el catálogo y no borra metadatos históricos.

Rechazo requiere motivo no vacío; aprobación permite omitirlo. Se valida longitud antes de normalizar espacios con `strip`; motivo vacío de aprobación se persiste como NULL. Identidad y fecha provienen del servidor. El JSON solo admite `decision` y `motivo`: campos adicionales, incluso administrador o fechas, son inválidos.

## API administrativa

Todos los endpoints exigen **ROLE_ADMIN_STYLERADAR**, mediante matchers explícitos de SecurityConfig y comprobación del principal en el servicio, incluyendo lecturas. Los demás roles autenticados no acceden a los datos administrativos. JWT, JwtAuthFilter y configuración de sesiones existentes se conservan.

| Método | Ruta | Resultado 200 |
| --- | --- | --- |
| GET | /api/v1/admin/almacenes/verificacion | PageResponseDTO de almacenes administrativos |
| GET | /api/v1/admin/almacenes/{nit}/verificacion | Detalle administrativo |
| POST | /api/v1/admin/almacenes/{nit}/verificacion/solicitar-revision | Detalle en PENDIENTE; sin body |
| PATCH | /api/v1/admin/almacenes/{nit}/verificacion | Detalle con última decisión |

GET de lista: `estado` opcional; `page=0`, `size=20`, máximo 100. Orden `nombreComercial ASC, nit ASC`. Un filtro presente vacío o desconocido es 400. Se incluyen tiendas sin coordenadas.

Ejemplos de decisión:

```json
{"decision":"APROBADO","motivo":"Datos revisados"}
```

```json
{"decision":"RECHAZADO","motivo":"Información insuficiente"}
```

`VerificacionAlmacenResponseDTO` expone NIT, nombre comercial, descripción, teléfono, correo de contacto, latitud, longitud, reputación, categorías, estado persistido, indicador de legado y los tres metadatos de decisión. Correo de contacto no es identidad del administrador del almacén. La consulta no asegura que el NIT esté validado externamente.

DTOs de entrada: `VerificacionConsultaRequestDTO`, `DecisionVerificacionRequestDTO`. Controller: `VerificacionAlmacenController`. Documentación OpenAPI: interfaz `controller/docs/VerificacionAlmacenApi`, bearerAuth existente, rol, ejemplos y respuestas 200/400/401/403/404/409/422/500.

## Visibilidad pública e interacción con RF61

`VisibilidadAlmacen` concentra la política en boolean, predicado Criteria y constantes JPQL para almacenes e ítems. Se combina mediante AND con `VisibilidadModeracion` y con disponibilidad según cada endpoint. Aprobar un almacén no aprueba sus publicaciones; rechazarlo no cambia `EstadoModeracion` ni `EstadoItem`.

| Componente | Integración |
| --- | --- |
| AlmacenRepository.findGeolocalizables | Visibilidad, coordenadas y categoría en WHERE antes de count/orden/página |
| AlmacenServiceImpl | Resumen/novedades exigen existencia pública; distancia verifica visibilidad antes de coordenadas |
| CatalogoServiceImpl.obtenerCatalogo | Existencia pública y consulta de publicaciones públicas; escritura interna conserva acceso a almacenes ocultos |
| ItemCatalogoRepository | JPQL público de catálogo, resumen y novedades combina RF61 y RF62 |
| ItemCatalogoSpecifications | Búsqueda directa, ordenamientos especiales y candidatos para similitud filtran almacén en SQL |
| PersonalizacionCatalogoRepositoryImpl | Feed/recomendaciones aplican RF62 en los predicados de count y selección de IDs |
| PrendaRepository | Si hay publicaciones exige al menos una con almacén visible y moderación elegible |
| PlaylistServiceImpl / PrendaServiceImpl | Heredan la consulta RF61 ampliada; no eliminan asociaciones ni cambian likes/edición |
| DetectorDisponibilidad | Verifica visibilidad persistida del almacén bajo bloqueo antes de generar eventos |

PENDIENTE/RECHAZADO reciben 404 en catálogo, resumen, novedades y distancia, igual que NIT inexistente; el consumidor no recibe el estado administrativo. Un almacén visible sin coordenadas mantiene 422 en distancia. El DTO público del mapa no incorpora teléfono/correo ni metadatos administrativos. La administración continúa pudiendo revisar tiendas ocultas.

Se conservan Haversine, similitud, filtros estructurados, preferencias, tallas, prioridades, desempates y fallback de perfil vacío. Las prendas sin publicaciones mantienen su semántica anterior; una prenda compartida sigue visible si alguna publicación cumple ambas políticas. Stock cero se conserva excluido en las consultas que lo exigían, sin imponer stock a definiciones o playlists.

## Alertas y concurrencia

Ambos eventos, NUEVA_COINCIDENCIA y DISPONIBILIDAD_RESTABLECIDA, quedan suprimidos para PENDIENTE/RECHAZADO, incluyendo coincidencia tardía de talla. No se borran alertas, historial ni datos comerciales. Aprobar no genera notificaciones retroactivas: solo posteriores eventos de stock elegibles siguen las reglas existentes.

Las operaciones RF62 usan `PESSIMISTIC_WRITE` por NIT y una transacción que incluye transición, metadatos y `flush`. Dos decisiones finales concurrentes solo permiten un ganador. El segundo administrador que observa una decisión ya completada recibe 422 por transición inválida; errores del mecanismo de bloqueo/concurrencia se traducen a 409 mediante el handler existente. Errores técnicos de persistencia no generan 200 y revierten la decisión.

El detector mantiene el bloqueo existente del ítem y toma después el del almacén antes de bloquear alertas: **ítem → almacén → alertas**. RF62 bloquea únicamente el almacén y no toma bloqueos de publicaciones, evitando invertir ese orden. Consulta nuevamente existencia pública en SQL después del bloqueo para no depender de un estado antiguo ya cargado en el contexto JPA; no introduce caché. El bloqueo dura hasta el commit de la transacción del stock.

Si ocultar el almacén se confirma primero, la evaluación posterior de stock no emite evento. Si el evento y stock se confirman primero, una revisión posterior conserva esa notificación como historial. El bloqueo por almacén puede serializar aumentos de stock de distintos ítems del mismo NIT; medir contención en QA. Ciclos, deduplicación, rollback y bloqueo de ítems/alertas previos se conservan.

Las pruebas de carreras usan transacciones independientes y coordinación explícita en H2. **No constituyen una garantía ya validada sobre PostgreSQL**. Queda pendiente reproducirlas con el esquema real de QA, aislamiento, timeouts y planes de ejecución usados allí.

## Consultas y rendimiento

Lista administrativa y mapa paginan identificadores en BD; después se cargan solo esos almacenes con EntityGraph de categorías. La hidratación se reordena según la página de IDs. No se realiza un fetch de categorías sobre LIMIT/OFFSET, ni un join multiplicador para filtrar categoría: MEMBER OF evita duplicados. La consulta administrativa estándar se verifica con hasta tres sentencias (IDs, count y carga). Categorías siguen LAZY fuera de esa carga controlada; las herramientas administrativas no requieren geolocalización. Offsets superiores a Integer.MAX_VALUE devuelven una página vacía con count real sin desbordar JPA.

La búsqueda con similitud y los órdenes DISTANCIA/REPUTACION mantienen el diseño previo: reúnen candidatos elegibles de SQL y resuelven su algoritmo en memoria antes de construir la página. RF62 filtra candidatos en BD, no una página recuperada. Escalar estos algoritmos y revisar sus cargas de asociaciones pertenece a una optimización posterior ya reconocida; RF62 no cambia su algoritmo.

## Errores y logging

Se reutilizan GlobalExceptionHandler, ErrorResponseDTO, ReglaDeNegocioException y RecursoNoEncontradoException. HTTP: 400 request/filtro/página inválidos; 401 JWT ausente/inválido o cuenta bloqueada; 403 rol insuficiente; 404 almacén inexistente/no público; 409 conflicto de bloqueo/concurrencia/integridad; 422 revisión repetida/transición inválida; 500 error técnico inesperado.

SLF4J registra NIT, administradorId, estado previo/nuevo y operación. No registra JWT, hashes, cabeceras Authorization, contacto ni texto completo del motivo. Se conservan los manejadores de errores existentes.

## Pruebas y resultados

Pruebas nuevas:

- VerificacionAlmacenServiceTest: Clock fijo, principal real, motivos, estados, reapertura, defensa de rol también en lecturas, paginación e inexistencia.
- VerificacionHttpIntegrationTest: MockMvc con FilterChain real en las cuatro rutas; JWT ausente/manipulado/cuenta bloqueada 401, tres roles ajenos 403, administrador permitido, JSON inválido/campos inyectados, estados de filtro, DTO legado y Swagger.
- VerificacionVisibilidadIntegrationTest: cuatro estados, mapa/categorías, 404/422, catálogo/resumen/novedades, búsqueda directa/similitud/órdenes especiales, feed/recomendaciones, prendas/playlists, páginas menores que resultados, matriz RF61/RF62/stock, ambos eventos de alertas, talla tardía, ausencia de eventos retroactivos, conservación de datos, enum STRING, nullable, categorías LAZY y consultas administrativas sin N+1.
- VerificacionTransaccionesIntegrationTest: decisiones concurrentes, rollback de aprobación/rechazo/reapertura y rollback posterior al flush, longitud persistente, contención decisión-stock con contexto previo cargado y conservación del evento confirmado antes de reabrir.

Las dos pruebas unitarias existentes de AlmacenServiceImpl/CatalogoServiceImpl solo adaptan el mock de existencia de las operaciones públicas a `existsPublicoByNit`. Conservan casos y assertions; los mocks de escritura siguen usando existencia interna. No se elimina ninguna prueba anterior, ni se modifica POM/umbral/exclusiones.

La regresión detectó una falla intermitente en `AlertasPersistenceIntegrationTest.todosLosFiltrosAndCompatiblesYNormalizados` (la ejecución aislada pasó). El escenario requería una publicación estrictamente posterior a la alerta, pero usaba dos lecturas del reloj del sistema sin separación garantizada. Ese fixture ahora controla Clock con instantes separados por un segundo, manteniendo el assertion original de una coincidencia y todos los filtros. No se modifica la comparación temporal de producción ni se agrega una espera artificial.

Resultados finales verificados el 9 de octubre de 2026, desde `style-radar`:

| Verificación | Resultado |
| --- | --- |
| mvn clean test | BUILD SUCCESS, código de salida 0; 945 pruebas |
| mvn clean verify | BUILD SUCCESS, código de salida 0; 945 pruebas; JAR empaquetado y checks JaCoCo cumplidos |
| Pruebas anteriores conservadas | 819 |
| Pruebas nuevas RF62 | 126: 26 unitarias, 57 HTTP, 34 de persistencia/visibilidad y 9 transaccionales |
| Failures / Errors / Skipped | 0 / 0 / 0 en ambas regresiones finales |
| JaCoCo LINE BUNDLE | 95,69 % (2085 cubiertas, 94 no cubiertas; valor sin redondear 95,6861 %) |
| JaCoCo BRANCH BUNDLE | 81,44 % (601 cubiertas, 137 no cubiertas; valor sin redondear 81,4363 %) |

Se mantuvo el mínimo LINE BUNDLE de 85 %, sin cambiar el POM ni excluir código. Los resultados proceden de Surefire y de los contadores BUNDLE de `target/site/jacoco/jacoco.xml`. La falla intermitente detectada durante una ejecución previa y el ajuste exclusivo del fixture temporal se describen arriba; ambas regresiones completas posteriores pasaron.

## Estrategia PostgreSQL y pendientes de integración

Cambios de esquema a coordinar con el responsable de infraestructura, **sin ejecución en bases reales durante esta etapa**:

- `almacenes.estado_verificacion`: varchar para los tres nombres del enum, nullable y sin default/backfill.
- `almacenes.fecha_decision_verificacion`: timestamp con zona horaria compatible con Instant y precisión de microsegundos.
- `almacenes.administrador_decision_id`: bigint nullable; identificador del principal que tomó la última decisión, sin nueva tabla.
- `almacenes.motivo_verificacion`: varchar(1000) nullable.
- Revisar generación/check de valores del enum y tipos exactos contra Hibernate/esquema de QA. No inferir defaults, fechas de alta ni aprobaciones históricas.
- Evaluar con EXPLAIN y volumen real un índice por estado/nombre comercial/NIT para consulta administrativa. No cambiar nombres/tipos de las columnas existentes.
- Validar en QA carreras administrativas y de alertas, rollback, timeout/409 y planes de consulta. Despliegue/migraciones deben programarse con el responsable, fuera de esta implementación local.

RF67/RF68: no implementados; RF62 no depende de nuevos servicios de esos RF para funcionar. Si posteriormente incluyen auditoría, reportes o gestión de aliados, deberán consumir el estado real y los metadatos disponibles, sin interpretar NULL como APROBADO ni atribuir un historial completo a estos cuatro campos. Su contrato final queda por definir con el equipo.

Potenciales conflictos de integración: AlmacenEntity/AlmacenEntityMapper, repositorios de catálogo/prendas, política de búsqueda, consultas de personalización, detector y SecurityConfig son archivos compartidos. Coordinar al fusionar el trabajo del Líder para conservar los AND de RF61/RF62, los matchers específicos y el orden de bloqueos. No se modifica su matriz global de seguridad ni RF23–RF27, RF40–RF49, RF63–RF65.

No se modifican Docker, Compose, .dockerignore, .env.example, workflows, Azure, README ni diagramas UML. Actualización de diagramas y cierre de transición NULL quedan pendientes de integración final. No se implementan RF66/RF67/RF68/RF72 ni registro público, pagos, suscripciones o asociación arbitraria usuario-NIT. No se hace commit/push/PR ni se continúa con 6C.

## Inventario de archivos

14 archivos creados y 15 modificados; ningún archivo fuente eliminado. Se eliminó únicamente un log temporal generado durante la ejecución. Rutas relativas a la raíz del repositorio:

| Estado | Archivo |
| --- | --- |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/config/SecurityConfig.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/docs/AlmacenApi.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/mapper/AlmacenEntityMapper.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/entity/AlmacenEntity.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/AlmacenRepository.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/ItemCatalogoRepository.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/PersonalizacionCatalogoRepositoryImpl.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/PrendaRepository.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/specification/ItemCatalogoSpecifications.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/DetectorDisponibilidad.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/AlmacenServiceImpl.java` |
| Modificado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/CatalogoServiceImpl.java` |
| Modificado | `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/AlmacenServiceImplTest.java` |
| Modificado | `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/AlertasPersistenceIntegrationTest.java` |
| Modificado | `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/CatalogoServiceImplTest.java` |
| Creado | `docs/sprint3-etapa6b-verificacion-almacenes.md` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/VerificacionAlmacenController.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/docs/VerificacionAlmacenApi.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/DecisionVerificacionAlmacen.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/EstadoVerificacionAlmacen.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/DecisionVerificacionRequestDTO.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/VerificacionConsultaRequestDTO.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/response/VerificacionAlmacenResponseDTO.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/VisibilidadAlmacen.java` |
| Creado | `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/VerificacionAlmacenService.java` |
| Creado | `style-radar/src/test/java/edu/dosw/proyecto/style_radar/controller/VerificacionHttpIntegrationTest.java` |
| Creado | `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/VerificacionAlmacenServiceTest.java` |
| Creado | `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/VerificacionTransaccionesIntegrationTest.java` |
| Creado | `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/VerificacionVisibilidadIntegrationTest.java` |
