## Why

Algunos endpoints autorizados exigen cookies de sesión o un token CSRF devuelto en un paso anterior.

## What Changes

Permitir bindings explícitos de valores sensibles a headers/cookies autorizados del mismo origen y dentro de la misma interacción.

## Capabilities

### New Capabilities

- `target-scoped-credentials` cubre uso de credenciales efímeras por paso con ámbito de origen y restricciones de cookies y headers.

### Modified Capabilities

Los contratos consolidados conservan su alcance. La capacidad se activa por configuración explícita y se verifica en este change.

## Impact

Extiende pasos y transporte seguro. Conserva Bearer estático y control de origen. El índice de arquitectura y dependencias está en [generalize-target-integration](../generalize-target-integration/roadmap.md).

Fuera de alcance de este incremento: Navegador, redirecciones, propagación automática, OAuth y sustitución dinámica de Authorization.
