# ADR-007: Diseño del experimento académico

## Estado

Aceptada como diseño conceptual del experimento final del TFM.

El tamaño del dataset, las repeticiones, el ground truth definitivo, el tratamiento estadístico y los valores objetivo permanecen **Pendientes de decisión**.

## Pregunta de investigación

> ¿Puede una plataforma web basada en pruebas controladas y evaluación híbrida detectar de forma reproducible vulnerabilidades comunes en aplicaciones LLM?

## Decisión

Se realizará un benchmark controlado y reproducible utilizando escenarios autorizados. `demo-vulnerable/` será una fuente principal de casos vulnerables y se prepararán escenarios no vulnerables equivalentes. Se podrán incluir casos ambiguos cuando la evidencia o el contexto no permitan establecer un veredicto concluyente.

El benchmark cubrirá:

- LLM01 – Prompt Injection.
- LLM02 – Sensitive Information Disclosure.
- LLM08 – Hidden Context Exposure.
- LLM10 – Improper Output Handling.

## Estrategias comparadas

Cada caso se analizará, cuando sea aplicable, mediante:

1. Solo reglas deterministas.
2. Solo LLM evaluador.
3. Evaluación híbrida.

El objetivo es comprobar si la combinación mejora la calidad de detección respecto a cada estrategia aislada.

## Dataset conceptual

```text
ExperimentCase
- id
- owaspCategory
- expectedVerdict
- expectedSeverity opcional
- description
- targetScenario
```

No se crean casos ni payloads concretos en este ADR.

## Métricas

Se registrarán true positives, true negatives, false positives, false negatives, precision, recall, reproducibilidad, tiempo medio por scan y coste aproximado de llamadas LLM.

Cada ejecución registrará test, resultado esperado, resultado obtenido, estrategia, confidence, severidad, tiempo, coste y modelo evaluador.

Las fórmulas conceptuales de precision y recall serán:

```text
precision = TP / (TP + FP)
recall    = TP / (TP + FN)
```

Los casos `INCONCLUSIVE` se analizarán separadamente y no se convertirán automáticamente en positivos o negativos.

## Resultados esperados en la memoria

La memoria deberá incluir tablas de resultados, matrices de confusión, análisis de falsos positivos y negativos, comparación entre estrategias, costes, reproducibilidad, limitaciones y conclusiones.

## Limitaciones

El experimento estará condicionado por la variabilidad de los LLM, el tamaño del dataset, la dificultad de establecer ground truth, la dependencia del evaluador, la representatividad limitada de `demo-vulnerable/` y la generalización limitada a otras aplicaciones.

## Fuera del alcance del experimento

No se ejecutarán pruebas contra aplicaciones no autorizadas ni se diseñarán payloads ofensivos en esta fase documental. El estudio con desarrolladores sobre comprensión del informe queda fuera del diseño adoptado, salvo decisión posterior.

