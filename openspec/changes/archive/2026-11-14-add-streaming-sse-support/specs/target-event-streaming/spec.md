## Purpose

Reconstruir la respuesta textual de una petición autorizada con eventos SSE o JSONL y entregarla como una sola interacción.

## ADDED Requirements

### Requirement: SSE-01 Colecta incremental y terminal

Un target vinculado explícitamente SHALL poder responder a un POST JSON con text/event-stream o el media type JSONL declarado. SHALL decodificar UTF-8 y separar frames/líneas incrementalmente, usar el selector declarado, descartar eventos no útiles y aplicar delta o snapshot en orden. SHALL completar una única NormalizedInteraction con texto reconstruido solo tras terminal explícito y al menos un evento de contenido válido. Un prefijo tras EOF, error, cancelación o terminal ausente SHALL NOT ser evaluable. El camino REST y chat sin binding SHALL conservar su comportamiento.

#### Scenario: Reconstrucción con fragmentación
- **WHEN** se reciben caracteres y frames partidos, comentarios y un terminal después de contenido útil
- **THEN** el resultado contiene exactamente el texto ensamblado, con una sola ejecución y sin frames brutos

### Requirement: SSE-03 Negociación Accept del binding

El intercambio de streaming SHALL enviar un único `Accept` determinado por su binding, `text/event-stream` para SSE o el media type JSONL declarado (`application/x-ndjson` o `application/jsonl`). SHALL reemplazar el valor ordinario configurado o el default solo en esa ruta. Si un header de credencial, sensible o reservado reclama ese nombre, el binding SHALL rechazarse antes de red y descifrado mediante comparación de nombres insensible a mayúsculas. REST_LEGACY y OPENAI_COMPATIBLE sin binding SHALL conservar su negociación actual.

#### Scenario: Target negocia por Accept
- **WHEN** el target de prueba exige `Accept: text/event-stream` y el binding SSE se usa con un `Accept: application/json` ordinario configurado
- **THEN** el target recibe un único `Accept: text/event-stream`, devuelve un stream válido y se reconstruye el texto exacto

#### Scenario: Accept reclamado por credencial
- **WHEN** un binding SSE usa un target cuya credencial o asignación sensible o reservada reclama `aCcEpT`
- **THEN** se rechaza antes de red y descifrado, con cero requests y diagnóstico sin valores sensibles

### Requirement: SSE-02 Cierre y límites seguros

SHALL aplicar allowlist, DNS/IP fijada, TLS, autorización de credencial y cero redirects/retries. SHALL reutilizar las cotas vigentes de respuesta, texto y evidencia y acotar cada frame/línea a 32 KiB y cada petición a 1 024 unidades lógicas como máximos iniciales. Un límite efectivo menor SHALL prevalecer. El deadline absoluto SHALL incluir el presupuesto restante del Scan y los heartbeats SHALL consumir cuota. Agotamiento o interrupción SHALL cerrar la lectura activa. La redacción SHALL operar también sobre el texto ensamblado antes de persistir.

#### Scenario: Flood y credencial partida
- **WHEN** un stream divide una credencial entre deltas o supera el límite de eventos con heartbeats
- **THEN** el caso completo redacta la coincidencia y el exceso termina con fallo técnico y conexión cerrada, sin evaluar prefijo
