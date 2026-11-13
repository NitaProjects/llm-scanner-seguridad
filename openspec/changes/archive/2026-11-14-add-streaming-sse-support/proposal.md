## Why

Los targets que entregan texto por eventos requieren leer, validar y reconstruir la respuesta antes de evaluarla.

## What Changes

Una petición POST JSON con salida SSE o JSONL, selección explícita de eventos, acumulación acotada y un resultado final por prueba.

El binding fija el `Accept` efectivo de su ruta según el formato declarado. Una colisión de `Accept` con un header de autenticación, sensible o reservado invalida el binding antes de red. REST_LEGACY y OPENAI_COMPATIBLE sin binding conservan su negociación actual.

## Capabilities

### New Capabilities

- `target-event-streaming` cubre SSE, JSONL, finalización, presupuestos y normalización de texto.

### Modified Capabilities

Los contratos consolidados conservan su alcance. La capacidad se activa por configuración explícita y se verifica en este change.

## Impact

Transporte HTTP, configuración operativa y fixtures de protocolo. El Scan consume un único resultado por prueba. El índice de arquitectura y dependencias está en [generalize-target-integration](../generalize-target-integration/roadmap.md).

Fuera de alcance de este incremento: Sesiones, cookies dinámicas, workflows y herramientas.
