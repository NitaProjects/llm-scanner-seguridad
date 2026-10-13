## 1. Review Gate

Estado de implementación **READY FOR REVIEW**, cerrado el 2026-10-13: **32/32 tareas completadas**, sin pendientes. D-01..D-26 conservan la aprobación del 2026-10-11 y las aclaraciones registradas en design. Los artefactos de planificación conservan su contexto histórico READY FOR IMPLEMENTATION; este registro documenta la ejecución posterior solicitada explícitamente por el usuario. Fuentes: [spec](specs/frontend-polish/spec.md) y [design](design.md).

Al retomar había 16 tareas marcadas (1.1..5.4), el acabado visual implementado, tests dirigidos, suite de 160 tests, build final y matriz QA completa. Faltaba registrar/verificar 6.1..8.2. Esta continuación completa esas 16 tareas con revisión del diff, suite final de 161 tests, QA transversal, comparativas finales, strict y documentación. No se ha modificado código de producción ni tests al retomar.

- [x] 1.1 Registrar aprobación explícita de las 26 decisiones visuales y actualizar el estado a READY FOR IMPLEMENTATION, incorporando SVG reutilizables, LOW distinto de CTA y capturas representativas para QA completa; verificar registro y coherencia de proposal/spec/design, sin modificar los contratos históricos de frontend-mvp (FP-01/FP-05/FP-06/FP-16; D-01..D-26).
- [x] 1.2 Antes de editar, revisar estado/diff del área afectada y fijar el inventario visual contra las fuentes ya identificadas en design; verificar que no se incluyen cambios ajenos, lógica de clases existentes, servicios, routing, modelos, dependencias o backend, con la única adición visual de Icon descrita en D-24 (FP-01; D-25).

## 2. Visual Foundation

- [x] 2.1 Definir en styles.css los tokens de colores, alias text/muted, spacing, tipos, ancho, radios y sombra propuestos; verificar por lectura del CSS que tienen una fuente única y que los usos existentes siguen resueltos, sin variables o variantes sin consumidor (FP-02/FP-03/FP-06; D-03..D-08/D-11).
- [x] 2.2 Ajustar base tipográfica, links, controles, primary/secondary, focus/disabled y motion reducido; verificar estilos por estado y conservar controles ≥44px, labels, ayudas y tipos de input. La comprobación renderizada se registra en 7.11 (FP-02/FP-05/FP-15; D-05/D-09/D-23/D-24).
- [x] 2.3 Reducir acumulaciones de márgenes/cards en shared/ui/page.css y separar layout específico scoped donde sea necesario; verificar que patrones comunes se reutilizan, no hay selectores frágiles/estilos HTML inline ni refactor de componentes, y revisar las seis superficies en QA (FP-02/FP-03; D-04/D-07/D-08/D-25).
- [x] 2.4 Crear el componente visual ligero Icon con SVG inline propios y paths centralizados para reutilizarlos sin duplicación masiva, y unificar StatusBadge/FeedbackPanel y tonos de availability en las ramas existentes. Verificar vocabulario cerrado, ausencia de librerías externas, accesibilidad del SVG, paleta completa e inputs/outputs existentes intactos; LOW azul desaturado debe distinguirse de primary/CTA y conservar apariencia estática sin hover/foco/cursor de acción. Mantener mensajes/roles y ausencia de garantía de seguridad implícita para NO_FINDINGS o COMPLETED (FP-05/FP-06/FP-13; D-10/D-11/D-21/D-24/D-25).

## 3. Shell And Projects

- [x] 3.1 Refinar app.html/app.css: contenedor común, marca/símbolo propio, navegación activa accesible y header envolvente; verificar destinos, RouterOutlet, footer, skip-link y foco intactos, Proyectos activo en descendientes y sin activo en ruta desconocida (FP-03..FP-05/FP-14; D-02/D-03/D-12).
- [x] 3.2 Compactar ProjectsPage: título/CTA, filas con datos y empty state con CTA conectado a la apertura existente; verificar que showForm, id new-project, orden de lista, descripciones, disabled, creación/cancelación y retorno del foco se preservan (FP-07; D-13/D-14).
- [x] 3.3 Equilibrar ProjectPage y ProjectScanList con preparación/acción próximas e histórico compacto; verificar las ramas target ausente/configurado/activo, errores parciales y avisos existentes, sin llamadas nuevas o métricas por fila (FP-08; D-15).
- [x] 3.4 Aplicar patrón visual común a NotFoundPage y vacíos de histórico/hallazgos/evaluaciones; verificar título/explicación/retorno existentes y ausencia legítima diferenciada de loading/error, sin inventar acciones (FP-13; D-14/D-21).

## 4. Target Form

- [x] 4.1 Ajustar TargetPage/TargetForm en sus cuatro secciones con separadores, ancho 960px máximo y grupos responsive; verificar por diff que no cambian árbol FormGroup/FormArray, controls, directivas, IDs, data-field, aria-describedby, tipos, validators o handlers (FP-09/FP-14; D-16/D-22/D-25).
- [x] 4.2 Refinar filas de cabeceras, JSON/responsePath, auth/secret y Guardar/Cancelar; verificar que ayudas, constantes, errores, advertencias y condiciones originales siguen presentes, sin acordeones ni revelado de secretos. Comprobar visualmente auth con campo password vacío y metadatos sintéticos, sin capturar credenciales (FP-05/FP-09/FP-15; D-09/D-16/D-23/D-24).

## 5. Scan Results

- [x] 5.1 Refinar ScanPage/RiskSummary: cabecera compacta y superficie con score/riesgo/hallazgos/inconclusos; verificar DOM/IDs/fragments, orden, origen de cada dato y ramas existentes sin cambios de state/polling/relecturas (FP-10; D-17/D-18).
- [x] 5.2 Ajustar CategorySummary y ScanProgress/EvaluationList para jerarquía secundaria y densidad consistente; verificar grids 4/2/1, textos de progreso honestos, etiquetas, INCONCLUSIVE/degradación y separación de fallo técnico sin filtros o cálculos nuevos (FP-06/FP-10/FP-13/FP-14; D-10/D-11/D-17/D-21/D-22).
- [x] 5.3 Refinar FindingList sin card exterior anidada, con summary/badges/acentos e iconos propios; verificar orden existente, textos completos, IDs/enlaces, confianza secundaria y details nativos que conservan descripción/evidencia/recomendación (FP-05/FP-11; D-19/D-24).
- [x] 5.4 Refinar ReportSections como documento con lectura acotada, dl y limitaciones; verificar referencia/resumen propio según comparación actual, cuatro interpretaciones, nueve limitaciones literales en orden y trazabilidad existente, sin otra lista de Findings/evaluaciones (FP-12; D-20).

## 6. Regression Boundary

- [x] 6.1 Revisar el diff de código contra FP-01 y el inventario: verificar que no cambian lógica/estado de negocio, DTOs, secrets, navegación/destinos, polling, contratos, backend, OpenAPI, paquetes, lockfiles o despliegue; la única adición de componente es Icon, puramente visual según D-24. Documentar el resultado de revisión en este archivo (FP-01; D-25).
- [x] 6.2 Revisar tests afectados por el markup; adaptar o añadir únicamente casos necesarios para CTA vacío reutilizado, navegación activa/aria-current, iconos reutilizables/SVG decorativos, asociaciones accesibles o estados visuales alterados. Verificar diff de tests conservando la cobertura funcional existente, sin tests de detalle de cada regla CSS, nuevos frameworks ni fixtures de producción (FP-05/FP-07/FP-15/FP-16; D-24/D-26).
- [x] 6.3 Cuando se solicite expresamente ejecutar comprobaciones, correr `npm test -- --watch=false` y `npm run build` desde frontend; registrar comandos, resultados y límites de evidencia. Si no se autoriza, mantener esta tarea pendiente y comunicar comandos sin ejecutarlos. No ejecutar Maven/Docker/backend, instalar dependencias ni crear lint inexistente (FP-16; D-26).

## 7. Visual QA

Precondición de ejecución: solicitud expresa de QA y, si hace falta, del servidor/build auxiliar. Reutilizar navegador instalado y enfoque documentado en frontend-mvp/tasks.md (sección 11: API interceptada con fixtures sintéticas, sin backend/target/OpenAI reales), sin nuevas dependencias o herramientas de regresión. Las autorizaciones de aquel change no autorizan ejecutar comandos en este. No importar fixtures en producción ni añadir selector demo. Si falta un caso, extender únicamente la fixture de prueba pertinente.

Cada tarea 7.1..7.10 cubre **1440, 1280, 1024, 768 y 360px** de ancho CSS. Registrar también alto real del viewport, escala/zoom, navegador y fecha. Usar al menos 800px de alto en desktop, 768×1024 para tablet y 360×800 para móvil; el contenido largo debe desplazarse verticalmente con normalidad.

Validar y registrar las **50 combinaciones**, conservando capturas representativas de estados y anchos principales; no se requieren 50 capturas ni una por celda. En las tareas siguientes, la evidencia por ancho/caso puede ser un registro textual de comprobación. Elegir capturas que, en conjunto, muestren composición desktop, tablet y 360px, los estados principales y los detalles visuales de riesgo/incertidumbre, formulario y vacíos.

- [x] 7.1 QA Projects empty en los cinco anchos; verificar composición compacta, icono, explicación, CTA, apertura inline/cancelación y foco devuelto. Registrar resultado y evidencia por ancho (FP-03/FP-07/FP-15/FP-16).
- [x] 7.2 QA Projects con datos en los cinco anchos; verificar nombre/fecha/descripción, varias filas, textos extensos y ausencia de recorte/overflow. Registrar resultado y evidencia por ancho (FP-07/FP-14/FP-16).
- [x] 7.3 QA Project detail en los cinco anchos, con target ausente/configurado y análisis activo como variantes; verificar CTA correspondiente, aviso de autorización, target público e histórico legibles, con error parcial local. Registrar resultado y evidencia por ancho/variante (FP-08/FP-13/FP-14/FP-16).
- [x] 7.4 QA Target form en los cinco anchos; verificar cuatro secciones, URL/JSON largos, varias cabeceras, auth NONE/Bearer/API key, errores/disabled/guardado y acciones. Usar password vacío, sin leer ni capturar secretos; registrar resultado de layout por ancho/variante y capturas representativas (FP-09/FP-14..FP-16).
- [x] 7.5 QA Scan RUNNING en los cinco anchos, incluyendo PENDING y error/pausa de lectura como variantes; verificar badge, progreso técnico sin porcentaje/fases, mensajes/reintento existentes y ausencia de cifras inventadas. Registrar resultado y evidencia por ancho (FP-06/FP-10/FP-13/FP-16).
- [x] 7.6 QA Scan COMPLETED con Findings en los cinco anchos; verificar resumen prioritario, severidades LOW/MEDIUM/HIGH y CRITICAL sintético, categorías, findings cerrados/abiertos, evaluaciones e informe con nueve limitaciones. Registrar resultado y evidencia por ancho (FP-06/FP-10..FP-12/FP-16).
- [x] 7.7 QA Scan score 0 en los cinco anchos; verificar 0/100, NO_FINDINGS neutro, advertencia completa de que no demuestra seguridad y contadores, sin señal verde de certificación. Registrar resultado y evidencia por ancho (FP-06/FP-10/FP-16).
- [x] 7.8 QA Scan con INCONCLUSIVE en los cinco anchos, incluyendo cuatro inconclusos con score 0 y un caso mixto; verificar contador/advertencia visibles, motivo de evaluación y diferencia de FAILED/NOT_VULNERABLE. Registrar resultado y evidencia por ancho (FP-06/FP-10/FP-13/FP-16).
- [x] 7.9 QA Scan FAILED en los cinco anchos; verificar motivo público, aviso de análisis incompleto, datos conservados accesibles, score/report ausentes sin cero ficticio. Registrar resultado y evidencia por ancho (FP-06/FP-13/FP-16).
- [x] 7.10 QA histórico NOT_APPLICABLE en los cinco anchos; verificar bloques independientes, caso score AVAILABLE/report NOT_APPLICABLE y ambos no aplicables sin plan, con presentación neutra. Registrar resultado y evidencia por ancho (FP-06/FP-13/FP-16).
- [x] 7.11 QA transversal de shell y NotFoundPage en los cinco anchos; verificar navegación activa, marca/retorno, hover/focus/disabled, LOW visualmente distinto del CTA y badge estático, teclado completo, nombres accesibles/iconos reutilizables/SVG sin foco, zoom 200%, contraste por estado, reduced-motion y falta de overflow global. Revisar antes/después con idénticos datos/viewport para shell, Projects, Project, Target, Scan y ruta desconocida; registrar comprobaciones y conservar comparativas representativas, corregir/revisar combinaciones afectadas (FP-02..FP-06/FP-14..FP-16).

### QA Matrix

Las **50 celdas PASS** corresponden a los registros `rows` de [results.json](qa/results.json), identificados por escenario y viewport, con geometría, variantes, fecha y navegador. [supplement.json](qa/supplement.json) verifica el shell final y las comprobaciones transversales posteriores. Las capturas representativas se relacionan debajo.

| Escenario | 1440 | 1280 | 1024 | 768 | 360 |
| --- | --- | --- | --- | --- | --- |
| Projects empty | PASS | PASS | PASS | PASS | PASS |
| Projects con datos | PASS | PASS | PASS | PASS | PASS |
| Project detail | PASS | PASS | PASS | PASS | PASS |
| Target form | PASS | PASS | PASS | PASS | PASS |
| Scan RUNNING | PASS | PASS | PASS | PASS | PASS |
| COMPLETED con Findings | PASS | PASS | PASS | PASS | PASS |
| Score 0 | PASS | PASS | PASS | PASS | PASS |
| INCONCLUSIVE | PASS | PASS | PASS | PASS | PASS |
| FAILED | PASS | PASS | PASS | PASS | PASS |
| Histórico NOT_APPLICABLE | PASS | PASS | PASS | PASS | PASS |

### Evidence Record

**Entorno y reutilización.** Chrome 155.0.8059.39 instalado, headless, build de producción servido exclusivamente en loopback y API interceptada con datos públicos sintéticos de `frontend/src/testing/fixtures.ts`. Desktop 1440/1280/1024×900; tablet 768×1024; móvil 360×800; escala 1 y zoom 100%. No backend, target, OpenAI ni solicitudes externas; los registros runtime no contienen errores de página. La matriz completa finalizó el 2026-10-11 a las 00:49:46 (Europe/Madrid). El último ajuste posterior solo corrigió la presentación activa del enlace Proyectos en un wildcard descendiente; se verificó con tests dirigidos y el build final de las 00:51:11. Una segunda pasada redundante se interrumpió por `ERR_NETWORK_IO_SUSPENDED`; no sustituye ni invalida la primera matriz completa. Se reutiliza esta evidencia y se revalida el shell final en los cinco anchos el 2026-10-13, a las 02:00:38, junto con teclado, estados, zoom y contraste. El suplemento contiene comprobaciones adicionales; su array `rows` vacío no representa otra matriz.

**Resultados por escenario, en los cinco anchos (7.1..7.10):**

| Registro `scenario` | Comprobación y variantes PASS |
| --- | --- |
| `projects-empty` | Icono/título/explicación/CTA; Enter abre el formulario existente, foco inicial, disabled y cancelación con foco devuelto a `new-project`. |
| `projects-data` | Varias filas, nombre sin espacios y descripción extensos, fecha y texto completos, sin overflow. |
| `project` | Target configurado/ausente, scan activo y error parcial local; CTA correspondiente, autorización e histórico. |
| `target` | Cuatro fieldsets; URL/JSON largos, dos cabeceras; NONE/Bearer/API key; password vacío en reemplazo; labels, error de URL, disabled durante guardado y confirmación existente. |
| `running` | RUNNING/PENDING y pausa tras tres errores de lectura; progreso técnico, reintento y ausencia de score inventado. |
| `completed` | Cuatro severidades sintéticas coherentes; categorías 4/2/1; details abiertos/cerrados con Enter; LOW estático desaturado; nueve limitaciones. |
| `score-zero` | 0/100, NO_FINDINGS neutro, advertencia completa y contadores sin certificación verde. |
| `inconclusive` | Cuatro inconclusos con cero y variante mixta con uno; contador, advertencia y motivos diferenciados de FAILED. |
| `failed` | Motivo público, aviso de incompleto, Finding conservado y score/informe indisponibles sin cero ficticio. |
| `historical` | Score/report NOT_APPLICABLE independientes, sin plan; variante score AVAILABLE/report NOT_APPLICABLE. |

**QA transversal (7.11).** PASS: shell en todas las superficies y anchos, wildcard raíz/descendiente sin activo, navegación de proyectos activa con `aria-current`, retorno, H1 único, labels, controles/summaries ≥44px, SVG decorativos sin foco y ausencia de overflow global. Recorrido Tab completo en las seis superficies, orden DOM y foco de 3px; findings se expanden con teclado. Loading explícito y error local con Reintentar diferenciados del vacío. Primary/secondary hover, disabled legible, LOW sin apariencia interactiva, reduced-motion sin transiciones/scroll suave. Zoom mediante CSS `zoom: 2` en las seis superficies a viewport 1280×900: sin overflow ni pérdida de controles; no se presenta como prueba de zoom nativo de todos los navegadores. [Contraste calculado](qa/supplement.json): los 16 pares de texto superan 4.5:1 (mínimo 5.73:1), y los tres pares de bordes/foco superan 3:1 (mínimo 3.88:1). La revisión cubre estas comprobaciones concretas, no una auditoría WCAG completa ni integración real.

**Capturas representativas conservadas y revisadas:**

| Estado | Evidencia |
| --- | --- |
| Projects empty desktop/móvil | [1440](qa/projects-empty-1440.png), [360](qa/projects-empty-360.png) |
| Projects con datos | [1440](qa/projects-data-1440.png) |
| Project detail | [1440](qa/project-1440.png) |
| Target / API key con password vacío | [1440](qa/target-1440.png), [360](qa/target-api-key-empty-360.png) |
| Scan RUNNING | [1440](qa/running-1440.png), [360 viewport](qa/running-360-viewport.png) |
| COMPLETED y Findings | [1440](qa/completed-1440.png), [768](qa/completed-768.png), [360](qa/completed-360.png), [resumen desktop](qa/completed-1440-viewport.png), [resumen móvil](qa/completed-360-viewport.png) |
| Findings abiertos | [1440](qa/findings-open-1440.png), [360](qa/findings-open-360.png) |
| Score 0 | [1440](qa/score-zero-1440.png) |
| INCONCLUSIVE | [1440](qa/inconclusive-1440.png), [360 viewport](qa/inconclusive-360-viewport.png) |
| FAILED | [1440](qa/failed-1440.png) |
| Histórico NOT_APPLICABLE | [1440](qa/historical-1440.png) |

**Comparativas con los mismos datos y viewport 1440×900.** Baseline capturado el 2026-10-11, sin revertir trabajo; capturas finales actualizadas el 2026-10-13 sobre el build ya validado. El shell está incluido en cada comparación. Revisadas composición, densidad, jerarquía, alineación y lectura:

| Superficie | Antes | Después |
| --- | --- | --- |
| Projects empty | [Antes](qa/before-projects-empty-1440.png) | [Después](qa/after-projects-empty-1440.png) |
| Projects con datos | [Antes](qa/before-projects-data-1440.png) | [Después](qa/after-projects-data-1440.png) |
| Project | [Antes](qa/before-project-1440.png) | [Después](qa/after-project-1440.png) |
| Target | [Antes](qa/before-target-1440.png) | [Después](qa/after-target-1440.png) |
| Scan | [Antes](qa/before-completed-1440.png) | [Después](qa/after-completed-1440.png) |
| NotFound | [Antes](qa/before-not-found-1440.png) | [Después](qa/after-not-found-1440.png) |

### Regression And Validation Record

Revisión FP-01 PASS: diff limitado a presentación frontend, tests visuales, fixtures de test y documentación de este change. [scope-review.json](qa/scope-review.json) registra 14 cuerpos de clases existentes idénticos a HEAD (normalizando únicamente CRLF/LF), 79 atributos contractuales de Target intactos, nueve archivos frontend nuevos y tokens sin variables huérfanas. La navegación activa se resuelve en el template del shell y no cambia destinos/routing. Servicios, estado, polling, DTOs, validadores, secrets, API, backend, dependencias, lockfiles y despliegue permanecen intactos. La fixture de cuatro severidades solo se usa en tests/QA y no se importa en producción. `git diff --check`: PASS.

| Validación autorizada | Resultado y vigencia |
| --- | --- |
| Tests dirigidos de markup, formularios, resultados, flujos e Icon | Sesión anterior: 7 archivos / 84 tests PASS. La ampliación visual conserva las pruebas funcionales existentes. |
| `npm test -- --watch=false --include='src/app/app.spec.ts' --include='src/app/fragment-navigation.spec.ts'` | 2026-10-11 00:49:57: 2 archivos / 19 tests PASS, después del último ajuste del shell; reutilizado sin cambios posteriores. |
| `npm test -- --watch=false` | 2026-10-13 01:56:21: **10 archivos / 161 tests PASS**, exit 0; [salida completa](qa/frontend-tests.log). Sustituye la pasada previa de 160 tests anterior al caso wildcard descendiente. |
| `npm run build` | 2026-10-11 00:51:11: **PASS**, exit 0, producción en `frontend/dist/frontend/browser`, initial 286.67kB / transferencia estimada 79.45kB, sin warnings de budget. Reutilizado: no hay cambios de fuente posteriores; timestamps y hashes SHA-256 actuales en scope-review.json. Solo se han cerrado documentación/evidencias en esta continuación. |
| `openspec validate frontend-polish --strict` | 2026-10-13: **PASS**, exit 0; [salida](qa/openspec-strict.log). |

Las comprobaciones se ejecutaron por solicitud expresa del usuario. Los bloqueos EPERM del sandbox se resolvieron ejecutando las comprobaciones autorizadas fuera de él; no se instalaron dependencias. El servidor estático auxiliar y Chrome finalizaron. No hay desviaciones de D-01..D-26 ni bloqueos de implementación; la validación real con backend/target y una auditoría completa de accesibilidad no forman parte de este change.

### File Inventory

Rutas relativas a `frontend/`. Archivos creados en la implementación previa, conservados en esta continuación:

- `src/app/shared/ui/icon.ts`, `icon.spec.ts`, `status-badge.css`.
- `src/app/features/scans/components/category-summary.css`, `finding-list.css`, `risk-summary.css`, `report-sections.css`.
- `src/app/features/scans/pages/scan-page.css`.
- `src/app/features/target/components/target-form.css`.

Archivos modificados en la implementación previa, conservados sin cambios al retomar:

- `src/styles.css`; `src/app/app.css`, `app.html`, `app.ts`, `app.spec.ts`.
- `src/app/shared/ui/page.css`, `status-badge.ts`, `feedback-panel.ts`, `not-found-page.ts`.
- `src/app/features/projects/pages/projects-page.ts`, `projects-page.spec.ts`, `project-page.ts`; `src/app/features/projects/components/project-scan-list.ts`.
- `src/app/features/target/components/target-form.ts`, `target-form.html`.
- `src/app/features/scans/pages/scan-page.ts`, `scan-page.html`.
- `src/app/features/scans/components/category-summary.ts`, `evaluation-list.ts`, `finding-list.ts`, `risk-summary.ts`, `report-sections.ts`.
- `src/testing/fixtures.ts`.

Esta continuación modifica únicamente `frontend/README.md`, este `tasks.md` y evidencia bajo `qa/`: añade `frontend-tests.log`, `openspec-strict.log`, `scope-review.json`, `supplement.json`, `supplement-runtime.json` y cuatro capturas `*-viewport.png`; actualiza las seis comparativas `after-*.png` y `after-runtime.json`. La matriz, capturas representativas anteriores y artefactos de planificación se reutilizan. Los scripts temporales de QA/revisión permanecen fuera del repositorio; no se añade infraestructura ni framework de pruebas.

## 8. Documentation And Closure

- [x] 8.1 Tras implementar, actualizar solo la descripción visual directamente afectada de frontend/README.md con enlace a este diseño/tokens y guía QA; verificar que no se duplica documentación CSS ni se reescriben contratos/históricos de frontend-mvp, arquitectura o checklist TFM sin cambio real que lo justifique (FP-01/FP-16; D-25/D-26).
- [x] 8.2 Revisar alineación de FP-01..FP-16, D-01..D-26 y evidencia; ejecutar strict únicamente cuando se solicite su comprobación en esa fase, registrar resultado y mantener pendientes las pruebas no ejecutadas. Verificar que cierre no implica despliegue, demo real, entrega TFM o archivo automático del change (FP-01/FP-16).

### Traceability

| Requisitos | Tareas principales |
| --- | --- |
| FP-01 | 1.1/1.2, 6.1, 8.1/8.2 |
| FP-02/FP-03 | 2.1..2.3, 3.1, 7.1/7.11 |
| FP-04 | 3.1, 7.11 |
| FP-05/FP-06 | 2.2/2.4, 3.1/3.2, 5.1..5.3, 7.5..7.11 |
| FP-07/FP-08 | 3.2/3.3, 7.1..7.3 |
| FP-09 | 4.1/4.2, 7.4 |
| FP-10/FP-11/FP-12 | 5.1..5.4, 7.5..7.8 |
| FP-13 | 2.4, 3.4, 7.3..7.5, 7.9/7.10 |
| FP-14/FP-15 | 2.2, 3.1, 4.1/4.2, 7.1..7.11 |
| FP-16 | 6.2/6.3, 7.1..7.11, 8.1/8.2 |
