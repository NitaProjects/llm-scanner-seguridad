# Visión del producto

## Problema

Las aplicaciones basadas en modelos de lenguaje pueden presentar riesgos que no siempre se detectan mediante pruebas funcionales tradicionales. Una aplicación puede obedecer instrucciones maliciosas, revelar información sensible, exponer instrucciones internas o producir respuestas inseguras.

Estas comprobaciones suelen realizarse de forma manual o mediante herramientas técnicas poco accesibles para equipos que necesitan interpretar los resultados y tomar medidas correctoras.

## Producto

**LLM Scanner Seguridad** es una plataforma web defensiva que permite evaluar, con autorización, la seguridad de aplicaciones basadas en LLM. El usuario configura una aplicación objetivo e inicia un scan sobre el plan fijo de cuatro SecurityTests, sin selección configurable por usuario. El backend permite consultar findings con resúmenes de evidencia, severidad, explicación y recomendación, además del score y el informe estructurado.

El nombre oficial del proyecto es **LLM Scanner Seguridad**.

## Usuario objetivo

El producto está dirigido principalmente a:

- Desarrolladores de aplicaciones con LLM.
- Equipos de seguridad y calidad.
- Estudiantes y equipos técnicos que necesiten evaluar riesgos de forma reproducible.

El usuario debe contar con autorización para analizar el endpoint configurado.

## Propuesta de valor

La plataforma centraliza pruebas básicas de seguridad LLM y transforma las respuestas obtenidas en resultados comprensibles y accionables. Su valor principal es combinar:

- Pruebas controladas alineadas con riesgos conocidos.
- Evaluación híbrida mediante reglas y un LLM evaluador.
- Evidencias y explicaciones asociadas a cada finding.
- Un flujo web sencillo y reproducible.

## Visión general

El backend core aprobado e implementado permite crear proyectos de análisis, configurar un target, ejecutar scans asíncronos con las cuatro pruebas fijas, aplicar evaluación híbrida, consultar el histórico y obtener `scoring-v1` e informes estructurados `report-v1`. Cada Scan nuevo COMPLETED requiere cuatro ejecuciones y cuatro evaluaciones COMPLETED, ScanScore y SecurityReport confirmados; un fallo LLM consolidado no implica automáticamente Scan FAILED. El score es heurístico y cero no acredita seguridad. Los históricos conservan su significado sin recálculo.

El frontend/dashboard y la demo vulnerable siguen en progreso. La instalación/ejecución completa, operación real de OpenAI, experimento académico, AWS y entrega final siguen pendientes. HTML/Markdown/PDF son futuras exportaciones del informe estructurado ya implementado.

La primera versión se centrará en cuatro riesgos del OWASP GenAI LLM Top 10 (2026): LLM01 – Prompt Injection, LLM02 – Sensitive Information Disclosure, LLM08 – Hidden Context Exposure y LLM10 – Improper Output Handling.

## Qué no pretende ser

LLM Scanner Seguridad no pretende ser:

- Una herramienta de ataque contra sistemas no autorizados.
- Un sustituto de una auditoría de seguridad completa.
- Un sistema autónomo de explotación o jailbreak.
- Una garantía de que una aplicación es segura.
- Una réplica completa de herramientas maduras de red teaming.
- Un producto que cubra inicialmente todos los tipos de RAG, agentes, herramientas o proveedores LLM.
