# Sprint 3 — Etapa 5: alertas y notificaciones

Alcance exclusivo: SR-RF-29, SR-RF-30, SR-RF-31 y SR-RF-32. Las notificaciones se persisten y el frontend las consulta por REST. No se incorpora envío externo ni infraestructura adicional.

## Base y auditoría

Rama verificada antes de escribir: `feature/s3-daniel-functional-security`. HEAD inicial: `509fb7b`, `feat(personalization): implement RF05-RF06 deterministic feed`. `git status --short` inicial vacío. Base reportada: 574 pruebas, LINE 94,21 %, BRANCH 76,28 %.

Se inspeccionaron Usuario, BusquedaGuardada, ItemCatalogo, Prenda e InventarioTalla, sus entidades, repositorios, mappers y pruebas; UsuarioPrincipal, JwtAuthFilter, SecurityConfig, GlobalExceptionHandler, SwaggerConfig, EstadoItemCalculator, BusquedaCatalogoCriteria, ItemCatalogoSpecifications, PersonalizacionCatalogoRepositoryImpl y sus servicios/pruebas actuales. El bean `Clock` existente está en `TimeConfig`; no existe una clase `ClockConfig`.

Se revisaron especialmente publicar, actualizar, retirar, registrarTallas, actualizarDisponibilidad y eliminarBusquedaGuardada. La publicación y el registro de tallas siguen creando stock cero. Retirar conserva el borrado físico existente. No se duplica el cálculo de EstadoItem ni se usa su etiqueta como evento.

## Arquitectura y responsabilidades

`AlertaController` obtiene exclusivamente `UsuarioPrincipal.getUsuarioId()` y delega a `IAlertaService`. `AlertaServiceImpl` usa `AlertaValidator`, repositorios y `AlertaEntityMapper`. `AlertaMapper` transforma dominios en respuestas HTTP. Controller no accede a repositorios ni devuelve entidades. El dominio no incorpora JPA ni Spring MVC.

`DetectorDisponibilidad` es un servicio independiente y reutilizable, sin dependencia de AlertaServiceImpl, CatalogoServiceImpl o InventarioServiceImpl. Su propagación `MANDATORY` exige la transacción del llamador. No hay dependencia circular, broker, scheduler, endpoint de reevaluación ni generación de eventos en GET.

`CriteriosBusquedaGuardada` extrae la normalización previamente local a PersonalizacionServiceImpl. Feed y alertas usan exactamente esa conversión y `ItemCatalogoSpecifications.coincidenciaExacta()`. El orden y los resultados RF05/RF06 permanecen iguales. No hay fallback de similitud RF16.

## Modelos y restricciones

| Modelo | Datos y relaciones |
| --- | --- |
| Alerta / AlertaEntity | ID, propietario Usuario LAZY sin cascadas, tipo BUSQUEDA_GUARDADA o ITEM_CATALOGO, identificador escalar positivo del objetivo, activa, fechaCreacion, fechaDesactivacion, activaUnica interna |
| Notificacion / NotificacionEntity | ID, destinatario Usuario LAZY, alerta LAZY, tipo de evento, itemCatalogoId histórico escalar, ciclo interno, mensaje breve, fechaCreacion; sin cascadas |
| DisponibilidadItemEntity | Relación uno a uno LAZY con ItemCatalogo, PK/FK item_catalogo_id, primeraDisponibilidad y ciclo no negativo; no pertenece a la respuesta REST |

La pareja no nula tipo/identificador expresa un solo objetivo en persistencia. Un CHECK limita los tipos y exige ID positivo. No hay dos columnas opcionales de objetivos ni referencia a Prenda. El request y el servicio comprueban XOR antes de persistir.

La alerta conserva un ID histórico del objetivo sin FK a la búsqueda o publicación eliminable. Esta decisión permite eliminar físicamente objetivos sin borrar historia. El servicio verifica existencia/propiedad al crear y aplica desactivación durante las eliminaciones existentes. Un borrado directo por SQL fuera de estos servicios no aplica esta política y debe evitarse. No es una relación navegable a un objetivo vigente después de su eliminación.

La notificación referencia por FK a una alerta conservada y al destinatario. El ID del ítem también es histórico, sin FK a ItemCatalogo. Ninguna de estas relaciones introduce cascade REMOVE.

Las fechas nuevas se truncan a microsegundos para mantener igualdad entre respuesta y PostgreSQL/H2 y preservar idempotencia al releer.

## Endpoints y seguridad

| Método y ruta | Request / respuesta | Códigos |
| --- | --- | --- |
| POST /api/v1/usuarios/me/alertas | Exactamente `busquedaGuardadaId` o `itemCatalogoId`; AlertaResponseDTO | 201, 400, 401, 404, 409, 422, 500 |
| GET /api/v1/usuarios/me/alertas | Lista propia, fechaCreacion DESC e id DESC | 200, 401, 500 |
| PATCH /api/v1/usuarios/me/alertas/{id} | `{"activa":false}`; AlertaResponseDTO | 200, 400, 401, 404, 409, 500 |
| GET /api/v1/usuarios/me/notificaciones | page=0, size=20; PageResponseDTO de NotificacionResponseDTO | 200, 400, 401, 500 |

Los DTO de creación no contienen usuarioId, actividad ni fechas. Los campos de identidad en query/body no participan en la selección del propietario. El destino siempre procede del JWT. PATCH rechaza true/null/ausencia y conserva la primera fecha de desactivación. No hay reactivación ni marcado como leído.

Todos los endpoints conservan `anyRequest().authenticated()` sin cambios de la matriz SecurityConfig ni de JWT. Se usa 404 tanto para alertas/búsquedas inexistentes como ajenas. Las consultas se filtran en SQL por usuario autenticado. page debe ser no negativo y size estar entre 1 y 100; valores vacíos o mal tipados producen 400. Una página con offset mayor que los resultados devuelve una página vacía y evita desbordar el offset int de JPA.

La configuración exige búsqueda propia significativa o ítem sin stock efectivo. La creación no produce una notificación inmediata. Las reglas de negocio responden 422; duplicados/integridad y conflictos concurrentes, 409. GlobalExceptionHandler se conserva y añade manejo acotado de solicitudes de alerta inválidas y ConcurrencyFailureException. Los errores no incluyen stacktraces en la respuesta.

Swagger está en `controller/docs/AlertaApi`, con DTO, restricciones, códigos y `bearerAuth` existente.

## Detección y política funcional

Publicar registra en la misma transacción un marcador de disponibilidad con primeraDisponibilidad=false y ciclo=0. No evalúa coincidencias todavía porque el inventario está vacío. Registrar tallas no genera eventos porque solo incorpora unidades cero.

ActualizarDisponibilidad obtiene el bloqueo del ítem antes de leer inventario. Calcula el stock previo y posterior mediante el dominio y su mapper existente, actualiza EstadoItem usando el calculator existente y llama al detector dentro de la misma transacción.

Solo la transición total real `0 -> positivo` avanza el ciclo de reposición del ítem. Cambios `3 -> 5`, solicitudes repetidas o una talla añadida cuando otra ya tiene stock no producen otra DISPONIBILIDAD_RESTABLECIDA. El ciclo `0 -> 2 -> 0 -> 4` permite dos reposiciones legítimas para alertas de ítem activas.

Para búsquedas guardadas, cada aumento de stock positivo puede producir la primera coincidencia efectiva por alerta e ítem. Se buscan alertas activas cuya creación sea estrictamente anterior a fechaPublicacion y que no tengan una NUEVA_COINCIDENCIA persistida para ese ítem; la exclusión se hace con NOT EXISTS en base de datos. Las búsquedas se cargan en lote. Todos los filtros efectivos se combinan con AND mediante coincidenciaExacta: query, tipo, color, talla, precioMin, precioMax, marca y estilo. Una talla filtrada debe tener unidades positivas. No se usa EstadoItem para inferir el evento.

Los almacenes/publicaciones existentes no tienen atributo privado, oculto o moderado: el catálogo elegible actual es un ItemCatalogo existente asociado a su almacén, con stock positivo. Se conserva esa política pública, sin inventar reglas del Líder Técnico.

Ejemplo verificado: alerta de talla M y estilo DEPORTIVO anterior a publicación. L recibe 5 unidades y no produce coincidencia para esa búsqueda; M recibe 3 y produce una NUEVA_COINCIDENCIA; M sube a 8 o vuelve a agotarse/reponerse y no produce otra para la misma alerta/ítem. Dos alertas con tallas distintas conservan su propia oportunidad de primera coincidencia. No se envían coincidencias de publicaciones anteriores o iguales a la activación.

El marcador primeraDisponibilidad conserva información global del ítem, pero no decide si una búsqueda ya coincidió: esa evidencia es su notificación persistida. Si falta el marcador, se inicializa al observar una reposición para seguimiento de ítem. Los artículos legados anteriores a la alerta siguen excluidos por fechaPublicacion, aunque una talla obtenga stock después.

## Deduplicación y concurrencia

`uk_alerta_activa` es UNIQUE(usuario_id, tipo_objetivo, identificador_objetivo, activa_unica). activaUnica vale true mientras activa; al desactivar queda NULL. PostgreSQL y H2 permiten varias filas con NULL en una restricción UNIQUE ordinaria. El CHECK exige correspondencia entre activa, activaUnica y fechaDesactivacion. Así se admite historial ilimitado sin dos activas idénticas, incluso si se intenta insertar fuera del servicio.

Revisión final de nulabilidad: usuario_id, tipo_objetivo e identificador_objetivo son NOT NULL; el CHECK prohíbe activa_unica NULL para una fila activa. Por ello todas las activas participan con una clave completa y el UNIQUE impide duplicados concurrentes. Las desactivadas pueden compartir objetivo porque su última columna es NULL. Es válido con UNIQUE ordinario en PostgreSQL y no requiere un índice parcial ni DDL exclusivo de ese motor. No debe sustituirse por NULLS NOT DISTINCT, que impediría múltiples historiales. Fuente: [documentación oficial de PostgreSQL sobre UNIQUE y NULL](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-UNIQUE-CONSTRAINTS). La restricción de notificaciones usa exclusivamente columnas NOT NULL.

Crear alerta bloquea el objetivo con PESSIMISTIC_WRITE; elimina la ventana entre comprobar y guardar y serializa contra eliminación/stock. La restricción UNIQUE es la protección persistente adicional. Las desactivaciones bloquean su alerta y no reescriben la fecha si ya está desactivada.

Las modificaciones del mismo ItemCatalogo se serializan con PESSIMISTIC_WRITE antes de cargar inventario. La consulta bloqueada no hace fetch joins de colecciones para evitar bloqueos PostgreSQL sobre lados opcionales de outer joins. Registrar tallas, actualizar datos y retirar comparten esta protección. La consulta compartida también se usa en registro de imágenes, dentro de una transacción de escritura existente.

El detector bloquea alertas en ID ascendente y vuelve a comprobar activa. Los marcadores/ciclos se modifican bajo el bloqueo del ítem. `uk_notificacion_evento` es UNIQUE(alerta_id, item_catalogo_id, tipo_evento, ciclo). NUEVA_COINCIDENCIA exige ciclo=0; DISPONIBILIDAD_RESTABLECIDA exige ciclo>0. La primera tiene una única combinación por alerta/ítem; la segunda una por ciclo real. No existe deduplicación exclusivamente en memoria.

Los conflictos de bloqueo/deadlock se devuelven como 409 y revierten la transacción; el cliente puede reintentar. No hay reintentos silenciosos ni absorción de errores técnicos. La garantía supone modificaciones por los servicios transaccionales; una carga manual de stock por SQL no representa un evento de la aplicación.

## Consistencia y ciclo de vida

Inventario, EstadoItem, marcador y notificaciones participan en una sola transacción JPA. El flush previo a coincidenciaExacta permite evaluar unidades nuevas con SQL, sin comprometer todavía. Un error técnico al registrar una notificación revierte todo; un error posterior al detector también revierte sus inserts. No se notifica externamente antes de commit.

EliminarBusquedaGuardada bloquea la búsqueda con la consulta existente, desactiva sus alertas activas bajo bloqueo y elimina físicamente la búsqueda. No toca otras alertas ni notificaciones. El contrato HTTP sigue siendo 204.

Retirar bloquea el ítem, desactiva sus alertas de seguimiento, elimina el marcador de disponibilidad y luego elimina físicamente ItemCatalogo, como antes. Las búsquedas activas que coincidieron con ese ítem continúan siguiendo otras publicaciones; sus notificaciones anteriores conservan itemCatalogoId. El contrato HTTP sigue siendo 204.

Los objetivos eliminados no pueden producir nuevos eventos: las alertas de ese objetivo quedan inactivas y las consultas de candidatas exigen búsqueda existente y propia. No se borra el historial de alertas ni notificaciones.

## Logging

SLF4J registra creación/desactivación de alerta, desactivación por objetivo eliminado, tipo de evento y registro de notificación, rechazos de configuración y conflictos de integridad/concurrencia. Solo se añaden IDs internos y tipos/ciclos. No se añade contenido de búsqueda, datos personales, JWT, credenciales ni un log por cada ítem evaluado. El logging previo de otros módulos permanece fuera de este cambio.

## Persistencia y despliegue PostgreSQL QA

Se necesitan tres tablas nuevas: `alertas`, `notificaciones`, `disponibilidad_items`, sus PK/FK, CHECK y restricciones UNIQUE. No hay nuevas columnas en las tablas existentes ni modificación de dependencias Maven.

Índices nuevos explícitos:

- `ix_alerta_usuario_fecha` (usuario_id, fecha_creacion, id).
- `ix_alerta_objetivo` (tipo_objetivo, identificador_objetivo, activa).
- `ix_notificacion_usuario_fecha` (usuario_id, fecha_creacion, id).
- Índices de PK y UNIQUE generados por la base para las restricciones indicadas.

El equipo de infraestructura debe coordinar y verificar la creación del esquema en QA antes del despliegue y conservar UNIQUE con la semántica ordinaria de NULL (no NULLS NOT DISTINCT). La configuración existente usa `spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}`; no se cambia ese valor ni se presupone que update verifique todas las restricciones de un esquema ya desplegado. H2 en modo PostgreSQL verifica el comportamiento y restricciones; esta etapa no ejecuta SQL ni pruebas sobre PostgreSQL QA/PROD. Debe validarse en QA el DDL generado y el comportamiento de locks bajo el motor PostgreSQL real. No se requiere backfill de marcadores para los ítems anteriores: su ausencia tiene una política definida.

## Rendimiento y límites

Las alertas de ítem se consultan solamente durante una reposición total. Una actualización sin aumento de stock positivo regresa antes de buscar alertas o ciclos. Un aumento de stock también consulta búsquedas pendientes, incluso si antes había stock en otra talla. No se cargan todos los usuarios ni todas las publicaciones. Las consultas propias de alertas/notificaciones se filtran en base de datos, y la paginación no hace fetch de colecciones.

En cada aumento positivo se evalúan A alertas de búsqueda activas anteriores a publicación y aún sin coincidencia notificada: O(A) candidatas y una consulta de existencia restringida al único ítem por búsqueda significativa. Las coincidencias ya registradas se excluyen por NOT EXISTS apoyado en el índice UNIQUE de notificaciones. Se cargan búsquedas en un lote, sin un select de relación por cada alerta; hay bloqueos explícitos por alerta y consultas de criterios, no una navegación N+1 accidental. Estas operaciones mantienen locks hasta el commit y pueden aumentar latencia con muchas alertas. La lista de alertas propias no está paginada en este alcance.

La evolución futura puede particionar candidatas por filtros indexables, proyectar coincidencias en lotes y usar outbox si se aprobara envío asíncrono. No se incorpora esa complejidad ahora. Reactivación, leído, canales externos y cambios de elegibilidad necesitarían alcance funcional posterior.

## Pruebas y validación

- AlertaValidatorTest: objetivos, desactivación y paginación en el servicio.
- AlertasPersistenceIntegrationTest: RF29-RF32; ocho filtros, AND, normalización, talla positiva, ausencia de similitud, antigüedad, cero stock, primera disponibilidad, ciclos, orden, páginas, privacidad, desactivación idempotente y ciclo de vida de objetivos.
- AlertasHttpIntegrationTest: MockMvc con WebApplicationContext y Spring Security FilterChain real; cuatro operaciones con JWT ausente/inválido/expirado/bloqueado, códigos de negocio, privacidad, DTOs, Swagger, contratos 204 y acceso previo público/protegido.
- AlertasTransaccionesIntegrationTest: transacciones independientes, persistencia tras cerrar contextos, dos hilos de creación duplicada para ambos objetivos, reposición en igual/distinta talla, coincidencia concurrente, UNIQUE/CHECK/FK, índices, LAZY, error técnico y rollback posterior al insert.
- Se conservan las 574 pruebas anteriores y sus assertions. Solo se añade un mock del nuevo detector a los constructores de CatalogoServiceImplTest, InventarioServiceImplTest y UsuarioServiceImplTest.

Resultados de la primera entrega, ejecutados desde `style-radar` (la reverificación posterior se registra abajo):

| Verificación | Resultado |
| --- | --- |
| mvn clean test | BUILD SUCCESS; 706 pruebas, 0 failures, 0 errors, 0 skipped |
| mvn clean verify | BUILD SUCCESS; 706 pruebas, 0 failures, 0 errors, 0 skipped |
| Pruebas previas | 574 conservadas y aprobadas |
| Pruebas nuevas | 132: 12 unitarias de validator, 46 JPA funcionales, 59 HTTP y 15 de transacciones/persistencia/concurrencia |
| JaCoCo BUNDLE LINE | 95,27 %; 1893 líneas cubiertas, 94 sin cubrir |
| JaCoCo BUNDLE BRANCH | 78,89 %; 497 ramas cubiertas, 133 sin cubrir |
| Umbral LINE BUNDLE | 85 % original; comprobación aprobada, sin exclusiones nuevas |
| git diff --check | Exit code 0; sin errores de whitespace. Aviso de Git LF/CRLF en PersonalizacionServiceImpl |
| HEAD final | 509fb7b7d37778d8338421121a360933b4bc529b; sin commit |

La primera ejecución focalizada detectó redondeo nanosegundos/microsegundos en dos assertions de fechas. Se corrigió la precisión de las fechas nuevas; las assertions funcionales se mantuvieron. La ejecución posterior focalizada y ambas regresiones completas aprobaron.

## Verificación final de RF30 antes del commit

La prueba explícita primeraCoincidenciaEfectivaEnMDespuesDeStockEnLNotificaUnaSolaVez se ejecutó primero contra el detector original y falló: después de M=3 había cero notificaciones. La causa era el retorno temprano con stockAnterior positivo y la evaluación limitada a primeraDisponibilidad global.

Corrección acotada a DetectorDisponibilidad y su consulta de candidatas en AlertaRepository: evaluar búsquedas aún sin coincidencia en aumentos de inventario, conservando fechaPublicacion posterior a activación, coincidenciaExacta AND, bloqueo previo del ítem, bloqueo de alertas, transacción MANDATORY y UNIQUE persistente. La reposición de alertas de ítem sigue exclusivamente la transición total 0 -> positivo; no hay cambios de esquema, endpoints, infraestructura u otros RF.

Se añadieron ocho casos: cuatro JPA funcionales (M tardía, reposición total independiente, publicación anterior y dos búsquedas por talla), una coincidencia tardía concurrente que conserva un solo evento de reposición, un rollback técnico de M sin perder L ni historia previa y dos casos concurrentes con múltiples historiales NULL para ambos tipos de objetivo. Se conservan las 706 pruebas de la entrega anterior.

Pendiente PostgreSQL QA: comprobar el DDL realmente desplegado (NOT NULL, ck_alerta_actividad y ambos UNIQUE ordinarios), la preservación de múltiples filas históricas NULL y los locks/concurrencia sobre PostgreSQL real. H2 prueba esta semántica y la concurrencia de la aplicación, pero no sustituye la validación del despliegue PostgreSQL. No se necesita DDL específico adicional para esta corrección y no se ha ejecutado SQL en QA/PROD.

Resultados de esta reverificación, ejecutada desde `style-radar`:

| Verificación | Resultado |
| --- | --- |
| mvn clean test | BUILD SUCCESS; 714 pruebas, 0 failures, 0 errors, 0 skipped |
| mvn clean verify | BUILD SUCCESS; 714 pruebas, 0 failures, 0 errors, 0 skipped; umbral JaCoCo aprobado |
| Regresión | 706 pruebas anteriores conservadas; ocho casos añadidos |
| JaCoCo BUNDLE LINE | 95,28 %; 1896 líneas cubiertas, 94 sin cubrir |
| JaCoCo BUNDLE BRANCH | 79,05 %; 498 ramas cubiertas, 132 sin cubrir |

Esta reverificación modifica cinco archivos de la entrega aún sin commit: DetectorDisponibilidad.java, AlertaRepository.java, AlertasPersistenceIntegrationTest.java, AlertasTransaccionesIntegrationTest.java y este documento. No añade ni elimina archivos; HEAD permanece en 509fb7b7d37778d8338421121a360933b4bc529b.

## Inventario de archivos

Estado final: 31 archivos nuevos sin seguimiento (`??`), 10 modificados (`M`), 0 eliminados, 0 staged. `git diff --stat` muestra los diez archivos ya versionados: 53 inserciones y 25 eliminaciones; Git no incluye los nuevos sin seguimiento en ese comando.

Para las rutas Java siguientes, el prefijo es `style-radar/src/main/java/edu/dosw/proyecto/style_radar/`:

| Archivos creados | |
| --- | --- |
| Controller y Swagger | controller/AlertaController.java; controller/docs/AlertaApi.java |
| Excepción | exception/SolicitudAlertaInvalidaException.java |
| Mappers | mapper/AlertaEntityMapper.java; mapper/AlertaMapper.java |
| Dominio | model/domain/Alerta.java; model/domain/Notificacion.java; model/domain/TipoEventoNotificacion.java; model/domain/TipoObjetivoAlerta.java |
| Request DTO | model/dto/request/CrearAlertaRequestDTO.java; model/dto/request/DesactivarAlertaRequestDTO.java; model/dto/request/NotificacionesRequestDTO.java |
| Response DTO | model/dto/response/AlertaResponseDTO.java; model/dto/response/NotificacionResponseDTO.java |
| Entidades | model/entity/AlertaEntity.java; model/entity/DisponibilidadItemEntity.java; model/entity/NotificacionEntity.java |
| Repositorios | repository/AlertaRepository.java; repository/DisponibilidadItemRepository.java; repository/NotificacionRepository.java |
| Servicios | service/CriteriosBusquedaGuardada.java; service/DetectorDisponibilidad.java; service/IAlertaService.java; service/impl/AlertaServiceImpl.java |
| Validator | validator/AlertaValidator.java |

Prefijo de pruebas: `style-radar/src/test/java/edu/dosw/proyecto/style_radar/`:

- controller/AlertasHttpIntegrationTest.java.
- service/impl/AlertasPersistenceIntegrationTest.java.
- service/impl/AlertasTransaccionesIntegrationTest.java.
- support/AlertasFixtures.java.
- validator/AlertaValidatorTest.java.

Documentación creada: `docs/sprint3-etapa5-alertas.md`.

Archivos existentes modificados (mismos prefijos de producción/pruebas):

- exception/GlobalExceptionHandler.java.
- repository/BusquedaGuardadaRepository.java.
- repository/ItemCatalogoRepository.java.
- service/impl/CatalogoServiceImpl.java.
- service/impl/InventarioServiceImpl.java.
- service/impl/PersonalizacionServiceImpl.java.
- service/impl/UsuarioServiceImpl.java.
- service/impl/CatalogoServiceImplTest.java (test).
- service/impl/InventarioServiceImplTest.java (test).
- service/impl/UsuarioServiceImplTest.java (test).

## Cierre del Sprint

Pendientes de actualización manual: diagramas de clases/datos, relaciones de Alerta/Notificacion/DisponibilidadItem, secuencias de inventario y eliminación y contratos REST. No se generaron diagramas, imágenes, Mermaid ni PlantUML. README se actualizará cuando se indique el cierre del Sprint.

Coordinación con el Líder Técnico: cambios en GlobalExceptionHandler y consultas compartidas de ItemCatalogoRepository/BusquedaGuardadaRepository; futuro criterio de catálogo público si su módulo incorpora visibilidad/moderación. No se modifica su matriz de seguridad, JWT, Haversine ni se implementan RF de otros responsables.

Dockerfile, docker-compose.yml, .dockerignore, .env.example, GitHub Actions y Azure no se modifican. No hay commit, push, PR ni avance a Etapa 6.
