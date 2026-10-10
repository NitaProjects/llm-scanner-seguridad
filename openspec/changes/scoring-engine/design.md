## Context

Motivación/alcance: [proposal.md](proposal.md). Contrato aprobado: [specs/scoring-engine/spec.md](specs/scoring-engine/spec.md), SC-01..SC-13. Este documento es la referencia principal de arquitectura y reglas de scoring; las tareas concretan su implementación, no sustituyen decisiones.

Baseline aprobado por el usuario: project-management, target-configuration, scan-execution, security-test-execution y evaluation-engine, implementados/validados/aprobados. `openspec/specs/` solo contiene `.gitkeep`; se utilizan las specs de esos changes sin editarlas, archivarlas ni sincronizarlas.

Observaciones relevantes:

- ADR-005 distingue severity/confidence/score y conserva pesos **provisionales** 1/3/7/10 sin fórmula cerrada. confidence-v1 y severity-v1 sí están aprobadas y se respetan; CRITICAL no se asigna automáticamente en MVP.
- EvaluationResult/Finding son terminales inmutables, confirmados conjuntamente; Finding existe solo para COMPLETED/VULNERABLE, máximo uno por ejecución. V5 tiene UNIQUE por evaluación/ejecución y FK compuesta para relaciones, categoría, verdict, severity y confidence.
- El catálogo/plan actual contiene cuatro pruebas, una por categoría, revisión 1. El scoring no selecciona pruebas ni redefine categorías. LLM10 sigue siendo OUTPUT_ONLY, sin inferir un sink.
- `ProcessScanUseCase` llama trabajo y después `lifecycle.complete`. `TransactionalScanLifecycle.complete` ya agrupa los guards de ejecución/evaluación y la transición en una transacción REQUIRES_NEW corta. Es el punto natural para incorporar scoring sin otra fase durable ni red.
- Scan tiene siete campos públicos, cuatro estados y tres motivos públicos de fallo. `ScanStartup` interrumpe activos en una sola transacción antes de abrir compuerta/admisión. No reanuda análisis y no se cambiará esa decisión.
- `scans.global_score NUMERIC(5,2)` está sin mapeo/uso desde V1. V3/V4/V5 y documentación exigen conservarlo; no hay contrato de score vigente que reutilizar.
- EvaluationRepositoryPort ofrece metadatos públicos y allCompleted, pero no una proyección de scoring completa. Se adopta un puerto de lectura específico, sin cargar evidenceProjection, prompts, assessment, target ni credenciales y sin modificar el puerto de evaluación. La proyección solo vive en memoria; no se duplica como snapshot persistido.
- OpenAPI tiene consultas scoped de Scan/executions/evaluations/findings, no-store y errores estables. El nuevo contrato seguirá esas convenciones.
- docs/00-contexto conserva referencias antiguas a decisiones de cifrado/executor pendientes; changes y ADRs posteriores ya las cerraron. No se reabren ni se hace mantenimiento ajeno. Investigación/memoria no aportan calibración empírica del score. ADR-007 mantiene separada la comparación de estrategias y el ground truth.

No se ha encontrado contradicción objetiva que obligue a reabrir los modelos cerrados. Requerir scoring para COMPLETED es una **extensión prospectiva aprobada**, identificada expresamente en este change, no una corrección silenciosa del baseline.

## Goals / Non-Goals

**Goals:** Política pura pequeña y versionada; lectura mínima de inputs íntegros; resultado inmutable; atomicidad score/cierre sobre la transacción existente; incertidumbre visible y consulta sin efectos laterales.

**Non-Goals:** Arquitectura distribuida, repositorios genéricos, nuevos estados de scoring, dependencia de LLM/red, pesos configurables en runtime o experimento de calibración dentro de este incremento. Exclusiones de producto: proposal.md.

## Decisions

**D-01..D-17 APROBADAS expresamente el 2026-10-10**, con los ajustes de no usar input hash (D-12/D-14/D-16/D-17), formalizar availability (D-15) y mantener categorías como breakdown de ScanScore sin agregados/lifecycle independientes (D-08/D-14/D-16). Las alternativas siguientes documentan el razonamiento y están descartadas para este MVP; no son decisiones pendientes. El estado READY FOR IMPLEMENTATION acredita diseño aprobado, no código implementado ni autorización para implementarlo en esta entrega.

### D-01 — Escala de riesgo creciente, entera y heurística

**Problema y decisión adoptada.** Un resumen necesita una dirección inequívoca sin sugerir probabilidad real de compromiso. Se adopta una escala entera 0..100: mayor número significa mayor riesgo detectado según scoring-v1; no se usa símbolo de porcentaje. Cien representa saturación de la heurística, no certeza de compromiso.

**Alternativas y descarte.** Una escala de seguridad creciente haría que un valor alto significase seguridad mientras HIGH significa gravedad, creando una inversión innecesaria. Fracciones/porcentajes aparentarían precisión o calibración que el proyecto no ha demostrado. Un resumen solo cualitativo no cumpliría el objetivo de disponer de score numérico reproducible.

**Consecuencias e invariantes.** Dominio y API usan enteros; PostgreSQL valida 0..100. Toda interpretación debe mencionar riesgo detectado en las pruebas, sin prometer comparabilidad entre planes, versiones o coberturas diferentes. Por ejemplo, 75 significa nivel HIGH bajo v1, no 75 % de probabilidad. La dirección y el rango pertenecen a la versión; no se alteran al consultar ni durante recuperación. SC-02 verifica esta semántica.

### D-02 — RiskLevel independiente de Severity

**Problema y decisión adoptada.** Severity pertenece a un Finding y expresa su impacto; RiskLevel pertenece a ScanScore y resume el riesgo detectado. Se adopta un tipo `RiskLevel` independiente, aunque comparta cuatro nombres. No se convierte ni modifica Severity para calcular el nivel agregado.

| Score | RiskLevel |
| --- | --- |
| 0 | NO_FINDINGS |
| 1..39 | LOW |
| 40..69 | MEDIUM |
| 70..89 | HIGH |
| 90..100 | CRITICAL |

**Alternativas y descarte.** Reutilizar Severity mezclaría tipos y obligaría a etiquetar como LOW un Scan sin Findings. Nombres totalmente distintos añaden vocabulario sin ventaja clara frente a separar tipos y campos. UNKNOWN como nivel mezclaría incertidumbre con riesgo conocido: un HIGH puede coexistir con tres inconclusos, sin dejar de ser HIGH.

**Consecuencias e invariantes.** El nivel se deriva del score y se persiste como parte del resumen validado; API/lectura rechazan discordancias. Los intervalos son ordinales, no probabilísticos. Fronteras del mapeo: 39→LOW, 40→MEDIUM, 69→MEDIUM, 70→HIGH, 89→HIGH, 90→CRITICAL. No todos son alcanzables por la fórmula v1: máximos LOW=25, MEDIUM=55, HIGH=85, CRITICAL=100. Esos huecos conservan precedencia, no se rellenan normalizando. Con Findings, RiskLevel coincide en nombre con su severity máxima como consecuencia de D-03..D-06; sigue siendo un concepto y tipo distintos. La recuperación conserva ambos ejes sin reclasificarlos. SC-02/SC-04.

### D-03 — Inputs finales, bases y plan vigente

**Problema y decisión adoptada.** El motor necesita una base reproducible que preserve la gravedad y se apoye solo en resultados finales confiables. Se adoptan LOW=10, MEDIUM=40, HIGH=70, CRITICAL=90. Son anclas/suelos, no daño monetario: la separación mínima de veinte puntos supera el ajuste máximo total de confidence y acumulación (5+10). LOW comienza en diez para separar una detección de ninguna detección; HIGH y CRITICAL reservan los tramos altos para impacto alto/grave.

**Alternativas y descarte.** Escalar provisionalmente 1/3/7/10 a 10/30/70/100 deja CRITICAL siempre saturado y MEDIUM fuera de su banda, sin aportar fórmula de agregación. Suma/media de pesos tampoco protege suelos. Las bases aprobadas sustituyen aquellos pesos provisionales solo en scoring; no cambian severity-v1 ni confidence-v1. ADR-005 se actualizará durante implementación autorizada, sin reabrir sus decisiones previas.

**Consecuencias e invariantes.** La fuente son Findings persistidos, finales de EvaluationResults y relaciones del plan. Deben existir exactamente las cuatro pruebas/revisiones aprobadas, cuatro ejecuciones/evaluaciones COMPLETED y un único Finding por VULNERABLE, coherente en todos sus vínculos/severity/confidence. N/I no tienen Finding. Leer todas las filas y validar antes de construir mapas; DISTINCT/GROUP BY/límites no deben ocultar duplicados o extras. Falta de evaluación o incoherencia es error técnico, nunca score parcial. La función pura admite más Findings en pruebas matemáticas, sin ampliar el plan productivo. CRITICAL es entrada válida para la fórmula, no autorización de asignación automática por el evaluador. API/recuperación no fabrican fuentes ausentes. SC-01/SC-03/SC-08.

### D-04 — Confidence como bonus limitado, sin reducir impacto

**Problema y decisión adoptada.** Confidence expresa soporte de la detección, no impacto. Se adopta `confidenceBonus=ROUND_HALF_UP(5*c)` a entero con decimal exacto y `w=severityBase+confidenceBonus`. Solo se usa confidence final persistida: nunca llmConfidence o suggestedSeverity. El bonus máximo de cinco puntos permite modular ligeramente el resumen, sin descontar la base.

**Alternativas y descarte.** Un multiplicador directo puede volver irrelevante un HIGH de poca confianza. Bandas de descuento imponen saltos y siguen rebajando impacto. Usarla solo como metadata sería simple, pero no reflejaría confidence en la métrica como prevé ADR-005. El bonus limitado positivo conserva impacto; tampoco es probabilidad ni calibración estadística.

**Consecuencias e invariantes.** Calcular con BigDecimal, HALF_UP a escala cero; validar c decimal no nula en 0..1 sin clamp silencioso. No modificar la confidence almacenada ni severity, ni descontar riesgo por confidence de N/I. El bonus derivado no se persiste como nueva fuente. Un HIGH c=0 obtiene 70 y c=0.95 obtiene 75; ambos siguen HIGH. La política acepta c=1 como límite matemático aunque confidence-v1 limite actualmente a 0.95. No añade red, estado ni recuperación propios. SC-03.

| Intervalo de c | Bonus | Ejemplo de frontera |
| --- | --- | --- |
| 0.00 <= c < 0.10 | 0 | 0.09→0 |
| 0.10 <= c < 0.30 | 1 | 0.10→1; 0.29→1 |
| 0.30 <= c < 0.50 | 2 | 0.30→2; 0.49→2 |
| 0.50 <= c < 0.70 | 3 | 0.50→3; 0.69→3 |
| 0.70 <= c < 0.90 | 4 | 0.70→4; 0.89→4 |
| 0.90 <= c <= 1.00 | 5 | 0.90→5; 0.95→5; 1.00→5 |

### D-05 — Máximo más incrementos amortiguados

**Problema y decisión adoptada.** La fórmula debe considerar número de Findings sin diluir el más grave ni crecer ilimitadamente. Para F, multiconjunto de Findings **con identidades distintas y coherentes**, se adopta:

```text
si F vacío: score = 0; riskLevel = NO_FINDINGS
para cada Finding:
  severityBase = {LOW:10, MEDIUM:40, HIGH:70, CRITICAL:90}[severity]
  confidenceBonus = ROUND_HALF_UP(5 * confidence)
  w = severityBase + confidenceBonus
B = max(w)
R = sum(w) - B
increment = min(10, floor(R / 10))
score = min(100, B + increment)
```

La rama vacía retorna antes de calcular max; no se invoca max sobre una colección vacía. Para F no vacío, riskLevel se deriva del score mediante D-02.

**Consecuencias técnicas.** Solo se resta una instancia del máximo aunque haya empate; no hace falta elegir un Finding por orden. R y la división son enteros no negativos; se aplica floor, no HALF_UP, al dividir R entre diez. En MVP hay cuatro filas; la función pura debe rechazar overflow, no envolver el entero. No hay float ni redondeo final adicional. Score, contadores y breakdown se construyen juntos y se guardan una sola vez, sin persistir cada paso matemático.

**Alternativas y descarte.** La media diluye gravedad; la suma simple hace dominar cantidad; producto/noisy-OR recuerda probabilidad y complica explicación; solo máximo ignora número de Findings. La combinación adoptada preserva máximo y aporta una décima de los pesos restantes, truncada, hasta diez puntos. Se cuentan identidades distintas sin afirmar independencia estadística ni deduplicarlas por texto/categoría.

**Invariantes y fronteras.** N/I no entran en F, denominadores ni restas; el resultado es independiente del orden. R=0→incremento0, R=10→1, R=99→9, R>=100→10. R entre1 y9 no se genera con Findings válidos v1, porque cada peso es al menos10. Un LOW adicional deja de aumentar el score cuando el incremento ya está saturado, aunque aumenta el contador; antes del cap aporta al menos una unidad por su peso mínimo10. HIGH c=0 más LOW c=0.09 produce70+floor(10/10)=71. Cambios pequeños del bonus de un Finding secundario pueden no cruzar una decena y dejar igual el incremento. Dos LOW c=0.90 empatan en w=15: B=15,R=15 y score=16, sin eliminar ambos máximos. Se mantiene la misma operación en memoria, persistencia y reconstrucción, nunca al consultar/recuperar. SC-04.

### D-06 — Floors, cap y propiedades

**Problema y decisión adoptada.** Cantidad o poca confianza no deben ocultar HIGH/CRITICAL ni hacer que muchos LOW se presenten como impacto alto. Se adoptan suelos HIGH>=70 y CRITICAL>=90, cap global100 e incremento máximo10. Los suelos derivan de bases no descontables, sin corrección posterior que esconda otra fórmula.

**Alternativas y descarte.** Un cap global sin suelos permitiría dilución si se promediara; suelos con suma ilimitada permitirían elevar artificialmente LOW a CRITICAL. Promocionar nivel por número de Findings inventaría impacto no asignado por evaluación. La base dominante y el incremento limitado conservan ambos extremos, sin cambiar severity individual.

**Consecuencias e invariantes.** Máximo LOW: B<=15 e incremento<=10 → score<=25; MEDIUM<=55; HIGH<=85; CRITICAL<=100. Así, veinte LOW no superan un HIGH de confidence cero; añadir LOW a HIGH puede elevar modestamente el número, nunca diluirlo ni convertirlo en CRITICAL. Varios HIGH tampoco producen CRITICAL. PostgreSQL/API conservan rango y correspondencia score/nivel; la recuperación no reclasifica. Con CRITICAL/0.90 (B=95), añadir MEDIUM/0.90 da R=45 y score99; añadir MEDIUM/0.00+LOW/0.00 da R=50 y score100 exacto; añadir MEDIUM/0.90+LOW/0.90 da R=60 y score100 por cap del valor101. Son conjuntos válidos de hasta tres Findings, sin ampliar el plan. SC-02/SC-04.

Propiedades: invariante por permutación; monotonía al agregar un Finding o elevar su confidence/severity; sin dilución por agregar NOT_VULNERABLE/INCONCLUSIVE; global>=máximo por categoría. Justificación de monotonía: si el nuevo peso no supera B, R crece; si lo supera, el nuevo B crece y R incluye todos los anteriores. Ambas funciones de incremento/cap son no decrecientes. Los floors no dependen de cantidad de pruebas ni de confidence promedio.

El techo 25 para LOW corresponde a la política matemática general, no a un plan mayor autorizado: con los cuatro Findings máximos del MVP, todos LOW/0.90, B=15, R=45 y score=19. Para comprobar las fronteras R=99/100/101 con cuatro entradas sintéticas válidas, pueden usarse B=95 y pesos secundarios 74+15+10, 75+15+10 y 75+15+11 respectivamente; el incremento pasa de9 a10 y permanece en10. No se crean pruebas de seguridad ni criterios CRITICAL nuevos con estos ejemplos.

Ejemplos exactos (c=0.90 salvo indicación, bonus=5):

| Findings / finales | B | R | Incremento | Score / nivel |
| --- | --- | --- | --- | --- |
| 0 Findings, cuatro NOT_VULNERABLE | — | — | 0 | 0 / NO_FINDINGS |
| 1 LOW | 15 | 0 | 0 | 15 / LOW |
| 1 MEDIUM | 45 | 0 | 0 | 45 / MEDIUM |
| 1 HIGH | 75 | 0 | 0 | 75 / HIGH |
| 1 CRITICAL | 95 | 0 | 0 | 95 / CRITICAL |
| 3 LOW | 15 | 30 | 3 | 18 / LOW |
| 4 LOW (máximo plan MVP) | 15 | 45 | 4 | 19 / LOW |
| 20 LOW (solo política pura) | 15 | 285 | 10 | 25 / LOW |
| HIGH + LOW | 75 | 15 | 1 | 76 / HIGH |
| HIGH + MEDIUM + LOW | 75 | 60 | 6 | 81 / HIGH |
| 4 HIGH | 75 | 225 | 10 | 85 / HIGH |
| CRITICAL + HIGH | 95 | 75 | 7 | 100 / CRITICAL (cap de 102) |
| HIGH, c=0.00 | 70 | 0 | 0 | 70 / HIGH (floor) |
| CRITICAL, c=0.00 | 90 | 0 | 0 | 90 / CRITICAL (floor) |
| 1 LOW + 1 N + 2 I | 15 | 0 | 0 | 15 / LOW; I=2 |
| 0 Findings, cuatro INCONCLUSIVE | — | — | 0 | 0 / NO_FINDINGS; I=4 |

N/I abrevia NOT_VULNERABLE/INCONCLUSIVE. Los ejemplos con CRITICAL son entradas sintéticas de la política, no nuevos criterios de asignación del evaluador; con tres/un Finding, las otras evaluaciones del plan pueden ser N/I. No insertar Findings históricos para demostrar ejemplos.

### D-07 — Incertidumbre explícita, sin riesgo inventado

**Problema y decisión adoptada.** Un resultado inconcluso expresa falta de soporte para concluir, no ausencia de vulnerabilidad. Se mantienen contadores evaluationCount, findingCount, notVulnerableCount e inconclusiveCount, globales y en el breakdown. Cada V final tiene exactamente un Finding; findingCount también cuenta vulnerables y no se duplica vulnerableCount. Un I COMPLETED no suma riesgo ni entra en F; permanece visible. Un FAILED técnico no es un I puntuable y bloquea el score. Fallo LLM consolidado usa el verdict final, sin revisar evaluación.

**Alternativas y descarte.** Penalizar riesgo por abstención inventaría vulnerabilidades; descontar por I/N inventaría seguridad; omitir I haría engañoso score0. Un porcentaje de cobertura/coverageStatus añadiría otra dimensión sin medir cobertura real: COMPLETE podría confundirse con una categoría totalmente probada. Los contadores explican lo conocido sin esa promesa.

**Consecuencias e invariantes.** `evaluationCount=findingCount+notVulnerableCount+inconclusiveCount=4` en v1. Los contadores se persisten/consultan con el resultado, no alimentan un lifecycle independiente. Cuatro I→score0,I=4,N=0; HIGH+N+I+I→score75,HIGH,I=2 con c=0.90. Cero I solo indica que esos tests concluyeron; N se limita al test concreto. Recuperación/fallo interno no fabrican I para completar estos contadores. SC-05/SC-08.

### D-08 — Categorías simétricas, global sobre Findings

**Problema y decisión adoptada.** El usuario necesita localizar riesgo/incertidumbre por categoría sin introducir cuatro nuevos agregados. Se adopta un único breakdown estructurado `ScanScore.categories`: cuatro entradas ordenadas LLM01,LLM02,LLM08,LLM10, con la misma fórmula aplicada al subconjunto de Findings de cada categoría y sin pesos OWASP distintos. Cada entrada contiene category, score, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount y severityCounts. Hoy cada categoría tiene una evaluación; definir la fórmula no autoriza más pruebas.

**Alternativas y descarte.** Promediar los scores por categoría ocultaría el máximo; sumarlos duplicaría acumulación; tomar solo el máximo perdería número de Findings. Pesos distintos requerirían justificación empírica inexistente. Agregados/tablas/repositorios/ciclos separados permitirían versiones o cierres parciales divergentes sin consumidor MVP. Se descartan: el global se calcula directamente sobre todos los Findings y las categorías son valores anidados, no entidades.

**Consecuencias e invariantes.** `CategoryScore` puede ser VO de datos, sin id, status, fechas, versión, endpoints, escritura o recuperación propios. Se construye junto con ScanScore y se persiste en su columna JSONB; todo el resultado confirma o revierte en la misma transacción. No tabla scan_score_categories ni agregados de dominio independientes. También es matemáticamente derivable de Findings/finales/versión; se materializa dentro de ScanScore para ofrecer una consulta simple y estable, sin recalcular en GET. Los contadores globales suman los del breakdown; el score global no es su suma/media y es >=máximo por categoría. SC-06/SC-11.

**Ejemplo.** HIGH c=0.90 en LLM01 y LOW c=0.90 en LLM02 dan categorías75 y15, global76. Una categoría con I tiene0/NO_FINDINGS e inconclusiveCount=1; con N tiene0 y notVulnerableCount=1. Ninguna se etiqueta segura. LLM10 conserva OUTPUT_ONLY, sin inventar un peso por cobertura downstream. Si falla una entrada, falla ScanScore entero: no se recupera esa categoría por separado.

### D-09 — Significado acotado de score cero

**Problema y decisión adoptada.** Cero puede confundirse con seguridad o falta de ejecución. Se adopta literalmente: **«Score 0 significa ningún Finding detectado en las pruebas completadas, no aplicación segura. Este valor no demuestra ausencia de vulnerabilidades ni cobertura completa; consulte los resultados inconclusos.»** Solo existe después de validar el plan completo. Se acompaña siempre de NO_FINDINGS y contadores.

**Alternativas y descarte.** SAFE/SECURE o «riesgo real cero» excederían lo observado. Restar valor por tests N no demuestra nada fuera de ellos. Usar cero como default de ausencia hace indistinguibles un histórico, un fallo técnico y un resultado válido; se utiliza null y availability en ausencia.

**Consecuencias e invariantes.** Dominio exige F vacío si score=0 y toda detección pesa al menos10; no hay cero con Findings ni cero generado por falta de evaluaciones. Persistencia no ofrece un default0 para tapar datos ausentes. API/OpenAPI deben incluir la frase y distinguir `AVAILABLE` con globalScore0 de `PENDING/UNAVAILABLE/NOT_APPLICABLE` con score=null. Cuatro N y cuatro I dan0, pero los contadores muestran distinto conocimiento. Fallo/recuperación conservan ausencia, sin inventar0. SC-01/SC-02/SC-05/SC-12.

### D-10 — Scoring dentro de la finalización técnica

**Problema y decisión adoptada.** En scans nuevos, COMPLETED debe significar flujo entero listo para consulta. Se adopta A: validar, calcular y persistir ScanScore como parte de la finalización técnica, tras confirmar las cuatro ejecuciones/evaluaciones COMPLETED y sus Findings. El Scan sigue RUNNING hasta el commit conjunto de score y cierre; VULNERABLE/I o fallo LLM consolidado no impiden ese éxito técnico.

**Alternativas y descarte.** B, scoring posterior no bloqueante, produciría COMPLETED sin resultado y exigiría otro disparador, fallo tardío y recuperación. Dos transacciones separadas también permitirían score durable con Scan activo. Para cuatro filas y cálculo local sin red, una sola transacción corta es más simple y evita ambos huecos. No se amplía el DTO Scan ni se añade estado SCORING.

**Composición aprobada:**

```text
T1 existente: crear Scan PENDING + evaluationProfile + scoringVersion requerida
dispatch / T2 existentes: claim RUNNING
trabajo existente: cuatro ejecuciones/evaluaciones; Findings confirmados
T3 nueva composición de cierre, una sola transacción corta:
  lock de Scan y lectura de estado/versión requerida
  si terminal: no reabrir ni calcular; comprobar integridad del score si corresponde
  validar plan y snapshot mínimo completo
  CalculateScanScoreUseCase -> ScoringPolicyV1 pura
  insertar ScanScore, confirmar transición condicional RUNNING -> COMPLETED
  commit conjunto
```

El use case de cálculo **participa** en T3; no abre otra REQUIRES_NEW ni confirma antes de completar Scan. Spring/transacciones/locking quedan en adaptadores/config. Orden de lock: Scan primero, consistente con los claims de evaluación; después filas necesarias ordenadas por identidad. La inmovilidad terminal protege el snapshot; no red ni transacciones durante target/LLM. No ampliar los 300 s ni los límites aprobados; el scoring local se ejecuta al cerrar el trabajo elegible dentro del flujo existente.

**Consecuencias e invariantes.** T1 fija versión requerida solo para scans nuevos, en la misma creación durable; no se decide por fecha ni por presencia de Findings al finalizar. Un histórico de evaluation-engine sin marcador conserva su semántica y no recibe scoring. T3 solo confirma RUNNING→COMPLETED si insert y guards son correctos; un false inesperado del cierre revierte también el insert. Una lectura nunca observa medio commit. Un scan con cuatro I puede completar con score0/I=4; uno sin evaluación requerida falla técnicamente. Las fechas respetan D-16. SC-07/SC-08/SC-11.

### D-11 — Fallos internos conservan la semántica técnica

**Problema y decisión adoptada.** El scoring es local y determinista: no poder calcularlo/persistirlo indica fallo del scanner, no ambigüedad del target. Error de cálculo, integridad, versión no soportada o persistencia implica rollback T3 y compensación existente FAILED/SCAN_PROCESSING_FAILED en otra transacción corta. No se añade otro motivo público de Scan ni un estado de scoring.

**Alternativas y descarte.** Score0 de fallback inventaría ausencia de Findings; INCONCLUSIVE de scoring confundiría fallo técnico con evaluación; completar sin score violaría D-10; reintentos automáticos introducirían una política no aprobada. Se conserva la compensación/compuerta ya existente.

**Consecuencias e invariantes.** Findings/evaluaciones terminales confirmados se conservan, sin convertirlos en FAILED/I ni cambiar verdict/confidence/severity. Si también falla compensar, se mantiene el último estado durable y se bloquean nuevos inicios/reclamaciones con la compuerta hasta recuperación. Un commit ambiguo no demuestra rollback: si puede leerse COMPLETED+score íntegros, se conservan; la compensación condicional no reabre un terminal. Si no puede confirmarse el estado ni compensar, se aplica compuerta/reinicio. Diagnóstico solo id/código estable, sin inputs/narrativa, SQL ni causas internas. No se promete escribir con DB indisponible. Un GET que lee correctamente un Scan FAILED sigue siendo200; el score devuelve UNAVAILABLE/null, mientras un fallo de lectura es500. SC-08/SC-12.

### D-12 — Identidad compuesta e inmutabilidad, sin upsert destructivo

**Problema y decisión adoptada.** Una entrega duplicada o commit ambiguo no debe duplicar ni cambiar el resumen histórico. Se adopta PK `(scan_id,scoring_version)`, lock Scan primero y resultado inmutable, incluido calculatedAt. El ScanScore existente se reutiliza sin escritura. No se usa input hash: las fuentes terminales persistidas son inmutables y la versión fija la fórmula.

**Alternativas y descarte.** UNIQUE solo por Scan impediría conservar versiones futuras. Un UUID adicional no aporta identidad funcional. Upsert con UPDATE podría alterar valores/fecha bajo el mismo nombre; se descarta. Hash/snapshot adicional no es necesario para idempotencia con inputs inmutables y se excluye por el ajuste aprobado. Ningún mecanismo v1 promete auditoría criptográfica de cambios externos a esas invariantes.

**Consecuencias e invariantes.** Al adquirir lock, un cierre duplicado sobre terminal no calcula ni cambia estado: devuelve/reconoce el resultado existente si es coherente. Si una operación interna ya trae un candidato calculado y encuentra fila de la misma identidad, compara globalScore, riskLevel, todos los contadores y breakdown contra el existente; no compara el nuevo reloj ni sustituye calculatedAt. Una diferencia es error de integridad. No se conserva una segunda lista de fuentes para comparar byte a byte ni se afirma detectar cualquier alteración ilegal que produzca el mismo resumen; la garantía es la inmutabilidad del baseline. GET y startup no recalculan para hacer esa comparación.

Si un conflicto SQL inesperado aborta la transacción, no se continúa dentro de ella: lectura durable fuera de la transacción fallida o compensación condicional según el estado real, sin retries. Un terminal sin score no se rellena y un FAILED no se reabre. Una versión futura solo podría añadir una fila mediante otro change, conservando las anteriores y la versión originalmente requerida. SC-09/SC-13.

### D-13 — Recuperación aprobada, atomicidad elimina la ventana nueva

**Problema y decisión adoptada.** Una caída puede dejar Findings completos y un Scan activo, pero la política aprobada interrumpe trabajo al reiniciar. Se conserva ScanStartup/RecoverInterruptedScansUseCase: activos a FAILED/SCAN_INTERRUPTED antes de admisión, hijos activos cerrados en la misma recuperación, terminales intactos, sin target/evaluador/scoring relanzados. T3 elimina un nuevo estado durable intermedio.

**Alternativas y descarte.** Reanudar solo scoring sería técnicamente posible por ser local, pero cambiaría la recuperación general ya aprobada. Cola de scores pendientes, leases o repair automático añaden complejidad y pueden inventar éxito tras una interrupción. Dejar activos huérfanos bloquearía Project. Todas se descartan para MVP.

| Momento de caída | Durable y recuperación |
| --- | --- |
| Después de confirmar Findings, antes de T3 | Scan activo se interrumpe a FAILED/SCAN_INTERRUPTED; Findings/evaluaciones terminales se conservan; score ausente. |
| Durante cálculo local | Igual; no hay score parcial ni nuevas llamadas al arrancar. |
| Después de insert/flush del score, antes de transición o commit | Ambos pertenecen a T3; rollback del insert/cierre. Recuperación interrumpe Scan activo, sin fabricar score. |
| Después de commit score+cierre, antes de responder al worker | Ambos terminales íntegros; arranque conserva ambos. Entrega duplicada no recalcula. |
| Confirmación de commit ambigua | Leer durable sin red externa: o ambos confirmados o ninguno; nunca repetir target/LLM. Compensación condicional conserva terminal confirmado. |
| Score durable con Scan activo/FAILED o COMPLETED marcado sin score | Invariante rota fuera del flujo diseñado: diagnóstico de integridad, no reparación/backfill. Consulta 500 sanitizado; no otorgar éxito ni borrar score. |

«Después de persistir score y antes de cerrar Scan» solo existe dentro de T3, **no como estado confirmado**. Esta elección conserva la política previa de interrupción aunque queden cuatro evaluaciones correctas antes de la caída. Reanudar solo scoring podría ser posible por ser local, pero reabriría una decisión aprobada de recovery y queda fuera del MVP.

**Consecuencias e invariantes.** La recuperación no inserta ScanScore, no toca su fecha/versión y no recupera categorías por separado. Conserva Findings disponibles de un Scan FAILED; availability será UNAVAILABLE/null si el Scan estaba sujeto a scoring sin resultado. Un score confirmado permanece AVAILABLE en el Scan completado. La ausencia en un histórico sigue NOT_APPLICABLE. Si no se confirma la recuperación completa no se abre compuerta/executor. Se mantiene una sola instancia; no se autoriza recuperación concurrente entre workers distribuidos. SC-10/SC-12.

### D-14 — ScanScore separado, V6 y global_score intacto

**Problema y decisión adoptada.** Se necesita un resultado versionado completo sin atribuir una fórmula nueva a históricos. Se adopta una tabla única `scan_scores` y `scans.scoring_version VARCHAR(64) NULL` como versión **requerida**, fijada en T1 para nuevos scans. El marcador no entra en el record/DTO público Scan. CHECK null o scoring-v1; cuando no es null exige evaluation_profile_version no nula e igual a evaluation-engine-v1, sin aceptar UNKNOWN SQL por NULL. Distingue históricos evaluados sin scoring de nuevos COMPLETED inconsistentes; inferirlo por fecha, Findings o global_score no es fiable.

| Columna scan_scores | Tipo / restricción aprobada |
| --- | --- |
| scan_id, scoring_version | UUID FK scans(id), VARCHAR(64) no vacío; NOT NULL y PK compuesta. Versión soportada por aplicación v1; política nueva exige change/migración correspondiente. |
| calculated_at | TIMESTAMPTZ(6) NOT NULL, fecha original, validada contra finales de inputs y cierre. |
| global_score | SMALLINT NOT NULL CHECK 0..100. |
| risk_level | VARCHAR(20) NOT NULL, cinco valores de D-02 y CHECK de correspondencia con score. |
| evaluation_count, finding_count, not_vulnerable_count, inconclusive_count | SMALLINT NOT NULL, 0..4; evaluation_count=4 en v1, igualdad entre suma de verdicts y total. |
| severity_counts | JSONB NOT NULL, objeto cerrado LOW/MEDIUM/HIGH/CRITICAL con enteros no negativos, suma=finding_count. |
| categories | JSONB NOT NULL, array de cuatro objetos cerrados de SC-06; orden/identidades únicos conocidos, enteros/rangos y sumas coherentes. |

**Consecuencias e invariantes.** JSONB mantiene cuatro entradas sin identidad/lifecycle propio dentro de la fila. No columna input_hash ni tabla de snapshots matemáticos/por categoría. Checks defensivos: NULL explícito, tipos JSON, claves exactas, enteros/rangos, longitud/identidad de categorías, sumas y correspondencia score/nivel; score0 equivale a findingCount0. Dominio/aplicación vuelven a validar vínculo con inputs/estado, que un CHECK de fila no garantiza. Cap combinado severity_counts+categories serializados16KiB. Sin defaults de resumen inventado, cascadas de borrado o triggers de recálculo; PK cubre lookup por Scan, sin índice redundante. Todo insert/cierre confirma o revierte conjuntamente.

Modelo de dominio: `ScanScore` como resultado persistente inmutable de identidad compuesta; `CategoryScore` y `SeverityCounts` solo VOs contenidos. Sin updatedAt, id separado, maxSeverity redundante, promedio de confidence, porcentaje de cobertura, snapshot de target ni narrativa. Campos públicos exactos y forma de cada entrada: D-15/SC-06/SC-13. No se editan categorías o contadores por separado.

**Alternativas y descarte.** A) global_score solo no permite versión/contadores/semántica histórica; B) tabla separada, adoptada, satisface trazabilidad; C) tabla+proyección global_score añade dual write y fuente divergente sin consumidor. Tablas/agregados por categoría facilitarían consultas ad hoc, pero no justifican su complejidad en cuatro entradas inmutables. Hash/copia de fuentes no añade capacidad necesaria con inputs persistidos e inmutables. Por tanto global_score permanece sin leer, mapear, calcular, modificar o exponer, sin fallback aun siendo histórico no nulo.

V6 prevista: `V6__incorporar_motor_scoring.sql`, **no creada ni ejecutada en esta entrega**. V1–V5 permanecen byte a byte. A diferencia del guard de V5, V6 acepta Findings legítimos de V5; no exige vaciar tablas. Añade marcador null sin UPDATE y tabla vacía; no inventa puntuaciones pasadas. Pruebas futuras sobre esquemas aislados de la base dedicada existente, sin dependencias nuevas.

### D-15 — Un GET específico con disponibilidad explícita

**Problema y decisión adoptada.** Una consulta debe distinguir resultado calculado de espera, fallo o histórico, sin confundir ausencia con score0. Se adopta GET `/api/projects/{projectId}/scans/{scanId}/score`,200 `{availability,score}`, sin query funcional. availability describe la existencia/disponibilidad de ScanScore, no un nuevo estado persistido ni fase de procesamiento.

| availability | Condición exacta en datos coherentes | score |
| --- | --- | --- |
| AVAILABLE | Existe ScanScore válido de la versión requerida. | Objeto ScanScore, también si globalScore=0. |
| PENDING | Scan sujeto a scoring aún procesándose: marcador presente, PENDING/RUNNING, sin ScanScore. | null. |
| UNAVAILABLE | Scan sujeto a scoring terminó FAILED sin ScanScore. | Siempre null. |
| NOT_APPLICABLE | Scan histórico anterior a scoring-engine: marcador null, sin ScanScore, con cualquier estado previo. | Siempre null. |

La atomicidad de D-10 implica que en el flujo válido AVAILABLE se observa después del commit con Scan COMPLETED; no es un requisito adicional que permita omitir un score existente al clasificar ausencia. Antes de clasificar se validan las invariantes: score con marcador ausente/versión errónea/Scan no COMPLETED, o COMPLETED marcado sin score, son corrupción y producen500. No se etiquetan UNAVAILABLE/NOT_APPLICABLE ni se reparan al consultar. Las categorías no tienen availability propio.

**Alternativas y descarte.** Ampliar Scan rompe sus siete campos y duplica breakdown en listados. Un número aislado o null sin motivo oculta históricos/fallos/pendientes; usar404 para toda ausencia mezcla recurso inexistente con resultado todavía no calculado. Un estado persistido adicional duplicaría Scan/existencia de fila. El wrapper explícito evita esas ambigüedades sin duplicar scoringVersion fuera/dentro del score.

**Consecuencias técnicas y de seguridad.** Validar ambos UUID antes de persistencia (INVALID_PROJECT_ID antes de INVALID_SCAN_ID); Project primero, Scan scoped después. 400 para UUID inválido,404 PROJECT_NOT_FOUND/SCAN_NOT_FOUND incluido ajeno,500 INTERNAL_ERROR para fallo de lectura/integridad. Errores `{code,message,fieldErrors:[]}`, español sanitizado y Cache-Control:no-store en respuestas. Leer marcador, estado y score en el mismo snapshot/proyección conjunta; dos consultas READ COMMITTED separadas pueden cruzar T3 y simular corrupción. Sin joins de evidencia/credenciales, red, cálculo o escrituras. Versión requerida desconocida es error, no histórico. Scoping no introduce autenticación multiusuario.

**Contrato del resultado.** ScanScore público contiene exactamente scanId, scoringVersion, calculatedAt, globalScore, riskLevel, evaluationCount, findingCount, notVulnerableCount, inconclusiveCount, severityCounts y categories. Cada entrada categories tiene exactamente los ocho campos de D-08; severityCounts tiene cuatro claves enteras LOW/MEDIUM/HIGH/CRITICAL, incluidos ceros. Fechas UTC ISO8601/Z con microsegundos, null explícito solo para ausencia del objeto score. No input hash, evidencia, señales o snapshots en esta respuesta. Referencias/detalles originales permanecen en evaluations/findings.

**Ejemplos de ausencia.** Scan nuevo recién creado o RUNNING antes del cierre: `{availability:PENDING,score:null}`. Nuevo que falla antes de preparar el plan, durante scoring o por interrupción: UNAVAILABLE/null, aunque existan Findings. Histórico evaluation-engine-v1 que ya tiene cuatro resultados y ningún score: NOT_APPLICABLE/null, no cero ni candidato a backfill. Histórico FAILED sigue NOT_APPLICABLE, no UNAVAILABLE. Error de DB produce500, no disponibilidad normal. Un resultado cero válido es AVAILABLE con objeto completo.

Ejemplo disponible (un HIGH/0.90 en LLM01, N en las otras tres):

```text
availability = AVAILABLE
score.scanId = UUID del Scan
score.scoringVersion = scoring-v1
score.calculatedAt = instante UTC original
score.globalScore = 75; score.riskLevel = HIGH
score.evaluationCount = 4; score.findingCount = 1
score.notVulnerableCount = 3; score.inconclusiveCount = 0
score.severityCounts = {LOW:0, MEDIUM:0, HIGH:1, CRITICAL:0}
score.categories = cuatro CategoryScore, cada una con los campos de SC-06
```

**Invariantes de consulta y recuperación.** AVAILABLE nunca score=null; PENDING/UNAVAILABLE/NOT_APPLICABLE nunca contienen un score parcial/numérico. La lectura no cambia fechas/estados ni crea fila; recovery interrumpe activos y luego deriva UNAVAILABLE, sin reanudar scoring. OpenAPI debe explicar cero, I/N, métrica heurística y cobertura limitada; reporting/recomendaciones nuevas siguen fuera. SC-12/SC-13.

### D-16 — Metadata mínima sin input hash y fronteras hexagonales

**Problema y decisión adoptada.** Debe poder reconstruirse cómo se obtuvo un score sin almacenar de nuevo sus fuentes ni acoplar negocio a infraestructura. Se adopta reproducibilidad mediante **inputs originales persistidos e inmutables + scoringVersion** y resultado mínimo de D-14/D-15. No se calcula, persiste o expone input hash en MVP, ni se necesita canonicalización/serializador de inputs para calcularlo. Las versiones/hash que ya pertenecen a evaluación permanecen intactas: se elimina únicamente el hash nuevo de scoring.

**Responsabilidades aprobadas para implementación futura:**

| Elemento | Responsabilidad |
| --- | --- |
| domain/scoring: ScoringPolicyV1, RiskLevel, ScanScore y VOs | Cálculo decimal/entero puro e invariantes de resultados, sin Spring/HTTP/JPA/proveedor. |
| application/scoring: CalculateScanScoreUseCase | Validar inputs/versión, calcular resumen/VOs y construir resultado; participar en la frontera de cierre, sin hash ni red. |
| ScoringInputPort | Obtener proyección completa de plan, ejecuciones, finales y Findings; ids, categorías, severity/confidence/verdict/fechas, sin narrativa ni evidencia. |
| ScanScoreRepositoryPort | Leer versión requerida/resultado e insertar inmutable; fijar versión requerida en T1; tipos propios, sin JDBC/JPA/transacciones en firmas. |
| GetScanScoreUseCase | Validar pertenencia con puertos existentes y devolver disponibilidad/proyección coherente sin calcular. |
| Adaptadores PostgreSQL y REST / config | SQL/JSONB/locking, DTO/mapeo/error, composición transaccional T1/T3 y snapshot de consulta. |

**Metadata y fuentes suficientes.** scanId permite recuperar plan/test/revisiones, cuatro ejecuciones/evaluaciones y Findings por sus FKs; finalVerdict determina elegibilidad/contadores, severity/confidence originales determinan pesos, category determina breakdown y scoringVersion determina las constantes/reglas. Las versiones de evaluación/confidence/severity siguen en EvaluationResult, sin duplicarlas en ScanScore. No usar ground truth, señal parcial, suggestedSeverity ni último criterio disponible como fuente retrospectiva.

**Alternativas y descarte.** Un hash detectaría diferencias contra una huella almacenada, pero no reconstruye el contenido ni sustituye a retenerlo y exige nuevas reglas de canonicalización/campo/tests. Copiar todos los inputs o pasos duplica fuentes ya inmutables. Reconsultar target/LLM no reproduce necesariamente la respuesta original y altera el experimento. Las tres alternativas se descartan; no se promete protección criptográfica frente a cambios administrativos no autorizados ni se diseña una nueva auditoría.

**Consecuencias e invariantes.** `calculatedAt=max(reloj UTC truncado a microsegundos,Scan.startedAt,finishedAt de ejecuciones/evaluaciones,createdAt de Findings)` y `Scan.finishedAt>=calculatedAt`; repetición conserva el instante original. El reloj no entra en el valor matemático. No red ni puerto LLM/SDK, Spring/JPA/HTTP en dominio/aplicación o nuevas dependencias. ScoringInputPort entrega proyecciones completas sin narrativa/evidencia/credenciales; CategoryScore es VO anidado, sin puerto propio. Cambiar orden SQL no cambia score/contadores; solo categories se ordena expresamente para API. No serializador canónico adicional.

La garantía depende de conservar los terminales originales y sus relaciones/versiones, no solo sus contadores. El MVP no los edita, borra ni reevalúa; cualquier futura retención/borrado debe resolver preservación de reproducibilidad en otro change. Por ejemplo, con HIGH/0.90 y LOW/0.90 persistidos, scoring-v1 reconstruye75+floor(15/10)=76 sin hash, red ni metadata del reloj. GET devuelve el resultado guardado; esta reconstrucción corresponde a política/tests o análisis explícito, no a recálculo automático. SC-13.

### D-17 — Versionado congelado y experimento separado

**Problema y decisión adoptada.** Un resultado histórico pierde significado si cambian las constantes bajo el mismo nombre o se mezcla scoring con evaluación/benchmark. `scoring-v1` congela conjuntamente bases, bonus/HALF_UP, división floor e incremento máximo10, floors, cap100, bandas, tratamiento N/I y cálculo del breakdown. El marcador fija esa versión al crear Scan; no se elige «la última» al consultar. La interpretación no depende de hash, proveedor o parámetros runtime.

**Alternativas y descarte.** Sobrescribir versiones o configurar pesos libremente haría imposible explicar una fila anterior. Recalcular al leer cambiaría el significado y daría efectos laterales a GET. Usar score para decidir verdict/ground truth contaminaría el experimento; extraer LLM-only de HYBRID_WITH_SIGNALS tampoco lo vuelve independiente. Esas alternativas se descartan.

**Consecuencias e invariantes.** Conservar especificación/implementación v1. Cambiar una constante, redondeo, banda o regla requiere nueva scoringVersion y otro change aprobado; nunca UPDATE de scores previos. Una versión futura podría añadir otra fila por Scan+versión mediante diseño específico, preservando el original; MVP no incluye comando/endpoint de recalculado ni selector de versión. El scoring consume solo finales/Finding, no modifica reglas, confianza, severidad, proveedor, estrategia o etiquetas. Las constantes aprobadas se justifican por propiedades, **no están validadas experimentalmente**. Dataset/calibración/métricas científicas siguen pendientes fuera de este change. SC-09/SC-13.

## Risks / Trade-offs

- Anchors/bonus/divisor subjetivos → fórmula explícita, propiedades verificables y versión estable; no presentar probabilidad ni calibración realizada.
- Precedencia impide que muchos LOW/MEDIUM crucen banda → decisión conservadora consciente; cantidad/breakdown visible, no sustituir Findings ni prometer orden total de daños reales.
- Mismo cero para cuatro N y cuatro I → contadores obligatorios y semántica API explícita; disponibilidad distinta de valor.
- Crash tras evaluar todo pierde posibilidad de completar ese Scan → preservar recuperación aprobada sin relanzamiento, Findings consultables y nuevo Scan solo por iniciativa del usuario.
- T3 contiene cálculo local → snapshot de cuatro filas y sin red; no reabrir transacciones del evaluador ni ampliar presupuesto/executor.
- JSONB/counters redundantes respecto a inputs → una proyección inmutable con validación de sumas/shape/valores derivados en dominio, adaptador y pruebas PostgreSQL; sin hash ni copias de fuentes.
- Dos lecturas pueden cruzar commit → snapshot/proyección conjunta para disponibilidad, sin falso 500 durante polling normal.
- Versiones viejas no conocen marcador/guard nuevo → no afirmar rollback operativo seguro; despliegue y retorno de versión requieren procedimiento explícito.

## Migration Plan

Plan futuro basado en D-01..D-17 ya aprobadas; requiere petición posterior de implementación, sin ejecutar migraciones/despliegues aquí:

1. Partir del Approval Record de este diseño y comprobar coherencia con spec/tasks. Si se solicita un cambio material nuevo, tramitarlo explícitamente antes de implementar; no volver a pedir aprobación de D-01..D-17 ni de los modelos previos ya cerrados.
2. Actualizar documentación directamente afectada por el diseño aprobado: ADR-003/ADR-005, decisiones abiertas sección 6, arquitectura, semántica scoring en estrategia-evaluacion, OpenAPI/modelo de datos. La memoria seguirá tratando score como resumen y el experimento como separado; no inventar resultados ni actualizar checklist de entrega sin afectación real.
3. Crear V6 conservadora y fixtures V5 con N/I/V y Findings legítimos. Comprobar V1–V5/históricos/global_score intactos y ausencia de backfill, sin guard de tablas vacías.
4. Implementar dominio/política/VOs, puertos/adaptadores, integración T1/T3 y GET, con pruebas de la matriz de tasks.md y sin dependencias nuevas, input hash, canonicalización ni agregados/lifecycle por categoría.
5. Ejecutar solo comprobaciones autorizadas posteriormente. Las pruebas de migración/persistencia requieren autorización de base dedicada y nunca shared/producción. No pruebas con proveedor/target real para scoring.

Rollback: no borrar V6, filas o marcador ni ejecutar down migrations/repair. Una aplicación vieja podría crear/cerrar scans sin la nueva garantía; no es rollback seguro del worker. Preferir corrección hacia delante y procedimiento operativo aprobado; esta entrega no autoriza detener servicios ni desplegar.

## Approval Status

**READY FOR IMPLEMENTATION.** D-01..D-17 aprobadas con los tres ajustes incorporados. No quedan decisiones contractuales ni bloqueos de diseño pendientes. La petición actual solo autoriza actualizar artefactos y ejecutar strict; no se implementa código ni se inician migraciones, tests backend u otro change.

## Approval Record

El usuario aprueba expresamente D-01..D-17 el **2026-10-10** manteniendo scoring-v1 y sus bandas, y concreta tres ajustes que prevalecen sobre la propuesta previa:

1. **Sin input hash en MVP:** D-12/D-14/D-16/D-17 eliminan el campo, cálculo, canonicalización y tareas asociadas. Reproducibilidad por inputs persistidos e inmutables y scoringVersion. No se cambia la metadata de hashes ya existente en evaluation-engine.
2. **availability formalizada:** D-15 y SC-12 distinguen AVAILABLE por ScanScore existente, PENDING por procesamiento aún activo, UNAVAILABLE por FAILED nuevo sin score y NOT_APPLICABLE por histórico anterior; los dos últimos siempre score=null. D-10/D-13 conservan la atomicidad y clasificación de corrupción fuera del flujo válido.
3. **Categorías simples:** D-08/D-14/D-16 y SC-06 definen cuatro VOs/entradas dentro de ScanScore, sin identidad, estados, repositorios, tablas, API ni recuperación independientes.

El resto de decisiones se mantiene: fórmula/bonus/bases exactos, floors/cap, RiskLevel separado, cero limitado a detecciones, I visible sin riesgo inventado, COMPLETED prospectivo con score, errores técnicos, idempotencia, recuperación aprobada, V6 conservadora/global_score intacto y GET mínimo. Las alternativas de este documento explican por qué se eligieron; no quedan abiertas para que implementación cambie el contrato.

## Coherence Review

La revisión documental contrasta intención y alcance de proposal, comportamiento verificable de spec, decisiones completas de design y tareas derivadas. La eliminación de hash afecta también idempotencia/persistencia/tests; availability se define por existencia y ausencia con invariantes técnicas separadas; categorías se mantienen contenidas en un único resultado. No quedan instrucciones activas de crear hash, canonicalización ni agregados de categoría, ni aprobaciones de diseño pendientes.

| Decisión | Contrato verificable | Tareas derivadas |
| --- | --- | --- |
| D-01 Escala | SC-02 | 2.1, 6.3 |
| D-02 RiskLevel/Severity | SC-02/SC-04 | 2.1, 2.4 |
| D-03 Inputs/bases | SC-01/SC-03 | 2.2, 3.1, 3.2 |
| D-04 Confidence | SC-03 | 2.2, 2.4 |
| D-05 Fórmula | SC-04 | 2.3, 2.4 |
| D-06 Floors/cap | SC-02/SC-04 | 2.3, 2.4 |
| D-07 INCONCLUSIVE/N | SC-05 | 2.6, 5.3 |
| D-08 Breakdown contenido | SC-06/SC-11 | 2.1, 2.5, 4.2 |
| D-09 Cero | SC-01/SC-02/SC-05/SC-12 | 2.6, 3.2, 6.3 |
| D-10 COMPLETED atómico | SC-07 | 5.1, 5.2, 5.3 |
| D-11 Fallos | SC-08 | 5.4, 5.5 |
| D-12 Idempotencia sin hash | SC-09 | 4.3, 5.5, 5.6 |
| D-13 Recuperación | SC-10 | 6.1, 6.2 |
| D-14 V6/global_score | SC-11 | 4.1..4.5 |
| D-15 API/availability | SC-12 | 6.2..6.5 |
| D-16 Metadata/reproducibilidad | SC-13 | 3.1..3.4, 4.3 |
| D-17 Versionado/experimento | SC-09/SC-13 | 3.4, 7.1, 7.3 |

Las tareas 1.2/1.3 trasladan el diseño aprobado a documentación consolidada durante implementación; no sustituyen ninguna regla ni autorizaron cambios de código en esta actualización. La validación strict se refiere a estructura OpenSpec, no a tests backend, PostgreSQL o calibración empírica.

## Fuera del MVP / Evolución futura

Calibración empírica con dataset/ground truth aprobados; nuevas scoringVersions y comparación histórica explícita; más pruebas por categoría solo mediante otro change; deduplicación semántica/correlación si se demuestra necesaria; revisión manual y reevaluación; resúmenes narrativos/reporting, UI; retención/borrado que preserve provenance; recuperación selectiva de scoring si se aprueba cambiar la política general. Sin ML, CVSS completo, pesos por categoría arbitrarios ni infraestructura distribuida para este MVP.
