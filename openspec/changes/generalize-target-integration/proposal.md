## Why

El [roadmap de protocolos](roadmap.md) es el índice vigente para los cinco próximos changes. Los artefactos de esta epic conservan la planificación original. La implementación y aceptación se revisan en cada change enlazado.

El contrato v1 de POST JSON con sustitución de `{{PROMPT}}` y extracción de texto excluye aplicaciones autorizadas con chat compatible con OpenAI, streaming y sesiones. Además, los criterios y permisos de exportación del evaluador v1 dependen de escenarios sintéticos: generalizar el transporte sin abordar esa frontera no convierte el scanner en una herramienta de evaluación real.

## What Changes

- Proponer una Fase 2 posterior al TFM: «Evaluar aplicaciones LLM autorizadas cuyo protocolo de interacción pueda describirse mediante uno de los adapters soportados».
- Introducir un puerto de interacción normalizada, adapters versionados y evidencia independiente del transporte, reutilizando el monolito hexagonal, la orquestación y la evaluación híbrida.
- Separar M1 REST mejorado, M2 OpenAI-compatible sin streaming, M3 SSE/JSONL, M4 workflows de sesión y M5 contrato de extensibilidad. La base mínima de M5 se necesita en M1; su estabilización termina después de M4.
- Primera unidad implementable D0/M1a: frontera normalizada y puente sobre REST legacy, sin cambios de BD/UI ni evaluación v1. Primer delivery funcional nuevo D1: revisiones, REST mínimo y OpenAI-compatible no streaming, construido en cortes independientes. D2: SSE real autorizado. La aceptación inicial completa requiere D1 y D2 con el mismo motor de seguridad; no incluye el intérprete de workflows.
- Diseñar un perfil prospectivo de evaluación con contexto declarado por el propietario, aplicabilidad por categoría y autorización de datos separada; no reutilizar ficticiamente el corpus sintético como permiso para datos reales.
- Diseñar configuración progresiva en Angular, protección de red por paso, presupuestos globales, evidencia minimizada, observabilidad y validación con ground truth independiente.
- Mantener las cuatro pruebas y categorías en D1/D2; cualquier revisión de prompts o ampliación del catálogo requiere aprobación adicional.
- Conservar `scoring-v1` para su perfil histórico; no recalibrar ni mezclar automáticamente perfiles. Proponer métricas de evidencia/cobertura separadas y versionar un informe prospectivo.
- **Compatibilidad:** API v1 y scans históricos conservan significado. Los nuevos contratos de integración, evaluación y reporting son opt-in/versionados; no se amplían silenciosamente los de v1.

## Capabilities

### New Capabilities

- `target-integration`: selección de adapter, REST mejorado, chat compatible y UX/API versionada.
- `normalized-interaction`: frontera neutral, estados, eventos, evidencia y puente hacia ejecución/evaluación.
- `target-streaming`: colección SSE/JSONL acotada y normalización determinista.
- `target-workflows`: DSL declarativa restringida para sesión, polling y streams.
- `integration-safety`: autorización de red, credenciales, privacidad y presupuestos compuestos.
- `real-target-validation`: perfil de evaluación aplicable a targets reales, métricas, informes y retirada progresiva de la dependencia de demo.

### Modified Capabilities

Ninguna spec consolidada: `openspec/specs/` está vacío. Las seis capacidades son contratos prospectivos v2 y extienden, sin reescribir, los contratos históricos de `target-configuration`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting` en sus respectivos changes. El diseño identifica expresamente las restricciones v1 que no pueden heredarse en v2. No se usa un delta MODIFIED contra una spec principal inexistente.

## Impact

La implementación futura afectará a `domain/target`, `domain/execution`, `application/execution`, preparación de evaluación, adapters HTTP/persistencia, DTOs/OpenAPI y `frontend/features/target` y `features/scans`. Reutilizará PostgreSQL, AES-GCM, el cliente HTTP existente y las transacciones cortas; no se propone un servicio distribuido ni instalar dependencias ahora. La selección de librería JSONPath/parser y su necesidad se revisará antes de aprobar implementación.

Este change contiene exclusivamente planificación pendiente de revisión. Se autoriza diseñar v2 fuera del MVP histórico, no modificar el alcance entregado del TFM. No declara decisiones aceptadas ni modifica ADRs previas.

Fuera de alcance: código, migraciones ejecutadas o creadas ahora, producción/AWS, commits/pushes/tags efectivos, borrado/traslado de demo, WebSockets, automatización de navegador, MCP, tool exploitation, RAG poisoning, multimodal, agentes complejos y corpus masivo. No se consultan proveedores ni targets externos durante este diseño; los contratos reales se comprobarán posteriormente con autorización específica.
