# Sprint 3 — Etapa 6C — SR-RF-66

## Alcance y punto de partida

Gestión administrativa de estados de cuentas exclusivamente. RF61 y RF62 siguen usando sus servicios,
estados y políticas anteriores. No se implementan RF67, RF68, RF72, eliminación, roles nuevos,
aprovisionamiento administrativo público, frontend, emails, SMS ni otra autenticación.

Verificación inicial: rama `feature/s3-daniel-functional-security`, HEAD
`0af82aed0e43fb95633814173d263618190ef8e8`, commit
`feat(admin): implement RF62 store verification`, working tree limpio.
Línea base informada: 945 pruebas, 0 failures/errors/skipped; LINE 95,69 %, BRANCH 81,44 %.
No se eliminan ni modifican pruebas anteriores.

## Estados y compatibilidad

Se reutiliza `EstadoCuenta`: ACTIVA, INACTIVA, BLOQUEADA. ACTIVA permite login y acceso según roles;
INACTIVA y BLOQUEADA producen 401 tanto en login como al usar un JWT previo. La distinción entre
ambas suspensiones es administrativa; no cambia contraseñas ni autoridades.

`NULL` histórico sigue siendo ACTIVA efectiva en autenticación, decisiones, notificaciones, DTO
administrativo y filtro ACTIVA. La lectura no actualiza registros. Se requiere un backfill futuro,
coordinado con el Líder Técnico y PostgreSQL QA, antes de introducir NOT NULL.
El DTO muestra el estado efectivo: ACTIVA no prueba que el valor almacenado sea explícito. Los
metadatos nulos no afirman que ocurrió una decisión. No se añade un estado de escritura NULL.

Se permiten las seis transiciones entre estados distintos. Todo cambio real persiste un enum explícito.
Solicitar el mismo estado efectivo es idempotente: no escribe estado ni metadatos y conserva la última
decisión, incluso si se aporta otro motivo. `NULL -> ACTIVA` efectivo tampoco fabrica una decisión
ni realiza backfill. Se valida el request y se rechaza el autocambio antes de esta salida idempotente.

Los dos registros públicos existentes conservan sus contratos y crean COMPRADOR/ACTIVA, ignorando
privilegios y estado aportados por el cliente según su comportamiento previo. El registro legado
sin contraseña no crea credenciales. El aprovisionamiento administrativo seguro sigue pendiente de
integración con el Líder Técnico.

## API y datos

Todas las rutas exigen Bearer y ROLE_ADMIN_STYLERADAR con cuenta activa:

| Método | Ruta | Contrato |
| --- | --- | --- |
| GET | `/api/v1/admin/usuarios` | `estado` opcional; `page=0`, `size=20`, máximo 100; id ASC |
| GET | `/api/v1/admin/usuarios/{id}` | Cuenta y última decisión; 404 si no existe |
| PATCH | `/api/v1/admin/usuarios/{id}/estado` | `{ "estado": "BLOQUEADA", "motivo": "Incumplimiento de las condiciones" }` |

El listado usa `PageResponseDTO`. La BD filtra y pagina IDs; una segunda consulta carga usuarios
y roles de la página mediante EntityGraph y el mapper reconstruye el orden de los IDs. No se paginan
colecciones con fetch join. Una página cuyo offset excede el rango admitido por JPA retorna contenido
vacío con el count correspondiente. La lectura del listado usa REPEATABLE_READ para mantener una
vista coherente entre IDs, count y contenido.

`CuentaAdministrativaResponseDTO` contiene exactamente: id, nombre, email, roles, estadoCuenta,
fechaRegistro, fechaCambioEstado, administradorCambioEstadoId, motivoCambioEstado. El motivo está
disponible en listado y detalle, ambos restringidos a administración. No contiene teléfono,
preferencias, búsquedas, notificaciones, credenciales, hash ni tokens. Los DTO públicos no cambian.
`CuentaAdministrativaMapper` limita explícitamente la respuesta y conserva la lectura histórica de
roles vacíos como COMPRADOR, igual que la autenticación existente.

Controller: `GestionEstadoCuentaController`; contrato Swagger: `controller/docs/GestionEstadoCuentaApi`.
Servicio: `GestionEstadoCuentaService`; bloqueos compartidos: `BloqueoCuentaService`.
Solicitudes: `CuentasConsultaRequestDTO` y `CambioEstadoCuentaRequestDTO`.

## Última decisión y validaciones

Se añaden a UsuarioEntity únicamente tres atributos nullable:

| Columna | Tipo lógico | Observación |
| --- | --- | --- |
| `fecha_cambio_estado` | Instant / timestamp con zona | Clock del servidor, truncado a microsegundos |
| `administrador_cambio_estado_id` | Long / bigint | ID del UsuarioPrincipal, nunca del JSON |
| `motivo_cambio_estado` | varchar(1000) | Opcional salvo BLOQUEADA |

No se añade tabla de auditoría ni FK nueva. El MVP conserva la última modificación, no un historial
exhaustivo. Se mantiene el constructor previo, constructor vacío y builder. El mapper de registro
ignora los campos administrativos; estos no ingresan al modelo ni DTO públicos.

Bean Validation exige estado no nulo, motivo de máximo 1000 caracteres y motivo no vacío para
BLOQUEADA. INACTIVA y ACTIVA admiten motivo omitido o blanco, persistido como null. Se eliminan
espacios exteriores con strip antes de persistir. El límite se valida sobre la entrada sin recortar.
Se rechazan estados desconocidos, JSON malformado y campos adicionales, incluidos IDs del actor,
fecha y roles. El servicio repite las reglas esenciales para llamadas internas.
La consulta rechaza filtro vacío/desconocido y paginación inválida; no acepta filtros inventados.

Se reutilizan GlobalExceptionHandler, ErrorResponseDTO y SecurityErrorHandler:
200 correcto, 400 request inválido, 401 identidad inválida o cuenta no activa, 403 rol insuficiente,
404 objetivo inexistente, 409 conflicto de bloqueo/integridad, 422 regla de negocio,
500 error técnico. El actor inexistente devuelve 401; no se confunde con un objetivo inexistente.
No se exponen SQL ni información de credenciales en las respuestas.

SLF4J registra IDs de actor y objetivo, estado anterior/nuevo y resultado idempotente o aplicado
en transacción. El flush no se presenta como confirmación de commit. No se registran motivos
completos, email, contraseñas, hashes, JWT ni Authorization; las lecturas no generan logs adicionales.

## Seguridad, autocambio y último administrador

SecurityConfig añade únicamente los matchers específicos de los dos GET y del PATCH. El servicio
comprueba el rol del principal y vuelve a leer rol y estado del actor. En modificaciones, la
revalidación ocurre bajo bloqueo. Un principal anterior a una suspensión no basta para modificar.

El ID objetivo se compara con `UsuarioPrincipal.getUsuarioId()`. Se rechazan ACTIVA, INACTIVA y
BLOQUEADA sobre la propia cuenta, incluido el caso idempotente, con 422 y un mensaje explícito.

Antes de suspender una cuenta que tenga ADMIN_STYLERADAR debe existir otra cuenta operativa:
rol ADMIN_STYLERADAR, estado efectivo ACTIVA, credencial BCrypt estructuralmente válida para el
encoder actual y email admitido por la validación del login, con identidad normalizada no ambigua.
No cuentan cuentas sin credenciales, hashes inutilizables, correos inválidos/ambiguos, otros roles
ni administradores suspendidos. No se comprueba ni conoce la contraseña original.
Los roles múltiples conservan exactamente sus roles. Activar no concede roles nuevos.

## Transacciones y concurrencia

Cada PATCH se ejecuta en READ_COMMITTED y consulta los IDs de todos los administradores, agrega
actor y objetivo y bloquea las filas individuales de usuarios con PESSIMISTIC_WRITE en ID ASC,
eliminando duplicados. No se usan joins de colecciones en FOR UPDATE. Todas las decisiones RF66
comparten esos locks; después de esperar se consultan proyecciones escalares y roles actuales en BD
para evitar entidades obsoletas del contexto JPA. No se hace refresh en cascada sobre relaciones.
Se revalida actor y luego se calcula la existencia de otro administrador operativo.
Los bloqueos se mantienen hasta finalizar la transacción, incluyendo estado y metadatos.
Una actualización JPQL cambia únicamente las cuatro columnas de estado y trazabilidad y la respuesta
se obtiene mediante proyección actual. Así se persiste la transición aunque el estado pedido coincida
con un snapshot obsoleto de la entidad, sin limpiar el contexto ni refrescar búsquedas en cascada.
Como en toda actualización JPQL, un caller interno que ya tenga UsuarioEntity en su contexto debe
consultar los datos actuales antes de consumir su estado; no debe confiar en ese snapshot anterior.
RF66 responde mediante proyección actual y el detector consulta estado escalar bajo bloqueo.

Para A->B y B->A, ambas operaciones comparten A y B en el mismo orden. La primera termina y la
segunda relee el actor suspendido y recibe 401; nunca pueden quedar ambos suspendidos por estas
operaciones. Dos cambios al mismo comprador se serializan; el último estado y los tres metadatos
pertenecen a una decisión completa. Se reutiliza el handler 409 para conflictos de adquisición.
`@DynamicUpdate` evita que una escritura concurrente de campos ajenos al estado, basada en una
lectura previa, reemplace estado y trazabilidad con valores obsoletos.

El protocolo cubre escrituras RF66. Aprovisionamiento, futuras operaciones de roles, eliminación o
actualización de credenciales administrativas deben coordinar este mismo bloqueo con el Líder
Técnico; están fuera de esta etapa. Un cambio externo de esas capacidades que ignore los locks no
puede recibir la garantía de RF66. No existe hoy un endpoint RF66 que las modifique.

## JWT y compatibilidad RF61/RF62

Se reutilizan sin modificar UsuarioPrincipal, UsuarioUserDetailsService.loadActiveById,
JwtAuthFilter, JwtUtil, AuthServiceImpl y AuthenticationManager. No se cambia HS256, secreto,
duración ni login. El filtro vuelve a leer usuario, roles y estado en cada solicitud.
INACTIVA/BLOQUEADA: login 401 y JWT anterior 401 en feed, recomendaciones, alertas, notificaciones
y operaciones administrativas, incluida moderación RF61 y verificación RF62. COMPRADOR activo
mantiene 403 en administración. La matriz general de permisos sigue bajo el Líder Técnico.

Tras reactivar funcionan el login con la misma contraseña, un nuevo JWT y las operaciones de sus
roles. Un JWT anterior todavía vigente también puede volver a funcionar: no hay blacklist ni versión
de token, por lo tanto no se afirma revocación permanente. No se introducen mecanismos nuevos.
Una solicitud que ya atravesó el filtro antes del commit de una suspensión puede terminar; RF66
revalida al actor bajo lock para sus propias escrituras. No se promete cancelar solicitudes en curso
de otros módulos.

## Alertas y conservación

DetectorDisponibilidad mantiene las consultas candidatas, eventos NUEVA_COINCIDENCIA y
DISPONIBILIDAD_RESTABLECIDA, talla tardía, deduplicación y ciclos existentes. Después de bloquear
alertas bloquea usuarios destinatarios en orden ASC, consulta su estado escalar actual y solo emite para ACTIVA efectiva,
incluyendo NULL. Orden: item -> almacén -> alertas -> usuarios ASC; RF66 solo toma usuarios y no
invierte ese orden. Los usuarios permanecen bloqueados hasta commit del inventario y notificaciones.
Un evento previo a la suspensión puede confirmarse y conservarse; un evento posterior encuentra
la cuenta suspendida y no genera notificaciones. Estado, inventario y eventos siguen sujetos a rollback.

Suspender no desactiva ni elimina alertas, notificaciones, búsquedas, preferencias, tallas,
credenciales, playlists, likes, archivos o publicaciones. Reactivar no invoca el detector ni reproduce
eventos omitidos. Los ciclos de reposición continúan aumentando aunque no se emita al destinatario.
Un evento futuro real, incluido un aumento de stock que cumpla una talla por primera vez mientras
la cuenta está activa, puede generar una coincidencia según las reglas anteriores; no es un replay
automático del evento omitido. No se agrega cola ni servicio externo.

## Pruebas y validación

Pruebas nuevas con Arrange / Act / Assert:

- GestionEstadoCuentaServiceTest: transiciones, motivos, idempotencia, NULL, actor, roles múltiples,
  último administrador, credenciales utilizables, preservación, Clock.fixed, paginación y mapper.
- BloqueoCuentaServiceTest: orden ASC sin refresh en cascada, deduplicación y conjunto vacío.
- GestionEstadoCuentaHttpIntegrationTest: matriz de las tres rutas con FilterChain real, JWT
  ausente/manipulado/expirado, cuatro roles, cuenta suspendida, payloads inválidos/suplantación,
  errores 400/401/403/404/409/422/500, Swagger, DTO limitado y login/JWT reales antes y después
  de bloqueo/inactivación/reactivación, incluido JWT anterior después de reactivar y RF61/RF62.
- GestionEstadoCuentaPersistenceIntegrationTest: JPA/H2, enum STRING, NULL y metadatos nullable,
  persistencia/idempotencia entre transacciones, filtros/count/páginas/orden, relaciones LAZY,
  carga de roles por lote medida con estadísticas Hibernate, locks y carreras entre administradores,
  actor y objetivo obsoletos en contexto, límite real varchar(1000), rollback de persistencia y posterior
  a flush y escritura ajena concurrente.
- GestionEstadoCuentaAlertasIntegrationTest: eventos por estado efectivo, preservación, no replay,
  reactivación, talla tardía, deduplicación y ciclos.
- GestionEstadoCuentaEventosConcurrentesIntegrationTest: eventos y suspensión en ambos órdenes,
  transacciones independientes, locks reales, conservación y participación en reposiciones futuras.

Las carreras se coordinan con latches/barriers y Futures con timeout; no se usa Thread.sleep.
La regresión incluye las 945 pruebas anteriores sin cambiar assertions ni eliminar casos.
Resultado final: 1092 pruebas, de las cuales 945 anteriores y 147 nuevas; 0 failures, 0 errors y 0 skipped.
`mvn clean test`: BUILD SUCCESS (2:35 min). `mvn clean verify`: BUILD SUCCESS (2:37 min), incluyendo
el control de cobertura. JaCoCo BUNDLE LINE: 2180 cubiertas / 2276 totales = 95,7821 %;
BRANCH: 688 cubiertas / 828 totales = 83,0918 %. No se reducen umbrales ni se agregan exclusiones.
El POM y el umbral BUNDLE LINE >= 85 % permanecen intactos.

## PostgreSQL QA, integración y límites

No se ejecuta SQL en PostgreSQL real. Las consultas son JPQL/JPA con bloqueo de fila; H2 en modo
PostgreSQL prueba el comportamiento local, pero no demuestra por sí solo las garantías del motor
de PostgreSQL. QA debe repetir con dos conexiones independientes y READ_COMMITTED:

1. Confirmar dos administradores con credenciales y sincronizar ambos después de consultar IDs;
   ejecutar A->B y B->A; verificar un éxito, rechazo del actor suspendido y un administrador operativo.
2. Ejecutar dos cambios sobre el mismo usuario; comprobar espera de FOR UPDATE, estado final y
   metadatos de una única decisión; probar timeout/deadlock y respuesta 409 sin SQL.
3. Cargar actor en un contexto anterior, suspenderlo desde otra conexión y comprobar revalidación.
4. Intercalar reposición y suspensión reteniendo el lock del usuario; verificar emisión solo en el
   orden evento-anterior y ausencia en el orden suspensión-anterior; probar rollback en ambos.
5. Repetir filtros/count/carga de roles, instante a microsegundos y rollback tras flush/antes de commit.
6. Coordinar cualquier aprovisionamiento o cambio de rol/credencial con el protocolo de usuarios.

Coordinar incorporación de las tres columnas nullable al esquema `usuarios`; no se impone NOT NULL
a estado_cuenta, ni se realiza backfill o migración masiva. No se modifica Docker, Compose,
workflows, Azure, variables, secretos, configuración JWT productiva o infraestructura.

Escalabilidad: las escrituras RF66 se serializan mediante todos los administradores, con consultas
y locks proporcionales a su cantidad. Es una solución mínima para el MVP; si crece ese conjunto,
evaluar con el Líder Técnico un mutex persistente dedicado o estrategia equivalente. El detector
retiene locks de destinatarios durante la transacción de inventario, aumentando contención para
eventos con muchos usuarios. Validar límites y tiempos con PostgreSQL QA antes del despliegue.
No se garantiza cancelación inmediata de peticiones ya autenticadas ni revocación definitiva JWT.
No se actualizan README ni diagramas UML; su ajuste posterior queda pendiente de coordinación.

## Inventario de cambios

Rutas Java relativas a `style-radar/src/main/java/edu/dosw/proyecto/style_radar/`:

| Estado | Archivo |
| --- | --- |
| Creado | `controller/GestionEstadoCuentaController.java` |
| Creado | `controller/docs/GestionEstadoCuentaApi.java` |
| Creado | `mapper/CuentaAdministrativaMapper.java` |
| Creado | `model/dto/request/CambioEstadoCuentaRequestDTO.java` |
| Creado | `model/dto/request/CuentasConsultaRequestDTO.java` |
| Creado | `model/dto/response/CuentaAdministrativaResponseDTO.java` |
| Creado | `service/BloqueoCuentaService.java` |
| Creado | `service/impl/GestionEstadoCuentaService.java` |
| Modificado, compartido | `config/SecurityConfig.java` |
| Modificado, compartido | `mapper/UsuarioEntityMapper.java` |
| Modificado, compartido | `model/entity/UsuarioEntity.java` |
| Modificado, compartido | `repository/UsuarioRepository.java` |
| Modificado, compartido | `service/DetectorDisponibilidad.java` |

Rutas de pruebas relativas a `style-radar/src/test/java/edu/dosw/proyecto/style_radar/`:

| Estado | Archivo |
| --- | --- |
| Creado | `controller/GestionEstadoCuentaHttpIntegrationTest.java` |
| Creado | `service/BloqueoCuentaServiceTest.java` |
| Creado | `service/impl/GestionEstadoCuentaServiceTest.java` |
| Creado | `service/impl/GestionEstadoCuentaPersistenceIntegrationTest.java` |
| Creado | `service/impl/GestionEstadoCuentaAlertasIntegrationTest.java` |
| Creado | `service/impl/GestionEstadoCuentaEventosConcurrentesIntegrationTest.java` |

También creado: `docs/sprint3-etapa6c-estados-cuenta.md`. Total: 15 archivos nuevos y 5 modificados.
Ningún archivo versionado eliminado; ninguna prueba anterior modificada. No se realiza commit, push,
merge, rebase, reset, cherry-pick, PR ni inicio de Etapa 7.

## Reporte final de los 63 puntos solicitados

1. Rama `feature/s3-daniel-functional-security`; HEAD `0af82aed0e43fb95633814173d263618190ef8e8`, sin cambios.
2. Git inicial limpio; rama, HEAD y mensaje del último commit coincidieron con la precondición.
3. Creados 15 archivos; inventario completo en la sección anterior, incluidos 8 de producción, 6 de pruebas y este documento.
4. Modificados 5 archivos compartidos: SecurityConfig, UsuarioEntityMapper, UsuarioEntity, UsuarioRepository y DetectorDisponibilidad.
5. Ningún archivo versionado eliminado.
6. RF66 permite listar, consultar y cambiar estados, conservando datos y última decisión administrativa.
7. EstadoCuenta reutilizado, sin enum nuevo ni cambios de nombres.
8. NULL histórico sigue siendo ACTIVA efectiva; sin backfill ni NOT NULL.
9. Trazabilidad nullable: fechaCambioEstado, administradorCambioEstadoId y motivoCambioEstado; Clock del servidor.
10. Permitidas las seis transiciones entre estados diferentes.
11. Idempotencia conserva estado persistido y metadatos; NULL->ACTIVA efectivo no fabrica una decisión.
12. BLOQUEADA exige motivo no blanco; ACTIVA/INACTIVA opcional; máximo 1000, strip antes de persistir.
13. GET /api/v1/admin/usuarios, GET /api/v1/admin/usuarios/{id}, PATCH /api/v1/admin/usuarios/{id}/estado.
14. Dos requests nuevos, respuesta administrativa de nueve campos y PageResponseDTO reutilizado; sin credenciales ni preferencias.
15. GestionEstadoCuentaController y controller/docs/GestionEstadoCuentaApi implementados siguiendo el patrón existente.
16. GestionEstadoCuentaService y BloqueoCuentaService, transacciones y defensa de rol.
17. UsuarioRepository incorpora consultas específicas; CredencialRepository se reutiliza solo para lectura.
18. Filtro y paginación de IDs en BD, id ASC, roles por lote, page=0/size=20, máximo 100 y count coherente.
19. CuentaAdministrativaMapper nuevo; UsuarioEntityMapper ignora campos administrativos al mapear registros.
20. Bean Validation, rechazo de JSON desconocido/malformado y validaciones de actor, objetivo, motivo y reglas en servicio.
21. ROLE_ADMIN_STYLERADAR en matchers específicos y servicio; sin modificar accesos públicos ni matriz global.
22. Autocambio rechazado con 422 para los tres estados, incluido caso idempotente.
23. Permanece otro administrador activo efectivo con credenciales utilizables e identidad de login no ambigua.
24. PESSIMISTIC_WRITE de usuarios ASC, proyecciones actuales bajo lock y escritura atómica de cuatro columnas; sin refresh en cascada.
25. ACTIVA autentica y accede según sus roles.
26. INACTIVA recibe 401 en login y Bearer previo.
27. BLOQUEADA recibe 401 en login y Bearer previo.
28. JWT previo recibe 401 mientras la cuenta esté suspendida, incluidas las cuatro rutas privadas y administración.
29. Reactivación permite login con la misma contraseña, emisión de JWT y acceso según roles; conserva información.
30. JWT anterior aún vigente puede volver a funcionar al reactivar; sin revocación permanente, blacklist o versionado.
31. Detector suspende emisiones por estado actual bajo bloqueo; conserva NULL activo, eventos, talla tardía, deduplicación y ciclos.
32. No se eliminan usuarios, credenciales, preferencias, búsquedas, alertas, notificaciones, playlists, likes ni otras relaciones.
33. RF61 conserva políticas; administrador suspendido 401 y comprador activo 403 por seguridad existente.
34. RF62 conserva políticas; administrador suspendido 401 y comprador activo 403 por seguridad existente.
35. Se reutilizan excepciones y handlers existentes; códigos 400/401/403/404/409/422/500, sin detalles SQL en respuestas.
36. Swagger documenta las tres rutas, bearerAuth, rol, estados, ejemplos y errores.
37. SLF4J con IDs, estados y resultado; sin motivo completo, email, credenciales, hashes o tokens.
38. 59 pruebas unitarias nuevas: 57 de gestión y 2 del servicio de bloqueo.
39. 60 pruebas HTTP nuevas con FilterChain real; matriz de las tres rutas, errores, suplantación, DTO y Swagger.
40. Login/JWT HTTP reales para comprador y administrador; bloqueo, inactivación, reactivación y reutilización de JWT previo.
41. 18 pruebas nuevas de persistencia/transacciones con JPA/H2, STRING, NULL, metadatos, consultas, LAZY y consultas de roles acotadas.
42. Carreras sobre el mismo usuario, A->B/B->A, actor y objetivo obsoletos y eventos concurrentes, sin Thread.sleep.
43. Rollback tras falla de persistencia, tras flush y ante restricción real de varchar(1000); sin decisión parcial.
44. 8 pruebas de alertas por estados y 2 de eventos concurrentes; además regresión completa de alertas anteriores.
45. Las 945 pruebas anteriores se mantienen y pasan sin modificar assertions, mocks ni archivos de pruebas existentes.
46. Total final: 1092 pruebas; nuevas: 147.
47. Failures 0; errors 0; skipped 0.
48. mvn clean test: BUILD SUCCESS.
49. mvn clean verify: BUILD SUCCESS, incluyendo check de cobertura.
50. JaCoCo LINE BUNDLE: 95,78 % (2180/2276).
51. JaCoCo BRANCH BUNDLE: 83,09 % (688/828).
52. Riesgos: contención de locks, peticiones ya autenticadas que estaban en curso y entidades JPA precargadas después de update JPQL.
53. PostgreSQL QA pendiente: repetir carreras, espera/timeout de locks, rollback, timestamp, consultas y coordinación con eventos en dos conexiones.
54. Esquema: tres columnas nullable en usuarios; sin tablas nuevas, sin NOT NULL ni migración masiva del estado.
55. Coordinar con Líder Técnico aprovisionamiento, escrituras de roles/credenciales, backfill y posible mutex futuro; no se implementan esos RF.
56. Archivos compartidos modificados: los cinco enumerados en el punto 4; no hay conflictos Git detectados.
57. Diagramas UML y README no modificados; ajuste posterior pendiente de coordinación.
58. Escalabilidad: serialización RF66 mediante administradores y locks de destinatarios durante inventario; medir en PostgreSQL antes de despliegue.
59. git diff --check: exit 0, sin errores de whitespace; Git avisa únicamente conversión LF->CRLF en dos archivos.
60. git status --short: 5 M y 15 ?? correspondientes al inventario; git diff --stat: 5 archivos versionados, 77 inserciones; no incluye los nuevos sin staging.
61. Infraestructura, POM, configuración JWT productiva y secretos sin modificaciones.
62. Solo RF66; no RF67/RF68/RF72, reimplementación de RF61/RF62 ni funcionalidades del Líder Técnico.
63. Listo para revisión y commit: SÍ, regresión y cobertura completas correctas. No se realiza commit ni push; QA PostgreSQL sigue pendiente para despliegue.

Evidencia local, ignorada por Git: `target/sprint3-etapa6c-clean-test.log`,
`target/sprint3-etapa6c-clean-verify.log`, `style-radar/target/surefire-reports/` y
`style-radar/target/site/jacoco/`. La primera regresión detectó una interacción de refresh en cascada
con eliminación de búsquedas; se corrigió con consultas escalares bajo lock, conservando intactas
las assertions y las pruebas previas, y se repitieron ambas ejecuciones completas con éxito.
