## Purpose

Permitir iniciar y consultar una ejecución persistente y asíncrona asociada a un proyecto configurado, estableciendo su ciclo de vida sin realizar todavía análisis de seguridad.

## Review Status

Contrato aprobado explícitamente por el usuario con las ocho concreciones registradas en design.md. Estado READY FOR IMPLEMENTATION, sin decisiones funcionales bloqueantes para este incremento. La ejecución de las tareas requiere una petición posterior; esta entrega solo actualiza OpenSpec. El dimensionamiento del executor se gestiona en configuración técnica, sin fijar números de hilos o plazas de cola en los requisitos funcionales.

## ADDED Requirements

### Requirement: SE-01 Identidad, pertenencia y datos de Scan

**Historia:** Como usuario, quiero identificar cada ejecución y su proyecto para consultar su estado e histórico.
**Prioridad propuesta:** Alta, base del MVP.
**Dependencias:** PM-01/PM-04, TC-01; D-04/D-06/D-07.

El sistema SHALL representar Scan mediante exactamente `id`, `projectId`, `status`, `createdAt`, `startedAt`, `finishedAt` y `errorReason`. Ambos identificadores SHALL ser UUID; id SHALL generarse en el sistema y ambos SHALL ser inmutables. Las fechas SHALL ser instantes UTC ISO 8601 con sufijo Z y precisión máxima de microsegundos. startedAt/finishedAt SHALL ser null mientras no haya sucedido el evento correspondiente. errorReason SHALL ser null excepto en FAILED, donde SHALL ser uno de SCAN_DISPATCH_FAILED, SCAN_PROCESSING_FAILED o SCAN_INTERRUPTED. Estos motivos SHALL ser códigos cerrados, nunca texto de excepciones. Este incremento SHALL NOT exponer contadores de pruebas, porcentaje, score ni resultados de seguridad.

#### Scenario: Representación inicial
- **WHEN** se confirma la creación de un Scan
- **THEN** tiene UUID propio, projectId, status PENDING y createdAt generado por el sistema
- **AND** startedAt, finishedAt y errorReason son null y no hay campos de pruebas o resultados

#### Scenario: Identidad y fechas conservadas al consultar
- **WHEN** se consulta o lista un Scan persistido
- **THEN** se devuelven sus siete campos sin modificar identidad, pertenencia o fechas

### Requirement: SE-02 Condiciones previas de inicio

**Historia:** Como usuario, quiero iniciar ejecuciones únicamente sobre proyectos preparados para el flujo posterior.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** PM-04, TC-01/TC-02/TC-05/TC-09; SE-09.

Antes de crear, el sistema SHALL comprobar que Project existe y que dispone de la TargetConfiguration persistida aceptada por target-configuration. Un Project inexistente SHALL producir 404 PROJECT_NOT_FOUND; si existe sin target SHALL producir 404 TARGET_NOT_CONFIGURED. Ninguno SHALL crear Scan ni lanzar trabajo. Un fallo al leer las condiciones previas SHALL producir 500 INTERNAL_ERROR y SHALL NOT interpretarse como ausencia. La comprobación SHALL NOT realizar DNS/HTTP, verificar conectividad, obtener claves o descifrar credenciales. Configuración válida SHALL significar validez local del contrato aprobado, no certificación del destino ni permiso de red.

#### Scenario: Proyecto inexistente
- **WHEN** se solicita iniciar para un UUID válido de Project que no existe
- **THEN** se devuelve 404 PROJECT_NOT_FOUND sin crear Scan ni lanzar trabajo

#### Scenario: Target ausente
- **WHEN** se solicita iniciar para un Project existente sin TargetConfiguration
- **THEN** se devuelve 404 TARGET_NOT_CONFIGURED sin crear Scan ni lanzar trabajo

#### Scenario: Target autenticado sin necesitar clave
- **WHEN** Project tiene una configuración válida con credencial cifrada y no está disponible la clave de cifrado
- **THEN** este incremento permite iniciar el ciclo de vida sin descifrar ni contactar con el target

### Requirement: SE-03 Creación y aceptación asíncrona

**Historia:** Como consumidor, quiero recibir un identificador rápidamente para consultar la ejecución sin esperar al procesamiento.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** SE-01/SE-02/SE-04/SE-05/SE-07/SE-09; D-02/D-05/D-07.

El sistema SHALL aceptar POST `/api/projects/{projectId}/scans` sin cuerpo. Un cuerpo no vacío, incluido `{}` o `null`, SHALL producir 400 INVALID_REQUEST sin crear ni lanzar. El cliente SHALL NOT seleccionar estado, fechas, target, pruebas ni modo de ejecución en esta petición.

Tras confirmar PENDING en persistencia y aceptar el lanzamiento diferido, el sistema SHALL devolver 202, Location `/api/projects/{projectId}/scans/{scanId}` y la representación inicial PENDING de SE-01. La respuesta SHALL NOT esperar a terminar el trabajo ni afirmar que se ha analizado la seguridad. El estado real consultado inmediatamente SHALL poder estar ya en RUNNING, COMPLETED o FAILED; la representación inicial no garantiza un tiempo mínimo en PENDING. Un fallo antes del commit SHALL producir 500 INTERNAL_ERROR sin 202 ni lanzamiento. Este contrato SHALL NOT prometer idempotencia de POST ni entrega exactamente una vez.

#### Scenario: Aceptación sin esperar al trabajo
- **WHEN** se inicia válidamente y el lanzamiento es aceptado mientras el trabajo está detenido por un doble controlado
- **THEN** se obtiene 202, Location y la representación inicial PENDING antes de liberar el trabajo
- **AND** GET permite consultar la evolución persistida del mismo id

#### Scenario: Entrada fuera de contrato
- **WHEN** POST tiene un cuerpo no vacío o intenta aportar estado, configuración o pruebas
- **THEN** se devuelve 400 INVALID_REQUEST sin escrituras ni lanzamiento

#### Scenario: Fallo o rollback de creación
- **WHEN** no se puede confirmar atómicamente la creación
- **THEN** se devuelve 500 INTERNAL_ERROR y no se lanza el trabajo ni queda una creación parcial

### Requirement: SE-04 Un único Scan activo por Project

**Historia:** Como responsable del scanner, quiero evitar ejecuciones solapadas sobre el mismo proyecto y limitar el trabajo admitido.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** SE-02/SE-03/SE-05; D-01 aprobada.

El sistema SHALL considerar activos PENDING y RUNNING. Para un Project con Scan activo, un nuevo inicio SHALL devolver 409 SCAN_ALREADY_ACTIVE sin crear otro Scan ni lanzar otra tarea. La exclusión SHALL mantenerse ante solicitudes concurrentes; una comprobación previa de aplicación no SHALL ser la única garantía. COMPLETED y FAILED SHALL liberar la posibilidad de iniciar un nuevo Scan. Proyectos diferentes SHALL poder tener scans activos, sujetos al límite global de admisión del procesamiento.

#### Scenario: Solicitudes simultáneas del mismo Project
- **WHEN** dos inicios compiten y el Scan ganador permanece activo
- **THEN** solo uno crea un Scan y el otro devuelve 409 SCAN_ALREADY_ACTIVE
- **AND** no se crea una segunda fila ni se lanza una segunda tarea

#### Scenario: Nuevo Scan después de terminar
- **WHEN** el Scan anterior del Project está COMPLETED o FAILED
- **THEN** un nuevo inicio válido puede crear un UUID distinto

### Requirement: SE-05 Estados, transiciones y fechas

**Historia:** Como usuario, quiero estados y fechas coherentes para distinguir una ejecución pendiente, activa y terminada.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** ADR-003, SE-01; D-03/D-06/D-07.

El sistema SHALL admitir únicamente PENDING, RUNNING, COMPLETED y FAILED. Las transiciones SHALL ser PENDING → RUNNING, RUNNING → COMPLETED, PENDING → FAILED y RUNNING → FAILED. Los estados terminales SHALL ser inmutables. PENDING SHALL tener startedAt/finishedAt/errorReason null. RUNNING SHALL tener startedAt no anterior a createdAt y finishedAt/errorReason null. COMPLETED SHALL tener startedAt no anterior a createdAt y finishedAt no anterior a startedAt, con errorReason null. FAILED SHALL tener finishedAt no anterior a createdAt y, si existe startedAt, este SHALL ser no anterior a createdAt y finishedAt no anterior a startedAt, más errorReason no nulo; startedAt SHALL conservarse null si nunca comenzó el trabajo.

Cada transición SHALL persistirse atómicamente solo si sigue vigente su estado de origen; dos entregas del mismo id SHALL NOT ejecutar el trabajo dos veces. El trabajo SHALL comenzar solo tras confirmar RUNNING. Un reloj que retrocede SHALL NOT producir fechas incoherentes: se conservará el máximo entre el instante actual y la última fecha aplicable. Este incremento SHALL NOT admitir CANCELLED/PARTIAL, reapertura, cancelación ni reintentos automáticos.

#### Scenario: Recorrido completo
- **WHEN** una ejecución aceptada empieza y termina correctamente
- **THEN** se confirma RUNNING con startedAt y después COMPLETED con finishedAt
- **AND** se conservan createdAt, identidad y pertenencia, y errorReason sigue null

#### Scenario: Entrega duplicada
- **WHEN** se recibe otra entrega del mismo id ya RUNNING o terminal
- **THEN** no se repite el trabajo ni se alteran los datos terminales

#### Scenario: No se confirma RUNNING
- **WHEN** falla la confirmación de la transición inicial del procesamiento
- **THEN** no se ejecuta el trabajo ni se comunica una finalización exitosa
- **AND** se aplica el tratamiento de persistencia fallida de SE-07

### Requirement: SE-06 Consulta individual e histórico para polling

**Historia:** Como usuario, quiero consultar el estado y recuperar el histórico de ejecuciones de mi proyecto.
**Prioridad propuesta:** Alta para consulta individual; Media para histórico MVP.
**Dependencias:** PM-04, SE-01/SE-09; D-05 aprobada.

GET `/api/projects/{projectId}/scans/{scanId}` SHALL devolver 200 y la representación persistida. Un Project inexistente SHALL producir 404 PROJECT_NOT_FOUND. Un Scan inexistente o perteneciente a otro Project SHALL producir 404 SCAN_NOT_FOUND sin revelar el otro proyecto. Consultar un Scan SHALL NOT requerir volver a comprobar o recuperar la configuración actual del target.

GET `/api/projects/{projectId}/scans` SHALL devolver 200 y el array completo de scans del proyecto, sin paginación ni filtros en este MVP local, ordenado por createdAt descendente e id canónico ascendente para empates. Para Project existente sin scans SHALL devolver `[]`; para Project inexistente, 404 PROJECT_NOT_FOUND. Ambas consultas SHALL reflejar datos confirmados, SHALL NOT lanzar ni reiniciar trabajo y SHALL NOT transformar fallos de almacenamiento en ausencia, éxito o array vacío. Las respuestas exitosas SHALL incluir Cache-Control: no-store para permitir polling sin respuestas cacheadas; no se define frecuencia ni UX de frontend.

#### Scenario: Consulta aislada por proyecto
- **WHEN** se consulta bajo un Project existente el id de un Scan de otro Project
- **THEN** se devuelve 404 SCAN_NOT_FOUND sin datos del otro proyecto

#### Scenario: Histórico vacío u ordenado
- **WHEN** se consulta el histórico de un Project existente
- **THEN** se obtiene `[]` si no tiene scans o todos sus scans una sola vez en el orden definido
- **AND** no aparecen scans de otros proyectos ni se modifican estados o fechas

#### Scenario: Fallo de lectura
- **WHEN** falla la persistencia al consultar un Scan o el histórico
- **THEN** se devuelve 500 INTERNAL_ERROR sin simular 404 ni array vacío

### Requirement: SE-07 Fallos de lanzamiento y procesamiento

**Historia:** Como usuario, quiero distinguir un fallo de infraestructura del éxito y disponer de un estado persistido interpretable.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** SE-03/SE-05/SE-09; D-02/D-03/D-07 aprobadas.

Si el lanzamiento se rechaza después de confirmar PENDING, el sistema SHALL intentar confirmar PENDING → FAILED con SCAN_DISPATCH_FAILED en una operación independiente antes de devolver 503 SCAN_DISPATCH_FAILED. La respuesta SHALL incluir Location del Scan ya creado; un GET posterior SHALL permitir observar FAILED si se confirmó la actualización. La capacidad de trabajo pendiente SHALL ser finita y su saturación SHALL rechazar inmediatamente, sin ejecutar el procesamiento en el hilo HTTP.

Una excepción ordinaria del trabajo, prevista o inesperada, después de RUNNING SHALL terminar en FAILED con SCAN_PROCESSING_FAILED, conservando startedAt y asignando finishedAt. Si falla preparar/confirmar RUNNING y una operación nueva permite registrar el fallo, SHALL usarse el mismo motivo desde PENDING, con startedAt null y sin ejecutar trabajo. Si falla confirmar COMPLETED, SHALL intentarse registrar FAILED con ese motivo desde el último estado confirmado. El POST ya aceptado SHALL NOT cambiar retrospectivamente su respuesta; GET devuelve 200 con el recurso FAILED. El sistema SHALL NOT persistir el mensaje de la excepción como errorReason.

Si la persistencia impide confirmar RUNNING o el estado terminal, el sistema SHALL NOT afirmar un estado no confirmado ni continuar trabajo que no pudo reclamar. Si falla además el registro de FAILED, SHALL conservarse el último estado confirmado; el proceso SHALL bloquear nuevos inicios y nuevas reclamaciones hasta un reinicio con recuperación correcta, sin reintentos automáticos. Las consultas disponibles SHALL seguir devolviendo el estado persistido, o 500 INTERNAL_ERROR si tampoco pueden leerlo. Un fallo de compensación de lanzamiento SHALL devolver 500 INTERNAL_ERROR con Location del Scan creado. La recuperación de SE-08 SHALL resolver estos scans tras reiniciar; no se promete actualización de PostgreSQL mientras está indisponible.

#### Scenario: Executor rechaza el lanzamiento
- **WHEN** el lanzamiento no se acepta y PostgreSQL permite registrar el fallo
- **THEN** POST devuelve 503 SCAN_DISPATCH_FAILED y Location
- **AND** el Scan está FAILED, con startedAt null, finishedAt asignado y errorReason SCAN_DISPATCH_FAILED

#### Scenario: Excepción inesperada del trabajo
- **WHEN** el trabajo de un Scan RUNNING lanza una excepción con datos internos
- **THEN** GET devuelve 200 con FAILED y errorReason SCAN_PROCESSING_FAILED
- **AND** respuesta, error persistido y logs no contienen esos datos

#### Scenario: Base de datos indisponible al finalizar
- **WHEN** fallan la escritura terminal y su registro de fallo
- **THEN** no se declara COMPLETED ni FAILED sin commit, se conserva el último estado confirmado y se bloquean nuevos inicios/reclamaciones
- **AND** tras reinicio se aplica SE-08 sin volver a ejecutar el trabajo

### Requirement: SE-08 Recuperación simple tras reinicio

**Historia:** Como usuario, quiero identificar ejecuciones interrumpidas y poder iniciar una nueva tras reiniciar.
**Prioridad propuesta:** Alta, MVP local de una única instancia.
**Dependencias:** SE-05/SE-07; D-03 aprobada.

Antes de aceptar nuevos inicios o procesar tareas en un arranque, el sistema SHALL confirmar la transición de todos los scans persistidos PENDING o RUNNING a FAILED con SCAN_INTERRUPTED y finishedAt coherente. SHALL conservar startedAt si ya existía y null si nunca empezó. COMPLETED y FAILED SHALL permanecer intactos. No SHALL relanzarse ninguno automáticamente. Si la recuperación no puede confirmarse, la instancia SHALL NOT aceptar scans ni arrancar su procesamiento hasta un nuevo arranque correcto; no SHALL anunciar que recuperó los estados. Esta política SHALL limitarse a una sola instancia backend y SHALL NOT aplicarse a un despliegue concurrente de múltiples instancias.

#### Scenario: Reinicio con estados activos y terminales
- **WHEN** se inicia una nueva instancia después de detener la anterior con PENDING, RUNNING, COMPLETED y FAILED persistidos
- **THEN** los dos activos quedan FAILED con SCAN_INTERRUPTED antes de admitir nuevos inicios
- **AND** los terminales y sus fechas se conservan sin relanzar trabajo

#### Scenario: Fallo de recuperación
- **WHEN** no se puede confirmar la recuperación al arrancar
- **THEN** no se habilitan inicio ni procesamiento de scans y no se afirma haberlos recuperado

### Requirement: SE-09 Errores HTTP sanitizados

**Historia:** Como consumidor, quiero códigos estables sin recibir datos sensibles o detalles técnicos.
**Prioridad propuesta:** Alta, MVP.
**Dependencias:** PM-07, TC-12 y SE-02 a SE-08; D-05/D-07.

Los errores SHALL conservar `{code,message,fieldErrors}` con mensajes en español y fieldErrors vacío. Identificadores de ruta inválidos SHALL producir 400 INVALID_PROJECT_ID o INVALID_SCAN_ID antes de persistencia; si ambos son inválidos SHALL tener precedencia INVALID_PROJECT_ID. Con identificadores válidos SHALL comprobarse Project antes de buscar Scan. Los demás códigos SHALL ser INVALID_REQUEST (400), PROJECT_NOT_FOUND/TARGET_NOT_CONFIGURED/SCAN_NOT_FOUND (404), SCAN_ALREADY_ACTIVE (409), SCAN_DISPATCH_FAILED (503), SCAN_EXECUTION_UNAVAILABLE (503, inicio bloqueado por infraestructura no recuperada) e INTERNAL_ERROR (500).

Errores HTTP, representaciones, errorReason y logs SHALL omitir cuerpos/configuración del target, credenciales, ciphertext, claves, entradas rechazadas, SQL, detalles de conexión y stack traces o mensajes internos. Un recurso FAILED SHALL ser consultable con 200; el fallo del procesamiento SHALL NOT convertirse en 500 al leer correctamente el recurso ni en un finding.

#### Scenario: Identificadores mal formados
- **WHEN** projectId o scanId no tiene formato UUID válido
- **THEN** se devuelve el código 400 correspondiente sin consultar persistencia

#### Scenario: Inicio temporalmente bloqueado
- **WHEN** se intenta iniciar después de un fallo no recuperado de infraestructura
- **THEN** se devuelve 503 SCAN_EXECUTION_UNAVAILABLE sin crear un nuevo Scan

### Requirement: SE-10 Trabajo temporal sin análisis de seguridad

**Historia:** Como responsable del proyecto, quiero validar el ciclo de vida sin presentar un no-op como análisis de seguridad real.
**Prioridad propuesta:** Alta, límite de este incremento.
**Dependencias:** SE-03/SE-05; D-07 y trabajo temporal aprobados explícitamente por el usuario.

En este incremento, el trabajo de Scan SHALL ser una unidad temporal sin operaciones externas que retorna correctamente después de reclamar RUNNING. SHALL permitir confirmar COMPLETED, que significa exclusivamente finalización del trabajo de ciclo de vida. SHALL NOT crear pruebas, TestExecution, Findings, veredictos, porcentajes, score, reportes, peticiones al target, llamadas al evaluador ni registros de evidencias ficticias. SHALL NOT introducir pausas artificiales para aparentar progreso. El contrato público documentado SHALL indicar esta semántica limitada. La sustitución por análisis real SHALL requerir su propio change aprobado con controles de ejecución del target; un no-op completado SHALL NOT acreditarse como análisis seguro o cobertura OWASP.

#### Scenario: Finalización del trabajo temporal
- **WHEN** se completa la unidad temporal de un Scan aceptado
- **THEN** el mismo Scan puede consultarse en COMPLETED con fechas coherentes
- **AND** no hubo DNS/HTTP, acceso criptográfico, evaluación ni escrituras en tablas de pruebas, ejecuciones o findings
