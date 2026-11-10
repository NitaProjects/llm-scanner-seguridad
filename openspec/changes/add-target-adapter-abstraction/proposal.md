## Why

`ExecuteSecurityTestsUseCase` consume directamente `TargetInvocationPort` y su resultado orientado a REST. D0 introduce una frontera de interacción realmente utilizada, preservando íntegramente el comportamiento REST v1 y sin activar ninguna capacidad v2.

## What Changes

- Añadir el puerto mínimo `TargetAdapter.interact(TargetConfiguration, String, ScanBudget)` y un resultado `NormalizedInteraction` de aplicación, solo en memoria.
- Añadir un bridge REST legacy que delega exactamente una vez en el `TargetInvocationPort` existente, sin tocar HTTP, render/extracción, red, cifrado ni presupuestos.
- Proyectar explícitamente los siete campos originales desde/hacia `InvocationResult`, sin pérdida, nuevos defaults o sanitización adicional.
- Cambiar la dependencia real del use case y su wiring Spring; conservar orden, clasificación de errores, interrupciones, persistencia y evaluación.
- Diseñar pruebas de ruta real, conversión reversible y equivalencia v1 incluyendo los resultados derivados.
- Relación con `generalize-target-integration`: exclusivamente contexto/roadmap D0/M1a. La epic no se implementará directamente ni aporta requisitos normativos a este change.

## Capabilities

### New Capabilities

- `target-adapter-boundary`: contrato interno mínimo de interacción, proyección legacy sin pérdida y conservación verificable del flujo existente.

La spec se incluye por petición explícita del usuario como contrato de arquitectura interna y compatibilidad; no añade comportamiento de producto ni funcionalidad nueva. No se inventan requisitos funcionales v2 para justificarla.

### Modified Capabilities

Ninguna. No cambia el comportamiento aprobado por `target-configuration`, `security-test-execution`, `evaluation-engine`, `scoring-engine` o `reporting`; tampoco se importan NI-01/NI-02 de la epic. No hay spec principal consolidada que modificar.

## Impact

Solo backend: nuevos tipos de aplicación y bridge legacy, `ExecuteSecurityTestsUseCase`, `ScanConfiguration`, `SecurityTestExecutionConfiguration` y tests relacionados. Ningún cambio de frontend/API pública/DTOs, dominio persistido, BD, JPA, migraciones, scoring, reporting, dependencias o configuración operativa.

Fuera de alcance: adapters OpenAI-compatible, streaming/SSE/JSONL/WebSockets, sesiones/workflows/polling, retries nuevos, registry/capabilities/describe, lifecycle complejo, revisions, metadata de proveedor/modelo, referencias de evidencia, nuevos perfiles/selectores, report-v2, precision/recall, AWS y retirada de demo. No se implementa código, no se ejecutan tests/builds, no se crean migraciones ni commits/pushes en esta planificación. Única comprobación ejecutable autorizada: validación estricta de este change.
