# Documento de arquitectura del backend — Funcionalidades expuestas (parte A)

Esta sección documenta los endpoints de los módulos de identidad y acceso, perfil, personalización, búsqueda, geolocalización de almacenes, catálogo e inventario, administración y suscripciones. Cubre los requerimientos RF-01 a RF-22 y RF-50 a RF-68 del análisis de requerimientos. La información se tomó del documento OpenAPI que genera la aplicación (`/v3/api-docs`), por lo que coincide con Swagger.

Cada endpoint se presenta con:

- **Encabezado:** método y ruta, rol requerido, código de la respuesta exitosa y requerimientos asociados.
- **Parámetros:** los de ruta y de consulta, con su tipo y restricciones.
- **Request y response:** una tabla de campos y un ejemplo en JSON. Los objetos anidados se describen en una tabla propia, debajo del objeto que los contiene.

En las tablas de request, la columna *Obligatorio* y las restricciones (longitud, rangos, formato y valores de las enumeraciones) corresponden a las validaciones de los DTO. Si alguna no se cumple, la API responde `400 Bad Request`.

En las tablas de response, la columna *Restricciones* muestra los valores posibles de las enumeraciones.

Los roles posibles son:

- **Público:** no requiere token.
- **Autenticado (cualquier rol):** exige un JWT válido, sin importar el rol.
- **Un rol específico:** el resto de endpoints exige el rol indicado. Sin token la respuesta es `401` y con otro rol es `403`.

Las fechas usan el formato ISO-8601 en UTC y los precios están en pesos colombianos (COP).

## Índice

- [Autenticación e identidad](#autenticación-e-identidad)
- [Perfil del usuario](#perfil-del-usuario)
- [Personalización](#personalización)
- [Búsqueda y descubrimiento de prendas](#búsqueda-y-descubrimiento-de-prendas)
- [Geolocalización de almacenes](#geolocalización-de-almacenes)
- [Catálogo e inventario del almacén](#catálogo-e-inventario-del-almacén)
- [Administración de la plataforma](#administración-de-la-plataforma)
- [Suscripciones de almacenes](#suscripciones-de-almacenes)

## Resumen de endpoints

| Método | Ruta | Rol | Éxito | RF |
|---|---|---|---|---|
| POST | `/api/v1/auth/registro` | Público (sin token) | 201 | RF-01 |
| POST | `/api/v1/usuarios` | Público (sin token) | 201 | RF-01 |
| POST | `/api/v1/auth/login` | Público (sin token) | 200 | RF-02 |
| GET | `/api/v1/usuarios/me` | COMPRADOR | 200 | RF-03, RF-04 |
| PUT | `/api/v1/usuarios/me/preferencias-estilo` | COMPRADOR | 200 | RF-03 |
| PUT | `/api/v1/usuarios/me/tallas-habituales` | COMPRADOR | 200 | RF-04 |
| GET | `/api/v1/usuarios/{id}` | ADMIN_STYLERADAR | 200 | RF-66 |
| GET | `/api/v1/usuarios/me/feed` | Autenticado (cualquier rol) | 200 | RF-05 |
| GET | `/api/v1/usuarios/me/recomendaciones` | Autenticado (cualquier rol) | 200 | RF-06 |
| GET | `/api/v1/catalogo/buscar` | Público (sin token) | 200 | RF-07 a RF-16, RF-54, RF-57 |
| GET | `/api/v1/prendas` | Público (sin token) | 200 | RF-07 |
| GET | `/api/v1/almacenes` | Público (sin token) | 200 | RF-17, RF-18 |
| GET | `/api/v1/almacenes/{nit}/catalogo/resumen` | Público (sin token) | 200 | RF-19 |
| GET | `/api/v1/almacenes/{nit}/catalogo/novedades` | Público (sin token) | 200 | RF-20 |
| GET | `/api/v1/almacenes/{nit}/distancia` | Público (sin token) | 200 | RF-21 |
| GET | `/api/v1/almacenes/{nit}/catalogo` | Público (sin token) | 200 | RF-22, RF-60 |
| POST | `/api/v1/almacenes/{nit}/catalogo` | Autenticado (cualquier rol) | 201 | RF-50, RF-53, RF-54 |
| PUT | `/api/v1/almacenes/{nit}/catalogo/{itemId}` | Autenticado (cualquier rol) | 200 | RF-53, RF-55 |
| DELETE | `/api/v1/almacenes/{nit}/catalogo/{itemId}` | Autenticado (cualquier rol) | 204 | RF-58 |
| POST | `/api/v1/almacenes/{nit}/catalogo/{itemId}/imagenes` | Autenticado (cualquier rol) | 201 | RF-51 |
| PUT | `/api/v1/almacenes/{nit}/catalogo/{itemId}/tallas` | Autenticado (cualquier rol) | 200 | RF-52 |
| PATCH | `/api/v1/almacenes/{nit}/catalogo/{itemId}/inventario/{talla}` | Autenticado (cualquier rol) | 200 | RF-56, RF-57, RF-59 |
| GET | `/api/v1/admin/catalogo/moderacion` | ADMIN_STYLERADAR | 200 | RF-61 |
| PATCH | `/api/v1/admin/catalogo/{itemId}/moderacion` | ADMIN_STYLERADAR | 200 | RF-61 |
| POST | `/api/v1/admin/catalogo/{itemId}/moderacion/solicitar-revision` | ADMIN_STYLERADAR | 200 | RF-61 |
| GET | `/api/v1/admin/almacenes/verificacion` | ADMIN_STYLERADAR | 200 | RF-62 |
| GET | `/api/v1/admin/almacenes/{nit}/verificacion` | ADMIN_STYLERADAR | 200 | RF-62 |
| PATCH | `/api/v1/admin/almacenes/{nit}/verificacion` | ADMIN_STYLERADAR | 200 | RF-62 |
| POST | `/api/v1/admin/almacenes/{nit}/verificacion/solicitar-revision` | ADMIN_STYLERADAR | 200 | RF-62 |
| POST | `/api/v1/admin/fundaciones` | ADMIN_STYLERADAR | 201 | RF-63 |
| GET | `/api/v1/admin/fundaciones` | ADMIN_STYLERADAR | 200 | RF-63 |
| PUT | `/api/v1/admin/fundaciones/{id}` | ADMIN_STYLERADAR | 200 | RF-64 |
| PATCH | `/api/v1/admin/fundaciones/{id}/desactivar` | ADMIN_STYLERADAR | 200 | RF-65 |
| GET | `/api/v1/admin/usuarios` | ADMIN_STYLERADAR | 200 | RF-66 |
| GET | `/api/v1/admin/usuarios/{id}` | ADMIN_STYLERADAR | 200 | RF-66 |
| PATCH | `/api/v1/admin/usuarios/{id}/estado` | ADMIN_STYLERADAR | 200 | RF-66 |
| GET | `/api/v1/planes-suscripcion` | Público (sin token) | 200 | RF-67 |
| PUT | `/api/v1/admin/almacenes/{nit}/administradores/{usuarioId}` | ADMIN_STYLERADAR | 200 | RF-67 |
| POST | `/api/v1/almacenes/{nit}/suscripciones` | ADMIN_ALMACEN | 201 | RF-67 |
| GET | `/api/v1/almacenes/{nit}/suscripciones/vigente` | ADMIN_ALMACEN | 200 | RF-67 |
| GET | `/api/v1/almacenes/{nit}/suscripciones` | ADMIN_ALMACEN | 200 | RF-67 |
| PATCH | `/api/v1/almacenes/{nit}/suscripciones/{id}/renovacion` | ADMIN_ALMACEN | 200 | RF-68 |

## Autenticación e identidad

### `POST /api/v1/auth/registro`

**Rol:** Público (sin token) · **Respuesta exitosa:** 201 Created · **RF:** RF-01

Registrar un nuevo comprador con contraseña.

Request: `RegistroAuthRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nombre` | String | Sí | mínimo 2 caracteres, máximo 100 caracteres | Nombre |
| `email` | String (email) | Sí | no vacío, formato email | Correo electrónico |
| `telefono` | String | No | patrón `^[+0-9\-\s()]{7,20}$` | Teléfono de contacto |
| `password` | String | Sí | no vacía, máximo 72 bytes (validación @PasswordValido) | Contraseña |

```json
{
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567",
  "password": "Clave.Segura1"
}
```

Response: `UsuarioResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `telefono` | String | — | — | Teléfono de contacto |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `preferenciasEstilo` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilos preferidos del usuario |
| `tallasHabituales` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas habituales del usuario |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "preferenciasEstilo": [
    "CASUAL"
  ],
  "tallasHabituales": [
    "XS"
  ]
}
```


### `POST /api/v1/usuarios`

**Rol:** Público (sin token) · **Respuesta exitosa:** 201 Created · **RF:** RF-01

Registrar usuario.

Request: `RegistrarUsuarioRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nombre` | String | Sí | mínimo 2 caracteres, máximo 100 caracteres | Nombre |
| `email` | String (email) | Sí | no vacío, formato email | Correo electrónico |
| `telefono` | String | No | patrón `^[+0-9\-\s()]{7,20}$` | Teléfono de contacto |

```json
{
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567"
}
```

Response: `UsuarioResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `telefono` | String | — | — | Teléfono de contacto |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `preferenciasEstilo` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilos preferidos del usuario |
| `tallasHabituales` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas habituales del usuario |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "preferenciasEstilo": [
    "CASUAL"
  ],
  "tallasHabituales": [
    "XS"
  ]
}
```


### `POST /api/v1/auth/login`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-02

RF02: iniciar sesión con correo y contraseña.

Request: `LoginRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `email` | String (email) | Sí | no vacío, formato email | Correo electrónico |
| `password` | String | Sí | no vacía, máximo 72 bytes (validación @PasswordValido) | Contraseña |

```json
{
  "email": "comprador@styleradar.co",
  "password": "Clave.Segura1"
}
```

Response: `LoginResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `accessToken` | String | — | — | Token JWT firmado para el encabezado Authorization |
| `tokenType` | String | — | — | Tipo de token (Bearer) |
| `expiresIn` | Long | — | — | Segundos de vigencia del token |
| `usuarioId` | Long | — | — | Identificador del usuario |
| `roles` | Lista de enum | — | COMPRADOR · ADMIN_ALMACEN · REPRESENTANTE_FUNDACION · ADMIN_STYLERADAR | Roles del usuario |

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "usuarioId": 1,
  "roles": [
    "COMPRADOR"
  ]
}
```


## Perfil del usuario

### `GET /api/v1/usuarios/me`

**Rol:** COMPRADOR · **Respuesta exitosa:** 200 OK · **RF:** RF-03, RF-04

Consultar mi perfil.

Response: `UsuarioResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `telefono` | String | — | — | Teléfono de contacto |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `preferenciasEstilo` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilos preferidos del usuario |
| `tallasHabituales` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas habituales del usuario |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "preferenciasEstilo": [
    "CASUAL"
  ],
  "tallasHabituales": [
    "XS"
  ]
}
```


### `PUT /api/v1/usuarios/me/preferencias-estilo`

**Rol:** COMPRADOR · **Respuesta exitosa:** 200 OK · **RF:** RF-03

Configurar mis preferencias de estilo.

Request: `ActualizarPreferenciasEstiloRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `preferencias` | Lista de enum | Sí | sin repetidos, CASUAL · FORMAL · DEPORTIVO · URBANO | Estilos preferidos del usuario |

```json
{
  "preferencias": [
    "CASUAL"
  ]
}
```

Response: `UsuarioResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `telefono` | String | — | — | Teléfono de contacto |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `preferenciasEstilo` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilos preferidos del usuario |
| `tallasHabituales` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas habituales del usuario |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "preferenciasEstilo": [
    "CASUAL"
  ],
  "tallasHabituales": [
    "XS"
  ]
}
```


### `PUT /api/v1/usuarios/me/tallas-habituales`

**Rol:** COMPRADOR · **Respuesta exitosa:** 200 OK · **RF:** RF-04

Configurar mis tallas habituales.

Request: `ActualizarTallasHabitualesRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `tallas` | Lista de enum | Sí | sin repetidos, XS · S · M · L · XL · XXL | Tallas registradas |

```json
{
  "tallas": [
    "XS"
  ]
}
```

Response: `UsuarioResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `telefono` | String | — | — | Teléfono de contacto |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `preferenciasEstilo` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilos preferidos del usuario |
| `tallasHabituales` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas habituales del usuario |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "preferenciasEstilo": [
    "CASUAL"
  ],
  "tallasHabituales": [
    "XS"
  ]
}
```


### `GET /api/v1/usuarios/{id}`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-66

Consultar usuario por ID.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `id` | ruta | Long | Sí | — |

Response: `UsuarioResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `telefono` | String | — | — | Teléfono de contacto |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `preferenciasEstilo` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilos preferidos del usuario |
| `tallasHabituales` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas habituales del usuario |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "telefono": "3001234567",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "preferenciasEstilo": [
    "CASUAL"
  ],
  "tallasHabituales": [
    "XS"
  ]
}
```


## Personalización

### `GET /api/v1/usuarios/me/feed`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 200 OK · **RF:** RF-05

Obtener mi feed personalizado.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `page` | query | Integer | Sí | ≥ 0 |
| `size` | query | Integer | Sí | ≥ 1, ≤ 100 |

Response: `PageResponseDTOCatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `CatalogoItemResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `CatalogoItemResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "content": [
    {
      "itemId": 1,
      "prendaId": 1,
      "almacenNit": "900123456-1",
      "nombre": "Chaqueta de cuero negra",
      "descripcion": "Chaqueta de cuero con cierre frontal",
      "tipo": "SUPERIOR",
      "marca": "Urbano",
      "color": "negro",
      "estilo": "CASUAL",
      "precio": 189900.0,
      "stock": 12,
      "estado": "DISPONIBLE",
      "fechaPublicacion": "2026-10-18T15:00:00Z",
      "tallasDisponibles": [
        "XS"
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `GET /api/v1/usuarios/me/recomendaciones`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 200 OK · **RF:** RF-06

Obtener mis recomendaciones de prendas.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `page` | query | Integer | Sí | ≥ 0 |
| `size` | query | Integer | Sí | ≥ 1, ≤ 100 |

Response: `RecomendacionesResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `CatalogoItemResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |
| `generalSinPreferencias` | Boolean | — | — | true si el usuario no tiene preferencias y se devuelve el catálogo general |

Objeto anidado `CatalogoItemResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "content": [
    {
      "itemId": 1,
      "prendaId": 1,
      "almacenNit": "900123456-1",
      "nombre": "Chaqueta de cuero negra",
      "descripcion": "Chaqueta de cuero con cierre frontal",
      "tipo": "SUPERIOR",
      "marca": "Urbano",
      "color": "negro",
      "estilo": "CASUAL",
      "precio": 189900.0,
      "stock": 12,
      "estado": "DISPONIBLE",
      "fechaPublicacion": "2026-10-18T15:00:00Z",
      "tallasDisponibles": [
        "XS"
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "generalSinPreferencias": false
}
```


## Búsqueda y descubrimiento de prendas

### `GET /api/v1/catalogo/buscar`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-07 a RF-16, RF-54, RF-57

Buscar en el catálogo global.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `q` | query | String | No | no puede estar en blanco |
| `tipo` | query | Enum | No | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO |
| `color` | query | String | No | no puede estar en blanco |
| `talla` | query | Enum | No | XS · S · M · L · XL · XXL |
| `precioMin` | query | Double | No | ≥ 0.0 |
| `precioMax` | query | Double | No | ≥ 0.0 |
| `marca` | query | String | No | no puede estar en blanco |
| `estilo` | query | Enum | No | CASUAL · FORMAL · DEPORTIVO · URBANO |
| `orden` | query | Enum | No | DISTANCIA · REPUTACION |
| `latitudUsuario` | query | Double | No | ≥ -90.0, ≤ 90.0 |
| `longitudUsuario` | query | Double | No | ≥ -180.0, ≤ 180.0 |
| `page` | query | Integer | Sí | ≥ 0 |
| `size` | query | Integer | Sí | ≥ 1, ≤ 100 |

Response: `PageResponseDTOCatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `CatalogoItemResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `CatalogoItemResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "content": [
    {
      "itemId": 1,
      "prendaId": 1,
      "almacenNit": "900123456-1",
      "nombre": "Chaqueta de cuero negra",
      "descripcion": "Chaqueta de cuero con cierre frontal",
      "tipo": "SUPERIOR",
      "marca": "Urbano",
      "color": "negro",
      "estilo": "CASUAL",
      "precio": 189900.0,
      "stock": 12,
      "estado": "DISPONIBLE",
      "fechaPublicacion": "2026-10-18T15:00:00Z",
      "tallasDisponibles": [
        "XS"
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `GET /api/v1/prendas`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-07

Obtener prendas.

Response: lista de `PrendaResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |

```json
[
  {
    "id": 1,
    "nombre": "Chaqueta de cuero negra",
    "descripcion": "Chaqueta de cuero con cierre frontal",
    "tipo": "SUPERIOR",
    "marca": "Urbano",
    "color": "negro",
    "estilo": "CASUAL"
  }
]
```


## Geolocalización de almacenes

### `GET /api/v1/almacenes`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-17, RF-18

Consultar almacenes geolocalizables.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `categoria` | query | Enum | No | CASUAL · FORMAL · DEPORTIVA · VINTAGE · ACCESORIOS |
| `page` | query | Integer | No | ≥ 0 |
| `size` | query | Integer | No | ≥ 1, ≤ 100 |

Response: `PageResponseDTOAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `AlmacenResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `AlmacenResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nit` | String | — | — | NIT del almacén |
| `nombreComercial` | String | — | — | Nombre comercial del almacén |
| `descripcion` | String | — | — | Descripción |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `reputacion` | Double | — | — | Reputación del almacén |
| `categorias` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVA · VINTAGE · ACCESORIOS | Categorías comerciales del almacén |

```json
{
  "content": [
    {
      "nit": "900123456-1",
      "nombreComercial": "Urbano Norte",
      "descripcion": "Chaqueta de cuero con cierre frontal",
      "latitud": 4.6683,
      "longitud": -74.0534,
      "reputacion": 4.6,
      "categorias": [
        "CASUAL"
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `GET /api/v1/almacenes/{nit}/catalogo/resumen`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-19

Consultar resumen público del catálogo.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `limite` | query | Integer | No | ≥ 1, ≤ 20 |

Response: lista de `CatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
[
  {
    "itemId": 1,
    "prendaId": 1,
    "almacenNit": "900123456-1",
    "nombre": "Chaqueta de cuero negra",
    "descripcion": "Chaqueta de cuero con cierre frontal",
    "tipo": "SUPERIOR",
    "marca": "Urbano",
    "color": "negro",
    "estilo": "CASUAL",
    "precio": 189900.0,
    "stock": 12,
    "estado": "DISPONIBLE",
    "fechaPublicacion": "2026-10-18T15:00:00Z",
    "tallasDisponibles": [
      "XS"
    ]
  }
]
```


### `GET /api/v1/almacenes/{nit}/catalogo/novedades`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-20

Consultar novedades semanales.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `page` | query | Integer | No | ≥ 0 |
| `size` | query | Integer | No | ≥ 1, ≤ 100 |

Response: `PageResponseDTOCatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `CatalogoItemResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `CatalogoItemResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "content": [
    {
      "itemId": 1,
      "prendaId": 1,
      "almacenNit": "900123456-1",
      "nombre": "Chaqueta de cuero negra",
      "descripcion": "Chaqueta de cuero con cierre frontal",
      "tipo": "SUPERIOR",
      "marca": "Urbano",
      "color": "negro",
      "estilo": "CASUAL",
      "precio": 189900.0,
      "stock": 12,
      "estado": "DISPONIBLE",
      "fechaPublicacion": "2026-10-18T15:00:00Z",
      "tallasDisponibles": [
        "XS"
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `GET /api/v1/almacenes/{nit}/distancia`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-21

Estimar distancia geográfica al almacén.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `latitudUsuario` | query | Double | Sí | ≥ -90, ≤ 90 |
| `longitudUsuario` | query | Double | Sí | ≥ -180, ≤ 180 |

Response: `DistanciaAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `almacenNit` | String | — | — | NIT del almacén |
| `distanciaKm` | Número | — | — | Distancia estimada en kilómetros |
| `tipoEstimacion` | String | — | — | Método usado para estimar la distancia |

```json
{
  "almacenNit": "900123456-1",
  "distanciaKm": 2.4,
  "tipoEstimacion": "texto"
}
```


## Catálogo e inventario del almacén

### `GET /api/v1/almacenes/{nit}/catalogo`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-22, RF-60

Consultar catálogo.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |

Response: lista de `CatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
[
  {
    "itemId": 1,
    "prendaId": 1,
    "almacenNit": "900123456-1",
    "nombre": "Chaqueta de cuero negra",
    "descripcion": "Chaqueta de cuero con cierre frontal",
    "tipo": "SUPERIOR",
    "marca": "Urbano",
    "color": "negro",
    "estilo": "CASUAL",
    "precio": 189900.0,
    "stock": 12,
    "estado": "DISPONIBLE",
    "fechaPublicacion": "2026-10-18T15:00:00Z",
    "tallasDisponibles": [
      "XS"
    ]
  }
]
```


### `POST /api/v1/almacenes/{nit}/catalogo`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 201 Created · **RF:** RF-50, RF-53, RF-54

Publicar prenda.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |

Request: `CatalogoItemRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nombre` | String | Sí | máximo 100 caracteres | Nombre |
| `descripcion` | String | Sí | máximo 200 caracteres | Descripción |
| `tipo` | Enum | Sí | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | Sí | máximo 30 caracteres | Marca de la prenda |
| `color` | String | Sí | máximo 30 caracteres | Color principal de la prenda |
| `estilo` | Enum | Sí | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | Sí | > 0 | Precio en COP |

```json
{
  "nombre": "Chaqueta de cuero negra",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "tipo": "SUPERIOR",
  "marca": "Urbano",
  "color": "negro",
  "estilo": "CASUAL",
  "precio": 189900.0
}
```

Response: `CatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "itemId": 1,
  "prendaId": 1,
  "almacenNit": "900123456-1",
  "nombre": "Chaqueta de cuero negra",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "tipo": "SUPERIOR",
  "marca": "Urbano",
  "color": "negro",
  "estilo": "CASUAL",
  "precio": 189900.0,
  "stock": 12,
  "estado": "DISPONIBLE",
  "fechaPublicacion": "2026-10-18T15:00:00Z",
  "tallasDisponibles": [
    "XS"
  ]
}
```


### `PUT /api/v1/almacenes/{nit}/catalogo/{itemId}`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 200 OK · **RF:** RF-53, RF-55

Editar prenda del catálogo.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `itemId` | ruta | Long | Sí | — |

Request: `CatalogoItemRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nombre` | String | Sí | máximo 100 caracteres | Nombre |
| `descripcion` | String | Sí | máximo 200 caracteres | Descripción |
| `tipo` | Enum | Sí | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | Sí | máximo 30 caracteres | Marca de la prenda |
| `color` | String | Sí | máximo 30 caracteres | Color principal de la prenda |
| `estilo` | Enum | Sí | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | Sí | > 0 | Precio en COP |

```json
{
  "nombre": "Chaqueta de cuero negra",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "tipo": "SUPERIOR",
  "marca": "Urbano",
  "color": "negro",
  "estilo": "CASUAL",
  "precio": 189900.0
}
```

Response: `CatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "itemId": 1,
  "prendaId": 1,
  "almacenNit": "900123456-1",
  "nombre": "Chaqueta de cuero negra",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "tipo": "SUPERIOR",
  "marca": "Urbano",
  "color": "negro",
  "estilo": "CASUAL",
  "precio": 189900.0,
  "stock": 12,
  "estado": "DISPONIBLE",
  "fechaPublicacion": "2026-10-18T15:00:00Z",
  "tallasDisponibles": [
    "XS"
  ]
}
```


### `DELETE /api/v1/almacenes/{nit}/catalogo/{itemId}`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 204 No Content · **RF:** RF-58

Retirar prenda.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `itemId` | ruta | Long | Sí | — |

Response: sin cuerpo.


### `POST /api/v1/almacenes/{nit}/catalogo/{itemId}/imagenes`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 201 Created · **RF:** RF-51

Registrar fotografía.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `itemId` | ruta | Long | Sí | — |

Request (multipart/form-data):

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `archivo` | Archivo | Sí | — | Imagen de la prenda (JPEG, PNG o WebP) |

Response: `ImagenCatalogoResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `itemCatalogoId` | Long | — | — | Identificador del ítem del catálogo |
| `contentType` | String | — | — | Tipo MIME de la imagen |

```json
{
  "id": 1,
  "itemCatalogoId": 1,
  "contentType": "image/jpeg"
}
```


### `PUT /api/v1/almacenes/{nit}/catalogo/{itemId}/tallas`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 200 OK · **RF:** RF-52

Registrar tallas.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `itemId` | ruta | Long | Sí | — |

Request: `TallasDisponiblesRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `tallas` | Lista de enum | Sí | mínimo 1 elementos, sin repetidos, XS · S · M · L · XL · XXL | Tallas registradas |

```json
{
  "tallas": [
    "XS"
  ]
}
```

Response: `CatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "itemId": 1,
  "prendaId": 1,
  "almacenNit": "900123456-1",
  "nombre": "Chaqueta de cuero negra",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "tipo": "SUPERIOR",
  "marca": "Urbano",
  "color": "negro",
  "estilo": "CASUAL",
  "precio": 189900.0,
  "stock": 12,
  "estado": "DISPONIBLE",
  "fechaPublicacion": "2026-10-18T15:00:00Z",
  "tallasDisponibles": [
    "XS"
  ]
}
```


### `PATCH /api/v1/almacenes/{nit}/catalogo/{itemId}/inventario/{talla}`

**Rol:** Autenticado (cualquier rol) · **Respuesta exitosa:** 200 OK · **RF:** RF-56, RF-57, RF-59

Actualizar disponibilidad.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `itemId` | ruta | Long | Sí | — |
| `talla` | ruta | Enum | Sí | XS · S · M · L · XL · XXL |

Request: `ActualizarStockRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `unidades` | Integer | Sí | ≥ 0 | Unidades disponibles en la talla |

```json
{
  "unidades": 3
}
```

Response: `CatalogoItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `prendaId` | Long | — | — | Identificador de la prenda |
| `almacenNit` | String | — | — | NIT del almacén |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |
| `precio` | Double | — | — | Precio en COP |
| `stock` | Integer | — | — | Unidades totales disponibles |
| `estado` | Enum | — | DISPONIBLE · ULTIMAS_UNIDADES · NUEVA_PRENDA · AGOTADA | Estado del recurso |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `tallasDisponibles` | Lista de enum | — | XS · S · M · L · XL · XXL | Tallas con unidades disponibles |

```json
{
  "itemId": 1,
  "prendaId": 1,
  "almacenNit": "900123456-1",
  "nombre": "Chaqueta de cuero negra",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "tipo": "SUPERIOR",
  "marca": "Urbano",
  "color": "negro",
  "estilo": "CASUAL",
  "precio": 189900.0,
  "stock": 12,
  "estado": "DISPONIBLE",
  "fechaPublicacion": "2026-10-18T15:00:00Z",
  "tallasDisponibles": [
    "XS"
  ]
}
```


## Administración de la plataforma

### `GET /api/v1/admin/catalogo/moderacion`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-61

Consultar publicaciones para moderación.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `estado` | query | Enum | Sí | NO_REQUERIDA · PENDIENTE · APROBADA · RECHAZADA |
| `page` | query | Integer | Sí | ≥ 0 |
| `size` | query | Integer | Sí | ≥ 1, ≤ 100 |

Response: `PageResponseDTOModeracionItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `ModeracionItemResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `ModeracionItemResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `almacenNit` | String | — | — | NIT del almacén |
| `prenda` | Objeto `PrendaResponseDTO` | — | — | Prenda asociada |
| `precio` | Double | — | — | Precio en COP |
| `estadoModeracion` | Enum | — | NO_REQUERIDA · PENDIENTE · APROBADA · RECHAZADA | Estado de moderación de la publicación |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `fechaDecisionModeracion` | Fecha-hora ISO-8601 | — | — | Fecha de la decisión de moderación |
| `administradorDecisionId` | Long | — | — | Administrador que tomó la última decisión |
| `motivoModeracion` | String | — | — | Motivo de la decisión de moderación |

Objeto anidado `PrendaResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |

```json
{
  "content": [
    {
      "itemId": 1,
      "almacenNit": "900123456-1",
      "prenda": {
        "id": 1,
        "nombre": "Chaqueta de cuero negra",
        "descripcion": "Chaqueta de cuero con cierre frontal",
        "tipo": "SUPERIOR",
        "marca": "Urbano",
        "color": "negro",
        "estilo": "CASUAL"
      },
      "precio": 189900.0,
      "estadoModeracion": "NO_REQUERIDA",
      "fechaPublicacion": "2026-10-18T15:00:00Z",
      "fechaDecisionModeracion": null,
      "administradorDecisionId": 1,
      "motivoModeracion": "texto"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `PATCH /api/v1/admin/catalogo/{itemId}/moderacion`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-61

Aprobar o rechazar publicación pendiente.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `itemId` | ruta | Long | Sí | — |

Request: `DecisionModeracionRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `decision` | Enum | Sí | APROBADA · RECHAZADA | Decisión del administrador |
| `motivo` | String | No | máximo 1000 caracteres | Motivo de la decisión |

```json
{
  "decision": "APROBADA",
  "motivo": "Datos verificados"
}
```

Response: `ModeracionItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `almacenNit` | String | — | — | NIT del almacén |
| `prenda` | Objeto `PrendaResponseDTO` | — | — | Prenda asociada |
| `precio` | Double | — | — | Precio en COP |
| `estadoModeracion` | Enum | — | NO_REQUERIDA · PENDIENTE · APROBADA · RECHAZADA | Estado de moderación de la publicación |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `fechaDecisionModeracion` | Fecha-hora ISO-8601 | — | — | Fecha de la decisión de moderación |
| `administradorDecisionId` | Long | — | — | Administrador que tomó la última decisión |
| `motivoModeracion` | String | — | — | Motivo de la decisión de moderación |

Objeto anidado `PrendaResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |

```json
{
  "itemId": 1,
  "almacenNit": "900123456-1",
  "prenda": {
    "id": 1,
    "nombre": "Chaqueta de cuero negra",
    "descripcion": "Chaqueta de cuero con cierre frontal",
    "tipo": "SUPERIOR",
    "marca": "Urbano",
    "color": "negro",
    "estilo": "CASUAL"
  },
  "precio": 189900.0,
  "estadoModeracion": "NO_REQUERIDA",
  "fechaPublicacion": "2026-10-18T15:00:00Z",
  "fechaDecisionModeracion": null,
  "administradorDecisionId": 1,
  "motivoModeracion": "texto"
}
```


### `POST /api/v1/admin/catalogo/{itemId}/moderacion/solicitar-revision`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-61

Solicitar revisión manual.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `itemId` | ruta | Long | Sí | — |

Response: `ModeracionItemResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `itemId` | Long | — | — | Identificador del ítem del catálogo |
| `almacenNit` | String | — | — | NIT del almacén |
| `prenda` | Objeto `PrendaResponseDTO` | — | — | Prenda asociada |
| `precio` | Double | — | — | Precio en COP |
| `estadoModeracion` | Enum | — | NO_REQUERIDA · PENDIENTE · APROBADA · RECHAZADA | Estado de moderación de la publicación |
| `fechaPublicacion` | Fecha-hora ISO-8601 | — | — | Fecha de publicación |
| `fechaDecisionModeracion` | Fecha-hora ISO-8601 | — | — | Fecha de la decisión de moderación |
| `administradorDecisionId` | Long | — | — | Administrador que tomó la última decisión |
| `motivoModeracion` | String | — | — | Motivo de la decisión de moderación |

Objeto anidado `PrendaResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `tipo` | Enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipo de prenda |
| `marca` | String | — | — | Marca de la prenda |
| `color` | String | — | — | Color principal de la prenda |
| `estilo` | Enum | — | CASUAL · FORMAL · DEPORTIVO · URBANO | Estilo de la prenda |

```json
{
  "itemId": 1,
  "almacenNit": "900123456-1",
  "prenda": {
    "id": 1,
    "nombre": "Chaqueta de cuero negra",
    "descripcion": "Chaqueta de cuero con cierre frontal",
    "tipo": "SUPERIOR",
    "marca": "Urbano",
    "color": "negro",
    "estilo": "CASUAL"
  },
  "precio": 189900.0,
  "estadoModeracion": "NO_REQUERIDA",
  "fechaPublicacion": "2026-10-18T15:00:00Z",
  "fechaDecisionModeracion": null,
  "administradorDecisionId": 1,
  "motivoModeracion": "texto"
}
```


### `GET /api/v1/admin/almacenes/verificacion`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-62

Consultar almacenes para verificación.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `estado` | query | Enum | No | PENDIENTE · APROBADO · RECHAZADO |
| `page` | query | Integer | Sí | ≥ 0 |
| `size` | query | Integer | Sí | ≥ 1, ≤ 100 |

Response: `PageResponseDTOVerificacionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `VerificacionAlmacenResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `VerificacionAlmacenResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nit` | String | — | — | NIT del almacén |
| `nombreComercial` | String | — | — | Nombre comercial del almacén |
| `descripcion` | String | — | — | Descripción |
| `telefono` | String | — | — | Teléfono de contacto |
| `correoContacto` | String | — | — | Correo de contacto del almacén |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `reputacion` | Double | — | — | Reputación del almacén |
| `categorias` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVA · VINTAGE · ACCESORIOS | Categorías comerciales del almacén |
| `estadoVerificacion` | Enum | — | PENDIENTE · APROBADO · RECHAZADO | Estado de verificación del almacén |
| `legadoSinVerificacion` | Boolean | — | — | Almacén anterior al proceso de verificación |
| `fechaDecisionVerificacion` | Fecha-hora ISO-8601 | — | — | Fecha de la decisión de verificación |
| `administradorDecisionId` | Long | — | — | Administrador que tomó la última decisión |
| `motivoVerificacion` | String | — | — | Motivo de la decisión de verificación |

```json
{
  "content": [
    {
      "nit": "900123456-1",
      "nombreComercial": "Urbano Norte",
      "descripcion": "Chaqueta de cuero con cierre frontal",
      "telefono": "3001234567",
      "correoContacto": "contacto@urbanonorte.co",
      "latitud": 4.6683,
      "longitud": -74.0534,
      "reputacion": 4.6,
      "categorias": [
        "CASUAL"
      ],
      "estadoVerificacion": "PENDIENTE",
      "legadoSinVerificacion": false,
      "fechaDecisionVerificacion": null,
      "administradorDecisionId": 1,
      "motivoVerificacion": "texto"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `GET /api/v1/admin/almacenes/{nit}/verificacion`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-62

Revisar datos de un almacén.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |

Response: `VerificacionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nit` | String | — | — | NIT del almacén |
| `nombreComercial` | String | — | — | Nombre comercial del almacén |
| `descripcion` | String | — | — | Descripción |
| `telefono` | String | — | — | Teléfono de contacto |
| `correoContacto` | String | — | — | Correo de contacto del almacén |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `reputacion` | Double | — | — | Reputación del almacén |
| `categorias` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVA · VINTAGE · ACCESORIOS | Categorías comerciales del almacén |
| `estadoVerificacion` | Enum | — | PENDIENTE · APROBADO · RECHAZADO | Estado de verificación del almacén |
| `legadoSinVerificacion` | Boolean | — | — | Almacén anterior al proceso de verificación |
| `fechaDecisionVerificacion` | Fecha-hora ISO-8601 | — | — | Fecha de la decisión de verificación |
| `administradorDecisionId` | Long | — | — | Administrador que tomó la última decisión |
| `motivoVerificacion` | String | — | — | Motivo de la decisión de verificación |

```json
{
  "nit": "900123456-1",
  "nombreComercial": "Urbano Norte",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "telefono": "3001234567",
  "correoContacto": "contacto@urbanonorte.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "reputacion": 4.6,
  "categorias": [
    "CASUAL"
  ],
  "estadoVerificacion": "PENDIENTE",
  "legadoSinVerificacion": false,
  "fechaDecisionVerificacion": null,
  "administradorDecisionId": 1,
  "motivoVerificacion": "texto"
}
```


### `PATCH /api/v1/admin/almacenes/{nit}/verificacion`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-62

Aprobar o rechazar vinculación pendiente.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |

Request: `DecisionVerificacionRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `decision` | Enum | Sí | APROBADO · RECHAZADO | Decisión del administrador |
| `motivo` | String | No | máximo 1000 caracteres | Motivo de la decisión |

```json
{
  "decision": "APROBADO",
  "motivo": "Datos verificados"
}
```

Response: `VerificacionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nit` | String | — | — | NIT del almacén |
| `nombreComercial` | String | — | — | Nombre comercial del almacén |
| `descripcion` | String | — | — | Descripción |
| `telefono` | String | — | — | Teléfono de contacto |
| `correoContacto` | String | — | — | Correo de contacto del almacén |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `reputacion` | Double | — | — | Reputación del almacén |
| `categorias` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVA · VINTAGE · ACCESORIOS | Categorías comerciales del almacén |
| `estadoVerificacion` | Enum | — | PENDIENTE · APROBADO · RECHAZADO | Estado de verificación del almacén |
| `legadoSinVerificacion` | Boolean | — | — | Almacén anterior al proceso de verificación |
| `fechaDecisionVerificacion` | Fecha-hora ISO-8601 | — | — | Fecha de la decisión de verificación |
| `administradorDecisionId` | Long | — | — | Administrador que tomó la última decisión |
| `motivoVerificacion` | String | — | — | Motivo de la decisión de verificación |

```json
{
  "nit": "900123456-1",
  "nombreComercial": "Urbano Norte",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "telefono": "3001234567",
  "correoContacto": "contacto@urbanonorte.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "reputacion": 4.6,
  "categorias": [
    "CASUAL"
  ],
  "estadoVerificacion": "PENDIENTE",
  "legadoSinVerificacion": false,
  "fechaDecisionVerificacion": null,
  "administradorDecisionId": 1,
  "motivoVerificacion": "texto"
}
```


### `POST /api/v1/admin/almacenes/{nit}/verificacion/solicitar-revision`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-62

Iniciar o reabrir revisión.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |

Response: `VerificacionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nit` | String | — | — | NIT del almacén |
| `nombreComercial` | String | — | — | Nombre comercial del almacén |
| `descripcion` | String | — | — | Descripción |
| `telefono` | String | — | — | Teléfono de contacto |
| `correoContacto` | String | — | — | Correo de contacto del almacén |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `reputacion` | Double | — | — | Reputación del almacén |
| `categorias` | Lista de enum | — | CASUAL · FORMAL · DEPORTIVA · VINTAGE · ACCESORIOS | Categorías comerciales del almacén |
| `estadoVerificacion` | Enum | — | PENDIENTE · APROBADO · RECHAZADO | Estado de verificación del almacén |
| `legadoSinVerificacion` | Boolean | — | — | Almacén anterior al proceso de verificación |
| `fechaDecisionVerificacion` | Fecha-hora ISO-8601 | — | — | Fecha de la decisión de verificación |
| `administradorDecisionId` | Long | — | — | Administrador que tomó la última decisión |
| `motivoVerificacion` | String | — | — | Motivo de la decisión de verificación |

```json
{
  "nit": "900123456-1",
  "nombreComercial": "Urbano Norte",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "telefono": "3001234567",
  "correoContacto": "contacto@urbanonorte.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "reputacion": 4.6,
  "categorias": [
    "CASUAL"
  ],
  "estadoVerificacion": "PENDIENTE",
  "legadoSinVerificacion": false,
  "fechaDecisionVerificacion": null,
  "administradorDecisionId": 1,
  "motivoVerificacion": "texto"
}
```


### `POST /api/v1/admin/fundaciones`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 201 Created · **RF:** RF-63

Registrar fundación aliada (RF-63).

Request: `FundacionRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nombre` | String | Sí | máximo 120 caracteres | Nombre |
| `descripcion` | String | No | máximo 500 caracteres | Descripción |
| `direccion` | String | Sí | máximo 200 caracteres | Dirección física |
| `telefono` | String | No | máximo 30 caracteres | Teléfono de contacto |
| `email` | String (email) | Sí | no vacío, formato email | Correo electrónico |
| `latitud` | Double | Sí | ≥ -90.0, ≤ 90.0 | Latitud geográfica |
| `longitud` | Double | Sí | ≥ -180.0, ≤ 180.0 | Longitud geográfica |
| `categoriasAceptadas` | Lista de enum | Sí | mínimo 1 elementos, sin repetidos, SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipos de prenda que recibe la fundación |
| `representanteId` | Long | Sí | — | Usuario representante de la fundación |

```json
{
  "nombre": "Fundación Abrigo Bogotá",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "direccion": "Calle 85 # 12-30, Bogotá",
  "telefono": "3001234567",
  "email": "comprador@styleradar.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "categoriasAceptadas": [
    "SUPERIOR"
  ],
  "representanteId": 1
}
```

Response: `FundacionResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `direccion` | String | — | — | Dirección física |
| `telefono` | String | — | — | Teléfono de contacto |
| `email` | String | — | — | Correo electrónico |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `categoriasAceptadas` | Lista de enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipos de prenda que recibe la fundación |
| `representanteId` | Long | — | — | Usuario representante de la fundación |
| `activa` | Boolean | — | — | Indica si el registro está activo |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `distanciaKm` | Double | — | — | Distancia estimada en kilómetros |

```json
{
  "id": 1,
  "nombre": "Fundación Abrigo Bogotá",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "direccion": "Calle 85 # 12-30, Bogotá",
  "telefono": "3001234567",
  "email": "comprador@styleradar.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "categoriasAceptadas": [
    "SUPERIOR"
  ],
  "representanteId": 1,
  "activa": true,
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "distanciaKm": 2.4
}
```


### `GET /api/v1/admin/fundaciones`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-63

Listar todas las fundaciones.

Response: lista de `FundacionResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `direccion` | String | — | — | Dirección física |
| `telefono` | String | — | — | Teléfono de contacto |
| `email` | String | — | — | Correo electrónico |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `categoriasAceptadas` | Lista de enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipos de prenda que recibe la fundación |
| `representanteId` | Long | — | — | Usuario representante de la fundación |
| `activa` | Boolean | — | — | Indica si el registro está activo |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `distanciaKm` | Double | — | — | Distancia estimada en kilómetros |

```json
[
  {
    "id": 1,
    "nombre": "Fundación Abrigo Bogotá",
    "descripcion": "Chaqueta de cuero con cierre frontal",
    "direccion": "Calle 85 # 12-30, Bogotá",
    "telefono": "3001234567",
    "email": "comprador@styleradar.co",
    "latitud": 4.6683,
    "longitud": -74.0534,
    "categoriasAceptadas": [
      "SUPERIOR"
    ],
    "representanteId": 1,
    "activa": true,
    "fechaRegistro": "2026-10-18T15:00:00Z",
    "distanciaKm": 2.4
  }
]
```


### `PUT /api/v1/admin/fundaciones/{id}`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-64

Editar fundación aliada (RF-64).

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `id` | ruta | Long | Sí | — |

Request: `FundacionRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `nombre` | String | Sí | máximo 120 caracteres | Nombre |
| `descripcion` | String | No | máximo 500 caracteres | Descripción |
| `direccion` | String | Sí | máximo 200 caracteres | Dirección física |
| `telefono` | String | No | máximo 30 caracteres | Teléfono de contacto |
| `email` | String (email) | Sí | no vacío, formato email | Correo electrónico |
| `latitud` | Double | Sí | ≥ -90.0, ≤ 90.0 | Latitud geográfica |
| `longitud` | Double | Sí | ≥ -180.0, ≤ 180.0 | Longitud geográfica |
| `categoriasAceptadas` | Lista de enum | Sí | mínimo 1 elementos, sin repetidos, SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipos de prenda que recibe la fundación |
| `representanteId` | Long | Sí | — | Usuario representante de la fundación |

```json
{
  "nombre": "Fundación Abrigo Bogotá",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "direccion": "Calle 85 # 12-30, Bogotá",
  "telefono": "3001234567",
  "email": "comprador@styleradar.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "categoriasAceptadas": [
    "SUPERIOR"
  ],
  "representanteId": 1
}
```

Response: `FundacionResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `direccion` | String | — | — | Dirección física |
| `telefono` | String | — | — | Teléfono de contacto |
| `email` | String | — | — | Correo electrónico |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `categoriasAceptadas` | Lista de enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipos de prenda que recibe la fundación |
| `representanteId` | Long | — | — | Usuario representante de la fundación |
| `activa` | Boolean | — | — | Indica si el registro está activo |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `distanciaKm` | Double | — | — | Distancia estimada en kilómetros |

```json
{
  "id": 1,
  "nombre": "Fundación Abrigo Bogotá",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "direccion": "Calle 85 # 12-30, Bogotá",
  "telefono": "3001234567",
  "email": "comprador@styleradar.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "categoriasAceptadas": [
    "SUPERIOR"
  ],
  "representanteId": 1,
  "activa": true,
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "distanciaKm": 2.4
}
```


### `PATCH /api/v1/admin/fundaciones/{id}/desactivar`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-65

Desactivar fundación aliada (RF-65).

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `id` | ruta | Long | Sí | — |

Response: `FundacionResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `descripcion` | String | — | — | Descripción |
| `direccion` | String | — | — | Dirección física |
| `telefono` | String | — | — | Teléfono de contacto |
| `email` | String | — | — | Correo electrónico |
| `latitud` | Double | — | — | Latitud geográfica |
| `longitud` | Double | — | — | Longitud geográfica |
| `categoriasAceptadas` | Lista de enum | — | SUPERIOR · INFERIOR · CALZADO · VESTIDO · ACCESORIO | Tipos de prenda que recibe la fundación |
| `representanteId` | Long | — | — | Usuario representante de la fundación |
| `activa` | Boolean | — | — | Indica si el registro está activo |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `distanciaKm` | Double | — | — | Distancia estimada en kilómetros |

```json
{
  "id": 1,
  "nombre": "Fundación Abrigo Bogotá",
  "descripcion": "Chaqueta de cuero con cierre frontal",
  "direccion": "Calle 85 # 12-30, Bogotá",
  "telefono": "3001234567",
  "email": "comprador@styleradar.co",
  "latitud": 4.6683,
  "longitud": -74.0534,
  "categoriasAceptadas": [
    "SUPERIOR"
  ],
  "representanteId": 1,
  "activa": true,
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "distanciaKm": 2.4
}
```


### `GET /api/v1/admin/usuarios`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-66

Consultar cuentas.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `estado` | query | Enum | No | ACTIVA · INACTIVA · BLOQUEADA |
| `page` | query | Integer | Sí | ≥ 0 |
| `size` | query | Integer | Sí | ≥ 1, ≤ 100 |

Response: `PageResponseDTOCuentaAdministrativaResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `CuentaAdministrativaResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `CuentaAdministrativaResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `roles` | Lista de enum | — | COMPRADOR · ADMIN_ALMACEN · REPRESENTANTE_FUNDACION · ADMIN_STYLERADAR | Roles del usuario |
| `estadoCuenta` | Enum | — | ACTIVA · INACTIVA · BLOQUEADA | Estado de la cuenta |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `fechaCambioEstado` | Fecha-hora ISO-8601 | — | — | Fecha del último cambio de estado |
| `administradorCambioEstadoId` | Long | — | — | Administrador que hizo el último cambio de estado |
| `motivoCambioEstado` | String | — | — | Motivo del último cambio de estado |

```json
{
  "content": [
    {
      "id": 1,
      "nombre": "Laura Gómez",
      "email": "comprador@styleradar.co",
      "roles": [
        "COMPRADOR"
      ],
      "estadoCuenta": "ACTIVA",
      "fechaRegistro": "2026-10-18T15:00:00Z",
      "fechaCambioEstado": null,
      "administradorCambioEstadoId": 1,
      "motivoCambioEstado": "texto"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `GET /api/v1/admin/usuarios/{id}`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-66

Consultar cuenta y última decisión.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `id` | ruta | Long | Sí | — |

Response: `CuentaAdministrativaResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `roles` | Lista de enum | — | COMPRADOR · ADMIN_ALMACEN · REPRESENTANTE_FUNDACION · ADMIN_STYLERADAR | Roles del usuario |
| `estadoCuenta` | Enum | — | ACTIVA · INACTIVA · BLOQUEADA | Estado de la cuenta |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `fechaCambioEstado` | Fecha-hora ISO-8601 | — | — | Fecha del último cambio de estado |
| `administradorCambioEstadoId` | Long | — | — | Administrador que hizo el último cambio de estado |
| `motivoCambioEstado` | String | — | — | Motivo del último cambio de estado |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "roles": [
    "COMPRADOR"
  ],
  "estadoCuenta": "ACTIVA",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "fechaCambioEstado": null,
  "administradorCambioEstadoId": 1,
  "motivoCambioEstado": "texto"
}
```


### `PATCH /api/v1/admin/usuarios/{id}/estado`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-66

Activar, inactivar o bloquear una cuenta.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `id` | ruta | Long | Sí | — |

Request: `CambioEstadoCuentaRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `estado` | Enum | Sí | ACTIVA · INACTIVA · BLOQUEADA | Estado del recurso |
| `motivo` | String | No | máximo 1000 caracteres | Motivo de la decisión |

```json
{
  "estado": "ACTIVA",
  "motivo": "Datos verificados"
}
```

Response: `CuentaAdministrativaResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `email` | String | — | — | Correo electrónico |
| `roles` | Lista de enum | — | COMPRADOR · ADMIN_ALMACEN · REPRESENTANTE_FUNDACION · ADMIN_STYLERADAR | Roles del usuario |
| `estadoCuenta` | Enum | — | ACTIVA · INACTIVA · BLOQUEADA | Estado de la cuenta |
| `fechaRegistro` | Fecha-hora ISO-8601 | — | — | Fecha de registro |
| `fechaCambioEstado` | Fecha-hora ISO-8601 | — | — | Fecha del último cambio de estado |
| `administradorCambioEstadoId` | Long | — | — | Administrador que hizo el último cambio de estado |
| `motivoCambioEstado` | String | — | — | Motivo del último cambio de estado |

```json
{
  "id": 1,
  "nombre": "Laura Gómez",
  "email": "comprador@styleradar.co",
  "roles": [
    "COMPRADOR"
  ],
  "estadoCuenta": "ACTIVA",
  "fechaRegistro": "2026-10-18T15:00:00Z",
  "fechaCambioEstado": null,
  "administradorCambioEstadoId": 1,
  "motivoCambioEstado": "texto"
}
```


## Suscripciones de almacenes

### `GET /api/v1/planes-suscripcion`

**Rol:** Público (sin token) · **Respuesta exitosa:** 200 OK · **RF:** RF-67

Consultar planes configurados y habilitados.

Response: lista de `PlanSuscripcionResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `nombre` | String | — | — | Nombre |
| `periodo` | Integer | — | — | Duración del plan |
| `unidadDuracion` | Enum | — | MESES_CALENDARIO | Unidad de la duración del plan |

```json
[
  {
    "id": 1,
    "nombre": "Plan mensual",
    "periodo": 1,
    "unidadDuracion": "MESES_CALENDARIO"
  }
]
```


### `PUT /api/v1/admin/almacenes/{nit}/administradores/{usuarioId}`

**Rol:** ADMIN_STYLERADAR · **Respuesta exitosa:** 200 OK · **RF:** RF-67

Asociar administrador autorizado a un NIT existente.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `usuarioId` | ruta | Long | Sí | — |

Response: `AdministradorAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `usuarioId` | Long | — | — | Identificador del usuario |
| `almacenNit` | String | — | — | NIT del almacén |
| `asignadoPorId` | Long | — | — | Administrador que hizo la asociación |
| `fechaAsignacion` | Fecha-hora ISO-8601 | — | — | Fecha de la asociación |

```json
{
  "id": 1,
  "usuarioId": 1,
  "almacenNit": "900123456-1",
  "asignadoPorId": 1,
  "fechaAsignacion": "2026-10-18T15:00:00Z"
}
```


### `POST /api/v1/almacenes/{nit}/suscripciones`

**Rol:** ADMIN_ALMACEN · **Respuesta exitosa:** 201 Created · **RF:** RF-67

Registrar suscripción a un plan configurado.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |

Request: `CrearSuscripcionRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `planId` | Long | Sí | > 0 | Identificador del plan de suscripción |

```json
{
  "planId": 1
}
```

Response: `SuscripcionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `almacenNit` | String | — | — | NIT del almacén |
| `planId` | Long | — | — | Identificador del plan de suscripción |
| `nombrePlan` | String | — | — | Nombre del plan |
| `estadoSuscripcion` | Enum | — | ACTIVA · FINALIZADA | Estado de la suscripción |
| `fechaCreacion` | Fecha-hora ISO-8601 | — | — | Fecha de creación |
| `inicioPeriodo` | Fecha-hora ISO-8601 | — | — | Inicio del periodo |
| `finPeriodo` | Fecha-hora ISO-8601 | — | — | Fin del periodo pagado |
| `renovacionHabilitada` | Boolean | — | — | Indica si el plan se renueva |
| `fechaCancelacionRenovacion` | Fecha-hora ISO-8601 | — | — | Fecha en que se canceló la renovación |

```json
{
  "id": 1,
  "almacenNit": "900123456-1",
  "planId": 1,
  "nombrePlan": "Plan mensual",
  "estadoSuscripcion": "ACTIVA",
  "fechaCreacion": "2026-10-18T15:00:00Z",
  "inicioPeriodo": "2026-10-18T15:00:00Z",
  "finPeriodo": "2026-11-18T15:00:00Z",
  "renovacionHabilitada": true,
  "fechaCancelacionRenovacion": null
}
```


### `GET /api/v1/almacenes/{nit}/suscripciones/vigente`

**Rol:** ADMIN_ALMACEN · **Respuesta exitosa:** 200 OK · **RF:** RF-67

Consultar suscripción vigente propia.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |

Response: `SuscripcionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `almacenNit` | String | — | — | NIT del almacén |
| `planId` | Long | — | — | Identificador del plan de suscripción |
| `nombrePlan` | String | — | — | Nombre del plan |
| `estadoSuscripcion` | Enum | — | ACTIVA · FINALIZADA | Estado de la suscripción |
| `fechaCreacion` | Fecha-hora ISO-8601 | — | — | Fecha de creación |
| `inicioPeriodo` | Fecha-hora ISO-8601 | — | — | Inicio del periodo |
| `finPeriodo` | Fecha-hora ISO-8601 | — | — | Fin del periodo pagado |
| `renovacionHabilitada` | Boolean | — | — | Indica si el plan se renueva |
| `fechaCancelacionRenovacion` | Fecha-hora ISO-8601 | — | — | Fecha en que se canceló la renovación |

```json
{
  "id": 1,
  "almacenNit": "900123456-1",
  "planId": 1,
  "nombrePlan": "Plan mensual",
  "estadoSuscripcion": "ACTIVA",
  "fechaCreacion": "2026-10-18T15:00:00Z",
  "inicioPeriodo": "2026-10-18T15:00:00Z",
  "finPeriodo": "2026-11-18T15:00:00Z",
  "renovacionHabilitada": true,
  "fechaCancelacionRenovacion": null
}
```


### `GET /api/v1/almacenes/{nit}/suscripciones`

**Rol:** ADMIN_ALMACEN · **Respuesta exitosa:** 200 OK · **RF:** RF-67

Consultar historial propio.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `page` | query | Integer | No | ≥ 0 |
| `size` | query | Integer | No | ≥ 1, ≤ 100 |

Response: `PageResponseDTOSuscripcionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `content` | Lista de Objeto `SuscripcionAlmacenResponseDTO` | — | — | Elementos de la página |
| `page` | Integer | — | — | Número de página, desde 0 |
| `size` | Integer | — | — | Tamaño de la página |
| `totalElements` | Long | — | — | Total de elementos |
| `totalPages` | Integer | — | — | Total de páginas |
| `first` | Boolean | — | — | Es la primera página |
| `last` | Boolean | — | — | Es la última página |

Objeto anidado `SuscripcionAlmacenResponseDTO`:

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `almacenNit` | String | — | — | NIT del almacén |
| `planId` | Long | — | — | Identificador del plan de suscripción |
| `nombrePlan` | String | — | — | Nombre del plan |
| `estadoSuscripcion` | Enum | — | ACTIVA · FINALIZADA | Estado de la suscripción |
| `fechaCreacion` | Fecha-hora ISO-8601 | — | — | Fecha de creación |
| `inicioPeriodo` | Fecha-hora ISO-8601 | — | — | Inicio del periodo |
| `finPeriodo` | Fecha-hora ISO-8601 | — | — | Fin del periodo pagado |
| `renovacionHabilitada` | Boolean | — | — | Indica si el plan se renueva |
| `fechaCancelacionRenovacion` | Fecha-hora ISO-8601 | — | — | Fecha en que se canceló la renovación |

```json
{
  "content": [
    {
      "id": 1,
      "almacenNit": "900123456-1",
      "planId": 1,
      "nombrePlan": "Plan mensual",
      "estadoSuscripcion": "ACTIVA",
      "fechaCreacion": "2026-10-18T15:00:00Z",
      "inicioPeriodo": "2026-10-18T15:00:00Z",
      "finPeriodo": "2026-11-18T15:00:00Z",
      "renovacionHabilitada": true,
      "fechaCancelacionRenovacion": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```


### `PATCH /api/v1/almacenes/{nit}/suscripciones/{id}/renovacion`

**Rol:** ADMIN_ALMACEN · **Respuesta exitosa:** 200 OK · **RF:** RF-68

Cancelar la próxima renovación.

Parámetros:

| Nombre | Ubicación | Tipo | Obligatorio | Restricciones |
|---|---|---|---|---|
| `nit` | ruta | String | Sí | — |
| `id` | ruta | Long | Sí | — |

Request: `CancelarRenovacionRequestDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `renovacionHabilitada` | Boolean | Sí | — | Indica si el plan se renueva |

```json
{
  "renovacionHabilitada": true
}
```

Response: `SuscripcionAlmacenResponseDTO`.

| Campo | Tipo | Obligatorio | Restricciones | Descripción |
|---|---|---|---|---|
| `id` | Long | — | — | Identificador |
| `almacenNit` | String | — | — | NIT del almacén |
| `planId` | Long | — | — | Identificador del plan de suscripción |
| `nombrePlan` | String | — | — | Nombre del plan |
| `estadoSuscripcion` | Enum | — | ACTIVA · FINALIZADA | Estado de la suscripción |
| `fechaCreacion` | Fecha-hora ISO-8601 | — | — | Fecha de creación |
| `inicioPeriodo` | Fecha-hora ISO-8601 | — | — | Inicio del periodo |
| `finPeriodo` | Fecha-hora ISO-8601 | — | — | Fin del periodo pagado |
| `renovacionHabilitada` | Boolean | — | — | Indica si el plan se renueva |
| `fechaCancelacionRenovacion` | Fecha-hora ISO-8601 | — | — | Fecha en que se canceló la renovación |

```json
{
  "id": 1,
  "almacenNit": "900123456-1",
  "planId": 1,
  "nombrePlan": "Plan mensual",
  "estadoSuscripcion": "ACTIVA",
  "fechaCreacion": "2026-10-18T15:00:00Z",
  "inicioPeriodo": "2026-10-18T15:00:00Z",
  "finPeriodo": "2026-11-18T15:00:00Z",
  "renovacionHabilitada": true,
  "fechaCancelacionRenovacion": null
}
```

