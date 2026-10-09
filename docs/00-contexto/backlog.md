# Backlog inicial por épicas

Este backlog representa un roadmap de alto nivel. Las tareas concretas de implementación se definirán mediante cambios OpenSpec.

## Gestión de proyectos

Permitir crear, consultar y organizar proyectos de análisis.

## Configuración del target

Permitir definir el endpoint de la aplicación LLM autorizada y los parámetros mínimos necesarios para interactuar con él.

## Motor de scans

Coordinar la ejecución controlada de Security Tests, registrar su estado y conservar el histórico de scans.

## Catálogo de Security Tests

Definir y mantener los casos de prueba disponibles, sus categorías, objetivos y referencias de seguridad.

## Evaluación híbrida

Combinar reglas deterministas y evaluación mediante LLM para analizar las respuestas de la aplicación objetivo.

## Findings

Registrar y presentar evidencias, severidad, explicación, confianza y recomendaciones asociadas a los resultados.

## Informes

Generar un informe consolidado de los scans y sus findings.

## Dashboard

Mostrar de forma resumida el estado de los proyectos, scans, niveles de riesgo y resultados relevantes.

## Demo vulnerable

Proporcionar una aplicación controlada que permita demostrar y reproducir las vulnerabilidades contempladas.

## Seguridad de la propia plataforma

Proteger la plataforma y sus integraciones, incluyendo autorización, SSRF, límites de uso, gestión de datos sensibles y aislamiento de respuestas no confiables.

## Despliegue

Preparar la ejecución local y definir posteriormente el despliegue final en AWS.

El proveedor cloud, la arquitectura de despliegue y el nivel de automatización quedan como **Pendiente de decisión**.
