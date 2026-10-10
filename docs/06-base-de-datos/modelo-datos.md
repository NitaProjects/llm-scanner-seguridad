# Modelo de datos conceptual

El modelo vigente del backend core aprobado e implementado llega hasta V7, con EvaluationResult, ScanScore y SecurityReport separados de las columnas semánticas heredadas sin uso. Los contratos aprobados están actualmente en `openspec/changes/`; `openspec/specs/` aún no contiene specs consolidadas.

Las referencias a V1–V6 describen el alcance histórico de esos incrementos, sin reinterpretar datos previos. Los registros de implementación y validación de cada change conservan la secuencia de comprobaciones; las menciones históricas a migraciones/tests preparados sin ejecución no describen las validaciones técnicas posteriores registradas, por ejemplo en [reporting/tasks.md](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas).

## Entidades principales

### Project

Representa un proyecto de análisis con cero o una configuración de target en el MVP y sus scans.

Modelo aprobado e implementado por `project-management`, limitado a la tabla `projects`:

| Campo | Columna | Tipo PostgreSQL | Restricciones |
| --- | --- | --- | --- |
| `id` | `id` | `UUID` | Clave primaria; generado por la aplicación. Se conserva el default `gen_random_uuid()` de V1. |
| `name` | `name` | `VARCHAR(120)` | Obligatorio, no vacío ni solo espacios; nombres repetidos permitidos. |
| `description` | `description` | `VARCHAR(2000)` | Opcional; ausencia o texto vacío normalizado a `NULL`. |
| `createdAt` | `created_at` | `TIMESTAMPTZ(6)` | Obligatorio; instante generado por la aplicación. |
| `updatedAt` | `updated_at` | `TIMESTAMPTZ(6)` | Obligatorio; no anterior a `created_at`. |

El dominio retira los espacios en blanco iniciales y finales de ambos textos y cuenta los límites en puntos de código Unicode, sin alterar espacios interiores. Al crear, ambas fechas reciben el mismo instante UTC con precisión de microsegundos; las consultas no las modifican. La creación se confirma tras el commit de la transacción.

El proyecto puede crearse sin targets ni scans. `target_url` y `app_type` se retiran de `projects`; `TargetConfiguration` se almacena por separado en `target_configurations`. Se conservan la FK existente `scans.project_id → projects.id` y `idx_scans_project_id`, sin rediseñar `scans`.

Por la excepción inicial aprobada en `openspec/changes/project-management/design.md`, se ajusta directamente V1, sin V2 correctiva. Desde su uso como baseline real, V1 será inmutable y cualquier evolución requerirá una nueva migración Flyway. No se han ejecutado migraciones durante esta implementación.

### TargetConfiguration

Recurso único opcional por Project, aprobado en `target-configuration`. V2 crea la tabla sin modificar V1, projects ni scans. Múltiples targets quedan como evolución futura.

| Columna | Tipo PostgreSQL | Restricciones |
| --- | --- | --- |
| id | UUID | PK generada por aplicación |
| project_id | UUID | NOT NULL, FK projects(id), UNIQUE uq_target_configurations_project_id |
| name | VARCHAR(120) | Opcional |
| url | VARCHAR(2048) | NOT NULL |
| request_template | TEXT | NOT NULL; hasta 32768 bytes UTF-8 en aplicación |
| response_path | VARCHAR(1024) | NOT NULL |
| headers | JSONB | NOT NULL, objeto no sensible, default {} |
| auth_type | VARCHAR(20) | NONE, BEARER_TOKEN o API_KEY_HEADER |
| auth_header_name | VARCHAR(128) | Solo para API_KEY_HEADER |
| credential_ciphertext | BYTEA | Envelope AES-256-GCM v1; null para NONE |
| timeout_ms | INTEGER | NOT NULL, 1000..60000 |
| created_at | TIMESTAMPTZ(6) | NOT NULL |
| updated_at | TIMESTAMPTZ(6) | NOT NULL, >= created_at |

Checks: headers objeto; NONE sin header/blob, Bearer con blob y sin header, API key con ambos. UNIQUE garantiza cardinalidad incluso en creación concurrente. Sin cascada de borrado anticipada. method/contentType/promptPlaceholder son constantes derivadas y no se persisten, tampoco authConfig, secretValue ni hasCredentials.

El material cifrado separa la credencial de la configuración pública. Formato y provisión de clave externa (nunca en PostgreSQL) en ADR-002. Creación/actualización son atómicas, con commit antes del éxito; PUT preserva identidad/createdAt y no retrocede updatedAt. No hay versionado optimista ni detección de cambios perdidos.

### Scan

Recurso de ejecución asociado a Project, definido por `scan-execution` (SE-01 a SE-10). `security-test-execution` mantiene los siete campos y añade trabajo técnico real. V5/evaluation-engine incorpora cuatro evaluaciones; V6/scoring-engine y V7/reporting añaden sus marcadores internos prospectivos. En el estado actual, cada Scan nuevo COMPLETED requiere cuatro ejecuciones y cuatro evaluaciones COMPLETED, ScanScore scoring-v1 y SecurityReport report-v1 íntegros, confirmándose score, informe y cierre en la misma T3. Un fallo técnico global puede llevar a FAILED conservando éxitos/Findings. Un fallo/degradación del componente LLM puede consolidar INCONCLUSIVE o VULNERABLE según las reglas sin implicar Scan FAILED. Los históricos conservan la semántica de sus marcadores sin recálculo ni backfill; los no-op mantienen ejecuciones vacías. COMPLETED no acredita seguridad ni vulnerabilidad.

| Campo | Columna | Tipo PostgreSQL | Restricciones |
| --- | --- | --- | --- |
| id | id | UUID | PK generada por aplicación; default V1 conservado. |
| projectId | project_id | UUID | NOT NULL, FK projects(id) conservada, sin cascada. |
| status | status | VARCHAR(30) | NOT NULL; PENDING, RUNNING, COMPLETED o FAILED. |
| createdAt | created_at | TIMESTAMPTZ(6) | NOT NULL; default V1 conservado, asignado por aplicación. |
| startedAt | started_at | TIMESTAMPTZ(6) | Nullable; no anterior a created_at. |
| finishedAt | finished_at | TIMESTAMPTZ(6) | Nullable; no anterior a created_at ni started_at si existe. |
| errorReason | error_reason | VARCHAR(40) | Solo FAILED, obligatorio: SCAN_DISPATCH_FAILED, SCAN_PROCESSING_FAILED o SCAN_INTERRUPTED. |

V3 `V3__ajustar_ciclo_vida_scans.sql` añade `error_reason`, `ck_scans_status`, `ck_scans_error_reason`, `ck_scans_lifecycle` y `uq_scans_active_project`. El índice único parcial sobre project_id para PENDING/RUNNING garantiza un único activo por Project incluso ante concurrencia. Conserva `idx_scans_project_id`, FK, V1/V2 y `global_score NUMERIC(5,2)`: este último no se mapea, expone, calcula ni modifica; nuevas filas lo dejan null.

PENDING exige inicio/fin/error null; RUNNING exige inicio y fin/error null; COMPLETED exige inicio/fin y error null; FAILED exige fin/error, conservando inicio si ocurrió. SCAN_DISPATCH_FAILED exige inicio null. Los checks comprueban NULL explícitamente. Aplicación trunca a microsegundos y limita cada fecha al máximo entre reloj y fecha previa. Las transiciones son condicionales y los terminales no se reabren.

Tras reiniciar una única instancia se confirma PENDING/RUNNING → FAILED/SCAN_INTERRUPTED antes de admitir nuevos scans, sin relanzar ni alterar terminales. V3 debe fallar transaccionalmente ante históricos incompatibles o activos duplicados; no inventa fechas/motivos ni borra datos. Recuperación no sustituye esa validación. No se ha ejecutado Flyway durante la implementación.

La admisión mantiene la comprobación de existencia del target mediante proyección sin blob. Scan no añade FK directa ni snapshot. El worker real lee target una sola vez al preparar el plan y conserva id/updatedAt en TestExecution; su configuración completa/cifrado solo permanece en memoria. El progreso se deriva de GET executions sin añadir totalTests/completedTests a Scan. Históricamente V3/V4 no incorporaban evaluación ni scoring; V5/V6/V7 añaden evaluación, score e informe prospectivos, descritos en las secciones siguientes y en [ADR-008](../02-arquitectura/decisiones/ADR-008-reporting-snapshot-inmutable.md). Cancelación y PARTIAL siguen fuera del MVP actual.

### SecurityTest

Modelo aprobado por security-test-execution: exactamente id UUID, code, revision positiva, name, category (LLM01/LLM02/LLM08/LLM10) y prompt. V4 evoluciona test_cases con code VARCHAR(64), revision INTEGER y UNIQUE(code,revision); prompt mapea a payload_template TEXT con máximo 4096 bytes UTF-8. Conserva description, owasp_reference y created_at como metadatos de seed, fuera del dominio. Los cuatro UUID literales y prompts exactos están en V4; códigos estables y revisión 1. No CRUD, enabled, selección configurable ni reglas/campos semánticos. Nuevas revisiones requieren otro change, sin editar las originales referenciadas.

### TestExecution

Representa un intento técnico único por (scan_id,test_case_id). V4 conserva las FKs originales y las referencias de findings, y añade target_configuration_id UUID FK, target_updated_at TIMESTAMPTZ(6), status VARCHAR(20), started_at/finished_at TIMESTAMPTZ(6), http_status INTEGER, latency_ms BIGINT, error_reason VARCHAR(40), request_redacted/response_redacted BOOLEAN NOT NULL. La definición se obtiene del catálogo inmutable; no copia prompt/code/category. No guarda URL, headers, snapshot sensible ni credenciales. El timestamp del target no representa historial recuperable.

Estados PENDING/RUNNING/COMPLETED/FAILED. PENDING no tiene inicio ni datos técnicos; RUNNING tiene inicio; COMPLETED requiere fechas, HTTP 2xx, latencia y ambas evidencias; FAILED exige fin y código cerrado STE-10/STE-12. Un FAILED previo a inicio solo admite aborto/interrupción, sin HTTP/latencia/evidencia inventados. Terminales inmutables y fechas UTC microsegundo no regresivas, limitadas por Scan.startedAt. executed_at se conserva como preparación, sin campo redundante de dominio/API.

request_payload TEXT contiene JSON preparado/redactado; response_payload TEXT contiene una cadena JSON serializada con comillas y escapes, decodificada una sola vez al leer internamente. U+0000 queda como escape recuperable, nunca NUL literal; no se usa JSONB para ese texto. Flags de redacción exacta de credencial conocida con marcador [REDACTED_CREDENTIAL]. No se guarda body bruto de respuesta. Caps DB: request 65536 bytes y evidencia combinada 524288 bytes; la configuración aplica además defaults 131072 por ejecución/524288 por Scan y límite de texto antes de redactar. La cuenta incluye escapes y redacción antes de cada escritura.

evaluator_result/confidence se conservan sin mapeo ni escrituras, null en nuevas filas. global_score permanece intacto. V4 conserva su alcance técnico; V5 incorpora resultados/Findings separados sin reutilizar los campos semánticos heredados. El GET executions usa una proyección sin evidencia ni target. La lectura interna preparada en V4 para un evaluador posterior ya es utilizada por evaluation-engine; su persistencia independiente se describe en EvaluationResult.

V4 falla transaccionalmente si test_cases o test_executions contienen históricos incompatibles; no inventa identidades/estados ni borra datos. V1/V2/V3 quedan intactas. Preparación completa, inicio y resultado usan transacciones nuevas cortas; HTTP/cuota quedan fuera. Cierre de Scan y ejecuciones activas, y recuperación conjunta, son atómicos. Recuperación marca EXECUTION_INTERRUPTED/SCAN_INTERRUPTED sin reenvíos, preservando éxitos confirmados. Migración y tests preparados, sin ejecución automática.

### EvaluationResult

V5 `V5__incorporar_motor_evaluacion.sql` introduce resultados independientes de la evidencia técnica. V1–V4 permanecen intactas; no hay dual writes en evaluator_result/confidence heredados ni cambios en test_cases/global_score.

| Grupo / columnas | Tipos y contrato |
| --- | --- |
| id, test_execution_id, scan_id, test_case_id, category | UUID y VARCHAR(10); PK, UNIQUE por ejecución, FK compuesta a identidad de TestExecution; categoría validada contra catálogo al preparar/reclamar. |
| status, created_at, started_at, finished_at | PENDING/RUNNING/COMPLETED/FAILED; TIMESTAMPTZ(6) UTC y fechas no regresivas. Claim semántico exige padre COMPLETED. |
| error_reason, outcome_reason | Motivos cerrados globales y de abstención, distintos de errores LLM. COMPLETED exige error_reason null. |
| final_verdict, deterministic_verdict, decision_code, strategy | Tres verdicts, HYB-01..07 y HYBRID; decisión null antes de terminal. |
| severity, confidence, confidence_components | VARCHAR(10), NUMERIC(3,2) 0..1 y JSONB; severity solo V; I=0.00. Componentes/versiones reproducibles. |
| degraded, verdict_discrepancy, severity_discrepancy | BOOLEAN; conservan fallback y desacuerdos sin sustituir señales. |
| deterministic_signals, llm_assessment | JSONB de esquema validado; assessment sanitizado separado, nunca output bruto ni chain-of-thought. |
| llm_status, llm_error_reason, llm_attempt_count | NOT_REQUESTED/SUCCEEDED/FAILED/SKIPPED; error de componente cerrado; SMALLINT 0/1. |
| reasoning_summary, recommendation, evidence_summary | TEXT controlado/inertizado para usuario/auditor, límites explícitos. |
| criteria_id/version, scenario_id/version | Identidades de criterio/contexto; null solo donde falta contexto/configuración recuperable. |
| engine/policy/confidence/severity/prompt/schema_version, llm_input_profile | Versiones; HYBRID_WITH_SIGNALS diferencia este input de un LLM-only experimental. |
| configuration_snapshot, configuration_hash, sampling_parameters | JSONB seguro, SHA-256 canónico serializer-v1 y parámetros explícitos; sin corpus completo ni target. |
| evidence_projection, evidence_complete, redacted, input_bytes, evidence_bytes, input_hash | TEXT JSON serializado con Unicode/NUL escapado, BOOLEAN, INTEGER y SHA-256 de proyección sintética; sin duplicar evidencia libre. |
| requested_provider/model, resolved_model, latency_ms, input_tokens, output_tokens | Configuración externa y metadata realmente recibida; desconocido null, nunca inventado. |
| reserved_cost_usd, estimated_cost_usd, pricing_version, estimate_version | NUMERIC(12,6) y versiones; control monetario opcional. Todo importe estimado persistido conserva pricing_version y estimate_version, también sin reserva/límite activo. Tarifa desconocida: importe/versiones ausentes. Reserva ambigua conservada; estimación distinta de factura. |
| provider_request_id | Identificador sanitizado interno opcional, no público. |

PENDING/RUNNING no admiten decisión terminal; COMPLETED requiere fechas/decisión y admite LLM FAILED/SKIPPED degradado con error de componente separado; FAILED exige motivo global, I/0.00/severity null. Checks explícitos de NULL, rangos, tipos JSON y cap adicional 64 KiB. Claim y reserva usan lock de Scan/resultado y commit antes de red. Resultado terminal inmutable; una tentativa máxima y sin reenvío tras recuperación.

`scans.evaluation_profile_version VARCHAR(64)` es nullable: null histórico o evaluation-engine-v1. T1 nueva lo fija, sin exponer octavo campo. COMPLETED nuevo requiere cuatro ejecuciones y cuatro evaluaciones COMPLETED; los verdicts no hacen fallar el Scan. Recuperación conserva terminales/Findings y cierra activos en la misma transacción. Solo permite registros explícitos FAILED/EVALUATION_INTERRUPTED para evaluación ausente de una ejecución COMPLETED de un Scan nuevo que estaba activo.

V5 falla íntegramente si existen findings heredados o evaluator_result/confidence no nulos, sin borrar ni convertir. Datos técnicos V4 válidos y sus fechas/relaciones permanecen. Las pruebas aplican V5 únicamente a esquemas aislados de llm_scanner_seguridad_test, nunca a producción.

### Finding

Un Finding por EvaluationResult COMPLETED/VULNERABLE y por TestExecution, confirmado en la misma transacción corta. N/I no crean Finding. No duplica señales/assessment/snapshots: se consultan a través de evaluation_result_id.

| Campo | Columna / representación |
| --- | --- |
| id, evaluationResultId, testExecutionId, scanId, securityTestId | UUID obligatorios, relaciones coherentes. |
| category, severity, confidence | owasp_category, risk_level y confidence heredadas, proyección de resultado final. |
| verdict | final_verdict obligatorio VULNERABLE, derivado en dominio/API. |
| title | VARCHAR(150), plantilla controlada. |
| description, evidenceSummary, recommendation | explanation, evidence y recommendation TEXT, hasta 1000 puntos de código; sin respuesta bruta ni narrativa libre. |
| createdAt | TIMESTAMPTZ(6) UTC, asignada al terminal, no anterior a finishedAt. |

UNIQUE(test_execution_id) y UNIQUE(evaluation_result_id). FK compuesta a EvaluationResult incluye resultado/ejecución/Scan/test/categoría/verdict/severity/confidence; ningún campo nullable puede omitir esa integridad. El resultado vulnerable solo puede estar COMPLETED por los checks de ciclo. No estados de revisión ni score.

### ScanScore (V6 / scoring-v1)

`V6__incorporar_motor_scoring.sql` añade `scans.scoring_version VARCHAR(64) NULL`: null identifica históricos, scoring-v1 exige evaluation_profile_version no null e igual a evaluation-engine-v1. T1 lo fija antes de dispatch, sin añadir campo al DTO de Scan. V1–V5 permanecen intactas. V6 admite Findings V5 legítimos y no convierte históricos ni escribe/lee `scans.global_score`, aunque tenga valor previo.

| Columna scan_scores | Tipo PostgreSQL / restricciones |
| --- | --- |
| scan_id, scoring_version | UUID FK scans(id), VARCHAR(64) NOT NULL; PK compuesta y versión scoring-v1. Sin UUID adicional. |
| calculated_at | TIMESTAMPTZ(6) NOT NULL, UTC; fecha original no anterior a startedAt/finales de inputs/createdAt de Findings ni posterior a Scan.finishedAt. |
| global_score | SMALLINT NOT NULL, 0..100, cero si y solo si finding_count=0. |
| risk_level | VARCHAR(20) NOT NULL; NO_FINDINGS/LOW/MEDIUM/HIGH/CRITICAL y correspondencia exacta con bandas. |
| evaluation_count | SMALLINT NOT NULL, exactamente 4 en v1. |
| finding_count, not_vulnerable_count, inconclusive_count | SMALLINT NOT NULL, 0..4; suma=evaluation_count. |
| severity_counts | JSONB NOT NULL; objeto cerrado con LOW/MEDIUM/HIGH/CRITICAL, enteros 0..4 incluidos ceros, suma=finding_count. |
| categories | JSONB NOT NULL; array cerrado de cuatro VOs ordenados LLM01/LLM02/LLM08/LLM10. |

Cada VO contiene exactamente category, score, riskLevel, evaluationCount=1, findingCount, notVulnerableCount, inconclusiveCount y severityCounts. Se comprueban tipos JSON, enteros, claves exactas, orden/identidades, bandas, sumas de contadores/severities y correspondencia del score global con los cuatro pesos de categoría del plan v1. Tamaño combinado serializado severity_counts+categories<=16 KiB. Funciones SQL inmutables `scoring_v1_summary_valid` y `scoring_v1_categories_valid` implementan CHECKs defensivos; aplicación valida además vínculos, terminales y fechas. JSON null/missing no omite integridad. No hay defaults de score, input_hash, snapshots de inputs, tabla/lifecycle por categoría, updatedAt ni cascadas.

Una fila representa todo el resultado inmutable. Insert y RUNNING→COMPLETED comparten T3, con lock de Scan primero; fallo revierte ambas escrituras. La PK garantiza exclusión también en persistencia. Reentrada reutiliza la fila sin overwrite/actualizar calculatedAt; un candidato derivado diferente es error. Recovery no calcula ni reanuda: activos→FAILED/SCAN_INTERRUPTED conservando fuentes, y score+COMPLETED confirmados permanecen intactos. Corrupción durable no se repara automáticamente.

Fórmula reproducible congelada: bases LOW=10/MEDIUM=40/HIGH=70/CRITICAL=90; w=base+ROUND_HALF_UP(5*confidence) decimal exacto. Sin Findings, 0; con Findings B=max(w), R=sum(w)-B, score=min(100,B+min(10,floor(R/10))). HIGH>=70, CRITICAL>=90, solo LOW<=25, incremento adicional<=10 y cap100. RiskLevel: 0 NO_FINDINGS, 1..39 LOW, 40..69 MEDIUM, 70..89 HIGH, 90..100 CRITICAL. Misma fórmula por categoría sin pesos distintos. INCONCLUSIVE no suma riesgo ni equivale a N. Cero significa ningún Finding detectado en las pruebas completadas, no aplicación segura; contadores distinguen cuatro N de cuatro I.

GET scoped /score lee marcador/estado/resultado en una instantánea conjunta: AVAILABLE con objeto incluso cero, PENDING/null para activos nuevos, UNAVAILABLE/null para FAILED nuevo y NOT_APPLICABLE/null para cualquier histórico sin marcador. Un COMPLETED marcado sin score, versión/vínculo incorrectos o score en Scan no COMPLETED son integridad inválida/500; error de lectura nunca se transforma en ausencia. No fallback a global_score, backfill ni recálculo al consultar. Fuentes inmutables, relaciones, scoringVersion y calculatedAt permiten reproducir sin hash nuevo; cambiar política exige otra versión/change. Contrato exacto en OpenAPI y fundamentos en ADR-005.

### SecurityReport (V7 / report-v1)

`V7__incorporar_informes_seguridad.sql` añade `scans.report_version VARCHAR(64) NULL`: null histórico o report-v1 con scoring_version y evaluation_profile_version explícitamente no null y correctos. T1 fija el marcador antes de dispatch; no hay default/backfill ni cambio V1–V6/global_score.

| Columna security_reports | Tipo / restricciones |
| --- | --- |
| report_id | UUID PK, generado por aplicación e inmutable. |
| scan_id | UUID NOT NULL FK scans(id), sin cascada. |
| report_version | VARCHAR(64) NOT NULL report-v1; UNIQUE(scan_id,report_version). |
| scoring_version | VARCHAR(64) NOT NULL scoring-v1; FK compuesta a scan_scores. |
| generated_at | TIMESTAMPTZ(6) NOT NULL, instante original UTC, no de consulta. |
| locale | VARCHAR(8) NOT NULL es. |
| payload | JSONB NOT NULL, snapshot completo cerrado <=262144 bytes de su texto UTF-8. |

Payload contiene doce propiedades obligatorias: reportId, scanId, projectId, generatedAt, reportVersion, locale, scope, summary, evaluations, findings, interpretation, limitations. Metadata duplicada coincide con columnas; aplicación coteja projectId y scope con Scan, y summary por igualdad con score confirmado. No project_id relacional redundante ni tablas hijas. `report_v1_payload_valid` valida descriptor cerrado, tipos/nulls/enums, UUID lowercase, UTC seis decimales, longitudes, arrays, orden, vínculos y contadores; no implementa fórmulas de scoring. CHECKs usan IS TRUE para impedir escapes por SQL UNKNOWN. JSONB conserva orden de arrays, sin promesa de bytes/canonicalización/hash.

Summary copia los once campos de ScanScore y cuatro categorías sin cálculo. Evaluations contiene exactamente diecinueve campos y siete identificadores de versions (D-03/OpenAPI), nunca entidades/assessment/evidencia/señales internas. Findings copia quince campos públicos y sus cuatro textos literalmente en el idioma original. Máximo cuatro deriva del plan MVP, no es límite universal futuro. Locale es localiza solo cuatro interpretaciones, nueve limitaciones y labels propios versionados; códigos técnicos conservan inglés. Textos title<=150 y description/evidenceSummary/recommendation<=1000 puntos de código, no blancos/Unicode inválido/NUL/markup prohibido. Guard adicional de objeto API<=256KiB y wrapper<=512 bytes, sin recortes.

T3 confirma score+report+COMPLETED juntos, con lock Scan primero y fechas no regresivas. Insert inmutable sin UPDATE/upsert; candidato semánticamente igual conserva reportId/generatedAt originales y divergencia falla. Rollback/compensación técnica no altera finales/Findings. Recovery valida el conjunto, preserva confirmados e interrumpe activos sin generar; corrupción impide admisión, sin reparación. GET /report usa una instantánea coherente y no enriquece desde fuentes actuales: AVAILABLE con objeto incluso cero; PENDING/UNAVAILABLE/NOT_APPLICABLE con null. Históricos V6 con score siguen NOT_APPLICABLE sin backfill. ADR-008 y OpenAPI detallan interpretación, privacidad y nueve limitaciones.

## Relaciones conceptuales

```text
Project 1 ---- N Scan
Project 1 ---- 0..1 TargetConfiguration (MVP; N como evolución futura)
TargetConfiguration: precondición de Scan a través de Project, sin FK/snapshot en scan-execution
Scan 1 ------- N TestExecution
SecurityTest 1 ---- N TestExecution
Scan 1 ------- N Finding
TestExecution 1 - 0..1 EvaluationResult (MVP)
EvaluationResult 1 - 0..1 Finding (solo VULNERABLE)
TestExecution 1 - 0..1 Finding (MVP)
Scan 1 ------- 0..1 ScanScore por scoringVersion
Scan 1 ------- 0..1 SecurityReport por reportVersion
ScanScore 1 --- 0..1 SecurityReport por reportVersion
```

## Consideraciones de seguridad

- No almacenar claves API en `Project`.
- Las credenciales o referencias de autenticación deben gestionarse de forma segura y solo durante la ejecución autorizada.
- Las peticiones, respuestas y evidencias pueden contener información sensible.
- Deben aplicarse minimización, redacción, control de acceso y retención limitada.
- Project, TargetConfiguration, Scan, SecurityTest, TestExecution, EvaluationResult/Finding, ScanScore y SecurityReport tienen esquema aprobado hasta V7. Retención temporal, borrado y autorización de usuarios requieren decisiones posteriores; los límites por Scan no acotan el crecimiento histórico.

## Persistencia

PostgreSQL y Flyway gestionan los esquemas aprobados descritos arriba hasta V7. Solo migraciones nuevas; sin reinterpretación ni generación retrospectiva automática.
