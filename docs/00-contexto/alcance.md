# Alcance del proyecto

## Alcance del MVP

El MVP incluirá:

- Creación y gestión básica de proyectos de análisis.
- Configuración de un endpoint de una aplicación LLM autorizada.
- Selección de Security Tests disponibles.
- Ejecución controlada de scans.
- Registro del histórico de scans.
- Captura de las respuestas y evidencias relevantes.
- Generación de findings con severidad, explicación y recomendación.
- Evaluación híbrida mediante reglas deterministas y un LLM evaluador.
- Las siguientes cuatro categorías de OWASP GenAI LLM Top 10 (2026):
  - LLM01 – Prompt Injection.
  - LLM02 – Sensitive Information Disclosure.
  - LLM08 – Hidden Context Exposure.
  - LLM10 – Improper Output Handling.
- Dashboard básico para consultar el estado y los resultados.
- Generación de un informe final.
- Aplicación vulnerable propia para demostraciones y pruebas reproducibles.
- Persistencia de proyectos, scans, ejecuciones y findings.
- Ejecución asíncrona de scans, con consulta posterior de estado y progreso.

Como decisión cerrada del MVP, el target utilizará únicamente HTTP REST con método `POST`, `Content-Type: application/json`, body JSON configurable mediante plantilla, placeholder `{{PROMPT}}`, headers configurables, autenticación opcional, `responsePath` configurable y timeout configurable.

La configuración conceptual del target incluirá:

- Nombre.
- URL.
- Método `POST`.
- Content-Type `application/json`.
- Plantilla de request.
- Placeholder de prompt `{{PROMPT}}`.
- Ruta de extracción de respuesta mediante `responsePath`.
- Headers opcionales.
- Tipo de autenticación.
- Timeout.

El scanner no dependerá de un body fijo como `{ "message": "..." }`. El usuario podrá configurar cómo construir la request, dónde insertar el prompt, qué headers enviar, cómo autenticarse y dónde extraer el texto de la respuesta.

## Fuera del alcance inicial

Quedan fuera del MVP:

- Auditorías de seguridad completas o certificaciones.
- Ataques contra aplicaciones sin autorización.
- Explotación autónoma, denegación de servicio o pruebas destructivas.
- Cobertura exhaustiva de todos los riesgos del OWASP GenAI LLM Top 10 (2026).
- Soporte completo para cualquier proveedor, formato o protocolo LLM.
- Soporte inicial para agentes, herramientas externas, streaming o RAG avanzado.
- Streaming, multipart, WebSocket, SSE, GET, SOAP y gRPC.
- Agentes complejos o herramientas externas invocadas por el target.
- Monitorización continua en producción.
- Gestión avanzada de usuarios, organizaciones, roles y multi-tenancy.
- Integración con sistemas externos de tickets o gestión de vulnerabilidades.
- Despliegue productivo completo en AWS.

El modelo de autenticación del usuario queda como **Pendiente de decisión**. Para el target, el MVP soportará únicamente `NONE`, `BEARER_TOKEN` y `API_KEY_HEADER`.

OAuth2, mTLS, Basic Auth avanzada, cookies o sesión, flujos interactivos y autenticación multi-step quedan fuera del alcance inicial.

Las credenciales nunca se incluirán en logs, informes, respuestas al frontend ni payloads persistidos sin sanitización. La estrategia tecnológica concreta para cifrado y gestión de claves queda como **Pendiente de decisión**.

Los scans del MVP se ejecutarán de forma asíncrona. La solicitud inicial creará el scan y devolverá un identificador; el frontend consultará posteriormente su estado y progreso mediante polling. La tecnología concreta para procesar las tareas en segundo plano queda como **Pendiente de decisión**.

Los estados mínimos del scan serán `PENDING`, `RUNNING`, `COMPLETED` y `FAILED`. `CANCELLED` y `PARTIAL` no forman parte del conjunto mínimo del MVP y su incorporación queda como **Pendiente de decisión**.

## Limitaciones iniciales

- La calidad de los resultados dependerá de los casos de prueba, las reglas y el modelo evaluador seleccionados.
- La evaluación mediante LLM puede producir falsos positivos o falsos negativos.
- El análisis no demuestra la ausencia de vulnerabilidades.
- El soporte inicial se limitará a los formatos de integración que se definan para el MVP.
- El almacenamiento de respuestas puede contener información sensible y requerirá medidas de minimización y protección.
- Las peticiones a targets configurados por usuarios introducen riesgo de SSRF y requieren controles específicos.
- La evaluación mediante un segundo LLM no garantiza por sí sola la corrección del resultado.
- La fórmula numérica definitiva, la normalización y los umbrales de severidad todavía deben validarse.
- No se incluirá inicialmente soporte específico para agentes, herramientas externas, streaming ni RAG avanzado.
- La autenticación será opcional en la configuración del target; el mecanismo concreto de gestión segura de credenciales queda pendiente.
- El procesamiento asíncrono requerirá límites de tiempo, concurrencia, frecuencia, número de pruebas y coste de llamadas al LLM; sus valores concretos quedan como **Pendiente de decisión**.
- No se define todavía una estrategia definitiva de reintentos, recuperación tras reinicios ni cancelación de scans.

## Evolución futura

El catálogo detallado de funcionalidades, categorías OWASP, integraciones y mejoras fuera del MVP se mantiene en `docs/00-contexto/evolucion-futura.md`.
