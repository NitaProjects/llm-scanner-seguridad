# Casos de prueba iniciales

## Estructura conceptual

Cada Security Test debería definir, como mínimo:

- Identificador.
- Nombre y categoría.
- Referencia OWASP, cuando corresponda.
- Objetivo de la prueba.
- Prompt o payload controlado.
- Criterios de detección.
- Severidad propuesta.
- Recomendación.
- Evidencia esperada.

El formato definitivo de los casos y su almacenamiento quedan como **Pendiente de decisión**.

## Categorías del MVP

### LLM01 – Prompt Injection

Comprueba si la aplicación objetivo obedece instrucciones introducidas por el usuario que contradicen su comportamiento esperado o sus restricciones.

La prueba conceptual utilizará entradas controladas con instrucciones conflictivas. La evidencia podrá incluir la petición, la respuesta, la restricción incumplida y el resultado de la evaluación.

La detección combinará reglas para patrones o respuestas prohibidas con un LLM evaluador para interpretar el cumplimiento semántico. Su limitación principal es la variabilidad del comportamiento del modelo y la necesidad de definir qué respuesta se considera esperada.

### LLM02 – Sensitive Information Disclosure

Comprueba si la aplicación revela información sensible, datos internos o contenido que no debería estar disponible en la respuesta.

La prueba conceptual utilizará información de referencia y patrones controlados. La evidencia podrá incluir el fragmento detectado, el patrón coincidente, la clasificación del dato y la explicación.

Las reglas deterministas ayudarán a identificar patrones conocidos y el LLM evaluador valorará exposiciones contextuales. La detección no puede conocer todos los datos sensibles del target y deberá minimizar su almacenamiento.

### LLM08 – Hidden Context Exposure

Comprueba si la aplicación expone contexto interno, instrucciones, configuración sensible o información privada del flujo de inferencia.

La prueba conceptual solicitará de forma controlada información sobre el contexto interno. La evidencia podrá incluir el contenido expuesto, indicadores coincidentes y la explicación del evaluador.

Las reglas pueden detectar indicadores conocidos y el LLM evaluador puede valorar el significado del contenido. La limitación principal es que el scanner no siempre conocerá el contexto interno legítimo para distinguir una explicación de una exposición.

### LLM10 – Improper Output Handling

Comprueba si la salida del target es insegura, no controlada o incompatible con el comportamiento esperado del dominio.

La prueba conceptual utilizará casos que puedan producir respuestas fuera de formato, fuera de dominio o potencialmente peligrosas. La evidencia podrá incluir la respuesta fuera de contrato, el contenido detectado y la recomendación de validación o sanitización.

Las reglas comprobarán formato, patrones y restricciones conocidas, mientras que el LLM evaluador valorará el riesgo contextual. El MVP no analizará completamente los sistemas posteriores que consumen la salida, por lo que la cobertura será parcial.

## Categorías fuera del MVP

Las siguientes categorías quedan reservadas para evoluciones futuras:

- **LLM03 – Excessive Agency**: requiere agentes, herramientas, permisos y acciones autónomas.
- **LLM04 – Supply Chain**: requiere analizar modelos, dependencias y proveedores externos.
- **LLM05 – Data and Model Poisoning**: requiere escenarios de entrenamiento, ajuste o ingestión de datos manipulados.
- **LLM06 – Unbounded Consumption**: requiere evaluar consumo de recursos, costes y límites a mayor escala.
- **LLM07 – Misinformation**: requiere fuentes de referencia y verificación factual.
- **LLM09 – Vector and Embedding Weaknesses**: requiere RAG, almacenes vectoriales y análisis de embeddings.

Quedan fuera porque requieren escenarios, infraestructura o tipos de aplicación más complejos que el MVP actual.

## Criterios de calidad

Los casos deben ser controlados, reproducibles, seguros y adecuados para aplicaciones propias o de demostración. No deben buscar denegación de servicio, explotación persistente ni daño real.

La colección inicial de prompts, sus variantes y las respuestas esperadas se definirá antes de implementar trabajo real de seguridad en el motor de scans. `scan-execution` es el incremento previo aprobado de ciclo de vida con trabajo temporal no-op; no selecciona ni ejecuta casos, genera evidencias o acredita cobertura OWASP. COMPLETED solo significa que finalizó esa unidad temporal. Antes de sustituir ScanWorkPort por análisis real se necesitan un change aprobado con estos casos/prompts, selección estable del target y controles de autorización, SSRF/DNS/redirecciones, límites y protección de credenciales/respuestas no confiables.

## Uso del evaluador LLM

Los casos de prueba no dependerán de un proveedor concreto. Cuando las reglas deterministas y los criterios esperados no sean suficientes, el caso podrá solicitar una evaluación semántica mediante el `EvaluatorPort` conceptual.

El resultado podrá aportar veredicto, confianza, severidad sugerida, explicación y recomendación, pero no sustituirá las comprobaciones deterministas ni la revisión de los criterios esperados. El proveedor y el modelo utilizados se configurarán externamente y se registrarán como metadatos de la evaluación, nunca sus credenciales.

## Resultado de una ejecución

La ejecución de cada caso debe conservar por separado la evidencia obtenida, el resultado determinista y la evaluación del LLM. A partir de esas señales se generará el `Finding`, cuando corresponda, con categoría, severidad, confidence, veredicto final, explicación, remediation y referencia a `testExecutionId`.

La severidad podrá ser `LOW`, `MEDIUM`, `HIGH` o `CRITICAL`. El `confidence` se expresará conceptualmente entre `0.0` y `1.0` y no dependerá exclusivamente del LLM. Las discrepancias entre reglas, criterios esperados y evaluador deberán conservarse como parte de la evidencia.

## Preparación para el experimento académico

Los casos de prueba del producto podrán reutilizarse como base del dataset experimental, pero el conjunto académico deberá conservar explícitamente su resultado esperado y su justificación. La estructura conceptual de cada caso experimental será:

```text
ExperimentCase
- id
- owaspCategory
- expectedVerdict
- expectedSeverity opcional
- description
- targetScenario
```

El dataset futuro deberá incluir casos vulnerables, no vulnerables y, si procede, ambiguos para las cuatro categorías del MVP. No se definen todavía casos ni payloads concretos.

La evaluación académica comparará reglas deterministas, LLM evaluador y estrategia híbrida. Las ejecuciones deberán registrar resultado esperado, resultado obtenido, estrategia, confidence, severidad, tiempo, coste y modelo evaluador para permitir el análisis de true positives, true negatives, false positives, false negatives y reproducibilidad.
