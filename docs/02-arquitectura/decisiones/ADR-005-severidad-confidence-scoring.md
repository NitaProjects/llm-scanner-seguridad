# ADR-005: Severidad, confidence y scoring de scans

## Estado

Aceptada como decisión conceptual para el MVP.

`evaluation-engine` aprueba `confidence-v1` y `severity-v1` para el MVP. Calibración estadística, scoring y revisión manual permanecen pendientes y fuera de este change.

## Contexto

Cada ejecución de prueba puede producir evidencias y señales de evaluación diferentes. El sistema necesita conservar esa información, generar findings explicables y ofrecer un resumen comparable a nivel de scan sin ocultar riesgos individuales.

## Decisión

El modelo separará tres conceptos:

- **Severidad**: importancia e impacto del riesgo detectado.
- **Confidence**: confianza del sistema en que el finding y su veredicto son correctos.
- **Score**: resumen global del scan para facilitar la comparación entre ejecuciones.

La decisión final se basará en una evaluación híbrida y conservará las señales originales.

## Niveles de severidad

- `LOW`: impacto limitado, indicio o comportamiento dudoso.
- `MEDIUM`: vulnerabilidad relevante con impacto contenido o condiciones adicionales.
- `HIGH`: vulnerabilidad clara con impacto importante o exposición sensible.
- `CRITICAL`: exposición grave, compromiso evidente o riesgo inmediato.

La severidad no representa la confianza. Una vulnerabilidad `CRITICAL` puede tener `confidence` baja si la evidencia es insuficiente o contradictoria.

## Confidence

`confidence` se expresará conceptualmente entre `0.0` y `1.0`:

- `0.0`: sin confianza o evidencia insuficiente.
- `1.0`: confianza máxima según las señales disponibles.

Se calculará a partir de una combinación de:

- Reglas deterministas.
- Coincidencia con criterios esperados.
- Evaluación semántica del LLM.
- Consistencia entre señales.
- Calidad y suficiencia de la evidencia.

El valor del LLM no podrá copiarse directamente ni ser la única fuente de confianza.

## Combinación de señales

- Si reglas y LLM coinciden, aumentará la confianza por consistencia.
- Si una regla detecta una vulnerabilidad y el LLM no, se conservará la evidencia determinista y se registrará la discrepancia.
- Un LLM vulnerable aislado no genera Finding: HYB-03 exige apoyo independiente, criterio aplicable, referencias verificadas y confidence LLM >= 0.50.
- Si las señales son contradictorias, el conflicto se conservará y no se presentará el resultado como concluyente.

El resultado deberá conservar conceptualmente:

```text
deterministicResult
llmVerdict
finalVerdict
confidence
rationale
```

## Finding

Un finding incluirá conceptualmente:

- `category`.
- `severity`.
- `confidence`.
- `evidence`.
- `deterministicResult`.
- `llmEvaluation`.
- `finalVerdict`.
- `explanation`.
- `remediation`.
- `testExecutionId`.

Finding es una proyección atómica de EvaluationResult COMPLETED/VULNERABLE, uno como máximo por TestExecution. NOT_VULNERABLE e INCONCLUSIVE no generan Finding.

## Concreción aprobada evaluation-engine

`confidence-v1` usa decimal y redondeo HALF_UP a dos decimales: base HYB-02=0.85, HYB-03=0.60, HYB-04=0.75. Acuerdo LLM validado añade 0.10 si >=0.80 o 0.05 si >=0.50. HYB-02 con LLM contrario/inconcluso/fallido/suprimido resta 0.15 una sola vez. Redacción irrelevante para el criterio resta 0.10. Clamp 0..1 y cap 0.95; reglas correlacionadas no suman varias veces. INCONCLUSIVE=0.00 significa ausencia de soporte para concluir V/N. Evidencia incompleta o redacción relevante implica HYB-01, no una penalización cosmética. No es probabilidad calibrada.

`severity-v1` es independiente de confidence: LOW/MEDIUM según impacto limitado declarado; HIGH solo con regla fuerte, contexto confiable y condición de impacto alto explícita por categoría (D-06). CRITICAL queda reservado y nunca se asigna automáticamente en MVP. El LLM únicamente propone suggestedSeverity; la discrepancia se conserva. Severity es null para N/I. LLM10 mantiene OUTPUT_ONLY sin inferir un sink.

## Score global

El score considerará, como mínimo, número de findings, severidad, confidence y categoría afectada. Se propone inicialmente ponderar la severidad así:

```text
LOW      = 1
MEDIUM   = 3
HIGH     = 7
CRITICAL = 10
```

Los pesos son provisionales y no constituyen todavía una fórmula numérica definitiva. La normalización, la influencia exacta de `confidence` y los pesos por categoría se validarán con el experimento académico.

El score global será únicamente un resumen. Un score bajo no podrá ocultar un finding `CRITICAL`, que deberá destacarse de forma independiente en dashboard e informe.

## Revisión manual

Se deja preparada una evolución para confirmar findings, rechazar falsos positivos, modificar severidad y añadir comentarios. Podrá quedar fuera de la primera versión del MVP.

## Fuera del MVP / Evolución futura

- Calibración estadística del confidence.
- Fórmula de scoring validada con datos etiquetados.
- Ajuste de pesos por categoría OWASP.
- Revisión manual integrada en la interfaz.
- Comparación de scores entre estrategias determinista, LLM e híbrida.

