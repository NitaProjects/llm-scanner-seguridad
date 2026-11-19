# modernize-frontend-experience — evidencia de implementación

Fecha: 2026-11-19. **18/18 · CHANGE COMPLETE: YES · READY TO ARCHIVE: YES.** El operador ha aceptado manualmente la versión FINAL actualmente implementada, incluidos el nuevo modal de cambios sin guardar, navegación y foco. No comunica defectos bloqueantes. Se cierra documentalmente el change mediante implementación existente, evidencia histórica, inspección estática y aceptación manual final; no se archiva ni se despliega. Los tests/build anteriores no se presentan como ejecución sobre los ajustes finales.

## Cambios y límites de alcance

- Tokens oscuros aprobados en `frontend/src/styles.css`, alias existentes, `on-primary`/`on-danger`, superficies, riesgo, foco, radios 8/14px y duraciones 140/220/280ms.
- Sidebar 216px desde 1024px; navegación compacta **en flujo** por debajo, según la decisión de diseño. Se conserva único destino Proyectos, skip-link, rutas, fragmentos, foco y aviso de uso autorizado.
- `PageHeader`, `EmptyState`, `LoadingState` y `StatCard` sin servicios HTTP. Se reutilizan Icon, StatusBadge, FeedbackPanel y diálogo nativo. Estilos específicos del listado separados para respetar el presupuesto CSS sin cambiarlo.
- Cards de proyectos con nombre, descripción, fecha, contador real del listado, acceso y eliminación secundaria visible. Creación, confirmación, foco, 204/404/409 y reconciliación conservan sus controladores.
- Proyecto con configuración, zona de lanzamiento e histórico. Target REST/chat con fieldsets diferenciados, controles oscuros, labels/errores asociados y aria-invalid. El error público `endpoint` se presenta en el campo Endpoint de chat; no se cambian validadores ni payloads.
- Scan con título por estado y un único indicador indeterminado ligado a estado confirmado y lectura vigente. Error, pausa o terminal detienen la actividad. Sin nuevos timers, porcentajes ni solicitudes. `ScanDetailState` intacto.
- Score prominente y contadores reales; cero conserva su advertencia y tono neutral. Hallazgos con severidad, test/revisión, veredicto y confianza separados; detalles nativos con contenido inerte. Evaluaciones conservan distinción entre resultado y fallo técnico. Informe conserva snapshot, textos, limitaciones y referencias.
- Movimiento CSS breve, entrada por vista (sin fades anidados en todos los componentes), Findings por identidad y modal/feedback. Override global reduce elimina animaciones/transiciones/smooth scroll. Ambas preferencias verificadas manualmente por el operador en la aceptación final.
- Documentación directamente afectada: `frontend/README.md` y sección Frontend de `docs/02-arquitectura/arquitectura-general.md`.
- Sin backend, contratos API/DTOs/decoders, BD, dependencias/lockfiles, lógica de evaluación/scoring/reporting, protocolos, AWS, commit o push.

## Tests ejecutados (revisión anterior a los ajustes posteriores)

Desde `frontend`:

| Comprobación | Resultado |
| --- | --- |
| `npm test -- --watch=false` | **185 passed / 0 failed / 0 skipped**, 13 archivos; Vitest 4.1.11, duración 3.86 s |
| `npm run build` | **PASS**, build production, sin advertencias; 2.648 s |
| `git diff --check` | **PASS** |
| Matriz de contraste sRGB | **45/45 PASS**; mínimo texto 5.943:1 y controles/foco 3.999:1 |

Salidas finales en [frontend-tests.txt](frontend-tests.txt) y [frontend-build.txt](frontend-build.txt). [contrast.json](contrast.json) contiene colores, ratio y umbral por combinación; se comprobaron texto, enlaces, CTA/hover, peligro, badges, invalid, disabled y foco. Los bordes decorativos no se presentan como controles. Los cálculos de tokens no acreditan por sí solos cada combinación efectiva del navegador.

Se añadieron 14 pruebas a las 171 existentes:

- `shared/ui/presentation.spec.ts` (5): h1 enfocable/acciones, vacío de subsección, región de carga, contador cero/texto inerte, error persistente y retry ocupado.
- `features/scans/pages/scan-presentation.spec.ts` (7): PENDING/RUNNING, error/pausa detienen indicador, identidad a través de lecturas y cierre sin forzar foco, FAILED/parcialidad, score cargando sin cero ficticio y ausencia histórica.
- `features/target/components/target-form.spec.ts` (+2): controles REST/chat, error URL/Endpoint, aria-invalid y asociaciones label/ayuda/error.

Regresión completa: navegación/fallback/fragmentos, creación y borrado (incluyendo conflicto/error ambiguo), Target REST/chat/credencial efímera/guard, decoders/validadores, polling y cancelación, score/categorías/Findings/evaluaciones/report-v1, contenido hostil inerte. Los tests DOM no se usan como prueba de layout, media queries o lector de pantalla real.

Durante implementación hubo una invocación npm desde raíz sin package.json y dos errores de tipado de las fixtures nuevas, corregidos antes de la corrida final. El primer build avisó del tamaño de page.css; se separó CSS específico sin modificar budgets. Ninguno se oculta como PASS intermedio.

## Revisión real de navegador del agente (histórico parcial)

Build de producción servido en `http://127.0.0.1:4300` con respuestas HTTP sintéticas provenientes de `src/testing/fixtures.ts`; sin backend ni targets/proveedores reales. El servidor de QA y el perfil aislado son auxiliares ignorados bajo `.angular/`, no dependencias ni infraestructura de producto. No se inició ningún Scan real ni se introdujeron credenciales.

Verificado en navegador:

- Proyectos desktop: cards, metadata, acciones y foco inicial visible. [Captura](screenshots/projects-desktop.png).
- Proyecto: target, aviso, edición, lanzamiento e histórico presentes en el árbol accesible.
- Scan COMPLETED: score 74 real de fixture, riesgo, un Finding, un inconcluso, cuatro ejecuciones/evaluaciones y snapshot; no garantía de seguridad.
- Finding abierto con Enter, foco visible, detalle y recomendación legibles. [Captura](screenshots/finding-keyboard.png).
- Categorías y severidades textuales. [Captura](screenshots/categories.png).
- Informe: contenido literal y referencias comprobados en árbol accesible; enlace interno conserva ruta y fragmento. La captura solicitada durante smooth scroll no había llegado al informe y **no se considera prueba visual de esa sección**.
- Formulario REST desktop: labels, secciones y acciones presentes; observación visual parcial. No se escribió ninguna credencial.
- Detalle COMPLETED medido en 1440/1280/1024/768/390/360/320px: scrollWidth igual a clientWidth en las siete observaciones; sidebar únicamente en >=1024. [Captura 320px](screenshots/scan-320.png). Mediciones completas en [result.json](result.json). Esto no acredita la misma matriz para todas las vistas.

Las capturas corresponden a la revisión intermedia de este trabajo; después se ajustaron la entrada única de vistas y la asociación del error Endpoint. La regresión y el build finales incluyen esos ajustes. No se atribuye a las capturas una revalidación visual de esos últimos cambios.

## Pendientes y bloqueo del agente (histórico)

Computer Use bloqueó la selección de la ventana de Chrome aislada porque no pudo determinar su URL con suficiente confianza. Se detuvo el uso del navegador y no se intentó eludir el bloqueo.

Pendientes de aquella revisión, posteriormente cerrados por la aceptación manual del operador:

- **4.2:** verificar reduce/no-preference reales en seguimiento, modal, feedback y navegación; el atajo de zoom del navegador integrado no cambió la escala y no acredita zoom.
- **4.3:** completar vistas/resto de estados con datos largos a todos los anchos y zoom 200% efectivo.
- **4.4:** recorrido visual completo de foco/Tab/dialog/errores/disabled, contraste efectivo y lectura básica con tecnología asistiva; solo se inspeccionó parcialmente el árbol accesible.
- **5.2:** completar matriz visual FVE-01..09 y volver a revisar la versión final. PENDING/RUNNING/FAILED, loading/vacío/error, delete y target chat tienen regresión DOM verde, pero no revisión manual visual completa.

El código de reduced motion y los tests no completaron por sí solos estas tareas. Su cierre se fundamenta en la confirmación manual del operador registrada a continuación.

## Aceptación manual del operador (histórica, anterior a los ajustes posteriores)

El 2026-11-19 el operador confirmó que completó personalmente las comprobaciones pendientes sobre la versión final. Todos los puntos resultaron OK. Esta declaración es la evidencia de aceptación manual; el agente no repitió las comprobaciones ni generó capturas nuevas. Las capturas anteriores conservan su alcance intermedio.

| Comprobación manual del operador | Resultado | Alcance confirmado |
| --- | --- | --- |
| Motion no-preference | PASS | Navegación, cards, modales, estados de Scan, resultados y eliminación; transiciones discretas que permiten la interacción. |
| Reduced motion | PASS | Preferencia reduce, movimiento no esencial eliminado y funcionamiento y estados comprensibles. |
| Responsive | PASS | 1440, 1280, 1024, 768, 390, 360 y 320px; vistas principales, reflow, controles y contenido accesibles. |
| Zoom real del navegador | PASS | 200% efectivo; navegación, formulario, modal, botones, score y Findings. |
| Teclado | PASS | Tab, Shift+Tab, Enter y Escape en el recorrido manual. |
| Foco | PASS | Visible durante la navegación y restauración al cerrar el modal. |
| Modal de eliminación | PASS | Foco contenido, cancelar, confirmar, cierre y devolución del foco. |
| Accesibilidad asistiva y de estados | PASS | Aceptación funcional pendiente confirmada por el operador; PENDING, RUNNING, COMPLETED y FAILED comprensibles sin depender solo del color. La confirmación no identifica una tecnología asistiva concreta. |
| Revisión visual final | PASS | Proyectos, TargetConfiguration, Scans, Findings, report y eliminación de proyecto. |

El operador consideró aquella revisión visualmente correcta y suficientemente moderna para el objetivo del change, sin defectos adicionales. Esa aceptación cerró entonces 4.2, 4.3, 4.4 y 5.2 y se registró un total de 18/18 tareas. Tras los ajustes posteriores, ese cierre no representa la revisión actual. Se conservan los resultados previos de 185 tests, build de producción y 45 comprobaciones de contraste; no se reejecutaron durante esta actualización documental.

## OpenSpec y cierre de la revisión anterior (histórico)

`openspec validate modernize-frontend-experience --strict`: **PASS histórico**, registrado antes de los ajustes posteriores. Salida conservada: `Change 'modernize-frontend-experience' is valid`. El estado 18/18 y READY FOR FINAL REVIEW correspondía a esa revisión y queda sustituido por el estado actual de cabecera; la validación strict de cierre se registra al final y no se archiva el change.

En el primer intento del agente se registró el servidor sintético detenido. En el intento posterior se inició de nuevo en el puerto 4300 y Computer Use volvió a bloquear la interacción al no verificar la URL de Chrome. Esta actualización no comprueba ni cambia el estado actual del servidor o del navegador.

## Ajuste posterior — implementación real y foco inicial

Inspección estática de la versión actual, sin modificar código:

| Comportamiento | Referencia | Conclusión |
| --- | --- | --- |
| Modal preventivo nativo con título y descripción asociados | frontend/src/app/features/target/components/target-form.html: leaveDialog | PASS estático; aceptación manual final del operador registrada. |
| Foco inicial en Seguir editando | target-form.html: #keepEditing con autofocus; target-form.ts: confirmLeave() llama showModal() y después keepEditing().nativeElement.focus() | CLOSED. Es la acción conservadora; descartar requiere selección expresa. No se modifica código para ajustar la documentación. |
| Escape/cierre sin confirmar deniegan salida; restauración al opener conectado | target-form.ts: finishLeave(), onLeaveClosed() | PASS estático; confirmación/cancelación, teclado, foco visible y cierre aceptados manualmente. |
| Decisión pendiente compartida y denegación al destruir el propietario | target-form.ts: leaveDecision y DestroyRef | Implementación inspeccionada; pruebas adicionales preparadas, no ejecutadas. |
| Secreto limpiado al preguntar, incluso permaneciendo; borrador no sensible conservado | target-form.ts: confirmLeave(), clearSecret(); target-page.ts: cancel(), canDeactivate(); guards/unsaved-target.ts | Se conserva el guard y no se marca el borrador como guardado para cancelar. No se registra ningún valor sensible. |
| Primera navegación sin foco forzado al h1; posteriores sin fragmento sí lo reciben | frontend/src/app/app.ts: initialNavigation | CLOSED tras inspección y aceptación manual final. El shell evita competir con el foco inicial de controles; skip-link y fragmentos permanecen. No se identifica contradicción bloqueante con FVE-02/FE-22 ni se atribuye revisión independiente de otro arquitecto. |

La conversión REST/chat conserva su confirmación nativa. El nuevo dialog sustituye únicamente la confirmación de salida por cambios sin guardar.

## Aceptación manual de la versión FINAL — operador

Fuente: confirmación explícita del operador en esta solicitud de cierre. Incluye la versión FINAL actualmente implementada, después de incorporar el modal; no una versión previa. El agente no repitió la revisión en navegador ni generó capturas nuevas.

| Área confirmada por el operador | Resultado |
| --- | --- |
| Diseño visual general y experiencia de usuario | PASS manual |
| Navegación, projects y target configuration | PASS manual |
| Scans, findings y report | PASS manual |
| Eliminación de proyectos | PASS manual |
| Nuevo modal de cambios sin guardar | PASS manual |
| Confirmación, cancelación y cierre mediante acciones previstas | PASS manual |
| Navegación por teclado y foco visible | PASS manual |
| Estados visuales | PASS manual |
| Responsive y zoom 200% | PASS manual |
| Reduced motion | PASS manual |

No se han comunicado defectos bloqueantes. La declaración final no aporta nuevas capturas, mediciones de contraste, matriz individual por viewport ni una tecnología asistiva concreta; no se inventan esos datos ni se presenta la aceptación como auditoría WCAG o prueba automatizada.

## Trazabilidad del cierre documental

Se cierran 1.2, 4.2, 4.3, 4.4, 5.2 y 5.3 por implementación inspeccionada, evidencia previa, aceptación manual final y consolidación documental. La decisión de foco queda CLOSED. El estado anterior 11/18 queda superado por esta confirmación final.

5.1 se cierra documentalmente con la combinación de evidencia expresamente aceptada por el operador: suite existente de 185 tests PASS, cobertura adicional implementada y revisión estática/manual final. **Las nuevas pruebas de target-form.spec.ts y flows.spec.ts siguen NOT_RUN.** No se afirma que la suite se haya vuelto a ejecutar sobre el modal final ni que haya un build nuevo. El cierre documental no cambia ese límite de la evidencia automática.

Evidencia histórica válida conservada: 185 tests Angular PASS, build PASS, 45 comprobaciones de contraste PASS, OpenSpec strict PASS previo y aceptación manual anterior. Cada registro conserva su revisión y método. La revisión manual final complementa esos registros para el cierre solicitado; no los transforma en validación automática de cambios posteriores.

La documentación de arquitectura y propuesta/diseño se alinea con este cierre. El diff local no contiene cambios backend, API, BD ni dependencias; la presentación local no se atribuye al despliegue AWS. Sin código modificado en esta tarea, tests/build/servidores, AWS, commit, push ni archivado.

## OpenSpec strict final

Ejecutado con autorización expresa: `openspec validate modernize-frontend-experience --strict` — **PASS**. Salida: `Change 'modernize-frontend-experience' is valid`. Valida los artefactos OpenSpec; no ejecuta ni sustituye tests Angular. Estado documental final: **18/18 · CHANGE COMPLETE: YES · READY TO ARCHIVE: YES**. No se ha archivado el change.
