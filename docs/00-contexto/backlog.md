# Backlog inicial por épicas

Este backlog representa un roadmap de alto nivel. Las tareas concretas de implementación se definirán mediante cambios OpenSpec.

El backend core de estas épicas está aprobado e implementado mediante `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`. Sus contratos están en `openspec/changes/`; `openspec/specs/` sigue sin specs consolidadas. Frontend/dashboard y demo vulnerable permanecen en progreso; procedimientos completos de instalación/ejecución, experimento, operación real de OpenAI y entrega final siguen pendientes. Las épicas siguientes describen el roadmap y no implican que todo el producto esté completado.

## Gestión de proyectos

Permitir crear, consultar y organizar proyectos de análisis.

## Configuración del target

Permitir definir el endpoint de la aplicación LLM autorizada y los parámetros mínimos necesarios para interactuar con él.

## Motor de scans

Coordinar la ejecución controlada de Security Tests, registrar su estado y conservar el histórico de scans.

## Catálogo de Security Tests

El catálogo MVP implementado contiene exactamente cuatro SecurityTests inmutables, uno por categoría, sin selección configurable por usuario. Las variantes requieren revisión y un change posterior.

## Evaluación híbrida

Combinar reglas deterministas y evaluación mediante LLM para analizar las respuestas de la aplicación objetivo.

## Findings

Registrar y presentar evidencias, severidad, explicación, confianza y recomendaciones asociadas a los resultados.

## Informes

El backend genera un informe estructurado persistido `report-v1` con el resultado `scoring-v1`, evaluaciones y findings. Exportación HTML/Markdown/PDF sigue como evolución futura.

## Dashboard

Mostrar de forma resumida el estado de los proyectos, scans, niveles de riesgo y resultados relevantes.

## Demo vulnerable

Proporcionar una aplicación controlada que permita demostrar y reproducir las vulnerabilidades contempladas.

## Seguridad de la propia plataforma

Proteger la plataforma y sus integraciones, incluyendo autorización, SSRF, límites de uso, gestión de datos sensibles y aislamiento de respuestas no confiables.

## Despliegue

Preparar la ejecución local y definir posteriormente el despliegue final en AWS.

AWS es el destino previsto. La arquitectura concreta de despliegue, la custodia operativa de claves y el nivel de automatización quedan como **Pendiente de decisión**; el despliegue real sigue pendiente.
