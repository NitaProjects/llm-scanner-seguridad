## 1. Contratos aprobados y documentación afectada

- [x] 1.1 Al iniciar la implementación solicitada posteriormente, contrastar sus contratos con Approval Record y la trazabilidad requisito→escenario→diseño→tarea; verificar las concreciones ya aprobadas sin volver a pedir aprobación genérica ni tratar strict como implementación terminada (EE-01..EE-14).
- [x] 1.2 Preparar fixtures locales del contrato Responses API/Structured Outputs aprobado y una lista de comprobaciones operativas de cuenta, retención y disponibilidad para una futura operación real autorizada; verificar que no se afirma disponibilidad/snapshot/tarifa no comprobados ni se sustituye OpenAI/gpt-6-luna silenciosamente. Estas comprobaciones externas no bloquean implementar con fake; tarifa solo obligatoria si se activa el control monetario opcional (EE-05/EE-06/EE-09).
- [x] 1.3 Actualizar OpenAPI con los dos GET y DTOs exactos D-13, errores, no-store, semántica del perfil nuevo e histórico; comprobar por revisión que Scan sigue teniendo siete campos, GET executions conserva su contrato y no se publica evidencia ni endpoints extra (EE-10/EE-13).
- [x] 1.4 Actualizar únicamente arquitectura/ADR-003..006, modelo de datos y documentos de seguridad/IA directamente afectados con decisiones efectivamente aprobadas y referencias al change; verificar alineación y límites OUTPUT_ONLY/sintético, sin marcar requisitos de entrega TFM completados por diseño (EE-01..EE-14).

## 2. Dominio, criterios y contexto

- [x] 2.1 Definir EvaluationResult, estados técnicos, tres verdicts, llmStatus/llmErrorReason separados de status/errorReason global, motivos cerrados, RuleSignal, LlmAssessment y Finding independientes de infraestructura; preparar pruebas de invariantes, terminales inmutables, UTC/microsegundos, enums, confidence decimal/rangos y severity nullable, revisando imports sin Spring/JPA/HTTP/proveedor (EE-01/EE-04/EE-07/EE-08).
- [x] 2.2 Implementar catálogo separado de cuatro criterios v1 ligado a code/revision persistidos, con objetivos, precondiciones, reglas tipadas y plantillas sin duplicar prompts; verificar pruebas de catálogo completo, criterio ausente, revisión desconocida e inmutabilidad de SecurityTest/V4 (EE-01/EE-02).
- [x] 2.3 Implementar lectura local acotada del manifiesto sintético D-02 y snapshot único por Scan; preparar casos de contexto ausente/desactualizado/ajeno, fichero malformado, Unicode/duplicate keys/propiedades desconocidas/caps, cambio concurrente de fichero y ausencia de URL/credenciales/ground truth, comprobando binding project/target/timestamp/test (EE-01/EE-06).
- [x] 2.4 Definir puertos EvaluatorPort, EvaluationRepositoryPort, EvaluationCriteriaCatalogPort y EvaluationScenarioPort con DTOs propios y presupuestos; revisar firmas, toString y contrato de resultado/fallo para que no propaguen entidades, secretos, Throwable o tipos del cliente, ni devuelvan el verdict híbrido como voto LLM (EE-05/EE-08).

## 3. Señales y consolidación pura

- [x] 3.1 Implementar KNOWN_SYNTHETIC_VALUE y KNOWN_HIDDEN_CONTEXT con coincidencia exacta/reservada y referencias por puntos de código; preparar casos fuerte positivo, negativo que reproduce el canario, valor público/en prompt, ausencia de contexto, Unicode y no ejecución/no uso de secreto operativo (EE-02).
- [x] 3.2 Implementar EXPECTED_MARKER, EXPECTED_SAFE_RESPONSE y OUTPUT_CONTRACT con parámetros cerrados; preparar casos marcador citado/permitido, negativa segura exacta, negativa genérica, texto vacío, NO_MATCH/NOT_APPLICABLE, HTML inerte LLM10 permitido y contrato de seguridad conocido infringido sin inferir sink (EE-02/EE-03).
- [x] 3.3 Implementar matriz HYB-01..07 y validación semántica/anclas D-04; preparar tabla de decisiones vulnerable/segura/ambigua, fuerte vs LLM contrario, conflicto objetivo, weak vs LLM, LLM-only sin soporte, citas falsas y paráfrasis controlada sustentada, conservando decisionCode/discrepancias (EE-03).
- [x] 3.4 Implementar confidence-v1 exactamente según bases/buckets/penalizaciones/cap/redondeo de D-05; preparar pruebas de 0.95/0.70/0.65/0.85, límites 0.50/0.80, cap, redacción relevante vs irrelevante y reglas correlacionadas sin doble suma, independiente de severity (EE-04).
- [x] 3.5 Implementar severity-v1 y plantillas de explicación/evidencia/recomendación; preparar casos LOW/MEDIUM por impacto declarado y HIGH con señal fuerte/condición explícita por categoría, sin techo global MEDIUM; verificar CRITICAL reservado aunque el LLM lo sugiera, category/contexto no inventados, null para N/I, límite OUTPUT_ONLY y ausencia de cambios de severity por confidence (EE-04/EE-08).

## 4. V5 y persistencia transaccional

- [x] 4.1 Crear V5 descrita en D-12, con tabla evaluation_results, perfil nullable, evolución de findings, FKs compuestas, UNIQUE y checks explícitos de NULL/ciclo/rangos; verificar revisión SQL y conservación byte a byte V1–V4/test_cases/evidencia técnica/global_score, sin ejecutar migración (EE-12).
- [x] 4.2 Preparar fixtures PostgreSQL 16 dedicadas sobre V4 con scans/ejecuciones existentes: éxito V5 sin inventar resultados, guard con findings/evaluator_result/confidence heredados, rollback íntegro, relaciones y fechas conservadas; verificar mediante pruebas previstas que no se exige vaciar datos técnicos ni se altera una base compartida (EE-12).
- [x] 4.3 Implementar adaptador de resultados y proyecciones scoped, persistencia segura JSONB/TEXT y snapshot/hash canónico versionado; preparar round-trip Unicode/NUL escapado, nulls/metadata desconocida, cap adicional 64 KiB, rechazo de JSON/snapshot excesivo antes de enviar y ausencia de escrituras en evaluator_result/confidence heredadas (EE-08/EE-09/EE-12).
- [x] 4.4 Implementar preparación atómica de cuatro ejecuciones/evaluaciones y claim condicional PENDING→RUNNING con padre COMPLETED, reserva y attempt 0/1; preparar dos reclamaciones concurrentes, commit ambiguo, rollback de plan/reserva y comprobación desde conexión independiente de commit previo a red (EE-01/EE-10/EE-11).
- [x] 4.5 Implementar terminalización EvaluationResult+Finding en una transacción nueva corta; preparar fallo insert/flush/commit, lectura concurrente, UNIQUE de ejecución/resultado y FK que rechaza scan/test/category/verdict/severity/confidence incompatibles. Verificar fallback fuerte COMPLETED/VULNERABLE+Finding con llmStatus FAILED/errorReason null y fallo global FAILED/INCONCLUSIVE sin Finding, sin estado parcialmente visible (EE-07/EE-08/EE-11/EE-12).

## 5. Input, prompt y seguridad del evaluador

- [x] 5.1 Implementar proyección mínima desde responseText ya decodificado y prompt de catálogo, permiso externo específico/corpus sintético completo; preparar ausencia de autorización, texto desconocido/sensible, URL/headers/auth/ciphertext excluidos, falta de contexto, redacción que afecta criterio y límites exactos/excedidos sin truncamiento ni copia bruta (EE-05/EE-06/EE-09).
- [x] 5.2 Versionar prompt/schema/serializador v1 y construir mensajes separados mediante JSON, con objetivo/criterios/referencias y límites LLM10; preparar fixtures de roles falsos, escapes, instrucciones incrustadas/URLs/exfiltración, verificando que no alteran sistema/contexto/configuración ni habilitan tools/memoria (EE-05/EE-06).
- [x] 5.3 Implementar parser estricto de output D-08; preparar un documento JSON válido y casos markdown, duplicados, trailing data, campo ausente/desconocido, enums/nulabilidad incorrectos, confidence no finito/string/fuera de rango, offsets/citas/criterios inventados, exceso y respuesta incompleta/refusal, sin aceptar texto libre (EE-05).
- [x] 5.4 Implementar filtrado del resumen breve para usuario/auditor y fallback de textos públicos a plantillas inertes; preparar tests con secretos/cadena de pensamiento sintéticos en cuerpo/error/recomendación, verificando su ausencia en persistencia, logs, toString y DTOs. Verificar resúmenes seguros en evaluations, recomendaciones de Finding controladas y ningún HTML/markdown/comando activo (EE-06/EE-08/EE-13).

## 6. Adaptador único, cuota y coste

- [x] 6.1 Implementar el único adaptador OpenAI Responses API con Structured Outputs/JSON Schema strict, EVALUATOR_PROVIDER/EVALUATOR_MODEL externos e inicial gpt-6-luna, cliente/JSON ya existentes y API key solo en transporte; verificar max_output_tokens/store=false, contrato contra fixture local/fake, TLS/hostname/destino fijo, redirects/retries/proxies/tools deshabilitados y metadata solicitada/resuelta sin model ID en dominio, nuevo SDK/dependencia ni llamadas reales automatizadas (EE-05/EE-06).
- [x] 6.2 Implementar límites bytes/tokens/timeout y compuerta global LLM D-15, con deadline compartido de Scan y sin transacción abierta durante red/espera; preparar relojes/fakes deterministas para límite exacto/excedido, truncación, DNS/conexión/lentitud, cancelación, respuesta tardía, dos scans con concurrencia uno e intervalo mínimo (EE-09/EE-11).
- [x] 6.3 Implementar reserva atómica de intento/cupo y control opcional de coste estimado D-15, deshabilitado por defecto y ligado a provider/model/pricingVersion/estimateVersion; preparar casos sin tarifa con control deshabilitado, control activo con estimación ausente/incompatible o agotada y reservas ambiguas conservadas. Verificar hasta cuatro intentos, importes desconocidos null, estimación distinta de factura, coste target separado y consolidación coherente sin importe funcional fijo (EE-07/EE-08/EE-09).
- [x] 6.4 Implementar traducción cerrada de timeout, 429, 401/403, 4xx, 5xx, conexión, salida inválida/incompleta y agotamiento a llmStatus/llmErrorReason D-09, sin cuerpo/causa sensibles; preparar cada error, supresión restante por Scan, continuidad individual y cierre COMPLETED con fuerte VULNERABLE o INCONCLUSIVE sin fuerte. Distinguir fallo global/deadline que sí impide consolidar; verificar cero retries/reparaciones/cambios de proveedor y no Scan FAILED automático por componente (EE-07/EE-10).

## 7. Worker, finalización y recuperación

- [x] 7.1 Integrar EvaluateExecutionUseCase tras finish confirmado de cada TestExecution dentro de ExecuteSecurityTestsUseCase y fijar perfil nuevo en T1, preservando snapshot target/cuatro probes/POST; preparar orden de eventos commit→evaluación→siguiente y cierre de evaluación PENDING por TestExecution FAILED sin llamada semántica (EE-01/EE-10).
- [x] 7.2 Ampliar guard terminal/T3: cuatro TestExecutions y cuatro EvaluationResults COMPLETED; V/N/I y llmStatus FAILED/SKIPPED coherentemente consolidados permiten Scan COMPLETED. Preparar todos seguros, vulnerable con Finding, todos inconclusos, timeout/fallback/supresión 429/coste sin fallo global; verificar Scan FAILED ante evaluación global FAILED, deadline/interrupción o fallos técnicos del target según STE, conservando éxitos y códigos de Scan (EE-07/EE-10).
- [x] 7.3 Ampliar recuperación en la misma transacción de ScanStartup: cerrar eval/ejecuciones/scans activos, ausencia de fila en scan nuevo activo y huérfanos activos de scan terminal; preparar cada frontera de caída D-11, arranque repetido, commit fallido/compuerta cerrada, terminales/Findings conservados y cero llamadas/relleno semántico histórico (EE-11).

## 8. API mínima

- [x] 8.1 Implementar ListEvaluationsUseCase y GET scoped de evaluations con envelope/DTO exacto D-13; preparar estados PENDING/RUNNING/FAILED/COMPLETED, llmStatus/llmErrorReason distintos de errorReason global, resumen/recomendación seguros, perfil histórico null, plan no preparado, orden, findingId y metadata provider/model, sin cargar/publicar evidencia/target/credencial/prompts internos/chain-of-thought (EE-07/EE-13).
- [x] 8.2 Implementar ListScanFindingsUseCase y GET scoped de findings con campos/orden exactos D-13; preparar cero/un/varios Findings, Scan RUNNING/FAILED, texto inerte de plantilla, verdict derivado VULNERABLE y no GET individual/POST de reevaluación (EE-08/EE-13).
- [x] 8.3 Aplicar errores/no-store sin refactorizar handlers ajenos; preparar UUID malformados y precedencias, Project ausente, Scan ajeno, 500 de lectura sin []/404 falsos y GET sin target/clave/proveedor ni mutación de fechas, preservando GET executions y siete campos de Scan (EE-13).

## 9. Integración y evidencia de validación

- [x] 9.1 Preparar recorrido completo con fake LLM/HTTP sintético y PostgreSQL dedicado: cuatro ejecuciones y evaluaciones, Findings atómicos, metadata/versiones, redacción, límites, fallos y reinicio; verificar con conexiones independientes garantías reales de commit/rollback/constraints y ausencia de servicios/secretos reales (EE-01..EE-14).
- [x] 9.2 Revisar trazabilidad EE-01..EE-14/D-01..D-16 y preparar comandos dirigidos de tests/migración sin ejecutarlos por defecto; registrar solo resultados autorizados reales, omisiones y comprobaciones pendientes, sin atribuir validación strict/changes anteriores a esta implementación ni inventar un benchmark (EE-14).

## Trazabilidad

Los nombres de escenarios corresponden literalmente a spec.md; D-xx refiere a los identificadores originales de design.md, relacionados con la aprobación del usuario en Approval Record. Todas las tareas conservan su comprobación definida; la implementación y las comprobaciones expresamente autorizadas de esta continuación se registran al final.

| Requisito | Escenarios cubiertos | Diseño | Tareas principales |
| --- | --- | --- | --- |
| EE-01 | Contexto ausente o desactualizado; Cambio de configuración durante el trabajo; Ejecución fallida o criterio desconocido | D-02/D-03/D-10 | 2.2/2.3/4.4/7.1 |
| EE-02 | Canario oculto confirmado y falsa negativa; Marcador y HTML inerte sin incumplimiento | D-03 | 3.1/3.2 |
| EE-03 | Detección fuerte y discrepancia semántica; Opinión vulnerable sin soporte; Cumplimiento seguro y ambigüedad | D-04 | 3.3/5.3 |
| EE-04 | Confidence reproducible; Propuesta de CRITICAL; Evidencia fuerte de impacto alto | D-05/D-06 | 2.1/3.4/3.5 |
| EE-05 | Proveedor real intercambiable; JSON inválido o respuesta incompleta | D-07/D-08 | 1.2/2.4/5.1..5.3/6.1 |
| EE-06 | Evidencia intenta controlar al evaluador; Posible secreto o falta de permiso externo | D-02/D-08/D-16 | 2.3/5.1/5.2/5.4/6.1 |
| EE-07 | Timeout individual; Rate limit o proveedor caído; Fallback objetivo | D-09 | 2.1/4.5/6.4/7.2 |
| EE-08 | Finding y resultado coherentes; Metadatos incompletos del proveedor | D-01/D-12/D-14 | 2.1/4.3/4.5/5.4/6.3/8.2 |
| EE-09 | Evidencia excedida o truncada; Presupuesto agotado y concurrencia | D-15 | 4.3/5.1/6.2/6.3 |
| EE-10 | Vulnerabilidad con procesamiento correcto; Fallo LLM consolidado y fallo global; Histórico y falta de contexto | D-10/D-12 | 1.3/4.4/7.1/7.2 |
| EE-11 | Dos solicitudes y commit ambiguo; Caída en cada frontera | D-11 | 4.4/4.5/6.2/7.3 |
| EE-12 | Migración sobre datos técnicos existentes; Histórico semántico incompatible | D-12 | 4.1..4.5 |
| EE-13 | Evaluación incompleta y ausencia de Findings; Scan ajeno y fallo de lectura | D-13 | 1.3/5.4/8.1..8.3 |
| EE-14 | Repetición determinista sin red externa | D-14 | 9.1/9.2 y pruebas de todas las tareas |

1.1 comprueba el contrato aprobado; 1.4 mantiene la documentación afectada durante implementación. 9.1 integra los escenarios de todos los requisitos y 9.2 conserva evidencia real de las comprobaciones autorizadas. Quedan trazados los 14 requisitos y sus 32 escenarios, incluidas las concreciones de HIGH y de fallo LLM consolidable.

## Review Status

Implementación expresamente autorizada por el usuario en esta continuación, incluidas validaciones Java 21, PostgreSQL de test, migraciones de test, package y OpenSpec. Las 34 tareas están implementadas y contrastadas con las pruebas/evidencias indicadas abajo. Estado técnico: **READY FOR REVIEW**. No se archiva ni se inicia otro change. La aprobación de planificación y los registros históricos siguientes no sustituyen la revisión del código entregado.

## Validation Record

Fecha: 2026-10-10. Change creado mediante `openspec new change evaluation-engine`, esquema spec-driven instalado, seguido de instrucciones/estado reales para proposal, specs, design y tasks. OpenSpec 1.12.0 necesitó ejecución fuera del sandbox con aprobación porque dentro notificaba que no encontraba spec-driven aunque lo listaba como instalado; no se instalaron herramientas ni dependencias.

La validación inicial anterior a la aprobación devolvió salida 0, `Change 'evaluation-engine' is valid`; ese resultado es histórico y no sustituye la validación de los artefactos actualizados. Estado CLI consultado en esta continuación: los cuatro artefactos done e isPlanningComplete=true; describe planificación existente, no implementación terminada.

Inventario actualizado: 14 requisitos, 32 escenarios y 34 tareas sin marcar; D-01..D-16 aprobadas, sin decisiones bloqueantes. Revisión dirigida de coherencia/procedencia, sin cambios en código, V1–V4, documentos consolidados, contratos efectivos ni changes anteriores. No Maven/tests/builds/servidores/migraciones, proveedor real, Internet, commits o pushes. Scoring global e informes fuera del change.

Validación de esta actualización, 2026-10-10: `openspec validate evaluation-engine --strict`, telemetría deshabilitada, salida 0 y texto exacto `Change 'evaluation-engine' is valid`. Ejecutada fuera del sandbox con aprobación por el problema de resolución del esquema descrito arriba. La validación de artefactos no acredita implementación ni funcionamiento del proveedor. Estado final de planificación: **READY FOR IMPLEMENTATION**.

## Implementation Record — 2026-10-10

La petición posterior de implementación autoriza expresamente tests, PostgreSQL dedicado, migraciones de test, package y OpenSpec; sustituye la restricción de no ejecutar del registro histórico de planificación. Requisitos, diseño, Approval Record, V1–V4, versiones, dependencias y lockfiles no se modifican. Únicamente evaluation-engine.

### Evidencia por tareas

| Tareas | Implementación / comprobación real |
| --- | --- |
| 1.1/1.2/9.2 | Lectura de contratos EE-01..14/D-01..16 aprobados; fixtures locales Responses/Structured Outputs y comprobaciones operativas explícitas en prompts-evaluadores.md. Sin disponibilidad, tarifa, snapshot real o benchmark inventados. |
| 1.3/8.1..8.3 | EvaluationControllerTest, EvaluationOpenApiContractTest y recorrido PostgreSQL: DTOs exactos 26/15 campos, Scan siete campos, GET scoped, UUID/404/500, no-store, históricos/plan vacío, metadatos seguros. GET executions conserva su contrato. |
| 1.4 | Actualizados únicamente arquitectura/ADRs/decisiones directamente afectadas, seguridad/IA, OpenAPI y modelo de datos. Revisado checklist TFM; ningún requisito oficial se marca completado. |
| 2.1/3.1..3.5 | EvaluationPoliciesTest: invariantes/terminales/UTC, reglas exactas/canario en negativa/Unicode/marcadores citados/HTML inerte/contrato JSON tipado; HYB-01..07, anclas independientes, confianza/buckets/cap/correlación/redacción, LOW/MEDIUM/HIGH por categoría y CRITICAL nunca automático. |
| 2.2/2.3/4.3/5.1 | EvaluationScenarioTest: catálogo completo/revisión desconocida, binding/manifiesto/snapshot único, Unicode/duplicados/unknown keys/caps, exportación exacta y permiso externo, ausencia de contexto, límites exactos/excedidos, proyección NUL escapada, redacción relevante y snapshot excesivo/manipulado. |
| 2.4/5.2..5.4/6.1 | Puertos/DTOs propios sin infraestructura/entidades/secretos; EvaluatorContractTest, OpenAiTransportTest y EvaluateExecutionUseCaseTest: seis campos cerrados, referencias reales, narrativa descartada, inyección inerte, sin tools, configuración/provider/model y errores seguros, no OpenAI real. |
| 4.1/4.2 | EvaluationMigrationIntegrationTest sobre PostgreSQL 16.15: V4 técnico preservado, V5 sin backfill, guard con findings/evaluator_result/confidence y rollback, checks NULL/ciclo/rangos/FKs. V1–V4 sin diff. |
| 4.4/4.5/6.3 | EvaluationEnginePostgresIntegrationTest: ocho filas atómicas antes de red, conexión independiente, claim concurrente, reserva 0/1, rollback/commit ambiguo de claim y terminal, insert Finding fallido, relaciones compuestas y unicidad, coste opcional sin precio/con precio/cupo/reservas conservadas. Limpieza del corpus transitorio al abortar o revertir preparación. |
| 6.2/6.4/7.1/7.2 | OpenAiTransportTest/EvaluatorContractTest/EvaluateExecutionUseCaseTest y PostgreSQL: 16/24/16 KiB, espera/intervalo/concurrencia uno, timeout individual vs 300 s globales, DNS/conexión/respuesta tardía, errores cerrados/429/auth/cupo/supresión; fallback fuerte y abstención sin fuerte, continuidad y orden commit→evaluación→siguiente HTTP. |
| 7.3 | Recuperación PostgreSQL: terminal/Finding conservados, fila ausente explícitamente interrumpida, pendientes/activas/huérfanas, segundo reinicio sin mutación, cero llamadas, fallo de recuperación con rollback de evaluaciones/ejecuciones/Scan y arranque rechazado. |
| 9.1 | Recorrido con servidor HTTP sintético loopback, EvaluatorPort fake y esquemas dedicados: cuatro ejecuciones/evaluaciones, estados coherentes, Findings atómicos, metadatos/versiones, inyección/NUL, fallos y recuperación. No servicios externos ni secretos reales en fixtures. |

### Validaciones ejecutadas

Java y javac: Temurin 21.0.12.1, verificados antes de cada Maven. Maven offline, sin instalar/actualizar dependencias. Credenciales de test cargadas desde .env exclusivamente en memoria, sin mostrar sus valores. Cada ScanTestDatabase verifica URL y SELECT current_database() = llm_scanner_seguridad_test y PostgreSQL 16 antes de migrar; solo esquemas aleatorios scan_test_*, eliminados por la fixture. No se usa llm_scanner_seguridad ni se migra una base compartida.

- Regresión autorizada: `./mvnw.cmd -o '-Dtest=*,!LlmScannerSeguridadApplicationTests,!ProjectPostgresIntegrationTest,!TargetPostgresIntegrationTest' test`. BUILD SUCCESS, 278 pruebas, 0 fallos/errores/omitidas, final 2026-10-10 13:19:16 +02:00. Log: backend/target/evaluation-regression.log. Excluidos contextLoads y las dos integraciones previas con configuración de DB distinta, para no acceder a un destino no verificado. Incluye las integraciones Scan/STE y migraciones previas.
- Validación final del change y package: `./mvnw.cmd -o '-Dtest=Evaluation*Test,EvaluatorContractTest,OpenAiTransportTest,EvaluateExecutionUseCaseTest' package`. BUILD SUCCESS, 55 pruebas, 0 fallos/errores/omitidas, final 2026-10-10 13:24:32 +02:00. Log: backend/target/evaluation-package.log. Incluye los últimos cambios de preparación/limpieza, contrato OpenAPI y recuperación. No se suman como pruebas distintas de la regresión.
- JAR generado: backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar.
- Los errores de migración/commit/recuperación inducidos por las fixtures son escenarios negativos esperados, con rollback comprobado; no fallos de la suite.
- `git diff --check`: sin errores; revisión de imports de dominio/aplicación sin Spring/JPA/Jackson/HTTP/OpenAI/model ID. V1–V4/pom/dependencias sin diff.

### Comprobaciones operativas pendientes y límites

No se llama a OpenAI real ni se acredita acceso de cuenta, disponibilidad de gpt-6-luna, snapshot inmutable, retención o tarifa real. La lista previa a operación real autorizada está en docs/04-ia/prompts-evaluadores.md; no bloquea la implementación con fake aprobada. Exportación externa desactivada por defecto y corpus sintético finito obligatorio. Sin calibración estadística/experimento completo, scoring global, informes, sink downstream LLM10 o soporte distribuido. Sin commits/pushes/despliegues ni otro change.

### Inventario de archivos creados

58 archivos de código, recursos y tests:

- backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/EvaluationController.java
- backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/EvaluationExceptionHandler.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluationEvidenceProjector.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluationJson.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluatorInvocationGate.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluatorMessages.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluatorSettings.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/LocalEvaluationScenarioAdapter.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/OpenAiEvaluatorAdapter.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/ResourceEvaluationCriteriaCatalog.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/StructuredAssessmentParser.java
- backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/evaluation/PostgresEvaluationRepositoryAdapter.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluateExecutionUseCase.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationCallBudget.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationCriteriaCatalogPort.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationListing.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationMetadata.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationPlan.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationPreparationPort.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationRepositoryPort.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluationScenarioPort.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluatorInput.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluatorOutcome.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluatorPort.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/FindingMetadata.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/ListEvaluationsUseCase.java
- backend/src/main/java/com/llmscannerseguridad/application/evaluation/ListScanFindingsUseCase.java
- backend/src/main/java/com/llmscannerseguridad/config/EvaluationConfiguration.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/DeterministicRules.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/EvaluationCriteria.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/EvaluationError.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/EvaluationResult.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/EvaluationStatus.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/EvidenceReference.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/HybridDecisionPolicy.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/LlmAssessment.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/LlmError.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/LlmStatus.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/OutcomeReason.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/RuleSignal.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/Severity.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/SyntheticContext.java
- backend/src/main/java/com/llmscannerseguridad/domain/evaluation/Verdict.java
- backend/src/main/java/com/llmscannerseguridad/domain/finding/Finding.java
- backend/src/main/resources/db/migration/V5__incorporar_motor_evaluacion.sql
- backend/src/main/resources/evaluation/criteria-v1.json
- backend/src/main/resources/evaluation/evaluator-output-v1.json
- backend/src/main/resources/evaluation/evaluator-prompt-v1.txt
- backend/src/test/java/com/llmscannerseguridad/adapters/in/rest/scan/EvaluationControllerTest.java
- backend/src/test/java/com/llmscannerseguridad/adapters/in/rest/scan/EvaluationOpenApiContractTest.java
- backend/src/test/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluationFixtures.java
- backend/src/test/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluationScenarioTest.java
- backend/src/test/java/com/llmscannerseguridad/adapters/out/evaluation/EvaluatorContractTest.java
- backend/src/test/java/com/llmscannerseguridad/adapters/out/evaluation/OpenAiTransportTest.java
- backend/src/test/java/com/llmscannerseguridad/application/evaluation/EvaluateExecutionUseCaseTest.java
- backend/src/test/java/com/llmscannerseguridad/domain/evaluation/EvaluationPoliciesTest.java
- backend/src/test/java/com/llmscannerseguridad/integration/EvaluationEnginePostgresIntegrationTest.java
- backend/src/test/java/com/llmscannerseguridad/integration/EvaluationMigrationIntegrationTest.java

### Inventario de archivos existentes modificados

- backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/execution/PostgresTestExecutionRepositoryAdapter.java
- backend/src/main/java/com/llmscannerseguridad/application/execution/ExecuteSecurityTestsUseCase.java
- backend/src/main/java/com/llmscannerseguridad/config/ScanConfiguration.java
- backend/src/main/java/com/llmscannerseguridad/config/SecurityTestExecutionConfiguration.java
- backend/src/main/java/com/llmscannerseguridad/config/TransactionalScanLifecycle.java
- backend/src/test/java/com/llmscannerseguridad/integration/ScanTestDatabase.java
- docs/01-investigacion/experimento-academico.md
- docs/02-arquitectura/arquitectura-general.md
- docs/02-arquitectura/decisiones/ADR-003-ejecucion-asincrona-scans.md
- docs/02-arquitectura/decisiones/ADR-004-evaluador-llm.md
- docs/02-arquitectura/decisiones/ADR-005-severidad-confidence-scoring.md
- docs/02-arquitectura/decisiones/ADR-006-criterios-deteccion-mvp.md
- docs/02-arquitectura/decisiones/decisiones-abiertas.md
- docs/03-seguridad/modelo-de-amenazas.md
- docs/03-seguridad/politica-de-autorizacion.md
- docs/03-seguridad/riesgos-owasp.md
- docs/04-ia/casos-de-prueba.md
- docs/04-ia/estrategia-evaluacion.md
- docs/04-ia/prompts-evaluadores.md
- docs/05-api/openapi.yaml
- docs/06-base-de-datos/modelo-datos.md
- openspec/changes/evaluation-engine/tasks.md (este registro y las 34 casillas; los demás artefactos aprobados permanecen intactos).

### OpenSpec final

openspec validate evaluation-engine --strict: salida 0, texto exacto: Change 'evaluation-engine' is valid. Telemetría deshabilitada; ejecución autorizada fuera del sandbox por resolución de esquema. Las 34 tareas quedan registradas y el change se entrega READY FOR REVIEW, sin archivar.

## Corrección del review F1–F4 (2026-10-10)

Esta continuación corrige exclusivamente los cuatro findings solicitados. Conserva requirements, D-01..D-16, HYB-01..07, confidence-v1, severity-v1, EvaluatorPort, endpoints, proveedor/modelo y semántica de Scan. No modifica V1–V5 ni dependencias.

- [x] F1 (tarea 3.2): propertyTypes trataba ausencia como tipo incorrecto. DeterministicRules comprueba tipos únicamente en claves presentes; requiredKeys mantiene la obligatoriedad. EvaluationPoliciesTest verifica propiedad opcional ausente, presente correcta, presente incorrecta y requerida ausente.
- [x] F2 (tareas 3.1/3.2/5.1): OUTPUT_CONTRACT podía interpretar la representación redactada como incumplimiento del target. Las comprobaciones de estructura e igualdad completa identifican dependencia de evidencia alterada; una aparente infracción estructural redactada sin literal intacto independiente queda NOT_APPLICABLE/EVIDENCE_REDACTED. EvaluationEvidenceProjector aplica abstención existente HYB-01, INCONCLUSIVE/0.00. Las coincidencias literales no pueden solapar el marcador insertado por el scanner; canarios intactos y redacción irrelevante conservan señales válidas. EvaluationPoliciesTest, EvaluationScenarioTest y scannerRedactionOfRequiredStructureNeverCreatesFinding cubren claves/estructura redactadas, literales intactos, marcador fabricado y ausencia de Finding, usando la redacción real con datos sintéticos en PostgreSQL.
- [x] F3 (tareas 4.3/5.2/9.1): prepare() consumía el corpus antes del claim durable. Ahora consulta sin eliminar; PostgresEvaluationRepositoryAdapter libera después del commit de cierre/aborto, conservándolo tras rollback. concurrentFullEvaluationsPrepareIndependentlyAndOnlyClaimWinnerConsumesCall ejecuta dos casos de uso completos con preparación independiente, fuerza ambos órdenes de preparación/claim, verifica corpus exportable durante el intento ganador, una llamada y un Finding/resultado por ejecución, y liberación al finalizar. ambiguousClaimAndTerminalCommitsNeverAuthorizeAnotherCallOrPartialFinding verifica además conservación tras rollback y liberación tras commit real, incluso ambiguo.
- [x] F4 (tarea 6.3): la provenance se asignaba solo con reserva monetaria. EvaluatorSettings separa validez de tarifa y reserva; OpenAiEvaluatorAdapter estima usage con tarifa válida sin exigir reserva; claim conserva pricingVersion/estimateVersion independientemente del límite monetario. EvaluateExecutionUseCase descarta importes sin provenance conocida. usageCostNeedsKnownTariffButDoesNotNeedMonetaryControlOrReservation y persistedUsageCostAlwaysHasTariffProvenanceWithAndWithoutMonetaryControl prueban cálculo/round-trip durable sin control monetario, tarifa desconocida y reserva activa; las pruebas existentes de cupo/timeout siguen pasando.

### Archivos de esta corrección

- Dominio: backend/src/main/java/com/llmscannerseguridad/domain/evaluation/DeterministicRules.java.
- Aplicación: backend/src/main/java/com/llmscannerseguridad/application/evaluation/EvaluateExecutionUseCase.java.
- Adapters: backend/src/main/java/com/llmscannerseguridad/adapters/out/evaluation/{EvaluationEvidenceProjector,EvaluatorSettings,OpenAiEvaluatorAdapter}.java y backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/evaluation/PostgresEvaluationRepositoryAdapter.java.
- Tests: backend/src/test/java/com/llmscannerseguridad/domain/evaluation/EvaluationPoliciesTest.java; backend/src/test/java/com/llmscannerseguridad/adapters/out/evaluation/{EvaluationScenarioTest,EvaluatorContractTest}.java; backend/src/test/java/com/llmscannerseguridad/integration/EvaluationEnginePostgresIntegrationTest.java. Diez métodos nuevos; se amplía también el caso existente de rollback/commit ambiguo.
- Documentación: docs/04-ia/estrategia-evaluacion.md, docs/06-base-de-datos/modelo-datos.md y este tasks.md. Sin archivos nuevos en esta continuación.

### Evidencia ejecutada de esta corrección

Java y javac: Temurin 21.0.12.1, verificados antes de cada Maven. Maven Wrapper offline, sin instalar/actualizar dependencias. Solo se leen en memoria las tres variables SCAN_TEST_DB_* de .env; no se imprimen credenciales. ScanTestDatabase verifica current_database() = llm_scanner_seguridad_test y PostgreSQL 16 antes de preparar esquemas aislados; versión observada 16.15. EvaluatorPort fake y transporte controlado, sin OpenAI real.

- Dirigidos F1–F4 y regresiones inmediatas: `mvnw.cmd -o "-Dtest=EvaluationPoliciesTest,EvaluationScenarioTest,EvaluatorContractTest,EvaluationEnginePostgresIntegrationTest#scannerRedactionOfRequiredStructureNeverCreatesFinding+concurrentFullEvaluationsPrepareIndependentlyAndOnlyClaimWinnerConsumesCall+persistedUsageCostAlwaysHasTariffProvenanceWithAndWithoutMonetaryControl+ambiguousClaimAndTerminalCommitsNeverAuthorizeAnotherCallOrPartialFinding+optionalCostInvalidPricingSkipsAndValidReservationsAreConservedAfterTimeout" test`: **36 tests, 0 fallos, 0 errores, 0 omitidos; BUILD SUCCESS**. Log local backend/target/evaluation-review-directed.log.
- Batería evaluation-engine: `mvnw.cmd -o "-Dtest=Evaluation*Test,EvaluatorContractTest,OpenAiTransportTest,EvaluateExecutionUseCaseTest" test`: **65 tests, 0 fallos, 0 errores, 0 omitidos; BUILD SUCCESS**. Incluye 17 tests de integración del motor y 3 de migración V5 con PostgreSQL real. Log local backend/target/evaluation-review-suite.log.
- Regresión amplia y package: `mvnw.cmd -o "-Dtest=*,!LlmScannerSeguridadApplicationTests" package`: **304 tests, 0 fallos, 0 errores, 0 omitidos; BUILD SUCCESS**. Incluye ProjectPostgresIntegrationTest (7), TargetPostgresIntegrationTest (5), Scan/ejecuciones/migraciones y los 65 del motor. PROJECT_TEST_DB_* y TARGET_TEST_DB_* se vinculan en el proceso a los SCAN_TEST_DB_* de la base ya verificada, habilitando la fixture Target. Todos usan esquemas aleatorios aislados; se excluye únicamente LlmScannerSeguridadApplicationTests porque no aísla esquema. El total no incluye ese smoke test excluido. Log local backend/target/evaluation-review-package.log.
- PostgreSQL: current_database() exacto verificado y migraciones reales V1–V5 en esquemas de llm_scanner_seguridad_test; batería de migración V5 (3) e integración del motor (17) pasan también dentro de package. Sin modificar SQL V1–V5, sin datos históricos inventados ni uso de la base habitual.
- Package: JAR ejecutable generado en backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar, repackage correcto, salida 0.
- OpenSpec strict tras corregir F1–F4 y registrar tests dirigidos/batería: salida 0, `Change 'evaluation-engine' is valid`; telemetría deshabilitada.

Estado de esta corrección: **READY FOR RE-REVIEW**. F1–F4 cubiertos por evidencia ejecutada; sin bloqueos. Permanece excluido el smoke test sin esquema aislado. La abstención conservadora ante estructura redactada evita atribuir al target una transformación que no puede verificarse con la evidencia retenida. No se ha probado OpenAI real ni se ha iniciado otro change.
