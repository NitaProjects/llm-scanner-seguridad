## Why

El MVP y su evaluador real ya están integrados y validados, pero aún faltan controles de entrada y un procedimiento reproducible para publicarlo de forma académica controlada en AWS antes del 26 de octubre de 2026. F1–F4 deben resolverse sin convertirlo en plataforma pública ni cambiar el core.

## What Changes

- Diseñar una única EC2 con Nginx/Angular estático, un backend Spring Boot y PostgreSQL 16 aislado; configuración de producción separada del Compose local.
- Resolver F1 con HTTPS443 públicamente accesible y Basic Auth obligatorio en Nginx para UI/API, sin conocer la IP del tribunal ni añadir usuarios/roles internos; allowlist IP opcional y backend inaccesible directamente desde Internet.
- Terminar TLS en Nginx y provisionar/renovar certificado Let's Encrypt mediante Certbot manual DNS-01 desde el equipo del operador, sin abrir80 ni añadir contenedores; dominio/DNS bajo control del operador y material TLS real fuera del repositorio.
- Resolver F2 con límite previo de body, buffering y timeouts de recepción en el único punto de entrada; comprobar ausencia de bypass.
- Resolver F3 con credenciales nuevas, red privada de BD, persistencia cifrada y arranque Flyway desde BD limpia, sin copiar datos locales.
- Resolver F4 con README verificable desde checkout limpio, builds y variables no secretas, distinguiendo entorno local y producción, incluido que Spring no carga .env automáticamente.
- Mantener pro, una sola réplica, healthchecks reales existentes y logging mínimo. No desplegar demo-vulnerable: demostrar su recorrido y evaluator real localmente con evidencia identificable, sin relajar SSRF.
- Diseñar provisión mínima de secretos en Parameter Store SecureString/KMS y distribución por entorno del backend, preservando la clave AES de credenciales de targets.

**Fuera de alcance:** implementación en esta petición, AWS/Internet/consolas, despliegue, nuevos Scans o llamadas OpenAI, HA, RDS/ALB/NAT Gateway, SQS, workers distribuidos, Kubernetes, autoscaling, plataforma multiusuario, login de aplicación, nuevas categorías, features, refactors, tuning y benchmark. La ejecución posterior necesita autorización separada.

## Capabilities

### New Capabilities

- `aws-production-readiness`: garantías de exposición, configuración y reproducción para una instalación académica AWS de una instancia, con criterios F1–F4 y separación explícita de la demo local.

### Modified Capabilities

Ninguna del core. Se conservan contratos de API/dominio, single-instance de ADR-003, SSRF de target-configuration/security-test-execution y gates del evaluator. Los controles nuevos son de operación/entrada; no se reescriben changes históricos. La decisión operativa de secretos PRE/PRO pendiente en ADR-002 se concreta como propuesta acotada a este entorno, para aprobación e incorporación documental durante implementación.

## Impact

Esta entrega crea únicamente los cuatro artefactos de planificación. La implementación posterior afecta a empaquetado/configuración de despliegue nuevos y separados en docker/production, bootstrap mínimo, README y documentación/ADR directamente afectados. No se modifica el Compose local, dependencias/lockfiles, migraciones existentes, backend core ni frontend funcional. No se necesita un endpoint de health nuevo.

Estado: **READY FOR IMPLEMENTATION** del diseño; no acredita preparación operativa ni despliegue realizado. Región, dominio/certificado, credenciales Basic Auth de prueba del TFM, identificadores AWS, versiones/digests exactos de imágenes y aprobación de coste se completan antes de desplegar. No se requiere conocer la IP del tribunal; los CIDRs solo se configuran si el operador activa la restricción opcional. La checklist TFM mantiene sus estados actuales hasta disponer de evidencia real.

Validación documental del 2026-10-20: `openspec validate aws-production-readiness --strict` → `Change 'aws-production-readiness' is valid`. Cuatro artefactos completos; ninguna tarea de implementación o despliegue ejecutada.
