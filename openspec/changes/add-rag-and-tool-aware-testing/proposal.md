## Why

Una respuesta final puede ocultar qué recuperó o solicitó el target. Algunos entornos de prueba exponen eventos que permiten comprobar ese comportamiento.

## What Changes

Distinguir texto, retrieval, petición de herramienta, resultado y respuesta final. Añadir comprobaciones locales de trazas declaradas de fixtures autorizadas.

## Capabilities

### New Capabilities

- `target-retrieval-tool-observations` cubre observaciones tipadas y comprobaciones defensivas en un entorno controlado.

### Modified Capabilities

Los contratos consolidados conservan su alcance. La capacidad se activa por configuración explícita y se verifica en este change.

## Impact

Decodificación de eventos y verificación local complementaria. El evaluador actual sigue consumiendo la respuesta final. El índice de arquitectura y dependencias está en [generalize-target-integration](../generalize-target-integration/roadmap.md).

Fuera de alcance de este incremento: Despacho de herramientas, RAG poisoning, nuevos Findings o categorías, ejecución arbitraria, shell, navegador y agentes autónomos.
