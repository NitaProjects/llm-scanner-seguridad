# Visión del producto

## Problema

Las aplicaciones basadas en modelos de lenguaje pueden presentar riesgos que no siempre se detectan mediante pruebas funcionales tradicionales. Una aplicación puede obedecer instrucciones maliciosas, revelar información sensible, exponer instrucciones internas o producir respuestas inseguras.

Estas comprobaciones suelen realizarse de forma manual o mediante herramientas técnicas poco accesibles para equipos que necesitan interpretar los resultados y tomar medidas correctoras.

## Producto

**LLM Scanner Seguridad** es una plataforma web defensiva que permite evaluar, con autorización, la seguridad de aplicaciones basadas en LLM. El usuario configura una aplicación objetivo, selecciona pruebas controladas, ejecuta un scan y consulta los findings obtenidos junto con su evidencia, severidad, explicación y recomendación.

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

El producto permitirá crear proyectos de análisis, configurar un target, seleccionar Security Tests, ejecutar scans, consultar el histórico, visualizar un dashboard y generar un informe.

La primera versión se centrará en cuatro riesgos del OWASP GenAI LLM Top 10 (2026): LLM01 – Prompt Injection, LLM02 – Sensitive Information Disclosure, LLM08 – Hidden Context Exposure y LLM10 – Improper Output Handling.

## Qué no pretende ser

LLM Scanner Seguridad no pretende ser:

- Una herramienta de ataque contra sistemas no autorizados.
- Un sustituto de una auditoría de seguridad completa.
- Un sistema autónomo de explotación o jailbreak.
- Una garantía de que una aplicación es segura.
- Una réplica completa de herramientas maduras de red teaming.
- Un producto que cubra inicialmente todos los tipos de RAG, agentes, herramientas o proveedores LLM.
