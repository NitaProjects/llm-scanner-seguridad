## 1. Base visual y navegación

- [x] 1.1 Evolucionar tokens de styles.css según design (incluidos on-primary/on-danger, variantes de riesgo, radios/elevaciones/duraciones y color-scheme); verificar todos los consumidores y combinaciones normal/hover/focus/invalid/disabled mediante matriz de contraste documentada. [FVE-01, FVE-08]
- [x] 1.2 Adaptar app.html/app.css a sidebar >=1024px y navegación compacta en flujo por debajo de 1024px, conservando rutas/skip-link/footer autorizado/foco; verificar app.spec y fragment-navigation.spec para acceso directo, fallback, enlaces internos y navegación por teclado. [FVE-02]
- [x] 1.3 Extraer PageHeader, EmptyState y LoadingState y adaptar page.css/FeedbackPanel/StatusBadge/Icon sin duplicarlos; verificar tests de labels, estados, acciones emitidas y roles sin HTTP en componentes compartidos. [FVE-01 a FVE-04]

## 2. Proyectos y configuración

- [x] 2.1 Modernizar ProjectsPage y ProjectCreateForm con cards, metadata, contador del listado y feedback consistente; verificar vacío/error/carga/creación/cancelación y ausencia de consultas agregadas o cambios en requests. [FVE-03]
- [x] 2.2 Integrar estilo oscuro del diálogo delete y acción destructiva secundaria visible; verificar projects-page.spec para Cancelar/foco/confirmación única/204/404/409/respuesta ambigua, sin retirada optimista ni DELETE automático. [FVE-04]
- [x] 2.3 Modernizar ProjectPage y ProjectScanList separando target, lanzamiento e histórico; verificar target ausente/configurado, scan activo, admisión ambigua, errores y actualización manual con las acciones vigentes. [FVE-03, FVE-05]
- [x] 2.4 Adaptar TargetPage/TargetForm con secciones y estados oscuros, labels y asociaciones ARIA para todos los errores; verificar suite target-form/validators/decoder, REST/chat, Conservar/Reemplazar, limpieza del secreto, confirmaciones y guard sin modificar validaciones/payloads. [FVE-03, FVE-08]

## 3. Seguimiento y resultados

- [x] 3.1 Adaptar ScanPage/ScanProgress con header por estado, contadores técnicos e indicador indeterminado condicionado a lectura vigente; verificar fixtures PENDING/RUNNING/COMPLETED/FAILED, pausa/desactualización y no duplicación de anuncios/HTTP por refresco, manteniendo scan-detail-state.spec. [FVE-05]
- [x] 3.2 Refinar RiskSummary y CategorySummary y extraer StatCard para los contadores existentes; verificar valores/etiquetas de score, severidades e inconclusos, cero neutral y ausencia/error/NOT_APPLICABLE mediante results.spec. [FVE-06]
- [x] 3.3 Refinar FindingList/EvaluationList con jerarquía, badges y details, conservando orden/IDs/textos; verificar confianza separada, fallos técnicos/veredictos, expansión con teclado y markup como texto inerte sin datos internos. [FVE-06, FVE-08]
- [x] 3.4 Refinar ReportSections con resumen, alcance, interpretación, limitaciones y trazabilidad; verificar snapshot histórico, versiones, fragmentos y divergencia score/report sin generar narrativas ni sustituir fuentes. [FVE-06]

## 4. Movimiento, accesibilidad y responsive

- [x] 4.1 Aplicar transiciones CSS breves y entrada única a vistas/cards/modal/feedback y señal activa acotada; verificar que no reinician por polling, no alteran score ni retrasan navegación/foco/mutaciones y cesan en pausa/terminal/salida. [FVE-07]
- [x] 4.2 Aplicar reduce a todo movimiento no esencial y smooth scroll; verificar manualmente en navegador ambas preferencias con scans, modal, feedback y navegación, dejando indicador estático textual bajo reduce. [FVE-07]
- [x] 4.3 Adaptar grids/headers/listas/modal a 1440/1280/1024/768/390/360px y reflow 320px/zoom 200%; verificar con datos sintéticos largos ausencia de scroll de página, acciones recortadas o evidencia irrecuperable, registrando matriz/capturas. [FVE-08]
- [x] 4.4 Revisar contraste efectivo, foco visible/no oculto, orden de teclado/encabezados, aria-live, labels/errores, dialog y lectura básica con tecnología asistiva; verificar combinaciones normal/hover/invalid/disabled y corregir incumplimientos sin cambiar lógica funcional. [FVE-01, FVE-08]

## 5. Regresión y cierre

- [x] 5.1 Completar tests de presentación e integración con HTTP simulado para navegación, formularios, delete, estados, score/Findings/report y errores independientes; verificar suite Angular existente (npm test -- --watch=false desde frontend, solo cuando se autorice expresamente), sin Internet/targets reales ni dependencias nuevas. [FVE-09]
- [x] 5.2 Registrar resultados de revisión visual/accesible y pruebas ejecutadas/pendientes en el change; verificar cobertura de todas las filas de auditoría y escenarios FVE-01..09, sin atribuir a jsdom pruebas de layout/contraste. [FVE-09]
- [x] 5.3 Actualizar frontend/README.md y sección frontend de docs/02-arquitectura/arquitectura-general.md únicamente en lo afectado; revisar diff para verificar cero cambios backend/API/DB/contratos de evaluación/scoring/report/protocolos/dependencias y ejecutar openspec validate modernize-frontend-experience --strict solo con autorización vigente. [FVE-09]

## Estado de implementación y validación final (2026-11-19)

**18/18 tareas completas. CHANGE COMPLETE: YES. READY TO ARCHIVE: YES.** Cierre documental por implementación existente, evidencia automática histórica, inspección estática y aceptación manual explícita del operador de la versión FINAL, incluido el modal de cambios sin guardar. Sin defectos bloqueantes comunicados. No se archiva desde esta tarea ni se acredita despliegue de estos cambios.

| Tarea cerrada | Fundamento y alcance |
| --- | --- |
| 1.2 | Navegación/foco inspeccionados estáticamente; suite anterior y aceptación manual final de navegación, teclado y foco visible. Decisión de foco cerrada según design.md. |
| 4.2 | Reduced motion y estados visuales de la versión final, incluido el nuevo modal, aceptados manualmente por el operador. |
| 4.3 | Responsive y zoom 200% de la versión final aceptados por el operador; la matriz y capturas anteriores mantienen su alcance histórico. No se generan nuevas mediciones por viewport. |
| 4.4 | Teclado, foco visible, confirmación/cancelación y cierre mediante acciones previstas aceptados manualmente; semántica y foco inicial del modal inspeccionados estáticamente. No equivale a certificación WCAG ni a nueva prueba con tecnología asistiva identificada. |
| 5.1 | Suite histórica de 185 tests PASS y pruebas adicionales del modal implementadas; cierre documental aceptado por el operador junto con inspección estática y aceptación manual final. Las nuevas pruebas de target-form.spec.ts y flows.spec.ts siguen NOT_RUN: no se afirma regresión automática de la versión final ni un nuevo total de tests PASS. |
| 5.2 | Aceptación final registrada por separado de la anterior en evidence/result.md y evidence/result.json, con método y límites explícitos. |
| 5.3 | Documentación alineada con la versión aceptada y diff de alcance revisado: sin cambios backend/API/BD/dependencias. OpenSpec strict final registrado en evidence/result.md. |

### Modal y foco inicial — CLOSED

El dialog usa showModal(), título/descripción asociados y foco inicial en Seguir editando mediante autofocus y focus() explícito. Se prioriza conservar el trabajo; Salir y descartar requiere acción expresa. Escape o cierre sin confirmar deniegan la salida, con restauración al elemento de apertura si sigue conectado. Las solicitudes comparten la decisión pendiente; destruir el formulario resuelve como denegación. El secreto se limpia incluso al permanecer; el borrador no sensible se conserva. Cancelar pasa por el guard existente. La conversión REST/chat conserva su confirmación nativa.

App no mueve el foco al h1 en la primera navegación; en cambios posteriores sin fragmento sí lo hace. No se fuerza un segundo traslado inicial desde el shell que compita con el foco de los controles; skip-link y tratamiento de fragmentos permanecen. La inspección estática y la aceptación manual final no muestran contradicción bloqueante con FVE-02/FE-22. Se cierra la revisión de foco, sin atribuir una revisión independiente a otro agente o arquitecto ni modificar contratos.

### Evidencia y límites

- Históricos conservados: 185 tests Angular PASS, build PASS, 45 comprobaciones de contraste PASS, OpenSpec strict PASS previo y aceptación manual anterior.
- Aceptación manual FINAL: diseño, navegación, proyectos, target, scans, findings, report, eliminación, modal, confirmación/cancelación, teclado, foco, cierre, estados, responsive, zoom 200%, reduced motion y experiencia general. Fuente: confirmación explícita del operador; no revisión automatizada ni nuevas capturas del agente.
- El cierre documental no convierte las pruebas adicionales NOT_RUN en PASS ni el build histórico en build de la versión final. La aceptación de esta revisión usa la combinación de evidencia solicitada por el operador.
- En esta tarea no se ejecutan tests, build, servidor ni navegador; solo OpenSpec strict, autorizado expresamente. No código, AWS, commit, push ni archivado.
