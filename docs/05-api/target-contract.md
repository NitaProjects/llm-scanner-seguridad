# Contrato conceptual del target

Este documento describe la integración entre LLM Scanner Seguridad y la aplicación LLM objetivo. No define todavía endpoints internos de LLM Scanner Seguridad ni un contrato OpenAPI definitivo.

## Restricciones del MVP

- Protocolo: HTTP REST.
- Método: `POST`.
- Content-Type: `application/json`.
- Request: body JSON construido mediante plantilla.
- Prompt: placeholder `{{PROMPT}}`.
- Response: JSON con ruta configurable mediante `responsePath`.
- Headers: configurables.
- Autenticación: opcional, con `NONE`, `BEARER_TOKEN` o `API_KEY_HEADER`.
- Timeout: configurable y sujeto a límites de seguridad.

## Configuración conceptual

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
- authConfig
- timeout
```

## Construcción de la request

La plantilla define el body JSON que se enviará al target. El scanner sustituirá el placeholder `{{PROMPT}}` por el prompt del Security Test.

Ejemplo:

```json
{
  "message": "{{PROMPT}}",
  "temperature": 0.2
}
```

Por tanto, no se asume un formato fijo como:

```json
{
  "message": "..."
}
```

## Extracción de la respuesta

`responsePath` indica dónde se encuentra el texto relevante dentro del JSON recibido.

Ejemplo:

```text
responsePath = $.response.text
```

La sintaxis exacta, validación de rutas y tratamiento de respuestas sin coincidencia quedan pendientes de especificar durante el diseño del change correspondiente.

## Autenticación y headers

El target puede no requerir autenticación o puede necesitar credenciales mediante uno de los tipos soportados.

### `NONE`

No se envían credenciales.

### `BEARER_TOKEN`

Header conceptual:

```text
Authorization: Bearer <TOKEN>
```

### `API_KEY_HEADER`

Configuración conceptual:

```text
headerName
secretValue
```

Ejemplo:

```text
X-API-Key: <SECRET>
```

Las credenciales nunca deben aparecer en logs, informes, errores ni respuestas al frontend. Tampoco deben almacenarse en texto plano ni incluirse sin sanitización en payloads persistidos.

La estrategia concreta de cifrado y gestión de claves queda como **Pendiente de decisión**. En producción, AWS Secrets Manager podrá almacenar y gestionar los secretos, mientras que AWS KMS podrá gestionar las claves de cifrado; ambos servicios pueden utilizarse conjuntamente.

## Fuera del MVP

No se soportarán inicialmente OAuth2, mTLS, Basic Auth avanzada, autenticación basada en cookies o sesión, flujos interactivos, autenticación multi-step, streaming, multipart, WebSocket, SSE, GET, SOAP, gRPC, agentes complejos ni herramientas externas invocadas por el target.
