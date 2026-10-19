# Checklist de entrega

Esta checklist es un control vivo. Solo se utilizarán los estados `COMPLETADO`, `EN PROGRESO`, `PENDIENTE` y `NO APLICA`. No se marcará un requisito como `COMPLETADO` sin evidencia real.

El estado COMPLETADO del backend core se limita a `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`, aprobados e implementados. Sus contratos y registros de validación están en `openspec/changes/`, con [evidencia de regresión hasta reporting](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas). No acredita la entrega completa, operación real de OpenAI ni experimento académico. La consolidación a `openspec/specs/` y el archivo de changes siguen pendientes del workflow posterior.

| Requisito | Estado | Evidencia | Observaciones |
|---|---|---|---|
| README completo | EN PROGRESO | [`/Readme.md`](../../Readme.md), [`aws-production-readiness`](../../openspec/changes/aws-production-readiness/tasks.md) | Instalación/ejecución y preparación de producción documentadas y probadas localmente; URL pública y contenido final de entrega pendientes. |
| Descripción general | COMPLETADO | [`/README.md`](../../README.md), [`vision.md`](../00-contexto/vision.md) | Descripción inicial documentada. |
| Stack tecnológico | COMPLETADO | [`/README.md`](../../README.md), [`AGENTS.md`](../../AGENTS.md) | Stack previsto documentado. |
| Instalación | COMPLETADO | [`/Readme.md`](../../Readme.md), [`evidencia local`](../../openspec/changes/aws-production-readiness/evidence/local-validation.json), [`build limpio`](../../openspec/changes/aws-production-readiness/evidence/clean-backend-build.json) | Build frontend sin node_modules previo y backend desde copia limpia de fuentes; instalación local aislada de tres componentes/Flyway comprobada. No acredita provisión AWS. |
| Ejecución | COMPLETADO | [`guía`](../../docker/production/README.md), [`evidencia local`](../../openspec/changes/aws-production-readiness/evidence/local-validation.json) | Arranque local protegido, GET API, SPA/deep links, persistencia y reinicio comprobados. Despliegue público permanece pendiente. |
| Estructura del proyecto | COMPLETADO | [`/README.md`](../../README.md), [`docs/README.md`](../README.md) | Estructura principal documentada. |
| Funcionalidades principales | EN PROGRESO | [`/README.md`](../../README.md), [`alcance.md`](../00-contexto/alcance.md), [`frontend-mvp/tasks.md`](../../openspec/changes/frontend-mvp/tasks.md), [`demo-vulnerable/tasks.md`](../../openspec/changes/demo-vulnerable/tasks.md) | Backend core, frontend MVP y target sintético independiente implementados. Frontend verificado con HTTP simulado; integración E2E, safety aislado de la demo y entrega completa pendientes. |
| Backend core | COMPLETADO | [`backend/`](../../backend/), [`changes`](../../openspec/changes/), [`reporting/tasks.md`](../../openspec/changes/reporting/tasks.md#validaciones-ejecutadas) | Siete changes aprobados e implementados; plan fijo de cuatro pruebas, evaluación híbrida, scoring-v1 y report-v1. |
| Credenciales de prueba | PENDIENTE | [`guía`](../../docker/production/README.md) | Basic Auth de Nginx preparado; emisión/custodia y entrega de credencial académica real por canal privado pendientes. Sin usuarios/roles Spring. |
| Repositorio GitHub | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | URL pendiente de configurar. |
| Despliegue | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | AWS previsto; aún no existe URL real. |
| Slides | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | No creadas todavía. |
| Vídeo | PENDIENTE | [`enlaces-entrega.md`](./enlaces-entrega.md) | No creado todavía. |
| Documentación del proyecto | EN PROGRESO | [`docs/`](../README.md) | Documentación vigente alineada con el backend core; memoria final, benchmark y procedimientos completos siguen pendientes. |
| Información de despliegue | EN PROGRESO | [`guía`](../../docker/production/README.md), [`enlaces-entrega.md`](./enlaces-entrega.md) | Configuración/guía/plantilla y pruebas locales disponibles; cuenta/AMI/dominio/certificado/URL y verificaciones AWS no ejecutadas. |

