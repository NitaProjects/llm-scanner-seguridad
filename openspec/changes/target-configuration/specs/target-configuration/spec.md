## Purpose

Permitir configurar de forma persistente una aplicación LLM objetivo por proyecto, con un contrato REST/JSON validado localmente y credenciales protegidas, sin ejecutar solicitudes contra el target.

## ADDED Requirements

### Requirement: TC-01 Asociación y cardinalidad

**Historia:** Como usuario, quiero asociar un target a un proyecto existente para preparar su análisis posterior.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** PM-04 de `project-management`.

El sistema SHALL permitir que un Project exista sin target y tenga como máximo una `TargetConfiguration`. Cada configuración SHALL tener UUID propio y `projectId` UUID inmutable, asociado a un proyecto existente. Dos proyectos SHALL poder configurar la misma URL de forma independiente. Configurar un target SHALL NOT modificar los datos o fechas de Project. El MVP SHALL NOT incluir selección de configuración activa ni múltiples configuraciones por proyecto.

#### Scenario: Proyecto sin target
- **WHEN** existe un proyecto al que no se ha configurado target
- **THEN** el proyecto sigue siendo consultable según PM-04 y no se crea una configuración implícita

#### Scenario: Proyecto inexistente o identificador inválido
- **WHEN** se consulta, crea o actualiza el target de un UUID de proyecto inexistente
- **THEN** se devuelve `404 PROJECT_NOT_FOUND`
- **AND** un identificador mal formado produce `400 INVALID_PROJECT_ID` antes de persistencia

### Requirement: TC-02 Creación y consulta

**Historia:** Como usuario, quiero guardar y recuperar la configuración del target sin ejecutarlo.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** TC-01 y TC-04 a TC-12.

El sistema SHALL aceptar `POST /api/projects/{projectId}/target` para crear la configuración. Tras confirmar persistencia SHALL devolver `201`, `Location: /api/projects/{projectId}/target` y la representación segura de TC-04. Si el proyecto ya tiene target SHALL devolver `409 TARGET_ALREADY_CONFIGURED` sin sobrescribirlo, incluso ante creaciones concurrentes. `GET` en la misma ruta SHALL devolver `200` con la configuración segura, o `404 TARGET_NOT_CONFIGURED` si el proyecto existe sin target. Ninguna operación SHALL contactar con el endpoint objetivo.

#### Scenario: Crear y recuperar
- **WHEN** se envía una configuración válida para un proyecto existente sin target
- **THEN** se devuelve `201` y su Location, y el GET posterior devuelve los mismos datos no sensibles con `200`
- **AND** se conserva Project sin cambios y no se hace ninguna solicitud al target

#### Scenario: Segunda creación
- **WHEN** se intenta crear otra configuración para el mismo proyecto, también mediante solicitudes concurrentes
- **THEN** como máximo una creación tiene éxito y la otra devuelve `409 TARGET_ALREADY_CONFIGURED`

#### Scenario: Consulta sin configuración
- **WHEN** se consulta el target de un proyecto existente que no tiene configuración
- **THEN** se devuelve `404 TARGET_NOT_CONFIGURED`

### Requirement: TC-03 Actualización de la configuración

**Historia:** Como usuario, quiero corregir el target configurado sin crear otro recurso.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** TC-01, TC-04 y TC-09 a TC-12.

El sistema SHALL aceptar `PUT /api/projects/{projectId}/target` para reemplazar los datos configurables, con la excepción explícita de conservación de credenciales de TC-10. La actualización válida SHALL devolver `200` con la representación segura y conservar `id`, `projectId` y `createdAt`. `updatedAt` SHALL reflejar la actualización, sin retroceder respecto de su valor previo ni de `createdAt`. Una actualización inválida o fallida SHALL conservar íntegra la configuración y credencial anteriores. Si no existe target SHALL devolver `404 TARGET_NOT_CONFIGURED`; PUT SHALL NOT crear implícitamente el recurso.

#### Scenario: Actualización válida
- **WHEN** se reemplaza válidamente la configuración de un target existente
- **THEN** se devuelve `200`, se conservan identidad y fecha de creación y se actualizan los datos configurables y `updatedAt`

#### Scenario: Actualización inválida o sin target
- **WHEN** los datos del PUT son inválidos
- **THEN** se devuelve `400 VALIDATION_ERROR` y no se modifica la configuración ni el secreto
- **AND** un PUT válido para un proyecto sin target devuelve `404 TARGET_NOT_CONFIGURED`

### Requirement: TC-04 Campos y contrato fijo del MVP

**Historia:** Como consumidor de la API, quiero conocer los datos admitidos y su representación pública.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** TC-01, TC-08 y TC-09.

La entrada SHALL ser un objeto JSON con `url`, `requestTemplate`, `responsePath` y `authType` obligatorios. `name`, `headers`, `timeoutMs` y `authConfig` SHALL ser opcionales según las reglas siguientes. `method`, `contentType` y `promptPlaceholder` SHALL admitir únicamente `POST`, `application/json` y `{{PROMPT}}`; si se omiten, el sistema SHALL aplicar esas constantes. Valores distintos SHALL producir `400 VALIDATION_ERROR`. Estas tres constantes SHALL NOT persistirse como columnas o datos configurables.

`name` SHALL ser texto opcional de hasta 120 puntos de código tras trim exterior; omisión, null o texto en blanco SHALL normalizarse a null. `headers` omitido SHALL normalizarse a `{}`. `timeoutMs`, si se especifica, SHALL ser un entero entre 1000 y 60000 inclusive, expresado en milisegundos; su omisión SHALL aplicar 10000 tanto en POST como en PUT, sin conservar el timeout anterior. Null explícito SHALL ser inválido. `url` SHALL admitir hasta 2048 puntos de código tras trim. `requestTemplate` SHALL ser una cadena con un documento JSON de hasta 32768 bytes UTF-8 y `responsePath` una cadena de hasta 1024 puntos de código tras trim. Los límites quedan aceptados provisionalmente para el MVP sin calibración adicional.

La representación pública SHALL contener `id`, `projectId`, `name`, `url`, `method`, `contentType`, `requestTemplate`, `promptPlaceholder`, `responsePath`, `headers`, `authType`, `authConfig`, `hasCredentials`, `timeoutMs`, `createdAt` y `updatedAt`. `authConfig` SHALL contener únicamente `headerName` para `API_KEY_HEADER`, y ser null para los otros tipos. `hasCredentials` SHALL indicar si hay una credencial persistida sin revelar su contenido. Al crear ambas fechas SHALL ser iguales, generadas por el sistema en UTC ISO 8601 con precisión de microsegundos. El sistema SHALL rechazar propiedades de entrada desconocidas y campos generados como `id`, `projectId`, fechas o `hasCredentials` con `400 INVALID_REQUEST`.

#### Scenario: Constantes y valores opcionales
- **WHEN** se crea una configuración válida sin name, headers, timeoutMs, method, contentType ni promptPlaceholder
- **THEN** la respuesta contiene name null, headers vacío, timeoutMs 10000, POST, application/json y `{{PROMPT}}`
- **AND** ambas fechas iniciales son iguales

#### Scenario: Límites y constantes inválidos
- **WHEN** timeoutMs está fuera de 1000..60000, no es entero, se supera un límite o se solicita un método o formato distinto
- **THEN** se devuelve `400 VALIDATION_ERROR` con los campos afectados sin almacenar ni actualizar el target

#### Scenario: Timeout omitido en actualización
- **WHEN** un PUT válido omite timeoutMs para una configuración que tenía otro timeout
- **THEN** se persiste y devuelve timeoutMs 10000
- **AND** un timeoutMs explícitamente null produce `400 VALIDATION_ERROR`

### Requirement: TC-05 URL y separación de controles SSRF

**Historia:** Como responsable del scanner, quiero distinguir una configuración bien formada de un destino autorizado para ejecución.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** ADR-001 y modelo de amenazas.

El sistema SHALL validar localmente que la URL sea absoluta, tenga esquema HTTP o HTTPS, host no vacío y puerto válido si se indica, sin userinfo, fragmento, espacios interiores ni caracteres de control. SHALL bloquear por defecto `localhost`, sus subdominios y formas con punto final, y literales IP reconocibles de loopback y redes privadas sin DNS. SHALL existir una excepción explícita y controlada para destinos local/demo autorizados, incluida demo-vulnerable, desactivada por defecto y habilitada mediante configuración externa del entorno local, limitada a destinos expresamente declarados; SHALL NOT ser un bypass activable desde la petición o desde TargetConfiguration. La excepción SHALL NOT desactivar validación estructural ni permitir indiscriminadamente redes internas o metadatos cloud. Las credenciales SHALL introducirse únicamente por `authConfig`, nunca por userinfo, parámetros de URL, headers ordinarios o plantilla. Los campos ordinarios se consideran no sensibles; esta regla SHALL NOT interpretarse como garantía de detección automática de cualquier secreto arbitrario en texto libre.

Guardar una configuración SHALL NOT realizar DNS, conexiones de prueba ni requests HTTP, ni certificar autorización o seguridad de red. La futura ejecución SHALL volver a validar el destino, resolver DNS y bloquear por defecto loopback, redes privadas, interfaces locales, link-local/metadatos y destinos no autorizados, además de controlar redirecciones y cambios de resolución y respetar exclusivamente la excepción local/demo controlada. Esta obligación contractual SHALL NOT sustituirse por la validación de configuración; su implementación queda fuera de este change. La política concreta de autorización, resolución y transporte durante ejecución sigue pendiente para ese change, sin bloquear la configuración local.

#### Scenario: URL estructuralmente inválida
- **WHEN** la URL es relativa, usa otro esquema, contiene userinfo, fragmento, puerto inválido, controles o un localhost/loopback/IP privada literal sin excepción autorizada
- **THEN** se devuelve `400 VALIDATION_ERROR` asociado a url sin acceder a red

#### Scenario: Host válido sin comprobación de red
- **WHEN** se configura una URL bien formada cuyo host no requiere rechazo local
- **THEN** se valida sin resolver DNS ni contactar con el target
- **AND** el resultado no afirma que el destino esté autorizado o permitido para ejecutar solicitudes

#### Scenario: Excepción local/demo controlada
- **WHEN** el entorno local habilita explícitamente un destino demo concreto en su configuración externa y se configura una URL de ese destino
- **THEN** se admite la configuración si supera las demás validaciones, sin DNS ni HTTP
- **AND** el mismo destino se rechaza por defecto y los destinos locales/privados no declarados siguen bloqueados
- **AND** ninguna propiedad de la petición permite habilitar o ampliar la excepción

### Requirement: TC-06 Plantilla JSON y placeholder único

**Historia:** Como usuario, quiero describir el cuerpo JSON sin introducir código o transformaciones arbitrarias.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** ADR-001 y TC-04.

El sistema SHALL exigir que `requestTemplate` no esté vacío y sea un documento JSON válido cuya raíz sea un objeto, sin claves duplicadas. Tras decodificar sus cadenas SHALL contener exactamente una aparición de `{{PROMPT}}`, situada como valor completo de una cadena, nunca como nombre de propiedad ni como fragmento de otra cadena. Cero o múltiples apariciones, ubicación incorrecta, JSON inválido, plantilla vacía o raíz distinta de objeto SHALL producir `400 VALIDATION_ERROR` asociado a requestTemplate. No se reconoce ningún otro placeholder funcional, script o transformación.

En una ejecución futura, el prompt SHALL insertarse como dato de cadena JSON con el escape correspondiente, conservando la estructura, sin sustitución textual que permita inyectar propiedades. Este change valida la plantilla; SHALL NOT construir ni enviar solicitudes reales.

#### Scenario: Plantilla válida
- **WHEN** requestTemplate contiene el documento `{"message":"{{PROMPT}}","temperature":0.2}`
- **THEN** la plantilla supera la validación local sin sustituir ni ejecutar el prompt

#### Scenario: Placeholder ausente o repetido
- **WHEN** la plantilla contiene cero o más de una aparición de `{{PROMPT}}` después de decodificar las cadenas
- **THEN** se devuelve `400 VALIDATION_ERROR` para requestTemplate

#### Scenario: JSON o ubicación no admitidos
- **WHEN** la plantilla está vacía, no es JSON válido, tiene claves duplicadas, no es un objeto o usa el token como clave o dentro de otra cadena
- **THEN** se devuelve `400 VALIDATION_ERROR` para requestTemplate y no se persiste

### Requirement: TC-07 JSONPath restringido

**Historia:** Como usuario, quiero indicar qué texto del JSON de respuesta será relevante para un análisis posterior.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** Decisión aprobada de JSONPath restringido y TC-04.

El sistema SHALL validar `responsePath` usando únicamente raíz `$`, propiedades e índices de arrays. Las propiedades SHALL expresarse como `.identifier`, con identifier ASCII `[A-Za-z_][A-Za-z0-9_]*`, o como `["property"]`, donde property es una cadena JSON no vacía con escapes JSON válidos. Los índices SHALL ser enteros decimales no negativos sin signo ni ceros iniciales salvo `0`, en la forma `[0]`. Se permite combinar segmentos y usar `$` para seleccionar la raíz. La ruta SHALL contener como máximo 32 segmentos y cada índice SHALL ser como máximo 2147483647.

El sistema SHALL rechazar ruta vacía, sintaxis inválida, filtros, scripts, comodines, selección múltiple/uniones, slices, descenso recursivo, índices negativos y expresiones complejas con `400 VALIDATION_ERROR` asociado a responsePath. No se ejecuta la ruta contra el target al configurar.

En una futura ejecución, la selección SHALL recuperar un único valor de tipo cadena. Propiedad ausente, índice fuera de rango o valor no textual SHALL constituir fallo de extracción, sin usar como alternativa el JSON completo ni convertir objetos a texto. Este change solo define esa frontera contractual.

#### Scenario: Propiedades e índices válidos
- **WHEN** responsePath es `$.response.text`, `$.choices[0].message.content`, `$["response-text"]` o `$`
- **THEN** se acepta su sintaxis sin solicitar ninguna respuesta real

#### Scenario: Expresiones fuera del subconjunto
- **WHEN** la ruta contiene un comodín, filtro, script, unión, slice, descenso recursivo, índice negativo o sintaxis no admitida
- **THEN** se devuelve `400 VALIDATION_ERROR` para responsePath

### Requirement: TC-08 Headers no sensibles

**Historia:** Como usuario, quiero configurar headers ordinarios sin duplicar credenciales ni alterar cabeceras controladas por el cliente HTTP.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** TC-04 y TC-09.

El sistema SHALL admitir headers como objeto de nombres y valores de cadena no sensibles, con como máximo 20 entradas. Los nombres SHALL cumplir la sintaxis HTTP token ASCII y tener hasta 128 caracteres. Los valores SHALL tener hasta 4096 bytes UTF-8, sin caracteres de control U+0000..U+001F ni U+007F. El conjunto de nombres y valores SHALL ocupar como máximo 16384 bytes UTF-8. Los nombres SHALL compararse sin distinción de mayúsculas para detectar duplicados y colisiones.

En headers ordinarios SHALL prohibirse `Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie`, `X-API-Key`, `API-Key`, `Host`, `Content-Length`, `Content-Type`, `Connection`, `Transfer-Encoding`, `TE`, `Trailer`, `Upgrade`, `Keep-Alive`, `Proxy-Connection`, `Forwarded` y nombres con prefijo `X-Forwarded-`. El header configurado para API key SHALL estar ausente de headers ordinarios. Las credenciales y nombres de autenticación SHALL gestionarse exclusivamente mediante TC-09, sin duplicar secretos en headers. Colisiones, tipos inválidos, nombres prohibidos o límites incumplidos SHALL producir `400 VALIDATION_ERROR` para headers, omitiendo nombre y valor de entrada en el error.

#### Scenario: Headers ordinarios válidos
- **WHEN** se configura `{"Accept":"application/json","X-Correlation-Id":"demo"}`
- **THEN** se almacena como headers no sensibles y se devuelve sin añadir credenciales

#### Scenario: Header sensible, reservado o duplicado
- **WHEN** se incluye Authorization, Cookie, Host, un nombre repetido con distinto caso, el header de authConfig o un valor con CR/LF
- **THEN** se devuelve `400 VALIDATION_ERROR` para headers sin reflejar sus datos

### Requirement: TC-09 Tipos de autenticación y representación segura

**Historia:** Como usuario, quiero configurar autenticación del target sin recuperar después sus secretos.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** ADR-002, TC-08 y TC-11.

El sistema SHALL admitir únicamente `NONE`, `BEARER_TOKEN` y `API_KEY_HEADER`. Para NONE, authConfig SHALL estar omitido o ser null y no se guardará credencial. Para BEARER_TOKEN, la entrada authConfig SHALL contener `secretValue` y ningún headerName; su uso futuro será `Authorization: Bearer <secretValue>`. Para API_KEY_HEADER, authConfig SHALL contener `headerName` y `secretValue`. En PUT, secretValue SHALL admitir únicamente la excepción de omisión/conservación de TC-10. En creación autenticada, secretValue SHALL ser obligatorio, no vacío ni solo espacios, de hasta 8192 bytes UTF-8 y sin caracteres de control; SHALL conservarse exactamente sin trim. El token Bearer SHALL además carecer de espacios. No se admite un secreto en campos adicionales.

En `authConfig.secretValue`, el sistema SHALL rechazar cualquier surrogate UTF-16 alto o bajo aislado antes de cifrar o escribir, con HTTP `400`, `VALIDATION_ERROR` y error de campo `authConfig.secretValue` con código `INVALID_CHARACTER`, sin reflejar el valor rechazado. Los pares surrogate válidos que representan caracteres Unicode suplementarios SHALL aceptarse si cumplen las demás reglas. El sistema SHALL NOT eliminar, sustituir ni normalizar silenciosamente caracteres. Un PUT rechazado por esta regla SHALL conservar íntegramente la configuración y credencial anteriores.

El headerName de API key SHALL cumplir el formato/límite de nombres de TC-08; SHALL NOT ser un header reservado de transporte/proxy, Authorization, Proxy-Authorization, Cookie o Set-Cookie. `X-API-Key` y `API-Key` son válidos aquí. No se admiten colisiones con headers ordinarios.

Todas las respuestas exitosas SHALL omitir secretValue y cualquier credencial en claro, cifrada o enmascarada, incluyendo claves, nonce y metadatos criptográficos. SHALL devolver únicamente authType, el headerName no sensible cuando corresponda y hasCredentials. El sistema SHALL NOT descifrar credenciales para consultar o devolver la configuración.

#### Scenario: Configuración sin autenticación
- **WHEN** se crea con authType NONE y sin authConfig
- **THEN** se devuelve authConfig null y hasCredentials false y no se guarda secreto

#### Scenario: Configuración autenticada
- **WHEN** se crea con BEARER_TOKEN o API_KEY_HEADER y sus datos válidos
- **THEN** la respuesta y los GET posteriores indican hasCredentials true y omiten el secreto y su representación cifrada

#### Scenario: Credencial ausente o autenticación no admitida
- **WHEN** una creación autenticada no tiene secreto válido o se solicita otro authType
- **THEN** se devuelve `400 VALIDATION_ERROR` sin almacenar configuración ni credenciales

#### Scenario: Surrogate aislado en una credencial
- **WHEN** POST o PUT contiene un surrogate alto o bajo aislado en `authConfig.secretValue`
- **THEN** se devuelve HTTP `400`, `VALIDATION_ERROR` y `INVALID_CHARACTER` para `authConfig.secretValue`, sin incluir el valor rechazado
- **AND** no se invoca el cifrador ni se escribe en persistencia
- **AND** en PUT se conservan la configuración y credencial anteriores

#### Scenario: Par surrogate válido en una credencial
- **WHEN** `authConfig.secretValue` contiene un par surrogate válido y cumple las demás reglas
- **THEN** la credencial se acepta y se cifra sin alterar el carácter suplementario

### Requirement: TC-10 Sustitución y conservación explícita de credenciales

**Historia:** Como usuario, quiero actualizar datos ordinarios o reemplazar credenciales sin tener que leer un secreto almacenado.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** TC-03, TC-09 y TC-11.

En PUT autenticado, omitir secretValue SHALL conservar la credencial existente únicamente si se mantienen authType, headerName cuando corresponda y el origen de URL: esquema, host normalizado y puerto efectivo. authConfig SHALL seguir presente, con `{}` para BEARER_TOKEN o headerName para API_KEY_HEADER. Una creación, cambio de authType, headerName u origen SHALL exigir secretValue nuevo; el sistema SHALL rechazar la omisión con `400 VALIDATION_ERROR`, sin trasladar silenciosamente credenciales a otro destino. secretValue explícitamente null o vacío SHALL ser inválido, sin significar conservar o eliminar.

Un secretValue nuevo SHALL reemplazar la credencial anterior de forma atómica. Cambiar a NONE SHALL eliminar la credencial y devolver hasCredentials false. La actualización ordinaria que conserva la credencial SHALL NOT necesitar descifrarla. Cambiar la URL conservando el mismo origen mantiene esa regla; esto no certifica que su ruta sea un destino autorizado para ejecución.

#### Scenario: Actualizar sin reemplazar el secreto
- **WHEN** se actualizan datos ordinarios, se mantienen origen/tipo/header y se omite secretValue con authConfig válido
- **THEN** se conserva el secreto sin devolverlo ni descifrarlo

#### Scenario: Cambiar destino o autenticación
- **WHEN** cambia el origen de URL, authType o headerName y no se facilita un secreto nuevo para el tipo autenticado
- **THEN** se devuelve `400 VALIDATION_ERROR` y se conserva íntegra la configuración anterior

#### Scenario: Retirar o reemplazar credencial
- **WHEN** se actualiza válidamente a NONE o se facilita un secretValue nuevo
- **THEN** se elimina o reemplaza respectivamente la credencial anterior en la misma transacción que la configuración

### Requirement: TC-11 Persistencia cifrada y conservación

**Historia:** Como responsable del scanner, quiero conservar configuración y credenciales sin guardar secretos en texto plano.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** TC-01, TC-09 y TC-10; D-CRYPTO aprobado para el MVP local.

El sistema SHALL persistir la configuración en `target_configurations`, separada de Project, con FK a projects y UNIQUE(project_id), y conservar sus datos tras reiniciar. Las credenciales SHALL cifrarse a nivel de aplicación mediante AES-256-GCM antes de llegar al almacenamiento, con clave de 256 bits suministrada externamente mediante variable de entorno/configuración segura, nunca en PostgreSQL ni en el repositorio. Cada cifrado SHALL utilizar un nonce aleatorio nuevo. El material persistido SHALL contener una versión de formato y la información técnica necesaria para descifrar y verificar integridad, incluyendo nonce, ciphertext y etiqueta de autenticación, nunca la clave. El sistema SHALL NOT guardar secretos en claro en columnas, JSON de headers/authConfig, logs, respuestas, informes, errores ni copias auxiliares de la entrada. El descifrado SHALL realizarse solo en memoria cuando sea necesario; este change no introduce descifrado para GET ni ejecución del target. AWS KMS/Secrets Manager quedan como evolución futura sin cambiar el contrato de dominio.

Configuración y credencial SHALL confirmarse o fallar como una unidad. Un fallo de cifrado o almacenamiento SHALL producir `500 INTERNAL_ERROR`, sin éxito aparente ni modificaciones parciales. Si el mecanismo de cifrado o clave no está disponible, el sistema SHALL rechazar la operación que requiera cifrar, sin guardar plaintext ni degradar autenticación a NONE. Un GET SHALL poder devolver metadatos no sensibles sin acceder a la clave.

#### Scenario: Conservación sin secretos en claro
- **WHEN** se configura válidamente un target autenticado y se reinicia conservando la base de datos
- **THEN** los datos no sensibles se recuperan y solo existe la credencial cifrada en almacenamiento
- **AND** no se expone ni se descifra para GET

#### Scenario: Cifrado o persistencia fallidos
- **WHEN** falla el cifrado, falta una clave necesaria o falla la transacción
- **THEN** se devuelve `500 INTERNAL_ERROR`, sin persistir plaintext ni confirmar parcialmente el cambio

#### Scenario: Cifrado nuevo y formato recuperable
- **WHEN** se cifra dos veces la misma credencial con una clave sintética externa válida
- **THEN** se genera un nonce nuevo en cada operación y el material versionado permite recuperar y verificar el secreto en memoria con esa clave
- **AND** una clave incorrecta o la alteración del material no produce plaintext utilizable ni expone detalles en errores

### Requirement: TC-12 Validación de entrada y errores sanitizados

**Historia:** Como consumidor, quiero errores previsibles sin que un fallo revele credenciales o datos internos.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** TC-01 a TC-11 y convención de errores PM-07.

El sistema SHALL rechazar cuerpo ausente, JSON mal formado, raíz no objeto, claves duplicadas o propiedades desconocidas, también dentro de authConfig, con `400 INVALID_REQUEST`. Los campos requeridos ausentes/nulos y tipos, formatos o límites incorrectos SHALL producir `400 VALIDATION_ERROR` con detalle de campo. U+0000 en textos, claves o cadenas decodificadas de plantilla/headers/ruta SHALL rechazarse antes de normalizar, sin eliminarlo o sustituirlo. El sistema SHALL validar antes de cifrar o escribir.

Los errores SHALL usar `{code,message,fieldErrors}`, con mensajes en español y fieldErrors vacío salvo validación. Se admiten `INVALID_REQUEST`, `VALIDATION_ERROR`, `INVALID_PROJECT_ID`, `PROJECT_NOT_FOUND`, `TARGET_NOT_CONFIGURED`, `TARGET_ALREADY_CONFIGURED` e `INTERNAL_ERROR`, con estados definidos anteriormente. Los errores de campo SHALL usar `REQUIRED`, `INVALID_TYPE`, `TOO_LONG`, `INVALID_FORMAT`, `INVALID_CHARACTER`, `OUT_OF_RANGE`, `NOT_ALLOWED` o `CONFLICT`. Los nombres de campo SHALL limitarse al contrato, nunca incluir claves arbitrarias de headers o fragmentos de ruta/template.

Respuestas y logs SHALL omitir valores rechazados, cuerpos completos, secretos, ciphertext, claves, detalles criptográficos, SQL, conexión y stack traces con datos de entrada. Un fallo de lectura SHALL devolver `500 INTERNAL_ERROR`, nunca simular ausencia. Este change SHALL NOT generar informes ni registrar credenciales para uso posterior.

#### Scenario: Error interpretable sin reflejar entrada
- **WHEN** se rechaza un header o un secreto inválido
- **THEN** se devuelve `400 VALIDATION_ERROR` con un campo contractual y mensaje en español, sin nombre/valor de header aportado ni secreto

#### Scenario: U+0000 o propiedades desconocidas
- **WHEN** un campo contiene U+0000 o el objeto incluye una propiedad desconocida
- **THEN** se devuelve respectivamente `400 VALIDATION_ERROR` con INVALID_CHARACTER o `400 INVALID_REQUEST`, sin cifrar ni escribir

#### Scenario: Fallo interno de lectura
- **WHEN** falla el almacenamiento al recuperar una configuración
- **THEN** se devuelve `500 INTERNAL_ERROR` con fieldErrors vacío y sin detalles internos
