# Índice inicial de la memoria del TFM

La memoria final sigue **EN PROGRESO**; este documento es su índice inicial. El backend core aprobado e implementado comprende `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`, cuyos contratos están actualmente en `openspec/changes/`, aún sin consolidar en `openspec/specs/`. Esta evidencia no completa frontend, demo vulnerable, experimento, operación real ni entrega final.

## 1. Introducción

- Contexto del problema.
- Motivación.
- Objetivos.
- Alcance y limitaciones.

## 2. Estado del arte

- Seguridad en aplicaciones LLM.
- OWASP GenAI LLM Top 10 (2026).
- Prompt injection y fuga de información.
- Hidden context exposure e improper output handling.
- Herramientas existentes de evaluación y red teaming.

## 3. Metodología

- Desarrollo guiado por especificaciones con OpenSpec.
- Arquitectura y proceso de desarrollo asistido por IA.
- Diseño de casos de prueba.
- Estrategia de evaluación híbrida.
- Modelo de severidad, confidence y scoring.

## 4. Análisis y requisitos

- Especificaciones funcionales gestionadas con OpenSpec.
- Requisitos no funcionales y restricciones de seguridad.
- Alcance del MVP y decisiones pendientes.

## 5. Diseño de la solución

- Arquitectura conceptual.
- Modelo de datos.
- Diseño de la API.
- Seguridad de la plataforma.
- Mitigación de SSRF.

## 6. Implementación

El capítulo describirá el backend implementado con plan fijo de cuatro SecurityTests sin selección configurable, Scan asíncrono, evaluación híbrida, scoring-v1 e informe estructurado report-v1. Los nuevos scans COMPLETED requieren cuatro ejecuciones y cuatro evaluaciones COMPLETED, ScanScore y SecurityReport, con score/informe/cierre atómicos. Un fallo técnico global puede llevar a FAILED; el fallo/degradación LLM puede consolidar INCONCLUSIVE o VULNERABLE según las reglas. El score es heurístico y cero no demuestra seguridad. Frontend y demo vulnerable siguen en progreso; exportación HTML/Markdown/PDF sigue siendo futura.

- Frontend.
- Backend.
- Motor de scans.
- Evaluación de respuestas.
- Cobertura de LLM01, LLM02, LLM08 y LLM10.
- Findings e informes.
- Transformación de evidencias en findings y resultados globales.
- Aplicación vulnerable de demostración.

## 7. Evaluación experimental

El diseño del experimento se documenta en `docs/01-investigacion/experimento-academico.md`. Se utilizará un dataset controlado que cubra LLM01, LLM02, LLM08 y LLM10, con casos vulnerables, no vulnerables y, si procede, ambiguos. La aplicación `demo-vulnerable/` será una fuente principal de escenarios.

La pregunta de investigación provisional es:

> ¿Puede una plataforma web basada en pruebas controladas y evaluación híbrida detectar de forma reproducible vulnerabilidades comunes en aplicaciones LLM?

La evaluación considerará, cuando sea viable:

- Número de vulnerabilidades detectadas.
- True positives y true negatives.
- Falsos positivos y falsos negativos.
- Reproducibilidad entre ejecuciones.
- Tiempo de análisis.
- Coste aproximado.
- Diferencia entre reglas deterministas, LLM evaluador y estrategia híbrida.
- Efecto de los criterios de severidad y confidence.
- Relación entre severidad, confidence y score global.

El estudio con desarrolladores sobre comprensión del informe queda fuera del diseño experimental adoptado por [ADR-007](../02-arquitectura/decisiones/ADR-007-experimento-academico.md), salvo decisión posterior.

Se compararán tres estrategias: reglas deterministas, LLM evaluador y evaluación híbrida. La memoria incluirá tablas por caso y estrategia, matrices de confusión, análisis de falsos positivos y falsos negativos, reproducibilidad, tiempos, costes y limitaciones.

La comparación sigue pendiente: el assessment HYBRID_WITH_SIGNALS conservado por el backend no equivale a una ejecución LLM-only independiente. Se requiere una entrada experimental sin señales, ground truth separado y perfiles/prompts versionados; las pruebas técnicas registradas no sustituyen el benchmark ni la validación real de OpenAI.

El score global se tratará como un resumen y no como sustituto de los findings individuales. Los hallazgos críticos deberán conservar visibilidad independiente aunque el score agregado sea bajo.

El tamaño definitivo del conjunto de pruebas, su etiquetado, el número de repeticiones, el tratamiento estadístico y los valores objetivo quedan como **Pendiente de decisión**. El diseño conceptual del procedimiento queda documentado en `docs/01-investigacion/experimento-academico.md`.

## 8. Conclusiones y trabajo futuro

- Resultados obtenidos.
- Limitaciones.
- Mejoras futuras.
- Extensiones para RAG, agentes, monitorización y despliegue.
- Evolución futura hacia LLM03, LLM04, LLM05, LLM06, LLM07 y LLM09.
