## Purpose

Permitir crear y consultar proyectos de análisis persistentes como contenedores iniciales del producto, sin configurar targets ni ejecutar análisis en este cambio.

## ADDED Requirements

### Requirement: PM-01 Información básica del proyecto

**Historia:** Como usuario de la plataforma, quiero identificar y describir un proyecto para distinguir el trabajo que organizaré en él.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** Ninguna capacidad previa; PM-02 define la validez de sus datos.

El sistema SHALL representar cada proyecto mediante exactamente `id`, `name`, `description`, `createdAt` y `updatedAt`. `id` SHALL ser un UUID generado por el sistema e inmutable. `name` SHALL ser obligatorio y `description` SHALL ser texto opcional representado como `null` cuando no tenga contenido. Las fechas SHALL ser generadas por el sistema y representadas como instantes ISO 8601 en UTC, con sufijo `Z`. Al crear, `updatedAt` SHALL ser igual a `createdAt`. Las consultas SHALL conservar ambos instantes sin actualizarlos. Este cambio no define operaciones de edición.

#### Scenario: Representación de un proyecto recién creado
- **WHEN** se crea un proyecto válido
- **THEN** la representación contiene los cinco campos definidos, un UUID generado y ambas fechas con el mismo instante UTC
- **AND** no contiene datos de targets, scans ni otras capacidades fuera del cambio

#### Scenario: Consultar no modifica las fechas
- **WHEN** se consulta individualmente o se lista un proyecto ya creado
- **THEN** `createdAt` y `updatedAt` mantienen los valores persistidos durante su creación

### Requirement: PM-02 Validación de los datos de creación

**Historia:** Como usuario de la plataforma, quiero que se rechacen datos inválidos para evitar proyectos sin una identificación útil.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** PM-01 y PM-07.

El sistema SHALL aceptar únicamente un objeto JSON de creación con `name` y, opcionalmente, `description`. `name` SHALL ser una cadena no nula de entre 1 y 120 caracteres después de retirar los espacios en blanco iniciales y finales (trim); un nombre vacío después del trim SHALL ser inválido. `description` SHALL ser una cadena de hasta 2000 caracteres después del mismo trim, o `null`; su omisión, una cadena vacía o solo espacios SHALL normalizarse a `null`. Las longitudes SHALL contarse en puntos de código Unicode, sin restringir el texto a ASCII y sin modificar los espacios interiores.

El sistema SHALL rechazar U+0000 en `name` y `description` antes de acceder a persistencia, con HTTP `400`, `VALIDATION_ERROR` y un error `INVALID_CHARACTER` por cada campo afectado. El carácter SHALL NOT eliminarse ni sustituirse silenciosamente. Los mensajes SHALL estar en español y omitir el valor rechazado. Esta exclusión se limita a U+0000 y no modifica el tratamiento del resto de Unicode.

El sistema SHALL rechazar con HTTP `400` un cuerpo ausente, JSON mal formado, un valor raíz distinto de un objeto, propiedades desconocidas o campos generados por el sistema. Los tipos incorrectos, la ausencia de `name` y los límites incumplidos SHALL producir HTTP `400` con detalle del campo, sin crear ningún proyecto. Los errores SHALL ajustarse a PM-07.

#### Scenario: Nombre normalizado y descripción omitida
- **WHEN** se solicita crear un proyecto con `{"name":"  Análisis inicial  "}`
- **THEN** el nombre almacenado es `Análisis inicial` y la descripción es `null`

#### Scenario: Nombre obligatorio y con contenido
- **WHEN** `name` está ausente, es `null`, es `""` o contiene únicamente espacios en blanco
- **THEN** se devuelve HTTP `400` con código `VALIDATION_ERROR` y un error asociado a `name`
- **AND** no se persiste ningún proyecto

#### Scenario: Límites del nombre
- **WHEN** el nombre normalizado contiene 120 puntos de código Unicode
- **THEN** el nombre se acepta
- **AND** un nombre de 121 puntos de código se rechaza con HTTP `400` y `VALIDATION_ERROR` asociado a `name`

#### Scenario: Descripción opcional y limitada
- **WHEN** se proporciona una descripción nula, vacía o formada solo por espacios
- **THEN** la descripción almacenada es `null`
- **AND** una descripción normalizada de 2000 puntos de código se acepta y una de 2001 se rechaza con HTTP `400` y `VALIDATION_ERROR` asociado a `description`

#### Scenario: Tipos incorrectos
- **WHEN** `name` es un valor no nulo distinto de una cadena o `description` no es una cadena ni `null`
- **THEN** se devuelve HTTP `400` y `VALIDATION_ERROR` con el campo afectado
- **AND** no se persiste ningún proyecto

#### Scenario: U+0000 en un campo de texto
- **WHEN** `name` o `description` contiene U+0000, también si llega como `\u0000` escapado en JSON
- **THEN** se devuelve HTTP `400`, `VALIDATION_ERROR` y un error `INVALID_CHARACTER` asociado al campo afectado
- **AND** no se accede a persistencia ni se crea un proyecto
- **AND** no se elimina ni sustituye el carácter y no se incluye el valor rechazado en la respuesta

#### Scenario: U+0000 en ambos campos
- **WHEN** `name` y `description` contienen U+0000 en la misma entrada
- **THEN** se devuelve HTTP `400` y `VALIDATION_ERROR` con un error `INVALID_CHARACTER` para cada uno de los dos campos
- **AND** no se accede a persistencia ni se crea un proyecto

#### Scenario: Cuerpo inválido o propiedades no admitidas
- **WHEN** el cuerpo está ausente, no es JSON válido, no es un objeto o incluye propiedades distintas de `name` y `description`, como `id` o `createdAt`
- **THEN** se devuelve HTTP `400` y `INVALID_REQUEST`
- **AND** no se persiste ningún proyecto

### Requirement: PM-03 Creación de un proyecto

**Historia:** Como usuario de la plataforma, quiero crear un proyecto para disponer de un contenedor persistente para mi trabajo posterior.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** PM-01, PM-02, PM-06 y PM-07.

El sistema SHALL permitir crear un proyecto mediante `POST /api/projects` con un cuerpo `application/json` válido. Tras confirmar su persistencia, SHALL devolver HTTP `201`, un cuerpo JSON con la representación completa de PM-01 y la cabecera `Location: /api/projects/{id}` correspondiente al identificador generado. Los nombres SHALL poder repetirse: la identidad del proyecto depende de `id`, sin exigir unicidad de `name`.

#### Scenario: Creación válida y consulta posterior
- **WHEN** se envía `{"name":"Proyecto de prueba","description":"Descripción de prueba"}` a `POST /api/projects`
- **THEN** se devuelve HTTP `201`, la representación del proyecto persistido y su `Location`
- **AND** una consulta a esa ubicación devuelve el mismo identificador y los mismos datos

#### Scenario: Proyectos distintos con el mismo nombre
- **WHEN** se crean dos proyectos válidos con el mismo nombre
- **THEN** ambas solicitudes devuelven HTTP `201` y los proyectos tienen identificadores diferentes

### Requirement: PM-04 Consulta por identificador

**Historia:** Como usuario de la plataforma, quiero consultar un proyecto concreto para recuperar sus datos básicos.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** PM-01, PM-06 y PM-07.

El sistema SHALL permitir consultar un proyecto mediante `GET /api/projects/{id}`. Para un UUID existente SHALL devolver HTTP `200` y la representación completa de PM-01. Para un UUID válido inexistente SHALL devolver HTTP `404` y `PROJECT_NOT_FOUND`. Un identificador con formato distinto de UUID SHALL producir HTTP `400` y `INVALID_PROJECT_ID`, antes de consultar la persistencia.

#### Scenario: Proyecto existente
- **WHEN** se consulta el identificador de un proyecto persistido
- **THEN** se devuelve HTTP `200` con sus cinco campos y los valores almacenados

#### Scenario: Proyecto inexistente
- **WHEN** se consulta un UUID válido que no corresponde a ningún proyecto
- **THEN** se devuelve HTTP `404` y `PROJECT_NOT_FOUND`

#### Scenario: Identificador mal formado
- **WHEN** se solicita `GET /api/projects/no-es-un-uuid`
- **THEN** se devuelve HTTP `400` y `INVALID_PROJECT_ID`
- **AND** no se consulta la persistencia

### Requirement: PM-05 Listado de proyectos

**Historia:** Como usuario de la plataforma, quiero listar los proyectos disponibles para elegir cuál consultar.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** PM-01, PM-06 y PM-07.

Para el MVP, el sistema SHALL devolver HTTP `200` y un array JSON completo de todos los proyectos persistidos, sin paginación, mediante `GET /api/projects`. Cada elemento SHALL contener la representación completa de PM-01. El listado SHALL ordenarse por `createdAt` descendente y, en caso de empate, por `id` ascendente según su representación UUID canónica. Sin proyectos SHALL devolver `[]`. Este contrato no incluye búsqueda ni filtros; la paginación se documenta como posible evolución futura en `design.md`.

#### Scenario: Listado vacío
- **WHEN** se solicita el listado y no existen proyectos
- **THEN** se devuelve HTTP `200` con `[]`

#### Scenario: Listado con varios proyectos
- **WHEN** se solicita el listado y existen varios proyectos persistidos
- **THEN** todos aparecen una sola vez, con sus cinco campos y del más reciente al más antiguo
- **AND** los proyectos con igual `createdAt` aparecen ordenados por `id` ascendente

### Requirement: PM-06 Conservación persistente de los proyectos

**Historia:** Como usuario de la plataforma, quiero conservar los proyectos creados para recuperarlos después de reiniciar la aplicación.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** PM-01 y PM-07.

El sistema SHALL conservar los cinco campos de cada proyecto más allá del ciclo de vida del proceso de aplicación. Una creación SHALL confirmarse como exitosa únicamente después de guardar atómicamente el proyecto. Una consulta o un listado SHALL recuperar los valores persistidos. Un fallo de almacenamiento SHALL producir el error de PM-07. El sistema SHALL evitar presentar ese fallo como creación exitosa, proyecto inexistente o listado vacío.

#### Scenario: Recuperación después de reiniciar
- **WHEN** se crea correctamente un proyecto, se reinicia la aplicación conservando la base de datos y se vuelve a consultar y listar
- **THEN** el proyecto conserva identificador, nombre, descripción y fechas sin cambios

#### Scenario: Fallo al guardar antes de confirmar la transacción
- **WHEN** la persistencia falla antes de confirmar la creación
- **THEN** se devuelve HTTP `500` y `INTERNAL_ERROR`, sin respuesta `201`
- **AND** no queda un proyecto parcialmente guardado

#### Scenario: Fallo de lectura
- **WHEN** la persistencia falla al consultar un proyecto o listar proyectos
- **THEN** se devuelve HTTP `500` y `INTERNAL_ERROR`
- **AND** el fallo no se transforma en HTTP `404` ni en HTTP `200` con `[]`

### Requirement: PM-07 Errores previsibles y sin detalles internos

**Historia:** Como consumidor de la API, quiero distinguir entradas inválidas, ausencia de un proyecto y fallos internos para interpretar correctamente cada respuesta.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** Transversal a PM-02, PM-03, PM-04, PM-05 y PM-06.

Para los errores definidos en este cambio, el sistema SHALL devolver un objeto JSON con `code`, `message` y `fieldErrors`. `code` SHALL ser estable y `message` SHALL contener una explicación comprensible en español. `fieldErrors` SHALL ser un array de objetos con `field`, `code` y `message`; SHALL estar vacío salvo para errores de validación de campos.

Los códigos SHALL ser `INVALID_REQUEST` (HTTP `400`, estructura o propiedades del cuerpo no admitidas), `VALIDATION_ERROR` (HTTP `400`, datos de campos inválidos), `INVALID_PROJECT_ID` (HTTP `400`), `PROJECT_NOT_FOUND` (HTTP `404`) e `INTERNAL_ERROR` (HTTP `500`). Los códigos de campo SHALL distinguir `REQUIRED`, `INVALID_TYPE`, `TOO_LONG` e `INVALID_CHARACTER`; para `name` ausente, nulo o en blanco se usará `REQUIRED` y para U+0000 en cualquiera de los dos textos se usará `INVALID_CHARACTER` en cada campo afectado. Las respuestas SHALL omitir valores de entrada rechazados, stack traces, SQL, detalles de conexión y credenciales.

#### Scenario: Error de validación interpretable
- **WHEN** se rechaza un nombre en blanco
- **THEN** se devuelve HTTP `400`, `code = VALIDATION_ERROR`, un mensaje en español y un elemento de `fieldErrors` con `field = name` y `code = REQUIRED`
- **AND** no se incluye el valor rechazado

#### Scenario: Error sin campo concreto
- **WHEN** se devuelve `PROJECT_NOT_FOUND` o `INTERNAL_ERROR`
- **THEN** la respuesta contiene `code`, un mensaje comprensible y `fieldErrors = []`
- **AND** no expone detalles internos ni datos de conexión
