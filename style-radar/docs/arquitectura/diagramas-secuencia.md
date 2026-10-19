# Documento de arquitectura del backend — Diagramas de secuencia

Un diagrama de secuencia muestra cómo interactúan los componentes de la API a lo largo del tiempo para resolver un caso de uso concreto. Cada diagrama recorre la petición desde el cliente hasta la base de datos y de vuelta: filtro JWT, Controller, validación, Mapper, Service, Repository y PostgreSQL. También incluye las rutas de error que devuelve la API. Los archivos editables están en [`docs/uml/secuencia`](../uml/secuencia).

## Notación

- **Líneas de vida:** cada componente tiene una caja en la parte superior y una línea vertical punteada. Los rectángulos delgados sobre la línea son las barras de activación e indican el tiempo en que el componente está procesando.
- **Mensajes síncronos:** flecha continua con punta rellena. Es una llamada que espera respuesta.
- **Mensajes de respuesta:** flecha punteada con punta abierta.
- **Mensajes a sí mismo:** flecha que sale y vuelve al mismo componente. Representa un paso interno, por ejemplo validar el token o calcular un valor.
- **Fragmento `alt`:** marco con alternativas separadas por una línea punteada. Cada alternativa tiene su condición entre corchetes: ❌ para la ruta de error y ✅ para el camino feliz.

Todos los diagramas usan el mismo código de colores:

| Color | Componente |
|---|---|
| Blanco | Cliente |
| Gris | Filtro JWT, configuración de seguridad y utilidades |
| Azul | Controller |
| Amarillo | Validación (`@Valid`) y Mapper |
| Verde | Service y calculadores |
| Morado | Repository |
| Amarillo claro (cilindro) | PostgreSQL |
| Rojo | Manejadores de error (`GlobalExceptionHandler`, `SecurityErrorHandler`) |

## Índice

- [Inicio de sesión](#inicio-de-sesión)
- [Publicar una prenda en el catálogo](#publicar-una-prenda-en-el-catálogo)
- [Búsqueda de prendas con filtros](#búsqueda-de-prendas-con-filtros)
- [Suscripción de un almacén a un plan](#suscripción-de-un-almacén-a-un-plan)

## Inicio de sesión

**RF-02 · Iniciar sesión** · `POST /api/v1/auth/login` · Público

![Secuencia de inicio de sesión](../uml/secuencia/SecuenciaInicioSesion.png)

**Camino feliz:**

1. El cliente envía el correo y la contraseña. Como la ruta es pública, `JwtAuthFilter` no exige token y deja continuar la petición.
2. `AuthController` valida el `LoginRequestDTO` con `@Valid`.
3. `AuthServiceImpl` normaliza el correo y delega en el `AuthenticationManager` de Spring Security.
4. `UsuarioUserDetailsService` carga el usuario, sus roles y su credencial desde PostgreSQL.
5. El `AuthenticationManager` compara la contraseña con el hash BCrypt y revisa que la cuenta esté activa.
6. Si todo es correcto, `JwtUtil` firma el token con el identificador del usuario.
7. La API responde `200 OK` con el token, su tipo (`Bearer`), su vigencia, el identificador del usuario y sus roles.

**Rutas de error:**

- **Datos inválidos:** si el correo o la contraseña llegan vacíos, o el correo no tiene formato válido, `@Valid` lanza `MethodArgumentNotValidException`. `GlobalExceptionHandler` la convierte en `400 Bad Request`.
- **Credenciales incorrectas:** si la contraseña no coincide, el usuario no existe o la cuenta no está `ACTIVA`, la respuesta es `401 Unauthorized` con el mensaje "Autenticación inválida". El mensaje es el mismo en los tres casos para no revelar qué correos están registrados.

## Publicar una prenda en el catálogo

**RF-50 · Publicar prenda en el catálogo** (incluye RF-53 y RF-54) · `POST /api/v1/almacenes/{nit}/catalogo` · `ADMIN_ALMACEN`

![Secuencia de publicación de prenda](../uml/secuencia/SecuenciaPublicarPrenda.png)

**Camino feliz:**

1. `JwtAuthFilter` valida la firma y la vigencia del token y carga el usuario activo, y `SecurityConfig` exige el rol `ADMIN_ALMACEN`.
2. `CatalogoController` valida el `CatalogoItemRequestDTO` con `@Valid`.
3. `CatalogoItemMapper` convierte el DTO en el objeto de dominio `Prenda`.
4. `CatalogoServiceImpl` busca el almacén por NIT.
5. El servicio guarda la prenda.
6. `EstadoItemCalculator` calcula el estado inicial del ítem (`NUEVA_PRENDA`, `DISPONIBLE`, `ULTIMAS_UNIDADES` o `AGOTADA`).
7. El servicio guarda el ítem en `items_catalogo`.
8. El Mapper construye la respuesta y la API devuelve `201 Created`.

**Rutas de error:**

- **Sin autenticación:** sin token, con token vencido o con una cuenta inactiva, `SecurityErrorHandler` responde `401`.
- **Rol incorrecto:** si el usuario no tiene el rol `ADMIN_ALMACEN`, la respuesta es `403 Forbidden` con "Acceso denegado".
- **Datos inválidos:** si falta un campo obligatorio, un texto supera su longitud máxima o el precio no es mayor que cero, la respuesta es `400` con el detalle de cada campo.
- **Almacén inexistente:** si no hay un almacén con ese NIT, el servicio lanza `RecursoNoEncontradoException` y la API responde `404`.

## Búsqueda de prendas con filtros

**RF-07 a RF-16 · Búsqueda, filtros, ordenamiento y prendas similares** · `GET /api/v1/catalogo/buscar` · Público

![Secuencia de búsqueda con filtros](../uml/secuencia/SecuenciaBusquedaFiltros.png)

**Camino feliz:**

1. La búsqueda es pública, así que el filtro JWT deja pasar la petición sin token.
2. `@Valid` revisa los filtros y la paginación.
3. `BusquedaCatalogoMapper` convierte los parámetros en un `BusquedaCatalogoCriteria`.
4. `BusquedaCatalogoServiceImpl` arma la consulta con `ItemCatalogoSpecifications`, que combina solo los filtros enviados: texto, tipo, color, talla, precio, marca y estilo. `ItemCatalogoRepository` la ejecuta en PostgreSQL de forma paginada.
5. El fragmento `alt` interno muestra el comportamiento de RF-16: si no hay coincidencias directas y se envió un texto `q`, el servicio busca de nuevo sin el filtro de texto y ordena los candidatos por similitud.
6. En los dos casos, el servicio aplica el estado efectivo de cada ítem, de modo que las prendas agotadas no aparecen como disponibles (RF-57).
7. La respuesta es `200 OK` con una página de resultados. Si nada coincide, la lista `content` llega vacía.

**Ruta de error:** si `precioMin` es mayor que `precioMax`, `size` supera 100 o se pide ordenar por distancia sin las dos coordenadas, la respuesta es `400 Bad Request`.

## Suscripción de un almacén a un plan

**RF-67 · Suscribir almacén aliado** · `POST /api/v1/almacenes/{nit}/suscripciones` · `ADMIN_ALMACEN`

![Secuencia de suscripción de almacén](../uml/secuencia/SecuenciaSuscripcionAlmacen.png)

**Camino feliz:**

1. `JwtAuthFilter` valida el token.
2. `SecurityConfig` exige el rol `ADMIN_ALMACEN` para esta ruta.
3. El Controller valida el `CrearSuscripcionRequestDTO`.
4. `SuscripcionAlmacenServiceImpl` llama a `AutorizacionSuscripcionService` para confirmar que el administrador está vinculado al NIT.
5. El servicio consulta, con bloqueo de escritura, el almacén, el plan y la suscripción anterior. El bloqueo evita que dos peticiones simultáneas creen dos suscripciones activas.
6. El servicio calcula el fin del periodo con el `Clock` de la aplicación y la duración del plan.
7. Guarda la nueva suscripción `ACTIVA` y la API responde `201 Created`.

**Rutas de error:**

- **Rol incorrecto:** si el usuario no tiene el rol `ADMIN_ALMACEN`, la respuesta es `403 Forbidden` con "Acceso denegado".
- **Regla de negocio:** si el almacén no está `APROBADO` o el plan está inactivo, la respuesta es `422 Unprocessable Content`.
- **Conflicto:** si el almacén ya tiene una suscripción vigente, la respuesta es `409 Conflict` con "El almacén ya tiene una suscripción vigente activa".
