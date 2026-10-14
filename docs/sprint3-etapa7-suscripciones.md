# Sprint 3 — Etapa 7: suscripciones de almacenes

## Alcance y auditoría

Implementa exclusivamente SR-RF-67 (registrar una suscripción de un almacén verificado a un plan configurado) y SR-RF-68 (cancelar la renovación del siguiente período). La asociación de administradores con almacenes es el mecanismo mínimo habilitador de su autorización; no constituye un módulo de personal.

Base verificada: rama `feature/s3-daniel-functional-security`, HEAD `c1b9c3440fecbf223898262cc2ac40f4187e658d`, commit `feat(admin): implement RF66 account status management`, árbol inicialmente limpio. La línea base reportada comprende 1092 pruebas, LINE 95,78 % y BRANCH 83,09 %.

Se inspeccionaron Almacen/AlmacenEntity/AlmacenRepository, EstadoVerificacionAlmacen, VerificacionAlmacenService, VisibilidadAlmacen, Usuario/UsuarioEntity/UsuarioRepository, EstadoCuenta, Rol, UsuarioPrincipal, UsuarioUserDetailsService, JwtAuthFilter, SecurityConfig, CredencialEntity/Repository, CatalogoServiceImpl, AlmacenServiceImpl, ItemCatalogoRepository, GestionEstadoCuentaService, BloqueoCuentaService, GlobalExceptionHandler, ErrorResponseDTO, TimeConfig/Clock, SwaggerConfig y PageResponseDTO. La búsqueda de modelos de planes, contratos, pagos, renovación y vínculos de titularidad no encontró equivalentes reutilizables. `administradorDecisionId` de RF62 identifica al actor de una decisión, no al administrador de la tienda. Se reutilizan el lock por NIT y los locks de cuentas de RF66.

## Frontera comercial

Esta versión registra suscripciones e intención de renovación. No confirma, simula ni ejecuta cobros; no declara suscripciones pagadas o facturadas. No existe integración PSE, Stripe, PayPal, tarjetas, webhooks ni pasarela ficticia. No guarda información bancaria, transacciones inventadas o comprobantes.

No hay precios, nombres ni beneficios comerciales oficiales configurados por el código. No se siembran planes productivos o de QA al iniciar. El equipo comercial debe definir condiciones antes de habilitar planes reales. Solo deben aprovisionarse planes cuya activación no requiera una confirmación de pago pendiente. Si un plan requiere pago obligatorio, no debe habilitarse en este MVP: su activación comercial deberá esperar una integración verificable y una evolución del contrato de autorización de pago. El estado ACTIVA acredita vigencia del registro, nunca pago.

`renovacionHabilitada=true` es una intención provisional inicial, no un consentimiento a cobros implementados ni evidencia de renovación ejecutada. No hay scheduler, extensión automática, workers o cobros. Una suscripción vencida no sigue vigente por esa preferencia. No existe endpoint de reactivación de la renovación.

## Planes y aprovisionamiento confiable

`PlanSuscripcion` / `PlanSuscripcionEntity`: id Long generado, nombre no vacío de hasta 150 caracteres, periodo Integer positivo, unidad explícita `MESES_CALENDARIO`, activo boolean. La unidad se persiste como STRING. JPA/Bean Validation rechaza configuraciones inválidas y un CHECK SQL exige periodo positivo y unidad admitida. El servicio vuelve a comprobar la duración antes de calcular el período. No existe precio obligatorio ni administración comercial pública.

QA puede crear planes mediante fixtures JPA usando `PlanSuscripcionRepository` en pruebas H2. Para configurar un plan real, un operador autorizado debe usar el procedimiento de cambio de datos del equipo, con nombre y número de meses aprobados, inicialmente deshabilitado, y habilitarlo después de validar sus condiciones. No hay endpoint de creación pública. El siguiente es un patrón parametrizado de aprovisionamiento, no datos ficticios ni un script ejecutado:

```sql
INSERT INTO planes_suscripcion (nombre, periodo, unidad_duracion, activo)
VALUES (:nombre_aprobado, :meses_aprobados, 'MESES_CALENDARIO', false);
-- Tras la revisión del equipo y únicamente si no exige pago pendiente:
UPDATE planes_suscripcion SET activo = true WHERE id = :plan_aprobado_id;
```

El operador debe aplicar las mismas validaciones al usar SQL: nombre válido, duración positiva dentro del rango temporal soportado por PostgreSQL y ausencia de condición económica pendiente. Los parámetros son valores aprobados por el equipo, no valores recibidos de un cliente. No se ejecutaron modificaciones sobre PostgreSQL real. Antes de modificar condiciones de un plan con contratos, el equipo debe definir su política; las fechas de períodos ya registrados no se recalculan.

## Asociación segura usuario–NIT

`AdministradorAlmacenEntity` conserva usuario, almacén, asignadoPor y fechaAsignacion UTC. Son relaciones ManyToOne sin cascade a identidades. Tres FK aseguran que usuario, almacén y actor administrativo existan. `uk_administrador_almacen(usuario_id, almacen_nit)` evita duplicados. No hay asociaciones inferidas por correoContacto, texto libre, rol o registros históricos.

`PUT /api/v1/admin/almacenes/{nit}/administradores/{usuarioId}` es la única operación habilitadora. Requiere JWT, cuenta operativa y ROLE_ADMIN_STYLERADAR. No recibe body. El objetivo debe existir y tener ADMIN_ALMACEN en BD; el NIT debe existir. La operación es idempotente y conserva el actor/fecha originales. No cambia Rol, correos, verificación o suscripciones. El administrador debe verificar fuera de esta API la legitimidad de la asignación antes de aprovisionarla; la API registra su decisión administrativa confiable.

Un comprador o administrador de tienda no puede autoasignarse un NIT. ADMIN_STYLERADAR tampoco obtiene autorización automática para activar o cancelar suscripciones: necesita ADMIN_ALMACEN y el vínculo persistido como cualquier gestor de tienda. La asociación no crea usuarios o almacenes. No hay backfill de titularidad histórica ni administración adicional de personal.

## Modelo y vigencia de suscripción

`SuscripcionAlmacen` / `SuscripcionAlmacenEntity` conserva id, almacen, plan, estadoSuscripcion (`ACTIVA`, `FINALIZADA`, STRING), fechaCreacion, inicioPeriodo, finPeriodo, renovacionHabilitada y fechaCancelacionRenovacion. Se conserva todo el historial; no se reutiliza una fila anterior para una nueva contratación.

Todas las fechas de esta funcionalidad proceden del Clock inyectado y se truncan a microsegundos antes de persistir, coherente con PostgreSQL. `fechaCreacion=inicioPeriodo=Clock.instant()` después de adquirir los locks. `finPeriodo=inicioPeriodo.atZone(UTC).plusMonths(periodo).toInstant()`. No se usa Instant.plus(MONTHS), fechas del request ni el huso local del equipo.

La vigencia es `[inicioPeriodo, finPeriodo)`: inicio incluido, fin excluido, con instantes completos. Debe estar persistida ACTIVA y cumplir inicio <= ahora < fin. El DTO muestra estado efectivo FINALIZADA tras el vencimiento aun si el campo persistido conserva ACTIVA. La consulta de vigente devuelve 404 cuando no hay período vigente. El cálculo conserva hora UTC y ajusta al último día válido del mes: 31 de enero de 2024 más un mes termina el 29 de febrero; en 2023 termina el 28.

La creación requiere RF62 `APROBADO` explícito. PENDIENTE, RECHAZADO y NULL histórico devuelven 422. NULL puede seguir visible por compatibilidad de RF62, pero no equivale a aprobación. Suscribir no cambia la verificación, sus fechas, su actor ni su motivo.

Si un almacén posteriormente pasa a PENDIENTE o RECHAZADO, no se borra ni acorta la suscripción existente. Su administrador vinculado puede consultar y cancelar la intención de renovación vigente. La visibilidad pública sigue exclusivamente RF61/RF62; un rechazo permanece oculto aunque exista contrato. El efecto comercial de un rechazo posterior y eventuales reembolsos queda pendiente de definición del equipo, sin implementación ficticia.

## RF67, unicidad y transacciones

La creación valida identidad autenticada, rol ADMIN_ALMACEN actual, estado operativo actual y vínculo al NIT, obtiene el almacén mediante `findForUpdate`, exige APROBADO, obtiene el plan persistido y exige activo/duración válida. Solo el identificador del plan proviene del request. Crea un período UTC nuevo y responde 201.

Todas las escrituras usan `@Transactional` con READ_COMMITTED. Primero se bloquean las filas de usuarios necesarias mediante BloqueoCuentaService y se revalidan cuenta y roles mediante proyecciones actuales. Después se bloquea la fila del almacén con PESSIMISTIC_WRITE, compartida con RF62. Esto serializa escrituras sobre el mismo NIT y evita decisiones simultáneas RF66 sobre las cuentas involucradas. Los locks de usuarios se toman en orden estable. Almacenes de usuarios distintos no requieren un bloqueo global compartido; operaciones de la misma cuenta comparten su lock por coherencia con RF66.

La columna auxiliar `almacen_activo_nit` contiene exactamente el NIT para ACTIVA y NULL para FINALIZADA. UNIQUE `uk_suscripcion_almacen_activa` impide dos filas ACTIVA del mismo almacén incluso si se omite la comprobación del servicio. Un CHECK exige consistencia entre estado, clave auxiliar y NIT. H2 y PostgreSQL admiten varios NULL bajo UNIQUE ordinario; no debe usarse `NULLS NOT DISTINCT` en QA.

Si la fila ACTIVA anterior no ha vencido se responde 409, aunque el plan solicitado sea diferente o su renovación esté cancelada. Si ya venció, se marca FINALIZADA y se limpia la clave auxiliar; se hace flush antes del INSERT. La finalización y el nuevo registro pertenecen a una sola transacción: un fallo posterior revierte ambos. No se cierran períodos futuros para evadir unicidad. No hay tarea de finalización automática: el estado efectivo se interpreta en consultas; la fila vencida se finaliza al necesitar una nueva activación.

## RF68 e idempotencia

Cancelar renovación requiere identidad, rol, cuenta, pertenencia al NIT y una suscripción de ese almacén efectivamente vigente. Adquiere los mismos locks que RF67. Solo permite `renovacionHabilitada=false`, registra fechaCancelacionRenovacion desde Clock y responde 200. No cambia inicioPeriodo, finPeriodo, plan, almacén o fechaCreacion; permanece ACTIVA mientras dure el período. No borra derechos vigentes ni registros.

Una segunda cancelación durante la vigencia devuelve el mismo contrato con la primera fecha intacta; no extiende fechas ni reactiva. Ausencia, contrato ajeno, FINALIZADA o vencimiento devuelve 404, no éxito ficticio. La API no ejecuta un proceso comercial de renovación. No se modifican reglas de mapa, catálogo, recomendaciones, moderación ni alertas.

## Endpoints y contratos

| Método y ruta | Acceso | Resultado |
| --- | --- | --- |
| GET /api/v1/planes-suscripcion | Público | 200 lista de habilitados, solo id/nombre/periodo/unidadDuracion; [] si vacía |
| POST /api/v1/almacenes/{nit}/suscripciones | ADMIN_ALMACEN vinculado, cuenta operativa | 201 y SuscripcionAlmacenResponseDTO; body solo planId |
| GET /api/v1/almacenes/{nit}/suscripciones/vigente | ADMIN_ALMACEN vinculado | 200 vigente o 404 |
| PATCH /api/v1/almacenes/{nit}/suscripciones/{id}/renovacion | ADMIN_ALMACEN vinculado | 200; body solo renovacionHabilitada=false |
| GET /api/v1/almacenes/{nit}/suscripciones | ADMIN_ALMACEN vinculado | PageResponseDTO; page=0, size=20, máximo 100 |
| PUT /api/v1/admin/almacenes/{nit}/administradores/{usuarioId} | ADMIN_STYLERADAR | 200 asociación nueva o existente; sin body |

SuscripcionAlmacenResponseDTO expone id, almacenNit, planId, nombrePlan, estadoSuscripcion efectivo, fechaCreacion, inicioPeriodo, finPeriodo, renovacionHabilitada y fechaCancelacionRenovacion. No se devuelven entities ni datos bancarios. DTOs request usan Bean Validation y rechazan campos JSON desconocidos con 400, incluso usuarioId, precio, fechas, estado o identidad de aprobación. La identidad gestora siempre proviene de UsuarioPrincipal.getUsuarioId().

El historial se pagina en BD por NIT, fechaCreacion DESC e id DESC. No se cargan todos los contratos para después paginar. Los offsets que exceden Integer.MAX_VALUE devuelven página vacía con el conteo verdadero sin desbordar la API JPA.

SecurityConfig agrega exclusivamente estos matchers; conserva la matriz restante. JwtAuthFilter/UsuarioPrincipal/EstadoCuenta no se modifican. Cuenta INACTIVA o BLOQUEADA devuelve 401 aunque el JWT se emitiera antes de la suspensión. Se conserva la compatibilidad de cuentas históricas NULL como operativas definida por RF66; esta política no se aplica al estado de verificación NULL del almacén. Rol incorrecto devuelve 403 y NIT ajeno o sin vínculo 404 para evitar revelar contratos.

Se reutilizan GlobalExceptionHandler, ErrorResponseDTO y SecurityErrorHandler. Errores: 400 solicitud/paginación inválida, 401 autenticación/cuenta, 403 rol, 404 recurso ausente/ajeno, 409 duplicado/concurrencia/integridad, 422 regla de elegibilidad o configuración, 500 fallo técnico con respuesta genérica sin SQL o stacktrace. Swagger usa interfaces controller/docs y Bearer en rutas privadas; el catálogo de planes queda explícitamente público. Logs de asociación, creación y cancelación contienen identificadores y operación, sin JWT, secretos o pagos; “aplicado-en-transaccion” no afirma que ya ocurrió el commit.

## Esquema PostgreSQL QA

Nuevas tablas: `planes_suscripcion`, `administradores_almacen`, `suscripciones_almacen`. IDs BIGINT generados. Relaciones sin cascade: suscripción -> almacenes/planes; asociación -> usuarios/almacenes/usuario actor. Instantes requieren almacenamiento UTC compatible con Hibernate/PostgreSQL (timestamp con zona, precisión de microsegundos); boolean NOT NULL para activo/renovación y ENUM como VARCHAR STRING.

Restricciones e índices adicionales:

- Plan: nombre, periodo, unidad y activo NOT NULL; CHECK periodo > 0 y unidad MESES_CALENDARIO.
- Asociación: todas las FK/fecha NOT NULL; UNIQUE(usuario_id, almacen_nit).
- Suscripción: todas las FK, estado, creación, inicio, fin y renovación NOT NULL. Clave activa y fecha de cancelación admiten NULL según estado/intención.
- UNIQUE(almacen_activo_nit), CHECK de estado/clave/NIT, fin > inicio, creación = inicio y coherencia boolean/fecha de cancelación.
- `ix_suscripcion_historial(almacen_nit, fecha_creacion, id)` para filtro y orden; PostgreSQL puede recorrer el índice en sentido inverso dentro del mismo NIT.

No se alteran tablas existentes ni se inventan relaciones históricas. El equipo de infraestructura debe revisar/aplicar el DDL que corresponde a estas entidades mediante su procedimiento de esquema: no asumir que ddl-auto=update añadirá todos los CHECK, índices o restricciones en un entorno existente. No se incorporaron herramientas de migración ni dependencias.

Pendientes QA PostgreSQL: revisar tipos/precisión y generación del esquema, FK y CHECK, UNIQUE con múltiples NULL y transiciones flush-before-insert, planes inválidos por SQL, locks por fila bajo READ_COMMITTED, concurrencia/rollback y planes con duración comercial aprobada dentro del rango PostgreSQL. Se verifican H2/JPA localmente; no se afirma validación contra PostgreSQL real. No hay diferencias funcionales previstas en la estrategia de locks/NULL, pero sus tiempos de espera y el rango temporal del motor deben validarse en QA.

## Pruebas y cierre

Pruebas nuevas en SuscripcionAlmacenServiceImplTest, SuscripcionTemporalTest y AutorizacionSuscripcionServiceTest: RF67/RF68, aprobación explícita vs visibilidad NULL, plan válido/inactivo/inexistente/duración inválida, fechas Clock, meses/bisiestos, duplicados por plan, cancelación idempotente, conservación de contrato, vencimiento, roles y cuentas actuales, pertenencia y paginación.

SuscripcionesHttpIntegrationTest usa MockMvc con FilterChain real y JWT firmado/alterado/expirado; verifica las cuatro operaciones por roles, estado actual y propiedad persistida, vínculo ausente, esquema estricto de request, 201/200/400/401/403/404/409/422/500, catálogo vacío, seguridad del aprovisionamiento y Swagger. No se considera suficiente simular un principal.

SuscripcionesPersistenceIntegrationTest verifica esquemas H2, FK, CHECK, UNIQUE, enums STRING, fechas, asociación confiable/idempotente, historial y paginación, vencimiento y rechazo posterior sin acortar contrato. Las consultas SQL negativas se ejecutan exclusivamente contra H2 de pruebas.

SuscripcionesConcurrenciaIntegrationTest usa transacciones independientes, fixtures comprometidos y CountDownLatch, sin Thread.sleep. Cubre dos activaciones del mismo NIT con mismo/distinto plan, dos cancelaciones, activación contra cancelación en ambos órdenes, nuevas activaciones al vencimiento, almacenes independientes y rollback después de escrituras reales (nueva suscripción/finalización previa/cancelación/asociación).

La regresión completa conservó las 1092 pruebas anteriores. Se ejecutaron desde style-radar `mvn clean test` y `mvn clean verify`, sin reducir el umbral JaCoCo LINE BUNDLE >= 85 %, excluir clases, modificar POM o deshabilitar pruebas.

## Resultados finales verificados

| Validación | Resultado |
| --- | --- |
| mvn clean test | BUILD SUCCESS, código de salida 0, 1278 pruebas |
| mvn clean verify | BUILD SUCCESS, código de salida 0, 1278 pruebas; todos los checks de cobertura cumplidos |
| Pruebas anteriores conservadas | 1092, sin cambios en sus archivos |
| Pruebas nuevas | 186 |
| Failures / errors / skipped | 0 / 0 / 0 en ambas ejecuciones |
| JaCoCo LINE BUNDLE | 2333 cubiertas / 98 no cubiertas = 95,9687 % (95,97 %) |
| JaCoCo BRANCH BUNDLE | 761 cubiertas / 143 no cubiertas = 84,1814 % (84,18 %) |
| Unitarias nuevas | 55: servicio 35, temporal 7, autorización 13 |
| HTTP nuevas | 94, cadena real y JWT |
| Persistencia nuevas | 26, JPA/H2 |
| Concurrencia y rollback nuevas | 11: 7 concurrentes y 4 de rollback |

Se comprobaron los totales de los XML Surefire y los contadores BUNDLE de jacoco.xml. La rama y HEAD siguen siendo los iniciales. Git muestra tres archivos existentes modificados, 37 nuevos sin seguimiento y ningún archivo eliminado; no se preparó el índice. `git diff --stat` cuenta únicamente los tres archivos existentes: 23 inserciones y una eliminación. `git diff --check` pasa. No se hicieron commit, push, checkout, pull, merge, rebase, reset, cherry-pick o PR.

Listo para commit: **SI**, implementación RF67/RF68, autorización por NIT, persistencia, regresiones y documentación completas. La validación PostgreSQL QA, el aprovisionamiento de condiciones reales y la actualización futura de diagramas siguen siendo responsabilidades pendientes para integración/despliegue; no se ejecutaron acciones sobre PostgreSQL real. No se continúa con Etapa 8.

## Inventario de archivos

Archivos creados (37):

- `docs/sprint3-etapa7-suscripciones.md`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/AsociacionAdministradorAlmacenController.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/PlanSuscripcionController.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/SuscripcionAlmacenController.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/docs/AsociacionAdministradorAlmacenApi.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/docs/PlanSuscripcionApi.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/docs/SuscripcionAlmacenApi.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/exception/SolicitudSuscripcionInvalidaException.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/exception/SuscripcionActivaDuplicadaException.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/mapper/SuscripcionAlmacenMapper.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/EstadoSuscripcion.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/PlanSuscripcion.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/SuscripcionAlmacen.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/UnidadPeriodoSuscripcion.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/CancelarRenovacionRequestDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/CrearSuscripcionRequestDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/SuscripcionesConsultaRequestDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/response/AdministradorAlmacenResponseDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/response/PlanSuscripcionResponseDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/response/SuscripcionAlmacenResponseDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/entity/AdministradorAlmacenEntity.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/entity/PlanSuscripcionEntity.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/entity/SuscripcionAlmacenEntity.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/AdministradorAlmacenRepository.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/PlanSuscripcionRepository.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/SuscripcionAlmacenRepository.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/AutorizacionSuscripcionService.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/ISuscripcionAlmacenService.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/AsociacionAdministradorAlmacenService.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/SuscripcionAlmacenServiceImpl.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/controller/SuscripcionesHttpIntegrationTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/model/domain/SuscripcionTemporalTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/AutorizacionSuscripcionServiceTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/SuscripcionAlmacenServiceImplTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/SuscripcionesConcurrenciaIntegrationTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/service/impl/SuscripcionesPersistenceIntegrationTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/support/SuscripcionesFixtures.java`

Archivos modificados (3):

- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/config/SecurityConfig.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/config/SwaggerConfig.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/exception/GlobalExceptionHandler.java`

Archivos eliminados: ninguno. Las pruebas preexistentes permanecen sin modificaciones.

Archivos compartidos modificados exclusivamente: SecurityConfig.java, SwaggerConfig.java y GlobalExceptionHandler.java. Coordinación con Líder Técnico: revisar estos matchers/contratos nuevos y la política de asociación, sin cambiar sus RF. Coordinación con infraestructura: tres tablas nuevas y sus restricciones antes del despliegue; sin cambios en Docker, Compose, .env.example, Actions, CI/CD o Azure. README y UML permanecen sin cambios. Los diagramas del modelo quedan pendientes para una etapa autorizada; no se crearon Mermaid, PlantUML o imágenes.
