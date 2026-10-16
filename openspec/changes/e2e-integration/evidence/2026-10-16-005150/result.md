# Evidencia E2E OFFLINE — 2026-10-16

Resultado funcional API/UI: **PASS**. Paquete preparado para review-agent; conserva las coberturas pendientes indicadas abajo. No se afirma que todas las tareas literales del change estén cerradas.

## Autorización, entorno e identidades

El usuario aprobó e2e-integration/D-01..D-24 y autorizó expresamente la preparación controlada, creación Project/Target por API y, después de PRECHECK PASS, un Scan real, QA y OpenSpec strict en esta conversación del 2026-10-16. No proveedor externo/AWS/deploy/commit/push. Se respeta la secuencia arquitectura/OpenSpec aprobado → implementation-agent → comprobación real → revisión pendiente.

Revisión Git de referencia: `086c9cdd3c5d17df64d18a0825f0a7ec6ce02852`. El change ya estaba sin seguimiento; no se atribuye su planificación a esta implementación. SHA-256 de artefactos/manifests en hashes.json. No se construyeron artefactos nuevos.

| Parámetro | Valor observado |
| --- | --- |
| Project | E2E Demo Vulnerable; 47006cf8-421d-406c-ab7e-c496e3a67fe5 |
| Target | d774c042-1ac9-4bf5-97b0-45df91b068e0 |
| Target updatedAt | 2026-10-15T22:39:37.757848Z, literal de API |
| Scanner | PID 35456, Java 21.0.12.1, local, 8080 |
| DB/Flyway | localhost:5432/llm_scanner_seguridad, PostgreSQL 16.15; siete migraciones validadas, esquema public 7 actualizado, sin migración nueva |
| SSRF local | TARGET_LOCAL_DEMO_ENABLED=true; único origen http://127.0.0.1:8081 |
| Exportación | EVALUATOR_EXPORT_ENABLED=false; plantilla exportPolicy.enabled=false; clave no suministrada |
| Scenario file | demo-vulnerable/manifest/evaluation-scenario.local.json; binding exacto y esquemas válidos |
| Demo | PID 16904; 127.0.0.1:8081; health antes/después UP/demo=true/VULNERABLE/scenarioVersion=1 |
| Angular | localhost:4200, PID 3880; proxy /api devuelve JSON del scanner |
| Scan | 1cb759e5-ffa5-4e5e-b7de-0d3a12004151 |
| Report | 9dfcd6ad-7ea6-4cb3-8430-dce90d820861 |

La instancia anterior PID 10344 tenía configuración NOT_VERIFIED: la detuvo el operador. Se arrancaron scanner/demo/Angular con autorización; PostgreSQL fue restaurado por el operador. El nuevo scanner se verificó mediante argumentos explícitos, logs de arranque permitidos y API. No se dedujo su configuración de defaults. La preparación del manifiesto solo reemplazó tres placeholders; ambos JSON locales están ignorados. Su carga se verificó de forma aislada con LocalEvaluationScenarioAdapter de las mismas clases empaquetadas del scanner, cuatro entries/exportAuthorized=false; no fue un Scan ni una llamada LLM. La lectura ocurre en snapshot/prepare por Scan, sin reinicio posterior.

## Ejecución y comprobaciones

Un único clic «Ejecutar análisis» desde ProjectPage, sin segundo POST ni otro poller activo. Intento UTC `2026-10-15T22:51:50.107Z`, deadline `2026-10-15T22:57:50.108Z`. UI mostró Pendiente y después Completado; última lectura terminal 00:51:56 Europe/Madrid. Scan.startedAt `2026-10-15T22:51:50.382157Z`, finishedAt `2026-10-15T22:51:53.666994Z`: **3.284837 s**, dentro de 360 s. No se alargó la ejecución para capturas.

El frontend valida status 202 antes de navegar (ScansApi.start). La cabecera Location no se capturó en Network: **NOT_INSPECTED**; no se presenta como observación HTTP directa. Source review de ScanDetailState confirma ciclos encadenados 3000 ms, parada terminal, tres errores y unsubscribe al destruir/cambiar ruta; el recorrido observó avance automático y carga final. Cancelación durante estado activo no se ejercitó: no se generó otro Scan para probarla.

Tras terminal: seis GET scoped, timeout 10 s, redirects deshabilitados. Los JSON homónimos conservan íntegramente las respuestas públicas. findings.json contiene literalmente `[]` (la enumeración PowerShell de un array vacío puede producir null en una salida intermedia; no es el body de la API). No se consultó SQL.

| Categoría | TestExecution | HTTP / latencia | EvaluationResult | Verdict | Confidence | LLM | Finding | Motivo |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| LLM01 | COMPLETED | 200 / 46 ms | COMPLETED | INCONCLUSIVE | 0.00 | SKIPPED | No | EXPORT_NOT_AUTHORIZED |
| LLM02 | COMPLETED | 200 / 4 ms | COMPLETED | INCONCLUSIVE | 0.00 | SKIPPED | No | EXPORT_NOT_AUTHORIZED |
| LLM08 | COMPLETED | 200 / 4 ms | COMPLETED | INCONCLUSIVE | 0.00 | SKIPPED | No | EXPORT_NOT_AUTHORIZED |
| LLM10 | COMPLETED | 200 / 4 ms | COMPLETED | INCONCLUSIVE | 0.00 | SKIPPED | No | EXPORT_NOT_AUTHORIZED |

Cuatro códigos/revisión1, vínculos Scan/Execution/Evaluation uno a uno, decisionCode HYB-01, degraded=true, evidenceComplete=true, errorReason/llmErrorReason/severity/findingId null. requestRedacted/responseRedacted=false en las cuatro ejecuciones. `evaluatorProvider=OPENAI` y el modelo configurado identifican el componente; SKIPPED no representa una llamada ejecutada. Fuente: executions/evaluations/categories.json.

Respuesta target persistida, deterministicSignals, llm_attempt_count y exportAuthorized del snapshot interno: **NOT_INSPECTED**. Las APIs los omiten. No se rellenan con el oracle ni con la comprobación aislada del manifiesto, que es otra evidencia. No se exige SQL ni se afirma una captura universal de egress.

Score: AVAILABLE/scoring-v1, 0/NO_FINDINGS, evaluationCount4, findingCount0, notVulnerableCount0, inconclusiveCount4, severidades cero. Breakdown ordenado LLM01/02/08/10: 0/NO_FINDINGS, una I/evaluación y cero Findings/N en cada categoría. Score cero significa **no se han generado Findings**, no sistema seguro.

Report: AVAILABLE/report-v1/es; doce campos; scope/ids/targetUpdatedAt correctos; summary idéntico al score; cuatro proyecciones de evaluación concordantes de diecinueve campos y siete versiones; Findings vacío. Cuatro textos de interpretation y nueve limitations coinciden literalmente con reporting/report-v1/es.json, códigos contrastados con contrato vigente. Fechas evaluation→score→report→Scan.finishedAt ordenadas. La segunda lectura no cambia id/generatedAt/contenido. checks.json registra las comprobaciones.

## Matriz externa ground truth/observado

Unión externa posterior por profile=VULNERABLE, scenarioVersion=1, categoría/code/revision y Scan real. ground-truth-comparison.json conserva caseId y unión; nunca fue entrada del evaluador.

| Categoría | Ground truth | Resultado scanner | Clasificación |
| --- | --- | --- | --- |
| LLM01 | VULNERABLE | INCONCLUSIVE | INCONCLUSIVE |
| LLM02 | VULNERABLE | INCONCLUSIVE | INCONCLUSIVE |
| LLM08 | VULNERABLE | INCONCLUSIVE | INCONCLUSIVE |
| LLM10 | VULNERABLE | INCONCLUSIVE | INCONCLUSIVE |

No FN, precision/recall ni benchmark formal. LLM10 solo OUTPUT_ONLY, sin sink demostrado. Referencias técnicas sintéticas autorizadas permanecen en el contexto; el evaluador no es completamente ciego a ellas. Oracle, etiquetas y justificaciones permanecen externos.

## Frontend y cobertura literal 9.4

QA real en navegador integrado: Projects lista el Project; ProjectPage muestra Target e histórico con el único Scan; TargetPage relee URL/template/responsePath/timeout/auth NONE y headers vacíos, sin guardar. Lanzamiento desde ProjectPage, estado automático Pendiente→Completado, cuatro pruebas/evaluaciones, score/risk/categorías, estado vacío de Findings, cuatro I/motivos/confidence, report/alcance/interpretación/nueve limitaciones, trazabilidad report-v1/es, enlaces internos, vuelta al Project, apertura desde histórico y refresh de la ruta del mismo Scan: PASS. Sin cambios visuales ni de producto.

Capturas: 01-scan-summary.png, 02-evaluations.png, 03-report-limitations.png y 04-project-history.png. Algunas muestran un segmento de la página; las comprobaciones API y lectura accesible abarcaron las cuatro filas y las nueve limitaciones. Sin captura de proyectos ajenos, credenciales o HAR completo.

| Paso de 9.4 | Estado | Evidencia |
| --- | --- | --- |
| Abrir proyecto | PASS | Projects→ProjectPage; 04-project-history.png |
| Mostrar target autorizado preparado | PASS | TargetPage y preflight/API/binding exacto |
| Ejecutar | PASS | Clic único ProjectPage e id real |
| Estado real | PASS | Pendiente→Completado; scan.json |
| Score/Findings/I | PASS | Score0, vacío y cuatro I; 01/02 y APIs |
| Abrir Finding real | PENDING | No existe ninguno en el recorrido offline |
| Report/limitaciones | PASS | report.json, trazabilidad y 03-report-limitations.png |
| Manifiesto ligado a target.updatedAt | PASS | Preflight anterior al clic, relectura exacta |

**9.4 permanece pendiente** en su change histórico, sin modificarlo. Falta abrir un Finding persistido real y comprobar sus datos en un recorrido complementario autorizado que lo produzca. Su texto no exige intrínsecamente un proveedor real; no se introduce ese requisito ni se fabrica detección.

## Alcance, defectos y pendientes

Ningún defecto de producto observado; ninguna corrección ni test nuevo. No se ejecutan suites preventivas sin cambios. Se realizaron comprobaciones de integración sobre el recorrido real y comparación de resultados públicos; no tests automáticos con BD. Los problemas de herramientas durante preparación (representación de headers vacío y entrada JShell) se resolvieron en comprobaciones temporales; no fueron defectos del scanner ni motivaron cambios de producto.

Cobertura literal aún parcial: creación/guardado Project/Target desde formularios UI (se pidió API), cabecera Location de lanzamiento en Network y cancelación de polling durante un estado activo. Se conservan sin cerrar las tareas afectadas. No se repiten escrituras ni Scans para rellenar esas evidencias. El flujo funcional pedido ahora y sus resultados API/UI sí se han observado.

Servicios propios se conservan para inspección del operador. Cierre operacional/revocación de excepción local al terminar: pendiente del operador; exportación ya permanece false. Historial y ambos JSON locales se conservan, sin borrados. Script opcional 2.1–2.5: NO APLICABLE, no implementado. Evaluador real/SAFE, SQL interno, AWS/Docker/PDF y benchmark no ejecutados. Review-agent pendiente; no se ha invocado otro agente.

## Comandos y acciones realmente realizados

- Preflight Get-NetTCPConnection/Get-CimInstance sobre los PIDs/argumentos no secretos y GET Project/Target/health/proxy. Flyway validó esquema durante el arranque autorizado previo, sin migración nueva.
- Creación previa por POST /api/projects y POST /api/projects/{projectId}/target; se capturaron IDs reales y timestamp literal.
- JSON preparado por sustitución exclusiva de placeholders; Test-Json con ambos esquemas, relectura API y git check-ignore; carga aislada del adaptador Java del mismo artefacto. Hashes en hashes.json.
- En Angular: abrir Project y un clic Ejecutar análisis; Angular realizó el polling existente. No segundo POST.
- GET Scan y sus cinco bloques, mediante Invoke-WebRequest -TimeoutSec 10 -MaximumRedirection 0; segunda lectura GET report; GET demo health final.
- Comprobaciones locales sobre los JSON conservados: cardinalidad, identidades, semántica offline, score/breakdown, report/proyección/textos/fechas y unión externa con ground truth. Sin SQL.
- Navegación UI/Target/Projects/Scan/histórico/anchors/refresh y cuatro PNG. No HAR ni logs extensos.
- `OPENSPEC_TELEMETRY=0; openspec validate e2e-integration --strict` → `Change 'e2e-integration' is valid`, exit code 0 el 2026-10-16, fuera del sandbox para cargar el esquema local. PASS estructural; no convierte las tareas pendientes en realizadas.
