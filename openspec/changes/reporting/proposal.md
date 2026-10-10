## Why

Los resultados finales, Findings y ScanScore ya son persistentes y trazables, pero su consulta separada no ofrece un informe estable y comprensible. Reporting debe reunirlos en un snapshot estructurado, reproducible y seguro, conservando incertidumbre y limitaciones sin volver a evaluar ni calcular riesgo.

## What Changes

- Definir `SecurityReport` inmutable, identificado por reportId y único por scanId+reportVersion, con `report-v1` y locale `es`. Copiar únicamente identificación, referencia histórica del target, resumen ScanScore, metadatos finales mínimos de evaluación, Findings públicos y textos controlados de interpretación/limitaciones. `evaluations` es una proyección cerrada para interpretar resultados e INCONCLUSIVE, no una copia de EvaluationResult: excluye LlmAssessment completo, reasoning interno, prompts, schemas internos, señales deterministas internas, provider raw response, evidencia raw, chain-of-thought y secretos.
- Mantener enums/identificadores en inglés y localizar al castellano solo textos propiedad de report-v1: labels, nombres visibles, interpretation, limitations y presentación controlada, incluido un resumen determinista futuro. title, description, evidenceSummary y recommendation de Finding se copian literalmente en su idioma original: el baseline usa plantillas castellanas, pero locale=es no obliga a traducir Finding. Executive summary estructurado persistido; texto derivable mediante reglas de presentación v1, sin narrativa LLM ni servicios externos de traducción.
- Consumir literalmente el resumen global y breakdown persistidos de ScanScore; validar correspondencia de contadores y fuentes sin invocar fórmulas de scoring, reglas, evaluadores ni target. Findings ordenados por gravedad descendente, categoría e identidad estable; recomendaciones originales por Finding, sin deduplicación.
- **Extensión contractual prospectiva aprobada:** Scan nuevo con report-v1 solo confirma COMPLETED junto con ScanScore y SecurityReport en la misma T3. Mantener estados, siete campos públicos, motivos técnicos y recuperación sin relanzamiento. No cambiar semántica de scans anteriores ni evaluation/scoring-v1.
- Definir V7 para `security_reports` con metadatos relacionales y snapshot JSONB cerrado/acotado, más marcador nullable `scans.report_version` fijado en T1. V1–V6, global_score y datos existentes quedan intactos; sin backfill.
- Añadir GET scoped `/api/projects/{projectId}/scans/{scanId}/report`, de solo lectura: `{availability,report}`. AVAILABLE con snapshot; PENDING activo sujeto a reporting; UNAVAILABLE FAILED sujeto a reporting sin snapshot; NOT_APPLICABLE histórico sin marcador. Ausencia siempre report=null; incoherencia/lectura fallida es error técnico, no disponibilidad normal.
- Hacer explícitos score 0, NOT_VULNERABLE, INCONCLUSIVE, fallos técnicos, alcance OUTPUT_ONLY, heurística y límites de privacidad. No exponer URL/configuración actual como si fuera histórica: solo targetConfigurationId/targetUpdatedAt persistidos en el plan.
- Mantener máximo cuatro Findings en report-v1, derivado del plan MVP: cuatro SecurityTests, una evaluación por categoría y máximo un Finding por evaluación. No es una propiedad universal de Reporting; múltiples tests por categoría exigirán revisar/versionar este límite y su presupuesto de tamaño en otro change.

## Non-Goals

Esta entrega no implementa código, SQL ni endpoints. El incremento aprobado no incluye frontend, render/export HTML/Markdown/PDF/DOCX, email, S3/AWS, enlaces públicos, nuevas pruebas de seguridad, traducción automática/i18n completo, autenticación avanzada, narrativa LLM, cambios de evaluación/scoring ni experimento académico. No generar informes parciales o históricos bajo demanda; no hash nuevo, retries, colas o reparación automática.

## Capabilities

### New Capabilities

- `reporting`: snapshot mínimo/versionado de resultados finales, interpretación controlada, límites/privacidad, persistencia y consulta, finalización atómica e idempotencia/recuperación.

### Modified Capabilities

Ninguna spec consolidada: `openspec/specs/` solo contiene `.gitkeep`. Los seis changes previos implementados/aprobados constituyen el baseline y no se editan, sincronizan ni archivan. RP-08..RP-11 extienden expresamente SC-07..SC-10 y EE-10/EE-11 solo para nuevos scans marcados report-v1; COMPLETED anteriores conservan su significado. Las exclusiones de reporting de los changes previos delimitan su alcance histórico y no son contradicciones.

## Impact

- Backend futuro: resultado de dominio independiente de infraestructura; GenerateReportUseCase/GetSecurityReportUseCase, proyecciones/puertos mínimos y adaptadores PostgreSQL/REST/presentación es; integración dirigida en TransactionalScanLifecycle y configuración de creación/recuperación. No dependencias nuevas ni refactorizaciones ajenas.
- Persistencia aprobada para implementación futura: V7 nueva, security_reports híbrida (metadatos+JSONB), UNIQUE(scan_id,report_version), sin tablas hijas ni agregados por categoría. Una sola fila y confirmación conjunta; fuentes anteriores conservadas.
- API aprobada para implementación futura: un GET específico sin duplicar report en Scan ni añadir endpoints de generación/exportación. Reutilizar convenciones de errores UUID/scoping/no-store; availability de reporting con tipo propio aunque comparta vocabulario con scoring.
- Documentación que deberá actualizarse durante implementación aprobada: arquitectura, ADR-003 y ADR nueva de reporting, modelo de datos, OpenAPI y política de seguridad directamente afectada. No se modifican ahora ADRs, OpenAPI ni requisitos oficiales del TFM; slides/vídeo/despliegue no son funcionalidades de reporting.
- Baseline leído: reglas/perfiles/configuración; proposal/spec/design/tasks relevantes de evaluation-engine y scoring-engine; arquitectura/ADRs, estrategia de evaluación, API actual, modelo de datos y requisitos/checklist TFM; Finding, metadatos/versiones de evaluación, ScanScoreSnapshot, CalculateScanScoreUseCase, TransactionalScanLifecycle y migraciones aplicadas. No hay contradicción objetiva que obligue a reabrir decisiones anteriores.

## Review Status

**READY FOR IMPLEMENTATION.** D-01..D-24 aprobadas expresamente por el usuario el 2026-10-10, con las aclaraciones incorporadas sobre idioma (D-04), proyección mínima de evaluations (D-03/D-24) y máximo cuatro Findings ligado al plan MVP (D-23). El resto de decisiones permanece sin cambios; no quedan decisiones contractuales pendientes. Esta entrega actualiza únicamente los cuatro artefactos de planificación y valida strict. La implementación requiere una petición posterior.
