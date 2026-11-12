## Why

La frontera TargetAdapter implementada permite incorporar un contrato de chat real sin cambiar el motor de Scan. Un formulario tipado para endpoints OpenAI-compatible evita que el usuario tenga que escribir manualmente plantillas JSON y responsePath para ese contrato conocido.

## What Changes

- Proponer un corte vertical mínimo: tipo REST_LEGACY/OPENAI_COMPATIBLE, configuración API discriminada y selector/formulario Angular.
- Añadir OpenAiCompatibleTargetAdapter con model, system opcional, prompt de catálogo como user y stream=false; sin lógica por proveedor.
- Reutilizar el transporte HTTP seguro existente mediante un punto interno de extracción de respuesta, preservando el camino legacy y su semántica.
- Seleccionar entre los dos adapters con un delegador exhaustivo sencillo, sin registry/reflection ni nuevas operaciones en TargetAdapter.
- Mantener NormalizedInteraction y la proyección de sus siete valores intactos, junto al resto del pipeline.
- Persistir explícitamente adapter_type, model y system_prompt; request_template/response_path son exclusivos de REST_LEGACY y null en chat. La request chat se genera solo en memoria. Diseñar una única migración incremental con nulabilidad/checks por rama, sin tablas, config JSON o revisiones. No se crea ni ejecuta SQL en esta fase.
- Proteger la actualización mediante escritura condicional por adapter_type esperado, incluida configuración y credencial en la misma operación/transacción; evitar que un PUT antiguo sobrescriba una conversión concurrente a chat, sin versionado general de entidades.
- Prever tests aislados y validación real posterior, expresamente autorizada, con Groq inicialmente u OpenRouter como alternativa.

## Capabilities

### New Capabilities

- `openai-compatible-target`: configuración tipada, invocación/errores, UX y aceptación aislada/real del contrato de chat no streaming.

### Modified Capabilities

Ninguna spec principal consolidada. Este contrato añade una rama de configuración e invocación, sin modificar el comportamiento legacy de target-configuration ni los contratos de evaluación/scoring/reporting. add-target-adapter-abstraction es la base implementada; sus prohibiciones de capacidades nuevas se limitan a D0, no a este incremento posterior. generalize-target-integration es solo roadmap, sin herencia normativa.

## Impact

Backend: configuración tipada de target, JPA/mapper/repositorio y actualización condicional, validación/generador HTTP en memoria, DTO/API, routing/wiring, adapter nuevo y punto interno de construcción/extracción del HTTP existente. Frontend: tipos/decoders y formulario de target. Documentación directamente afectada: OpenAPI/contrato target, arquitectura/modelo de datos y limitaciones de uso. Implementación posterior requerirá una única migración incremental de tres columnas y constraints por rama, justificada en design.md; ahora solo se corrigen artefactos OpenSpec.

No cambia la evaluación sintética v1: una respuesta de proveedor no prevista por el corpus sigue sin exportarse al evaluador y puede resultar INCONCLUSIVE. La aceptación demuestra integración real del Scan engine, no detección general de vulnerabilidades en servicios comerciales. Se mantienen scoring-v1/report-v1 y sus límites; no se afirma que un transporte nuevo amplíe los criterios de evaluación.

Fuera de alcance: SSE/JSONL/stream=true/WebSockets, sesiones/polling/multi-turn, herramientas/agentes/RAG/multimodal, extras por proveedor, temperature/reasoning/response_format, nuevos perfiles/corpus/score/report, precision/recall, AWS/deployment y retirada de legacy. No se implementa código, crea/ejecuta SQL, instala dependencias, llama servicios externos ni hace commit/push durante este diseño.
