# Índice inicial de la memoria del TFM

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
- Comprensión del informe por parte de desarrolladores.

Se compararán tres estrategias: reglas deterministas, LLM evaluador y evaluación híbrida. La memoria incluirá tablas por caso y estrategia, matrices de confusión, análisis de falsos positivos y falsos negativos, reproducibilidad, tiempos, costes y limitaciones.

El score global se tratará como un resumen y no como sustituto de los findings individuales. Los hallazgos críticos deberán conservar visibilidad independiente aunque el score agregado sea bajo.

El tamaño definitivo del conjunto de pruebas, su etiquetado, el número de repeticiones, el tratamiento estadístico y los valores objetivo quedan como **Pendiente de decisión**. El diseño conceptual del procedimiento queda documentado en `docs/01-investigacion/experimento-academico.md`.

## 8. Conclusiones y trabajo futuro

- Resultados obtenidos.
- Limitaciones.
- Mejoras futuras.
- Extensiones para RAG, agentes, monitorización y despliegue.
- Evolución futura hacia LLM03, LLM04, LLM05, LLM06, LLM07 y LLM09.
