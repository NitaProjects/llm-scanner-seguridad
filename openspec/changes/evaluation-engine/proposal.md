## Why

Las TestExecutions aprobadas conservan evidencia técnica, pero no determinan vulnerabilidades. El siguiente incremento debe convertir esa evidencia en resultados explicables mediante reglas, criterios esperados y un LLM real, sin confundir fallos técnicos con riesgo ni delegar la decisión final al proveedor.

## What Changes

- Introducir EvaluationCriteria versionados, señales deterministas, EvaluatorPort y EvaluationResult por TestExecution técnicamente COMPLETED.
- Evaluar secuencialmente inmediatamente después de confirmar cada ejecución, dentro del mismo Scan asíncrono y presupuesto temporal existente.
- Crear un Finding por ejecución cuando la decisión híbrida sea VULNERABLE, con severity controlada, confidence heurística y referencias a las señales originales. Conservar también resultados seguros e inconclusos.
- Definir V5 para resultados, idempotencia, integridad de findings y discriminación del alcance histórico, sin modificar V1–V4 ni inventar evaluaciones anteriores.
- Incorporar dos GET scoped para resultados de evaluación y findings, sin devolver evidencia bruta. Mantener el GET executions técnico existente.
- **Cambio de semántica observable aprobado:** para scans nuevos del perfil evaluation-engine-v1, COMPLETED exigirá ejecución y evaluación cerradas técnicamente. Un fallo del componente LLM que permite consolidación coherente no hace fallar la evaluación ni el Scan; se registra aparte. Una vulnerabilidad o INCONCLUSIVE tampoco implica FAILED. Se conservan estados, siete campos públicos de Scan, admisión, targets y recuperación sin relanzamiento.
- Aplicar controles de privacidad, prompt injection indirecta, JSON estricto, tamaños, cuota, coste operativo opcional y fallos del evaluador. Severity final LOW/MEDIUM/HIGH según criterio e impacto; CRITICAL reservado sin asignación automática en MVP, sin techo global MEDIUM.

## Non-goals

No implementar código, migraciones, prompts operativos ni llamadas reales en esta entrega. No scoring global, reports, dashboard, frontend, multiusuario, billing, RAG, agentes, SQS, cancelación, retries, varios adaptadores completos, fine-tuning, tests dinámicos ni experimento académico completo. No alterar los cuatro probes/revisiones aprobados ni los contratos de Project/TargetConfiguration/TestExecution.

## Capabilities

### New Capabilities

- `evaluation-engine`: evaluación híbrida, criterios separados, integración LLM, persistencia de resultados/findings, consultas mínimas y extensión prospectiva de la finalización/recuperación del Scan.

### Modified Capabilities

No hay specs consolidadas en `openspec/specs/` (solo .gitkeep). Por ello no se fabrica un delta MODIFIED contra un baseline inexistente. Los cuatro changes previos se usan como baseline aprobado sin editar, archivar ni sincronizar. EE-10/EE-11 concretan expresamente la extensión de STE-10/STE-12 y SE-05/SE-08: la exclusión de evaluación de aquellos incrementos sigue describiendo su alcance histórico; los nuevos scans del perfil de este change incorporarán la evaluación aprobada.

## Impact

- Backend futuro: nuevos módulos domain/application/evaluation y finding, adaptadores de persistencia/LLM/REST y composición dirigida sobre ExecuteSecurityTestsUseCase, TransactionalScanLifecycle y ScanStartup. No refactorizar módulos ajenos.
- V5 aprobada: evaluation_results, marcador interno de perfil en scans y evolución conservadora de findings. test_cases y el ciclo técnico de test_executions permanecen intactos; evaluator_result/confidence heredados no se reutilizan ni reciben dual writes. Los errores del componente LLM se persisten separados del error global de evaluación.
- API aprobada: GET `/api/projects/{projectId}/scans/{scanId}/evaluations` y GET `/api/projects/{projectId}/scans/{scanId}/findings`. Sin POST de reevaluación ni GET Finding individual en MVP.
- Proveedor MVP OpenAI, modelo inicial `gpt-6-luna`, Responses API y Structured Outputs mediante JSON Schema estricto. `EVALUATOR_PROVIDER` y `EVALUATOR_MODEL` configuran externamente el adaptador único detrás de EvaluatorPort; el identificador del modelo no se codifica en dominio. La elección procede de la aprobación expresa del usuario, sin afirmar comprobación de cuenta, disponibilidad o tarifas mediante Internet en esta entrega.
- Privacidad inicial conservada del diseño general aprobado: escenarios sintéticos declarados y proyección de salida permitida; no exportar automáticamente evidencia libre de un target real. La redacción previa exacta de credenciales no garantiza ausencia de otros secretos.
- Durante implementación posterior deberán actualizarse OpenAPI, modelo de datos, arquitectura/ADR-003 a ADR-006 y documentos de seguridad/IA directamente afectados con estas concreciones. En esta entrega se mantienen intactos: la aprobación se registra exclusivamente en OpenSpec.
- Referencias revisadas: AGENTS, perfil architecture-agent, openspec/config.yaml, cuatro changes aprobados; docs/00-contexto, 01-investigacion, 02-arquitectura y ADR-001..007, 03-seguridad, 04-ia, 05-api, 06-base-de-datos y 07-memoria; V1/V4, puertos, worker y composición transaccional actuales.

## Review Status

Diseño general aprobado expresamente por el usuario el 2026-10-10 con sus concreciones D-01..D-16, incorporadas y relacionadas con los identificadores previos en design.md. **READY FOR IMPLEMENTATION**, sin decisiones contractuales bloqueantes pendientes. Esta petición autoriza únicamente actualizar artefactos y ejecutar validación strict; implementar requiere una petición posterior. Las 34 tareas de implementación permanecen sin ejecutar. La validación strict comprueba artefactos OpenSpec, no calidad del modelo ni funcionamiento del backend.
