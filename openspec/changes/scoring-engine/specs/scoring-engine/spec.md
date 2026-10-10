## Purpose

Resumir de forma determinista y versionada el riesgo detectado en un Scan, conservando la visibilidad de los Findings, la incertidumbre de las evaluaciones y las limitaciones de las pruebas ejecutadas.

## Review Status

Contrato **READY FOR IMPLEMENTATION**. D-01..D-17 y SC-01..SC-13 aprobados expresamente el 2026-10-10 con los ajustes de no usar input hash, availability explícita y breakdown sin lifecycle independiente por categoría. Los SHALL describen el comportamiento aprobado para implementación posterior; esta entrega no lo implementa.

## ADDED Requirements

### Requirement: SC-01 Inputs finales completos y coherentes

**Historia:** Como usuario, quiero un resumen basado en resultados confirmados para evitar puntuaciones parciales o inventadas.
**Prioridad:** MUST, MVP.
**Dependencias:** STE-01/STE-10, EE-01/EE-08/EE-10; D-03/D-10/D-16.

El sistema SHALL consumir exclusivamente Findings persistidos del Scan, sus EvaluationResults finales y las identidades/categorías del plan persistido. Para scoring-v1 de producción SHALL exigir exactamente las cuatro pruebas aprobadas, una por LLM01/LLM02/LLM08/LLM10, cuatro TestExecutions COMPLETED y cuatro EvaluationResults COMPLETED correspondientes uno a uno. Cada VULNERABLE SHALL tener exactamente un Finding coherente en Scan, ejecución, test, evaluación, categoría, severity y confidence. NOT_VULNERABLE e INCONCLUSIVE SHALL tener cero Findings. Confidence SHALL ser decimal válido 0..1 y severity una de LOW/MEDIUM/HIGH/CRITICAL para Findings. El sistema SHALL rechazar ausencias, duplicados, relaciones ajenas, valores inválidos y resultados requeridos no COMPLETED; SHALL NOT deduplicar silenciosamente, adivinar verdicts ni producir score parcial.

#### Scenario: Plan completo sin Findings
- **WHEN** existen las cuatro ejecuciones y evaluaciones COMPLETED y ninguna evaluación es VULNERABLE
- **THEN** se admite el cálculo con cero Findings y se conservan separados NOT_VULNERABLE e INCONCLUSIVE

#### Scenario: EvaluationResult ausente o técnicamente fallido
- **WHEN** falta un resultado esperado o está PENDING, RUNNING o FAILED, aunque su verdict sea INCONCLUSIVE
- **THEN** no se produce ScanScore y la finalización falla técnicamente según SC-08

#### Scenario: Finding duplicado o incoherente
- **WHEN** dos Findings comparten evaluación/ejecución o existe una diferencia con la decisión final vinculada
- **THEN** se rechaza el conjunto sin sumar ni escoger una de las filas

### Requirement: SC-02 Escala y nivel agregado independientes de severity

**Historia:** Como usuario, quiero interpretar el resumen sin confundir impacto individual con probabilidad de compromiso.
**Prioridad:** MUST, MVP.
**Dependencias:** ADR-005, SC-01; D-01/D-02/D-09.

El score SHALL ser entero de 0 a 100, donde mayor valor significa mayor riesgo detectado según la heurística; SHALL NOT presentarse como porcentaje, probabilidad de compromiso, CVSS ni certificación. RiskLevel SHALL ser un tipo distinto de Finding Severity con valores NO_FINDINGS para 0, LOW para 1..39, MEDIUM para 40..69, HIGH para 70..89 y CRITICAL para 90..100. Score 0/NO_FINDINGS SHALL significar «ningún Finding detectado en las pruebas completadas», no «aplicación segura»; SHALL NOT resolver la incertidumbre ni acreditar cobertura exhaustiva. El score SHALL NOT sustituir, eliminar, modificar ni ocultar Findings individuales.

#### Scenario: Score cero con incertidumbre
- **WHEN** no hay Findings y las cuatro evaluaciones son INCONCLUSIVE
- **THEN** score=0, riskLevel=NO_FINDINGS e inconclusiveCount=4
- **AND** el contrato explica que no se confirmó ausencia de vulnerabilidades

#### Scenario: Riesgo agregado e impacto individual
- **WHEN** un Scan con un Finding HIGH se consulta
- **THEN** el resumen tiene riskLevel HIGH y el Finding conserva severity HIGH y su confidence original

### Requirement: SC-03 Pesos y ajuste limitado de confidence

**Historia:** Como investigador, quiero una política explícita y estable que mantenga separados impacto y confianza.
**Prioridad:** MUST, MVP.
**Dependencias:** EE-04, SC-02; D-03/D-04/D-06.

scoring-v1 SHALL asignar bases LOW=10, MEDIUM=40, HIGH=70 y CRITICAL=90. Por Finding SHALL añadir `bonus=ROUND_HALF_UP(5*confidence)` a entero, en decimal exacto, y usar `w=base+bonus`. El bonus SHALL pertenecer a 0..5; confidence SHALL NOT multiplicar ni descontar la base ni reclasificar severity. Solo la confidence final persistida SHALL influir, nunca llmConfidence, suggestedSeverity ni señales parciales. Una confidence inválida SHALL producir fallo técnico, no clamp silencioso. La política SHALL aceptar CRITICAL como entrada válida sin habilitar su asignación automática por el evaluador MVP.

#### Scenario: Confidence alta y baja en HIGH
- **WHEN** se puntúa individualmente un HIGH con confidence 0.00, 0.60 y 0.95
- **THEN** los scores respectivos son 70, 73 y 75 y todos tienen nivel HIGH

#### Scenario: Redondeo decimal reproducible
- **WHEN** confidence es 0.09, 0.10, 0.29, 0.30, 0.49, 0.50, 0.69, 0.70, 0.89, 0.90 o 1.00
- **THEN** el bonus respectivo es 0, 1, 1, 2, 2, 3, 3, 4, 4, 5 o 5 sin diferencias por representación binaria

### Requirement: SC-04 Agregación amortiguada con floors y cap

**Historia:** Como usuario, quiero que el riesgo más grave prevalezca y que varios hallazgos no generen crecimiento ilimitado.
**Prioridad:** MUST, MVP.
**Dependencias:** SC-03; D-05/D-06.

Para un conjunto F vacío scoring-v1 SHALL devolver 0. Para F no vacío SHALL calcular `B=max(w)`, `R=sum(w)-B`, `increment=min(10,floor(R/10))` y `score=min(100,B+increment)`. Los resultados NOT_VULNERABLE e INCONCLUSIVE SHALL NOT entrar en F ni en denominadores ni restar puntuación. El resultado SHALL ser independiente del orden; un HIGH SHALL imponer score>=70 y un CRITICAL score>=90. Agregar Findings válidos SHALL NOT reducir score; agregar resultados no vulnerables/inconclusos SHALL NOT reducirlo. El incremento por todos los demás Findings SHALL limitarse a diez puntos. Con solo LOW, el máximo SHALL ser 25; con severity máxima MEDIUM, 55; con máxima HIGH, 85; con algún CRITICAL, 100. No SHALL aplicarse media simple, suma ilimitada ni deduplicación por similitud textual.

#### Scenario: Un Finding por severity
- **WHEN** se calcula por separado un LOW, MEDIUM, HIGH o CRITICAL con confidence 0.90
- **THEN** los scores respectivos son 15, 45, 75 y 95

#### Scenario: Múltiples LOW y límite de acumulación
- **WHEN** la política pura recibe tres, cuatro o veinte LOW con confidence 0.90
- **THEN** produce respectivamente 18, 19 o 25, todos LOW y menores que un HIGH con confidence cero
- **AND** veinte Findings son un caso de prueba de la fórmula, no una ampliación del plan MVP de cuatro

#### Scenario: HIGH no diluido por LOW ni por resultados negativos
- **WHEN** existe un HIGH y un LOW, ambos con confidence 0.90, y las otras dos evaluaciones son NOT_VULNERABLE
- **THEN** score=76 y riskLevel=HIGH
- **AND** agregar negativos no cambia la puntuación ni oculta el HIGH

#### Scenario: CRITICAL no diluido y cap cien
- **WHEN** hay un CRITICAL con confidence 0.00 o un CRITICAL y un HIGH ambos con confidence 0.90
- **THEN** sus scores respectivos son 90 y 100, con riskLevel CRITICAL
- **AND** ninguna combinación supera 100

#### Scenario: Orden y empate
- **WHEN** se permutan los mismos Findings o dos tienen igual peso máximo
- **THEN** B, R, score y nivel son idénticos y solo se resta un máximo en R

#### Scenario: Fronteras de amortiguación y saturación
- **WHEN** R es 0, 10, 99, 100 o 101
- **THEN** el incremento respectivo es 0, 1, 9, 10 o 10
- **AND** B=95 con R=45, 50 o 60 produce respectivamente score 99, 100 o 100

### Requirement: SC-05 Incertidumbre y resultados no vulnerables explícitos

**Historia:** Como usuario, quiero conocer las abstenciones del análisis para no interpretar un número bajo como seguridad.
**Prioridad:** MUST, MVP.
**Dependencias:** EE-04/EE-07, SC-01/SC-02; D-07/D-09.

El sistema SHALL contar los verdicts finales INCONCLUSIVE de evaluaciones COMPLETED por Scan y categoría, separados de NOT_VULNERABLE. INCONCLUSIVE SHALL NOT sumar riesgo directo, crear Findings, convertirse en NOT_VULNERABLE ni alterar severity/confidence. El resumen SHALL incluir evaluationCount, findingCount, notVulnerableCount e inconclusiveCount, cumpliendo `evaluationCount=findingCount+notVulnerableCount+inconclusiveCount`. Un error del componente LLM consolidado en COMPLETED SHALL usar su verdict final: VULNERABLE aporta su Finding y un INCONCLUSIVE aumenta su contador. Un EvaluationResult FAILED SHALL impedir el score, aunque contenga verdict técnico INCONCLUSIVE. El contrato SHALL explicar que NOT_VULNERABLE solo describe el test ejecutado y que cero inconclusos no acredita cobertura completa de una categoría/aplicación.

#### Scenario: Mezcla de verdicts
- **WHEN** las cuatro evaluaciones son VULNERABLE LOW/0.90, NOT_VULNERABLE, INCONCLUSIVE e INCONCLUSIVE
- **THEN** score=15, evaluationCount=4, findingCount=1, notVulnerableCount=1 e inconclusiveCount=2

#### Scenario: Ninguna detección y negativos concluyentes
- **WHEN** las cuatro evaluaciones son NOT_VULNERABLE
- **THEN** score=0, NO_FINDINGS, notVulnerableCount=4 e inconclusiveCount=0
- **AND** no se afirma que las categorías estén completamente seguras

### Requirement: SC-06 Resumen por categoría sin pesos arbitrarios

**Historia:** Como usuario, quiero localizar categorías afectadas sin promediar riesgos ni esconder incertidumbre.
**Prioridad:** MUST, MVP.
**Dependencias:** SC-04/SC-05; D-08.

El resumen SHALL contener exactamente cuatro entradas ordenadas LLM01, LLM02, LLM08 y LLM10. Cada entrada SHALL tener category, score, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount y severityCounts con claves LOW/MEDIUM/HIGH/CRITICAL, incluidos ceros. SHALL aplicar la misma fórmula al subconjunto de Findings de la categoría. Sin Findings SHALL devolver 0/NO_FINDINGS conservando las abstenciones. El score global SHALL calcularse sobre todos los Findings, no sobre media/suma de scores por categoría; SHALL ser al menos el máximo de sus scores. Los contadores globales SHALL ser las sumas de los de categoría. No SHALL asignar pesos diferentes a categorías ni interpretar una prueba como cobertura OWASP completa; LLM10 SHALL conservar el alcance OUTPUT_ONLY.

El breakdown SHALL formar parte del resultado estructurado e inmutable de ScanScore, confirmado y consultado conjuntamente. Las entradas de categoría SHALL NOT tener identidad, estados, fechas, versión, escritura, API ni recalculado independientes. El sistema SHALL NOT introducir agregados de dominio, repositorios, tablas o recuperación específicos por categoría; una entrada incompleta/incoherente SHALL invalidar el conjunto, no constituir un resultado parcial publicable.

#### Scenario: Categoría inconclusa y categoría vulnerable
- **WHEN** LLM01 tiene un HIGH/0.90 y LLM02 un INCONCLUSIVE, con LLM08/LLM10 NOT_VULNERABLE
- **THEN** LLM01 tiene 75/HIGH, LLM02 tiene 0/NO_FINDINGS e inconclusiveCount=1 y el global es 75/HIGH

#### Scenario: Global no promediado
- **WHEN** LLM01 tiene HIGH/0.90 y LLM02 LOW/0.90, sin otros Findings
- **THEN** sus scores son 75 y 15 y el global es 76, no 45 ni 90

#### Scenario: Breakdown indivisible
- **WHEN** falla la construcción o persistencia de una entrada de categoría
- **THEN** no se publica ni confirma un ScanScore parcial
- **AND** ninguna categoría se completa, reintenta o recupera por separado

### Requirement: SC-07 Cálculo y COMPLETED prospectivos y atómicos

**Historia:** Como usuario, quiero que un Scan nuevo completado incluya su resultado de scoring confirmado.
**Prioridad:** MUST, MVP.
**Dependencias:** SE-05, STE-10, EE-10, SC-01..SC-06; D-10/D-14.

La creación de cada Scan nuevo tras adoptar el change SHALL fijar internamente scoringVersion requerida=scoring-v1 junto con evaluation-engine-v1 antes del dispatch. Scan SHALL permanecer RUNNING hasta validar los inputs, calcular y confirmar el score. La inserción de ScanScore y RUNNING→COMPLETED SHALL confirmarse atómicamente después de Findings/evaluaciones: ninguna lectura SHALL observar un Scan marcado COMPLETED sin su score requerido ni un score nuevo con el cierre sin confirmar. COMPLETED SHALL exigir cuatro ejecuciones/evaluaciones COMPLETED y un score coherente de la versión fijada. Vulnerabilidad/INCONCLUSIVE SHALL seguir permitiendo COMPLETED; no SHALL añadirse estados ni campos al DTO de Scan. Históricos sin marcador de scoring SHALL conservar la semántica previa, incluso con evaluación ya disponible.

#### Scenario: Flujo completo
- **WHEN** el plan nuevo está íntegro y la transacción de cierre confirma
- **THEN** score y Scan COMPLETED son consultables conjuntamente
- **AND** los siete campos de Scan y sus cuatro estados se conservan

#### Scenario: Caída después del insert antes del cierre
- **WHEN** el proceso cae dentro de la transacción tras insertar/flush del score y antes de confirmar COMPLETED
- **THEN** no queda score confirmado aislado y Scan permanece en su último estado durable

### Requirement: SC-08 Fallos internos técnicos sin falsos INCONCLUSIVE

**Historia:** Como operador, quiero distinguir un error del scanner de incertidumbre sobre el target.
**Prioridad:** MUST, MVP.
**Dependencias:** SE-07, EE-07/EE-10, SC-01/SC-07; D-11.

Un error de cálculo, input inconsistente, versión no soportada o fallo de escritura/commit SHALL impedir confirmar el score/cierre como éxito. SHALL intentarse el cierre técnico FAILED con SCAN_PROCESSING_FAILED en transacción independiente, conservando evaluaciones/Findings terminales. SHALL NOT crear INCONCLUSIVE, modificar decisiones existentes, fabricar score=0 ni añadir motivos públicos a Scan. Si también falla la compensación SHALL conservar el estado confirmado y bloquear nuevos inicios/reclamaciones mediante la compuerta existente hasta recuperación. Un commit ambiguo SHALL NOT justificar repetir trabajo externo ni reabrir un terminal; si se confirmó conjuntamente score+COMPLETED SHALL conservar ambos.

#### Scenario: Error de cálculo o persistencia
- **WHEN** el cálculo lanza un error o el insert/commit falla de forma conocida
- **THEN** score/cierre revierten y se intenta FAILED/SCAN_PROCESSING_FAILED
- **AND** los Findings previos permanecen consultables y no se inventa incertidumbre semántica

#### Scenario: Persistencia y compensación indisponibles
- **WHEN** tampoco se confirma FAILED
- **THEN** se conserva el último estado durable y se aplica la compuerta y recuperación existentes

### Requirement: SC-09 Idempotencia y resultados inmutables

**Historia:** Como investigador, quiero que repetir una operación no cambie el resultado histórico ni cree duplicados.
**Prioridad:** MUST, MVP.
**Dependencias:** SC-07/SC-08; D-12/D-17.

El sistema SHALL garantizar una única fila por `(scanId, scoringVersion)`. Un resultado confirmado SHALL ser inmutable, incluido calculatedAt; sus fuentes terminales persistidas SHALL permanecer inmutables. Una repetición interna con los mismos inputs y versión SHALL devolver el resultado existente sin sobrescribirlo. Si existe un candidato ya calculado en una operación interna duplicada, una discrepancia entre sus valores derivados y los del resultado confirmado SHALL ser error de integridad, no upsert de actualización; SHALL NOT requerir hash o snapshot duplicado de inputs. La repetición sobre Scan terminal SHALL NOT cambiar su estado ni generar otro score. No SHALL existir recalculado HTTP, cálculo al consultar, backfill ni sustitución de un histórico en MVP. Una fórmula/constante/redondeo/semántica distinta SHALL requerir nueva scoringVersion y otro change aprobado; SHALL NOT reinterpretar filas scoring-v1.

#### Scenario: Entrega duplicada
- **WHEN** dos cierres de la misma versión compiten o se repite una operación ya confirmada
- **THEN** persiste una sola fila con el mismo resultado y calculatedAt y no cambia el terminal

#### Scenario: Resumen derivado divergente
- **WHEN** una operación interna duplicada presenta para scanId/versión un candidato con valores derivados distintos del score confirmado
- **THEN** se señala integridad inválida sin reemplazar el score

### Requirement: SC-10 Recuperación local sin relanzamiento

**Historia:** Como operador, quiero resolver caídas sin duplicar análisis ni inventar scores retrospectivos.
**Prioridad:** MUST, MVP.
**Dependencias:** SE-08, STE-12, EE-11, SC-07..SC-09; D-13.

La recuperación de una única instancia SHALL conservar la política aprobada: cerrar activos como interrumpidos antes de admitir trabajo, conservar terminales/Findings y no relanzar target/evaluador ni calcular scores. Tras caída después de Findings, durante cálculo o antes de commit de cierre, el Scan activo SHALL terminar FAILED/SCAN_INTERRUPTED y sin score confirmado. Tras commit de score+COMPLETED, ambos SHALL permanecer intactos. Una situación durable marcada COMPLETED sin score, o score de este flujo asociado a Scan no COMPLETED, SHALL considerarse integridad inválida, no ausencia normal ni autorización para completar/recalcular. No SHALL introducir colas, leases, reintentos o reparación automática.

#### Scenario: Reinicio antes y después de commit
- **WHEN** se reinicia con un Scan activo con Findings, otro con insert de score no confirmado y otro con score+cierre confirmados
- **THEN** los dos primeros quedan interrumpidos sin score y conservando Findings terminales
- **AND** el tercero conserva score y COMPLETED sin nuevas llamadas ni duplicados

### Requirement: SC-11 Persistencia versionada y migración conservadora

**Historia:** Como mantenedor, quiero incorporar scoring sin alterar datos históricos ni migraciones aplicadas.
**Prioridad:** MUST, MVP.
**Dependencias:** V1–V5, SC-07/SC-09; D-14/D-16/D-17.

El sistema SHALL incorporar V6 nueva para scan_scores y la versión interna nullable requerida en scans, sin modificar V1–V5. SHALL persistir la identidad compuesta, calculatedAt UTC/microsegundos, score entero, riskLevel, contadores globales/por severity y breakdown estructurado por categoría descritos por SC-06/SC-13, con nulabilidad/rangos/integridad/uniqueness defensivos. SHALL NOT añadir input_hash, snapshot duplicado de inputs ni tablas de categoría en MVP. Los históricos SHALL conservar marcador null y cero filas nuevas de score. scans.global_score SHALL permanecer intacto y sin lecturas/escrituras/proyección/fallback, incluso si contiene un valor histórico. V6 SHALL aceptar datos semánticos válidos de V5, sin exigir findings vacía, sin borrar datos ni inventar scores. Fechas del score SHALL ser no anteriores al fin de sus inputs y no posteriores al finishedAt confirmado del Scan.

#### Scenario: V5 con Findings e históricos
- **WHEN** V6 se aplica posteriormente en una fixture dedicada con datos V5 válidos y global_score histórico
- **THEN** se conservan sus valores/fechas/relaciones, scoringVersion requerida es null y scan_scores está vacía
- **AND** la presencia de Findings legítimos no bloquea la migración

#### Scenario: Restricciones e inserciones inválidas
- **WHEN** se intenta persistir un score fuera de rango, contador negativo, versión vacía, relación inexistente o clave duplicada
- **THEN** se rechaza la escritura sin modificar resultados previos

### Requirement: SC-12 Consulta mínima scoped y ausencia explícita

**Historia:** Como usuario, quiero consultar el resumen y distinguirlo de un resultado todavía ausente o histórico.
**Prioridad:** MUST, MVP.
**Dependencias:** SE-06/SE-09, EE-13, SC-07/SC-10/SC-11; D-15.

GET `/api/projects/{projectId}/scans/{scanId}/score` SHALL devolver 200 con exactamente `{availability, score}`. availability SHALL describir existencia o ausencia del resultado, no un lifecycle adicional:

- AVAILABLE: existe ScanScore válido; score SHALL contener ese resultado, incluido cuando globalScore=0.
- PENDING: scan sujeto a scoring aún procesándose, en PENDING/RUNNING y sin ScanScore; score SHALL ser null.
- UNAVAILABLE: scan sujeto a scoring terminado FAILED sin ScanScore; score SHALL ser siempre null.
- NOT_APPLICABLE: scan histórico anterior a scoring-engine, identificado por marcador de scoring null y sin ScanScore; score SHALL ser siempre null, independientemente de su estado o de si ya tenía evaluaciones.

Ausencia SHALL NOT representarse con score numérico 0. La atomicidad de SC-07 SHALL garantizar que un ScanScore válido se confirma junto con Scan COMPLETED; availability SHALL NOT permitir omitir esa invariante. Un COMPLETED marcado sin score, una fila con versión/vínculo inválidos o un score durable asociado a Scan no COMPLETED SHALL producir 500 INTERNAL_ERROR, no availability normal ni reparación automática. Un fallo de lectura SHALL producir también 500, nunca UNAVAILABLE/NOT_APPLICABLE. SHALL validar ambos UUID antes de persistencia con precedencia INVALID_PROJECT_ID/INVALID_SCAN_ID, comprobar Project y después Scan scoped, responder 404 PROJECT_NOT_FOUND/SCAN_NOT_FOUND incluido ajeno y usar `{code,message,fieldErrors:[]}` sanitizado y Cache-Control no-store. La consulta SHALL ser de solo lectura y funcionar sin target/clave/proveedor; SHALL NOT exponer evidencia, snapshots, secretos, texto no confiable ni invocar cálculo. No SHALL incluir score en el DTO de Scan, reporting, POST de recálculo ni selección de versión pública.

#### Scenario: Cero real frente a datos ausentes
- **WHEN** se consultan un Scan nuevo COMPLETED sin Findings, uno RUNNING, uno FAILED y un histórico evaluation-engine-v1 sin scoring
- **THEN** se obtiene respectivamente AVAILABLE con score global 0, PENDING/null, UNAVAILABLE/null y NOT_APPLICABLE/null

#### Scenario: Recurso ajeno, inválido o almacenamiento fallido
- **WHEN** los UUID son inválidos, el Scan pertenece a otro Project o falla la lectura
- **THEN** se devuelve respectivamente 400 con precedencia estable, 404 SCAN_NOT_FOUND o 500 INTERNAL_ERROR
- **AND** no se sustituyen errores por score cero/ausencia ni se filtran datos internos

#### Scenario: COMPLETED inconsistente
- **WHEN** un Scan con scoring requerido está COMPLETED pero carece de su fila válida
- **THEN** la consulta devuelve 500 INTERNAL_ERROR, sin cálculo automático ni NOT_APPLICABLE

#### Scenario: Fallo anterior al plan y pertenencia histórica
- **WHEN** un Scan nuevo sujeto a scoring falla antes de preparar evaluaciones y otro histórico evaluado termina FAILED
- **THEN** el primero devuelve UNAVAILABLE/null y el segundo NOT_APPLICABLE/null
- **AND** la falta de datos no reclasifica un Scan nuevo como histórico

### Requirement: SC-13 Metadata reproducible y aislamiento académico

**Historia:** Como investigador, quiero reconstruir la puntuación sin volver a evaluar ni contaminar el experimento.
**Prioridad:** MUST, MVP.
**Dependencias:** EE-14, ADR-007, SC-01..SC-12; D-16/D-17.

ScanScore público SHALL contener exactamente scanId, scoringVersion, calculatedAt, globalScore, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount, severityCounts y categories. Los cuatro contadores por severity SHALL estar presentes incluidos ceros, sumar findingCount y corresponder a los Findings originales. La reproducibilidad SHALL basarse en inputs originales persistidos e inmutables vinculados por scanId, sus versiones y la política scoring-v1 conservada. El MVP SHALL NOT calcular, almacenar ni exponer input hash ni exigir un serializador canónico de esos inputs. calculatedAt SHALL reflejar la creación original, no la consulta/repetición. El scoring SHALL consumir el verdict final y los Findings existentes sin modificar evaluación, ground truth, señales, confidence, severity, proveedor o estrategia. SHALL NOT llamar LLM ni convertir el score en métrica de precision/recall o de cobertura; el assessment HYBRID_WITH_SIGNALS SHALL seguir sin equivaler a un experimento LLM-only.

#### Scenario: Reconstrucción y versión estable
- **WHEN** se repiten los mismos inputs persistidos con scoring-v1 y diferente orden de lectura
- **THEN** se reconstruyen el mismo score, nivel, contadores y breakdown a partir de esos inputs y scoringVersion, sin hash
- **AND** la consulta conserva calculatedAt y no produce llamadas ni cambios de evaluación

#### Scenario: Evolución futura de fórmula
- **WHEN** se propone cambiar pesos, redondeo, bonus, límites o reglas de agregación
- **THEN** se exige nueva versión y aprobación separada, conservando la interpretación de scoring-v1
