# ADR-003: Ejecución asíncrona de scans

## Estado

Aceptada para el comportamiento funcional del MVP.

La tecnología concreta de procesamiento y las políticas operativas asociadas permanecen **Pendientes de decisión**.

## Contexto

Un scan puede ejecutar varias pruebas contra el target y realizar llamadas adicionales al modelo evaluador. La duración total es variable y no debe depender de que una petición HTTP permanezca abierta hasta completar todo el análisis.

La ejecución debe permitir registrar el progreso, conservar el histórico y distinguir los errores del target, de las pruebas y del evaluador.

## Decisión

Los scans del MVP serán asíncronos.

El flujo conceptual será:

```text
Solicitud de scan
    -> creación de Scan con estado PENDING
    -> respuesta inmediata con scanId
    -> procesamiento en segundo plano
    -> estado RUNNING
    -> ejecución de SecurityTests
    -> evaluación de resultados
    -> generación de Findings
    -> cálculo de resultados
    -> estado COMPLETED o FAILED
```

El controller no realizará directamente todo el procesamiento. La ejecución quedará desacoplada de la entrada HTTP mediante los casos de uso y puertos de aplicación definidos por la arquitectura.

El frontend consultará periódicamente el estado y el progreso del scan mediante polling. La API definitiva y sus contratos se definirán en el cambio OpenSpec correspondiente; este ADR no fija endpoints internos definitivos.

## Estados mínimos

- `PENDING`: el scan ha sido creado y todavía no ha comenzado.
- `RUNNING`: el scan está procesando sus pruebas y evaluaciones.
- `COMPLETED`: el procesamiento ha terminado.
- `FAILED`: un error impide completar el procesamiento del scan.

Un error de una prueba individual no implica necesariamente que el scan completo pase a `FAILED`. El error debe persistirse en su `TestExecution` y el comportamiento final ante resultados parciales queda **Pendiente de decisión**.

`CANCELLED` y `PARTIAL` no se incorporan al conjunto mínimo del MVP. Su necesidad se revisará al diseñar cancelación, recuperación y tratamiento de errores parciales.

## Persistencia conceptual

`Scan` debe permitir consultar el progreso y el ciclo de vida mediante, como mínimo:

- `id`.
- `projectId`.
- `status`.
- `createdAt`.
- `startedAt`.
- `finishedAt`.
- `errorReason` opcional.
- `totalTests`.
- `completedTests`.

Cada `TestExecution` debe conservar su resultado o su error, sin confundir un fallo del LLM evaluador con una vulnerabilidad del target.

## Límites y errores

La ejecución asíncrona deberá contemplar timeouts, concurrencia, rate limiting, número máximo de pruebas y coste de llamadas al LLM. Los valores concretos son **Pendiente de decisión**.

No se define todavía la estrategia de reintentos, recuperación tras reinicios, cancelación ni manejo definitivo de scans parciales.

## Alternativas descartadas para el MVP

- Ejecución síncrona dentro de la petición HTTP: puede producir timeouts y una peor experiencia de usuario.
- Cola y workers persistentes: aportan aislamiento y escalabilidad, pero introducen infraestructura adicional antes de que sea necesaria.

## Fuera del MVP / Evolución futura

Se podrán valorar posteriormente:

- AWS SQS.
- Workers independientes.
- Procesamiento distribuido.
- Reintentos automáticos.
- Dead-letter queue.
- Cancelación de scans.
- WebSocket, Server-Sent Events y otras notificaciones en tiempo real.

