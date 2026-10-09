# ADR-005: Severidad, confidence y scoring de scans

## Estado

Aceptada como decisión conceptual para el MVP.

La fórmula numérica definitiva, los umbrales, la calibración de `confidence` y la revisión manual permanecen **Pendientes de decisión**.

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
- Si el LLM detecta una vulnerabilidad y las reglas no, podrá generarse un finding con menor confianza o pendiente de revisión.
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

No se definen clases Java concretas en este ADR.

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

