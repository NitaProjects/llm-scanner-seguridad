## 1. Proyecciones de datos y selección

- [x] 1.1 Adaptar proyecciones puras de overview, bandas y técnica usando DTOs ya cargados; verificar origen de score/conteos frente a outcome de evaluación, datos ausentes, score0+INCONCLUSIVE y ausencia de derivaciones/estadísticas nuevas, sin cambios DTOs/decoders. [AL-02/03/11]
- [x] 1.2 Mantener correlación por IDs/Scan/test/revisión y selección local de test/Finding; verificar duplicados/referencias incompatibles, Finding consultable sin evaluación, selección/fragmentos estables, primera inspección según orden por severidad vigente y ninguna nueva solicitud. [AL-04/05/06]

## 2. Sistema visual Risk Intelligence y navegación

- [x] 2.1 Aplicar tokens carbón/grafito/teal, superficies selectivas, radios/tipografía editorial y score jerárquico sin métricas gigantes; verificar contraste de texto/controles/badges/modales y estados normal/hover/focus/invalid/disabled, sin violeta dominante/glow/glass/gradientes llamativos ni cuerpo monospace. [AL-01/09]
- [x] 2.2 Rework/reutilizar PageHeader, StatusBadge, LoadingState, EmptyState y shell/sidebar para navegación sobria y estados legibles; verificar rutas/fragmentos/skip-link y acceso contextual a histórico/scans/target/informe sin nuevos destinos globales. [AL-01/08/09]

## 3. Scan COMPLETED: Risk Overview y OWASP Risk Bands

- [x] 3.1 Rework de RiskSummary como RiskOverview con score/riesgo/estado/duración/versión/conteos disponibles, agrupación selectiva y reutilización de StatCard solo donde aporte; verificar advertencia exacta «Score heurístico. No representa probabilidad ni garantía de seguridad.», cero/inconclusos, score ausente/error/histórico y conteo según resumen con listado fallido, sin fallback a report. [AL-02]
- [x] 3.2 Transformar CategorySummary en OwaspRiskBands con cuatro filas comparables, score/outcome/conteos de sus fuentes reales y referencias verificadas; verificar score0 inconcluso, evaluación o score ausentes, diferencias entre recursos, todos los valores secundarios existentes y advertencia output-only LLM10 sin cuatro cards equivalentes. [AL-11/06]
- [x] 3.3 Integrar jerarquía terminal overview → bandas → Findings → profundidad técnica; verificar continuidad de identidad/selección al cierre, cargas/errores independientes y FAILED con datos parciales sin simular éxito ni exigir geometría de consola idéntica a RUNNING. [AL-02/05/06]

## 4. Findings protagonistas y Finding Intelligence Panel

- [x] 4.1 Rework de FindingList para lista priorizada con severidad/categoría/título/confianza/verdict y acción Inspeccionar; verificar orden por severidad actual, acceso a todos, selección estable y listado vacío/cargando/fallido diferenciados. [AL-05]
- [x] 4.2 Reutilizar trazabilidad/EvidenceChain dentro de FindingIntelligencePanel con EVIDENCIA → DECISIÓN → RECOMENDACIÓN → REFERENCIAS; verificar test/revisión, description/evidenceSummary/verdict/severity/confidence/recommendation íntegros, label «Resumen de evidencia», texto inerte, sin cita/raw/internals ni cuatro cards. [AL-04/05]
- [x] 4.3 Preservar índices/secciones/fragmentos y ReportSections con su snapshot, limitaciones y referencias; verificar enlace a Finding/evaluación no seleccionados, reveal/foco, históricos no correlacionables y divergencias sin reparación ni ocultación. [AL-06/09]

## 5. RUNNING y Projects

- [x] 5.1 Reutilizar ScanProgress/EvaluationList como Technical Test Rail y detalle seleccionado, con cabecera compacta; verificar ejecución/evaluación/resultado separados, selección distinta de actividad, ausencias/desactualización/pausa/error, veredicto individual sin score/riesgo provisional y ninguna carga anticipada de resultados. Mantener ScanDetailState intacto. [AL-03/04/10]
- [x] 5.2 Mantener Projects como registro editorial de nombre/descripcion/fecha y único Abrir más Eliminar, sin grid de cards ni riesgo de proyecto; verificar creación inline/carga/vacío/error/validaciones y cero agregaciones nuevas. [AL-07]
- [x] 5.3 Armonizar diálogo/feedback de eliminación y consumidores de tokens en ProjectPage/TargetForm/guard/ReportSections sin cambiar controladores; verificar foco/Cancelar/petición única/204/404/409/reconciliación ambigua, REST/chat/credenciales efímeras/validaciones y contenido del informe intactos. [AL-06/07/09/10]

## 6. Regresión y aceptación desktop

- [x] 6.1 Completar fixtures/tests de proyecciones, overview, bandas, Findings/panel y transición RUNNING→COMPLETED; adaptar solo aserciones visuales obsoletas de app/flows/fragments/scan-presentation/results, manteniendo suites scan-detail-state/projects-delete/target. Verificar con npm test -- --watch=false desde frontend solo cuando se autorice, sin Internet ni nuevas dependencias. [AL-02 a AL-11]
- [x] 6.2 Revisar UI implementada a 1440/1280/1024px con score0/inconclusos, riesgo alto, Findings largos, error por recurso e histórico; registrar lectura priorizada de riesgo/problema/evidencia y comprobación acotada de reflow/zoom por debajo, sin diseño móvil dedicado ni nuevos mockups. Ejecutar navegador solo con autorización. [AL-01/08/10]
- [x] 6.3 Verificar contraste efectivo, teclado/foco/fragments/modales, reduced motion y lectura de riesgo/estado/selección en escala de grises; registrar pruebas ejecutadas y pendientes sin atribuir layout/contraste a jsdom ni aprobar por color solamente. [AL-09/10]
- [x] 6.4 Actualizar únicamente frontend/README.md y sección Frontend de arquitectura según implementación real, registrar evidencia y revisar diff: cero cambios backend/API/BD/DTOs/decoders/Findings/seguridad/validaciones/polling/evaluación/scoring/report/protocolos/dependencias. Verificar openspec validate apply-risk-intelligence-visual-identity --strict con autorización vigente; sin cambios de nombre adicionales ni reescritura de historial. [AL-01/10]

## 7. Corrección de acabado: shell, iconos y acciones

- [x] 7.1 Rework de app shell/sidebar según design12: anchura224–240px, marca dos niveles, separadores, activo/focus y navegación contextual sobre rutas existentes; verificar Projects sin contexto, proyecto, target, RUNNING y terminal con links válidos, sin páginas globales ficticias ni lecturas nuevas. Conservar cambios locales de implementación existentes. [AL-12]
- [x] 7.2 Añadir pie de versión frontend de fuente package.json.version mediante metadata readonly de presentación sincronizada y comprobación de igualdad; verificar label actual «Frontend v0.0.0», ninguna copia de v1.0.0 ni claim de release/backend y cero cambios package.version/dependencias/pipeline/despliegue/backend. [AL-12]
- [x] 7.3 Ampliar Icon solo con open/trash y símbolos mínimos necesarios, manteniendo estilo SVG propio y reutilizando folder/plus/document/settings/shield/warning/check/question; verificar catálogo, tamaños, aria-hidden y nombres accesibles sin nueva librería. [AL-14]
- [x] 7.4 Aplicar variantes primary/secondary/destructive con borde/fondo en reposo e icono+label y estados hover/focus/disabled/loading; verificar Abrir anchor con affordance de botón y Eliminar button, min-height44px, cero duplicados y modales/guard/foco/credenciales intactos. [AL-14/09]

## 8. Corrección de acabado: Projects y grupos de Scan

- [x] 8.1 Añadir resumen Projects con único conteo real e icono/label y orientación textual, sin KPIs globales; verificar loaded3/loaded0/loading/error, ninguna solicitud por proyecto/Scan y no cero ficticio o tres cards vacías. [AL-13]
- [x] 8.2 Dar presencia al registro: panel/borde/encabezado/columnas, icono folder por fila, fecha alineada y acciones enmarcadas; verificar filas normales88–104px y crecimiento para texto/zoom, único Abrir, delete204/404/409/ambiguo, sin grid de cards ni espacio artificial de relleno. [AL-13/14/15]
- [x] 8.3 Consolidar RiskOverview/bandas/listaFindings/inspector como paneles jerárquicos con encabezados/iconos y superficies elevadas selectivas; verificar layout desktop de dos columnas cuando quepa, grupo de estado/duración/versión/Findings/inconclusos solo reales y evidencia/decisión/recomendación dentro de un panel, sin trasladar inconsistencia del mockup ni datos raw. [AL-02/05/11/15]
- [x] 8.4 Ajustar PageHeader, styles/page.css, gutters/content-max, tipografía y densidad según design15; verificar ancho útil a1440/1280/1024, contenido largo sin recorte, reflow inferior funcional y sin cambios de lógica, rutas, API o porcentajes inventados. [AL-15/08]

## 9. Gate de reimplementación visual

- [x] 9.1 Extender pruebas existentes de app/Projects/Icon/acciones/Scan para context links, versión, resumen disponible/ausente y semántica de controles; verificar mismos requests, payloads, validaciones, polling, selección/correlación, score/report y delete. Ejecutar suites solo con autorización explícita; no usar tests DOM como prueba de densidad/layout. [AL-12 a AL-16]
- [x] 9.2 Registrar revisión visual real a1440/1280/1024 y altura de referencia900px con tres proyectos normales, vacío/loading/error y Scan/Finding largo; entregar checklist de marca/navegación/versionado/iconos/botones/paneles/proporciones/jerarquía y mediciones del diseño, además de teclado/contraste/reduced motion. Si no está autorizada la ejecución, mantener tarea pendiente y no declarar PASS. [AL-16/09]
- [x] 9.3 Confirmar que no se han introducido métricas/rutas/profile/help ficticios, v1.0.0, URL histórica de target no disponible ni Finding para INCONCLUSIVE; verificar diff limitado al alcance autorizado y evidencia de regresión, actualizar documentación vigente directamente afectada y validar OpenSpec strict cuando esté autorizado, sin reescribir historial ni eliminar trabajo local. [AL-10/16]

## Evidencia de la revisión R2 — 20/11/2026

28/28 completas según [resultado](evidence/result.md). Suite final registrada:232 passed/0failed/0skipped; buildPASS. Las capturas finales son exclusivamente losJPG `r2-*` del manifiesto. Las capturas y matriz previas son intermedias.

6.3 y9.2 completadas con aceptación manual del operador sobre R2: escala de grises, foco y navegación con zoom real al200%, y revisión regional AL-16 a1440/1280/1024px. El operador aprueba el acabado final sin regresiones visuales bloqueantes ni datos ficticios. Esta confirmación completa la evidencia previa; no se presenta como prueba automatizada ni como una nueva sesión de Computer Use. Change completo y listo para archivar; no archivado en esta actualización.
