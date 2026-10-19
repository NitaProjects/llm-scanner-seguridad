Estado actual de la corrección de exportPolicy: **READY FOR IMPLEMENTATION**, todavía no aplicada. Esta revisión solo modifica OpenSpec y no reanuda operaciones. Las tareas 0.x diseñan la corrección mínima previa; las tareas operativas y sus registros históricos se conservan sin acreditar un Scan real. No iniciar servicios ni consumir OpenAI por el hecho de que estos artefactos estén completos.

## 0. Corrección previa del contrato exportPolicy

- [ ] 0.1 Sustituir exclusivamente const:false por type:boolean en enabled del schema local evaluation-scenario-v1, documentando que true no autoriza por sí solo; verificar diff limitado, schemaVersion=1, required/provider/estructura y restantes restricciones intactas (REV-11; D-08). Requiere petición posterior de implementación.
- [ ] 0.2 Ampliar ManifestTest con instancias de ambas plantillas ligadas: aceptar false/true; rechazar tipos no booleanos, enabled ausente, provider incorrecto/ausente y campos extra; verificar regresiones existentes de placeholders/binding/fecha/entries/oracle y que ambas plantillas versionadas siguen false sin alterar corpus ni manifests benchmark/ground truth (REV-11; D-08).
- [ ] 0.3 Revisar la cobertura local de EvaluationScenarioTest y completar solo casos ausentes de la combinación de permisos: true aislado no autoriza, cada gate ausente bloquea, manifest false bloquea con otros permisos presentes, todos los gates válidos permiten exportAuthorized y binding discordante no selecciona escenario; verificar resultados con fixtures sin clave/transporte real ni cambios de producción backend (REV-03/REV-11; D-08).
- [ ] 0.4 Actualizar únicamente las secciones afectadas del README demo para distinguir schema booleano, templates false, offline false y copia real autorizada true con todos los gates; verificar que el preflight offline rechaza true aun con schema PASS y conservar el runbook E2E offline y sus evidencias históricas (REV-11; D-08).
- [ ] 0.5 Tras solicitud explícita de las comprobaciones, ejecutar ManifestTest y los tests de gates afectados con transporte simulado; validar copias sintéticas completas false/true contra el schema real y su rechazo de tipos inválidos. Registrar resultados, sin servicios/Scan/OpenAI, instalaciones ni benchmark. Verificar después OpenSpec strict y coherencia documental. Estas comprobaciones de producto no se ejecutan en esta planificación (REV-11).
- [ ] 0.6 Antes de reanudar 2.2/2.3/3.1, comprobar corrección aplicada y resultados de contrato, releer Target/fecha y validar el manifest/binding local vigente; registrar que el bloqueo histórico const:false queda resuelto solo con esa evidencia, manteniendo el resto de preflight y gates. No relanzar servicios ni Scan durante esta revisión (REV-04/REV-11).

## Registro operativo — 2026-10-19

Estado de esta sesión: **BLOCKED en preflight**, sin Scan. El operador declara el change APPROVED / READY FOR IMPLEMENTATION y autoriza servicios locales y un único Scan, con hasta cuatro llamadas, sin retries manuales; declara API key creada, billing/saldo preparado y hard spend limit configurado. Esto no acredita acceso al modelo ni una tarifa o configuración efectiva del gate monetario del scanner.

- Lecturas realizadas: AGENTS raíz/backend, perfiles de agentes, artefactos de este change, README de demo y configuración Java del evaluator. OpenSpec informa schema `spec-driven`, estado `ready`, 0/18 tareas completas antes de esta ejecución.
- Comprobación booleana no secreta del entorno **Process** del ejecutor: `EVALUATOR_API_KEY_PROCESS=ABSENT`. No se ha leído ni mostrado el valor de ninguna clave, ni consultado archivos .env. Un scanner iniciado por este ejecutor no heredaría esa variable. No se ha inspeccionado el entorno de un scanner previamente iniciado; no se infiere su configuración desde otra consola.
- Por instrucción expresa del operador se detiene la preparación operativa y se solicita configurar la variable en la terminal que arrancará el backend. No se inician servicios ni se modifica manifest/binding/exportación.
- Código confirmado: `OpenAiEvaluatorAdapter.ENDPOINT=https://api.openai.com/v1/responses`; proveedor default OPENAI; modelo default `gpt-6-luna` mediante `EVALUATOR_MODEL`, sin comprobar disponibilidad ni seleccionar alternativa. Clave mediante `EVALUATOR_API_KEY`; exportación default false; allowlist default vacía, aprobada exclusivamente `demo-vulnerable-v1`; archivo mediante `EVALUATION_SCENARIO_FILE`, sin default. La ejecución efectiva exige además exportPolicy enabled/OPENAI y binding vigente.
- Límites/defaults confirmados en `EvaluationConfiguration`, `EvaluatorSettings` y `SecurityTestExecutionConfiguration`: cuatro llamadas como máximo, timeout 20000 ms, intervalo mínimo/default 1000 ms, máximo 1024 output tokens por llamada; evidence/input/response/record 16384/24576/16384/65536 bytes; presupuesto temporal del Scan default 300000 ms. Son valores de código, no configuración runtime acreditada. Presupuesto monetario estimado default null; no se inventan precios ni importes.
- ScanId: no existe para esta sesión. Scans lanzados y llamadas al proveedor por esta sesión: 0. No se han realizado peticiones exploratorias a OpenAI ni al target. Acceso al modelo: BLOCKED por preflight, sin evidencia de rechazo del proveedor.
- TestExecutions/EvaluationResults, LLM verdict/confidence, consolidación, Findings, Score y Report: no obtenidos. Las cuatro filas LLM01/LLM02/LLM08/LLM10 permanecen sin observación; no se asignan TP/FN/INCONCLUSIVE ni errores técnicos de evaluación. TN/FP no observables con este perfil. Frontend 9.4 permanece pendiente y sin recorrido en esta sesión.
- Usage/model metadata: `NOT_INSPECTED`, sin Scan propio que consultar. Coste/usage facturado: no consultado; cero llamadas iniciadas por esta sesión no acredita el saldo o consumo de la cuenta. Errores del proveedor: ninguno observado, sin petición.
- Cierre: ningún proceso iniciado ni exportación activada por esta sesión; ningún archivo local de escenario/binding alterado; ninguna clave persistida. No se acredita la configuración ni el cierre de procesos ajenos no inspeccionados. Sin cambios de código ni tests; únicamente este registro documental. Las tareas operativas incompletas conservan sus casillas pendientes.
- Validación ejecutada: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit code 0. La validez documental no acredita la ejecución operativa.

## 1. Autorización y preflight

Preflight posterior al arranque declarado por el operador — 2026-10-19:

- No se vuelve a comprobar la clave del ejecutor ni se lee la clave o el entorno del backend. El operador declara que el backend se arrancó desde la terminal preparada.
- Comprobación local fuera del sandbox: listener 8080, PID 33488, java.exe, iniciado 2026-10-19T21:40:47.996795+02:00. `GET http://localhost:8080/api/projects` devuelve HTTP 200. No se conserva su contenido ni se selecciona Project en esta fase.
- Listeners PostgreSQL 5432 observados (PIDs 31736 y 1116); nombre efectivo de BD/perfil no acreditados por este dato.
- Demo 127.0.0.1:8081 y Angular localhost:4200 no responden; no se observan listeners en esos puertos. GET demo /health no pudo verificar VULNERABLE/version 1.
- Extracción limitada a argumentos no secretos de proceso: spring.profiles.active, server.port, EVALUATOR_PROVIDER/MODEL/EXPORT_ENABLED/ALLOWED_SCENARIOS, EVALUATION_SCENARIO_FILE y TARGET_LOCAL_DEMO_ENABLED/ORIGINS no aparecen como argumentos -- o -D. No se muestra la línea de comandos completa ni se inspecciona el bloque de entorno. Esto no demuestra que falten variables; significa que su valor efectivo no es verificable por esta vía.
- GET /actuator y /actuator/health devuelven 404. Los controladores inspeccionados no ofrecen diagnóstico de configuración. No se añaden endpoints ni instrumentación para esta operación.
- Estado **BLOCKED en preflight**: profile/modelo/gates/escenario/SSRF efectivos no confirmados y demo/frontend ausentes. Hace falta una fuente verificable de configuración no secreta del proceso, por ejemplo argumentos explícitos de arranque bajo control del operador, sin incluir la clave ni otros secretos. No se aceptan los defaults de código como runtime observado.
- Cero Scans y cero llamadas al proveedor iniciados por esta sesión; no se crean procesos, no se alteran gates/manifest/binding, no se detiene el backend del operador. Su capacidad de exportación sigue sin verificar, por lo que no se acredita revocación ni cierre de esa instancia. La validación operativa y frontend 9.4 continúan pendientes.

Reanudación del 2026-10-19: el operador confirma que ha configurado `EVALUATOR_API_KEY` en la terminal que arrancará el backend. La nueva comprobación booleana del proceso ejecutor sigue devolviendo `EVALUATOR_API_KEY_PROCESS=ABSENT`; no se ha leído su valor. La confirmación de otra terminal no acredita herencia en este ejecutor ni presencia en un scanner arrancado. Se mantiene BLOCKED antes de iniciar servicios o lanzar Scan; hace falta arrancar el backend desde la terminal preparada por el operador o proporcionar un ejecutor nuevo que herede la variable. Esta reanudación no inicia llamadas, procesos ni exportación.

- [x] 1.1 Obtener aprobación del change y autorización operativa local/proveedor/modelo/coste; verificar registro explícito no secreto de la sesión y alcance de un único Scan (REV-01; D-01/D-02).
- [x] 1.2 Confirmar acceso de la cuenta a gpt-6-luna y política monetaria: gate existente con tarifa/reserva/presupuesto aprobados o aceptación expresa de operar sin él; verificar modelo y valores no secretos, sin consulta externa automática, llamada exploratoria ni tarifa inventada (REV-05; D-02).
- [x] 1.3 Preparar o reutilizar bajo esa autorización PostgreSQL, demo VULNERABLE, scanner local y Angular; verificar BD de desarrollo, health/version, puertos, API/proxy y excepción SSRF exacta, sin builds/tests, limpieza ni cambios versionados (REV-02; D-03 pasos 2..6).
- [x] 1.4 Suministrar EVALUATOR_API_KEY únicamente al entorno del proceso scanner mediante entrada segura; confirmar presencia sin valor y contrastar configuración efectiva no secreta de proveedor/modelo, gates y límites con D-01, sin volcar entorno ni logs (REV-03/REV-05).

## 2. Preparación del escenario

- [x] 2.1 Identificar Project por id real y Target autorizado preparado con el contrato D-03; verificar por API todos sus campos/updatedAt y ausencia de Scan activo, sin elegir proyectos por nombre ambiguo (REV-02/REV-04).
- [x] 2.2 Preparar copias locales no versionadas de manifest VULNERABLE/binding, con los tres placeholders reales, exportPolicy.enabled=true/provider OPENAI y contexto/corpus intactos; verificar ambos schemas, cuatro code/revision originales y correspondencia exacta con Target releído (REV-03/REV-04).
- [x] 2.3 Confirmar EVALUATION_SCENARIO_FILE, EVALUATOR_EXPORT_ENABLED=true y allowlist solo demo-vulnerable-v1 en el proceso de esta sesión; verificar ruta/configuración declarada y binding vigente antes del lanzamiento (REV-02..REV-05).

## 3. Único Scan y recopilación

- [ ] 3.1 Desde Angular abrir proyecto/target preparado y lanzar una sola vez el Scan; conservar id, instante, 202 y Location observados, verificando correspondencia con la navegación; ante respuesta ambigua detener/reconciliar por histórico sin repetir POST (REV-05/REV-09).
- [x] 3.2 Esperar terminal usando seguimiento Angular existente y tope de observación 360 s; registrar estado/fechas o agotamiento real, sin segundo poller, retry ni pausa simulada (REV-05/REV-06; D-03).
- [x] 3.3 Recopilar por GET scoped Scan, cuatro ejecuciones, cuatro EvaluationResults, Findings, score y report; verificar relaciones, estados/errores/degradación, cuatro categorías y semántica de disponibilidades, registrando faltantes/fallos sin inventarlos (REV-06/REV-07).
- [x] 3.4 Documentar el diagnóstico opcional de metadata: con autorización y disponibilidad, consultar READ ONLY la proyección D-05 filtrada al Scan y registrar modelo solicitado/resuelto, intentos, usage y coste observados, verificando columnas V5 y ausencia de modificaciones/cuerpos/secretos; sin autorización o disponibilidad, registrar NOT_INSPECTED y su motivo. Verificar que no se inventan valores ni se atribuye consumo cero; cualquiera de las dos ramas resuelve esta tarea sin bloquear lanzamiento ni aceptación funcional (REV-10).
- [x] 3.5 Verificar Findings enlazados únicamente a evaluaciones COMPLETED/VULNERABLE; para COMPLETED contrastar scoring-v1, report-v1/es, summary coincidente, interpretaciones/nueve limitaciones y report estable al releer; registrar fallos técnicos como tales (REV-07).

## 4. Comparación y recorrido frontend

- [x] 4.1 Completar cuatro filas de comparación externa con ground-truth-v1 VULNERABLE por code/revision/version/Scan: TP/FN/INCONCLUSIVE según resultado real; verificar matriz general TP/TN/FP/FN/I, TN/FP no observables en este perfil y errores técnicos separados, sin métricas formales ni etiquetas enviadas al proveedor (REV-08).
- [x] 4.2 Revisar en Angular estado, score/Findings/I y report/limitaciones del mismo Scan; si existe un Finding persistido, abrirlo y cotejar contenido/categoría/severidad/confianza/referencias con API; verificar contenido tratado como texto sin cambios visuales (REV-07/REV-09).
- [x] 4.3 Documentar cobertura exacta del criterio original frontend-mvp 9.4; proponer su cierre solo con secuencia íntegra observada y apertura de Finding real, manteniéndola pendiente sin Finding o con otro paso ausente; verificar evidencia de cada paso sin reescribir su requisito (REV-09; D-06).

## 5. Cierre y resultado operativo

- [x] 5.1 Aplicar cierre D-07 incluso ante error/interrupción: deshabilitar exportación efectiva/allowlist, exportPolicy local false y retirar clave del entorno; verificar parada de la instancia propia o reinicio sin clave/exportación, sin borrar BD ni afectar otros procesos (REV-10).
- [x] 5.2 Conservar paquete mínimo no secreto con resultados separados de proveedor, cadena scanner y cobertura 9.4; verificar cuatro respuestas LLM válidas para aceptación completa del proveedor o registrar parcial/fallida, sin exigir cuatro VULNERABLE ni relanzar para conseguir Findings (REV-06..REV-10).
- [x] 5.3 Revisar coherencia final de evidencia/OpenSpec y registrar defectos o contradicciones sin corregir código; identificar únicamente documentación directamente afectada y proponer actualizaciones de estado sustentadas para autorización posterior, sin marcar TFM ni 9.4 completados sin evidencia (REV-01/REV-09/REV-10).

## Último preflight y cierre — 2026-10-19

Este registro sustituye los bloqueos anteriores de configuración no observable, conservándolos como histórico. Resultado actual: **BLOCKED por contradicción entre REV-04/D-03 y el schema de demo**, antes del único Scan. No hay cambio de producto.

### Autorización y configuración comprobadas

El operador reinició el backend y declara la clave exclusivamente en su entorno. No se leyó la API key ni el bloque de entorno; no se volvió a comprobar la variable del ejecutor. El operador autorizó expresamente: «Sí, con el hard spend limit de OpenAI y máximo cuatro llamadas», para operar sin gate monetario estimado interno. No se inventaron precios ni importes. El modelo aprobado solicitado es gpt-6-luna; acceso real no comprobado.

Listener backend 8080, PID 26104, java.exe, iniciado 2026-10-19T22:17:22.484721+02:00. Extracción exclusivamente de argumentos no secretos (sin mostrar línea de comandos completa), y GET /api/projects HTTP200:

| Propiedad | Valor explícito observado |
| --- | --- |
| spring.profiles.active | local |
| server.port | 8080 |
| spring.datasource.url | jdbc:postgresql://localhost:5432/llm_scanner_seguridad |
| TARGET_LOCAL_DEMO_ENABLED | true |
| TARGET_LOCAL_DEMO_ORIGINS | http://127.0.0.1:8081 |
| EVALUATOR_PROVIDER | OPENAI |
| EVALUATOR_MODEL | gpt-6-luna |
| EVALUATOR_EXPORT_ENABLED | true |
| EVALUATOR_ALLOWED_SCENARIOS | demo-vulnerable-v1 |
| EVALUATION_SCENARIO_FILE | C:\Users\Develop\Desktop\llm-scanner-seguridad\demo-vulnerable\manifest\evaluation-scenario.local.json |
| EVALUATOR_MAX_CALLS | 4 |
| EVALUATOR_TIMEOUT_MS | 20000 |
| EVALUATOR_INTERVAL_MS | 1000 |
| EVALUATOR_MAX_OUTPUT_TOKENS | 1024 |
| TEST_EXECUTION_SCAN_BUDGET_MS | 300000 |

Estos argumentos de proceso, junto con los bindings Environment/configuración Java ya inspeccionados, acreditan la configuración explícita para este preflight. No acreditan que el modelo sea accesible ni que la cuenta haya consumido llamadas.

### Entorno y binding

- Se reutilizó PostgreSQL existente; listeners5432 PIDs31736/1116 y API con acceso persistido satisfactoria. No consulta SQL, lectura de credenciales ni migración.
- Se arrancó el jar existente de demo con Java21, profile VULNERABLE/bind127.0.0.1/puerto8081, PID3772. GET /health HTTP200: demo=true/status=UP/profile=VULNERABLE/scenarioVersion="1". No probes extra.
- Se arrancó Angular con CLI ya instalada, sin instalar dependencias, PID32216, host127.0.0.1/puerto4200. HTML app-root y GET /api/projects por proxy HTTP200. Navegador integrado abrió /projects y mostró el Project QA real. El primer intento localhost ocurrió antes de disponibilidad y falló; se usó después el literal loopback IPv4 disponible. No se pulsó ejecutar.
- Project identificado por el binding: 65f132db-20c5-40b8-a3ee-6ee7d2cb2d76. GET del Project, Target e histórico HTTP200. Target3ae92e44-4e81-427f-93ce-021a6a751643, updatedAt exacto2026-10-16T16:25:10.710151Z, POST http://127.0.0.1:8081/api/chat, application/json, plantilla message con {{PROMPT}}, responsePath $.response, headers vacíos/authType NONE/sin credenciales/timeout5000. Histórico solo COMPLETED, sin Scan activo.
- Manifest local comparado con la plantilla VULNERABLE tras sustituir exactamente los tres placeholders: coincidencia estructural completa, cuatro entries originales y contexto/corpus intactos. Binding existente coincide con identidad/fecha/perfil/version; Test-Json contra bindings-v1.schema.json PASS. Ambos JSON locales siguen ignorados por Git.

### Contradicción bloqueante

REV-04 y D-03 paso9 exigen validar el manifest contra `demo-vulnerable/manifest/schemas/evaluation-scenario-v1.schema.json`, conservando a la vez exportPolicy.enabled=true para este recorrido real. Ese schema, líneas236..237, fija **const:false** para enabled. No existe una instancia que satisfaga ambos requisitos a la vez.

Se cambió únicamente enabled false→true en la copia local para el preflight. SHA256 autorizado temporal: 2219C27C4E866EDD79845A663DC88844956A383EB4FDB0B6943AC905DC22F75C. `Test-Json -LiteralPath demo-vulnerable/manifest/evaluation-scenario.local.json -SchemaFile demo-vulnerable/manifest/schemas/evaluation-scenario-v1.schema.json` devuelve false y diagnóstico Expected "false" en /scenarios/0/exportPolicy/enabled. También imprime diagnósticos de ramas anyOf/outputContract en la evaluación fallida; no se atribuyen a defectos adicionales: el mismo documento restaurado con enabled=false valida satisfactoriamente.

No se corrige el schema, no se relaja la validación ni se cambia el requisito. Hace falta autorización específica para resolver la contradicción, por ejemplo permitir el booleano de exportación en un schema aplicable al recorrido real y preservar las restricciones históricas del offline, o aprobar un contrato de validación separado. No se selecciona una solución arquitectónica permanente ni se ejecuta un Scan para comprobar si el runtime acepta el documento.

### Resultados y cierre observado

- ScanId: ninguno; Scans nuevos0; llamadas al proveedor iniciadas por la sesión0, sin llamadas exploratorias/retries. Acceso al modelo **BLOCKED por preflight**, sin evidencia de rechazo de OpenAI.
- Cuatro categorías LLM01/02/08/10: no ejecutadas; sin TestExecutions, EvaluationResults, LLM verdict/confidence ni consolidación nuevos. Findings/Score/Report no obtenidos. Matriz ground truth sin clasificaciones observadas, sin inventar FN/INCONCLUSIVE ni TN/FP. Frontend9.4 pendiente, no se realizó su secuencia.
- Usage/model metadata **NOT_INSPECTED** (no Scan propio); coste/usage facturado no consultado. Errores del proveedor: ninguno observado, no hubo petición. No hay validación del proveedor ni cadena scanner completada.
- Manifest local restaurado a exportPolicy.enabled=false; schema PASS. SHA256 restaurado C603784A8599201D7C82914147382C0471676FE34E02F76CD5BEAD0DD32F8334. Binding/plantillas/schema versionados sin modificación.
- Antes de parar se consultaron únicamente históricos públicos de los cuatro Projects para evitar afectar un Scan activo: ninguno PENDING/RUNNING. Se verificó identidad java.exe/PID26104/fecha de inicio, y se detuvieron scanner26104, demo3772 y Angular32216 conforme al cierre autorizado. Comprobación posterior: sin listeners8080/8081/4200; PostgreSQL5432 conservado en sus PIDs originales. No borrados, SQL, cambios de dependencias, código, AWS, deploy, commits ni pushes.
- La exportación de esta instancia queda inoperativa por su parada y el permiso local queda false. El agente no puede retirar una variable de la consola padre del operador; queda pendiente que el operador ejecute `Remove-Item Env:EVALUATOR_API_KEY -ErrorAction SilentlyContinue` en la terminal preparada. Ese comando no lee ni imprime la clave. Por este paso pendiente no se marca completa 5.1.
- Único archivo versionable modificado por la tarea: este tasks.md. Tests afectados no aplicables: sin cambios de código. No se actualiza frontend-mvp ni checklist TFM. Tareas completas3/18; las verificaciones parciales no cierran sus tareas.
- OpenSpec strict final ejecutado: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. No elimina la contradicción del schema JSON de demo ni acredita ejecución del Scan.

## Corrección acotada aprobada del schema V1 — 2026-10-19

Solicitud posterior explícita del operador: implementar únicamente la corrección auxiliar del schema, tests necesarios y README, sin servicios, API key, llamadas OpenAI ni commit/push. Este alcance sustituye la prohibición de corregir producto del registro operativo anterior solo para esta corrección. El recorrido real del evaluador permanece pendiente; no se autoriza relanzarlo en esta tarea.

- [x] C.1 Cambiar exclusivamente exportPolicy.enabled de `const:false` a `type:boolean` en evaluation-scenario-v1.schema.json, manteniendo V1, required y todas las demás restricciones.
- [x] C.2 Cubrir ambos booleanos, ausencia, tipos no booleanos sin coerción, provider incorrecto/ausente y plantillas offline false mediante ManifestTest.
- [x] C.3 Cubrir gates existentes con manifest true y conservar pruebas de binding, contexto/corpus/tamaños y transporte sin clave, sin modificar runtime ni llamar al proveedor.
- [x] C.4 Aclarar en README estructura frente a política offline y autorización conjunta de exportación.
- [x] C.5 Ejecutar tests afectados y suite unitaria relevante, revisar diff/compatibilidad offline y registrar resultado real.
- [x] C.6 Ejecutar OpenSpec strict final y registrar evidencia.

### Cambio y compatibilidad

El único cambio del schema es `"const": false` → `"type": "boolean"` para enabled. Sigue siendo obligatorio, sin default ni coerción. Provider conserva const OPENAI. Ningún otro campo o versión cambia. Las dos plantillas SAFE/VULNERABLE siguen usando false y no se modifican; el manifest local conservado del preflight tampoco se modifica. Las instancias V1 previamente válidas con false continúan siendo válidas. Los tests existentes validan las dos plantillas después de completar bindings sintéticos, rechazando sus placeholders originales. No se altera código, configuración, evidencia ni requisitos del E2E offline.

README de demo distingue validación estructural, política offline false y true permitido solo para operación real autorizada. Explicita que true por sí solo no autoriza exportación y enumera permisos globales, allowlist, manifest, binding, clave y controles de evidencia/presupuesto.

### Tests y gates

- ManifestTest añade `exportPolicyRequiresExplicitBooleanAndTheExistingProviderWithoutChangingOfflineTemplates`: SAFE/VULNERABLE, false/true válidos; enabled ausente, null, strings true/false, enteros0/1, array y objeto inválidos; ausencia no rellenada ni valores coercionados; provider OTHER o ausente inválidos. Confirma schemaVersion1 y política false de ambas plantillas.
- EvaluationScenarioTest añade `manifestTrueStillRequiresGlobalPermissionAllowlistAndMatchingProvider`: un manifest true válido no produce entrada LLM si falta permiso global, allowlist vacía/no coincidente o provider coincidente; exportAuthorized=false, EXPORT_NOT_AUTHORIZED e input null. Incluye control positivo con todos los permisos, fecha divergente por un microsegundo, archivo ausente e inválido. Los tests existentes conservan rechazos de projectId/targetId/fecha distintos, documento malformado, campos desconocidos y caps; también corpus no coincidente, evidencia incompleta/redactada y tamaño excesivo.
- OpenAiTransportTest añade `missingOrBlankKeyNeverReachesTransportEvenWithPreparedInput`: supplier de test null/vacío/blanco, error CONFIGURATION_INVALID y contador de transporte cero. No consulta variables de entorno. Los demás tests usan fixtures y transporte en memoria; se conservan contrato estructurado, errores, límites, timeout, concurrencia y ausencia de retries.
- No cambia ninguna clase de producción backend, gate, dependencia, arquitectura, lockfile, manifest offline o configuración de despliegue. La cobertura es unitaria de las capas existentes; no acredita un Scan real ni operación del proveedor.

### Ejecución observada

Java y javac comprobados antes de Maven: Temurin21.0.12.1, usando JAVA_HOME `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`. Maven offline, sin descargas ni instalación de dependencias:

```powershell
# Desde demo-vulnerable/
.\mvnw.cmd -o '-Dtest=ManifestTest,ScenarioTest' test
# Desde backend/
.\mvnw.cmd -o '-Dtest=EvaluationScenarioTest,EvaluatorContractTest,OpenAiTransportTest' test
```

Demo BUILD SUCCESS: ManifestTest6 + ScenarioTest8 =14, failures0/errors0/skipped0. Backend BUILD SUCCESS: EvaluationScenarioTest9 + EvaluatorContractTest7 + OpenAiTransportTest5 =21, failures0/errors0/skipped0. Total35 tests. Surefire XML confirma ejecución de los tres métodos nuevos. El warning DEMO_CONFIGURATION_INVALID de ScenarioTest corresponde al caso negativo existente de configuración inválida; la suite termina satisfactoriamente. No se ejecutan HttpContractTest, integración PostgreSQL ni suites que levanten servidores. Ningún servicio arrancado, migración, Scan o llamada real OpenAI; ninguna clave leída/usada/mostrada.

Diff revisado: cinco archivos de schema/README/tests más este tasks.md (change aún no versionado). Sin regresiones observadas en las suites ejecutadas. El bloqueo concreto del schema se resuelve; la validación operativa y frontend9.4 siguen pendientes. OpenSpec strict final: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. Estado de esta corrección: **READY FOR REVIEW**, sin declarar completo el recorrido real.

## Reanudación real autorizada tras aprobar el schema — 2026-10-19

Operador: APPROVED / READY TO RESUME REAL EVALUATOR VALIDATION, un único Scan, máximo cuatro llamadas, sin retries ni modelo alternativo. Resultado posterior: **BLOCKED antes del Scan por detención de Computer Use al no poder determinar la URL activa del navegador con suficiente confianza**. Se respetó la detención y no se lanzó por una vía alternativa. La autorización del único Scan sigue sin consumirse.

Evidencia: [result.md](evidence/2026-10-19-real/result.md), preflight.json, target-before.json y closure.json en el mismo directorio. PostgreSQL TCP y API persistida verificados; demo2940 UP/VULNERABLE/version1; backend35120 arrancado por el operador en su terminal preparada, argumentos no secretos explícitos verificados (local/DB correcta/SSRF exacto/modelo gpt-6-luna/gates/límites). No se leyó la API key ni el entorno del scanner. Manifest habilitado y binding pasan ambos schemas, coinciden con plantilla autorizada e identidad/fecha de Target releído. Se completan2.2/2.3 por evidencia real, conservando el histórico anterior. Angular35680 se inició para el recorrido UI, pero su disponibilidad/pantalla/proxy no llegó a verificarse;1.3 y lanzamiento/recorrido siguen pendientes. Las verificaciones parciales no cierran1.2/1.4.

Sin ScanId, llamadas0, resultados por categoría/Findings/Score/Report no obtenidos, matriz sin clasificaciones observadas, frontend9.4 pendiente; metadata/usage/coste NOT_INSPECTED. No hay evidencia de rechazo ni éxito del modelo. No se inventan consumo de cuenta, fallos del proveedor ni resultados de evaluaciones.

Cierre real tras comprobar cero Scans activos en los cuatro Projects: manifest local false/schema PASS; scanner35120, demo2940 y Angular35680 detenidos por identidad/fecha; sin listeners8080/8081/4200, PostgreSQL5432 conservado. Exportación de esta instancia inoperativa. Retirada de variable de la terminal padre pendiente del operador (comando sin lectura en result.md), por lo que5.1 no se completa. Ningún dato/histórico borrado. Sin cambios de producto ni tests nuevos en esta reanudación.

OpenSpec strict ejecutado al cerrar: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. Cinco de18 tareas operativas completas, además de las seis tareas de corrección C.1..C.6 previamente terminadas. Estado de validación real **BLOCKED**; no se modifica frontend-mvp9.4 ni checklist TFM.

## Único lanzamiento UI y resultado real — 2026-10-19

La nueva petición del operador autorizó usar exclusivamente la pestaña ya abierta del Project47006cf8-421d-406c-ab7e-c496e3a67fe5 y lanzar una sola vez tras verificar pantalla/preflight. Esta vez el navegador integrado tab3 permitió leer la UI sin navegar ni cambiar de pestaña; no se usó la ventana de claves de Chrome. Evidencia completa: [result.md](evidence/2026-10-19-ui-real/result.md), preflight/binding hash/Target, seis respuestas públicas, comparación ground truth, checks, launch, observaciones UI, tres capturas y closure en ese directorio.

Preflight VERIFIED: backend32828/local/BD correcta/SSRF exacto/gates y modelo gpt-6-luna por argumentos no secretos; Project/Target de pantalla coincidentes por API. Demo ausente arrancada34520/VULNERABLE/8081 y health UP/version1. Se anunció y renovó únicamente el manifest/binding local que seguía ligado a QA, para Targetd774c042-1ac9-4bf5-97b0-45df91b068e0/updatedAt2026-10-15T22:39:37.757848Z; plantilla/corpus intactos, export true/OPENAI, ambos schemas PASS; ningún Scan activo. UI Angular existente operativa. No se leyó clave ni entorno: presencia suministrada por declaración del operador; resultado runtime EVALUATOR_UNAVAILABLE, distinto del rechazo de clave ausente, sin convertirlo en inspección directa del secreto. Se mantiene autorización previa sin gate monetario estimado interno y con hard spend limit declarado.

Un clic UI: Scan **fffb200e-8d9f-422d-be44-a06671afd48a**, PENDING→COMPLETED, duración3.478217s (UI3.479s), sin segundo poller, retries ni Scan adicional. Cuatro TestExecutions COMPLETED/HTTP200 y cuatro EvaluationResults COMPLETED. Cuatro LLM FAILED/EVALUATOR_UNAVAILABLE, cero SUCCEEDED; no assessment/verdict/confidence LLM disponibles. Todas consolidan VULNERABLE/HYB-02/degraded=true/confidence0.70. Findings4 persistidos y enlazados correctamente (LOW2/MEDIUM2). Score AVAILABLE/scoring-v1/51/MEDIUM, I0; Report AVAILABLE/report-v1/es, summary idéntico al score, nueve limitaciones y relectura estable. Matriz externa consolidada TP4/FN0/I0; TN/FP no observables; errores LLM separados. No se atribuye éxito de OpenAI a los Findings deterministas.

9.4: secuencia original observada y documentada; Finding LLM02 real abierto y contrastado con API (severidad/confianza/descripción/evidencia/recomendación), trazabilidad/ref evaluación/ejecución/Finding verificadas. Cobertura **PASS; cierre propuesto**, sin modificar el histórico frontend-mvp/tasks.md según D-06. Interpretación/limitaciones visibles. No se simula detección ni ejecuta markup.

Metadata interna/usage/coste NOT_INSPECTED, rama opcional documentada3.4 sin consulta SQL. Cuatro resultados LLM fallidos no prueban cuatro peticiones HTTP recibidas por OpenAI: el número de HTTP reales no es observable por la API pública; no se inventan intentos durables, facturación o consumo cero. EVALUATOR_UNAVAILABLE no acredita acceso/denegación al modelo ni una causa concreta de red/proveedor.

Se completan1.3/1.4,3.2..3.5,4.1..4.3 y5.2 por esta evidencia.3.1 permanece parcial: clic/id/instante/navegación observados, pero HTTP202/Location no se capturaron directamente; NOT_INSPECTED, sin relanzamiento para obtenerlos.1.2 sigue pendiente por acceso no acreditado.5.1 sigue pendiente únicamente por retirada de variable de terminal padre a cargo del operador.

Cierre aplicado tras UI/API sin pérdida de Computer Use: manifest false/schema PASS; verificados cero Scans activos e identidad/fechas, scanner32828 y demo34520 detenidos; sin listeners8080/8081; PostgreSQL y datos conservados. Angular4200 permanece para revisión de la pantalla cargada. No se crea, lee, imprime ni persiste API key. Comando de retirada en result.md. Sin cambios de código ni tests adicionales en esta ejecución. Quince de18 tareas operativas completas, C.1..C.6 completas. **BLOCKED para validar proveedor/acceso al modelo**; cadena scanner y cobertura9.4 observadas PASS, paquete disponible para revisión sin nuevo consumo autorizado.

OpenSpec strict final de esta ejecución: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. No implica éxito de OpenAI ni completa las tres tareas parciales pendientes.

## Reparación HTTPS autorizada — 2026-10-19

La solicitud posterior autoriza corregir únicamente el transporte HTTPS y su observabilidad, añadir regresión offline y ejecutar tests afectados/suite relevante/strict. Esta excepción de alcance concreta REV-12/D-09; no autoriza otro Scan ni contacto con OpenAI. Se conservan los registros previos y sus estados operativos: esta sección acredita reparación offline, no validación real del proveedor.

- [x] T.1 Sustituir el constructor obsoleto con registry null por BasicHttpClientConnectionManager.create con registry https/TlsSocketStrategy estándar y resolver fijado tras BoundedDnsResolver; verificar firma en fuentes locales 5.6.4 y diff sin cambios de endpoint/modelo/clave/gates/límites/validaciones.
- [x] T.2 Añadir regresión sobre el mismo factory del manager real con peer efímero de loopback, comprobar ClientHello y rechazo de respuesta no TLS; verificar fallo NPE anterior y paso con la corrección, sin fixtureTransport/mocks del manager, certificados ni tráfico externo.
- [x] T.3 Añadir diagnóstico mínimo por categoría/código/tipo técnico y prueba de saneamiento; verificar categorías DNS/TLS_CONNECTION/TIMEOUT/HTTP_PROVIDER/PARSE_SCHEMA y ausencia de mensaje/throwable/cuerpos/clave en eventos/argumentos. Conservar mapeos públicos y prioridad de deadline DNS.
- [x] T.4 Ejecutar tests afectados y suite relevante offline, revisar diff y actualizar únicamente el incidente documental afectado, conservando evidencia histórica y sin modificar frontend9.4; ver resultados y comandos siguientes.
- [x] T.5 Ejecutar OpenSpec strict final y registrar resultado; no interpretar validez documental ni tests offline como acceso al proveedor.

### Corrección y límites preservados

OpenAiEvaluatorAdapter usa ahora BasicHttpClientConnectionManager.create(DefaultSchemePortResolver.INSTANCE, resolver, registry, null), con RegistryBuilder<TlsSocketStrategy> y DefaultClientTlsStrategy.createDefault() registrado para https. Se extrae solo connectionManager(resolver,remainingMs), package-private, para compartir construcción/configuración reales con el test. El null final selecciona la factory de conexiones por defecto; no es el registry. Mismos connect/socket/TLS timeouts, resolver DNS acotado, caps 1..16 IPs y primera IP; mismo endpoint Responses API, store=false, modelo configurable, exportación, secreto solo en entorno, cuota, intervalo, deadline y ausencia de retries/redirects/proxy. Sin nueva dependencia, esquema BD ni campo REST.

WARN limitado a evaluatorFailure, reason y exceptionType. Categorías cerradas DNS, TLS_CONNECTION, TIMEOUT, HTTP_PROVIDER, PARSE_SCHEMA, CONFIGURATION e INTERNAL. No se pasa Throwable al logger ni mensaje/cadena de causas/stack trace, cuerpo, URL, header o evidencia. Se mantienen LlmError/supresión/consolidación, incluida prioridad TIMEOUT sobre UnknownHost cuando expira el presupuesto. El diagnóstico permite identificar etapa y tipo técnico, no reconstruir mensajes descartados.

### Regresión y comprobaciones ejecutadas

`realConnectionManagerSelectsTlsAndEmitsClientHelloWithoutExternalNetwork` utiliza manager/cliente reales y DNS de fixture tls-fixture.invalid → 127.0.0.1, con peer TCP efímero y acotado por timeout. El peer observa cabecera ClientHello (tipo22/versión3) y envía bytes no TLS: se exige SSLException; no hay certificado/clave, trust-all ni HTTP de aplicación. Se comprueba una resolución DNS y los timeouts efectivos del manager.

Primera ejecución en sandbox: socket bloqueado por Permission denied: getsockopt, sin diagnosticarlo como fallo de la corrección. Repetición fuera del sandbox bajo aprobación: **FAIL esperado con NullPointerException en DefaultHttpClientConnectionOperator/adapt**, constructor antiguo. Tras registro TLS válido: **PASS**. La prueba cubre selección/arranque TLS; no handshake completo ni confianza/hostname del proveedor. No se cambia endpoint de producción ni se llama a evaluate con transporte real.

`diagnosticsKeepFailureStagesAndExceptionTypesWithoutSensitiveDetails` verifica siete casos de diagnóstico, con UnknownHost/SSLHandshake/Connect/SocketTimeout/NPE y HTTP429/parse inválido; eventos sin ThrowableProxy y argumentos sin mensajes privados, clave sintética ni canario. Se conservan los tests previos, incluido clave ausente/blanca y cero llamadas, caps, errores HTTP, timeout/concurrencia y no retries. Se añade a la prueba de timeout existente UnknownHost posterior al deadline para conservar EVALUATOR_TIMEOUT.

Desde backend, Java21 Temurin21.0.12.1 mediante JAVA_HOME local, Maven offline sin descargas ni instalación:

```powershell
# Reproducción antes de corregir y comprobación posterior: FAIL NPE → PASS
.\mvnw.cmd -o '-Dtest=OpenAiTransportTest#realConnectionManagerSelectsTlsAndEmitsClientHelloWithoutExternalNetwork' test
# Tests afectados: PASS, 44 tests (incluidos en la suite siguiente)
.\mvnw.cmd -o '-Dtest=OpenAiTransportTest,EvaluatorContractTest,EvaluationScenarioTest,EvaluateExecutionUseCaseTest,EvaluationPoliciesTest' test
# Suite relevante final: PASS, 157 tests
.\mvnw.cmd -o '-Dtest=**/domain/**/*Test,**/application/**/*Test,**/adapters/out/evaluation/*Test,EvaluationControllerTest,EvaluationOpenApiContractTest,ScoringLifecycleTest,SecurityTestExecutionTransactionsTest' test
```

Tests afectados:44/failures0/errors0/skipped0. Suite final:157/failures0/errors0/skipped0, incluidos OpenAiTransportTest7, contratos REST de evaluación, dominio/casos de uso, scoring/reporting y transacciones. Tras el último ajuste del presupuesto DNS se repitió la suite relevante y pasó157/157. No se suman44+157 como tests distintos. Los warnings de agente Mockito son existentes; el log SCAN_EXECUTION_UNAVAILABLE corresponde a una prueba negativa de ciclo de vida. Sin integración PostgreSQL, migraciones ni arranque de servicios de producto; el único socket es fixture de loopback que se cierra al terminar el test.

Archivos de esta reparación: OpenAiEvaluatorAdapter.java, OpenAiTransportTest.java, proposal/design/spec/tasks de este change y docs/04-ia/estrategia-evaluacion.md (incidente directamente afectado). Se conservan los cambios previos de schema/README/ManifestTest/EvaluationScenarioTest y el test previo de clave ausente. Ningún ajuste a docs de arquitectura concurrentes ni frontend/demo en esta reparación. Sin nuevo Scan, OpenAI, API key real, commit o push. Sin regresiones observadas en el alcance ejecutado; acceso real al proveedor/modelo y causa exacta del Scan histórico permanecen no acreditados.

OpenSpec strict ejecutado: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. Estado de esta reparación: **READY FOR REVIEW**. Las tareas operativas pendientes no se cierran por esta corrección y no se autoriza ni realiza otro Scan.

## Revalidación real posterior al fix TLS — 2026-10-19

Autorización nueva y explícita del operador: **APPROVED / READY FOR REAL OPENAI REVALIDATION**, un único Scan adicional para comprobar el fix, sin repetir UI/frontend9.4, sin retries, modelo gpt-6-luna y hasta cuatro llamadas. Esta excepción autoriza lanzamiento REST y observación API en este incremento; no modifica la evidencia del Scan histórico ni simula su captura de cabeceras. Resultado funcional: **PASS / READY FOR REVIEW**. Evidencia: [result.md](evidence/2026-10-19-tls-revalidation/result.md) y JSON mínimos en el mismo directorio.

- [x] R.1 Verificar PostgreSQL, demo VULNERABLE, backend local, gates, binding y uso del artefacto con transporte TLS corregido.
- [x] R.2 Lanzar un único Scan REST y conservar HTTP202/Location/id reales, seguimiento hasta terminal, sin segundo Scan ni retries.
- [x] R.3 Verificar cuatro componentes LLM, acceso/modelo resuelto y verdict/confidence reales; recopilar metadata disponible READ ONLY, sin claves/cuerpos libres.
- [x] R.4 Cotejar Findings persistidos, score/report, invariantes y comparación externa ground truth; conservar metadata/usage/coste sin inventar tarifas.
- [x] R.5 Deshabilitar permiso del manifest y capacidad efectiva del scanner; conservar datos y documentar la retirada de la variable de terminal pendiente del operador.
- [x] R.6 Ejecutar OpenSpec strict final y registrar resultado, preservando frontend9.4 sin repetirlo ni archivar el change.

Al comenzar solo PostgreSQL estaba activo; el jar era anterior al fix. Se preparó el ejecutable con Java/javac21 y Maven offline `-DskipTests package` → BUILD SUCCESS, sin nueva suite ni cambios de código/dependencias. Hash jar515A644DC61986F9BA7C37447080F4FDD0FE07D2C4946B9DAB827A8C3FE8C2DA; clase empaquetada idéntica a target/classes; bytecode inspeccionado confirma DefaultClientTlsStrategy.createDefault/BasicHttpClientConnectionManager.create con registry HTTPS. Backend34020 arrancado por operador después del build desde su terminal preparada; argumentos no secretos verificados. Clave no leída ni entorno volcado. Demo18248 UP/VULNERABLE/8081/version1. Manifest habilitado/corpus intacto/schema PASS y binding exacto con Targetd774c042-1ac9-4bf5-97b0-45df91b068e0/updatedAt2026-10-15T22:39:37.757848Z. Ningún Scan activo.

Un POST → HTTP202, Location literal `/api/projects/47006cf8-421d-406c-ab7e-c496e3a67fe5/scans/7cb34443-f65d-4368-a4e4-61029ab03965`. Scan **7cb34443-f65d-4368-a4e4-61029ab03965**, COMPLETED en28.735017s. Se conserva marcador previo de intento para evitar reenvíos, respuesta202/Location y polling API; sin navegador ni repetición9.4.

Cuatro TestExecutions/EvaluationResults COMPLETED y cuatro componentes LLM **SUCCEEDED**, sin error/degradación. Lectura PostgreSQL local **BEGIN READ ONLY/ROLLBACK**, limitada al Scan y verificada su pertenencia al Project: cuatro llm_attempt_count=1 y cuatro provider_request_id distintos; solicitado/resuelto gpt-6-luna en todos. Solo metadata y escalares llm_assessment.verdict/confidence, nunca assessment libre, prompts, cuerpos ni credenciales. Veredictos LLM VULNERABLE; confianza LLM LLM01=.99/LLM02=1/LLM08=.99/LLM10=.99. Híbrido VULNERABLE/HYB-02/confidence.95/degraded=false en las cuatro categorías. Acceso real al modelo y transporte corregido PASS; se completa1.2 con evidencia real y autorización monetaria previa, sin alterar sus registros históricos.

Findings4 (LOW2/MEDIUM2) con relaciones comprobadas; score AVAILABLE/scoring-v1/52/MEDIUM, I0; categorías15/45/45/15. Report AVAILABLE/report-v1/es, id4ef4655a-4cd0-46c6-9209-ce9b31d195e3, summary idéntico a score, nueve limitaciones y relectura estable. Comparación externa consolidada TP4/FN0/I0, TN/FP no observables, LLM10 OUTPUT_ONLY. Usage observado input3366/output2398 (total5764); coste estimado null en cuatro registros y coste facturado **NOT_INSPECTED**, sin tarifas inventadas. Proveedor HTTP2xx acreditado por respuesta aceptada; status exacto no retenido/no inventar200. Sin código/error de proveedor ni eventos de fallo sanitizados observados. Cuatro respuestas reales no se confunden con una estimación de cuota.

Cierre23:47:26 Europe/Madrid: manifest false/schema PASS; cero Scans activos, identidad/fecha verificadas, scanner34020 y demo18248 detenidos; sin listeners8080/8081, PostgreSQL y Angular ajeno conservados. Exportación efectiva inoperativa. Clave nunca leída/impresa/persistida por el agente; se pidió retirada de variable de terminal padre y el operador respondió **«La retiraré después»**, pendiente explícito. No se marca completa5.1.3.1 conserva su pendiente histórico del recorrido UI original: las cabeceras REST nuevas están registradas en R.2 sin atribuirlas al Scan anterior. Dieciséis de18 tareas operativas originales completas; R.1..R.5 completas y R.6 pendiente de strict. Frontend9.4 y su cobertura anterior se conservan, sin repetición ni edición. Sin AWS, deploy, commit/push, migraciones, limpieza ni escrituras SQL.

OpenSpec strict final de la revalidación ejecutado: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. R.6 completada; seis de seis tareas de revalidación completas. **READY FOR REVIEW**, con retirada de variable de terminal diferida por el operador y pendientes históricos indicados, sin archivar ni cerrar automáticamente el change.

## Cierre de pendientes F1 y F2 del review final

- **F1 CLOSED.** El operador confirma posteriormente que ha retirado `EVALUATOR_API_KEY` de la terminal preparada. [closure.json](evidence/2026-10-19-tls-revalidation/closure.json) refleja `parentTerminalKeyRemoval = COMPLETED` y [result.md](evidence/2026-10-19-tls-revalidation/result.md) registra la procedencia. Sin leer ni registrar el valor de la clave ni acreditar otros procesos o shells no verificados. Se completa 5.1: diecisiete de dieciocho tareas operativas originales completas. Esta confirmación resuelve el pendiente de retirada descrito en los registros anteriores, que se conservan como históricos; 3.1 mantiene su pendiente del recorrido UI original.
- **F2 CLOSED.** Actualizados únicamente los pasajes afectados de `docs/02-arquitectura/arquitectura-general.md` y `docs/04-ia/estrategia-evaluacion.md`, con referencias a la revalidación existente: integración OpenAI `VALIDATED`, transporte corregido y validado, acceso efectivo a `gpt-6-luna`, cuatro respuestas reales aceptadas y evaluación híbrida sin degradación. Usage observado input3366/output2398; coste monetario `NOT_INSPECTED`. La causa histórica sigue sin demostrarse retrospectivamente y frontend9.4 no se revalidó en esta ejecución.

OpenSpec strict de esta corrección documental: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. **READY FOR FINAL REVIEW**. Sin cambios de código/tests, nuevas pruebas salvo strict, Scans, llamadas OpenAI, commit/push ni archivo del change.
