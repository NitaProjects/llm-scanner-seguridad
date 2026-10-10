# ADR-003: Ejecución asíncrona de scans

## Estado

Aceptada para el comportamiento funcional del MVP.

`scan-execution` concreta executor interno acotado y una única instancia. `security-test-execution` aprueba el worker real, los límites de transporte y la política de fallos individuales/globales descrita abajo. Reintentos y cancelación siguen fuera del incremento.

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

Un fallo técnico individual del target permite continuar, pero cualquier TestExecution FAILED obliga a Scan FAILED/SCAN_PROCESSING_FAILED conservando éxitos. Un fallo global aborta pendientes. `evaluation-engine` extiende prospectivamente el cierre mediante un perfil interno durable: nuevos scans exigen cuatro TestExecutions y cuatro EvaluationResults COMPLETED. VULNERABLE, INCONCLUSIVE y fallos LLM consolidados coherentemente no hacen fallar Scan. EvaluationResult FAILED o un fallo global de tiempo/persistencia/invariantes sí lo impiden. Históricos sin perfil mantienen su significado sin backfill. No se añade PARTIAL ni un octavo campo público.

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

Estos siguen siendo exactamente los siete campos. security-test-execution permite derivar total/terminales del listado GET executions, sin añadir totalTests/completedTests a Scan ni porcentajes artificiales. La columna global_score se conserva sin uso ni mapeo.

Cada `TestExecution` debe conservar su resultado o su error, sin confundir un fallo del LLM evaluador con una vulnerabilidad del target.

## Límites y errores

El worker usa los límites técnicos de STE-09: cuatro pruebas, request 64 KiB, response 256 KiB, texto 64 KiB, evidencia 128 KiB por ejecución/512 KiB por Scan y presupuesto compartido 300 s. Hay una invocación target global y al menos 1 s entre inicios. DNS máximo 2 s, conexión/TLS 5 s y timeout del target 1..60 s, recortados por deadline monotónico. Los límites independientes del evaluador están aprobados por evaluation-engine D-15 y ADR-004 dentro de los mismos 300 s.

El executor dedicado es `ThreadPoolTaskExecutor`, con `AbortPolicy` y configuración externa `SCAN_EXECUTOR_CORE_POOL_SIZE`, `SCAN_EXECUTOR_MAX_POOL_SIZE` y `SCAN_EXECUTOR_QUEUE_CAPACITY`. Defaults técnicos 1/1/10, ajustables y ajenos al contrato funcional. Se valida core positivo, max >= core y cola finita no negativa; no se ejecuta en el hilo HTTP al saturar. La exclusión por Project corresponde al índice único parcial `uq_scans_active_project` para PENDING/RUNNING; un exists previo no es garantía suficiente.

T1 confirma PENDING antes de dispatch; T2 confirma el reclamo condicional RUNNING; el trabajo ocurre sin transacción larga; T3 confirma COMPLETED/FAILED. Cada escritura usa TransactionTemplate/REQUIRES_NEW en infraestructura, sin Spring en dominio/aplicación. Un rollback ambiente no revierte esas transacciones. El hueco commit/dispatch no es atómico: una caída deja un activo para la recuperación posterior, sin outbox ni entrega durable.

El rechazo de dispatch se compensa a FAILED/SCAN_DISPATCH_FAILED y devuelve 503 con Location. Una excepción ordinaria o fallo de confirmar RUNNING/COMPLETED intenta FAILED/SCAN_PROCESSING_FAILED en otra transacción. Si tampoco se confirma FAILED, se conserva el estado durable, se registra solo id/código y se bloquean nuevos inicios/reclamos hasta reinicio; tareas ya RUNNING pueden terminar y GET sigue consultando lo confirmado.

`ScanStartup` confirma recuperación de ejecuciones PENDING/RUNNING a FAILED/EXECUTION_INTERRUPTED y scans activos a FAILED/SCAN_INTERRUPTED en una misma transacción, antes de habilitar admisión y crear el executor. Conserva startedAt, evidencia y terminales; no relanza. También cierra ejecuciones activas de scans terminales. Fallar la recuperación impide arrancar. T3 fallida cierra hijos activos y Scan atómicamente; una transición de Scan que no coincide no modifica hijos. Esta limpieza solo es válida con una instancia. Reintentos y cancelación no se deciden aquí.

## Alternativas descartadas para el MVP

- Ejecución síncrona dentro de la petición HTTP: puede producir timeouts y una peor experiencia de usuario.
- Cola y workers persistentes: aportan aislamiento y escalabilidad, pero introducen infraestructura adicional antes de que sea necesaria.

## Extensión evaluation-engine

La extensión `evaluation-engine` conserva las fronteras transaccionales: preparación de ocho filas; claim condicional de evaluación y reserva 0/1 confirmadas antes de red; resultado terminal y Finding en una transacción nueva corta. No se repiten llamadas ante commit ambiguo. ScanStartup recupera también evaluaciones activas a FAILED/EVALUATION_INTERRUPTED en la misma transacción antes de admisión/executor. Conserva terminales y Findings. Solo un Scan nuevo que estaba activo permite registrar una evaluación ausente como interrupción explícita; nunca se rellenan históricos ni se reconstruye una respuesta perdida.

## Extensión scoring-engine

T1 fija scoring-v1 junto a evaluation-engine-v1 antes del dispatch, sin modificar los siete campos públicos. Para estos scans, COMPLETED requiere además ScanScore de la versión fijada. T3 bloquea Scan, comprueba plan/finales y correspondencia de Findings, calcula localmente e inserta score y transición a COMPLETED en la misma transacción corta. Un false inesperado o error de cálculo/escritura/commit revierte ambos y aplica FAILED/SCAN_PROCESSING_FAILED; si también falla compensar se conserva la compuerta existente. Nunca se fabrica INCONCLUSIVE técnico.

La PK (scan_id,scoring_version) y el lock persistente evitan duplicados concurrentes; terminales no se reabren, fechas/resultados no se sobrescriben. Un commit ambiguo conserva el par ya confirmado o permite la compensación condicional sin relanzar target/evaluador. Recovery interrumpe activos sin score y conserva Findings; tras commit conserva score+COMPLETED. COMPLETED marcado sin score o score durable con estado incompatible es error de integridad, no reparación automática. Históricos sin marcador mantienen su semántica sin scoring retroactivo; global_score permanece sin uso. La consulta /score distingue resultado cero de ausencia mediante availability, según ADR-005/OpenAPI.

## Fuera del MVP / Evolución futura

Se podrán valorar posteriormente:

- AWS SQS.
- Workers independientes.
- Procesamiento distribuido.
- Reintentos automáticos.
- Dead-letter queue.
- Cancelación de scans.
- WebSocket, Server-Sent Events y otras notificaciones en tiempo real.
