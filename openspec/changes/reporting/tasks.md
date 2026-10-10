## 1. Aprobación y documentación afectada

Implementación solicitada y validaciones técnicas autorizadas expresamente por el usuario el 2026-10-10. D-01..D-24 permanecen aprobadas, con las aclaraciones sobre idioma propio de report-v1, proyección mínima de evaluations y máximo cuatro Findings derivado del plan MVP, registradas en design. Las casillas reflejan trabajo realizado; los comandos, resultados y trazabilidad se registran al final.

- [x] 1.1 Tomar como baseline D-01..D-24 y Approval Status ya aprobados; verificar por revisión que proposal/spec/design/tasks conservan el contrato exacto, lifecycle, límites y alcance, incluidas las tres aclaraciones documentales, sin reabrir decisiones previas (RP-01..RP-14).
- [x] 1.2 Actualizar arquitectura general/ADR-003 y crear ADR de reporting aprobado con alternativas y Fuera del MVP; verificar correspondencia con D-01..D-24, atomicidad score+report+cierre e históricos, sin cambiar políticas de evaluation/scoring.
- [x] 1.3 Actualizar OpenAPI/modelo de datos y privacidad directamente afectada; verificar por revisión los doce campos raíz, GET/availability, V7 y límites, y mantener requisitos oficiales/checklist TFM sin estados completados no acreditados (RP-02/RP-08/RP-12..RP-14).

## 2. Resultado inmutable y presentación controlada

- [x] 2.1 Crear SecurityReport, VOs de copia y disponibilidad propia independientes de Spring/JPA/Jackson/HTTP/PDF; preparar pruebas de esquema cerrado, relaciones/nulls/inmutabilidad/UTC e imports, verificando que no se construye ScanScore ni se invoca su política desde reporting (RP-01/RP-02/RP-10/RP-14).
- [x] 2.2 Crear recurso local versionado report-v1/es con cuatro interpretaciones, nueve limitaciones y mappings de D-04; localizar únicamente labels/nombres visibles y textos controlados propiedad del informe, no textos de Finding. Verificar igualdad literal/orden/locale, resource cerrado sin texto libre/URL y conservación de versiones históricas (RP-03/RP-07/RP-10).
- [x] 2.3 Implementar carga tipada de constantes mediante ReportPresentationPort, sin renderer ni executiveSummaryText; preparar recursos inválidos/ausentes/versión desconocida y verificar que generan fallo técnico, y que enum HIGH sigue en inglés con labels Alta/Alto separados (RP-02/RP-03/RP-09).
- [x] 2.4 Implementar comparador explícito severity descendente/categoría/id lowercase lexicográfico; preparar permutaciones, HIGH con confidence menor que MEDIUM, CRITICAL sintético y desempates en comparador puro; verificar copia exacta de recomendaciones repetidas sin deduplicar ni alterar orden del GET Findings anterior (RP-04).
- [x] 2.5 Preparar fixtures de report sin Findings/cuatro N, cuatro I, LOW, HIGH+LOW, múltiples severities y CRITICAL/score100 sintético; verificar score0 acotado, I/N/motivos visibles, contadores/categorías copiados, recomendaciones originales y nueve limitaciones siempre presentes. Añadir Finding válido con title/description/evidenceSummary/recommendation en inglés: verificar copia literal de los cuatro, locale=es y textos propios castellanos, sin traducción/reformulación ni servicio externo (RP-03..RP-07).
- [x] 2.6 Implementar validación de longitudes/colecciones/Unicode y guard de bytes en adaptadores; preparar title150/151 y textos1000/1001 puntos de código, no blancos/NUL/markup prohibido, cuatro/cinco filas, versiones64/65 y criterio64/65/32/33; verificar cuatro Findings admitidos/cinco rechazados sin recorte, como límite report-v1 derivado de cuatro SecurityTests/una evaluación por categoría/máximo un Finding por evaluación, no universal. Probar guard aislado262144/262145 y wrapper512 sin fabricar un report-v1 inválido para rellenar tamaño, más fixture completa máxima con escapes (RP-02/RP-14, D-23).

## 3. Inputs mínimos y generación determinista

- [x] 3.1 Definir ReportInputPort/proyecciones propias e implementar SQL de columnas permitidas para Scan, target refs persistidas, score insertado, finales/versiones y Findings; evaluations usará exactamente los diecinueve campos y siete identificadores de versions de D-03. Verificar por revisión SQL y pruebas de proyección que no usa SELECT *, LlmAssessment completo, reasoning interno, prompts/schemas internos, señales deterministas internas, provider raw response, evidencia raw, chain-of-thought, target actual o secretos/credenciales ni construye ScanScore o EvaluationResult completos. No introducir señales sin campo público expresamente aprobado; report-v1 no tiene ninguno (RP-01/RP-02/RP-14).
- [x] 3.2 Validar conjunto completo de cuatro pruebas/revisión1/categorías, igualdad de target refs, vínculos V↔Finding, confidence/severity y contadores frente al summary original; preparar ausencias/extras/duplicados/relaciones ajenas/ref target divergente/summary discordante y verificar error sin deduplicación, recorte ni recálculo de score (RP-01/RP-05/RP-09).
- [x] 3.3 Crear GenerateReportUseCase con UUID/reloj inyectables, orden/copia literal y fechas no regresivas, participante en T3 sin transacción propia; verificar con fixtures N/I/V/fallback LLM los doce campos, proyección mínima de evaluations, textos exactos y versions/null de origen. Verificar title/description/evidenceSummary/recommendation de Finding literalmente en su idioma original, summary literal y cero llamadas/decisiones/cálculos/escrituras sobre fuentes o traducciones externas (RP-01..RP-08).
- [x] 3.4 Preparar repetición desde mismos inputs/versiones con distinto orden/reloj, cambio posterior de Project/target y recursos actuales; verificar payload semántico/orden originales, ninguna URL actual atribuida al análisis, preservación de ids/fechas del snapshot ya confirmado y ausencia de datos sensibles en toString/log/errores (RP-02/RP-10/RP-14).

## 4. V7 y persistencia del snapshot

- [x] 4.1 Crear V7__incorporar_informes_seguridad.sql con scans.report_version nullable y security_reports de D-17; verificar revisión SQL: PK reportId, UNIQUE Scan+versión, FKs Scan/score, marker con dependencia explícita no-NULL, no defaults/backfill/cascadas/tablas hijas y V1–V6/global_score intactos (RP-08/RP-12).
- [x] 4.2 Definir CHECKs de metadata/payload y función report_v1_payload_valid para esquema cerrado, nulls/tipos/enums/relaciones internas/orden/contadores/bytes; preparar JSON incorrecto, claves desconocidas/ausentes, fracciones, duplicados, UUID/UTC y metadata discordante. Verificar que no reproduce fórmulas de scoring ni SQL UNKNOWN permite eludir checks (RP-02/RP-12/RP-14).
- [x] 4.3 Implementar SecurityReportRepositoryPort y adaptador con insert inmutable/lectura tipada, sin upsert update ni reconstrucción mediante scoring; preparar roundtrip de todos los campos/Unicode/fechas/versiones, candidato igual conservando reportId/generatedAt, divergencia y errores de flush/commit sin éxito provisional (RP-02/RP-10/RP-12).
- [x] 4.4 Preparar integración PostgreSQL y fixture V6→V7 con N/I/V, Findings/scores legítimos, activos/históricos y global_score previo; verificar migración conservadora, marcador null/tabla vacía, FKs/uniqueness/checks/cap, rollback y snapshot inmutable desde conexión independiente, exclusivamente en esquema dedicado autorizado (RP-10/RP-12/RP-14).

## 5. Integración de finalización, fallos y recuperación

- [x] 5.1 Fijar report-v1 requerido en T1 junto con perfiles previos antes de dispatch, sin campo público nuevo; preparar rollback de creación y comparación nuevo/histórico V6 para verificar elegibilidad durable y ausencia de flag público de omisión (RP-08/RP-12).
- [x] 5.2 Ampliar composición T3 de TransactionalScanLifecycle: lock Scan primero, validar durable previo, insertar score, generar/insertar report dentro de esa transacción y transición conjunta; preparar fallos después de score, construcción, insert report y transición para verificar rollback íntegro y ningún uso post-insert del guard público ScanScoreSnapshot sobre RUNNING (RP-08/RP-09).
- [x] 5.3 Preparar recorrido nuevo con cuatro N, cuatro I, LOW y mezcla V/N/I con fallo LLM consolidado; verificar COMPLETED con score+report y contadores/copias exactos, preservando siete campos/cuatro estados/motivos/cuotas/deadline300s, terminales y contratos anteriores (RP-01/RP-06/RP-08).
- [x] 5.4 Integrar error técnico de reporting con FAILED/SCAN_PROCESSING_FAILED y compuerta existentes; preparar fuente inválida, versión/recurso desconocido, tamaño excedido, escritura fallida y compensación indisponible; verificar rollback de score/report, preservación de Findings/evaluaciones y ausencia de falso I/score0/report parcial (RP-09).
- [x] 5.5 Preparar dos cierres concurrentes, reentrega, candidato divergente y commit ambiguo confirmado/no confirmado; verificar una fila por Scan+reportVersion, reportId/generatedAt/payload originales, sin overwrite/reapertura/regeneración, sin continuar transacción abortada y compensación condicional que conserva conjunto confirmado (RP-09/RP-10).
- [x] 5.6 Extender validación de integridad en recuperación conjunta existente y preparar todas las fronteras D-16, recovery repetida e históricos; verificar activos interrumpidos sin score/report, terminales íntegros, cero generación/red, corrupción durable sin repair/backfill y gate cerrado si falla validación/recuperación (RP-11/RP-12).

## 6. Consulta mínima y protección de salida

- [x] 6.1 Crear GetSecurityReportUseCase y GET scoped con DTO cerrado {availability,report}; implementar proyección conjunta Scan/marcadores/score/report sin enriquecer desde fuentes actuales; preparar polling concurrente a T3 y verificar PENDING o AVAILABLE íntegros, sin falso error por instantáneas cruzadas ni cálculo/ensamblado/escritura en GET (RP-02/RP-13).
- [x] 6.2 Preparar availability AVAILABLE con score0/100, PENDING para PENDING/RUNNING, UNAVAILABLE FAILED nuevo antes del plan/durante reporting/recuperación y NOT_APPLICABLE histórico V6 con score o FAILED; verificar nulls, reportVersion/locale/shape, invariantes de /score preservadas y ninguna generación retrospectiva (RP-06/RP-10/RP-12/RP-13).
- [x] 6.3 Preparar UUID inválidos con precedencia, Project ausente, Scan ajeno, DB fallida, versión desconocida, COMPLETED marcado sin report, report con estado/marker/score/summary incoherentes; verificar 400/404/500 sanitizados y no-store, nunca falsa ausencia/reparación, y corrupción que bloquea recovery (RP-09/RP-11/RP-13).
- [x] 6.4 Preparar pruebas de contrato OpenAPI/privacidad y regresión de consultas previas: campos exactos, UTC, orden, límites JSONB/API/wrapper; LlmAssessment completo, reasoning interno, prompts/schemas internos, señales deterministas internas, provider raw response, evidencia raw, chain-of-thought y secretos sintéticos excluidos de proyección/logs/errores. Verificar evaluations mínimo sin copiar EvaluationResult, conservación literal del idioma de los cuatro textos de Finding al consultar, texto inerte y GET sin claves/target/proveedor; ausencia de frontend/render/export/dependencias nuevas y Scan con siete campos (RP-02..RP-04/RP-13/RP-14).

## 7. Coherencia y comprobaciones autorizadas

- [x] 7.1 Revisar trazabilidad de los escenarios RP-01..RP-14 a pruebas previstas y comparar proposal↔spec↔design↔tasks↔implementación/documentación; comprobar explícitamente localización solo de presentación propia, cuatro textos de Finding literales, evaluations mínimo/seguro y cadena causal del máximo cuatro Findings, con revisión/versionado obligatorio si hay múltiples tests por categoría en el futuro. Verificar que no hay interpretación/evaluación/scoring nuevos, report parcial, exposición pública, hash o modificaciones de modelos previos, ni cambio del experimento TFM.
- [x] 7.2 Ejecutar exclusivamente comprobaciones autorizadas posteriormente, registrar comandos/resultados y omisiones reales; PostgreSQL/Flyway solo en fixture aislada verificada y reporting sin servicios externos. Verificar que strict no se presenta como tests backend o validación de privacidad/calibración empírica.
- [x] 7.3 Revisar entrega final y documentación directamente afectada con evidencia real; indicar pendientes de pruebas/operación y no marcar requisitos académicos completos por planning. No aprobar, archivar, sincronizar ni iniciar otro change automáticamente.

## Traceability

| Requisito | Tareas principales |
| --- | --- |
| RP-01 Fuentes/separación | 2.1, 3.1..3.3, 5.3, 7.1 |
| RP-02 Snapshot/contrato | 2.1..2.3, 2.6, 3.1, 3.3, 4.2..4.3, 6.1, 6.4 |
| RP-03 Presentación castellana/textos originales/resumen | 2.2..2.3, 2.5, 3.3, 6.4 |
| RP-04 Findings/orden/recomendaciones | 2.4..2.5, 3.3, 6.4 |
| RP-05 Copia de scoring/contadores | 2.5, 3.2..3.3 |
| RP-06 Cero/I/fallos | 2.5, 3.3, 5.3, 6.2 |
| RP-07 Limitaciones | 2.2, 2.5, 3.3 |
| RP-08 Lifecycle | 3.3, 5.1..5.3 |
| RP-09 Errores | 2.3, 3.2, 5.2, 5.4..5.5, 6.3 |
| RP-10 Versionado/idempotencia | 2.2, 3.4, 4.3..4.4, 5.5, 6.2 |
| RP-11 Recovery | 5.6, 6.3 |
| RP-12 V7/históricos | 4.1..4.4, 5.1, 5.6, 6.2 |
| RP-13 API/availability | 6.1..6.4 |
| RP-14 Privacidad/límites | 2.1, 2.6, 3.1, 3.4, 4.2..4.4, 6.4 |

Comparadores con empates y guard de bytes se prueban aisladamente; no amplían el plan de cuatro pruebas ni permiten alterar constantes para crear un informe artificial. Los ejemplos CRITICAL son fixtures sintéticas válidas del lector/report, no nuevos criterios de Evaluation Engine.

## Evidencia de implementación (2026-10-10)

Implementación exclusiva de reporting conforme a D-01..D-24. No se han modificado proposal/design/spec, políticas de scoring/evaluación, dependencias ni V1–V6. Las constantes de interpretación y las nueve limitaciones se copiaron literalmente de design al recurso versionado. La checklist TFM se revisó y conserva sus estados: implementar el informe estructurado no acredita entrega académica, exportación, despliegue ni calibración.

### Trazabilidad de los 33 escenarios

Abreviaturas: G = GenerateReportUseCaseTest; D = SecurityReportTest; P = ReportingPostgresIntegrationTest; M = ReportingMigrationIntegrationTest; A = ReportControllerTest; O = ReportingOpenApiContractTest. Cada clase se encuentra en el inventario inferior.

| Requisito / escenarios | Evidencia ejecutable |
| --- | --- |
| RP-01 (3): fuentes válidas, ausencias/duplicados, target modificado | G.copiesAllResultsCountsAndLiteralEnglishTexts, rejectsPartialExtraDuplicateForeignAndDivergentTargetRows; P.zeroInconclusiveMixedLiteralTextsVersionsAndIndependentRoundTrip (target cambiado antes y después del cierre). |
| RP-02 (2): snapshot histórico, esquema cerrado | P.zeroInconclusiveMixedLiteralTextsVersionsAndIndependentRoundTrip; D.exactShapeImmutableReferencesAndStrictUtcRoundTrip; M.v1ToV7AndClosedSqlSchemaNullsTypesRelationsOrderingMetadataLimitsAndFks. |
| RP-03 (3): resumen, idioma/enums, Finding original | G.mixedFallbackAndOrderingAreStableWithDifferentInputOrderAndClock; D.controlledResourceMatchesApprovedLiteralTextsAndLabelsAndRejectsBadResources; P.zeroInconclusiveMixedLiteralTextsVersionsAndIndependentRoundTrip. |
| RP-04 (3): LOW, orden, recomendaciones repetidas | G.oneLowHasOneOriginalRecommendationAndThreeNegatives, copiesAllResultsCountsAndLiteralEnglishTexts; D.comparatorHasExplicitSeverityAndUnsignedLexicalUuidTieBreak; G.mixedFallbackAndOrderingAreStableWithDifferentInputOrderAndClock. |
| RP-05 (2): global/breakdown, cien/CRITICAL | G.mixedFallbackAndOrderingAreStableWithDifferentInputOrderAndClock, severityAndScoreAreCopiedNotReinterpreted; A.availabilityNullsAndAvailableZeroAndHundred. |
| RP-06 (3): cuatro N, cuatro I, mezcla/fallo componente | G.copiesAllResultsCountsAndLiteralEnglishTexts (parametrizado), mixedFallbackAndOrderingAreStableWithDifferentInputOrderAndClock; P.privateSourceDocumentsNeverEnterSnapshotAndRealWorkerProducesFourInconclusiveWithoutExternalServices. |
| RP-07 (1): nueve limitaciones sin detecciones | D.controlledResourceMatchesApprovedLiteralTextsAndLabelsAndRejectsBadResources; G.copiesAllResultsCountsAndLiteralEnglishTexts. |
| RP-08 (2): cierre íntegro, rollback | P.newCreationMarkersRollbackAndWorkerCompletesWithoutOpenAi, privateSourceDocumentsNeverEnterSnapshotAndRealWorkerProducesFourInconclusiveWithoutExternalServices, failuresInConstructionSerializationValidationInsertAfterInsertAndTransitionRollBackTogether. |
| RP-09 (2): fallo interno, commit ambiguo/compensación | P.failuresInConstructionSerializationValidationInsertAfterInsertAndTransitionRollBackTogether, ambiguousCommitPreservesConfirmedTrioOrAllowsConditionalCompensation, failedCompensationClosesGateUntilRecoveryWithoutFabricatingResults. |
| RP-10 (2): concurrentes, divergencia/evolución | P.concurrentCloseStableIdentityCandidatesUniqueAndNoOverwrite; D.controlledResourceMatchesApprovedLiteralTextsAndLabelsAndRejectsBadResources; G.mixedFallbackAndOrderingAreStableWithDifferentInputOrderAndClock. |
| RP-11 (2): antes/después de commit, corrupción durable | P.recoveryPreservesConfirmedAndInterruptsUnconfirmedWithoutGenerating, corruptedDurableStatesFailGetAndRecoveryAndKeepAdmissionClosed, missingReportOrDivergentSummaryProjectAndUnknownVersionAreIntegrityErrors. |
| RP-12 (2): V6 legítima, escritura inválida | M.v6WithLegitimateScoreFindingsAndActiveHistoryUpgradesWithoutBackfill, v1ToV7AndClosedSqlSchemaNullsTypesRelationsOrderingMetadataLimitsAndFks, migrationsV1ToV6ArePackagedWithoutChanges. |
| RP-13 (3): availability, histórico/polling, errores/scoping | A.availabilityNullsAndAvailableZeroAndHundred, uuidPrecedenceScopedOwnershipAndSanitizedErrorsNoStore; P.pollingDuringUncommittedReportSeesPendingThenAvailable; M.v6WithLegitimateScoreFindingsAndActiveHistoryUpgradesWithoutBackfill. |
| RP-14 (3): privacidad, límites, texto inerte/sin exportador | P.privateSourceDocumentsNeverEnterSnapshotAndRealWorkerProducesFourInconclusiveWithoutExternalServices; D.sourceTextCodePointBoundariesUnicodeAndNoSilentTruncation, maximumCompleteFixtureAndIsolatedByteBoundaries, reportingDomainAndApplicationNeverImportFrameworksOrScoringPolicy; O.exactClosedContractsReadOnlyEndpointNoStoreAndMinimalProjections. |

Las pruebas de bytes262144/262145 y wrapper512/513 son guards aislados; la fixture máxima válida de cuatro Findings y escapes mantiene los límites originales. No se fabrica un informe inválido para rellenar256KiB. Desempates se prueban en comparador puro. JSONB duplicado en origen se detecta por lector estricto antes del cast; JSONB no conserva duplicación de claves ni orden textual de propiedades. Las comparaciones de históricos y de snapshots son semánticas y preservan arrays/identidades/fechas, sin hash.

### Inventario de archivos creados

- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/ReportController.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/ReportResponse.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/reporting/PostgresReportInputAdapter.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/reporting/PostgresSecurityReportRepositoryAdapter.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/reporting/ReportRows.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/reporting/LocalReportPresentationAdapter.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/reporting/ReportJson.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/GenerateReportUseCase.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/GetSecurityReportUseCase.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/ReportAvailability.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/ReportInputPort.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/ReportListing.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/ReportPresentationPort.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/ReportSnapshot.java`
- `backend/src/main/java/com/llmscannerseguridad/application/reporting/SecurityReportRepositoryPort.java`
- `backend/src/main/java/com/llmscannerseguridad/config/ReportingConfiguration.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/reporting/ReportEvaluation.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/reporting/ReportFinding.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/reporting/ReportRules.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/reporting/ReportSummary.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/reporting/SecurityReport.java`
- `backend/src/main/resources/db/migration/V7__incorporar_informes_seguridad.sql`
- `backend/src/main/resources/reporting/report-v1/es.json`
- `backend/src/test/java/com/llmscannerseguridad/adapters/in/rest/scan/ReportControllerTest.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/in/rest/scan/ReportingOpenApiContractTest.java`
- `backend/src/test/java/com/llmscannerseguridad/application/reporting/GenerateReportUseCaseTest.java`
- `backend/src/test/java/com/llmscannerseguridad/application/reporting/ReportingFixtures.java`
- `backend/src/test/java/com/llmscannerseguridad/domain/reporting/SecurityReportTest.java`
- `backend/src/test/java/com/llmscannerseguridad/integration/ReportingMigrationIntegrationTest.java`
- `backend/src/test/java/com/llmscannerseguridad/integration/ReportingPostgresIntegrationTest.java`
- `docs/02-arquitectura/decisiones/ADR-008-reporting-snapshot-inmutable.md`

### Archivos existentes modificados

- `.gitignore`
- `backend/src/main/java/com/llmscannerseguridad/config/TransactionalScanLifecycle.java`
- `backend/src/main/java/com/llmscannerseguridad/config/ScanConfiguration.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/EvaluationExceptionHandler.java`
- `backend/src/test/java/com/llmscannerseguridad/integration/SecurityTestExecutionPostgresIntegrationTest.java`
- `docs/02-arquitectura/arquitectura-general.md`
- `docs/02-arquitectura/decisiones/ADR-003-ejecucion-asincrona-scans.md`
- `docs/03-seguridad/politica-de-autorizacion.md`
- `docs/05-api/openapi.yaml`
- `docs/06-base-de-datos/modelo-datos.md`
- `openspec/changes/reporting/tasks.md`

En .gitignore se conservan los cambios previos del usuario y se añade únicamente la excepción para V7, que quedaba ocultada por *.sql. La fixture de regresión que simula un COMPLETED histórico elimina también report_version, sin debilitar guards productivos.

### Validaciones ejecutadas

Java y javac verificados: Temurin21.0.12.1. PostgreSQL verificado por fixture mediante current_database() y server_version_num: exactamente llm_scanner_seguridad_test, PostgreSQL16.15. Credenciales cargadas en proceso exclusivamente desde las tres variables SCAN_TEST_DB_* autorizadas de .env, sin mostrarlas. Cada fixture destructiva utiliza su esquema aislado validado; no se ha usado llm_scanner_seguridad.

| Comprobación / comando | Resultado real |
| --- | --- |
| `java -version`, `javac -version` | Java21 confirmado antes de Maven. |
| `./mvnw.cmd -o -Dtest=SecurityReportTest,GenerateReportUseCaseTest,ReportControllerTest,ReportingOpenApiContractTest,ReportingPostgresIntegrationTest,ReportingMigrationIntegrationTest test` | PASS:35 pruebas,0 fallos/errores/omisiones. Log backend/target/reporting-all.log; fin2026-10-10 19:30:44 Europe/Madrid. |
| PostgreSQL/V1→V7 y V6→V7 | PASS dentro de ReportingMigrationIntegrationTest (3 pruebas). V6 con N/I/V, Findings, score, activos y global_score previo permanece sin backfill; FK/UNIQUE/CHECKs/NULL/tipos/límites rechazados correctamente. |
| `./mvnw.cmd -o package` con integraciones SCAN/PROJECT/TARGET habilitadas sobre la misma DB de test | PASS:400 pruebas,0 fallos/errores/omisiones; BUILD SUCCESS,2min16s. Log backend/target/reporting-package-final.log; fin2026-10-10 19:29:37 Europe/Madrid. Incluye regresión previa completa y35 de reporting. |
| `./mvnw.cmd -o -Dtest=ReportingPostgresIntegrationTest,ReportingOpenApiContractTest test` | PASS:12 pruebas,0 fallos/errores/omisiones. Verifica la última aserción de Project ajeno/404/no-store y documentación OpenAPI después de package, sin cambios productivos adicionales. Log backend/target/reporting-final-scope.log; fin2026-10-10 19:34:34 Europe/Madrid. |
| `openspec validate reporting --strict` | PASS. Solo valida estructura OpenSpec; no sustituye backend, PostgreSQL, privacidad ni calibración empírica. |
| `git diff --check`; revisión dirigida de V1–V6/pom/dependencias y scope | PASS, sin cambios en migraciones anteriores ni dependencias. |

Los primeros intentos detectaron y corrigieron errores de compilación de tests, una aserción documental y el conflicto entre el delimitador SQL dollar-quoted y placeholders Flyway; no se presentan esos intentos como PASS. Mockito/OpenSpec requirieron ejecución autorizada fuera del sandbox por restricciones locales de attach/acceso al esquema. No se instalaron dependencias ni se llamó a OpenAI/targets externos para reporting.

### Cierre de implementación

READY FOR REVIEW. Treinta tareas implementadas y verificadas, con revisión humana pendiente. Sin contradicciones objetivas ni desviaciones del contrato aprobado. Riesgos conservados y documentados: privacidad depende de proyecciones/plantillas upstream; snapshot no reconstruye URL/configuración histórica; retención/auth multiusuario y rollback operativo a binarios antiguos requieren decisiones posteriores. Ningún requisito académico se marca completado por estas pruebas. No commits, push, despliegue, archive, sync ni siguiente change.

