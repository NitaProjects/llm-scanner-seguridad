# Modelo de amenazas inicial

## Propósito

Este documento identifica las amenazas principales del MVP de LLM Scanner Seguridad. No sustituye a un análisis de riesgos completo y deberá evolucionar junto con la arquitectura y los requisitos OpenSpec.

## Activos principales

- Configuración de proyectos y targets.
- Credenciales o referencias de autenticación del target.
- Prompts, peticiones y respuestas de los scans.
- Evidencias y findings.
- Informes generados.
- Credenciales de proveedores LLM.
- Disponibilidad y coste de la plataforma.

## Actores y superficies de ataque

- Usuario autorizado que configura y ejecuta análisis.
- Usuario no autorizado que intenta acceder a proyectos o resultados.
- Endpoint objetivo configurado por el usuario.
- Respuestas maliciosas del target que intentan influir en el evaluador.
- Atacante externo que intenta utilizar el scanner como proxy o extraer información.
- Proveedor externo de modelo evaluador.

## Riesgo SSRF

El backend realizará peticiones a URLs proporcionadas por usuarios mediante una configuración de target. Un atacante podría intentar utilizar el scanner para acceder a servicios internos, localhost, rangos privados o endpoints de metadatos cloud.

Controles previstos:

- Aceptar únicamente protocolos y destinos permitidos.
- Bloquear localhost, loopback, redes privadas, interfaces locales y metadatos cloud.
- Resolver y validar el destino antes de realizar la petición.
- Controlar redirecciones y evitar que salten a destinos no autorizados.
- Aplicar timeouts, límites de tamaño, frecuencia y concurrencia.
- Mantener un mecanismo para demostrar la autorización del target.
- Validar la plantilla JSON, el `responsePath` y los headers configurables sin permitir transformaciones arbitrarias.

La estrategia concreta de allowlist, validación DNS y autorización queda como **Pendiente de decisión**.

## Configuración del target

La configuración admitirá headers y autenticación opcional mediante `NONE`, `BEARER_TOKEN` o `API_KEY_HEADER`. Estos valores pueden contener secretos y no deben aparecer en logs, findings ni informes sin redacción.

Reglas de protección:

- Las credenciales nunca deben aparecer en logs.
- Nunca deben incluirse en informes.
- Nunca deben almacenarse en texto plano.
- No deben exponerse al frontend después de ser configuradas.
- Los errores no deben devolver secretos.
- Los payloads persistidos deben sanitizar headers sensibles.
- Los secretos deben descifrarse únicamente en memoria durante la ejecución autorizada.

La estrategia concreta de cifrado y gestión de claves queda como **Pendiente de decisión**. En producción, AWS Secrets Manager podrá almacenar y gestionar los secretos, mientras que AWS KMS podrá gestionar las claves de cifrado; ambos servicios pueden utilizarse conjuntamente.

El MVP no aceptará configuraciones basadas en GET, streaming, multipart, WebSocket, SSE, SOAP o gRPC.

## Respuestas no confiables y prompt injection indirecta

La aplicación objetivo puede devolver contenido diseñado para influir en el modelo evaluador o en el sistema. Las respuestas deben tratarse como datos no confiables, delimitarse en el contexto de evaluación y no tener capacidad para cambiar instrucciones, configuración o flujo.

## Exposición de información sensible

Las peticiones y respuestas pueden contener credenciales, datos personales, información interna o secretos. Deben aplicarse minimización, redacción, controles de acceso y políticas de retención antes de almacenar o mostrar evidencias.

## Errores de evaluación

El evaluador LLM puede producir falsos positivos, falsos negativos o resultados inconsistentes. La mitigación prevista es combinar reglas deterministas, expectativas de resultado, evaluación semántica, confianza y revisión manual opcional.

La integración se realizará mediante un puerto independiente del proveedor. La selección concreta del proveedor y del modelo puede introducir dependencia externa, costes, límites de uso y exposición de datos enviados para evaluación. Las API keys deben proceder de configuración externa y no aparecer en código, logs, findings ni informes.

También deben controlarse el número de evaluaciones, los timeouts, el tamaño de prompts y respuestas, el rate limiting y el comportamiento ante un fallo del proveedor. Un error del evaluador debe distinguirse de una vulnerabilidad del target y no debe generar automáticamente un finding.

## Abuso de recursos

Un usuario podría provocar costes o degradación mediante demasiados scans, prompts grandes o targets lentos. Se deberán aplicar límites de peticiones, tamaño, tiempo, concurrencia y coste.

La ejecución asíncrona evita mantener una petición HTTP abierta durante todo el análisis, pero introduce trabajo pendiente y tareas en segundo plano que también deben protegerse. Deben controlarse el número máximo de pruebas por scan, la concurrencia de scans y ejecuciones, el rate limiting, los timeouts del target y del evaluador y el coste máximo de llamadas al LLM.

Los fallos individuales deben persistirse sin reintentos automáticos no definidos. La estrategia de reintentos, recuperación tras reinicios, cancelación y limpieza de tareas huérfanas queda como **Pendiente de decisión**. Un fallo del modelo evaluador debe registrarse como error de evaluación y no clasificarse automáticamente como vulnerabilidad del target.

## Alcance pendiente

- Modelo de autenticación y autorización de usuarios.
- Gestión de credenciales del target.
- Política de retención y borrado de evidencias.
- Nivel de aislamiento de los scans.
- Controles concretos para producción y AWS.
