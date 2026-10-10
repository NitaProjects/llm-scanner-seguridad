# Checklist de entrega

Esta checklist es un control vivo. Solo se utilizarán los estados `COMPLETADO`, `EN PROGRESO`, `PENDIENTE` y `NO APLICA`. No se marcará un requisito como `COMPLETADO` sin evidencia real.

El estado COMPLETADO del backend core se limita a `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`, aprobados e implementados. Sus contratos y registros de validación están en `openspec/changes/`, con [evidencia de regresión hasta reporting](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas). No acredita la entrega completa, operación real de OpenAI ni experimento académico. La consolidación a `openspec/specs/` y el archivo de changes siguen pendientes del workflow posterior.

| Requisito | Estado | Evidencia | Observaciones |
|---|---|---|---|
| README completo | EN PROGRESO | [`/README.md`](../../README.md) | La estructura está preparada; faltan completar instalación, ejecución y contenido final. |
| Descripción general | COMPLETADO | [`/README.md`](../../README.md), [`vision.md`](../00-contexto/vision.md) | Descripción inicial documentada. |
| Stack tecnológico | COMPLETADO | [`/README.md`](../../README.md), [`AGENTS.md`](../../AGENTS.md) | Stack previsto documentado. |
| Instalación | PENDIENTE | [`/README.md`](../../README.md) | Pendiente de documentar el procedimiento validado. |
| Ejecución | PENDIENTE | [`/README.md`](../../README.md) | Pendiente de documentar el procedimiento validado. |
| Estructura del proyecto | COMPLETADO | [`/README.md`](../../README.md), [`docs/README.md`](../README.md) | Estructura principal documentada. |
| Funcionalidades principales | EN PROGRESO | [`/README.md`](../../README.md), [`alcance.md`](../00-contexto/alcance.md) | Backend core implementado; frontend/dashboard y demo vulnerable siguen en progreso. |
| Backend core | COMPLETADO | [`backend/`](../../backend/), [`changes`](../../openspec/changes/), [`reporting/tasks.md`](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas) | Siete changes aprobados e implementados; plan fijo de cuatro pruebas, evaluación híbrida, scoring-v1 y report-v1. |
| Credenciales de prueba | NO APLICA | [`/README.md`](../../README.md) | No existe login documentado actualmente; revisar al cierre. |
| Repositorio GitHub | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | URL pendiente de configurar. |
| Despliegue | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | AWS previsto; aún no existe URL real. |
| Slides | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | No creadas todavía. |
| Vídeo | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | No creado todavía. |
| Documentación del proyecto | EN PROGRESO | [`docs/`](../README.md) | Documentación vigente alineada con el backend core; memoria final, benchmark y procedimientos completos siguen pendientes. |
| Información de despliegue | PENDIENTE | [`/README.md`](../../README.md), [`enlaces-entrega.md`](./enlaces-entrega.md) | Pendiente de despliegue real y documentación final. |

