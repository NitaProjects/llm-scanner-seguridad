# Backlog inicial por épicas

Este backlog representa un roadmap de alto nivel. Las tareas concretas de implementación se definirán mediante cambios OpenSpec.

Las épicas describen el alcance inicial y las líneas de evolución. Consulta el [README](../../Readme.md) para ver las capacidades disponibles y la evidencia de validación, y OpenSpec para distinguir qué se ha implementado y qué sigue pendiente.

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

La [guía de despliegue](../../docker/production/README.md) recoge la topología registrada en AWS. La automatización adicional y la operación a escala quedan como evolución futura.
