## Why

Project y TargetConfiguration ya permiten preparar un análisis, pero falta una ejecución persistente cuyo estado pueda consultarse sin mantener abierta la petición HTTP. Este cambio define únicamente el ciclo de vida asíncrono de Scan, como base para incorporar pruebas y resultados en changes posteriores.

## What Changes

- Crear/iniciar un Scan para un Project existente con TargetConfiguration válida y persistida, sin contactar con el target.
- Definir identidad, pertenencia, estados PENDING/RUNNING/COMPLETED/FAILED, fechas y motivo general de error sanitizado.
- Definir creación asíncrona, consulta individual para polling e histórico por proyecto.
- Mantener como máximo un Scan activo por Project, executor interno de Spring acotado y configurable, transacciones independientes y recuperación simple tras reinicio.
- Separar el lanzamiento asíncrono del trabajo mediante puertos de aplicación. Definir un adaptador temporal no-op que completa exclusivamente el ciclo de vida, sin simular pruebas de seguridad.
- Aplazar totalTests/completedTests hasta SecurityTest/TestExecution, sin contadores ni porcentajes artificiales.
- Planificar V3 para evolucionar scans; V1 y V2 permanecen inmutables y global_score se conserva sin utilizarse.

## Non-goals

SecurityTest/TestCase reales, payloads, categorías OWASP concretas, TestExecution, Findings, evaluación híbrida/LLM, scoring, reports, frontend/UX, HTTP/DNS al target, descifrado de credenciales, SSRF completo, despliegue AWS, SQS, workers separados/distribuidos, CANCELLED/PARTIAL, cancelación, reintentos avanzados, DLQ y WebSocket/SSE/push.

Esta entrega actualiza únicamente artefactos OpenSpec. No implementa código ni SQL, no ejecuta migraciones ni comprobaciones de aplicación y no modifica los changes anteriores ni documentación consolidada.

## Capabilities

### New Capabilities

- `scan-execution`: creación, persistencia, ejecución asíncrona del ciclo de vida, consulta, histórico, estados, errores y recuperación de Scan.

### Modified Capabilities

Ninguna. No cambia el comportamiento aprobado de Project ni TargetConfiguration. openspec/specs/ contiene únicamente .gitkeep; se toman como referencias las specs de los changes anteriores, aceptados por el usuario como implementados, validados y aprobados.

## Impact

- Futuro backend: domain/scan, application/scan, adaptadores REST/PostgreSQL/ejecución y composición Spring, con dependencias dirigidas hacia dominio/aplicación.
- Contrato aprobado: POST /api/projects/{projectId}/scans, GET /api/projects/{projectId}/scans/{scanId} y GET /api/projects/{projectId}/scans. POST devuelve 202, representación inicial y Location.
- Persistencia aprobada: conservar FK scans.project_id → projects.id, índice y global_score existente sin usarlo; añadir mediante V3 error_reason, checks e índice único parcial de scans activos.
- Sin dependencias nuevas. Executor y transacciones usan capacidades ya presentes de Spring/Java.
- Durante la implementación posterior: actualizar únicamente las partes afectadas de OpenAPI, modelo de datos, arquitectura general, ADR-003 y decisiones abiertas. No marcar requisitos de entrega del TFM como completados por esta planificación.

## Review Status

**READY FOR IMPLEMENTATION.** El usuario ha aprobado explícitamente las decisiones D-01 a D-07 con las ocho concreciones registradas en design.md. La aprobación de diseño no autoriza implementar en esta entrega: se requiere una petición posterior de implementación. No quedan decisiones bloqueantes para este incremento.

La aprobación concreta dos referencias por fases: totalTests/completedTests de ADR-003 se incorporarán con SecurityTest/TestExecution; la exigencia de prompts de docs/04-ia/casos-de-prueba.md se mantiene antes de ejecutar pruebas reales, mientras el no-op actual solo valida el ciclo de vida. COMPLETED en esta fase no implica pruebas de seguridad ejecutadas ni resultados de análisis. La actualización documental afectada queda en las tareas futuras; no cambia el compromiso del MVP completo ni se modifica ahora documentación consolidada.

Pool y cola son configuración técnica del executor, no límites numéricos rígidos del contrato funcional. Sus valores iniciales se documentarán en la configuración del backend al implementar, conforme a D-02, sin introducir ahora cambios de código o configuración.

Referencias revisadas: reglas y perfil de arquitectura, openspec/config.yaml, specs y artefactos completos de project-management/target-configuration, docs/00-contexto/, arquitectura general y ADR-003/decisiones abiertas, modelo de amenazas, política de autorización vacía, criterios de seguridad/evaluación, contrato del target, OpenAPI, modelo de datos, V1/V2 y puertos/composición existentes. No se deduce una política de autorización nueva de documentos vacíos.
