# StyleRadar

StyleRadar es una plataforma web que conecta compradores con almacenes de moda locales. Los almacenes publican su inventario —fotografías, tallas y disponibilidad— y los usuarios pueden buscar prendas y consultar tiendas cercanas. El producto general también contempla experiencias como el probador virtual, la venta de ropa usada y las donaciones.

StyleRadar no es un marketplace: es el puente entre la intención de compra digital y la tienda física.

## Backend Sprint 2: catálogo y búsqueda

### Requisitos

- Java 21.
- Maven.
- PostgreSQL.

### Configuración local

La aplicación obtiene la conexión a PostgreSQL exclusivamente de estas variables de entorno:

- `DB_URL`: URL JDBC, por ejemplo `jdbc:postgresql://localhost:5432/styleradar`.
- `DB_USERNAME`: usuario de la base de datos.
- `DB_PASSWORD`: contraseña de la base de datos.

No se deben versionar credenciales reales. Desde la carpeta `style-radar/` se pueden ejecutar:

```shell
mvn clean test
mvn clean verify
mvn spring-boot:run
```

`mvn clean verify` ejecuta las pruebas y la regla de cobertura JaCoCo configurada en el proyecto.

### Swagger/OpenAPI

Con la aplicación en ejecución, la interfaz y el documento OpenAPI están disponibles en:

- `/swagger-ui.html`
- `/v3/api-docs`

Las interfaces del paquete `controller/docs` describen únicamente los endpoints implementados de prendas, catálogo, inventario, imágenes y búsqueda.

### Arquitectura implementada

El módulo mantiene una separación por responsabilidades:

1. Los `Controller` atienden HTTP y delegan la conversión de contratos a los mappers de presentación.
2. El modelo de `Domain` representa los datos usados por los casos de uso.
3. Los `Service` coordinan reglas, validadores y operaciones del módulo.
4. Los `Validator` y calculadores contienen reglas específicas de inventario, estados, distancia y similitud.
5. Los mappers de persistencia convierten entre dominio y entidades JPA.
6. Los `Repository` acceden a PostgreSQL mediante Spring Data JPA.
7. Las `Entity` representan tablas, restricciones y relaciones de persistencia.

El flujo habitual es `Controller -> Mapper -> Domain -> Service -> Validator -> Persistence Mapper -> Repository -> Entity -> PostgreSQL`; los casos simples omiten capas que no aportan transformación o reglas.

### Alcance implementado por este backend

- Publicación, consulta, edición y retiro de ítems del catálogo de un almacén.
- Inventario por talla y cálculo derivado del stock disponible.
- Registro de múltiples imágenes y respuesta basada en metadatos.
- Cálculo de estados: agotada, últimas unidades, nueva prenda y disponible.
- Búsqueda global con texto libre, filtros combinados y paginación.
- Orden por distancia y reputación del almacén.
- Fallback de prendas similares cuando una búsqueda textual no tiene coincidencias directas.

### Pruebas y calidad

La suite utiliza JUnit, Mockito y H2 para pruebas unitarias, web y de persistencia. JaCoCo mide cobertura de líneas y ramas durante `mvn clean verify`; el build exige una cobertura de líneas global mínima del 85 %. El perfil `test` permite validar el contexto Spring sin depender de una instancia PostgreSQL externa.

## Diagramas académicos

### Diagrama de contexto

![](style-radar/docs/uml/diagrama-contexto-style-radar.jpeg)

### Diagrama de componentes general

![](style-radar/docs/uml/DiagramaComponentesGeneral.png)

### Diagrama de componentes específico

![](style-radar/docs/uml/DiagramaComponentesEspecifico.png)

### Diagrama de clases

El diagrama existente es un artefacto académico que debe actualizarse manualmente para reflejar el código vigente del Sprint 2.

![](style-radar/docs/uml/DiagramaClasesStyleRadar.drawio.png)

## Estado de los patrones de diseño

Las siguientes ilustraciones conservan las propuestas de diseño del producto. Su presencia no implica que exista una implementación en el alcance actual del backend.

### Strategy — diseño propuesto, no implementado

La propuesta plantea algoritmos intercambiables para sugerencias del feed. El módulo actual no implementa un feed, una interfaz Strategy ni estrategias concretas.

![](style-radar/docs/images/patron_strategy_ilustracion.png)

### Observer — diseño propuesto, no implementado

La propuesta contempla notificaciones a compradores cuando se publica una prenda. En el código actual no existen el sujeto, los observadores ni las clases `PublicaciónPrenda` y `UsuarioComprador` descritas originalmente.

![](style-radar/docs/images/patron_observer_ilustracion.png)

![](style-radar/docs/images/DC_Observer.png)

### Adapter — diseño propuesto, no implementado

La propuesta considera adaptar pasarelas de pago y proveedores de IA. Ninguna de esas integraciones pertenece al módulo actual y no existen implementaciones como `ProbadorVirtual` o `AdaptadorGemini`.

![](style-radar/docs/images/patron_adapter1_ilustracion.png)

![](style-radar/docs/images/patron_adapter2_ilustracion.png)

![](style-radar/docs/images/DC_Adapter2.png)

### Composite — composición del framework, no patrón propio implementado

La búsqueda combina filtros con `Specification<ItemCatalogoEntity>` de Spring Data JPA en `ItemCatalogoSpecifications`. Esa composición permite unir predicados con AND y OR, pero el proyecto no define una jerarquía Composite propia como `FiltroTalla`, `FiltroColor` o `FiltroCompuestoAnd`. La ilustración se conserva como diseño conceptual.

![](style-radar/docs/images/patron_composite_ilustracion.png)

### Iterator — diseño propuesto, no implementado

El código utiliza colecciones, streams y mecanismos de iteración provistos por Java y Spring Data. No existe un iterador personalizado del dominio; la ilustración representa una posibilidad de diseño, no el estado del código.

![](style-radar/docs/images/patron_iterator_ilustracion.png)

## Trazabilidad funcional del Sprint 2

Durante este Sprint se implementó el módulo de catálogo, inventario y búsqueda de StyleRadar, cubriendo los siguientes requerimientos funcionales:

| Requerimiento | Funcionalidad |
|---|---|
| RF-07 | Búsqueda de prendas mediante texto libre |
| RF-08 | Filtro de prendas por tipo |
| RF-09 | Filtro de prendas por color |
| RF-10 | Filtro de prendas por talla disponible |
| RF-11 | Filtro de prendas por rango de precio |
| RF-12 | Filtro de prendas por marca |
| RF-13 | Filtro de prendas por estilo |
| RF-14 | Ordenamiento de resultados por distancia |
| RF-15 | Ordenamiento por reputación del almacén |
| RF-16 | Consulta de prendas similares cuando no existen coincidencias directas |
| RF-22 | Consulta del catálogo de un almacén |
| RF-50 | Publicación de prendas en el catálogo |
| RF-51 | Registro de fotografías de prendas |
| RF-52 | Registro de tallas disponibles |
| RF-53 | Registro del precio de una prenda |
| RF-54 | Registro de atributos utilizados para búsqueda |
| RF-55 | Edición de prendas del catálogo |
| RF-56 | Actualización de disponibilidad por talla |
| RF-57 | Ocultamiento de prendas agotadas en resultados públicos |
| RF-58 | Retiro de prendas del catálogo |
| RF-59 | Identificación automática de últimas unidades |
| RF-60 | Identificación automática de nuevas llegadas |

### Endpoints principales

El módulo expone, entre otros, los siguientes endpoints:

```text
GET    /api/v1/prendas

GET    /api/v1/almacenes/{nit}/catalogo
POST   /api/v1/almacenes/{nit}/catalogo
PUT    /api/v1/almacenes/{nit}/catalogo/{itemId}
DELETE /api/v1/almacenes/{nit}/catalogo/{itemId}

PUT    /api/v1/almacenes/{nit}/catalogo/{itemId}/tallas
PATCH  /api/v1/almacenes/{nit}/catalogo/{itemId}/inventario/{talla}

POST   /api/v1/almacenes/{nit}/catalogo/{itemId}/imagenes

GET    /api/v1/catalogo/buscar