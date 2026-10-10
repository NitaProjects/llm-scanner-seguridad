# Alcance del proyecto

## Alcance del MVP

El MVP incluirá:

- Creación y gestión básica de proyectos de análisis.
- Configuración de un endpoint de una aplicación LLM autorizada.
- Ejecución de un plan fijo de exactamente cuatro SecurityTests, sin selección configurable por usuario.
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
- Generación de un informe estructurado persistido `report-v1` y un resultado `scoring-v1`.
- Aplicación vulnerable propia para demostraciones y pruebas reproducibles.
- Persistencia de proyectos, scans, ejecuciones y findings.
- Ejecución asíncrona de scans, con consulta posterior de estado y progreso.

El backend core está aprobado e implementado por `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`. El plan contiene un SecurityTest inmutable por categoría. Para confirmar COMPLETED, cada Scan nuevo requiere cuatro ejecuciones y cuatro evaluaciones COMPLETED, ScanScore y SecurityReport íntegros; score, informe y cierre se confirman en una misma transacción. Un fallo técnico global puede llevar a FAILED; el fallo/degradación LLM consolidado puede producir INCONCLUSIVE o VULNERABLE según las reglas, sin implicar Scan FAILED. Los históricos conservan su semántica sin backfill.

Frontend/dashboard y demo vulnerable siguen en progreso. La instalación/ejecución completa, el experimento académico, la validación real de OpenAI y los entregables finales del TFM siguen pendientes. Los contratos aprobados están en `openspec/changes/`; `openspec/specs/` aún no contiene specs consolidadas.

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

Las credenciales nunca se incluirán en logs, informes, respuestas al frontend ni payloads persistidos sin sanitización. D-CRYPTO está cerrado para el MVP local: AES-256-GCM, envelope versionado y clave externa de 256 bits, según [ADR-002](../02-arquitectura/decisiones/ADR-002-autenticacion-target-credenciales.md). Custodia operativa, rotación avanzada y gestión en AWS siguen como **Pendiente de decisión**.

Los scans del MVP se ejecutan de forma asíncrona. La solicitud inicial crea el scan y devuelve un identificador; la API permite consultar estado y progreso derivado de las ejecuciones mediante polling. `scan-execution` implementa un ThreadPoolTaskExecutor interno acotado y configurable, con rechazo inmediato y un único Scan activo por Project. La recuperación local marca activos FAILED/SCAN_INTERRUPTED sin relanzar trabajo, según [ADR-003](../02-arquitectura/decisiones/ADR-003-ejecucion-asincrona-scans.md). La integración frontend sigue en progreso.

Los estados mínimos del scan serán `PENDING`, `RUNNING`, `COMPLETED` y `FAILED`. `CANCELLED` y `PARTIAL` no forman parte del conjunto mínimo del MVP y su incorporación queda como **Pendiente de decisión**.

## Limitaciones iniciales

- La calidad de los resultados dependerá de los casos de prueba, las reglas y el modelo evaluador seleccionados.
- La evaluación mediante LLM puede producir falsos positivos o falsos negativos.
- El análisis no demuestra la ausencia de vulnerabilidades.
- El soporte inicial se limitará a los formatos de integración que se definan para el MVP.
- El almacenamiento de respuestas puede contener información sensible y requerirá medidas de minimización y protección.
- Las peticiones a targets configurados por usuarios introducen riesgo de SSRF y requieren controles específicos.
- La evaluación mediante un segundo LLM no garantiza por sí sola la corrección del resultado.
- confidence-v1, severity-v1 y scoring-v1 tienen políticas ejecutables aprobadas e implementadas, incluidas fórmula, redondeo y bandas de riesgo (ADR-005). La calibración empírica sigue pendiente. El score es heurístico, no una probabilidad; cero significa ningún Finding detectado en las pruebas completadas, sin demostrar seguridad. INCONCLUSIVE conserva un contador separado.
- No se incluirá inicialmente soporte específico para agentes, herramientas externas, streaming ni RAG avanzado.
- La autenticación del target es opcional; el cifrado local está implementado. Autenticación/autorización productiva de usuarios, retención/borrado de evidencia y custodia/rotación avanzada de claves siguen pendientes.
- El procesamiento aplica límites aprobados de tiempo, concurrencia, frecuencia, tamaños y cuatro pruebas por Scan (STE-09/ADR-003), además de cuatro intentos LLM como máximo y cuotas del evaluador (evaluation D-15/ADR-004). El presupuesto compartido es 300 s; el control monetario estimado es opcional y externo, sin acreditar tarifa ni coste real verificados.
- La recuperación tras reinicios de una única instancia está definida e implementada sin reenvíos. Reintentos, cancelación, PARTIAL, cola durable y workers distribuidos siguen pendientes y fuera del MVP actual.
- El informe estructurado está implementado; exportaciones HTML/Markdown/PDF, despliegue AWS y operación productiva permanecen fuera del incremento actual y pendientes de trabajo posterior.

## Evolución futura

El catálogo detallado de funcionalidades, categorías OWASP, integraciones y mejoras fuera del MVP se mantiene en `docs/00-contexto/evolucion-futura.md`.
