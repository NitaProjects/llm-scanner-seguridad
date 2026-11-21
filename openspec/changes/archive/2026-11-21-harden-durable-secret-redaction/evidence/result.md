# Resultado de harden-durable-secret-redaction

Fecha: 2026-11-21. Estado: **READY FOR REVIEW**, con validación PostgreSQL real posterior a la corrección del terminal CHAT de la fixture. No se ha archivado el change.

## Implementación

`ProtectedContentInspector` es la única definición SAFE/MATCH/UNINSPECTABLE utilizada por redacción durable y veto de local-only/exportación. N4 cubre escapes JSON estándar/Unicode, hex de ambos casos, escapes parciales y strings anidados/repetidos. Cuatro capas, profundidad JSON 32, 512 valores protegidos, 4096 nodos/candidatos/intervalos y vistas acumuladas 5 × B. La inspección incompleta prevalece sobre un MATCH temprano. No se acepta un prefijo como sustituto de un valor completo.

Los intervalos literales se buscan sobre el original, se fusionan y se sustituyen una vez. MATCH únicamente decodificado sustituye el campo completo; colisión del marcador, JSON ambiguo, agotamiento, deadline o interrupción suprimen evidencia no certificable. Se conservan las prioridades técnicas de deadline/interrupción. Los wrappers propios de JSON no consumen una capa atacante.

`ProtectionScope` conserva efímeramente originales conocidos de credencial, sesión, cookies rotadas, CSRF, retrieval/tool, IDs y aliases, antes de descartar o sanear; se libera al cerrar/fallar/interrumpir. Sus cotas son conservadoras: no se conserva un universo parcial marcado como completo. No se persiste el registro ni hashes de secretos.

`DurableEvidenceProtectionPort` actúa antes de `executions.finish`; el repositorio vuelve a verificar antes de replace/binding. Los catches del transporte mantienen el candidato original solo en memoria y lo revalidan con los valores capturados posteriormente. No añaden requests ni reintentos. Prepare/claim/finish de evaluación verifican candidatos finales; Finding deriva de esa evaluación segura. Campo obligatorio no certificable aborta escritura. La proyección opcional insegura queda ausente junto con su hash. Score e informe se verifican antes del cierre conjunto. La compensación escribe metadata segura con componentes de confidence válidos para relectura.

Sin API/UI, schema/migración, dependencias, despliegue, AWS, proveedor real, commit ni push. `credential_ciphertext` conserva el almacenamiento cifrado autorizado. Garantía prospectiva; no limpieza de históricos/backups/WAL ni detección de secretos desconocidos.

## Validaciones offline

- Dirigidas: `HttpTargetFailurePrecedenceTest,ProtectedContentInspectorTest,DurableEvidenceProtectionTest`: **80 ejecutadas, 80 aprobadas, 0 fallos, 0 errores, 0 omitidas**.
- Suite completa: `.\mvnw.cmd -o test`: **852 contabilizadas, 748 aprobadas, 0 fallos, 0 errores, 104 omitidas; BUILD SUCCESS**, 1 min 41 s, finalizado a las 14:01:03 +01:00; corrida final tras conservar UTF-8. Desglose exacto por clase en `offline-test-summary.json`.
- Las 104 omitidas no cuentan como PASS. Desglose en `result.json`; incluyen PostgreSQL y contexto condicionado por entorno. El ejecutor no hereda SCAN_TEST_DB_*.
- Regresiones offline: REST_LEGACY, OPENAI_COMPATIBLE, SSE/JSONL, sesiones, workflows, cookies/CSRF, observaciones, hybrid-v1/v2, scoring-v1/report-v1. La prueba diferencial de restricción protegida exige el mismo veto del inspector y del proyector.
- Se actualizaron fixtures incompatibles con el endurecimiento aprobado: escapes ambiguos ya no son evidencia exportable, cancelación no conserva cuerpo sin certificar y el test de precedencia utiliza la firma privada nueva sin relajar sus expectativas.
- Logs locales no versionados: `backend/target/dsp-final-directed.log`, `backend/target/dsp-backend-final.log`. No se copian bodies ni credenciales a esta evidencia.

## PostgreSQL 16: PASS

La primera corrida del operador obtuvo 5 tests, 0 fallos, 1 error y 0 omitidas: `InvalidTargetRequest` por el `secretValue` fuera de `authConfig`, ya corregido. La segunda corrida, finalizada a las 14:02:46 +01:00, obtuvo **5 tests, 1 fallo, 0 errores y 0 omitidas**: REST terminó correctamente y CHAT terminó FAILED; Workflow pasó 1/1. SSE/JSONL estaban después de CHAT en el bucle, por lo que esa corrida no llegó a acreditarlos.

### Diagnóstico del fallo CHAT

**FIXTURE BUG**, no defecto demostrado de N4. La respuesta CHAT emitía solo `choices[0].message.content`, sin `finish_reason`. `OpenAiCompatibleCodec.extract` exige `finish_reason=stop` y devuelve `RESPONSE_FORMAT_ERROR` si falta, antes de devolver el texto al saneamiento. Esto coincide con el contrato aprobado openai-compatible-target (y docs/05-api/target-contract.md): HTTP 200 no certifica una respuesta completa. REST utiliza responsePath y no requiere el terminal Chat Completions.

La regresión `DurableChatFixtureTest.originalChatFixtureFailsProtocolBeforePrivacyInspectionForEveryEncoding` reproduce exactamente la forma anterior con las cuatro respuestas sintéticas (Unicode, encode^1, encode^2, encode^4) y acredita `RESPONSE_FORMAT_ERROR`. `explicitStopPreservesAllN4PayloadsAndAllowsDurableRedaction` añade únicamente el terminal válido, comprueba extracción idéntica, MATCH del inspector para el valor sintético protegido y redacción SAFE sin error. El valor protegido no es la causa del rechazo original: N4 aún no recibía el texto extraído. No se cambian las capas ni el contenido para obtener verde.

No se han recuperado las filas de la corrida anterior: ScanTestDatabase elimina el schema de fixture al cerrar. El resumen recibido acredita FAILED, pero no incluye scanId, error del Scan ni IDs/errores persistidos de cada TestExecution. No se inventan esos datos. El camino de código transforma el error de extracción en fallo técnico y no entrega respuesta parcial; ProcessScanUseCase compensa con SCAN_PROCESSING_FAILED. La aserción PostgreSQL ahora añade solo metadata tipada del Scan y de las ejecuciones (ID interno, status, error, HTTP y flags), nunca payloads, para diagnosticar cualquier fallo posterior.

Corrección mínima: añadir `finish_reason: "stop"` a la respuesta CHAT de la fixture, conservar todas las codificaciones y la expectativa COMPLETED. Ningún archivo productivo se modificó en esta corrección. Pruebas dirigidas `DurableChatFixtureTest,OpenAiCompatibleTargetAdapterTest`: **32 ejecutadas/aprobadas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, 7.482 s, 14:07:13 +01:00. Log local no versionado: `backend/target/dsp-chat-diagnosis.log`.

Se repitió el comando PostgreSQL exacto en el ejecutor: **5 contabilizadas, 0 fallos, 0 errores, 5 omitidas**, por ausencia de SCAN_TEST_DB_*. Su BUILD SUCCESS **no es aceptación PostgreSQL**; se conserva únicamente como antecedente del entorno del ejecutor.

**Resultado final aportado por el operador:** 5 tests ejecutados/aprobados, 0 fallos, 0 errores, 0 omitidos; **BUILD SUCCESS**, 57.453 s, finalizado el **2026-11-21 a las 14:10:01 +01:00**. Corresponde a la corrida posterior al ajuste `finish_reason=stop`, no a los intentos previos. `DurableSecretPostgresIntegrationTest` aporta cuatro casos y `WorkflowPostgresIntegrationTest` uno (16.50 s). Procedencia: resumen Maven de la terminal del operador con PostgreSQL dedicado; el agente no leyó credenciales.

Comando confirmado, desde backend y exclusivamente en la base dedicada:

```powershell
.\mvnw.cmd -o '-Dtest=DurableSecretPostgresIntegrationTest,WorkflowPostgresIntegrationTest' test
```

La suite ejecutada exige PostgreSQL 16 y la URL dedicada localhost:5432/llm_scanner_seguridad_test. Incluye transporte HTTP local REST/chat/SSE/JSONL, workflow/cookies/CSRF/RAG/tool, valores locales e IDs descartados/solapados y sus copias finales. Comprueba cuatro ejecuciones/evaluaciones, score, informe y ausencia durable con conexión JDBC independiente postcommit y tras cerrar/reabrir contexto. `IndependentPrivacyOracle` usa traversal/decoding propio de fixture, no importa ni llama al inspector de producción ni se limita a LIKE; inspecciona columnas mediante to_jsonb de las nueve tablas, excluyendo únicamente ciphertext autorizado.

Las escrituras secundarias obligatorias no certificables se rechazan; triggers sintéticos provocan fallo de Finding y de cierre score/report para verificar rollback y aislamiento de un Scan anterior. Se comprueba también la relectura de evaluaciones FAILED con confidence válida. Criterio satisfecho: **5 ejecutadas, 0 fallos, 0 errores, 0 omitidas** en la nueva corrida. La aceptación abarca auditoría JDBC independiente postcommit/reapertura, rollback secundario y cierre del pipeline con cuatro ejecuciones/evaluaciones, scoring-v1 y report-v1, sin copias recuperables de los valores protegidos de fixture. No amplía la garantía fuera de N4 ni a secretos desconocidos.

## Matriz T01–T28

| ID | Caso | Cobertura |
| --- | --- | --- |
| T01 | Literal | `ProtectedContentInspectorTest.t01ToT08FindsRuntimeN4Equivalents` |
| T02 | JSON escapes estándar | `ProtectedContentInspectorTest.t02AllStandardJsonEscapesAndMixedHexAreInspected` |
| T03 | Primer carácter Unicode | `ProtectedContentInspectorTest.t01ToT08FindsRuntimeN4Equivalents` |
| T04 | Unicode múltiple | `ProtectedContentInspectorTest.t01ToT08FindsRuntimeN4Equivalents` |
| T05 | Hex minúscula/mayúscula | `ProtectedContentInspectorTest.t02AllStandardJsonEscapesAndMixedHexAreInspected` |
| T06 | JSON anidado | `ProtectedContentInspectorTest.t01ToT08FindsRuntimeN4Equivalents` |
| T07 | Doble encoding | `ProtectedContentInspectorTest.t01ToT08FindsRuntimeN4Equivalents` |
| T08 | Cuatro capas | `ProtectedContentInspectorTest.t01ToT08FindsRuntimeN4Equivalents` |
| T09 | Quinta capa/falta de punto fijo | `ProtectedContentInspectorTest.t09DepthFiveAndFreeTextExhaustionAreNotSafeEvenWithoutFindingSecret` |
| T10 | Malformed fail-closed | `ProtectedContentInspectorTest.t10MalformedEscapesAndOtherBranchesPrecedeEarlyMatch` |
| T11 | Overlap | `ProtectedContentInspectorTest.t11T12OriginalIntervalsOverlapAndOrderAreEquivalent` |
| T12 | Prefix/suffix overlap | `ProtectedContentInspectorTest.t11T12OriginalIntervalsOverlapAndOrderAreEquivalent` |
| T13 | Retrieval local-only | `DurableEvidenceProtectionTest.t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields` |
| T14 | Tool argument | `DurableEvidenceProtectionTest.t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields` |
| T15 | Tool result | `DurableEvidenceProtectionTest.t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields` |
| T16 | Session variable | `DurableEvidenceProtectionTest.t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields` |
| T17 | CSRF | `DurableEvidenceProtectionTest.t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields` |
| T18 | Cookie/rotación | `DurableEvidenceProtectionTest.t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields` |
| T19 | JSON response | `DurableEvidenceProtectionTest.t19ToT24PreservesBenignBodiesAndSanitizesLateCapturesOnError; HttpTargetInvocationAdapterTest` |
| T20 | SSE reconstructed | `StreamingTargetTest (incluye StreamCollector)` |
| T21 | JSONL reconstructed | `StreamingTargetTest (incluye StreamCollector)` |
| T22 | Request | `DurableEvidenceProtectionTest.t13ToT18ProtectsEveryClassifiedOriginalAndCopiesInBothFields` |
| T23 | Response | `DurableEvidenceProtectionTest.uninspectableScopeAndDepthSuppressBodiesAndKeepOnlySafeMetadata` |
| T24 | Error/safeRequest/precedencia | `DurableEvidenceProtectionTest.t19ToT24PreservesBenignBodiesAndSanitizesLateCapturesOnError; HttpTargetFailurePrecedenceTest` |
| T25 | Secondary persistence | `DurableEvidenceProtectionTest.t25SameSanitizedEvaluationFeedsFindingWhileRequiredSnapshotCannotBeRepaired` |
| T26 | Semantic consistency | `HybridV2EvaluationTest.durableScopeAndSemanticExportShareN4ForProtectedRestrictionOnly` |
| T27 | Scoring/report | `ScoringPolicyV1Test; CalculateScanScoreUseCaseTest; GenerateReportUseCaseTest; SecurityReportTest` |
| T28 | Historical | `Regression suites; WorkflowPostgresIntegrationTest` |

La cobertura unitaria anterior se complementa con la auditoría física aprobada de las fixtures PostgreSQL descritas arriba. Se conservan controles benignos, surrogate/NUL, claves duplicadas, números en key/value, límites exactos y +1, orden de secretos/overlap, marcador, aislamiento concurrente y cleanup.

## Tasks y OpenSpec

**23/23 tareas completas**, con 5.1–5.3 y 6.1 respaldadas ahora por la corrida PostgreSQL final. Las 104 omitidas de la suite offline siguen registradas como omitidas y no se convierten en aprobadas por esta corrida específica; las dos pruebas de diagnóstico CHAT se registran por separado, no como una nueva suite completa. OpenSpec strict: **PASS**, `openspec validate harden-durable-secret-redaction --strict` (2026-11-21). **READY FOR REVIEW**. Sin desviaciones pendientes identificadas dentro del alcance aprobado. No se declara archivo ni revisión externa realizada.
