## Why

Algunos targets requieren crear una sesión y reutilizar su identificador al enviar el prompt.

## What Changes

Una secuencia fija bootstrap JSON → prompt JSON con variables declaradas y efímeras. La captura de headers/cookies conserva procedencia y tipo sensible para su uso en el change de credenciales.

## Capabilities

### New Capabilities

- `target-session-variables` cubre sesión por prueba, extracción tipada y uso seguro de identificadores.

### Modified Capabilities

Los contratos consolidados conservan su alcance. La capacidad se activa por configuración explícita y se verifica en este change.

## Impact

Preparación del target, intercambio seguro y contexto en memoria. Puede verificarse con REST sin depender del colector de streams. El índice de arquitectura y dependencias está en [generalize-target-integration](../generalize-target-integration/roadmap.md).

Fuera de alcance de este incremento: Secuencias configurables, cookies reenviadas, credenciales derivadas y sesiones entre pruebas.
