## 1. Aprobación y contratos afectados

Implementación solicitada expresamente el 2026-10-10. D-01..D-17 y los tres ajustes del Approval Record se conservan como baseline. El usuario autoriza tests dirigidos, integración, regresión, package, PostgreSQL/Flyway exclusivamente en llm_scanner_seguridad_test y OpenSpec strict, con Java 21 y sin proveedor real. Los checks marcados reflejan trabajo realizado; evidencia y trazabilidad al final de este archivo.

- [x] 1.1 Tomar el Approval Record ya aprobado como baseline y comprobar su correspondencia con proposal/spec/design/tasks al iniciar implementación; verificar fórmula, bandas, metadata sin input hash, availability, categorías anidadas, marcador y extensión de COMPLETED, sin reabrir decisiones aprobadas ni modelos previos (SC-01..SC-13).
- [x] 1.2 Actualizar únicamente arquitectura general, ADR-003/ADR-005 y decisiones abiertas sección 6 con la política aprobada, sustitución de pesos provisionales, atomicidad y semántica histórica; verificar trazabilidad SC-02..SC-11 y conservar calibración/experimento/revisión manual como evolución futura.
- [x] 1.3 Documentar el contrato GET y ScanScore en OpenAPI, y V6/modelo en docs/06-base-de-datos; ajustar solo la semántica de scoring afectada en estrategia-evaluacion; verificar campos exactos, cero/N/I, ausencia, límites OWASP/OUTPUT_ONLY y que no se inventan resultados académicos ni estados de entrega (SC-05/SC-06/SC-11..SC-13).

## 2. Dominio y política pura scoring-v1

- [x] 2.1 Crear ScanScore y RiskLevel independientes de infraestructura; CategoryScore/SeverityCounts serán solo VOs contenidos, sin identidad/lifecycle/repositorio propio, y la identidad compuesta pertenece exclusivamente a ScanScore. Preparar casos de sumas, forma cerrada, inmutabilidad conjunta, límites, NULL, fechas y mapeo de bandas incluidas fronteras39/40/69/70/89/90 (SC-02/SC-06/SC-11/SC-13).
- [x] 2.2 Implementar bases 10/40/70/90 y bonus decimal HALF_UP(5*c); preparar las fronteras completas de D-04 con confidence0.00/0.09/0.10/0.29/0.30/0.49/0.50/0.60/0.69/0.70/0.89/0.90/0.95/1.00, valores inválidos y las cuatro severities, sin modificar confidence-v1/severity-v1 (SC-03).
- [x] 2.3 Implementar máximo más incremento amortiguado/cap de D-05; preparar los ejemplos exactos de D-06 para cero, LOW, MEDIUM, HIGH, CRITICAL, tres/cuatro/veinte LOW, HIGH+LOW, HIGH+MEDIUM+LOW y cuatro HIGH; veinte Findings se prueban solo en la política pura sin ampliar el catálogo (SC-04).
- [x] 2.4 Preparar casos de propiedad de la política: permutación/empates, monotonía al agregar Findings o elevar severity/confidence, LOW<=25/MEDIUM<=55/HIGH<=85, HIGH>=70/CRITICAL>=90 incluso c=0, divisor/cap en R0/10/99/100/101, scores99/100/100 con B95 y R45/50/60, cap100 con CRITICAL+HIGH y overflow rechazado; verificar que no hay media, suma ilimitada o descuentos por N/I (SC-03/SC-04).
- [x] 2.5 Implementar el resumen global y cuatro entradas de breakdown ordenadas dentro de ScanScore con la misma fórmula; preparar HIGH en LLM01, LOW en LLM02 y N/I en las restantes para verificar global76, categorías75/15/0/0, global>=máximo, sumas, sin pesos por categoría y rollback del resultado completo si falla una entrada; no crear tablas/agregados/lifecycle específicos (SC-06).
- [x] 2.6 Preparar casos de cero con cuatro N, cero con cuatro I, mezcla V/N/I/I, Finding con fallo LLM consolidado y ausencia de Findings individuales ocultados; verificar contadores exactos, NO_FINDINGS sin SAFE/garantía, ningún I convertido en N y ningún FAILED técnico puntuado (SC-02/SC-05/SC-13).

## 3. Inputs finales, elegibilidad y reproducibilidad

- [x] 3.1 Definir ScoringInputPort y proyecciones propias, e implementar lectura PostgreSQL de plan/ejecuciones/evaluaciones/Findings con todos los ids y finales necesarios; verificar que detecta filas ausentes/extras y no carga evidencia, prompts, target, ciphertext, assessment ni narrativa, ni usa DISTINCT/limitado que oculte duplicados (SC-01/SC-13, D-03/D-16).
- [x] 3.2 Implementar validación de cuatro pruebas/revisiones/categorías, cuatro ejecuciones/evaluaciones COMPLETED y correspondencia exacta V↔Finding; preparar ausencia de plan/evaluación, estados PENDING/RUNNING/FAILED, extras/duplicados, Scan ajeno y discordancias de ids/categoría/severity/confidence (SC-01/SC-08).
- [x] 3.3 Crear CalculateScanScoreUseCase sobre política/puertos y reloj inyectable, asignando calculatedAt no regresivo; preparar casos de reloj anterior a inputs, versión no soportada, datos inconsistentes y resultados derivados correctos; verificar que no abre transacción propia, no calcula input hash ni llama al target/evaluador (SC-01/SC-07/SC-08/SC-13).
- [x] 3.4 Preparar reconstrucción desde los mismos inputs persistidos y scoringVersion, con lectura desde nueva instancia y diferente orden; verificar mismo score/nivel/contadores/breakdown, conservación de finales/Findings/calculatedAt, ausencia de evaluación nueva y ninguna constante runtime variable; no crear hash, serializador canónico ni copia persistida de fuentes (SC-09/SC-13).

## 4. V6 y persistencia PostgreSQL

- [x] 4.1 Crear V6__incorporar_motor_scoring.sql con scans.scoring_version nullable y scan_scores de D-14, sin modificar V1–V5 ni global_score; verificar PK/FK, dependencia de perfil sin aceptar NULL ambiguo, tipos, rangos, no defaults/backfill ni columna input_hash/tablas por categoría, y aceptación de Findings V5 legítimos (SC-11).
- [x] 4.2 Definir checks de score/riskLevel, contadores, JSONB cerrado de severities/categorías, sumas/orden/identidades y cap16KiB; preparar pruebas de escrituras inválidas, null, fraccionarios, claves desconocidas/ausentes, duplicados de categoría/PK, FK ajena y límites0/100; verificar que breakdown y resumen son una sola fila inmutable (SC-06/SC-11/SC-13).
- [x] 4.3 Implementar ScanScoreRepositoryPort y adaptador PostgreSQL con lectura mínima e insert inmutable, sin upsert update ni setters públicos; preparar roundtrip exacto de todos los campos, decimal/fechas/JSON, clave compuesta y error de flush/commit sin éxito provisional (SC-09/SC-11/SC-13).
- [x] 4.4 Preparar fixture de migración V5→V6 sobre esquema aislado de la base de pruebas existente con N/I/V, Findings legítimos y global_score histórico no nulo; verificar preservación byte a byte de V1–V5, datos/fechas/relaciones, marcador null en anteriores y cero filas de score inventadas, sin guard que exija findings vacía (SC-11).
- [x] 4.5 Preparar integración PostgreSQL para resultado cero, breakdown mixto, fechas, restricciones, rollback e idempotencia concurrente; usar el soporte de pruebas existente sin dependencias nuevas ni servicios reales; verificar que solo opera sobre esquema aislado autorizado y no consulta credenciales para documentar resultados (SC-07..SC-11).

## 5. Finalización atómica, fallos e idempotencia

- [x] 5.1 Fijar scoring-v1 requerido junto con evaluation-engine-v1 dentro de T1 antes del dispatch; preparar rollback de creación y comparación de Scan nuevo frente a histórico evaluado, verificando marcador durable sin octavo campo público ni flag de omisión (SC-07/SC-11).
- [x] 5.2 Integrar cálculo/insert y complete dentro de una única T3 de TransactionalScanLifecycle, con lock Scan primero y guards completos; preparar fallo tras insert/flush y antes de transición/commit para verificar rollback conjunto y ninguna fila de score visible con Scan activo (SC-07/SC-08).
- [x] 5.3 Preparar integración de flujo nuevo con cuatro N, cuatro I y mezcla vulnerable/no vulnerable/inconclusa; verificar COMPLETED con score requerido, Findings conservados, fallos LLM consolidados permitidos y ausencia de cambios de estados/siete campos/300s/cuotas existentes (SC-01/SC-05/SC-07).
- [x] 5.4 Integrar errores de scoring con compensación SCAN_PROCESSING_FAILED y compuerta existentes; preparar error de cálculo, evaluación ausente, Finding incoherente, escritura fallida y compensación indisponible, verificando conservación de terminales, ningún I fabricado y bloqueo solo conforme a la política ya aprobada (SC-08).
- [x] 5.5 Preparar dos cierres concurrentes, entrega duplicada y commit ambiguo con ramas confirmada/no confirmada; verificar una fila por Scan+versión, calculatedAt inmutable, terminal no reabierto, no actualización de fila existente, sin continuar una transacción SQL abortada y sin llamadas externas repetidas (SC-08/SC-09).
- [x] 5.6 Preparar un candidato interno duplicado con valores derivados divergentes o un score durable con Scan activo/FAILED; verificar diagnóstico de integridad/500 sanitizado y conservación sin overwrite, borrado, backfill o completado automático. La comprobación compara resumen/breakdown, no usa hash ni afirma auditar cualquier alteración externa de fuentes (SC-09/SC-10/SC-12).

## 6. Recuperación y consulta API

- [x] 6.1 Preparar recuperación local en las fronteras de D-13: tras Findings, durante cálculo, después de insert/flush no confirmado, después de score+cierre confirmados y commit ambiguo; verificar FAILED/SCAN_INTERRUPTED sin score en activos, conservación de terminales/Findings, sin recálculo ni red y compuerta cerrada si falla recuperación (SC-10).
- [x] 6.2 Crear GetScanScoreUseCase y proyección coherente de consulta con snapshot/lectura conjunta de marcador, estado y score; preparar carrera con T3 para verificar que se observa PENDING o AVAILABLE válido, nunca falsa corrupción por dos lecturas de distinta instantánea (SC-12, D-15).
- [x] 6.3 Exponer solo GET /api/projects/{projectId}/scans/{scanId}/score con DTO cerrado {availability,score}; preparar AVAILABLE con ScanScore existente0/100, PENDING/null en PENDING/RUNNING, UNAVAILABLE/null en FAILED nuevo incluso antes del plan y NOT_APPLICABLE/null en históricos no-op/técnicos/evaluados también FAILED, incluido global_score no nulo; verificar que los dos últimos siempre tienen score=null, sin fallback/duplicación en Scan (SC-02/SC-12/SC-13).
- [x] 6.4 Preparar API de UUID inválidos con precedencia, Project/Scan inexistentes o ajenos, fallo de lectura y COMPLETED marcado sin score; verificar 400/404/500, mensajes/códigos existentes, no-store, shape/fechas/enum/version y ningún secreto/evidencia/valor rechazado (SC-12).
- [x] 6.5 Preparar regresión de los siete campos públicos de Scan y GET executions/evaluations/findings, consulta sin target/clave/evaluador, cero escrituras/recálculos y no ocultación de Findings de scans FAILED; verificar independencia de contratos previos y correspondencia con OpenAPI (SC-02/SC-07/SC-12/SC-13).

## 7. Verificación transversal y cierre futuro

- [x] 7.1 Revisar trazabilidad de todos los escenarios SC-01..SC-13 a los casos preparados, incluida separación entre unidades matemáticas de veinte Findings y plan real de cuatro; verificar invariantes, fronteras numéricas, versionado/históricos/ausencia, reproducibilidad sin hash y categorías sin lifecycle propio; contrastar proposal↔spec↔design↔tasks sin ampliar MVP.
- [x] 7.2 Ejecutar exclusivamente las comprobaciones que el usuario autorice posteriormente y registrar comandos/resultados reales con versión de fuentes; PostgreSQL/Flyway solo en fixture dedicada autorizada. Verificar que strict no se presenta como test backend ni validación empírica de scoring-v1; si no se ejecutan tests, indicarlos pendientes.
- [x] 7.3 Revisar coherencia final entre implementación autorizada, OpenSpec y documentación directamente afectada; verificar que no cambia evaluación/experimento, no introduce frontend/reporting/dependencias y no marca requisitos TFM completados sin evidencia. No aprobar/archivar/iniciar otro change automáticamente.

## Evidencia de implementación (2026-10-10)

Fuentes: árbol de trabajo sobre `0538f4d`, sin commit. Java/javac Temurin 21.0.12.1; PostgreSQL 16.15. Credenciales cargadas únicamente en el entorno desde `.env`, sin mostrarlas. URL e identidad de base verificadas como `llm_scanner_seguridad_test`; las fixtures de integración usan esquemas aleatorios dedicados. Maven se ejecuta offline (`-o`), sin instalar ni cambiar dependencias y sin OpenAI real.

La suite dirigida final `mvnw.cmd -o "-Dtest=Scoring*Test,CalculateScanScoreUseCaseTest,ScoreControllerTest,SecurityTestExecutionPostgresIntegrationTest" test` pasa: 67 tests, 0 fallos, 0 errores, 0 omitidos. Incluye 60 tests propios de scoring y 7 de regresión del worker. Evidencia local: `backend/target/scoring-validation.log` y los informes Surefire correspondientes. Los fallos iniciales de sintaxis de V6 y dobles/aserciones de tests se corrigieron antes de esta ejecución.

La primera regresión amplia detectó una fixture histórica del worker que fabricaba COMPLETED con el nuevo marcador y sin score. Se corrigió únicamente su SQL de preparación, fijando scoring_version=null para representar el histórico que ya describía su comentario. Los tests de scoring siguen rechazando ese mismo estado si el marcador está presente. No se relajó ninguna invariante ni se modificó Evaluation Engine.

### Trazabilidad SC-01..SC-13

| Requisito / tareas | Evidencia ejecutable |
| --- | --- |
| SC-01; 3.1..3.3 | CalculateScanScoreUseCaseTest: plan completo, ausencias/extras/duplicados, estados no COMPLETED, versión/revisión no soportada, Scan ajeno y vínculos/categoría/severity/confidence/fechas incoherentes. SQL de ScoringInputPort lee todas las filas sin evidencia ni DISTINCT. |
| SC-02/SC-03/SC-04; 2.1..2.4 | ScoringPolicyV1Test: bases, bandas/fronteras, HALF_UP decimal, confidence 0..1, ejemplos exactos, floors/caps, empate/permutación/monotonía y overflow. Veinte LOW solo en función matemática; el plan productivo sigue siendo cuatro. |
| SC-05/SC-06; 2.5/2.6 | CalculateScanScoreUseCaseTest y ScoringEnginePostgresIntegrationTest: cuatro N frente a cuatro I, mezcla V/N/I, global76 frente a categorías75/15/0/0, contadores y orden. CategoryScore/ScanScore rechazan construcción parcial. |
| SC-07; 5.1..5.3 | ScoringEnginePostgresIntegrationTest: marcador durable y rollback T1, worker real sin proveedor, cierre atómico con cuatro terminales, fallo tras insert y antes de transición, reloj futuro sin regresión de finishedAt. |
| SC-08; 5.4/5.5 | ScoringLifecycleTest y ScoringEnginePostgresIntegrationTest: fallo de fuente/cálculo/escritura/cierre, rollback, compensación técnica, compuerta cerrada si también falla persistencia y commit ambiguo confirmado/no confirmado. Findings previos conservados. |
| SC-09; 3.4/4.3/5.5/5.6 | Dos cierres simultáneos con lock PostgreSQL y PK, una fila, candidato igual reutilizado conservando calculatedAt y divergente rechazado. Reconstrucción por nueva instancia y orden invertido; consulta/reinicio conservan resultado y fuentes. |
| SC-10; 6.1 | Reinicio tras Findings, fallo durante cálculo e insert no confirmado: activos interrumpidos sin score y con fuentes conservadas. Tras commit/commit ambiguo confirmado: score+COMPLETED intactos. Corrupción durable bloquea recuperación y produce error sanitizado; sin reparación. |
| SC-11; 4.1..4.5 | ScoringMigrationIntegrationTest: V1→V6, V5→V6 con Findings N/I/V y global_score histórico, preservación de fuentes/fechas/relaciones, marcador null y tabla vacía sin backfill. Constraints de PK/FK, bandas, contadores, JSON cerrado, nulls/fracciones/tipos/orden/identidades y dependencia explícita del perfil. `git diff` confirma V1–V5 intactas. |
| SC-12; 6.2..6.5 | ScoreControllerTest, ScoringOpenApiContractTest y ScoringEnginePostgresIntegrationTest: cuatro availabilities, cero/cien, UUID/precedencia, scoping, 400/404/500/no-store, exactitud de campos y UTC. Polling durante insert no confirmado observa PENDING y después AVAILABLE, nunca falso error por instantáneas distintas. Scan conserva siete campos y las consultas anteriores mantienen su contrato. |
| SC-13; 3.4/7.1/7.3 | Campos exactos/versiones/fechas/contadores, reconstrucción sin hash, entradas estructuradas mínimas, fuentes inmutables, ausencia de cálculo en GET y de nuevas dependencias/LLM/evaluación/experimento. Fórmula y limitaciones documentadas en ADR-005, estrategia y OpenAPI. |

### Archivos creados

Rutas relativas al repositorio; no se incluyen artefactos ignorados de `backend/target/`.

- `backend/src/main/java/com/llmscannerseguridad/domain/scoring/`: `RiskLevel.java`, `SeverityCounts.java`, `CategoryScore.java`, `ScanScore.java`, `ScoringPolicyV1.java`.
- `backend/src/main/java/com/llmscannerseguridad/application/scoring/`: `ScoringInputPort.java`, `ScanScoreSnapshot.java`, `ScanScoreRepositoryPort.java`, `ScoreAvailability.java`, `ScoreListing.java`, `GetScanScoreUseCase.java`, `CalculateScanScoreUseCase.java`.
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/scoring/`: `ScoringJson.java`, `PostgresScoringInputAdapter.java`, `PostgresScanScoreRepositoryAdapter.java`.
- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/ScoreResponse.java` y `ScoreController.java`.
- `backend/src/main/java/com/llmscannerseguridad/config/ScoringConfiguration.java`.
- `backend/src/main/resources/db/migration/V6__incorporar_motor_scoring.sql`.
- `backend/src/test/java/com/llmscannerseguridad/domain/scoring/ScoringPolicyV1Test.java`.
- `backend/src/test/java/com/llmscannerseguridad/application/scoring/ScoringFixtures.java` y `CalculateScanScoreUseCaseTest.java`.
- `backend/src/test/java/com/llmscannerseguridad/adapters/in/rest/scan/ScoreControllerTest.java` y `ScoringOpenApiContractTest.java`.
- `backend/src/test/java/com/llmscannerseguridad/config/ScoringLifecycleTest.java`.
- `backend/src/test/java/com/llmscannerseguridad/integration/ScoringDatabaseFixture.java`, `ScoringEnginePostgresIntegrationTest.java` y `ScoringMigrationIntegrationTest.java`.

### Archivos modificados

- `backend/src/main/java/com/llmscannerseguridad/config/TransactionalScanLifecycle.java` y `ScanConfiguration.java`.
- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/EvaluationExceptionHandler.java`: reutilización exclusiva del manejo sanitizado/no-store para el nuevo controlador.
- `backend/src/test/java/com/llmscannerseguridad/integration/SecurityTestExecutionPostgresIntegrationTest.java`: marcador null en una fixture que simula un histórico.
- `docs/02-arquitectura/arquitectura-general.md`.
- `docs/02-arquitectura/decisiones/ADR-003-ejecucion-asincrona-scans.md`, `ADR-005-severidad-confidence-scoring.md` y `decisiones-abiertas.md` (solo sección 6).
- `docs/04-ia/estrategia-evaluacion.md`, `docs/05-api/openapi.yaml` y `docs/06-base-de-datos/modelo-datos.md`.
- Este `tasks.md`. Proposal, spec, design y Approval Record permanecen intactos.

Checklist TFM revisada: el incremento no cambia el estado de ningún requisito oficial de entrega; no se modifica ni se marca completado un requisito académico. La heurística sigue sin calibración empírica; strict solo valida estructura OpenSpec. Sin frontend/reporting, nuevos SecurityTests, hash/backfill, dependencias, despliegue, commit, push, archivo del change ni inicio de otro change.

### Resultado final de validación

- `mvnw.cmd -o package`: **BUILD SUCCESS**, 365 tests, 0 fallos, 0 errores y 0 omitidos. La misma ejecución verifica la regresión amplia final y genera `backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar`. Evidencia: `backend/target/scoring-package.log` (2026-10-10, 18:11 Europe/Madrid).
- Para esa ejecución, PROJECT_TEST_DB_URL/USERNAME/PASSWORD y TARGET_TEST_DB_URL/USERNAME/PASSWORD recibieron en memoria los mismos valores SCAN_TEST_DB de `.env`, ya verificados para llm_scanner_seguridad_test; TARGET_POSTGRES_TESTS_ENABLED=true. Se ejecutaron también los 12 tests PostgreSQL de Project/Target inicialmente omitidos por sus flags, sin cambiar sus fixtures ni dependencias.
- PostgreSQL/Flyway: **PASS**, V1→V6 y V5→V6 conservadora, incluida fixture V5 con Findings legítimos y score heredado no nulo. Las fixtures eliminan únicamente sus esquemas aleatorios de la base dedicada autorizada. La prueba contextLoads usa también el datasource de test explícito, sin fallback a la base habitual.
- `openspec validate scoring-engine --strict`: **PASS**. Solo estructura OpenSpec, sin afirmar calibración empírica.
- `git diff --check`: **PASS**; V1–V5, pom, configuración de despliegue y artefactos de otros changes sin cambios.
- Estado de implementación: **32/32 tareas completadas, 0 pendientes; READY FOR REVIEW**. No se aprueba ni archiva el change automáticamente.
