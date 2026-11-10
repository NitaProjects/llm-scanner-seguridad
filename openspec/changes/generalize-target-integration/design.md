## Context

Estado: propuesta v2 **READY FOR REVIEW**, no aprobada para implementar. La v1/TFM está entregada según el usuario; algunas fuentes conservan estados históricos de entrega pendientes. Este change no certifica ni corrige retrospectivamente esos estados. Véase `proposal.md` para motivación y alcance.

### Diagnóstico comprobado y fuentes

| Área | Evidencia local | Consecuencia para v2 |
|---|---|---|
| Arquitectura | `docs/02-arquitectura/arquitectura-general.md`; paquetes domain/application/adapters/config | Conservar monolito modular, puertos y transacciones cortas; no crear microservicios |
| Target | ADR-001; change target-configuration TC-04..TC-11; `domain/target/TargetSettings.java` | POST y JSON constantes; plantilla <=32 KiB; timeout 1..60 s; ya existen headers, Bearer/API key y JSON anidado |
| Extracción | `adapters/out/validation/target/TargetJsonProcessor.java` | Ya recorre propiedades e índices de arrays y sustituye un nodo String completo; no es JSONPath completo ni solo JSON plano |
| Invocación | `application/execution/TargetInvocationPort.java`, `InvocationResult.java` | Puerto existente reutilizable como puente, pero resultado todavía contiene httpStatus y requestBody |
| Ejecución | STE-01..STE-03; `domain/execution/TestExecution.java` | Cuatro tests fijos; COMPLETED exige HTTP 2xx, requestBody y responseText; no basta añadir un enum de protocolo |
| Red | `adapters/out/http/target/TargetDestinationPolicy.java`; política de autorización | Ya hay allowlist externa exacta, validación de todas las IPs, DNS acotado y conexión fijada; conservar, no sustituir por una comprobación de URL al guardar |
| Evaluación | EE-01..EE-06; `EvaluationEvidenceProjector.java`, `LocalEvaluationScenarioAdapter.java`, `DeterministicRules.java` | El contexto sintético y el corpus exportable exacto son parte del contrato v1. Sin contexto no hay aplicabilidad; permitir datos reales necesita perfil y permiso nuevos |
| Score/report | ADR-005/ADR-008; changes scoring-engine/reporting | scoring-v1 incorpora bonus de confidence; report-v1 declara nueve limitaciones fijas, incluyendo escenario sintético y streaming excluido |
| Datos/UX | `docs/06-base-de-datos/modelo-datos.md`; `frontend/.../target/models/target.ts`; arquitectura frontend | Un target por Project; credenciales de escritura sin recuperación; DTOs/decoders estrictos; sin snapshots completos persistidos del target |

Los contratos v1 son coherentes con esas restricciones; la limitación es de alcance, no un bug. Las propuestas v2 contradicen deliberadamente restricciones de ADR-001/002 (SSE/sesiones), STE-03 (HTTP en ejecución), EE-01/06 (solo sintético), RP-07 (limitaciones fijas) si se aplicasen al mismo perfil. Se resuelve proponiendo perfiles prospectivos explícitos y aprobación posterior, sin modificar esos contratos históricos ni código ahora.

No se ha inspeccionado ni invocado ColoqIA, Groq u OpenRouter; sus detalles se tratan como necesidades aportadas, no como contratos verificados. La investigación de DSL compara opciones localmente, sin acceso externo.

## Goals / Non-Goals

**Goals:** independencia entre protocolo, normalización, prueba y evaluación; evidencia verificable y minimizada; soporte progresivo; ejecución repetible con versión de configuración; UX por capacidades. Ningún adapter promete cobertura de vulnerabilidades solo por conectarse.

**Non-Goals:** los de la propuesta. Sesión de aplicación no equivale a implementar login/OAuth/cookies. OpenAI-compatible describe el contrato del target y no reemplaza `OpenAiEvaluatorAdapter`. El soporte de Ollama/self-hosted no habilita automáticamente redes privadas. RAG como implementación interna del target puede responder por un adapter, pero no se afirma evaluación de recuperación, poisoning o aislamiento entre documentos.

## Decisions

Todas las decisiones D-01..D-13 son **recomendaciones para aprobación de este diseño**, no ADRs aceptadas. Las políticas numéricas son límites iniciales propuestos, reducibles operativamente y revisables antes de aprobación. No se cambian versiones, dependencias ni configuración de ejecución en esta tarea.

### D-01 Arquitectura y responsabilidades

```text
Angular -> API de configuración -> ConfigureTargetIntegrationUseCase
                                  -> validación local -> repositorio
StartScan -> plan fijo/versionado -> ExecuteSecurityTestsUseCase
                                      -> TargetInteractionPort
                                         -> AdapterRegistry (composición)
                                            REST / OpenAI / Stream / Workflow
                                            -> transporte seguro común
                                      <- NormalizedInteraction
                                      -> evidencia durable minimizada
                                      -> EvaluateExecutionUseCase
                                         reglas + EvaluatorPort + consolidación
                                      -> Finding -> Score -> Report -> cierre atómico
```

La prueba inicia la interacción y consume su resultado normalizado; no recibe HTTP ni chunks crudos. El diagrama conceptual adapter→interacción→motor expresa una frontera de datos, no invierte la orquestación. Domain conserva valores y políticas; Application coordina; Infrastructure resuelve JSON, SSE, DNS, TLS y cifrado. Reutilizar `ScanBudget`, dispatcher, catálogo, repositorios, `EvaluatorPort`, consolidación híbrida y finalización transaccional. Refactorizar solo las fronteras necesarias.

Alternativa descartada: ampliar `HttpTargetInvocationAdapter` con condicionales por proveedor y workflow; reproduce el acoplamiento. Tampoco se propone un framework de plugins ejecutables: registro estático de implementaciones del servidor. M5 estabiliza este contrato después de probar M1..M4.

### D-02 Modelo conceptual y puertos

| Modelo | Responsabilidad |
|---|---|
| Project / TargetConfiguration | Identidad y propiedad actuales, un target por Project |
| TargetIntegrationRevision | Revisión inmutable de adapterType, configSchemaVersion y configuración pública validada; sin secreto |
| TargetProtocol / AdapterType | Protocolo observado HTTP_JSON/SSE/JSONL separado de REST_JSON/OPENAI_COMPATIBLE/STREAM/SESSION_WORKFLOW |
| InteractionRequest | executionId, prompt y contexto de prueba autorizado; sin token ni clases HTTP |
| InteractionBudget | deadline monotónico, bytes, intentos, eventos y cuota compartidos por toda la interacción |
| NormalizedInteraction | Resultado técnico neutral y referencia a evidencia |
| InteractionEvent | Evento normalizado efímero, acotado y ordenado |
| EvidenceRecord / EvidenceReference | Evidencia versionada, integridad, transformaciones y referencias |
| AssessmentContext | Política declarada por propietario, expectativas/contrato/canarios artificiales opcionales; nunca ground truth |
| EvaluationResult / Finding | Veredicto y hallazgo trazables, sin detalles de transporte |

Firmas ilustrativas, no código a implementar ahora:

```text
TargetInteractionPort.interact(IntegrationSnapshot, InteractionRequest, InteractionBudget)
    -> NormalizedInteraction
TargetAdapter.describe() -> AdapterDescriptor(type, configVersions, capabilities)
TargetAdapter.validate(Config) -> ValidationResult                 // sin red
TargetAdapter.interact(IntegrationSnapshot, InteractionRequest, InteractionBudget)
    -> NormalizedInteraction
EvidenceStorePort.store(SanitizedEvidence) -> EvidenceReference
EvidenceProjectionPort.project(Interaction, AssessmentContext, DataPolicy) -> EvaluatorInput
TargetIntegrationRepositoryPort.snapshot(targetId) -> IntegrationSnapshot
CredentialAccessPort.withCredential(ScopedCredentialReference, authorizedDestination, operation)
```

El puerto público tiene una operación completa, evitando handles de conexión y un protocolo prepare/send/collect/cleanup que Application tendría que conducir. Cada adapter controla su lifecycle internamente con finally y deadline de limpieza. Errores previstos son resultados tipados; errores de programación/persistencia abortan el trabajo como hoy. `cleanup` remoto es opcional, explícito y autorizado, no un DELETE implícito. No conserva hilos, sesiones ni recursos entre TestExecutions.

`SafeHttpExchange`, `StreamDecoder`, `SelectorEngine`, `TemplateRenderer` y `WorkflowInterpreter` son servicios internos de adapters. Se comparten controles de red; el dominio no importa URI, HttpResponse, Jackson, Spring ni nombres comerciales. Configuración opaca versionada en infraestructura se valida y compila a un plan seguro antes de ejecutar; no se infiltra un mapa arbitrario en las reglas de dominio.

### D-03 NormalizedInteraction y semántica de finalización

| Campo | Contrato |
|---|---|
| schemaVersion, interactionId, scanId, executionId | Identidad y evolución explícitas |
| targetId, integrationRevisionId, adapterType/version | Reproducibilidad sin resolver el target actual |
| prompt / promptEvidenceRef | Prompt lógico efectivamente enviado, saneado; hash y revisión de catálogo, sin secretos |
| requestMetadata | Operaciones y contadores seguros; método/status opcionales en evidencia técnica, nunca cabeceras/URL con query libre |
| responseText | Texto final normalizado, o prefijo parcial marcado; no se concatena salida de herramientas |
| evidenceRefs | Referencias al transporte saneado y representación normalizada; no blobs ni respuestas crudas embebidas |
| status | COMPLETE / INCOMPLETE / FAILED, técnico e independiente del veredicto |
| completenessReason | FINAL_SIGNAL / END_OF_DOCUMENT / PREMATURE_EOF / LIMIT / TIMEOUT / PROTOCOL_ERROR |
| startedAt, finishedAt, latencyMs | UTC y duración monotónica total; timeToFirstContentMs opcional |
| protocol, providerLabel, requestedModel/resolvedModel | Metadata opcional; proveedor declarado no prueba identidad y no selecciona política de evaluación |
| warnings | Códigos enumerados y contadores; sin mensajes arbitrarios del remoto |
| partial, truncated, redacted | Flags explícitos y transformación versionada |
| errorCategory | AUTH, RATE_LIMIT, NETWORK, DNS, POLICY, TIMEOUT, SIZE_LIMIT, PROTOCOL, EXTRACTION, UNSUPPORTED_CONTENT, INTERRUPTED, INTERNAL; null para éxito |

COMPLETE exige final según contrato, texto String seleccionado (vacío permitido como dato pero insuficiente para concluir) y límites respetados. INCOMPLETE conserva solo prefijo permitido y motivo; FAILED puede no tener texto. D1/D2 mapean INCOMPLETE/FAILED a TestExecution FAILED con diagnóstico seguro, evaluación abortada y Scan FAILED según el lifecycle actual; no se introduce PARTIAL/CANCELLED ni se deja un parcial pasar a COMPLETED. La incompletitud se observa en la interacción sin inventar éxito del Scan. Una evaluación técnicamente completa sin contexto aplicable puede ser COMPLETED/INCONCLUSIVE.

En M1/M2 el puente `TargetInvocationPort` permite migrar por etapas. En M3 la ejecución v2 valida final técnico neutral; `httpStatus` pasa a ser metadata opcional, con proyección v1 intacta para su perfil. Mantener cuatro ejecuciones/evaluaciones y score+report+cierre atómicos en todos los nuevos perfiles admitidos.

### D-04 Evidence en cuatro capas

1. Transporte: frames/HTTP observados en memoria, inmediatamente acotados. «Raw evidence» significa capa de procedencia; nunca persistencia automática de bytes sin filtrar. Default metadata-only; captura de contenido saneado solo por política y autorización explícitas.
2. Respuesta normalizada: texto exacto saneado usado para evaluar, schema/version, tamaño, digest y transformaciones. Preservar whitespace/unicode del contenido; no trim, render HTML ni reparar JSON. Canonicalización del envelope no altera el texto.
3. Evaluador: proyección autorizada, referencias literales con offsets por puntos de código sobre esa proyección, señales y versiones; output estructurado validado, sin chain-of-thought.
4. Evidencia de seguridad: criterio, observación verificable y contexto declarado mínimo vinculados al Finding. Sin httpStatus, URL o cabeceras como prueba de vulnerabilidad.

Cada capa referencia a su origen y documenta extracción/acumulación/redacción. Un hash no anonimiza contenido ni autoriza retenerlo. Si redacción invalida el criterio o las citas, INCONCLUSIVE. No prometer detectar todos los secretos; las respuestas no sanitizables no se exportan ni se conservan como evidencia libre. Contenido para evaluación en memoria efímera; persistencia de proyección sensible cifrada y con caducidad, además de control de lectura local. Antes de datos reales se requiere una política explícita (D-09); metadata puede conservarse sin cuerpos.

El cifrado de evidencia se propone detrás de `EvidenceEncryptionPort`, con AES-GCM, nonce único, keyId/versión de envelope y AAD que liga Project/Scan/execution/tipo/versión. Clave externa distinta de la de credenciales, sin generación implícita ni lectura en consultas metadata-only; ausencia o fallo criptográfico bloquea captura, sin fallback plaintext. No se altera el envelope de credenciales v1 ni se prescribe un servicio cloud. Esta captura es una tarea separable: D0 no la necesita y D1 no la activa sin política y clave provisionadas.

### D-05 REST y OpenAI-compatible

REST: conservar v1 como `REST_JSON_LEGACY`; v2 emplea documento JSON base y `promptInputPath` para sustituir un único nodo existente por un String escapado. Ruta de salida selecciona un String único. Subconjunto determinista de JSONPath: raíz `$`, propiedad por punto o corchete, índice no negativo, índice final `[-1]` solo lectura y array no vacío. Sin scripts, filtros, recursión, wildcard ni múltiples resultados; no crear arrays implícitos, serializar objetos como texto o concatenar resultados sin declaración. Así se cubren estructuras anidadas/arrays sin lenguaje ejecutable. v1 ya cubre parte de ello; la mejora es contrato explícito y validación consistente.

OpenAI-compatible: URL completa configurable terminada en ruta compatible (no añadir `/v1` a ciegas), `model`, mensaje system opcional declarado y un mensaje user que contiene el prompt de prueba; `stream=false`. Un system configurado pertenece al contexto autorizado del target, no al evaluador. Extraer exclusivamente `choices[0].message.content` String y estado final compatible. Respuestas vacías, arrays multimodales, null, refusals sin texto, `tool_calls` y finish_reason de longitud/herramienta se distinguen como insuficientes/no soportadas; no ejecutar herramientas ni declarar COMPLETE por HTTP 200. D1 solicita una sola elección y no acepta selección implícita entre alternativas. Límites de output controlados por perfil y presupuesto. Los presets de proveedores son ayuda UI, nunca ramas en dominio. Un endpoint de inferencia solo representa esa interfaz: no el sistema RAG/agente de producción que no se está invocando.

Retries: default cero; GET de polling puede repetir como paso explícito, no es reenvío del prompt. Retry de POST solo si el contrato remoto soporta idempotency key y se configura esa garantía; timeout ambiguo después de enviar no se reintenta por defecto. Máximo propuesto dos retries con backoff acotado/jitter; 429/Retry-After no puede superar deadline. No retry de auth, política, parsing o evidencia; no failover silencioso de proveedor.

### D-06 Streaming y eventos

`InteractionEvent` v1 contiene sequence local, offset monotónico, kind START/TEXT_DELTA/TEXT_SNAPSHOT/USAGE/END/ERROR/WARNING y payload acotado. Datos remotos event/id son metadata no confiable; no identifican autorizaciones ni comandos. No se introduce un bus, broker ni API reactiva en dominio.

`SseAdapter` compone transporte + decoder SSE; JSONL usa otro decoder y misma colección. SSE respeta UTF-8 incremental, CRLF/LF, multiline data y frames separados por línea vacía; comentarios/heartbeats cuentan para bytes/eventos pero no para contenido ni reinician deadline absoluto. JSONL exige documento completo por línea y cota por línea. Nunca parsear un buffer de red como un evento completo.

La configuración declara selector de contenido, delta frente a snapshot, discriminador de error y condición de terminal (`[DONE]`, tipo de evento o campo/valor exacto). En delta se concatena en orden; snapshot sustituye y no se duplica. Un EOF solo completa si el contrato lo permite explícitamente; default exige terminal. No reconexión ni Last-Event-ID automáticos; una caída después de chunks queda INCOMPLETE. Error tras texto conserva prefijo saneado y fallo; no lo vende como respuesta final. Tool events generan warning/capacidad no soportada, nunca ejecución.

Backpressure: leer/decodificar/procesar secuencialmente con buffer acotado; no cola sin límite. Límites propuestos M3: total 60 s (cap operativo 120 s), inactividad de contenido 15 s, 1 MiB de bytes recibidos incluso descartados/descomprimidos, 64 KiB por evento, 4096 eventos, texto <=128 KiB. Son topes por interacción compartidos con el Scan, no se reinician por chunk o retry. Se corta y cierra conexión al primer límite; tiempo hasta primer contenido y causa de terminación son observables.

### D-07 DSL de workflows M4

Comparación: JSON es estricto y coincide con REST/JSONB; YAML es cómodo a mano pero añade aliases/tags/tipos ambiguos y parser; tablas por cada detalle facilitan consultas pero multiplican joins/migraciones. Recomendación: UI de pasos → AST tipado → JSON canónico con schemaVersion → JSONB en revisión inmutable. YAML e import/export de texto quedan para evolución; sin motor de expresiones, JS, SpEL, shell, referencias remotas ni plantillas ejecutables.

Ejemplo ilustrativo sin credenciales ni destino operativo:

```json
{
  "schemaVersion": 1,
  "origins": {"app": "https://target.example.invalid"},
  "credentialBindings": {"app": "target-primary"},
  "steps": [
    {"id": "create", "type": "http", "origin": "app", "method": "POST", "path": "/sessions", "body": {},
     "extract": [{"from": "$.id", "as": "sessionId", "type": "string", "sensitive": true}]},
    {"id": "answer", "type": "http", "origin": "app", "method": "POST", "path": "/sessions/{sessionId}/answer",
     "body": {"prompt": "{{PROMPT}}"},
     "collect": {"format": "sse", "contentPath": "$.delta", "mode": "delta", "terminal": {"eventType": "done"}}},
    {"id": "final", "type": "poll", "origin": "app", "method": "GET", "path": "/sessions/{sessionId}",
     "until": {"path": "$.status", "equals": "completed"}, "intervalMs": 1000, "maxAttempts": 5,
     "extract": [{"from": "$.messages[-1].content", "as": "finalAnswer", "type": "string", "sensitive": true}]}
  ],
  "result": {"variable": "finalAnswer"}
}
```

El POST y el polling son una composición posible, no requisitos de ColoqIA. También se puede seleccionar como resultado el texto completo de un stream sin polling. El ejemplo requiere que la API real documente esas rutas/condiciones; no se afirma que las tenga.

Variables: inputs reservados PROMPT/scanId/executionId; valores step-scoped extraídos como sessionId, conversationId o messageId. Declaración y uso anterior al consumo, tipo String/number/bool, longitud, sensibilidad y ámbito; sin overwrite de inputs ni secretos. Variables derivadas de respuestas son siempre no confiables y sensibles por defecto. Insertar en cuerpo mediante nodos JSON, en path como un único segmento percent-encoded (rechazar `/`, `..`, separadores/encoding ambiguo antes de construir), en query como valor codificado. Nunca en scheme/host/puerto, nombre de header, credential binding o política de autorización. Orígenes fijos en configuración validada; la API no puede concederles autorización operativa. No seguir URLs retornadas por el target.

Condiciones: `exists`, `equals`, `in` sobre valores tipados y HTTP status permitido; guard puede ejecutar/saltar solo el paso siguiente en lista acíclica, sin goto/recursión. Errores y variable ausente/tipo incorrecto fallan con código y stepId. Poll es la única repetición declarativa, con stop-success, terminal-failure y máximo de intentos; no reenvía POST. Cleanup opcional HTTP se restringe a una sesión creada por esa ejecución y también consume presupuesto; fallo se registra sin ocultar el error principal. GET/POST inicialmente, DELETE solo cleanup aprobado, sin métodos arbitrarios.

Defaults propuestos M4: <=8 pasos +1 cleanup, <=16 variables, cada valor <=4 KiB, <=10 requests totales (incluyendo polls/retries/cleanup), deadline de interacción <=120 s y siempre menor que el restante del Scan; un solo stream activo. Reservar presupuesto para cleanup; si no queda o el proceso cae, no prometer borrado remoto. Sesión nueva por TestExecution para evitar contaminación entre categorías. Reanudar tras reinicio y sesión compartida por Scan quedan fuera.

### D-08 API y frontend progresivos

Conservar endpoints/DTOs v1 para targets legacy. Proponer `/api/v2/projects/{projectId}/target-integration` GET/POST/PUT, mismo recurso lógico y cardinalidad; schemaVersion+adapterType discriminan config. GET devuelve configuración segura, revisión y hasCredentials, jamás secretos. PUT con expectedRevision evita actualización perdida; 409 ante revisión obsoleta. v1 sigue operativo sobre legacy y devuelve conflicto seguro si se intenta editar un target v2; no reconvierte ni pierde campos. La v2 puede leer y convertir explícitamente una configuración legacy, con preview y conservación de identidad.

La ruta actual de inicio de Scan se conserva; la revisión seleccionada se fija al preparar el plan, como hoy, y el perfil requerido se deriva de esa revisión, no de un flag que el target pueda cambiar. API de resultados v2 expone perfiles, adapter y completitud; el endpoint v1 no proyecta v2 como v1: devuelve incompatibilidad explícita y guía a v2. Antes de activar scans v2, Angular deberá consumir ambas versiones; no se modifica report-v1 para hacerlo parecer compatible.

El PUT v2 incluye `assessmentContext` tipado por las cuatro categorías (solo expectativas y referencias artificiales autorizadas), con revisión/hash propios fijados junto al target, y una referencia de política de datos. Un panel «Criterios y datos autorizados» permite declarar estos prerrequisitos y muestra qué categorías quedarían inconclusas. Guardarlos no concede permisos: la autorización operativa de exportación se resuelve fuera de los datos controlados por el cliente y debe coincidir con revisión/proveedor. No se recoge ground truth en este formulario ni se solicitan secretos de producción como canarios. Esta entrada de contexto es necesaria para D1 útil; no se pospone implícitamente a un motor de workflows.

UX: selector REST JSON / OpenAI-compatible / Streaming / Workflow avanzado, con campos exclusivos por tipo. REST muestra documento base, ubicación de prompt y selector de salida con preview local sobre ejemplo aportado manualmente. OpenAI muestra endpoint, modelo, system opcional, autenticación y límites; roles fijos iniciales. Streaming añade formato, mapeo de evento y señal final. Workflow ofrece tarjetas ordenadas Crear sesión/Enviar/Poll/Recoger stream, extracción y selector final, errores por paso y presupuesto total visible. Mostrar vista previa del plan sin secretos ni tráfico; editor JSON queda fuera de la experiencia primaria.

Guardar y validar son operaciones locales. «Comprobar conexión» futuro exige acción explícita y muestra destino, número máximo de peticiones/coste; nunca ocurre por abrir formulario, cambiar tipo o guardar. Conservar/Reemplazar/Eliminar credencial y limpieza de memoria se mantienen; cambiar origen requiere nueva credencial o referencia autorizada, sin arrastre automático. Mostrar errores técnicos separados de verdict, inconclusos y cobertura por criterio. URLs y salida siempre texto inerte, sin HTML ni storage sensible en navegador.

### D-09 Seguridad y privacidad por diseño

| Riesgo | Control propuesto y comportamiento al fallar |
|---|---|
| SSRF por workflow o variable | Orígenes inmutables allowlisted externamente; validación de URL final antes de cada intento; variables solo en segmentos/valores |
| DNS rebinding/TOCTOU | Resolver acotado por exchange; validar todas las direcciones IPv4/IPv6 y bloquear rangos especiales/interfaces; fijar conexión a IP validada manteniendo hostname TLS/SNI; no segunda resolución del cliente |
| Redirect indirecto | Default cero redirects. Cualquier soporte futuro reautoriza cada salto, prohíbe downgrade y no reenvía auth entre orígenes; D1..M4 no siguen Location |
| Red privada/autorización insuficiente | Bloqueo por defecto incluso con allowlist; excepción local explícita existente no se generaliza. Ollama privado precisa decisión operativa separada y origen exacto, nunca bypass por adapter |
| Secretos por múltiples endpoints | Referencias cifradas AES-GCM existentes, ligadas a origen/uso; descifrar tras validar destino; nunca variables de workflow, URLs ni headers persistidos con valor secreto |
| Headers/cookies | Headers permitidos validados y límites; bloquear Host, framing, proxy y overrides de auth. Secretos solo en slots especiales. No cookie jar, OAuth o refresh automático inicial |
| Streaming infinito/flooding | Deadline absoluto, inactividad de contenido, bytes descomprimidos, eventos, profundidad JSON y buffer acotados; cerrar socket e interrupción cooperativa |
| Amplificación por pasos/retries | Presupuesto atómico global de requests/bytes/deadline/cuota/concurrencia; reservas de intento antes de red; mismos límites para cleanup |
| Exfiltración al evaluador | Permiso por target/revisión, proveedor, campos y finalidad independiente del permiso de red; default denegado; proyección mínima y validación de referencias |
| Respuestas sensibles en evidencia | Cifrado de proyección persistida, TTL explícito, lectura solo por política, logs sin cuerpo; tokens de sesión sensibles y efímeros; no copias brutas por comodidad |
| Prompt injection indirecta | Texto del target delimitado/no confiable, sin tools, navegación o instrucciones ejecutadas; contexto confiable no derivado de su contenido |
| Abuso de API local | Mantener entorno restringido actual; scoping de IDs no es autenticación. No publicar scanner v2 ni admitir multiusuario sin diseño de identidad/acceso separado |

Para aceptación real D1/D2 se recomienda inicialmente usar únicamente datos sintéticos/no sensibles en targets reales autorizados y un corpus de respuestas no enumerado previamente. Aun así, hay consentimiento explícito del propietario y proyección permitida. Datos reales sensibles quedan bloqueados hasta aprobar retención, acceso, cifrado y autorización de exportación. Recomendación de retención inicial: contenido persistido deshabilitado por defecto; si se habilita, TTL explícito <=7 días, purga idempotente y exclusión/expiración de backups acordadas antes de activarlo. Metadatos sin contenido siguen su política operativa; no fijar una retención legal universal. No se crea infraestructura de secretos ni jobs ahora.

### D-10 Persistencia e invariantes

Diseño lógico para migraciones aditivas futuras, sin reservar número Flyway ni escribir SQL ahora:

| Cambio | Propósito y restricciones |
|---|---|
| target_integration_revisions | UUID, target FK, revision unique por target, adapter_type/version, schema_version, config JSONB seguro, hash, createdAt; append-only |
| target_configurations.active_integration_revision_id nullable | null significa legacy, no rellenar históricos ni eliminar columnas legacy; FK y consistencia por target |
| scan_integration_snapshots | scan PK/FK, target/revision, perfil de interacción/evaluación/reporte y referencia de política; sin credenciales, tokens, URLs con datos ni cuerpos sensibles |
| normalized_interactions | execution FK único, estados/completitud/versiones, contadores/tiempos y refs de evidencia; httpStatus opcional por exchange |
| interaction_evidence | interacción FK, tipo/version, datos saneados o ciphertext, expiresAt y transforms; metadata consultable sin descifrar; cotas por fila/Scan |
| referencias en evaluation_results | vínculos de evidencia/perfil prospectivos; contexto/criterio versionado; no reutilizar columnas heredadas sin mapeo |
| configuración criptográfica | reutilizar credencial cifrada existente para binding principal; múltiples bindings pospuestos a M4 y con tablas/refs scoped, sin duplicar plaintext |

La entidad JPA actual reconstruye siempre `TargetSettings` y exige `url/request_template/response_path` no nulos: el mapper v2 deberá discriminar por revisión antes de construir el modelo legacy. Una migración nueva conservará esas columnas pero sustituirá sus NOT NULL por checks condicionales: rama legacy exige todos sus campos originales; rama v2 exige revisión válida y permite null en los campos exclusivos legacy. Auth/cifrado principal y sus checks permanecen obligatorios por su contrato. No rellenar plantillas ficticias para satisfacer constraints ni permitir que un binario viejo interprete la rama nueva. La selección legacy/v2 debe ocurrir también en las proyecciones de admisión y repositorios.

El perfil nuevo no escribirá proyecciones sensibles en `request_payload/response_payload` ni `evaluation_results.evidence_projection` legacy como copia plaintext del almacén cifrado. Sus referencias y disponibilidad se resolverán por la capa de evidencia v2. Cuando no se autorice captura, se permite evaluar en memoria y conservar únicamente metadata/resumen verificable no sensible; el contenido no será recuperable para reanálisis. Tras caducidad, la referencia queda EXPIRED con versión/metadata mínima y no se afirma que el contenido siga disponible; el informe histórico inmutable describe lo observado al evaluar, sin regenerarse ni restaurar cuerpos desde logs.

Al preparar el plan, fijar revisión pública y política; la configuración con cifrado sigue solo en memoria. Preservar `targetUpdatedAt` por compatibilidad pero usar revisionId como identidad recuperable. La revisión pública puede contener rutas/plantillas de datos sintéticos: rechazar secretos literales, no asumir que JSONB es seguro por naturaleza. Ningún endpoint devuelve ciphertext.

Cambiar constraints HTTP de TestExecution solo para perfil de interacción nuevo y mediante comprobación explícita de perfil; nunca relajar sin condición el histórico. Planificar índices/FKs/unique/cotas y round-trip de datos al diseñar cada migración real. Nuevas migraciones, nunca editar V1..Vn ejecutadas. Target/config update y puntero de revisión se confirman juntos. Evidencia y resultado terminal se publican coherentemente antes de evaluación; no transacciones abiertas durante red ni parsing de streams. El resultado de evaluación y Finding permanecen atómicos; score, informe y Scan COMPLETED también.

### D-11 Evaluación real, scoring y report

Perfil propuesto `authorized-app-v2`: `AssessmentContext` registrado por propietario/operador autorizado, ligado a target revision y test revision, contiene restricciones verificables, límites del caso, canarios artificiales y contrato de salida cuando existan. No sustituir `SyntheticContext` por texto inventado ni aceptar políticas autodeclaradas por una respuesta. Reutilizar reglas aplicables y consolidación, cambiando preparación/versiones/proyección y textos que hoy afirman escenario sintético. El motor continúa recibiendo criterio, señales y texto, sin saber si procede de REST o SSE.

LLM01 necesita prohibición/acción observable declarada; LLM02 y LLM08 necesitan referencias artificiales reservadas conocidas o un criterio verificable equivalente aprobado; LLM10 sigue OUTPUT_ONLY con contrato de seguridad conocido, sin demostrar ejecución en sink. Sin esos prerrequisitos, INCONCLUSIVE/CRITERION_NOT_APPLICABLE, no NOT_VULNERABLE. Las cuatro pruebas v1 se mantienen como probes iniciales: su utilidad/recall fuera del escenario puede ser baja y se mide, no se presupone. Contexto de aplicación no equivale a ground truth experimental. No introducir LLM-only ni autoconfirmación por el target.

Para datos sintéticos en target real, reemplazar en el perfil v2 el permiso de corpus enumerado por política explícita de proyección: salida normalizada saneada y acotada, contexto mínimo y prompt de catálogo; proveedor declarado, consentimiento y rechazo si datos no autorizados. La autorización v1 permanece exacta. Validar por separado las decisiones de resultado y exportación: una señal independiente fuerte conserva el tratamiento híbrido aprobado; la falta de permiso nunca autoriza enviar datos. No cambiar reglas de confidence/severity sin nueva versión si sus fórmulas/precondiciones dejan de cumplirse.

Separar cuatro ejes: severity=impacto justificado; confidence=soporte heurístico del veredicto, no probabilidad; evidenceStrength=NONE/SUPPORTING/STRONG con criterio y completitud; scannerScore=resumen heurístico versionado y coverage=inconclusos/aplicabilidad. No multiplicar severity×confidence ni usar estado del transporte como severidad.

Recomendación D1/D2: mantener fórmula scoring-v1 y sus cuatro inputs estructurados cuando sean compatibles, identificando además evaluationProfile y testPlanVersion; no presentarlo como calibrado para aplicaciones reales ni comparable entre perfiles. Si se modifica la fórmula/precondiciones, crear scoring-v2 en otro incremento; propuesta futura: score basado en impacto y cobertura separada, sin bonus de confidence, pendiente de calibración/aprobación. No alterar scores históricos.

Se requiere `report-v2` prospectivo con las mismas fuentes estructuradas, trazabilidad de adapter/revisión, restricciones de cobertura/aplicabilidad, privacidad y estado técnico. No retirar las nueve limitaciones de report-v1 ni copiarlas literalmente al report-v2: «SSE excluido» y «solo escenario sintético» serían falsas para el nuevo perfil. D1/D2 no añaden exportadores. No usar report-v1 para cerrar un Scan de perfil nuevo.

### D-12 Observabilidad mínima

Logs estructurados existentes con correlationId del Scan, executionId, interactionId, stepId y attempt; MDC queda en adapters/config, no dominio. Eventos de inicio/fin/denegación con adapter/version, código de error cerrado, duración, contadores y completitud. CorrelationId se genera localmente; no se confía en headers remotos. Identificadores internos no contienen sesión/token/model output.

Métricas: latencia total/primer contenido, exchanges por ejecución, fallos por adapter y categoría técnica, truncamientos, streams sin terminal, bytes/eventos descartados, evaluaciones suprimidas/fallidas e inconclusos por motivo. Labels de cardinalidad acotada (tipo/versión/código), nunca URL completa, sessionId, ScanId o texto. Diagnóstico de adapter no es una vulnerabilidad. Contadores de uso/coste solo si el proveedor los devuelve o hay estimación versionada, nunca inventados. Sin plataforma nueva de observabilidad ni dashboards obligatorios en D1.

### D-13 Validación y retirada de la dependencia de demo

Tres niveles separados: contratos de protocolo con fixtures sintéticas mínimas; integración controlada con targets reales expresamente autorizados; calidad de detección con dataset etiquetado independientemente. La demo sigue como evidencia TFM/fixture histórica, no como condición de runtime ni definición del comportamiento universal del target.

Proponer posteriormente tag/release `tfm-v1.0` en commit revisado e identificado por el usuario; no asumir que HEAD es la entrega ni crear tag ahora. Después retirar demo del runtime por defecto sin borrarla; valorar `legacy/tfm-v1/demo-vulnerable` frente a mantener carpeta documentada según referencias existentes. No duplicar árbol ni romper manifiestos/evidencias históricas. Un traslado se hará en un change autorizado separado.

Fixtures: HTTP JSON anidado/arrays, errores auth/429, UTF-8 dividido, multiline SSE, snapshots/deltas, final ausente, JSONL parcial, event flooding, timeout, sesiones/polling, DNS/redirects/variables maliciosas simulados, sin red externa en automatización. Probar el mismo texto/criterio transportado por adapters distintos produce la misma entrada de evaluación/veredicto. Golden/round-trip de v1 para API, constraints, score y report históricos; no crear tests que solo reflejen nombres de métodos.

Corpus real: registrar permiso, target/application version, protocolo, modelo/config conocidos, fecha, criterio/contexto y revisión de prueba, límites, dataset split y etiquetado. Propietario valida ground truth fuera del evaluador; dos revisores cuando sea viable y desacuerdos identificados. No se afirma conocer el system prompt o todos los secretos de una caja negra. Endpoints sin ground truth sirven para validar conectividad y aplicabilidad, no precision/recall. Evitar fuga de ejemplos al prompt evaluador y separar calibración de evaluación; agrupar splits por aplicación/familia para reducir contaminación.

Con ground truth positivo/negativo adjudicado y predicción concluyente: TP=positivo→V, FP=negativo→V, TN=negativo→N, FN=positivo→N. INCONCLUSIVE es abstención, no FP/FN automático; truth ambiguo/no adjudicado queda fuera de matriz binaria y se cuenta aparte. precision=TP/(TP+FP), recall_conclusive=TP/(TP+FN), FPR_conclusive=FP/(FP+TN), FNR_conclusive=FN/(TP+FN). Denominador cero → N/A, nunca 0. Publicar siempre soporte y cobertura concluyente sobre elegibles, inconclusos positivos/negativos y fallos técnicos. Complementar con recall efectivo TP/todos_los_positivos_elegibles (incluye abstención/fallo) y tasa de positivos no detectados para evitar inflar resultados al abstenerse; no llamar FN a un inconcluso. Métricas por categoría/adapter/aplicación, micro y macro solo con soporte explícito; intervalos/incertidumbre y repeticiones por no determinismo. No declarar umbrales de calidad ni métricas logradas antes de dataset suficiente.

## Risks / Trade-offs

- [Conectividad se confunde con detección] → gates separados de transporte, aplicabilidad y calidad; INCONCLUSIVE explícito, no promesa de «cualquier LLM».
- [DSL se convierte en lenguaje de programación] → AST cerrado, lista acíclica, polling único, límites compuestos y UX de tarjetas; posponer M4.
- [Reintento duplica generación/coste] → cero por defecto, garantía de idempotencia y presupuesto antes de cada envío.
- [Compatibilidad OpenAI solo nominal] → perfil de capacidades y contratos de prueba; no asumir tools/multimodal/stream idénticos.
- [Privacidad reduce evidencia útil] → política explícita y minimización; aceptar inconclusos cuando no sea posible conservar/verificar sin divulgar.
- [Streaming y workflows ocupan workers] → mantener concurrencia/cuota global acotadas; plazos por interacción dentro del Scan, no crear pools ilimitados.
- [Reutilizar score v1 sugiere comparación inválida] → identificar perfiles/coverage y advertir heurística; no mezclar series.
- [Estado de sesiones remotas tras caída] → no replay automático, expiración remota acordada y limpieza best-effort; no garantizar rollback de red.
- [Catálogo v1 sobreajustado al TFM] → mantener trazabilidad, medir abstención y diseñar después revisiones de tests aprobadas, sin mutarlos silenciosamente.
- [Cifrado y TTL no cubren backups] → gate operativo de retención/copias antes de permitir evidencia sensible; no afirmar anonimización.

## Migration Plan

1. Revisar/aprobar este diseño y únicamente el delivery autorizado. Registrar decisiones aprobadas en ADR v2 relacionada, sin reescribir ADR-001/002/005/008. Sin aprobación no aplicar tareas.
2. Diseñar migraciones aditivas específicas y verificaciones de compatibilidad con datos sintéticos; defaults mantienen legacy. Identificar release histórica por separado.
3. Implementar D1 con puente legacy, revisions, puertos neutrales y REST/OpenAI; API/frontend compatibles; nuevo perfil e informe habilitados solo tras consentimiento y contexto aplicable.
4. Activación opt-in por configuración nueva; legacy sigue intacto. Sin cambios en runtime de producción/AWS por este change de planificación.
5. D2 añade SSE/JSONL y prueba equivalente con el mismo motor. La aceptación inicial completa exige un proveedor OpenAI-compatible autorizado y un SSE autorizado, sin lógica comercial en dominio.
6. M4 y estabilización M5 se revisan de forma independiente, no se arrastran automáticamente al primer delivery.
7. Rollback funcional: desactivar nuevas admisiones v2, dejar terminar o terminalizar trabajos según recuperación existente, conservar lector de perfiles históricos y filas nuevas. No downgrade destructivo ni reconvertir target v2 a legacy. El binario anterior no es seguro si existen targets v2; mantener versión compatible de lectura o restauración operativa aprobada por separado. No borrar evidencia para «arreglar» compatibilidad.

### Milestones y gates

| Hito | Entrega verificable | Dependencias / gate |
|---|---|---|
| D0 / M1a | Interacción normalizada en memoria con adapter/puente legacy, sin BD/API/UI nuevas | Primera unidad recomendada tras aprobación; mismo comportamiento observable v1 y sin tráfico externo |
| M1 | Contrato neutral y REST mejorado, JSONPath restringido, límites, auth existentes | Aprobación de D-01..05/08..11; compatibilidad histórica |
| M2 | OpenAI-compatible stream=false sin ramas por proveedor | M1; aceptación D1 con un endpoint autorizado y contexto/política explícitos |
| M3 | SSE y JSONL, eventos/terminal/partial, diagnostics | D1; aceptación D2 con SSE real autorizado y equivalencia de evaluación |
| M4 | Workflow visual, sesión/poll/stream/cleanup acotados | M3; revisión de seguridad y credenciales por origen |
| M5 | Suite de conformidad y documentación de extensión del registro estático | Base desde M1; estabilizar tras M4; WebSockets/tools no incluidos |

M1 completo puede dividirse: D1 incluye solo escritura en ruta única, lectura de String único y retries cero; retry idempotente configurable es tarea posterior M1b, sin bloquear D1. D1 no contiene DSL ni SSE. D2 completa el caso de aceptación inicial, no se declara realizado por aprobar el diseño.

D1 se divide en cortes revisables: (a) revisiones/contratos API y REST v2 sin activar nuevos scans; (b) adapter chat y fixtures; (c) contexto/proyección autorizada, reporte y lectura frontend antes de activación. Ningún corte intermedio afirma aceptación funcional v2. Si se quiere una sola sesión pequeña de implementación, elegir D0, no todo D1. De esta forma el change diseña la evolución completa sin exigir una reescritura en el primer PR.

## Open Questions

Decisiones a revisar: se presentan opciones y recomendación sin convertirlas en definitivas. No bloquean entregar planificación; las gates indicadas bloquean activar la implementación/operación correspondiente.

| ID | Pendiente y alternativas | Recomendación / gate |
|---|---|---|
| Q1 | Aprobar contratos y perfiles v2 vs ampliar silenciosamente v1 | Aprobar la propuesta prospectiva; gate antes de código |
| Q2 | Primer proveedor Groq u OpenRouter, endpoint SSE y autorización concreta | Seleccionar uno de chat y uno SSE con permisos y límites documentados; gate de aceptación externa, no de diseño |
| Q3 | Datos sintéticos en app real vs datos sensibles; conservación y exportación | D1/D2 solo datos no sensibles con consentimiento; contenido persistido opt-in/TTL, sensibles bloqueados; gate antes de red real |
| Q4 | JSONPath propio restringido vs biblioteca con evaluación limitada | Reutilizar parser existente ampliando solo índice final/corchetes si procede; cualquier dependencia nueva requiere aprobación separada, mismo contrato observable |
| Q5 | Runtime demo en carpeta actual vs legacy | Mantener ahora; identificar commit TFM y revisar referencias antes de traslado en change posterior |
| Q6 | Score v1 con perfil visible vs score v2 sin confidence | Reutilizar fórmula congelada en D1/D2, sin comparar perfiles; nuevo score solo tras dataset/aprobación |
| Q7 | Valores exactos de límites por entorno y TTL autorizado | Adoptar topes conservadores propuestos y reducir según target; confirmar antes de operación, sin ilimitados |
| Q8 | APIs privadas/self-hosted y autenticación más compleja | Bloqueo por defecto; diseño de autorización explícita posterior, no activar excepción demo en producción |

### Fuera del MVP / Evolución futura

WebSocketAdapter, ToolCallingAdapter, OAuth/cookies, sesión compartida/multiturno, reanudación durable, UI de revisión manual, score calibrado, nuevas revisiones/corpus de probes, RAG específico, identidad multiusuario y operación distribuida requieren cambios posteriores. Las interfaces no incorporan ahora operaciones especulativas para esas capacidades.
