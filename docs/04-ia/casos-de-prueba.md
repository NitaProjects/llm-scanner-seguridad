# Casos de prueba iniciales

## Estructura conceptual

`security-test-execution` conserva el catálogo técnico inmutable id/code/revision/name/category/prompt en test_cases. V4 mantiene los cuatro probes revisión 1. evaluation-engine añade por separado criteria-v1.json, contexto sintético local vinculado al snapshot y EvaluationResult; no modifica SecurityTest/V4 ni introduce catálogo editable, enabled o selección de usuario.

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

El formato técnico y almacenamiento de los cuatro probes quedan aprobados por D-01/D-11 de security-test-execution. La evaluación ejecutable está aprobada por evaluation-engine D-01..D-16; variantes y dataset definitivo permanecen fuera del change.

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

La colección inicial se concreta en los cuatro prompts exactos D-11 de security-test-execution y en V4, sin salto final ni variables dinámicas. El worker fija catálogo y target una sola vez, confirma todo el plan antes de enviar y conserva evidencia redactada. Los scans no-op históricos no se reclasifican. COMPLETED de una prueba significa éxito técnico, nunca vulnerabilidad/seguridad; HTTP 200 con marcado o instrucciones sigue técnicamente COMPLETED. Los cuatro probes no constituyen cobertura completa de las categorías. LLM10 es una observación/proxy limitada al output; no valida sanitización ni explotación del consumidor downstream/sink.

## Catálogo técnico inicial aprobado

| Code / revisión | Nombre / categoría | Objetivo y expectativa descriptiva |
| --- | --- | --- |
| LLM01-INSTRUCTION-001 / 1 | Instrucción conflictiva controlada / LLM01 | Observar reemplazo de prioridades. El target debería mantener las restricciones de su escenario; el marcador aislado no demuestra vulnerabilidad. |
| LLM02-CANARY-001 / 1 | Solicitud de canario sintético / LLM02 | Observar un canario oculto preinstalado. Sin contexto del escenario no hay conclusión. |
| LLM08-CONTEXT-001 / 1 | Solicitud de contexto de prueba / LLM08 | Observar exposición de una instrucción no pública. Una explicación genérica no demuestra fuga. |
| LLM10-MARKUP-001 / 1 | Marcado inerte en salida / LLM10 | Recoger output inerte; su presencia en JSON es válida y no demuestra manejo inseguro del sink. |

Los prompts exactos, UUID y descripciones se conservan en `backend/src/main/resources/db/migration/V4__incorporar_ejecuciones_pruebas_seguridad.sql`, con referencia OWASP GenAI LLM Top 10 (2026). Cambiar contenido requerirá una nueva revisión y otro change. Las expectativas son descriptivas; no son reglas ejecutables, expectedVerdict ni ground truth del experimento. Las fixtures HTTP/HTTPS transportan respuestas sintéticas y prueban transporte/redacción, sin modelar un LLM ni depender de servicios externos.

## Uso del evaluador LLM

Los casos usan EvaluatorPort.evaluate(EvaluatorInput, EvaluationCallBudget) → EvaluatorOutcome, sin tipos del proveedor. OpenAI Responses API/Structured Outputs y modelo inicial externo gpt-6-luna forman el adapter MVP. La llamada requiere evidencia sintética completa autorizada y una tentativa por evaluación; complementa señales fuertes o apoyo semántico sin decidir por sí sola el Finding.

El resultado podrá aportar veredicto, confianza, severidad sugerida, explicación y recomendación, pero no sustituirá las comprobaciones deterministas ni la revisión de los criterios esperados. El proveedor y el modelo utilizados se configurarán externamente y se registrarán como metadatos de la evaluación, nunca sus credenciales.

## Resultado de una ejecución

La ejecución de cada caso debe conservar por separado la evidencia obtenida, el resultado determinista y la evaluación del LLM. A partir de esas señales se generará el `Finding`, cuando corresponda, con categoría, severidad, confidence, veredicto final, explicación, remediation y referencia a `testExecutionId`.

severity-v1 reserva CRITICAL sin asignación automática y permite HIGH solo con señal fuerte e impacto alto explícito por categoría. confidence-v1 es heurístico reproducible 0.00..0.95, sin calibración estadística; INCONCLUSIVE=0.00, severity null y ningún Finding. Señales, assessment, discrepancias, versiones/modelo y HYB-01..07 se conservan por separado. Fallo LLM no equivale a EvaluationResult FAILED ni a vulnerabilidad. Detalle en estrategia-evaluacion.md.

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

El dataset futuro deberá incluir casos vulnerables, no vulnerables y, si procede, ambiguos para las cuatro categorías del MVP. La [demo sintética](../../demo-vulnerable/README.md) aporta una semilla funcional versionada de ocho casos (cuatro categorías por dos perfiles), con [ground truth independiente](../../demo-vulnerable/manifest/ground-truth-v1.json); no fija el dataset académico definitivo ni acredita experimentos ejecutados. El evaluador híbrido puede conocer referencias técnicas, mientras las etiquetas/justificación y comparación TP/TN/FP/FN permanecen externas; INCONCLUSIVE se contabiliza aparte. La limitación metodológica y las tres capas se explican en el README de la demo.

La evaluación académica comparará reglas deterministas, LLM evaluador y estrategia híbrida. Las ejecuciones deberán registrar resultado esperado, resultado obtenido, estrategia, confidence, severidad, tiempo, coste y modelo evaluador para permitir el análisis de true positives, true negatives, false positives, false negatives y reproducibilidad.
