## Why

Los componentes del MVP tienen contratos e implementaciones separados, pero todavía falta evidencia del recorrido local Angular → scanner → demo → ejecución/evaluación → score/report → frontend. `frontend-mvp` 9.4 sigue pendiente y el implementation-agent necesita un change revisable que delimite la operación E2E sin activar un proveedor externo.

## What Changes

- Definir un recorrido obligatorio OFFLINE con demo `VULNERABLE`, PostgreSQL de desarrollo, target y binding reales, cuatro categorías y comprobación de resultados observados.
- Preparar un procedimiento reproducible de arranque manual, preflight, ejecución desde frontend, polling acotado y recopilación mínima de evidencia. Proponer como apoyo opcional un único script PowerShell para APIs locales, sin gestión de procesos ni llamadas de proveedor.
- Verificar la semántica vigente: contexto válido y exportación deshabilitada producen evaluaciones COMPLETED/INCONCLUSIVE, LLM SKIPPED, cero Findings y score cero con incertidumbre; no forzar detecciones.
- Separar el oráculo de los requests del target y del evaluador; contrastar resultados mediante una matriz externa TP/TN/FP/FN/INCONCLUSIVE.
- Delimitar la evidencia parcial de 9.4: el recorrido offline sin Findings no demuestra abrir un Finding real. Conservar pendiente ese criterio.
- Documentar, sin ejecutar ni convertir en aceptación obligatoria, el procedimiento futuro con evaluador real y el control SAFE opcional.

### Non-goals

No implementar en esta fase. No activar OpenAI, suministrar claves reales, arrancar servicios, ejecutar E2E/tests/builds/migraciones, cambiar backend/frontend/demo, arquitectura, contratos, catálogo, reglas, scoring/reporting, dependencias o despliegue. Sin AWS, PDF, benchmark académico formal, precision/recall, nuevos cambios visuales, refactors, CI/CD ni Docker global. No cerrar decisiones pendientes ajenas ni requisitos de entrega TFM.

## Capabilities

### New Capabilities

- `e2e-integration`: procedimiento e instrumentación operacional local reproducible, contrato de preflight, límites de ejecución y evidencia de integración del MVP existente.

### Modified Capabilities

Ninguna. Se consumen los contratos aprobados de `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine`, `reporting`, `frontend-mvp`, `frontend-polish` y `demo-vulnerable` sin redefinirlos. La tarea 9.4 conserva su criterio original.

## Impact

Esta propuesta crea únicamente artefactos dentro de `openspec/changes/e2e-integration/`. La futura implementación, previa aprobación, entregaría un procedimiento manual reproducible y evidencia dentro del change; `scripts/e2e-local.ps1` sería apoyo opcional cuya ausencia no bloquea aceptación; no necesita cambios de producto ni dependencias nuevas. Las lecturas HTTP públicas son la fuente de estados/resultados; para respuesta sintética persistida y señales, no expuestas por API, se conserva una consulta local acotada de solo lectura como diagnóstico complementario sujeto a autorización. APIs + UI bastan para aceptación funcional; campos internos no inspeccionados se registran como NOT_INSPECTED sin inventar valores. No se añade una API de evidencia ni se modifica la BD.

Compatibilidad observada: Angular 4200 y proxy `/api/**` → `localhost:8080`; scanner 8080; demo loopback 8081; PostgreSQL 5432. E2E manual usa `llm_scanner_seguridad`; tests automáticos con BD usan exclusivamente `llm_scanner_seguridad_test`. El estado tras corregir F1/F2 es READY FOR RE-REVIEW; no constituye aprobación para implementar u operar servicios.
