Implementación solicitada explícitamente por el usuario sobre D-01 a D-07 aprobadas y sus ocho concreciones. Las casillas de implementación/preparación reflejan archivos presentes y revisión del código y de los escenarios escritos. La validación técnica ha sido autorizada expresamente; 7.3 está completada con la evidencia ejecutada y la confirmación externa del usuario para la revisión inicial. Las correcciones y validaciones posteriores de los findings se registran al final de este archivo. La validación strict previa acredita únicamente OpenSpec.

## 1. Aprobación y contratos afectados

- [x] 1.1 Registrar la aprobación explícita del usuario de D-01 a D-07 en design/proposal/spec, incluido aplazamiento de contadores de ADR-003, frontera anterior a prompts y executor con pool/cola configurables; verificar coherencia con sus ocho concreciones. Completada únicamente la aprobación documental, sin implementación.
- [x] 1.2 Tras 1.1, actualizar únicamente las rutas/esquemas/respuestas de scans de docs/05-api/openapi.yaml con SE-01 a SE-10: POST sin cuerpo, 202/Location, consultas no-store, errores/Location tras creación y advertencia del no-op; verificar por revisión que se conserva el contrato de Project/TargetConfiguration.
- [x] 1.3 Tras 1.1, actualizar sección Scan de docs/06-base-de-datos/modelo-datos.md, ADR-003, apartado asíncrono de decisiones-abiertas y referencias directamente afectadas de arquitectura/casos-de-prueba; verificar siete campos, fases del MVP, recuperación/executor aprobados y exigencia de prompts/controles antes de trabajo real, sin cerrar decisiones ajenas ni modificar la checklist TFM sin impacto real.

## 2. Dominio y contratos internos

- [x] 2.1 Tras 1.1, definir Scan, ScanStatus y ScanFailureReason con identidad inmutable, cuatro estados, motivos cerrados y combinaciones de nulabilidad; preparar pruebas de transiciones permitidas/prohibidas y terminales inmutables y verificar ausencia de Spring/JPA/HTTP/executor/AWS (SE-01/SE-05/SE-07).
- [x] 2.2 Tras 2.1, concretar asignación de instantes UTC a microsegundos y reloj no regresivo; preparar casos para creación, inicio, fallo antes/después de RUNNING, finalización y reloj anterior, verificando orden temporal y ausencia de cambios en lectura (SE-01/SE-05).
- [x] 2.3 Tras 2.1, definir ScanRepositoryPort y operaciones de persistencia confirmada, inserción, consulta por pertenencia, listado y transiciones condicionales con resultados/errores propios; verificar que permiten distinguir ausencia, conflicto, no reclamado y fallo sin exponer JPA, SQL, tipos Spring ni commit prematuro (SE-03 a SE-09).
- [x] 2.4 Tras 2.3, definir ScanDispatcherPort y ScanWorkPort acotados al id/trabajo, sin Future ni tipos de infraestructura, y adaptar lectura de existencia de TargetConfiguration con existsByProjectId; verificar por firmas y prueba/proyección que la comprobación no carga credential_ciphertext ni cambia el contrato público del target (SE-02/SE-03/SE-10).

## 3. Esquema y adaptador PostgreSQL

- [x] 3.1 Tras 1.3/2.3, crear V3__ajustar_ciclo_vida_scans.sql con error_reason, precisión temporal y checks de estados/fechas/motivos; revisar reglas SQL de NULL explícitas y verificar conservación de V1/V2, FK, idx_scans_project_id, global_score y demás tablas, sin ejecutar Flyway (SE-01/SE-05).
- [x] 3.2 Tras 3.1, incorporar uq_scans_active_project conforme a D-01 aprobada, único parcial sobre project_id para PENDING/RUNNING; preparar casos PostgreSQL de conflicto concurrente, estados terminales que liberan Project y proyectos distintos, verificando que no basta un exists previo ni se altera concurrencia de PUT target (SE-04).
- [x] 3.3 Tras 3.1/3.2, preparar fixtures de evolución V1/V2 → V3 con scans compatibles y con estados/fechas/activos incompatibles; verificar que no se transforman ni eliminan históricos silenciosamente y que incompatibilidad hace fallar la migración transaccional. Ejecutar solo con autorización explícita y base dedicada (D-06, SE-05/SE-08).
- [x] 3.4 Tras 2.3/2.4/3.2, implementar entidad JPA separada y adaptador PostgreSQL, existencia de target sin blob, consulta scoped/listado ordenado y updates condicionales; preparar pruebas de mapeo/orden, preservación de global_score, conflicto por constraint concreta y reclamo único sin mezclar fallos técnicos con ausencia (SE-01/SE-02/SE-04 a SE-09).

## 4. Casos de uso y transacciones independientes

- [x] 4.1 Tras 2.x/3.4, implementar operación de creación y decoración transaccional T1 REQUIRES_NEW siguiendo config existente: Project, target, id/createdAt, inserción PENDING y commit antes de retornar; preparar pruebas de precondiciones y fallo insert/flush/commit que verifican cero lanzamientos y ausencia de éxito parcial (SE-02/SE-03/SE-04).
- [x] 4.2 Tras 4.1, implementar StartScanUseCase fuera de T1 para dispatch y compensación nueva en caso de rechazo; preparar casos 202 con fotografía PENDING, 503 con Scan FAILED/Location y 500 con Location si falla compensar, verificando que no espera al trabajo (SE-03/SE-07/SE-09).
- [x] 4.3 Tras 3.4, implementar GetScanUseCase y ListProjectScansUseCase con Project previo y consulta por pertenencia; preparar casos existente, Project/Scan ausente, Scan ajeno, lista vacía/ordenada y fallo de lectura, verificando que no cargan target, no lanzan trabajo ni actualizan fechas (SE-06/SE-09).
- [x] 4.4 Tras 2.2/2.3/3.4, implementar reclamación T2 nueva y ProcessScanUseCase: solo trabajar tras commit RUNNING, ignorar entrega no reclamable y confirmar terminal en T3 independiente; preparar casos de doble entrega, fallo de commit inicial y trabajo fuera de transacciones HTTP/persistencia largas (SE-05/SE-07).
- [x] 4.5 Tras 4.4, implementar traducción de excepción a SCAN_PROCESSING_FAILED, incluyendo preparación fallida desde PENDING, fallo al confirmar COMPLETED y tentativa independiente de FAILED; preparar fallos controlados que verifican último estado confirmado, startedAt null cuando no comenzó y errorReason sin mensajes internos (SE-05/SE-07/SE-09).

## 5. Ejecución local acotada y recuperación

- [x] 5.1 Tras 4.2/4.4, implementar adaptador ScanDispatcherPort con executor interno de Spring dedicado, pool/cola configurables y rechazo inmediato aprobado; declarar/documentar los defaults técnicos iniciales de D-02 en la configuración del backend, validando coherencia y sin fijarlos en el contrato funcional. Preparar pruebas con al menos dos configuraciones y trabajo bloqueado que acrediten configuración efectiva, saturación y rechazo sin CallerRunsPolicy, espera de capacidad, common pool ni ejecución en el hilo HTTP (SE-03/SE-07, D-02).
- [x] 5.2 Tras 4.4, implementar NoOpScanWorkAdapter como unidad temporal sin efectos externos ni pausas; verificar recorrido PENDING → RUNNING → COMPLETED con barreras solo en pruebas y cero conexiones DNS/HTTP, cifrado/descifrado, evaluación o escrituras en test_cases/test_executions/findings (SE-10).
- [x] 5.3 Tras 4.5/5.1, componer bloqueo local de nuevos inicios y reclamaciones si no puede persistirse tampoco FAILED; preparar prueba de fallo persistente que verifica 503 SCAN_EXECUTION_UNAVAILABLE sin nuevas filas y consultas del estado confirmado, sin reintentos automáticos (SE-07/SE-09, D-03).
- [x] 5.4 Tras 3.4/5.3, implementar RecoverInterruptedScansUseCase y su transacción de arranque antes de habilitar admisión/executor; preparar casos mixtos PENDING/RUNNING/terminales, reloj regresivo y fallo de recuperación, verificando FAILED/SCAN_INTERRUPTED, preservación terminal y ausencia de relanzamientos (SE-08).
- [x] 5.5 Tras 5.4, preparar integración de reinicio con una segunda instancia secuencial que conserva el mismo esquema PostgreSQL; verificar recuperación antes de admitir scans, liberación de Project y fallo seguro de arranque sin soporte de instancias concurrentes. No arrancar procesos ni ejecutar migraciones sin autorización (SE-04/SE-08).

## 6. Adaptador REST y errores

- [x] 6.1 Tras 1.2/4.2/4.3/5.4, implementar únicamente los tres endpoints y DTO de siete campos delegando en aplicación, POST sin cuerpo y consultas Cache-Control no-store; preparar pruebas 202/Location/200/listado y cuerpos rechazados, verificando el contrato sin diseñar frontend ni tocar handlers de Project/target (SE-01/SE-03/SE-06/SE-10).
- [x] 6.2 Tras 6.1, implementar validación UUID y mapeo de todos los errores de SE-09 con precedencia y Location cuando existe intento confirmado; preparar casos 400/404/409/500/503, Scan ajeno y recurso FAILED consultable con 200, verificando {code,message,fieldErrors:[]} sin acceso a persistencia ante UUID inválido (SE-02/SE-06/SE-07/SE-09).
- [x] 6.3 Tras 6.2, preparar comprobaciones de privacidad con marcadores sintéticos en excepciones de persistencia/trabajo, sin utilizar credenciales reales; verificar que respuestas/logs/error_reason no incluyen entradas, mensajes técnicos, SQL, configuración del target, claves o blobs y que no se pide clave para iniciar/consultar (SE-02/SE-07/SE-09/SE-10).

## 7. Integración transversal y revisión final

- [x] 7.1 Tras 4.x/5.x/6.x, preparar una prueba PostgreSQL con conexiones independientes que acredite PENDING visible tras commit antes de dispatch, cero dispatch al rollback y un único trabajo ante reclamos concurrentes; usar barreras, no sleeps arbitrarios, y verificar T1/T2/T3 realmente independientes, también con transacción ambiente (SE-03/SE-04/SE-05).
- [x] 7.2 Tras 7.1, revisar trazabilidad SE-01 a SE-10 entre código, casos preparados, OpenAPI, datos y ADR-003; verificar exclusiones, conservación de Project/TargetConfiguration y V1/V2, ausencia de dependencias nuevas y distinción explícita entre no-op y análisis real. Registrar evidencia ejecutada y pendiente sin atribuirle resultados de otros changes.
- [x] 7.3 Tras 7.2, solicitar o utilizar autorización expresa para las comprobaciones concretas de aplicación e integración preparadas y registrar sus comandos/resultados reales; no ejecutar Maven, servidores, PostgreSQL ni migraciones por autorización implícita de OpenSpec, ni marcar garantías verificadas solo por tener tests escritos. Mantener pendientes las comprobaciones no autorizadas.

## Trazabilidad

Los escenarios son los bloques Scenario de cada Requirement de la spec; se enumeran sus nombres abreviados para relacionar los 24 escenarios con diseño y tareas sin duplicar sus criterios de aceptación.

| Requisito | Escenarios de la spec | Diseño | Tareas principales |
| --- | --- | --- | --- |
| SE-01 | Representación inicial; identidad/fechas conservadas al consultar. | D-04/D-06/D-07. | 1.2/1.3, 2.1/2.2, 3.1/3.4, 6.1. |
| SE-02 | Proyecto inexistente; target ausente; target autenticado sin necesitar clave. | D-05/D-07. | 2.4, 3.4, 4.1, 6.2/6.3. |
| SE-03 | Aceptación sin esperar; entrada fuera de contrato; fallo/rollback de creación. | D-02/D-05/D-07. | 4.1/4.2, 5.1, 6.1, 7.1. |
| SE-04 | Solicitudes simultáneas; nuevo Scan después de terminar. | D-01/D-06. | 3.2/3.4, 4.1, 5.5, 7.1. |
| SE-05 | Recorrido completo; entrega duplicada; RUNNING no confirmado. | D-03/D-04/D-06/D-07. | 2.1/2.2, 3.1, 4.4/4.5, 7.1. |
| SE-06 | Consulta aislada por proyecto; histórico vacío/ordenado; fallo de lectura. | D-04/D-05. | 3.4, 4.3, 6.1/6.2. |
| SE-07 | Lanzamiento rechazado; excepción inesperada; base de datos indisponible al finalizar. | D-02/D-03/D-05/D-07. | 4.2/4.4/4.5, 5.1/5.3, 6.2/6.3. |
| SE-08 | Reinicio con estados activos/terminales; fallo de recuperación. | D-03/D-06/D-07. | 3.3, 5.4/5.5. |
| SE-09 | Identificadores mal formados; inicio temporalmente bloqueado. | D-03/D-05. | 4.3/4.5, 5.3, 6.2/6.3. |
| SE-10 | Finalización del trabajo temporal sin análisis real. | D-02/D-07. | 1.2/1.3, 5.2, 6.1/6.3, 7.2. |

## Evidencia de implementación y preparación

Archivos del backend relativos a `backend/src/`; documentación relativa a la raíz. Los tests sin PostgreSQL se han ejecutado correctamente según el registro inferior. El usuario ha comunicado también la ejecución externa correcta de la integración PostgreSQL, la evolución V1/V2 → V3, las restricciones de base de datos y package. La tarea 7.3 queda completada con esta evidencia.

| Requisito | Código presente | Casos preparados |
| --- | --- | --- |
| SE-01 | `Scan.java`, `ScanResponse.java`, `ScanJpaEntity.java` (rutas en el inventario inferior). | `ScanTest`, `ScanPersistenceTest`, `ScanControllerTest`: siete campos, nulabilidad, precisión e identidad. |
| SE-02 | `ScanLifecycleService.create`, `TargetConfigurationRepositoryPort.existsByProjectId`, proyección escalar del adaptador target. | `ScanUseCasesTest`, `ScanPersistenceTest`, `ScanPostgresIntegrationTest`: Project/target ausentes, sin cargar blob, target autenticado sin clave. |
| SE-03 | `StartScanUseCase`, `TransactionalScanLifecycle`, `ScanController`. | `ScanInfrastructureTest`, `ScanControllerTest`, `ScanPostgresIntegrationTest`: fotografía 202, cuerpo prohibido, commit visible desde otra conexión antes de dispatch, rollback ambiente y fallos insert/commit sin lanzamiento. |
| SE-04 | `V3__ajustar_ciclo_vida_scans.sql`, `ScanPersistenceErrors`, `PostgresScanRepositoryAdapter`. | `ScanPostgresIntegrationTest`: dos creaciones pasan exists con barrera y compiten en DB; solo un activo/lanzamiento, terminales liberan Project y proyectos distintos. `ScanInfrastructureTest`: traducción de constraint concreta también al confirmar. |
| SE-05 | `Scan`, `ScanLifecycleService`, `ProcessScanUseCase`, `ScanWorkerEntry`, transiciones PostgreSQL condicionales. | `ScanTest`, `ScanUseCasesTest`, `ScanInfrastructureTest`, `ScanPostgresIntegrationTest`: reloj regresivo, terminales inmutables, reclamos concurrentes, T2/T3 y trabajo fuera de transacción ambiente. |
| SE-06 | `GetScanUseCase`, `ListProjectScansUseCase`, consulta scoped/ordenada y Cache-Control no-store. | `ScanControllerTest`, `ScanPostgresIntegrationTest`: polling, aislamiento por Project, vacío/orden canónico, errores de lectura y fechas conservadas. |
| SE-07 | `StartScanUseCase`, `ProcessScanUseCase`, `ScanExecutionGate`, `SpringScanDispatcherAdapter`. | `ScanUseCasesTest`, `ScanInfrastructureTest`, `ScanExecutorTest`, `ScanPostgresIntegrationTest`: saturación en dos configuraciones, rechazo/compensación con Location, fallos de commit RUNNING/COMPLETED/FAILED y bloqueo sin inventar terminales. |
| SE-08 | `RecoverInterruptedScansUseCase`, `ScanStartup` y dependencia de arranque del executor. | `ScanUseCasesTest`, `ScanInfrastructureTest`, `ScanPostgresIntegrationTest`: arranque cerrado, recuperación mixta y con reloj regresivo, segunda instancia secuencial sin relanzar, fallo de arranque. `ScanMigrationIntegrationTest`: V1/V2 → V3 compatible/incompatible, rollback sin modificar históricos. |
| SE-09 | `ScanExceptionHandler`, validación UUID previa, motivos/códigos cerrados y log operacional solo id/código. | `ScanControllerTest`, `ScanInfrastructureTest`: 400/404/409/500/503, precedencia, Location, FAILED con 200 y marcadores sintéticos de privacidad sin causas ni mensajes internos. |
| SE-10 | `NoOpScanWorkAdapter`, `ScanWorkPort`, advertencias en OpenAPI/ADR-003/arquitectura/casos-de-prueba. | `ScanPostgresIntegrationTest`: no-op hasta COMPLETED, sin clave de cifrado y sin filas en test_cases/test_executions/findings; global_score null en nuevas filas y valores históricos conservados. Código no-op sin operaciones de red/criptografía/evaluación ni pausas. |

Documentación afectada actualizada: `docs/05-api/openapi.yaml`, `docs/06-base-de-datos/modelo-datos.md`, arquitectura general, ADR-003, apartado asíncrono de decisiones-abiertas, modelo de amenazas y frontera previa a prompts en casos-de-prueba. No cambia ningún requisito administrativo de entrega: la checklist TFM no se modifica.

## Inventario inicial de archivos previo al review

32 archivos de producción nuevos (31 Java y V3), 8 clases de tests y 1 fixture compartida. Paquetes Java de producción bajo `backend/src/main/java/com/llmscannerseguridad/`:

| Directorio | Archivos nuevos |
| --- | --- |
| domain/scan | Scan.java, ScanStatus.java, ScanFailureReason.java. |
| application/scan | StartScanUseCase.java, GetScanUseCase.java, ListProjectScansUseCase.java, ProcessScanUseCase.java, RecoverInterruptedScansUseCase.java, ScanLifecycleService.java, ScanLifecyclePort.java, ScanRepositoryPort.java, ScanDispatcherPort.java, ScanWorkPort.java, ScanFailureHandlerPort.java, ScanOperationException.java. |
| adapters/in/rest/scan | ScanController.java, ScanResponse.java, ScanApiError.java, InvalidScanRequestException.java, ScanExceptionHandler.java. |
| adapters/out/persistence/scan | ScanJpaEntity.java, PostgresScanRepositoryAdapter.java, ScanPersistenceErrors.java. |
| adapters/out/execution/scan | SpringScanDispatcherAdapter.java, NoOpScanWorkAdapter.java. |
| config | ScanConfiguration.java, TransactionalScanLifecycle.java, ScanWorkerEntry.java, ScanExecutionGate.java, ScanExecutorSettings.java, ScanStartup.java. |
| backend/src/main/resources/db/migration | V3__ajustar_ciclo_vida_scans.sql (ruta desde la raíz). |

Tests bajo `backend/src/test/java/com/llmscannerseguridad/`: `domain/scan/ScanTest.java`, `application/scan/ScanUseCasesTest.java`, `config/ScanInfrastructureTest.java`, `adapters/out/execution/scan/ScanExecutorTest.java`, `adapters/out/persistence/scan/ScanPersistenceTest.java`, `adapters/in/rest/scan/ScanControllerTest.java`, `integration/ScanPostgresIntegrationTest.java`, `integration/ScanMigrationIntegrationTest.java` y fixture `integration/ScanTestDatabase.java`.

11 archivos existentes modificados: puerto/adaptador target para existencia escalar, `backend/src/main/resources/application.yml`, los siete documentos enumerados arriba y este `tasks.md`. Proposal/design/spec aprobados conservados. V1/V2 y dependencias sin cambios.

## Estado de implementación previo al review

**READY FOR REVIEW: 27/27 tareas completadas, incluida 7.3 con la evidencia de validación registrada.** La aprobación 1.1 se conserva. D-01 a D-07 se aplican sin decisiones bloqueantes nuevas, otro change ni ampliación funcional. V1/V2, pom.xml, lockfiles y despliegue sin cambios. No se realizan commits, pushes ni cambios de rama. Las baterías sin PostgreSQL y de integración pasan según la evidencia registrada; la integración, la migración y package han sido confirmados externamente por el usuario.

La validación strict previa del 2026-10-10 consta como `Change 'scan-execution' is valid`. Durante la fase inicial de implementación solo se consultaron `openspec status` e `instructions apply` con telemetría desactivada, sin ejecutar validaciones técnicas. La fase posterior autorizada se registra a continuación; no se ha repetido strict. No se atribuye a este change evidencia de ejecución de otros changes.

### Validación técnica autorizada del 2026-10-10

Ejecutada desde `backend/` con Temurin Java 21.0.12.1, configurando `JAVA_HOME` únicamente para el proceso de validación.

```powershell
.\mvnw.cmd '-Dtest=ScanTest,ScanUseCasesTest,ScanInfrastructureTest,ScanExecutorTest,ScanPersistenceTest,ScanControllerTest' test
```

Primera ejecución: 30 tests, 0 fallos de aserción, 2 errores, 0 omitidos. Los errores se debían a reconfigurar mocks que ya lanzaban excepciones mediante `when(...)`: se corrigieron únicamente `ScanControllerTest` y `ScanInfrastructureTest` usando `doThrow` y `doReturn`, respectivamente. Sin cambios de producción ni requisitos.

Segunda ejecución completa: **BUILD SUCCESS; 30 tests, 0 fallos, 0 errores, 0 omitidos**. Desglose: ScanTest 3, ScanUseCasesTest 10, ScanInfrastructureTest 5, ScanExecutorTest 2, ScanPersistenceTest 2 y ScanControllerTest 8. Evidencia local: `backend/target/surefire-reports/` para estas seis clases.

En la sesión inicial del agente no se ejecutaron la integración PostgreSQL ni la evolución V1/V2 → V3 porque faltaban `SCAN_TEST_DB_URL`, `SCAN_TEST_DB_USERNAME` y `SCAN_TEST_DB_PASSWORD`. Tampoco se ejecutó package. Este bloqueo de integración quedó resuelto posteriormente mediante la ejecución externa comunicada por el usuario, registrada a continuación.

### Evidencia de validación externa comunicada por el usuario

Resultados facilitados por el usuario, sin nueva ejecución ni verificación independiente por el agente en esta actualización:

| Validación | Tests ejecutados | Fallos | Errores | Omitidos | Resultado |
| --- | --- | --- | --- | --- | --- |
| Sin PostgreSQL | 30 | 0 | 0 | 0 | BUILD SUCCESS |
| Integración PostgreSQL | 14 | 0 | 0 | 0 | BUILD SUCCESS |

La integración se ejecutó contra la base dedicada `llm_scanner_seguridad_test`. El usuario confirma que la evolución V1/V2 → V3 y las restricciones PostgreSQL quedaron validadas.

Comando de integración correspondiente a la validación solicitada previamente:

```powershell
.\mvnw.cmd '-Dtest=ScanPostgresIntegrationTest,ScanMigrationIntegrationTest' test
```

**Package: BUILD SUCCESS**, confirmado externamente por el usuario. Comando correspondiente a la validación solicitada previamente:

```powershell
.\mvnw.cmd -DskipTests package
```

Con la confirmación de package, la tarea 7.3 queda completada: **27/27 tareas; READY FOR REVIEW**.

En esta actualización se modifica únicamente `tasks.md`: sin cambios de código, repetición de tests, ejecución de package ni acceso a PostgreSQL. No se incluyen credenciales en la evidencia.

## Corrección de findings del review

Petición expresa del usuario limitada a F1, F2 y F3 de `scan-execution`, con validación técnica autorizada mediante Java 21. Los resultados anteriores son históricos y no se atribuyen a esta revisión de código. No se cambian requisitos, decisiones D-01 a D-07, modelo Scan, endpoints ni migraciones.

| Finding | Corrección implementada | Cobertura |
| --- | --- | --- |
| F1 — Resultado incierto al confirmar RUNNING | `ProcessScanUseCase` trata el false de la compensación condicional desde PENDING como resultado no resuelto y cierra la compuerta. No ejecuta trabajo ni intenta modificar RUNNING; conserva el estado durable y permite finalizar a entregas ya legítimamente RUNNING. | `ScanUseCasesTest` y `ScanInfrastructureTest`: compensación no aplicable, confirmación perdida tras commit simulado, cero trabajo, conservación del estado y bloqueo. `ScanPostgresIntegrationTest`: commit PostgreSQL real seguido de excepción sintética de confirmación, polling RUNNING, 503 sin nuevas filas, rechazo de reclamaciones y recuperación posterior; ejecución externa correcta confirmada por el usuario (13 tests, sin fallos, errores ni omisiones). F1 VALIDATED. |
| F2 — Materialización arbitraria de body | `ScanController` lee como máximo un byte del ServletInputStream; cualquier byte devuelve 400 INVALID_REQUEST, sin depender de Content-Length ni establecer límites globales. | `ScanControllerTest`: POST sin body, cuerpos pequeños y stream sin Content-Length que falla si se intenta leer un segundo byte, sin grandes reservas de memoria. |
| F3 — Diagnóstico de recuperación durante startup | `ScanStartup` descarta la causa técnica y propaga un fallo controlado sin stack trace. `ScanRecoveryFailureDiagnostics`, registrado en spring.factories, emite únicamente el diagnóstico estable SCAN_RECOVERY_FAILED para ese fallo; un filtro temporal suprime solo la cancelación de contexto que expondría detalles de beans y se elimina al reportar. El backend sigue sin arrancar y los fallos ajenos conservan su logging normal. | `ScanInfrastructureTest`: captura de stdout/stderr del contexto completo de Spring Boot con recuperación fallida, presencia de diagnóstico útil y ausencia de stack traces, SQL, marcadores privados, rutas y detalles de beans. Pruebas adicionales de eliminación del filtro y de fallo de arranque ajeno con su diagnóstico habitual. |

Archivos de esta corrección (rutas desde la raíz):

- Producción modificada: `backend/src/main/java/com/llmscannerseguridad/application/scan/ProcessScanUseCase.java`, `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/ScanController.java`, `backend/src/main/java/com/llmscannerseguridad/config/ScanStartup.java`.
- Producción añadida: `backend/src/main/java/com/llmscannerseguridad/config/ScanRecoveryFailureDiagnostics.java`, `backend/src/main/resources/META-INF/spring.factories`.
- Tests modificados: `ScanUseCasesTest.java`, `ScanInfrastructureTest.java`, `ScanControllerTest.java` y `ScanPostgresIntegrationTest.java` en sus paquetes existentes bajo `backend/src/test/java/com/llmscannerseguridad/`.
- Documentación: precisiones de implementación en `design.md` para D-03 y detección de cuerpo sin alterar su semántica; este `tasks.md`. Spec, OpenAPI, ADRs y V1/V2/V3 no requieren cambios.

### Validación de las correcciones

Ejecución desde `backend/` con Temurin Java 21.0.12.1. Los seis tests unitarios nuevos amplían la batería de 30 a 36; se añade además un escenario de integración PostgreSQL.

| Comprobación | Resultado real |
| --- | --- |
| Primera ejecución de los tests afectados (`ScanUseCasesTest,ScanInfrastructureTest,ScanControllerTest`) | BUILD SUCCESS: 28 tests, 0 fallos, 0 errores, 0 omitidos. Después se añadió la comprobación del logging de fallos ajenos y se ajustaron las fixtures; la batería completa inferior incluye estos cambios finales. |
| Batería completa sin PostgreSQL con el código final | BUILD SUCCESS: 36 tests, 0 fallos, 0 errores, 0 omitidos. ScanTest 3, ScanUseCasesTest 11, ScanInfrastructureTest 9, ScanExecutorTest 2, ScanPersistenceTest 2 y ScanControllerTest 9. Evidencia local en `backend/target/surefire-reports/`. |
| Integración PostgreSQL de esta revisión | Ejecución externa confirmada por el usuario contra la base dedicada `llm_scanner_seguridad_test`: ScanPostgresIntegrationTest, 13 tests, 0 fallos, 0 errores, 0 omitidos; BUILD SUCCESS. F1 VALIDATED. Sin nueva ejecución ni verificación independiente por el agente en esta actualización. |
| Package del código final | `./mvnw.cmd -DskipTests package`: BUILD SUCCESS. Artefacto `backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar`. |

Comandos ejecutados:

```powershell
.\mvnw.cmd '-Dtest=ScanUseCasesTest,ScanInfrastructureTest,ScanControllerTest' test
.\mvnw.cmd '-Dtest=ScanTest,ScanUseCasesTest,ScanInfrastructureTest,ScanExecutorTest,ScanPersistenceTest,ScanControllerTest' test
.\mvnw.cmd -DskipTests package
```

No se repite `ScanMigrationIntegrationTest`: V3 no se modifica y los findings no alteran DDL ni evolución de históricos. La integración de F1 con transacciones reales queda completada mediante la ejecución externa confirmada por el usuario. La fixture de `ScanPostgresIntegrationTest` inicializa V1/V2/V3 mediante Flyway exclusivamente en esquemas aleatorios de la base dedicada, porque estos escenarios lo requieren.

Comando correspondiente a la integración externa confirmada por el usuario:

```powershell
.\mvnw.cmd '-Dtest=ScanPostgresIntegrationTest' test
```

La corrección posterior de compilación se limitó a `ScanPostgresIntegrationTest`: el último parámetro de `ScanLifecycleService` es `Supplier<UUID>`, por lo que se sustituyó la referencia inválida `UUID::randomUUID()` por `UUID::randomUUID`, manteniendo el escenario de F1. La ejecución del agente tras esa corrección dio BUILD SUCCESS con 13 tests omitidos por falta de SCAN_TEST_DB_URL; ese resultado no se utilizó para acreditar integración. La ejecución externa posterior registrada arriba sí confirmó los 13 tests sin omisiones.

**F1 VALIDATED. Estado actual: READY FOR RE-REVIEW.** Las tres correcciones están implementadas; F2 y F3 están comprobadas mediante la batería sin PostgreSQL, y F1 dispone de validación real PostgreSQL confirmada externamente por el usuario. La evidencia histórica de 7.3 se conserva. En esta actualización se modifica únicamente `tasks.md`, sin cambios de código ni nuevas ejecuciones de tests. No se inicia otro change, se añaden dependencias ni se realizan operaciones Git de escritura.
