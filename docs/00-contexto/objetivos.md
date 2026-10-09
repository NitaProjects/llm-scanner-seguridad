# Objetivos del proyecto

## Objetivo general

Diseñar y desarrollar una plataforma web defensiva que permita evaluar de forma controlada y reproducible riesgos de seguridad en aplicaciones basadas en LLM, generando findings explicables e informes útiles para desarrolladores.

## Objetivos específicos

- Definir un flujo de análisis basado en proyectos, targets, scans, ejecuciones, findings e informes.
- Permitir configurar una aplicación LLM objetivo con autorización explícita.
- Ejecutar un conjunto inicial de pruebas sobre riesgos LLM seleccionados.
- Cubrir en el MVP LLM01, LLM02, LLM08 y LLM10 del OWASP GenAI LLM Top 10 (2026).
- Conservar la evidencia necesaria para comprender cada resultado.
- Mostrar los resultados mediante un dashboard básico.
- Generar un informe consolidado de cada análisis.
- Proporcionar una aplicación vulnerable propia para demostraciones y evaluación.

## Objetivos técnicos

- Implementar una aplicación web con Angular y un backend Spring Boot.
- Utilizar PostgreSQL como persistencia principal.
- Gestionar la evolución del esquema mediante Flyway.
- Mantener una arquitectura de monolito modular con principios de arquitectura hexagonal.
- Separar el dominio de los detalles de infraestructura mediante puertos y adaptadores.
- Exponer una API REST coherente entre frontend y backend.
- Mantener los análisis y sus resultados trazables y reproducibles.
- Preparar el sistema para un despliegue final en AWS.
- Comparar los resultados de reglas deterministas, evaluación mediante LLM y evaluación híbrida.
- Medir, dentro del experimento del TFM, detección, falsos positivos, falsos negativos, reproducibilidad, tiempo y coste aproximado.

El diseño concreto del despliegue en AWS queda como **Pendiente de decisión**.

## Objetivos relacionados con IA

- Separar la aplicación LLM analizada del modelo utilizado para evaluar sus respuestas.
- Combinar reglas deterministas con evaluación mediante LLM.
- Registrar los criterios y evidencias utilizados para producir un finding.
- Documentar las limitaciones, incertidumbre y posibles falsos positivos o negativos de la evaluación.
- Mantener versionados los casos de prueba y prompts de evaluación.
- Evitar que las respuestas de la aplicación objetivo se interpreten como instrucciones confiables para el sistema evaluador.
- Incorporar una escala de confianza y dejar prevista una revisión manual opcional de los resultados.

## Objetivos académicos

- Evaluar la detección sobre un conjunto controlado de casos vulnerables, no vulnerables y, cuando proceda, ambiguos.
- Comparar reglas deterministas, LLM evaluador y evaluación híbrida.
- Medir true positives, true negatives, false positives, false negatives, precision, recall, reproducibilidad, tiempo y coste aproximado.
- Analizar las limitaciones del experimento y evitar generalizar sus resultados a todas las aplicaciones LLM.

El tamaño del dataset, el número de repeticiones, el procedimiento de ground truth y los valores objetivo quedan como **Pendiente de decisión**.

El proveedor o proveedores concretos del modelo evaluador quedan como **Pendiente de decisión**.

## Objetivos relacionados con seguridad

- Ejecutar pruebas únicamente contra aplicaciones propias o autorizadas.
- Incorporar controles frente a SSRF en el acceso a endpoints configurados por usuarios.
- Aplicar límites de tiempo, tamaño, frecuencia y concurrencia a las peticiones de análisis.
- Evitar la exposición innecesaria de credenciales, tokens y datos sensibles.
- Proteger las evidencias y respuestas almacenadas frente a exposición accidental.
- Relacionar los resultados con categorías de riesgo del OWASP GenAI LLM Top 10 (2026) cuando corresponda.
- Dejar claro que un resultado sin findings no equivale a una garantía de seguridad.
