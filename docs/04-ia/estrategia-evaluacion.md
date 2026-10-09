# Estrategia de evaluación

## Objetivo

Evaluar si la respuesta de una aplicación LLM presenta un comportamiento asociado a la prueba ejecutada y generar un finding explicable para el usuario.

## Evaluación híbrida

La estrategia no dependerá exclusivamente de un LLM evaluador. Combinará, según el caso:

1. Reglas deterministas para patrones claros y verificables.
2. Comparación con respuestas o comportamientos esperados cuando puedan definirse.
3. Evaluación semántica mediante un modelo LLM independiente.
4. Cálculo de una escala de confianza.
5. Revisión manual opcional para resultados ambiguos o de alto impacto.

La estrategia conceptual queda cerrada en este documento. Los umbrales concretos de detección, la calibración de `confidence` y la fórmula definitiva del score deberán validarse experimentalmente.

## Severidad

Cada finding tendrá uno de estos niveles:

- `LOW`: indicio, impacto limitado o comportamiento dudoso que requiere atención, pero no evidencia una exposición relevante por sí solo.
- `MEDIUM`: vulnerabilidad o desviación relevante con impacto contenido o condiciones adicionales para materializarse.
- `HIGH`: vulnerabilidad clara con impacto importante, exposición sensible o probabilidad elevada de producir consecuencias relevantes.
- `CRITICAL`: exposición grave, compromiso evidente, afectación amplia o riesgo inmediato para la aplicación o sus datos.

La severidad representa el impacto y la relevancia del riesgo, no la confianza de la detección. Un finding puede tener severidad alta y confidence baja si la evidencia es insuficiente o existen señales contradictorias.

Los criterios concretos que asignen cada nivel a cada categoría OWASP quedan como **Pendiente de decisión**.

## Categorías cubiertas por el MVP

La estrategia se aplicará a estas cuatro categorías del OWASP GenAI LLM Top 10 (2026):

- **LLM01 – Prompt Injection**.
- **LLM02 – Sensitive Information Disclosure**.
- **LLM08 – Hidden Context Exposure**.
- **LLM10 – Improper Output Handling**.

Las seis categorías restantes quedan fuera del MVP. No se definirán payloads concretos en esta fase documental; se documentarán posteriormente dentro de los casos de prueba aprobados.

## Separación de responsabilidades

- La aplicación objetivo responde a los prompts de prueba.
- El evaluador analiza la respuesta, pero no controla el flujo del sistema.
- Las reglas deterministas aportan comprobaciones reproducibles.
- El backend consolida la evidencia y genera el finding.

La respuesta del target debe estar claramente delimitada y tratarse como contenido no confiable para reducir el riesgo de prompt injection indirecta.

## Confidence

`confidence` representa la confianza del sistema en que el finding y su veredicto final son correctos. Utilizará conceptualmente un valor entre `0.0` y `1.0`:

- `0.0`: ausencia de confianza o evidencia insuficiente.
- `1.0`: confianza máxima según las señales disponibles.

El valor se calculará combinando, cuando estén disponibles:

- Resultado de reglas deterministas.
- Coincidencia con criterios y comportamiento esperado.
- Evaluación semántica del LLM.
- Consistencia entre señales.
- Calidad, claridad y suficiencia de la evidencia.

El `confidence` no podrá copiar directamente el valor devuelto por el LLM ni depender exclusivamente de él. La fórmula, pesos y umbrales concretos quedan como **Pendiente de decisión**.

## Combinación de señales

La evaluación conservará las señales por separado y no ocultará discrepancias:

| Reglas deterministas | LLM evaluador | Tratamiento conceptual |
|---|---|---|
| Coinciden en vulnerabilidad | Coincide en vulnerabilidad | Mayor confidence por consistencia de señales. |
| Detectan vulnerabilidad | No la detecta | Se mantiene la evidencia determinista y se registra la discrepancia. |
| No detectan vulnerabilidad | Detecta vulnerabilidad | Puede generarse un finding con menor confidence o solicitar revisión. |
| Resultados contradictorios | Resultado contradictorio o incierto | Se conserva el conflicto y se evita presentar el resultado como concluyente. |

El resultado deberá conservar conceptualmente `deterministicResult`, `llmVerdict`, `finalVerdict`, `confidence` y `rationale`.

## Finding

Cada finding deberá representar la evidencia y la decisión resultante de una ejecución concreta. Sus datos conceptuales incluirán:

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

La decisión de crear un finding no eliminará las señales originales ni la explicación de las discrepancias.

## Score global del scan

El scan tendrá un score global orientativo para comparar resultados, pero no será una media simple. Considerará como mínimo:

- Número de findings.
- Severidad.
- `confidence`.
- Categoría afectada.

Como propuesta inicial de pesos de severidad:

```text
LOW      = 1
MEDIUM   = 3
HIGH     = 7
CRITICAL = 10
```

Estos pesos son provisionales y deberán validarse experimentalmente. La fórmula definitiva deberá ser transparente, reproducible y evitar que muchos hallazgos leves oculten un hallazgo crítico.

Independientemente del score global, cualquier finding `CRITICAL` deberá mostrarse claramente en el dashboard y en el informe. Un score bajo nunca podrá ocultar ni neutralizar una vulnerabilidad crítica.

La normalización, el peso de cada categoría y la influencia exacta del `confidence` quedan como **Pendiente de decisión**.

## Revisión manual

La arquitectura permitirá una revisión manual futura para confirmar un finding, rechazar un falso positivo, modificar su severidad o añadir un comentario. Esta capacidad podrá quedar fuera de la primera versión del MVP, pero debe conservarse la evidencia y las señales necesarias para incorporarla posteriormente.

## Abstracción del evaluador

La evaluación mediante LLM se realizará a través de un puerto conceptual independiente del proveedor:

```text
EvaluatorPort
    ↓
Adaptador de proveedor concreto
```

El puerto podrá representar conceptualmente:

```text
evaluate(
    securityTest,
    targetRequest,
    targetResponse,
    deterministicResults
) → EvaluationResult
```

El resultado podrá incluir:

- `verdict`.
- `confidence`.
- `reasoningSummary`.
- `suggestedSeverity`.
- `recommendation`.
- `evaluatorProvider`.
- `evaluatorModel`.
- `evaluatedAt`.

El MVP utilizará inicialmente un único proveedor y modelo configurables mediante configuración externa o variables de entorno. El dominio y los casos de uso no dependerán de nombres concretos de proveedores o modelos.

El LLM evaluador podrá aportar interpretación semántica, explicación y recomendación, pero el resultado final deberá combinar reglas deterministas, criterios esperados y evaluación mediante LLM. No se considerará suficiente únicamente el veredicto del modelo.

## Evaluación dentro de scans asíncronos

Las reglas deterministas y las llamadas al modelo evaluador se ejecutarán como parte del procesamiento en segundo plano del scan. Cada `TestExecution` debe conservar su resultado, confianza y fecha, o un error controlado cuando la evaluación no pueda completarse.

Un timeout o fallo del target debe diferenciarse de un fallo del modelo evaluador. Ninguno de los dos debe convertirse automáticamente en un finding de vulnerabilidad. La estrategia definitiva de reintentos, recuperación y tratamiento de ejecuciones incompletas queda como **Pendiente de decisión**.

## Información de una evaluación

Cada evaluación debería conservar, cuando sea necesario y de forma segura:

- Identificador del caso de prueba.
- Objetivo de la prueba.
- Petición enviada, sanitizada cuando contenga headers o datos sensibles.
- Respuesta recibida, con minimización o redacción de datos sensibles.
- Reglas aplicadas y resultado.
- Resultado del evaluador LLM.
- Confianza.
- Veredicto final.
- Fecha y hora de ejecución.
- Severidad propuesta.
- Explicación y recomendación.

El proveedor y el modelo utilizados deberán quedar identificados en el resultado de evaluación, sin almacenar sus credenciales.

## Limitaciones

- El evaluador LLM puede interpretar incorrectamente una respuesta.
- Dos ejecuciones pueden producir resultados diferentes.
- Una respuesta aparentemente segura no demuestra la ausencia de vulnerabilidades.
- Los resultados dependen de la calidad de los casos de prueba y de los criterios definidos.
- La cobertura de LLM10 será parcial si no se puede observar el sistema que consume posteriormente la salida del modelo.
- La disponibilidad, coste, límites y variabilidad del proveedor evaluador pueden afectar a la reproducibilidad.
- Un score global es un resumen y no sustituye la revisión de findings individuales.

## Límites y configuración operativa

La integración del evaluador deberá contemplar, como mínimo:

- Coste por llamada.
- Número máximo de evaluaciones por scan.
- Timeout.
- Rate limits.
- Longitud máxima de prompts y respuestas.
- Fallback controlado ante fallo del proveedor.

Los valores concretos y la estrategia definitiva de fallback quedan como **Pendiente de decisión**. Las API keys y demás secretos deberán proceder de configuración externa, nunca de código, logs, findings o informes.

## Evaluación académica

El diseño experimental se documenta en `docs/01-investigacion/experimento-academico.md`.

Se compararán tres estrategias:

1. Solo reglas deterministas.
2. Solo LLM evaluador.
3. Evaluación híbrida.

El conjunto controlado cubrirá LLM01, LLM02, LLM08 y LLM10, utilizando principalmente `demo-vulnerable/` y casos no vulnerables equivalentes. Se registrarán true positives, true negatives, false positives, false negatives, precision, recall, reproducibilidad, tiempo medio y coste aproximado.

La cantidad de casos, repeticiones, ground truth definitivo, tratamiento estadístico de `INCONCLUSIVE` y valores objetivo quedan como **Pendiente de decisión**.
