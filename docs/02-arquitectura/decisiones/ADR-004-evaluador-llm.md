# ADR-004: Abstracción y proveedor del LLM evaluador

## Estado

Aceptada para la arquitectura del MVP.

Proveedor, modelo inicial y límites aprobados por `evaluation-engine`, D-07..D-09/D-15: OpenAI, `gpt-6-luna` configurable, Responses API y Structured Outputs con JSON Schema estricto. No se ha verificado una cuenta real ni su disponibilidad o tarifa mediante Internet.

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

El único adaptador MVP es `OpenAiEvaluatorAdapter`; los demás permanecen como evolución futura.

## Responsabilidades del evaluador

El evaluador podrá:

- Interpretar semánticamente respuestas del target.
- Detectar comportamientos que no puedan resolverse solo con reglas.
- Interpretar la categoría y los criterios fijados por el backend sin modificarlos.
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

El contrato aprobado e implementado es:

```text
evaluate(EvaluatorInput, EvaluationCallBudget) → EvaluatorOutcome
```

El outcome contiene assessment validado o fallo tipado; no contiene el verdict híbrido ni tipos HTTP/SDK. El assessment conserva:

- `verdict`.
- `confidence`.
- `reasoningSummary`.
- `suggestedSeverity`.
- `recommendation`.
- `evidenceReferences` verificadas contra la evidencia proyectada.

`EvaluationResult` conserva separadamente señales, assessment sanitizado, decisión final, proveedor/modelo solicitado y resuelto, versiones, fechas, tamaños y uso/coste conocido. El engine decide Finding, confidence y severity. No se almacena chain-of-thought.

## Seguridad, coste y límites

- Las API keys nunca se hardcodearán ni se incluirán en el repositorio.
- Los secretos no aparecerán en logs, findings ni informes.
- El target y sus respuestas se tratarán como contenido no confiable.
- Se limitarán el coste por llamada, el número de evaluaciones por scan, el timeout, el rate limiting y la longitud de prompts y respuestas.
- Los fallos del proveedor se diferenciarán de las vulnerabilidades del target.
- El comportamiento de fallback será controlado y no se asumirá automáticamente.

Límites: cuatro intentos por Scan, evidencia 16 KiB, input completo 24 KiB, respuesta HTTP 16 KiB, 1024 output tokens por defecto, timeout 20 s incluyendo espera, concurrencia global uno e intervalo mínimo 1 s; deadline compartido de Scan 300 s. Exportación deshabilitada por defecto, solo corpus sintético completo autorizado para proveedor/escenario. API key únicamente en transporte desde `EVALUATOR_API_KEY`; `EVALUATOR_PROVIDER` y `EVALUATOR_MODEL` son externos. Destino fijo, TLS/hostname, cero redirects/retries/proxy/tools.

Timeout/5xx/formato inválido son fallos individuales del componente; 429/auth/cupo suprimen llamadas posteriores de ese Scan. La consolidación coherente termina COMPLETED, con `errorReason=null` y `llmErrorReason` separado: señal fuerte permite VULNERABLE; sin ella INCONCLUSIVE. No se repite la llamada ni se cambia proveedor. El control monetario estimado es opcional y exige tarifa/perfil versionados compatibles; desconocido es null, no cero.

## Fuera del MVP / Evolución futura

- Múltiples proveedores configurables simultáneamente.
- Fallback automático entre proveedores.
- Comparación entre modelos.
- Evaluación por consenso.
- Modelos locales u open source.
- Selección dinámica según coste y calidad.
- Benchmarking específico de evaluadores.

