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
- Documentar un despliegue reproducible con la aplicación y la base de datos separadas de la entrada HTTPS.

La topología validada en AWS se describe en la [guía de despliegue](../../docker/production/README.md).

## Objetivos relacionados con IA

- Separar la aplicación LLM analizada del modelo utilizado para evaluar sus respuestas.
- Combinar reglas deterministas con evaluación mediante LLM.
- Registrar los criterios y evidencias utilizados para producir un finding.
- Documentar las limitaciones, incertidumbre y posibles falsos positivos o negativos de la evaluación.
- Mantener versionados los casos de prueba y prompts de evaluación.
- Evitar que las respuestas de la aplicación objetivo se interpreten como instrucciones confiables para el sistema evaluador.
- Incorporar una escala de confianza y dejar prevista una revisión manual opcional de los resultados.

## Validación futura

- Evaluar la detección sobre un conjunto controlado de casos vulnerables, no vulnerables y, cuando proceda, ambiguos.
- Comparar reglas deterministas, LLM evaluador y evaluación híbrida.
- Medir true positives, true negatives, false positives, false negatives, precision, recall, reproducibilidad, tiempo y coste aproximado.
- Analizar las limitaciones del experimento y evitar generalizar sus resultados a todas las aplicaciones LLM.

El tamaño del dataset, el número de repeticiones, el procedimiento de ground truth y los valores objetivo quedan como **Pendiente de decisión**.

La integración con OpenAI [se validó en local](../../openspec/changes/real-evaluator-validation/evidence/2026-10-19-tls-revalidation/result.md) para un escenario sintético. Esa validación no acredita un benchmark, una evaluación de retención ni una tarifa.

## Objetivos relacionados con seguridad

- Ejecutar pruebas únicamente contra aplicaciones propias o autorizadas.
- Incorporar controles frente a SSRF en el acceso a endpoints configurados por usuarios.
- Aplicar límites de tiempo, tamaño, frecuencia y concurrencia a las peticiones de análisis.
- Evitar la exposición innecesaria de credenciales, tokens y datos sensibles.
- Proteger las evidencias y respuestas almacenadas frente a exposición accidental.
- Relacionar los resultados con categorías de riesgo del OWASP GenAI LLM Top 10 (2026) cuando corresponda.
- Dejar claro que un resultado sin findings no equivale a una garantía de seguridad.
