## Purpose

Recoger respuestas SSE y JSONL con límites y finalización explícita para obtener texto normalizado verificable sin exponer los protocolos al motor de seguridad.

## ADDED Requirements

### Requirement: TS-01 Eventos normalizados y ensamblado

**Historia:** Como analista quiero reconstruir contenido transmitido por eventos sin duplicar snapshots ni confundir chunks con mensajes.
**Prioridad:** Alta M3/D2. **Dependencias:** NI-01..03; IS-03.

La integración SHALL soportar SSE text/event-stream y JSONL mediante configuración de selector textual, modo delta/snapshot, señal de error y señal final. SHALL procesar UTF-8 incremental, frames fragmentados y SSE multiline data; JSONL SHALL exigir JSON completo por línea acotada. Eventos normalizados SHALL tener secuencia local, tiempo relativo y tipo START/TEXT_DELTA/TEXT_SNAPSHOT/USAGE/END/ERROR/WARNING. Delta SHALL concatenarse en orden y snapshot SHALL sustituir, sin duplicación. Comentarios/heartbeats SHALL contar para límites pero no añadir contenido. Tools y eventos desconocidos SHALL NOT ejecutarse.

#### Scenario: Fragmentación y snapshot
- **WHEN** un carácter UTF-8 y un frame SSE llegan en buffers separados y después se recibe un snapshot de todo el texto
- **THEN** se reconstruye texto válido en orden y el snapshot sustituye al acumulado sin duplicarlo

### Requirement: TS-02 Final explícito y error parcial

**Historia:** Como motor de evaluación quiero conocer si el stream terminó según el contrato.
**Prioridad:** Alta M3/D2. **Dependencias:** TS-01; NI-02.

El sistema SHALL requerir terminal configurado por defecto y distinguir EOF autorizado expresamente de EOF prematuro. Error después de contenido SHALL conservar partial y motivo, sin promover el prefijo a respuesta final. SHALL NOT reconectar, reproducir prompt ni aplicar Last-Event-ID automáticamente. Final ambiguo o desconocido SHALL considerarse incompleto.

#### Scenario: Conexión cerrada sin terminal
- **WHEN** llegan deltas válidos y la conexión se cierra sin la señal final requerida
- **THEN** la interacción queda INCOMPLETE con PREMATURE_EOF y no se evalúa como completa

### Requirement: TS-03 Presupuestos de streaming

**Historia:** Como operador quiero que un stream infinito o flood no agote workers ni almacenamiento.
**Prioridad:** Alta M3/D2. **Dependencias:** IS-03; NI-04.

La integración SHALL imponer deadline absoluto, timeout de contenido, bytes totales/descomprimidos, tamaño por frame/línea, eventos, texto acumulado y buffer acotado. El primero que se alcance SHALL cerrar la conexión con diagnóstico seguro y completitud falsa. Heartbeats, frames descartados y errores SHALL consumir presupuesto; ningún evento SHALL reiniciar el deadline absoluto. SHALL respetar límites agregados del Scan y procesar sin colas ilimitadas.

#### Scenario: Flood de heartbeats
- **WHEN** un endpoint emite comentarios sin contenido útil hasta agotar tiempo o eventos
- **THEN** se corta la lectura y se liberan recursos, sin mantener el worker vivo por actividad artificial
