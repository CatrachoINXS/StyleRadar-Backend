# Sprint 3 — Etapa 9: auditoría de seguridad, integración y calidad

Fecha: 14 de octubre de 2026. **Estado de la etapa: PENDIENTE de cierre integral.**
Las verificaciones locales y las correcciones descritas no certifican PostgreSQL,
el arranque Docker ni una matriz de permisos de negocio que todavía no está definida.

## Estado inicial y alcance

Rama inicial y revalidada tras las aclaraciones: `feature/s3-daniel-functional-security`.
HEAD: `5060379bbea86718e20ca5aad6957fcc44664388`,
`feat(playlist): implement RF72 popular playlists ranking`, 2026-10-14 13:51:30 -05:00.
`git status --short` al comenzar la auditoría en el turno anterior: vacío.
Al recibir las aclaraciones obligatorias se volvió a comprobar el estado real:
misma rama, mismo HEAD y mismo último commit; 10 archivos modificados y 10 nuevos,
sin staging ni eliminaciones, correspondientes a esta auditoría. Se preservan.

La referencia remota vigente proporcionada por el usuario es
`5060379bbea86718e20ca5aad6957fcc44664388`, y coincide con el HEAD local revalidado.
Su mensaje es `feat(playlist): implement RF72 popular playlists ranking`.
El usuario confirma que Etapa 8 ya fue implementada y subida. No se hizo fetch ni
consulta de red para afirmar una comprobación independiente del remoto.
La referencia inicial `0e430bd4dee0c33d81574cde767bcc3b876b27a1` queda sustituida
por esa aclaración; corresponde al commit anterior de suscripciones.
RF72 está integrado en el HEAD real: repositorio con ranking paginado por IDs,
count separado, carga por lotes, likes únicos, lock y restricción de unicidad;
controller, Swagger y pruebas HTTP/JPA/concurrencia presentes. No se reimplementa
RF72 ni se realiza ninguna operación de integración Git para ajustar el SHA.

Se revisaron las etapas 2–8, README, POM, SecurityConfig, todos los componentes
de security, controladores y contratos afectados, servicios de autenticación,
personalización, alertas, catálogo/inventario/imágenes, moderación, verificación,
cuentas, suscripciones y playlists; validadores, GlobalExceptionHandler,
mappings/repositorios relevantes y configuración de persistencia/Docker.
No se encontraron instrucciones AGENTS.md aplicables en el repositorio o sus ancestros.

Los **18 RF asignados ya están implementados**, conforme a la aclaración del usuario.
La documentación local los identifica como **02, 05, 06, 17, 18, 19, 20, 21, 29, 30,
31, 32, 61, 62, 66, 67, 68 y 72**. Esta etapa audita su integración y calidad;
no es un backlog de RF por implementar. No se encontró la especificación oficial textual
de estos RF. Las etapas previas constituyen el contrato provisional disponible.
En particular, Etapa 3 agrupa RF17–21 y Etapa 5 agrupa RF29–32 sin asignar cada
operación a cada número. No se inventan esos títulos ni esa correspondencia.
La aceptación individual definitiva requiere el documento oficial o la confirmación
del responsable de requisitos. RF30 sí está asociado explícitamente a coincidencias
tardías por talla en la corrección final de Etapa 5.

## Clasificación obligatoria del cierre tras las aclaraciones

### A. Validaciones completadas localmente

- Rama, HEAD y último commit revalidados al recibir las aclaraciones; coinciden con
  la referencia vigente indicada por el usuario. Los cambios anteriores se conservan.
- Línea base realmente ejecutada: 1340 pruebas, 0 fallos, 0 errores, 0 omitidas;
  LINE 96,056911 % y BRANCH 84,531590 %, equivalentes a los valores reportados
  96,06 % y 84,53 %. La repetición aislada confirmó esos mismos contadores.
- Regresión local final realmente ejecutada en el turno anterior: `mvn clean verify`,
  código Maven 0; 1370 pruebas, 0 fallos, 0 errores, 0 omitidas. Se conservan los
  1340 casos anteriores y se agregaron 30 casos de integración HTTP con seguridad real.
- JaCoCo final: LINE 96,056338 %, BRANCH 84,375000 %; umbral LINE >= 85 % intacto
  y aprobado. La reducción porcentual de ramas se informa expresamente.
- H2: restricciones, consultas, paginación, visibilidad, transacciones, rollback y
  concurrencia de las suites descritas. Inspección SQL adicional: 17 casos aprobados.
- Docker: inspección de CLI/daemon y validación de sintaxis Compose completadas en
  el turno anterior; no se afirma arranque de la API ni integración con PostgreSQL.

En esta continuación se revalida Git y se ajusta documentación/evidencia; **no se
vuelve a ejecutar Maven ni se modifica código funcional**. Las cifras anteriores
proceden de ejecuciones ya realizadas y de los artefactos conservados, no de una
ejecución nueva atribuida a estas aclaraciones.

### B. Hallazgos que requieren corrección o revisión técnica

| Hallazgo | Evidencia y estado | Responsable / siguiente paso |
| --- | --- | --- |
| Suplantación de identidad y exposición de playlists privadas por likes | Reproducción previa: 26 casos, 18 fallos; correcciones mínimas existentes verificadas por los 30 casos nuevos y la regresión completa | Cambios locales pendientes de revisión y autorización; no se extiende el alcance |
| Clave JWT requerida no transmitida por Compose | Inspección de Compose y JwtUtil; propuesta de corrección documentada, arranque no ejecutado | Responsable de infraestructura; no modificar sus archivos en esta etapa |
| Escrituras legadas de catálogo/inventario/imágenes y lectura de perfiles ajenos | Hallazgo estático de permisos vigentes; no se afirma que una política de negocio todavía desconocida esté incumplida | Líder Técnico y responsable funcional; decidir según E antes de corregir |
| Concurrencia de likes frente a edición/eliminación de playlist | Brecha de evidencia: los casos ejecutados no abarcan todas esas combinaciones; no es un defecto reproducido | Ampliar QA según C y el plan de integración D; no modificar código basándose solo en la hipótesis |

Las correcciones funcionales ya presentes se justifican en la sección de defectos
reproducidos y en reproduccion.json. No se agregan correcciones nuevas por estas
aclaraciones, ni se modifican funcionalidades del Líder Técnico o infraestructura
del otro desarrollador. La ausencia de una prueba no se presenta como defecto confirmado.

### C. Pendientes de PostgreSQL QA

No se ejecutaron pruebas contra PostgreSQL. Falta un destino de StyleRadar
autorizado, configuración válida y una base descartable exclusiva para fixtures.
El PostgreSQL de otro proyecto no se utilizó. Quedan pendientes el SQL del dialecto
real, planes de ejecución, constraints sobre el esquema existente, históricos NULL
y duplicados, aislamiento, locks, timeouts, deadlocks y rollback bajo ese motor.
Los requisitos y comandos aislados por suite figuran en la sección PostgreSQL QA.
H2 MODE=PostgreSQL no sustituye esta validación.

### D. Pendientes de integración con develop

**No se hizo merge con develop ni se ejecutaron pruebas sobre un árbol combinado.**
El build verde local certifica únicamente la rama de trabajo con sus cambios de
auditoría. No se afirma compatibilidad completa con el trabajo aún no integrado
del Líder Técnico o del otro desarrollador, ni se afirman conflictos no comprobados.

Cuando el responsable autorice y realice la integración en otro paso, revisar la
composición de matchers y permisos del Líder con los controladores afectados,
`IdentidadAutenticada`, `PlaylistServiceImpl`, contratos Swagger y pruebas. Después
ejecutar la regresión, JaCoCo y verificaciones HTTP/persistentes sobre el resultado
real integrado. La provisión JWT y la validación Docker corresponden al responsable
de infraestructura. Esta lista es trabajo pendiente, no una integración ejecutada.
No se hace checkout, merge, rebase, pull, cherry-pick ni resolución de conflictos.

### E. Decisiones de negocio sin especificación

- Matriz definitiva por rol, actor–NIT y excepciones para escribir catálogo,
  inventario e imágenes; no extender automáticamente el vínculo de suscripciones.
- Visibilidad y acceso a perfiles ajenos por campo y rol; no alterar ese contrato
  sin definición del responsable.
- Correspondencia documental individual de los paquetes RF17–21 y RF29/31/32.
  Es una limitación de aceptación/trazabilidad, **no RF pendientes de implementar**.
- Se conservan las políticas provisionales aprobadas: revisión manual, compatibilidad
  histórica NULL, feed determinista, ranking por likes y registro de intención de
  renovación sin cobros. No se introducen renovación automática, pagos, revocación
  permanente JWT ni otros comportamientos comerciales por inferencia.

El cierre integral sigue pendiente por B–E. Se entrega este reporte para revisión
y se espera autorización; no se inicia otra etapa ni se hace commit o push.

## Defectos reproducidos y corrección mínima

La nueva suite `CierreSeguridadHttpIntegrationTest` se ejecutó primero contra el
código anterior: **26 casos, 18 fallos de aserción, 0 errores, 0 omitidos**.
Evidencia conservada: [reproduccion.json](etapa9/reproduccion.json).
Dieciséis operaciones no rechazaban la identidad ajena con 403. El caso de like
con identidad ajena en el body ya estaba protegido y pasó. Los otros dos fallos
fueron lectura privada legítima sin `usuarioConsultaId` y like sobre privada ajena.

Cambios de producción:

- `IdentidadAutenticada` contrasta IDs de cliente con el UsuarioPrincipal establecido
  por JwtAuthFilter. Los rechazos son 403 genéricos y ocurren antes de delegar.
- Playlists: crear, editar, agregar/retirar prendas, eliminar y listar la colección
  completa del usuario requieren identidad propia. El listado contiene privadas:
  no es un listado público de un tercero. La comunidad conserva el GET de públicas.
- Consulta por ID: usa identidad JWT para aplicar la regla privada existente.
  `usuarioConsultaId` se conserva opcional; si se envía debe coincidir con el JWT.
  Una privada ajena sin ese parámetro sigue produciendo 422 según el contrato previo;
  un intento explícito de suplantar el parámetro produce 403.
- Likes: después del lock se aplica `PlaylistValidator.validarAccesoLectura` antes
  de modificar la colección. Evita devolver el DTO privado como respuesta al like.
  No cambia la alternancia ni el ranking ni introduce una política de interacción nueva.
- Preferencias, tallas y búsquedas guardadas: impiden seleccionar otro perfil,
  contaminar su personalización o consultar/borrar sus búsquedas.
- Segunda mano: publicar, editar, vender y retirar contrastan al actor real con
  el usuario declarado, manteniendo las comprobaciones de propiedad del servicio.
- Swagger agrega únicamente los 403 reales y el 422 de lectura privada en likes,
  y aclara la identidad de consulta/listado. Rutas, payloads y DTOs permanecen.

No se cambiaron SecurityConfig, JwtAuthFilter, JwtUtil, secretos, POM, umbrales,
mappings/esquema, infraestructura, Docker, workflows o Azure. Los tres tests
standalone existentes reciben un principal de fixture y la dependencia real de
identidad; conservan todos sus casos y aserciones. No son evidencia JWT.

## Matriz RF → prueba → esperado → evidencia

Los nombres de clase se resuelven bajo `style-radar/src/test/java/edu/dosw/proyecto/style_radar/`:
`controller/`, `security/` o `service/impl/`. El registro final por clase y caso,
incluidos parámetros y resultado, está en [final.json](etapa9/final.json).
"Verificado local" significa evidencia HTTP/JPA en H2 para la política disponible;
no aceptación del texto oficial ausente ni certificación PostgreSQL.

| RF | Caso(s) y evidencia | Resultado esperado | Estado real |
| --- | --- | --- | --- |
| 02 | `AuthHttpIntegrationTest.loginSinAutenticacionPreviaFuncionaParaLosCuatroActores`, `tokenExpiradoProduce401`; `CierreSeguridadHttpIntegrationTest.jwtRechazadoNoPuedeEliminarDatos` | Login 200; errores 401; sin sesión, secretos o escrituras tras rechazo | Verificado local |
| 05 | `PersonalizacionPersistenceIntegrationTest.feedPriorizaAmbasSenalesLuegoUnaLuegoNingunaSinExcluirCatalogo`, `feedOrdenaGlobalmenteAntesDePaginarSinDuplicados`; `PersonalizacionHttpIntegrationTest.busquedasAjenasNoInfluyenEnHttpYPropiasSi` | Perfil/actividad propios, prioridades antes de LIMIT, count y orden estables | Verificado local |
| 06 | `PersonalizacionPersistenceIntegrationTest.recomendacionesExigenEstiloYTallaYNoRellenanIncompatibles`, `perfilSinCoincidenciasDevuelveVacioSinFallback`; `PersonalizacionHttpIntegrationTest.jwtEmitidoPorLoginDa200ConDtoDeCatalogo` | Compatibilidad estricta; vacío legítimo; fallback solo para perfil vacío | Verificado local |
| 17 | Paquete D de descubrimiento (casos debajo) | Contratos públicos y reglas de Etapa 3 | Paquete verificado; asignación individual pendiente |
| 18 | Paquete D de descubrimiento | Contratos públicos y reglas de Etapa 3 | Paquete verificado; asignación individual pendiente |
| 19 | Paquete D de descubrimiento | Contratos públicos y reglas de Etapa 3 | Paquete verificado; asignación individual pendiente |
| 20 | Paquete D de descubrimiento | Contratos públicos y reglas de Etapa 3 | Paquete verificado; asignación individual pendiente |
| 21 | Paquete D de descubrimiento | Contratos públicos y reglas de Etapa 3 | Paquete verificado; asignación individual pendiente |
| 29 | Paquete A de alertas (casos debajo) | Contratos propios, eventos persistidos y políticas de Etapa 5 | Paquete verificado; asignación individual pendiente |
| 30 | `AlertasPersistenceIntegrationTest.primeraCoincidenciaEfectivaEnMDespuesDeStockEnLNotificaUnaSolaVez`; `AlertasTransaccionesIntegrationTest.coincidenciaTardiaConcurrenteEsUnicaSinDuplicarReposicionTotal` | Una coincidencia tardía por alerta/ítem, independiente de reposición total | Verificado local con referencia explícita en Etapa 5 |
| 31 | Paquete A de alertas | Contratos propios, eventos persistidos y políticas de Etapa 5 | Paquete verificado; asignación individual pendiente |
| 32 | Paquete A de alertas | Contratos propios, eventos persistidos y políticas de Etapa 5 | Paquete verificado; asignación individual pendiente |
| 61 | `ModeracionHttpIntegrationTest.adminRevisaDecideYConsulta`, `rolAjeno403`; `ModeracionVisibilidadIntegrationTest.todosLosCaminosExcluyenPendientesYRechazadasConCountYPaginaReal`; `ModeracionTransaccionesIntegrationTest.rollbackPosteriorALaDecisionRevierteInclusoDespuesDelFlush` | Rol administrativo; transiciones; filtrado previo a count; rollback íntegro | Verificado local |
| 62 | `VerificacionHttpIntegrationTest.adminRevisaDecideYConsulta`; `VerificacionVisibilidadIntegrationTest.matrizRF61RF62YStock`; `VerificacionTransaccionesIntegrationTest` | Verificación y moderación combinadas; ocultamiento, serialización y rollback | Verificado local |
| 66 | `GestionEstadoCuentaHttpIntegrationTest.loginJwtRealCambioAdministrativoYReactivacion`; `GestionEstadoCuentaPersistenceIntegrationTest.dosAdministradoresNoPuedenDeshabilitarseMutuamenteNiDejarConteoCero`, `rollbackPosteriorAlFlushNoDejaDecisionParcial` | 401 mientras suspendido; preservar datos y otro administrador operativo; rollback | Verificado local |
| 67 | `SuscripcionesHttpIntegrationTest.crear201ConsultarYCancelar200SinPago`, `seguridadJWTRealRolEstadoYPropiedad`; `SuscripcionesConcurrenciaIntegrationTest.dosActivacionesMismoNitUnaAceptadaInclusoPlanesDiferentes`, `falloPosteriorAlInsertRevierteNuevaYFinalizacionAnterior` | Rol y vínculo NIT; APROBADO explícito; un período activo; historia y rollback | Verificado local |
| 68 | `SuscripcionesPersistenceIntegrationTest.cancelacionJPANoModificaPeriodoYNoSobrescribeFecha`, `rechazoPosteriorNoBorraContratoNiPermiteEludirVisibilidad`; `SuscripcionesConcurrenciaIntegrationTest.falloPosteriorACancelarRevierteIntencionYFecha` | Solo deshabilitar intención; idempotencia, vigencia e historia intactas; sin cobro | Verificado local |
| 72 | `PlaylistsPopularesJpaIntegrationTest.ordenDiezCincoCeroExcluyePrivadaYCount`, `rankingReflejaLikeRetiroVisibilidadYEliminacion`; `PlaylistsLikesConcurrenciaIntegrationTest`; `CierreSeguridadHttpIntegrationTest.propietarioCambiaVisibilidadYRankingPorHTTPConservandoLikes` | Públicas, likes únicos activos DESC/fecha DESC/ID ASC; count y LIMIT en BD; integridad | Verificado local |

**Paquete D**, `AlmacenDescubrimientoIntegrationTest`: `marcadoresExcluyenHistoricosSinCoordenadasYDatosPrivados`,
`filtraCategoriasPersistidasSinDuplicados`, `resumenUsaCincoRecientesPorDefectoConStockEstadoYTallas`,
`novedadesIncluyenFinExactoYExcluyenUnMicrosegundoFueraDeAmbosExtremos`,
`distanciaConocidaSePresentaEnKmConDosDecimales`, `ordenDeterministaYPaginacionEnBD`.
Comprueba coordenadas reales válidas, categorías, resumen reciente disponible,
ventana UTC de siete días inclusiva, kilómetros en línea recta y paginación.
`VerificacionVisibilidadIntegrationTest.mapaCategoriaCatalogoResumenNovedadesYDistanciaRespetanEstado`
verifica después la integración con RF62, incluida la compatibilidad NULL histórica.

**Paquete A**, `AlertasHttpIntegrationTest.crearBusquedaPropiaConsultarYDesactivarConDtos`,
`AlertasPersistenceIntegrationTest.cadaFiltroExactoCompatibleProduceEvento`,
`todosLosFiltrosAndRechazanCualquierCoincidenciaParcial`,
`reposicionesSoloEnTransicionesRealesYVariasTallasNoDuplican`,
`paginaYOrdenDeNotificacionesSonDeterministasInclusoMismaFecha`,
`desactivacionIdempotenteConservaBusquedaFechaEHistorialYDetieneEventos`.
Se suman `AlertasTransaccionesIntegrationTest.errorTecnicoDeNotificacionRevierteStockEstadoCicloYPermiteReintento`
y `rollbackPosteriorAEventoNoDejaNotificacionesFalsasNiCambiosDeStock`.
La evidencia atraviesa detector, inventario, búsqueda exacta, alertas y notificaciones
persistidas; no afirma envío de email, push o eventos externos.

## Matriz de autenticación y autorización

| Flujo | Permitido | Denegado y efecto | Evidencia |
| --- | --- | --- | --- |
| Login | Cuenta operativa y credencial BCrypt | Inexistente, contraseña errónea, sin credencial, INACTIVA/BLOQUEADA: 401 genérico | AuthHttpIntegrationTest |
| JWT en DELETE sensible | JWT válido, actor propietario | Ausente, inválido, payload manipulado sin nueva firma, expirado, INACTIVA/BLOQUEADA: 401 y snapshot BD idéntico | CierreSeguridadHttpIntegrationTest, 6 variantes |
| Moderación/verificación/cuentas/asignación de NIT | ADMIN_STYLERADAR actual | Otros roles 403; suspendido 401 | ModeracionHttpIntegrationTest, VerificacionHttpIntegrationTest, GestionEstadoCuentaHttpIntegrationTest, SuscripcionesHttpIntegrationTest |
| Suscripciones | ADMIN_ALMACEN actual con vínculo persistido | Otros roles 403; NIT ajeno/sin vínculo 404; cuenta suspendida 401 | SuscripcionesHttpIntegrationTest.seguridadJWTRealRolEstadoYPropiedad, 44 variantes |
| Identidad en playlists/perfil/búsquedas/segunda mano | Actor igual al ID declarado; después se aplica propiedad del servicio | Suplantación 403 genérico, sin leer privados ni cambiar ninguna de nueve tablas observadas | CierreSeguridadHttpIntegrationTest.identidadAjenaEs403SinLeerPrivadosNiModificarDatos, 17 variantes |
| Playlist privada y likes | Propietario JWT, incluso sin query de identidad | Privada ajena 422 según regla previa; query de suplantación 403; sin modificar likes | CierreSeguridadHttpIntegrationTest |
| Ranking de populares | Cualquier cuenta operativa autenticada | Sin JWT 401; nunca devuelve privadas ni las incluye en count | PlaylistsPopularesHttpIntegrationTest y PlaylistsPopularesJpaIntegrationTest |
| Perfil/búsquedas y segunda mano propios | Contratos 200/201/204 previos | Identidad propia no reemplaza propiedad del recurso | CierreSeguridadHttpIntegrationTest, cuatro flujos exitosos/propiedad y prueba Swagger |

Los 401 conservan `WWW-Authenticate: Bearer` y ErrorResponseDTO. Los 403 conservan
`Acceso denegado`. No se sustituyó la autenticación por `@WithMockUser` en la suite nueva:
login real para propietario, tokens firmados con la clave exclusiva de tests para el
otro actor, usuario/estado/roles releídos por el filtro, servicios y repositorios reales.

La política vigente no tiene blacklist, refresh token ni versión de credencial.
Cambios de estado/roles afectan la siguiente solicitud; una cuenta eliminada no puede
reutilizar el JWT. Reactivar permite nuevamente un JWT anterior aún vigente, como
documenta RF66 y prueba GestionEstadoCuentaHttpIntegrationTest. No se declara
revocación permanente, invalidación al cambiar contraseña o cancelación de requests
que ya atravesaron el filtro. No se inventó una política de logout/recuperación.

### Qué atraviesa realmente seguridad

`AuthHttpIntegrationTest`, `AlmacenDescubrimientoIntegrationTest`,
`PersonalizacionHttpIntegrationTest`, `PersonalizacionErroresHttpTest`,
`AlertasHttpIntegrationTest`, `ModeracionHttpIntegrationTest`,
`VerificacionHttpIntegrationTest`, `GestionEstadoCuentaHttpIntegrationTest`,
`SuscripcionesHttpIntegrationTest`, `PlaylistsPopularesHttpIntegrationTest` y
`CierreSeguridadHttpIntegrationTest` usan WebApplicationContext y `springSecurity()`.
Los dos tests de visibilidad Moderacion/Verificacion también incluyen peticiones
con esa cadena, además de llamadas directas de servicio. GestionEstadoCuentaAlertas
es integración de servicios y persistencia; no atraviesa filtros HTTP.

AuthHttpIntegrationTest agrega una cadena y endpoints exclusivos de prueba bajo
`/test-security/**`: sus assertions de rol sobre esa ruta no certifican permisos
productivos. Los tests administrativos posteriores sí prueban rutas productivas.
PersonalizacionErroresHttpTest sustituye el servicio para simular fallos; no acredita
persistencia. Las suites de verificación, cuentas y suscripciones usan spies para
fallos controlados; conservan autenticación real. Clock fijo es un doble temporal.
Las pruebas concurrentes usan spies para coordinar o inyectar fallos, manteniendo
repositorios, transacciones y locks reales en las operaciones verificadas.

`PlaylistControllerTest`, `UsuarioControllerTest`, `SegundaManoControllerTest`,
`CatalogoControllerTest`, `InventarioControllerTest`, `ImagenCatalogoControllerTest`
y `BusquedaCatalogoControllerTest` son MockMvc standalone con servicios simulados.
Los tests unitarios Mockito de servicios/validadores/mappers tampoco prueban JWT.
Las suites JPA o transaccionales sin MVC no acreditan autorización HTTP.
Los indicadores `securityFilterChain`, `standaloneMvc`, `springContext`,
`testSecurityChain` y `doubles` de final.json permiten auditar esta distinción por clase.

## Persistencia y flujos completos

Motor ejecutado: **H2 2.4.240 en MODE=PostgreSQL**, no PostgreSQL.
Perfil `test`, esquema `create-drop`, Open Session in View deshabilitado.
Concurrencia: fixtures confirmados, transacciones independientes, latches y futuros
con timeout en las suites transaccionales previas. Las peticiones de la suite nueva
comparten la transacción de test y se revierten al terminar; sus snapshots comprueban
la ausencia de escrituras, pero no equivalen a pruebas de commit o aislamiento entre
clientes. Esa evidencia procede de las suites transaccionales independientes.

| Aspecto | Evidencia ejecutable en la regresión | Límite |
| --- | --- | --- |
| Paginación/agregación/count | PopularesJpa, PersonalizacionPersistence, AlmacenDescubrimiento; rechazo de fetch paginado de colecciones, estadísticas de consultas | Plan y SQL dialectal de PostgreSQL pendientes |
| UNIQUE/CHECK/FK/NULL | AlertasTransacciones, SuscripcionesPersistence, PopularesJpa y cuentas; duplicados, varios históricos NULL, relaciones y enum STRING | Esquema existente de QA no inspeccionado |
| Rollback tras flush | AlertasTransacciones, ModeracionTransacciones, VerificacionTransacciones, GestionEstadoCuentaPersistence, SuscripcionesConcurrencia, PlaylistsLikesConcurrencia | Comportamiento real PostgreSQL pendiente |
| Integridad concurrente | Likes de distintos/mismo usuario, una alerta activa, evento único, un contrato activo/NIT, doble decisión y protección de último administrador | Deadlocks, SQLSTATE, aislamiento y tiempos reales de PG pendientes |
| Visibilidad RF61/RF62 | VerificacionVisibilidad.matrizRF61RF62YStock, ModeracionVisibilidad; catálogo, feed, búsqueda, mapa, prendas, playlists y detector | NULL históricos visibles provisionalmente; suscribir exige APROBADO explícito |
| Vigencia y renovación | SuscripcionTemporalTest, SuscripcionesPersistence/Concurrencia/Http | Meses calendario UTC, [inicio, fin), fin excluido; sin scheduler ni pagos |
| Ranking después de estado/likes | PopularesJpa y nuevo flujo HTTP pública→like→privada | Snapshot por request, sin snapshot compartido entre páginas sucesivas |

Además se ejecutó `mvn '-Dtest=PlaylistsPopularesJpaIntegrationTest'
'-Djacoco.skip=true' '-Dlogging.level.org.hibernate.SQL=DEBUG' test` para inspeccionar
el SQL generado: **17/0/0/0, BUILD SUCCESS**, 24,584 s. La instrumentación/reporte
JaCoCo se omite solo en esta observación adicional para conservar el informe de la
regresión completa, que ya había aprobado todos sus controles sin omitir JaCoCo.
No se modifica el POM. [sql-generado-h2.txt](etapa9/sql-generado-h2.txt) conserva
sentencias reales sin valores de parámetros: LEFT JOIN, COUNT DISTINCT, count
independiente, agrupación y `offset ? rows fetch first ? rows only` en H2, más la
restricción `uk_playlist_like_usuario`. No se extrapola ese dialecto a PostgreSQL.

Se conservan las políticas de recencia, filtros AND, stock efectivo, ausencia de
similitud en personalización/alertas, historia sin borrado y no notificación retroactiva.
No se ejecutaron migraciones, depuración de duplicados, backfill o cambios históricos.

### PostgreSQL QA pendiente

No hay DB_URL/DB_USERNAME/DB_PASSWORD de QA en el entorno, archivo `.env` local o
instancia identificada de StyleRadar. Docker muestra un PostgreSQL de otro proyecto,
que no constituye un destino autorizado. No se probó ni modificó esa instancia.

Requisitos para ejecutar: destino QA explícitamente autorizado, **base vacía y
descartable exclusivamente de tests**, versión del motor objetivo (Compose declara
16-alpine), credenciales efímeras, permiso de red y DDL de fixtures, y recursos para
concurrencia. El perfil de test usa create-drop; nunca apuntarlo a QA compartida,
producción o una base con historia. Las migraciones reales requieren otro procedimiento.

Tras aprovisionamiento externo, ejecutar por suite en procesos separados para
evitar que varios contextos de Spring creen/eliminen el mismo esquema. Pasar las
propiedades datasource como propiedades de JVM/Maven; definir únicamente DB_URL
no reemplaza el datasource H2 explícito del perfil test. Ejemplo PowerShell, sin
credenciales literales ni comandos de aprovisionamiento:

```powershell
cd style-radar
# QA_TEST_* deben proceder del mecanismo seguro del equipo, solo para la BD descartable.
mvn '-Dtest=PlaylistsPopularesJpaIntegrationTest' `
  "-Dspring.datasource.url=$env:QA_TEST_DB_URL" `
  "-Dspring.datasource.username=$env:QA_TEST_DB_USERNAME" `
  "-Dspring.datasource.password=$env:QA_TEST_DB_PASSWORD" `
  '-Dspring.datasource.driver-class-name=org.postgresql.Driver' test
```

Repetir de forma aislada para PersonalizacionPersistenceIntegrationTest,
AlertasTransaccionesIntegrationTest, ModeracionTransaccionesIntegrationTest,
VerificacionTransaccionesIntegrationTest, GestionEstadoCuentaPersistenceIntegrationTest,
SuscripcionesPersistenceIntegrationTest, SuscripcionesConcurrenciaIntegrationTest y
PlaylistsLikesConcurrenciaIntegrationTest. No se afirma que ya pasen en PostgreSQL;
revisar assertions de metadatos específicas H2 antes de certificar su portabilidad.

En QA se deben registrar SQL generado y EXPLAIN (ANALYZE, BUFFERS), COUNT DISTINCT,
NULL/duplicados históricos, constraints físicos, orden/precisión de fechas,
REPEATABLE_READ, FOR UPDATE sin joins de colecciones, lock/statement timeout,
deadlocks, rollback, errores que requieren reintento y carga/contención de ranking.
Las verificaciones sobre datos históricos productivos deben ser de solo lectura
y coordinadas con su responsable; no se prescribe limpiarlos en esta etapa.

## Docker e infraestructura

Docker CLI y daemon accesibles: **29.8.0**. `docker compose -f
style-radar/docker-compose.yml config --quiet` acepta la sintaxis (código 0),
con advertencias por DB_NAME/DB_USERNAME/DB_PASSWORD ausentes. No existe `.env`
en style-radar. No se mostraron valores de secretos ni se inició/detuvo ningún contenedor.

Compatibilidad estática: compilador, build de Docker y JRE usan Java 21; PostgreSQL
driver es dependencia runtime; H2 es test scope; Surefire ejecuta `*IntegrationTest`
porque su nombre termina en Test. El Dockerfile hace package con skipTests:
construir una imagen no certifica pruebas de integración ni umbral de verify.

**Bloqueo de arranque:** Compose transmite DB_URL/DB_USERNAME/DB_PASSWORD,
SERVER_PORT y JPA_DDL_AUTO, pero no JWT_SECRET_BASE64. JwtUtil exige una clave
Base64 de al menos 32 bytes y falla con la propiedad vacía. Un `.env` de Compose
no la introduce en el contenedor automáticamente. Propuesta separada para el equipo
de infraestructura: proporcionar la clave desde su gestor de secretos al entorno
de la API y revisar la duración JWT; no versionarla ni reutilizar el fixture de tests.
Completar variables DB y validar primero contra un esquema controlado, pues el
default JPA_DDL_AUTO=update puede intentar DDL. **No se modificaron estos archivos
ni se construyó/arrancó la aplicación con Docker. Compatibilidad de ejecución pendiente.**

## Riesgos abiertos y decisión de cierre

1. **Autorización legada de almacenes:** SecurityConfig exige solo autenticación
   para escrituras de catálogo, inventario e imágenes; los servicios comprueban
   relación ítem–NIT, pero no actor–NIT ni rol del actor. El vínculo de suscripciones
   no se aplica automáticamente: Etapas 2/3/6 dejan esa matriz al Líder Técnico.
   Falta política aprobada para ampliar su uso y roles/excepciones. Hallazgo por
   inspección de código; no se acredita denegación por rol en esas rutas.
2. `GET /usuarios/{id}` permite a cualquier autenticado consultar otro perfil con
   su DTO vigente. La política de acceso a perfiles ajenos no está definida en la
   documentación disponible. Resolver visibilidad por campo/rol con el responsable;
   no modificar este contrato por inferencia.
3. PostgreSQL QA, revisión de datos históricos y arranque Docker siguen pendientes
   por los requisitos concretos descritos. H2 no certifica esos motores/entornos.
4. Falta especificación oficial y correspondencia individual de RF17–21 y RF29/31/32.
   Los paquetes tienen evidencia real; no se declara aceptación individual completa.
5. Se conservan limitaciones documentadas: JWT anterior reutilizable tras reactivar;
   solicitudes ya autenticadas pueden finalizar; no hay renovación/pagos automáticos;
   compatibilidad temporal NULL; ranking por offset y coste de materializar colecciones;
   errores de rutas inexistentes tratados por el handler genérico previo.
6. Las ediciones/eliminaciones de playlists no comparten el método explícito
   findForUpdate de likes. La interacción con cambios simultáneos de visibilidad,
   eliminación y asociaciones requiere ampliar QA de contención; las seis pruebas
   actuales de likes no certifican todas esas combinaciones.

Con estos puntos abiertos **no se cumplen todos los criterios de finalización**.
No se declara Etapa 9 completa ni lista para despliegue.

| Criterio de cierre | Resultado |
| --- | --- |
| 18 RF con trazabilidad individual suficiente | Parcial: 18 identificados; paquetes probados, falta especificación/correspondencia oficial |
| Flujos críticos de seguridad con integración real | Corregidos y probados los descritos; permisos legados de almacenes/perfiles pendientes |
| Regresión completa | Aprobada, 1370/0/0/0 |
| Umbrales existentes | Aprobados sin cambios |
| Persistencia relevante | H2 aprobada; PostgreSQL QA pendiente |
| Compatibilidad Docker ejecutada | Pendiente; clave JWT no transmitida y entorno DB sin configurar |
| Documentación fiel y comandos reproducibles | Entregados con límites explícitos |
| Alcance y cambios Git documentados | 20 paths inventariados, sin cambios de infraestructura o integración Git |

## Reproducción local y evidencia

Desde la raíz:

```powershell
git status --short
git branch --show-current
git rev-parse HEAD
git log -1 --format='%H %s'
cd style-radar
mvn clean verify
cd ..
node docs/etapa9/recopilar-evidencia.cjs final
git diff --check
```

Para el conjunto corregido: desde style-radar,
`mvn '-Dtest=CierreSeguridadHttpIntegrationTest,PlaylistControllerTest,UsuarioControllerTest,SegundaManoControllerTest,PlaylistServiceImplTest,PlaylistsPopularesHttpIntegrationTest' test`.
PowerShell requiere citar el parámetro con comas. Una primera invocación sin esas
comillas no llegó a Maven; no se cuenta como resultado de pruebas.

El script de evidencia solo lee Surefire y counters BUNDLE directos de JaCoCo;
no cambia Maven ni ejecuta pruebas. final.json clasifica las suites y compara todos
los nombres de casos con baseline.json para verificar la conservación de la regresión.
Para capturar otra línea base, usar `baseline` antes de cambios; `reproduccion`
filtra únicamente la suite que demostró los defectos, sin atribuirle cobertura global.
No sobrescribir las evidencias históricas al reproducir con otra revisión.

Durante la consolidación se corrigió el lector de elementos testcase autocerrados
y la lectura UTF-16 de logs PowerShell. Ahora exige que la cantidad de casos leídos
coincida con el contador de cada suite y valida que los métodos citados en la matriz
aparezcan en los XML. Para reconstruir la comparación nominal se repitió la línea
base sobre una copia temporal obtenida con `git archive HEAD`, sin checkout ni cambios
en el árbol de trabajo. baseline.json conserva también los contadores originales
en `initialRun`; su inventario nominal inicial incompleto no se usa para certificar
conservación de casos. La copia temporal permanece bajo TEMP/styleradar-etapa9-baseline-5060379-20261014.
La repetición aislada terminó con BUILD SUCCESS, **1340/0/0/0**, 3 min 13 s,
a las 14:19:36 -05:00; reprodujo exactamente los counters LINE y BRANCH iniciales.
La comparación posterior del inventario completo confirmó 1340 conservados y 30 nuevos.

La línea base real fue `mvn clean verify`: BUILD SUCCESS, **1340/0/0/0**,
3 min 1 s. LINE **2363 cubiertas / 97 no cubiertas = 96,0569 %**;
BRANCH **776 / 142 = 84,5316 %**. [baseline.json](etapa9/baseline.json).
La primera verificación específica tras corregir produjo **91/0/0/0**, BUILD SUCCESS,
1 min 21 s. Después se agregaron cuatro casos HTTP de compatibilidad/Swagger/propiedad,
que deben contarse en la regresión final. Los resultados finales se registran debajo.

### Resultado final verificado

`mvn clean verify`: **BUILD SUCCESS**. Primera ejecución completa con los cambios:
3 min 35 s, finalizada a las 14:10:33 -05:00. Repetición final para capturar
explícitamente el código de Maven: **3 min 9 s, 14:24:48 -05:00, MAVEN_EXIT_CODE=0**.
Surefire: **1370 pruebas, 0 fallos, 0 errores, 0 omitidas**.
La comparación de casos confirma **1340 anteriores conservados y 30 nuevos**, sin
casos faltantes. Los 30 nuevos atraviesan la cadena real con servicios y JPA reales.

| Counter BUNDLE | Cubiertas | No cubiertas | Cobertura |
| --- | ---: | ---: | ---: |
| LINE | 2387 | 98 | 96,056338 % |
| BRANCH | 783 | 145 | 84,375000 % |

El único umbral configurado es LINE BUNDLE >= 85 %: `All coverage checks have been met`.
No hay umbral BRANCH configurado. Se conserva el POM sin exclusiones ni reducción
de límites. La cobertura de ramas pasa de 84,531590 % a 84,375000 % al incorporar
las nuevas comprobaciones; no se presenta como una mejora porcentual. El cambio
de líneas es 96,056911 % → 96,056338 % (ambas redondean a 96,06 %).
Fuentes: [final.json](etapa9/final.json), XML Surefire de target/surefire-reports
y counters directos de target/site/jacoco/jacoco.xml. El informe HTML queda en
target/site/jacoco/index.html; el log completo final, en target/etapa9-final.log.
Las primeras sesiones del lanzador devolvieron código de shell 1 pese al BUILD SUCCESS
registrado, tras redirigir diagnósticos nativos con PowerShell. Por eso se repitió
la regresión con captura explícita de LASTEXITCODE y `exit` con ese valor. La última
sesión confirma código 0 y final.json conserva `mavenExitCode: 0`; no se dedujo el
código de proceso únicamente del texto del log.

## Inventario y control Git final

Rutas relativas a la raíz. Prefijos Java: `style-radar/src/main/java/edu/dosw/proyecto/style_radar/`
y `style-radar/src/test/java/edu/dosw/proyecto/style_radar/`.

| Estado | Prefijo | Ruta |
| --- | --- | --- |
| Modificado | main | controller/PlaylistController.java |
| Modificado | main | controller/UsuarioController.java |
| Modificado | main | controller/SegundaManoController.java |
| Modificado | main | controller/docs/PlaylistApi.java |
| Modificado | main | controller/docs/UsuarioApi.java |
| Modificado | main | controller/docs/SegundaManoApi.java |
| Modificado | main | service/impl/PlaylistServiceImpl.java |
| Modificado | test | controller/PlaylistControllerTest.java |
| Modificado | test | controller/UsuarioControllerTest.java |
| Modificado | test | controller/SegundaManoControllerTest.java |
| Creado | main | security/IdentidadAutenticada.java |
| Creado | test | controller/CierreSeguridadHttpIntegrationTest.java |
| Creado | test | support/PrincipalControllerFixture.java |
| Creado | raíz | docs/sprint3-etapa9-cierre-seguridad.md |
| Creado | raíz | docs/etapa9/recopilar-evidencia.cjs |
| Creado | raíz | docs/etapa9/baseline.json |
| Creado | raíz | docs/etapa9/reproduccion.json |
| Creado | raíz | docs/etapa9/final.json |
| Creado | raíz | docs/etapa9/sql-generado-h2.txt |
| Creado | raíz | docs/etapa9/inventario-git.json |

Eliminados: ninguno. Los logs finales son artefactos generados, ubicados en
style-radar/target; no forman parte del código versionable. Los logs iniciales de
target se sustituyeron durante clean; sus contadores/casos están conservados en JSON.
No se hizo commit, push, pull, merge, rebase, reset, cherry-pick, checkout ni cambios
de rama. No se tocaron CI/CD, Azure, Docker o secretos. No se inicia otra etapa.

Control final: misma rama y HEAD iniciales; **10 archivos modificados, 10 creados,
0 eliminados, 0 staged**. [inventario-git.json](etapa9/inventario-git.json) enumera
los 20 paths con estado porcelain, rama, SHA y resultado vacío de `git diff --check`.
Todos los cambios se explican en este documento. La etapa permanece pendiente.
