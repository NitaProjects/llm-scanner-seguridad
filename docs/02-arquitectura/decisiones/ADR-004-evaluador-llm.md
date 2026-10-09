# ADR-004: Abstracción y proveedor del LLM evaluador

## Estado

Aceptada para la arquitectura del MVP.

El proveedor, el modelo concreto y los límites operativos permanecen **Pendientes de decisión**.

## Contexto

LLM Scanner Seguridad necesita interpretar semánticamente algunas respuestas del target. Esta evaluación puede complementar reglas deterministas y criterios esperados, pero no debe convertir al sistema en dependiente de un proveedor concreto ni delegar en un LLM toda la decisión de vulnerabilidad.

## Decisión

La integración con el modelo evaluador se realizará mediante un puerto conceptual `EvaluatorPort`, implementado por adaptadores de proveedores concretos.

```text
EvaluatorPort
    ↓
Adaptador de proveedor
```

Durante el MVP se utilizará inicialmente un único proveedor y modelo. El proveedor y el modelo se configurarán mediante variables de entorno o configuración externa. El dominio y los casos de uso no incluirán nombres específicos de modelos o proveedores.

Podrán existir adaptadores futuros como:

- `OpenAiEvaluatorAdapter`.
- `GeminiEvaluatorAdapter`.
- `HuggingFaceEvaluatorAdapter`.

Estos nombres son ejemplos arquitectónicos y no implican que el proveedor correspondiente esté seleccionado para el MVP.

## Responsabilidades del evaluador

El evaluador podrá:

- Interpretar semánticamente respuestas del target.
- Detectar comportamientos que no puedan resolverse solo con reglas.
- Proponer una categoría de riesgo.
- Generar una explicación técnica.
- Generar una recomendación o remediation.
- Devolver un nivel de confianza.

El resultado final se construirá mediante evaluación híbrida:

```text
Reglas deterministas
        +
Criterios esperados
        +
LLM evaluador
        =
Resultado de evaluación
```

El veredicto del LLM no será, por sí solo, una prueba suficiente de vulnerabilidad.

## Contrato conceptual

El puerto podrá representar una operación similar a:

```text
evaluate(
    securityTest,
    targetRequest,
    targetResponse,
    deterministicResults
) → EvaluationResult
```

`EvaluationResult` podrá contemplar:

- `verdict`.
- `confidence`.
- `reasoningSummary`.
- `suggestedSeverity`.
- `recommendation`.
- `evaluatorProvider`.
- `evaluatorModel`.
- `evaluatedAt`.

Este ADR no define clases Java ni el contrato definitivo de la API interna.

## Seguridad, coste y límites

- Las API keys nunca se hardcodearán ni se incluirán en el repositorio.
- Los secretos no aparecerán en logs, findings ni informes.
- El target y sus respuestas se tratarán como contenido no confiable.
- Se limitarán el coste por llamada, el número de evaluaciones por scan, el timeout, el rate limiting y la longitud de prompts y respuestas.
- Los fallos del proveedor se diferenciarán de las vulnerabilidades del target.
- El comportamiento de fallback será controlado y no se asumirá automáticamente.

Los valores concretos y la estrategia de fallback quedan **Pendientes de decisión**.

## Fuera del MVP / Evolución futura

- Múltiples proveedores configurables simultáneamente.
- Fallback automático entre proveedores.
- Comparación entre modelos.
- Evaluación por consenso.
- Modelos locales u open source.
- Selección dinámica según coste y calidad.
- Benchmarking específico de evaluadores.

