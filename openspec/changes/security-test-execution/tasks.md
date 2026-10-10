La implementación fue solicitada expresamente después de aprobar el diseño. **D-01 a D-11 y HttpClient 5 están aprobados; no volver a solicitar esa aprobación.** El registro y la correspondencia están en design.md. Las casillas registran código, contratos y tests preparados y revisados por lectura; no acreditan tests ejecutados. Maven, servidores fixture, PostgreSQL, migraciones y validaciones necesitan petición expresa conforme a AGENTS.md. Los registros iniciales al final corresponden únicamente a la planificación anterior.

**Estado actual: READY FOR RE-REVIEW — 29/29 tareas completadas.** Los dos findings del review quedaron corregidos y las validaciones autorizadas se ejecutaron con Java 21, según el registro final de este archivo. Se conservan como históricos los resultados externos previos y los bloqueos ya resueltos.

## 1. Contratos y documentación del comportamiento aprobado

- [x] 1.1 Actualizar OpenAPI conforme al diseño aprobado con el único GET executions, su DTO sin evidencia, orden, no-store, errores y semántica técnica nueva/histórica de Scan; verificar por revisión correspondencia STE-10/STE-13/STE-14 y conservación de los siete campos, rutas y admisión anteriores.
- [x] 1.2 Actualizar únicamente modelo-datos, target-contract, arquitectura/ADR-003/decisiones abiertas, modelo de amenazas/política de autorización y casos-de-prueba directamente afectados; verificar catálogo D-11, destino autorizado, límites como valores técnicos iniciales, fallos mixtos, privacidad, no-op histórico y frontera con evaluación. Explicitar que las cuatro pruebas no cubren completamente OWASP y LLM10 observa únicamente output, sin validar consumidor downstream/sink; no cerrar proveedor/scoring/experimento/retención ni cambiar requisitos de entrega (STE-01 a STE-14).

## 2. Dominio y contratos internos

- [x] 2.1 Definir SecurityTest y SecurityCategory con UUID/code/revision/name/category/prompt, identidad de revisión, cuatro categorías y límites aprobados; preparar pruebas de definiciones inválidas, Unicode/prompt máximo y revisiones inmutables, sin enabled ni campos semánticos (STE-01, D-01/D-10/D-11).
- [x] 2.2 Definir TestExecution, estados/códigos cerrados y evidencia con flags; preparar pruebas de las cuatro transiciones, terminales inmutables, nulabilidad, HTTP/latencia/fechas, reloj regresivo y FAILED previo a inicio, verificando que HTTP 200 con contenido peligroso no se juzga vulnerable (STE-03/STE-09/STE-14).
- [x] 2.3 Definir SecurityTestCatalogPort, TestExecutionRepositoryPort y TargetInvocationPort con tipos propios y snapshot interno protegido; verificar firmas/imports/toString sin Spring/JPA/Jackson/HttpClient, secretos descifrados o Throwable en resultados y sin un puerto por cada método (STE-01 a STE-05, D-03/D-07).

## 3. V4, catálogo y persistencia

- [x] 3.1 Crear V4 con guard transaccional ante test_cases/test_executions heredadas, columnas/checks/UNIQUE/FKs e índices conservados según D-08; revisar SQL, tratamiento explícito de NULL y conservación byte a byte de V1/V2/V3 y relaciones de findings, sin ejecutar migraciones ni modificar scans/global_score (STE-11).
- [x] 3.2 Sembrar en V4 los cuatro casos exactos D-11 con UUID literales fijos, code/revision únicos, objetivos/expectativas descriptivas y referencias OWASP; preparar comprobaciones de contenido/identidad estable, sin canarios reales ni duplicar un catálogo mutable en recursos/código (STE-01/STE-02).
- [x] 3.3 Implementar adaptadores PostgreSQL/entidades separadas para catálogo y ejecuciones, mapeando evidencia JSON serializada en TEXT y preservando columnas futuras sin uso; preparar casos de round-trip Unicode/U+0000 escapado, flags, datos técnicos, JOIN al test inmutable y lectura scoped sin target/credenciales. Mantener lectura de evidencia interna mediante aplicación/repositorio para un evaluador posterior, sin implementarlo ni exponerla por API (STE-03/STE-09/STE-11/STE-13).
- [x] 3.4 Componer preparación atómica del plan, reclamación y terminalización por ejecución en transacciones nuevas cortas; preparar pruebas de rollback insert/flush/commit, plan visible desde otra conexión antes del envío, UNIQUE por prueba/Scan, updates condicionales y ninguna transacción abierta durante HTTP/espera de cuota (STE-02/STE-03/STE-10).

## 4. Reutilización JSON y credenciales

- [x] 4.1 Reutilizar la gramática existente de plantilla/ruta mediante helpers internos de renderizado/extracción, sin cambiar contrato del validador de configuración; preparar casos de placeholder como nodo string, caracteres escapados/Unicode, estructura conservada, JSON duplicado/inválido y rutas por propiedad/índice/raíz, ausencia/no-string/string vacío/surrogate inválido (STE-04/STE-08).
- [x] 4.2 Añadir CredentialDecryptionPort y operación de descifrado al adaptador AES existente, compartiendo clave/envelope/JCA sin duplicar cifrador; preparar pruebas reales con claves sintéticas de round-trip, clave ausente/incorrecta, tag/envelope/version alterados y ausencia de llamadas crypto para NONE/GET/PUT de conservación (STE-05, D-05).
- [x] 4.3 Implementar evidencia minimizada y redacción exacta de credencial conocida antes de retornar del adaptador, con flags y representación TEXT escapada; preparar pruebas de eco Bearer/API key en texto/cadenas/claves JSON, colisión de redacción, ausencia de copias brutas y límites después de escapes/redacción, verificando no persistencia de NUL literal, headers, URLs, secretos/blobs o causas (STE-05/STE-09).

## 5. Autorización, DNS y transporte HTTP

- [x] 5.1 Añadir únicamente HttpClient 5 cuando sea necesario para el transporte aprobado, conforme a la autorización registrada en D-05, con versión gestionada existente; si no hay gestión en BOM, detener el cambio de POM y acordar versión. Verificar encapsulación detrás de TargetInvocationPort y diff del POM sin actualizaciones del stack/otras dependencias; no instalar ni resolver artefactos sin petición expresa (D-05).
- [x] 5.2 Implementar política externa de orígenes públicos exactos y reutilizar la excepción local/demo aprobada; preparar casos sin permiso/allowlist vacía, normalización de esquema/host/puerto, wildcards rechazados, local sin perfil/flag/lista y ausencia de DNS/crypto antes de autorización, sin habilitación desde API (STE-06).
- [x] 5.3 Implementar clasificación de direcciones y resolvedor acotado conectado al transporte, con máximo 16 resultados, validación de todas las A/AAAA y fijación de una IP; preparar pruebas parametrizadas IPv4/IPv6/mapeadas/especiales/interfaces/metadatos, mezcla pública/bloqueada y excepción demo que nunca admite link-local/metadatos (STE-06, D-04).
- [x] 5.4 Implementar adaptador TargetInvocationPort con POST/JSON/headers ordinarios, NONE/Bearer/API key y TLS/SNI/hostname originales; desactivar proxy ambiental, pooling entre intentos, retries, cookies, autenticación automática, redirects/compresión y HTTP/2. Preparar prueba de conexión efectiva a IP validada y rebinding sin segunda resolución, HTTPS con certificado incorrecto y rechazo de credencial por HTTP público, conservando HTTP demo autorizado (STE-04 a STE-07).
- [x] 5.5 Preparar fixture `jdk.httpserver` HTTP/HTTPS local con datos/certificados exclusivamente sintéticos y dobles DNS/conexión, sin dependencia de demo-vulnerable ni targets externos; verificar captura controlada de método/body/headers y rutas 2xx/401/403/429/5xx/301/302/303/307/308. No arrancarlo hasta autorización; pruebas de SSRF/redirect verifican que el segundo destino no recibe conexiones (STE-04 a STE-08).

## 6. Límites y errores del adaptador

- [x] 6.1 Implementar configuración externa validada con los valores técnicos iniciales aprobados, una invocación simultánea y separación configurable nunca inferior a 1 s, deadlines monotónicos de intento/Scan y capacidad DNS/scheduler finita; preparar casos con valores por defecto y otra configuración admitida, espera de cuota, DNS agotado/saturado/terminado tarde, conexión/TLS lenta, headers/body lento y presupuesto global, verificando cierre/cancelación y cero envío tardío o transacción larga (STE-09, D-06).
- [x] 6.2 Implementar lectura limitada y validación de headers/media type/UTF-8/JSON/responsePath, sin fallback ni descompresión; preparar casos exactos y excedidos de request/response/texto/evidencia, chunked/Content-Length ausente/falso, profundidad, claves duplicadas, encoding no admitido y respuesta inválida. Verificar códigos de STE-08/STE-09 y que jamás se declara completo un texto truncado.
- [x] 6.3 Traducir fallos del transporte/crypto a códigos cerrados sin causas/valores ni logs de cuerpos; preparar marcadores sintéticos en excepciones del cliente, mensajes TLS/DNS, response y credenciales, verificando ausencia en logs, resultados y API y precedencia SCAN_BUDGET_EXCEEDED al agotar presupuesto global (STE-05/STE-09/STE-10/STE-14).

## 7. Trabajo real de Scan y recuperación

- [x] 7.1 Implementar ExecuteSecurityTestsUseCase sobre ScanWorkPort: selección exacta/ordenada, snapshot único de target y ejecución secuencial tras commits; preparar catálogo vacío/incompleto/duplicado/fuera de límite, cuatro peticiones máximas, target actualizado entre pruebas y próximo Scan con nueva configuración, sin modificar POST ni concurrencia de PUT (STE-01/STE-02/STE-10).
- [x] 7.2 Implementar continuación ante fallos individuales y aborto ante seguridad/configuración/credenciales/persistencia/presupuesto; preparar secuencias todos exitosos, uno fallido con éxitos posteriores, todos fallidos, target inaccesible, timeout, JSON/path inválidos y SSRF/crypto que cierran pendientes. Verificar retorno del trabajo solo con plan no vacío íntegramente COMPLETED y Scan FAILED ante cualquier fallo (STE-03/STE-10).
- [x] 7.3 Sustituir la composición del no-op por el caso de uso y ampliar T3 para cerrar ejecuciones activas y Scan atómicamente, preservando transiciones/motivos de Scan; preparar fallos de commit de resultado/T3/compensación, false condicional, transacción ambiente, compuerta de persistencia y terminales inmutables sin requests adicionales (STE-10, D-07).
- [x] 7.4 Ampliar recuperación bajo la misma transacción corta de arranque para TestExecution y Scan; preparar caída tras envío antes del commit, mezcla de pruebas terminales/PENDING/RUNNING, ejecución activa de Scan terminal, reinicio secuencial y commit fallido. Verificar EXECUTION_INTERRUPTED/SCAN_INTERRUPTED, conservación de éxitos y bloqueo del arranque sin relanzamiento (STE-12).

## 8. Consulta de metadatos

- [x] 8.1 Implementar ListTestExecutionsUseCase y único GET scoped con DTO exacto STE-13; preparar UUID inválidos/precedencia, Project ausente, Scan ajeno, plan no preparado, histórico no-op, estados mixtos, orden y no-store, verificando ausencia de acceso a target/clave y ningún endpoint de evidencia/catálogo/Findings.
- [x] 8.2 Implementar errores conforme a la convención existente y pruebas REST de 400/404/500/200 con recurso FAILED; verificar que fallos de lectura no simulan ausencia/array vacío y que DTOs/logs omiten prompts/respuestas/headers/URL/blobs y no modifican fechas o estados (STE-13/STE-14).

## 9. Integración transversal y evidencia de comprobaciones

- [x] 9.1 Preparar integración PostgreSQL 16 dedicada siguiendo fixtures existentes, sin Docker ni nuevas dependencias: V1–V3 → V4, seed estable, constraints, conservación de scans/global_score/findings, rechazo transaccional de históricos de pruebas y recuperación desde otra instancia. Verificar con conexiones independientes las garantías reales de commit/rollback/UNIQUE/FK; no ejecutar Flyway/servidores/tests sin autorización (STE-01/STE-03/STE-11/STE-12).
- [x] 9.2 Preparar recorrido REST → worker → fixture HTTP → PostgreSQL → polling con cuatro pruebas, Bearer/API key sintéticas, redacción de eco, fallo individual y global, mezcla de éxitos y presupuesto. Verificar toda la evidencia/resultado final, commits antes de red, ausencia de semántica de vulnerabilidad y ningún write en findings/evaluator_result/confidence/global_score, sin datos o servicios reales (STE-01 a STE-14).
- [x] 9.3 Revisar trazabilidad final de código/spec/documentación y conservar cambios ajenos; registrar qué comprobaciones se prepararon y cuáles se ejecutaron realmente con petición expresa y sus resultados. Verificar exclusiones/decisiones aprobadas y no atribuir el éxito de OpenSpec o resultados de changes anteriores a esta implementación; mantener pendientes las comprobaciones de aplicación no autorizadas (STE-14).

## Trazabilidad

| Requisitos | Diseño | Tareas principales |
| --- | --- | --- |
| STE-01 catálogo | D-01/D-10/D-11 | 1.2, 2.1, 3.2, 7.1, 9.1 |
| STE-02 plan/snapshot | D-01/D-03/D-07 | 2.3, 3.4, 7.1, 9.2 |
| STE-03 estados | D-02/D-03 | 2.2, 3.3/3.4, 7.2/7.3 |
| STE-04 request/headers | D-05 | 4.1, 5.4/5.5, 6.2 |
| STE-05 credenciales | D-03/D-05 | 4.2/4.3, 5.4/5.5, 6.3 |
| STE-06 SSRF | D-04/D-05 | 5.2/5.3/5.4/5.5, 6.1 |
| STE-07 redirects/HTTP | D-05/D-06 | 5.4/5.5, 6.2, 7.2 |
| STE-08 extracción | D-03/D-05 | 4.1, 5.5, 6.2 |
| STE-09 límites/evidencia | D-03/D-06 | 3.3, 4.3, 6.1/6.2/6.3, 9.2 |
| STE-10 fallos/Scan | D-07 | 3.4, 7.1/7.2/7.3, 9.2 |
| STE-11 V4 | D-08 | 3.1/3.2/3.3, 9.1 |
| STE-12 recuperación | D-07/D-08 | 7.3/7.4, 9.1 |
| STE-13 API | D-09 | 1.1, 8.1/8.2, 9.2 |
| STE-14 frontera | D-03/D-10/D-11 | 1.2, 2.2/2.3, 6.3, 8.2, 9.2/9.3 |

## Estado de planificación inicial (histórico)

**READY FOR IMPLEMENTATION.** Decisiones aprobadas, sin bloqueos de diseño. Todas las tareas permanecen pendientes de implementación. No se ha implementado código, creado V4, cambiado POM, ejecutado servidor fixture ni conectado a PostgreSQL. La aprobación procede del usuario y está registrada en design.md; la validación OpenSpec no acredita comportamiento de aplicación.

## Registro de validación inicial — 2026-10-10

- CLI real con esquema spec-driven: new change → instrucciones/status por artefacto → proposal → spec → design → tasks. No se crearon manualmente la raíz del change ni su metadata.
- `openspec validate security-test-execution --strict`: salida 0, `Change 'security-test-execution' is valid`.
- `openspec status --change security-test-execution`: 4/4 artefactos completos. Contenido: 14 requisitos, 35 escenarios y 29 tareas pendientes.
- Revisión manual de coherencia entre modelos, políticas, límites, API, V4 y trazabilidad. No se ejecutaron comprobaciones de aplicación, servidores, PostgreSQL o migraciones; no se accedió a targets ni servicios externos.
- El CLI local necesitó ejecución fuera del sandbox porque iniciar openspec.cmd devolvía acceso denegado; se utilizó la aprobación correspondiente y telemetría desactivada. La autorización del comando es distinta de la aprobación posterior del diseño registrada en design.md.
- Este registro corresponde a la creación inicial; la revisión posterior incorpora las concreciones aprobadas por el usuario. Sin código implementado ni siguiente change iniciado.

## Validación tras aplicar las decisiones aprobadas — 2026-10-10

- Revisados proposal → spec → design → tasks con las concreciones aprobadas, sin decisiones pendientes de este incremento ni bloqueos de diseño.
- `openspec validate security-test-execution --strict`: código de salida 0; salida exacta `Change 'security-test-execution' is valid`.
- `openspec status --change security-test-execution`: esquema spec-driven, 4/4 artefactos completos y `All planning artifacts complete!`.
- Recuento final: 14 requisitos, 35 escenarios y 29 tareas de implementación, ninguna marcada como realizada.
- Estado: **READY FOR IMPLEMENTATION**. Solo se actualizaron los cuatro artefactos OpenSpec del change; no se implementó código ni se inició el siguiente change.

## Registro de implementación — 2026-10-10

**READY FOR REVIEW.** Las 29 tareas tienen implementación o pruebas preparadas y revisión manual de trazabilidad. Las casillas no acreditan compilación ni tests superados. Esta implementación se autoriza en la petición posterior del usuario; los registros anteriores describen exclusivamente la planificación histórica.

### Resultado y trazabilidad

- STE-01/02: SecurityTest tiene únicamente id/code/revision/name/category/prompt. Catálogo PostgreSQL de solo lectura, cuatro definiciones exactas de D-11 y revisión 1 sembradas por V4; plan completo y snapshot único de target confirmados antes de la primera petición.
- STE-03/10/12: TestExecution contiene exclusivamente los datos aprobados, estados técnicos y códigos cerrados. Worker secuencial real, continuación individual y aborto global; COMPLETED exige las cuatro ejecuciones técnicamente correctas. T3 y recuperación cierran hijos/Scan en una sola transacción, preservando terminales.
- STE-04/05/06/07/08: HttpClient 5 detrás de TargetInvocationPort, sustitución del nodo string completo, headers ordinarios y NONE/Bearer/API key. Descifrado AES existente justo antes de auth, secreto solo en memoria y redacción exacta antes de retornar evidencia. Autorización previa a DNS/crypto, todas las IPs comprobadas y una IP fijada; sin redirects/retries/proxy ambiental. TLS conserva autoridad/SNI y verificación normal. Excepción demo únicamente con perfil/flag/origen explícitos.
- STE-09: valores iniciales aprobados, caps externos validados, lectura y evidencia serializada limitadas, deadlines monotónicos incluyendo planificación, DNS finito, una invocación en vuelo y separación conservadora de al menos un segundo desde el cierre anterior. No se promete cancelación del resolvedor del sistema ni borrado de Strings inmutables de la JVM.
- STE-11: V4 evoluciona las tablas existentes, conserva FKs/índices/columnas futuras y rechaza explícitamente históricos incompatibles antes de alterar tablas. V1/V2/V3 no se modificaron. No se conectó a una base para averiguar su estado ni se ejecutó la migración.
- STE-13/14: único GET de executions scoped, DTO de 14 campos técnicos y no-store, sin evidencia/target/credenciales. Sin Findings, evaluación, scoring o nuevos endpoints. LLM10 observa exclusivamente el output del endpoint.
- Actualizados únicamente los contratos, arquitectura/ADRs, seguridad, estrategia/casos IA y modelo de datos directamente afectados. No se inició, archivó ni sincronizó otro change.

### Pruebas preparadas y comprobaciones realizadas

| Pruebas preparadas | Cobertura principal |
| --- | --- |
| SecurityTestExecutionDomainTest, ExecuteSecurityTestsUseCaseTest | Modelos, transiciones, plan/orden/snapshot, fallos individuales/globales, persistencia y presupuesto incluyendo preparación. |
| TargetJsonProcessorTest, CredentialDecryptionTest | Plantilla/path estrictos, Unicode/escapes/NUL, redacción/collision, AES real con material sintético y fallos sanitizados. |
| TargetDestinationPolicyTest, ExecutionLimitsAndGateTest | Orígenes, IPv4/IPv6/especiales/mixed DNS/interfaces/demo, DNS finito, defaults/caps/cuota, separación y exclusión simultánea. |
| HttpTargetInvocationAdapterTest + ControlledTargetServer/ControlledHttpsServer | HTTP local real controlado, auth/headers/body, IP fijada/rebinding, redirects/retries, errores HTTP/JSON/path/tamaño/UTF-8/headers, deadlines, TLS y ausencia de secretos en resultados/logs. |
| SecurityTestExecutionTransactionsTest, TestExecutionControllerTest | Atomicidad/rollback/cierre condicional/recuperación, consulta scoped, precedencia de UUID/404/500, DTO sin evidencia. |
| SecurityTestMigrationIntegrationTest | PostgreSQL dedicado, seed exacto/estable, V1–V3 → V4, relaciones y datos históricos conservados, guard transaccional. |
| SecurityTestExecutionPostgresIntegrationTest | REST → worker → HTTP local → PostgreSQL → polling; plan visible desde otra conexión antes de red, evidencia/Unicode/redacción, límites, snapshot, recuperación y commits/rollback reales. |

Comprobaciones realizadas: lectura manual de código/spec/documentación/diffs, inventario Git y consulta local de las firmas HttpClient/core y del BOM existente. Se consultaron status e instrucciones apply de OpenSpec con telemetría desactivada. El BOM Spring Boot existente gestiona httpclient5; el POM añade solamente esa dependencia sin versión explícita.

**No ejecutado:** compilación, tests unitarios/integración, type-checkers/linters/formatters/análisis estático, Maven/npm/Docker, servidores fixture/aplicación, conexiones PostgreSQL, migraciones, despliegues ni llamadas a Internet. No se instalaron/resolvieron dependencias ni se realizaron operaciones Git de escritura. La validación strict consignada anteriormente no se volvió a ejecutar y no constituye validación de este código.

### Comandos pendientes para el usuario

Desde `backend/`, con Java 21. Estos comandos compilan/ejecutan pruebas; se dejan preparados y no se ejecutaron:

```powershell
.\mvnw.cmd "-Dtest=SecurityTestExecutionDomainTest,ExecuteSecurityTestsUseCaseTest,SecurityTestExecutionTransactionsTest,TestExecutionControllerTest,TargetJsonProcessorTest,CredentialDecryptionTest,TargetDestinationPolicyTest,HttpTargetInvocationAdapterTest,ExecutionLimitsAndGateTest" test
```

Para integración, configurar externamente `SCAN_TEST_DB_URL`, `SCAN_TEST_DB_USERNAME` y `SCAN_TEST_DB_PASSWORD` apuntando exclusivamente a PostgreSQL 16 dedicado. Las fixtures crean/eliminan esquemas aleatorios y ejecutan Flyway y servidores HTTP locales. Sin esas variables las pruebas PostgreSQL se omiten:

```powershell
.\mvnw.cmd "-Dtest=SecurityTestMigrationIntegrationTest,SecurityTestExecutionPostgresIntegrationTest,ScanPostgresIntegrationTest,ScanMigrationIntegrationTest" test
```

Después de las comprobaciones dirigidas, regresión completa opcional solicitada por el usuario: `.\mvnw.cmd test`. No se afirma ningún resultado hasta ejecutarlas.

### Riesgos y decisiones pendientes

- Compilación, compatibilidad en ejecución con HttpClient/JPA y garantías PostgreSQL/Flyway pendientes de comprobar mediante los comandos anteriores.
- V4 rechaza explícitamente test_cases/test_executions preexistentes; una base incompatible necesita un diseño de conversión aprobado, sin borrar ni inventar históricos.
- La redacción exacta no detecta secretos arbitrarios ni variantes codificadas. Retención/borrado y autenticación de usuarios conservan su estado futuro aprobado; no bloquean este incremento local ni se declaran resueltos.
- Sin nuevas decisiones de diseño bloqueantes identificadas durante la revisión manual. Los artefactos de planificación ya estaban presentes al iniciar la implementación; se conserva su contenido aprobado.

### Archivos creados (44)

- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/TestExecutionController.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/execution/securitytest/ExecutionLimits.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/BoundedDnsResolver.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/HttpTargetInvocationAdapter.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/InvocationFailure.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/InvocationGate.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/LimitedResponseParserFactory.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/TargetDestinationPolicy.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/execution/PostgresSecurityTestCatalogAdapter.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/execution/PostgresTestExecutionRepositoryAdapter.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/execution/SecurityTestJpaEntity.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/execution/TestExecutionJpaEntity.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/validation/target/TargetJsonProcessor.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/ExecuteSecurityTestsUseCase.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/ExecutionMetadata.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/ExecutionPlan.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/InvocationResult.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/ListTestExecutionsUseCase.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/ScanBudget.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/SecurityTestCatalogPort.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/TargetInvocationPort.java`
- `backend/src/main/java/com/llmscannerseguridad/application/execution/TestExecutionRepositoryPort.java`
- `backend/src/main/java/com/llmscannerseguridad/application/target/CredentialDecryptionPort.java`
- `backend/src/main/java/com/llmscannerseguridad/application/target/EphemeralCredential.java`
- `backend/src/main/java/com/llmscannerseguridad/config/SecurityTestExecutionConfiguration.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/execution/ExecutionError.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/execution/ExecutionStatus.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/execution/TestExecution.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/securitytest/SecurityCategory.java`
- `backend/src/main/java/com/llmscannerseguridad/domain/securitytest/SecurityTest.java`
- `backend/src/main/resources/db/migration/V4__incorporar_ejecuciones_pruebas_seguridad.sql`
- `backend/src/test/java/com/llmscannerseguridad/adapters/in/rest/scan/TestExecutionControllerTest.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/out/crypto/target/CredentialDecryptionTest.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/ControlledHttpsServer.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/ControlledTargetServer.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/ExecutionLimitsAndGateTest.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/HttpTargetInvocationAdapterTest.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/TargetDestinationPolicyTest.java`
- `backend/src/test/java/com/llmscannerseguridad/adapters/out/validation/target/TargetJsonProcessorTest.java`
- `backend/src/test/java/com/llmscannerseguridad/application/execution/ExecuteSecurityTestsUseCaseTest.java`
- `backend/src/test/java/com/llmscannerseguridad/config/SecurityTestExecutionTransactionsTest.java`
- `backend/src/test/java/com/llmscannerseguridad/domain/execution/SecurityTestExecutionDomainTest.java`
- `backend/src/test/java/com/llmscannerseguridad/integration/SecurityTestExecutionPostgresIntegrationTest.java`
- `backend/src/test/java/com/llmscannerseguridad/integration/SecurityTestMigrationIntegrationTest.java`

### Archivos modificados (23)

- `backend/pom.xml`
- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/ScanController.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/in/rest/scan/ScanExceptionHandler.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/crypto/target/AesGcmCredentialEncryptionAdapter.java`
- `backend/src/main/java/com/llmscannerseguridad/adapters/out/validation/target/JsonTargetConfigurationValidator.java`
- `backend/src/main/java/com/llmscannerseguridad/config/ScanConfiguration.java`
- `backend/src/main/java/com/llmscannerseguridad/config/TargetConfigurationBeans.java`
- `backend/src/main/java/com/llmscannerseguridad/config/TransactionalScanLifecycle.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/llmscannerseguridad/integration/ScanPostgresIntegrationTest.java`
- `backend/src/test/java/com/llmscannerseguridad/integration/ScanTestDatabase.java`
- `docs/02-arquitectura/arquitectura-general.md`
- `docs/02-arquitectura/decisiones/ADR-002-autenticacion-target-credenciales.md`
- `docs/02-arquitectura/decisiones/ADR-003-ejecucion-asincrona-scans.md`
- `docs/02-arquitectura/decisiones/decisiones-abiertas.md`
- `docs/03-seguridad/modelo-de-amenazas.md`
- `docs/03-seguridad/politica-de-autorizacion.md`
- `docs/04-ia/casos-de-prueba.md`
- `docs/04-ia/estrategia-evaluacion.md`
- `docs/05-api/openapi.yaml`
- `docs/05-api/target-contract.md`
- `docs/06-base-de-datos/modelo-datos.md`
- `openspec/changes/security-test-execution/tasks.md`

## Validación técnica autorizada — 2026-10-10 (histórico)

**BLOCKED para completar la validación:** las variables SCAN_TEST_DB_URL/USERNAME/PASSWORD no están disponibles en el proceso de Codex. La batería general pasó, pero la integración dedicada se omitió y package queda pendiente de esa integración. Este registro actualiza el estado de validación sin cambiar requisitos ni las 29 casillas de implementación/pruebas preparadas.

### Resultados reales

| Comando desde backend con Java 21.0.12.1 | Resultado observado |
| --- | --- |
| `.\mvnw.cmd test` | BUILD SUCCESS, salida 0, 36.338 s. 192 tests contabilizados: 156 ejecutados, 36 omitidos, 0 fallos y 0 errores. Incluye 70 tests nuevos de este change sin dependencia PostgreSQL, todos superados. |
| `.\mvnw.cmd "-Dtest=LlmScannerSeguridadApplicationTests" test` después de corregir aislamiento | BUILD SUCCESS, salida 0, 1.574 s. Un test omitido por ausencia de SCAN_TEST_DB_URL; no arranca contexto ni datasource. |
| `.\mvnw.cmd "-Dtest=SecurityTestMigrationIntegrationTest,SecurityTestExecutionPostgresIntegrationTest" test` | BUILD SUCCESS, salida 0, 1.601 s, pero 9/9 tests omitidos: 7 de ejecución PostgreSQL y 2 de migración. **No acredita integración superada.** |
| `.\mvnw.cmd -DskipTests package` | No ejecutado: no se cumple la condición del usuario de superar primero la integración PostgreSQL dedicada. |

La batería completa compiló fuentes principales y tests con release 21. HttpTargetInvocationAdapterTest (19), TargetDestinationPolicyTest (28), ExecutionLimitsAndGateTest (3), TargetJsonProcessorTest (3), CredentialDecryptionTest (2), ExecuteSecurityTestsUseCaseTest (6), SecurityTestExecutionTransactionsTest (3), TestExecutionControllerTest (3) y SecurityTestExecutionDomainTest (3) suman los 70 tests nuevos superados. Las fixtures HTTP/HTTPS fueron locales y controladas; se ejecutaron los casos de auth, redacción, límites, DNS/SSRF/mixed DNS, conexión fijada, redirects, JSON/path, deadlines y coordinación del plan. Las garantías PostgreSQL de esos flujos siguen pendientes de las pruebas dedicadas omitidas.

El primer intento dentro del sandbox no arrancó Maven Wrapper (NullArray/Cannot start maven from wrapper); se repitió con aprobación fuera del sandbox. No hubo fallos de compilación ni assertions. Las advertencias de APIs deprecadas del cliente y carga dinámica de Mockito no produjeron errores; no se cambiaron dependencias/versiones para eliminarlas. Los mensajes ERROR del caso ScanInfrastructureTest forman parte de sus escenarios de fallos controlados y sus nueve tests pasaron.

### Incidencia de aislamiento y corrección

Durante la primera batería, el test preexistente LlmScannerSeguridadApplicationTests arrancó con la configuración habitual. El log registra PostgreSQL 16.15 en `jdbc:postgresql://localhost:5432/llm_scanner_seguridad`, esquema public inicialmente vacío, y Flyway aplicando V1, V2, V3 y V4 correctamente. **Esa ejecución excedió la autorización limitada a la base dedicada de test y no se presenta como integración autorizada superada.** No se hicieron nuevas operaciones sobre esa base ni se intentó deshacer/eliminar las migraciones.

Se corrigió únicamente el aislamiento necesario para validar este incremento en `backend/src/test/java/com/llmscannerseguridad/LlmScannerSeguridadApplicationTests.java`: el arranque exige SCAN_TEST_DB_URL exactamente igual a la base dedicada indicada por el usuario; datasource y credenciales se toman obligatoriamente de SCAN_TEST_DB_*, sin fallback a configuración habitual. Se volvió a ejecutar solo ese test: quedó omitido de forma segura al faltar la variable. No se modificaron producción, SQL V1–V4, requisitos, catálogo, transporte ni dependencias durante esta validación.

Resultado V1/V2/V3 → V4 **en base dedicada:** pendiente. La aplicación observada de las cuatro migraciones sobre una base inicialmente vacía no comprueba preservación de históricos, guard de incompatibilidad, seed estable entre entornos ni rollback transaccional en la base autorizada. Los nueve tests seleccionados siguen sin evidencia de ejecución.

Los informes Surefire actuales combinan la batería inicial con las dos comprobaciones dirigidas posteriores; su agregado tiene un test de arranque omitido adicional. No se atribuye ese agregado a una segunda ejecución completa de la batería.

### Continuación externa pendiente

En PowerShell externo, con PostgreSQL 16 dedicado ya disponible. Introducir en el prompt de terminal la contraseña local que el usuario ya indicó; no se repite ni se guarda en este documento:

```powershell
Set-Location 'C:\Users\Develop\Desktop\llm-scanner-seguridad\backend'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:PATH = $env:JAVA_HOME + '\bin;' + $env:PATH
$env:SCAN_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test'
$env:SCAN_TEST_DB_USERNAME = 'llm_scanner'
$testDatabasePassword = Read-Host 'Contraseña de PostgreSQL de test' -AsSecureString
$env:SCAN_TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $testDatabasePassword).Password
.\mvnw.cmd "-Dtest=SecurityTestMigrationIntegrationTest,SecurityTestExecutionPostgresIntegrationTest" test
if ($LASTEXITCODE -eq 0) { .\mvnw.cmd -DskipTests package }
```

La integración debe contabilizar nueve tests realmente ejecutados y cero omitidos antes de darla por superada. Las fixtures crean/eliminan solo esquemas aleatorios de test y ejecutan Flyway/HTTP local dentro de la base dedicada. No se crean bases, contenedores ni servicios nuevos como parte de esta continuación. Tras recibir resultados reales podrá completarse este registro; no se inició otro change.

## Corrección del límite acumulado exacto — 2026-10-10 (histórico)

**BLOCKED únicamente en la validación pendiente.** Corrección implementada sin modificar requisitos, migraciones ni la semántica de cierre de las restantes pendientes.

- Evidencia del fallo anterior: el informe Surefire existente de SecurityTestExecutionPostgresIntegrationTest registraba siete tests ejecutados, un fallo y cero errores/omisiones. El fallo estaba en accumulatedEvidenceExhaustionStopsBeforeNextRequestAndBudgetProducesClosedError: dos COMPLETED y dos FAILED/EXECUTION_ABORTED, sin ninguna ejecución con EVIDENCE_LIMIT_EXCEEDED. Ese informe correspondía a una ejecución externa previa, no a una nueva ejecución de Codex.
- Causa exacta: las evidencias de las dos primeras pruebas ocupan 214 y 298 bytes serializados, sumando exactamente el límite configurado de 512. La comprobación `storedBytes >= scanEvidenceBytes` de start() lanzaba un error genérico antes de confirmar RUNNING para la siguiente prueba. T3 cerraba entonces ambas pendientes con EXECUTION_ABORTED, sin haber persistido la causa de agotamiento.
- Corrección en `backend/src/main/java/com/llmscannerseguridad/adapters/out/persistence/execution/PostgresTestExecutionRepositoryAdapter.java`: confirmar el inicio de la ejecución seleccionada; si el presupuesto ya está agotado, confirmar su FAILED/EVIDENCE_LIMIT_EXCEEDED mediante finish(), sin HTTP, latencia ni evidencia inventados, y solo después lanzar el aborto global. T3 conserva ese terminal y las ejecuciones COMPLETED; únicamente las restantes pendientes reciben EXECUTION_ABORTED, como exigen STE-03/STE-10 y D-07. Las transacciones siguen siendo nuevas y cortas, sin red durante ellas.
- Regresión reforzada en `backend/src/test/java/com/llmscannerseguridad/integration/SecurityTestExecutionPostgresIntegrationTest.java`: se conserva la aserción original EVIDENCE_LIMIT_EXCEEDED; se añaden exactamente dos requests, dos COMPLETED con evidencia intacta, un FAILED con la causa específica sin datos de red y un pendiente abortado con startedAt null. No se cambia el test para aceptar un aborto genérico en lugar de la causa específica.
- Comando dirigido ejecutado desde backend con Java 21.0.12.1, fuera del sandbox con aprobación y rechazo explícito de una URL configurada distinta de la base dedicada autorizada: `.\mvnw.cmd "-Dtest=SecurityTestExecutionPostgresIntegrationTest#accumulatedEvidenceExhaustionStopsBeforeNextRequestAndBudgetProducesClosedError" test`. Salida 0 / BUILD SUCCESS / 1.666 s, **un test omitido, cero ejecutados**. No demuestra que la corrección pase en PostgreSQL.
- SCAN_TEST_DB_URL/USERNAME/PASSWORD están ausentes tanto en el proceso como en los ámbitos persistentes User/Machine de Windows. No se reconstruyeron credenciales ni se utilizó la base habitual. No hubo conexiones PostgreSQL en esta comprobación.
- No se ejecutaron después la selección de nueve tests ni package: no se cumplió la condición de superar realmente el test dirigido. No se modificaron las casillas de implementación ni se marcó integración/package como completados.

Continuación desde la terminal externa que ya tenga configuradas las variables de la base dedicada, con Java 21 y ubicación backend:

```powershell
.\mvnw.cmd "-Dtest=SecurityTestExecutionPostgresIntegrationTest#accumulatedEvidenceExhaustionStopsBeforeNextRequestAndBudgetProducesClosedError" test
# Solo si el anterior ejecuta y supera un test, sin omitirlo:
.\mvnw.cmd "-Dtest=SecurityTestMigrationIntegrationTest,SecurityTestExecutionPostgresIntegrationTest" test
# Solo si los nueve tests anteriores pasan, sin omisiones:
.\mvnw.cmd -DskipTests package
```

Archivos modificados en esta corrección: los dos anteriores y este tasks.md. Sin otros cambios de alcance ni otro change iniciado.

## Validación externa final comunicada por el usuario — 2026-10-10

**READY FOR REVIEW — 29/29 tareas completadas, ninguna pendiente.** El usuario comunica expresamente que completó externamente las validaciones pendientes después de la corrección del límite acumulado de evidencia. Esta es la fuente de los resultados siguientes; no se atribuyen a nuevas ejecuciones de Codex ni se inventan logs, duraciones o recuentos adicionales.

| Validación | Evidencia comunicada por el usuario | Estado |
| --- | --- | --- |
| Test dirigido accumulatedEvidenceExhaustionStopsBeforeNextRequestAndBudgetProducesClosedError | BUILD SUCCESS; 0 fallos, 0 errores, 0 omitidos. | SUPERADA |
| SecurityTestMigrationIntegrationTest + SecurityTestExecutionPostgresIntegrationTest | 9 tests ejecutados; 0 fallos, 0 errores, 0 omitidos; BUILD SUCCESS. | SUPERADA |
| Evolución V1/V2/V3 → V4 | Validada contra la base dedicada de test, confirmada expresamente por el usuario. | SUPERADA |
| Package con tests omitidos mediante -DskipTests | BUILD SUCCESS. | SUPERADA |

La batería general previamente ejecutada por Codex conserva su resultado histórico: BUILD SUCCESS, 192 tests contabilizados, 156 ejecutados y 36 omitidos, sin fallos ni errores. No se afirma una nueva ejecución completa posterior a la corrección. La validación externa final acredita el test dirigido corregido, los nueve tests PostgreSQL sin omisiones, la evolución a V4 en la base dedicada y el package; resuelve los bloqueos de validación anteriores.

Las 29 casillas permanecen completadas sin cambiar el contenido de las tareas, requisitos o alcance. En esta actualización se modificó exclusivamente este tasks.md: no se modificó código, no se ejecutaron tests/builds/migraciones ni comandos de validación y no se inició otro change.

## Correcciones del review F1/F2 — 2026-10-10

**READY FOR RE-REVIEW — 29/29 tareas completadas.** Correcciones limitadas a la precedencia de errores de invocación y a la comprobación actualizada de interfaces locales. Sin cambios de requisitos, decisiones D-01..D-11, migraciones V1..V4, modelos, catálogo, endpoints, dependencias ni política de continuación/aborto.

### F1 — Precedencia de errores y timeout individual

- Causa: failure() convertía cualquier error distinto de DNS_RESOLUTION_FAILED en TARGET_TIMEOUT si el deadline individual vencía al traducirlo, incluidos motivos globales y límites ya detectados. El worker podía continuar al perder esa clasificación global.
- Corrección en `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/HttpTargetInvocationAdapter.java`: aplicar esa traducción únicamente a TARGET_CONNECTION_FAILED; conservar los motivos específicos ya clasificados y mantener la comprobación prioritaria del presupuesto global de Scan.
- Regresiones en el nuevo `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/HttpTargetFailurePrecedenceTest.java`: deadline individual vencido con los 19 motivos distintos de TARGET_CONNECTION_FAILED, incluido EVIDENCE_LIMIT_EXCEEDED sin evidencia excedente; transporte antes/después del deadline; precedencia global para los 20 códigos; aborto del worker con TARGET_ADDRESS_BLOCKED, CREDENTIAL_UNAVAILABLE, TARGET_CONFIGURATION_INVALID y EXECUTION_INTERNAL_ERROR conservados, sin iniciar otras ejecuciones. Los deadlines se fuerzan de manera determinista, sin carreras temporales.

### F2 — Interfaces locales vigentes en cada intento

- Causa: el constructor de TargetDestinationPolicy enumeraba las interfaces una sola vez, dejando fuera las direcciones adquiridas después del arranque.
- Corrección en `backend/src/main/java/com/llmscannerseguridad/adapters/out/http/target/TargetDestinationPolicy.java`: proveedor Supplier interno al adaptador, con enumeración del sistema en cada select(); una instantánea coherente para comprobar todas las respuestas DNS del intento, sin caché entre intentos. Error de enumeración produce TARGET_ADDRESS_BLOCKED sanitizado, también en demo. Se conservan CIDRs, autorización del origen, excepción demo, rechazo de DNS mixto y selección de IP fijada.
- Regresiones en `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/TargetDestinationPolicyTest.java`: conjunto A seguido de A+B para IPv4 e IPv6, nueva B bloqueada y rechazo mixto; destino permitido restante; fallo cerrado sin causa interna filtrada; demo condicionado a perfil, flag y origen exacto. Se conservan las pruebas anteriores de rangos especiales.

### Observación TLS — Cobertura positiva añadida solo en tests

- Archivos: `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/ControlledHttpsServer.java` y `backend/src/test/java/com/llmscannerseguridad/adapters/out/http/target/HttpTargetInvocationAdapterTest.java`.
- Certificado sintético efímero con SAN DNS correspondiente, trust limitado a la fixture y verificación de hostname activa. El resolvedor controlado devuelve únicamente loopback mediante la excepción demo explícita; se comprueban una sola resolución, conexión efectiva a la fixture, HTTP 200, texto extraído y valores originales de Host y SNI. Se mantienen los casos negativos de certificado no confiable/hostname incorrecto y el timeout de handshake. Sin cambios TLS de producción ni conexiones a Internet.

### Validaciones ejecutadas por Codex

Desde backend, con Java 21.0.12.1. Todos los comandos finales terminaron con salida 0 / BUILD SUCCESS:

| Comando | Resultado real |
| --- | --- |
| `.\mvnw.cmd "-Dtest=HttpTargetFailurePrecedenceTest" test` | 44 tests; 0 fallos, 0 errores, 0 omitidos; 3.142 s. |
| `.\mvnw.cmd "-Dtest=TargetDestinationPolicyTest" test` | 31 tests; 0 fallos, 0 errores, 0 omitidos; 2.640 s. |
| `.\mvnw.cmd "-Dtest=HttpTargetInvocationAdapterTest" test` | 20 tests HTTP/HTTPS; 0 fallos, 0 errores, 0 omitidos; 22.851 s. Incluye el nuevo caso positivo Host/SNI. |
| `.\mvnw.cmd "-Dtest=HttpTargetFailurePrecedenceTest,HttpTargetInvocationAdapterTest,TargetDestinationPolicyTest,ExecutionLimitsAndGateTest,TargetJsonProcessorTest,CredentialDecryptionTest,ExecuteSecurityTestsUseCaseTest,SecurityTestExecutionTransactionsTest,TestExecutionControllerTest,SecurityTestExecutionDomainTest" test` | Batería relevante: 118 tests; 0 fallos, 0 errores, 0 omitidos; 26.315 s. Incluye nuevamente los tests dirigidos y HTTP/HTTPS; no se suman como tests distintos. |
| `.\mvnw.cmd "-Dtest=SecurityTestMigrationIntegrationTest,SecurityTestExecutionPostgresIntegrationTest" test` | 9 tests PostgreSQL (7 de ejecución y 2 de migración); 0 fallos, 0 errores, 0 omitidos; 30.632 s. |
| `.\mvnw.cmd -DskipTests package` | JAR generado y reempaquetado; tests omitidos expresamente por -DskipTests; 2.533 s. |

PostgreSQL se ejecutó exclusivamente en `jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test`, con SCAN_TEST_DB_* cargadas desde el .env autorizado sin mostrar ni registrar credenciales, y rechazo previo de cualquier otra URL. Las fixtures usaron sus esquemas aislados habituales. La evolución V1/V2/V3 → V4, seed estable, conservación de relaciones/históricos compatibles y rechazo transaccional de históricos incompatibles quedaron nuevamente validados. Los mensajes de rollback de Flyway pertenecen a los casos negativos esperados, que pasaron.

El primer intento dirigido de F1 dentro del sandbox quedó detenido tras iniciar Mockito y se interrumpió; no se contabiliza como validación superada. Se repitió fuera del sandbox con aprobación y terminó correctamente, al igual que la batería con Mockito, HTTP/HTTPS y PostgreSQL. No se cambiaron herramientas/dependencias para resolver esa restricción del entorno. Los informes finales están en backend/target/surefire-reports; 127 tests distintos superados entre batería relevante e integración. No se ejecutó de nuevo la batería general completa ni se inició otro change.

Archivos de esta corrección: los dos de producción, los cuatro archivos de tests indicados y este tasks.md. No quedan validaciones autorizadas pendientes para F1/F2.
