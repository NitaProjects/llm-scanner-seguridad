## Context

El API de proyectos ofrece POST, GET por UUID y GET de listado en `ProjectController`. No hay edición de Project expuesta en el código revisado. `ProjectRequestMapper` y `ProjectExceptionHandler` fijan la validación del ID y el formato de errores. `ProjectJpaEntity` contiene solo sus campos propios. TargetConfiguration, Scan y TestExecution tienen sus propios mapeos JPA. Evaluación, Findings, scoring y reporting usan repositorios SQL. Se revisaron las migraciones oficiales V1–V8 y estos mapeos, sin consultar una base activa.

## Goals / Non-Goals

Eliminar el agregado persistido en una operación explícita, mantener el aislamiento entre proyectos y aplicar el modelo de acceso existente. El alcance operativo de archivos, copias y servicios externos está definido en la propuesta.

## Decisions

### D-01 Grafo verificado

Las flechas de esta tabla apuntan a las tablas referenciadas. Las FK de este grafo usan el comportamiento por defecto de PostgreSQL, NO ACTION. Las migraciones revisadas no declaran ON DELETE CASCADE en estas relaciones ni tablas dependientes adicionales sin FK.

| Entidad o datos | Tabla | FK y propiedad |
|---|---|---|
| Project | `projects` | Raíz |
| TargetConfiguration y credencial cifrada | `target_configurations` | project_id → projects, única por proyecto. Configuración y ciphertext están en la misma fila. V2/V8 |
| Scan y marcadores de perfiles | `scans` | project_id → projects. El marcador histórico `global_score` es un campo de la fila. V1/V3/V5–V7 |
| TestExecution y evidencia de transporte | `test_executions` | scan_id → scans, target_configuration_id → target_configurations, test_case_id → test_cases. Incluye request_payload, response_payload, flags y campos históricos de evaluación. V1/V4 |
| EvaluationResult y evidencia de evaluación | `evaluation_results` | scan_id → scans, test_case_id → test_cases y FK compuesta (test_execution_id, scan_id, test_case_id) → test_executions. Snapshot, señales, referencias internas, assessment y proyección/hash viven en la fila. V5 |
| Finding | `findings` | FK directas a scans, test_cases, test_executions y evaluation_results, además de FK compuesta a la evaluación para identidad, categoría, veredicto, severidad y confianza. Incluye su resumen de evidencia. V1/V5 |
| ScanScore | `scan_scores` | scan_id → scans. Desglose y contadores en la misma fila, sin tabla hija por categoría. V6 |
| SecurityReport report-v1 | `security_reports` | scan_id → scans y (scan_id, scoring_version) → scan_scores. El payload es un snapshot JSONB en esta fila, sin FK individuales a los Findings/Evaluations reproducidos en él. V7 |
| SecurityTest | `test_cases` | Catálogo global sin project_id. Las referencias vienen de ejecuciones, evaluaciones y hallazgos. Se conserva completo |

Comprobar el grafo también mediante `pg_catalog` en la futura prueba PostgreSQL con todas las migraciones vigentes. `docs/06-base-de-datos/modelo-datos.md` documenta estos datos y `ADR-008` reserva el borrado para un change posterior. La inmutabilidad de report-v1 se aplica a los snapshots que existen. El DELETE elimina el snapshot completo como parte del agregado y los informes de otros proyectos conservan su contenido.

### D-02 Estrategia explícita y ámbito

Crear `DeleteProjectUseCase` con la persistencia y transacción en adaptadores/configuración, siguiendo la separación de Project. Un único adaptador de borrado ejecuta SQL parametrizado por projectId. Usar el gestor transaccional existente y la misma base/conexión transaccional. Mantener SQL y decisiones de bloqueo fuera del dominio. El adaptador de proyectos aporta el bloqueo de raíz a las escrituras que lo necesitan.

Orden de sentencias dentro de la transacción:

1. `security_reports`
2. `scan_scores`
3. `findings`
4. `evaluation_results`
5. `test_executions`
6. `scans`
7. `target_configurations`
8. `projects`

Las cinco tablas de resultados con scan_id se acotan mediante scans.project_id, mientras los scans todavía existen. Scans y target se acotan por project_id. El DELETE final exige exactamente una fila. Un target de A referenciado por una ejecución de B es representable con las FK independientes actuales. En ese caso se deja que la FK bloquee la sentencia de target y se revierte el conjunto. No se amplía el predicado para borrar filas de B. Las consultas de control leen IDs y estados, sin deserializar reports, abrir evidencia ni descifrar credenciales.

Cascada JPA exigiría asociaciones y lifecycle nuevos y no cubriría por sí sola los repositorios SQL. ON DELETE CASCADE requeriría migraciones y haría que otras escrituras pudieran activar el borrado. Una combinación añadiría dos mecanismos. La secuencia explícita cubre datos V1–V8 y conserva las FK como última defensa. La estrategia no requiere migración, índices adicionales ni materializar entidades o listas de IDs del historial.

### D-03 Transacción y concurrencia

DELETE toma `SELECT … FOR UPDATE` de `projects` por UUID en una transacción READ COMMITTED. Tras obtenerlo ejecuta una consulta separada para comprobar `scans.status IN ('PENDING','RUNNING')`, de modo que vea una admisión confirmada mientras esperaba. Si hay actividad, responde conflicto y no escribe. Si el proyecto ya no existe, responde 404. Ejecutar los ocho borrados en la misma transacción y enviar 204 después de commit. Traducir excepciones fuera de la transacción, incluyendo fallos de flush o commit. Ante pérdida de confirmación, el cliente recarga el listado. Una transacción comprometida elimina todo el agregado y una abortada conserva todo.

La admisión de Scan en T1, y POST/PUT de TargetConfiguration dentro de sus transacciones vigentes, toman `FOR KEY SHARE` de la misma fila antes de leer o escribir datos dependientes. Mantienen el bloqueo hasta commit. Es compatible entre esas escrituras y excluyente frente al `FOR UPDATE` del DELETE. El orden es Project primero y filas dependientes después. Mantener el índice de scan activo y el UPDATE condicional de tipo del target. La admisión sigue despachando solo después de confirmar. Proponer un `lock_timeout` local de 5 s para DELETE, acotando las esperas por contención sin cambiar la configuración global de PostgreSQL. No limita por sí solo la duración de un borrado grande. Un timeout, deadlock o fallo interno revierte y usa INTERNAL_ERROR, sin falsificar un conflicto de actividad.

| Carrera | Resultado |
|---|---|
| Dos DELETE | El ganador confirma 204. La segunda transacción, tras adquirir el bloqueo o comprobar que desapareció la fila, devuelve 404. Si el primero revierte, el segundo puede eliminar |
| DELETE y admisión | Si T1 confirma primero, el DELETE ve PENDING o RUNNING y devuelve 409. Si DELETE confirma primero, T1 ve PROJECT_NOT_FOUND y no despacha |
| DELETE y POST/PUT del target | Si la escritura confirma primero, el DELETE incluye el target resultante. Si DELETE confirma primero, la escritura ve PROJECT_NOT_FOUND |
| Evaluación, Finding, score o informe en curso | PENDING/RUNNING se mantienen activos hasta el cierre confirmado. El DELETE verifica actividad sin bloquear primero filas de resultado. La evaluación y el Finding confirman juntos. Score, report y transición terminal confirman juntos |
| DELETE tras cierre | Un COMPLETED o FAILED confirmado permite borrar el conjunto, incluso hijos parciales dejados por un scan fallido o históricos sin score/report. Si el cierre aún no es visible, 409 y nueva acción explícita |
| GET/list durante DELETE | MVCC permite ver la fila previa mientras no confirme. Una nueva lectura iniciada tras 204 ve 404 en detalle y lista sin ese proyecto. No se promete invalidar respuestas antiguas ya emitidas |

Los estados terminales no se reabren. Claim solo avanza PENDING → RUNNING, ambos cubiertos por la barrera, por lo que no necesita bloquear Project. Los escritores del worker siguen la disciplina actual de Scan activo, con finalización y compensación antes del estado terminal visible. Verificar esas precondiciones con barreras de prueba y mantener rollback si aparece una FK inesperada. No se espera a que termine la red dentro del DELETE y no se modifica la admisión global ni el ciclo de vida.

### D-04 Estado efímero y propiedad del alcance

El executor de scans encola scanId localmente. PENDING cubre el intervalo entre commit y despacho y la espera en cola. RUNNING cubre el plan y snapshots en memoria, la red, InteractionSession, variables de workflow, handles de cookies/headers y observaciones tipadas. Su descarte sigue el cierre del scope de ejecución. Una entrega tardía de un scan ya ausente acaba con claim vacío, sin trabajo. La recuperación actual gestiona interrupciones. DELETE no cancela trabajo, cambia sus estados, reinicia ejecutores ni toca la compuerta global.

`EvaluationEvidenceProjector` mantiene un corpus transitorio por evaluationId, liberado en los caminos vigentes de commit/aborto. Antes de borrar, intersectar los IDs del corpus actualmente en memoria con las evaluaciones del proyecto mediante consultas acotadas y sin leer payloads. Registrar solo esas coincidencias para limpieza tras commit mediante un puerto efímero que reutiliza `release(id)`. Así la cantidad de IDs retenidos para limpiar depende del corpus en memoria y el rollback no invalida entradas. La limpieza no ejecuta red y no puede convertir un commit durable en error de borrado. El HTTP y los locks de cuota son compartidos. No se encontró un job durable, sesión, workflow o almacén de evidence con tabla propia para el proyecto en V1–V8.

Los manifiestos de escenarios e integraciones son archivos de operador, vinculados por IDs y configuración, ajenos al almacenamiento gestionado por Project. Sus bindings dejan de ser aplicables al desaparecer las identidades. Los logs, backups y recursos creados en targets remotos no forman parte del DELETE.

### D-05 API y acceso

`DELETE /api/projects/{projectId}` sin body. Usar validación UUID de ProjectRequestMapper, formato de `ProjectApiError` con `code`, `message` en español y `fieldErrors=[]`, y `Cache-Control: no-store`. El éxito no tiene contenido.

| HTTP | Código | Condición |
|---|---|---|
| 204 | — | Commit del borrado completo |
| 400 | INVALID_PROJECT_ID | UUID inválido |
| 404 | PROJECT_NOT_FOUND | Proyecto ausente, también al repetir después de borrar |
| 409 | SCAN_ALREADY_ACTIVE | Proyecto con PENDING o RUNNING |
| 409 | PROJECT_DELETE_CONFLICT | FK de borrado impide eliminar el conjunto. SQLSTATE 23503 atribuible al borrado, tras rollback |
| 500 | INTERNAL_ERROR | Otro fallo de almacenamiento, timeout, deadlock o commit sin confirmación |

La repetición es idempotente en efecto. 404 coincide con la semántica GET existente y permite distinguir una ausencia previa. La API no devuelve contadores, nombres de constraints, SQL, credenciales o IDs de terceros. Los conflictos previstos conservan su código fuera del catch genérico de Project.

El backend revisado carece de usuarios, roles y propiedad por Project. Usa el perímetro local/restringido existente. En el perfil servido por Nginx se conserva Basic Auth de entrada y su 401 si falta una credencial válida. Esa protección es global. El alcance de DELETE corresponde al de los operadores que ya pueden administrar el recurso. Si el perímetro existente rechaza acceso mantiene su respuesta, sin revelar existencia. No hay 403 de negocio por proyecto mientras carezca de identidad y ACL. El UUID, CORS y la autorización del target no sustituyen los controles de acceso. La capacidad se diseña para el entorno de operador confiable actual.

### D-06 Frontend y confirmación

Añadir «Eliminar proyecto» a cada tarjeta de `ProjectsPage`. Reutilizar `ProjectsApi` y el mapeo de errores con una operación DELETE que comprueba 204 sin decodificar un DTO. Usar diálogo accesible con nombre interpolado como texto, foco inicial en Cancelar, vuelta de foco al salir y botón explícito «Eliminar proyecto». Texto propuesto: «¿Eliminar el proyecto “X”? Se eliminarán también su target, scans, hallazgos e informes. Esta acción no se puede deshacer.» Una confirmación modal simple es proporcionada para un proyecto cada vez en el entorno de operador.

Mientras se envía, deshabilitar duplicados y acciones de confirmación/cancelación del diálogo. Tras 204 cerrar, retirar por ID de la lista y anunciar éxito. Invalidar una lectura en vuelo que pueda reintroducir la fila retirada. En 409 conservar el elemento y mostrar el motivo. En 404 reconciliar con una lectura fresca y mostrar «El proyecto ya no existe». En fallo ambiguo consultar antes de permitir otro intento. No repetir DELETE automáticamente. Tras retirar la tarjeta, mover foco al siguiente elemento o al encabezado/lista vacía.

## Risks / Trade-offs

- P1 potencial: borrar recursos fuera del proyecto. Predicados por project_id, FK intactas y prueba de referencia cruzada con rollback.
- P1 potencial: borrar durante admisión o ejecución. Bloqueo de raíz en la misma transacción y compuerta durable PENDING/RUNNING comprobada antes de escribir.
- P2: una operación destructiva amplía lo que permite el perímetro de operador. Confirmación explícita y contrato de acceso documentado. La separación multiusuario requeriría autorización por recurso.
- P2: locks largos o pérdida de confirmación con historial voluminoso. SQL por conjuntos, espera acotada y reconciliación sin reintentar automáticamente.
- P3: manifiestos, copias y recursos remotos conservan su política propia. Documentar el alcance del borrado.

## Migration Plan

Implementación prospectiva sobre V1–V8 con los lectores vigentes. Desactivar la opción de borrado retirando el endpoint/acción en una versión posterior no restaura registros borrados. No requiere migración.

## Open Questions

Sin dependencias de diseño pendientes para el entorno de operador actual. La aprobación del change y la ejecución futura de pruebas PostgreSQL se rigen por el flujo y permisos del repositorio.
