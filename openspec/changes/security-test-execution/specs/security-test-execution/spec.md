## Purpose

Ejecutar un conjunto pequeño de pruebas controladas contra el target autorizado de un Scan, conservar evidencia técnica minimizada y consultar el resultado de ejecución sin evaluar vulnerabilidades.

## Review Status

Contrato aprobado explícitamente por el usuario con las concreciones D-01 a D-11 registradas en design.md. **READY FOR IMPLEMENTATION**, sin decisiones de diseño bloqueantes para este incremento. La aprobación no autoriza implementar en esta entrega; hace falta una petición posterior. Los límites numéricos son valores técnicos iniciales configurables dentro de los márgenes aprobados, no campos configurables por API ni requisitos de igualdad rígida para toda instalación.

## ADDED Requirements

### Requirement: STE-01 Catálogo inicial identificable y reproducible

**Historia:** Como investigador, quiero conocer exactamente qué prueba se ejecutó para interpretar y reproducir su evidencia.
**Prioridad:** Alta, MVP.
**Dependencias:** Categorías aprobadas en ADR-006; D-01/D-10/D-11.

El sistema SHALL disponer de cuatro SecurityTests iniciales inmutables, uno por categoría LLM01, LLM02, LLM08 y LLM10, con los códigos `LLM01-INSTRUCTION-001`, `LLM02-CANARY-001`, `LLM08-CONTEXT-001` y `LLM10-MARKUP-001`, todos en revisión 1. Cada definición SHALL tener UUID, code, revision positiva, name, category y prompt. El UUID SHALL identificar la revisión persistida; code y revision SHALL identificar su contenido reproducible. La categoría SHALL pertenecer exactamente al conjunto aprobado y determinar su referencia OWASP GenAI LLM Top 10 (2026). Los prompts exactos y sus objetivos/expectativas de comportamiento figuran en D-11; SHALL NOT incorporar secretos reales, herramientas, acciones persistentes, acceso a archivos ni carga masiva.

El catálogo SHALL conservarse en persistencia sin edición mediante API. El MVP SHALL ejecutar exactamente las cuatro pruebas iniciales sembradas y vigentes, sin selección configurable por usuario ni ejecución automática de filas/revisiones desconocidas. Una ejecución histórica SHALL seguir referenciando la definición original; cambiar un prompt SHALL requerir una nueva revisión en un change posterior. Este incremento SHALL NOT introducir CRUD, enabled, proveedor, expectedVerdict, severity, confidence, verdict ni criterios/reglas ejecutables de evaluación.

#### Scenario: Catálogo inicial completo
- **WHEN** se prepara un Scan con el catálogo de este incremento
- **THEN** existen exactamente los cuatro códigos en revisión 1 y sus cuatro categorías aprobadas
- **AND** cada código/revisión identifica siempre el mismo UUID y prompt tras reiniciar

#### Scenario: Evidencia histórica estable
- **WHEN** se consulta una ejecución previamente persistida
- **THEN** su definición de prueba conserva código, revisión, nombre, categoría y prompt originales
- **AND** no se reemplaza por una variante nueva ni por contenido mutable

### Requirement: STE-02 Plan interno y selección estable del target

**Historia:** Como usuario, quiero que todo el Scan utilice un conjunto de pruebas y una configuración coherentes.
**Prioridad:** Alta, MVP.
**Dependencias:** SE-02/SE-03/SE-05; TC-01 a TC-11; STE-01; D-01/D-03/D-07.

Después de confirmar Scan RUNNING y antes de enviar peticiones, el sistema SHALL fijar una sola vez las cuatro definiciones en orden ascendente de code y revision y leer una única configuración vigente del target de ese Project. SHALL preparar atómicamente una TestExecution PENDING por definición, conservando targetConfigurationId y targetUpdatedAt. La configuración completa, incluidos headers y credencial cifrada, SHALL permanecer exclusivamente en memoria del trabajo, sin snapshot sensible en Scan/TestExecution. Actualizar el target después de esta lectura SHALL NOT cambiar el destino, autenticación, plantilla, ruta o timeout entre pruebas del mismo Scan. Un Scan posterior SHALL leer la configuración entonces vigente. La selección se fija al comenzar el worker, no al POST inicial.

Un catálogo vacío, incompleto, duplicado, no compatible con el conjunto aprobado o superior al límite configurado SHALL producir fallo de procesamiento sin peticiones. SHALL NOT truncarse silenciosamente el plan. Una ausencia/fallo de lectura de target SHALL producir fallo de procesamiento sin usar valores predeterminados. El POST de inicio SHALL conservar su contrato sin cuerpo, sin selección del cliente, DNS ni descifrado. Ningún envío SHALL ocurrir antes del commit del plan y del inicio RUNNING de su TestExecution.

#### Scenario: Cambio de target durante el Scan
- **WHEN** se actualiza el target después de fijar el plan y antes de la segunda prueba
- **THEN** todas las pruebas usan la configuración leída al preparar el plan
- **AND** otro Scan usa la actualización y no se bloquea ni modifica el PUT aprobado

#### Scenario: Plan no utilizable
- **WHEN** falta una definición, el catálogo está vacío o supera el máximo permitido
- **THEN** Scan termina FAILED con SCAN_PROCESSING_FAILED si puede persistirse
- **AND** no se envía ninguna petición ni se oculta el error como Scan vacío COMPLETED

#### Scenario: Preparación no confirmada
- **WHEN** falla el commit de las cuatro ejecuciones PENDING
- **THEN** no se envía ninguna petición y no queda un plan parcialmente confirmado
- **AND** se aplica el tratamiento de persistencia y fallo de STE-10

### Requirement: STE-03 Identidad y estados técnicos de TestExecution

**Historia:** Como usuario, quiero distinguir pruebas pendientes, activas, finalizadas y fallidas sin confundirlo con vulnerabilidad.
**Prioridad:** Alta, MVP.
**Dependencias:** STE-02/STE-09/STE-10; D-02/D-03.

Cada TestExecution SHALL tener UUID inmutable, scanId, referencia a la revisión persistida de SecurityTest y los metadatos de target de STE-02. Los estados SHALL ser PENDING, RUNNING, COMPLETED y FAILED. SHALL existir como máximo una ejecución por (scanId, revisión de prueba), sin reintentos.

PENDING SHALL tener startedAt, finishedAt, errorReason, httpStatus, latencyMs y evidencia null. RUNNING SHALL tener startedAt y el resto anterior null. COMPLETED SHALL tener startedAt, finishedAt, httpStatus 200..299, latencyMs no negativa, requestBody y responseText, y errorReason null. FAILED SHALL tener finishedAt y un errorReason cerrado; startedAt SHALL ser null solo si no llegó a RUNNING, con httpStatus/latencyMs/evidencia null en ese caso. Un FAILED iniciado SHALL conservar únicamente los datos técnicos/evidencia disponibles y permitidos. requestRedacted/responseRedacted SHALL ser booleanos, inicialmente false y false cuando la evidencia correspondiente sea null.

Las transiciones SHALL limitarse a PENDING → RUNNING, RUNNING → COMPLETED, RUNNING → FAILED y PENDING → FAILED por aborto/interrupción. Los terminales SHALL ser inmutables. Fechas SHALL ser UTC con precisión de microsegundos, no anteriores a Scan.startedAt y con finishedAt no anterior a startedAt cuando exista; el reloj no SHALL hacer retroceder fechas. latencyMs SHALL medir con reloj monotónico la duración del intento de invocación, incluyendo preparación/DNS/transporte/extracción, excluyendo espera de cuota y persistencia; no es una métrica de inferencia del LLM.

#### Scenario: Respuesta potencialmente peligrosa técnicamente correcta
- **WHEN** el target devuelve HTTP 200, JSON válido y un string seleccionable que contiene instrucciones o marcado
- **THEN** TestExecution termina COMPLETED sin inspeccionar semánticamente ese texto
- **AND** no se crea un veredicto, finding ni fallo técnico por su contenido

#### Scenario: Prueba no iniciada al abortar
- **WHEN** un fallo global obliga a detener el Scan con pruebas todavía PENDING
- **THEN** esas pruebas quedan FAILED con EXECUTION_ABORTED y startedAt null
- **AND** no se inventan peticiones, respuesta HTTP o latencia

#### Scenario: Commit de RUNNING fallido o entrega duplicada
- **WHEN** no se confirma el inicio de una TestExecution o se intenta reclamar una ya iniciada/terminal
- **THEN** no se repite ni se envía una petición desde esa reclamación
- **AND** un fallo de persistencia sigue STE-10, sin convertirlo en ausencia

### Requirement: STE-04 Construcción JSON y protocolo de invocación

**Historia:** Como usuario, quiero que el payload llegue como dato al campo configurado sin alterar el documento JSON.
**Prioridad:** Alta, MVP.
**Dependencias:** TC-04/TC-06/TC-08; STE-05 a STE-08; D-05.

El sistema SHALL usar POST y application/json, los headers ordinarios aprobados y la autenticación separada. SHALL parsear la plantilla validada, reemplazar únicamente el valor de cadena completo `{{PROMPT}}` por el prompt seleccionado y serializar JSON válido en UTF-8. Comillas, barras, saltos de línea y caracteres Unicode SHALL permanecer como contenido de cadena, sin sustitución textual sobre el documento ni templating genérico. La estructura y los demás valores SHALL conservarse. Una plantilla persistida corrupta SHALL fallar sin enviar. El sistema SHALL NOT aceptar cookies/sesiones, autenticación automática, proxies heredados, reintentos de transporte ni compresión de respuesta; SHALL solicitar identity y rechazar Content-Encoding distinto de identity/ausente.

#### Scenario: Prompt con caracteres especiales
- **WHEN** el prompt contiene comillas, barras, saltos de línea y Unicode
- **THEN** el servidor fixture recibe un único valor de cadena igual al prompt y ninguna propiedad inyectada
- **AND** los demás campos mantienen su estructura y valor

#### Scenario: Headers ordinarios
- **WHEN** el target configura headers ordinarios válidos
- **THEN** se transmiten sin sustituir Content-Type, Host ni cabeceras reservadas y sin colisionar con autenticación
- **AND** ningún header se incorpora a TestExecution o logs

### Requirement: STE-05 Autenticación efímera y protección de credenciales

**Historia:** Como responsable del scanner, quiero invocar targets autenticados sin volver persistente su secreto descifrado.
**Prioridad:** Alta, MVP.
**Dependencias:** TC-09 a TC-11 y ADR-002; STE-06/STE-09; D-04/D-05.

NONE SHALL enviar sin credencial y sin consultar la clave. BEARER_TOKEN SHALL enviar únicamente `Authorization: Bearer <secret>` y API_KEY_HEADER únicamente su headerName/secret, además de los headers ordinarios. El secreto SHALL descifrarse usando el mecanismo AES-256-GCM/envelope existente, solo en memoria y después de autorizar/validar el destino, inmediatamente antes de construir la autenticación del envío. SHALL NOT descifrarse para POST/GET de Scan, consulta de ejecuciones ni GET/PUT de conservación de target. La credencial SHALL corresponder siempre a la configuración fijada para este Scan.

Clave ausente/incorrecta, envelope alterado/versión desconocida o secreto no utilizable SHALL producir CREDENTIAL_UNAVAILABLE, sin envío ni degradación a NONE. Autenticación sobre HTTP público SHALL ser rechazada con TARGET_TRANSPORT_REJECTED; HTTP autenticado SHALL admitirse exclusivamente para un origen local/demo autorizado mediante la excepción aprobada. TLS público SHALL verificar cadena de confianza, hostname y SNI del host original, sin trust-all.

Credencial, ciphertext, clave, nonce, headers de autenticación y mensajes de excepciones SHALL NOT persistirse en ejecuciones, logs, errores o respuestas API. Las copias de buffers propios SHALL limpiarse y las referencias efímeras liberarse; no se promete borrado garantizado de Strings del runtime. Las coincidencias exactas del secreto conocido, también si aparece reflejado dentro del texto extraído o cadenas JSON del request, SHALL redactarse antes de persistir y marcarse según STE-09.

#### Scenario: Bearer y API key
- **WHEN** el servidor controlado recibe una ejecución autorizada con cada tipo de autenticación
- **THEN** recibe exactamente el header correspondiente con la credencial sintética correcta
- **AND** esa credencial no aparece en evidencia, API, errores ni logs

#### Scenario: Clave o envelope inutilizables
- **WHEN** la credencial no puede recuperarse íntegramente
- **THEN** no se hace la petición, la ejecución iniciada falla con CREDENTIAL_UNAVAILABLE y el resto se aborta
- **AND** se conserva el cifrado original sin intentar recifrar, reemplazar o revelar detalles

#### Scenario: Credencial reflejada por el target
- **WHEN** el string extraído incluye una coincidencia exacta del secreto sintético enviado
- **THEN** se persiste su sustitución por `[REDACTED_CREDENTIAL]` con responseRedacted true
- **AND** no se conserva una copia alternativa del secreto, del body completo o de headers

### Requirement: STE-06 Autorización de destino, DNS efectivo y SSRF

**Historia:** Como responsable del scanner, quiero impedir que una URL configurable convierta el backend en un proxy hacia destinos no autorizados.
**Prioridad:** Alta, condición previa a cualquier red.
**Dependencias:** TC-05, ADR-001 y modelo de amenazas; D-04/D-05.

Antes de cada intento, el sistema SHALL exigir un origen exacto autorizado mediante configuración externa administrada, inicialmente vacía para destinos públicos. La autorización SHALL comparar esquema, host normalizado y puerto efectivo, sin comodines, redes completas ni habilitación desde API/TargetConfiguration. La declaración operativa SHALL representar la autorización de sistemas propios o con permiso; no certifica por sí sola titularidad ni sustituye la validación de red. La excepción demo SHALL conservar exclusivamente perfil local, TARGET_LOCAL_DEMO_ENABLED y TARGET_LOCAL_DEMO_ORIGINS ya aprobados, nunca un bypass general.

El sistema SHALL resolver efectivamente todas las direcciones A/AAAA y validar la totalidad del conjunto antes de conectar, con máximo de 16 direcciones por resolución; exceso, DNS vacío o fallido SHALL producir DNS_RESOLUTION_FAILED sin fallback. Las IP literales SHALL canonicalizarse y pasar la misma política sin necesitar DNS. Por defecto SHALL bloquear localhost/loopback, interfaces del propio backend, direcciones privadas, link-local, CGNAT, multicast, unspecified, broadcast, rangos especiales/reservados/no globales y metadatos cloud conocidos. SHALL normalizar IPv4, IPv6 y IPv4 embebida, sin escapes de clasificación por representaciones alternativas. Un hostname que resuelva a una IP bloqueada, o a una mezcla permitida/bloqueada, SHALL ser rechazado.

El transporte SHALL conectar únicamente a la dirección validada del intento, manteniendo Host/SNI/verificación TLS originales; SHALL NOT efectuar una segunda resolución fuera de esa política, usar proxy implícito ni reutilizar una conexión de otro intento. No basta resolver/validar y después conectar nuevamente por hostname. El siguiente test SHALL resolver y validar otra vez. La excepción demo SHALL permitir solo loopback/privadas del origen explícito; SHALL NOT admitir metadatos, link-local ni las demás redes especiales, incluso declaradas.

#### Scenario: Origen sin autorización operativa
- **WHEN** una URL configurada tiene origen no declarado para ejecución
- **THEN** la ejecución falla con TARGET_NOT_AUTHORIZED sin DNS, descifrado ni conexión
- **AND** los siguientes tests se abortan y el usuario no puede habilitar el destino desde el POST

#### Scenario: DNS hacia dirección bloqueada o mezcla
- **WHEN** un hostname autorizado resuelve a una dirección bloqueada, aunque otra sea pública
- **THEN** no se abre ninguna conexión y se registra TARGET_ADDRESS_BLOCKED

#### Scenario: Rebinding entre validación y conexión
- **WHEN** un resolvedor fixture cambia su respuesta después de devolver una IP permitida
- **THEN** el intento solo conecta a una IP previamente validada, sin una segunda resolución ajena
- **AND** un intento posterior que obtiene una IP bloqueada no conecta

#### Scenario: Demo explícita y exclusiones absolutas
- **WHEN** se invoca un origen demo loopback/privado declarado y habilitado únicamente en perfil local
- **THEN** puede conectarse si supera las demás validaciones
- **AND** el mismo origen sin excepción, otro origen privado, link-local y metadatos siguen bloqueados

### Requirement: STE-07 Cero redirecciones y errores HTTP

**Historia:** Como usuario, quiero que el scanner contacte solo con el destino aprobado sin repetir peticiones o filtrar autenticación.
**Prioridad:** Alta, MVP.
**Dependencias:** STE-04 a STE-06; D-05/D-06.

El máximo de redirecciones SHALL ser cero. Toda respuesta 3xx SHALL registrarse como HTTP_STATUS_ERROR con el estado recibido, sin seguir Location, resolver otro destino, cambiar POST a GET ni reenviar headers/credenciales. El sistema SHALL NOT almacenar Location, cookies ni headers de respuesta. Cualquier HTTP no 2xx, incluidos 401, 403, 429 y 5xx, SHALL producir HTTP_STATUS_ERROR, sin reintento automático ni interpretación de vulnerabilidad. Para estos errores SHALL omitirse el body de respuesta y SHALL cerrarse la conexión sin drenar un cuerpo ilimitado. Un 2xx SHALL requerir además la extracción válida de STE-08 para ser COMPLETED.

#### Scenario: Redirect hacia metadatos u otro origen
- **WHEN** el target devuelve 301, 302, 303, 307 o 308 hacia cualquier destino
- **THEN** se conserva únicamente el estado HTTP y el fallo técnico
- **AND** no hay segunda petición, DNS del nuevo destino, GET ni transferencia de autenticación

#### Scenario: HTTP de error y respuesta válida peligrosa
- **WHEN** un test recibe 500 y el siguiente recibe 200 con JSON/string válido de contenido potencialmente peligroso
- **THEN** el primero queda FAILED y el segundo COMPLETED
- **AND** no se genera un finding ni se clasifica el segundo como fallo por su significado

### Requirement: STE-08 Extracción textual y validez técnica de respuesta

**Historia:** Como evaluador futuro, quiero un texto bien identificado en lugar de un body arbitrario o una conversión implícita.
**Prioridad:** Alta, MVP.
**Dependencias:** TC-07; STE-07/STE-09; D-03/D-05/D-06.

Para HTTP 2xx, el sistema SHALL exigir Content-Type application/json o un media type application/*+json, decodificación UTF-8 estricta, documento JSON único sin claves duplicadas y extracción usando exactamente la gramática de TC-07. El resultado SHALL ser exactamente un string, incluido string vacío; `$` SHALL admitir una raíz string. SHALL NOT convertirse objeto, array, número, booleano o null a texto ni usar el documento completo como fallback.

Content-Type/encoding no admitido SHALL producir RESPONSE_FORMAT_ERROR; JSON inválido, Unicode inválido, múltiples documentos, claves duplicadas o límites del parser incumplidos SHALL producir RESPONSE_INVALID_JSON. Path ausente/índice fuera de rango SHALL producir RESPONSE_PATH_NOT_FOUND; tipo no string SHALL producir RESPONSE_NOT_TEXT. Exceder tamaños SHALL producir los códigos de STE-09, sin truncar y declarar COMPLETED. Una respuesta JSON con string que incluya marcado, instrucciones o datos sensibles ajenos a la autenticación conocida SHALL seguir tratándose como evidencia no confiable, sin juicio semántico.

#### Scenario: Ruta restringida válida y raíz textual
- **WHEN** se reciben JSON válidos para rutas de propiedad, propiedad entre comillas, índice o raíz `$`
- **THEN** se obtiene exactamente el string seleccionado, también si es vacío
- **AND** no se ejecutan filtros, scripts, comodines ni selección múltiple

#### Scenario: Body y ruta inválidos
- **WHEN** se reciben body no JSON, claves duplicadas, UTF-8/surrogates inválidos, path ausente o valor no textual
- **THEN** se registra el código técnico correspondiente, sin body completo ni fallback
- **AND** puede continuar la siguiente prueba conforme a STE-10

### Requirement: STE-09 Límites efectivos y evidencia minimizada

**Historia:** Como responsable del scanner, quiero acotar consumo y conservar solo la evidencia útil para una evaluación posterior.
**Prioridad:** Alta, MVP.
**Dependencias:** TC-04/TC-08/TC-09; STE-03 a STE-08; D-03/D-06.

El sistema SHALL aplicar límites finitos de configuración técnica con los valores iniciales siguientes. Los parámetros configurables SHALL recibirse externamente, respetar sus márgenes de seguridad y nunca proceder del target o la API de inicio. Los valores iniciales SHALL usarse como defaults, no como constantes funcionales rígidas ni propiedades de los DTOs/entidades de negocio. Una configuración inválida/no finita SHALL impedir habilitar procesamiento, sin fallback ilimitado. Cambiar un límite técnico dentro de sus márgenes SHALL NOT cambiar las cuatro pruebas seleccionadas ni la semántica técnica de COMPLETED/FAILED.

| Recurso | Valor técnico inicial aprobado | Margen de configuración |
| --- | --- | --- |
| SecurityTests por Scan | 4 | 1..10; no recorta el catálogo si no cabe |
| Prompt individual | 4096 bytes UTF-8 | Fijo para catálogo de este incremento |
| Body de request serializado | 65536 bytes UTF-8 | Reducible; máximo 65536 |
| Response de red | 262144 bytes | Reducible; máximo 1048576 |
| responseText antes de redacción | 65536 bytes UTF-8 | Reducible; máximo 262144 |
| Evidencia request+response serializada tras redacción | 131072 bytes por ejecución | Reducible; máximo 524288 |
| Evidencia de un Scan | 524288 bytes | Reducible; máximo 1048576 |
| Tiempo de intento | timeoutMs fijado del target, 1000..60000 ms | No modifica el rango/default 10000 aprobado |
| DNS / conexión | Hasta 2000 / 5000 ms respectivamente | Reducibles, siempre dentro del tiempo de intento |
| Tiempo total de trabajo de Scan | 300000 ms | 60000..600000 ms |
| Redirects / reintentos | 0 / 0 | Fijos |
| Invocaciones globales en esta instancia | Una simultánea, inicios separados al menos 1000 ms | Concurrencia 1 en el MVP; intervalo externo >= 1000 ms, sin ráfagas |
| Headers de response | 32768 bytes, 100 headers, 8192 bytes por línea | Fijos |
| Profundidad JSON de response | 64 niveles | Fija |

El tiempo de intento SHALL incluir validación, DNS, conexión/TLS, envío y lectura/extracción completos, sin reiniciarse por cada fragmento. El presupuesto global SHALL incluir preparación, espera de cuota y todas las pruebas desde Scan.startedAt; agotarlo SHALL abortar nuevas invocaciones y cancelar/cerrar la activa. El tiempo efectivo SHALL ser el menor del timeout del target y presupuesto restante. La lectura SHALL detenerse al detectar exceso, también con chunked o Content-Length ausente/falso. DNS y transporte SHALL usar capacidad acotada, sin crear hilos/tareas ilimitadas ante bloqueo. Una espera de cuota SHALL respetar el presupuesto restante y no mantener transacción PostgreSQL.

requestBody SHALL contener exclusivamente el body JSON preparado, minimizado/redactado de credenciales conocidas; no acredita recepción por el target. responseText SHALL contener únicamente el string seleccionado y redactado, nunca el JSON bruto ni otros campos. La evidencia SHALL omitir headers, URL/query, credenciales, ciphertext, clave, nonce, cookies y dumps de excepciones. requestRedacted/responseRedacted SHALL señalar alteraciones por redacción; otras fugas o datos sensibles del target SHALL conservarse solo dentro de este fragmento limitado, como datos no confiables, sin borrar automáticamente evidencia necesaria. No se garantiza detección universal de secretos arbitrarios/codificados. SHALL NOT registrarse evidencia en logs ni exponerse por la API de este incremento.

Los límites de almacenamiento SHALL contarse después de serializar/redactar, incluidos escapes JSON; requestBody redactado SHALL respetar además el máximo de 65536 bytes del campo persistido. U+0000 en texto extraído SHALL conservarse mediante representación JSON escapada recuperable, sin almacenar NUL literal ni sustitución silenciosa. Bytes no válidos o surrogates aislados SHALL rechazarse. Exceso de request preparado para envío SHALL producir REQUEST_TOO_LARGE sin envío; response de red, RESPONSE_TOO_LARGE; texto o almacenamiento tras redacción, EVIDENCE_LIMIT_EXCEEDED, sin persistir la evidencia excedente. El presupuesto acumulado SHALL comprobarse también antes de cada escritura.

#### Scenario: Response demasiado grande sin longitud fiable
- **WHEN** el target supera el límite técnico configurado de bytes, con el default o con otro valor admitido, mediante streaming chunked o sin longitud
- **THEN** se interrumpe y cierra la lectura con RESPONSE_TOO_LARGE, sin acumular el body completo
- **AND** la ejecución queda FAILED sin evidencia truncada presentada como completa

#### Scenario: Slow response, DNS bloqueado y presupuesto global
- **WHEN** DNS/conexión/lectura exceden su límite, o la espera de cuota agota el presupuesto de Scan
- **THEN** se obtiene DNS_RESOLUTION_FAILED, TARGET_TIMEOUT o SCAN_BUDGET_EXCEEDED según la fase
- **AND** no hay envío tardío, tareas ilimitadas, transacción larga ni nuevos intentos después del límite

#### Scenario: Evidencia limitada y recuperable
- **WHEN** un texto válido incluye U+0000, comillas y Unicode suplementario
- **THEN** su representación persistida permite recuperarlo exactamente salvo redacción explícita
- **AND** el cómputo de límites incluye escapes y no provoca error de PostgreSQL por NUL

### Requirement: STE-10 Continuación individual y resultado final del Scan

**Historia:** Como usuario, quiero conservar los éxitos y distinguir un Scan técnicamente completo de uno con pruebas fallidas.
**Prioridad:** Alta, MVP.
**Dependencias:** SE-05/SE-07/SE-10; STE-02 a STE-09; D-07.

El trabajo real SHALL sustituir el no-op expresamente temporal de SE-10 sin cambiar los cuatro estados, siete campos, admisión, executor y motivos cerrados de Scan. SHALL ejecutar el plan secuencialmente, una petición máxima por test, sin evaluador. Ante un fallo individual de transporte, timeout de intento, HTTP, formato, ruta o tamaño, SHALL persistir FAILED y continuar con el siguiente test mientras haya presupuesto y no exista fallo global. Los códigos individuales SHALL ser DNS_RESOLUTION_FAILED, TARGET_CONNECTION_FAILED, TARGET_TIMEOUT, HTTP_STATUS_ERROR, REQUEST_TOO_LARGE, RESPONSE_TOO_LARGE, RESPONSE_FORMAT_ERROR, RESPONSE_INVALID_JSON, RESPONSE_PATH_NOT_FOUND, RESPONSE_NOT_TEXT y EVIDENCE_LIMIT_EXCEEDED.

TARGET_NOT_AUTHORIZED, TARGET_ADDRESS_BLOCKED, TARGET_TRANSPORT_REJECTED, TARGET_CONFIGURATION_INVALID y CREDENTIAL_UNAVAILABLE SHALL abortar el plan por afectar seguridad/configuración compartida. SCAN_BUDGET_EXCEEDED SHALL abortarlo por consumo global. El test activo SHALL registrar el código correspondiente si puede persistirse; pendientes SHALL quedar FAILED/EXECUTION_ABORTED. Un agotamiento del presupuesto total de evidencia SHALL abortar sin nuevas peticiones. Fallos inesperados SHALL registrarse como EXECUTION_INTERNAL_ERROR sin causa ni entrada y abortar. Un fallo de persistencia SHALL detener todo envío adicional; SHALL NOT confundirse con un fallo individual recuperable.

Scan SHALL terminar COMPLETED exclusivamente si el plan no vacío está íntegramente persistido y todas sus TestExecutions están COMPLETED. Una o todas las pruebas FAILED, o un fallo global, SHALL producir Scan FAILED/SCAN_PROCESSING_FAILED, conservando las ejecuciones exitosas. FAILED con mezcla de resultados SHALL indicar ejecución técnica incompleta, no vulnerabilidad ni ausencia de evidencia. SHALL NOT añadirse PARTIAL ni presentarse un fallo como éxito agregado. La terminalización de Scan y el cierre de cualquier TestExecution activa SHALL confirmarse atómicamente en una transacción corta; si no puede confirmarse, SHALL conservarse el último estado durable y aplicarse el bloqueo/recuperación aprobados en SE-07. La invocación de red SHALL permanecer fuera de transacciones de persistencia.

Los scans no-op históricos SHALL conservarse sin reclasificar ni inventar TestExecutions. Un COMPLETED anterior a este incremento no acredita pruebas; su listado de ejecuciones vacío SHALL permitir reconocer esa ausencia. No se promete entrega exactamente una vez: una caída después del envío y antes del commit puede perder evidencia, pero SHALL NOT causar reenvío automático.

#### Scenario: Cuatro éxitos técnicos
- **WHEN** las cuatro pruebas obtienen JSON/string válidos y sus resultados se confirman
- **THEN** existen cuatro TestExecutions COMPLETED y Scan termina COMPLETED
- **AND** no se declara que el target sea seguro o vulnerable

#### Scenario: Fallo individual y éxitos posteriores
- **WHEN** una prueba falla por timeout o respuesta inválida y las siguientes pueden ejecutarse
- **THEN** se conservan el FAILED y los éxitos posteriores y se intentan todas las restantes
- **AND** Scan termina FAILED con SCAN_PROCESSING_FAILED, sin borrar la evidencia exitosa

#### Scenario: Todos los intentos fallan
- **WHEN** el target no responde a ninguna prueba y se registran los timeouts dentro del presupuesto
- **THEN** cada intento queda FAILED y Scan queda FAILED, nunca COMPLETED por haber terminado el bucle

#### Scenario: Fallo global o persistencia no confirmable
- **WHEN** aparece bloqueo SSRF, credencial inutilizable, presupuesto agotado o fallo de persistencia
- **THEN** no se envían pruebas posteriores y se intenta cerrar pendientes y Scan conforme a la política global
- **AND** si no puede confirmarse la terminalización se bloquean nuevas admisiones/reclamaciones según SE-07 sin afirmar estados inexistentes

### Requirement: STE-11 Persistencia relacional y evolución V4

**Historia:** Como investigador, quiero recuperar cada intento y su definición después de reiniciar sin alterar datos de capacidades aprobadas.
**Prioridad:** Alta, MVP.
**Dependencias:** STE-01 a STE-03/STE-09/STE-10; D-08.

El sistema SHALL conservar definición, pertenencia, estados, fechas, httpStatus, latencyMs, errorReason, metadatos de target y evidencia permitida tras reiniciar. SHALL garantizar FK a Scan y definición de prueba, unicidad por (scan, definición/revisión), códigos/revisiones únicos, categorías cerradas, nulabilidad temporal coherente y límites de evidencia. SHALL conservar las relaciones existentes con findings sin escribir findings ni campos de evaluación/confidence/global_score.

La evolución SHALL realizarse exclusivamente mediante V4, conservando V1/V2/V3 inmutables y sin modificar projects/target_configurations/scans. SHALL NOT eliminar tablas, FKs, columnas futuras o históricos. Si las filas heredadas de test_cases/test_executions no permiten añadir la definición/revisión/estado sin inventar datos, V4 SHALL fallar transaccionalmente preservando el esquema/datos previos; su conversión necesitará autorización y diseño específicos. Los scans históricos sin ejecuciones SHALL seguir siendo válidos.

#### Scenario: Evolución desde baseline sin pruebas heredadas
- **WHEN** se aplica V4 sobre V1–V3 con test_cases/test_executions vacías y scans previos
- **THEN** se dispone del catálogo inicial y las constraints de ejecución conservando los scans y sus campos
- **AND** las referencias originales siguen válidas, sin writes de evaluación o scoring

#### Scenario: Históricos de pruebas incompatibles
- **WHEN** existen definiciones/ejecuciones previas sin revisión/estado verificables
- **THEN** V4 falla como unidad sin asignar códigos, estados, timestamps o resultados ficticios
- **AND** no se borran ni reinterpretan datos o referencias de findings

### Requirement: STE-12 Recuperación sin reenvíos

**Historia:** Como usuario, quiero reconocer intentos interrumpidos y conservar los ya terminados al reiniciar.
**Prioridad:** Alta, MVP de instancia única.
**Dependencias:** SE-08; STE-03/STE-10/STE-11; D-07/D-08.

Antes de habilitar admisión/executor, la recuperación SHALL marcar PENDING/RUNNING de TestExecution como FAILED/EXECUTION_INTERRUPTED, conservar sus fechas/evidencia confirmadas y terminar los scans activos con FAILED/SCAN_INTERRUPTED según SE-08. SHALL conservar las TestExecutions ya terminales, incluso si pertenecen a un Scan interrumpido. Ambas recuperaciones SHALL confirmarse en la misma transacción; su fallo SHALL impedir habilitar procesamiento. Una ejecución activa heredada de un Scan ya terminal SHALL cerrarse también como interrumpida sin modificar ese Scan. SHALL NOT reanudar, repetir requests o generar evidencia ausente. La política SHALL conservar su límite de una instancia.

#### Scenario: Caída entre request y persistencia
- **WHEN** se reinicia con un intento RUNNING que pudo haber llegado al target y otros pendientes
- **THEN** los no terminales quedan FAILED/EXECUTION_INTERRUPTED y Scan activo queda FAILED/SCAN_INTERRUPTED
- **AND** no hay reenvío ni afirmación de conocer la respuesta perdida; los éxitos previos se conservan

#### Scenario: Recuperación no confirmada
- **WHEN** falla el commit conjunto de recuperación
- **THEN** no se habilita admisión/executor ni se anuncia recuperación parcial

### Requirement: STE-13 Consulta mínima de ejecuciones sin evidencia sensible

**Historia:** Como usuario, quiero consultar progreso y fallos técnicos sin exponer prompts o respuestas del target por defecto.
**Prioridad:** Alta para polling/debug MVP.
**Dependencias:** PM-04, SE-06/SE-09; STE-03/STE-09; D-09.

GET `/api/projects/{projectId}/scans/{scanId}/executions` SHALL devolver 200, Cache-Control no-store y el array completo de metadatos confirmados, ordenado por securityTestCode y securityTestRevision. Cada elemento SHALL contener exactamente id, scanId, securityTestCode, securityTestRevision, securityTestName, category, status, startedAt, finishedAt, httpStatus, latencyMs, errorReason, requestRedacted y responseRedacted. El array SHALL ser vacío para un Scan PENDING todavía sin plan, uno fallido antes de prepararlo o uno histórico no-op. La ausencia de plan SHALL NOT interpretarse automáticamente como cero pruebas satisfactorias. Progreso SHALL derivarse del número de filas planificadas y terminales; SHALL NOT añadir porcentajes ni modificar Scan.

La consulta SHALL validar primero ambos UUID, con precedencia INVALID_PROJECT_ID sobre INVALID_SCAN_ID y sin persistencia ante formato inválido. Con UUID válidos SHALL comprobar Project y pertenencia del Scan: PROJECT_NOT_FOUND o SCAN_NOT_FOUND con 404, también para Scan ajeno; un fallo de lectura SHALL producir 500 INTERNAL_ERROR, nunca array vacío o 404. Errores SHALL conservar `{code,message,fieldErrors:[]}` y mensajes en español sin entrada/causa. Un Scan/TestExecution FAILED consultable SHALL devolverse con 200. Leer SHALL NOT consultar target/clave, descifrar, ejecutar HTTP ni modificar fechas. No SHALL existir endpoint individual, de catálogo, de evidencia o Findings en este incremento.

#### Scenario: Polling durante y después del trabajo
- **WHEN** se consulta mientras hay pruebas PENDING/RUNNING y después de terminar
- **THEN** se observan solo estados confirmados y luego todos los resultados terminales en el mismo orden
- **AND** ninguna respuesta incluye requestBody, responseText, prompt, URL, headers, credenciales o blobs

#### Scenario: Aislamiento, UUID y fallo de lectura
- **WHEN** se consultan UUID inválidos, un Project ausente, un Scan ajeno o una lectura fallida
- **THEN** se aplican respectivamente 400, 404 PROJECT_NOT_FOUND, 404 SCAN_NOT_FOUND o 500 INTERNAL_ERROR
- **AND** no hay acceso a persistencia con UUID inválido ni simulación de array vacío por fallo técnico

### Requirement: STE-14 Frontera estricta con evaluación y exposición futura

**Historia:** Como responsable del proyecto, quiero que la evidencia obtenida no se confunda con resultados de seguridad o datos confiables.
**Prioridad:** Alta, límite del incremento.
**Dependencias:** ADR-004 a ADR-007; STE-01 a STE-13.

Este incremento SHALL NOT producir reglas/veredictos de vulnerabilidad, evaluación LLM, confidence, severity, Finding, score, informe o cobertura OWASP acreditada. Una respuesta técnicamente COMPLETED SHALL significar únicamente invocación/extracción/persistencia válidas, nunca vulnerable, no vulnerable o seguro. Los cuatro probes SHALL permitir observar las categorías, sin representar cobertura completa ni demostrar detección o resistencia. LLM10 SHALL documentarse como observación/proxy limitada al output observable del endpoint. Mientras no se observe el consumidor downstream/sink, el sistema SHALL NOT afirmar validación completa de Improper Output Handling, sanitización efectiva del consumidor ni explotación de ese sink. El criterio técnico JSON/path SHALL NOT presentarse como detección de LLM10.

La evidencia SHALL tratarse como contenido no confiable, sin ejecución de HTML/scripts, instrucciones, URLs o herramientas y sin envío a terceros/evaluadores en este incremento. El evaluador posterior SHALL poder recuperarla internamente mediante aplicación/repositorio, sin necesitar un endpoint público de evidencia; su integración pertenece a otro change. La exposición pública futura de evidencia SHALL requerir un change con autorización de acceso, redacción, tratamiento seguro y retención/borrado definidos. Este incremento SHALL limitarse al MVP local con targets propios/autorizados y datos sintéticos; el número/tamaño por Scan está acotado, pero SHALL NOT afirmarse retención temporal ni control de acceso de usuario todavía inexistentes.

#### Scenario: Evidencia adversarial
- **WHEN** el string extraído contiene instrucciones para modificar el scanner, HTML o URLs
- **THEN** se conserva solo como evidencia limitada y no modifica configuración, flujo, instrucciones o sistemas externos
- **AND** no se renderiza ni evalúa semánticamente y no aparece en la API de metadatos
- **AND** el marcado observado por la prueba LLM10 no se presenta como validación del consumidor downstream/sink ni como cobertura completa de la categoría
