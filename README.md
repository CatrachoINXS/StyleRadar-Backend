# **StyleRadar**
StyleRadar es una plataforma web que conecta compradores con almacenes de moda locales. Los almacenes publican su inventario (fotos, tallas y disponibilidad) y el usuario puede buscar prendas, consultar tiendas cercanas y probarlas virtualmente con IA. También permite publicar ropa usada para vender o donar a una fundación aliada.
StyleRadar no es un marketplace: es el puente entre la intención de compra digital y la tienda física.

### Cómo levantar el proyecto localmente

1. Descarga o clona el repositorio con el comando `git clone https://github.com/CatrachoINXS/StyleRadar-Backend.git` 
2. Dirigete al directorio donde clonaste el repositorio e ingresa a la carpeta del proyecto `(.../StyleRadar-Backend/style-radar)` y en la terminal ejecuta `mvn clean compile` para compilar el proyecto.
3. Ingresa el comando `mvn spring-boot:run` para ejecutar el proyecto.
4. Una vez iniciado, estará disponible en el puerto configurado. 

## Diagrama de Contexto
En el siguiente diagrama de contexto se presenta la arquitectura general y los actores que interactúan con StyleRadar.

![](style-radar/docs/uml/diagrama-contexto-style-radar.jpeg)

### Actores
Como actores tenemos a los *usuarios compradores, a los almacenes de moda, a las fundaciones aliadas y al administrador*.

Cada uno de los susodichos interactúa de manera diferente en la plataforma, tal y como se menciona en el diagrama.

### Sistemas externos
Como sistemas externos están *Mapbox* para que los usuarios compradores puedan saber a donde tienen que ir para comprar su prenda. *PSE* como pasarela de pagos para las suscripciones y la *API del Probador Virtual* que se conecta a un servicio de IA para generar una imagen del usuario comprador con determinada prenda.


## Diagrama de Componentes General
El siguiente diagrama de componentes muestra la arquitectura general del sistema, dividida en tres partes principales.

![](style-radar/docs/uml/DiagramaComponentesGeneral.png)

La primer parte es el ***Front-End*** que representa la interfaz de usuario con la que interactuan todos los actores. La segunda parte es el ***Back-End***, encargado de toda la lógica de negocio y de proveer la interfaz para que sea utilizada por el front. La tercera parte es la ***Base de Datos***, componente encargado de persistir la información de la plataforma y de proveer la interfaz para el back.

## Diagrama de Componentes Especifico
Este diagrama de componentes específico muestra la arquitectura interna del Back-End de StyleRadar. En él tienen cinco módulos explicados a continuacion.

![](style-radar/docs/uml/DiagramaComponentesEspecifico.png)

### Módulos
Cada módulo se encarga de una parte específica de la plataforma. 
- **Usuarios:** Gestiona y valida la información de los usuarios. 
- **Catálogo:** Para administrar el inventario y prendas que suben los almacenes a la plataforma.
- **Donaciones:** Maneja todo lo relacionado con las donaciones de prendas para las fundaciones aliadas.
- **Probador virtual:** Maneja la lógica con la herramienta IA para que los usuarios compradores puedan probarse las prendas virtualmente.
- **Suscripciones:** Se encarga de los planes de los usuarios compradores y los almacenes administrando los pagos.
  
### Estructura de los módulos
En cáda módulo los componentes interactuan de la siguiente manera: Un actor envía una solicitud y esta llega al `Controller`. Este transfiere los datos a un `MapperIn` para transformarlos en objetos `DTO` y enviarlos al `Validator`, el cual comprueba que se cumplan todas las reglas de negocio. 

Después de la verificacion se pasa la informacion al `Service`, quien contiene toda la lógica de negocio e interactua con los objetos de dominio. Despues de ello, se pasa al `MapperOut` para transformar la informacion y pasarla al `Repository`, el cual se encarga de la comunicación con la base de datos.

Al final, todos los repositorios de cada módulo llegan a la base de datos, para almacenar y recuperar la informacion cuando se requiera.

## Diagrama de Clases
En el siguiente diagrama de clases se pueden observar los objetos del dominio, se muestran las entidades principales, sus atributos y las interacciones que conectan el sistema.

![](style-radar/docs/uml/DiagramaClasesStyleRadar.drawio.png)

### Estructura general del diagrama de clases
En el diagrama se observa en color azul todo lo relacionado con la gestión de usuarios, perfiles y catálogos de almacén. Se encuentra la clase abstracta `Usuario`, de la cual heredan los diferentes tipos de actores: el `UsuarioComprador`, el `Almacen`, el `Administrador` y la `FundacionAliada`. Además, incluye clases auxiliares como `Direccion`, `Mensaje` y una enumeración con los estados del usuario.

En color amarillo se agrupa lo referente a las prendas, con la clase `Prenda` y sus enumeraciones tipo de prenda, estilo y estado. También se agrupa aquí lo relacionado con las publicaciones, mediante `PublicacionPrenda` y `PublicacionSegundaMano`.

En color verde se observa una de las ideas diferenciadoras del proyecto: las Playlists de estilo, las cuales pueden contener múltiples publicaciones de prendas.

En color morado se aprecia otro de los aspectos clave y diferenciadores de StyleRadar: el probador virtual, herramienta con la cual los usuarios compradores podrán probarse las prendas del catálogo.

## Justificación Patrones de Diseño

### Patrón Strategy
El patrón Strategy en el contexto de StyleRadar resuelve el problema de tener algoritmos de sugerencia para el feed que deben ser intercambiables según las preferencias del usuario. Sin este patrón, la clase encargada de administrar dichos algoritmos estaría acoplada a cada uno de ellos, lo que dificultaría su extensión y dejaría el código abierto a modificaciones que podrían romper su funcionamiento.

![](style-radar/docs/images/patron_strategy_ilustracion.png) 

### Patrón Observer

El patrón Observer resuelve el problema de notificar automáticamente a los usuarios suscritos a la publicacion de una prenda cuando un almacen aliado sube una unidad.

![](style-radar/docs/images/patron_observer_ilustracion.png)

El lugar donde vive el patrón Observer es en el dominio, específicamente en la clase `PublicaciónPrenda` quien es el sujeto y la clase `UsuarioComprador` quien es el observador notificado cuando se sube una prenda a la publicación.

Así se ve el patrón en el diagrama de clases:

![](style-radar/docs/images/DC_Observer.png)

### Patrón Adapter

El patrón adapter nos resuelve dos problemas. El primero de ellos es la compatibilidad con diferentes pasarelas de pago; usando este patrón podemos usar una interfaz común de pagos en nuestro sistema para adaptar las pasarelas de pago externas.

![](style-radar/docs/images/patron_adapter1_ilustracion.png)

El segundo problema que resuelve es la compatibilidad con la Inteligencia Artificial utilizada para el probador virtual de prendas. Usando Adapter podemos integrar la interfaz de cualquier IA con nuestro sistema sin necesidad de acoplarla.

![](style-radar/docs/images/patron_adapter2_ilustracion.png)

El lugar donde vive el patron adapter es el dominio, específicamente en las clases `ProbadorVirtual` y `AdaptadorGemini`.

![](style-radar/docs/images/DC_Adapter2.png)


### Patrón Composite

El patrón composite nos resuelve el problema de la combinacion de filtros en las búsquedas inteligentes. Con este patrón podemos crear filtros simples como `FiltroTalla` o `FiltroColor`, y filtros complejos como `FiltroCompuestoAnd` (que combina dos filtros) y tratarlos de la misma forma. De esta forma, evaluar si una prenda cumple con un filtro de búsqueda complejo, se vuelve más sencillo. 

![](style-radar/docs/images/patron_composite_ilustracion.png)

### Patrón Iterator

El patron iterator por otro lado nos resuelve el problema de recorrer la colección de prendas durante la búsqueda inteligente usando filtros, pero sin exponer el contenido de las prendas. Usando este patron podemos iterar sobre los elementos y recolectar solo aquellas que cumplan con determinado filtro.

![](style-radar/docs/images/patron_iterator_ilustracion.png)
