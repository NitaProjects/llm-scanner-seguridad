La implementación de las tareas siguientes ha sido autorizada explícitamente por el usuario conforme a las decisiones aprobadas en `design.md`, incluida la excepción limitada de V1. Las comprobaciones que ejecuten tests, Maven, servidores, PostgreSQL o migraciones requieren autorización expresa adicional conforme a `AGENTS.md`; preparar casos de prueba no autoriza ejecutarlos. No se realizan cambios en frontend ni demo-vulnerable. Las casillas completadas acreditan código, pruebas preparadas y revisión documental, no resultados de pruebas ejecutadas.

## 1. Contratos y documentación directamente afectada

- [x] 1.1 Documentar en `docs/05-api/openapi.yaml` las tres rutas, los cinco campos de respuesta, el cuerpo de creación, `Location`, el listado y los errores; verificar por revisión que cada entrada, salida y escenario HTTP de PM-01 a PM-07 tiene representación en el contrato.
- [x] 1.2 Actualizar únicamente la sección de `Project` de `docs/06-base-de-datos/modelo-datos.md` con el modelo y las restricciones aprobadas, incluida la igualdad inicial de fechas; verificar que coincide con PM-01, PM-02 y PM-06 sin redefinir relaciones o entidades futuras.

## 2. Dominio y puerto de persistencia

- [x] 2.1 Definir `Project` y sus invariantes, normalización, límites Unicode y descripción opcional, con casos de prueba de límites y valores inválidos; verificar en los artefactos que PM-01 y PM-02 están cubiertos y que el dominio no importa Spring, Jakarta Validation, JPA, HTTP o PostgreSQL.
- [x] 2.2 Definir `ProjectRepositoryPort` en aplicación para inserción, búsqueda opcional por UUID y listado ordenado, junto con errores propios para distinguir ausencia y fallo técnico; verificar las firmas y su correspondencia con PM-03 a PM-07 sin filtrar entidades JPA, tipos Spring Data o excepciones SQL.

## 3. Casos de uso

- [x] 3.1 Implementar `CreateProjectUseCase` usando las invariantes del dominio, identidad generada, un único instante para ambas fechas y el puerto de inserción; verificar PM-01, PM-02 y PM-03 mediante casos de prueba que contemplen valores normalizados y nombres repetidos con identificadores distintos.
- [x] 3.2 Implementar `GetProjectUseCase` con búsqueda por identificador y ausencia diferenciada de error de persistencia; verificar PM-04, PM-06 y PM-07 mediante casos de prueba de proyecto existente, inexistente y fallo del puerto.
- [x] 3.3 Implementar `ListProjectsUseCase` con el orden de PM-05; verificar mediante casos de prueba lista vacía, varios proyectos, empate temporal y propagación controlada de fallo de lectura, sin modificar las fechas de PM-01.

## 4. Adaptador PostgreSQL y composición

- [x] 4.1 Alinear, solo durante la implementación autorizada, la definición de `projects` en `backend/src/main/resources/db/migration/V1__crear_tablas_iniciales.sql` conforme a la excepción aprobada en la sección 5.1 del diseño: UUID, `name VARCHAR(120) NOT NULL`, `description VARCHAR(2000)` nullable, `created_at` y `updated_at` no nulos, sin `target_url` ni `app_type`, con las restricciones defensivas previstas; verificar por revisión del cambio la correspondencia con PM-01, PM-02 y PM-06 y que se conservan la FK `scans.project_id → projects.id`, `idx_scans_project_id` y el resto de V1, sin crear V2 ni ejecutar migraciones. Registrar que, desde su uso como baseline real, V1 será inmutable y las siguientes modificaciones usarán nuevas migraciones Flyway.
- [x] 4.2 Implementar el modelo JPA separado, su mapeo y el adaptador de `ProjectRepositoryPort` usando las dependencias existentes; verificar correspondencia de los cinco campos, UUID, nulabilidad, precisión temporal y orden `createdAt` descendente/`id` ascendente de PM-01 y PM-03 a PM-06.
- [x] 4.3 Conectar los casos de uso, el puerto y el adaptador desde la composición Spring y establecer la frontera transaccional de creación; verificar que la confirmación precede a la respuesta exitosa, que las lecturas no actualizan fechas y que no se introducen anotaciones de infraestructura en dominio o contratos de aplicación (PM-01 y PM-06).
- [x] 4.4 Preparar pruebas de integración del adaptador para creación/lectura, nombres repetidos, descripción nula, empate temporal, conservación de fechas y fallo antes de commit; verificar que cubren PM-01, PM-03, PM-05 y PM-06 sobre PostgreSQL y pueden ejecutarse posteriormente con autorización, sin añadir dependencias o infraestructura de despliegue.

## 5. Adaptador REST y errores

- [x] 5.1 Implementar DTOs y conversión de entrada/salida, rechazo de propiedades desconocidas o generadas y validación estricta del formato UUID; verificar PM-01, PM-02 y PM-04 mediante casos de prueba de JSON mal formado, raíz no válida, campos ausentes, nulos, tipos incorrectos y UUID inválido sin acceso a persistencia.
- [x] 5.2 Exponer únicamente `POST /api/projects`, `GET /api/projects/{id}` y `GET /api/projects`, delegando en los tres casos de uso; verificar los códigos `201`/`200`, `Location`, las cinco propiedades y el array vacío/ordenado de PM-03 a PM-05 contra el contrato OpenAPI.
- [x] 5.3 Implementar el mapeo de los cinco códigos de error y los errores de campo de PM-07; verificar con casos de prueba las respuestas `400`, `404` y `500`, la estructura `code`/`message`/`fieldErrors`, los mensajes en español y la ausencia de valores rechazados, stack traces, SQL y datos de conexión.

## 6. Verificación transversal y cierre de implementación

- [x] 6.1 Preparar pruebas del recorrido REST/aplicación/persistencia para creación seguida de consulta/listado, límites Unicode, nombres repetidos, inexistencia, orden y fallos de almacenamiento; verificar trazabilidad de los escenarios PM-01 a PM-07 y que ningún fallo se presenta como éxito, ausencia o listado vacío.
- [x] 6.2 Preparar un caso de integración que cree un proyecto y lo consulte/liste desde una nueva instancia de aplicación conservando la misma base de datos; verificar que comprueba los cinco valores persistidos sin cambios y satisface el escenario de recuperación de PM-06, sin arrancar procesos ni ejecutar pruebas hasta recibir autorización.
- [x] 6.3 Revisar la alineación final entre implementación, spec y documentación afectada y registrar qué comprobaciones se ejecutaron realmente y cuáles siguen pendientes; verificar que el cambio no incorpora capacidades excluidas, modificaciones de dependencias, frontend, demo-vulnerable ni requisitos de entrega marcados como completados sin evidencia.

## 7. Correcciones de los dos findings de revisión

- [x] 7.1 Obtener aprobación expresa para rechazar únicamente U+0000 con HTTP `400`, `VALIDATION_ERROR` e `INVALID_CHARACTER` por campo afectado, sin eliminarlo ni sustituirlo; reflejar la decisión en PM-02/PM-07, diseño y OpenAPI.
- [x] 7.2 Centralizar el rechazo de U+0000 en `Project.validateCreation`, reutilizado por el flujo existente antes de acceder a persistencia, conservando trim, descripción opcional y límites Unicode.
- [x] 7.3 Preparar casos de U+0000 interior en cada campo y en ambos simultáneamente en dominio, REST e integración; comprobar errores de campo, ausencia de valores rechazados, ausencia de llamadas al puerto en REST y ausencia de inserciones en integración, sin ejecutar los casos ni alterar los existentes de Unicode válido y límites.
- [x] 7.4 Corregir únicamente el registro de comprobaciones con los seis tests del informe histórico existente, sus limitaciones de correspondencia con fuentes y las validaciones pendientes posteriores a la corrección.

## Registro de implementación y comprobaciones

- Implementación preparada el 2026-10-09. Revisión manual de artefactos, firmas, mapeos, SQL y contrato REST; consultas locales de estado/instrucciones OpenSpec. No se ha repetido la validación estricta del change.
- PM-01/PM-02: `domain/project/Project` protege invariantes, trim y límites por puntos de código; `CreateProjectCommand` valida antes de iniciar la transacción. Casos preparados en `ProjectTest`, `ProjectUseCasesTest` y `ProjectControllerTest`.
- PM-03/PM-04/PM-05: casos de uso explícitos, `ProjectRepositoryPort` y `ProjectController`; orden por fecha descendente y UUID canónico ascendente en PostgreSQL. Casos de creación, duplicados, consulta, ausencia, listado vacío y empate preparados en pruebas unitarias y `ProjectPostgresIntegrationTest`.
- PM-06: `ProjectJpaEntity`, `PostgresProjectRepositoryAdapter` y `TransactionalCreateProjectUseCase`; commit antes de respuesta, rollback ante fallo diferido y lectura desde una nueva instancia. Casos preparados en `TransactionalCreateProjectUseCaseTest` y `ProjectPostgresIntegrationTest`.
- PM-07: `ProjectRequestMapper` y `ProjectExceptionHandler` implementan los cinco códigos y errores de campo, con mensajes genéricos en español. Pruebas preparadas de entrada inválida sin acceso al puerto y fallos de lectura/escritura sin detalles internos.
- OpenAPI y únicamente la sección `Project` del modelo de datos actualizados. V1 conserva FK e índice de `scans` y los demás elementos preexistentes; no se crea V2. V1 será inmutable desde su uso como baseline real.
- Durante la implementación inicial no se ejecutaron compilaciones, tests, linters, type-checkers, servidores, Docker, PostgreSQL ni migraciones. No se han cambiado dependencias, versiones, despliegue ni requisitos oficiales de entrega del TFM. El intento de validación autorizado posteriormente se registra a continuación.

Comandos pendientes, desde `backend/`, para ejecución posterior por el usuario:

```powershell
.\mvnw.cmd "-Dtest=ProjectTest,ProjectUseCasesTest,TransactionalCreateProjectUseCaseTest,ProjectControllerTest" test
.\mvnw.cmd "-Dtest=ProjectPostgresIntegrationTest" test
.\mvnw.cmd -DskipTests package
```

La prueba PostgreSQL se omite si no está definida `PROJECT_TEST_DB_URL` con una URL JDBC PostgreSQL. Requiere además `PROJECT_TEST_DB_USERNAME` y `PROJECT_TEST_DB_PASSWORD`, configuradas externamente, y una base dedicada de PostgreSQL 16 con permisos para Flyway/pgcrypto y esquemas de prueba. Cada caso crea y elimina únicamente su esquema aleatorio `project_test_<uuid>`; ejecuta V1 en dicho esquema y abre contextos Spring sin servidor. El caso de reinicio conserva ese mismo esquema entre dos instancias. No usar una base compartida ni un baseline local con checksum antiguo sin resolverlo expresamente; no se ha ejecutado `repair`, `clean` ni ninguna corrección automática de bases existentes.

### Validación técnica autorizada el 2026-10-09

- `mvnw.cmd "-Dtest=ProjectTest,ProjectUseCasesTest,TransactionalCreateProjectUseCaseTest,ProjectControllerTest" test`: `BUILD FAILURE`, código de salida 1, durante compilación principal: `release version 21 not supported`. Ningún test llegó a ejecutarse.
- `mvnw.cmd -DskipTests package`: `BUILD FAILURE`, código de salida 1, por el mismo error de compilación. No se generó el package.
- Diagnóstico del entorno: `java -version` y `javac -version` muestran 17.0.12; `JAVA_HOME` no está configurada. No se encontró JDK 21 en las ubicaciones habituales inspeccionadas. No se ha instalado Java ni cambiado el nivel Java 21 del proyecto.
- `ProjectPostgresIntegrationTest`: no se ejecutó. Los dos comandos previos no superaron la compilación y no están configuradas `PROJECT_TEST_DB_URL`, `PROJECT_TEST_DB_USERNAME` ni `PROJECT_TEST_DB_PASSWORD`; solo se comprobó su presencia, sin mostrar credenciales ni acceder a PostgreSQL.
- El primer intento del wrapper en sandbox quedó bloqueado al crear su caché `.m2`; tras la aprobación correspondiente se repitió fuera del sandbox. Maven descargó artefactos ya declarados, sin modificar el POM, dependencias ni versiones.
- Estado de validación: **BLOCKED** por JDK incompatible. Pendiente configurar JDK 21 y repetir las validaciones anteriores. No hay resultados de tests superados ni fallos de código confirmados; no se han modificado fuentes o infraestructura durante este intento.

### Repetición autorizada con Java 21 el 2026-10-09

- Se localizó el JDK instalado en `C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot`. El entorno heredado por Maven seguía seleccionando Java 17; se ajustó `JAVA_HOME` únicamente para los procesos de validación. `mvnw.cmd -version` confirmó Java 21.0.12.1, sin cambios permanentes de configuración.
- `mvnw.cmd "-Dtest=ProjectTest,ProjectUseCasesTest,TransactionalCreateProjectUseCaseTest,ProjectControllerTest" test`: **BUILD SUCCESS**, salida 0. Se compilaron fuentes principales y de pruebas. Resultado: 21 tests, 0 fallos, 0 errores, 0 omitidos (`ProjectTest`: 5, `ProjectUseCasesTest`: 5, `TransactionalCreateProjectUseCaseTest`: 1, `ProjectControllerTest`: 10), confirmado en los informes Surefire.
- `mvnw.cmd -DskipTests package`: **BUILD SUCCESS**, salida 0. Generado y reempaquetado `backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar`; los tests no se repitieron durante package, conforme a `-DskipTests`.
- `ProjectPostgresIntegrationTest`: no se ejecutó porque siguen sin estar configuradas `PROJECT_TEST_DB_URL`, `PROJECT_TEST_DB_USERNAME` y `PROJECT_TEST_DB_PASSWORD`. No se accedió a PostgreSQL ni se modificó infraestructura.
- No se necesitaron correcciones de código. Solo se actualiza este registro; no se modifican spec, fuentes, dependencias ni versiones. Los avisos de Mockito sobre carga dinámica del agente no provocaron fallos.
- Estado en aquella validación: **READY FOR REVIEW**, con integración PostgreSQL pendiente por falta de configuración del entorno. Este resultado sustituyó al bloqueo anterior por Java 17; la evidencia de integración localizada posteriormente se registra a continuación.

### Corrección del registro de integración — finding 2

- Se revisó por lectura, sin ejecutar tests, el informe existente `backend/target/surefire-reports/com.llmscannerseguridad.integration.ProjectPostgresIntegrationTest.txt`. Registra **6 tests, 0 fallos, 0 errores y 0 omitidos**, con duración de 5.309 s. Se reconoce esta evidencia histórica de integración y se corrige la indicación desactualizada de que no existía un resultado de integración disponible.
- El informe de texto no acredita el comando utilizado, la versión de PostgreSQL, la revisión de fuentes ni el contexto de ejecución. No se ha podido confirmar su correspondencia con las fuentes actuales; no se atribuyen esos datos sin evidencia. No fue necesario abrir el XML ni consultar propiedades de entorno o credenciales.
- Estos seis resultados históricos no acreditan automáticamente cambios posteriores ni los casos nuevos o modificados de U+0000 del finding 1. La ejecución de dichos casos y la validación de las fuentes posteriores a la corrección siguen pendientes de autorización explícita; la integración requiere además autorización para PostgreSQL y las migraciones de la fixture, con una base dedicada configurada externamente.
- Finding 2 corregido únicamente en este registro. No se añadieron tests para la corrección documental ni se alteraron informes generados. En esta tarea solo se realizaron lecturas dirigidas y revisión manual; no se ejecutaron Maven, compilaciones, tests, servidores, PostgreSQL ni migraciones.

### Corrección de U+0000 — finding 1

- El usuario aprobó expresamente la propuesta contractual en esta conversación: rechazar U+0000 en `name` y `description` antes de persistencia, con HTTP `400`, `VALIDATION_ERROR` e `INVALID_CHARACTER` por cada campo afectado, mensajes en español sin valores rechazados y sin eliminar ni sustituir el carácter.
- Se actualizaron únicamente PM-02/PM-07 de la spec, la regla correspondiente del diseño y el contrato OpenAPI. La implementación se limita a `Project.validateCreation`; `CreateProjectCommand` ya la reutiliza antes de iniciar la creación y el manejador REST ya propaga los errores de campo. No fue necesario modificar otros archivos de producción, arquitectura, transacciones, V1, esquema, dependencias ni versiones.
- Se añadieron tres métodos de prueba a `ProjectTest` (nombre, descripción y ambos campos), uno a `ProjectControllerTest` y uno a `ProjectPostgresIntegrationTest`, cada uno de estos dos últimos con los tres casos de JSON con `\u0000` escapado. Comprueban código/campo/mensaje y ausencia de valores rechazados; REST comprueba que no se accede al puerto e integración comprueba que no se inserta ningún proyecto. Los casos previos de Unicode válido y límites se conservan.
- Validaciones realmente realizadas en esta corrección: lectura dirigida del contexto, código afectado e informe Surefire de texto; revisión manual de coherencia entre regla, flujo existente, pruebas preparadas y contratos. No se ejecutaron tests, Maven, compilaciones, linters, formatters, servidores, PostgreSQL ni migraciones. No se abrió el informe XML, se consultaron credenciales ni se modificaron informes generados.
- Los informes históricos de validaciones previas, incluidos los seis tests de integración, no acreditan estas fuentes posteriores ni los cinco métodos de prueba añadidos. No se ha confirmado una correspondencia entre esos informes y las fuentes actuales. Los nuevos casos siguen pendientes de ejecución con autorización expresa.
- Estado de las correcciones: finding 1 corregido con decisión contractual aprobada; finding 2 corregido documentalmente. **READY FOR RE-REVIEW**, con validación ejecutable posterior pendiente. No se aprueba, cierra ni archiva el change.

Comandos propuestos para una autorización posterior, desde `backend/`:

```powershell
.\mvnw.cmd "-Dtest=ProjectTest,ProjectUseCasesTest,TransactionalCreateProjectUseCaseTest,ProjectControllerTest" test
.\mvnw.cmd "-Dtest=ProjectPostgresIntegrationTest" test
```

El segundo comando requiere autorización explícita para PostgreSQL y las migraciones ejecutadas por la fixture, además de una base dedicada configurada externamente. No se han consultado sus credenciales ni su configuración sensible en esta corrección.
