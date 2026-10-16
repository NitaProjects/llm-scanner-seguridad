# Corrección exclusiva F3/F4 — 2026-10-16

Estado actual: **READY FOR RE-REVIEW — F3/F4 PASS**. La confirmación manual complementaria recibida después del bloqueo está en la última sección; el registro de la limitación inicial se conserva como histórico. Complementa la evidencia de 2026-10-16-005150; no reemplaza sus IDs ni atribuye al Scan anterior la cabecera capturada en este nuevo lanzamiento. Sin cambios de arquitectura, backend/frontend/demo ni dependencias. Solo JSON operacionales ignorados, datos sintéticos por API/UI y documentación del change.

## F3 — respuesta HTTP real

Se revalidaron proceso scanner PID35456/local/8080, BD de desarrollo, gate export=false, excepción local exacta, demo UP/VULNERABLE/version1, binding real y ausencia de activo. Un POST sin body al scanner real produjo la respuesta conservada en f3-launch-http.json:

```text
HTTP version: 1.1
Status: 202
Location: /api/projects/47006cf8-421d-406c-ab7e-c496e3a67fe5/scans/eb0c960f-dfa6-4968-8b8a-2f3c9d2f3793
Body.id: eb0c960f-dfa6-4968-8b8a-2f3c9d2f3793
Body.projectId: 47006cf8-421d-406c-ab7e-c496e3a67fe5
Body.status: PENDING
```

Location se obtuvo directamente de reply.Headers['Location']; el valor esperado se construyó únicamente para comparación, nunca como sustituto de la cabecera. Coincide literalmente con el recurso scoped exigido por OpenAPI y contiene el id real del body. GET a esa ruta confirmó el mismo Scan/Project. No se guardan cabeceras ajenas, cookies ni auth. El campo reason de la herramienta era vacío; no se inventa una reason phrase.

Intento UTC 2026-10-16T16:23:43.9233435+00:00. Un observador API serial, HTTP ≤10 s, pausas 3 s, deadline360 s y tres errores consecutivos como límite; sin reintentar POST. f3-polling.json registra RUNNING, RUNNING y COMPLETED. Duración del worker 3.112346 s. Respuestas públicas íntegras f3-scan/executions/evaluations/findings/score/report.json y comprobaciones f3-checks.json.

Regresión funcional: cuatro ejecuciones COMPLETED/HTTP200, cuatro evaluaciones COMPLETED/INCONCLUSIVE/confidence0.00/SKIPPED/EXPORT_NOT_AUTHORIZED, cero Findings, score AVAILABLE/0/NO_FINDINGS/cuatro I, report AVAILABLE/report-v1/es y summary igual a score. Ninguna divergencia observada. F3 PASS; complementa el lanzamiento UI ya acreditado para 4.4.

## F4 A — formularios reales

Desde Angular Projects se abrió «Nuevo proyecto», se completaron nombre/descripcion y se pulsó «Crear proyecto». Navegó al id real **65f132db-20c5-40b8-a3ee-6ee7d2cb2d76**, nombre `E2E Demo Vulnerable QA F4`, inicialmente sin Target. 01-project-form.png muestra el formulario, recortado al propio formulario para no conservar proyectos ajenos.

Se abrió «Configurar target», se rellenó y guardó el contrato exacto benchmark-v1: URL http://127.0.0.1:8081/api/chat, POST, application/json, template string {"message":"{{PROMPT}}"}, placeholder {{PROMPT}}, responsePath $.response, auth NONE, headers vacíos y timeout5000. Angular mostró «Configuración guardada». Al recargar, los valores persistidos permanecieron correctos; volver al Project mostró el Target y «Ejecutar análisis». 02-target-form.png conserva la relectura tras refresh; no se guardó de nuevo después del binding.

GET reales f4-project.json/f4-target.json corroboran Project, **targetId 3ae92e44-4e81-427f-93ce-021a6a751643** y **updatedAt literal 2026-10-16T16:25:10.710151Z**. Sin credenciales. Se preservó binding-before-f4.json y se renovaron solo los dos JSON locales ignorados con esos IDs/fecha, la misma plantilla VULNERABLE y export false. Ambos esquemas válidos, sin oracle añadido ni reinicio. f4-binding.json preserva esta preparación.

F4 A PASS: el Target resultó utilizable en el Scan real descrito abajo, con cuatro ejecuciones HTTP200. No errores funcionales ni correcciones de producto.

## F4 B — abandono del componente durante estado activo: registro anterior a la confirmación manual

La semántica aprobada es detener polling/suscripciones al salir de ScanPage; no cancelar el trabajo remoto ni invocar un endpoint de cancelación. Se lanzó desde ProjectPage el Scan **12a55098-d2b5-4a36-8662-d62b360b99a1** y se observó «Análisis en curso», una de cuatro pruebas finalizada y resultados pendientes. 03-polling-active.png y f4-ui-lifecycle.json conservan esta observación real.

| Evento | UTC observado |
| --- | --- |
| Intento UI | 2026-10-16T16:32:40.407Z |
| Estado activo observado | 2026-10-16T16:32:40.665Z |
| Salida a ProjectPage | 2026-10-16T16:32:40.715Z |
| Scan terminó remotamente | 2026-10-16T16:32:43.590013Z |
| Retorno a ScanPage | 2026-10-16T16:33:24.752Z |

44.037 s fuera, sin ralentizar el scanner. El histórico mostró el activo al salir; volver obtuvo COMPLETED con duración3.123 s, cero Findings y cuatro I. 04-returned-scan.png. APIs f4-scan/executions/evaluations/findings/score/report.json confirman finalización mientras el componente estaba abandonado, identidades/Target/fecha correctas, cuatro ejecuciones/evaluaciones COMPLETED, score0/NO_FINDINGS y report-v1/es coherente e inmutable. Esto demuestra que navegar no alteró indebidamente el Scan remoto y que reentrar recuperó su estado real.

**Ausencia de nuevas peticiones periódicas: NOT_VERIFIED todavía.** El navegador integrado no expone Network/Resource Timing mediante su API disponible. El operador ofreció observar Network, pero la captura aportada después correspondía a Chrome en /projects, mientras el agente había operado otra pestaña del navegador integrado. Cero coincidencias sin baseline en esa otra pestaña no prueba la cancelación del observador original. El GET /projects/{projectId}/scans observado es la carga del histórico, no polling de un Scan individual. No se convierten esos datos en PASS.

Se solicitó una comprobación complementaria en la misma pestaña Chrome del operador, con Preserve log, baseline del nuevo scanId, salida mientras activo, al menos12 s fuera y retorno; sin repetir automáticamente lanzamientos. Su resultado real aún no se ha recibido. 5.1 y 6.3 permanecen pendientes hasta demostrar esta cobertura. La revisión de unsubscribe en fuente no sustituye a esta observación runtime.

## Conservación y validaciones al registrar el bloqueo inicial

Cuatro capturas nuevas, dos lanzamientos del agente separados por propósito y todos los históricos conservados. SQL/campos internos NOT_INSPECTED. 9.4 sigue pendiente porque no existe Finding real que abrir. Sin proveedor real, AWS, deploy, Docker, benchmark, commit/push ni nuevas suites. No hubo cambios de código que justifiquen tests afectados. La comprobación de contratos de f3-checks/f4-checks PASS no convierte en verificado el dato Network pendiente.

La tarea 6.3 se desmarcó antes de comenzar, corrigiendo el cierre prematuro. 4.3 y 4.4 tienen ya evidencia real; 5.1/6.3 no se cerrarán por inferencia. El runbook incorpora el procedimiento correcto de observación en una única pestaña. `OPENSPEC_TELEMETRY=0; openspec validate e2e-integration --strict` ejecutado el 2026-10-16: `Change 'e2e-integration' is valid`, exit0, fuera del sandbox para cargar el esquema local. PASS estructural, sin acreditar cierre de F4 ni las tareas pendientes.

Bloqueo concreto de entrega: falta el conteo de nuevos GET al Scan individual y sus subrecursos durante una salida en activo, observado en la misma pestaña con baseline inicial. La captura de otra pestaña no lo demuestra. No se añaden lanzamientos automáticos. Binding operacional actual conserva el Project QA para la comprobación manual solicitada al operador; no se cambia mientras esa comprobación pueda estar en curso.

## Confirmación posterior y cierre F4 — 2026-10-16

El operador confirmó explícitamente en esta conversación:

> Confirmado en la misma pestaña de Chrome con Keep log activo: salí del Scan mientras estaba activo y, durante más de 12 s en la página del Project, no aparecieron nuevos GET periódicos del scan individual. Las peticiones visibles restantes pertenecen al recorrido previo y a la carga normal del Project. Polling detenido correctamente.

**F4 PASS mediante evidencia manual complementaria.** Fuente: f4-network-operator-confirmation.json. Mismo navegador/pestaña, salida mientras activo, observación superior a12 s y cero nuevos GET periódicos; las peticiones previas y la carga del Project quedan excluidas. Esto resuelve el bloqueo registrado arriba y permite completar 5.1/6.3. No se presenta como observación Network directa del agente ni como validación de la captura anterior de otra pestaña.

La confirmación no incluye UUID ni timestamps exactos. Se conserva literalmente con esa limitación de procedencia, sin inventar una correlación a un Scan concreto. Como comprobación independiente, el histórico del mismo Project QA muestra los Scans complementarios 738c599b-1c4d-4db5-bbad-cbff6f2588c1 y b15b9301-5d46-4a34-b452-11e58da38416. Se recogieron sus seis respuestas públicas sin mutaciones: ambos COMPLETED en 3.106125/3.111577 s, cuatro ejecuciones HTTP200 y cuatro evaluaciones inconclusas completadas, SKIPPED/EXPORT_NOT_AUTHORIZED/confidence0; cero Findings, score0/NO_FINDINGS/cuatro I y report AVAILABLE/report-v1/es, resumen concordante y binding correcto. qa-complementary-checks.json registra este contraste separado.

F3 conserva su Location real y PASS; F4 formularios, abandono/reentrada/estado remoto y Network quedan cubiertos por las evidencias complementarias identificadas. 4.3/4.4/5.1/6.3 completas; 22/29 tareas completas, cinco opcionales no aplicables, cierre operacional6.4 y revisión6.6 pendientes. 9.4 continúa pendiente por falta de Finding real que abrir. Sin regresiones observadas ni cambios de código, por lo que no se ejecutaron tests afectados. SQL NOT_INSPECTED, proveedor real/AWS/Docker/benchmark sin ejecutar. No se generaron nuevos Scans ni capturas durante este cierre. Strict final ejecutado el 2026-10-16: `OPENSPEC_TELEMETRY=0; openspec validate e2e-integration --strict` → `Change 'e2e-integration' is valid`, exit0, registrado también en tasks.md sección11. Estado **READY FOR RE-REVIEW**.
