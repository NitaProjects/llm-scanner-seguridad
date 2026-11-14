# Contrato del target MVP

El change `target-configuration` (TC-01 a TC-12) configura un único target por Project sin ejecutar HTTP, resolver DNS, construir payloads ni extraer respuestas. El contrato interno completo se documenta en [OpenAPI](openapi.yaml).

## Recurso interno

POST, GET y PUT `/api/projects/{projectId}/target`. POST devuelve 201 y Location relativo tras commit; GET/PUT devuelven 200. Project inexistente produce PROJECT_NOT_FOUND, target ausente TARGET_NOT_CONFIGURED y segunda creación TARGET_ALREADY_CONFIGURED (409), también en concurrencia. PUT no hace upsert.

La rama REST_LEGACY (también cuando se omite adapterType) exige url, requestTemplate, responsePath y authType. name es opcional (120 puntos de código tras trim; blanco/null/omisión → null); headers omitido → {}; timeoutMs omitido → 10000 en POST y PUT, con rango 1000..60000 y null inválido. method=POST, contentType=application/json y promptPlaceholder={{PROMPT}} son constantes públicas, opcionales en entrada y no persistidas.

Las fechas UTC tienen precisión de microsegundos y son iguales al crear. PUT conserva id/projectId/createdAt y no hace retroceder updatedAt. Nunca cambia Project.

## Validaciones locales

- URL absoluta HTTP/HTTPS, hasta 2048 puntos de código tras trim, sin userinfo, fragmento, espacios interiores ni controles, con puerto válido. Se bloquean localhost, loopback y literales privados por defecto.
- Excepción demo: solo perfil local más TARGET_LOCAL_DEMO_ENABLED=true y orígenes declarados en TARGET_LOCAL_DEMO_ORIGINS (lista separada por comas), comparados por esquema/host/puerto efectivo. No admite metadatos/link-local ni redes completas; no se activa desde la API.
- requestTemplate es cadena JSON objeto, hasta 32768 bytes UTF-8, sin claves duplicadas y exactamente un token {{PROMPT}} decodificado como valor completo de cadena, nunca clave/fragmento. No se aplica trim ni sustitución.
- responsePath tiene trim, hasta 1024 puntos de código y 32 segmentos: raíz $, .identifier ASCII, propiedades ["cadena JSON no vacía"] e índices [0]..[2147483647], sin ceros iniciales, filtros, scripts, comodines, uniones, slices ni descenso recursivo.
- Headers: hasta 20; nombres HTTP token ASCII de hasta 128 caracteres; valores hasta 4096 bytes UTF-8 y total nombres/valores hasta 16384 bytes. Sin controles U+0000..U+001F/U+007F, duplicados por caso ni colisiones con autenticación. Se prohíben Authorization, Proxy-Authorization, Cookie, Set-Cookie, X-API-Key, API-Key, Host, Content-Length, Content-Type, Connection, Transfer-Encoding, TE, Trailer, Upgrade, Keep-Alive, Proxy-Connection, Forwarded y prefijo X-Forwarded-.
- U+0000 se rechaza antes de normalizar, también en claves/cadenas JSON decodificadas y rutas. No se elimina ni sustituye. JSON estricto sin coerción, claves duplicadas ni campos desconocidos/generados.

`security-test-execution` realiza POST/JSON mediante HttpClient 5, inserta el prompt como nodo string completo y extrae exactamente un texto con la gramática anterior, incluido vacío. Exige response application/json o application/*+json y UTF-8 estricto, sin duplicados, múltiples documentos ni profundidad superior a 64. No sigue redirects, reintenta ni acepta compresión. Un JSON/path/tipo/tamaño inválido es fallo técnico individual; no significa vulnerabilidad. Guardar la configuración sigue sin autorizar tráfico: TARGET_ALLOWED_ORIGINS declara orígenes públicos exactos y la excepción demo conserva perfil local, flag y lista explícita. Todas las IPs efectivas se validan antes de conectar a una sola, sin segunda resolución ni fallback. Credenciales sobre HTTP público se rechazan; solo HTTP demo explícito puede autenticarse.

Defaults técnicos externos: request 64 KiB, response 256 KiB, texto 64 KiB, evidencia serializada 128 KiB/ejecución y 512 KiB/Scan, Scan 300 s, una invocación simultánea y separación >= 1 s. Los márgenes están en STE-09. El timeout del target conserva 1000..60000 ms. El snapshot se fija al preparar el plan; PUT no altera pruebas posteriores de ese Scan. GET executions publica únicamente metadatos técnicos y nunca evidencia.

Una credencial que no pueda representarse íntegramente en un valor de header HTTP se considera no utilizable (CREDENTIAL_UNAVAILABLE); no se envía con sustituciones de caracteres. Esto no modifica la validación/cifrado UTF-8 del recurso de configuración.

## Autenticación y secretos

NONE exige authConfig omitido/null. BEARER_TOKEN requiere authConfig.secretValue y prohíbe headerName. API_KEY_HEADER exige headerName y secretValue; X-API-Key/API-Key son válidos aquí, pero se prohíben nombres sensibles de sesión/Authorization y de transporte/proxy.

secretValue es write-only: obligatorio al crear con autenticación, hasta 8192 bytes UTF-8, no vacío/blanco ni controles; se conserva exactamente sin trim. Bearer no admite espacios. Se rechazan surrogates UTF-16 altos o bajos aislados antes de cifrar o escribir: 400 VALIDATION_ERROR e INVALID_CHARACTER para authConfig.secretValue, sin reflejar el valor. Los pares válidos se aceptan sin alterar el carácter suplementario. Un PUT inválido conserva configuración y credencial anteriores. No se admiten credenciales por URL, plantilla o headers ordinarios; no se promete detectar automáticamente cualquier secreto arbitrario en texto libre.

GET nunca descifra ni consulta la clave. Solo se devuelve authType, hasCredentials y authConfig con headerName para API_KEY_HEADER (null para otros tipos). Nunca secreto, máscara, ciphertext, clave, nonce ni metadatos criptográficos.

PUT reemplaza campos ordinarios; omitir secretValue conserva el blob solo manteniendo origen URL normalizado, authType y headerName exacto, con authConfig presente ({} para Bearer). Cambiar cualquiera de esos elementos exige secreto nuevo. null/vacío no conserva. NONE elimina la credencial en la misma transacción.

AES-256-GCM y el envelope versionado se describen en [ADR-002](../02-arquitectura/decisiones/ADR-002-autenticacion-target-credenciales.md). La clave externa Base64 de 256 bits se suministra mediante TARGET_CREDENTIAL_ENCRYPTION_KEY, sin valor por defecto. Ausencia/formato inválido impide cifrar con 500 INTERNAL_ERROR y sin cambios parciales. Consulta/conservación no necesitan clave. No se registran entradas, secretos ni causas internas; no habilitar logs de SQL/bindings/cuerpos para este flujo.

## Errores

Forma {code,message,fieldErrors}, mensajes en español sin valores rechazados. 400: INVALID_REQUEST (estructura, claves duplicadas/desconocidas), INVALID_PROJECT_ID o VALIDATION_ERROR. 404: PROJECT_NOT_FOUND/TARGET_NOT_CONFIGURED. 409: TARGET_ALREADY_CONFIGURED, TARGET_TYPE_REQUIRED o TARGET_TYPE_CHANGED. 500: INTERNAL_ERROR, sin SQL, conexión ni detalles criptográficos. fieldErrors solo se usa en validación, con rutas contractuales y REQUIRED, INVALID_TYPE, TOO_LONG, INVALID_FORMAT, INVALID_CHARACTER, OUT_OF_RANGE, NOT_ALLOWED o CONFLICT.

## Fuera del MVP / Evolución futura

Múltiples targets, historial, borrado, protocolos distintos de REST/POST/JSON, OAuth2, mTLS, cookies/sesión, AWS y rotación avanzada requieren otros changes. La concurrencia del mismo tipo sigue sin versionado general; las conversiones se protegen mediante el UPDATE condicional descrito abajo. Evidencia pública, autenticación de usuario y retención/borrado requieren un contrato posterior antes de datos reales.

## Extensión OpenAI-compatible — add-openai-compatible-adapter

POST/GET/PUT mantienen la ruta. `adapterType=OPENAI_COMPATIBLE` exige `endpoint`
completo, `model` (trim, 1..200 puntos de código, sin controles/placeholder),
`authType=BEARER_TOKEN`, `authConfig` y timeout 1000..60000/default 10000.
`systemPrompt` es opcional/null, o texto no blanco sin trim de hasta 16 KiB UTF-8.
Solo textos sintéticos/no sensibles. No admite requestTemplate/responsePath/headers,
parámetros de modelo adicionales ni descubrimiento remoto. La cota del documento base
generado con el prompt reservado es 32 KiB. La request efectiva conserva límites v1.

GET chat devuelve id/projectId/name/adapterType/endpoint/model/systemPrompt/authType,
authConfig=null/hasCredentials/timeoutMs/createdAt/updatedAt; no plantilla/ruta/headers
ni token. GET legacy conserva exactamente los 16 campos anteriores. Conservar credencial
requiere el mismo origen y mecanismo; cambiar origen requiere token nuevo.

PUT chat exige discriminador explícito. Sin él devuelve 409 TARGET_TYPE_REQUIRED.
El repositorio condiciona TODAS las actualizaciones por id/project_id/tipo esperado:
REST_LEGACY si falta tipo; tipo leído si es explícito. Una conversión concurrente
produce cero filas y 409 TARGET_TYPE_REQUIRED o TARGET_TYPE_CHANGED respectivamente,
sin retry ni escritura parcial de configuración, NULLs, credencial o fecha. La entidad
managed no se modifica por dirty checking y se descarta el contexto tras el bulk UPDATE.

RoutingTargetAdapter selecciona legacy o OpenAiCompatibleTargetAdapter. Este último
genera model/messages=[system opcional,user con SecurityTest.prompt]/stream=false solo
en memoria y usa la misma instancia de transporte HTTP seguro. Extrae estrictamente
una choices[0].message.content textual con finish_reason=stop; role/index presentes
assistant/0 y sin herramientas. No guarda el documento como configuración.

401/403/404/429/5xx conservan HTTP_STATUS_ERROR/status sin cuerpo remoto ni retry.
JSON vacío/inválido: RESPONSE_INVALID_JSON; choices vacío o ruta ausente:
RESPONSE_PATH_NOT_FOUND; estructura no soportada, múltiples elecciones, tools o final
incompleto: RESPONSE_FORMAT_ERROR; content null/no textual: RESPONSE_NOT_TEXT.
Texto vacío/whitespace con stop conserva éxito técnico. Timeout, SSRF, TLS, límites,
autorización previa a descifrado y redacción siguen siendo los existentes.

El formulario confirma conversiones y muestra los campos propios de cada rama.
Integración chat; criterios de evaluación v1 limitados a escenarios sintéticos
autorizados. Sin contexto/corpus aplicable, cuatro inconclusos y score 0 no prueban
seguridad. No hay habilitación automática del evaluator. La aceptación Groq/OpenRouter
pertenece a una fase posterior autorizada; no se ejecuta en los tests normales.
### Respuestas SSE y JSONL por binding operativo

`TARGET_STREAM_BINDINGS_FILE` señala un manifiesto JSON local opcional. El worker
lo valida y fija el binding durante la preparación del Scan. Cada entrada debe
coincidir con `projectId`, `targetConfigurationId` y `targetUpdatedAt` exactos de
un target `REST_LEGACY`. La ausencia de un binding conserva el flujo habitual.
Para retirar un binding, quitar su entrada o desconfigurar el fichero. Un fichero
configurado ilegible, una entrada ambigua o un vínculo desactualizado para ese
target bloquean la preparación antes de red.

```json
{
  "schemaVersion": 1,
  "bindings": [{
    "projectId": "10000000-0000-4000-8000-000000000001",
    "targetConfigurationId": "10000000-0000-4000-8000-000000000002",
    "targetUpdatedAt": "2026-11-14T09:00:00Z",
    "mediaType": "text/event-stream",
    "contentEvent": "content",
    "errorEvent": "error",
    "terminalEvent": "end",
    "textPath": "$.text",
    "mode": "DELTA"
  }]
}
```

Para JSONL, elegir `application/x-ndjson` o `application/jsonl`. Los frames son
objetos por línea. Para discriminar eventos JSONL declarar `eventPath`, por
ejemplo `$.type`. En SSE se usa el campo `event` y se unen los campos `data` por
salto de línea. `textPath` emplea los selectores restringidos de `responsePath`.
`DELTA` concatena y `SNAPSHOT` sustituye el texto anterior. `contentEvent` y
`errorEvent` son opcionales. Se declara exactamente uno entre `terminalEvent`
y `terminalLiteral`. Este último compara el `data` SSE completo o una línea
JSONL que sea un objeto válido, sin el delimitador.

El binding fija un único `Accept` con su media type y reemplaza cualquier valor
ordinario. El nombre del header de credencial y los nombres declarados como
sensibles o reservados en `reservedHeaders` no pueden reclamar `Accept`, sin
distinción de mayúsculas. El manifiesto admite solo nombres en esa lista. La
colisión se rechaza antes de DNS, descifrado y envío.

La petición usa el POST JSON, credencial, autorización y presupuesto existentes.
Aplican el menor plazo entre timeout del target y presupuesto restante, las cotas
efectivas de entidad/texto/evidencia, 32 KiB por frame o línea y 1 024 unidades
lógicas. Los heartbeats y registros ignorados cuentan. Solo una respuesta
ensamblada tras terminal y redactada llega a evaluación. Los contadores, bytes,
duración, terminal y error quedan en diagnóstico técnico DEBUG. Los frames
crudos no se guardan en el resultado.

### Sesión efímera de target

`TARGET_SESSION_BINDINGS_FILE` indica un manifiesto local opcional con
`schemaVersion: 1` y `bindings`. El worker fija el binding para el Scan por
`projectId`, `targetConfigurationId` y `targetUpdatedAt` exactos de un target
REST_LEGACY. Un mismo target puede tener un binding de sesión o de streaming.
Cada prueba abre un contexto nuevo. Ejecuta un bootstrap JSON GET o POST,
seguido del POST de prompt con el template del target. Ambas peticiones
comparten el timeout del target y el presupuesto restante del Scan.

Ejemplo de entrada en `bindings`:

```json
{
  "projectId": "10000000-0000-4000-8000-000000000001",
  "targetConfigurationId": "10000000-0000-4000-8000-000000000002",
  "targetUpdatedAt": "2026-11-14T09:00:00Z",
  "bootstrap": {"method": "GET", "path": "/bootstrap"},
  "promptPath": "/chats/{{chatId}}/sessions/{{sessionId}}",
  "captures": [
    {"name": "chatId", "source": "JSON", "selector": "$.chatId", "type": "STRING", "required": true, "sensitive": false, "maxBytes": 256},
    {"name": "sessionId", "source": "JSON", "selector": "$.session.id", "type": "STRING", "required": true, "sensitive": false, "maxBytes": 256}
  ]
}
```

El POST de bootstrap admite un objeto JSON estático en `body`. Si falta
`promptPath`, el POST final usa la URL configurada. Los slots ocupan un segmento
completo de la ruta o un valor string completo del template, por ejemplo
`"session":"{{sessionId}}"`. Se sustituyen una vez como segmentos UTF-8
codificados o nodos JSON tipados. El scanner rechaza variables ausentes,
traversal, delimitadores y percent encoding en segmentos.

Cada binding admite hasta 16 capturas de hasta 4 KiB UTF-8 por valor. Los
nombres empiezan por letra ASCII, admiten letras y dígitos y tienen hasta 32
caracteres. Los nombres reservados y duplicados se rechazan sin distinguir
mayúsculas. Las fuentes son JSON con selector restringido y tipo STRING,
INTEGER o BOOLEAN, HEADER con nombre explícito y COOKIE con nombre explícito
en Set-Cookie. HEADER y COOKIE requieren STRING y sensibilidad activa. Sus
valores, origen y atributos de cookie quedan en memoria. Los valores sensibles
se excluyen de los slots, se redactan en la evidencia final y no se reenvían
como headers o cookies.

El contexto se elimina al completar, fallar o cancelar la prueba. La evidencia
persistida corresponde al POST final y contiene su estado HTTP y texto saneado.
Los IDs no sensibles usados en el cuerpo final pueden aparecer en esa evidencia.
El bootstrap queda fuera. La limpieza de sesiones remotas depende del TTL
acordado con el target.

### Workflow HTTP v1

El operador puede fijar un plan con `TARGET_WORKFLOW_BINDINGS_FILE`. El archivo
JSON tiene `schemaVersion: 1` y una lista `bindings`. Cada entrada vincula
`projectId`, `targetConfigurationId` y `targetUpdatedAt` exactos a un target
REST_LEGACY. `workflowId` identifica el plan y `revision` es un entero positivo.
El workflow se selecciona al preparar el Scan. Es incompatible con otros
bindings de sesión o streaming para el mismo target.

Ejemplo de entrada en `bindings`:

```json
{
  "projectId": "10000000-0000-4000-8000-000000000001",
  "targetConfigurationId": "10000000-0000-4000-8000-000000000002",
  "targetUpdatedAt": "2026-11-14T09:00:00Z",
  "workflowId": "chat-session",
  "revision": 1,
  "steps": [
    {
      "id": "chat", "method": "POST", "path": "/api/chats",
      "body": {}, "dependsOn": [], "response": {"type": "JSON"},
      "captures": [
        {"name": "chatId", "source": "JSON", "selector": "$.chatId", "type": "STRING", "required": true, "sensitive": false, "maxBytes": 256}
      ]
    },
    {
      "id": "session", "method": "POST", "path": "/api/chats/{{chatId}}/sessions",
      "body": {}, "dependsOn": ["chat"], "response": {"type": "JSON"},
      "captures": [
        {"name": "sessionId", "source": "JSON", "selector": "$.sessionId", "type": "STRING", "required": true, "sensitive": false, "maxBytes": 256}
      ]
    },
    {
      "id": "answer", "method": "POST", "path": "/api/chats/{{chatId}}/sessions/{{sessionId}}/answer",
      "body": {"prompt": "{{PROMPT}}"}, "dependsOn": ["chat", "session"],
      "response": {
        "type": "SSE",
        "stream": {"mediaType": "text/event-stream", "mode": "DELTA", "textPath": "$.delta", "terminalLiteral": "[DONE]"}
      }
    }
  ]
}
```

Los pasos intermedios GET sin body y POST con objeto JSON aceptan únicamente
respuesta JSON completa. Las capturas se limitan a escalares JSON y admiten
selectores sobre arrays raíz, por ejemplo `$[0].id`. Los slots
y las guardas usan capturas no sensibles de dependencias declaradas anteriores.
Una guarda intermedia puede declarar `{"name":"chatId","operator":"exists"}` o
`{"name":"chatId","operator":"equals","value":"chat-123"}`. Una guarda falsa
omite ese paso. Un consumidor que depende del paso omitido aborta antes de
enviar. `headers` admite valores estáticos de `Accept-Language` y
`Content-Language`.

El último paso es un POST incondicional con exactamente un nodo `{{PROMPT}}`.
Para JSON se configura `{"type":"JSON","textPath":"$.text"}`. Para JSONL se
usa `type: "JSONL"` con la configuración `stream` descrita en la sección
de streaming. El binding terminal fija el `Accept` de SSE o JSONL.

Cada prueba admite hasta seis pasos y comparte el menor plazo entre el timeout
del target y el presupuesto restante del Scan. Las cotas efectivas de petición
y respuesta se aplican también a los bytes acumulados de sus entidades. Los
datos de stream descartados consumen cuota. El stream conserva sus límites de
frame y unidades. La espera de cuota consume el plazo compartido. La latencia
persistida suma la preparación y los intentos de cada paso sin esa espera.
Cada exchange aplica el control de frecuencia y revalida
SSRF, DNS/IP y TLS. La ruta se limita al mismo esquema, host y puerto efectivo
del target antes de DNS y descifrado del Bearer estático.

El diagnóstico DEBUG correlaciona cada paso con su TestExecution y registra
ordinal, ID, estado, código, duración, contadores y flags de redacción. El
resultado guarda únicamente la evidencia del paso terminal según el contrato
actual. El contexto local se libera al terminar, fallar o cancelar. Acordar
un TTL con el target para los recursos creados durante las pruebas.
