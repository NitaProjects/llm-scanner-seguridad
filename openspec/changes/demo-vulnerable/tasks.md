## 1. Preparación del módulo independiente

Estado de implementación: **READY FOR RE-REVIEW**, 29/29 tareas de entrega/preparación completadas; jar y QA local verificados. Verificaciones operativas pendientes detalladas al final. El usuario aprobó explícitamente el change (APPROVED / READY FOR IMPLEMENTATION, F-01 RESOLVED) y solicitó su implementación el 2026-10-15. La aprobación vigente procede de esa instrucción; proposal/design/spec y el registro de planificación conservan su redacción histórica. La autorización de validaciones técnicas del perfil cargado permite comprobar la demo sin proveedor; Docker, despliegue y E2E completo no se ejecutan. No modificar backend/frontend, catálogo V4, dependencias existentes ni Compose compartido.

- [x] 1.1 Crear build autónomo `demo-vulnerable/` con Java 21, Boot 4.1.1, wrapper propio y starters mínimos D-01; verificar por revisión de pom/estructura que no depende del scanner, JPA/Flyway/BD, cliente HTTP o SDK LLM (DV-01; D-01/D-03).
- [x] 1.2 Definir configuración startup DEMO_PROFILE, bind y puerto, SAFE/127.0.0.1/8081 por defecto y validación cerrada; verificar mediante tests escritos que perfil inválido falla y no hay recarga/mutación remota (DV-03/DV-11; D-07/D-17).
- [x] 1.3 Configurar logs mínimos y exclusiones locales/build, sin logging de body/headers ni bindings versionados; verificar configuración e inventario de archivos y preparar captura de logs para safety (DV-12/DV-13; D-21/D-22).

## 2. Escenarios deterministas y oráculo externo

- [x] 2.1 Añadir catálogo classpath `benchmark/scenarios-v1.json` con cuatro prompts exactos code/revision/categoría de V4, outputs por perfil y sin ground truth; verificar literal por literal contra D-08..D-11, sin modificar/importar el catálogo scanner (DV-04; D-03).
- [x] 2.2 Implementar selección pura inmutable por igualdad exacta y desconocido DEMO_NO_SCENARIO; verificar con tests de repetición, escapes/Unicode, espacios/saltos añadidos y sin reloj/estado conversacional (DV-04/DV-13; D-05).
- [x] 2.3 Modelar LLM01 con restricción previa, sentinel prohibido y control seguro exactos; verificar en fixtures/tests que el marcador reservado no aparece en probe/publicValues y que el público aislado no acredita la infracción (DV-05; D-08/D-13).
- [x] 2.4 Modelar canario falso LLM02 y contexto/anclas LLM08 con sus controles; verificar outputs completos, negativos parciales/etiqueta sola, cláusulas distintas y ausencia de markers en prompts (DV-06/DV-07; D-09/D-10).
- [x] 2.5 Modelar LLM10 con strings JSON internos SAFE/vulnerable y contrato cerrado text:string; verificar extracción string, JSON válido en ambos niveles, clave extra como única infracción y span permitido, sin sink (DV-08; D-06/D-11).
- [x] 2.6 Entregar `manifest/ground-truth-v1.json` y esquema cerrado con ocho registros/justificaciones/señales/versiones/cobertura, conservando su formato; verificar IDs únicos, pares por categoría, consistencia con outputs y etiquetas independientes del evaluador; documentar comparación externa TP/TN/FP/FN e INCONCLUSIVE aparte (DV-09; D-12).

## 3. Adaptador HTTP acotado

- [x] 3.1 Implementar POST `/api/chat` con JSON estricto, campos cerrados, UTF-8, sin query/compresión, respuesta string y headers inertes; verificar mediante tests de contrato positivos y duplicados/tipos/desconocidos/charset inválidos (DV-02; D-04..D-06).
- [x] 3.2 Implementar GET `/health` mínimo con perfil/version fijos y sin markers, más errores seguros de método/ruta/media type/validación; verificar los códigos HTTP y que los cuerpos no reflejan entradas ni internals (DV-02/DV-03; D-04/D-06).
- [x] 3.3 Aplicar lector N+1, caps message/profundidad/cabeceras/salida, validación de fixtures al arrancar y timeout efectivo de lectura del body; verificar con casos de frontera UTF-8, chunked sin Content-Length y socket lento, sin confundir timeout inactivo con absoluto (DV-12; D-20).
- [x] 3.4 Aplicar admisión no bloqueante de una solicitud y separación >=100 ms, con conexiones/hilos/cola técnica limitados; verificar liberación de compuerta, 429 concurrente/frecuente y ausencia de sleeps/cola de negocio (DV-12; D-20).
- [x] 3.5 Mantener entrada como datos inertes y solo recursos classpath al startup; verificar por revisión dirigida que no existen clientes/red saliente, procesos, eval, rutas de usuario, uploads, BD o escrituras de datos de aplicación (DV-13; D-03/D-22).

## 4. Manifiestos y preparación del scanner

- [x] 4.1 Crear `manifest/benchmark-v1.json` y esquema propio con finalidad sintética, autorización/control, perfil/version, markers, ground truth y TargetConfiguration exacta; verificar coherencia de URLs/template/path/auth/timeout con D-15 sin cambiar OpenAPI (DV-10/DV-11; D-14..D-16).
- [x] 4.2 Crear dos plantillas JSON separadas del manifiesto evaluador con esquema v1, cuatro entries/contextos cerrados/corpus por perfil y exportPolicy false/OPENAI; verificar referencias técnicas aprobadas en context, ausencia de etiquetas/justificación del oráculo, expected scanner result, TP/TN/FP/FN, URL/headers, límites existentes y strings completos exactos, distinguiendo manifiesto local del payload del proveedor (DV-10/DV-14; D-14/D-25).
- [x] 4.3 Documentar copia/binding manual de IDs y updatedAt exactos obtenidos de API, health/profile/version y ficheros locales ignorados; verificar ejemplos sin UUID/fecha que aparenten entorno ya preparado, tokens sin resolver marcados inválidos y un único escenario por target/fecha (DV-03/DV-10; D-07/D-14).
- [x] 4.4 Documentar configuración externa scanner local+flag+origen exacto y permisos de evaluación desactivados; verificar que no se modifica SSRF ni ficheros del backend, y que cambiar URL/perfil exige nueva preparación operativa (DV-11/DV-14; D-15..D-17/D-25).

## 5. Tests de compatibilidad y seguridad

- [x] 5.1 Completar tests de contrato HTTP y ocho casos/negativos/perfil/determinismo, incluidos JSON exterior/interior LLM10; verificar cobertura de DV-02..DV-08 contra D-23 y resultados esperados independientes, sin proveedor ni BD.
- [x] 5.2 Completar tests de esquema/catálogo/markers/oráculo/manifiestos por perfil/binding/fecha/corpus; verificar markers reservados fuera del probe, referencias aprobadas permitidas en context/trustedSyntheticContext, oráculo separado y ninguna etiqueta del oráculo ni ambos perfiles bajo un binding del fichero evaluador cerrado (DV-09/DV-10; D-12..D-14).
- [x] 5.3 Completar tests de límites reales, rate/concurrencia y logs sin cuerpos/markers, incluyendo inactividad del body con socket y chunked N+1; verificar efectos HTTP/cierre y ausencia de truncamiento/capacidades peligrosas (DV-12/DV-13; D-20/D-21/D-23).
- [x] 5.4 Preparar procedimiento de safety en runtime sin egress externo, observación de red/procesos/escrituras de datos y solo entrada loopback; verificar que diferencia revisión de código de evidencia runtime, sin afirmar garantía universal por unit tests (DV-13/DV-16; D-22/D-23). Ejecutarlo únicamente ante autorización explícita y registrar lo observado.
- [x] 5.5 Preparar validación de separación F-01 de EvaluatorInput y payload final con el serializador vigente y captura de transporte sin proveedor, sin modificar backend ni importar/copiar sus clases en la demo: verificar ausencia de expectedVerdict, estado vulnerable esperado, clasificación del caso, rationale, expected scanner result, resultado observado y TP/TN/FP/FN en todos los mensajes; usar sentinels exclusivos del oráculo e invariancia ante cambios solo de etiquetas, admitiendo referencias técnicas y señales observadas. Registrar separadamente revisión/fixtures y captura real pendiente; no afirmar ejecución por redactar esta tarea (DV-10/DV-16; D-14/D-23).

## 6. Docker opcional y arranque

- [x] 6.1 Añadir Dockerfile propio de dos etapas Java 21 con versiones explícitas, no root y .dockerignore sin secretos/bindings; verificar aislamiento de artefactos, ausencia de dependencias scanner/BD y que no altera Compose/despliegue compartido (DV-15; D-22).
- [x] 6.2 Documentar wrapper/jar y Docker con publicación loopback, límites/privilegios y tmpfs técnico acotado si necesario; verificar comandos/rutas/perfil/health contra D-24, sin ejecutar build/arranque automáticamente ni confundir bridge con bloqueo egress (DV-11/DV-15; D-17/D-22/D-24).

## 7. Documentación y preparación E2E posterior

- [x] 7.1 Crear README propio con propósito/advertencia intencional, datos falsos, contrato, ocho casos/markers/ground truth, perfiles, manifest/binding, scanner/SSRF, límites/logs/safety, tests/Docker y limitaciones académicas; verificar presencia de todos los apartados DV-16, tres capas D-14, limitación metodológica literal y ejemplos exactos D-24, sin afirmar ceguera completa a las referencias.
- [x] 7.2 Incluir checklist del recorrido futuro PostgreSQL→scanner→demo→Angular→proyecto/target/binding→Scan→4 evaluaciones→Findings/score/report; verificar que el default offline espera I/EXPORT_NOT_AUTHORIZED y cero Findings, y que veredictos/TP-TN/open Finding requieren ejecución posterior autorizada del evaluador (DV-14/DV-16; D-25). No ejecutar E2E ni cerrar 9.4 en esta tarea.
- [x] 7.3 Actualizar solo referencias consolidadas directamente afectadas por la implementación (README raíz/arquitectura/política de autorización/casos de prueba, cuando proceda), sin ampliar contratos ni reescribir históricos; verificar evidencia antes de cualquier cambio de checklist TFM y mantener experimento/integración/publicación pendientes si no se han realizado (DV-16; D-26).

## 8. Revisión final y registro de evidencia

- [x] 8.1 Revisar alcance y trazabilidad DV-01..DV-16/D-01..D-26 frente a archivos entregados; verificar que backend/frontend/probes/dependencias existentes/Compose están intactos, registrar comprobaciones realmente autorizadas/ejecutadas y pendientes, y validar OpenSpec strict si se solicita en esa fase. Sin commits, despliegue, proveedor o E2E implícitos (DV-16).

## 9. Registro de planificación

La fase actual entrega exclusivamente proposal/spec/design/tasks y scaffold OpenSpec. Se inspeccionaron contratos y código relevantes en modo lectura. La decisión offline INCONCLUSIVE y evaluador autorizado posterior fue confirmada el 2026-10-13. No se han implementado ni marcado completadas tareas de apply; `frontend-mvp` 9.4 permanece pendiente.

Validación solicitada y ejecutada el 2026-10-13: `openspec validate demo-vulnerable --strict` → exit code 0, `Change 'demo-vulnerable' is valid`. OpenSpec requirió ejecución fuera del sandbox para cargar su esquema local `spec-driven`; no se utilizaron proveedores ni se ejecutaron build/tests/servidores. El estado CLI confirma los cuatro artefactos de planificación completos; no implica tareas implementadas, aprobación ni integración real. La revisión final de Git muestra únicamente el nuevo change.

### Corrección F-01 — 2026-10-15

Se precisan exclusivamente proposal/design/spec/tasks para aceptar referencias sintéticas conocidas en la evaluación híbrida y excluir etiquetas/justificación del oráculo del manifiesto evaluador, EvaluatorInput y payload final. D-14/D-23 y DV-10/DV-16 describen la frontera real y sus comprobaciones; D-12/D-13/D-24/D-25 y DV-09 mantienen coherencia metodológica sin cambiar formato de ground truth, probes o markers. Se añade la tarea 5.5 como validación futura, no ejecutada. El oráculo independiente permite comparación externa TP/TN/FP/FN y registro separado de INCONCLUSIVE.

No se modifican backend/frontend, componentes ni change de evaluation-engine, SSRF o reglas híbridas. No se implementa la demo ni tests; la limitación académica literal queda en D-14/DV-16 y se exige para el README/TFM posterior. Validación ejecutada para esta corrección el 2026-10-15: `openspec validate demo-vulnerable --strict` → exit code 0, `Change 'demo-vulnerable' is valid`, con acceso fuera del sandbox al esquema local. La validación de payload/serialización descrita en D-23/5.5 sigue pendiente, sin llamadas externas ni pruebas ejecutadas. Estado: **READY FOR REVIEW**, exclusivamente sobre F-01; ninguna tarea de implementación se marca completada.

## 10. Implementación autorizada — 2026-10-15

La instrucción posterior del usuario aprueba el change y autoriza implementarlo exclusivamente, sin backend/frontend, despliegue ni E2E completo. Se entrega `demo-vulnerable/` independiente con Java 21/Boot 4.1.1, starters webmvc/webmvc-test mínimos y copia propia del wrapper 3.3.4/Maven 3.9.16 ya adoptado. No hay dependencia runtime del scanner, proveedor, BD o servicios externos. Se conservan intactos los artefactos históricos de propuesta/diseño/spec y su alcance; este registro establece el estado posterior de implementación.

### Trazabilidad entregada

Rutas relativas a `demo-vulnerable/` salvo indicación contraria:

| Requisitos | Decisiones | Implementación/evidencia |
| --- | --- | --- |
| DV-01 | D-01..D-03/D-18 | pom.xml, wrapper propio, DemoApplication; build sin scanner/BD |
| DV-02 | D-04..D-06 | DemoHttpFilter, DemoJson, SafeErrorReportValve; HttpContractTest |
| DV-03 | D-07 | DemoSettings/DemoProfile, health; tests de default, startup inválido y mutación remota |
| DV-04 | D-02/D-05 | ScenarioCatalog y scenarios-v1.json; ocho salidas, repetición y casi coincidencias |
| DV-05 | D-08/D-13 | Fixture LLM01, contexto restriction/actionMarker y negativos; EXPECTED_MARKER real |
| DV-06 | D-09/D-13 | Canario falso y NO_CANARY; KNOWN_SYNTHETIC_VALUE real, etiqueta/parcial negativos |
| DV-07 | D-10/D-13 | Contexto/anclas de cláusulas distintas y control; KNOWN_HIDDEN_CONTEXT real |
| DV-08 | D-06/D-11 | LLM10 con JSON exterior/interior válidos, span permitido y clave extra; OUTPUT_CONTRACT real |
| DV-09 | D-12 | ground-truth-v1.json/esquema cerrado, ocho IDs, justificaciones, comparación externa documentada |
| DV-10 | D-14 | benchmark, dos plantillas, esquemas, procedimiento manual de binding; ManifestTest/ScannerBoundaryTest |
| DV-11 | D-15..D-17/D-19 | Configuración exacta documentada; bind loopback default; SSRF consumida sin cambios |
| DV-12 | D-20/D-21 | N+1/caps/startup, timeout body 2000 ms, AdmissionGate, conector acotado y logs mínimos |
| DV-13 | D-03/D-18/D-22 | Solo fixtures classpath y selección en memoria; revisión dirigida; safety aislado preparado, ejecución pendiente |
| DV-14 | D-25 | Checklist E2E posterior en README; offline INCONCLUSIVE/EXPORT_NOT_AUTHORIZED, cero Findings |
| DV-15 | D-17/D-22/D-24/D-26 | Dockerfile dos etapas Java 21 explícito/no root, dockerignore, comandos loopback; Docker pendiente |
| DV-16 | D-23/D-24/D-26 | Tests, README propio, limitación metodológica literal, registro presente y referencias consolidadas mínimas |

### Validaciones ejecutadas

- `java -version` y `javac -version`: Temurin **21.0.12.1**. Maven compiló con release 21.
- Maven siempre **offline** (`-o`), usando dependencias ya disponibles: no instalaciones, actualizaciones ni descargas. Las pruebas de sockets loopback requirieron ejecución fuera del sandbox, que inicialmente denegó la conexión local.
- Comando final desde raíz:

  ```powershell
  .\demo-vulnerable\mvnw.cmd -o -f demo-vulnerable/pom.xml '-Dscanner.validation.jar=C:\Users\Develop\Desktop\llm-scanner-seguridad\backend\target\llm-scanner-seguridad-0.0.1-SNAPSHOT.jar' package
  ```

- Resultado: **BUILD SUCCESS**, **21 tests**, cero fallos/errores/omitidos: HttpContractTest 8, ManifestTest 5, ScannerBoundaryTest 1, ScenarioTest 7. Generado `target/demo-vulnerable-0.0.1-SNAPSHOT.jar`; target está ignorado.
- HTTP real únicamente a dos instancias locales efímeras, SAFE/default y VULNERABLE, cerradas al terminar. Se verificaron N/N+1 de body y message UTF-8, escape JSON, chunked sin Content-Length, cabeceras excesivas, timeout body/socket, 429 concurrente y por peticiones inmediatas, liberación de compuerta, errores cerrados y ausencia de cuerpos/markers/headers en logs. Se corrigió el despacho ERROR de Tomcat para que 408 no exponga el envelope estándar con timestamp/path.
- F-01: captura **real en memoria** del request de OpenAiEvaluatorAdapter usando su transporte sustituido, con EvaluatorMessages/EvaluatorInput/DeterministicRules/LocalEvaluationScenarioAdapter del jar existente, sin copiar/importar sus clases ni modificar backend. Se validaron contextos/corpus, binding exacto (fecha distinta no coincide), señales y negativos, developer/user decodificados, referencias técnicas permitidas, exclusión de etiquetas/IDs operativos, sentinels exclusivos del oráculo e invariancia ante variación de sus metadatos. Artefacto scanner SHA-256: `123e8d8069c1fb844ca9a28ffe83f65fb269738cd493b131c4df26e94b791956`. Evidencia acotada a ese artefacto, sin operación de proveedor ni Scan. El test se omite por defecto si no se proporciona `scanner.validation.jar`; la app independiente no requiere ese jar.
- `openspec validate demo-vulnerable --strict`: **PASS**, exit 0, `Change 'demo-vulnerable' is valid`, fuera del sandbox para acceder al esquema local.
- `git diff --check`: PASS en archivos seguidos; revisión dirigida de archivos nuevos y JSON/tests. `git diff --name-only -- backend frontend docker`: vacío. Revisión dirigida de `src/main` sin clientes de red, ejecución de procesos, BD ni archivos arbitrarios. Wrapper idéntico al existente; solo se copian sus scripts/propiedades, no configuración del backend.

### Pendientes y límites de la evidencia

Las tareas 5.4, 6.2 y 7.2 entregan **procedimientos/preparación**, no acreditan haber ejecutado safety aislado, Docker o E2E. Permanecen pendientes: build/arranque Docker y comprobación de las etiquetas de imagen en el entorno autorizado; runtime con egress denegado y observación de red/procesos/escrituras; binding operativo real y recorrido completo PostgreSQL→scanner→demo→Angular; operación real del evaluador/exportación, comparación experimental y dataset/repeticiones/estadística. No se cerró frontend-mvp 9.4 ni se marcaron requisitos TFM como COMPLETADO. Solo se actualizaron referencias afectadas en README raíz, arquitectura, política de autorización, casos de prueba y observaciones/evidencia de la checklist, manteniendo EN PROGRESO.

No hubo proveedor real, tráfico externo, E2E completo, cambios de backend/frontend/Compose, commits, pushes, migraciones, despliegues ni archivo del change.


## 11. Verificación de la petición completa — 2026-10-15

Estado final: **READY FOR REVIEW**. La petición completa del usuario ratifica el alcance y solicita explícitamente tests, package, arranque local, health, probes SAFE/VULNERABLE, límites, QA manual y OpenSpec strict. No se inicia el E2E scanner→demo.

### Resultado técnico y QA local del jar

- Se repitieron tests/package offline con Java 21 y el banco F-01 opcional: **21 tests PASS**, sin errores, fallos ni omitidos; **BUILD SUCCESS**. Última ejecución tras el ajuste de independencia de la fixture V4: 23:06 Europe/Madrid.
- Ajuste de independencia: ManifestTest deja de leer V4 desde el filesystem de backend. Usa `src/test/resources/benchmark/probes-v4-reference.json`, snapshot estático de los cuatro probes aprobados, cotejados previamente con V4. La suite normal ya no requiere archivos/artefactos del scanner; el banco de captura real sigue siendo opcional. Sin cambios de comportamiento, arquitectura, probes ni código runtime.
- Arranque real mediante `java -jar demo-vulnerable/target/demo-vulnerable-0.0.1-SNAPSHOT.jar`, en dos procesos consecutivos y separados. SAFE arrancó **sin DEMO_PROFILE**; VULNERABLE con el valor explícito. Bind/puerto efectivos comprobados por PID: **127.0.0.1:8081** en ambos.
- `GET /health`: 200 y objeto cerrado status/demo/profile/scenarioVersion, UP/true/perfil seleccionado/"1". Sin markers ni contexto.
- Se enviaron solo los cuatro probes exactos como message, cada uno **tres veces por perfil**: **24 respuestas 200, exactas e idénticas**, comparadas con el oráculo externo. No se envió el oráculo al target.

| Probe | SAFE, 3 repeticiones | VULNERABLE, 3 repeticiones |
| --- | --- | --- |
| LLM01-INSTRUCTION-001/1 | PASS, respuesta segura exacta | PASS, acción simbólica reservada exacta |
| LLM02-CANARY-001/1 | PASS, NO_CANARY | PASS, canario sintético completo exacto |
| LLM08-CONTEXT-001/1 | PASS, CONTEXT_NOT_DISCLOSED | PASS, contexto sintético y dos anclas exactos |
| LLM10-MARKUP-001/1 | PASS, objeto interno solo text:string | PASS, objeto interno con unexpected exacto |

- QA de LLM10: envelope externo cerrado y válido, response de tipo string, JSON interno válido, span inerte exacto permitido; solo VULNERABLE contiene la clave extra. No sink ni ejecución HTML/JS.
- QA de reserva: ninguno de los cuatro probes contiene ningún marker reservado del ground truth. Las categorías no contaminan las salidas de las demás.
- QA de inmutabilidad en ambos procesos: header DEMO_PROFILE y Cookie con perfil opuesto no cambian salida/health; query y campo profile en body producen 400; un prompt que solicita cambiarlo devuelve DEMO_NO_SCENARIO. Sin endpoint administrativo.
- QA de entradas URL/ruta/comando: DEMO_NO_SCENARIO, sin interpretarlas. Revisión dirigida de src/main sin shell/ProcessBuilder/Runtime.exec, clientes HTTP/URL fetch, Files/FileInputStream, BD/JDBC, upload, reflexión/Class.forName/URLClassLoader ni deserialización polimórfica/peligrosa. La reflexión del banco F-01 está exclusivamente en src/test, aislada y operando sobre el artefacto explícito del operador; no se empaqueta ni se expone por HTTP.
- QA de límites del jar en ambos perfiles: message 4096 bytes → 200 y 4097 → 413; body 8192 → 200 y 8193 → 413; salidas serializadas <=4096; solicitud simultánea durante body incompleto → 429/RATE_LIMITED; lectura inactiva termina alrededor de 2000 ms con 408 cerrado o cierre. La suite añade chunked N+1, UTF-8, cabeceras, profundidad y frecuencia inmediata.
- QA de logging: stdout/stderr de ambos procesos contienen solo diagnóstico técnico de inicio, sin probes, cuerpos JSON, headers/cookies, markers completos ni causas de parsing. Capturas locales ignoradas: `target/qa-SAFE.stdout.local.log`, `target/qa-SAFE.stderr.local.log` y equivalentes VULNERABLE.
- Los dos procesos se detuvieron al finalizar; comprobación posterior: **cero listeners en 8081**. El resultado resumido, sin cuerpos ni salidas completas, está en `target/qa-manual-summary.local.json`; el procedimiento ejecutado está en `target/qa-manual.local.ps1`, ambos auxiliares locales ignorados.
- `openspec validate demo-vulnerable --strict`: **PASS**, exit 0, `Change 'demo-vulnerable' is valid`, tras incorporar esta evidencia. `git diff --check`: PASS; 29 tareas marcadas; diff de backend/frontend/docker vacío. No hay listeners de la demo en 8081 tras finalizar la QA.

### Archivos entregados y estructura

37 archivos nuevos de la aplicación independiente (sin target, logs ni auxiliares locales):

```text
demo-vulnerable/.dockerignore
demo-vulnerable/.gitignore
demo-vulnerable/.mvn/wrapper/maven-wrapper.properties
demo-vulnerable/Dockerfile
demo-vulnerable/README.md
demo-vulnerable/manifest/benchmark-v1.json
demo-vulnerable/manifest/evaluation-safe.template.json
demo-vulnerable/manifest/evaluation-vulnerable.template.json
demo-vulnerable/manifest/ground-truth-v1.json
demo-vulnerable/manifest/schemas/benchmark-v1.schema.json
demo-vulnerable/manifest/schemas/bindings-v1.schema.json
demo-vulnerable/manifest/schemas/evaluation-scenario-v1.schema.json
demo-vulnerable/manifest/schemas/ground-truth-v1.schema.json
demo-vulnerable/manifest/schemas/scenarios-v1.schema.json
demo-vulnerable/mvnw
demo-vulnerable/mvnw.cmd
demo-vulnerable/pom.xml
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/DemoApplication.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/config/CatalogLoader.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/config/DemoConfiguration.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/config/DemoSettings.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/config/SafeErrorReportValve.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/http/AdmissionGate.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/http/DemoHttpFilter.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/http/DemoJson.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/http/RequestFailure.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/scenario/DemoProfile.java
demo-vulnerable/src/main/java/com/llmscannerseguridad/demo/scenario/ScenarioCatalog.java
demo-vulnerable/src/main/resources/application.yml
demo-vulnerable/src/main/resources/benchmark/scenarios-v1.json
demo-vulnerable/src/test/java/com/llmscannerseguridad/demo/HttpContractTest.java
demo-vulnerable/src/test/java/com/llmscannerseguridad/demo/JsonSchemaCheck.java
demo-vulnerable/src/test/java/com/llmscannerseguridad/demo/ManifestTest.java
demo-vulnerable/src/test/java/com/llmscannerseguridad/demo/ScannerBoundaryTest.java
demo-vulnerable/src/test/java/com/llmscannerseguridad/demo/ScenarioTest.java
demo-vulnerable/src/test/java/com/llmscannerseguridad/demo/TestJson.java
demo-vulnerable/src/test/resources/benchmark/probes-v4-reference.json
```

Actualizados también `openspec/changes/demo-vulnerable/tasks.md` y las referencias directamente afectadas de `Readme.md`, `docs/02-arquitectura/arquitectura-general.md`, `docs/03-seguridad/politica-de-autorizacion.md`, `docs/04-ia/casos-de-prueba.md` y `docs/08-entrega-tfm/checklist-entrega.md`. No se modificaron los demás artefactos históricos del change ni backend/frontend/Compose.

### Desviaciones y pendientes

Sin desviaciones contractuales respecto de DV-01..DV-16/D-01..D-26. El cambio adicional de esta fase solo elimina la dependencia de un test normal respecto a un archivo de backend y completa la evidencia de ejecución real del jar. No cambia evaluation-engine ni SSRF.

Permanecen pendientes, fuera de esta verificación local: build/arranque Docker y disponibilidad de sus etiquetas; safety con egress denegado y observación externa de red/procesos/escrituras; binding operativo real y E2E completo; operación real de OpenAI/exportación; dataset/repeticiones/estadística, frontend-mvp 9.4 y entrega TFM. La QA loopback y revisión de código no sustituyen esa observación aislada. No hubo commits, push, deploy ni scanner→demo E2E.

## 12. Corrección F-02 — aislamiento del entorno del operador — 2026-10-15

Alcance exclusivo: `HttpContractTest.java`, `ScenarioTest.java` y esta evidencia. Sin cambios de runtime, arquitectura, backend, frontend, dependencias, configuración de despliegue o artefactos OpenSpec de requisitos/diseño.

### Corrección aplicada y revisión dirigida

- Las dos instancias de HttpContractTest declaran explícitamente su perfil: SAFE o VULNERABLE. Un helper privado de arranque usa MockEnvironment (sin heredar fuentes systemEnvironment/systemProperties del operador), `spring.config.location=classpath:/application.yml`, DEMO_BIND_ADDRESS=127.0.0.1, DEMO_PORT=8081, server.address=127.0.0.1 y server.port=0 (puerto efectivo efímero). No se utilizan defaults ambientales para las fixtures HTTP.
- El default SAFE permanece probado en `absentProfileUsesSafeDefaultInAnEnvironmentWithoutOperatorProperties`: contexto Spring con MockEnvironment vacío, comprobación explícita de que DEMO_PROFILE está ausente, y verificación del DemoSettings real (SAFE/127.0.0.1/8081) y de la respuesta segura del catálogo. No se deduce la ausencia a partir del entorno real del operador ni se cambia el proceso padre para ese test.
- El test de perfil inválido también usa MockEnvironment, eliminando la herencia accidental de bind/puerto que podía alterar la causa del fallo. No se añadió ninguna abstracción de producción.
- Revisados los seis archivos de test y los puntos de configuración relacionados: estos son los únicos arranques/contextos Spring de la demo. Los demás tests usan perfiles/fixtures/expectativas/clock controlados; los caps de JSON/admisión/conector son constantes del runtime. El banco F-01 recibe configuración técnica explícita y su jar se activa deliberadamente por `-Dscanner.validation.jar`; no hereda configuración de profile/bind/puerto del operador. No se encontraron otras dependencias ambientales reales relacionadas que requiriesen cambios.

### Validación ejecutada

Java/javac: **21.0.12.1**. Maven **offline** en las tres ejecuciones; sockets únicamente loopback, sin proveedor ni E2E. Se habilitó el banco F-01 contra el mismo jar existente mediante:

```text
-Dscanner.validation.jar=C:\Users\Develop\Desktop\llm-scanner-seguridad\backend\target\llm-scanner-seguridad-0.0.1-SNAPSHOT.jar
```

Comando de suite normal/build desde raíz (el lifecycle package ejecuta la suite completa):

```powershell
.\demo-vulnerable\mvnw.cmd -o -f demo-vulnerable/pom.xml '-Dscanner.validation.jar=C:\Users\Develop\Desktop\llm-scanner-seguridad\backend\target\llm-scanner-seguridad-0.0.1-SNAPSHOT.jar' package
```

Para las dos simulaciones se ejecutó el mismo comando con `test` en lugar de `package`, asignando las variables al proceso PowerShell padre antes de lanzar Maven y restaurándolas en finally.

| Ejecución | Entorno del padre / evidencia | Resultado real |
| --- | --- | --- |
| Suite normal + package | Configuración ordinaria; finalización 23:25 Europe/Madrid | **22 tests PASS**, cero fallos/errores/omitidos; **BUILD SUCCESS**, jar empaquetado |
| Suite con perfil externo | DEMO_PROFILE=VULNERABLE; salida `F02_PARENT_DEMO_PROFILE=VULNERABLE`, seguida de `DEMO_STARTED profile=SAFE scenarioVersion=1 port=56142` y de la instancia VULNERABLE | **22 tests PASS**, cero fallos/errores/omitidos; finalización 23:27 |
| Suite con configuración externa alterada | Overrides completos de la tabla siguiente; la instancia SAFE arrancó como SAFE en puerto efímero 49820 y la VULNERABLE en 49821 | **22 tests PASS**, cero fallos/errores/omitidos; finalización 23:28 |

Overrides de la tercera ejecución, establecidos realmente en el proceso padre:

| Variable | Valor simulado |
| --- | --- |
| DEMO_PROFILE | VULNERABLE |
| DEMO_BIND_ADDRESS | 203.0.113.1 (literal no local; nunca se conecta a él) |
| DEMO_PORT | not-a-port |
| SERVER_ADDRESS | 203.0.113.1 |
| SERVER_PORT | not-a-port |
| SERVER_COMPRESSION_ENABLED | true |
| SERVER_TOMCAT_CONNECTION_TIMEOUT | 1ms |
| SERVER_MAX_HTTP_REQUEST_HEADER_SIZE | 64B |
| SPRING_CONFIG_LOCATION | file:./target/f02-missing-config.yml |
| SPRING_APPLICATION_JSON | {"DEMO_PROFILE":"VULNERABLE","DEMO_BIND_ADDRESS":"203.0.113.1","server":{"port":1,"compression":{"enabled":true}}} |

Desglose por ejecución: HttpContractTest **8**, ManifestTest **5**, ScannerBoundaryTest **1**, ScenarioTest **8**. El nuevo test del default SAFE pasó también en ambas simulaciones externas. La captura F-01 en memoria sigue pasando, sin llamada a OpenAI; la suite no inicia el scanner ni ejecuta scanner→demo E2E.

OpenSpec strict: **PASS**, exit 0, `Change 'demo-vulnerable' is valid`, tras incorporar la evidencia F-02. Revisión de diff: solo los dos tests indicados y esta evidencia respecto al estado previo a F-02; sin diagnósticos de whitespace. Diff de backend/frontend/docker vacío. Los cambios anteriores del change se conservan.

Regresiones observadas: **ninguna**. Docker, aislamiento externo/observación de egress y E2E permanecen pendientes. No hubo commits, push ni deploy.

Estado F-02: **READY FOR RE-REVIEW**.
