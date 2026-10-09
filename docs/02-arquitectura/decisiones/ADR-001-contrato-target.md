# ADR-001: contrato configurable del endpoint objetivo

## Estado

Aceptada para el MVP.

## Contexto

Las aplicaciones LLM no comparten necesariamente el mismo formato de request ni la misma ubicación del prompt o del texto de respuesta. Un contrato fijo limitaría el scanner a una única estructura JSON.

## Decisión

El MVP soportará únicamente targets con:

- HTTP REST.
- Método `POST`.
- `Content-Type: application/json`.
- Body JSON configurable mediante plantilla.
- Placeholder de prompt `{{PROMPT}}`.
- Headers configurables.
- Autenticación opcional.
- `responsePath` configurable para extraer el texto de la respuesta.
- Timeout configurable.

La configuración conceptual se denomina `TargetConfiguration` y contiene:

```text
TargetConfiguration
- name
- url
- method = POST
- contentType = application/json
- requestTemplate
- promptPlaceholder = {{PROMPT}}
- responsePath
- headers opcionales
- authType
- timeout
```

El usuario podrá configurar cómo construir la request, dónde insertar el prompt, qué headers enviar, cómo autenticarse y dónde extraer la respuesta.

## Ejemplo conceptual

Request template:

```json
{
  "message": "{{PROMPT}}",
  "temperature": 0.2
}
```

Extracción de respuesta:

```text
responsePath = $.response.text
```

## Fuera del MVP / Evolución futura

No se soportarán inicialmente:

- Streaming.
- Multipart.
- WebSocket.
- SSE.
- GET.
- SOAP.
- gRPC.
- Agentes complejos.
- Herramientas externas invocadas por el target.

La incorporación futura de estos protocolos o capacidades requerirá una nueva decisión arquitectónica y controles específicos.

## Seguridad

La configuración del target deberá quedar sujeta posteriormente a:

- Validación estricta de URL.
- Bloqueo de localhost y loopback.
- Bloqueo de IP privadas y redes internas.
- Bloqueo de endpoints de metadata cloud.
- Límites de timeout.
- Límites de tamaño de respuesta.
- Límites de número y frecuencia de peticiones.
- Tratamiento seguro de headers y secretos.

Esta ADR fija el contrato conceptual, pero no diseña todavía la implementación concreta de esos controles.

## Consecuencias

La decisión permite analizar targets con estructuras JSON diferentes sin ampliar el MVP a múltiples protocolos. A cambio, introduce configuración adicional, validación de plantillas y la necesidad de proteger headers y credenciales.
