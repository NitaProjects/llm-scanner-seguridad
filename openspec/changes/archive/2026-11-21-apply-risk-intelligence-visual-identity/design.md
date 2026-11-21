## Context

Dirección final: **Risk Intelligence como capa visual principal; AppSec Lab como profundidad técnica**. Esta decisión del usuario sustituye la primacía AppSec Lab de la revisión anterior del mismo change. Ver proposal.md. El nombre final autorizado es apply-risk-intelligence-visual-identity; se mantienen ruta appsec-lab-identity e IDs AL por continuidad documental, sin nuevas capabilities/archivos. No se requieren mockups nuevos.

Frontend inspeccionado en esta revisión: Angular standalone, CSS propio, componentes por funcionalidad, signals/RxJS. Ya existen cambios locales de Risk Intelligence: Projects es un registro sin iconos de fila y Abrir es enlace subrayado; sidebar de 176px, solo Proyectos y sin versión; RiskSummary y FindingIntelligencePanel existen. La jerarquía/paneles de Scan están implementados parcialmente, no ausentes por completo. ScanDetailState conserva GET inmediato, ciclos separados 3s, pausa/reintentos/cancelación y carga terminal de Findings/score/report; no se modifica. El árbol contiene trabajo no confirmado de implementación, que esta revisión documental no toca ni presenta como validado.

Fuentes: docs/02-arquitectura/arquitectura-general.md, docs/05-api/openapi.yaml, frontend-mvp, frontend-visual-experience en modernize-frontend-experience, project-deletion consolidado y frontend/src/app/features/scans/models/scan.ts. No se detecta necesidad de backend. Este documento no acredita ejecución de UI ni tests.

## Goals / Non-Goals

**Goals:** primera lectura de riesgo, Findings, ubicación y soporte; jerarquía editorial analítica; cuatro componentes distintivos; profundidad técnica correlacionada y segura; desktop-first 1440/1280/1024px.

**Non-Goals:** nuevo cálculo de riesgo o outcome, métricas/tendencias, cambios backend/API/DTOs/decoders/BD/polling/evaluación/scoring/report/Findings/protocolos/validaciones/delete/seguridad, datos internos adicionales, nuevas consultas, dependencias, experiencia móvil dedicada o mockups.

## Decisions

### 1. Precedencia y elementos retenidos

La versión final prioriza Risk overview → OWASP/category analysis → Findings → Evidence/decision trace. De AppSec Lab se retienen correlación test/evidencia/decisión/recomendación, separación técnica/semántica, códigos/revisiones y Technical Test Rail en actividad/detalle. Se retiran como obligaciones: score relegado a 20–24px en dossier, geometría idéntica RUNNING/COMPLETED, rails protagonistas del cierre, cadena que empieza visualmente por TEST y estética uniforme de consola plana.

Respecto de FVE anterior, AL-01 sustituye paleta violeta/azul, repetición de cards, peso del shell, protagonismo de métricas y entradas de todas las filas; mantiene interpretación, funcionalidad, accesibilidad y reflow. Aceptación visual principal desktop sustituye la matriz de diseños móviles detallados, no sus garantías de usabilidad. No se reescriben changes/evidencias anteriores.

El rename autorizado a apply-risk-intelligence-visual-identity alinea el nombre con la dirección final sin crear otro change. La ruta de capability y prefijos AL siguen siendo identificadores documentales, no instrucciones de estilo.

### 2. Arquitectura y current → target

ScanPage coordina únicamente proyecciones puras y selección local de registros ya cargados. Componentes reciben datos y emiten acciones; sin HTTP ni motor de evaluación. Mantener servicios, ScanDetailState, controladores, guard, DTOs/decoders y fuentes por bloque. Sin persistencia de navegador ni store global.

| Actual | Target/reutilización |
| --- | --- |
| RiskSummary | Base de RiskOverview; score y riesgo importantes con límites visibles, sin trasladarlos únicamente al dossier |
| StatCard | Reutilizable para conteos reales dentro de una agrupación selectiva, sin fila de widgets equivalentes para todo |
| CategorySummary | OwaspRiskBands con filas comparables y detalles secundarios existentes |
| FindingList | Lista de señales priorizadas por severidad con acción Inspeccionar; FindingIntelligencePanel para selección |
| ScanProgress | TechnicalTestRailList/TechnicalTestRail en RUNNING y detalle técnico, no contenido dominante de COMPLETED |
| EvaluationList | Reutilizar metadatos/motivos/labels/avisos en detalle técnico y trazabilidad sin ampliar internals |
| PageHeader | Encabezado editorial en cierre/Projects; cabecera compacta de identificación en RUNNING, sin hero genérico |
| StatusBadge | Labels/símbolos semánticos consistentes; badges selectivos de riesgo/severidad/estado, no pills para cada dato |
| Projects/project-list | Registro compacto editorial, nombre no enlazado, único Abrir y Eliminar explícito |
| LoadingState/EmptyState | Estado de región, sin métricas falsas ni panel vacío gigante |
| Shell/sidebar | Sobrio y compacto; conservar Proyectos y accesos contextuales existentes a histórico, target e informe |
| ReportSections/TargetForm/FeedbackPanel | Solo coherencia de tokens/superficies; contratos, contenido, controles y validaciones intactos |

Firmas: RiskOverview, OwaspRiskBands, FindingIntelligencePanel y TechnicalTestRail. Se permite realizar el rework dentro del componente existente si evita una extracción innecesaria; no borrado automático ni framework de dashboards.

### 3. Datos permitidos y origen de cada indicador

| Información | Fuente real y límite |
| --- | --- |
| Score, riesgo, versión | ScanScoreDto.globalScore/riskLevel/scoringVersion, solo /score AVAILABLE |
| Findings e inconclusos del resumen | ScanScoreDto.findingCount/inconclusiveCount, etiquetados como resumen del score |
| Estado, fechas, duración | ScanDto.status/createdAt/startedAt/finishedAt; duración solo con inicio y fin válidos |
| Score/conteos por categoría | ScanScoreDto.categories: CategoryScoreDto.score, riskLevel, findingCount, inconclusiveCount, notVulnerableCount, evaluationCount, severityCounts |
| Outcome de categoría | EvaluationMetadataDto.finalVerdict con status COMPLETED y relación verificada con su test; CategoryScoreDto no tiene un campo outcome |
| Finding | FindingDto: title/category/severity/confidence/verdict/description/evidenceSummary/recommendation y referencias |
| Técnica | TestExecutionMetadataDto y proyección pública actual de EvaluationList |
| Proyecto | ProjectDto.name/description/createdAt y sus acciones actuales |

Cero de score no implica NOT_VULNERABLE. Un inconclusiveCount es conteo, no porcentaje de incertidumbre. Si falta evaluación correlacionada, outcome «No disponible», sin inferirlo del score. Si falta score, la banda conserva identidad y evaluación disponible pero no fabrica score/conteos; error, carga e histórico permanecen diferenciados.

Findings del overview proceden del resumen score; listado/detail proceden de /findings. Si score indica un Finding y el listado falla, mostrar «1 según resumen» y error de listado, nunca ausencia confirmada. Ante discrepancias no sobrescribir ni reparar datos. Igual independencia para snapshot report-v1: no usarlo como fallback del score; conservar aviso de divergencia.

Datos NO disponibles para esta UI: evolución temporal, tendencias/comparaciones históricas, porcentaje de seguridad, health/attack surface, riesgo o métricas agregadas por proyecto, actividad reciente global. No están expuestos en estos contratos públicos prompts, respuesta cruda ni contenido/traces de RAG/tools; no solicitarlos ni simularlos. No mostrar secretos ni internals de evaluator por estar presentes en algún DTO.

### 4. Scan COMPLETED: jerarquía Risk Intelligence

Cabecera contextual breve identifica proyecto/Scan, sin hero. Primera zona Risk Overview asimétrica: aproximadamente un tercio para score de 48–56px y riesgo, y el resto para estado, duración si existe, versión y conteos reales agrupados. Es una superficie de resumen selectiva, no tres métricas enormes en cards independientes. El score es importante, acompañado siempre de «Score heurístico. No representa probabilidad ni garantía de seguridad.» Cero conserva la advertencia adicional de no garantía; inconclusos quedan visibles. Score no es porcentaje.

Debajo, cuatro bandas OWASP comparables de altura acotada; después, Findings protagonistas. En desktop, lista de Findings aproximadamente 40% y panel de inspección 60%, con wrap/ajuste a 1024px. Cada entrada presenta severidad, categoría, título, confianza heurística, verdict y acción Inspeccionar. Orden vigente por severidad; no nuevo ranking que combine confianza/riesgo. Se inspecciona el primero del orden actual al cargar por primera vez si no hay selección/fragmento previo. Selección explícita no se cambia al refrescar ni por llegada de otro dato. Si el usuario ya eligió un test con Finding, conservar esa relación al cerrar; si no tiene Finding, no saltar automáticamente ocultando su detalle: el listado permite inspeccionar.

Detalles técnicos y referencias al informe siguen accesibles debajo/en expansión, sin duplicar de entrada todas las listas de ejecuciones/evaluaciones. No se modifica report-v1 ni se genera narrativa ejecutiva nueva. COMPLETED no fuerza todos los registros a COMPLETED: mostrar los recibidos. FAILED conserva error y datos parciales identificados; si un resultado real existe se etiqueta su disponibilidad, nunca se presenta un cierre exitoso inventado. Todo bloque conserva loading/error/retry propio.

Alternativa descartada: mantener dossier + rails como vista principal terminal, porque obliga a buscar primero metadatos técnicos para encontrar el problema. También se descarta dashboard de charts: no hay datos ni necesidad de nuevas magnitudes.

### 5. OWASP Risk Bands

Orden estable LLM01, LLM02, LLM08, LLM10, sin secuencia temporal. Columnas: código/nombre → score /100 y riesgo si disponibles → outcome recibido → Findings asociados/conteo → inconclusos. Severidades y conteos restantes siguen en detalle accesible de la banda. Categoría y outcome se comparan por alineación, no con cuatro cards equivalentes.

No radar, tendencia ni área que pretenda medir attack surface. Se prefieren número y etiquetas sin gráfico nuevo. Las bandas señalan Findings asociados solo mediante referencias reales. Los conteos por categoría se identifican como procedentes del score cuando el listado no esté disponible. Si hay diferencias entre recursos se explican, no se recalcula el resultado. LLM10 conserva alcance output-only y advertencia del consumidor posterior.

### 6. Finding Intelligence Panel y trazabilidad

Una composición única de lectura: EVIDENCIA → DECISIÓN → RECOMENDACIÓN → REFERENCIAS, con cabecera de título/categoría y severidad/confianza separadas. Referencia al test accesible desde cabecera y detallada al final. No cuatro cards independientes.

| Sección | Contenido |
| --- | --- |
| EVIDENCIA | Finding.evidenceSummary bajo label literal «Resumen de evidencia»; description contextual visible sin reemplazar el resumen |
| DECISIÓN | Finding.verdict, severity y confidence propios; confidenceLabel/confidenceHelp vigentes |
| RECOMENDACIÓN | Finding.recommendation literal |
| REFERENCIAS | securityTestCode, securityTestRevision, IDs públicos de Finding/evaluación/ejecución y Scan cuando correspondan |

No convertir evidenceSummary en cita del target ni raw trace. Interpolación inerte, sin HTML/Markdown activo, resaltado de spans inventados o dump de JSON. No ampliar la proyección restringida de EvaluationList para volcar reasoningSummary/proveedor u otros internals.

Correlacionar ejecución-evaluación por testExecutionId y scanId, confirmar category/code/revision; correlacionar Finding con evaluationResultId, testExecutionId, scanId y findingId si existe. No basta categoría/posición. Un Finding sigue consultable por sus campos públicos aunque falte la evaluación: sus referencias se muestran sin afirmar la cadena completa. Si hay IDs incompatibles o duplicados, señalar limitación de correlación y no atribuir evidencia a otro test ni seleccionar arbitrariamente. No recalcular verdict/confidence/severity.

Sin Finding, mostrar disponibilidad cargando/error o ausencia confirmada según /findings. INCONCLUSIVE permanece evaluación sin Finding, con su motivo público disponible, no recomendación/evidencia fabricada.

### 7. RUNNING: profundidad técnica sin riesgo provisional

Identificación compacta con Scan ID completo recuperable, estado, creación/inicio etiquetados y última lectura como hora del navegador. Sin score/riesgo provisionales, duración cerrada ficticia, ETA, cronómetro nuevo ni timeline.

Technical Test Rail conserva cuatro categorías independientes, ejecución/evaluación/resultado separados e inspector de selección. Selección != actividad. Registros no recibidos indican ausencia de lectura, no PENDING supuesto. INCONCLUSIVE es veredicto de evaluación COMPLETED, no Lifecycle. Fallos técnicos FAILED no significan VULNERABLE.

Actividad solo si status RUNNING real y fuente vigente; ante pausa/error se identifican datos desactualizados y cesa movimiento. No imponer exactamente un test activo. Información técnica solo HTTP/latencia/tiempos/motivos disponibles. Durante RUNNING puede mostrarse un veredicto recibido, pero no convertirlo en riesgo/score agregado provisional ni crear/consultar anticipadamente Findings.

Al cerrar se conserva identidad de tests, selecciones y referencias, pero cambia deliberadamente la jerarquía a Risk Overview/bandas/Findings. No obligación de conservar idéntica geometría. No forzar foco ni scroll por polling/terminal. Cadencia, tres fallos, lectura de recursos, timeouts/reintentos y cancelación de ScanDetailState intactos.

### 8. Selección, navegación, Projects y compatibilidad

Selección local técnica por test y de Finding por ID dentro del Scan; sin storage/store. Al cambiar Scan reiniciar; fragmento reconocido tiene prioridad. En RUNNING primera fila registrada (o primera categoría sin registro), nunca selección automática del activo. En cierre aplicar reglas de la sección 4 sin interrumpir una selección previa. Mantener índices para registros históricos/no correlacionables.

Conservar #risk/#categories/#findings/#executions/#evaluations/#report y #finding-{id}/#evaluation-{id}. Un fragmento revela el panel/sección antes de enfocar/desplazar; sin nuevas consultas ni IDs duplicados. Orden por severidad de todos los Findings preservado. No esconder datos históricos porque no encajen en la correlación.

Navegación sobria pero completa dentro del contexto disponible; la corrección de acabado de las secciones 11–16 sustituye la antigua referencia de sidebar de 176px. Histórico/análisis desde proyecto, target en ruta existente e informe como sección/fragmento del Scan. No nuevas páginas globales de Scans/Reports/configuración. Conservar breadcrumb/volver, deep links, skip-link, títulos y guard.

Projects: registro editorial continuo de nombre/description/createdAt, Abrir único principal y Eliminar visible. Sin riesgo por proyecto ni updatedAt como actividad. Nuevo proyecto/creación inline y delete conservan controladores: diálogo/foco, petición única, 204, 404, 409 y reconciliación ambigua, sin retirada optimista ni reenvío automático. Target/forms/modales e informe solo armonizan tokens/legibilidad. Ajuste solicitado por el operador el 20/11/2026: el formulario del target ocupa el mismo ancho útil que su aviso de autorización, sin la cota específica de 960px. Risk Intelligence se conserva como nombre de la dirección visual, no como texto visible del producto: la marca usa el descriptor Análisis de seguridad LLM y la franja superior muestra únicamente la ubicación actual.

### 9. Tokens, tipografía y movimiento

CSS variables propias y aliases --accent/--text/--muted existentes. No fuentes/dependencias externas. Propuesta final para implementar, sujeta a medir contraste efectivo:

| Token | Valor |
| --- | --- |
| --background / --surface / --surface-raised / --surface-subtle | #14191C / #1D2529 / #273237 / #182024 |
| --text-primary / --text-secondary | #F0EFE8 / #BAC4C2 |
| --primary / --primary-hover / --primary-soft / --on-primary | #79CBC4 / #A1E0D9 / #203D3D / #112522 |
| --border / --control-border / --focus | #46575A / #81979A / #B4EEE7 |
| --danger / --danger-soft / --on-danger | #F28F96 / #3D252C / #29151B |
| --warning / --warning-soft | #EAC17A / #3A3022 |
| --success / --success-soft | #99CEA7 / #24382D |
| --info / --info-soft | #ADD3DF / #24363D |
| --risk-none / --risk-low / --risk-medium / --risk-high / --risk-critical | #C5CECB / #ADD3DF / #EAC17A / #F5A0A0 / #F28F96 |
| --radius-control / --radius-panel | 6px / 8px |
| --shadow-panel | 0 4px 16px rgb(0 0 0 / 10%) solo en resumen/panel selectivo |
| --duration-fast / --duration-normal | 140ms / 200ms |

HIGH/CRITICAL se distinguen por texto/símbolo además de tono rojo, no por inventar severidad. Verde puede identificar finalización/resultado positivo recibido con alcance explícito; nunca score cero como «seguro». Inconcluso usa ámbar. Selección teal no implica actividad. Superficies diferenciadas, volumen contenido sin encajonar cada sección, sin glass/glow/gradientes llamativos ni estética terminal.

Sans base 16px, títulos editoriales 28–32px, score 48–56px con /100 y advertencia; a 1024px ajuste sin dominar toda la página. Monospace 13–14px solo IDs/códigos/versiones/timestamps/referencias. No números animados desde cero. Transiciones locales 140–200ms, sin reentrada de listas por polling. Reduced motion elimina lo no esencial/smooth scroll; ninguna acción/foco depende de animationend.

### 10. Validación desktop, accesibilidad y límites

Aceptar a 1440/1280/1024px con resumen, bandas y Findings legibles, inspector largo, estados mixtos e históricos. Debajo solo reflow funcional, controles/forms/modales/navegación accesibles sin overflow de página ni contenido recortado. No nueva experiencia ni matriz de diseño móvil. Preservar zoom/reflow y targets vigentes.

Teclado, h1/encabezados, focus visible no oculto, labels/errores, modales/guard, skip-link y fragmentos intactos. Controles de selección/Inspeccionar nativos y accesibles, sin tablist/grid incompleto. aria-live para cambios relevantes, sin anunciar cada polling ni mover foco. Contraste normal >=4.5:1; texto grande/controles/foco informativos >=3:1; no solo color; reduced motion vigente.

Pruebas futuras con Vitest/TestBed/HTTP simulado actual: overview real vs ausente, advertencia exacta, bandas/score/outcome de fuentes independientes, score0+INCONCLUSIVE, HIGH/CRITICAL, orden/selección/correlación, Finding inerte y campos completos, cambios RUNNING→COMPLETED sin riesgo anticipado, errores por recurso, report histórico, fragments y delete. Mantener suites app/flows/scan-presentation/results/scan-detail-state/projects/target; actualizar solo aserciones visuales obsoletas. Verificación en navegador de desktop/contraste/teclado/reduced motion/desaturación; jsdom no acredita layout. Ejecutar pruebas/build/servidor solo con autorización explícita; strict de OpenSpec sí solicitado.

### 11. GAP de acabado y alcance de las referencias

Las dos imágenes adjuntas muestran una vista Projects y una vista Scan COMPLETED elaboradas. No se identifica inequívocamente una captura austera separada. No atribuir a esas imágenes defectos que solo constan en el relato/código. Estado actual en esta tabla significa inspección estática del frontend local; no se ejecutó servidor ni medición nueva en navegador. Referencias visuales orientan acabado, no rutas, valores ni comportamiento.

| Referencia | Actual observado en código | Corrección necesaria |
| --- | --- | --- |
| Marca con símbolo y dos niveles | Shield existente, nombre en una línea lógica y peso reducido | Símbolo 30–36px, nombre 18–20px y descriptor Análisis de seguridad LLM 13–14px; no nuevo logo externo ni rebranding del producto |
| Sidebar con grupos, iconos y pie | 176px, un enlace global, sin versión ni grupos | 224–240px, grupo global + contexto real, separadores y pie de metadatos |
| Proyectos/Análisis/Informes/Configuración | Solo Proyectos es global; otras rutas son de proyecto/Scan | Navegación contextual condicionada; no enlaces globales ficticios |
| Tres métricas globales | Listado provee solo proyectos; no agregación global | Un panel de resumen con conteo real e información de uso, no tres KPIs simulados |
| Registro dentro de panel, iconos, fecha alineada | Registro abierto; fecha bajo nombre; sin iconos de fila | Panel único con encabezado y columnas icono, nombre/descripción, creado, acciones |
| Abrir/Eliminar con icono y borde | Abrir es enlace subrayado; Eliminar transparente | Ambos con área rectangular y borde visible, icono+label; conservar semántica enlace/botón |
| Resumen y detalle de Scan con presencia | Ya hay RiskSummary, bandas y FindingIntelligencePanel, con jerarquía parcial | Consolidar superficies/encabezados e iconos; no reescribir datos ni volver a crear funcionalidad |
| Versión al pie v1.0.0 | package.json declara 0.0.0; no release pública expuesta | Mostrar «Frontend v0.0.0» desde metadata del paquete, no v1.0.0 ni versión backend inventada |
| Ejemplo de Target y Scan-00042 | ScanDto no aporta endpoint histórico ni alias secuencial | ID real, abreviación recuperable; no atribuir URL del target actual al Scan histórico |
| Ejemplo de Finding LLM08 e INCONCLUSIVE simultáneos | Contrato solo permite Finding VULNERABLE | No copiar la inconsistencia del mockup; separar inconclusos de Findings reales |

La insuficiencia del diseño anterior estaba en permitir que «sobrio/compacto» se interpretara como eliminar presencia y affordances. Las siguientes decisiones son obligatorias y prevalecen sobre referencias aproximadas incompatibles de las secciones anteriores.

### 12. Shell y sidebar: presencia sin destinos inventados

Shell desktop de dos columnas; sidebar 240px a 1440/1280px y 224px a 1024px. Marca en bloque de 80–88px con icono shield propio, nombre LLM Scanner Seguridad y descriptor Análisis de seguridad LLM. Separador inferior; navegación con filas de 48px, iconos 20–22px y labels 15–16px. Estado activo mediante fondo teal-soft, indicador lateral 3px y aria-current; sin triple decoración ni subrayado obligatorio. Hover/focus visibles también en elementos no activos.

| Contexto | Enlace permitido | Condición |
| --- | --- | --- |
| Global | Proyectos → /projects | Siempre |
| Proyecto | Análisis del proyecto → /projects/{projectId}, histórico existente | Solo con projectId válido ya cargado; puede usarse anchor de histórico de presentación |
| Proyecto | Configuración del target → /projects/{projectId}/target | Solo con contexto de proyecto; no ajustes globales |
| Scan | Informe del análisis → ruta actual de Scan#report | Solo cuando la sección terminal correspondiente existe; también histórico/FAILED con disponibilidad explicada |

No hay /scans, /reports ni /settings globales. En Projects sin proyecto seleccionado se presenta una nota no interactiva «Los análisis, informes y configuración se consultan dentro de cada proyecto», no tres opciones falsas/disabled. La navegación contextual se alimenta de parámetros/datos que las páginas ya poseen, sin consultas ni nuevas selecciones de proyecto. Estado activo exacto: no marcar Proyectos como única sección activa en todas las rutas. Separa grupo global y contexto con título discreto; no cuenta acciones de shell como features nuevas.

Franja superior de contenido de 56–64px para breadcrumb/contexto existente, sin perfil, ayuda o ajustes ficticios para llenar el espacio. En Projects puede limitarse a contexto de colección; no breadcrumb duplicado. Pie de sidebar al fondo con separador, símbolo técnico discreto y versión frontend; puede incluir aviso breve de uso autorizado. Mantener el contenido/foco accesible a poca altura, sin fijaciones que lo tapen.

Versión: fuente única semántica frontend/package.json.version (actual 0.0.0), etiquetada «Frontend v…» y no «versión desplegada del producto/backend». Proyección de presentación local sincronizada con esa metadata, mediante valor readonly y comprobación de igualdad con package.json en la suite existente; no endpoint, nueva dependencia, bump, cambios de pipeline/configuración o copia arbitraria de v1.0.0. Si se opta por reutilizar metadata compilada directamente, no añadir configuración fuera de alcance. No introducir telemetría ni lectura HTTP de package.json.

### 13. Projects: resumen honesto y registro con presencia

Cabecera con título 32–36px y descripción útil de una línea, sin eyebrow repetido; Nuevo proyecto a la derecha como botón primary de 44–48px con plus. Misma creación inline y estados existentes.

| Métrica de referencia | Disponibilidad actual | Resolución visual |
| --- | --- | --- |
| Proyectos | Sí: length del listado completo cargado | Un panel resumen de 88–104px, icono folder en soporte 40–44px, número 28–32px y label «Proyectos en este listado» |
| Análisis totales | No como agregación disponible de la colección | Omitir; no recorrer cada proyecto/Scan, ni mostrar cero/placeholder como KPI |
| Hallazgos totales | No como agregado global | Omitir; no sumar respuestas parciales ni usar conteos de un Scan como global |
| Riesgo por proyecto/actividad | No en ProjectDto | Omitir; conservar descripción/createdAt reales |

El único panel resumen usa el resto del ancho para una frase breve de orientación sobre proyectos autorizados, no un valor inventado ni un diagrama de features. No tres cards deshabilitadas «sin datos». Mientras carga muestra estado textual; error no muestra count0; loaded vacío sí permite 0.

Registro en un panel surface con borde de 1px y radio 8px. Encabezado en surface-raised de 44–48px con columnas Nombre/Descripción, Creado, Acciones (más espacio de icono). Cada fila lleva folder 24px, nombre 18–20px/600, descripción 14–16px, fecha alineada 13–14px y Abrir/Eliminar a la derecha. Nombre no clickable; único Abrir como enlace con aspecto secondary. Eliminar botón destructive-outline con trash. El panel es único, filas separadas por divisores y hover suave/focus-within, no cards flotantes por proyecto. Descripción/IDs largos hacen wrap; no truncado irrecuperable.

Filas objetivo 88–104px con contenido normal y padding vertical 16–20px; altura crece si lo requiere contenido/zoom, no recorte. Anchuras de referencia: icono 40px, texto flexible, fecha 136–160px, acciones 208–224px con gap8. A 1024px permitir wrap de fecha/acciones antes de reducir legibilidad; la versión de ancho menor conserva labels y panel. Pocos proyectos dejan espacio natural al pie: no estirar filas para rellenar viewport ni añadir métricas ficticias.

### 14. Iconografía y sistema de acciones

Reutilizar Icon (SVG lineal propio, stroke 1.75, viewBox24). Ampliar solo el catálogo mínimo dentro del mismo sistema: open/arrow-up-right (sin implicar nueva pestaña), trash y, si hace falta, clock/layers para datos existentes. No instalar librerías ni mezclar emoji/icon fonts/SVG de proveedores. Mapeo: Projects=folder; análisis=shield o play según contexto; informes=document; configuración=settings; nuevo=plus; abrir=open; eliminar=trash; completado=check; riesgo/error=warning; inconcluso=question; referencia=document. Iconos decorativos aria-hidden, acción conserva label textual y nombre accesible de proyecto cuando se repite.

| Acción | Tratamiento obligatorio |
| --- | --- |
| Primary | Fondo teal sólido, on-primary contrastado, borde y radio6; plus+Nuevo proyecto; no más de una acción principal por zona |
| Secondary | Fondo surface-subtle, borde control visible, texto/icono teal o principal; Abrir conserva anchor/routerLink, no onclick ni enlace subrayado como única affordance |
| Destructive | Eliminar con trash+label, borde visible y semántica danger discreta en reposo; hover danger-soft; confirmación del diálogo danger relleno con contraste |
| Focus | Outline >=2px, offset2–3px, >=3:1 y no oculto |
| Disabled | native disabled donde corresponda, diferencia clara sin depender solo de opacidad; no hover de activo; jamás enlace funcional con solo aspecto disabled |
| Loading | Label «Creando…»/«Eliminando…» y busy vigente, ancho estable y sin duplicados; indicador pequeño opcional estático bajo reduce |

Botones/anchors con affordance de botón: min-height44px, padding-inline14–18px, iconos18–20px y gap8; secondary/destructive objetivo 92–108px cada uno, adaptables a traducción/nombre accesible. Transiciones de140–200ms, sin elevar/mover el layout al hover ni esperar animaciones para negocio. Mantener Cancelar y su foco inicial/retorno y bloqueo durante delete existentes.

### 15. Superficies, densidad y tipografía obligatorias

Superficies ya propuestas se mantienen, pero se explicitan usos: background para lienzo; surface para paneles de colección/overview/categorías/Findings/inspección; surface-raised para encabezados/resumen secundario o metadatos agrupados; surface-subtle para hover/área de evidencia. Borde estructural visible de1px; sombra contenida solo overview/overlay, no cada fila. Icono de resumen puede tener soporte cuadrado 40–44px en tono suave, sin glow.

Paneles obligatorios: resumen Projects con único conteo real; registro Projects con encabezado; Risk Overview con bloque score/riesgo y grupo de metadatos/conteos; panel bandas OWASP; panel lista Findings; Finding Intelligence Panel. Evidencia/decisión/recomendación siguen secciones internas con icono/label/divisor, no cuatro cards independientes. RUNNING agrupa rails/inspector en workspace delimitado. No convertir cada párrafo/control/fecha en card.

En Scan COMPLETED, tras overview, a 1440/1280px se prefiere composición de dos columnas: izquierda bandas+lista Findings, derecha inspección alineada al inicio de bandas, proporción aproximada55/45. A 1024px puede hacer reflow a una columna si mantener las dos compromete lectura; debe conservar prioridad y agrupación. Esta variante sustituye la interpretación rígida de «todo bandas y después todo Findings» que desperdicia ancho. Orden DOM conserva overview, categorías/Findings, detalle con foco/enlaces. Dossier de metadatos puede tener icono/labels/separadores; no usar endpoint actual como URL histórica ni alias Scan-00042 fabricado. Grupo de resumen usa iconos de estado/duración/versión/Findings/inconclusos; solo datos reales, sin duplicar widgets si la disponibilidad es parcial.

| Medida | Objetivo desktop |
| --- | --- |
| Content max-width | 1440px del área principal, fluido hasta el límite; sin columna de lectura estrecha para registros |
| Sidebar | 240px en1440/1280; 224px en1024 |
| Gutters de contenido | 32px en1440/1280; 24px en1024 |
| Inicio del contenido | 24–32px tras franja contextual; no hero ni espaciadores vacíos |
| Gap entre secciones/columnas | 24px normal, 16–20px donde el ancho lo requiera |
| Padding panel | 20–24px; registros 16–20px vertical |
| Fila navegación / proyectos / bandas | 48px / 88–104px / 64–80px como mínimos de diseño normal, no alturas máximas |
| Título de página | 32–36px/650–700; 28–32px a1024 |
| Título de sección / proyecto | 20–22px/600–650 / 18–20px/600 |
| KPI Projects / score Scan | 28–32px / 48–56px, números tabulares |
| Descripción / metadata / referencia técnica | 14–16px / 13–14px / 13–14px monospace |

Lectura extensa limitada a72ch dentro del panel, no max-width aplicado a todo el workspace. A1440, el registro aprovecha aproximadamente90% o más del área principal (límite por gutters); a1280/1024 sigue ocupando el ancho útil. No min-height artificial para lista ni márgenes verticales masivos. Titulación y espacios no sustituyen contenido existente; solo se compara con referencias usando igual viewport/zoom y cantidad de datos.

### 16. Aceptación de acabado y componentes afectados

Modificar App shell/app.html/app.css (o componente contextual mínimo si evita duplicación), Icon y su test, PageHeader, botones en estilos compartidos, ProjectsPage/CSS y resumen/StatCard reutilizado, RiskSummary, CategorySummary, FindingList, FindingIntelligencePanel y sus estilos. Metadata de versión local es presentación, sin tocar package.version ni backend. LoadingState/EmptyState/FeedbackPanel conservan roles y se integran en regiones existentes, con icono apropiado y CTA vigente, no zonas vacías impersonales. Formularios, guard y modales solo regresión/armonización visual.

La implementación no se acepta únicamente por DOM/textos o por estar en dark mode. Registrar matriz por región a1440/1280/1024, con estados loaded/loading/empty/error y datos sintéticos largos, sin red externa. Debe demostrar: marca2niveles; grupos contextuales correctos; pie de versión real; iconos y botones de acciones; único conteo real; panel y columnas Projects; superficies/jerarquía Scan; alineación, hover/focus y reduced motion. En Projects usar fixture de tres proyectos para comparar con la referencia y otro vacío, sin poblar runtime con datos ficticios.

Objetivos de viewport con altura de referencia900px: título, CTA, resumen, encabezado y primeras tres filas normales Projects visibles sin hero/scroll inicial innecesario. No obligar a que descripciones largas/zoom quepan en ese límite. Captura Scan con Finding seleccionado muestra overview y comienzo de bandas/Findings/inspección en el primer viewport; contenido largo sigue desplazable. Verificar medidas contra valores de la sección15 (tolerancias por fuente/contenido), no aprobación subjetiva de «parecido». Pruebas DOM verifican semántica/iconos/labels/rutas/datos; medición visual real valida proporciones/superficies/densidad. Si no hay autorización para ejecutar esa revisión, registrar pendiente y no marcar aceptación visual como PASS.

Las capturas suministradas no acreditan esta implementación ni sus datos. No copiar ayuda/perfil sin feature existente, v1.0.0, totales globales, URL histórica no disponible ni Finding derivado de INCONCLUSIVE. No se requiere nuevo mockup; la siguiente fase aplica estas correcciones al código ya existente y conserva su trabajo local.

## Risks / Trade-offs

- P1 riesgo/puntuación inventados → fuentes y disponibilidades estrictas, sin transformaciones nuevas ni provisionalidad.
- P1 evidencia atribuida a otro test o leakage → correlación exacta y proyección pública mínima/texto inerte.
- P2 score visualmente parece seguridad → advertencia fija exacta, /100 no %, cero neutral e inconclusos prominentes.
- P2 score/counts y listado divergen → etiquetar origen, error propio, no reparar ni ocultar discrepancias.
- P2 pérdida de datos/anchors o accesibilidad al priorizar Findings → índices completos, reveal/foco y regresión histórica/keyboard.
- P3 volumen devuelve grid genérico → solo resumen/inspección selectivos, bandas y registro continuo.
- P3 identificadores históricos de capability confunden → nombre del change alineado con Risk Intelligence y aclaración de que appsec-lab-identity/AL son referencias documentales.

## Migration Plan

Solo planificación en esta revisión; no código/migración/DB/deploy. Implementación futura por fases: proyecciones/tokens, Risk Overview/bandas, Findings/panel, RUNNING, Projects y regresión. Reutilizar componentes sin borrado automático. Actualizar frontend/README.md y sección Frontend de arquitectura solo al reflejar implementación real. No reescribir evidencia ni changes anteriores. Rollback futuro solo de presentación, sin cambios de datos.

## Open Questions

Ninguna decisión material bloqueante. Risk Intelligence y desktop-first están decididos. El rename autorizado a apply-risk-intelligence-visual-identity está aplicado y no altera el alcance de implementación.
