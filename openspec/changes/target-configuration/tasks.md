Implementación autorizada del change target-configuration. Las tareas se marcan únicamente con evidencia en código y documentación. Preparar pruebas no autoriza Maven, tests, servidores, PostgreSQL ni migraciones: su ejecución necesita petición expresa según `AGENTS.md`. No se modificará el change aprobado `project-management`, V1, dependencias, frontend ni demo-vulnerable.

**Decisiones aprobadas:** D-CRYPTO está cerrado para el MVP local y no quedan decisiones bloqueantes. Las dependencias entre tareas indican orden técnico, no decisiones pendientes. AWS y los controles SSRF completos de ejecución siguen fuera del change. Preparar pruebas no implica que se hayan ejecutado.

## 1. Registro y concreción interna del cifrado aprobado

- [x] 1.1 Registrar en ADR-002 y las referencias directamente afectadas de decisiones abiertas/modelo de amenazas el cierre aprobado de D-CRYPTO: AES-256-GCM, clave externa de 256 bits, nonce aleatorio nuevo, formato versionado, secretos solo en memoria cuando sean necesarios; verificar coherencia con TC-11, sin elegir tecnología AWS, leer claves reales o cambiar dependencias.
- [x] 1.2 Concretar/documentar el layout interno versionado con nonce/ciphertext/tag y parámetros estándar GCM, codificación y propiedad de provisión externa de clave, y casos de indisponibilidad/integridad; verificar que el adaptador puede recuperar datos con la misma clave tras reinicio, sin clave por defecto ni cambios del contrato de dominio (TC-09 a TC-12). Son detalles internos de la decisión aprobada, no una nueva puerta de aprobación arquitectónica.

## 2. Contrato y documentación afectada

- [x] 2.1 Documentar los POST/GET/PUT del recurso único y sus DTOs diferenciados de entrada/salida en `docs/05-api/openapi.yaml`; verificar por revisión cobertura de TC-01 a TC-12, timeout opcional con default 10000 en POST/PUT y rango 1000..60000, Location, códigos/errores de campo y ausencia de secretos en respuestas, manteniendo sin cambios los endpoints de Project.
- [x] 2.2 Actualizar únicamente la documentación de target afectada en `docs/05-api/target-contract.md`, `docs/06-base-de-datos/modelo-datos.md` y las referencias de seguridad/arquitectura que correspondan, incluidas ADR-001/modelo de amenazas por la excepción local/demo; verificar cardinalidad MVP 0..1, N como evolución futura, JSONPath acotado, constantes no persistidas, separación configuración/ejecución y cifrado local aprobado, sin reabrir Project ni avanzar otros changes.

## 3. Dominio y validadores locales

- [x] 3.1 Definir el dominio de target y sus valores/invariantes, metadatos de autenticación y representación segura sin credenciales en claro; verificar TC-01/TC-04/TC-09 mediante revisión de firmas y casos preparados, sin imports Spring/JPA/HTTP/Jackson/PostgreSQL ni modificaciones del dominio Project.
- [x] 3.2 Preparar entrada JSON estricta y reglas de campos obligatorios/opcionales, constantes, tipos, límites Unicode/UTF-8 y rechazo de U+0000 antes de normalización; verificar TC-04/TC-12 con timeout omitido → 10000 en POST/PUT, null inválido, claves duplicadas, campos desconocidos dentro de authConfig y límites exactos sin coerción ni reflejo de valores.
- [x] 3.3 Implementar validación sintáctica de URL y comparación normalizada de origen sin DNS, con bloqueo por defecto de localhost/loopback/literales privados y excepción local/demo externa, desactivada por defecto y limitada a destinos declarados; verificar TC-05/TC-10 con excepción habilitada/deshabilitada, destinos no declarados, imposibilidad de activación desde API y validaciones estructurales siempre vigentes, sin DNS ni cliente HTTP.
- [x] 3.4 Implementar el validador de requestTemplate detrás de un puerto sin filtrar tipos Jackson; verificar TC-06 mediante casos preparados de objeto válido, vacío, JSON inválido, claves duplicadas, cero/múltiples tokens, token escapado, clave y fragmento de cadena, sin sustituir o enviar prompts.
- [x] 3.5 Implementar el parser/validador del JSONPath restringido de TC-07; verificar casos preparados de propiedades por punto/comillas JSON, índices, raíz, límites y rechazo de filtros/scripts/comodines/uniones/slices/descenso recursivo, sin motor general de expresiones ni extracción de respuestas reales.
- [x] 3.6 Implementar validaciones de headers y authConfig separados; verificar TC-08/TC-09/TC-12 con casos preparados de límites, controles, duplicados sin distinción de caso, reservados, API key válida, colisiones, secretos omitidos/nulos y NONE incompatible con authConfig, sin imprimir entradas.

## 4. Puertos y casos de uso

- [x] 4.1 Definir TargetConfigurationRepositoryPort/CredentialEncryptionPort y ConfigureTargetUseCase, reutilizando ProjectRepositoryPort.findById; verificar TC-01/TC-02/TC-11 mediante dobles: proyecto ausente, target existente, UUID/fechas, validación previa, cifrado solicitado solo con autenticación y ausencia de escritura tras fallo, sin implementar todavía un cifrador concreto.
- [x] 4.2 Definir GetTargetConfigurationUseCase con salida segura y distinción de proyecto inexistente, target ausente y error técnico; verificar TC-02/TC-09/TC-12 mediante casos preparados que no llaman al puerto criptográfico ni devuelven material secreto/cifrado.
- [x] 4.3 Definir UpdateTargetConfigurationUseCase y conservación/sustitución de credenciales con estado vigente; verificar TC-03/TC-10 con casos preparados de preservación sin descifrado, cambio de origen/tipo/header que exige secreto, retirada a NONE, fallo sin cambios parciales e identidad/fechas de Project intactas.

## 5. Cifrado AES-256-GCM y clave externa

- [x] 5.1 Tras 1.2, implementar el adaptador AES-256-GCM y envelope versionado con nonce aleatorio criptográfico nuevo por cifrado y tag de autenticación; verificar TC-09/TC-11 con pruebas preparadas del mecanismo real usando datos sintéticos y evitando plaintext en persistencia/salida/logs. No añadir descifrado a GET ni ejecución del target.
- [x] 5.2 Tras 1.2, incorporar clave externa de 256 bits mediante variable de entorno/configuración segura y fallo seguro ante ausencia/formato inválido/indisponibilidad, sin clave por defecto o efímera automática; verificar separación respecto a PostgreSQL/repositorio, sin consultar credenciales reales ni implementar AWS (TC-11).
- [x] 5.3 Tras 5.1/5.2, preparar pruebas reales de nonces nuevos al cifrar el mismo secreto, recuperación en memoria con clave sintética estable y formato versionado, rechazo de clave incorrecta/material alterado/versión desconocida y errores sanitizados; verificar que ejercitan el mecanismo real y no atribuyen garantías criptográficas a los dobles de 4.x (TC-11/TC-12).

## 6. Persistencia y transacciones simples

- [x] 6.1 Tras 1.2, crear la siguiente migración Flyway disponible para target_configurations con FK/UNIQUE de project_id, datos ordinarios, material cifrado versionado y checks aprobados; verificar TC-01/TC-04/TC-11, sin columnas method/content_type/prompt_placeholder, sin modificar V1/projects/scans ni ejecutar migraciones.
- [x] 6.2 Tras 6.1, implementar entidad JPA separada y adaptador PostgreSQL con headers no sensibles y credencial opaca; verificar TC-01/TC-09/TC-11, traducción de UNIQUE a TARGET_ALREADY_CONFIGURED, constantes derivadas sin persistencia y ausencia de secretValue/authConfig en claro en columnas/JSON.
- [x] 6.3 Tras 5.1/5.2/6.2, componer transacciones simples de creación/actualización que consultan el estado persistido y escriben atómicamente, sin ETag, versionado optimista ni bloqueos explícitos adicionales; verificar commit antes de éxito, rollback completo y reemplazo/retirada de credencial, manteniendo aplicación/dominio independientes de Spring y unicidad de creación mediante la base de datos (TC-02/TC-03/TC-10/TC-11).
- [x] 6.4 Tras 6.3, preparar integración PostgreSQL con datos y claves sintéticos para unicidad concurrente de creación, consulta segura, actualización, rollback y recuperación desde una nueva instancia; verificar TC-01 a TC-03/TC-09 a TC-12 y conservación de Project, sin prometer detección de cambios perdidos en PUT ni ejecutar pruebas, conectar a bases reales o migrar sin autorización.

## 7. Adaptador REST

- [x] 7.1 Implementar DTOs seguros y los tres endpoints de configuración delegando en aplicación; verificar TC-01 a TC-04/TC-09/TC-10 mediante pruebas REST preparadas de estados, Location, constantes, fechas, timeout por defecto y write-only, sin red al target. La composición operativa con persistencia autenticada depende técnicamente de 6.3.
- [x] 7.2 Implementar el manejador de errores de target con la forma existente, campos contractuales y mensajes en español; verificar TC-12 y errores de TC-01 a TC-11 con casos preparados de ausencia, validación, conflicto y fallo técnico, sin alterar el comportamiento de Project ni incluir valores, headers arbitrarios, blobs, SQL o datos criptográficos.

## 8. Verificación transversal y cierre

- [x] 8.1 Tras 6.4/7.x, preparar los casos integrados TC-01 a TC-12, incluyendo marcadores sintéticos ausentes de respuesta/logs, GET/PUT de conservación sin descifrado, default de timeout, excepción local/demo controlada, ausencia de DNS/HTTP y errores sin efectos parciales; verificar trazabilidad de cada escenario y distinguir pruebas preparadas de las ejecutadas con autorización.
- [x] 8.2 Tras 8.1, revisar código/OpenSpec/documentación y registrar resultados reales y comprobaciones pendientes; verificar que no se marca completa la implementación mientras falten cifrado o pruebas necesarias. Ejecutar comprobaciones solo con autorización expresa, sin implementar scans/AWS ni modificar requisitos aprobados de Project.

## Registro de implementación — 2026-10-10

**Estado: READY FOR REVIEW.** Las 24 tareas cuentan con implementación/documentación y pruebas preparadas. No significa validación técnica ejecutada ni aprobación del change; no se cierra ni archiva.

### Correspondencia requisitos → implementación

| Requisitos | Implementación y evidencia preparada |
| --- | --- |
| TC-01/TC-02 | ConfigureTargetUseCase/GetTargetConfigurationUseCase, servicio, ProjectRepositoryPort existente, FK/UNIQUE V2, REST 201/Location/200/404/409 y prueba concurrente PostgreSQL. |
| TC-03/TC-10 | UpdateTargetConfigurationUseCase, comparación de origen local sin DNS, preservación de blob sin descifrar, reemplazo/retirada atómica, identidad/fechas preservadas, reloj no regresivo y rollback de commit. |
| TC-04/TC-12 | TargetSettings/TargetRules, TargetRequestMapper y StrictTargetJson: campos/tipos/constantes, default timeout 10000 también en PUT, límites Unicode/UTF-8, claves duplicadas/desconocidas y rechazo de U+0000 antes de normalizar. |
| TC-05 | TargetUrl y LocalTargetUrlPolicy: sintaxis, literales IPv4/IPv6 normalizados, bloqueo local/privado, metadatos conocidos y excepción externa acotada solo con perfil local. No existen DNS/HTTP ni certificación de autorización. |
| TC-06/TC-07 | ConfigurationValidationPort y JsonTargetConfigurationValidator: objeto JSON estricto, token decodificado único como valor completo y JSONPath restringido, sin renderizado/extracción. |
| TC-08/TC-09 | Validación de headers/authConfig separada, NONE/Bearer/API key; TargetCommand con toString protegido, TargetView/TargetResponse sin secreto/blob, errores con rutas contractuales. |
| TC-11 | CredentialEncryptionPort, AES-256-GCM real y CredentialEnvelope v1, clave externa Base64 de 32 bytes sin fallback, nonce nuevo de 12 bytes, tag de 128 bits y versión como AAD; JPA separado y transacciones con commit antes del éxito. |
| TC-12 | TargetExceptionHandler y TargetOperationException sanitizados, sin causas internas/entrada; fallo de lectura no se interpreta como ausencia. Se desactivan categorías SQL/bind/extract/error de Hibernate para evitar logs con material persistido en fallos. |

### Pruebas preparadas, no ejecutadas

- TargetConfigurationTest: 3 métodos de dominio, Unicode, límites, headers y secretos.
- TargetValidationTest: 3 métodos de URL/excepción demo, plantilla y JSONPath, incluyendo límites y U+0000 decodificado.
- TargetUseCasesTest: 4 métodos de creación/consulta/actualización, ausencias/conflicto, conservación y cambios de origen/tipo/header, errores y reloj no regresivo.
- AesGcmCredentialEncryptionAdapterTest: 2 métodos con JCA real y datos/claves sintéticos: nonce nuevo, recuperación con clave estable/nueva instancia, integridad, clave incorrecta/ausente/inválida y versión desconocida/truncamiento.
- TargetControllerTest: 5 métodos REST, estados/Location/representación pública/defaults, estructura/tipos/validaciones, marcadores sintéticos ausentes de respuesta/logs y ausencia de escrituras/cifrado tras validación fallida.
- TargetTransactionsTest: 2 métodos de commit/rollback, REQUIRES_NEW y errores sanitizados.
- TargetPostgresIntegrationTest: 5 métodos de persistencia/cifrado real, consulta y conservación tras reinicio sin clave, UNIQUE concurrente, sustitución/retirada, Project intacto, validación sin filas parciales, clave ausente, rollback por fallo diferido de commit y lectura fallida.

Total: **24 métodos preparados (19 sin PostgreSQL y 5 de integración)**. No se atribuyen resultados históricos de Project a estas fuentes.

### Comprobaciones realmente realizadas

Lectura dirigida de instrucciones, artefactos actuales, código afectado y documentación; consulta local del estado/instrucciones apply de OpenSpec (ready, inicialmente 0/24); revisión manual de los cambios, contratos, imports por capas, formato versionado, mapeos y trazabilidad. La revisión manual corrigió una duplicación accidental durante la edición de OpenAPI antes de finalizar.

No se ejecutaron Maven, compilación/package, tests, linters, formatters, servidores, Docker, PostgreSQL, migraciones ni validación estricta OpenSpec. No se leyeron claves, credenciales de entorno ni archivos .env; no hubo acceso a servicios externos ni operaciones Git de escritura. El resultado estricto previo indicado por el usuario no acredita esta implementación.

No cambian requisitos administrativos del TFM; no se modifica su checklist. Tampoco se modifican project-management, V1, projects/scans, frontend, demo, dependencias, versiones o despliegue.

### Comandos propuestos, pendientes de autorización

Desde backend/:

```powershell
.\mvnw.cmd "-Dtest=TargetConfigurationTest,TargetValidationTest,TargetUseCasesTest,AesGcmCredentialEncryptionAdapterTest,TargetTransactionsTest,TargetControllerTest" test
.\mvnw.cmd -DskipTests package
.\mvnw.cmd "-Dtest=TargetPostgresIntegrationTest" test
```

El tercero requiere autorización expresa para PostgreSQL **y las migraciones/limpieza del esquema aislado de la fixture**, una base dedicada configurada externamente mediante TARGET_TEST_DB_URL/TARGET_TEST_DB_USERNAME/TARGET_TEST_DB_PASSWORD y TARGET_POSTGRES_TESTS_ENABLED=true. La fixture genera su clave sintética y solo elimina el esquema aleatorio target_test_<UUID> que crea. No se han consultado ni configurado estas variables.

### Configuración y limitaciones

- TARGET_CREDENTIAL_ENCRYPTION_KEY: Base64 estándar de 32 bytes, externa a PostgreSQL/repositorio, sin valor por defecto. Se consulta solo al cifrar; pérdida/cambio de clave impide recuperar blobs anteriores. Provisión/rotación AWS fuera del change.
- Excepción demo: perfil local más TARGET_LOCAL_DEMO_ENABLED=true y TARGET_LOCAL_DEMO_ORIGINS con orígenes separados por comas (sin ruta específica/query). Desactivada por defecto; no admite metadatos/link-local ni activación desde API.
- PUT concurrente aplica última escritura; no hay ETag, versiones ni locks explícitos. La UNIQUE protege creación concurrente.
- Protección SSRF completa, DNS, redirecciones, ejecución HTTP, extracción y autorización efectiva del target requieren cambios posteriores.
- Validación ejecutable y regresión de Project quedan pendientes; no se certifica compilación ni integración con esta revisión manual.

### Archivos creados (41)

Dominio (bajo backend/src/main/java/com/llmscannerseguridad/):

- `domain/target/AuthType.java`
- `domain/target/EncryptedCredential.java`
- `domain/target/TargetConfiguration.java`
- `domain/target/TargetFieldViolation.java`
- `domain/target/TargetRules.java`
- `domain/target/TargetSettings.java`
- `domain/target/TargetUrl.java`
- `domain/target/TargetValidationException.java`

Aplicación y puertos (bajo backend/src/main/java/com/llmscannerseguridad/):

- `application/target/ConfigurationValidationPort.java`
- `application/target/ConfigureTargetUseCase.java`
- `application/target/CredentialEncryptionPort.java`
- `application/target/GetTargetConfigurationUseCase.java`
- `application/target/TargetCommand.java`
- `application/target/TargetConfigurationRepositoryPort.java`
- `application/target/TargetConfigurationService.java`
- `application/target/TargetOperationException.java`
- `application/target/TargetView.java`
- `application/target/UpdateTargetConfigurationUseCase.java`

Adaptadores REST (bajo backend/src/main/java/com/llmscannerseguridad/):

- `adapters/in/rest/target/InvalidTargetRequestException.java`
- `adapters/in/rest/target/TargetApiError.java`
- `adapters/in/rest/target/TargetController.java`
- `adapters/in/rest/target/TargetExceptionHandler.java`
- `adapters/in/rest/target/TargetRequestMapper.java`
- `adapters/in/rest/target/TargetResponse.java`

Adaptadores de validación/cifrado/persistencia (bajo backend/src/main/java/com/llmscannerseguridad/):

- `adapters/out/validation/target/StrictTargetJson.java`
- `adapters/out/validation/target/LocalTargetUrlPolicy.java`
- `adapters/out/validation/target/JsonTargetConfigurationValidator.java`
- `adapters/out/crypto/target/CredentialEnvelope.java`
- `adapters/out/crypto/target/AesGcmCredentialEncryptionAdapter.java`
- `adapters/out/persistence/target/TargetConfigurationJpaEntity.java`
- `adapters/out/persistence/target/PostgresTargetConfigurationRepositoryAdapter.java`

Composición (bajo backend/src/main/java/com/llmscannerseguridad/):

- `config/TargetConfigurationBeans.java`
- `config/TargetTransactions.java`

Migración:

- `backend/src/main/resources/db/migration/V2__crear_target_configurations.sql`

Tests (bajo backend/src/test/java/com/llmscannerseguridad/):

- `domain/target/TargetConfigurationTest.java`
- `application/target/TargetUseCasesTest.java`
- `adapters/out/validation/target/TargetValidationTest.java`
- `adapters/out/crypto/target/AesGcmCredentialEncryptionAdapterTest.java`
- `adapters/in/rest/target/TargetControllerTest.java`
- `config/TargetTransactionsTest.java`
- `integration/TargetPostgresIntegrationTest.java`

### Archivos modificados (10)

- `backend/src/main/resources/application.yml`
- `docs/02-arquitectura/arquitectura-general.md`
- `docs/02-arquitectura/decisiones/ADR-001-contrato-target.md`
- `docs/02-arquitectura/decisiones/ADR-002-autenticacion-target-credenciales.md`
- `docs/02-arquitectura/decisiones/decisiones-abiertas.md`
- `docs/03-seguridad/modelo-de-amenazas.md`
- `docs/05-api/openapi.yaml`
- `docs/05-api/target-contract.md`
- `docs/06-base-de-datos/modelo-datos.md`
- `openspec/changes/target-configuration/tasks.md`

## Validación técnica autorizada — 2026-10-10

Este registro actualiza las comprobaciones pendientes indicadas en la implementación anterior, sin cambiar requisitos ni tareas implementadas.

- Primer intento del comando de tests: el wrapper no llegó a arrancar Maven, con error PowerShell de índice sobre matriz nula. No se ejecutaron tests en ese intento.
- La sesión seleccionaba Java 17 por PATH. Se utilizó temporalmente JAVA_HOME apuntando al JDK 21 instalado, únicamente en los procesos de validación, sin modificar wrapper, versiones ni configuración del proyecto. El reintento fuera del sandbox arrancó correctamente.
- `./mvnw.cmd "-Dtest=TargetConfigurationTest,TargetValidationTest,TargetUseCasesTest,AesGcmCredentialEncryptionAdapterTest,TargetTransactionsTest,TargetControllerTest" test`: **BUILD SUCCESS**, release 21; **19 tests, 0 fallos, 0 errores y 0 omitidos**. Reparto: dominio 3, validación 3, casos de uso 4, cifrado 2, transacciones 2 y REST 5.
- `./mvnw.cmd -DskipTests package`: **BUILD SUCCESS**; JAR ejecutable generado en backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar. No vuelve a ejecutar tests.
- En el intento del agente, la integración no se ejecutó: faltaban TARGET_TEST_DB_URL, TARGET_TEST_DB_USERNAME y TARGET_TEST_DB_PASSWORD y TARGET_POSTGRES_TESTS_ENABLED no estaba habilitada. No se consultaron ni mostraron valores de credenciales, no se conectó a PostgreSQL y no se ejecutaron migraciones ni limpieza desde el agente.
- Posteriormente, el usuario aportó la salida de su ejecución de TargetPostgresIntegrationTest: **BUILD SUCCESS; 5 tests, 0 fallos, 0 errores y 0 omitidos**. Finalización indicada: 2026-10-10T01:05:18+02:00; duración Maven 7.278 s, clase 5.740 s. Se registra como evidencia aportada por el usuario, sin atribuirla a una ejecución del agente ni afirmar versión de PostgreSQL, comando exacto, configuración o revisión de fuentes que el fragmento no acredita. No se consultaron informes XML ni credenciales y no se repitió la prueba.
- No hubo fallos del código en las validaciones ejecutadas ni correcciones de producción/tests. Único archivo editado durante esta validación: este registro. Las advertencias de Mockito/Byte Buddy sobre carga dinámica de agente no impidieron los tests.

**Estado de validación: READY FOR REVIEW.** Queda resuelto el bloqueo de integración con la evidencia aportada por el usuario. Resultado combinado: 19 tests ejecutados por el agente y 5 de integración reportados por el usuario, todos sin fallos, errores ni omisiones; package correcto. No se cierra ni archiva el change.

## Corrección del finding de surrogates aislados — 2026-10-10

- [x] Rechazar únicamente surrogates UTF-16 aislados en authConfig.secretValue mediante TargetRules.secret, antes de convertir a UTF-8/cifrar/escribir, con 400 VALIDATION_ERROR e INVALID_CHARACTER; aceptar pares válidos sin alterar caracteres.
- [x] Añadir cobertura de surrogate alto/bajo aislado, par válido, ausencia de llamadas al cifrador/persistencia y PUT inválido sin cambios en configuración/credencial.
- [x] Alinear TC-09, design, OpenAPI y target-contract con la corrección expresamente autorizada, sin modificar otros requisitos, arquitectura, AES-GCM, dependencias ni esquema.
- [x] Ejecutar los tests autorizados de target y package, y revisar los cambios.

Regla: String.codePoints combina pares válidos en puntos de código suplementarios; cualquier punto restante en U+D800..U+DFFF identifica un surrogate aislado y produce INVALID_CHARACTER. No se elimina, sustituye ni normaliza el secreto.

Se añadieron cinco métodos: uno en TargetConfigurationTest (ambos tipos autenticados), tres en TargetControllerTest (POST inválido, PUT inválido y par válido sin alteración), y uno en AesGcmCredentialEncryptionAdapterTest (recuperación exacta de un carácter suplementario con espacios exteriores). Los casos REST envían escapes JSON explícitos para no sustituir el surrogate durante la preparación del cuerpo; verifican respuestas sin valores rechazados y logs sin marcadores de entrada.

Validación autorizada con Java 21, JAVA_HOME temporal para cada proceso:

- Primer intento dentro del sandbox: el wrapper no arrancó Maven por el error PowerShell de matriz nula; no llegó a ejecutar tests. Se repitió fuera del sandbox con aprobación.
- `mvnw.cmd "-Dtest=TargetConfigurationTest,TargetValidationTest,TargetUseCasesTest,AesGcmCredentialEncryptionAdapterTest,TargetTransactionsTest,TargetControllerTest" test`: BUILD SUCCESS, salida 0; 24 tests, 0 fallos, 0 errores y 0 omitidos. Dominio 4, validación 3, aplicación 4, cifrado 3, transacciones 2, REST 8.
- `mvnw.cmd -DskipTests package`: BUILD SUCCESS, salida 0; JAR reempaquetado. No se repitieron tests durante package.
- No se repitió integración PostgreSQL: la corrección se aplica antes de acceder al puerto; los tests REST comprueban cero interacciones y preservación del estado anterior. No se cambian persistencia, V2 ni transacciones. Los cinco resultados de integración anteriores son evidencia histórica, no una ejecución de estas fuentes corregidas.
- No se accedió a bases de datos, servicios externos ni credenciales, ni se ejecutaron migraciones. Los avisos de Mockito/Byte Buddy no causaron fallos.

Estado: **READY FOR RE-REVIEW**. Corrección limitada al finding; no se aprueba, cierra ni archiva el change.
