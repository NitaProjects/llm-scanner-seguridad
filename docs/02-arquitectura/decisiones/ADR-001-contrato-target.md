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

Contexto histórico de esta decisión: la configuración del target debía quedar sujeta posteriormente a:

- Validación estricta de URL.
- Bloqueo de localhost y loopback.
- Bloqueo de IP privadas y redes internas.
- Bloqueo de endpoints de metadata cloud.
- Límites de timeout.
- Límites de tamaño de respuesta.
- Límites de número y frecuencia de peticiones.
- Tratamiento seguro de headers y secretos.

El change `target-configuration` concreta la validación local sin DNS ni HTTP: URL HTTP/HTTPS estructuralmente válida, bloqueo por defecto de localhost y literales loopback/privados, plantilla JSON objeto con un único valor completo `{{PROMPT}}` y JSONPath restringido a propiedades e índices. Timeout 1000..60000 ms, por defecto 10000; method/contentType/promptPlaceholder son constantes y no se persisten.

La excepción local/demo solo se activa con el perfil Spring `local`, `TARGET_LOCAL_DEMO_ENABLED=true` y orígenes explícitos separados por comas en `TARGET_LOCAL_DEMO_ORIGINS`. Se compara esquema, host normalizado y puerto efectivo; no se habilitan redes completas ni metadatos/link-local. La API no puede activar esta excepción. Guardar una URL no autoriza su ejecución: DNS, direcciones efectivas, redirecciones y política de autorización se implementarán en el change de ejecución.

**Nota de estado actual:** el párrafo anterior conserva la separación histórica entre configuración y ejecución. `security-test-execution` ya implementa autorización externa, DNS efectivo, validación de todas las IPs, conexión fijada a una dirección y ausencia de redirecciones/reintentos. El contrato vigente se resume en [target-contract.md](../../05-api/target-contract.md) y en la [política de autorización](../../03-seguridad/politica-de-autorizacion.md). Configurar el target sigue sin autorizar tráfico. La operación productiva y AWS permanecen pendientes.

## Consecuencias

La decisión permite analizar targets con estructuras JSON diferentes sin ampliar el MVP a múltiples protocolos. A cambio, introduce configuración adicional, validación de plantillas y la necesidad de proteger headers y credenciales.
