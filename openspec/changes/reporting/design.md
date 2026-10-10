## Context

Motivación y alcance: [proposal.md](proposal.md). Comportamiento verificable: [specs/reporting/spec.md](specs/reporting/spec.md), RP-01..RP-14. Este documento contiene el diseño aprobado y es la referencia principal para implementar reporting cuando se solicite; ninguna tarea sustituye una regla o decisión de diseño.

Baseline revisado, sin modificarlo:

- AGENTS.md, perfiles README/architecture-agent y openspec/config.yaml; proposal/spec/design/tasks relevantes de evaluation-engine y scoring-engine. Los seis changes previos se consideran implementados, validados y aprobados según el usuario y sus registros. `openspec/specs/` solo tiene .gitkeep; no se fabrican deltas MODIFIED contra specs inexistentes.
- Arquitectura general, ADR-003, decisiones abiertas, estrategia de evaluación, OpenAPI actual, modelo de datos y requisitos/checklist oficiales TFM. Reporting sigue pendiente en documentación conceptual; vídeos/slides/repositorio/despliegue son entregables académicos, no contenido ni funcionalidades del informe.
- EvaluationResult terminal es inmutable. Finding existe si y solo si COMPLETED/VULNERABLE, máximo uno por ejecución y confirmado atómicamente con evaluación. Sus quince campos públicos incluyen textos de plantillas controladas, no narrativa libre del proveedor. title<=150 puntos de código; description/evidenceSummary/recommendation<=1000 cada uno. No copiar raw evidence aunque una columna histórica se llame evidence.
- Cuatro pruebas/revisión1, una por LLM01/LLM02/LLM08/LLM10. Confidence y severity independientes; CRITICAL reservado sin asignación automática. LLM10 OUTPUT_ONLY. Los fallos de componente LLM pueden consolidarse en COMPLETED con V o I; un FAILED global impide completar Scan. HYBRID_WITH_SIGNALS no es benchmark LLM-only.
- ScanScore v1 persiste once campos públicos, con breakdown de cuatro VOs sin lifecycle independiente. Los scores/contadores son la fuente de resumen; no hay input hash nuevo. `scans.global_score` permanece sin uso. V6 ya existe, y no se modifica.
- El constructor actual de ScanScore valida el score agregado usando ScoringPolicyV1. Reporting utilizará una proyección de copia propia, no reconstruirá ScanScore desde JSON para generar/consultar el informe: así no introduce una invocación indirecta de scoring. Las validaciones existentes de scoring en su lifecycle/repositorio se mantienen dentro de su módulo, sin cambiarlas.
- TransactionalScanLifecycle.create fija evaluationProfile/scoringVersion en T1. complete bloquea Scan, valida snapshot, calcula/inserta score y confirma transición en T3 REQUIRES_NEW corta. ScanScoreSnapshot.validate exige score durable solo en COMPLETED: no sirve como validador público de un score recién insertado dentro de T3 con Scan aún RUNNING. La nueva composición no debilitará ese guard.
- Recovery valida integridad antes de interrumpir activos e iniciar executor/admisión; no relanza trabajo. Compensación usa los tres motivos públicos existentes y compuerta si tampoco puede confirmarse FAILED. Scan conserva siete campos y cuatro estados.
- TestExecution fija targetConfigurationId/targetUpdatedAt del target leído una sola vez, pero no URL histórica ni snapshot recuperable de configuración. Project/target pueden cambiar de nombre/configuración. Consultarlos ahora para describir el endpoint original sería inexacto y podría incorporar datos sensibles.
- OpenAPI ya tiene GET scoped executions/evaluations/findings/score, UUID/errores estables y no-store. No hay endpoint ni tabla de informes en V1–V6. Se reutilizan convenciones, sin llamadas HTTP internas entre módulos.

No se ha identificado contradicción objetiva que obligue a reabrir modelos aprobados. Añadir report como condición de COMPLETED es una extensión prospectiva **aprobada** expresamente por el usuario, visible en D-12/D-13. No es corrección retroactiva de scoring-engine; la implementación sigue fuera de esta entrega documental.

## Goals / Non-Goals

**Goals:** Snapshot mínimo legible sin joins actuales; presentación propia es controlada y textos de Finding originales; trazabilidad de finales/versiones; atomicidad conjunta sin nueva fase durable; consulta de solo lectura y recuperación existente; diseño suficiente para implementar sin este chat.

**Non-Goals:** Motor de interpretación, recalculado, framework de templates/i18n, nuevo workflow de informes, auditoría criptográfica, reconstrucción de configuración histórica, copias de inputs ni renderer/PDF. Exclusiones de producto completas: proposal.

## Decisions

**D-01..D-24: APROBADAS.** El usuario aprobó expresamente las veinticuatro decisiones el 2026-10-10, con las tres aclaraciones registradas en Approval Status. Cada apartado conserva problema, decisión adoptada, alternativas descartadas, consecuencias e invariantes. La aprobación procede del usuario, no de crear archivos ni pasar strict. Las decisiones anteriores de evaluación/scoring permanecen aprobadas; no quedan decisiones contractuales pendientes en reporting.

### D-01 — SecurityReport como resultado persistido inmutable

**Problema y decisión adoptada.** Una vista ensamblada con recursos actuales cambiaría cuando se modifiquen textos o presentación. Elegir snapshot persistido/versionado al finalizar, un resultado `SecurityReport` de identidad reportId y clave funcional Scan+versión, sin estados propios, updatedAt o edición. Es un agregado pequeño de salida, no otro proceso de análisis.

**Alternativas y descarte.** Derivarlo en cada GET evita almacenamiento, pero exige joins constantes y puede cambiar el informe histórico. Lazy con escritura desde GET introduce carreras, efectos laterales y fallos tardíos. Snapshot manual bajo demanda requeriría POST/autorización/recovery propios. Para cuatro pruebas la copia mínima es acotada y más sencilla que esos workflows.

**Consecuencias e invariantes.** Dominio representa un resultado independiente de Spring/JSON/PDF; persistencia conserva una fila inmutable; API solo consulta y recovery no genera. Confirmado una vez, se conserva igual aunque cambie el nombre de Project. No se promete sobrevivir al borrado de la base: retención futura tendrá que conservar snapshot y provenance. RP-02/RP-10/RP-12.

### D-02 — Snapshot de salida mínimo más referencias de origen

**Problema y decisión adoptada.** Solo referencias no preservan presentación; copiar todas las entidades duplicaría evidencia/secretos. Copiar los datos públicos necesarios para leer el informe completo y mantener ids hacia Scan, EvaluationResult, TestExecution, Finding y ScanScore+versión. No snapshot de inputs, reglas o configuración. Las referencias sirven para auditoría del origen; el contenido visible no depende de volver a resolverlas.

**Alternativas y descarte.** Solo ids obliga a reconstruir incluso títulos/recomendaciones. Copiar toda la BD/entidades incorpora prompts, ciphertext, señales y assessment innecesarios. Copiar nombre/URL actuales del target afirma una historia que no se conserva. Se elige combinación limitada y no un historial completo.

**Consecuencias e invariantes.** scope conserva targetConfigurationId/targetUpdatedAt idénticos en las cuatro ejecuciones y fechas iniciales del Scan. No nombre/description de Project, nombre/URL del target ni headers: son editables o libres y no necesarios para una identificación exacta. Los códigos/revisiones/categorías de las cuatro evaluations identifican las pruebas; no repetir otra lista de tests. Una modificación posterior del target deja intacto el informe. No afirmar que targetUpdatedAt permite recuperar su configuración: la limitación TARGET_REFERENCE_ONLY lo explicita. RP-01/RP-02/RP-07.

### D-03 — Estructura exacta y fronteras del modelo

**Problema y decisión adoptada.** Un JSON abierto impediría saber qué es estable/seguro y complicaría exportadores posteriores. Elegir modelo tipado cerrado y un único snapshot con estas doce propiedades raíz, todas obligatorias:

| Campo | Contenido / fuente / invariantes |
| --- | --- |
| reportId | UUID generado por aplicación; identidad estable, no id del proveedor. |
| scanId, projectId | UUID originales, pertenencia inmutable. |
| generatedAt | UTC ISO8601 con exactamente seis decimales y Z, instante original de construcción, no de consulta/commit. |
| reportVersion, locale | report-v1 y es. |
| scope | Objeto de cuatro campos: scanCreatedAt, scanStartedAt, targetConfigurationId, targetUpdatedAt. Todos no null para informe completo. |
| summary | Copia de los once campos públicos de ScanScore, incluidos scanId/scoringVersion/calculatedAt, sin recalcular ni traducir. |
| evaluations | Cuatro valores finales mínimos, orden code/revision/id; contrato debajo. |
| findings | Cero a cuatro copias de FindingMetadata (quince campos EE-13), orden D-06. |
| interpretation | Objeto de cuatro textos es de D-07/D-08/D-14, congelados y copiados desde recurso controlado. |
| limitations | Nueve valores {code,text} de D-09, sin identidad/lifecycle propio. |

`summary` contiene exactamente scanId, scoringVersion, calculatedAt, globalScore, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount, severityCounts, categories. CategoryScore tiene sus ocho campos aprobados; todas las claves severity están presentes con ceros. No calcular maxSeverity/confidence media/coverage/affectedCategories como nuevas fuentes persistidas. Afectadas se deriva con findingCount>0; sumas se validan contra los originales.

Cada `evaluations` contiene exactamente:

```text
id, testExecutionId, securityTestCode, securityTestRevision, category,
status, finalVerdict, confidence, severity, outcomeReason,
llmStatus, llmErrorReason, degraded, evidenceComplete,
criteriaId, criteriaVersion, versions, finishedAt, findingId
versions = engineVersion, policyVersion, confidenceVersion, severityVersion,
           promptVersion, schemaVersion, llmInputProfile
```

status siempre COMPLETED; decision fields originales; severity null para N/I, findingId null sin Finding. outcomeReason y llmErrorReason conservan el null/código original, no se deducen por texto. criteriaId/version y cada valor de versions copian valor o null si realmente desconocido; no usar versión del despliegue actual como sustituto. configuration puede ser desconocida según el registro origen: no inventar versión, criterio o prompt; el perfil requerido del Scan y scoringVersion siguen conocidos. La proyección lee columnas de versiones sin cargar configuration_snapshot, evidence_projection ni llm_assessment. No provider/model/usage, reasoningSummary de evaluador, recommendation de N/I, señales o decisionCode: los detalles técnicos completos pertenecen al endpoint de evaluaciones ya aprobado y no se copian a evaluations del informe mínimo. Los códigos del estado de componente que se copian ya son públicos y cerrados, no revelan información interna del proveedor.

**Finalidad y minimización de evaluations.** Es una proyección de diecinueve campos, no una copia de EvaluationResult ni de su DTO completo. Identidades/referencias/categoría localizan la prueba y su Finding; status/finalVerdict/confidence/severity permiten interpretar la conclusión; outcomeReason hace visible por qué un INCONCLUSIVE no concluye. llmStatus/llmErrorReason/degraded/evidenceComplete son códigos/indicadores públicos mínimos para distinguir una conclusión consolidada de un fallo del componente o evidencia incompleta. criteriaId/criteriaVersion y los siete identificadores de versions proporcionan trazabilidad sin copiar criterios, prompts o schemas; finishedAt fija el momento original. No se añaden narrativas o detalles internos por comodidad de implementación.

**Exclusiones expresas.** evaluations nunca incluye LlmAssessment completo, reasoning interno, prompts, schemas internos, señales deterministas internas, provider raw response, evidencia raw, chain-of-thought ni secretos. promptVersion/schemaVersion identifican versiones, no sus documentos ni contenido. Report-v1 no aprueba ningún campo de señales deterministas: una excepción futura requerirá un campo público expresamente aprobado y revisar/versionar el contrato cerrado. Que un campo exista en EvaluationResult o en otro endpoint público no autoriza automáticamente su copia. Por ejemplo, una fila origen con llm_assessment y deterministic_signals solo aporta a report los diecinueve campos permitidos; esos dos documentos no se cargan ni se serializan. La persistencia y el GET mantienen esta misma proyección; recovery valida el snapshot, sin ampliarlo desde el origen.

Cada `findings` copia id, evaluationResultId, scanId, testExecutionId, securityTestCode, securityTestRevision, category, verdict, severity, confidence, title, description, evidenceSummary, recommendation, createdAt. Textos exactos, verdict VULNERABLE. No nuevos campos en Finding ni cambio de su endpoint.

**Alternativas y descarte.** Serializar entidades completas filtra implementación y datos privados; esquema genérico con maps permite claves arbitrarias; añadir nombres redundantes, métricas nuevas o fallos globales ficticios confunde responsabilidades. El conjunto cerrado tiene una única interpretación verificable.

**Consecuencias e invariantes.** Valores anidados son VOs inmutables, sin repositorios/estados por categoría/Finding del report. Dominio conserva datos y restricciones; traducciones pertenecen a presentación, no a Severity/EvaluationResult/Finding. Adaptador mapea a JSONB/REST explícitamente. Deserialización rechaza claves desconocidas/duplicadas, faltantes, coerciones y null indebido. Sin report parcial. RP-02/RP-05/RP-14.

### D-04 — Presentación propia es y textos de Finding en su idioma original

**Problema y decisión adoptada.** Mostrar solo HIGH/INCONCLUSIVE no es suficiente para un lector castellano; traducir datos de dominio cambiaría contratos o síntesis aprobadas. Elegir locale=es único para la presentación propiedad de report-v1: labels, nombres visibles, interpretation, limitations y textos controlados, incluido un executive summary determinista futuro si existe. Enums en inglés y catálogo `report-v1/es` en recursos del adaptador, inmutable/versionado. Reporting no traduce, reescribe ni regenera title, description, evidenceSummary o recommendation de Finding: los copia literalmente en el idioma original. No detección automática de idioma, traducción LLM, servicio externo de traducción o locale negociable.

**Alternativas y descarte.** Localizar Severity/Finding acopla dominio a interfaz; i18n general, Accept-Language y múltiples snapshots por idioma añaden rutas no necesarias; traducción runtime cambia evidencia explicativa y puede introducir errores. Rechazar un Finding válido por estar en otro idioma impone una restricción ajena a su contrato. Por eso se admite contenido original en otro idioma dentro de un report locale=es; esto no modifica reportVersion. Cambiar el idioma/textos de presentación propios del informe sí exige revisar/versionar su contrato.

**Consecuencias e invariantes.** reportVersion fija también locale y labels; locale no es otra dimensión de unicidad en v1. Los cuatro textos interpretation y nueve limitations se copian desde presentación al snapshot para preservar wording. Labels técnicos se conservan como recurso v1 para render futuro, sin duplicar un diccionario completo por fila ni introducir traducciones en entidades previas. GET entrega datos y textos propios es, con los cuatro textos originales de Finding en cualquier idioma válido de origen. No garantiza que todo el contenido esté en castellano. Persistencia, reentrada, consulta y recuperación conservan esos textos sin transformarlos. RP-03/RP-10.

**Baseline y ejemplo.** Finding.from genera actualmente el título «Incumplimiento controlado de ...»; EvaluateExecutionUseCase produce description, evidenceSummary y recommendation con plantillas controladas en castellano. Reporting conserva esos valores tal como están, incluidos códigos técnicos en inglés. Si una fuente válida aporta title «Observable output issue», description, evidenceSummary y recommendation en inglés, los cuatro permanecen idénticos y locale sigue es; labels, interpretation y limitations siguen en castellano. Esto aplica a un Finding válido conforme a las políticas upstream, no autoriza evidencia libre ni cambios al Evaluation Engine.

| Concepto técnico | Etiqueta es de report-v1 |
| --- | --- |
| Severity LOW / MEDIUM / HIGH / CRITICAL | Baja / Media / Alta / Crítica. |
| RiskLevel NO_FINDINGS / LOW / MEDIUM / HIGH / CRITICAL | Sin hallazgos detectados / Bajo / Medio / Alto / Crítico. |
| VULNERABLE / NOT_VULNERABLE / INCONCLUSIVE | Vulnerabilidad detectada / No se detectó vulnerabilidad en esta prueba / Evaluación inconclusa. |
| LLM01 / LLM02 / LLM08 / LLM10 | Inyección de instrucciones / Divulgación de información sensible / Exposición de contexto oculto / Tratamiento inadecuado de la salida. Mantener código y referencia OWASP junto al label. |
| status COMPLETED / llmStatus SUCCEEDED | Evaluación finalizada / Componente evaluador finalizado. No equivalen a seguridad. |
| llmStatus FAILED / SKIPPED / NOT_REQUESTED | Componente evaluador fallido / Llamada al evaluador omitida / Llamada al evaluador no solicitada. |

Motivos semánticos es, asociados a códigos originales sin volver a clasificarlos:

| outcomeReason | Etiqueta |
| --- | --- |
| CONTEXT_UNAVAILABLE | Contexto de evaluación no disponible. |
| CRITERION_NOT_APPLICABLE | Criterio no aplicable. |
| EXPORT_NOT_AUTHORIZED | Envío al evaluador no autorizado. |
| EVIDENCE_NOT_EXPORTABLE | Evidencia no autorizada para envío. |
| EVIDENCE_TOO_LARGE | Evidencia o entrada excede el límite. |
| EVIDENCE_INCOMPLETE | Evidencia incompleta. |
| INSUFFICIENT_EVIDENCE | Evidencia insuficiente para concluir. |
| SIGNAL_CONFLICT | Conflicto entre señales. |
| LLM_COMPONENT_UNAVAILABLE | Componente evaluador no disponible para concluir. |

| llmErrorReason | Etiqueta |
| --- | --- |
| EVALUATOR_TIMEOUT | Tiempo de espera del evaluador agotado. |
| EVALUATOR_UNAVAILABLE | Evaluador no disponible. |
| EVALUATOR_INVALID_OUTPUT | Salida del evaluador inválida. |
| EVALUATOR_RATE_LIMITED | Límite de solicitudes del evaluador. |
| EVALUATOR_CONFIGURATION_INVALID | Configuración operativa del evaluador no válida. |
| EVALUATOR_BUDGET_EXCEEDED | Presupuesto operativo del evaluador agotado. |
| EVALUATOR_SUPPRESSED | Llamada suprimida por un fallo previo del componente. |

Null sigue null, no se muestra como fallo ni como motivo inventado. Los labels de secciones son Resumen del análisis, Alcance, Resultados por categoría, Hallazgos, Evaluaciones, Interpretación y Limitaciones. El catálogo es pequeño/cerrado; estas tablas fijan el contrato de presentación, no un framework i18n.

### D-05 — Executive summary estructurado y texto derivable

**Problema y decisión adoptada.** Una narrativa libre puede contradecir números y generar coste no necesario. Elegir summary como copia persistida de ScanScore y no persistir/devolver executiveSummaryText en MVP. Un presenter/exportador posterior podrá formatear determinísticamente datos, labels v1 e interpretation persistida. No nuevo puerto LLM.

**Alternativas y descarte.** Persistir una segunda narrativa duplica contadores y necesita otra validación; LLM inventa/interpreta y contamina reproducibilidad; producir todo el texto en GET añade una superficie no pedida ahora. La estructura ya sirve a Angular y exportadores futuros.

**Consecuencias e invariantes.** La regla de representación v1, para consumidores futuros, es: primera frase `Se detectaron {n} hallazgo(s) en las pruebas completadas.` con singular si n=1; incluir por orden CRITICAL/HIGH/MEDIUM/LOW solo contadores positivos usando labels de severidad. Segunda frase `La puntuación heurística de riesgo detectado es {score}/100 ({labelRiskLevel}).` Tercera frase `{i} de {evaluationCount} evaluaciones quedaron inconclusas.` (singular `1 de ... evaluaciones quedó inconclusa`). Si n=0, usar en su lugar la primera advertencia de D-08, sin afirmar cero riesgo. Mostrar siempre la interpretación/limitaciones, no solo esas tres frases. Los números siempre proceden del resumen persistido, nunca de otra fórmula o narrativa.

Ejemplo con HIGH y LOW, dos I y score76: «Se detectaron 2 hallazgos en las pruebas completadas: 1 de severidad alta y 1 de severidad baja. La puntuación heurística de riesgo detectado es 76/100 (alto). 2 de 4 evaluaciones quedaron inconclusas.» Es ejemplo de presentación futura, no campo/renderer que haya que implementar en este change. Los datos copiados permiten producirlo sin reinterpretar Findings. RP-03/RP-05/RP-06.

### D-06 — Orden total de Findings, independiente del orden de consulta

**Problema y decisión adoptada.** Un orden accidental puede esconder lo grave o variar entre ejecuciones. Elegir CRITICAL>HIGH>MEDIUM>LOW, categoría ascendente LLM01/02/08/10 y UUID canónico lowercase lexicográfico ascendente. Usar comparador explícito de etiquetas de gravedad, no ordinal accidental/orden alfabético de enum; UUID lexical evita ambigüedad de comparadores de enteros con signo.

**Alternativas y descarte.** Orden createdAt del endpoint previo conserva cronología, pero no prioridad de lectura. Confidence primero confunde soporte con impacto; score por Finding no existe. Orden solo por severity deja empates inestables. La tercera clave total preserva determinismo aun si cambia cardinalidad futura.

**Consecuencias e invariantes.** Reporting ordena su propia copia, no altera Findings ni su GET aprobado createdAt/id. En el plan actual solo hay uno por categoría; desempates se prueban en comparador puro sin ampliar catálogo/permitir filas extras en generación productiva. HIGH/0.60 sigue antes que MEDIUM/0.95. Orden es parte de reportVersion y persiste en el array. RP-04.

### D-07 — INCONCLUSIVE visible y distinto del fallo técnico

**Problema y decisión adoptada.** Solo mostrar Findings hace indistinguible una prueba negativa de una sin conclusión. Copiar las cuatro filas finales mínimas y los contadores I/N originales del score. No sumar riesgo, descontar riesgo o crear Findings/reportes de error a partir de abstenciones. Incluir outcomeReason original y fallo/degradación de componente público, sin narrativa interna.

**Alternativas y descarte.** Omitir I oculta incertidumbre; inventar un severity UNKNOWN o coverageStatus añade otro significado no aprobado; convertir FAILED en I hace parecer técnicamente completo un Scan fallido. Los tres ejes originales se mantienen separados: estado de evaluación, verdict y estado del componente.

**Consecuencias e invariantes.** `interpretation.verdictMeaning` exacto: «NOT_VULNERABLE indica que esa prueba concreta no detectó vulnerabilidad; no acredita seguridad de la categoría o aplicación. INCONCLUSIVE indica que la evaluación no pudo concluir con soporte suficiente y no equivale a NOT_VULNERABLE.»

Un COMPONENT FAILED puede coexistir con evaluación COMPLETED/VULNERABLE y Finding, o COMPLETED/I; copiar cada código/valor, nunca inferir fallo global. Un EvaluationResult FAILED no puede entrar al report completo; es fallo técnico de Scan, consultable en endpoints previos cuando report esté UNAVAILABLE. Cuatro I producen report AVAILABLE, score0/I4 y cuatro motivos, nunca un informe de aplicación segura. RP-06/RP-08/RP-13.

### D-08 — Cero significa ausencia de Findings detectados, no seguridad

**Problema y decisión adoptada.** Cero tiene que ser resultado real y no un default de ausencia o una certificación. Conservar score0/NO_FINDINGS del ScanScore y siempre esta interpretación exacta, también en informes con puntuación positiva para explicar la escala:

`interpretation.scoreMeaning` = «La puntuación resume el riesgo detectado mediante una heurística de 0 a 100; no es un porcentaje ni una probabilidad de compromiso. No se detectaron Findings en las pruebas completadas cuando el score es 0. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.»

La frase de primera presentación cuando n=0 es exactamente: «No se detectaron Findings en las pruebas completadas. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.»

`interpretation.confidenceMeaning` = «Confidence expresa el soporte heurístico de la conclusión y es independiente de la severidad del hallazgo. No es una probabilidad calibrada ni una medida de impacto.»

**Alternativas y descarte.** SAFE/«sin riesgos»/«0% riesgo» exceden evidencia; null para un cero legítimo oculta un resultado; score0 como default en ausencia inventa éxito. Elegir objeto disponible con cero y null únicamente para report ausente.

**Consecuencias e invariantes.** Sin Findings, findings=[], no recomendaciones inventadas; con cuatro N se copia N4/I0, con cuatro I N0/I4. Ambos cero, distinta información. Severity individual y RiskLevel agregado conservan sus tipos; labels Alta/Alto los diferencian. No recalcular ni normalizar los valores. API/docs/ejemplos mantienen advertencia y contadores; recovery no produce cero. RP-03/RP-06/RP-13.

### D-09 — Nueve limitaciones controladas, no otra narrativa

**Problema y decisión adoptada.** Un informe sin alcance puede parecer una auditoría exhaustiva. Incluir siempre nueve entradas ordenadas con code técnico y text es literal:

| Orden / code | text de report-v1 |
| --- | --- |
| 1 IMPLEMENTED_CATEGORIES_ONLY | El análisis se limita a las pruebas implementadas para LLM01, LLM02, LLM08 y LLM10; no cubre por completo esas categorías ni el conjunto de OWASP. |
| 2 TARGET_REFERENCE_ONLY | Se identifica la configuración analizada mediante su id y fecha persistidos en las ejecuciones. No se conserva aquí la URL ni una copia histórica recuperable de su configuración; la configuración actual puede haber cambiado. |
| 3 POINT_IN_TIME_TESTS | Los resultados corresponden a pruebas puntuales sobre la configuración referenciada; no constituyen una auditoría exhaustiva ni garantizan resultados futuros. |
| 4 LLM10_OUTPUT_ONLY | LLM10 observa únicamente la salida del modelo. No comprueba el tratamiento posterior de esa salida ni la explotación o protección de un consumidor. |
| 5 POSSIBLE_FALSE_RESULTS | Los criterios y la evaluación pueden producir falsos positivos y falsos negativos; los hallazgos requieren revisión en su contexto. |
| 6 INCONCLUSIVE_RESULTS | Una evaluación inconclusa no confirma ni descarta vulnerabilidades. Debe interpretarse junto con su motivo y los contadores de resultados. |
| 7 HEURISTIC_METRICS | Score y confidence son métricas heurísticas, no probabilidades calibradas, porcentajes de seguridad o medidas de cobertura. |
| 8 EXCLUDED_CAPABILITIES | Este MVP no evalúa RAG avanzado, agentes, herramientas ni streaming, ni capacidades fuera de las pruebas implementadas. |
| 9 SYNTHETIC_SCENARIO_SCOPE | La evaluación del MVP depende de criterios y escenarios sintéticos autorizados. La falta de contexto o permiso de envío puede impedir conclusiones; no se acredita validación sobre datos reales ni un benchmark académico completo. |

**Alternativas y descarte.** Texto libre por informe varía interpretación y puede revelar contenido; ocultar limitaciones si no hay I sugiere cobertura completa; una memoria extensa dentro del producto distrae y no es funcionalidad MVP. Nueve frases breves cubren las restricciones reales sin informe académico adicional.

**Consecuencias e invariantes.** Copiar constantes de presentación al snapshot, sin modificar criterio/score ni generar recomendaciones. No enlaces externos recuperados, evidencia, valores de escenario o canarios. Codes/orden/textos pertenecen a report-v1; actualización futura usa otra versión. También se muestran en report0 y report100. RP-07/RP-14.

### D-10 — Una recomendación por Finding, sin deduplicación

**Problema y decisión adoptada.** Agregar recomendaciones similares puede eliminar el vínculo al problema o inventar una mitigación general. Copiar recommendation exactamente dentro de cada Finding, siguiendo D-06. Sin lista global adicional ni textos nuevos para N/I.

**Alternativas y descarte.** Deduplicación por texto/similitud pierde contexto y exige nueva política; LLM para síntesis global viola separación; sustituir por catálogo actual altera históricos. Incluso dos recomendaciones idénticas se conservan dos veces con sus ids.

**Consecuencias e invariantes.** Cero Findings significa cero recomendaciones de hallazgos; limitaciones e interpretación no se presentan como recomendaciones nuevas. Recomendaciones son texto inerte y no comandos a ejecutar. API/persistencia conservan texto y longitud originales; lectura/recovery no los modifica. RP-04/RP-14.

### D-11 — report-v1 congela interpretación y no sustituye otras versiones

**Problema y decisión adoptada.** Identificar el informe con scoringVersion no distingue cambios de estructura/textos. Fijar reportVersion=report-v1 y conservar sus esquema, idioma de presentación propia, constantes, mappings, orden y semántica. summary conserva scoringVersion/calculatedAt original; evaluations conserva versiones de criterios/engine/policies/prompt/schema que existían. El nombre de prompt es provenance, nunca su contenido ni instrucción.

**Alternativas y descarte.** Reusar scoring-v1 para todo acopla presentación a fórmula; latest al leer reinterpreta filas; overwrite de snapshots destruye evidencia de entrega. Copiar versión actual donde falta origen inventa provenance. Se descartan.

**Consecuencias e invariantes.** Nueva estructura/wording/idioma de presentación propia/orden requiere report-v2 y otro change aprobado; preservar reader/recursos v1. El idioma original de un Finding no es una variante de presentación: copiarlo según D-04 no exige nueva reportVersion. No selector público/regeneración por versión ahora. PK funcional permite futuro versionado sin diseñar cómo producirlo. No añadir input hash: copia inmutable+referencias+versiones bastan; no garantía criptográfica contra alteraciones administrativas. Fecha/UUID originales se preservan. RP-03/RP-10.

### D-12 — Generación dentro de T3 desde score insertado

**Problema y decisión adoptada.** «Scoring confirmado → report → COMPLETED» puede sugerir dos commits, pero SC-07 prohíbe score durable con Scan activo. Elegir una sola T3: confirmar inputs anteriores, insertar score, leerlo dentro de la misma transacción, construir/insertar report y cerrar Scan; commit conjunto. «Persistido» aquí es fila insertada/visible para la transacción propia, no confirmación externa anticipada. Reporting no recalcula el score aunque todavía no sea visible fuera.

**Alternativas y descarte.** Generar después de COMPLETED introduce otra condición/availability/recovery y deja frontend esperando. Lazy en GET escribe; score confirmado antes de report rompe la atomicidad previa. No hay red ni renderer, solo cuatro filas/Findings y constantes: extender una T3 corta es defendible para MVP.

**Composición aprobada**, sin tocar transacciones de evaluación:

```text
T1: crear Scan PENDING + evaluationProfile + scoringVersion + reportVersion requerida
dispatch/T2/trabajo: sin cambios; cuatro terminales/Findings ya confirmados
T3 única REQUIRES_NEW:
  lock Scan primero; leer marcadores y resultados en snapshot coherente
  validar integridad durable previa (score/report ausentes si activo)
  si terminal: validar requisitos históricos/aplicables y retornar sin generación
  guards previos; CalculateScanScoreUseCase e insert ScanScore, sin cambios de fórmula
  si reportVersion requerida:
    ReportInputPort lee ese score insertado + proyecciones permitidas
    GenerateReportUseCase valida fuentes/contadores, copia/ordena y construye
    insert SecurityReport (misma transacción; sin REQUIRES_NEW anidada)
  finishedAt >= generatedAt >= calculatedAt
  transición condicional RUNNING -> COMPLETED; false inesperado es error
  commit score+report+cierre
```

**Consecuencias e invariantes.** No invocar el GET /score ni ScanScoreSnapshot.validate después del insert mientras Scan sigue RUNNING: su guard público permanece intacto. GenerateReportUseCase recibe una proyección interna de cierre validada por T3; lectura pública solo acepta resultados confirmados. La versión de report se fija al crear, no por fecha/presencia posterior. `generatedAt=max(relojUTC truncado a microsegundos,score.calculatedAt,fechas terminales/Finding)`; `finishedAt=max(relojUTC truncado tras construir,generatedAt)`. Las fechas no entran en contenido matemático; no son hora exacta de commit. El report no copia finishedAt de Scan, todavía no confirmado, para evitar un campo provisional/circular; scope conserva inicio/creación y el GET Scan conserva cierre exacto. No ampliar los 300 s ni agregar fase REPORTING. RP-01/RP-08.

### D-13 — COMPLETED exige report solo en scans nuevos marcados

**Problema y decisión adoptada.** Sin una garantía de lifecycle, COMPLETED no asegura informe listo. Elegir sí para los scans creados con report-v1: cuatro ejecuciones/evaluaciones COMPLETED, ScanScore y SecurityReport íntegros confirmados. Un fallo de reporting impide COMPLETED aunque análisis/evaluaciones ya sean correctos.

**Alternativas y descarte.** Mantener report opcional evita acoplar presentación a éxito técnico, pero exige disparador posterior y estados fallidos propios. Para un snapshot local sin exportación el coste crítico es pequeño y la garantía al frontend compensa. No extender retroactivamente COMPLETED de históricos V6 ni añadir status PARTIAL/REPORT_FAILED.

**Consecuencias e invariantes.** Admitir cuatro I con report completo y score0; vulnerabilidad no implica fallo técnico. DTO/estados/motivos de Scan intactos. El guard de ausencia se aplica por marcador durable report_version, no por scoring_version: un histórico scoring-v1 puede estar COMPLETED sin report legítimamente. Esto amplía requisitos de cierre solo de forma explícita/prospectiva; no cambia evaluación, fórmula ni la garantía previa score+COMPLETED. RP-08/RP-12/RP-13.

### D-14 — Error de reporting es técnico, sin falso resultado de seguridad

**Problema y decisión adoptada.** No construir un resultado local indica fallo del scanner, no incertidumbre del target. Error de versión/datos/textos/size/persistencia/commit revierte T3 e intenta FAILED/SCAN_PROCESSING_FAILED en la compensación existente independiente. Conservar evaluaciones/Findings terminales ya confirmados. No crear INCONCLUSIVE, summary0, report de error o motivos de Scan nuevos.

**Alternativas y descarte.** Report parcial o cero default ocultan fallos; permitir COMPLETED sin report viola D-13; retries cambiarían recovery aprobado. Un error de componente LLM ya consolidado no es error de reporting: se copia como tal y puede permitir report completo.

**Consecuencias e invariantes.** Si falla report, el nuevo score también revierte: availability de /score será UNAVAILABLE en ese FAILED, conservando su contrato aprobado. Findings/evaluations siguen consultables con éxitos. Si compensación falla, compuerta bloquea inicios/claims hasta recuperación y se conserva estado durable. Commit ambiguo: comprobar durable; preservar conjunto completo si confirmado, compensar condicionalmente si no; no considerar excepción prueba de rollback ni repetir cálculo/generación automáticamente. Error de GET es500, no UNAVAILABLE falso.

`interpretation.technicalFailureMeaning` = «Una evaluación inconclusa no es un fallo técnico del Scan. Un fallo del componente evaluador puede coexistir con un resultado final consolidado y se identifica por separado. Si un fallo técnico global impide completar el Scan, no se genera este informe completo; los resultados técnicos y hallazgos ya confirmados se consultan en sus recursos.»

Diagnóstico solo ids/códigos/tiempos, sin Throwable/payload/textos. No prometer escritura si DB no responde. RP-06/RP-09/RP-13/RP-14.

### D-15 — Idempotencia por Scan+versión, insert inmutable

**Problema y decisión adoptada.** Dos cierres o reentrada no deben crear documentos distintos para el mismo análisis. Elegir reportId UUID PK, UNIQUE(scan_id,report_version), lock Scan primero como en scoring e insert sin UPDATE. Terminal confirmado retorna/reconoce original, sin generar otro UUID/fecha ni reconstruir payload.

**Alternativas y descarte.** Solo reportId no evita duplicados lógicos; UNIQUE(scan_id) dificulta futuras versiones; upsert update cambia historia; hash/canonicalización nueva no necesarios con snapshot/fuentes inmutables. Se descartan.

**Consecuencias e invariantes.** Una operación interna que ya trae candidato puede comparar todos los datos semánticos y textos/orden contra existente, excluyendo el UUID/instante candidato; conservar identidad/fecha originales. Diferencia es integridad inválida, no sustitución. La igualdad es de objetos tipados, no orden textual de claves JSONB ni recálculo de scores. Conflicto SQL inesperado no se maneja continuando una transacción abortada: rollback y lectura durable fuera, con política ambigua/compensación existente. No reabrir FAILED ni rellenar COMPLETED sin report. No prometer detección criptográfica de cambios externos ilegales con el mismo resultado. RP-10/RP-11.

### D-16 — Recuperación conserva interrupción, no reanuda generación

**Problema y decisión adoptada.** Tras una caída puede quedar análisis completo sin cierre, pero la recuperación aprobada no reanuda. Mantenerla y extender solo validación de integridad de reporting, dentro de la misma recuperación antes de admisión. No genera snapshot ni score, no toca terminales válidos ni llama a red.

| Frontera de caída | Estado durable / comportamiento |
| --- | --- |
| Tras Findings antes de T3 | Activo→FAILED/SCAN_INTERRUPTED; conservar finales/Findings, sin score/report. |
| Tras insert de score antes de construir report | Insert pertenece a T3 y revierte; activo se interrumpe, sin score/report. |
| Durante construcción/serialización/insert report | T3 no confirmada revierte; activo se interrumpe. |
| Tras insert/flush report antes de transición/commit | No report durable aislado; rollback conjunto y recuperación del activo. |
| Después de commit conjunto | COMPLETED+score+report conservados, sin generación/red ni cambio de fechas. |
| Commit ambiguo | Estado durable será conjunto completo o ausencia; condicionales preservan terminal y compuerta si no se puede confirmar. |
| Report con Scan activo/FAILED, report sin score, o COMPLETED marcado sin report | Integridad rota, diagnóstico/500 en consulta y recuperación no abre admisión; no borrar, backfill o repair. |

**Alternativas y descarte.** Reanudar solo presentación parece barato, pero cambia decisión de recovery y puede anunciar un éxito no confirmado antes de la caída. Colas/leases/otro lifecycle son innecesarios para una instancia. Un report confirmado antes de cerrar Scan sería precisamente la ventana que T3 evita.

**Consecuencias e invariantes.** Validar filas de report y marcadores junto con invariantes de scores, antes de interrumpir; un fallo revierte recuperación completa y mantiene gate cerrado. Sin modificaciones a versiones/Findings, sin recuperación por categoría y sin reconstruir snapshots anteriores. Recovery repetida conserva contenido/fechas y no agrega filas. RP-11/RP-13.

### D-17 — V7 y marcador interno prospectivo

**Problema y decisión adoptada.** Hace falta distinguir histórico sin informe de nuevo COMPLETED corrupto, sin editar migraciones aplicadas. Elegir futura `V7__incorporar_informes_seguridad.sql`, añadir `scans.report_version VARCHAR(64) NULL` y nueva tabla. CHECK marcador null o report-v1; cuando presente, exigir scoring_version no null=scoring-v1 y evaluation_profile_version no null=evaluation-engine-v1, comprobando NULL explícitamente para impedir UNKNOWN SQL. T1 fija los tres en la misma creación durable.

**Tabla aprobada para implementación futura:**

| Columna security_reports | Tipo / restricción |
| --- | --- |
| report_id | UUID NOT NULL PK, generado por aplicación. |
| scan_id | UUID NOT NULL FK scans(id), sin cascada. |
| report_version | VARCHAR(64) NOT NULL CHECK report-v1; UNIQUE(scan_id,report_version). |
| scoring_version | VARCHAR(64) NOT NULL CHECK scoring-v1; FK(scan_id,scoring_version)→scan_scores, insertable en la misma T3 tras el score. |
| generated_at | TIMESTAMPTZ(6) NOT NULL, fecha original. |
| locale | VARCHAR(8) NOT NULL CHECK es. |
| payload | JSONB NOT NULL, objeto completo cerrado de D-03, <=256KiB como texto UTF-8. |

**Alternativas y descarte.** Fecha de despliegue como elegibilidad es frágil; usar solo scoring_version confunde scans V6 con nuevos; columna report en scans acopla lifecycle a payload voluminoso; migrar V1–V6 cambia historial Flyway. Tabla y marcador separados limitan impacto.

**Consecuencias e invariantes.** No project_id redundante relacional: projectId del payload se valida contra Scan en aplicación/consulta; FK Scan conserva pertenencia. Metadata duplicada en payload debe igualar reportId/scanId/reportVersion/summary.scoringVersion/generatedAt/locale de columnas; projectId debe corresponder al Scan unido. Las claves cubren lookup, sin índice redundante por Scan, timestamps de update ni cascade. No cambiar/leer/escribir global_score. V7 admite Findings/scores legítimos y activos V6 con marcador null; sin guard de tablas vacías ni backfill. No SQL creado/ejecutado en esta entrega. RP-08/RP-12.

### D-18 — Persistencia híbrida: metadata relacional y JSONB controlado

**Problema y decisión adoptada.** Normalizar cada sección exige numerosas tablas hijas para un documento que solo se consulta completo. Elegir metadata relacional D-17 para identidad/FKs/lookup y JSONB del snapshot completo, no JSON arbitrario. Usar Jackson ya existente solo en adaptadores; dominio mantiene tipos propios.

**Alternativas y descarte.** Normalización completa facilita analítica ad hoc, fuera del MVP, y complica atomicidad/versionado/orden de arrays. Solo JSON sin columnas pierde FK/uniqueness y obliga a extraer paths para lookup. Guardar Markdown/PDF fija renderer y dificulta API estructurada. Híbrido preserva integridad y salida portable con una fila.

**Consecuencias e invariantes.** Definir validación tipada completa al construir y leer, más función CHECK SQL nueva `report_v1_payload_valid` de forma cerrada y validación de metadata. Comprobar tipos JSON, claves exactas, longitudes/arrays/orden, enums/rangos/null explícito, UUID/UTC, sumas de contadores y relaciones internas Finding↔evaluations. No copiar fórmula ni ejecutar scoring en CHECK de report; summary se coteja por igualdad con ScanScore dentro de T3 y consulta conjunta. Las correspondencias con todas las fuentes se validan al generar, no mediante triggers que vuelvan a evaluarlas. Falta de propiedad/null no puede pasar por UNKNOWN. JSONB no conserva orden de claves: reproducibilidad semántica y de arrays, no bytes/canonical hash; orden de arrays sí se conserva.

JSONB se elige porque inputs son summaries controlados sin U+0000; no replicar la evidencia TEXT escapada de evaluación. Texto inválido, NUL o Unicode inválido se rechaza, no se recorta. GET solo deserializa/mapea snapshot confirmado y valida integridad, sin ensamblar desde Findings actuales. No UPDATE público/interno de payload, wrappers de ORM genéricos, tablas hijas o repositorios por sección. RP-02/RP-10/RP-12/RP-14.

### D-19 — Históricos NOT_APPLICABLE, sin generación retrospectiva

**Problema y decisión adoptada.** Un Scan V6 con score podría permitir construir informe, pero nunca se generó bajo report-v1 al ejecutar. Elegir ningún backfill automático ni bajo demanda: marcador null significa no sujeto a reporting. Histórico conserva su significado, consultas y datos originales.

**Alternativas y descarte.** Generar al primer GET cambia fecha/semántica histórica y añade writes; llenar todos al migrar mezcla cambio de esquema con interpretación de datos previos; marcador por presencia de score clasifica mal históricos V6. Preservar ausencia explícita es consistente con scoring.

**Consecuencias e invariantes.** V7 añade null a filas anteriores, tabla vacía, sin UPDATE ni default report-v1 que los marque. Histórico COMPLETED con score100 o Findings sigue NOT_APPLICABLE/null; histórico FAILED igual, aunque tenga I técnicos. Un activo anterior al despliegue conserva marcador null y recovery previa, sin report. Generación retrospectiva explícita podría diseñarse posteriormente con provenance y permisos, fuera del MVP. RP-11/RP-12/RP-13.

### D-20 — Un GET scoped específico, sin generar al leer

**Problema y decisión adoptada.** Incluir un documento de hasta256KiB en Scan/listados duplicaría datos y rompería siete campos. Elegir GET `/api/projects/{projectId}/scans/{scanId}/report`, sin filtros, formato, paginación ni selector de versión,200 `{availability,report}`; report completo D-03 o null. La versión requerida se lee del marcador original.

**Alternativas y descarte.** GET Scan ampliado rompe contrato; GET por reportId sin scope facilita acceso cruzado; POST generación requiere lifecycle independiente; múltiples endpoints de secciones/export anticipan consumidores no implementados. El recurso subordinado conserva pertenencia y consulta simple.

**Consecuencias e invariantes.** Validar ambos UUID antes de repositorio: INVALID_PROJECT_ID precede INVALID_SCAN_ID. Project primero, Scan scoped después:404 PROJECT_NOT_FOUND/SCAN_NOT_FOUND incluido ajeno.400/404/500 con `{code,message,fieldErrors:[]}` español sanitizado y Cache-Control:no-store en todas las respuestas;500 INTERNAL_ERROR para lectura/estructura/relación/versiones inválidas. Una proyección/snapshot SQL coherente de Scan+marcadores+score+report evita falsa corrupción durante T3. Validación del contenido es lectura, no regeneración. No leer target/credencial/proveedor/Findings/evidencia para enriquecer cada GET; la proyección solo necesita datos del snapshot y el score confirmado para igualdad/integridad.

Antes de clasificar ausencia, aplicar las invariantes aprobadas de scoring y las de report: un Scan marcado COMPLETED sin resultado requerido no es una availability normal. No usar la clase ScoreAvailability como tipo semántico de report, aunque comparta etiquetas. GET funciona sin claves y sin exportación externa. RP-13/RP-14.

### D-21 — Cuatro estados de disponibilidad justificados por el ciclo elegido

**Problema y decisión adoptada.** Ausencia de snapshot puede significar espera, error o histórico, no score0. Elegir los cuatro estados, derivados de marcador+Scan+existencia sin tabla/lifecycle de disponibilidad propio:

| availability | Condición válida exacta | report |
| --- | --- | --- |
| AVAILABLE | Snapshot íntegro de versión requerida, con score y COMPLETED confirmados conjuntamente. | Objeto incluso con summary.globalScore0. |
| PENDING | report_version presente, Scan PENDING/RUNNING y report/score ausentes fuera de T3. | null. |
| UNAVAILABLE | report_version presente, Scan FAILED y report ausente, sin nuevo score confirmado. | null. |
| NOT_APPLICABLE | report_version null y sin report, cualquier estado histórico; score previo permitido. | null. |

**Alternativas y descarte.** Dos estados solo existente/ausente ocultan histórico/fallo;404 para todo confunde recurso inexistente con resultado no disponible; reutilizar automáticamente availability de /score atribuye informe a histórico que ya tiene score. Sí se comparte vocabulario por los mismos cuatro casos de lifecycle, pero no su fuente ni implementación de tipo.

**Consecuencias e invariantes.** Report existente con marcador null/versión errónea/Scan no COMPLETED, falta de score, summary distinto del score, o COMPLETED marcado sin report producen500, no UNAVAILABLE/NOT_APPLICABLE; no se ignora una fila incoherente. Fallo DB también500. En flujo normal no hay Scan COMPLETED/PENDING report porque D-12 lo elimina. Nuevo FAILED antes del plan es UNAVAILABLE; histórico scoring-v1 COMPLETED con score es NOT_APPLICABLE. AVAILABLE jamás null; otras tres siempre null, no objeto parcial ni número0. RP-13/RP-11.

### D-22 — Formato estructurado ahora, render/export después

**Problema y decisión adoptada.** Un formato visible puede ser útil para demostración, pero acopla el primer incremento a presentación/layout y validación adicional. Elegir solo SecurityReport y endpoint JSON. No generar Markdown, HTML, PDF/DOCX ni descargar archivos en este change.

**Alternativas y descarte.** Markdown determinista tiene poco código inicial, pero introduce escape de enlaces/markup y otro contrato/versionado que no necesita el backend mínimo. PDF añade dependencias/render/layout; HTML complejo duplica frontend. Preparar ahora un renderer abstracto sin consumidor sería abstracción anticipada.

**Consecuencias e invariantes.** Dominio no depende de PDF/librerías/HTTP. Snapshot tipado, idioma/textos y arrays estables permiten presenters futuros que consuman report, sin volver a consultar/evaluar/calcular. No renderer/exportport vacío en MVP, no Content-Disposition, S3/email/URL pública. Ejemplos de texto de D-05 fijan intención de presentación, no autorizan implementar otro formato. RP-03/RP-14.

### D-23 — Límites derivados de fuentes y cap defensivo

**Problema y decisión adoptada.** Snapshot debe estar acotado sin introducir recortes que oculten Findings. Reutilizar límites originales y catálogo actual; cap total256KiB para payload JSONB serializado UTF-8 y objeto SecurityReport de API. Wrapper `{availability,report}` añade como máximo512 bytes; cap de respuesta262656 bytes. No compresión como sustituto de límite ni truncamiento.

**Origen del máximo cuatro.** El plan MVP vigente tiene cuatro SecurityTests, uno por cada categoría implementada: cuatro SecurityTests → una evaluación por categoría → máximo un Finding por evaluación → máximo cuatro Findings por Scan. Report-v1 reproduce ese límite de sus fuentes, no define una propiedad universal de Reporting. Con tres evaluaciones VULNERABLE y una INCONCLUSIVE hay tres Findings; con cuatro VULNERABLE puede haber cuatro. Una quinta fila es incompatible con el plan/report-v1 y causa fallo técnico de integridad, nunca selección de las primeras cuatro.

**Consecuencia de evolución.** Si un cambio futuro permite múltiples tests por categoría, deberá revisar/versionar la cardinalidad de report y su presupuesto de tamaño, esquema/validadores de persistencia, contrato API y comprobaciones. No aumentar el límite de report-v1 silenciosamente ni descartar Findings para acomodarlo. El cap actual se justifica con cuatro Findings; no se presume suficiente para una cardinalidad futura. Recovery no reduce arrays inválidos para hacerlos encajar.

| Elemento | Límite / validación |
| --- | --- |
| Findings / evaluaciones / categorías | report-v1: 0..4 / exactamente4 / exactamente4, derivado del plan MVP anterior; detectar extras antes de crear mapas. No tomar primeras4. |
| Finding.title | 1..150 puntos de código, no blanco, restricciones originales. |
| Finding.description/evidenceSummary/recommendation | Cada uno1..1000 puntos de código, no blanco; prohibidos NUL, <, > y backtick según Finding vigente. No normalizar/traducir. |
| securityTestCode / revisiones | Catálogo aprobado, código ASCII<=64 y revision1. No nuevas pruebas. |
| Criteria id / versiones | criteriaId<=64 puntos de código y criteriaVersion<=32, según V5; siete valores versions<=64, de recursos controlados, no blancos si no null. Ausencia real null; versiones de report/scoring fijas. |
| interpretation / limitations | Cuatro/nueve constantes literales D-07/D-08/D-09/D-14; cada texto<=1000 puntos de código, codes<=64 ASCII. No texto de cliente, llm o target. |
| Confidence / contadores / score | Decimal exacto0..1 original, enteros según ScanScore y plan, score0..100 sin modificarlo. |
| Identificadores / fechas | UUID canónicos lowercase y fechas UTC seis decimales; sin strings arbitrarios de proveedor. |
| Payload / respuesta | <=262144 bytes por snapshot; wrapper<=512. Comprobar antes de insert y al serializar/mapping. |

**Justificación del cap.** Cuatro Findings aportan como máximo4×(150+3×1000)=12600 puntos de código textuales. Reservando incluso doce bytes por punto de código para un serializador que escape pares Unicode, son151200 bytes. Summary ya está acotado en origen (JSON de categorías/severities<=16KiB); con sus campos simples reservar20KiB. Las cuatro proyecciones de evaluación tienen campos controlados y versiones cortas (reserva32KiB), constantes es16KiB y estructura/identidades8KiB. Total presupuestado229024 bytes, menor que256KiB; el margen absorbe escapes y estructura sin subir límites de origen. No es autorización para texto libre de esos tamaños: arrays/constantes y esquema también se validan. Si un serializer o futuro origen excede el cap, fallar técnicamente y revisar el contrato mediante otro change, no truncar ni recortar Findings.

**Alternativas y descarte.** Límite arbitrario de n Findings menor que4 oculta resultados; ausencia de cap confía demasiado en tipos; cap pequeño sin analizar escapes puede rechazar datos válidos. Un cap monetario, porcentajes o campos nuevos de cobertura no tienen relación con reporting.

**Consecuencias e invariantes.** Puntos de código no son bytes ni Java String.length. Validar Unicode antes de JSON; JSONB no acepta NUL y no debe recibirlo desde summaries. Dominio valida longitudes/colecciones; adaptador valida bytes reales con serialización explícita y DB CHECK usa octet_length(convert_to(payload::text,'UTF8'))<=262144. JSONB::text puede añadir espacios: comprobar también esa representación, no asumir igualdad byte a byte con API compacta. Fronteras exactas aceptadas si todo lo demás válido; +1 rechazado. Tamaño inválido no convierte Finding en I ni elimina textos. RP-02/RP-14.

El guard genérico de bytes se prueba aisladamente en262144/262145 con datos sintéticos; no se infiere que un report-v1 válido alcance necesariamente el cap. Las fixtures de informe completo usan el máximo permitido de cuatro Findings/textos/Unicode y deben quedar por debajo del cap sin relajar constantes/esquema para rellenarlo artificialmente.

### D-24 — Privacidad, puertos mínimos y ausencia de reinterpretación

**Problema y decisión adoptada.** Serializar datos técnicos completos o enriquecer narrativas puede filtrar secretos, ejecutar output o cambiar resultados. Elegir proyecciones positivas de campos, texto inerte y separación estricta de reporting/evaluation/scoring. No nueva integración externa, resolución DNS/URL, provider SDK ni uso de secretos.

**Arquitectura aprobada:**

| Elemento | Responsabilidad / exclusiones |
| --- | --- |
| domain/reporting SecurityReport y VOs | Inmutabilidad, forma cerrada, relaciones internas, contadores/orden/límites. Sin traducción de entidades previas, Spring/JPA/Jackson/HTTP/PDF ni fórmulas. |
| application/reporting GenerateReportUseCase | Obtener proyecciones mínimas bajo T3, validar conjuntos/igualdades, copiar/ordenar, fijar UUID/reloj y construir snapshot. No calcular score, volver a decidir ni abrir su propia transacción. |
| ReportInputPort | Leer Scan/target refs del plan, proyección literal del score insertado y columnas finales/Finding permitidas. No SELECT *, hidratación de EvaluationResult/ScanScore completo ni puertos de evidencia/target. Leer todas las filas para detectar extras, sin DISTINCT que los esconda. |
| ReportPresentationPort | Proporcionar únicamente constantes report-v1/es tipadas de interpretación/limitaciones y recursos de labels conservados. Adaptador local empaquetado, sin fichero arbitrario/URL/LLM o plantilla programable por usuario. |
| SecurityReportRepositoryPort | Fijar marcador en T1, insert inmutable, lectura scoped conjunta y validación de integridad para recovery. Sin upsert update, firmas Spring/SQL o APIs por sección. |
| GetSecurityReportUseCase | Validar UUID/pertenencia y disponibilidad desde snapshot coherente; devolver report guardado. No llamar a GenerateReportUseCase. |
| Adaptadores PostgreSQL/REST + config | Tipos JSON/SQL/DTO, límites bytes, error/no-store, lock/instantánea/T1/T3/recovery y composición dirigida sobre lifecycle existente. Usar dependencias presentes. |

**Alternativas y descarte.** Reutilizar getters de entidades/serializador global carga raw evidence y filtra campos nuevos sin revisión; Regex como garantía de anonimato desconoce secretos arbitrarios; LLM para reformular sintetiza otra interpretación; consultas HTTP internas complican transacción y auth. Se descartan. ReportInputPort es proyección de salida, no otro evaluador ni duplicado de ScoringInputPort con fórmulas.

La copia de summary usa un VO de lectura propio con los mismos once campos y validación de forma/igualdades, sin construir ScanScore, invocar ScoringPolicyV1 ni reproducir sus fórmulas. Lo mismo se aplica a la lectura conjunta del endpoint: comparar proyecciones persistidas, no llamar al caso de uso/mapper de scoring que reconstruya su agregado. Esta separación no cambia los guards existentes de scoring ni pretende reemplazarlos.

evaluations usa exclusivamente la proyección de diecinueve campos y siete identificadores de versions de D-03, necesaria para entender conclusiones, INCONCLUSIVE, fallos de componente y provenance. No copiar EvaluationResult ni su DTO completo. Quedan expresamente excluidos LlmAssessment completo, reasoning interno, prompts, schemas internos, señales deterministas internas, provider raw response, evidencia raw, chain-of-thought y secretos. Los nombres de versión permitidos no autorizan cargar sus contenidos. Ninguna señal determinista es pública en report-v1; una excepción requiere campo público expresamente aprobado y revisión/versionado, no serialización automática de nuevas propiedades de origen.

**Consecuencias e invariantes.** Solo copiar Finding público controlado/sanitizado por evaluación; validar su forma no certifica por sí solo la ausencia de secretos. La garantía MVP depende de plantillas upstream y minimización; no se admite evidencia libre como evidenceSummary ni se vuelve a redactar texto para ocultar errores de origen. Un Finding malformado o fuera de política falla, sin alterar original. provider raw output, CoT, llmAssessment, evidenceProjection, configurationSnapshot, contexto/canarios, prompts/system prompts, requests/responses, auth headers, ciphertext, tokens y API keys no se leen para report. Los nombres actuales libres tampoco se necesitan.

Logs/toString/errores solo ids/códigos/tiempos/contadores; nunca payload, textos, SQL/bindings o causa remota. Todos los textos se devuelven como strings JSON; futuro renderer deberá escaparlos como texto, no ejecutar Markdown/HTML/enlaces. No modificar evaluaciones/confidence/severity/scoring/ground truth ni inferir recomendaciones. No contaminar experimento deterministic-only/LLM-only/hybrid. Endpoint scoped presupone entorno local/proyecto autorizado, sin afirmar autenticación multiusuario ni permiso de publicación. Sin SSRF nuevo porque no hay acceso externo; no copiar URL para inducir navegación. RP-01/RP-07/RP-09/RP-14.

## Risks / Trade-offs

- Reporting crítico puede hacer FAILED un análisis con Findings correctos → snapshot mínimo local, T3 corta, rollback score/report/cierre y consultas previas intactas; no fallback engañoso.
- Duplicación intencional de summaries → copia única inmutable, validación de contadores/fuentes al generar, igualdad de summary con ScanScore confirmado; no dos políticas.
- Snapshot sin URL histórica ofrece menos contexto humano → referencia exacta id/fecha y limitación explícita; no usar configuración actual para inventar historia. Historial seguro de target sería otro change.
- Presentación propia es y recursos v1 permanentes → mantener readers/textos/labels antiguos; nuevos idiomas de presentación/interpretaciones requieren nueva versión y diseño. Los cuatro textos de Finding pueden conservar otro idioma original sin cambiar reportVersion.
- JSONB reduce integridad relacional de secciones → FKs/UNIQUE de cabecera, forma/tipos/checks cerrados y validación de correspondencia interna/aplicación; no triggers de recálculo.
- Datos modificados fuera del flujo permitido podrían vulnerar inmutabilidad → validación de integridad y ausencia de repair automático; sin promesa de auditoría criptográfica/hash.
- Los límites por informe no acotan el histórico total → retención/borrado futuro debe proteger provenance y privacidad, no diseñado aquí.
- Versiones antiguas de app desconocen report_version → no presentar vuelta a binario antiguo como rollback seguro del worker; requiere procedimiento operativo aprobado.

## Migration Plan

Plan del diseño aprobado, posterior a una instrucción explícita de implementar; no ejecutar SQL/tests/despliegues ahora:

1. Tomar como referencia D-01..D-24 y Approval Status ya aprobados, y verificar coherencia de los cuatro artefactos, incluidas las tres aclaraciones documentales. Mantener decisiones previas y no confundir strict con implementación.
2. Actualizar documentación directamente afectada: arquitectura general, ADR-003 y ADR de reporting, modelo de datos, OpenAPI y privacidad/política de presentación. Referenciar este diseño para contrato detallado. Requisitos oficiales TFM/checklist no cambian de estado por este diseño.
3. Crear V7 conservadora con tabla/función/checks/marcador; fixtures V6 con datos legítimos N/I/V, scores/históricos/activos/global_score. Sin UPDATE/backfill o tablas vacías obligatorias, V1–V6 byte a byte.
4. Implementar resultado/puertos, proyecciones/contenido es, generación sin red y persistencia inmutable; integrar T1/T3/recovery y GET de solo lectura.
5. Preparar/verificar la matriz de tests de tasks, incluyendo competencia, conexiones independientes y puntos de fallo reales. Ejecutar únicamente comprobaciones autorizadas posteriormente, PostgreSQL/Flyway solo en esquema dedicado verificado, sin leer secretos ni usar proveedor/target real para reporting.

Rollback: no borrar V7/reportes/marcador ni ejecutar down migrations/repair. Una app vieja no preservaría el guard report-v1 y no es rollback operativo seguro. Preferir corrección hacia delante y un procedimiento aprobado; esta entrega no autoriza despliegues ni detener servicios.

## Approval Status

**READY FOR IMPLEMENTATION.** D-01..D-24 aprobadas expresamente por el usuario el 2026-10-10; no quedan decisiones contractuales pendientes ni bloqueo técnico de planificación. La aprobación incluye snapshot/estructura, extensión prospectiva de COMPLETED, V7/JSONB, availability y límites/privacidad. Código, SQL, OpenAPI implementada y ejecución de tasks requieren una petición posterior; esta entrega no los implementa.

Aclaraciones incorporadas, manteniendo el resto de decisiones sin cambios:

1. D-04: solo textos propiedad de report-v1 se localizan al castellano; title/description/evidenceSummary/recommendation de Finding se conservan literalmente en su idioma original. El baseline tiene plantillas castellanas; otro idioma válido no se traduce ni obliga a cambiar reportVersion. Sin LLM/servicio externo de traducción.
2. D-03/D-24: evaluations es la proyección mínima cerrada definida aquí, con exclusión expresa de assessment, reasoning, prompts/schemas internos, señales internas, raw response/evidence, chain-of-thought y secretos. Ningún campo interno se incorpora por serialización del objeto origen.
3. D-23: máximo cuatro Findings para report-v1 deriva de cuatro tests, una evaluación por categoría y máximo un Finding por evaluación. No es universal; múltiples tests por categoría requieren revisar/versionar el límite.

## Traceability

| Decisión | Requisitos | Tareas derivadas |
| --- | --- | --- |
| D-01 Persistido | RP-02/RP-10/RP-12 | 2.1, 4.1, 4.3 |
| D-02 Snapshot/referencias | RP-01/RP-02/RP-07 | 3.1, 3.2 |
| D-03 Estructura | RP-02/RP-05/RP-14 | 2.1, 3.3, 4.2, 6.1 |
| D-04 Idioma | RP-03/RP-10 | 2.2, 2.3, 2.5, 3.3, 6.4 |
| D-05 Resumen | RP-03/RP-05/RP-06 | 2.3, 3.3 |
| D-06 Orden | RP-04 | 2.4, 3.3 |
| D-07 Inconclusos | RP-06/RP-08/RP-13 | 2.5, 3.3, 5.3 |
| D-08 Cero | RP-03/RP-06/RP-13 | 2.5, 6.2 |
| D-09 Limitaciones | RP-07/RP-14 | 2.2, 2.5 |
| D-10 Recomendaciones | RP-04/RP-14 | 2.4, 3.3 |
| D-11 Versionado | RP-03/RP-10 | 2.2, 4.3, 6.2 |
| D-12 Generación | RP-01/RP-08 | 3.3, 5.2 |
| D-13 COMPLETED | RP-08/RP-12/RP-13 | 5.1..5.3 |
| D-14 Fallos | RP-06/RP-09/RP-13/RP-14 | 5.4, 5.5, 6.3 |
| D-15 Idempotencia | RP-10/RP-11 | 4.3, 5.5 |
| D-16 Recovery | RP-11/RP-13 | 5.6, 6.3 |
| D-17 V7 | RP-08/RP-12 | 4.1..4.4, 5.1 |
| D-18 JSONB | RP-02/RP-10/RP-12/RP-14 | 4.2..4.4 |
| D-19 Históricos | RP-11/RP-12/RP-13 | 4.4, 5.6, 6.2 |
| D-20 API | RP-13/RP-14 | 6.1..6.4 |
| D-21 Availability | RP-11/RP-13 | 6.1..6.3 |
| D-22 Exportación | RP-03/RP-14 | 2.1, 6.4, 7.1 |
| D-23 Límites | RP-02/RP-14 | 2.6, 4.2, 6.4 |
| D-24 Privacidad/puertos | RP-01/RP-07/RP-09/RP-14 | 3.1..3.4, 6.4, 7.1 |

## Coherence Review

Revisión cruzada de los cuatro artefactos: proposal delimita el snapshot/endpoint y la extensión prospectiva; spec contiene 14 requisitos y 33 escenarios verificables; design desarrolla D-01..D-24 aprobadas y la trazabilidad; tasks contiene 30 tareas futuras, todas sin ejecutar. Las matrices relacionan decisiones, requisitos y comprobaciones sin delegar reglas importantes a las tareas.

Coinciden la copia de los once campos de ScanScore sin fórmula nueva, el modelo de doce campos raíz, los quince campos originales de Finding, las cuatro evaluaciones/categorías, las nueve limitaciones, locale es y report-v1. Se distingue texto derivable futuro de renderer fuera de alcance, resultado cero de informe ausente, fallo de componente de fallo global y referencia histórica de configuración actual. T3 confirma score/report/cierre juntos; V7 no modifica V1–V6, históricos o global_score. Reporting no construye ScanScore para evitar recálculo indirecto, no usa input hash y GET/recovery no generan.

Coinciden las tres aclaraciones: locale=es pertenece solo a presentación propia y no altera textos de Finding, evaluations se limita a diecinueve campos con siete identificadores de versions y exclusiones expresas, y cuatro Findings es límite de report-v1 derivado del plan actual con evolución versionada. Las tareas 2.5/3.3 comprueban idioma original; 3.1/6.4, proyección y privacidad; 2.6/3.2/4.2, cardinalidad sin recortes.

La aprobación de D-01..D-24 procede de la respuesta expresa del usuario registrada arriba. Esta revisión y strict no acreditan tests de implementación, PostgreSQL, privacidad empírica o calibración. Strict comprueba la estructura OpenSpec; la autorización actual incluye esa validación documental y ningún comando de implementación.

## Fuera del MVP / Evolución futura

Renderer Angular/exportación HTML/Markdown/PDF/DOCX, accesibilidad/layout, varios idiomas, informes parciales/fallos, generación retrospectiva explícita con provenance, múltiples versiones por análisis con selección autorizada, historial seguro del target, retención/borrado, autorización multiusuario y publicación privada controlada. No adelantar interfaces/librerías/infraestructura para esas extensiones; ninguna permite reinterpretar automáticamente verdict, impacto, confidence o score históricos.
