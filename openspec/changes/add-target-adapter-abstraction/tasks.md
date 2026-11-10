Estado: **READY FOR REVIEW** (2026-11-10). **11/11 tareas completas**.
Aprobación e implementación autorizadas expresamente por el operador; no se implementa
`generalize-target-integration`. T7 ejecutado por el operador en su terminal preparada
y contrastado con Surefire local: **2 passed / 0 failed / 0 skipped**. La suite completa
anterior conserva su alcance histórico: 522 passed / 0 failed / 83 skipped, sin convertir
los omitidos restantes en PASS.

## 1. Contrato mínimo y proyección en memoria

- [x] 1.1 Tras aprobación explícita, crear TargetAdapter con únicamente interact(TargetConfiguration, String, ScanBudget) y NormalizedInteraction/LegacyCompatibility según D0-01/02 (TAB-01/02); verificar por revisión de firmas ausencia de metadata futura, validaciones nuevas y dependencias Spring/HTTP/JPA.
- [x] 1.2 Crear LegacyInvocationResultProjection pura fromLegacy/toLegacy y toString seguros de ambos records, sin modificar InvocationResult (TAB-02); verificar T2: igualdad de los siete campos para éxitos, todos los errores, metadata en fallos, nulls, latencia 0, Unicode/NUL y flags independientes; comprobar que toString no expone textos.

## 2. Bridge y uso real

- [x] 2.1 Crear LegacyRestTargetAdapter delegando una sola vez en TargetInvocationPort y proyectando el resultado (TAB-01/03); verificar T3: mismos objetos target/budget, mismo prompt, excepción e interrupción intactas, sin red propia, retries ni cierre del delegado.
- [x] 2.2 Sustituir TargetInvocationPort por TargetAdapter en los dos constructores/campo del use case y proyectar a InvocationResult en el try actual (TAB-01..03); verificar T1/T4: prepare/start→interact→finish→evaluación, presupuesto agotado sin interact, RuntimeException convertida como hoy y finish fuera del catch. No añadir constructor de bypass legacy.
- [x] 2.3 Añadir bean TargetAdapter con delegado TargetInvocationPort por interfaz y actualizar ScanConfiguration.scanWorkPort, preservando beans/destroyMethod existentes (TAB-01/03/04); verificar T5 con fake @Primary que la cadena real pasa por bridge, alcanza solo el fake y no crea/cierra otro cliente HTTP.

## 3. Compatibilidad y seguridad verificables

- [x] 3.1 Adaptar únicamente inyección/construcción de ExecuteSecurityTestsUseCaseTest y HttpTargetFailurePrecedenceTest, conservando sus expectativas (TAB-03); verificar T4 con cuatro prompts/finish, alcance local/global, persistencia fallida, precedencia de presupuesto e interrupciones, sin rebajar aserciones.
- [x] 3.2 Añadir la cobertura mínima del bridge sobre fixtures de transporte existentes sin modificar HTTP productivo (TAB-02/03); verificar T6: mismo JSON enviado/extracción responsePath/texto saneado/flags, autorización antes de descifrado, SSRF/DNS/TLS, límites/timeout y limpieza. Todo destino de fixture será local controlado o mock, nunca externo.
- [x] 3.3 Reutilizar/ampliar fixtures de integración existentes para probar cuatro ejecuciones/evaluaciones y mismos Findings, score y report-v1 a través del wiring real (TAB-04); verificar T7 con evaluador fake, casos inconclusos y con Finding, versiones/textos/relaciones intactos y cero servicios externos; no cambiar lógica de evaluación/scoring/reporting.
- [x] 3.4 Con petición explícita de comprobación, ejecutar las suites actuales pertinentes de ejecución/transporte/evaluación/scoring/reporting y los tests nuevos (TAB-04); verificar T8 con resultados reales separados de suites omitidas. Integraciones PostgreSQL solo con entorno dedicado autorizado existente; no habilitar proveedor real, descargar dependencias ni crear infraestructura para probar D0.

## 4. Revisión final del refactor

- [x] 4.1 Revisar el diff permitido y actualizar solo la descripción de la cadena de invocación en arquitectura-general si la implementación la deja desactualizada; verificar ausencia de cambios frontend/API pública/BD/JPA/migraciones/perfiles/score/report productivos y ausencia de requisitos heredados de la epic (TAB-01..04).
- [x] 4.2 Registrar evidencia de equivalencia y validaciones pendientes en este change, sin afirmar suites pasadas si no fueron ejecutadas; verificar trazabilidad TAB-01..04↔T1..T8 y confirmar que ningún paso requiere implementar directamente generalize-target-integration.

## Evidencia inicial de implementación y pruebas — histórica, anterior a T7

- Tipos mínimos, proyección reversible y bridge implementados según D0-01..05. Ambos
  constructores del worker reciben únicamente TargetAdapter; no existe dependencia ni
  constructor alternativo TargetInvocationPort. HTTP productivo y sus recursos intactos.
- **T1–T6 PASS**: secuencia real, cero llamadas si prepare/budget impiden envío,
  round-trip de siete campos (192 combinaciones más dos casos diagnósticos/null), errores
  y metadata/global, flags independientes, Unicode/NUL/vacíos, misma excepción e
  interrupción, precedencia y cierre. Wiring Spring usa las dos configuraciones reales,
  fake @Primary y exactamente un cierre del delegado. La suite HTTP existente atraviesa
  el bridge sin rebajar expectativas ni cambiar el transporte productivo.
- **T7 NOT_RUN / BLOCKED_BY_ENV**: ampliada EvaluationEnginePostgresIntegrationTest para
  comprobar bridge y cuatro ejecuciones/evaluaciones, Findings/confidence/severity,
  scoring-v1 y report-v1, casos vulnerables/seguros/inconclusos y relaciones persistidas.
  La cobertura compila, pero no se cuenta como PASS porque su condición de entorno omite
  toda la clase. Por eso 3.3 permanece abierta.
- **T8 ejecutado**: `.\mvnw.cmd -o test`, Java/javac 21.0.12.1, exit 0 / BUILD SUCCESS.
  **605 tests descubiertos: 522 passed, 0 failed/errors y 83 skipped**. Estos últimos
  no son PASS; detalle de clases/casos en [backend-tests.json](evidence/backend-tests.json).
- El operador confirma PostgreSQL dedicada `localhost:5432/llm_scanner_seguridad_test`,
  usuario `llm_scanner`. SCAN_TEST_DB_URL/USERNAME/PASSWORD están ausentes en este
  ejecutor; la contraseña configurada en otra terminal no se hereda. Se inspeccionaron
  fixtures/configuración: no existe application-test.* ni Compose de test dedicado;
  las suites usan variables externas y esquemas aleatorios. No se leyó contraseña ni
  se probó con credenciales por defecto, BD de demo o producción. Sin infraestructura,
  migraciones de test, Scans reales o llamadas a proveedores en esta ejecución.
- La ejecución dirigida inicial se interrumpió al quedar bloqueado el agente Mockito
  dentro del sandbox. Fuera del sandbox mostró un fallo de la nueva fixture de wiring
  por precisión temporal; corregida a microsegundos sin tocar dominio/producto.
  La suite completa posterior y la final terminan correctamente; no se ocultan incidencias
  ni se suman sus resultados repetidos como tests distintos.
- Único ajuste técnico documental fuera del change: descripción de la cadena de
  invocación en arquitectura-general.md. Sin cambios de API/DTOs, frontend, BD/JPA,
  migraciones, perfiles, dependencias, prompts, evaluación/scoring/report productivos,
  demo o AWS; sin commit/push.
- `openspec validate add-target-adapter-abstraction --strict`: **PASS**. El change no
  se archiva ni se declara regresión funcional completa mientras T7 siga pendiente.

Configuración exacta para completar la validación PostgreSQL y alcance de las omisiones:
[resultado](evidence/result.md). El bloqueo inicial de T7/entorno queda resuelto por la
ejecución dirigida siguiente; no se reescribe el resultado histórico de la suite completa.

## Cierre T7 PostgreSQL — 2026-11-10, 17:46 Europe/Madrid

- Operador: ejecución en la terminal con SCAN_TEST_DB_* preparada para la BD dedicada
  `localhost:5432/llm_scanner_seguridad_test`; no se comparte ni registra contraseña.
- Comando dirigido exclusivamente a `fourEvaluationsFindingsMetadataEndpointsAndIdempotenceAreDurable`
  y `clearSafeIsolatedLlmAndEveryComponentFailureCloseCoherently` de
  EvaluationEnginePostgresIntegrationTest. Resultado: **2 passed / 0 failed / 0 skipped**, 
  BUILD SUCCESS; Maven 13.059 s, tests 11.212 s.
- Resultado comunicado por el operador y comprobado contra el XML Surefire local:
  nombres exactos de ambos casos, tests=2, failures=0, errors=0, skipped=0. Evidencia
  saneada: [t7-postgres.json](evidence/t7-postgres.json). No se conserva XML completo ni
  logs de contexto con propiedades o datos privados dentro de la evidencia versionada.
- T7 acredita wiring real por el bridge, cuatro ejecuciones/evaluaciones y sus resultados
  persistidos: Findings/severity/confidence, score y report-v1, escenarios vulnerables,
  seguros e inconclusos, relaciones e idempotencia; target/evaluator fakes sin llamadas
  externas reales. No hubo código adicional ni cambios en demo/producción/AWS.
- **11/11 tareas completas.** Las 83 omisiones de la ejecución completa previa siguen
  registradas como históricas; estos dos PASS no acreditan las demás integraciones
  omitidas ni equivalen a una nueva ejecución de toda la suite.
- `openspec validate add-target-adapter-abstraction --strict`: **PASS**. Estado final:
  **READY FOR REVIEW**, sin archivo del change, commit/push ni despliegue.
