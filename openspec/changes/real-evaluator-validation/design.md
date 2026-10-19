## Context

Motivación y alcance: [proposal.md](proposal.md). Contrato operativo: [spec.md](specs/real-evaluator-validation/spec.md), REV-01..REV-10. Solo planificación, sin ejecución. Fuentes contrastadas el 2026-10-19:

- `backend/src/main/java/com/llmscannerseguridad/config/EvaluationConfiguration.java` y `SecurityTestExecutionConfiguration.java`.
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/`: OpenAiEvaluatorAdapter, EvaluatorSettings, EvaluatorMessages, EvaluatorInvocationGate, LocalEvaluationScenarioAdapter y EvaluationEvidenceProjector.
- `backend/src/main/java/com/llmscannerseguridad/application/evaluation/`: EvaluateExecutionUseCase, EvaluationCallBudget y EvaluationMetadata.
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/evaluation/PostgresEvaluationRepositoryAdapter.java` y `domain/evaluation/HybridDecisionPolicy.java`.
- `demo-vulnerable/manifest/`: benchmark-v1, ground-truth-v1, plantilla VULNERABLE y schemas; configuración de demo y `frontend/proxy.conf.json`.
- ADR-004, `docs/04-ia/estrategia-evaluacion.md`; evaluation-engine EE-05..EE-10, demo-vulnerable, e2e-integration E2E-16..E2E-24 y su runbook; frontend-mvp tasks 9.4/FE-24/D-30.

No se ha inspeccionado el entorno secreto de ningún proceso ni verificado una cuenta externa en esta revisión. Los valores siguientes son configuración/defaults reales del código, no afirmaciones sobre servicios actualmente activos. El preflight registrado posteriormente en tasks.md identificó una contradicción: el schema auxiliar de demo exige enabled=false y REV-04 requiere validar una copia autorizada con true. D-08 diseña su corrección, aún no implementada. La prohibición de exportar del E2E offline conserva su alcance histórico.

## Goals / Non-Goals

**Goals:** validar el transporte y parseo reales por el puerto existente, mantener independiente la consolidación híbrida y trazar el mismo Scan hasta UI. Registrar honestamente detección, abstención, discrepancia o fallo.

**Non-Goals:** los de proposal; tampoco introducir SDK, endpoints de diagnóstico, migraciones, herramientas, scripts ni nuevos parámetros del evaluador. No cambiar el dominio o los casos de uso. La autorización posterior de reparación backend se limita a D-09; no otros cambios de runtime, frontend o demo. Se conserva el diseño histórico de corrección del schema auxiliar D-08.

## Decisions

### D-01 Configuración real inspeccionada

| Aspecto | Valor observado en código y configurabilidad |
| --- | --- |
| API | POST Responses API, `https://api.openai.com/v1/responses`; constante ENDPOINT, sin base URL configurable. |
| Formato | Structured Outputs `text.format=json_schema`, strict=true, schema evaluator-output-v1; `store=false`. |
| Proveedor | `EVALUATOR_PROVIDER`, default OPENAI; el validador solo admite OPENAI. |
| Modelo solicitado | `EVALUATOR_MODEL`, default `gpt-6-luna`; configurable. No se ha comprobado disponibilidad real. |
| Clave | `EVALUATOR_API_KEY`, sin default; el adaptador la obtiene para transporte. No es OPENAI_API_KEY. |
| Gate global | `EVALUATOR_EXPORT_ENABLED`, default false. |
| Allowlist | `EVALUATOR_ALLOWED_SCENARIOS`, default vacío, lista separada por comas. Usar solo demo-vulnerable-v1. |
| Archivo | `EVALUATION_SCENARIO_FILE`, sin default; JSON local. |
| Gate manifest | exportPolicy.enabled=true y provider OPENAI coincidente; binding projectId/targetConfigurationId/targetUpdatedAt exactos. |
| Gate de evidencia | Contexto aplicable, evidencia completa, respuesta extraída coincidente exactamente con exportableResponses y tamaños válidos. No basta autorizar el host. |
| Timeout | `EVALUATOR_TIMEOUT_MS` default/cap 20000 ms; rango permitido 1000..20000. Incluye preparación, claim, espera de cuota y transporte, acotado por presupuesto restante del Scan. |
| Transporte | DNS hasta 2000 ms; connect/TLS hasta 5000 ms cada uno, siempre acotados por tiempo restante. Sin retries, redirects ni proxy configurable del target. |
| Cuota | `EVALUATOR_MAX_CALLS` default/cap 4, rango 1..4; un intento durable por evaluación, sin reenvío por commit ambiguo. |
| Concurrencia y frecuencia | Semáforo de un permiso compartido por el adaptador singleton de esta instancia; `EVALUATOR_INTERVAL_MS` default 1000, rango 1000..60000; distancia mínima entre inicios, no cuota externa garantizada. |
| Tokens | `EVALUATOR_MAX_OUTPUT_TOKENS` default/cap 1024, rango 256..1024. Hasta 4096 tokens de salida autorizados entre cuatro llamadas; no equivale a total de tokens ni factura máxima. |
| Tamaños | `EVALUATOR_EVIDENCE_BYTES` 16384, `EVALUATOR_INPUT_BYTES` 24576, `EVALUATOR_RESPONSE_BYTES` 16384, `EVALUATOR_RECORD_BYTES` 65536 por defecto. El exceso no se trunca para enviar. |
| Deadline Scan | `TEST_EXECUTION_SCAN_BUDGET_MS` default 300000 ms, compartido con ejecución del target. |

Los application*.yml no declaran overrides del evaluador en la búsqueda dirigida. Los overrides de proceso deberán confirmarse antes de ejecutar sin volcar entorno ni secretos. Se recomienda fijar explícitamente el modelo/defaults anteriores para trazabilidad; no escoger otro modelo en silencio si no existe o la cuenta carece de acceso.

### D-02 Coste sin inventar precios

Un único Scan inicial, hasta cuatro llamadas, sin retries automáticos o manuales. Se conservan los límites actuales; reducir la cuota impediría validar las cuatro invocaciones y ampliar tokens/timeout requeriría otro cambio. No hay presupuesto monetario por defecto: `EVALUATOR_MAX_ESTIMATED_COST_USD` es null si no se configura.

La opción recomendada para la ejecución es el gate monetario ya existente con importe aprobado, sin añadir código. Exige:

- `EVALUATOR_MAX_ESTIMATED_COST_USD` y `EVALUATOR_ESTIMATED_MAX_CALL_COST_USD` positivos.
- `EVALUATOR_PRICING_PROVIDER`, `EVALUATOR_PRICING_MODEL` coincidentes con el proveedor/modelo solicitado.
- `EVALUATOR_PRICING_VERSION`, `EVALUATOR_ESTIMATE_VERSION`, `EVALUATOR_PRICING_DATE` ISO, `EVALUATOR_PRICING_CURRENCY=USD`.
- `EVALUATOR_INPUT_PRICE_PER_MILLION` y `EVALUATOR_OUTPUT_PRICE_PER_MILLION` no negativos y confirmados por el operador; no se consultan por red en esta tarea.

Reserva por intento; si perfil inválido o suma de reservas excede el límite, no llama y registra EVALUATOR_BUDGET_EXCEEDED, suprimiendo posteriores. La reserva no se libera como gratuita ante ambigüedad. Para admitir cuatro llamadas el presupuesto estimado debe cubrir cuatro reservas; no se inventa un importe. Alternativa existente: operar sin gate monetario con aceptación explícita del operador, conservando límites de llamadas/tokens; deja un coste máximo en USD sin garantizar. La elección y los importes quedan pendientes para el preflight y no cambian el contrato ni las tareas.

El parser recoge `usage.input_tokens`, `usage.output_tokens`, modelo resuelto e id de respuesta cuando la respuesta completada es válida. El coste estimado solo se calcula con usage/tarifa válidos y coincidencia exacta del modelo resuelto con el solicitado; un alias resuelto distinto puede dar coste null. El presupuesto estimado y usage son datos distintos. No hacer llamadas para consultar tarifas ni verificar previamente el modelo: el operador confirma acceso y coste antes del único Scan.

### D-03 Preparación local y recorrido futuro

Secuencia reproducible, propuesta y **no ejecutada**:

1. Autorizar la sesión operativa y registrar modelo/coste, límites no secretos y procedencia de configuración del proceso. Comprobar esquema local existente sin lanzar migraciones como parte de esta planificación.
2. Usar PostgreSQL local puerto 5432, BD `llm_scanner_seguridad`; no tests ni BD `_test`, dumps o limpieza. Acceso/credenciales resueltos por el operador sin incluirlos en evidencia.
3. Disponer de demo `http://127.0.0.1:8081`, `DEMO_PROFILE=VULNERABLE`, `DEMO_BIND_ADDRESS=127.0.0.1`, `DEMO_PORT=8081`. GET /health: UP, demo=true, VULNERABLE, scenarioVersion=1. No enviar probes adicionales de preflight.
4. Scanner `http://localhost:8080`, `SPRING_PROFILES_ACTIVE=local`, `TARGET_LOCAL_DEMO_ENABLED=true`, `TARGET_LOCAL_DEMO_ORIGINS=http://127.0.0.1:8081`. Esta excepción exacta evita ampliar la política SSRF; no localhost alternativo/wildcards/redirecciones. Sin otro Scan activo.
5. Suministrar la clave al entorno del scanner con entrada segura del operador, sin literal en comandos/historial, sin archivo .env ni lectura/echo del valor. Confirmar únicamente presencia. Configurar proveedor/modelo, gates y límites de D-01/D-02 para esta sesión. La clave no se entrega al target ni a Angular. Si hay que cambiar un proceso existente, hacerlo solo sin Scan activo y bajo autorización de arranque/reinicio.
6. Angular `http://localhost:4200`, proxy `/api/**` a scanner `http://localhost:8080`. Confirmar UI/API; no tocar proxy/configuración versionada. Reutilizar Project por id explícito verificado o crear uno sintético identificable; no seleccionar por nombre ambiguo.
7. Preparar/releer Target mediante contrato existente: POST `http://127.0.0.1:8081/api/chat`, application/json, requestTemplate string `{"message":"{{PROMPT}}"}`, promptPlaceholder `{{PROMPT}}`, responsePath `$.response`, headers {}, authType NONE, sin authConfig y timeoutMs=5000. No volver a guardarlo tras el binding.
8. Preparar en ubicación local no versionada la copia de `demo-vulnerable/manifest/evaluation-vulnerable.template.json`; sustituir REPLACE_PROJECT_ID, REPLACE_TARGET_ID y REPLACE_TARGET_UPDATED_AT con valores reales exactos. Cambiar únicamente exportPolicy.enabled a true; conservar provider OPENAI, contexto y corpus. `EVALUATION_SCENARIO_FILE` debe señalar esa copia. No editar plantilla/manifest versionados ni código demo. La preparación de esos archivos locales pertenece a la futura operación autorizada.
9. Antes de reanudar, implementar y comprobar la corrección D-08; no omitir la validación mientras el schema vigente siga fijando const:false. Validar el documento con `evaluation-scenario-v1.schema.json` generalizado; un escenario demo-vulnerable-v1/revision="1", cuatro entries code/revision=1. Preparar binding local conforme a bindings-v1.schema.json con schemaVersion=1, benchmarkVersion="1", profile=VULNERABLE, IDs/fecha reales, checkedHealth y authorizationDeclaredByOperator=true. Releer Target/fecha antes de lanzar; si no coincide, detener y preparar de nuevo. El scanner lee el escenario al preparar el Scan; bindings es registro del operador, no entrada adicional del adaptador.
10. Abrir proyecto y target preparado en Angular. Pulsar «Ejecutar análisis» una sola vez. Registrar POST `/api/projects/{projectId}/scans`, 202, id/Location realmente recibidos. Respuesta ambigua: consultar histórico por id/instante o detener; nunca repetir POST automáticamente.
11. Observar polling existente Angular hasta COMPLETED o FAILED; no segundo poller paralelo. Tope de observación 360 s desde intento (deadline backend default 300 s); agotarlo no cambia estado del servidor ni autoriza relanzar.
12. Leer GET scoped del mismo Scan, `/executions`, `/evaluations`, `/findings`, `/score` y `/report`, bajo `/api/projects/{projectId}/scans/{scanId}`. Lecturas locales con timeout 10 s y cero redirects. Verificar cuatro categorías y relaciones reales; releer report sin regenerarlo. No invocar OpenAI directamente ni consultar el target otra vez para reconstruir evidencia.
13. Opcionalmente, recoger metadatos internos no secretos conforme a D-05 si existe autorización y disponibilidad; en otro caso registrar NOT_INSPECTED sin bloquear la aceptación funcional. Comparar externamente contra ground truth conforme a D-06. Conservar solo los recursos del proyecto/Scan autorizados.
14. En Angular revisar estado, score/riesgo/categorías, cuatro evaluaciones, incertidumbre/degradación y Findings reales. Si existe uno, abrir sus detalles y cotejarlo con API (categoría, severidad, confianza heurística, explicación/evidencia/recomendación y referencias); contenido inerte, sin ejecutar markup. Revisar report, interpretación y limitaciones.
15. Registrar los tres resultados independientes de D-04 y la cobertura exacta de 9.4. Aplicar cierre D-07 también ante error, sin retries para mejorar resultados.

### D-04 Expected behavior y aceptación

La integración existente mantiene Angular → REST → aplicación/dominio → puertos → adaptadores → PostgreSQL/target/evaluador. No se acopla dominio al proveedor ni se añade funcionalidad. Las decisiones aquí propuestas afectan a la validación operativa.

| Eje | Criterio |
| --- | --- |
| Integración del proveedor | Cuatro llmStatus SUCCEEDED y respuestas estructuradas localmente aceptadas: validación completa de las cuatro llamadas. La lectura interna de metadata es diagnóstica y opcional; NOT_INSPECTED no impide esta aceptación. De una a tres válidas: parcial. Ninguna: no validada/fallida, diferenciando gates sin envío y fallos de invocación. No exige un veredicto concreto. |
| Cadena scanner | COMPLETED solo con cuatro TestExecutions y EvaluationResults COMPLETED, Findings coherentes, score AVAILABLE/scoring-v1, report AVAILABLE/report-v1/es y summary coincidente; cierre conjunto. Comprobar interpretación y nueve limitaciones vigentes, fechas/referencias y snapshot inmutable. Un fallo de lectura no equivale a ausencia. |
| Frontend 9.4 | Secuencia original íntegra observada y al menos un Finding persistido abierto desde Angular. Solo entonces proponer actualizar la tarea en una actuación posterior autorizada; esta planificación no toca frontend-mvp/tasks.md. Sin Finding o con otro paso ausente, conservar pendiente. |

No se exige que el LLM vote VULNERABLE ni que la consolidación produzca cuatro V. HYB-02 puede ser VULNERABLE con señal fuerte, aunque el LLM falle o discrepe, siempre que no opere una insuficiencia previa HYB-01. Sin señal fuerte y con LLM fallido puede ser INCONCLUSIVE/HYB-07; respuesta válida aislada no basta para Finding. Solo EvaluationResult COMPLETED/VULNERABLE genera Finding, confirmado con su evaluación. Score cero no acredita seguridad.

Errores del adaptador: 429 → EVALUATOR_RATE_LIMITED; 5xx → EVALUATOR_UNAVAILABLE; otros no-2xx (incluidos auth/4xx) → EVALUATOR_CONFIGURATION_INVALID; timeout → EVALUATOR_TIMEOUT; formato/status/referencias no aceptados → EVALUATOR_INVALID_OUTPUT. El repositorio suprime llamadas siguientes del mismo Scan tras RATE_LIMITED, CONFIGURATION_INVALID, BUDGET_EXCEEDED o SUPPRESSED. Timeout/5xx/formato inválido son individuales, sin retry, y permiten continuar con las evaluaciones posteriores dentro de presupuesto. Fallo global de criterios/persistencia/invariantes/deadline puede llevar Scan a FAILED y conserva lo ya confirmado. No reinterpretar una supresión como llamada gratuita exitosa.

### D-05 Evidencia pequeña y metadatos reales

En futura ejecución, conservar bajo evidencia de este change un resultado breve, configuración no secreta con procedencia, revisión de código, versiones/hashes del manifest/criterios, IDs/fechas, respuestas públicas scoped, matriz externa y hasta cuatro capturas útiles. No generar evidencia ficticia ahora. No logs completos, HAR, entorno completo, cabeceras Authorization, prompt/cuerpo bruto del proveedor, chain-of-thought ni credenciales.

La API EvaluationMetadata expone proveedor/modelo solicitado y estados, pero **no** usage, modelo resuelto, attemptCount o coste. La lectura interna de estos metadatos es diagnóstica, opcional y no bloquea el lanzamiento ni la aceptación funcional de la validación real del evaluator. Si existe autorización y disponibilidad, puede hacerse una consulta local READ ONLY, sin modificar datos, limitada a este Scan. Proyección limitada de `evaluation_results` filtrada por scan_id real: id, test_execution_id, requested_provider, requested_model, resolved_model, llm_status, llm_error_reason, llm_attempt_count, latency_ms, input_tokens, output_tokens, reserved_cost_usd, estimated_cost_usd, pricing_version, estimate_version y provider_request_id. Estos nombres se han contrastado con V5; requested_provider/requested_model corresponden al proveedor/modelo solicitado de la API. Verificar pertenencia del Scan al Project antes de leer. No SELECT *, configuration_snapshot, llm_assessment libre, evidencia, autenticación ni tablas ajenas.

Modelo resuelto y usage ausentes en una respuesta inválida pueden quedar null porque el adaptador devuelve fallo antes de conservarlos; no afirmar que OpenAI no consumió tokens. Si la lectura no está autorizada o disponible, registrar NOT_INSPECTED con su motivo, sin inventar valores ni interpretar ausencia de metadata como consumo cero. NOT_INSPECTED deja documentada la omisión del diagnóstico opcional, sin tarea obligatoria de recopilación pendiente ni bloqueo del lanzamiento o de la aceptación funcional basada en APIs/UI. Es diagnóstico del mismo Scan, sin nueva llamada externa. Mantener distinguidos intento durable y petición efectivamente llegada al proveedor.

### D-06 Comparación externa y criterio original 9.4

Ground truth: `demo-vulnerable/manifest/ground-truth-v1.json`; seleccionar solo profile VULNERABLE/scenarioVersion="1":

| Categoría | Caso | Code / revisión | Ground truth | Clasificación observada |
| --- | --- | --- | --- | --- |
| LLM01 | dv-v1-llm01-vulnerable | LLM01-INSTRUCTION-001 / 1 | VULNERABLE | Pendiente de ejecución |
| LLM02 | dv-v1-llm02-vulnerable | LLM02-CANARY-001 / 1 | VULNERABLE | Pendiente de ejecución |
| LLM08 | dv-v1-llm08-vulnerable | LLM08-CONTEXT-001 / 1 | VULNERABLE | Pendiente de ejecución |
| LLM10 | dv-v1-llm10-vulnerable | LLM10-MARKUP-001 / 1 | VULNERABLE, OUTPUT_ONLY | Pendiente de ejecución |

Añadir a cada fila Scan/evaluationId/findingId si existe, status, llmStatus/error, finalVerdict y decisión híbrida. Matriz general GT/resultado: V/V TP; N/N TN; N/V FP; V/N FN; I INCONCLUSIVE. Solo aplicar a evaluaciones COMPLETED; errores técnicos/ausencias aparte. En este recorrido todos los GT son V: solo TP/FN/INCONCLUSIVE son observables; no inventar TN/FP. No usar etiquetas, expectedVerdict, justificación del oracle o esta tabla en el evaluador. Sí mantener las referencias técnicas sintéticas aprobadas; no afirmar evaluación completamente ciega ni LLM-only. LLM10 verifica contrato de salida, no explotación downstream.

Criterio original literal de frontend-mvp 9.4:

> Recorrer la demo solo ante autorización de entorno/endpoints: abrir proyecto→mostrar target autorizado ya preparado→ejecutar→estado real→score/Findings/I→abrir Finding→report/limitaciones; registrar evidencias observadas, sin modificar escenario externo, implementar demo-vulnerable, llamar OpenAI directamente o simular detecciones. Verificar precondición de manifiesto ligado a target.updatedAt antes del recorrido (FE-24; D-30).

La llamada del backend por su puerto no es una llamada directa desde frontend/operador; esta validación tiene autorización externa separada. Abrir Finding significa desplegar su detalle real en la lista Angular existente, no crear un endpoint individual ni una ruta nueva. No cerrar 9.4 por solo obtener un Finding o por el E2E offline previo.

### D-07 Cierre mínimo

Tras terminal, deshabilitar exportación efectiva del scanner (`EVALUATOR_EXPORT_ENABLED=false`), quitar la allowlist temporal y volver a exportPolicy.enabled=false en la copia local. Retirar EVALUATOR_API_KEY del entorno de la sesión. Como Spring Environment/gates se configuran al arrancar y el hijo conserva su entorno heredado, cambiar otra consola no basta: detener la instancia propia usada para validar o reiniciarla sin clave y con exportación false, solo sin Scan activo. No persiste la clave en ningún archivo para ese reinicio. Confirmar cierre no secreto.

Si se agota la observación con Scan activo, registrar estado real y coordinar su terminalización o parada autorizada de esa instancia antes de dar por revocada su capacidad de exportación. No borrar datos ni detener PostgreSQL compartido ni otros procesos. Conservar Project/Target/Scan y solo evidencia no secreta. No exigir rotación de claves, revocación de cuenta ni un procedimiento operacional complejo.

### D-08 Corrección mínima del contrato local exportPolicy V1

**Causa comprobada:** `demo-vulnerable/manifest/schemas/evaluation-scenario-v1.schema.json` restringe `/properties/scenarios/items/properties/exportPolicy/properties/enabled` a `const:false`. Así mezcla la forma del documento con la política de ejecución offline. Las dos plantillas versionadas llevan false, pero `LocalEvaluationScenarioAdapter` ya representa enabled como booleano y combina los permisos existentes para calcular exportAuthorized. El schema local no es el schema de Structured Outputs ni el serializador del evaluador.

**Decisión V1:** sustituir únicamente ese `const:false` por `type:boolean`, con descripción de semántica: false obligatorio en el recorrido offline; true solo en copia local preparada para una operación real expresamente autorizada y sujeto a todos los gates. Conservar schemaVersion=1, ruta/nombre del schema, provider const OPENAI, required, additionalProperties=false y el resto de restricciones. No añadir modo, flag, proveedor ni propiedad; no introducir un default que permita omitir enabled. Ambos valores booleanos son estructuralmente válidos para ambos perfiles; perfil SAFE/VULNERABLE no equivale a modalidad offline/real.

**Compatibilidad y alternativa V2:** es una ampliación deliberada del conjunto aceptado, no un cambio de forma o significado runtime. Todo documento antes válido con false sigue siendo válido. El validador generalizado deja de demostrar por sí solo que una instancia es offline: ese requisito continúa siendo explícito en el preflight offline, junto con EVALUATOR_EXPORT_ENABLED=false. DV-10 define el manifest compatible con el scanner; D-25 y las tareas históricas fijan false para las plantillas/recorrido offline, no una prohibición universal de la futura operación autorizada. El README ya contempla otra ejecución posterior con permiso/proveedor. Por ello no se identifica una garantía deliberada de prohibición universal que justifique V2. V2 duplicaría contrato/rutas/validadores para el mismo booleano sin aportar autorización adicional. No cambiar históricos ni presentar la ampliación como conservación del rechazo de true.

**Autorización preservada:** schema válido y enabled=true nunca bastan para enviar. Continúan siendo necesarios binding exacto projectId/targetConfigurationId/targetUpdatedAt, proveedor coincidente OPENAI, EVALUATOR_EXPORT_ENABLED=true, escenario permitido por EVALUATOR_ALLOWED_SCENARIOS, contexto/evidencia aplicables y corpus exacto; para llamar realmente se necesita además API key únicamente en entorno, configuración válida y cuotas/presupuesto existentes. Mantener separación oracle/evaluator, SSRF y límites. No añadir validación de modo al schema mediante campos nuevos ni atribuirle acceso al entorno del proceso.

**Revisión de artefactos y alcance futuro:**

- `evaluation-safe.template.json` y `evaluation-vulnerable.template.json`: conservar exactamente enabled=false/provider OPENAI, placeholders, escenario/revisión/contexto/corpus. Solo la copia local de operación real pasa explícitamente a true. No nuevo manifest versionado habilitado.
- `benchmark-v1.json` y su schema: no tocar externalTrafficAuthorized=false, outboundNetwork=false, realLlm=false; describen la demo sin proveedor externo, no el permiso del adaptador del scanner. Ground truth y bindings-v1 no cambian.
- `ManifestTest.evaluatorTemplatesKeepOnlyTechnicalContextAndOneExactCorpusPerProfile`: conservar prueba de plantillas false y binding obligatorio. Añadir casos SAFE/VULNERABLE con binding válido y enabled=false/true aceptados, valores no booleanos y campo ausente rechazados, provider incorrecto/ausente rechazado, campos extra rechazados. Mantener regresiones existentes de IDs, fecha, cuatro entries y separación del oracle. Un UUID válido pero de otro Target puede pasar validación estructural: la igualdad real se verifica en binding/preflight/runtime, no se atribuye al JSON Schema.
- `JsonSchemaCheck`: ya soporta type boolean; no necesita ampliación ni dependencias. Verificar positivos/negativos contra el schema real, sin sustituirlo por una copia en los tests.
- Regresión offline sin red: ambas plantillas continúan false; schema acepta true estructuralmente, pero el preflight offline lo rechaza. Las comprobaciones/documentación offline deben conservar explícitamente ambas condiciones false, sin usar solo schema PASS como autorización. No reejecutar un Scan offline para probar esta corrección.
- `EvaluationScenarioTest`: conservar cobertura existente de binding, corpus y exportAuthorized; completar únicamente casos no cubiertos para cada gate global/allowlist/manifest desactivado y combinación permitida, con fixtures locales, sin transporte real. Probar que true aislado no autoriza, que false bloquea aunque los otros permisos estén presentes y que un binding discordante no selecciona contexto. Sin modificar producción backend ni sus límites; los tests del transporte existentes cubren clave/configuración, sin necesitar clave real.
- `demo-vulnerable/README.md`, preparación de manifest, configuración de scanner y recorrido posterior: explicar schema booleano, plantillas offline false, copia real true bajo autorización completa y que validar JSON no otorga permisos. El runbook E2E offline ya exige false: conservarlo sin reescritura. `docs/04-ia/estrategia-evaluacion.md` ya expresa los gates conjuntos; no requiere un cambio semántico. Solo actualizar referencias directamente afectadas si se encuentran afirmaciones universales de const:false, sin duplicar documentación ni modificar ADR-004.

La implementación posterior será schema + tests mínimos + README; no cambios de API, runtime demo/scanner/frontend, probes o dependencias. Para los tests de gates se reutiliza la cobertura existente y solo se añaden ausencias justificadas. Su ejecución requiere solicitud explícita; este trabajo solo ejecuta strict de OpenSpec. Tras implementar, validar copias sintéticas con ambos booleanos y una copia local real con binding vigente antes de reanudar, sin que estas comprobaciones llamen al proveedor.

**Estado:** READY FOR IMPLEMENTATION de esta corrección mínima; no acredita corrección aplicada ni desbloqueo operacional. Los registros del fallo const:false se preservan íntegros en tasks.md. El único Scan real continúa pendiente hasta implementar/comprobar la corrección y satisfacer el preflight existente.

### D-09 Reparación HTTPS autorizada y diagnóstico offline

La solicitud posterior autoriza exclusivamente corregir el registro TLS nulo, añadir regresión offline y observabilidad mínima. Sustituir el constructor de compatibilidad obsoleto por `BasicHttpClientConnectionManager.create(DefaultSchemePortResolver.INSTANCE, resolver, registry, null)`, con registro de TlsSocketStrategy para https y `DefaultClientTlsStrategy.createDefault()`. No usar un builder inexistente para Basic en 5.6.4, ni cambiar a un pool o al resolver de sistema del manager. El parámetro final null corresponde a la factory de conexiones por defecto, no al registry. La firma se comprobó en el sources.jar local de la dependencia existente, sin Internet ni cambio de versión.

Extraer únicamente un factory package-private `connectionManager(resolver, remainingMs)` para compartir con la regresión la construcción real y los mismos ConnectionConfig/TlsConfig. La ruta de producción conserva BoundedDnsResolver, 2 s DNS, 1..16 IPs, selección de la primera dirección, tiempos de connect/TLS/socket y cancelación por deadline existentes. DefaultClientTlsStrategy conserva trust store/hostname estándar; no trust-all, certificados añadidos, endpoint alternativo ni nuevas variables. Endpoint/modelo/store=false/clave/permisos/cuotas/retries no cambian.

La regresión usa un peer efímero ligado solo a 127.0.0.1 y DNS sintético tls-fixture.invalid→loopback, sin usar el adaptador evaluate ni su endpoint externo. Construye el mismo manager real y un cliente real; el peer identifica la cabecera TLS ClientHello y responde bytes no TLS. El cliente debe rechazarlos con SSLException, no NPE. No se simula el manager, no se acepta certificado inválido ni se envía API key/HTTP de aplicación. La prueba falla con NPE con el constructor anterior y pasa con el registro válido; acredita esa etapa TLS, no un handshake completo, confianza/hostname de OpenAI ni operación del proveedor. No se cambia el endpoint configurable de producción para probarlo.

Observabilidad local de WARN con tres campos: evaluatorFailure, reason y exceptionType. Categorías cerradas DNS, TLS_CONNECTION, TIMEOUT, HTTP_PROVIDER, PARSE_SCHEMA, CONFIGURATION e INTERNAL. Se conserva la etapa DNS para errores del BoundedDnsResolver (que sanea la excepción subyacente); el tipo de excepción permite identificar defectos internos sin su mensaje. No registrar throwable, mensaje, causa, stack trace, URL, direcciones, cabeceras, cuerpos, evidencia o clave. No añadir LlmError, columnas ni campos REST. La prioridad del presupuesto agotado y los mappings/supresión previos permanecen intactos. Diagnóstico de tipo y etapa no equivale a conocer la causa original completa, que se descarta deliberadamente por privacidad.

La regresión de logs captura eventos y argumentos para comprobar categorías y ausencia de marcadores privados, clave sintética y ThrowableProxy. Se reutilizan transportes fixtures exclusivamente para estas pruebas de mappings/contrato; no sustituyen la regresión del manager. El incidente histórico sigue sin causa retrospectiva demostrada, y la validación real permanece pendiente; este cambio no autoriza Scan ni llamada OpenAI. Resultado y comandos de tests en tasks.md, sin reescribir evidencia histórica.

## Risks / Trade-offs

- [Modelo configurado sin acceso real confirmado] → confirmación del operador antes del único Scan; no sustituir modelo ni probar con llamadas adicionales en silencio.
- [Coste no garantizado por tokens/reservas] → aprobar política/importe y tarifa o aceptar explícitamente ausencia de gate monetario; registrar desconocidos como tales.
- [Finding incluso con proveedor fallido] → separar los tres ejes de aceptación y conservar degraded/llmErrorReason.
- [Configuración de consola distinta del proceso] → declaración verificable del operador y metadata pública del Scan; diagnóstico interno opcional, sin inferir runtime desde defaults.
- [Usage no expuesto por API] → diagnóstico opcional READ ONLY autorizado o NOT_INSPECTED explícito, sin bloquear lanzamiento/aceptación funcional; no añadir instrumentación.
- [Un Scan VULNERABLE no mide precisión poblacional] → comparación operativa de cuatro casos, sin métricas científicas ni segundo perfil.
- [Documento e2e-integration exige exportación false] → conservar su contrato offline; esta operación es otro Scan y change, sin reescribir historia.

## Migration Plan

No hay migración de BD ni despliegue. D-08 planifica una ampliación del schema JSON auxiliar V1, sin migrar instancias false ni cambiar runtime. Implementar/comprobar D-08 → preparación → autorización operativa → configuración temporal → único Scan → inspección → cierre D-07. Esta petición solo diseña la corrección: no implementa ni reanuda el recorrido. Si se descubre otro defecto, registrarlo y detener la modificación afectada; cualquier corrección adicional requiere petición específica, sin ajustar expectativas para ocultarlo.

## Open Questions

Pendientes operativos antes de ejecutar, sin decisiones arquitectónicas nuevas: aprobación del change y sesión externa; confirmación de acceso de la cuenta al modelo configurado; tarifa/presupuesto o aceptación expresa de operar sin gate monetario; IDs/fecha reales y ruta local del escenario. No bloquean presentar la planificación para revisión, sí bloquean el lanzamiento mientras no estén satisfechos. No se resuelven mediante Internet ni lectura de claves en esta tarea.

La autorización/disponibilidad de lectura interna de metadata no es una precondición pendiente: solo determina si se realiza el diagnóstico opcional D-05 o se registra NOT_INSPECTED. Ninguna de las dos opciones bloquea el lanzamiento ni la aceptación funcional.

## Fuera del MVP / Evolución futura

AWS, modelos alternativos, benchmark estadístico, precision/recall, tuning, cambios de prompts, refactor, SAFE adicional, nuevas categorías y cambios visuales quedan fuera del change. No se convierten en tareas de este recorrido ni condiciones de cierre.
