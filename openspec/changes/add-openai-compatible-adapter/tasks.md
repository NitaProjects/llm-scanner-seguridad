Estado: **READY FOR FINAL REVIEW** (2026-11-12).
**19/19 tareas completas**. Diseño/F1/F2 aprobados y aceptación externa con Groq
completada según el resultado manual aportado por el operador.
Evidencia: [resultado](evidence/result.md), [suite backend](evidence/backend-tests.json),
[tests dirigidos](evidence/focused-tests.json), [PostgreSQL](evidence/postgres-tests.json)
y [frontend](evidence/frontend-tests.json). Los skipped no cuentan como PASS.

Aceptación externa: **5.1 PASS · 5.2 PASS · 5.3 PASS**. El resultado final sustituye
al [preflight inicial bloqueado](evidence/real-provider-validation.md) como estado
vigente. Ver [evidencia estructurada del Scan](evidence/real-provider-validation.json).

## 1. Configuración y contrato vertical

- [x] 1.1 Tras aprobación, añadir adapterType/model/systemPrompt explícitos a TargetConfiguration/TargetSettings y snapshot con validación por rama (OAT-01/03, F1); verificar legacy con plantilla/ruta obligatorias y model/system NULL, chat con model obligatorio y plantilla/ruta NULL, sin cambios en TargetAdapter/NormalizedInteraction/proyección/use case.
- [x] 1.2 Implementar generador/codec HTTP solo en memoria desde campos chat explícitos, con stream=false y prompt efectivo como user (OAT-01/03, F1); verificar system omitido/null/presente, Unicode/escapes, cotas y rechazo de extras/model/system inválidos; comprobar que no escribe ni lee configuración chat desde request_template/response_path.
- [x] 1.3 En implementación posteriormente autorizada, crear una única migración incremental con adapter_type/model/system_prompt, retirar NOT NULL físico de request_template/response_path e incorporar checks condicionales OAC-03; adaptar mapper JPA sin editar SQL histórico (OAT-01, F1). Verificar legacy intacto/model-system NULL, chat round-trip leyendo columnas tras reinicio y rechazo DB de ramas inválidas/NULLs que producirían UNKNOWN; no ejecutar migraciones por defecto.
- [x] 1.4 Extender POST/GET/PUT y DTOs como unión discriminada, leyendo model/system directamente y conservando respuesta legacy exacta (OAT-01/02, F1/F2); verificar contratos/errores por tipo, campos omitidos/rechazados, identidad/fechas y conservación/reemplazo de credencial sin descifrar en consultas.
- [x] 1.5 Sustituir replace incondicional por actualización condicionada en repositorio con expectedAdapterType (REST_LEGACY obligatorio en PUT sin tipo); escribir todos los campos/NULLs/credencial/updatedAt juntos y tratar cero filas como 409 seguro según OAC-04a (OAT-02, F2). Verificar ausencia de dirty checking/merge/flush que eluda el predicado, estado JPA refrescado/descartado y cero retry sobre tipo nuevo, sin @Version/revisions.
- [x] 1.6 Diseñar/añadir test determinista de dos transacciones con barreras: T1 PUT antiguo lee legacy, T2 convierte y confirma chat, T1 UPDATE afecta cero filas/409 y no altera ningún campo/credencial/fecha de T2; cubrir también el orden inverso válido (OAT-02, F2). Ejecutarlo solo cuando se solicite comprobación con PostgreSQL dedicado, sin sleeps, red externa ni SQL ejecutado en esta revisión de diseño.

## 2. Adapter, extracción y selección

- [x] 2.1 Adaptar el punto interno común para requestRenderer en memoria y extractor, manteniendo invoke con renderer/extractor legacy como entrada equivalente (OAT-04/05, F1); verificar construcción chat sin plantilla/ruta persistidas, mismas políticas HTTP/DNS/TLS/límites/precedencia y cierre al fallar callback, sin duplicar clientes/controles ni exponer secretos a callbacks.
- [x] 2.2 Implementar OpenAiCompatibleTargetAdapter con codec, POST único y extractor estricto, devolviendo NormalizedInteraction D0 por proyección existente (OAT-03/04); verificar request exacta en servidor local: endpoint/model/messages/Bearer/stream=false y siete valores saneados, sin provider metadata.
- [x] 2.3 Cubrir la matriz OAC-07: 401/403/404/429/5xx, JSON/body inválido/vacío, choices/message/content ausentes/null/tipos, texto vacío, finish ausente/length/tools, timeouts y límites (OAT-04); verificar ExecutionError/httpStatus/alcance y ausencia de retry, éxito truncado o body remoto en errores.
- [x] 2.4 Añadir RoutingTargetAdapter con dos delegados explícitos y wiring Spring sin registry/reflection/fallback (OAT-03/05); verificar los dos caminos reales, conservación de fakes @Primary legacy y mismo snapshot/prompt/budget, sin ambigüedad de beans ni cierre doble.

## 3. Formulario usable y compatibilidad integrada

- [x] 3.1 Extender modelos/decoder/API de target Angular con unión legacy/chat y formulario progresivo (OAT-02); verificar que chat muestra solo endpoint/model/Bearer/system/timeout, lee GET seguro y nunca envía campos ocultos/JSON manual ni solicita modelos por red.
- [x] 3.2 Preservar conservar/reemplazar token y limpieza transitoria; confirmar cambio de tipo y mostrar alcance sintético v1 (OAT-02/05/06); verificar cancelar/error/cambio de tipo/origen/proyecto/navegación, sin storage/log de secreto ni llamada remota por guardar.
- [x] 3.3 Añadir fixture del pipeline chat con cuatro prompts independientes y evaluador fake/suprimido, reutilizando contexto/corpus de pruebas cuando proceda (OAT-06/07); verificar cuatro ejecuciones/evaluaciones, inconclusos honestos, Findings de fixtures aplicables y score/report-v1/cierre atómico intactos, sin proveedor real.
- [x] 3.4 Con petición explícita de comprobación, ejecutar tests nuevos y regresiones existentes de configuración/D0/HTTP/evaluación/scoring/reporting y formulario (OAT-07); verificar resultados reales e informar suites omitidas como pendientes, sin Internet ni instalación de dependencias. Integraciones BD y servidores de fixture solo en entorno dedicado autorizado.

## 4. Contratos y revisión del alcance

- [x] 4.1 Actualizar OpenAPI/target-contract, arquitectura-general/modelo-datos y referencias de decisión afectadas por la implementación entregada (OAT-01..06); verificar oneOf/errores/defaults, persistencia explícita/constraints de la única migración y escritura condicional por tipo, sin reescribir históricos ni afirmar completado el roadmap.
- [x] 4.2 Revisar el diff y evidencia aislada (OAT-01..07); verificar ausencia de nuevas dependencias, perfiles/estados/evidencia futura, SSE/workflows, cambios de algoritmo/recursos scoring-reporting y cualquier operación AWS/deployment.

## 5. Validación externa separada y autorizada

- [x] 5.1 Obtener autorización específica del target/cuenta y pruebas, confirmar modelo disponible de Groq (OpenRouter alternativo), origen y límites de coste/cuota; verificar acuerdo operativo registrado sin secreto (OAT-07). No ejecutar llamadas mientras falte permiso ni leer .env/API key.
- [x] 5.2 Con autorización y clave provisionada localmente por el usuario, configurar desde la UI y ejecutar un Scan de cuatro pruebas sin evaluator externo habilitado por conveniencia (OAT-06/07); verificar cuatro invocaciones y, si son válidas, cuatro evaluaciones/score/report-v1/COMPLETED con motor común. Si falla, registrar pendiente/fallo sin reenvíos ni aceptación ficticia.
- [x] 5.3 Registrar solo metadata/resultados seguros de la validación y limitaciones, manteniendo key/headers/cuerpos fuera de evidencias de validación (OAT-05..07); verificar que inconclusos no se presentan como detección o seguridad ni se publican métricas precision/recall.

## Evidencia de implementación — 2026-11-10

- F1: una sola migración V8, tres columnas explícitas y checks por rama con IS NULL /
  IS NOT NULL. Datos V7 legacy preservados; model/system chat releídos tras reiniciar
  contexto. Sin SQL histórico modificado, nuevas tablas o configuración JSON genérica.
- F2: reemplazo como única sentencia condicional id/project_id/adapter_type esperado;
  los campos, NULLs, credencial y fecha se escriben juntos. Sin merge/dirty checking de
  actualización, retry o @Version. Cero filas → 409 seguro, con distinción cliente viejo /
  PUT explícito. Estado del contexto descartado tras el bulk update.
- Transporte común con callbacks de construcción/extracción: sin segundo cliente, nuevas
  autorizaciones o cambio de ownership. Requests chat exactas model/messages/stream=false,
  Bearer protegido y parser estricto; matriz ExecutionError existente. Routing exhaustivo
  por enum; TargetAdapter/NormalizedInteraction/proyección/worker D0 intactos.
- API legacy conserva sus 16 campos; chat usa endpoint/model/systemPrompt y omite
  plantilla/ruta/headers. Frontend con selector, campos progresivos, confirmación de
  conversión, conservación/reemplazo y limpieza de token por tipo/origen/error/navegación.
  Sin endpoint /models ni HTTP al guardar; se mantiene el aviso de alcance sintético v1.
- Suite backend completa con PostgreSQL de test, confirmada por el operador y
  contrastada con Surefire: **641 tests / 629 passed / 0 failed / 0 errors /
  12 skipped**, BUILD SUCCESS. ProjectPostgresIntegrationTest: 7 skipped;
  TargetPostgresIntegrationTest: 5 skipped. No cuentan como PASS.
- Regresión histórica corregida exclusivamente en fixtures: historia V6 preparada
  con cálculo real JDBC, migraciones V7/V8 antes de JPA y expectativa final V8.
  ReportingMigrationIntegrationTest: **3 passed / 0 failed / 0 errors / 0 skipped**.
  Se comprueban datos legacy intactos, nuevas columnas y constraints por rama;
  sin backfill ficticio, cambios de SQL histórico ni relajación de validación JPA.
- PostgreSQL ejecutado por el operador en su terminal preparada, BD dedicada
  localhost:5432/llm_scanner_seguridad_test: **3 passed / 0 failed / 0 skipped**.
  Contrastado con XML Surefire local y comunicado por el operador: migración, carrera
  determinista 409 con preservación íntegra de T2 y orden inverso válido, y pipeline chat
  con cuatro ejecuciones/evaluaciones, caso inconcluso sin evaluator y caso sintético con
  Findings/score55/report-v1 mediante evaluator fake. No se conservan XML/logs completos.
- Frontend: **166 passed / 0 failed / 0 skipped**, 11 archivos. Build production **PASS**,
  bundle inicial 286.67 kB. Sin dependencias, versiones o lockfiles modificados.
- Validación externa Groq: **PASS**. Scan `36033cf4-941b-4345-a24f-421d5ae71480`
  COMPLETED con cuatro ejecuciones y cuatro evaluaciones, cuatro INCONCLUSIVE,
  cero Findings y score 0/100. El origen requirió allowlist explícita y la
  persistencia del Bearer requirió la clave AES local. La API key queda fuera
  de la evidencia. El evaluador LLM SKIPPED/degradado donde se observó no afecta
  a la aceptación del transporte.
- OpenAPI/contrato target, modelo de datos y cadena arquitectónica actualizados. No se
  reescriben ADRs históricos ni se cambia lógica de evaluación/scoring/reporting, demo,
  producción o AWS. El favicon ya modificado antes de esta tarea se conserva intacto.
- `openspec validate add-openai-compatible-adapter --strict`: **PASS**. Sin commit/push,
  despliegue ni archivo del change. Los 12 skipped se mantienen explícitamente como
  omitidos; no se suman a los 629 tests pasados.
