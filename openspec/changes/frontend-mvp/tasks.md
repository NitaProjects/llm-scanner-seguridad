## 1. Aprobación y límites de implementación

Estado de implementación **READY FOR RE-REVIEW** (2026-10-10): 39/40 tareas completadas; queda 9.4, recorrido manual condicionado a entorno/endpoints autorizados y manifiesto preparado. D-01..D-30 y FE-01..FE-24 se mantienen sin cambios. La petición posterior del usuario autorizó implementar frontend-mvp y ejecutar tests dirigidos, suite frontend, build, lint si existe y OpenSpec strict. La evidencia inicial está en la sección 11 y las correcciones F1/F2 en la sección 12; el registro de planificación de la sección 10 se conserva como histórico. No se implementa demo-vulnerable ni se acredita integración real, entrega, OpenAI o despliegue.

- [x] 1.1 Registrar aprobación explícita del usuario del 2026-10-10 de D-01..D-30, incorporar las cuatro aclaraciones en FE-01..FE-24/design/tasks y revisar coherencia de los cuatro artefactos; READY FOR IMPLEMENTATION, sin reabrir los siete contratos backend ni implementar código.
- [x] 1.2 Mantener el alcance de cambios en frontend y documentación directamente afectada; verificar por revisión de diff que no se cambian backend, demo-vulnerable, migraciones, versiones, dependencias, lockfiles, despliegue ni requisitos anteriores (FE-01/FE-20/FE-24).

## 2. Base Angular, configuración y navegación

- [x] 2.1 Sustituir bienvenida CLI por shell sobrio con marca, enlace a proyectos y RouterOutlet; poner lang es y título útil; verificar estructura semántica y preparar sustitución del test de «Hello, frontend» por navegación/cabecera observable, conservando bootstrap/builders existentes (FE-01/FE-21/FE-22; D-01/D-02/D-24).
- [x] 2.2 Crear environments production/development con apiBaseUrl `/api`, API_BASE_URL y provideHttpClient; incorporar solo fileReplacement y proxy de desarrollo `/api/**` tras comprobar el puerto local no sensible; verificar que todos los servicios consumen el token y que no se cambia CORS/despliegue/budgets/scripts ni se incluyen secretos (FE-20; D-22).
- [x] 2.3 Definir cuatro rutas lazy, redirección raíz, NotFoundPage y enlaces atrás/breadcrumbs; preparar pruebas RouterTestingHarness de entrada directa, ruta desconocida y cambio de parámetros sin navigation state; verificar que no existen guards/login ni páginas extra fuera del diseño, salvo CanDeactivate de target (FE-01; D-01..D-03).
- [x] 2.4 Crear tokens CSS/base y Shared StatusBadge/FeedbackPanel con estado textual, foco visible y estilos encapsulados; verificar por revisión los mappings separados Severity/RiskLevel y preparar assertions de loading/empty/error, labels y contenido no basado solo en color (FE-13/FE-19/FE-21/FE-22; D-23..D-26).

## 3. Contratos, HTTP y fixtures

- [x] 3.1 Crear tipos readonly Project/Target y requests de creación/actualización con auth discriminada y respuesta sin secretValue; cotejar todos los campos/nulls con schemas OpenAPI y preparar fixtures de NONE/Bearer/API key sin credenciales reales (FE-02..FE-06/FE-20; D-21/D-29).
- [x] 3.2 Crear tipos Scan, TestExecutionMetadata, EvaluationListing/EvaluationMetadata y Finding, separando status/finalVerdict/llmStatus; cotejar siete/catorce/veintiséis/quince campos, enums y nulabilidad con OpenAPI y preparar estados activos/terminales/históricos (FE-09/FE-14/FE-15/FE-18/FE-20; D-21).
- [x] 3.3 Crear ScoreResponse discriminado, ScanScore/CategoryScore/SeverityCounts y ReportResponse/SecurityReport/ReportEvaluation/versions/scope/interpretation/limitations; verificar once/doce/diecinueve/siete campos y cuatro categorías/nueve limitaciones con contrato, sin reutilizar evaluación amplia como report ni introducir fórmulas (FE-10..FE-17/FE-20; D-21).
- [x] 3.4 Crear narrowing/decoders mínimos de respuestas unknown y error mapper seguro con ApiClientError local; preparar respuestas válidas, enum/versión/shape incompatibles, nulls, status0/timeout/HTML/5xx y fieldErrors; verificar por revisión que no hay coerción/default de riesgo, body libre, SQL, stack o código backend inventado (FE-19/FE-20/FE-23; D-19/D-21).
- [x] 3.5 Implementar ProjectsApi y TargetApi con métodos/endpoints exactos; preparar HttpTestingController para GET/POST/PUT, inputs sin campos generados, conservación authConfig sin secretValue, errores y cero interceptores que copien auth del target al scanner (FE-02..FE-06/FE-20; D-20/D-27).
- [x] 3.6 Implementar ScansApi scoped y start sin body; preparar pruebas de las ocho operaciones (start/list/get y executions/evaluations/findings/score/report), 202, errores con/sin Location y Location externa rechazada; verificar ausencia de retry de escritura, endpoint individual Finding o generación/export (FE-07..FE-10/FE-20; D-10/D-20).
- [x] 3.7 Preparar factories/fixtures sintéticas coherentes de cuatro N, cuatro I/score0, mezcla V/N/I, V degradado, HIGH con menor confidence, CRITICAL sintético, V6 score sin report, no-op, FAILED antes/después de plan y payloads adversariales; verificar que solo las importan tests y que no hay fallback ficticio de producción (FE-12..FE-18/FE-23/FE-24; D-29).

## 4. Proyectos y centro del proyecto

- [x] 4.1 Construir ProjectsPage y ProjectCreateForm inline con nombre/descripción opcional, cancelación/foco y estados; preparar pruebas Unicode/límites/U+0000, listado ordenado/vacío/error, dobles envíos, 201→ruta real y POST ambiguo→consultar lista sin repetir; verificar que solo se envían name/description (FE-02/FE-19/FE-22; D-07).
- [x] 4.2 Construir ProjectPage con GET project y target/scans independientes, card target, ProjectScanList, Actualizar y enlaces; preparar casos sin target/activo/histórico vacío/error parcial, sin consultas score por fila; verificar que target con error no permite ejecutar y no bloquea históricos (FE-03/FE-19; D-07/D-10).
- [x] 4.3 Conectar CTA Ejecutar análisis/Ver análisis en curso y aviso de uso autorizado; preparar flujo 202→ScanPage→GET inmediato, conflicto409→reconciliar lista,503/500+Location→consultar intento, error ambiguo→GET sin repetir POST; verificar que el UI no garantiza idempotencia ni estado a partir de error de transporte (FE-07; D-10/D-19).

## 5. Target y credenciales

- [x] 5.1 Construir TargetPage/TargetForm tipado con conexión/petición/respuesta/auth/headers, constantes read-only y GET que distingue alta de edición; preparar POST201/PUT200, TARGET_NOT_CONFIGURED/TARGET_ALREADY_CONFIGURED, timeout original y opcionales enviados explícitos; verificar que guardar no contacta target ni afirma autorización/conectividad (FE-04/FE-19; D-08).
- [x] 5.2 Implementar validadores UX de URL/JSON objeto/placeholder único/límites Unicode y bytes/timeout/headers con FormArray; preparar límites exactos/excedidos, tokens en clave/fragmento, controles/colisiones/duplicados case-insensitive, JSONPath rechazado por backend y fieldErrors; verificar que no se implementan DNS/SSRF/gramática completa ni parser de claves JSON duplicadas nuevo (FE-05; D-08/D-19).
- [x] 5.3 Implementar Conservar/Reemplazar con hasCredentials, password vacío y la matriz de nueve transiciones TC-10 del design; preparar alta autenticada, Bearer↔API key con secreto nuevo, cambio origen/puerto/header exacto, cambio de ruta con mismo origen conservable, retorno al tipo original sin guardar, token sin trim y byte/surrogate inválido. Verificar NONE con authConfig null elimina solo tras PUT válido; cancelar/PUT fallido preserva credencial persistida, respuesta perdida exige consulta pública y vacío/null de secretValue nunca conservan/eliminan (FE-06; D-09).
- [x] 5.4 Limpiar secreto al enviar/error/cancelar/cambiar tipo/proyecto/salir y añadir CanDeactivate de formulario sucio sin persistir borrador; preparar pruebas de reinserción después de error, navegación cancelada/aceptada, submit bloqueado, éxito con DTO público y retorno al proyecto; verificar con spies que no hay storage/logs/URL ni secreto en singleton (FE-06/FE-23; D-08/D-09/D-27).

## 6. Estado de Scan, progreso y polling

- [x] 6.1 Crear ScanDetailState provisto por ScanPage y LoadState por recurso, dependientes de route key; preparar entrada directa/cambio de params/destroy y respuestas tardías; verificar que no necesita target actual, no comparte resultados de otro Scan ni crea store/caché global (FE-01/FE-08/FE-10; D-04/D-05).
- [x] 6.2 Implementar ciclo inmediato y repetición tres segundos después del ciclo previo, con timeout de diez segundos en GET y terminal inclusivo; preparar timers virtuales de arranque/actualización/terminal rápido/fin/cancelación/no solapamiento y cero peticiones tras salir; verificar una única suscripción propietaria y ausencia de polling en componentes hijos/ProjectPage (FE-08; D-06).
- [x] 6.3 Añadir error temporal/desactualizado y contador de tres fallos consecutivos de Scan; al límite detener automático, mostrar error no terminal y Reintentar con GET inmediato sin doble envío. Preparar recuperación activa (reset/reactivar único polling), terminal COMPLETED/FAILED (carga final sin polling) y nuevo error (seguir pausado sin retry automático); cubrir primer GET sin datos, errores secundarios y timeout, además de parada en400/404/contrato inválido. Verificar ningún POST ni estado FAILED inventado (FE-08/FE-19; D-06/D-19).
- [x] 6.4 Construir ScanProgress con estados generales, filas públicas y contadores observados separados de executions/evaluations; preparar plan vacío, fallo técnico, cuatro ejecuciones con evaluación todavía pendiente y lecturas con distinta actualidad; verificar que no muestra %, fases exactas, cobertura o «pruebas satisfactorias» derivadas de terminales (FE-09; D-11).
- [x] 6.5 Detener inmediatamente polling al obtener COMPLETED/FAILED y cargar evaluations/findings/score/report una vez, más executions final, con errores independientes. Preparar primer GET ya terminal, transición y recuperación manual terminal, todos los availability/error y reintento solo de bloque; verificar exactamente una carga final por entrada/transición, listas desde sus endpoints sin sustitución por report, atomicidad backend score+report+COMPLETED respetada, cero espera artificial/polling independiente de score/report y NOT_APPLICABLE histórico normal (FE-10/FE-17..FE-19; D-20).

## 7. Resultado, Findings, incertidumbre e informe

- [x] 7.1 Construir ScanPage terminal con secciones/anclas y RiskSummary; preparar score0/cuatro N, score0/cuatro I, score alto y conteos/severidades de API, duración conocida/null y fallo de lectura; verificar prioridad visual score/risk/Findings/I y ausencia de umbrales o cálculo de riesgo en cliente (FE-11..FE-13; D-10/D-12/D-13).
- [x] 7.2 Construir CategorySummary para las cuatro entradas originales y sus contadores, con alcance LLM10 salida observable; preparar categorías0 con I y mezcla HIGH/LOW, verificando copia literal, orden y que global no se obtiene sumando/promediando categorías ni se rellenan históricos vacíos (FE-11..FE-13/FE-17; D-12/D-17).
- [x] 7.3 Construir FindingList desde GET findings con detalles nativos, confidence complementaria y orden visual RP-04 sobre copia; preparar permutaciones/empates/HIGH menor confidence/CRITICAL, cuatro textos en idioma original y recomendaciones repetidas; verificar todos los Findings visibles sin mutar report ni crear segunda lista, sin filtro/raw evidence/HTTP individual (FE-14/FE-22/FE-23; D-14).
- [x] 7.4 Construir EvaluationList con proyección segura, motivos INCONCLUSIVE y aviso degraded/componente LLM; preparar V/N/I, confidence0 contextualizado, V con LLM_FAILED, evaluación FAILED con I técnico y motivos null; verificar que no cambia finalVerdict, no muestra internals y no inventa contador semántico en fallo global (FE-15/FE-18; D-15).
- [x] 7.5 Construir ReportSections report-v1/es priorizando scope/summary/interpretation/limitations y metadata pública aprobada; preparar report sin Findings, idioma original, cambio posterior target y versión desconocida. Verificar cuatro interpretaciones/nueve limitaciones literales, summary como referencia si coincide con score o rotulado si score falla, proyecciones Findings/evaluations solo como referencias útiles sin segundas listas/narrativas ni cambio de fuentes; ausencia de URL actual atribuida al pasado y cero exportación/generación/traducción (FE-16; D-16).
- [x] 7.6 Representar las cuatro availability por separado en score/report y FAILED con sus motivos/datos conservados; preparar todas las ausencias, V6 score AVAILABLE/report NOT_APPLICABLE, histórico no-op, FAILED con Finding y score/report fallidos de lectura; verificar que ausencia no es cero/error, conflicto summary-score/PENDING terminal no se repara y no se presenta informe parcial (FE-12/FE-17..FE-19; D-17/D-18/D-20).

## 8. Seguridad, accesibilidad e integración de flujos

- [x] 8.1 Revisar bindings/handlers y errores globales/bootstrapping del flujo agregado para no introducir logs de cuerpos o errores HTTP completos; preparar fixtures XSS en nombres/description/evidenceSummary/report y spies storage/console/red; verificar texto inerte, sin innerHTML/bypass/Markdown/preview target ni requests fuera de API scanner (FE-06/FE-23; D-27).
- [x] 8.2 Preparar pruebas de flujo con RouterTestingHarness + HttpTestingController para creación/abrir Project, alta/edición Target y ejecución/seguimiento/resultados del Scan, incluyendo 409/503/errores/refresh; verificar fixtures/backend real independientes y `verify()` sin solicitudes inesperadas ni mock de producción (FE-01..FE-20/FE-24; D-28/D-29).
- [x] 8.3 Completar labels/aria-describedby/foco/landmarks/headings/anuncios limitados/details/feedback; preparar assertions de DOM y lista de recorrido manual de teclado y contraste; verificar que riesgo/inconcluso no dependen del color y que no se anuncia cada tick de polling (FE-21/FE-22; D-24/D-26).
- [x] 8.4 Ajustar layouts1280/1024/768/360, textos largos, acciones y scroll solo local donde sea necesario; verificar posteriormente por revisión visual autorizada ausencia de overflow global y legibilidad de Score/Findings/Target, sin crear librería UI o assets remotos (FE-21/FE-22; D-23..D-26).

## 9. Documentación y cierre de implementación

- [x] 9.1 Actualizar únicamente sección frontend de arquitectura, estado real de alcance y README frontend con navegación/configuración/comandos y precondiciones same-origin/deep links; verificar referencias a este change sin duplicar contratos backend ni marcar features no implementadas (FE-01/FE-20/FE-24; D-22/D-30).
- [x] 9.2 Revisar checklist de entrega al implementar y actualizar solo requisitos cuyo estado cambie con evidencia; verificar que vídeo/slides/despliegue/OpenAI/benchmark/demo-vulnerable no se dan por completados por construir frontend (FE-24; D-30).
- [x] 9.3 Cuando el usuario solicite expresamente la comprobación frontend, ejecutar desde frontend `npm test -- --watch=false` y `npm run build` y registrar resultados reales; si no hay esa solicitud, dejarlos pendientes e indicar comandos sin ejecutarlos. Verificar que no se instalan dependencias ni se usan servicios externos (FE-24; D-28).
- [ ] 9.4 Recorrer la demo solo ante autorización de entorno/endpoints: abrir proyecto→mostrar target autorizado ya preparado→ejecutar→estado real→score/Findings/I→abrir Finding→report/limitaciones; registrar evidencias observadas, sin modificar escenario externo, implementar demo-vulnerable, llamar OpenAI directamente o simular detecciones. Verificar precondición de manifiesto ligado a target.updatedAt antes del recorrido (FE-24; D-30).
- [x] 9.5 Revisar correspondencia final FE-01..FE-24/D-01..D-30/código/docs y ejecutar strict OpenSpec cuando se solicite su comprobación; registrar pendientes reales y no confundir artefactos completos con implementación/verificación completadas (FE-24).

## 10. Evidencia histórica de planificación

El 2026-10-10 se ejecutó `openspec validate frontend-mvp --strict` tras incorporar las cuatro aclaraciones y revisar coherencia proposal↔spec↔design↔tasks: salida `Change 'frontend-mvp' is valid`, exit code 0. El CLI se ejecutó con la autorización existente fuera del sandbox para cargar el esquema spec-driven, con telemetría desactivada; no se accedió a servicios externos. Se confirmó por recuento el inventario final siguiente y se eliminaron las referencias al botón anterior y a fuentes de listas condicionadas al report.

Inventario final: una capacidad nueva, 24 requisitos, 51 escenarios, 30 decisiones aprobadas y 40 tareas: 1 documental completada (1.1), 39 futuras pendientes. `openspec status --change frontend-mvp --json` confirmó los cuatro artefactos done y planificación completa; ese estado del CLI no acredita implementación. La aprobación humana está registrada en design y 1.1.

La petición actual autoriza actualización de los cuatro artefactos y validación OpenSpec; no tests/builds/servidores frontend ni tráfico real. Revisión de alcance: esta revisión solo modifica proposal/spec/design/tasks existentes de `frontend-mvp`; metadata CLI, backend, frontend existente, demo-vulnerable y documentación consolidada permanecen sin cambios. Estado final **READY FOR IMPLEMENTATION**; D-01..D-30 aprobadas con aclaraciones incorporadas, sin decisiones funcionales pendientes ni contradicción backend identificada. No se ha implementado código.

## 11. Evidencia de implementación y revisión (2026-10-10)

La solicitud posterior autorizó implementar este change y sus comprobaciones técnicas. La planificación anterior se conserva como histórico, no como descripción del código actual. Estado **READY FOR REVIEW** con una comprobación manual condicionada pendiente; no se han reabierto decisiones ni modificado requisitos, OpenAPI o contratos backend.

### Implementación y trazabilidad

| Tareas / requisitos | Evidencia implementada |
| --- | --- |
| 1.2 / FE-01,20,24 | Revisión de estado/diff limitada a frontend, este tasks.md y documentación directamente afectada. Backend, demo-vulnerable, migraciones, versiones, dependencias, lockfile, scripts/budgets y despliegue sin cambios. Sin commits, pushes ni archivo del change. |
| 2.1..2.4 / FE-01,20..22 | Shell con marca/salto al contenido/RouterOutlet, lang es, cuatro páginas lazy, NotFoundPage, environments/API_BASE_URL/proxy, StatusBadge y FeedbackPanel. Puerto 8080 confirmado mediante búsqueda dirigida en application.yml, sin leer secretos. |
| 3.1..3.6 / FE-02..20,23 | DTOs readonly con auth discriminada, modelos separados de metadata/report, wrappers discriminados y decoders unknown sin coerciones ni defaults de negocio. ProjectsApi/TargetApi/ScansApi; POST Scan sin contenido, lectura 10s, Location validada y comprobación de pertenencia. Los UUID con mayúsculas siguen siendo consultables. api.spec.ts verifica métodos, payloads, enums/versiones/nulls, campos públicos, errores y rechazo de otro scope. |
| 3.7 / FE-12..18,23,24 | Fixtures sintéticas exclusivamente de tests: cuatro negativos, cuatro inconclusos/0, mezcla V/N/I, detección degradada, CRITICAL, HIGH con menor confidence, V6/no-op, FAILED y XSS. Casos deliberadamente incompatibles comprueban rechazo seguro. El bundle de producción no contiene los marcadores sintéticos buscados. |
| 4.1..4.3 / FE-02,03,07,19,22 | Creación inline tipada, estados distintos, prevención de doble envío, reconciliación de respuesta perdida; proyecto confirma existencia y mantiene target/histórico independientes. CTA configurado/activo, aviso autorizado, conflictos y consulta de intento scoped sin repetir POST. Las escrituras ambiguas no se reactivan si falla la reconciliación. |
| 5.1..5.4 / FE-04..06,19,23 | Target consulta Project y target desde la ruta; POST/PUT explícitos, opcionales y timeout conservados. Validadores Unicode/UTF-8/objeto/token/headers sin implementar DNS/SSRF/gramática completa. Matriz TC-10 respecto del original; NONE solo elimina al guardar. Password vacío y efímero; clear al enviar/error/tipo/proyecto/cancelar/salir. Respuesta perdida bloquea otro envío hasta GET público. CanDeactivate de cambios sin guardar. Tests de transitions, error/cleanup y guard cancelado. |
| 6.1..6.5 / FE-01,08..10,17..19 | ScanDetailState por ScanPage, estado independiente por recurso, GET inmediato, ciclos sin overlap separados 3s después del anterior, timeout 10s. Tres fallos consecutivos pausan, retry manual inmediato sin POST y solo éxito activo reanuda. Terminal carga cinco recursos una vez, sin espera extra/score polling. Params/destroy cancelan HTTP, carga terminal y timers; errores secundarios no cortan el seguimiento. |
| 7.1..7.6 / FE-11..19,22,23 | Resumen superior desde /score, riesgo/severidad textuales, cero real/advertencia I, categorías copiadas sin fórmulas, Findings completos ordenados sobre copia con details y confianza heurística. EvaluationList excluye internals y separa veredicto/degradación/fallo técnico. ReportSections muestra scope/summary/cuatro interpretaciones/nueve limitaciones y referencias, sin segundas narrativas/listas. FAILED/históricos y cuatro disponibilidades por bloque; conflicto de fuentes y PENDING terminal explícitos, sin reparar ni reconstruir. |
| 8.1..8.3 / FE-01..24 | Tests HTTP/RouterTestingHarness de flujos/rutas directas, errores y guard. XSS literal en Project/Finding/report, spies de storage/console y verify sin peticiones inesperadas. Sin innerHTML/bypass, storage o llamadas directas al target/evaluador; bootstrap no imprime errores completos. Labels, ayudas/errores asociados, foco de navegación/creación/cancelación, details nativos y anuncio de cambios de estado sin anunciar ticks. |
| 8.4 / FE-21,22 | Chrome headless ya instalado, servidor estático efímero en 127.0.0.1 y API interceptada con fixtures, cualquier origen externo bloqueado. Cuatro páginas × 1280/1024/768/360: 16 layouts sin overflow global, un h1 por página y todos los campos etiquetados. Textos largos, cards y formularios apilados. Capturas inspeccionadas; teclado de creación/cancelación/details y contraste de ocho pares de la paleta PASS. Cero pageErrors y peticiones externas. Ambos procesos cerrados al terminar. |
| 9.1,9.2 / FE-24 | README frontend, sección frontend de arquitectura y estado real de alcance actualizados. Checklist revisada: Funcionalidades principales continúa EN PROGRESO; solo se actualiza observación/evidencia del frontend. No se completan requisitos administrativos, demo, integración real, OpenAI ni despliegue. |

### Comprobaciones ejecutadas

| Comprobación | Resultado real |
| --- | --- |
| Tests dirigidos: `npm test -- --watch=false --include="src/app/core/api/api.spec.ts" --include="src/app/features/target/**/*.spec.ts" --include="src/app/features/scans/**/*.spec.ts"` | PASS inicial: 94 tests / 5 archivos. La suite final incluye los casos añadidos después. |
| Suite final: `npm test -- --watch=false` | PASS: 128 tests / 7 archivos, con Vitest/TestBed/HttpTestingController/RouterTestingHarness y timers virtuales; incluye locale es para fechas y URL con userinfo vacío rechazado. |
| `npm run build` | PASS de producción, 273.78 kB iniciales; sin avisos de presupuesto. Builders, budgets y scripts conservados. |
| Lint | No existe script de lint; no se añade tooling. |
| Revisión responsive/teclado/contraste con API simulada | PASS: 16 layouts, cero overflow/errores/red externa; capturas y qa.json entregados como artefactos locales fuera del repositorio. No sustituye demo real ni auditoría WCAG completa. |
| `git diff --check` y revisión de alcance | PASS de whitespace; cambios limitados a archivos indicados. Sin imports de fixtures ni marcadores sintéticos buscados en el bundle productivo. |
| `openspec validate frontend-mvp --strict` | PASS: `Change 'frontend-mvp' is valid`, exit code 0. FE-01..FE-24/D-01..D-30 revisados contra código y documentación; no se alteran los contratos aprobados. |

El primer intento de Vitest en sandbox falló antes de ejecutar tests por EPERM al renombrar temporales; se repitió con escalación autorizada y pasó. El CLI OpenSpec necesita ejecutarse fuera del sandbox para cargar el esquema local spec-driven; la telemetría se desactiva. Chrome instalado se reutiliza sin descargar navegadores. No se instalan dependencias ni se utilizan servicios externos.

### Inventario de archivos

Se crean 50 archivos de frontend. Las rutas siguientes son relativas a `frontend/`:

| Directorio | Archivos creados |
| --- | --- |
| raíz | proxy.conf.json |
| src/environments | environment.ts, environment.development.ts |
| src/app/core/api | api-config.ts, api-client.ts, api-error.ts, decode.ts, api.spec.ts |
| src/app/shared/models | contracts.ts, load-state.ts |
| src/app/shared/presentation | labels.ts |
| src/app/shared/ui | status-badge.ts, feedback-panel.ts, not-found-page.ts, page.css |
| src/app/features/projects/models | project.ts, project.decoder.ts |
| src/app/features/projects/data-access | projects-api.ts |
| src/app/features/projects/pages | projects-page.ts, project-page.ts |
| src/app/features/projects/components | project-create-form.ts, project-scan-list.ts |
| src/app/features/target/models | target.ts, target.decoder.ts |
| src/app/features/target/data-access | target-api.ts |
| src/app/features/target/pages | target-page.ts |
| src/app/features/target/components | target-form.ts, target-form.html, target-form.spec.ts |
| src/app/features/target/forms | validators.ts, validators.spec.ts |
| src/app/features/target/guards | unsaved-target.ts |
| src/app/features/scans/models | scan.ts, scan.decoder.ts, report.ts, report.decoder.ts |
| src/app/features/scans/data-access | scans-api.ts |
| src/app/features/scans/state | scan-detail-state.ts, scan-detail-state.spec.ts |
| src/app/features/scans/pages | scan-page.ts, scan-page.html |
| src/app/features/scans/components | scan-progress.ts, risk-summary.ts, category-summary.ts, finding-list.ts, evaluation-list.ts, report-sections.ts, results.spec.ts |
| src/app | flows.spec.ts |
| src/testing | fixtures.ts |

Se modifican los 11 archivos frontend existentes `README.md`, `angular.json`, `src/index.html`, `src/main.ts`, `src/styles.css`, `src/app/app.ts`, `app.html`, `app.css`, `app.config.ts`, `app.routes.ts` y `app.spec.ts`; los tres documentos `docs/02-arquitectura/arquitectura-general.md`, `docs/00-contexto/alcance.md` y `docs/08-entrega-tfm/checklist-entrega.md`; y este `tasks.md`. Los otros cuatro artefactos de frontend-mvp ya existían sin seguimiento Git antes de esta implementación y no se han recreado ni modificado.

### Pendiente condicionado: 9.4

No se ha proporcionado un entorno/endpoints reales autorizados ni manifiesto ligado a target.updatedAt. El recorrido real queda pendiente sin sustituirlo por fixtures: abrir proyecto → consultar target preparado → verificar manifiesto y autorización → ejecutar → observar estado confirmado → revisar score/Findings/INCONCLUSIVE → abrir Finding → leer informe/limitaciones. Preparar/modificar demo-vulnerable, operar OpenAI, grabar vídeo y desplegar no forman parte de esta implementación. La revisión aislada del frontend sí está completada; no hay bloqueo de código ni contradicción OpenSpec/OpenAPI identificada.

## 12. Correcciones del review F1/F2 (2026-10-10)

Se corrigen exclusivamente los dos findings MEDIUM del review, sin reabrir decisiones ni alterar requisitos, backend, dependencias o demo-vulnerable. Estado: **READY FOR RE-REVIEW**, 39/40 tareas completadas. **9.4 sigue pendiente**.

- **F1 (2.1/2.3/9.1/9.3; FE-01/FE-22):** los enlaces HTML con fragmento resolvían contra `<base href="/">`, abandonando el deep link. Shell, secciones del Scan y referencias de ReportSections usan ahora RouterLink con comandos vacíos y fragmento. `withInMemoryScrolling` habilita el desplazamiento al destino. El skip-link conserva la ruta, desplaza la vista y enfoca el main con tabindex -1; la gestión de foco del título solo corresponde a cambios de página y no interfiere con fragmentos. Se añaden diez regresiones con base raíz y ruta interna de Scan, cubriendo riesgo, categorías, resto de secciones, referencias del informe por sección/id, foco/desplazamiento y activación repetida del skip-link.
- **F2 (4.1/9.1; FE-02/FE-19):** el GET exitoso borraba siempre el bloqueo de creación ambigua. Cada lectura captura si al iniciarse ya existía esa ambigüedad; solo en ese caso su éxito puede desbloquear. Una lectura anterior actualiza el listado sin reconciliar la escritura. Cuatro regresiones cubren expresamente GET inicial pendiente → POST ambiguo → GET inicial tardío → bloqueo mantenido → GET posterior exitoso → desbloqueo, lectura normal, reconciliación posterior, error de reconciliación y ausencia de POST automático.

Archivos de esta corrección (rutas relativas a `frontend/`): `src/app/app.ts`, `src/app/app.html`, `src/app/app.config.ts`, `src/app/features/scans/pages/scan-page.html`, `src/app/features/scans/components/report-sections.ts`, `src/app/features/scans/components/results.spec.ts` (provider Router para los enlaces), `src/app/features/projects/pages/projects-page.ts`, los nuevos `src/app/fragment-navigation.spec.ts` y `src/app/features/projects/pages/projects-page.spec.ts`, y `README.md`. Solo se actualiza este `tasks.md` entre los artefactos OpenSpec.

| Validación ejecutada | Evidencia real |
| --- | --- |
| `npm test -- --watch=false --include=src/app/fragment-navigation.spec.ts` | PASS: 10/10 tests (F1). |
| `npm test -- --watch=false --include=src/app/features/projects/pages/projects-page.spec.ts` | PASS: 4/4 tests (F2). |
| `npm test -- --watch=false` | PASS: 142/142 tests en 9 archivos. |
| `npm run build -- --configuration production` | PASS: bundle inicial 278.37 kB; sin warnings de budgets. |
| `openspec validate frontend-mvp --strict` | PASS: `Change 'frontend-mvp' is valid`, exit code 0. |
| `git diff --check` | PASS de whitespace. Revisión dirigida: no quedan anchors HTML con fragmento plano en las vistas afectadas. |

El intento inicial de F1 en sandbox falló por EPERM al renombrar temporales antes de ejecutar tests; la repetición fuera del sandbox pasó. Tests y OpenSpec se ejecutaron con la escalación autorizada necesaria para temporales/esquema local. No se utiliza backend real, no se instalan dependencias y no se hacen commits. Esta evidencia complementa la sección 11, sin acreditar el recorrido real pendiente 9.4.

