## Context

Motivación y alcance: [proposal.md](proposal.md). Requisitos y escenarios: [specs/target-configuration/spec.md](specs/target-configuration/spec.md), TC-01 a TC-12.

La inspección dirigida confirma que `project-management` ya dispone de dominio puro, casos de uso, `ProjectRepositoryPort.findById(UUID)`, adaptadores REST/JPA y composición transaccional. Su contrato utiliza `/api/projects` y errores `{code,message,fieldErrors}`. V1 ya define los cinco campos de Project y conserva la FK de scans; no hay configuración de target implementada. No se revisa ni modifica el estado aprobado de ese change.

`openspec/specs/` todavía está vacío. Se han leído los artefactos del change anterior, alcance/backlog, arquitectura general y decisiones abiertas, ADR-001/ADR-002, modelo de amenazas, contrato del target, OpenAPI y modelo de datos. La política de autorización es un archivo vacío, no una decisión cerrada. El modelo documental 1→N de targets se concreta para el MVP en 0..1 por aprobación expresa del usuario, sin cambiar la gestión de Project.

Decisiones aprobadas en esta conversación:

- Un proyecto admite cero o una configuración, actualizable; múltiples configuraciones son evolución futura.
- JSONPath restringido a propiedades e índices, sin expresiones complejas.
- Cifrado a nivel de aplicación, clave externa a PostgreSQL y ausencia de secretos en respuestas, logs, errores e informes.
- D-CRYPTO cerrado para MVP local: AES-256-GCM, clave externa de 256 bits, nonce aleatorio nuevo y formato persistido versionado y recuperable. AWS queda como evolución futura no bloqueante.
- POST/GET/PUT del recurso único, límites provisionales aceptados, headers/authConfig, conservación condicionada de credenciales, plantilla y JSONPath aprobados.
- timeoutMs omitido toma 10000 ms; rango 1000..60000. Las constantes method/contentType/promptPlaceholder no se persisten.
- Bloqueo de destinos locales/privados por defecto con excepción explícita y controlada local/demo. Transacciones simples, sin ETag, versionado optimista ni mecanismos adicionales de concurrencia.

Estas aprobaciones actualizan las decisiones que todavía figuran como pendientes o más restrictivas en documentación consolidada. Se planifica reconciliar únicamente esas referencias durante la implementación; en esta entrega solo se modifica OpenSpec.

## Goals / Non-Goals

**Goals:**

- Separar target y Project, manteniendo el dominio independiente de Spring, JPA, clientes HTTP, PostgreSQL y proveedores LLM.
- Especificar un recurso único por proyecto y una actualización que preserve secretos solo en condiciones explícitas.
- Definir validaciones locales que no accedan a la red y una frontera clara con la futura ejecución.
- Mantener credenciales fuera del modelo público y de los headers ordinarios.

**Non-Goals:**

- Ejecutar requests, comprobar conectividad, implementar resolución DNS, allowlists de ejecución o redirecciones, o empezar el motor de scans. La excepción local/demo de validación sí pertenece a este change.
- Diseñar pantallas Angular, seleccionar un proveedor de cifrado o introducir dependencias, servicios externos o AWS.
- Añadir ownership, estados de activación, versiones de configuración, selección de múltiples targets o cambios en Project.
- Implementar código, migraciones o documentación consolidada en esta entrega.

## Decisions

### 1. Recurso único con creación y actualización separadas

Se aprueba `POST`, `GET` y `PUT /api/projects/{projectId}/target`. La cardinalidad aprobada se garantiza con FK y unicidad de `project_id`, además de validación de aplicación. La configuración conserva su propio UUID, útil para trazabilidad futura, pero el consumidor no necesita otra ruta ni elegir un target activo. No se añade `active` porque existe como máximo una configuración.

| Operación | Resultado |
| --- | --- |
| POST | `201` y Location tras commit; `409 TARGET_ALREADY_CONFIGURED` si ya existe. |
| GET | `200` con representación segura; `404 TARGET_NOT_CONFIGURED` si Project existe sin target. |
| PUT | `200` y representación segura; conserva identidad y fecha inicial; `404` si no había target. |
| Cualquiera | `400 INVALID_PROJECT_ID` para UUID inválido; `404 PROJECT_NOT_FOUND` si no existe Project; errores de validación o internos según TC-12. |

Alternativa: PUT con upsert. Ahorra un endpoint, pero confunde creación y actualización y complica distinguir una configuración desaparecida de una solicitud nueva. Se propone creación explícita y reemplazo de los datos configurables. No hay PATCH ni borrado en este change.

La aplicación comprueba Project mediante el puerto existente, sin añadir métodos o alterar sus invariantes. La FK cubre la integridad en concurrencia y la restricción única evita dos targets aun si ambas solicitudes observaron inicialmente ausencia. El adaptador traduce la violación de unicidad a conflicto funcional; un fallo técnico nunca se interpreta como ausencia.

### 2. Modelo conceptual y representación pública

```text
TargetConfiguration
- id: UUID
- projectId: UUID
- name: texto opcional
- url: texto validado
- method: POST
- contentType: application/json
- requestTemplate: documento JSON en cadena validada
- promptPlaceholder: {{PROMPT}}
- responsePath: JSONPath restringido validado
- headers: mapa no sensible
- authType: NONE | BEARER_TOKEN | API_KEY_HEADER
- authHeaderName: texto opcional, solo para API_KEY_HEADER
- credential: referencia interna a material cifrado, nunca secreto público
- timeoutMs: entero
- createdAt: instante UTC
- updatedAt: instante UTC
```

`authConfig` es un contrato de entrada/salida diferenciado, no un JSON que se guarde directamente en PostgreSQL. La entrada admite secretValue de un solo uso para guardar/reemplazar; la respuesta contiene únicamente headerName para API key y null en los otros casos, más `hasCredentials` como indicador derivado. No devuelve secreto, máscara, blob, claves, nonce o identificadores criptográficos internos. No se guarda `hasCredentials` como un segundo estado independiente que pueda divergir del material cifrado.

Se conserva name opcional porque ADR-001 prevé un nombre de target y puede resultar útil para describir el endpoint; no se exige ni se usa como identificador. Los tres valores fijos se devuelven como parte del contrato y pueden omitirse al configurar; se derivan de constantes del MVP y no se persisten. No son opciones de protocolos alternativos.

El único secreto entra por authConfig. Los comandos que lo contienen no deben usar registros automáticos/toString que revelen campos ni enviarse completos a logs. La configuración ordinaria y los DTOs de respuesta no contienen el secreto en claro. El texto en claro es inevitable durante recepción y cifrado inicial: esa ventana se minimiza y no autoriza almacenarlo o reutilizarlo. Consultar o preservar una credencial no requiere descifrarla; el descifrado posterior pertenece exclusivamente a la ejecución autorizada y no se implementa aquí.

Alternativa: guardar todos los headers, incluido Authorization, como un mapa. Es más corto, pero duplica secretos, dificulta su redacción y permite exponerlos por GET. Se rechaza en favor de la separación explícita.

### 3. Validaciones locales acotadas

TC-04 a TC-09 y TC-12 contienen el contrato verificable. Las siguientes concreciones están aprobadas; los límites se aceptan provisionalmente sin calibración adicional para el MVP:

| Elemento | Regla aprobada |
| --- | --- |
| name | Opcional, máximo 120 puntos de código tras trim, blanco → null. |
| URL | Máximo 2048 puntos de código tras trim; absoluta HTTP/HTTPS, sin userinfo, fragmento o controles. |
| timeoutMs | Entero entre 1000 y 60000 ms inclusive; omitido → 10000 en POST y PUT. Null explícito inválido. |
| requestTemplate | Cadena que contiene un objeto JSON, máximo 32768 bytes UTF-8; no se modifica su contenido con trim/interpolación. |
| responsePath | Trim exterior, máximo 1024 puntos de código y 32 segmentos. |
| headers | Máximo 20, nombres hasta 128 caracteres, valores hasta 4096 bytes y total de nombres/valores hasta 16384 bytes UTF-8. |
| secretValue | Máximo 8192 bytes UTF-8, no vacío/blanco ni controles ni surrogates UTF-16 aislados; se aceptan pares válidos. No se recorta ni altera. Bearer además sin espacios. |

Corrección aprobada del finding de conservación exacta: `TargetRules.secret`, reutilizado por `TargetCommand`, rechaza surrogates UTF-16 aislados antes de la conversión UTF-8, cifrado y escritura. Devuelve `400 VALIDATION_ERROR` e `INVALID_CHARACTER` para `authConfig.secretValue`, sin valores rechazados. Los pares válidos se conservan exactamente; un PUT inválido no alcanza el caso de uso ni modifica el estado anterior. No cambia AES-GCM ni amplía la regla a otros campos.

Se rechaza U+0000 en entradas textuales y en cadenas decodificadas de template/JSONPath antes de normalizar o persistir, coherentemente con la limitación ya descubierta en Project. No se amplían ni cambian los requisitos de Project. Los valores de headers/secretos prohíben además controles, para evitar inyección de cabeceras. Los errores utilizan rutas contractuales como `headers`, `requestTemplate`, `authConfig.secretValue` o `authConfig.headerName`, sin incorporar claves o datos aportados por el usuario.

La entrada JSON se analiza de forma estricta, incluidas claves duplicadas, sin coerción automática de números/booleanos a texto. El conteo Unicode no debe confundirse con longitud UTF-16. Las validaciones estructurales del JSON están en un adaptador detrás de un puerto de validación; ningún tipo Jackson se filtra al dominio. Los parsers, límites y DTOs siguen las dependencias/convenios ya presentes, sin añadir librerías automáticamente.

### 4. Plantilla como datos y ruta restringida

Se valida el documento, sus claves y sus cadenas decodificadas, no solo el número de coincidencias sobre texto bruto. Exactamente un token ocupa una cadena completa y nunca una clave; esto evita sustituciones ambiguas y permite insertar el futuro prompt como valor JSON con escape. Otros textos no se evalúan como plantillas, variables o scripts. No se materializan payloads ni se implementa el renderizado para requests reales en este change.

El subconjunto JSONPath de TC-07 admite `$`, `.identifier`, propiedades entre corchetes con cadena JSON entre comillas dobles e índices no negativos. Puede expresarse como:

```text
path = "$" segment*
segment = "." identifier | "[" jsonQuotedProperty "]" | "[" index "]"
identifier = [A-Za-z_][A-Za-z0-9_]*
index = 0 | [1-9][0-9]*    (máximo 2147483647)
```

Las propiedades entre comillas soportan nombres con espacios, Unicode o puntuación sin abrir expresiones complejas. Su validación decodifica la cadena JSON, incluida la comprobación de U+0000. No se soportan comillas simples, segmentos vacíos por punto, índices negativos, uniones, slices, filtros, scripts, comodines ni descenso recursivo. No se utiliza eval ni un motor de expresiones general. La validación puede producir una secuencia tipada de segmentos, sin evaluar una respuesta real.

Alternativa: JSON Pointer. Simplifica la gramática pero no coincide con la notación elegida; queda descartada para este cambio por la aprobación expresa de JSONPath. Un motor JSONPath completo introduciría funcionalidades prohibidas o necesitaría deshabilitarlas exhaustivamente.

La futura extracción necesita un único texto. Ausencia, índice fuera de rango o objeto/número/null son errores de extracción, nunca motivo para evaluar toda la respuesta. Estos criterios delimitan la integración futura; las tareas actuales solo cubren validar sintaxis, no implementan la ejecución o evaluación.

### 5. URL: configuración no equivale a permiso de ejecución

Configuración realiza validación sintáctica local y bloquea por defecto localhost, sus subdominios/variantes con punto final y literales identificables de loopback/redes privadas, sin DNS. La excepción aprobada local/demo permite destinos expresamente declarados mediante configuración externa del entorno local, desactivada por defecto y fuera del DTO y tabla de target. Su activación no permite todas las redes privadas ni metadatos cloud, no altera validaciones estructurales y no se puede solicitar desde POST/PUT. Así se podrá configurar demo-vulnerable sin convertir el formulario en un bypass SSRF. El nombre concreto de la propiedad de despliegue es un detalle de implementación, no una nueva capability.

No se resuelven hosts ni se conecta. Que una URL se guarde no la convierte en destino permitido: los controles de red completos se aplicarán obligatoriamente justo antes y durante la ejecución futura. El reconocimiento completo de variantes IP, resolución DNS y direcciones efectivas pertenece a ese change; no se atribuye protección SSRF completa al validador local.

| Configuración, este change | Ejecución futura, fuera de este change |
| --- | --- |
| Estructura URL, esquema, host, puerto, userinfo/fragmento y controles. | Resolver DNS y validar cada dirección efectiva y destino permitido. |
| Bloqueo por defecto de localhost y literales loopback/privados; excepción local/demo externa y acotada. | Bloquear por defecto rangos privados, interfaces locales, link-local/metadatos y variantes/resoluciones que el control local no cubre; aplicar exclusivamente excepciones autorizadas de demo. |
| Validar template/ruta/headers/timeout sin acceso a red. | Aplicar límites efectivos, política de redirecciones, rebinding y transporte HTTP. |
| Exigir canales separados para datos ordinarios y credenciales. | Evitar enviar secretos a destinos/transporte no autorizados. |

La excepción controlada local/demo queda aprobada con los límites anteriores. La política completa de autorización, allowlist efectiva y transporte de credenciales se concreta en ejecución, sin bloquear configurar targets aquí. La implementación de configuración no incluirá cliente HTTP ni comprobación de conectividad. URL/headers/template son campos no sensibles por contrato; no se promete un detector universal de secretos pegados arbitrariamente en texto libre.

### 6. Actualización y seguridad de credenciales

El PUT reemplaza todos los datos configurables; name/headers omitidos toman null/{} y timeoutMs omitido toma 10000, no su valor anterior. Los campos obligatorios deben reenviarse. La única excepción es el secreto write-only: su omisión conserva el anterior exclusivamente bajo TC-10. Un null explícito no significa conservar. Cambiar a NONE elimina el blob; cambiar tipo, header de API key u origen exige secreto nuevo.

La comparación de origen normaliza esquema/host, interpreta puertos por defecto 80/443 y compara la representación canónica de literales IP sin DNS; cambiar solo la ruta no cambia el origen. La operación verifica estas condiciones contra la configuración persistida dentro de su frontera transaccional, no contra una copia desactualizada enviada por el cliente. Proporcionar un secreto nuevo al cambiar origen constituye una elección explícita de configuración, no una autorización de red.

La actualización preserva el blob sin descifrar cuando no se cambia credencial. Si se reemplaza, primero se obtiene el nuevo material cifrado y después se persiste la configuración completa atómicamente. Si falla cifrado/commit se conserva la anterior. Un valor de reloj no debe hacer retroceder updatedAt; se usa un instante UTC compatible con microsegundos y no anterior a los instantes persistidos. No se altera updatedAt de Project.

Alternativas: exigir secreto en todo PUT dificulta editar sin conocerlo; mantenerlo ante cualquier cambio puede asociarlo silenciosamente a otro destino. Se aprueba conservación condicionada. Se usan transacciones simples con lectura del estado persistido y escritura atómica, sin PATCH, historial, ETag, versionado optimista, bloqueos explícitos de fila ni mecanismos adicionales de concurrencia. En actualizaciones concurrentes puede prevalecer la última escritura; no se promete detectar cambios perdidos. La FK/UNIQUE sigue garantizando cardinalidad incluso ante creaciones concurrentes.

### 7. Casos de uso y puertos

```text
Angular, consumidor futuro
  → Adaptador REST de configuración
  → ConfigureTargetUseCase / GetTargetConfigurationUseCase / UpdateTargetConfigurationUseCase
  → Dominio de target y puertos de aplicación
  → Adaptadores de validación / cifrado / persistencia
  → PostgreSQL (sin acceso al target)
```

| Elemento | Responsabilidad |
| --- | --- |
| Dominio de target | Invariantes, constantes, metadatos de autenticación y valores propios, sin Spring/JPA/HTTP/Jackson/PostgreSQL. |
| ConfigureTargetUseCase | Comprobar Project y ausencia, validar entrada, generar identidad/fechas, solicitar cifrado si procede y guardar. |
| GetTargetConfigurationUseCase | Distinguir Project inexistente y target ausente, recuperar solo datos de respuesta y presencia de credencial. |
| UpdateTargetConfigurationUseCase | Validar reemplazo, aplicar TC-10 con el estado vigente y persistir atómicamente sin modificar Project. |
| ProjectRepositoryPort existente | Consultar existencia mediante findById; no necesita cambiarse. |
| TargetConfigurationRepositoryPort | Insertar, recuperar y reemplazar configuración, con asociación/unicidad, usando tipos propios. |
| ConfigurationValidationPort | Validar documento JSON/plantilla/ruta sin introducir tipos de librería en dominio. |
| CredentialEncryptionPort | Convertir secreto efímero en material cifrado opaco; no necesita operación de descifrado en este change. |
| Adaptadores/composición | REST/DTOs, validación JSON y política local/demo externa, AES-256-GCM con clave externa, JPA/mapeos y transacciones simples. |

Las flechas describen ejecución; las dependencias de código apuntan hacia aplicación/dominio. Los adaptadores traducen errores técnicos al lenguaje de aplicación. Se conserva la forma de error existente sin refactorizar o cambiar los handlers de Project. La futura ampliación del enum OpenAPI para errores/campos de target no modifica las respuestas de sus endpoints.

### 8. Persistencia separada y mínima

Tabla conceptual aprobada `target_configurations`:

| Columna | Tipo propuesto | Restricciones |
| --- | --- | --- |
| id | UUID | PK, generado por aplicación. |
| project_id | UUID | NOT NULL, FK projects(id), UNIQUE; sin borrado en cascada anticipado. |
| name | VARCHAR(120) | Nullable. |
| url | VARCHAR(2048) | NOT NULL. |
| request_template | TEXT | NOT NULL, límites/validación en aplicación. |
| response_path | VARCHAR(1024) | NOT NULL. |
| headers | JSONB | NOT NULL, objeto no sensible, vacío por defecto. |
| auth_type | VARCHAR(20) | NOT NULL, tres valores MVP. |
| auth_header_name | VARCHAR(128) | Solo no nulo para API_KEY_HEADER. |
| credential_ciphertext | BYTEA | Nullable para NONE; envelope versionado con nonce, ciphertext y etiqueta GCM necesarios para recuperar/verificar el secreto; nunca clave o plaintext. |
| timeout_ms | INTEGER | NOT NULL, 1000..60000; la aplicación aplica 10000 cuando se omite. |
| created_at | TIMESTAMPTZ(6) | NOT NULL. |
| updated_at | TIMESTAMPTZ(6) | NOT NULL, no anterior a created_at. |

Los checks cubren intervalo de timeout y consistencia auth_type/header/blob: NONE no tiene header ni blob; BEARER_TOKEN requiere blob y no header; API_KEY_HEADER requiere ambos. method/contentType/promptPlaceholder se derivan de constantes del MVP: no hay columnas ni checks para ellas. Las reglas textuales complejas permanecen en validadores de aplicación/dominio. No se guardan secretValue, authConfig completo, hasCredentials, URL del target o app_type en projects. No se añaden tablas, columnas o relaciones en scans.

Se reutiliza JPA solo en el adaptador, con entidad separada del dominio. Se requiere inserción/actualización transaccional y traducción de la UNIQUE a TARGET_ALREADY_CONFIGURED. Una nueva migración, previsiblemente V2 si sigue disponible, incorporará esta tabla cuando se autorice implementar. V1 permanece intacta; no se ejecutan migraciones, repairs, recreaciones o borrados automáticos.

### 9. D-CRYPTO: cerrado para el MVP local

Decisión aprobada: AES-256-GCM a nivel de aplicación, con clave de 256 bits suministrada externamente mediante variable de entorno/configuración segura. La clave nunca se guarda en PostgreSQL ni en el repositorio, no se devuelve ni se registra. Debe conservarse de forma segura fuera de la base de datos para recuperar credenciales tras reinicio; perderla impide descifrar los datos. No hay claves por defecto ni generación automática de una clave efímera que invalide datos existentes.

Cada cifrado genera un nonce aleatorio nuevo mediante aleatoriedad criptográfica. El adaptador guarda un envelope con versión de formato, nonce, ciphertext y etiqueta de autenticación GCM; la versión identifica los parámetros y codificación necesarios para descifrar/verificar. El layout binario, codificación de la clave externa y parámetros estándar de nonce/tag se concretan y documentan como detalles internos del adaptador durante implementación, usando las capacidades Java existentes y sin cambiar el contrato de dominio ni añadir dependencias. Versiones desconocidas, datos alterados, clave inválida/ausente o fallos criptográficos se rechazan con error sanitizado, nunca con plaintext o degradación a NONE.

Los secretos solo se descifran en memoria cuando sean necesarios. GET y conservación en PUT no descifran; no se añade ahora ejecución HTTP ni descifrado de producción para scans. Las pruebas de recuperación pueden verificar en memoria el formato con datos/claves sintéticos, sin ampliar el puerto público de configuración. Una prueba con dobles no acredita el cifrado real.

Alternativas consideradas: ChaCha20-Poly1305 y gestores externos. AES-256-GCM queda seleccionado para el MVP local por aprobación expresa; KMS/Secrets Manager siguen como evolución AWS para provisión/gestión de claves sin modificar el contrato de dominio. La rotación avanzada y recuperación operativa de AWS no bloquean este change. D-CRYPTO deja de ser puerta de decisión y no autoriza cambios de dependencias o lectura de claves reales.

## Risks / Trade-offs

- Configuración sintácticamente válida confundida con target autorizado → No devolver estado de conectividad/seguridad; mantener explícita la validación de red en ejecución futura.
- Secretos en comandos, errores o logs → Tipos/DTOs separados, salida segura y mensajes sin entrada; comprobar todas las rutas de error con marcadores sintéticos.
- Headers personalizados usados indebidamente para secretos → Canales de autenticación separados, bloqueo de nombres conocidos/reservados y contrato de headers no sensibles; no prometer detección universal.
- Credencial reutilizada después de cambiar destino → Requerir secreto nuevo ante cambio de origen, tipo o header, dentro de la transacción.
- JSONPath completo ejecutado por accidente → Parser de gramática cerrada, rechazo explícito de expresiones complejas, sin eval ni ejecución sobre respuestas.
- Creaciones concurrentes → UNIQUE por Project y transacciones; actualizar usa última escritura sin detección de cambios perdidos, limitación aceptada del MVP simple.
- Pérdida de clave externa o nonce reutilizado → Conservar la clave fuera de PostgreSQL/repositorio, generar nonce criptográfico nuevo y verificar integridad/recuperación con claves sintéticas.
- Límites provisionales que no cubran algún target → Se aceptan sin calibración adicional para el MVP; revisar en una evolución si hay evidencia, sin ampliar protocolos ni ejecución aquí.
- Excepción local/demo demasiado amplia → Desactivada por defecto, externa a la API, limitada a destinos declarados y sin permitir metadatos; los controles de ejecución siguen siendo obligatorios.

## Migration Plan

Solo para una implementación posterior explícitamente autorizada:

1. Partir de las decisiones aprobadas, incluido D-CRYPTO cerrado localmente; no hay decisiones bloqueantes pendientes para este change.
2. Reflejar el contrato y modelo aprobados en OpenAPI/target-contract/modelo-datos, concretando 0..1 para el MVP y manteniendo N como evolución futura. Actualizar únicamente los documentos afectados por las decisiones ya aprobadas o que se aprueben entonces.
3. Crear la siguiente migración Flyway disponible para target_configurations; no volver a modificar V1. No ejecutar migraciones sin petición expresa.
4. Incorporar validadores, dominio, casos de uso, cifrado aprobado, adaptadores y composición. Preparar pruebas sin ejecutarlas automáticamente.
5. Verificar con autorización las garantías de persistencia, cifrado, errores y concurrencia; no confundir validación OpenSpec con comprobación de la aplicación.

No se incluye despliegue. Una reversión de aplicación debe conservar tabla y credenciales; no se prescribe migración destructiva ni borrado de datos. La misma clave externa debe conservarse de forma segura para recuperar los datos; nunca se sustituye automáticamente durante un rollback.

## Fuera del MVP / Evolución futura

Múltiples configuraciones por proyecto, selección activa, historial/versionado y rotación avanzada de credenciales. Nuevos protocolos y autenticaciones requieren otro cambio/decisión; AWS sigue fuera. La ejecución real y sus controles DNS/SSRF se abordarán en su change correspondiente, sin iniciarlo aquí.
