# Trazabilidad de requisitos de entrega

Este documento relaciona los requisitos oficiales del TFM con la evidencia prevista en el repositorio. La documentación técnica no sustituye los requisitos obligatorios del README raíz.

El backend core está aprobado e implementado por `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`. Los contratos y registros de implementación están en `openspec/changes/`; el [registro de reporting](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas) documenta validaciones técnicas hasta V7 y regresión de 400 pruebas. Esa evidencia no completa frontend, demo vulnerable, operación real de OpenAI, benchmark ni entregables académicos.

| Requisito | Evidencia | Estado actual |
|---|---|---|
| Objetivo y valor del proyecto | [`docs/00-contexto/vision.md`](../00-contexto/vision.md), [`docs/00-contexto/objetivos.md`](../00-contexto/objetivos.md) | COMPLETADO |
| README / descripción general | [`README.md`](../../README.md) | EN PROGRESO |
| Stack tecnológico | [`README.md`](../../README.md), [`AGENTS.md`](../../AGENTS.md) | COMPLETADO |
| Instalación | [`README.md`](../../README.md) | PENDIENTE |
| Ejecución | [`README.md`](../../README.md) | PENDIENTE |
| Estructura del proyecto | [`README.md`](../../README.md) | COMPLETADO |
| Funcionalidades principales | [`README.md`](../../README.md), [`docs/00-contexto/alcance.md`](../00-contexto/alcance.md) | EN PROGRESO |
| Arquitectura | [`docs/02-arquitectura/`](../02-arquitectura/) | COMPLETADO |
| Investigación | [`docs/01-investigacion/`](../01-investigacion/) | EN PROGRESO |
| Seguridad | [`docs/03-seguridad/`](../03-seguridad/) | EN PROGRESO |
| Evaluación IA | [`docs/04-ia/`](../04-ia/) | EN PROGRESO |
| API | [`docs/05-api/openapi.yaml`](../05-api/openapi.yaml), [`docs/05-api/target-contract.md`](../05-api/target-contract.md) | EN PROGRESO |
| Base de datos | [`docs/06-base-de-datos/`](../06-base-de-datos/) | EN PROGRESO |
| Memoria | [`docs/07-memoria/`](../07-memoria/) | EN PROGRESO |
| Entrega TFM | [`docs/08-entrega-tfm/`](./) | EN PROGRESO |
| OpenSpec | [`openspec/changes/`](../../openspec/changes/) | EN PROGRESO |
| Código frontend | [`frontend/`](../../frontend/) | EN PROGRESO |
| Backend core | [`backend/`](../../backend/), [`openspec/changes/`](../../openspec/changes/), [`reporting/tasks.md`](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas) | COMPLETADO |
| Aplicación vulnerable | [`demo-vulnerable/`](../../demo-vulnerable/) | EN PROGRESO |
| Código fuente en GitHub | [`enlaces-entrega.md`](./enlaces-entrega.md) | PENDIENTE |
| Despliegue funcional | [`enlaces-entrega.md`](./enlaces-entrega.md) | PENDIENTE |
| Slides | [`enlaces-entrega.md`](./enlaces-entrega.md) | PENDIENTE |
| Vídeo | [`enlaces-entrega.md`](./enlaces-entrega.md) | PENDIENTE |
| Formulario final | [`requisitos-oficiales.md`](./requisitos-oficiales.md) | PENDIENTE |

API figura EN PROGRESO porque el contrato del backend core ya existe, mientras la integración frontend y la documentación final siguen en desarrollo. OpenSpec figura EN PROGRESO porque los siete contratos están aprobados e implementados en los changes, pero `openspec/specs/` aún no contiene specs consolidadas; sincronización y archivo podrán realizarse posteriormente según el workflow sin reescribir históricos. Instalación/ejecución completa, despliegue AWS, enlaces GitHub, slides, vídeo, formulario y memoria final conservan sus estados pendientes o en progreso.

## Fuentes de verdad

- [`requisitos-oficiales.md`](./requisitos-oficiales.md): requisitos académicos oficiales transcritos.
- [`checklist-entrega.md`](./checklist-entrega.md): estado vivo de cumplimiento.
- [`README.md`](../../README.md): documentación principal para el evaluador.
- [`docs/`](../README.md): documentación técnica y académica ampliada.
- [`openspec/changes/`](../../openspec/changes/): contratos aprobados disponibles y registros de implementación; `openspec/specs/` aún sin consolidar.

