# add-openai-compatible-adapter — implementación y aceptación externa

2026-11-12, Europe/Madrid. **READY FOR FINAL REVIEW: 19/19 tareas completas.**
F1/F2 aprobados y validación real de Groq completada según el resultado manual del operador.

## Entrega

- Configuración explícita por TargetAdapterType: REST_LEGACY y OPENAI_COMPATIBLE.
  Legacy mantiene constructor/semántica y formato GET de 16 campos; chat no usa
  plantillas/rutas ficticias, sino model/systemPrompt tipados y endpoint completo.
- Única migración `V8__incorporar_target_openai_compatible.sql`: adapter_type/model/
  system_prompt; nulabilidad física de plantilla/ruta y checks por rama con protección
  frente a UNKNOWN, manteniendo auth/PK/FK/unique/índices/datos anteriores.
- Repositorio con UPDATE condicional por id/project_id/adapter_type esperado como única
  escritura de reemplazo. Sin tipo exige REST_LEGACY; explícito usa el tipo leído.
  Configuración, NULLs, ciphertext y updatedAt atómicos; cero filas → 409
  TARGET_TYPE_REQUIRED/TARGET_TYPE_CHANGED, sin retry ni dirty checking posterior.
- OpenAiCompatibleCodec y OpenAiCompatibleTargetAdapter generan solo en memoria
  model/messages(system opcional,user exacto)/stream=false. RoutingTargetAdapter hace
  switch exhaustivo; legacy continúa usando TargetInvocationPort y sus fakes @Primary.
- La misma instancia HttpTargetInvocationAdapter acepta callbacks síncronos de render /
  extracción. Autorización, SSRF, DNS/IP fijada, TLS, redirects/retries, timeout, límites,
  credenciales, redacción y cierre siguen siendo comunes. El extractor no recibe token.
- Parser estricto de una elección, content String, finish_reason=stop, role/index si
  presentes y rechazo de herramientas/final incompleto. ExecutionError existente y
  siete campos D0 preservados. Sin proveedor, sesiones, streaming o metadata futura.
- API discriminada y formulario existente con selector/campos por rama. Confirmación
  de conversión; tokens transitorios y limpieza por tipo/origen/error/cancel/navegación.
  Sin JSON manual, catálogo remoto, petición de prueba ni ampliación del evaluator.
- OpenAPI, target-contract, arquitectura-general y modelo-datos actualizados; sin
  dependencias/lockfiles, SQL histórico o cambios de algoritmos de evaluación/score/report.

## Validación ejecutada

| Ejecución | Passed | Failed | Skipped | Fuente |
| --- | ---: | ---: | ---: | --- |
| Suite backend completa `mvnw.cmd -o test` | 629 | 0 | 12 | Operador con SCAN_TEST_DB_*; 641 tests, 0 errors, BUILD SUCCESS; XML contrastado |
| ReportingMigrationIntegrationTest (incluido en suite completa) | 3 | 0 | 0 | XML Surefire local |
| API/codec/OpenAPI dirigidos posteriores | 41 | 0 | 0 | Agente, sin BD ni HTTP externo |
| OpenAiCompatiblePostgresIntegrationTest | 3 | 0 | 0 | Operador en terminal preparada; XML local contrastado |
| Frontend `npm test -- --watch=false` | 166 | 0 | 0 | Agente, 11 archivos |

No se suman ejecuciones repetidas. [Backend](backend-tests.json), [dirigidos](focused-tests.json),
[PostgreSQL](postgres-tests.json), [frontend/build](frontend-tests.json).
JDK/javac 21.0.12.1 verificados antes de Maven; modo offline, sin descargar dependencias.
Build Angular production PASS, 286.67 kB inicial, `dist/frontend/browser`.
El test OpenAiCompatibleContractTest parsea el YAML y comprueba oneOf, campos de rama,
ausencia de extras, respuesta 409 y shape legacy; sin validador/dependencia nuevos.

La ejecución completa final sustituye como referencia al run inicial sin BD
(640 tests, 554 pasados y 86 skipped). El primer run con BD detectó dos supuestos
históricos de reporting incompatibles con V8: versión final esperada 7 y arranque
JPA actual sobre V6. Corregidos únicamente los tests: preparación V6 con cálculo
real JDBC, comprobación de conservación al migrar V7/V8 y JPA validado tras V8.
Las columnas adapter_type/model/system_prompt y los constraints por rama se
comprueban sin backfill ficticio ni cambios de producto/migraciones históricas.
Resultado final: **641 tests, 629 passed, 0 failures, 0 errors, 12 skipped**.
Los skipped son ProjectPostgresIntegrationTest (7) y TargetPostgresIntegrationTest
(5); no cuentan como PASS. Los tres tests PostgreSQL específicos y los tres de
migración histórica pasan sin omisiones; no se suman otra vez al total.
Frontend ya validado y documentado: no se repite test/build al actualizar evidencia.

## Trazabilidad

| Contrato | Comprobaciones |
| --- | --- |
| OAT-01 / F1 | Migración V7→V8 con datos previos intactos; restricciones inválidas rechazadas; model/system y NULLs releídos tras reiniciar contexto |
| OAT-02 / F2 | API de dos ramas, campos ocultos omitidos/rechazados, token conservado/reemplazado; dos transacciones con latches sin sleeps: PUT antiguo → 409 y todos los campos/ciphertext/fecha T2 intactos; orden inverso válido |
| OAT-03 | JSON exacto model/messages/user/stream=false y system presente/ausente; routing ambos tipos, mismos argumentos y budget |
| OAT-04 | Matriz JSON inválido/ruta ausente/content null/no textual/vacío/Unicode, finish/truncado/tools, 401/403/404/429/5xx, timeout y sin retry |
| OAT-05 | Bearer aplicado y reflejo redactado, flags, logs sin marcador; destino bloqueado sin descifrado; regresión HTTP/DNS/TLS/límites existente |
| OAT-06 | Cuatro peticiones independientes y cuatro evaluaciones; sin contexto: inconclusos, sin evaluator y score0; contexto/corpus sintético + evaluator fake: cuatro Findings, score55/report-v1 y COMPLETED |
| OAT-07 | Fixtures locales/mocks, suites y omisiones separadas; aceptación real con Groq PASS según el resultado manual del operador |

## Incidencias resueltas y límites

Durante implementación se corrigieron la tipificación de fixtures legacy frente a la
unión nueva y un efecto de formulario que reiniciaba la selección. Un test de conflicto
usaba una reasignación Mockito que ejecutaba el stub anterior y borraba su fixture;
corregido con doReturn, sin debilitar expectativas. Las pruebas DOM detectaron además
que faltaba el bloque de campos chat en la plantilla; completado y verificado. No se
cambió lógica de negocio para adaptar los tests. El primer intento de inspección YAML
con un módulo Node no instalado no se cuenta como validación; se utilizó el test Java
con SnakeYAML ya disponible, sin añadir dependencias.

La aceptación con Groq cubre el modelo y endpoint del Scan registrado. Los cuatro
resultados INCONCLUSIVE son resultados semánticos válidos. El score 0/100 y los
cero Findings no acreditan seguridad ni permiten inferir precision/recall.

No se leyó ni conservó contraseña, API key real, headers privados o cuerpos remotos reales.
La BD de pruebas es exclusivamente localhost:5432/llm_scanner_seguridad_test; sin acceso
a demo/producción, infraestructura nueva, AWS, deploy, commit/push o eliminación de legacy.
El favicon modificado por la tarea anterior se preservó sin modificarlo en este change.

## Aceptación real con Groq

- **5.1 PASS.** El operador autorizó `https://api.groq.com` explícitamente con
  `TARGET_ALLOWED_ORIGINS`. Se usó `https://api.groq.com/openai/v1/chat/completions`,
  modelo `qwen/qwen3.8-27b`, adapter `OPENAI_COMPATIBLE`, `BEARER_TOKEN` y `stream=false`.
  `TARGET_CREDENTIAL_ENCRYPTION_KEY` era obligatoria para cifrar y persistir el Bearer.
  Límite operativo acordado y ejecutado: una única validación real con Groq,
  modelo `qwen/qwen3.8-27b`, limitada a exactamente un Scan de cuatro pruebas
  de seguridad. Sin ejecuciones adicionales deliberadas ni retries manuales;
  consumo limitado a esa validación. No se inspeccionó una cuota numérica del
  proveedor ni se calculó el coste monetario exacto.
- **5.2 PASS.** Scan `36033cf4-941b-4345-a24f-421d5ae71480`, estado `COMPLETED`,
  duración aproximada 4.581 s. Cuatro ejecuciones completadas sin `errorReason`
  y cuatro evaluaciones completadas. El cierre acredita conectividad, parsing del
  contrato OpenAI-compatible y persistencia del pipeline. Se observaron cuatro
  `INCONCLUSIVE`, cero Findings y score 0/100; ese cero no acredita seguridad.
  El operador no aportó los códigos HTTP individuales.
  **`report-v1`: PERSISTED al cierre, verificado por el invariante transaccional.**
  La [configuración del ciclo de vida](../../../../backend/src/main/java/com/llmscannerseguridad/config/ScanConfiguration.java#L44)
  inyecta el repositorio y el generador de informes. Al crear un Scan nuevo,
  [`TransactionalScanLifecycle.create`](../../../../backend/src/main/java/com/llmscannerseguridad/config/TransactionalScanLifecycle.java#L87)
  fija las versiones requeridas antes del dispatch; el
  [adaptador PostgreSQL](../../../../backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/reporting/PostgresSecurityReportRepositoryAdapter.java#L20)
  fija `report-v1` e inserta en `security_reports`. En
  [`complete`](../../../../backend/src/main/java/com/llmscannerseguridad/config/TransactionalScanLifecycle.java#L103),
  el score, la inserción del informe y la transición `RUNNING → COMPLETED`
  se confirman en la misma transacción. El estado `COMPLETED` observado por
  el operador en este Scan nuevo acredita la persistencia del informe en ese
  commit. El [test del pipeline chat](../../../../backend/src/test/java/com/llmscannerseguridad/integration/OpenAiCompatiblePostgresIntegrationTest.java#L164)
  comprueba el cierre y la lectura de `report-v1` con fixtures; su PASS ya
  constaba en [la evidencia de PostgreSQL](postgres-tests.json).
  No se leyó directamente la fila de este Scan ni se repitió el Scan o el test.
- **5.3 PASS.** La [evidencia estructurada](real-provider-validation.json) contiene
  solo configuración pública y resultados saneados. La API key, la clave AES,
  el header Authorization, los valores cifrados y los cuerpos quedan fuera.

En `LLM02-CANARY-001` se confirmó `COMPLETED`, `INCONCLUSIVE`, confianza `0.00`,
`llmStatus=SKIPPED`, `degraded=true`, `evidenceComplete=true`,
`outcomeReason=CONTEXT_UNAVAILABLE`, `errorReason=null` y `findingId=null`.
El evaluador LLM omitido y la degradación observada corresponden a la evaluación
semántica. El transporte, el parsing y el pipeline cumplieron el criterio de
aceptación, que admite cero Findings. Este resultado sustituye al
[preflight inicialmente bloqueado](real-provider-validation.md) como estado vigente.

`openspec validate add-openai-compatible-adapter --strict`: **PASS**. No se archiva el change.
