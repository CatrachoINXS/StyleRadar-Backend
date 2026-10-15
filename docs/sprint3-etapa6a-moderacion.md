# Sprint 3 · Etapa 6A · RF61: moderación de publicaciones

Implementación exclusiva de SR-RF-61. RF62 y RF66 quedan fuera de este bloque.
No se ejecutan cambios sobre PostgreSQL real, infraestructura, Azure o Docker Hub.

## Línea base y alcance

- Rama: `feature/s3-daniel-functional-security`.
- HEAD inicial: `948c0b528e335b3b18e51bb66f9052183a5de844`.
- Commit: `feat(alerts): implement RF29-RF32 availability alerts`.
- Working tree inicial limpio. Se conservan las 714 pruebas anteriores.
- Sin commit, push, PR, cambio de rama, README ni diagramas.

## Estados y compatibilidad

`EstadoModeracion` es independiente de `EstadoItem` e inventario:

| Estado de moderación | Visible por moderación |
| --- | --- |
| NO_REQUERIDA | Sí |
| PENDIENTE | No |
| APROBADA | Sí |
| RECHAZADA | No |
| NULL histórico | Sí, equivalente a NO_REQUERIDA |

La visibilidad por moderación no garantiza stock ni reemplaza los filtros de
disponibilidad existentes. Aprobar no modifica `EstadoItem`. Rechazar no elimina
la publicación, prendas, inventario ni imágenes.

`ItemCatalogoEntity` conserva el constructor anterior y sus relaciones LAZY.
Los campos nuevos son `estadoModeracion` (EnumType.STRING),
`fechaDecisionModeracion`, `administradorDecisionId` y `motivoModeracion`
(máximo 1000 caracteres). No se crea otra tabla. El ID administrativo es un
valor escalar; no se persisten credenciales ni JWT ni datos de contacto.

Las nuevas publicaciones ordinarias mantienen NO_REQUERIDA. El mapper de creación
ignora expresamente los metadatos de moderación y conserva ese valor predeterminado.
El dominio público `ItemCatalogo` y `CatalogoItemResponseDTO` conservan su contrato;
los detalles administrativos usan una proyección independiente.

## Selección provisional y transiciones

Solo un administrador de StyleRadar puede solicitar revisión manual:

- NO_REQUERIDA (incluido NULL) → PENDIENTE.
- PENDIENTE → APROBADA o RECHAZADA.
- APROBADA o RECHAZADA → PENDIENTE mediante nueva solicitud explícita.

Solicitar otra revisión de un ítem ya PENDIENTE devuelve 422. Decidir un ítem que
no está PENDIENTE devuelve 422; ninguna decisión repetida es un éxito silencioso.
RECHAZADA → APROBADA directamente está prohibido. La solicitud de revisión no
es una decisión ni cambia los metadatos de la última decisión.

La política automática para seleccionar publicaciones requiere acuerdo del equipo.
No existe detección de fraude, contenido ofensivo, IA ni aprobación universal.
Editar una publicación mantiene su moderación actual. La revisión automática
después de editar también requiere una decisión de negocio futura.

## API y seguridad

Todas estas rutas requieren JWT válido, cuenta activa y ROLE_ADMIN_STYLERADAR:

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | /api/v1/admin/catalogo/moderacion | Página administrativa |
| PATCH | /api/v1/admin/catalogo/{itemId}/moderacion | Última decisión actualizada |
| POST | /api/v1/admin/catalogo/{itemId}/moderacion/solicitar-revision | Estado PENDIENTE |

GET acepta `estado` (por defecto PENDIENTE), `page=0` y `size=20`. page debe ser
no negativo; size de 1 a 100. Orden global: fechaPublicacion ASC, itemId ASC.
NO_REQUERIDA incluye registros NULL. Se proyectan IDs antes del LIMIT/OFFSET y se
cargan almacén y prenda con EntityGraph sin cargar colecciones para paginar.

PATCH acepta exclusivamente las decisiones APROBADA y RECHAZADA:

```json
{"decision":"APROBADA","motivo":"Información revisada"}
```

```json
{"decision":"RECHAZADA","motivo":"Información incompleta"}
```

Rechazar exige motivo no vacío. Aprobar permite omitirlo. El motivo se normaliza
con strip y se limita a 1000 caracteres antes de normalizar. Bean Validation cubre
el request y la consulta. Campos JSON desconocidos, incluida cualquier identidad
administrativa, se rechazan con 400. El servidor toma el ID de UsuarioPrincipal y
la fecha de Clock (UTC, precisión de microsegundos para persistencia).

POST no necesita body. Ejemplo de estado de respuesta: `estadoModeracion=PENDIENTE`.
Las respuestas incluyen itemId, almacenNit, prenda, precio, estadoModeracion,
fechaPublicacion, fechaDecisionModeracion, administradorDecisionId y motivoModeracion.

SecurityConfig agrega únicamente tres matchers específicos con
hasRole("ADMIN_STYLERADAR"). No depende de anyRequest().authenticated ni de
anotaciones de métodos inactivas. El servicio vuelve a comprobar el rol antes
de escribir. La matriz global y propiedad de recursos quedan a cargo del Líder.

Se conserva ErrorResponseDTO y GlobalExceptionHandler sin cambios: 200 correcto,
400 request inválido, 401 token ausente/inválido o cuenta bloqueada, 403 rol ajeno,
404 ítem inexistente, 409 fallo real de bloqueo/concurrencia, 422 transición
inválida y 500 fallo técnico. Tras una carrera serializada, el segundo decisor
normalmente recibe 422 porque el ganador ya resolvió la revisión; no sobrescribe
la decisión. Un fallo de adquisición de bloqueo corresponde a 409.

La interfaz `controller/docs/ModeracionCatalogoApi` documenta las tres rutas,
bearerAuth, rol, validación, errores y ejemplos. SwaggerConfig existente ya
agrega bearerAuth a rutas protegidas y se conserva sin cambios.

## Trazabilidad y transacciones

Se conserva la última decisión: administrador autenticado, fecha, motivo y estado
actual. Una revisión nueva mantiene los datos anteriores hasta la siguiente decisión.
El MVP no tiene un historial completo ni un campo separado para el estado de la
decisión anterior: cuando el estado actual es PENDIENTE, los metadatos presentes
pertenecen a la decisión previa. Al decidir nuevamente se reemplazan. La solicitud
queda identificada en logging, sin almacenar un historial de solicitudes.

Solicitud y decisión son transaccionales. Se reutiliza findForUpdate con
PESSIMISTIC_WRITE, el mismo bloqueo de ítem usado por inventario/alertas.
Se verifica el estado después de adquirirlo. Flush ocurre dentro de la transacción;
un fallo revierte estado y todos los metadatos. Las consultas públicas no agregan
bloqueos. La consulta administrativa usa REPEATABLE_READ para coherencia de IDs,
total y datos. Logs contienen IDs y estado, sin motivo, imágenes ni JWT.

## Auditoría e integración de visibilidad

`VisibilidadModeracion` centraliza el predicado Criteria/Specification, la expresión
JPQL (alias i) y la regla para eventos. NULL, NO_REQUERIDA y APROBADA son visibles.

- Catálogo del almacén: consulta pública separada; conserva cálculo de disponibilidad.
- Resumen de mapa y novedades: filtra en la consulta de IDs antes de contar/paginar;
  conserva la ventana de siete días y orden previo.
- Búsqueda global, orden especial y similitud: Specification filtra candidatos en BD;
  mantiene fórmulas, desempates, parámetros y reglas previas de disponibilidad.
- RF05/feed y RF06/recomendaciones: el predicado compartido participa en count e IDs,
  antes de prioridades, orden y paginación. No se añade fallback ni score.
- Prendas: `/api/v1/prendas` expone definiciones y prendas creadas al publicar. La
  consulta conserva definiciones sin publicación; una prenda vinculada a catálogo
  exige al menos una publicación visible. No exige inventario, preservando RF07.
  Una prenda compartida por varios ítems sigue visible si alguno es visible.
- Playlists: también podían exponer la misma prenda. Se consulta en lote cuáles IDs
  están ocultos y se filtra solo la proyección de prendas de cada respuesta. No se
  eliminan asociaciones y se puede retirar una prenda oculta. No hay paginación
  de prendas anidadas ni se modifica el count de publicaciones de catálogo.
- Segunda mano crea una PrendaEntity propia por publicación, sin reutilizar los
  IDs de prendas del catálogo aliado. No es una ruta alternativa al ItemCatalogo
  moderado ni se extiende RF61 a segunda mano.
- Imágenes: solo existe POST de registro, protegido por autenticación previa;
  no existe GET/descarga de imagen ni catálogo que entregue sus bytes públicamente.

Los métodos internos findById, findByIdAndAlmacen_Nit, findForUpdate y findByIdIn
conservan acceso completo. Actualizar, retirar, inventario y registrar imágenes
pueden seguir localizando ítems ocultos. DELETE conserva su comportamiento físico.

## Alertas RF29–RF32

DetectorDisponibilidad descarta eventos de ítems PENDIENTES/RECHAZADOS antes de
generar coincidencias o reposiciones públicas. Una actualización de inventario
no elude la moderación. Se conservan bloqueos, transacciones, deduplicación por
alerta/ítem, reposiciones y coincidencias tardías por talla. Los eventos ocultos
no crean notificaciones ni avanzan el ciclo público de detección.

Moderar no borra alertas ni notificaciones históricas. Solicitar revisión, rechazar
y aprobar no llaman al detector. Aprobar con stock existente no envía eventos
retroactivos. Un evento futuro de inventario sobre un ítem visible sigue las reglas
vigentes. La creación/consulta de alertas existentes conserva su contrato y no
expone contenido de prenda: el detector impide nuevas notificaciones del ítem oculto.

## PostgreSQL pendiente y QA

No se ejecutó SQL sobre una base real. Se requiere coordinar una migración revisada:

1. Agregar columnas nullable: estado_moderacion VARCHAR para los cuatro valores,
   fecha_decision_moderacion TIMESTAMP WITH TIME ZONE, administrador_decision_id BIGINT
   y motivo_moderacion VARCHAR(1000).
2. Crear índice idx_item_moderacion_fecha_id sobre
   (estado_moderacion, fecha_publicacion, id), reflejado en el modelo JPA y validado en H2.
3. Backfill no destructivo pendiente: actualizar únicamente estado_moderacion IS NULL
   a NO_REQUERIDA; jamás a PENDIENTE. No tocar decisiones, publicaciones ni inventario.
4. Mantener temporalmente la compatibilidad con NULL durante despliegues y backfill.
5. QA PostgreSQL debe confirmar DDL/naming, planes de consulta, precisión UTC,
   comportamiento del bloqueo, timeouts y carga concurrente. H2 no reemplaza esta QA.

La búsqueda con similitud y orden especial conserva su estrategia previa de
candidatos en memoria, ya filtrados en BD. La paginación con EntityGraph de
inventario de la búsqueda directa también conserva la arquitectura previa;
RF61 no agrega filtrado de moderación después de LIMIT ni cambia el total.

## Validación automatizada

Pruebas nuevas: ModeracionHttpIntegrationTest, ModeracionCatalogoServiceTest,
ModeracionVisibilidadIntegrationTest y ModeracionTransaccionesIntegrationTest.
Cubren los cinco casos (incluido NULL), transiciones, motivos, Clock, administrador,
seguridad real en las tres rutas, Swagger, caminos públicos, paginación/count,
prendas compartidas, playlists, historial de alertas, falta de eventos retroactivos,
concurrencia y rollback. Solo se actualizan los mocks de las consultas públicas
en CatalogoServiceImplTest y PrendaServiceImplTest, preservando sus assertions.

`mvn clean test`: BUILD SUCCESS, 819 pruebas (714 existentes + 105 nuevas),
0 failures, 0 errors, 0 skipped.

`mvn clean verify`: BUILD SUCCESS, 819 pruebas, 0 failures, 0 errors, 0 skipped.
JaCoCo LINE BUNDLE: **95,43 %** (1986/2081). JaCoCo BRANCH: **80,38 %** (553/688).
El check de cobertura de verify termina con todos los controles aprobados.

Distribución de las 105 pruebas nuevas: 46 HTTP, 26 de negocio/Clock,
28 de visibilidad/persistencia/alertas y 5 de transacciones/concurrencia.
El umbral JaCoCo LINE BUNDLE >= 85 % se conserva sin exclusiones artificiales.

## Coordinación

Archivos compartidos relevantes: SecurityConfig, ItemCatalogoEntity,
ItemCatalogoRepository, ItemCatalogoSpecifications, PersonalizacionCatalogoRepositoryImpl,
DetectorDisponibilidad, mappers y servicios públicos. No se detectaron modificaciones
locales paralelas al iniciar. El Líder debe revisar la composición de sus reglas
con los tres matchers específicos antes de integrar cambios de seguridad.
Diagramas siguen pendientes de actualización por el equipo; no se modifican en 6A.

## Inventario de archivos

Nuevos (14), dentro de `style-radar/src/main/java/edu/dosw/proyecto/style_radar/`
salvo donde se indica otra raíz:

- controller/ModeracionCatalogoController.java
- controller/docs/ModeracionCatalogoApi.java
- model/domain/EstadoModeracion.java
- model/domain/DecisionModeracion.java
- model/dto/request/DecisionModeracionRequestDTO.java
- model/dto/request/ModeracionConsultaRequestDTO.java
- model/dto/response/ModeracionItemResponseDTO.java
- service/VisibilidadModeracion.java
- service/impl/ModeracionCatalogoService.java
- src/test/java/.../controller/ModeracionHttpIntegrationTest.java
- src/test/java/.../service/impl/ModeracionCatalogoServiceTest.java
- src/test/java/.../service/impl/ModeracionTransaccionesIntegrationTest.java
- src/test/java/.../service/impl/ModeracionVisibilidadIntegrationTest.java
- docs/sprint3-etapa6a-moderacion.md (raíz del repositorio)

Modificados (13), con la misma raíz Java principal salvo los tests:

- config/SecurityConfig.java
- mapper/ItemCatalogoEntityMapper.java
- model/entity/ItemCatalogoEntity.java
- repository/ItemCatalogoRepository.java
- repository/PersonalizacionCatalogoRepositoryImpl.java
- repository/PrendaRepository.java
- repository/specification/ItemCatalogoSpecifications.java
- service/DetectorDisponibilidad.java
- service/impl/CatalogoServiceImpl.java
- service/impl/PlaylistServiceImpl.java
- service/impl/PrendaServiceImpl.java
- src/test/java/.../service/impl/CatalogoServiceImplTest.java
- src/test/java/.../service/impl/PrendaServiceImplTest.java

Eliminados: ninguno. `git diff --stat` contabiliza solo los 13 archivos ya
versionados; los 14 nuevos siguen sin seguimiento hasta que el usuario decida agregarlos.

Control Git final: git diff --check sin errores; status contiene únicamente
13 archivos modificados y 14 nuevos, sin eliminaciones ni cambios staged.
El diff de archivos versionados registra 112 inserciones y 25 eliminaciones de líneas.
Los avisos LF/CRLF son normalización de Git en Windows, no errores de whitespace.
Rama y HEAD permanecen iguales a la línea base. Listo para commit del código RF61,
con migración y validaciones PostgreSQL pendientes antes de desplegar.
