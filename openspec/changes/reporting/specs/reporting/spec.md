## Purpose

Ofrecer un informe estructurado, comprensible y reproducible de los resultados finales de un Scan, preservando Findings, incertidumbre, versiones y limitaciones sin reevaluar ni recalcular riesgo.

## Review Status

**READY FOR IMPLEMENTATION.** D-01..D-24 aprobadas expresamente por el usuario el 2026-10-10, con las tres aclaraciones de idioma, proyección mínima de evaluations y límite derivado del plan MVP incorporadas. Todos los requisitos tienen prioridad MUST/MVP. Los SHALL definen el contrato aprobado; evaluación/scoring y contratos anteriores conservan sus reglas. Esta entrega es documental; implementar requiere una petición posterior.

## ADDED Requirements

### Requirement: RP-01 Fuentes finales y separación de responsabilidades

**Historia:** Como analista, quiero un informe fiel a los resultados originales para comprenderlos sin una segunda interpretación técnica.
**Dependencias:** EE-08/EE-10/EE-13, SC-01/SC-07/SC-13; D-01/D-02/D-12/D-24.

Reporting SHALL consumir únicamente la identidad/fechas del Scan, las referencias históricas de target del plan, cuatro evaluaciones COMPLETED, sus Findings públicos y ScanScore insertado de la versión requerida. SHALL validar pertenencia, correspondencia uno a uno y contadores antes de confirmar el snapshot. Cada VULNERABLE SHALL corresponder a un único Finding con idéntica severity/confidence/categoría; N/I SHALL carecer de Finding. SHALL rechazar filas ausentes/extras/duplicadas o relaciones inconsistentes sin deduplicar ni publicar un informe parcial. La validación de contadores SHALL NOT recalcular scores, verdicts, severities o confidence ni volver a aplicar reglas. Reporting SHALL NOT llamar target/proveedor, modificar resultados, consultar ground truth, assessment, evidencia, prompts o configuración actual del target.

#### Scenario: Fuentes válidas sin operaciones externas
- **WHEN** existen el plan, cuatro evaluaciones finales y ScanScore coherentes
- **THEN** el informe copia sus valores y referencias sin ninguna llamada ni evaluación/cálculo nuevos
- **AND** los resultados originales permanecen iguales

#### Scenario: Fuente ausente o duplicada
- **WHEN** falta una evaluación o un Finding vulnerable, hay filas extras/duplicadas o un contador no concuerda
- **THEN** la generación falla técnicamente y no confirma un snapshot parcial

#### Scenario: Target modificado después del plan
- **WHEN** se cambia la configuración del target tras preparar las ejecuciones
- **THEN** el informe conserva targetConfigurationId/targetUpdatedAt del plan
- **AND** no toma URL, nombre, headers o configuración actuales como datos históricos

### Requirement: RP-02 Snapshot mínimo con contrato cerrado

**Historia:** Como consumidor, quiero una representación estable con referencias suficientes para consultar el origen.
**Dependencias:** RP-01, EE-13, SC-13; D-02/D-03/D-18/D-23.

SecurityReport SHALL contener exactamente reportId, scanId, projectId, generatedAt, reportVersion, locale, scope, summary, evaluations, findings, interpretation y limitations. reportId SHALL ser UUID estable; reportVersion=report-v1 y locale=es. Todos los campos SHALL estar presentes, con null únicamente donde se permite expresamente.

- scope SHALL contener exactamente scanCreatedAt, scanStartedAt, targetConfigurationId y targetUpdatedAt. Las cuatro ejecuciones SHALL compartir la misma referencia de target. SHALL NOT incluir URL, nombre actual ni snapshot de configuración.
- summary SHALL ser copia exacta de los once campos públicos de ScanScore: scanId, scoringVersion, calculatedAt, globalScore, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount, severityCounts y categories. SHALL NOT traducir ni reordenar su breakdown.
- evaluations SHALL ser una proyección mínima y segura para interpretar el resultado, los INCONCLUSIVE y su trazabilidad, no una copia de EvaluationResult. SHALL contener cuatro entradas ordenadas securityTestCode, securityTestRevision e id, cada una con exactamente id, testExecutionId, securityTestCode, securityTestRevision, category, status, finalVerdict, confidence, severity, outcomeReason, llmStatus, llmErrorReason, degraded, evidenceComplete, criteriaId, criteriaVersion, versions, finishedAt y findingId. status SHALL ser COMPLETED. severity SHALL ser null para N/I; findingId null sin Finding; outcomeReason/llmErrorReason SHALL conservar null o el código original. criteriaId/criteriaVersion SHALL copiar valor o null de origen, sin inventar identidades. versions SHALL contener exactamente engineVersion, policyVersion, confidenceVersion, severityVersion, promptVersion, schemaVersion y llmInputProfile, cada valor original o null si desconocido. Los identificadores promptVersion/schemaVersion SHALL ser únicamente provenance, nunca contenido de prompts/schemas. SHALL NOT incluir LlmAssessment completo, reasoning interno ni resumen narrativo del evaluador, prompts, schemas internos, señales deterministas internas, provider raw response, evidencia raw, chain-of-thought, secretos, proveedor/modelo ni datos de facturación. Report-v1 SHALL NOT exponer señales deterministas: cualquier excepción futura SHALL exigir un campo público expresamente aprobado y revisar/versionar el contrato cerrado; la existencia de un dato en origen no autoriza su copia.
- findings SHALL copiar exactamente los quince campos públicos de EE-13: id, evaluationResultId, scanId, testExecutionId, securityTestCode, securityTestRevision, category, verdict, severity, confidence, title, description, evidenceSummary, recommendation y createdAt. El orden SHALL cumplir RP-04.
- interpretation SHALL contener exactamente scoreMeaning, confidenceMeaning, verdictMeaning y technicalFailureMeaning, textos controlados es congelados en D-03/D-07/D-08/D-14. limitations SHALL contener las nueve entradas {code,text} ordenadas de RP-07.

Los datos necesarios para presentar el informe SHALL permanecer copiados e inmutables; las referencias SHALL permitir trazabilidad pero no obligar a consultar datos actuales para completar su lectura. No SHALL duplicar toda la base, pasos matemáticos, inputs ni añadir input hash.

#### Scenario: Snapshot tras evolución de fuentes editables
- **WHEN** se consulta un informe confirmado después de cambiar nombres de Project/target o desplegar otro catálogo de presentación
- **THEN** devuelve el mismo contenido, identidad, textos, orden y generatedAt de report-v1
- **AND** no enriquece campos desde entidades actuales

#### Scenario: Contrato sin propiedades adicionales
- **WHEN** se intenta generar o leer un payload con una clave desconocida, campo obligatorio ausente o tipo incorrecto
- **THEN** se rechaza como error técnico sin publicar contenido parcial

### Requirement: RP-03 Castellano y resumen ejecutivo estructurado

**Historia:** Como usuario castellano hablante, quiero interpretar los resultados con términos claros sin perder identificadores técnicos.
**Dependencias:** RP-02, SC-02; D-04/D-05/D-11/D-22.

El informe SHALL fijar locale=es para su presentación propia. Enums, ids y versiones SHALL permanecer en inglés. Solo labels, nombres visibles, interpretation, limitations y textos controlados propiedad de report-v1, incluido el resumen determinista futuro si existe, SHALL localizarse al castellano mediante presentación report-v1/es, separada del dominio de evaluación/scoring. title, description, evidenceSummary y recommendation de Finding SHALL conservarse literalmente, sin traducción, reescritura, paráfrasis, resumen ni regeneración. El baseline MVP ya utiliza plantillas controladas en castellano; si una fuente válida contiene textos en otro idioma, SHALL conservar ese idioma aunque locale=es. SHALL NOT exigir idioma castellano a Finding ni invocar LLM o servicio externo de traducción.

summary SHALL ser el resumen ejecutivo persistido. El MVP SHALL NOT persistir ni devolver executiveSummaryText, ni generar HTML/Markdown/PDF. Su representación futura SHALL ser derivable de esos datos con la plantilla v1 documentada en design, sin LLM, y SHALL mencionar Findings, score/nivel e inconclusos, incluidos ceros. Cambiar textos/interpretación v1 SHALL requerir nueva reportVersion; SHALL conservar recursos de presentación históricos.

#### Scenario: Resumen estructurado sin narrativa añadida
- **WHEN** el ScanScore original es 76/HIGH con dos Findings y dos INCONCLUSIVE
- **THEN** el informe conserva exactamente esos valores, locale es e interpretación controlada
- **AND** no incluye un texto libre del LLM ni un campo executiveSummaryText

#### Scenario: Idioma y enums
- **WHEN** el informe contiene severity HIGH y riskLevel HIGH
- **THEN** ambos valores técnicos siguen HIGH y sus etiquetas de presentación v1 son Alta y Alto respectivamente
- **AND** title/description/evidenceSummary/recommendation originales no se reescriben

#### Scenario: Finding en su idioma original
- **WHEN** un Finding válido contiene title, description, evidenceSummary y recommendation en inglés
- **THEN** los cuatro campos se copian literalmente al snapshot y a su consulta, sin traducirlos ni regenerarlos
- **AND** locale sigue es y los textos propios de report-v1 siguen en castellano, sin llamada LLM ni servicio externo

### Requirement: RP-04 Findings íntegros ordenados y recomendaciones originales

**Historia:** Como analista, quiero encontrar primero lo más grave y conservar cada recomendación vinculada a su evidencia.
**Dependencias:** EE-08/EE-13, RP-01/RP-02; D-06/D-10/D-23.

El informe SHALL incluir todos los Findings persistidos, sin filtrar por confidence ni ocultarlos detrás del score. SHALL ordenarlos por severity CRITICAL>HIGH>MEDIUM>LOW, después category LLM01<LLM02<LLM08<LLM10 y después UUID id canónico minúsculo lexicográfico ascendente. SHALL NOT depender del orden DB ni cambiar el orden del endpoint Findings ya aprobado. Cada recommendation SHALL mostrarse dentro de su Finding, sin deduplicación, recomendaciones globales nuevas, texto de evaluación alternativa ni comandos ejecutados.

#### Scenario: Un LOW
- **WHEN** hay un Finding LOW y tres evaluaciones NOT_VULNERABLE
- **THEN** aparece ese Finding con todos sus campos originales, su recommendation y el score ya persistido

#### Scenario: Gravedad y desempate estables
- **WHEN** los mismos Findings llegan en distintos órdenes de lectura
- **THEN** CRITICAL precede a HIGH, HIGH a MEDIUM y MEDIUM a LOW, aplicando categoría e id en empates
- **AND** la salida es idéntica sin reclasificar severity

#### Scenario: Recomendaciones iguales
- **WHEN** dos Findings contienen la misma recommendation
- **THEN** ambas permanecen vinculadas a su Finding y en el orden definido, sin crear otra recomendación agregada

### Requirement: RP-05 Resumen global y categorías sin duplicar scoring

**Historia:** Como usuario, quiero localizar riesgo e incertidumbre por categoría con valores consistentes con el motor de scoring.
**Dependencias:** SC-05/SC-06/SC-13, RP-01/RP-02; D-03/D-05/D-09.

summary SHALL conservar globalScore/riskLevel, contadores y las cuatro categorías exactamente como ScanScore. SeverityCounts SHALL tener LOW/MEDIUM/HIGH/CRITICAL incluidos ceros y sumar findings.length; sus valores SHALL coincidir con Findings, sin recalcular severidades. Los contadores N/I SHALL corresponder a las cuatro evaluaciones finales. Las categorías afectadas SHALL ser derivables únicamente de categories con findingCount>0, sin otra lista persistida, peso, estado o lifecycle por categoría. Una categoría sin Finding SHALL conservar su score0/NO_FINDINGS y contadores N/I originales, sin etiquetarse segura.

#### Scenario: Global y breakdown copiados
- **WHEN** ScanScore contiene global76 y categorías75/15/0/0 para HIGH y LOW
- **THEN** reporting conserva global76 y categorías75/15/0/0, sin promediar ni sumar sus scores
- **AND** severityCounts y Findings coinciden exactamente

#### Scenario: Score cien y CRITICAL
- **WHEN** una fixture válida proporciona ScanScore100/CRITICAL y sus Findings originales
- **THEN** el informe conserva 100/CRITICAL y destaca el Finding mediante su orden
- **AND** no habilita asignación automática CRITICAL ni interpreta cien como probabilidad

### Requirement: RP-06 Incertidumbre y ausencia de detecciones sin falsa seguridad

**Historia:** Como usuario, quiero distinguir ausencia de detecciones, abstención y fallo técnico.
**Dependencias:** EE-04/EE-07/EE-10, SC-02/SC-05, RP-02/RP-05; D-07/D-08/D-14.

Score0 SHALL significar «No se detectaron Findings en las pruebas completadas. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.». La interpretación SHALL explicar que score es heurístico, no porcentaje/probabilidad, y confidence es soporte de la conclusión, independiente del impacto, no probabilidad calibrada. NOT_VULNERABLE SHALL limitarse al test concreto; INCONCLUSIVE SHALL ser falta de conclusión suficiente y permanecer visible en contadores y filas de evaluación, con el motivo original. SHALL NOT equipararlos, convertir errores técnicos en I ni usar «sin riesgos», «aplicación segura» o «0% riesgo» como conclusiones del informe.

Un fallo del componente LLM con evaluación COMPLETED SHALL conservar llmStatus/llmErrorReason/degraded y verdict final original; SHALL NOT afirmarse éxito del componente ni fallo técnico global por ese solo dato. Un Scan FAILED nuevo SHALL carecer de informe completo según RP-13, conservando consultas anteriores de resultados técnicos y Findings; reporting SHALL NOT fabricar un informe parcial con I para simular finalización.

#### Scenario: Cuatro negativos concluyentes
- **WHEN** las cuatro evaluaciones son NOT_VULNERABLE y ScanScore0/NO_FINDINGS
- **THEN** el informe tiene Findings vacío, notVulnerableCount4 e inconclusiveCount0 y conserva la advertencia acotada de cero

#### Scenario: Cuatro inconclusos
- **WHEN** las cuatro evaluaciones son COMPLETED/INCONCLUSIVE
- **THEN** el informe disponible conserva score0, inconclusiveCount4 y los cuatro motivos, sin afirmar seguridad

#### Scenario: Mezcla y componente fallido
- **WHEN** existen VULNERABLE, NOT_VULNERABLE e INCONCLUSIVE con fallo LLM consolidado coherentemente
- **THEN** cada verdict, confidence, Finding y código del componente permanece original y los contadores distinguen las tres conclusiones

### Requirement: RP-07 Limitaciones explícitas y acotadas

**Historia:** Como lector, quiero conocer el alcance real y las restricciones del análisis antes de interpretar el score.
**Dependencias:** ADR-006/ADR-007, RP-02/RP-06; D-09/D-24.

Todo report-v1 SHALL incluir exactamente nueve limitaciones {code,text}, con textos es inmutables de D-09 y este orden: IMPLEMENTED_CATEGORIES_ONLY, TARGET_REFERENCE_ONLY, POINT_IN_TIME_TESTS, LLM10_OUTPUT_ONLY, POSSIBLE_FALSE_RESULTS, INCONCLUSIVE_RESULTS, HEURISTIC_METRICS, EXCLUDED_CAPABILITIES, SYNTHETIC_SCENARIO_SCOPE. SHALL describir respectivamente las cuatro categorías sin cobertura completa, referencia de configuración sin URL histórica reconstruible, pruebas puntuales/no auditoría exhaustiva, output observable sin sink, posibles falsos positivos/negativos, inconclusos no seguros, score/confidence heurísticos, RAG/agentes/tools/streaming fuera de alcance y restricciones sintéticas de evaluación. Las limitaciones SHALL estar presentes también sin Findings o sin inconclusos; SHALL NOT añadir recomendaciones nuevas ni afirmar experimento/calibración ejecutados.

#### Scenario: Limitaciones sin detecciones
- **WHEN** se genera un informe con score0 y ningún inconcluso
- **THEN** aparecen las nueve limitaciones en el orden definido, incluida OUTPUT_ONLY y ausencia de cobertura exhaustiva

### Requirement: RP-08 Generación prospectiva y finalización atómica

**Historia:** Como usuario, quiero que un Scan nuevo COMPLETED tenga todos sus resultados disponibles al consultar.
**Dependencias:** SC-07, EE-10, RP-01..RP-07; D-12/D-13/D-17.

La creación de nuevos scans SHALL fijar report-v1 como versión requerida junto con scoring-v1 y evaluation-engine-v1 antes del dispatch, sin opción de omisión pública ni nuevos campos/estados de Scan. Tras finales/Findings confirmados, ScanScore SHALL insertarse y el report SHALL construirse desde ese resultado insertado, dentro de la misma transacción que confirma ambos y RUNNING→COMPLETED. No SHALL existir commit intermedio entre score, report y cierre. Una lectura coherente SHALL observar ausencia durante procesamiento o ambos resultados confirmados con COMPLETED, nunca un cierre parcial. generatedAt SHALL ser UTC con microsegundos, no anterior a calculatedAt ni inputs; Scan.finishedAt SHALL ser no anterior a generatedAt. Históricos sin marcador de reporting SHALL conservar su semántica anterior.

#### Scenario: Cierre íntegro
- **WHEN** la generación y persistencia de report-v1 terminan correctamente
- **THEN** ScanScore, SecurityReport y Scan COMPLETED se confirman conjuntamente y pueden consultarse
- **AND** Scan conserva sus siete campos, cuatro estados y motivos de fallo

#### Scenario: Rollback después de score o report
- **WHEN** falla la operación tras insertar score, tras insertar report o al confirmar la transición
- **THEN** ninguna de las nuevas filas queda confirmada ni se anuncia COMPLETED

### Requirement: RP-09 Fallos internos técnicos y compensación

**Historia:** Como operador, quiero que un error de reporting no altere los diagnósticos ni se oculte como éxito.
**Dependencias:** SC-08, RP-08; D-14/D-16/D-24.

Error de datos/versión, construcción, textos controlados inválidos, tamaño o escritura/commit SHALL revertir score/report/cierre y aplicar compensación técnica existente FAILED/SCAN_PROCESSING_FAILED en transacción independiente. Reporting SHALL NOT modificar finales/Findings ya confirmados, inventar score0/I, añadir motivos públicos de Scan ni completar sin report. Si falla también compensación SHALL conservar el último estado durable y aplicar la compuerta existente hasta recuperación. Commit ambiguo SHALL resolverse mediante estado durable: si el conjunto se confirmó, conservarlo; si no se confirmó, compensar condicionalmente; si no puede determinarse ni compensarse, cerrar admisión sin relanzar trabajo.

#### Scenario: Error de generación o persistencia
- **WHEN** la generación lanza error o falla el insert de report de forma conocida
- **THEN** score/report/cierre revierten y se intenta FAILED/SCAN_PROCESSING_FAILED, conservando Findings/evaluaciones

#### Scenario: Commit confirmado ambiguamente y compensación indisponible
- **WHEN** se pierde la confirmación del commit o también falla compensar
- **THEN** se preserva cualquier terminal confirmado y nunca se repiten target/LLM ni se sobrescriben resultados
- **AND** la admisión queda bloqueada cuando no puede confirmarse una salida durable segura

### Requirement: RP-10 Versionado e idempotencia inmutable

**Historia:** Como investigador, quiero conservar el mismo informe histórico aunque se repita el cierre o cambie la presentación futura.
**Dependencias:** SC-09, RP-02/RP-03/RP-08; D-11/D-15.

El sistema SHALL garantizar una única fila por scanId+reportVersion y un reportId único estable. El resultado, generatedAt y payload SHALL ser inmutables, sin upsert de actualización. Un cierre duplicado sobre terminal coherente SHALL reconocer el snapshot existente sin generar de nuevo ni cambiar fechas. Una comparación interna con candidato ya construido SHALL exigir iguales contenidos/versiones, excluyendo el UUID/instante nuevos propuestos; divergencia SHALL ser error de integridad, sin overwrite. No SHALL calcular/persistir hash nuevo.

report-v1 SHALL congelar esquema, campos, idioma de presentación propia, textos/labels de presentación, orden y semántica, independientemente de scoringVersion y versiones de evaluación. Cambiarlos SHALL requerir nueva reportVersion y otro change aprobado, conservando lectura/presentación de v1. Conservar el idioma original de un Finding SHALL NOT exigir otra reportVersion: es copia de fuente según RP-03, no cambio de la presentación propia. No SHALL incluir regeneración pública, selector de versión o sustitución retrospectiva de un report.

#### Scenario: Cierres concurrentes
- **WHEN** dos cierres compiten o se reentrega el mismo trabajo confirmado
- **THEN** existe un reportId/instante/payload original y una fila por versión, sin regeneración ni cambio de terminal

#### Scenario: Contenido divergente y evolución
- **WHEN** un candidato interno difiere del snapshot o se pretende cambiar textos/campos bajo report-v1
- **THEN** el primero se rechaza sin sobrescritura y lo segundo exige otra versión aprobada

### Requirement: RP-11 Recuperación local sin informes retrospectivos

**Historia:** Como operador, quiero resolver caídas sin informes duplicados o estados finales incoherentes.
**Dependencias:** EE-11, SC-10, RP-08..RP-10; D-16/D-19.

Recovery SHALL conservar la política de una instancia: antes de admisión, interrumpir activos a FAILED/SCAN_INTERRUPTED con cierre atómico de hijos activos, preservar terminales y no generar report/score ni relanzar red. Caída antes de T3, durante generación, después de insert no confirmado o antes del cierre SHALL dejar al activo sin score/report confirmados. Tras commit conjunto SHALL conservar ambos y COMPLETED. Report durable con Scan activo/FAILED, marcador/versión/vínculos incorrectos, report sin score requerido o COMPLETED marcado sin report SHALL ser error de integridad, sin reparación/backfill/borrado automáticos. Fallo de validación/recuperación SHALL impedir abrir admisión.

#### Scenario: Caídas antes y después de commit
- **WHEN** se reinicia tras Findings, durante generación, tras insert no confirmado o después de commit conjunto
- **THEN** los tres primeros activos se interrumpen sin report/score nuevos y el último conserva report/score/COMPLETED
- **AND** ningún caso inicia generación ni llamada externa desde recovery

#### Scenario: Estado durable incoherente
- **WHEN** existe un report confirmado sin cierre/score coherentes o falta report en un COMPLETED marcado
- **THEN** se diagnostica integridad inválida, la consulta devuelve 500 sanitizado y recovery no repara ni admite nuevo trabajo

### Requirement: RP-12 Persistencia V7 conservadora e históricos

**Historia:** Como mantenedor, quiero introducir informes sin modificar resultados aprobados o atribuirles una presentación que nunca tuvieron.
**Dependencias:** V1–V6, RP-02/RP-08/RP-10; D-17/D-18/D-19.

Una nueva V7 SHALL añadir almacenamiento de SecurityReport y marcador nullable de versión requerida en Scan, sin modificar V1–V6 ni fuentes/global_score. SHALL conservar ids/FKs, unicidad por Scan+versión, UTC, locale, payload de esquema cerrado y tamaño máximo; las restricciones SHALL rechazar NULL indebido, JSON/tipos/valores inválidos y metadata duplicada discordante. Históricos V6, incluso COMPLETED con score/Findings o FAILED, SHALL mantener marcador null y ninguna fila de report creada. SHALL aceptar datos V6 legítimos sin exigir tablas vacías ni inventar informes. No SHALL ejecutar migraciones durante planificación.

#### Scenario: V6 con datos reales válidos del producto
- **WHEN** se aplica V7 posteriormente sobre fixture dedicada V6 con scores, Findings y global_score histórico
- **THEN** sus datos/fechas/relaciones permanecen iguales, report_version es null y security_reports está vacía

#### Scenario: Escritura inválida
- **WHEN** se intenta insertar relación inexistente, clave duplicada, versión/locale inválidos, payload excesivo o metadata discordante
- **THEN** se rechaza la escritura sin modificar filas originales

### Requirement: RP-13 Consulta scoped y disponibilidad específica

**Historia:** Como usuario, quiero consultar un informe o comprender por qué aún no existe.
**Dependencias:** EE-13, SC-12, RP-08/RP-11/RP-12; D-20/D-21/D-22.

GET `/api/projects/{projectId}/scans/{scanId}/report` SHALL devolver 200 con exactamente {availability,report}, en una instantánea coherente del marcador/estado/score/report. Reporting SHALL tener disponibilidad propia, derivada sin estado persistido adicional:

- AVAILABLE: existe SecurityReport íntegro de la versión requerida y coherente con score+Scan COMPLETED; report SHALL contener el snapshot incluso si summary.globalScore=0.
- PENDING: Scan sujeto a reporting, PENDING/RUNNING y sin report; report SHALL ser null.
- UNAVAILABLE: Scan sujeto a reporting terminado FAILED sin report; report SHALL ser null.
- NOT_APPLICABLE: histórico con report_version null y sin report, independientemente de que tenga scoring o estado FAILED; report SHALL ser null.

Invariante rota/versión desconocida/payload inválido o fallo de lectura SHALL devolver 500 INTERNAL_ERROR, nunca una availability normal. SHALL validar ambos UUID antes de repositorio con precedencia INVALID_PROJECT_ID/INVALID_SCAN_ID, después Project y Scan scoped; 404 PROJECT_NOT_FOUND/SCAN_NOT_FOUND incluido ajeno. Errores SHALL ser {code,message,fieldErrors:[]} sanitizados en español. Todas las respuestas SHALL usar Cache-Control:no-store. Consulta SHALL ser de solo lectura, sin ensamblar de nuevo, consultar target/proveedor, generar datos o requerir claves. No SHALL ampliar Scan, añadir POST de generación, formatos/exportación ni publicaciones públicas.

#### Scenario: Cuatro disponibilidades
- **WHEN** se consultan un nuevo COMPLETED con report cero, uno nuevo activo, uno nuevo FAILED antes del plan y un histórico V6 COMPLETED con score
- **THEN** devuelven AVAILABLE con objeto, PENDING/null, UNAVAILABLE/null y NOT_APPLICABLE/null respectivamente

#### Scenario: Histórico fallido y consulta concurrente
- **WHEN** se consulta un histórico FAILED o se consulta un nuevo Scan mientras T3 confirma
- **THEN** el primero es NOT_APPLICABLE/null y el segundo observa PENDING o AVAILABLE completos, sin falsa corrupción por lecturas cruzadas

#### Scenario: Errores y scoping
- **WHEN** los UUID son inválidos, Project falta, Scan es ajeno o falla almacenamiento
- **THEN** se aplican 400 con precedencia, 404 correspondiente o 500, sin datos internos ni falsa ausencia

### Requirement: RP-14 Privacidad, límites y formatos futuros

**Historia:** Como responsable del proyecto, quiero un snapshot acotado que pueda presentarse sin revelar contenido sensible.
**Dependencias:** EE-06/EE-13, RP-01/RP-02; D-22/D-23/D-24.

Reporting SHALL usar proyecciones de campos permitidos y tipos propios, sin cargar evidencia ni serializar entidades completas. evaluations SHALL respetar exclusivamente los diecinueve campos de RP-02 y los siete identificadores de versions: no SHALL convertirse en una copia de EvaluationResult. SHALL NOT incluir LlmAssessment completo, reasoning interno, prompts, schemas internos, señales deterministas internas, provider raw response, evidencia raw, chain-of-thought, secretos, auth/request headers, credenciales/ciphertext, API keys, respuestas raw, contexto reservado ni errores libres. No SHALL añadir señales sin un campo público expresamente aprobado; report-v1 no tiene tal campo. Solo SHALL copiar los cuatro textos de Finding controlados/sanitizados por evaluación literalmente en su idioma original y los textos propios es de presentación confiable; todos SHALL ser texto inerte, nunca HTML/Markdown ejecutado. SHALL NOT pretender que regex/redacción garantizan ausencia de secretos arbitrarios ni reescribir un Finding inválido para publicarlo; datos inválidos SHALL hacer fallar generación/lectura.

Report-v1 SHALL admitir como máximo cuatro Findings, exactamente cuatro evaluaciones y cuatro categorías. El límite de Findings deriva del plan MVP vigente: cuatro SecurityTests → una evaluación por categoría → máximo un Finding por evaluación → máximo cuatro Findings por Scan. SHALL NOT tratarse como propiedad universal de Reporting ni aplicarse como recorte. Si futuras versiones incorporan múltiples tests por categoría, SHALL revisarse/versionarse el límite y el presupuesto de tamaño mediante otro change aprobado, sin ampliar silenciosamente report-v1. title SHALL mantener 1..150 puntos de código y description/evidenceSummary/recommendation 1..1000, no blancos y con las restricciones originales de Finding. Codes/versions SHALL cumplir las cotas de D-23. Payload JSONB y SecurityReport serializado UTF-8 SHALL ser cada uno <=262144 bytes (256 KiB), sin truncamiento; el wrapper HTTP SHALL añadir como máximo512 bytes. Las nueve limitaciones y cuatro interpretaciones SHALL ser constantes de presentación v1, sin textos arbitrarios del cliente. Logs/toString/errores SHALL limitarse a ids/códigos/tiempos/contadores, sin payload ni textos. Modelo SHALL ser independiente de renderer/PDF y no implementar exportador ni dependencias nuevas en MVP. El endpoint SHALL permanecer en el ámbito local autorizado del proyecto; scoping SHALL NOT presentarse como autenticación avanzada o permiso para publicación pública.

#### Scenario: Contenido sensible fuera de la proyección
- **WHEN** las filas origen contienen fixtures sintéticas de LlmAssessment completo, reasoning interno, prompts, schemas internos, señales deterministas internas, raw evidence, chain-of-thought, headers, ciphertext y output bruto de proveedor
- **THEN** esos campos no se leen ni aparecen en report, logs o errores, y solo se copian los campos permitidos de RP-02
- **AND** evaluations mantiene diecinueve campos, con siete identificadores en versions, sin adjuntar el resultado de evaluación completo

#### Scenario: Límites exactos y excedidos
- **WHEN** un texto alcanza su límite permitido o lo supera, un payload alcanza256KiB o lo excede, o se intenta generar report-v1 con cinco Findings
- **THEN** el límite exacto se admite si cumple el resto del esquema y el exceso falla técnicamente sin truncar ni cambiar fuentes
- **AND** el quinto Finding causa error sin recorte silencioso; ampliar el plan a múltiples tests por categoría exige revisar/versionar el contrato, no descartar Findings para mantener el límite

#### Scenario: Texto inerte y sin exportador
- **WHEN** un consumidor obtiene el JSON estructurado con recomendaciones
- **THEN** los textos son datos, no instrucciones ejecutadas, y no se genera archivo HTML/Markdown/PDF ni llamada LLM
