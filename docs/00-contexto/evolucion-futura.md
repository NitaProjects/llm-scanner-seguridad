# Evolución futura

## Propósito

Este documento centraliza las funcionalidades y mejoras identificadas que quedan fuera del MVP de LLM Scanner Seguridad. Su objetivo es demostrar que el alcance se ha acotado conscientemente y proporcionar una base para priorizar futuras versiones.

Las evoluciones aquí recogidas no forman parte del compromiso actual ni deben implementarse sin una decisión posterior.

## Principios

- No ampliar el MVP sin una decisión explícita.
- Priorizar seguridad, valor académico y viabilidad técnica.
- Analizar dependencias antes de incorporar una evolución.
- Mantener la compatibilidad con la arquitectura modular y hexagonal.
- Documentar cada decisión importante mediante un ADR o un change de OpenSpec.

## Autenticación avanzada

- OAuth2.
- mTLS.
- Basic Auth.
- Cookies y sesiones.
- Flujos interactivos.
- Autenticación multi-step.

Estas opciones requieren mayor complejidad de configuración, gestión de secretos, renovación de credenciales y validación de seguridad.

## Protocolos y formatos de integración

- Streaming.
- SSE.
- WebSocket.
- multipart.
- GET.
- SOAP.
- gRPC.

Su incorporación requeriría contratos, adaptadores y controles de seguridad específicos.

## Categorías OWASP futuras

Quedan fuera del MVP las siguientes categorías del OWASP GenAI LLM Top 10 (2026):

- **LLM03 – Excessive Agency**: requiere agentes, herramientas, permisos y acciones autónomas.
- **LLM04 – Supply Chain**: requiere analizar modelos, dependencias, proveedores y componentes externos.
- **LLM05 – Data and Model Poisoning**: requiere escenarios de entrenamiento, ajuste o ingestión de datos manipulados.
- **LLM06 – Unbounded Consumption**: requiere evaluar consumo de recursos, costes y límites a mayor escala.
- **LLM07 – Misinformation**: requiere fuentes de referencia y procedimientos de verificación factual.
- **LLM09 – Vector and Embedding Weaknesses**: requiere arquitecturas RAG, almacenes vectoriales y análisis de embeddings.

## Capacidades avanzadas del scanner

- Soporte para agentes complejos.
- Herramientas externas invocadas por el target.
- RAG avanzado.
- Ejecuciones programadas.
- Monitorización continua.
- Comparación entre versiones de una aplicación.
- Integración con pipelines CI/CD.
- Integración con sistemas de tickets o gestión de vulnerabilidades.

## Plataforma y operación

- Gestión avanzada de usuarios, roles y organizaciones.
- Multi-tenancy.
- AWS Secrets Manager para almacenar y gestionar secretos, junto con AWS KMS para gestionar las claves de cifrado.
- Políticas avanzadas de retención y auditoría.
- Despliegue completo y operación productiva en AWS.

## Ejecución y operación de scans

El MVP utilizará scans asíncronos con polling. Como evolución futura se podrán valorar:

- AWS SQS para desacoplar la solicitud y el procesamiento.
- Workers independientes.
- Procesamiento distribuido.
- Reintentos automáticos con políticas controladas.
- Dead-letter queue para tareas que no puedan procesarse.
- Cancelación de scans en curso.
- WebSocket, Server-Sent Events o notificaciones push para informar del progreso en tiempo real.

Estas capacidades requieren decidir previamente el modelo de concurrencia, la recuperación ante fallos, la idempotencia, la observabilidad y los límites de coste.

## Evaluación e inteligencia artificial

- Integración con varios proveedores LLM.
- Fallback automático entre proveedores.
- Comparación sistemática entre modelos.
- Evaluación por consenso entre varios modelos.
- Uso de modelos locales u open source.
- Selección dinámica del evaluador según coste y calidad.
- Benchmarking específico de evaluadores.
- Calibración avanzada del confidence score.
- Revisión colaborativa de findings.
- Nuevas métricas experimentales.
- Evaluación específica de factualidad y misinformation.

## Priorización futura

Cada evolución deberá analizarse considerando:

- Prioridad.
- Valor para el usuario.
- Complejidad.
- Dependencias.
- Impacto en seguridad.
- Aportación al TFM o a una versión posterior del producto.

La priorización definitiva queda como **Pendiente de decisión**.
