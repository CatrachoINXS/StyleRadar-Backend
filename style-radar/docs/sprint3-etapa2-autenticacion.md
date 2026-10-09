# Sprint 3 — Etapa 2: autenticación y RF02

Alcance: login con correo/contraseña, JWT Bearer y registro seguro de compradores.
Java 21 y Spring Boot 4.1.1 permanecen sin cambios. Spring Security 7.1.1 es administrado
por Boot. JJWT 0.13.0 se resolvió desde Maven Central y se verificó ejecutando generación,
validación y pruebas HTTP. Referencia primaria: https://github.com/jwtk/jjwt#installation.

## Contratos HTTP

- `POST /api/v1/auth/login`: JSON con `email` y `password`.
  Devuelve 200 con `accessToken`, `tokenType: Bearer`, `expiresIn` en segundos,
  `usuarioId` y `roles` (nombres del enum, sin prefijo ROLE_).
  Credenciales incorrectas, correo inexistente, credencial ausente y cuentas INACTIVA/BLOQUEADA
  reciben 401 con el mismo mensaje: "Autenticación inválida".
- `POST /api/v1/auth/registro`: `nombre`, `email`, `telefono` opcional y `password`.
  Devuelve 201 con el contrato existente de `UsuarioResponseDTO`, sin contraseña, hash ni JWT.
  El servicio impone COMPRADOR/ACTIVA. El cliente no puede seleccionar roles ni estado;
  campos desconocidos no se usan. Un correo existente, incluso histórico sin credencial,
  produce 422 con la regla de duplicado existente; un conflicto de unicidad concurrente produce 409.
- Sintaxis inválida: 400. La validación de contraseña comparte la regla existente de
  CredencialValidator: no nula, no blanca, sin NUL y máximo 72 bytes UTF-8. No se recorta
  ni normaliza la contraseña. Se heredan nombre/email/teléfono del DTO de registro existente.
- `POST /api/v1/usuarios` sigue disponible: crea una cuenta sin credencial. Esa cuenta
  no puede iniciar sesión hasta un aprovisionamiento confiable, con identidad verificada.

## Autenticación y filtros

AuthController transforma mediante UsuarioMapper cuando corresponde. IAuthService no recibe
DTO HTTP. AuthServiceImpl usa AuthenticationManager (ProviderManager y DaoAuthenticationProvider),
UsuarioUserDetailsService y el PasswordEncoder BCrypt existente. No hay otro comparador de hashes.
Spring borra el hash del principal después de autenticar con el proveedor.

UsuarioUserDetailsService obtiene candidatos por correo normalizado, exige exactamente uno,
recupera su credencial y construye UsuarioPrincipal. Este conserva ID y roles efectivos y genera
authorities ROLE_COMPRADOR, ROLE_ADMIN_ALMACEN, ROLE_REPRESENTANTE_FUNDACION y ROLE_ADMIN_STYLERADAR.
Roles ausentes equivalen únicamente a COMPRADOR; estado null equivale temporalmente a ACTIVA.
Sin credencial no hay login. Los valores históricos no se modifican al leerlos.

JwtUtil firma exclusivamente HS256. Incluye únicamente sub (ID estable positivo), iat y exp.
Valida firma, algoritmo permitido, expiración, presencia/coherencia de fechas e ID.
Usa el Clock ya existente, inyectable. No utiliza el ObjectMapper HTTP de Jackson 3:
jjwt-jackson carga su adaptador Jackson 2. El árbol resuelto contiene Jackson 3.1.5 para HTTP
y Jackson 2.21.5 para JJWT, en namespaces diferentes.

JwtAuthFilter solo establece un SecurityContext nuevo después de validar el JWT y cargar el
usuario activo. Consulta el usuario por ID una vez; el EntityGraph carga roles actuales.
No vuelve a consultar el hash para un Bearer. Se rechazan usuarios inexistentes, INACTIVA y
BLOQUEADA. Cambios de estado y roles se aplican a la siguiente solicitud aunque el JWT siga
vigente. Claims de roles adicionales se ignoran. No se almacena ni revoca individualmente el JWT.

El filtro se registra únicamente en Spring Security (FilterRegistrationBean deshabilita la
registración servlet automática). No deja un contexto parcial al rechazar autenticación.
Los errores JWT y de autenticación producen 401. Fallos de acceso a persistencia en el filtro
producen 500; fallos internos del proveedor durante login también producen 500.
Errores de negocio/controladores conservan el GlobalExceptionHandler existente.

SecurityErrorHandler implementa AuthenticationEntryPoint y AccessDeniedHandler y serializa
ErrorResponseDTO con Jackson HTTP. GlobalExceptionHandler añade 401 y 403 para errores MVC,
conservando 400/404/409/422/500. Los 401 incluyen WWW-Authenticate: Bearer. Los logs nuevos
no incluyen correos, passwords, hashes, tokens ni secretos; eventos exitosos utilizan IDs.

## Rutas y Swagger

SecurityFilterChain es STATELESS, sin Basic, form login, logout autenticado ni request cache.
CSRF está deshabilitado para esta API REST Bearer sin cookies de sesión.

Rutas públicas explícitas:

| Método | Ruta |
| --- | --- |
| POST | /api/v1/auth/login |
| POST | /api/v1/auth/registro |
| POST | /api/v1/usuarios |
| GET | /api/v1/prendas |
| GET | /api/v1/catalogo/buscar |
| GET | /api/v1/almacenes/{nit}/catalogo |
| GET | /swagger-ui.html |
| GET | /swagger-ui/** |
| GET | /v3/api-docs |
| GET | /v3/api-docs/** |

Todas las demás rutas requieren autenticación: anyRequest().authenticated().
Los GET de usuarios, búsquedas guardadas, playlists y segunda mano permanecen protegidos
por defecto; no se ha establecido una matriz por rol ni propiedad de recursos.

AuthApi mantiene la documentación bajo controller/docs. bearerAuth es HTTP/Bearer/JWT.
SwaggerConfig marca las operaciones protegidas con bearerAuth para que "Authorize" envíe
el token, y las públicas con security vacío. No añade endpoints inexistentes.

## Correo y datos históricos

Los registros nuevos (incluido el flujo legado) se guardan con strip y minúsculas Locale.ROOT.
Las reglas de unicidad siguen en UsuarioValidator, reutilizado por el servicio de registro.
Se consulta también la forma normalizada para impedir reclamar una cuenta histórica con
distinta capitalización. findByEmail sigue siendo exacto para sus consumidores existentes.

La nueva consulta devuelve una lista mediante lower(trim(email)), nunca un Optional ambiguo.
Login rechaza cero o múltiples candidatos, incluso si uno coincide exactamente.
Los correos históricos no se actualizan. No se consultó una base real; no se afirma que la
base existente esté libre de duplicados. La comparación normalizada requiere auditar los
datos históricos antes del despliegue. La garantía de unicidad en la BD actual sigue siendo
la columna email: registros por el servicio se normalizan y usan esa restricción;
escrituras externas deben respetar la misma política.

## Transición PostgreSQL pendiente de aprobación

No existe Flyway/Liquibase en el POM ni una infraestructura de migraciones en esta rama.
No se añade una migración automática. El esquema inspeccionado corresponde a los mappings
de Etapa 1: usuarios.estado_cuenta nullable, usuario_roles(usuario_id, rol) con unicidad del
par, y credenciales(usuario_id PK/FK, password_hash). No se inspeccionó ni modificó PostgreSQL real.

Plan que el equipo debe aprobar: copia de seguridad, verificación del schema efectivo y tipos,
auditoría de correos ambiguos, revisión del significado de estados null, ensayo en una copia
QA y ejecución por el responsable con ventana controlada. Sustituir public por el schema
verificado si fuese distinto. Primero ejecutar solo las consultas de diagnóstico:

```sql
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('usuarios', 'usuario_roles', 'credenciales')
ORDER BY table_name, ordinal_position;

SELECT lower(btrim(email)) AS email_normalizado, count(*) AS cuentas,
       array_agg(id ORDER BY id) AS ids
FROM public.usuarios
GROUP BY lower(btrim(email))
HAVING count(*) > 1;

SELECT count(*) AS estados_nulos
FROM public.usuarios WHERE estado_cuenta IS NULL;

SELECT count(*) AS usuarios_sin_roles
FROM public.usuarios u
WHERE NOT EXISTS (
    SELECT 1 FROM public.usuario_roles r WHERE r.usuario_id = u.id
);

SELECT count(*) AS usuarios_sin_credencial
FROM public.usuarios u
WHERE NOT EXISTS (
    SELECT 1 FROM public.credenciales c WHERE c.usuario_id = u.id
);
```

Tras aprobación, el siguiente ensayo no destructivo completa únicamente estados y roles ausentes.
Los locks evitan que escrituras concurrentes alteren el conjunto durante la transición.
No cambia emails, roles existentes ni credenciales. El script termina con ROLLBACK para ensayo;
solo el responsable podrá aprobar COMMIT después de validar conteos y reglas:

```sql
BEGIN;
LOCK TABLE public.usuarios, public.usuario_roles IN SHARE ROW EXCLUSIVE MODE;

UPDATE public.usuarios
SET estado_cuenta = 'ACTIVA'
WHERE estado_cuenta IS NULL;

INSERT INTO public.usuario_roles (usuario_id, rol)
SELECT u.id, 'COMPRADOR'
FROM public.usuarios u
WHERE NOT EXISTS (
    SELECT 1 FROM public.usuario_roles r WHERE r.usuario_id = u.id
);

SELECT count(*) FROM public.usuarios WHERE estado_cuenta IS NULL;
SELECT count(*) FROM public.usuarios u
WHERE NOT EXISTS (
    SELECT 1 FROM public.usuario_roles r WHERE r.usuario_id = u.id
);

ROLLBACK;
```

Este SQL no se ejecutó. Si existen correos ambiguos, requieren resolución con verificación de
identidad; nunca escoger una cuenta por preferencia arbitraria. Después de resolverlos, aprobar
por separado un índice único sobre lower(btrim(email)) para proteger también escrituras externas.
No crearlo automáticamente ni migrar emails masivamente. Las cuentas históricas sin credencial
seguirán sin poder iniciar sesión; completar roles/estados no aprovisiona una contraseña.

## Entrega a infraestructura

Variable requerida: JWT_SECRET_BASE64. Formato: Base64 estándar de al menos 32 bytes aleatorios
(256 bits de material real) generados mediante un CSPRNG. Longitud del texto Base64 no equivale
a longitud real de clave. Usar claves independientes en QA y PROD, almacenadas como secrets.
No reutilizar ninguna clave de src/test, ni guardar secretos en properties, imágenes Docker,
Git, logs o ejemplos compartidos. No hay secreto predeterminado de producción.

La ausencia, Base64 inválido o clave decodificada inferior a 32 bytes provoca fallo de creación
de JwtUtil y del arranque del contexto Spring. El mensaje identifica JWT_SECRET_BASE64 sin
imprimir su contenido. El constructor también rechaza duración inferior a un segundo.

Propiedad de duración: styleradar.security.jwt.duration; variable opcional JWT_DURATION.
Formato ISO-8601 Duration; valor inicial PT60M (60 minutos). expiresIn se devuelve en segundos.
No hay refresh tokens, blacklist ni tabla de JWT. Cambiar la duración solo afecta tokens nuevos.
Cambiar la clave invalida los tokens emitidos con la clave anterior.

Docker: el responsable debe inyectar JWT_SECRET_BASE64 desde su mecanismo de secrets al entorno
del proceso Java; si usa un secret montado, un arranque controlado debe leerlo sin imprimirlo y
exportarlo al proceso. Azure: configurar el mismo nombre como secret por entorno, preferentemente
con referencia a Key Vault cuando la plataforma elegida lo soporte, y reiniciar el servicio.
En CI, almacenar la clave de QA/PROD en secrets protegidos y enviarla únicamente al despliegue.
Los tests usan application-test.properties y no necesitan secretos de despliegue.
Validar el arranque en QA y un login/solicitud Bearer, sin imprimir secretos ni tokens en pipelines.

No se modificaron Dockerfile, docker-compose.yml, .dockerignore, .env.example ni .github/workflows/.

## Entrega al Líder y aprovisionamiento privilegiado

El Líder debe completar SecurityConfig: matriz roles/endpoints, autorización de propiedad,
reglas de almacenes/fundaciones y revisión transversal 401/403. Debe coordinar también
SwaggerConfig (lista de operaciones públicas), SecurityErrorHandler y JwtAuthFilter si altera
el flujo de filtros, y GlobalExceptionHandler si añade autorización a controladores.

ADMIN_ALMACEN, REPRESENTANTE_FUNDACION y ADMIN_STYLERADAR se probaron exclusivamente con fixtures.
No hay endpoint público ni cuenta administrativa de producción con contraseña conocida.
La integración posterior debe usar una operación interna controlada por el equipo o un
administrador autorizado, con identidad verificada, asignación de rol aprobada y auditada,
y una contraseña individual entregada por un canal seguro. Puede reutilizar ICredencialService
para guardar BCrypt en la misma identidad existente; conocer un correo no autoriza esa operación.
Debe impedir reemplazar una credencial existente sin una política de recuperación aprobada.
ADMIN_ALMACEN requerirá validar vínculo con el NIT; el rol por sí solo no autoriza cualquier almacén.

No había SecurityConfig/SecurityFilterChain del Líder al iniciar. No se inspeccionaron ni
integraron cambios remotos de otras ramas. Cualquier SecurityConfig paralelo deberá revisarse
antes de integrar; no se puede afirmar ausencia de conflictos futuros.

## Pruebas y límites de la evidencia

Se conservaron las 296 pruebas anteriores sin modificar archivos ni assertions existentes.
Se añadieron 80 ejecuciones:

| Clase | Ejecuciones | Evidencia |
| --- | ---: | --- |
| AuthenticationTest | 18 | Proveedor real, BCrypt, cuatro roles, estados, credencial ausente, ambigüedad, fallos internos, principal y servicios |
| JwtUtilTest | 24 | Firma, manipulación, expiración determinista, formato, sub, none/HS512, fechas y configuración inválida |
| AuthHttpIntegrationTest | 33 | H2, contexto Spring completo, FilterChainProxy, JwtAuthFilter, handlers, contratos, estados/roles actuales y Swagger |
| RegistroAtomicoIntegrationTest | 5 | Transacción real sin transacción de test: rollback de hash/guardado, commit y HTTP 500 ante fallos de persistencia |

El 403 se demuestra en /test-security/admin mediante una cadena/configuración y controller
exclusivos de tests. No se afirma que exista una regla de autorización por rol en producción.
Las pruebas HTTP anteriores que usan standaloneSetup o webAppContextSetup sin springSecurity
conservan su valor de regresión funcional y no demuestran seguridad.

Las pruebas de esta etapa usan H2 en modo PostgreSQL; no sustituyen validación sobre la instancia
PostgreSQL de QA. Persistencia real de cambios de estado/roles se fuerza con flush/clear.
La semántica de estado y rol se relee al iniciar la siguiente validación, sin garantía de cancelar
una solicitud que ya fue autenticada concurrentemente con una modificación.

Limitaciones deliberadas: sin rate limiting, refresh, revocación individual, recuperación de
contraseña o aprovisionamiento administrativo público; sin autorización definitiva de propiedad
o pertenencia. No se implementaron RF05, RF06, RF17–RF21, RF29–RF32, RF61, RF62, RF66–RF68 ni RF72.
Comprobar EstadoCuenta para autenticar no implementa la gestión funcional RF66.

Validación ejecutada desde style-radar: mvn clean test y mvn clean verify.
No se cambió la regla JaCoCo LINE >= 85 % BUNDLE, ni se excluyeron clases.

No se hicieron commit, push, merge, rebase, reset, cherry-pick ni PR. La Etapa 3 queda pendiente
de instrucción del usuario.

## Resultado final y revisión Git

- Rama inicial y final: feature/s3-daniel-functional-security.
- HEAD inicial y final: 859696ed3f4837303f4916aa95081ac416448da9.
- Último commit: feat(auth): add credentials and account roles.
- git status --short inicial: sin salida; árbol limpio.
- mvn clean test: BUILD SUCCESS, 376 pruebas, 0 failures, 0 errors, 0 skipped.
- mvn clean verify: BUILD SUCCESS, 376 pruebas, 0 failures, 0 errors, 0 skipped;
  empaquetado completado y "All coverage checks have been met".
- JaCoCo BUNDLE LINE: 1528 cubiertas, 107 no cubiertas; 93,4557 % (antes 92,44 %).
- JaCoCo BUNDLE BRANCH: 344 cubiertas, 126 no cubiertas; 73,1915 %.
- JaCoCo LINE >= 85 % permanece sin modificaciones.
- git diff --check: código de salida 0, sin errores de whitespace.
  Git avisa únicamente de conversión LF/CRLF en CredencialValidator.
- Estado final: 9 archivos modificados y 22 nuevos, sin staging ni eliminaciones.
- Listo para commit: SÍ, para revisión y versionado de esta etapa. El despliegue exige
  entregar el secret a infraestructura y aprobar la transición PostgreSQL y autorización posterior.
- Maven utilizó el repositorio local normal; no fue necesario uno temporal.
  Algunos procesos fallaron al crearse dentro del sandbox; se repitieron con permiso,
  sin cambiar el POM para ocultar fallos ambientales.

Archivos creados (22):

- `style-radar/docs/sprint3-etapa2-autenticacion.md`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/config/SecurityConfig.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/AuthController.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/docs/AuthApi.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/LoginResult.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/LoginRequestDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/RegistroAuthRequestDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/response/LoginResponseDTO.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/EmailNormalizer.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/JwtAuthFilter.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/JwtUtil.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/SecurityErrorHandler.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/UsuarioPrincipal.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/UsuarioUserDetailsService.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/IAuthService.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/AuthServiceImpl.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/PasswordRequestValidator.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/PasswordValido.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/AuthHttpIntegrationTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/AuthenticationTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/JwtUtilTest.java`
- `style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/RegistroAtomicoIntegrationTest.java`

Archivos modificados (9):

- `style-radar/pom.xml`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/config/SwaggerConfig.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/exception/GlobalExceptionHandler.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/UsuarioRepository.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/UsuarioServiceImpl.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/CredencialValidator.java`
- `style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/UsuarioValidator.java`
- `style-radar/src/main/resources/application.properties`
- `style-radar/src/test/resources/application-test.properties`

Archivos eliminados: ninguno. Ningún archivo de tests previo se modificó.

Snapshot equivalente a git status --short --untracked-files=all:

```text
 M style-radar/pom.xml
 M style-radar/src/main/java/edu/dosw/proyecto/style_radar/config/SwaggerConfig.java
 M style-radar/src/main/java/edu/dosw/proyecto/style_radar/exception/GlobalExceptionHandler.java
 M style-radar/src/main/java/edu/dosw/proyecto/style_radar/repository/UsuarioRepository.java
 M style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/UsuarioServiceImpl.java
 M style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/CredencialValidator.java
 M style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/UsuarioValidator.java
 M style-radar/src/main/resources/application.properties
 M style-radar/src/test/resources/application-test.properties
?? style-radar/docs/sprint3-etapa2-autenticacion.md
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/config/SecurityConfig.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/AuthController.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/controller/docs/AuthApi.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/domain/LoginResult.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/LoginRequestDTO.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/request/RegistroAuthRequestDTO.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/model/dto/response/LoginResponseDTO.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/EmailNormalizer.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/JwtAuthFilter.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/JwtUtil.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/SecurityErrorHandler.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/UsuarioPrincipal.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/security/UsuarioUserDetailsService.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/IAuthService.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/service/impl/AuthServiceImpl.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/PasswordRequestValidator.java
?? style-radar/src/main/java/edu/dosw/proyecto/style_radar/validator/PasswordValido.java
?? style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/AuthHttpIntegrationTest.java
?? style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/AuthenticationTest.java
?? style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/JwtUtilTest.java
?? style-radar/src/test/java/edu/dosw/proyecto/style_radar/security/RegistroAtomicoIntegrationTest.java
```

