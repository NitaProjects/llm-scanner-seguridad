## Why

El scan real de Groq completó cuatro ejecuciones y cuatro evaluaciones. El motivo observado `CONTEXT_UNAVAILABLE` indica que el motor recibió texto pero carecía de un escenario sintético aplicable. Incluso con contexto, la política actual puede bloquear un positivo determinista por falta de permiso para exportar la respuesta.

## What Changes

- Extender el manifiesto local de escenarios con una versión opt-in que permita declarar contexto sintético para una a cuatro pruebas. Fijar cada escenario al proyecto, configuración/fecha del target y revisiones del catálogo.
- Evaluar de forma independiente la exposición y las expectativas seguras en v2. Un conflicto material produce `INCONCLUSIVE/SIGNAL_CONFLICT` y conserva las dos señales para revisión.
- Exigir en LLM10 una infracción estructural verificable del contrato `OUTPUT_ONLY`. Los literales del marcado solicitado y los literales configurados quedan fuera de la señal positiva v2.
- Verificar las coincidencias completas de LLM02/08 contra la respuesta saneada dentro de la cota de texto existente. Conservar el intervalo completo para auditoría y una referencia de excerpt acotada. Una infracción fuerte verificable y sin conflicto permite un Finding local con omisión semántica deliberada. La interpretación sigue sujeta al permiso y al corpus sintético exacto.
- Comparar dos scans `hybrid-v2` con la misma respuesta saneada de LLM02. A carece de entrada LLM02 y da `CONTEXT_UNAVAILABLE`. B vincula el canario y da HYB-02 y un Finding si no hay conflicto, con cero llamadas y reservas externas. Groq aporta evidencia complementaria.

## Capabilities

### New Capabilities

- `real-target-evaluation-context` incorpora el contrato prospectivo de contexto sintético por prueba y la decisión local para targets autorizados.

### Modified Capabilities

Ninguna spec principal consolidada. La variante opt-in extiende los contratos históricos EE-01..06 sin editar sus artefactos ni cambiar `hybrid-v1`. En particular, la omisión semántica ante una infracción fuerte es propia de la política nueva.

## Impact

La implementación futura se concentra en el manifiesto, la preparación de evaluación y el despacho de política. Se reutilizan el catálogo, las reglas, el perfil de Scan, las columnas actuales de evaluación, el API y el cierre de scoring-v1/report-v1. REST_LEGACY y OPENAI_COMPATIBLE comparten el mismo camino después de confirmar TestExecution.

El alcance se limita a escenarios sintéticos controlados sobre targets reales autorizados. Quedan fuera el envío de texto real libre, los cambios de prompts y categorías, la adjudicación persistida, frontend nuevo, SSE, sesiones, workflows, WebSockets, RAG, tools, agentes, multimodal, scoring nuevo y AWS. Este change entrega planificación.
