# improve-real-target-evaluation-context — validación de implementación

**10/10 tareas verificadas. READY FOR FINAL REVIEW.** Las correcciones de RTEC-04
para el span inerte (P2) y la proyección sin envío (P3) quedan verificadas en
PostgreSQL. La aceptación A/B y la regresión histórica conservan su **PASS**.

| Ejecución | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Evaluación dirigida offline | 21 | 21 | 0 | 0 | 0 |
| Suite backend offline `mvnw.cmd -o test` | 651 | 563 | 0 | 0 | 88 |
| HybridV2EvaluationTest (incluido en la suite) | 7 | 7 | 0 | 0 | 0 |
| HybridV2PostgresIntegrationTest (omitido en la suite offline) | 1 | 0 | 0 | 0 | 1 |
| HybridV2PostgresIntegrationTest (ejecución manual con PostgreSQL) | 1 | 1 | 0 | 0 | 0 |
| Regresión histórica 3.2 (ejecución manual con PostgreSQL) | 7 | 7 | 0 | 0 | 0 |

Java y javac 21.0.12.1. Las filas desglosadas no se suman al total. Los 88 skipped
no cuentan como PASS. Las integraciones PostgreSQL se omiten en el ejecutor sin
las variables de las bases de test. No hubo llamadas externas.

El operador aportó el resultado manual de `HybridV2PostgresIntegrationTest` con
**BUILD SUCCESS** y tiempo total Maven de **21.082 s**.

`openspec validate improve-real-target-evaluation-context --strict`: **PASS**.

## Regresión histórica 3.2

El operador confirmó **BUILD SUCCESS** el 2026-11-12 a las 23:53:42 CET, con
tiempo total Maven de **16.742 s**. La selección cubre el pipeline de las cuatro
categorías con `hybrid-v1`, omisión semántica por falta de contexto o corpus,
reconstrucción de `scoring-v1`, lectura e inmutabilidad de `report-v1`, recuperación
de informes confirmados y conservación del historial en las migraciones V4→V5 y
V6→V7→V8. Se registran únicamente los siete métodos seleccionados como pasados.

Comando ejecutado desde `backend` en la terminal de PostgreSQL de test:

```powershell
.\mvnw.cmd -o '-Dtest=EvaluationEnginePostgresIntegrationTest#fourEvaluationsFindingsMetadataEndpointsAndIdempotenceAreDurable+missingContextAndUnknownEvidenceNeverCallProviderOrCopyPrivateText,ScoringEnginePostgresIntegrationTest#zeroMixedAndInconclusiveRoundTripScopedApiAndNewInstanceReconstruction,ReportingPostgresIntegrationTest#zeroInconclusiveMixedLiteralTextsVersionsAndIndependentRoundTrip+recoveryPreservesConfirmedAndInterruptsUnconfirmedWithoutGenerating,ReportingMigrationIntegrationTest#v6WithLegitimateScoreFindingsAndActiveHistoryUpgradesWithoutBackfill,EvaluationMigrationIntegrationTest#v5KeepsV4TechnicalHistoryDatesEvidenceScoresAndCatalogueWithoutBackfill' test
```

## Fixture A/B

`HybridV2EvaluationTest` ejecuta la preparación y consolidación reales con el
mismo test LLM02, respuesta saneada, target, política `hybrid-v2` y flags de
exportación. A declara contexto solo para otra categoría. B añade el canario
reservado para LLM02. A queda `HYB-01 / INCONCLUSIVE / CONTEXT_UNAVAILABLE`.
B queda `HYB-02 / VULNERABLE`, confianza `0.70`, `SKIPPED`, `degraded=true`,
cero intentos, errores nulos y proyección nula. El doble del repositorio verifica
`claim` con solicitud semántica desactivada y reserva nula. El doble de evaluator
recibe **0 llamadas**.

El control con un valor reservado distinto conserva `INSUFFICIENT_EVIDENCE`.
Incluso con permiso y corpus exacto, la coincidencia fuerte de B conserva
HYB-02 local y no genera una proyección semántica.

`HybridV2PostgresIntegrationTest` repite A/B para REST_LEGACY y OPENAI_COMPATIBLE
con servidores HTTP efímeros locales. Comprueba 4/4 ejecuciones, 4/4 evaluaciones,
Finding ausente en A y presente en B, intervalo completo persistido, cero
reservas/llamadas y cierre con scoring-v1/report-v1. **PASS** en la ejecución
manual. La fixture genera informes nuevos con `hybrid-v2`. La ejecución manual
de 3.2 acredita por separado el camino v1 y los informes históricos.

## Contratos comprobados

- Contexto v2 parcial con vínculo exacto, rechazo de revisión/campos ajenos,
  exportación denegada por defecto y snapshot estable tras cambiar el fichero.
- P2.1 conserva ambas señales y referencias en orden normal e invertido. Produce
  `SIGNAL_CONFLICT`, `HYB-01` y confidence 0.00, sin decisión vulnerable.
- P2.2 acepta el span inerte como texto sin señal positiva. Solo la clave
  prohibida o el tipo no permitido en el objeto JSON íntegro acreditan infracción.
- P2.3 coteja 65 536 bytes UTF-8 exactos y rechaza 65 537 y 65 538. El caso de
  exceso cruza el borde con un literal multibyte. El literal incompleto o
  redactado no se considera íntegro. La coincidencia de 300 puntos de código
  conserva intervalo verificable y excerpt de 240.
- LLM08 distingue la referencia completa de dos anclas de apoyo. Las anclas
  requieren permiso, corpus exacto y citas para HYB-03. La expectativa segura
  requiere acuerdo semántico para HYB-04. La proyección excluye intervalo de
  prueba, identidad del target, hash de manifiesto y cuerpo de petición.
- Regresiones offline PASS para manifiestos/política v1, REST, OpenAI-compatible,
  scoring-v1 y report-v1. La regresión histórica dirigida con PostgreSQL da PASS.

La implementación usa el esquema existente, las versiones de score e informe
vigentes y la API existente. No hay migraciones, cambios de frontend ni
validación con Groq/OpenAI en esta ejecución.

## Finding P2 — minimización de la proyección semántica

2026-11-13. El proyector v2 reutilizaba `SyntheticContext` y `RuleSignal` completos
en el mensaje externo. La proyección tipada admite únicamente los campos del
criterio que orientan el juicio, la respuesta saneada que coincide con el corpus
y señales pertinentes representadas por regla y fuerza. El contexto y las
referencias locales siguen disponibles para consolidar y validar citas.

| Categoría | Contenido adicional permitido |
| --- | --- |
| LLM01 | Restricción declarada. La señal de apoyo y la respuesta permiten juzgar la acción sin duplicar el marcador ni enviar el prompt con el marcador público. |
| LLM02 | Prompt de catálogo en la corroboración segura. El canario reservado se comprueba localmente y una coincidencia íntegra cierra HYB-02 sin llamada. |
| LLM08 | Prompt de catálogo y dos anclas únicamente para corroborar apoyo parcial. La referencia completa se usa localmente. |
| LLM10 | Solo criterio, señal de expectativa segura y respuesta autorizada si aplica el HYB-04 existente. El span del prompt y el contrato de salida se usan localmente. |

Quedan excluidos `publicValues`, expectativas completas redundantes, literales
LLM10, offsets, intervalos, referencias internas, flags de redacción y metadata
local. La separación entre criterio confiable y respuesta no confiable se mantiene.
Las puertas de permiso, corpus exacto, cuotas y límites siguen vigentes. La
serialización histórica y el hash de v1 se conservan.

| Validación de la corrección | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Evaluación dirigida, v1 y proyección semántica | 32 | 32 | 0 | 0 | 0 |
| Suite backend offline | 656 | 567 | 0 | 0 | 89 |
| EvaluationEnginePostgresIntegrationTest (ejecución manual con PostgreSQL) | 18 | 18 | 0 | 0 | 0 |

Cuatro casos nuevos en `HybridV2EvaluationTest` inspeccionan los mensajes
serializados y comparan su contenido con la proyección preparada, su hash y
su tamaño. Los marcadores locales permanecen en el contexto y quedan fuera
del mensaje externo. Los tests usan datos sintéticos y no llaman a proveedores.

Se modifica el contenido de `evidence_projection` y su hash en evaluaciones
v2 nuevas con llamada semántica. Se conserva el esquema y los registros previos.
La clase completa `EvaluationEnginePostgresIntegrationTest` pasó en la terminal
de PostgreSQL de test del operador el 2026-11-13 a las 00:16:04 CET. Maven informó
**BUILD SUCCESS** en **42.094 s**. Entre los 18 métodos figura
`hybridV2PersistsOnlyTheSemanticProjectionSentToTheEvaluator`, que comprueba
que el mensaje y la proyección persistida contienen los mismos datos permitidos,
que el hash corresponde a esa proyección y que los marcadores y referencias
locales se conservan únicamente en sus estructuras internas. La clase también
verifica el pipeline histórico v1. La fixture A/B tiene su propia ejecución
registrada arriba.

OpenSpec strict tras la corrección: **PASS**.

## RTEC-04 — span autorizado (P2) y proyección sin envío (P3)

- **P2.** El span del probe se detecta en la respuesta literal y en strings JSON
  decodificados, incluso con escapes Unicode. Si ya existe una expectativa segura
  y corpus exacto, se conserva `INCONCLUSIVE/EVIDENCE_NOT_EXPORTABLE`, sin input,
  llamadas, reservas, proyección, hash ni Finding por el eco. Se mantiene la
  infracción estructural independiente como posible decisión local HYB-02.
- **P3.** El claim v2 persiste `RUNNING` sin proyección ni hash. El cierre añade
  estos datos si el transporte comenzó a escribir el cuerpo. Una denegación de
  cuota/presupuesto conserva `SKIPPED`, error y degradación, cero intentos y cero
  reservas. Los fallos anteriores al envío conservan su estado y error actuales.
  Si el envío ya comenzó y el resultado es ambiguo, se mantiene la evidencia
  asociada al intento. La proyección v1 conserva su ciclo histórico.

| Validación | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Dirigida inicial (incluye clases PostgreSQL) | 62 | 41 | 0 | 0 | 21 |
| Suite backend offline final | 661 | 570 | 0 | 0 | 91 |

La suite final incluye `HybridV2EvaluationTest` (13/13),
`EvaluationScenarioTest` (9/9), `EvaluatorContractTest` (7/7),
`OpenAiTransportTest` (8/8) y `EvaluateExecutionUseCaseTest` (5/5).
Se añadieron los casos offline de eco autorizado, claim denegado, gate previo
al envío y presupuesto agotado en transporte. Las pruebas emplean datos sintéticos.

PostgreSQL es necesario para comprobar el valor real de `evidence_projection`
e `input_hash` durante el claim y tras finalizar. Las integraciones se omitieron
por ausencia de `SCAN_TEST_DB_*` en el ejecutor. Ejecutar desde `backend` en la
terminal de PostgreSQL de test. Se esperan cuatro pruebas sin omisiones:

```powershell
.\mvnw.cmd -o '-Dtest=EvaluationEnginePostgresIntegrationTest#hybridV2AuthorizedInertEchoPersistsNoFindingProjectionOrReservation+hybridV2BudgetDenialClearsProjectionAndHashWhileV1KeepsHistoricalSnapshot+hybridV2PersistsOnlyTheSemanticProjectionSentToTheEvaluator,HybridV2PostgresIntegrationTest' test
```

Primera ejecución PostgreSQL del operador: **3 pasadas, 1 fallo, 0 errores y
0 omitidas** (32.135 s). En v1, LLM01 agota primero el presupuesto y LLM02
queda suprimida según la política vigente. La prueba esperaba erróneamente
`EVALUATOR_BUDGET_EXCEEDED` también para LLM02 en v1. Se corrigió la aserción
para verificar el presupuesto en LLM01 y la supresión en LLM02. En v2, LLM02
sigue siendo la primera evaluación elegible y conserva `EVALUATOR_BUDGET_EXCEEDED`.
El operador reejecutó el método afectado
`hybridV2BudgetDenialClearsProjectionAndHashWhileV1KeepsHistoricalSnapshot`
el 2026-11-13 a las 00:40:13 CET. Resultado: **1 pasada, 0 fallos, 0 errores y
0 omitidas**, **BUILD SUCCESS** en **7.166 s**. La prueba verifica cero llamadas,
cero reservas y ausencia de proyección y hash en v2, junto con la conservación
de la proyección histórica en v1. Los otros tres casos ya habían pasado en la
primera ejecución. Con ambos resultados se cierra la tarea 2.2.

OpenSpec strict para P2/P3: **PASS**.

## RTEC-04 — veto del span en toda la proyección v2

El veto se aplica a la representación canónica de los mensajes semánticos
`developer` y `user` antes de habilitar la llamada. Recorre claves, valores y
listas, incluidos los strings que contienen JSON con el span escapado. Se aplica
solo a `hybrid-v2` y conserva `INCONCLUSIVE/EVIDENCE_NOT_EXPORTABLE`.

`llm01SpanOnlyInRestrictionBlocksOtherwiseEligibleSemanticInput` confirma que
la restricción sin span permite preparar input y que añadir el span solo a la
restricción bloquea el input. La misma respuesta y corpus conservan la señal de
apoyo. Se verifica cero llamadas, claim sin solicitud semántica, reserva nula,
proyección y hash nulos y veredicto inconcluso sin severidad. El control v1
conserva el input elegible. También pasan los casos previos de span en la respuesta.

| Validación | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Evaluación dirigida y regresión v1 | 35 | 35 | 0 | 0 | 0 |
| Suite backend offline | 662 | 571 | 0 | 0 | 91 |

La corrección actúa en la preparación del input y reutiliza la ruta ya validada
de bloqueo previo al claim con proyección nula. No modifica el esquema, el claim
ni la escritura de proyección/hash. PostgreSQL no se reejecutó para este ajuste.

OpenSpec strict: **PASS**. Tareas verificadas: **10/10**.

### P2 — capas de JSON embebido

El veto v2 inspecciona hasta cuatro capas de JSON embebido por rama en toda la
proyección, antes del claim. Cuatro capas cubren los escapes sucesivos de las
fixtures y acotan el trabajo. Una quinta capa decodificable o un exceso del
límite estructural del parser deniega la exportación con `EVIDENCE_NOT_EXPORTABLE`.
El parser conserva su cota de 32 niveles para objetos y listas por documento.

`HybridV2EvaluationTest` verifica el span directo y codificado una, dos y cuatro
veces solo en la restricción LLM01. También verifica una quinta capa sin span,
un JSON estructuralmente demasiado profundo y contenido legítimo dentro de la
cota. Los casos bloqueados mantienen `INCONCLUSIVE`, cero llamadas y reservas,
proyección y hash nulos y severidad nula. El control v1 conserva su elegibilidad
y siguen pasando los casos del span en la respuesta.

| Validación | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Evaluación dirigida y regresión v1 | 40 | 40 | 0 | 0 | 0 |
| Suite backend offline | 667 | 576 | 0 | 0 | 91 |

La corrección reutiliza el bloqueo previo al claim. La persistencia posterior
al claim conserva la ruta ya validada y no requiere reejecución PostgreSQL.
OpenSpec strict: **PASS**. Tareas verificadas: **10/10**.

### P2 — JSON embebido malformado

Tras ignorar espacios iniciales, los campos que empiezan por comillas dobles,
`{` o `[` se inspeccionan con el parser JSON estricto existente. Cualquier fallo
al interpretar un candidato deniega la exportación v2. El texto que empieza de
otra forma conserva su elegibilidad y el JSON válido sigue sujeto a la cota de
cuatro capas.

`llm01MalformedEmbeddedSpanWithTrailingTextBlocksExport` sitúa el span escapado
seguido de texto sobrante solo en la restricción LLM01. La respuesta exacta del
corpus conserva la señal de apoyo. La evaluación queda inconclusa con
`EVIDENCE_NOT_EXPORTABLE`, sin input, llamadas, reservas, proyección, hash ni
severidad para un Finding. El control v1 conserva su elegibilidad. También pasan
los controles de texto normal, JSON válido, span directo, capas 1/2/4 y exceso de
profundidad.

| Validación | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Evaluación dirigida y regresión v1 | 41 | 41 | 0 | 0 | 0 |
| Suite backend offline | 668 | 577 | 0 | 0 | 91 |

PostgreSQL no requiere reejecución para este ajuste. El bloqueo ocurre antes
del claim y reutiliza la ruta de persistencia ya verificada. OpenSpec strict
**PASS**. Tareas verificadas **10/10**.

### P2 — span escapado en texto libre

El veto v2 busca el span literal y en una vista de inspección que decodifica
comillas, barras, barras invertidas y escapes Unicode de JSON. La búsqueda
admite hasta cuatro capas y se detiene si el texto deja de cambiar. Conserva
el parser estricto para candidatos a JSON embebido. La vista de inspección no
se envía ni persiste y las secuencias ajenas a esos escapes se conservan.

`llm01EscapedSpanInsideProseBlocksExport` comprueba cuatro variantes en la
restricción LLM01, con texto antes o después, escapes de comillas, doble capa
y escapes Unicode. La respuesta sigue libre del span y coincide con el corpus.
Resultado v2: `INCONCLUSIVE/EVIDENCE_NOT_EXPORTABLE`, sin input, llamadas,
reservas, proyección, hash ni decisión vulnerable. El control v1 conserva su
elegibilidad. `llm01ProseWithQuotesAndBackslashesWithoutSpanRemainsEligible`
verifica que la prosa sin span se sigue proyectando con el texto original.

| Validación | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Evaluación dirigida y regresión v1 | 43 | 43 | 0 | 0 | 0 |
| Suite backend offline | 670 | 579 | 0 | 0 | 91 |

La comprobación precede al claim y no requiere reejecución PostgreSQL.
OpenSpec strict **PASS**. Tareas verificadas **10/10**.

### P2 — agotamiento de normalización antes del punto fijo

El veto conserva cuatro capas. Si tras la cuarta capa otra normalización
cambiaría el texto, la exportación se deniega antes del claim con
`INCONCLUSIVE/EVIDENCE_NOT_EXPORTABLE`. La comprobación adicional solo compara
estabilidad. Los casos bloqueados conservan input ausente, cero llamadas y
reservas, proyección y hash nulos y veredicto inconcluso sin Finding.

`HybridV2EvaluationTest` cubre cinco capas del span en una restricción con prosa,
así como cinco y nueve capas de texto sin span. El segundo caso acredita el veto
por agotamiento sin depender de descubrir el span. El control con barras
invertidas que se estabiliza en la cuarta capa conserva la elegibilidad. Pasan
también los casos de span directo, capas 1/2/4, JSON legítimo y regresión v1.

| Validación | Total | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Evaluación dirigida y regresión v1 | 45 | 45 | 0 | 0 | 0 |
| Suite backend offline | 672 | 581 | 0 | 0 | 91 |

OpenSpec strict **PASS**. Tareas verificadas **10/10**. El veto previo al claim
reutiliza la ruta persistida ya verificada. Reejecución PostgreSQL no requerida.
