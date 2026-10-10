# ADR-003: Ejecución asíncrona de scans

## Estado

Aceptada para el comportamiento funcional del MVP.

`scan-execution` concreta la tecnología y recuperación del incremento local de una única instancia: executor interno de Spring acotado/configurable, un scan activo por Project y recuperación a FAILED tras reinicio. Reintentos, cancelación y resultados parciales siguen pendientes.

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

El incremento `scan-execution` implementa únicamente el ciclo de vida mediante `StartScanUseCase`, `ProcessScanUseCase`, `GetScanUseCase`, `ListProjectScansUseCase` y `RecoverInterruptedScansUseCase`. `ScanDispatcherPort` recibe el UUID y `ScanWorkPort` separa trabajo de estados. Su adaptador temporal no-op no genera pruebas, findings, score ni informes; COMPLETED solo acredita esa unidad temporal. El flujo completo anterior sigue perteneciendo al MVP global y requiere changes posteriores.

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

En `scan-execution` estos son exactamente los siete campos. `totalTests` y `completedTests` se aplazan a SecurityTest/TestExecution, cuando haya un total significativo; no se publican 0/0 ni porcentajes artificiales. La columna existente global_score se conserva sin uso ni mapeo al modelo.

Cada `TestExecution` debe conservar su resultado o su error, sin confundir un fallo del LLM evaluador con una vulnerabilidad del target.

## Límites y errores

La ejecución asíncrona deberá contemplar timeouts, concurrencia, rate limiting, número máximo de pruebas y coste de llamadas al LLM. Los valores concretos son **Pendiente de decisión**.

El executor dedicado es `ThreadPoolTaskExecutor`, con `AbortPolicy` y configuración externa `SCAN_EXECUTOR_CORE_POOL_SIZE`, `SCAN_EXECUTOR_MAX_POOL_SIZE` y `SCAN_EXECUTOR_QUEUE_CAPACITY`. Defaults técnicos 1/1/10, ajustables y ajenos al contrato funcional. Se valida core positivo, max >= core y cola finita no negativa; no se ejecuta en el hilo HTTP al saturar. La exclusión por Project corresponde al índice único parcial `uq_scans_active_project` para PENDING/RUNNING; un exists previo no es garantía suficiente.

T1 confirma PENDING antes de dispatch; T2 confirma el reclamo condicional RUNNING; el trabajo ocurre sin transacción larga; T3 confirma COMPLETED/FAILED. Cada escritura usa TransactionTemplate/REQUIRES_NEW en infraestructura, sin Spring en dominio/aplicación. Un rollback ambiente no revierte esas transacciones. El hueco commit/dispatch no es atómico: una caída deja un activo para la recuperación posterior, sin outbox ni entrega durable.

El rechazo de dispatch se compensa a FAILED/SCAN_DISPATCH_FAILED y devuelve 503 con Location. Una excepción ordinaria o fallo de confirmar RUNNING/COMPLETED intenta FAILED/SCAN_PROCESSING_FAILED en otra transacción. Si tampoco se confirma FAILED, se conserva el estado durable, se registra solo id/código y se bloquean nuevos inicios/reclamos hasta reinicio; tareas ya RUNNING pueden terminar y GET sigue consultando lo confirmado.

`ScanStartup` confirma recuperación de todos los PENDING/RUNNING a FAILED/SCAN_INTERRUPTED antes de habilitar admisión y crear el executor. Conserva startedAt y terminales; no relanza. Fallar la recuperación impide arrancar el backend. Esta limpieza solo es válida con una instancia; no se soportan despliegues concurrentes. Reintentos, cancelación y manejo definitivo de resultados parciales no se deciden aquí.

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
