# Trazabilidad de requisitos de entrega

Este documento relaciona los requisitos oficiales del TFM con la evidencia prevista en el repositorio. La documentación técnica no sustituye los requisitos obligatorios del README raíz.

Reconciliación documental A+B de 2026-10-23: [matriz oficial O1–O7, procedencia y pendientes](../../openspec/changes/tfm-final-delivery/evidence/result.md). El backend core conserva sus contratos aprobados en OpenSpec. La evidencia posterior distingue integración local API/UI, proveedor OpenAI local y publicación AWS; no se reejecuta ni se convierte en experimento formal. Los cierres históricos de otros changes permanecen intactos.

| Requisito | Evidencia | Estado actual |
|---|---|---|
| Objetivo y valor del proyecto | [`docs/00-contexto/vision.md`](../00-contexto/vision.md), [`docs/00-contexto/objetivos.md`](../00-contexto/objetivos.md) | COMPLETADO |
| README / descripción general | [`Readme.md`](../../Readme.md), [`evidencia`](../../openspec/changes/tfm-final-delivery/evidence/result.md) | Contenido A+B READY y GitHub privado justificado documentado; slides/vídeo pendientes |
| Stack tecnológico | [`Readme.md`](../../Readme.md), [`AGENTS.md`](../../AGENTS.md) | COMPLETADO |
| Instalación | [`Readme.md`](../../Readme.md), [`preparación local`](../../openspec/changes/aws-production-readiness/evidence/result.md) | COMPLETADO para instrucciones y evidencia local conservada; sin nueva ejecución |
| Ejecución | [`Readme.md`](../../Readme.md), [`E2E offline`](../../openspec/changes/e2e-integration/evidence/2026-10-16-005150/result.md) | COMPLETADO para recorrido local y guía; acceso final C/D pendiente |
| Estructura del proyecto | [`Readme.md`](../../Readme.md) | COMPLETADO |
| Funcionalidades principales documentadas | [`Readme.md`](../../Readme.md), [`UI con Findings`](../../openspec/changes/real-evaluator-validation/evidence/2026-10-19-ui-real/result.md) | COMPLETADO para descripción MVP y recorrido conservado; sin cierre inferido de tasks históricos |
| Arquitectura | [`docs/02-arquitectura/`](../02-arquitectura/) | COMPLETADO |
| Investigación | [`docs/01-investigacion/`](../01-investigacion/) | EN PROGRESO |
| Seguridad | [`docs/03-seguridad/`](../03-seguridad/) | EN PROGRESO |
| Evaluación IA | [`docs/04-ia/`](../04-ia/), [`OpenAI local`](../../openspec/changes/real-evaluator-validation/evidence/2026-10-19-tls-revalidation/result.md) | Integración local real acreditada; metodología/experimento académico formal no completados |
| API | [`docs/05-api/openapi.yaml`](../05-api/openapi.yaml), [`docs/05-api/target-contract.md`](../05-api/target-contract.md) | EN PROGRESO |
| Base de datos | [`docs/06-base-de-datos/`](../06-base-de-datos/) | EN PROGRESO |
| Memoria | [`docs/07-memoria/`](../07-memoria/) | EN PROGRESO |
| Entrega TFM | [`docs/08-entrega-tfm/`](./) | EN PROGRESO |
| OpenSpec | [`openspec/changes/`](../../openspec/changes/) | EN PROGRESO |
| Código frontend | [`frontend/`](../../frontend/), [`UI real`](../../openspec/changes/real-evaluator-validation/evidence/2026-10-19-ui-real/result.md) | Implementado y recorrido real conservado; cierre formal 9.4 histórico no modificado |
| Backend core | [`backend/`](../../backend/), [`openspec/changes/`](../../openspec/changes/), [`reporting/tasks.md`](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas) | COMPLETADO |
| Aplicación vulnerable | [`demo-vulnerable/`](../../demo-vulnerable/), [`E2E local`](../../openspec/changes/e2e-integration/evidence/2026-10-16-005150/result.md) | Implementada e integrada localmente; aislamiento externo de egress/validación Docker no acreditados por este recorrido; no desplegada en AWS |
| Código fuente en GitHub | [`enlaces-entrega.md`](./enlaces-entrega.md), [`Fase C`](../../openspec/changes/tfm-final-delivery/evidence/result.md) | URL/revisión/inventario completos (3.1), revisión public-safe local PASS (3.2). Privado justificado, invitación enviada y acceso preparado; 3.3 pendiente exclusivamente de aceptación por el destinatario. 404 anónimo esperado; no se exige publicar |
| Despliegue funcional | [`enlaces-entrega.md`](./enlaces-entrega.md), [`evidencia AWS`](../../openspec/changes/aws-production-readiness/evidence/2026-10-20-aws-deployment.md) | COMPLETADO; acceso académico preparado, 6.1 completa por confirmación operativa/TLS y 6.4 completa técnicamente. 24/25 tareas completas: solo restore aislado (6.3) pendiente; entrega administrativa de credenciales no acreditada |
| Slides | [`enlaces-entrega.md`](./enlaces-entrega.md) | PENDIENTE |
| Vídeo | [`enlaces-entrega.md`](./enlaces-entrega.md) | PENDIENTE |
| Formulario final | [`requisitos-oficiales.md`](./requisitos-oficiales.md) | PENDIENTE |

API figura EN PROGRESO para su documentación final; el contrato core y la integración API/UI ya tienen evidencia. OpenSpec conserva contratos en changes y consolidación/archivo históricos pendientes, sin convertirlos en bloqueantes de O1–O7. AWS tiene publicación protegida y persistencia básica según evidencia del operador de 2026-10-20, sin Scan/demo/evaluator; la revalidación OpenAI de 2026-10-19 es local y no repitió frontend 9.4. Restore 6.3 está pospuesto y no bloquea entrega. Las credenciales se facilitarán por canal privado. C conserva revisión public-safe local PASS y acceso GitHub privado preparado por invitación, pendiente exclusivamente de aceptación por el destinatario. D–H siguen sin ejecución: acceso final al deployment, slides, vídeo y formulario/confirmación administrativa; no se declara DONE.

## Fuentes de verdad

- [`requisitos-oficiales.md`](./requisitos-oficiales.md): requisitos académicos oficiales transcritos.
- [`checklist-entrega.md`](./checklist-entrega.md): estado vivo de cumplimiento.
- [`Readme.md`](../../Readme.md): documentación principal para el evaluador, conservando el case real del archivo.
- [`docs/`](../README.md): documentación técnica y académica ampliada.
- [`openspec/changes/`](../../openspec/changes/): contratos aprobados disponibles y registros de implementación; `openspec/specs/` aún sin consolidar.

