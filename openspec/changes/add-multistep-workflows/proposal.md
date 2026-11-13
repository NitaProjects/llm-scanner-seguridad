## Why

Una sesión fija de dos llamadas resulta insuficiente cuando el target requiere crear chat, crear sesión y recoger eventos.

## What Changes

Componer pasos acotados en un plan JSON versionado, con dependencias anteriores, guardas simples y una única respuesta final.

Cada paso conserva exactamente el esquema, host y puerto efectivo del origen base. Los pasos intermedios extraen variables de documentos JSON completos. El paso terminal puede producir la respuesta final mediante JSON, SSE o JSONL.

## Capabilities

### New Capabilities

- `target-step-workflows` cubre plan declarativo, variables, ejecución secuencial, abortos y evidencia técnica por paso.

### Modified Capabilities

Los contratos consolidados conservan su alcance. La capacidad se activa por configuración explícita y se verifica en este change.

## Impact

Intérprete en infraestructura sobre sesiones y SSE/JSONL. La prueba y la evaluación siguen recibiendo un resultado. El índice de arquitectura y dependencias está en [generalize-target-integration](../generalize-target-integration/roadmap.md).

Fuera de alcance de este incremento: extracción de variables desde streams intermedios, bucles, polling, expresiones ejecutables, reanudación y UI de edición.
