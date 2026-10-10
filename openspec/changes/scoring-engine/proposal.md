## Why

Los Findings y EvaluationResults aprobados permiten explicar cada prueba, pero falta un resumen agregado del riesgo que conserve severidad, incertidumbre y trazabilidad. Este incremento define una heurística local y versionada, sin convertir la ausencia de detecciones en garantía de seguridad ni diluir Findings HIGH/CRITICAL.

## What Changes

- Introducir `ScanScore` inmutable, con score entero de riesgo `0..100`, `RiskLevel` separado de `Severity`, contadores y breakdown estructurado de las cuatro categorías OWASP. Las categorías son parte de ScanScore, sin agregados, identidad, repositorios, tablas ni lifecycle independientes.
- Adoptar `scoring-v1`: severidad dominante, ajuste positivo limitado de confidence y acumulación amortiguada/acotada. Sin medias, descuentos por resultados negativos ni pesos por categoría. Score 0 significa «ningún Finding detectado en las pruebas completadas», nunca «aplicación segura».
- Consumir exclusivamente Findings y decisiones finales persistidas; comprobar el plan completo y su integridad. Conservar INCONCLUSIVE explícitos, sin puntuarlos como vulnerabilidades ni tratarlos como NOT_VULNERABLE.
- **Cambio contractual prospectivo aprobado:** para scans nuevos marcados con `scoring-v1`, COMPLETED requerirá también un score confirmado. Insertar score y finalizar Scan en la misma transacción corta; conservar estados, siete campos públicos, motivos de error y recuperación sin relanzamiento.
- Definir V6 con tabla `scan_scores`, clave `(scan_id, scoring_version)` y marcador interno nullable de versión requerida en scans. Mantener V1–V5, históricos y `global_score` sin cambios ni backfill. La reproducibilidad se basa en inputs persistidos e inmutables más scoringVersion; no se calcula ni almacena input hash en MVP.
- Añadir una consulta scoped de score, sin duplicarlo en el DTO de Scan: AVAILABLE cuando existe ScanScore; PENDING mientras se procesa un scan sujeto a scoring; UNAVAILABLE si ese scan terminó FAILED sin score; NOT_APPLICABLE para históricos anteriores. En ausencia de score se devuelve null. No ampliar reporting ni frontend.

## Non-Goals

No implementar código, SQL, migraciones ni endpoints en esta entrega. No reports/PDF, dashboard, frontend, ML, CVSS, modelos estadísticos, billing, multiusuario, AWS, nuevos SecurityTests, cambios de evaluación/severity/confidence, revisión manual, recalculado público, reintentos o infraestructura distribuida. No iniciar otro change ni archivar/sincronizar los anteriores.

## Capabilities

### New Capabilities

- `scoring-engine`: inputs íntegros, fórmula reproducible, incertidumbre explícita, ScanScore versionado, finalización atómica, persistencia conservadora y consulta mínima.

### Modified Capabilities

Ninguna spec consolidada: `openspec/specs/` solo contiene `.gitkeep`. Los cinco changes previos aprobados son el baseline y permanecen intactos. SC-07..SC-10 extienden expresamente SE-05/SE-07/SE-08, STE-10/STE-12 y EE-10..EE-13 solo para scans nuevos con versión de scoring fijada. Las exclusiones de scoring de los incrementos anteriores describen su alcance histórico; no se corrigen ni se reinterpretan retrospectivamente.

## Impact

- Backend futuro: dominio `scoring`, casos de uso explícitos de cálculo/consulta, puertos de lectura mínima y persistencia, adaptadores PostgreSQL/REST y composición dirigida en `TransactionalScanLifecycle`. No modificar Project, TargetConfiguration, SecurityTest, TestExecution, EvaluationResult ni Finding.
- API aprobada: GET `/api/projects/{projectId}/scans/{scanId}/score`; el recurso Scan conserva sus siete campos. Consulta sin target, credenciales, proveedor, evidencia ni cálculo implícito.
- Persistencia aprobada: V6 nueva, sin reutilizar ni proyectar `scans.global_score`, sin input_hash ni tablas de categoría. No dependencias nuevas ni cambios de despliegue.
- Documentación futura directamente afectada: arquitectura general, ADR-003/ADR-005 y sección 6 de decisiones abiertas, semántica de scoring en estrategia de evaluación, OpenAPI y modelo de datos. Se actualizará al implementar un diseño aprobado; esta entrega solo crea OpenSpec y ejecuta strict. No cambia requisitos oficiales de entrega del TFM.
- Baseline revisado: reglas/perfil/configuración; cinco changes aprobados; docs/00-contexto, 01-investigacion, 02-arquitectura, 04-ia, 05-api, 06-base-de-datos y 07-memoria; V1/V3/V5, Finding, Scan, ProcessScanUseCase, EvaluationRepositoryPort, TransactionalScanLifecycle y ScanStartup. Se respetan confidence-v1, severity-v1, CRITICAL reservado, LLM10 OUTPUT_ONLY y separación del experimento académico.

## Review Status

**READY FOR IMPLEMENTATION.** D-01..D-17 aprobadas expresamente el 2026-10-10, con tres ajustes incorporados: sin input hash; availability formalizada; categorías como breakdown sin agregados/lifecycle propios. No quedan decisiones bloqueantes. Los pesos provisionales 1/3/7/10 de ADR-005 se sustituyen por las bases aprobadas de scoring-v1; su actualización consolidada está prevista al implementar. La petición actual autoriza únicamente actualizar estos artefactos y ejecutar strict; no autoriza implementar. La validación strict comprueba estructura OpenSpec, no valida empíricamente la heurística.
