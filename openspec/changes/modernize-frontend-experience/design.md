## Context

Motivación en proposal.md. Auditoría estática del código actual; no se ha ejecutado servidor ni revisión de píxeles en navegador. Fuentes contractuales: frontend-mvp (change aún disponible), project-deletion consolidado, docs/02-arquitectura/arquitectura-general.md y docs/05-api/target-contract.md. No se detecta contradicción que requiera backend. El contexto histórico TFM de openspec/config.yaml no se utiliza para ampliar ni reabrir alcance.

### Auditoría por área (situación anterior a la modernización)

| Área y archivos en frontend/src | Observado | Intervención propuesta |
| --- | --- | --- |
| styles.css | Tokens claros, escala de espaciado/tipo, controles de 44px y focus-visible ya presentes; no tema oscuro | Evolucionar las variables existentes, sin segundo sistema paralelo |
| app/app.html, app.css, app.ts, app.routes.ts | Header horizontal, único destino global Proyectos, footer, skip-link y foco de h1 por navegación; cuatro rutas lazy y fallback | Sidebar compacta y header contextual, sin destinos ficticios ni duplicación de enlaces |
| features/projects/pages/projects-page.ts | Cards nombre/descripción/fecha, creación inline, vacío/carga/error y borrado con dialog nativo; estilos del diálogo inline | Mejor jerarquía y acción destructiva secundaria pero identificable; diálogo y feedback coherentes |
| projects/components/project-create-form.ts | Nombre/descrición, validación y foco inicial; no vista de edición de Project | Mejorar agrupación, validación visual y acciones; no añadir edición |
| projects/pages/project-page.ts y components/project-scan-list.ts | Target/resumen, lanzamiento y estados de admisión ambiguos, histórico como lista; cargas mezclan paneles y párrafos | Zonas claras de configuración, acción principal e histórico; unificar feedback sin cambiar reconciliación |
| target/pages/target-page.ts y components/target-form.* | REST/chat condicionales, fieldsets, headers ordinarios, credencial efímera y guard; algunos errores carecen de asociación ARIA explícita; confirmaciones de cambio/tipo y salida son nativas | Paneles de conexión/contrato/auth, asociación label-error y estados consistentes; confirmación de salida integrada según ajuste autorizado; conversión de tipo nativa y validaciones conservadas |
| scans/pages/scan-page.* y state/scan-detail-state.ts | Larga composición vertical; título genérico de resultado también durante ejecución; polling controlado, errores por bloque y última lectura | Header dependiente del estado y dos jerarquías: seguimiento activo / resultados terminales; mantener ownership de estado y transporte |
| scans/components/scan-progress.ts | Contadores técnicos, lista de pruebas con estado/latencia; no porcentaje ni fase remota | Filas/cards de pruebas y señal indeterminada acotada, texto de lectura pausada/desactualizada |
| risk-summary.*, category-summary.* | Score grande y contadores reales; categorías 4/2/1 columnas; advertencias de cero e inconclusos | Refinar cards y severidades; conservar advertencias y distinguir riesgo, confianza y estado |
| finding-list.*, evaluation-list.ts | Details nativo, orden por severidad, resumen/evidencia/recomendación y metadatos seguros | Mejor contraste/lectura y secciones expandibles; ningún JSON/raw output nuevo |
| report-sections.* | Snapshot report-v1, alcance/interpretación/limitaciones/trazabilidad; conflictos de fuentes explícitos | Resumen existente destacado y detalle organizado, sin resumen ejecutivo generado ni sustitución por datos actuales |
| shared/ui/page.css, status-badge.*, feedback-panel.ts, icon.ts, not-found-page.ts | CSS compartido, badges textuales, iconos SVG propios; loading/feedback repetido entre páginas; algunos párrafos sin patrón uniforme | Reusar componentes y extraer patrones pequeños; mantener SVG único, semántica y fallback |

En la situación auditada no había dashboard global, gráficos, toast global ni sidebar. Había feedback inline de guardado/borrado. La implementación local incorpora la sidebar prevista; no añade dashboard, gráficos ni toast global. La API permite contar proyectos del listado ya cargado; el histórico aporta estado/fechas por proyecto y el detalle aporta score/conteos/categorías/duración. No aporta agregación global de Findings/riesgos/actividad: no recorrer proyectos/scans para fabricar dashboard. Los bindings streaming/sesiones/workflows/cookies/CSRF/observaciones RAG-tool son operativos, no formularios actuales: mantener su compatibilidad sin crear editores.

Problemas de fuente: tema claro incompatible con dirección pedida; superficies planas; estilos de diálogo locales; cargas expresadas de varias maneras; jerarquía poco diferenciada entre progreso/resultados; escasa microinteracción. Responsive actual ya colapsa grids y definiciones: no se afirma un fallo de viewport sin verlo. Riesgos a comprobar en navegador: nombres/URLs largos, fila de headers, modal, categorías y alto contraste del tema nuevo.

## Goals / Non-Goals

**Goals:** presentación oscura consistente y reconocible, seguimiento honesto, resultados legibles, acceso por teclado y adaptación móvil. Separar presentación de servicios/estado/contratos.

**Non-Goals:** los excluidos de proposal.md. Tampoco cambiar política de reintentos, cadencia polling, mensajes de veredicto, orden de Findings, expiración de secretos ni confirmaciones funcionales para facilitar animaciones.

## Decisions

### 1. Tokens propios y tema principal oscuro

CSS custom properties en styles.css porque ya existen y atraviesan componentes standalone sin dependencia nueva. Conservar alias --text/--muted/--accent y variantes --risk-* durante adaptación; --accent pasa a alias de --primary y se añade --info para azul. Preferirlo a Tailwind/Material u otro framework, que dispararía sustitución y riesgo. No selector de tema ni persistencia de preferencia en este incremento; color-scheme: dark en controles nativos.

Paleta propuesta, sujeta a verificación de contraste de cada combinación en implementación (no certificación actual):

| Token | Valor | Uso |
| --- | --- | --- |
| --background | #080F1F | Fondo general azul-negro |
| --surface | #101B30 | Cards/paneles |
| --surface-raised | #17243C | Modal/elementos elevados |
| --surface-subtle | #0D172A | Inputs y áreas secundarias |
| --border | #33425D | Separadores decorativos |
| --control-border | #71839A | Controles identificables |
| --text-primary | #EDF2FF | Texto principal |
| --text-secondary | #A5B4CF | Ayuda/metadatos |
| --primary / --primary-hover | #A99BFF / #C0B5FF | CTA/enlaces/activo |
| --on-primary | #101426 | Texto sobre CTA relleno, no --surface como texto implícito |
| --primary-soft | #272443 | Fondo activo sutil |
| --success / --success-soft | #63DCA0 / #123127 | Finalización técnica/éxito de operación |
| --warning / --warning-soft | #F4C46A / #342A19 | Inconcluso/advertencias |
| --danger / --danger-soft | #FF8E9E / #381E2B | Error/acción destructiva |
| --info / --info-soft | #69C8FF / #132D43 | Información/seguimiento |
| --focus | #8CD7FF | Foco visible independiente |
| --risk-none / --risk-low | #BCC9DC / #91C8ED | Sin hallazgos/bajo; ninguno implica seguridad |
| --risk-medium / --risk-high / --risk-critical | #F4C46A / #FFA979 / #FF8E9E | Severidad textual + tono semántico |

Variantes suaves de riesgo se asignan a superficies semánticas correspondientes. Danger relleno requiere --on-danger oscuro explícito y contraste comprobado; el resto usa texto claro sobre fondo oscuro. Verde no representa «seguro» por score cero. Rojo/ámbar/verde no se usan para decoración/marca; activo y CTA usan violeta/azul. Mantener fuentes system-ui y monospace locales, números tabulares y tamaño base 16px.

Espaciado: conservar --space-1..6 = 4/8/12/16/24/32px; añadir --space-7=48px solo para separación de áreas. Radios: --radius-control=8px, --radius-panel=14px, --radius-badge=999px. Elevación: --shadow-panel=0 4px 16px rgb(0 0 0 / 12%); --shadow-overlay=0 12px 32px rgb(0 0 0 / 24%). Bordes/superficies bastan en la mayoría de cards. Duraciones --duration-fast=140ms, --duration-normal=220ms, --duration-slow=280ms; easing de salida suave consistente. Nada de fondos animados, glow o blur.

### 2. Shell y datos disponibles

Sidebar de unos 216px en >=1024px, marca, destino Proyectos y aviso de uso autorizado. El único destino global evita simular dashboard/scans globales. Breadcrumb/header contextual se alimenta de la página ya cargada, sin HTTP adicional. En móvil/tablet <1024px usar navegación compacta en flujo con marca y enlace textual; se prefiere a un drawer por la única entrada disponible y para evitar gestión de foco innecesaria. Conservar skip-link, navegación activa, raíz/404, títulos, fragmentos, foco de h1 y guard.

Proyectos muestra número del listado cargado como dato contextual, no varias cards de métricas vacías. Cards con nombre prioritario, descripción, fecha y acceso; eliminar como botón textual secundario de peligro, visible también con teclado/touch, sin tarjeta entera clickable con botones anidados. Proyecto conserva target/ejecutar/histórico. No añadir búsquedas, filtros, paginación ni métricas remotas.

### 3. Composición y reutilización

Reutilizar Icon y StatusBadge (riesgo/severidad/estado/veredicto), FeedbackPanel (errores/reintento), RiskSummary, ScanProgress, FindingList, EvaluationList, CategorySummary, ReportSections y formularios existentes. No crear RiskBadge/StatusBadge duplicados, FindingCard si no se reutiliza, ni capa de estado global.

Extraer PageHeader en shared/ui por repetición real de título/descripcion/acciones; EmptyState por cuatro usos existentes; LoadingState por cargas repetidas, con etiqueta requerida y aria-busy en región. StatCard pequeño para contadores reales de RiskSummary, sin HTTP, score calculado ni lógica semántica; score permanece en RiskSummary. Nombres sin sufijo Component, selectores app-page-header/app-empty-state/app-loading-state/app-stat-card, coherentes con el repo. CSS base queda en styles.css y page.css; CSS específico junto al componente. No extraer un ConfirmDialog genérico de un único borrado: mantener dialog nativo en ProjectsPage y mover su estilo a hoja local si procede.

Feedback: éxito inline discreto role=status, error persistente FeedbackPanel con acción disponible. No gestor de toasts: evita nueva infraestructura y perder errores por autocierre. Modal delete conserva Cancelar como foco inicial, Escape y restauración de foco según estado vigente, bloqueo durante envío, rechazo por scan activo, 404 y reconciliación ambigua. Se anuncia éxito solo tras 204; no animación de desaparición optimista. Su nombre se interpola como texto inerte.

Forms: fieldsets diferenciados, controles oscuros y errores textuales asociados con aria-describedby/aria-invalid en cada campo pertinente, agrupación headers legible, acciones visibles sin sticky que oculte foco. Preservar REST/chat y Conservar/Reemplazar, eliminación de auth, límites, errores de servidor y limpieza de secretos. Sin previsualizaciones de credenciales ni editores JSON/workflow nuevos.

Ajuste solicitado explícitamente por el operador tras la implementación inicial: la confirmación de salida con cambios sin guardar usa un `dialog` integrado, con «Seguir editando» como foco inicial y «Salir y descartar» como acción destructiva. El guard existente acepta una decisión asíncrona; Escape o cierre no confirmado deniegan la salida y restauran foco cuando corresponde. Se limpia el secreto antes de preguntar, incluso si se permanece en el formulario. Cancelar pasa por el mismo guard sin segunda confirmación ni marcar el borrador como guardado. No se cambia la confirmación de conversión REST/chat ni se sustituye la UI del navegador al cerrar/recargar una pestaña.

### 4. Estados de Scan

| Estado | Presentación | Restricción |
| --- | --- | --- |
| PENDING | «Análisis pendiente», etiqueta, explicación de espera/preparación y señal indeterminada | No afirmar fase remota concreta ni contador 0/4 antes de plan recibido |
| RUNNING | «Análisis en curso», indicador indeterminado moderado, pruebas/evaluaciones finalizadas con denominadores recibidos, filas de estado y última lectura | No porcentaje, ETA ni total de pasos workflow inferido; finalizar técnicamente no significa pasar prueba |
| COMPLETED | «Resultado del análisis», score/conteos reales destacados, categorías/Findings y enlaces a evaluación/informe | Esperar cada recurso; no convertir loading/error/NOT_APPLICABLE en cero |
| FAILED | Mensaje seguro y datos parciales conservados; enlace existente al proyecto para recuperación | No afirmar informe completo ni reintento/cancelación remotos inexistentes |

La actividad visual corresponde a estado confirmado y lectura vigente. Si seguimiento está pausado/desactualizado, se detiene la animación y se explicita la incertidumbre; última lectura no es actividad remota. No cambiar ScanDetailState: GET inmediato, 3s entre ciclos, timeout actual, no solapamiento, pausa tras tres errores, cancelación en navegación/terminal. Transición a resultado se activa una vez por cambio real de estado/ID, no por cada polling; no forzar foco ni desplazar al usuario al llegar datos.

### 5. Riesgo, Findings y report-v1

RiskSummary como card principal: número 0–100 + etiqueta heurística/riesgo, conteos hallazgos/inconclusos; severidades en agrupación textual y badges. Sin gauge/ring: el número ya es claro y un progreso circular sugiere cobertura. Cero sigue neutral y conserva aviso de no garantía; INCONCLUSIVE, FAILED y NOT_VULNERABLE nunca se fusionan. Confianza se etiqueta separada de severidad. Mantener la interpretación vigente de degradación, sin inferir motivos nuevos desde degraded=true.

FindingList conserva orden/identidad y details accesible: cabecera severidad/categoría/título/confianza, cuerpo descripción/evidenceSummary/recomendación. Enlace al test/evaluación solo si ya existe relación pública verificable; no inventarlo. Evidencia se mantiene como texto inerte, no HTML/Markdown activo ni raw JSON. No descargar datos internos para decorar.

ReportSections destaca el resumen report-v1 existente y organiza alcance, interpretación, limitaciones y trazabilidad. No genera un resumen ejecutivo nuevo. Conserva snapshot histórico, versiones, enlaces por fragmento y aviso de divergencia con /score; categorías/Findings actuales no reemplazan silenciosamente el snapshot. Limitaciones y advertencias visibles, trazabilidad expandible.

### 6. Movimiento útil y económico

CSS transitions como mecanismo, sin Angular animations ni dependencias nuevas. Hover/focus de botones/cards 140ms; entrada única de vista/card/feedback 220ms con opacidad y desplazamiento <=4px; modal entrada 180ms y salida solo si implementación nativa no retrasa cierre/foco; salida instantánea aceptada por compatibilidad. Findings entran una vez al cargarse, no al refrescar ni expandirse repetidamente. Details abre nativamente, sin max-height que corte contenido. No animar posición mientras se usa teclado.

Indicador indeterminado durante seguimiento confirmado: una única barra/pulse sutil en ScanProgress; no varios spinners/pulsos por card. Cesa al pausar/terminal/salir. No listeners/timers nuevos para movimiento ni solicitudes adicionales. Eliminar puede atenuar el elemento después de éxito sin retrasar retirada/foco; no mantener tarjeta accionable residual. Feedback persistente sustituye toast, con entrada breve sin autocierre obligatorio.

prefers-reduced-motion: reduce elimina transforms, animaciones de entrada/salida, pulse/shimmer y smooth scroll; indicadores se vuelven estáticos con texto «En curso». No esperar animationend para negocio, foco o navegación. No animar números del score desde cero ni modificar valores con contadores visuales.

### 7. Responsive y accesibilidad verificables

Revisión a 1440/1280 (escritorio), 1024 (portátil/sidebar), 768 (tablet/nav compacta), 390 y 360px (móvil). Shell fluido y contenido de lectura limitado; categorías 4/2/1 según ancho útil, métricas se apilan, listas de scans siguen cards/rows y formulario de headers pasa de dos columnas a una. No tablas nuevas. URLs/IDs/evidencia largos hacen wrap; sin scroll horizontal de página ni truncado irrecuperable. Modal con ancho de viewport menos gutters y altura máxima desplazable; botones accesibles sin recorte. Comprobar reflow a ancho equivalente 320px y zoom 200%.

WCAG 2.2 AA aplicable: contraste texto normal >=4.5:1, texto grande >=3:1, controles/foco informativos >=3:1; bordes solo decorativos exentos, no controles. Foco visible sin ocultamiento, touch targets >=44px, orden de encabezados/DOM coherente, teclado para nav/details/forms/dialog, labels y errores asociados. Estado/severidad siempre texto además de icono/tono. aria-live anuncia solo cambios significativos, no toda la lista cada 3s; evitar regiones redundantes. No renderizar SVG/HTML recibido ni usar color como única distinción.

### 8. Verificación posterior a aprobación

Reutilizar Vitest/TestBed/HttpTestingController y suite actual: app.spec, fragment-navigation.spec, projects-page.spec (delete), target-form.spec/validators/decoder, scan-detail-state.spec y results.spec. Añadir pruebas de nuevos componentes y de integración de presentación. Fixtures sintéticos PENDING/RUNNING/COMPLETED/FAILED, datos largos, cero real vs ausente, inconclusos, snapshots históricos, errores independientes, conflicto score/report, delete 204/404/409/timeout ambiguo, REST/chat y secreto efímero. Espiar que render/transición no aumentan llamadas HTTP ni reenvían mutaciones.

Tests DOM comprueban roles/textos/labels/foco/acciones/clases de estado, no hashes de CSS ni snapshots masivos. jsdom no acredita layout/contraste/reduced-motion efectivo: verificar en navegador media reduce/no-preference, anchos/zoom, teclado y lector de pantalla básico; registrar capturas/observaciones sin datos reales. Automatizar layout/media solo si ya existe soporte adecuado; no incorporar runner/dependencia por este change. Medir contraste de combinaciones reales y comprobar que no hay animaciones no esenciales en reduce. Solo ejecutar suites y comprobaciones con autorización explícita al implementar.

## Risks / Trade-offs

- P1: cambio de presentación filtra evidencia o altera solicitud/secretos → interpolación inerte, sin nuevos datos y regresión de contratos; bloquea aceptación si ocurre.
- P2: tema oscuro reduce contraste, controles heredados usan --surface como foreground → tokens on-primary/on-danger y revisión de estados/default/hover/focus/disabled/invalid.
- P2: movimiento sugiere progreso/seguridad o repite anuncios → indeterminado honesto, cambios reales, reduced motion, cero neutral.
- P2: modal/anchors/sidebar rompen teclado o reconciliación → conservar controladores y tests específicos, no eliminación optimista.
- P3: sidebar con un destino ocupa espacio → tamaño contenido y nav compacta bajo 1024px; sin relleno de destinos inexistentes.
- P3: dark-only no cubre preferencia clara → tema principal solicitado; selector y segundo tema quedan como evolución futura, no infraestructura anticipada.

## Migration Plan

Sin migración de datos/API/DB ni despliegue en esta tarea. Implementar tokens y shell, patrones, proyectos/target, seguimiento/resultados y validación como fases del mismo change; no dejar consumidores con colores claros hardcodeados. Actualizar únicamente frontend/README.md y sección frontend de arquitectura si describen diferente presentación. Reversión futura del frontend por mecanismo vigente, sin rollback de datos. No editar documentación histórica archivada.

## Open Questions

Sin decisiones abiertas para este cierre documental. El operador ha aceptado manualmente la versión FINAL, incluido el modal, navegación, teclado, foco, responsive, zoom 200% y reduced motion. La evidencia automática anterior conserva su alcance histórico; las nuevas pruebas del modal siguen sin ejecución. Véase [evidencia](evidence/result.md).

**Decisión de foco — CLOSED:** TargetForm.confirmLeave() abre el dialog nativo y enfoca explícitamente Seguir editando; el botón también tiene autofocus. El diálogo tiene título/descripción asociados. Se favorece conservar los cambios; Salir y descartar requiere elección expresa. Escape/cierre no confirmado equivalen a permanecer y restauran el foco al elemento de apertura si sigue conectado. El comportamiento es determinista, preventivo y coherente con la aceptación manual final del operador.

En app.ts la primera navegación no traslada automáticamente el foco al h1; las posteriores sin fragmento sí. Así el shell no fuerza un segundo movimiento inicial que compita con el foco de un control. Se conservan skip-link y navegación por fragmentos. La inspección estática y la aceptación manual final no identifican una contradicción bloqueante con FVE-02/FE-22. Se cierra la revisión pendiente sin modificar requisitos ni código y sin atribuir aprobación a un arquitecto distinto del operador.
