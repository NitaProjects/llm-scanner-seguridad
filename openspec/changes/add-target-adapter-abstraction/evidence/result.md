# add-target-adapter-abstraction — evidencia del 2026-11-10

**Implementación PASS. T7 PASS. READY FOR REVIEW; 11/11 tareas completas.**
T7 ejecutado por el operador en su terminal preparada y contrastado con Surefire local:
2 passed / 0 failed / 0 skipped. No se implementa la epic `generalize-target-integration`.

## Implementación y archivos

- Nuevos `application/execution/TargetAdapter.java`, `NormalizedInteraction.java` y
  `LegacyInvocationResultProjection.java`: contrato mínimo, siete valores reversibles,
  nulabilidad intacta y representaciones textuales sin cuerpos.
- Nuevo `adapters/out/execution/securitytest/LegacyRestTargetAdapter.java`: una sola
  delegación legacy, sin transporte, retries, sanitización, reloj o cierre adicional.
- Modificados `ExecuteSecurityTestsUseCase.java`, `ScanConfiguration.java` y
  `SecurityTestExecutionConfiguration.java`: ruta real por TargetAdapter, proyección en
  el try existente y finish fuera del catch. Sin constructor legacy de bypass.
- Nuevos tests `LegacyInvocationResultProjectionTest`, `LegacyRestTargetAdapterTest`
  y `TargetAdapterWiringTest`; ampliado `ExecuteSecurityTestsUseCaseTest` y adaptada
  construcción en `HttpTargetFailurePrecedenceTest`.
- `HttpTargetInvocationAdapterTest` conserva sus expectativas; sus invocaciones pasan
  ahora por el bridge real y la proyección. El transporte productivo permanece intacto.
- `EvaluationEnginePostgresIntegrationTest` añade equivalencia de pipeline y derivados;
  los dos casos T7 fueron ejecutados posteriormente por el operador con PostgreSQL de test.
- `docs/02-arquitectura/arquitectura-general.md`: únicamente descripción de la cadena.
  `tasks.md` y evidencia actualizados; no cambios de versiones/dependencias/lockfiles.

## Matriz TAB/T1–T8

| Prueba | Estado | Evidencia |
| --- | --- | --- |
| T1 / TAB-01,03 | PASS | Worker con TargetAdapter fake, orden prepare/start/interact/finish/evaluación, ausencia de bypass y cero llamadas por plan fallido/budget agotado |
| T2 / TAB-02 | PASS | 194 tests de proyección: 192 combinaciones round-trip y dos tests null/diagnóstico; todos los ExecutionError con metadata/global, flags independientes, vacíos/Unicode/NUL y sin defaults |
| T3 / TAB-01,03 | PASS | 3 tests de bridge: mismos target/prompt/budget, exactamente una llamada, excepción/Error/interrupción/null preservados |
| T4 / TAB-03 | PASS | Expectativas previas conservadas, continuidad individual/aborto global, RuntimeException interna, finish fallido sin reenvío y precedencia |
| T5 / TAB-01,03,04 | PASS | Configuraciones Spring reales, @Primary fake seleccionado, cuatro interacciones y un HTTP bean; delegado cerrado exactamente una vez |
| T6 / TAB-02,03 | PASS | Suite HTTP existente a través del bridge: JSON/headers/auth/responsePath/redacción, errores, límites, DNS/SSRF/TLS y limpieza con fixtures locales |
| T7 / TAB-04 | PASS | Dos casos dirigidos con PostgreSQL dedicada: cuatro ejecuciones/evaluaciones, Findings y score/report, casos vulnerables/seguros/inconclusos; 2 passed, 0 failed, 0 skipped |
| T8 / TAB-04 | Ejecutado, alcance parcial | Suite backend completa invocada; tests omitidos separados de passed |

## Resultado real de la suite

Comando desde backend: `.\mvnw.cmd -o test`, JDK/javac 21.0.12.1 verificados antes de
Maven. Ejecución final: exit 0, BUILD SUCCESS, 34.267 s. **605 tests descubiertos,
522 passed, 0 failed/errors, 83 skipped.** No se suman ejecuciones repetidas.
Resumen saneado por clase/caso: [backend-tests.json](backend-tests.json).
No se versionan XML completos con propiedades de sistema ni logs crudos de ejecución.

Omitidos: EvaluationEnginePostgresIntegrationTest (17), EvaluationMigrationIntegrationTest
(3), ProjectPostgresIntegrationTest (7), ReportingMigrationIntegrationTest (3),
ReportingPostgresIntegrationTest (11), ScanMigrationIntegrationTest (2),
ScanPostgresIntegrationTest (13), ScoringEnginePostgresIntegrationTest (9),
ScoringMigrationIntegrationTest (3), SecurityTestExecutionPostgresIntegrationTest (7),
SecurityTestMigrationIntegrationTest (2), TargetPostgresIntegrationTest (5),
LlmScannerSeguridadApplicationTests (1). No cuentan como PASS.

La primera ejecución dirigida dentro del sandbox se interrumpió por bloqueo durante
auto-attach Mockito. Reejecutada con permiso fuera del sandbox: detectó un error de la
fixture nueva (Instant con nanosegundos); se corrigió únicamente la fixture a microsegundos.
Las ejecuciones completas posteriores pasaron sin fallos, con las omisiones indicadas.

## Configuración de test y bloqueo inicial de herencia — histórico

PostgreSQL dedicada confirmada por el operador: `localhost:5432`, BD
`llm_scanner_seguridad_test`, usuario `llm_scanner`. No se afirma que la BD no exista;
faltan las variables en **el ejecutor utilizado**. No se leyó ni se transfirió contraseña.

Para T7 y el resto de integración scan/evaluation/scoring/report, usar una terminal
preparada con la contraseña de test ya configurada por vía segura:

```powershell
$env:SCAN_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test'
$env:SCAN_TEST_DB_USERNAME = 'llm_scanner'
# SCAN_TEST_DB_PASSWORD debe existir en esta terminal, sin imprimirla ni escribirla en archivos.
# Con JAVA_HOME/PATH apuntando a JDK 21, desde backend:
java -version
javac -version
.\mvnw.cmd -o test
```

Para no omitir tampoco las suites independientes Project/Target se requieren además
`PROJECT_TEST_DB_URL`, `PROJECT_TEST_DB_USERNAME`, `PROJECT_TEST_DB_PASSWORD`,
`TARGET_TEST_DB_URL`, `TARGET_TEST_DB_USERNAME`, `TARGET_TEST_DB_PASSWORD` y
`TARGET_POSTGRES_TESTS_ENABLED=true`, todos referidos exclusivamente a esa BD dedicada,
con credenciales suministradas externamente. El helper ScanTestDatabase valida el nombre
exacto y major PostgreSQL 16, y aísla sus fixtures en esquemas aleatorios.

No se encontró application-test.* ni Compose de test separado. No se abrió el Compose
local con sus credenciales; no se creó infraestructura ni se ejecutó migración, consulta
o limpieza en demo/producción. No se invocaron proveedores reales ni targets externos.

## Revisión y cierre

`openspec validate add-target-adapter-abstraction --strict`: **PASS**.
No hay desviaciones de diseño implementadas ni funcionalidades futuras. El operador
resolvió la herencia del entorno ejecutando T7 en su terminal preparada. Su primer intento
omitió ambos casos (0 passed, 2 skipped), sin acreditar T7. La ejecución posterior terminó
BUILD SUCCESS a las 17:46:34+01:00: **2 passed, 0 failed, 0 skipped**, 11.212 s de tests y
13.059 s Maven. El XML Surefire local confirma exactamente los dos métodos seleccionados.
[Resumen saneado de T7](t7-postgres.json).

El pipeline persistido de T7 queda acreditado con target/evaluator fakes, cuatro ejecuciones
y evaluaciones, Findings, score y report-v1 para casos vulnerables/seguros/inconclusos.
No se reejecutó desde este agente ni se accedió a contraseñas o a la BD de demo/producción.
Las 83 omisiones de la suite completa previa conservan su alcance histórico; la ejecución
dirigida no convierte las demás integraciones omitidas en PASS ni representa una nueva
suite completa. **11/11 tareas completas; READY FOR REVIEW.**
Sin cambios de código HTTP, dominio/JPA, migraciones, APIs, evaluación/scoring/report,
frontend, demo, AWS o configuración operativa. Sin commit/push ni archivo del change.
