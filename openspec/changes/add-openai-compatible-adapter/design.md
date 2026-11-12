## Context

Estado: **READY FOR REVIEW**, diseño propuesto pendiente de aprobación. No se ha implementado ni validado ningún proveedor. generalize-target-integration es contexto, no contrato heredado.

Comprobado en código: TargetAdapter.interact(TargetConfiguration,String,ScanBudget) y NormalizedInteraction D0 existen. LegacyInvocationResultProjection preserva responseText/latencyMs/errorReason/requestRedacted/responseRedacted y legacy.httpStatus/requestBody. El bean único TargetAdapter delega hoy al bridge REST. TargetSettings exige URL, plantilla JSON objeto con un único nodo `{{PROMPT}}`, responsePath, auth y timeout; PostgreSQL exige los correspondientes campos. El mapper HTTP de entrada rechaza campos desconocidos; el decoder Angular espera el contrato legacy.

Fuentes: `domain/target/TargetSettings.java`, `application/target/TargetConfigurationService.java`, `adapters/in/rest/target/TargetRequestMapper.java` y TargetResponse, `adapters/out/validation/target/JsonTargetConfigurationValidator.java` y TargetJsonProcessor, `adapters/out/http/target/HttpTargetInvocationAdapter.java`, wiring D0, modelos/formulario Angular; docs de arquitectura, ADR-001/002/008, modelo de datos y estrategia de evaluación. D0 conserva sus restricciones históricas; este change autoriza proponer la segunda implementación, no reescribirlo.

El transporte actual ya renderiza, limita, autoriza, resuelve DNS, fija IP/TLS, descifra, envía, extrae, redacta y cierra recursos. Solo devuelve texto extraído, no el JSON completo. Por tanto el adapter nuevo no podría validar finish_reason/tool_calls después de recibir InvocationResult: necesita un punto de extracción dentro del transporte, antes de redacción/retorno.

La evaluación v1 requiere contexto sintético vinculado a targetUpdatedAt y corpus exportable exacto. Este cambio no elimina esa restricción. Mantener el motor no significa garantizar Findings útiles sobre cualquier caja negra; la aceptación de proveedor demuestra transporte y pipeline con inconclusos honestos.

## Goals / Non-Goals

**Goals:** configurar/usar un endpoint de chat desde la aplicación, preservar REST legacy y controles comunes, no ampliar la frontera normalizada ni el evaluador.

**Non-Goals:** los de proposal.md, además de catálogo de modelos remoto, detección automática de proveedor, historial de conversaciones, automatizar comprobaciones al guardar y habilitar redes privadas por seleccionar Ollama.

## Decisions

### OAC-01 Corte vertical mínimo frente a backend-only

Recomendación **B**, con backend/API/formulario juntos. Backend-only acorta frontend, pero aún necesita tipo persistido y entrada de configuración; si se reduce a una plantilla manual no cumple el objetivo de un adapter seleccionable/usabilidad. La UI añade un selector y cinco controles, reutilizando guardar/consultar y credenciales existentes. No añade pantallas, dashboard, editor JSON o conexión de prueba.

Tres cortes de implementación revisables: (1) configuración explícita/discriminador, escritura condicional y round-trip, (2) generador en memoria+transporte+adapter+routing con tests aislados, (3) UI y pipeline integrado. No activar tráfico hasta completar los tres. La validación real es una tarea final separada con permiso específico. La única migración incremental se diseña aquí, sin crear SQL ahora; su implementación/ejecución exige autorización posterior según reglas del proyecto.

### OAC-02 Configuración mínima y contrato de entrada

Usar URL **completa del endpoint**, no base URL más concatenación implícita. Ruta arbitraria válida para diferentes proveedores, sin forzar `/v1` ni inferir servicios por hostname. Ejemplo aportado, no invocado: `https://api.groq.com/openai/v1/chat/completions`.

```json
{
  "adapterType": "OPENAI_COMPATIBLE",
  "name": "Chat autorizado",
  "endpoint": "https://target.example.invalid/v1/chat/completions",
  "model": "modelo-disponible",
  "systemPrompt": "Instrucciones sintéticas opcionales",
  "authType": "BEARER_TOKEN",
  "authConfig": {"secretValue": "<valor recibido solo al escribir>"},
  "timeoutMs": 10000
}
```

La clave de ejemplo es un marcador, no un secreto ni una forma de persistencia. Bearer obligatorio para este primer contrato. Ollama/self-hosted solo son compatibles si aceptan ese contrato/autenticación; NONE para chat requiere incremento posterior, no se finge que todos los despliegues ya están cubiertos. HTTP público con Bearer se sigue rechazando; HTTPS normal por defecto, excepción local preexistente solo para fixtures autorizadas.

Model requerido, trim de bordes, 1..200 puntos de código, sin caracteres de control/NUL; no se valida disponibilidad mediante red. System opcional/null equivale a omisión; si se proporciona, no blanco, <=16 KiB UTF-8 y contenido preservado sin trim. Se conserva la cota de 32 KiB del documento base generado en memoria con el prompt reservado y el rechazo del token reservado `{{PROMPT}}` en model/system del contrato inicial, sin persistir ese documento ni usarlo como fuente de configuración. El body efectivo con SecurityTest.prompt queda sujeto al límite actual de request. Timeout conserva 1000..60000 ms/default10000. No parámetros adicionales necesarios: no temperature, tokens, tools, tool_choice, response_format, reasoning o extras. Si un modelo requiere parámetros especiales se declara no compatible con este perfil; no se amplía el formulario de forma implícita.

### OAC-03 Dominio y persistencia explícita por rama — F1

TargetConfiguration/TargetSettings representan explícitamente adapterType, model y systemPrompt como configuración tipada, sin extraerlos de JSON. REST_LEGACY conserva requestTemplate/responsePath obligatorios y model/systemPrompt null; OPENAI_COMPATIBLE exige model, admite systemPrompt nullable y exige requestTemplate/responsePath null. Validadores, constructor y mapper JPA discriminan por rama antes de validar: no construir un TargetSettings legacy ficticio para chat. No introducir Groq/OpenRouter, SDK o parser JSON en dominio; model es un identificador configurable opaco. El snapshot actual incluye estos campos; el use case no cambia.

Una única migración incremental futura sobre target_configurations:

| Columna | Nulabilidad/tipo propuestos | Condición |
|---|---|---|
| adapter_type | VARCHAR(32) NOT NULL DEFAULT 'REST_LEGACY' | CHECK con exactamente REST_LEGACY/OPENAI_COMPATIBLE |
| model | VARCHAR(200), nullable físicamente | Obligatorio y no blanco en chat; NULL en legacy |
| system_prompt | TEXT, nullable | Opcional en chat; NULL en legacy |
| request_template | TEXT, retirar NOT NULL físico | NOT NULL en legacy; NULL en chat |
| response_path | VARCHAR(1024), retirar NOT NULL físico | NOT NULL en legacy; NULL en chat |

El CHECK de rama es disyunción explícita: `(adapter_type='REST_LEGACY' AND request_template IS NOT NULL AND response_path IS NOT NULL AND model IS NULL AND system_prompt IS NULL)` OR `(adapter_type='OPENAI_COMPATIBLE' AND model IS NOT NULL AND btrim(model)<>'' AND request_template IS NULL AND response_path IS NULL AND auth_type='BEARER_TOKEN')`. Los IS NULL/IS NOT NULL son esenciales: PostgreSQL acepta un CHECK UNKNOWN y no bastaría comprobar solo longitud/model<>''. Para system_prompt, CHECK adicional `system_prompt IS NULL OR (octet_length(system_prompt)<=16384 AND btrim(system_prompt)<>'')`; validación de aplicación mantiene además Unicode/NUL/no blanco según contrato. No se aplica un nuevo requisito de contenido a plantillas/rutas históricas. Los checks existentes ck_target_auth/timeout/dates/headers, PK/FK/unique e índices se conservan.

Las filas actuales reciben solo adapter_type=REST_LEGACY por default; model/system_prompt quedan NULL y sus plantilla/ruta y restantes valores no se modifican. No hay backfill de modelo, extracción de templates existentes o conversión automática. Una conversión explícita de rama escribe todos sus campos, credencial y updatedAt atómicamente, limpiando a NULL los no aplicables en el mismo UPDATE; no existe un estado intermedio que vulnere checks. UUID, cardinalidad y fechas conservan reglas actuales.

La request con model/messages/stream=false se genera por un único codec/generador HTTP **solo en memoria** a partir de los campos tipados y el prompt efectivo; no se guarda en TargetConfiguration.requestTemplate ni se configura responsePath para chat. La extracción fija choices[0].message.content reside en el adapter. GET lee model/systemPrompt directamente de columnas/campos, sin parsear plantillas. El requestBody saneado que ya registra TestExecution sigue siendo evidencia de una invocación, no almacenamiento de configuración ni nueva fuente de verdad.

Se descartan model/system en request_template (F1), inferencia por URL/template, metadata en headers/name, nuevas tablas/JSON genérico/revisions y valores ficticios para columnas no aplicables. Model/system son configuración no secreta: solo textos sintéticos/no sensibles en esta entrega, sin credenciales o instrucciones privadas de producción. No crear/ejecutar SQL ahora ni editar V2 histórica: las adiciones, DROP NOT NULL y checks se describen para una sola migración nueva.

### OAC-04 API y compatibilidad

Reutilizar POST/GET/PUT `/api/projects/{projectId}/target` como unión discriminada. Omisión de adapterType en creación sigue significando REST_LEGACY; legacy conserva su request/response actual (GET puede omitir el discriminador para conservar forma exacta). Rama chat devuelve adapterType, endpoint/model/systemPrompt/timeout y metadata segura id/projectId/name/fechas/hasCredentials/authType/authConfig=null, sin requestTemplate/responsePath/headers ni secreto. POST/PUT chat rechazan campos de contrato genérico y parámetros fuera de alcance; 400 VALIDATION_ERROR por campo seguro, sin reflejar valores.

En PUT, un target chat requiere adapterType explícito: una escritura de cliente viejo sin tipo devuelve 409 TARGET_TYPE_REQUIRED sin convertirlo silenciosamente. Cambiar tipo requiere envío explícito y confirmación UI; hacia REST exige plantilla/ruta válidas. Conservar/reemplazar token sigue la regla exacta existente: mismo origen normalizado, mismo authType/header, authConfig presente sin secretValue; origen distinto requiere secreto nuevo. NONE en la rama legacy conserva su semántica. No descifrar en GET/PUT para conservar. El error de nuevo tipo no cambia el resource lifecycle ni introduce versionado optimista.

#### OAC-04a Escritura condicional atómica por tipo esperado — F2

El repositorio actual hace find→entity.replace→flush; una comprobación de tipo solo en servicio no evita que otra transacción convierta la fila antes del flush. Elegir actualización condicional en repositorio/SQL, no un framework de optimistic locking ni @Version. El puerto de reemplazo recibe el TargetConfiguration candidato y expectedAdapterType; devuelve éxito/ninguna coincidencia o conflicto tipado. El predicado incluye id, project_id y adapter_type=:expectedAdapterType. Un PUT sin discriminador exige siempre expectedAdapterType=REST_LEGACY; para PUT explícito se usa el tipo leído por el servicio en esa misma actualización (no un nuevo campo de versión enviado por UI). Todas las escrituras de actualización de este recurso usan esta operación, incluidas conversiones: no queda replace incondicional como bypass.

El mapper conserva en el comando en memoria si adapterType fue aportado explícitamente; aplicar el default legacy no debe borrar esa información antes de elegir el tipo esperado y el conflicto correspondiente. No es una columna nueva ni un token de versión público.

El UPDATE escribe configuración completa de destino, NULLs de campos no aplicables, credencial cifrada y updatedAt en una sola operación dentro de TargetTransactions existente. Si afecta 0 filas por conversión concurrente, el PUT antiguo sin tipo devuelve 409 TARGET_TYPE_REQUIRED; el explícito devuelve 409 TARGET_TYPE_CHANGED. No volver a leer y reintentar sobre el tipo nuevo, ni confirmar parcialmente credencial/configuración. La ausencia de target detectada inicialmente mantiene su 404. En PostgreSQL READ COMMITTED, si espera por una escritura concurrente, se reevalúa el predicado tras el commit de la otra transacción: REST_LEGACY ya no coincide con chat.

Evitar que JPA eluda la condición por dirty checking: leer snapshot sin modificar una entidad managed; ejecutar actualización condicional como única escritura, sin entity.replace/merge/flush incondicional previo/posterior. Tras bulk update, descartar/refrescar la entidad de target del contexto antes de reutilizarla, para no devolver ni guardar estado cacheado antiguo. Capturar el conteo de filas y no transformar el conflicto tipado en INTERNAL_ERROR dentro del catch genérico actual. Resultado solo tras commit de configuración+credencial como hoy.

Test determinista diseñado, no ejecutado: dos transacciones/conexiones sobre PostgreSQL dedicado, coordinadas por barreras/latches sin sleeps. T1 inicia PUT antiguo sin tipo, lee legacy y se pausa justo antes del UPDATE condicional. T2 convierte y confirma chat con model/system y plantilla/ruta NULL. Liberar T1; su UPDATE con expected REST_LEGACY afecta 0 filas y devuelve 409. Leer con una tercera conexión y comprobar que tipo/model/system/URL/ciphertext/updatedAt de T2 siguen intactos, sin escrituras parciales, HTTP al target ni retry. Añadir orden inverso: si PUT legacy confirma primero, la conversión posterior sigue siendo explícita/válida. No se promete resolver actualizaciones perdidas del mismo tipo ni ABA: F2 protege esta carrera de tipo, no versiona globalmente entidades. Row lock se descarta porque el UPDATE condicional es suficiente y evita mantener un bloqueo durante validación/cifrado de configuración.

No ampliar DTO de Scan, ejecución/evaluación/Finding/score/report. No abrir endpoint `/models`, no comprobar key o disponibilidad al guardar. OpenAPI refleja oneOf, discriminador y ausencia compatible para legacy; el decoder frontend acepta ambas ramas y falla seguro ante desconocidos.

### OAC-05 Selección e invocación

Un RoutingTargetAdapter de aplicación/composición recibe dos implementaciones explícitas y hace switch exhaustivo por settings.adapterType. No mapa de plugins ni reflexión, lookup por proveedor o fallback de tipo desconocido. El bean principal TargetAdapter es este delegador; los dos concretos se inyectan por nombre/tipo sin ambigüedad. LegacyRestTargetAdapter sigue usando TargetInvocationPort y respetando fakes @Primary como D0.

```text
ExecuteSecurityTestsUseCase -> RoutingTargetAdapter
  REST_LEGACY -> LegacyRestTargetAdapter -> TargetInvocationPort
  OPENAI_COMPATIBLE -> OpenAiCompatibleTargetAdapter
      -> generador HTTP en memoria desde model/systemPrompt
      -> mismo HttpTargetInvocationAdapter con extractor chat
  -> NormalizedInteraction D0 -> proyección legacy -> flujo existente
```

OpenAiCompatibleTargetAdapter implementa exactamente TargetAdapter.interact actual. Genera/valida contrato en memoria desde model/systemPrompt explícitos, sin rellenar requestTemplate/responsePath del target chat; preserva id/fechas/credencial/presupuesto y delega un único POST. El mismo prompt SecurityTest se envía como user, sin precederlo con instrucciones del evaluador ni añadir contexto/canarios no presentes. Cada una de las cuatro pruebas es una petición independiente; no hay memoria multi-turn.

### OAC-06 Reutilización segura de HTTP y parsing

El punto interno del cliente compartido recibe construcción de request en memoria y extracción de respuesta: `invokeWithResponseExtractor(target,prompt,budget,requestRenderer,extractor)`. Es la adaptación estrictamente necesaria de F1 para no invocar un renderer legacy con plantilla null ni inventar TargetSettings de transporte. requestRenderer genera JSON desde campos explícitos después de entrar en el mismo gate/deadline; no tiene red/credenciales/lifecycle y no modifica el snapshot. `TargetResponseTextExtractor` es el callback síncrono de cuerpo JSON ya acotado→String. El método actual TargetInvocationPort.invoke usa renderer y extractor legacy actuales, conservando sus firmas y comportamiento. Ambos caminos ejecutan una única implementación de autorización/DNS/TLS, request, lectura, límites, redacción, errores y cierre. El nuevo adapter usa la misma instancia cliente; no necesita otro application port de transporte, SDK ni cliente HTTP nuevo. Esta corrección de construcción no cambia routing ni controles de transporte.

El callback chat parsea estrictamente JSON con cotas/duplicados/Unicode equivalentes a TargetJsonProcessor; validar una sola elección, choices[0] objeto, message objeto, content String y finish_reason=`stop`. Role si aparece debe ser assistant e index si aparece debe ser 0. Longitud/herramientas/content_filter/terminal desconocido o ausente son contrato no soportado/incompleto: RESPONSE_FORMAT_ERROR, no prefijo evaluable. Rechazar tool_calls/function_call no vacíos aunque exista content; no ejecutar nada. Otros campos normales como id/model/usage se ignoran sin incorporarlos a NormalizedInteraction; no son opciones configurables.

El callback devuelve solo texto final. El transporte aplica después el mismo saneamiento por credencial conocida y evidencia/size antes de producir InvocationResult, que se convierte por fromLegacy sin alterar siete campos. El callback no ve ni añade Bearer; el cuerpo completo remoto no sale del transporte ni se almacena. Excepciones tipadas usan el mecanismo JsonFailure actual; no se inventa jerarquía de fallos. Mantener precedencia timeout/presupuesto actual. NormalizedInteraction, LegacyInvocationResultProjection, TestExecution y ExecuteSecurityTestsUseCase no requieren cambios de contrato.

### OAC-07 Errores y semántica

| Caso observado | ExecutionError existente | Comportamiento |
|---|---|---|
| 401/403 | HTTP_STATUS_ERROR | httpStatus conservado; no copiar cuerpo remoto; sin retry |
| 404 | HTTP_STATUS_ERROR | Mismo criterio, endpoint inválido no fallback |
| 429 | HTTP_STATUS_ERROR | Sin espera/retry/Retry-After automático |
| 5xx u otro no 2xx | HTTP_STATUS_ERROR | Mismo alcance individual v1 |
| Timeout de llamada | TARGET_TIMEOUT | Presupuesto de Scan agotado conserva precedencia SCAN_BUDGET_EXCEEDED |
| DNS/conexión/TLS/política | Códigos existentes respectivos | Sin recodificar ni saltar validaciones |
| Cuerpo vacío, JSON malformado, Unicode inválido | RESPONSE_INVALID_JSON | Sin evaluar texto parcial |
| choices ausente/vacío o message/content ausente | RESPONSE_PATH_NOT_FOUND | No fabricar respuesta |
| choices/message tipo erróneo, choices múltiples | RESPONSE_FORMAT_ERROR | Estructura de protocolo no soportada |
| content null/objeto/array/número | RESPONSE_NOT_TEXT | Sin stringify ni concatenación |
| content="" o solo whitespace con finish stop | Sin error técnico | Texto exacto, incluido vacío; criterios v1 deciden insuficiencia, no evidencia de seguridad |
| finish_reason distinto de stop/ausente/null o tool call | RESPONSE_FORMAT_ERROR | Sin éxito ficticio por HTTP 200, sin nuevo estado parcial |
| text/event-stream o encoding no admitido | RESPONSE_FORMAT_ERROR | No activar streaming |
| tamaños excedidos | RESPONSE_TOO_LARGE/EVIDENCE_LIMIT_EXCEEDED/REQUEST_TOO_LARGE actuales | Misma eliminación/redacción de evidencia y límites |

HTTP_STATUS_ERROR sigue siendo individual: si falla una prueba, se sigue el flujo v1 y al final falla el Scan; pueden ocurrir cuatro peticiones fallidas distintas, nunca reintentos de una prueba. No convertir 401/429 a nuevos errores globales en este change. UI puede explicar el httpStatus mediante texto controlado sin cambiar errorReason ni importar mensaje libre del proveedor. Fallos de configuración de chat al ejecutar → TARGET_CONFIGURATION_INVALID global; entradas inválidas al guardar → 400 antes de red.

### OAC-08 Seguridad y alcance de evaluación

Conservar allowlist operativa exacta separada de configuración, bloqueo privado/loopback/metadata/interfaces por defecto, validación de todas las IPs/DNS acotado/conexión fijada con TLS del hostname, cero redirects/proxy/cookies/retries automáticos. Seleccionar modelo/proveedor no concede autorización. Ollama local no se habilita automáticamente; excepción local vigente solo bajo sus tres condiciones y orígenes exactos. No ampliar permisos de red ni cambiar despliegue para aceptar el change.

Bearer se cifra mediante AES-GCM existente, se descifra solo tras autorizar y validar destino, no alcanza callback, se elimina de headers/buffers según mecanismo actual. Ningún log/error/Finding/GET/report muestra token, ciphertext o cuerpo remoto de error; requestBody persistido contiene JSON saneado, nunca cabeceras Authorization. Si el target refleja la key, se redacta con flags existentes; redacción no certifica anonimato arbitrario. System/model y parámetros de API se tratan como datos, no se registran como diagnósticos.

Validación real inicial solo con prompts de catálogo y system sintético/no sensible. El modelo de proveedor puede generar texto libre almacenado bajo las cotas actuales: esto no autoriza datos reales sensibles ni exportación al evaluador. EVALUATOR_EXPORT_* sigue deshabilitado o con su política exacta; ausencia de contexto/corpus produce INCONCLUSIVE y llmStatus SKIPPED, no una llamada «de prueba» al evaluador. No cambia HybridDecisionPolicy ni fórmulas/report-v1. Sus restricciones sintéticas siguen describiendo la evaluación aunque el transporte del target sea real. Configuración y UI deben explicar «Integración chat; criterios de evaluación v1 limitados a escenarios sintéticos autorizados» para no vender cobertura nueva.

### OAC-09 Frontend mínimo

Dentro de TargetForm/TargetPage existentes: selector REST JSON/OpenAI-compatible, endpoint/model/Bearer/system opcional/timeout en chat. REST conserva todos sus controles. Name/metadatos existentes pueden seguir siendo comunes, sin nueva pantalla. No mostrar requestTemplate/responsePath/headers/auth selector genérico para chat; mantener modelos de formulario separados o unión discriminada para no enviar campos ocultos residuales. Aceptar endpoint completo, no construir URL a partir de un preset comercial.

GET rellena campos seguros; token solo de escritura y conservar/reemplazar según flujo actual. Limpiar secreto al enviar/fallar/cancelar/cambiar tipo/proyecto/navegar; no browser storage. Confirmar cambio de tipo con pérdida de campos incompatibles antes de PUT; guardar no inicia Scan ni valida credenciales mediante red. Mensaje de alcance junto a configuración/acción de Scan, sin modificar el informe histórico.

### OAC-10 Tests y aceptación real

| Nivel | Pruebas necesarias sin servicios externos |
|---|---|
| Config/generador/API | Request/GET tipados, JSON generado solo en memoria, system presente/ausente, model y prompt correctos, rechazo de campos ocultos/extras/tipo, legacy sin discriminador, cambio de tipo explícito y conservación de credencial |
| Persistencia autorizada F1 | Una migración con tres columnas y checks por rama; legacy intacto y chat round-trip tras reinicio leyendo model/system_prompt directamente, plantilla/ruta NULL en chat; rechazar combinaciones inválidas incluso con NULL y no plaintext de Bearer |
| Carrera de actualización F2 | Dos transacciones con barreras según OAC-04a; conversión confirmada antes de UPDATE antiguo→0 filas/409 y chat/credencial/updatedAt intactos, sin retry; orden inverso válido |
| Adapter+HTTP local | POST/Content-Type/model/messages/stream=false exactos; Bearer llega al servidor controlado, sin copiarlo a evidencias; respuesta anidada, escapes/Unicode y todos los casos OAC-07 |
| Seguridad | DNS/SSRF/TLS/redirect/timeout/tamaño/credenciales reflejadas, mismo cliente seguro y cierre al fallar extractor; no nueva llamada ni uso de credencial antes de autorización |
| Routing/D0 | Dos tipos sin fallback, fakes legacy respetados, siete campos conservados; mismos prompts/payload/responsePath y fallos históricos para REST |
| Pipeline | Cuatro invocaciones chat independientes y evaluaciones existentes, caso sin contexto→cuatro inconclusos/score0/report-v1, fixtures sintéticas ya autorizadas para regresión de Findings. No abrir exportación para obtener un resultado deseado |
| UI | Cambiar tipo sin campos residuales, validación, lectura/guardado/token efímero y cero tráfico al guardar; mismo formulario legacy |

Reutilizar fixtures, clock/fakes y suites actuales de target, D0, HTTP, evaluación/scoring/reporting; no modificar expectativas históricas para encajar. HTTP/HTTPS server de prueba solo local, DNS simulado y PostgreSQL dedicado existente cuando se solicite. No ejecutar tests/builds/servidores/BD en diseño; suites condicionadas/omitidas no cuentan como aprobadas. No dependencias nuevas.

Validación real posterior: operador elige modelo disponible sin asumir un ID válido; obtiene permiso del endpoint/cuenta, verifica que las condiciones de uso permiten las cuatro pruebas y acuerda coste/cuota. Con permiso específico, allowlist exacta y clave introducida localmente por formulario (no se lee .env ni se versiona/se imprime secreto), configurar Groq o alternativamente OpenRouter. Ejecutar **un Scan de cuatro pruebas**, sin prueba previa adicional ni modelo evaluator real por defecto. Registrar solo IDs internos, fecha, endpoint público sin query sensible, model no secreto, cuatro estados/httpStatus, contadores y versiones/score/report; nunca key, headers, request/response completos. Confirmar Scan COMPLETED con cuatro ejecuciones/evaluaciones si todas las respuestas son válidas y hay presupuesto, score/report-v1 íntegros; INCONCLUSIVE es resultado esperado sin contexto. Si hay 429/longitud/auth, registrar fallo y no declarar aceptación ni repetir sin autorización. Proveedor real no disponible/permisos pendientes bloquea aceptación externa, no la revisión del diseño ni la aceptación de tests aislados.

### OAC-11 Componentes afectados y límites

Afectados: TargetConfiguration/TargetSettings/TargetAdapterType con model/systemPrompt explícitos y validación por rama; mapper JPA de tres columnas y nulabilidad condicional, repositorio/puerto de actualización condicionada por tipo; generador HTTP solo en memoria; TargetRequestMapper/TargetResponse/OpenAPI y servicio para discriminar actualización/errores seguros; OpenAiCompatibleTargetAdapter y RoutingTargetAdapter/wiring; punto interno de construcción/extracción del cliente seguro; TargetForm/modelos/decoders/API frontend y tests. Actualizar solo arquitectura-general, target-contract y modelo-datos directamente afectados al implementar. No reescribir ADRs históricos; registrar la extensión aprobada sin declarar cerrado el roadmap.

Intactos: TargetAdapter y NormalizedInteraction/proyección D0, contrato de ejecución/TestExecution, catálogo, EvaluatorPort/evaluador real/reglas/consolidación, Findings, ScoringPolicyV1 y reporting-v1, recursos de informe, dispatcher/recovery, excepción SSRF operativa, frontend de resultados salvo aviso de alcance en la configuración, demo y despliegue. No escribir migraciones ni tocar estos componentes productivos en la fase actual.

## Risks / Trade-offs

- [Cambios de nulabilidad debilitan legacy] → checks por rama con IS NULL/IS NOT NULL explícitos; legacy mantiene obligatoriedad y datos originales, chat campos propios.
- [Duplicación de fuentes model/system] → columnas explícitas como única fuente; generador solo en memoria, sin persistir config chat en template/ruta.
- [Extraer callback altera transporte legacy] → mantener invoke legacy y tests de precedencia/redacción/cierre; una implementación común, sin duplicar cliente ni leer cuerpos dos veces.
- [Proveedor «compatible» difiere] → perfil estricto sin herramientas/razonamiento; errores explícitos, no fallback ni extras; comprobar modelo real antes de aceptar.
- [Coste/output extenso sin max_tokens] → cotas de red/texto/tiempo actuales, cuatro llamadas y modelo elegido con límites de cuenta; no prometer límite monetario del target por cortar lectura. Si no basta, no ejecutar ese modelo y proponer parámetro acotado en otro incremento.
- [Confundir transporte real con evaluación general] → cuatro inconclusos legítimos y advertencia de alcance; no cambiar criterios/corpus para «hacer pasar» aceptación.
- [Self-hosted privado o sin auth no encaja] → mantener bloqueo/Bearer inicial, declarar perfil no soportado; no relajar SSRF ni añadir credencial ficticia por conveniencia.
- [Clientes antiguos sobrescriben chat tras conversión concurrente] → UPDATE condicionado por tipo esperado como única escritura, conteo de filas/409 sin retry, dirty checking controlado y test con barreras.
- [System contiene material sensible] → esta entrega limita contexto a datos sintéticos/no sensibles; guardado no es mecanismo seguro de custodia de instrucciones de producción.

## Migration Plan

Diseño exclusivamente: ningún archivo SQL se crea/ejecuta ahora. Después de aprobación, **una única migración incremental** (número aún no reservado) añade adapter_type/model/system_prompt, retira NOT NULL físico de request_template/response_path e incorpora checks de tipo/rama/system detallados en OAC-03, en una transacción. No editar V2 histórica ni crear tablas, config JSON/revisions o backfill ficticio. Filas existentes conservan valores originales y reciben únicamente default REST_LEGACY/model=NULL/system_prompt=NULL; sus plantilla/ruta siguen obligatorias por CHECK. Código/mapper de nulabilidad, UPDATE condicional, API y UI se entregan coordinados. Conversiones explícitas posteriores escriben/limpian campos en una sola sentencia; no convertir automáticamente targets ni tocar scans históricos.

Rollback funcional futuro: deshabilitar nuevas configuraciones chat y conservar lector de ambos tipos; no eliminar columnas ni reconvertir automáticamente datos. Un binario anterior exige plantilla/ruta no nulas, ignora los campos chat y carece de la protección atómica por tipo: no es rollback seguro para chat activo. No se propone despliegue, operación AWS ni procedimiento destructivo en este change.

## Open Questions

F1/F2 incorporan las correcciones explícitas del review: persistencia de tres campos propios y UPDATE condicionado por tipo esperado, sin versionado global. Las restantes decisiones OAC-01..11 siguen pendientes de aprobación; se mantiene B, Bearer y perfil estricto. No quedan decisiones bloqueantes de diseño. Pendientes operativos antes de validación real: modelo disponible, permiso explícito de cuenta/proveedor, coste/cuota, origen autorizado y clave provisionada por el usuario. No requieren inventar un model ID ni consultar Internet durante la planificación.

### Fuera del MVP / Evolución futura

NONE para self-hosted, límite de output configurable, nuevos perfiles de evaluación/datos, SSE/streaming, sesiones, tools/RAG y otras capacidades excluidas requieren cambios independientes. Este change no los anticipa con campos o mecanismos genéricos.
