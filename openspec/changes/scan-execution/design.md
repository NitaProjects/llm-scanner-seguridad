## Context

Motivación: [proposal.md](proposal.md). Contrato aprobado: [specs/scan-execution/spec.md](specs/scan-execution/spec.md), SE-01 a SE-10.

Observaciones relevantes, sin reabrir decisiones anteriores:

- El usuario confirma project-management y target-configuration implementados, validados y aprobados. Sus registros históricos no se reevalúan en esta tarea.
- openspec/specs/ solo contiene .gitkeep; se han revisado ambos changes como referencia funcional.
- El backend ya separa domain, application, adapters/in, adapters/out y config. ProjectRepositoryPort.findById permite comprobar Project. TargetConfigurationRepositoryPort.findByProjectId devuelve configuración con material cifrado: no es necesario recuperarlo para este incremento.
- La composición existente emplea TransactionTemplate con REQUIRES_NEW para garantizar commit antes de devolver éxito, manteniendo Spring fuera de dominio y contratos de aplicación.
- La API usa rutas /api/projects y errores {code,message,fieldErrors}. No hay contrato REST de scans ni implementación Java de Scan.
- V1 contiene scans(id UUID, project_id UUID FK projects, status VARCHAR(30), started_at TIMESTAMPTZ nullable, finished_at TIMESTAMPTZ nullable, global_score NUMERIC(5,2) nullable, created_at TIMESTAMPTZ NOT NULL con default). Existen PK e idx_scans_project_id; faltan error_reason, checks de estados/fechas y garantía de un activo por proyecto. V2 crea target_configurations y no altera scans.
- ADR-003 aprueba asincronía, polling y cuatro estados; su texto consolidado todavía deja tecnología y recuperación pendientes y enumera totalTests/completedTests como mínimos conceptuales. Este change registra las decisiones ahora aprobadas y planifica actualizar esas referencias durante implementación. El modelo de datos también prevé contadores y score futuro; aún no existen pruebas implementadas.
- docs/04-ia/casos-de-prueba.md exige prompts/expectativas antes del motor de scans. El usuario ha aprobado un incremento anterior de ciclo de vida sin pruebas; D-07 explicita esa frontera, sin omitir el requisito antes de ejecutar seguridad real.
- Modelo de amenazas y TC-05 exigen controles de ejecución del target. No se activan por iniciar este ciclo sin HTTP; deberán aprobarse antes de sustituir el no-op por trabajo de red.

**Estado: READY FOR IMPLEMENTATION.** El usuario aprueba D-01 a D-07 con las ocho concreciones registradas en Approval Record. No hay decisiones bloqueantes para este incremento. La implementación necesita una petición posterior; en esta entrega solo se actualizan artefactos OpenSpec, sin modificar ADRs, código, SQL o configuración del backend.

## Goals / Non-Goals

**Goals:** modelar un recurso de ejecución coherente, separar solicitud y trabajo, confirmar las transiciones sin transacciones largas, acotar el executor y permitir recuperación simple en una única instancia local.

**Non-Goals:** diseñar infraestructura distribuida, asegurar entrega exactamente una vez, implementar análisis de seguridad, anticipar un modelo completo de TestExecution, snapshot de target o recuperación empresarial. Las exclusiones funcionales están en proposal.md.

## Decisions

### D-01 — Exclusión de scans activos por Project (aprobada)

| Alternativa | Ventaja | Coste/riesgo |
| --- | --- | --- |
| Permitir simultáneos | Máxima flexibilidad y comparaciones concurrentes. | Multiplica consumo y futuras peticiones al mismo target; requiere más límites y coordinación. |
| Un activo por Project | Regla pequeña, comprensible y reduce solapamiento. | Un scan pendiente o bloqueado impide otro hasta terminar o recuperarse. |

**Decisión aprobada:** un activo por Project, definiendo activo como PENDING o RUNNING. SE-04 establece 409 SCAN_ALREADY_ACTIVE. La garantía corresponde a un índice único parcial PostgreSQL sobre project_id WHERE status IN ('PENDING','RUNNING'); un exists previo solo mejora la respuesta habitual. La violación del índice se traduce a conflicto incluso si aparece al confirmar la transacción, nunca mediante una consulta posterior que pudiera confundir otros errores.

Si el primer Scan ya terminó cuando se admite el segundo, ambos inicios pueden ser válidos: se prohíbe solapamiento de estados activos, no dos solicitudes cronológicamente próximas. No se promete idempotencia. La política no modifica la concurrencia aprobada de PUT de TargetConfiguration.

### D-02 — Executor interno acotado y configurable (aprobada)

| Alternativa | Evaluación para MVP local |
| --- | --- |
| @Async con executor explícito | Sencillo, pero las excepciones/rechazo, transacciones y llamadas internas al proxy requieren cuidado. @Async void y executor por defecto no bastan. |
| Lanzamiento explícito a executor Spring | Permite conocer aceptación/rechazo antes de responder, sin proxy implícito. No persiste la cola. Seleccionado. |
| Cola interna propia | También pierde memoria al reiniciar y obliga a mantener consumidores/ciclo de vida adicionales. No aporta valor aquí. |
| Worker separado | Aísla procesamiento, pero introduce despliegue y coordinación entre procesos. Evolución futura. |
| AWS SQS | Cola durable y escalabilidad, con infraestructura cloud, entrega repetida y políticas operativas. Evolución futura. |

**Decisión aprobada:** ThreadPoolTaskExecutor interno de Spring dedicado al módulo, con pool y cola configurables técnicamente y política de rechazo inmediato AbortPolicy; no CallerRunsPolicy, cola ilimitada, tareas en el common pool ni bloqueo esperando espacio. Ningún número de hilos o plazas de cola forma parte del contrato funcional ni REST. Proyectos diferentes pueden tener scans activos y ejecutar trabajo simultáneamente según el pool configurado, manteniendo siempre D-01 por Project. Al saturar se aplica SE-07: se conserva el intento como FAILED y se devuelve 503 con Location.

Valores iniciales razonables para el entorno local, a declarar y documentar en la configuración del backend durante la implementación:

| Parámetro técnico | Valor inicial orientativo | Semántica |
| --- | --- | --- |
| corePoolSize | 1 | Hilos base del executor. |
| maxPoolSize | 1 | Máximo inicial de hilos; modificable mediante configuración. |
| queueCapacity | 10 | Capacidad inicial finita; modificable mediante configuración. |

Estos valores son defaults técnicos ajustables, no requisitos rígidos ni una calibración de carga. Se enlazarán desde configuración externa siguiendo las convenciones del backend, sin exposición en el DTO de inicio, la representación Scan o PostgreSQL. Validar configuración coherente antes de habilitar procesamiento: corePoolSize positivo, maxPoolSize no inferior al core y queueCapacity finita no negativa. Las pruebas usarán capacidades pequeñas controladas y más de una configuración, sin asumir los defaults como comportamiento contractual. Esta entrega no crea ni modifica archivos de configuración.

ScanDispatcherPort expresa únicamente `dispatch(scanId)` con aceptación local o rechazo sanitizado. No devuelve Future ni tipos Spring. El adaptador incorpora UUID como dato, no una entidad JPA, sesión, secreto ni contexto transaccional del HTTP. La aceptación no prueba durabilidad de cola ni éxito del trabajo. Una entrada asíncrona llama ProcessScanUseCase; un futuro adaptador SQS podrá llamar el mismo caso de uso tras revisar entrega e idempotencia para efectos externos.

ScanWorkPort expresa la unidad de trabajo sustituible. Su adaptador temporal NoOpScanWorkAdapter retorna sin efectos externos, sin sleeps, pruebas simuladas ni contadores. El puerto tiene valor porque separa ciclo de vida de análisis futuro y permite probar errores con dobles; no se añade un puerto HTTP, evaluador o framework genérico de workflows.

### D-03 — Reinicio y fallos no recuperables (aprobada)

| Política tras reinicio | Ventaja | Riesgo/coste |
| --- | --- | --- |
| Reanudar PENDING y reintentar RUNNING | Reduce intervención manual. | Exige garantía de efectos repetidos y elección reproducible de trabajo/target; aún no existe. |
| Dejar activos intactos | Ningún trabajo de arranque. | Mantiene scans huérfanos y puede bloquear permanentemente Project. Descartada. |
| Marcar ambos FAILED | Consistente y simple; permite iniciar otro explícitamente. | Pierde trabajo encolado, sin recuperación automática. Seleccionada. |

Un RecoverInterruptedScansUseCase, invocado por infraestructura de arranque antes de habilitar REST de inicio y executor, confirma en una transacción PENDING/RUNNING → FAILED, errorReason SCAN_INTERRUPTED y finishedAt coherente. Conserva startedAt y todos los terminales. Si no puede confirmar, no habilita el subsistema de scans; puede fallar el arranque del backend, sin anunciar disponibilidad. El despliegue es de una única instancia; una segunda instancia no puede ejecutar esta limpieza mientras otra trabaja. No se introducen identificadores de worker, leases o locks distribuidos.

Durante ejecución, una excepción ordinaria de trabajo se traduce a SCAN_PROCESSING_FAILED mediante una transacción nueva. No se usa Throwable indiscriminadamente ni se promete persistir ante caída abrupta de JVM, falta de memoria o indisponibilidad de PostgreSQL. Las caídas abruptas se resuelven en el siguiente arranque.

Si falla confirmar RUNNING, no se invoca trabajo y se intenta registrar PENDING → FAILED con SCAN_PROCESSING_FAILED y startedAt null. Si falla confirmar COMPLETED, se intenta registrar FAILED con el mismo motivo desde el último estado confirmado. Si no puede confirmarse tampoco FAILED, se registra solo un evento operacional sanitizado con id/código, se bloquean nuevos inicios y reclamaciones mediante una compuerta local del adaptador/composición y se requiere reinicio con recuperación. Las tareas ya RUNNING intentan finalizar; las encoladas no reclaman y quedan para recuperación. No hay bucle automático, watchdog ni reintentos avanzados. GET sigue mostrando el estado confirmado o 500 si tampoco puede leer.

Esta degradación evita inventar estados confirmados; no garantiza que PostgreSQL cambie durante su caída. El coste asumido es que un fallo grave de persistencia obliga a reiniciar el subsistema/backend aun si PostgreSQL se recupera. La compuerta y el orden de arranque son detalles de infraestructura; dominio y aplicación reciben rechazo del lanzamiento/operación, sin conocer Spring.

Precisiones de implementación tras review, sin cambiar D-03 ni SE-07/SE-09: una excepción al confirmar la reclamación no demuestra rollback de RUNNING. Si la compensación condicional desde PENDING devuelve false, no se atribuye la reclamación a este worker ni se intenta modificar RUNNING; se conserva el estado durable, no se ejecuta trabajo y se bloquean nuevos inicios/reclamaciones hasta recuperación tras reinicio. Una entrega ya legítimamente RUNNING puede finalizar. El fallo de recuperación inicial se comunica mediante el diagnóstico estable `SCAN_RECOVERY_FAILED`, descartando la causa técnica antes de propagar el cierre de arranque. Un reporter específico de Spring Boot y un filtro temporal limitado al mensaje de cancelación de ese contexto evitan los detalles de beans y el stack trace; los fallos de arranque ajenos conservan su diagnóstico normal.

### D-04 — Progreso y modelo mínimo (aprobada)

| Opción | Evaluación |
| --- | --- |
| A: totalTests/completedTests = 0 | Parece compatible con el modelo conceptual, pero 0/0 puede interpretarse como selección vacía o progreso completo. No describe este no-op. |
| B: aplazar ambos | Evita semántica artificial y no acopla el recurso a pruebas inexistentes. Seleccionada para este incremento. |
| C: ambos opcionales/null | Distingue ausencia de datos, pero amplía ya el contrato y sus reglas de nulabilidad sin consumidor real. |

**Decisión B aprobada:** usar estado y fechas como progreso del ciclo de vida; no incluir totalTests/completedTests todavía y añadir ambos en SecurityTest/TestExecution cuando exista un total significativo. El usuario aprueba esta concreción por fases de ADR-003. La actualización de su sección de persistencia queda en las tareas de documentación durante implementación, manteniendo el compromiso del MVP completo. No se trata el código actual como autoridad para invalidar la ADR.

Modelo aprobado para este change:

| Campo | Tipo conceptual | Regla |
| --- | --- | --- |
| id | UUID | Generado por aplicación, inmutable. |
| projectId | UUID | Project existente, inmutable. |
| status | ScanStatus | Cuatro valores aprobados. |
| createdAt | Instant | Asignado al crear, no nulo. |
| startedAt | Instant nullable | Asignado al reclamar RUNNING. |
| finishedAt | Instant nullable | Asignado al confirmar estado terminal. |
| errorReason | ScanFailureReason nullable | Código cerrado; obligatorio solo en FAILED. |

No updatedAt, targetConfigurationId, targetSnapshot, versión de configuración, modo público, progreso numérico o score en este incremento. La asociación a TargetConfiguration es condición de inicio a través de Project y su cardinalidad 0..1; no se altera el modelo ni la tabla de target. No se promete reproducibilidad de una configuración pasada. Antes de trabajo real, el siguiente change deberá decidir snapshot/versionado o lectura vigente y revalidación segura ante PUT concurrente; almacenar solo un id de target no resolvería sus cambios de contenido. Este no-op no consume configuración y por ello no se ve afectado por un PUT posterior al inicio.

Reglas temporales: truncar instantes a microsegundos; startedAt = max(reloj, createdAt), finishedAt = max(reloj, startedAt si existe, createdAt). No se fuerza una duración positiva. SE-05 define la tabla de estados y terminales inmutables.

### D-05 — Contrato REST y polling (aprobada)

| Endpoint aprobado | Respuesta normal | Errores particulares |
| --- | --- | --- |
| POST /api/projects/{projectId}/scans | Sin cuerpo; 202 con Scan PENDING y Location después de commit y admisión. | 404 Project/target, 409 activo, 503 lanzamiento/indisponibilidad, 500 persistencia. |
| GET /api/projects/{projectId}/scans/{scanId} | 200 con siete campos persistidos, Cache-Control: no-store. | 404 Project/Scan, incluido Scan ajeno; 500 lectura. |
| GET /api/projects/{projectId}/scans | 200 con array completo ordenado, Cache-Control: no-store. | 404 Project; 500 lectura. |

El par POST/GET individual es el mínimo para asincronía y polling. **Se aprueba incluir también el listado** porque el histórico está en el alcance MVP y permite localizar un Scan si se pierde la respuesta inicial. Array completo sin filtros/paginación concuerda con la convención inicial de Project y limita el diseño local; el volumen creciente obligará a otro change de paginación, sin truncado oculto. Alternativa considerada: aplazar listado; reduce una operación pero pierde navegación del histórico en este incremento. 202 expresa procesamiento aceptado; 201 sería creación válida, pero describe peor la operación solicitada de iniciar trabajo diferido.

Ejemplo inicial (JSON del contrato futuro, no OpenAPI modificado en esta entrega):

```json
{
  "id": "ee6acb5f-a407-4a1a-8d04-5e9cd6ace45f",
  "projectId": "b33bfe85-a166-4bef-bb36-184a1c27e322",
  "status": "PENDING",
  "createdAt": "2026-10-10T08:00:00Z",
  "startedAt": null,
  "finishedAt": null,
  "errorReason": null
}
```

El body de 202 es la fotografía inicial, no una lectura garantizada del estado en el instante de envío. El worker puede terminar antes de que se entregue 202; no se demora para hacer observables PENDING/RUNNING. Las pruebas usan barreras controladas.

Forma de error heredada, sin refactorizar los handlers de otros módulos: code, message en español y fieldErrors = []. Los códigos y precedencias están en SE-09. SCAN_PROCESSING_FAILED/SCAN_INTERRUPTED son motivos del recurso, no nuevos errores HTTP de GET. SCAN_EXECUTION_UNAVAILABLE significa rechazo antes de crear porque el subsistema está bloqueado. Se mantiene 404 TARGET_NOT_CONFIGURED por consistencia con TC-02, frente a introducir 409 para la misma ausencia.

La presencia de cuerpo en POST se detecta leyendo como máximo un byte del stream de la petición, incluso sin Content-Length. Cualquier byte produce 400 INVALID_REQUEST antes de invocar aplicación, sin materializar el cuerpo ni imponer límites globales a otros endpoints.

Cuando PENDING ya está confirmado y dispatch falla, POST devuelve 503 SCAN_DISPATCH_FAILED con Location tras confirmar FAILED. Si la compensación también falla, devuelve 500 INTERNAL_ERROR con Location y bloquea admisión. Location permite consultar el intento durable incluso con respuesta de error. No se asegura reintento transparente; un nuevo POST crea otro recurso cuando las condiciones lo permitan. Las consultas comprueban pertenencia por (projectId, scanId); conocer UUID ajeno no permite recuperar su Scan. No se diseña autenticación de usuario aquí.

### D-06 — Evolución de scans mediante V3 (aprobada)

Se aprueba crear `V3__ajustar_ciclo_vida_scans.sql` durante la implementación posterior. No se crea SQL ahora, no se ejecuta Flyway y V1/V2 son inmutables.

| Elemento observado | Cambio aprobado |
| --- | --- |
| id UUID PK con default gen_random_uuid() | Conservar. Aplicación asigna UUID explícito. |
| project_id UUID NOT NULL FK projects(id) | Conservar FK y ausencia de cascada. |
| idx_scans_project_id | Conservar para consultas por Project; sin índice adicional de histórico anticipado. |
| status VARCHAR(30) NOT NULL | Conservar tipo; añadir ck_scans_status con los cuatro estados. |
| created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP | Conservar default y concretar precisión TIMESTAMPTZ(6), equivalente a la precisión existente por defecto. Aplicación suministra fecha. |
| started_at/finished_at TIMESTAMPTZ nullable | Conservar nulabilidad y concretar TIMESTAMPTZ(6). |
| error_reason ausente | Añadir VARCHAR(40) nullable y check de códigos cerrados. |
| Sin checks de ciclo de vida | Añadir ck_scans_lifecycle para combinaciones de estado/fechas/error y orden temporal de SE-05. |
| Sin exclusión de activos | Añadir uq_scans_active_project sobre project_id, índice único parcial para PENDING/RUNNING conforme a D-01 aprobada. |
| global_score NUMERIC(5,2) nullable | Conservar sin exponer, mapear al dominio, calcular ni modificar valores existentes. Nuevos scans la dejan null. |

El check de ciclo de vida debe comprobar nulabilidad explícitamente, sin permitir que UNKNOWN de SQL haga pasar combinaciones inválidas. FAILED requiere error_reason no nulo y reconocido; SCAN_DISPATCH_FAILED corresponde a startedAt null. SCAN_PROCESSING_FAILED admite startedAt null si falló preparar/confirmar RUNNING, y no nulo si el trabajo ya comenzó; SCAN_INTERRUPTED admite ambos. Validar las mismas reglas en dominio y base de datos. Las transiciones condicionales actualizan únicamente estado/fechas/error, sin sobrescribir global_score.

V1 admite filas históricas arbitrarias y este trabajo no consulta bases reales: no se presupone que scans esté vacía ni se inventan fechas/motivos históricos. Si al aplicar la migración hay estados, fechas, errores o duplicados activos incompatibles, **la migración debe fallar y conservar los datos**, sin normalizarlos silenciosamente, borrarlos ni modificar V1. Resolver esos datos requerirá una propuesta y autorización específicas. La recuperación de arranque ocurre después de Flyway y no soluciona duplicados que impidan crear el índice. No se diseñan todavía FKs/snapshots de target, columnas de progreso ni tablas adicionales.

### D-07 — Frontera transaccional, casos de uso y unidad temporal (aprobada)

Flujo aprobado, sin cambiar arquitectura:

```text
REST → StartScanUseCase → creación confirmada PENDING → ScanDispatcherPort
                                                     ↓ executor interno
                                               ProcessScanUseCase
                                                     ↓
                                          RUNNING confirmado
                                                     ↓
                                            ScanWorkPort (no-op)
                                                     ↓
                                       COMPLETED/FAILED confirmado

REST → GetScanUseCase / ListProjectScansUseCase → ScanRepositoryPort → PostgreSQL
Arranque → RecoverInterruptedScansUseCase → ScanRepositoryPort → PostgreSQL
```

Responsabilidades:

| Elemento | Responsabilidad |
| --- | --- |
| Domain Scan/ScanStatus/ScanFailureReason | Invariantes y transiciones, sin Spring/JPA/HTTP/executor/AWS. |
| StartScanUseCase | Orquestar creación confirmada, solicitar dispatch y tratar rechazo. No esperar al trabajo. |
| Operación de creación confirmada | Consultar Project/target, generar id/fecha y persistir PENDING dentro de transacción corta. Su decoración infra asegura commit antes de retornar a StartScanUseCase. |
| ProcessScanUseCase | Reclamar PENDING, confirmar RUNNING, invocar trabajo y confirmar terminal; ignorar entrega no reclamable. |
| GetScanUseCase / ListProjectScansUseCase | Comprobar Project, consultar por pertenencia y distinguir ausencia/fallo. |
| RecoverInterruptedScansUseCase | Aplicar la política de reinicio antes de habilitar admisión/procesamiento. |
| ScanRepositoryPort | Inserción, consulta scoped, listado ordenado y transiciones condicionales con resultado propio, sin JPA/SQL en firmas. |
| ScanDispatcherPort | Aceptar o rechazar lanzamiento local con id. |
| ScanWorkPort | Ejecutar unidad sustituible sin controlar estados ni transacciones del ciclo de vida. |
| Composición y adaptadores | Executor/compuerta de disponibilidad, transacciones, JPA, REST y traducción de errores técnicos. |

Se reutiliza ProjectRepositoryPort. Se añadirá únicamente `existsByProjectId(UUID)` al puerto/adaptador de persistencia de TargetConfiguration, mediante proyección de existencia que no seleccione credential_ciphertext ni materialice la entidad. Es una lectura interna necesaria para minimizar datos, no una nueva operación REST ni una revisión de sus invariantes o concurrencia. Alternativa: reutilizar findByProjectId, sin descifrar, pero carga innecesariamente el blob. No se crea un repositorio genérico ni un puerto de preparación del target que duplique su gestión.

**Límites transaccionales y orden:**

1. T1, nueva y corta: comprobar Project/target, construir Scan y persistir PENDING. La operación decorada con TransactionTemplate/REQUIRES_NEW retorna solo tras commit. No se envía 202 ni se llama dispatch si falla insert, flush o commit.
2. Fuera de T1: StartScanUseCase llama ScanDispatcherPort con el UUID confirmado. Se evita el hueco de lectura antes de commit. La eventual existencia de una transacción ambiente externa no contiene T1; el procesamiento no depende de su commit o rollback.
3. El executor acepta o rechaza inmediatamente. Si acepta, se devuelve la fotografía PENDING con 202 sin esperar al worker. Si rechaza, Tfail nueva confirma PENDING → FAILED; la respuesta es la descrita en SE-07.
4. Worker T2 nueva: carga el estado y realiza actualización condicional WHERE id = :id AND status = 'PENDING', con estado/startedAt y resto de invariantes. El resultado positivo solo es usable tras commit. Si otra entrega ganó o ya es terminal, no se invoca trabajo. Si no puede confirmar, tampoco se invoca.
5. Fuera de T2: invocar ScanWorkPort sin transacción de persistencia larga. No compartir sesión/entidad JPA con HTTP ni mantener locks durante trabajo.
6. T3 nueva: RUNNING → COMPLETED, o RUNNING → FAILED con código cerrado ante excepción. Si falla la confirmación de éxito, intentar FAILED en otra transacción; si no se puede persistir, aplicar D-03. Updates condicionales no reabren terminales.

Las transacciones se decoran en config/adaptadores, igual que las operaciones previas, con garantías de commit explícitas en sus contratos internos; no basta anotar un método llamado internamente y asumir que actúa un proxy. No se ejecuta todo StartScanUseCase en una transacción abierta hasta después de dispatch. Las lecturas REST son breves y no necesitan una transacción HTTP compartida.

Alternativas para evitar la carrera: evento transaccional AFTER_COMMIT ofrece la misma ordenación, pero complica comunicar rechazo/compensación antes de responder; outbox persistente permite recuperación del hueco entre commit y envío, pero añade una tabla, dispatcher y políticas de repetición fuera de la necesidad actual. Se adopta retorno explícito después de commit. La caída justo entre T1 y dispatch deja PENDING; D-03 lo marca FAILED en el siguiente arranque. La pérdida de cola o caída entre RUNNING y terminal sigue la misma política. No se afirma atomicidad entre PostgreSQL y memoria del executor.

**Semántica del seam temporal:** COMPLETED solo acredita que terminó la unidad de ciclo de vida. El adaptador no crea SecurityTest, TestExecution ni evidencia; no accede a red/credenciales. Mantener la advertencia en el contrato OpenAPI futuro y en la documentación afectada. Antes de reemplazarlo por análisis real, un change deberá definir prompts, casos, controles SSRF/DNS/redirecciones, selección estable de target, límites efectivos y protección de credenciales/respuestas no confiables. No se diseñan esas soluciones aquí.

## Risks / Trade-offs

- Hueco entre commit y dispatch y cola volátil → Recuperación a FAILED tras reinicio, sin promesa de entrega durable ni relanzamiento automático.
- COMPLETED del no-op interpretado como seguridad comprobada → Semántica explícita en contrato; ningún resultado ni evidencia de pruebas ficticias.
- Base de datos caída impide persistir fallo → Mantener último estado confirmado, bloquear nuevas ejecuciones y recuperar tras reinicio; no ocultar el límite físico.
- Reinicio de una segunda instancia marca trabajo de otra como interrumpido → Diseño soportado solo para instancia única; distribución requiere nuevo diseño antes de desplegar.
- Índice parcial no admite históricos incoherentes → Fallar migración sin alterar datos; resolver excepcionalmente con aprobación y evidencia.
- Listado completo crece y admisión no limita frecuencia total → Executor/cola acotados limitan trabajo simultáneo; paginación y controles de frecuencia/retención requerirán revisión antes de ampliar volumen o exposición.
- Target puede actualizarse tras creación → No-op no lo consume; no se promete snapshot y la ejecución real no podrá heredar implícitamente una configuración mutable.

## Migration Plan

Diseño aprobado; ejecutar este plan solo tras una petición posterior de implementación:

1. Partir de las aprobaciones registradas en este change y reconciliar únicamente ADR-003/decisiones abiertas y documentación directamente afectada, incluida la distinción entre ciclo de vida y motor de seguridad. Sin cambiar Project/TargetConfiguration ni requisitos de entrega.
2. Actualizar el contrato OpenAPI y sección Scan del modelo de datos con lo aprobado.
3. Crear V3 y preparar casos de compatibilidad; no ejecutar migración sin autorización separada.
4. Implementar dominio, operaciones transaccionales, puertos, persistencia, executor acotado/no-op, recuperación y REST conforme a tasks.md.
5. Preparar y ejecutar solo con autorización las pruebas que acrediten commit antes de lanzamiento, exclusión real concurrente, reclamo único, recuperación y errores sanitizados. Validar OpenSpec por separado no acredita funcionamiento de la aplicación.

No se prescribe despliegue ni migración de rollback destructiva. Si se revierte la aplicación, conservar tablas, columnas y datos; no editar V1/V2/migraciones aplicadas ni borrar scans.

## Approval Record

El usuario ha aprobado explícitamente las decisiones de scan-execution con ocho concreciones, incorporadas a D-01 a D-07:

| Decisión | Aprobación aplicada | Requisitos afectados |
| --- | --- | --- |
| D-01 | Un activo por Project, conflicto 409 y garantía DB. | SE-04, SE-07. |
| D-02 | Executor interno de Spring, pool/cola configurables y rechazo 503; instancia única. Los defaults técnicos no son contrato funcional. | SE-03, SE-07, SE-08. |
| D-03 | Reinicio marca activos FAILED; bloqueo de admisión ante persistencia irrecuperable hasta reinicio. | SE-05, SE-07 a SE-09. |
| D-04 | Aplazar totalTests/completedTests, concretando por fases ADR-003. | SE-01, SE-06. |
| D-05 | POST sin cuerpo/202, GET individual y listado completo, códigos y Location también en fallo posterior a creación. | SE-02/SE-03/SE-06/SE-09. |
| D-06 | V3 con checks/error_reason/índice, conservando V1/V2 y global_score sin uso ni transformación automática de históricos. | SE-01, SE-04/SE-05/SE-08. |
| D-07 | Orden transaccional aprobado, puerto no-op y lectura de existencia sin credenciales; delimitar ciclo de vida frente a requisito previo de prompts. | SE-02/SE-03/SE-05/SE-07/SE-10. |

La concreción de trabajo temporal se registra en D-07/SE-10: el no-op se permite únicamente para validar el ciclo de vida; COMPLETED no acredita pruebas de seguridad ejecutadas y no genera findings, scoring ni resultados ficticios. El ajuste de asincronía sustituye la anterior propuesta rígida de un hilo/cola diez por configuración técnica ajustable, sin modificar los invariantes funcionales.

No quedan decisiones bloqueantes para este change. Los asuntos de ejecución real del target y evolución futura siguen fuera de su alcance. READY FOR IMPLEMENTATION significa diseño aprobado y tareas preparadas; no autoriza iniciar código en esta entrega ni el siguiente change.

## Fuera del MVP / Evolución futura

AWS SQS, workers separados/distribuidos, outbox durable, leases, reintentos automáticos/DLQ, CANCELLED/PARTIAL, cancelación, notificaciones push y operación productiva requieren decisiones posteriores. Las pruebas reales, progreso cuantitativo, evaluación, scoring y reports pertenecen al MVP global pero a changes posteriores; no se convierten en exclusiones del producto por aplazarlos aquí.
