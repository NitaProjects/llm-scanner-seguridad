# e2e-integration

Estado de corrección: **READY FOR RE-REVIEW — F3/F4 resueltos** (2026-10-16). 23/29 tareas completas; cinco opcionales no aplicables y 6.6 pendiente de revisión. Cierre operacional 6.4 completado y documentado en sección 12. 4.3/4.4/5.1/6.3 completadas con evidencia real. La confirmación manual de Network y la comprobación API complementaria están en la sección 11 y evidence/2026-10-16-f3-f4/; las secciones 9/10 conservan los estados históricos.

## 1. Aprobación y procedimiento operativo

- [x] 1.1 Registrar la revisión y aprobación explícita de D-01..D-24 antes de iniciar implementation-agent; verificar referencia real a la autorización, alcance OFFLINE y exclusiones, sin autodeclarar aprobación por existir artefactos (E2E-01).
- [x] 1.2 Preparar `openspec/changes/e2e-integration/runbook.md` a partir de D-04..D-10 con comandos propuestos de los cuatro componentes, preflight BD/esquema/puertos/artefactos, SSRF y secuencia UI; verificar que no requiere instalaciones, migraciones automáticas ni secretos y distingue instrucciones de acciones ejecutadas; verificar también la vía manual completa sin script ni SQL obligatorio, con declaración de preparación BD y diagnóstico interno NOT_INSPECTED cuando corresponda (E2E-02/E2E-03/E2E-04/E2E-12/E2E-19).
- [x] 1.3 Incorporar al runbook manifest/binding, tabla de resultados offline, QA, matriz externa, limpieza y evaluador futuro; verificar correspondencia E2E-05..E2E-24 y que proveedor/SAFE/9.4 completo no son requisitos de éxito del primer E2E (E2E-07/E2E-11/E2E-16/E2E-24).

## 2. Apoyo opcional: herramienta local de preparación

- [ ] 2.1 (Opcional, solo si se elige script) Implementar únicamente `scripts/e2e-local.ps1` con modos Prepare/Collect, entradas tipadas, destinos locales fijos, redirects deshabilitados, errores sanitizados y códigos 0/1/2/3; verificar por revisión que no hay proceso/DB/secretos/proveedor/retries de POST/dependencias nuevas y que el inicio principal sigue en UI (E2E-19; D-19).
- [ ] 2.2 (Opcional, solo si se elige script) Implementar preflight HTTP y registro de declaración operacional, sin presentar variables del script como configuración del scanner; verificar health/version/perfil, API/proxy, target/binding y ausencia de activo del Project, devolviendo fallo explícito ante precondición inválida (E2E-02/E2E-04/E2E-08).
- [ ] 2.3 (Opcional, solo si se elige script) Implementar Prepare con id explícito de Project existente y, alternativamente, creación API explícita de proyecto/target sintéticos; verificar respuestas/ids reales, no selección por nombre, no PUT implícito ni reintento de escritura ambigua (E2E-05/E2E-06/E2E-19).
- [ ] 2.4 (Opcional, solo si se elige script) Automatizar la preparación de las dos copias locales ya ignoradas de demo a partir de plantilla/esquema vigentes; verificar fecha UTC exacta sin pérdida de precisión, health/autorización, cuatro entries y corpus del perfil, un escenario/binding, export false y rechazo de tokens sin resolver (E2E-07/E2E-18).
- [ ] 2.5 (Opcional, solo si se elige script) Registrar hashes/revisión y rutas no secretas de la preparación; verificar no sobrescritura silenciosa de binding distinto, no artefactos operacionales versionados dentro de demo y ningún cambio a fuentes/configuración backend/frontend/demo (E2E-07/E2E-21/E2E-23).

## 3. Recopilación por API y validaciones

- [x] 3.1 Definir y aplicar la recopilación manual por API sobre ProjectId/ScanId/instante de inicio reales con pertenencia comprobada y seguimiento serial, o Collect solo si se elige el script opcional; verificar deadline original 360 s, HTTP ≤10 s/tiempo restante, pausa 3 s acotada, tres fallos consecutivos y terminal sin polling adicional, sin convertir timeout local en FAILED ni relanzar Scan (E2E-09/E2E-10).
- [x] 3.2 Leer los cinco bloques finales del Scan y validar cardinalidad/identidad/code/revision/status; verificar cuatro ejecuciones/evaluaciones COMPLETED, estados I/SKIPPED/HYB-01/EXPORT_NOT_AUTHORIZED/confidence 0.00 y Findings vacíos conforme D-11, registrando cualquier divergencia como fallo (E2E-09/E2E-11).
- [x] 3.3 Validar score persistido y breakdown sin recalcular la fuente de verdad; verificar AVAILABLE/scoring-v1, ids/fechas, 0/NO_FINDINGS, cuatro I y contadores/severidades coherentes con evaluaciones/Findings (E2E-13).
- [x] 3.4 Validar report-v1/es y relectura inmutable; verificar AVAILABLE, scope/ids/fechas, summary idéntico a score, cuatro evaluaciones, findings vacío, interpretation y nueve limitations exactas de OpenAPI (E2E-14).
- [x] 3.5 Registrar evidencia API mínima manual o asistida, distinguiendo comprobación API de aceptación funcional con QA UI/matriz completas; verificar que campos internos no inspeccionados quedan NOT_INSPECTED y no bloquean aceptación sin SQL, y que matriz/QA obligatorias no se rellenan desde ground truth ni se ocultan como completadas (E2E-12/E2E-17/E2E-21).

## 4. Ejecución OFFLINE autorizada

- [x] 4.1 Obtener autorización operativa concreta y ejecutar preflight manual BD/servicios/configuración; verificar llm_scanner_seguridad local, esquema ya preparado, artefactos identificables, puertos D-02 y exportación false sin clave suministrada, bloqueando si falta alguna condición (E2E-01..E2E-04).
- [x] 4.2 Arrancar únicamente los componentes autorizados mediante las terminales del operador, reutilizando PostgreSQL activo; verificar demo VULNERABLE/UP/version 1 y proxy Angular→scanner, sin instalar dependencias, migrar, operar Docker o gestionar procesos ajenos (E2E-02/E2E-04/E2E-08).
- [x] 4.3 Crear Project y Target desde Angular y preparar manualmente binding/manifiesto con el id real antes del Scan, o usar Prepare si se elige el script opcional; verificar contrato exacto, binding/updatedAt, autorización local, corpus/export false y health, sin volver a guardar target después (E2E-05/E2E-06/E2E-07/E2E-15).
- [x] 4.4 Lanzar un único Scan desde ProjectPage y observar hasta terminal con la UI; verificar POST sin body/202/Location/id, ausencia de solapamiento, plazo operacional y carga final, sin sleeps artificiales ni proveedor (E2E-09/E2E-10/E2E-15).
- [x] 4.5 Recopilar manualmente por API los resultados reales de ese Scan, o usar Collect si se elige el script opcional; verificar D-11..D-14, conservar fallos observados y repetir health para confirmar perfil estable, sin redefinir expectativas (E2E-08/E2E-11/E2E-13/E2E-14).
- [x] 4.6 Registrar campos internos como NOT_INSPECTED cuando no se realice diagnóstico SQL, sin bloquear aceptación funcional API/UI. Solo si hay autorización de lectura y se elige inspeccionar, adjuntar extracción complementaria D-12 con transacción READ ONLY y filtros Project/Scan/target/fecha reales; contrastar respuesta persistida/señales/exportAuthorized/llm_attempt_count sin inventar valores ni usar SELECT *, secretos, tablas ajenas, UPDATE/DELETE u otra fabricación de estados (E2E-12/E2E-18).
- [x] 4.7 Completar las cuatro filas funcionales API/UI de evidencia, con internos NOT_INSPECTED si no se observaron, y la matriz ground truth/observado externa; verificar procedencia, unión caso/perfil/version/code/revision/Scan, I separada de TP/TN/FP/FN y fallos técnicos sin clasificación diagnóstica (E2E-12/E2E-17/E2E-21).

## 5. QA frontend y criterio 9.4

- [x] 5.1 Realizar QA de Projects/Project detail/Target/ScanPage/resultados sobre ids del recorrido; verificar navegación, refresh/rutas directas, cancelación al salir, score/risk/categorías, estado vacío de Findings, cuatro I y report/limitaciones, sin otro frontend-polish (E2E-15).
- [x] 5.2 Preparar la matriz de cobertura del criterio literal 9.4; verificar paso por paso sus evidencias, señalar apertura de Finding PENDING en offline y conservar 9.4 pendiente, sin modificar el change histórico para cerrar cobertura faltante (E2E-16).
- [x] 5.3 Guardar hasta cuatro capturas representativas y referencias de UI/Network sanitizadas; verificar que no contienen secretos/datos ajenos, HAR completo o resultados simulados y que una ejecución rápida no se ralentizó artificialmente (E2E-15/E2E-21).

## 6. Defectos, conservación y entrega a revisión

- [x] 6.1 Registrar defectos reales, si aparecen, mediante reproducir→localizar→corrección mínima autorizada→regresión no cubierta→reejecutar→evidencia; verificar que cambios de producto requieren autorización específica y que no se modifican expectativas para esconder fallos. Si no hay defectos, registrar «ninguno observado» sin inventar tests (E2E-20/E2E-22).
- [x] 6.2 Si un defecto exige tests nuevos, proponer/añadir solo la regresión necesaria y ejecutar comprobaciones únicamente ante solicitud explícita; verificar que cualquier test con BD valida llm_scanner_seguridad_test antes de escribir y nunca usa la BD manual. Sin defecto, registrar no aplicable (E2E-03/E2E-20).
- [x] 6.3 Consolidar paquete mínimo con comandos/configuración no secreta/ids/estados/matriz/capturas y resultado PASS/FAIL/PENDING; verificar ausencia de dumps/logs extensos/secrets, cobertura funcional API/UI completa para aceptar OFFLINE sin exigir SQL ni script, y limitaciones F-01/LLM10/cero/I visibles (E2E-18/E2E-21).
- [x] 6.4 Aplicar política de conservación/reutilización y cierre operacional por el operador; verificar ningún borrado BD ni API nueva, Project/Scans históricos conservados y revocación de flags locales cuando ya no se necesiten, sin que el script mate procesos (E2E-23).
- [x] 6.5 Revisar runbook futuro de evaluador sin ejecutarlo; verificar autorización independiente, clave solo EVALUATOR_API_KEY del scanner, gates/corpus/límites/presupuesto/modelo aprobados, comparación externa y SAFE opcional, sin convertirlo en tarea operativa obligatoria (E2E-24).
- [ ] 6.6 Entregar a review-agent el diff acotado y paquete de evidencia; verificar alineación proposal/spec/design/tasks/implementación, estado real de OFFLINE, pendientes 9.4/proveedor y strict solo si se solicita; no marcar requisitos TFM ni cerrar 9.4 sin evidencia suficiente (E2E-01/E2E-16/E2E-21/E2E-22).

## 7. Registro de planificación

Change creado mediante CLI OpenSpec el 2026-10-15; artefactos redactados/revisados hasta 2026-10-16. El sandbox no pudo cargar el esquema local spec-driven; scaffold y consultas del esquema se ejecutaron fuera del sandbox tras autorización, con OPENSPEC_TELEMETRY=0. No se accedió a servicios externos. La existencia/completitud de artefactos no acredita aprobación, implementación ni E2E.

Validación solicitada ejecutada el 2026-10-16: `openspec validate e2e-integration --strict` → `Change 'e2e-integration' is valid`; ejecución CLI finalizada con exit code 0. `openspec status --change e2e-integration --json` confirmó proposal/specs/design/tasks done e isPlanningComplete=true. Ese estado solo certifica la presencia de planificación; las 29 tareas de implementación/operación siguen pendientes. Inventario revisado: una capacidad, 24 requisitos, 27 escenarios, 24 decisiones, cuatro artefactos canónicos más índice spec.md y scaffold .openspec.yaml. `git status --short` muestra únicamente el nuevo directorio del change; no se modificaron backend/frontend/demo/docs externos.

Estado de entrega tras corregir F1/F2: **READY FOR RE-REVIEW**. Pendientes: revisión/aprobación humana, implementación operacional autorizada, preflight/E2E real y sus evidencias; 9.4 conserva pendiente la apertura de Finding. Proveedor real/SAFE posterior y entrega TFM permanecen fuera de aceptación obligatoria de este primer recorrido.


## 8. Corrección exclusiva F1/F2 — 2026-10-16

F1: E2E-12/E2E-19/E2E-21 y D-12/D-19/D-21 permiten aceptación funcional exclusivamente con APIs + UI; la consulta SQL queda como diagnóstico complementario autorizado. Campos internos no inspeccionados se registran NOT_INSPECTED sin inventar valores. Se elimina también la dependencia indirecta de lectura SQL en E2E-04/D-04 y en Risks/Migration Plan/Open Questions. Se conserva la consulta READ ONLY con filtros Project/Scan/target/fecha, sin UPDATE/DELETE ni fabricación de estados.

F2: E2E-19/D-19 conservan el diseño Prepare/Collect como apoyo opcional. Las tareas 2.1–2.5 dejan de ser entregables MUST; su omisión documentada no bloquea aceptación. Las tareas 1.2, 3.1/3.5, 4.3/4.5/4.6/4.7 y 6.3 admiten procedimiento manual reproducible y separan evidencia funcional de diagnóstico interno. Proposal e índice spec.md quedan alineados. No se implementa código ni se operan servicios/BD/proveedores.

Revisión de regresiones documentales: los restantes bloques normativos y decisiones permanecen idénticos, incluidos comportamiento offline, TargetConfiguration/SSRF, BD dev/test, polling 360 s/3 s, ground truth, frontera F-01, task 9.4, scoring/reporting y evaluador futuro. Inventario tras corrección: 24 requisitos, 30 escenarios, 24 decisiones y 29 tareas futuras (cinco de apoyo opcional). Validación de esta revisión ejecutada el 2026-10-16: `openspec validate e2e-integration --strict` → `Change 'e2e-integration' is valid`, exit code 0; status confirma los cuatro artefactos canónicos done y planificación completa. No acredita implementación ni E2E ejecutado.

Estado: **READY FOR RE-REVIEW**; las correcciones F1/F2 están autorizadas por la solicitud del usuario, sin aprobación implícita de apply ni de operación E2E.

## 9. Ejecución autorizada y evidencia — 2026-10-16

Aprobación humana explícita en esta conversación: «Implementa exclusivamente el change OpenSpec e2e-integration. Estado: APPROVED, READY FOR IMPLEMENTATION, F1 RESOLVED, F2 RESOLVED, OpenSpec strict PASS». Las instrucciones siguientes autorizaron liberar 8080 por el operador, arrancar scanner/demo/Angular con configuración verificable, crear Project/Target por API, completar el binding y finalmente ejecutar un Scan, QA y OpenSpec strict. No se reabrió arquitectura. Proposal/design/spec conservan su estado histórico de planificación; este registro documenta la autorización y ejecución posterior.

Fuente: [result.md](evidence/2026-10-16-005150/result.md), seis respuestas públicas, checks/categories/ground-truth-comparison/hashes JSON y cuatro PNG. [runbook.md](runbook.md) entrega procedimiento manual, preflight, preparación, polling, conservación y operación futura del proveedor sin ejecutarla. No se creó script: 2.1–2.5 NO APLICABLE, sin marcarlas implementadas.

Project 47006cf8-421d-406c-ab7e-c496e3a67fe5, Target d774c042-1ac9-4bf5-97b0-45df91b068e0, updatedAt exacto 2026-10-15T22:39:37.757848Z. Scanner PID35456 local/8080; BD de desarrollo llm_scanner_seguridad; Flyway validó siete migraciones, public7 actualizado, sin migración nueva. SSRF local true/origen exacto http://127.0.0.1:8081; export false. Demo VULNERABLE/UP/version1 antes/después, Angular4200/proxy verificado. Binding/manifiesto ignorados, schema válido, tres sustituciones y carga aislada con el adaptador empaquetado verificada; sin reinicio al completar el JSON.

Un clic real desde ProjectPage: Scan **1cb759e5-ffa5-4e5e-b7de-0d3a12004151**, COMPLETED, 3.284837 s. Polling exclusivamente Angular, Pendiente→Completado, deadline operacional 360 s respetado. Cuatro TestExecutions COMPLETED/HTTP200; cuatro EvaluationResults COMPLETED/INCONCLUSIVE, confidence0.00, SKIPPED, HYB-01, EXPORT_NOT_AUTHORIZED, degraded=true, sin errores ni Finding. Findings []; score AVAILABLE/scoring-v1, 0/NO_FINDINGS, cuatro I y una I por categoría. Report AVAILABLE/report-v1/es, summary literal del score, cuatro evaluaciones, cero Findings, interpretación y nueve limitaciones exactas, orden de fechas coherente y relectura inmutable. Matriz externa: cuatro VULNERABLE/INCONCLUSIVE→INCONCLUSIVE, nunca FN. Cero Findings no acredita seguridad.

QA de resultados API/UI PASS: Projects/Project/Target actual, lanzamiento, progreso automático/terminal, score/categorías, vacío/cuatro I, report/trazabilidad/limitaciones, histórico/retorno/enlaces/refresh de la misma ruta. SQL, respuesta persistida, señales, llm_attempt_count y exportAuthorized del snapshot interno NOT_INSPECTED. No se inventan datos internos. Cancelación mientras activo no se ejercitó; source review confirma encadenamiento/unsubscribe y cobertura existente, sin ejecutar sus tests.

Pendientes literales conservados:
- 4.3: se crearon Project/Target por API según autorización explícita; creación/guardado mediante formularios Angular no realizados. La relectura UI y binding sí se verificaron.
- 4.4: lanzamiento UI y transición real verificados; ScansApi exige 202 para navegar, pero Location no se capturó directamente en Network (NOT_INSPECTED). No se marca completa toda su evidencia literal.
- 5.1: QA funcional de resultados realizado; creación UI y cancelación durante estado activo no ejercitadas. Sin otro Scan ni demora artificial para simularlos.
- 6.4: historial conservado sin borrados; cierre operacional/revocación de la excepción local pendiente del operador, servicios mantenidos para inspección y export false.
- 6.6: paquete listo para revisión; review-agent no ha sido invocado ni ha emitido revisión, sin subagentes/mensajes a otras sesiones.

9.4 de frontend-mvp **permanece pendiente**, sin editar su histórico: falta abrir un Finding real y comprobar sus datos. La matriz de cobertura literal está en result.md. No exige intrínsecamente proveedor, pero este offline no genera Findings; cualquier recorrido complementario necesita autorización propia.

Defectos de producto: ninguno observado. Correcciones: ninguna. Tests nuevos/suites afectadas: NO APLICABLE; no hay cambios de producto que requieran regresión. Sí se efectuaron las comprobaciones de integración API/UI y de JSON/contratos registradas. No tests automáticos con BD, OpenAI, AWS, deploy, Docker, SAFE, benchmark, commit ni push.

OpenSpec strict ejecutado el 2026-10-16: `OPENSPEC_TELEMETRY=0; openspec validate e2e-integration --strict` → `Change 'e2e-integration' is valid`, exit code 0. Se ejecutó fuera del sandbox por la limitación ya documentada al cargar spec-driven. No acredita que las tareas pendientes estén realizadas. Estado del paquete: **READY FOR REVIEW**, alcance funcional OFFLINE observado PASS y pendientes literales explícitos, sin autodeclarar aprobación de review.

## 10. Corrección F3/F4 — en ejecución, 2026-10-16

Solicitud explícita del usuario: corregir únicamente F3/F4 y desmarcar 6.3 hasta demostrar toda la cobertura. Se desmarcó 6.3 antes de operar. Evidencia complementaria: [result.md](evidence/2026-10-16-f3-f4/result.md); el histórico de sección 9 permanece intacto.

F3 PASS: POST real al Project original → HTTP202, Scan eb0c960f-dfa6-4968-8b8a-2f3c9d2f3793 y Location literal /api/projects/47006cf8-421d-406c-ab7e-c496e3a67fe5/scans/eb0c960f-dfa6-4968-8b8a-2f3c9d2f3793. Cabecera recibida por HTTP, cotejada con body/id/recurso contractual y GET scoped. Terminó COMPLETED/3.112346 s y conserva la semántica offline. 4.4 se completa con esta evidencia adicional real y el lanzamiento UI histórico.

F4 formularios PASS: creación y guardado reales en Angular del Project QA 65f132db-20c5-40b8-a3ee-6ee7d2cb2d76 y Target 3ae92e44-4e81-427f-93ce-021a6a751643, updatedAt 2026-10-16T16:25:10.710151Z. Refresh/API verifican persistencia; binding regenerado antes del Scan, plantilla intacta/export false, origen autorizado sin cambios. 4.3 completada.

Scan UI 12a55098-d2b5-4a36-8662-d62b360b99a1: observado activo antes de salir a ProjectPage, 44.037 s fuera, COMPLETED remotamente mientras tanto; retornar obtiene resultado real, cuatro I, cero Findings, score0 y report disponible. La ausencia de nuevos GET periódicos todavía NO está verificada: la captura manual recibida correspondía a Chrome, distinto de la pestaña integrada que ejecutó el agente. Cero filas sin baseline no es prueba. Se pidió observación complementaria en una única pestaña del operador. 5.1/6.3 permanecen pendientes; no se cierra F4 por inferencia.

Ninguna modificación de código, refactor, dependencia, proveedor, AWS, deploy, commit/push ni borrado. Sin tests afectados: no hubo cambio de código. 9.4 sigue pendiente por falta de Finding real. Strict final y resultado de Network pendientes de registrar al terminar.

Validación posterior: `OPENSPEC_TELEMETRY=0; openspec validate e2e-integration --strict` → `Change 'e2e-integration' is valid`, exit0, el 2026-10-16. No acredita cobertura Network. Estado de entrega **BLOCKED** únicamente para cerrar F4/5.1/6.3: falta confirmar el conteo de nuevos GET durante abandono activo en la misma pestaña con baseline inicial. La captura aportada de Chrome no demuestra la cancelación del observador del navegador integrado. Se conserva preparado el Project/binding QA y el procedimiento del runbook para que el operador comunique esa observación, sin fabricar resultados ni cerrar tareas.

## 11. Cierre F4 mediante evidencia manual complementaria — 2026-10-16

Confirmación explícita del operador, recibida después del bloqueo de sección 10:

> Confirmado en la misma pestaña de Chrome con Keep log activo: salí del Scan mientras estaba activo y, durante más de 12 s en la página del Project, no aparecieron nuevos GET periódicos del scan individual. Las peticiones visibles restantes pertenecen al recorrido previo y a la carga normal del Project. Polling detenido correctamente.

Procedencia y alcance en [f4-network-operator-confirmation.json](evidence/2026-10-16-f3-f4/f4-network-operator-confirmation.json). Evidencia manual del operador, no captura Network del agente. No se reutiliza la captura anterior de otra pestaña ni se atribuye la confirmación a un UUID/timestamp que el operador no proporcionó. El criterio funcional E2E-15 de ausencia de nuevos GET al abandonar en activo queda verificado por esta confirmación complementaria en Chrome.

GET del histórico QA identifica dos Scans posteriores al recorrido del navegador integrado: 738c599b-1c4d-4db5-bbad-cbff6f2588c1 y b15b9301-5d46-4a34-b452-11e58da38416. Sus seis respuestas públicas scoped y qa-complementary-checks.json se conservan aparte. Ambos COMPLETED, cuatro ejecuciones HTTP200 y cuatro evaluaciones COMPLETED/INCONCLUSIVE/confidence0/SKIPPED/EXPORT_NOT_AUTHORIZED; cero Findings, score AVAILABLE/0/NO_FINDINGS/cuatro I y report AVAILABLE/report-v1/es/summary concordante/Target y fecha correctos. No se lanzó otro Scan por el agente para este cierre ni se modificó código.

F3 mantiene PASS por HTTP202 y Location literal recibida del Scan eb0c960f-dfa6-4968-8b8a-2f3c9d2f3793. F4 PASS por formularios reales, binding exacto, abandono en activo/reentrada/estado remoto ya documentados y confirmación manual Network complementaria. Se completan 5.1 y, solo después de conservar toda la evidencia funcional API/UI, 6.3. 4.3/4.4 ya completas. 22/29 tareas completas, cinco opcionales NO APLICABLE y 6.4/6.6 pendientes. 9.4 de frontend-mvp permanece pendiente por ausencia de Finding real que abrir.

Ninguna regresión observada. Tests afectados NO APLICABLE: sin cambios de código, no se ejecutan suites preventivas. SQL continúa NOT_INSPECTED. Sin proveedor real, AWS, Docker, benchmark, deploy, commit/push ni borrados. Runbook actualizado solo con el resultado real. Strict final de este cierre ejecutado el 2026-10-16: `OPENSPEC_TELEMETRY=0; openspec validate e2e-integration --strict` → `Change 'e2e-integration' is valid`, exit0; fuera del sandbox para cargar el esquema local. Estado **READY FOR RE-REVIEW**, sin autodeclarar revisión aprobada.

## 12. Cierre operacional autorizado — tarea 6.4, 2026-10-16

Solicitud explícita del usuario: detener únicamente demo-vulnerable, scanner E2E y Angular de QA, conservar PostgreSQL y datos/históricos, mantener exportación false y revisar los dos JSON ignorados. Evidencia: [operational-close.json](evidence/2026-10-16-f3-f4/operational-close.json). Operación finalizada a las 19:06:51 Europe/Madrid.

Antes de detener se comprobaron identidad, argumentos no secretos y fecha de creación de los procesos propios, y por GET se verificaron los cuatro Projects existentes: ningún Scan PENDING/RUNNING. Scanner PID35456 tenía EVALUATOR_EXPORT_ENABLED=false por argumento explícito. No se inició ni canceló ningún Scan.

Detenidos por PID verificado: Angular3880, scanner35456 y demo16904. Auxiliar esbuild12444 salió con su padre. Comprobación posterior: ninguno de esos PIDs presente y ningún listener en4200/8080/8081. La excepción local y el evaluador dejan de estar operativos al terminar su proceso; no se habilitó exportación/proveedor ni se cambió configuración persistente de usuario/sistema. Último valor efectivo del scanner antes del cierre: export=false.

PostgreSQL conservado: listeners5432 y propietarios de forwarding30876/33500 permanecen; conexión TCP a127.0.0.1:5432 satisfactoria después del cierre. No se detuvo Docker/WSL/PostgreSQL. Solo GET antes de apagar; ninguna escritura/borrado SQL, migración o limpieza. Project/Target/Scans, Findings/score/report y evidencias conservados.

Los archivos demo-vulnerable/manifest/evaluation-scenario.local.json y bindings.local.json se conservan ignorados, sin cambios y fuera de uso: el scanner lector está apagado. SHA-256 antes/después idénticos en operational-close.json. La política aprobada conserva copias/históricos y no exige borrarlos; no se eliminaron ni modificaron plantillas/manifests versionados. Una reutilización futura necesita nuevo preflight y verificación/renovación explícita del binding, nunca reutilización automática.

6.4 completada con cierre real. 23/29 tareas completas, cinco opcionales NO APLICABLE, 6.6/revisión pendiente. 9.4 permanece pendiente por falta de Finding real. Sin cambios de código, nuevas suites, proveedor real, AWS, deploy, commit/push o cambios al runbook/normativa. Estado: **entorno QA cerrado; READY FOR RE-REVIEW**.