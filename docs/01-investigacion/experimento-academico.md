# Experimento académico

## Propósito

El experimento evaluará si LLM Scanner Seguridad detecta de forma objetiva y reproducible vulnerabilidades comunes en aplicaciones LLM mediante pruebas controladas y evaluación híbrida.

## Pregunta de investigación

> ¿Puede una plataforma web basada en pruebas controladas y evaluación híbrida detectar de forma reproducible vulnerabilidades comunes en aplicaciones LLM?

La formulación de hipótesis y los valores objetivo se mantendrán como **Pendiente de decisión** hasta concretar el dataset y el procedimiento de etiquetado.

## Diseño general

Se preparará un conjunto controlado de casos con resultado conocido, cubriendo las cuatro categorías del MVP:

- LLM01 – Prompt Injection.
- LLM02 – Sensitive Information Disclosure.
- LLM08 – Hidden Context Exposure.
- LLM10 – Improper Output Handling.

El dataset incluirá, cuando sea viable:

- Casos vulnerables.
- Casos no vulnerables.
- Casos ambiguos o insuficientemente observables.

La aplicación `demo-vulnerable/` será una de las principales fuentes de escenarios controlados. También podrán utilizarse escenarios seguros equivalentes para construir los casos no vulnerables, siempre que el ground truth quede documentado.

No se crean todavía casos, payloads ni respuestas esperadas concretas.

## Estructura conceptual del dataset

Cada caso futuro tendrá una estructura equivalente a:

```text
ExperimentCase
- id
- owaspCategory
- expectedVerdict
- expectedSeverity opcional
- description
- targetScenario
```

`expectedVerdict` podrá ser `VULNERABLE`, `NOT_VULNERABLE` o `INCONCLUSIVE`. El ground truth deberá justificarse mediante el escenario, la expectativa definida y, cuando proceda, revisión manual.

## Estrategias comparadas

Cada caso se ejecutará, cuando sea aplicable, con tres configuraciones:

1. **Solo reglas deterministas**: utiliza patrones, estructura, valores de referencia y criterios verificables.
2. **Solo LLM evaluador**: utiliza la evaluación semántica del evaluador sin aplicar la decisión híbrida.
3. **Evaluación híbrida**: combina reglas, criterios esperados y LLM evaluador.

El objetivo es comparar si la estrategia híbrida mejora la calidad de detección y reduce las limitaciones individuales de las otras dos estrategias.

## Protocolo conceptual

1. Seleccionar el caso y registrar su ground truth.
2. Ejecutarlo contra el escenario controlado autorizado.
3. Aplicar cada estrategia comparable.
4. Registrar verdict, confidence, severidad, tiempo, coste y modelo evaluador cuando corresponda.
5. Repetir los casos seleccionados para estudiar variabilidad.
6. Comparar los resultados obtenidos con el resultado esperado.
7. Analizar discrepancias, falsos positivos, falsos negativos y resultados inconclusos.

La cantidad de casos, repeticiones, orden de ejecución y criterios de selección quedan como **Pendiente de decisión**.

## Métricas

Se registrarán como mínimo:

- **True Positive (TP)**: caso vulnerable clasificado como `VULNERABLE`.
- **True Negative (TN)**: caso no vulnerable clasificado como `NOT_VULNERABLE`.
- **False Positive (FP)**: caso no vulnerable clasificado como `VULNERABLE`.
- **False Negative (FN)**: caso vulnerable clasificado como `NOT_VULNERABLE`.
- **Precision**: proporción de detecciones positivas correctas sobre todas las detecciones positivas.
- **Recall**: proporción de casos vulnerables detectados sobre todos los casos vulnerables.
- **Reproducibilidad**: estabilidad del resultado al repetir el mismo caso bajo condiciones equivalentes.
- **Tiempo medio por scan**: duración media de las ejecuciones.
- **Coste aproximado**: coste estimado de las llamadas al LLM evaluador.

Conceptualmente:

```text
precision = TP / (TP + FP)
recall    = TP / (TP + FN)
```

Si un denominador es cero, el resultado deberá marcarse como no calculable en lugar de inventar un valor. Los casos `INCONCLUSIVE` se analizarán separadamente y no se convertirán automáticamente en positivos o negativos.

## Reproducibilidad

Para los casos seleccionados se realizarán varias ejecuciones. Cada registro deberá incluir:

- Test ejecutado.
- Resultado esperado.
- Resultado obtenido.
- Estrategia utilizada.
- `confidence`.
- Severidad.
- Tiempo.
- Coste estimado.
- Modelo evaluador y proveedor, cuando corresponda.

También se documentarán las condiciones relevantes de ejecución, como versión del caso, configuración del target y fecha. No se asumirán resultados idénticos cuando intervenga un modelo no determinista.

## Presentación de resultados

La memoria final deberá incluir, como mínimo:

- Tabla de resultados por caso y estrategia.
- Matrices de confusión.
- Precision y recall cuando sean calculables.
- Análisis de falsos positivos y falsos negativos.
- Análisis de resultados `INCONCLUSIVE`.
- Comparación entre reglas, LLM e híbrida.
- Reproducibilidad.
- Tiempo y coste.
- Limitaciones y amenazas a la validez.
- Conclusiones respecto a la pregunta de investigación.

## Limitaciones

- Variabilidad y posible no determinismo de los LLM.
- Tamaño limitado del dataset.
- Dificultad para establecer ground truth en casos ambiguos.
- Dependencia del modelo evaluador y del proveedor seleccionado.
- Representatividad limitada de `demo-vulnerable/`.
- Cobertura parcial de escenarios reales, especialmente consumidores posteriores en LLM10.
- Resultados no generalizables automáticamente a todas las aplicaciones LLM.

## Decisiones pendientes

- Tamaño y composición definitiva del dataset.
- Número de repeticiones por caso.
- Criterios de inclusión de casos ambiguos.
- Procedimiento de establecimiento y revisión del ground truth.
- Proveedor y modelo evaluador.
- Tratamiento estadístico definitivo de `INCONCLUSIVE`.
- Valores objetivo y umbrales de interpretación.

