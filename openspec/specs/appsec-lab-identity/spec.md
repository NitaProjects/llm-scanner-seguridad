# appsec-lab-identity Specification

## Purpose
Presentar los análisis con Risk Intelligence como capa visual principal —riesgo, categorías, Findings y evidencia— y trazabilidad técnica AppSec Lab como profundidad, sin modificar datos ni lógica del scanner.

## Requirements

### Requirement: AL-01 Dirección Risk Intelligence y precedencia

**Historia:** Como operador quiero localizar riesgo y problemas rápidamente. **Prioridad:** Alta. **Dependencias:** frontend-mvp, frontend-visual-experience y project-deletion vigentes.

La interfaz SHALL priorizar Risk overview → OWASP/category analysis → Findings → Evidence/decision trace. SHALL adoptar dark carbón/grafito, texto cálido y teal/petróleo, superficies diferenciadas y paneles selectivos sin encajonar todo. SHALL usar tipografía sans editorial y monospace solo para referencias técnicas. SHALL NOT adoptar consola terminal, violeta dominante, cyberpunk, glassmorphism, glow, gradientes llamativos o grid genérico de cards para toda la interfaz. Rojo SHALL identificar riesgo alto/crítico y error/destrucción con etiqueta; ámbar advertencias/inconclusos y verde resultados/estados positivos recibidos, sin prometer seguridad absoluta.

Esta dirección SHALL sustituir la primacía AppSec Lab de la revisión anterior de este change y las decisiones visuales incompatibles de FVE-01/02/05/06/07/08/09. SHALL conservar sus garantías funcionales/de accesibilidad. El change SHALL denominarse apply-risk-intelligence-visual-identity; ruta appsec-lab-identity e IDs AL SHALL mantenerse como identificadores documentales sin implicar estética de consola. SHALL NOT requerir nuevos mockups.

#### Scenario: Primera lectura de un resultado
- **WHEN** se abre un Scan COMPLETED con resultados disponibles
- **THEN** se identifican riesgo, problemas y categorías antes del detalle técnico
- **AND** evidencia, decisión, recomendación y referencias siguen accesibles sin depender de color o marca

### Requirement: AL-02 Risk Overview con fuentes reales

**Historia:** Como operador quiero interpretar el riesgo sin garantías falsas. **Prioridad:** Alta. **Dependencias:** FE-10/12/17, scoring-v1.

COMPLETED SHALL presentar Risk Overview con globalScore 0–100, riskLevel y scoringVersion de /score AVAILABLE; estado y duración de Scan cuando existan; findingCount e inconclusiveCount disponibles del resumen de score. Score SHALL tener jerarquía importante junto a Findings, sin monopolizar la pantalla. SHALL mantener visible el texto «Score heurístico. No representa probabilidad ni garantía de seguridad.» SHALL NOT representar score como porcentaje de seguridad ni usar report.summary como fallback. Cero SHALL conservar su advertencia adicional; inconclusos SHALL ser visibles. Carga/error/PENDING/UNAVAILABLE/NOT_APPLICABLE SHALL seguir diferenciados de cero. Conteos del score SHALL identificarse como tales si el listado no está disponible y SHALL NOT convertirse en ausencia confirmada o corregirse desde otro recurso.

#### Scenario: Cero con inconclusos
- **WHEN** /score devuelve globalScore=0 e inconclusiveCount mayor que cero
- **THEN** se muestran ambos datos y la advertencia heurística, sin afirmar seguridad ni ocultar incertidumbre

#### Scenario: Resumen y listado independientes
- **WHEN** el resumen indica un Finding y falla la lectura de /findings
- **THEN** se conserva el conteo según resumen y el error del listado, sin mostrar «sin hallazgos»

#### Scenario: Ausencia histórica
- **WHEN** score no está disponible o indica NOT_APPLICABLE
- **THEN** se explica su disponibilidad sin score/conteos inventados ni sustitución desde report-v1

### Requirement: AL-03 Technical Test Rail y RUNNING sin riesgo provisional

**Historia:** Como operador quiero distinguir actividad real de conclusiones. **Prioridad:** Alta. **Dependencias:** FE-08/09/15/18.

PENDING/RUNNING SHALL usar cabecera compacta con ID/estado/fechas disponibles y cuatro Technical Test Rails independientes LLM01/02/08/10 con identidad, ejecución, evaluación y resultado separados. SHALL NOT mostrar riesgo/score agregado provisional, porcentaje de progreso, ETA o timeline ficticio. Veredicto individual recibido SHALL mostrarse únicamente cuando evaluación COMPLETED lo proporcione, sin inferir score agregado. INCONCLUSIVE SHALL ser veredicto, no estado técnico. Registros no recibidos SHALL indicarse sin inventar PENDING/código/revisión. Selección SHALL ser distinta de actividad y solo RUNNING real con fuente vigente SHALL indicar actividad confirmada. Pausa/error SHALL identificar datos desactualizados y detener movimiento asociado. Lecturas, contadores técnicos y sus límites interpretativos SHALL conservarse.

#### Scenario: Selección y actividad diferentes
- **WHEN** se inspecciona una prueba completada mientras otra ejecución o evaluación está RUNNING
- **THEN** se distinguen selección y actividad sin cambios de estado, foco o score provisional

#### Scenario: Evaluación inconclusa y registros ausentes
- **WHEN** una evaluación está COMPLETED/INCONCLUSIVE y falta otra ejecución
- **THEN** se distingue resultado inconcluso de registro no recibido, sin inventar estado técnico ni fase

### Requirement: AL-04 Correlación y selección verificables

**Historia:** Como operador quiero inspeccionar la evidencia correcta. **Prioridad:** Alta. **Dependencias:** contratos públicos y AL-03/05.

La atribución entre ejecución, evaluación y Finding SHALL verificarse por IDs de ejecución/evaluación/Scan e identidad/revisión, con referencia findingId cuando exista. Categoría/posición por sí solas SHALL NOT bastar. Datos incompatibles/duplicados SHALL permanecer consultables con aviso seguro y sin atribución falsa. Un Finding sin evaluación recibida SHALL conservar sus campos/referencias propios sin afirmar cadena correlacionada completa. Selección SHALL ser local al Scan, estable al refrescar y reiniciarse al cambiarlo; fragmentos reconocidos SHALL tener prioridad. La selección SHALL NOT causar consultas adicionales, reordenar resultados, adelantar cargas terminales ni crear Findings.

#### Scenario: Correlación incompatible
- **WHEN** un Finding coincide en categoría pero sus referencias no corresponden al test seleccionado
- **THEN** no se atribuye a ese test, permanece consultable y se indica la limitación de correlación

#### Scenario: Veredicto anterior al cierre
- **WHEN** existe VULNERABLE individual durante RUNNING sin listado terminal cargado
- **THEN** se muestra ese veredicto sin fabricar Finding ni solicitar resultados antes del cierre

### Requirement: AL-05 Findings protagonistas y Finding Intelligence Panel

**Historia:** Como operador quiero entender el problema y qué revisar. **Prioridad:** Alta. **Dependencias:** AL-04, FE-14/15/18/23.

COMPLETED SHALL priorizar lista de Findings con severity, category, title, confidence, verdict y acción Inspeccionar, conservando el orden vigente por severidad. Finding Intelligence Panel SHALL presentar una composición única EVIDENCIA → DECISIÓN → RECOMENDACIÓN → REFERENCIAS, manteniendo test/revisión, description, evidenceSummary, verdict, severity, confidence y recommendation reales. evidenceSummary SHALL etiquetarse literalmente «Resumen de evidencia», nunca como cita textual, raw response o trace. Confianza SHALL ser soporte heurístico separado de severidad/probabilidad. Contenido SHALL ser inerte y SHALL NOT ampliar internals/prompts/respuestas crudas/RAG/tools/secretos. Listado ausente/cargando/fallido SHALL distinguirse de lista vacía; sin Finding SHALL NOT fabricarse evidencia/recomendación.

El cierre SHALL cambiar la jerarquía a overview/bandas/Findings conservando identidad de pruebas, selecciones/referencias y sin forzar foco. SHALL NOT exigir idéntica geometría técnica en RUNNING/COMPLETED. FAILED SHALL conservar explicación y datos parciales, sin simular cierre exitoso.

#### Scenario: Inspección de un Finding
- **WHEN** se selecciona un Finding correlacionado con markup en evidenceSummary
- **THEN** se muestra texto inerte como resumen, decisión, recomendación y referencias de test/revisión
- **AND** severity y confidence mantienen valores y significados separados

#### Scenario: Cierre y selección previa
- **WHEN** RUNNING pasa a COMPLETED con un test seleccionado
- **THEN** se presenta Risk Overview sin perder su referencia ni mover foco automáticamente
- **AND** puede inspeccionarse cualquier Finding desde la lista priorizada sin volver a una consola como vista principal

### Requirement: AL-06 Históricos, informe y fuentes independientes

**Historia:** Como operador quiero consultar resultados completos y trazables. **Prioridad:** Alta. **Dependencias:** FE-10 a FE-18 y report-v1.

La nueva jerarquía SHALL conservar todos los Findings/evaluaciones/conteos/severidades/motivos/avisos actuales, snapshot report-v1, limitaciones y referencias, con errores por recurso. SHALL mantener rutas y fragmentos de riesgo/categorías/Findings/ejecuciones/evaluaciones/informe y sus registros. Un enlace a un registro no seleccionado SHALL revelar el destino antes de desplazar/enfocar sin consulta adicional. Históricos no correlacionables SHALL seguir consultables. Discrepancias score/report u otros recursos SHALL seguir visibles sin recalcular/sustituir fuentes. degraded SHALL NOT reinterpretarse como un nuevo veredicto.

#### Scenario: Referencia desde informe
- **WHEN** report-v1 enlaza a otro Finding o evaluación
- **THEN** se conserva ruta, snapshot y referencias y se revela el destino consultable con teclado

### Requirement: AL-07 Projects como registro editorial

**Historia:** Como operador quiero abrir o eliminar proyectos con claridad. **Prioridad:** Alta. **Dependencias:** FE-02/03, PD-04.

Projects SHALL mantener registro compacto de nombre/descripcion/createdAt, Abrir único principal y Eliminar explícito, con estilo editorial sin grid de grandes cards. Nombre SHALL ser texto no enlazado. SHALL conservar creación inline, validaciones, foco y delete con diálogo/bloqueo/204/404/409/reconciliación ambigua existentes, sin retirada optimista ni reenvío automático. SHALL NOT mostrar riesgo/health/actividad del proyecto ni agregaciones no disponibles.

#### Scenario: Acciones de proyecto
- **WHEN** se abre o confirma eliminar un proyecto
- **THEN** existe un único Abrir y el borrado conserva confirmación/resultado vigente
- **AND** conflicto conserva la fila y respuesta ambigua requiere reconciliación sin otro DELETE automático

### Requirement: AL-08 Desktop-first y navegación real

**Historia:** Como operador quiero lectura rápida en escritorio sin perder acceso en anchos menores. **Prioridad:** Alta. **Dependencias:** AL-01 a AL-07/11.

Aceptación principal SHALL comprobar 1440/1280/1024px con overview, bandas, Findings e inspección legibles y RUNNING técnico utilizable. Debajo SHALL conservar reflow funcional, navegación/forms/modales/acciones y contenido sin overflow de página ni truncado irrecuperable, sin experiencia móvil dedicada. Navegación SHALL ser sobria/compacta y enlazar solo rutas/secciones actuales: Proyectos, histórico/análisis del proyecto, target e informe del Scan; SHALL NOT crear destinos globales inexistentes. SHALL conservar accesibilidad/reflow previos sin fase de mockups.

#### Scenario: Desktop y reflow
- **WHEN** se consulta un Finding largo a 1024px y se reduce el viewport
- **THEN** no se pierden acciones ni contenido y las secciones actuales siguen alcanzables sin navegación ficticia

### Requirement: AL-09 Accesibilidad y movimiento preservados

**Historia:** Como usuario de teclado quiero operar e interpretar sin depender del color. **Prioridad:** Alta. **Dependencias:** FE-22/23, FVE-07/08 y PD-04 en garantías conservadas.

La interfaz SHALL conservar teclado, foco visible no oculto, encabezados, labels/errores, skip-link, guard y modales. Contraste SHALL ser >=4.5:1 texto normal y >=3:1 texto grande/controles/foco informativos, con targets vigentes. Riesgo/severidad/estado/selección SHALL tener texto/símbolos además de color; verde SHALL NOT convertir score0 en garantía. Polling SHALL NOT repetir anuncios/mover foco/reiniciar animaciones. Reduced motion SHALL eliminar movimiento no esencial sin cambiar acciones ni depender de animationend. Score SHALL NOT animarse desde cero.

#### Scenario: Teclado y reduced motion
- **WHEN** se inspeccionan Findings, se seleccionan tests y se usan referencias/diálogos con reduce activo
- **THEN** destinos/acciones son accesibles, foco se mantiene/restaura y los estados se entienden sin movimiento ni color

### Requirement: AL-10 Regresión y límite de presentación

**Historia:** Como mantenedor quiero validar la identidad sin alterar el scanner. **Prioridad:** Alta. **Dependencias:** AL-01 a AL-09/11.

La implementación SHALL limitarse a presentación/selección/proyecciones de datos existentes, preservando backend, APIs/DTOs/decoders, Findings, validaciones, seguridad, acciones, polling, evaluación, scoring-v1, report-v1 y protocolos. SHALL NOT crear evolución temporal, tendencias/comparaciones, porcentaje de seguridad, attack surface, severidad nueva, métricas agregadas inexistentes o derivaciones de scoring no definidas. SHALL usar herramientas/dependencias existentes y fixtures sintéticos. Aceptación SHALL cubrir overview, bandas, Finding panel, estados/ausencias/errores/correlación/fragments/históricos/delete y suite Angular vigente, más revisión desktop/contraste/teclado/reduced motion. SHALL respetar autorización de ejecución y distinguir evidencia ejecutada/pendiente, sin presentar jsdom como verificación de layout. SHALL NOT requerir nuevos mockups.

#### Scenario: Presentación sin cambios de datos ni tráfico
- **WHEN** se seleccionan Findings/categorías/tests y llegan relecturas del seguimiento vigente
- **THEN** valores, decisiones, orden por severidad, cadencia y recursos solicitados permanecen iguales, sin mutaciones ni consultas decorativas

### Requirement: AL-11 OWASP Risk Bands comparables

**Historia:** Como operador quiero localizar la categoría problemática rápidamente. **Prioridad:** Alta. **Dependencias:** scoring-v1, AL-02/04 y evaluaciones públicas.

Las categorías LLM01/02/08/10 SHALL presentarse como filas/bandas comparables, no cuatro cards iguales. Cada banda SHALL mostrar código/nombre, score/riesgo/conteos de Findings e inconclusos solo desde CategoryScoreDto cuando score esté disponible; outcome solo desde finalVerdict de evaluación COMPLETED correlacionada. SHALL NOT inferir NOT_VULNERABLE de score0 ni inventar score/outcome si faltan recursos. Referencias de Findings SHALL proceder de relaciones verificadas; conteo de score SHALL NOT implicar listado recibido. Valores restantes y alcance output-only LLM10 SHALL seguir disponibles. SHALL NOT dibujar categorías como secuencia temporal o magnitud de attack surface.

#### Scenario: Score cero y outcome inconcluso
- **WHEN** una categoría tiene score0 y su evaluación correlacionada está COMPLETED/INCONCLUSIVE
- **THEN** se muestran ambos datos y el conteo real correspondiente sin etiquetarla como segura

#### Scenario: Score o evaluación ausentes
- **WHEN** falta uno de esos recursos y el otro está disponible
- **THEN** la banda conserva identidad y datos recibidos, explica el dato ausente y no reconstruye resultados desde otra fuente

### Requirement: AL-12 Shell completo con navegación y versión reales

**Historia:** Como operador quiero orientarme en un producto terminado sin enlaces ficticios. **Prioridad:** Alta. **Dependencias:** AL-08/09 y rutas actuales.

El shell desktop SHALL incluir sidebar de224–240px según viewport, marca con símbolo propio y dos niveles nombre/descriptor, navegación icono+label con fila objetivo48px, activo textual/accesible y separadores entre global/contexto. SHALL mostrar Proyectos global; Análisis del proyecto y Configuración del target solo con contexto válido, enlazando al detalle/histórico y target existentes; Informe del análisis solo con Scan/sección terminal disponible. SHALL NOT crear rutas globales /scans,/reports,/settings ni enlaces/controles de perfil/ayuda sin funcionalidad actual. Sin contexto SHALL explicar dónde se encuentran esas funciones mediante texto no interactivo, no navegación falsa.

El pie de sidebar SHALL mostrar versión real etiquetada «Frontend v…», sincronizada con frontend/package.json.version (actual0.0.0), sin afirmar versión backend/producto desplegado ni copiar v1.0.0 del mockup. SHALL NOT añadir backend, API, dependencias, bump de versión ni configuración de despliegue para obtenerla. Contexto SHALL usar datos ya cargados, sin nuevas lecturas. Esta concreción sustituye la anchura de176px de revisiones previas sin relajar accesibilidad/reflow.

#### Scenario: Navegación con y sin contexto
- **WHEN** se abre /projects y después un proyecto, su target o un Scan terminal
- **THEN** los destinos contextuales aparecen solo cuando pueden apuntar a sus rutas/secciones reales, con iconos y activo correcto
- **AND** no se añaden requests para llenar la sidebar ni opciones globales inventadas

#### Scenario: Versión del frontend
- **WHEN** se presenta el pie con la metadata actual del paquete
- **THEN** se ve «Frontend v0.0.0» y no v1.0.0 ni una versión backend deducida

### Requirement: AL-13 Projects con resumen honesto y panel de registro

**Historia:** Como operador quiero una colección legible con presencia visual. **Prioridad:** Alta. **Dependencias:** AL-07/10.

Projects SHALL presentar cabecera con título/descripción y CTA primary Nuevo proyecto con plus; un panel resumen de88–104px con folder, conteo de proyectos del listado cargado y label que explique su alcance; y un panel único surface con borde/radio8px y encabezado de columnas para el registro. SHALL NOT copiar KPIs de análisis/Findings totales, riesgo de proyecto o actividad no disponibles ni recorrer proyectos para agregarlos. Conteo SHALL distinguir carga/error de cero real. El resto del resumen puede incluir orientación textual sin fabricar otra métrica.

Cada fila SHALL incluir folder de24px, nombre/descripcion, createdAt alineado en su columna y botones visibles Abrir/Eliminar con icono+label. Nombre SHALL seguir no enlazado y Abrir SHALL ser el único acceso principal de fila. Filas SHALL tener altura normal objetivo88–104px, aumentar ante contenido largo/zoom y usar divisores/hover sin cards independientes ni mínimos de altura que rellenen artificialmente la página. SHALL preservar todas las acciones/validaciones de AL-07.

#### Scenario: Colección loaded y error
- **WHEN** el listado devuelve tres proyectos, cero proyectos o falla
- **THEN** se presentan respectivamente conteo3 y registro, cero real con empty state, o error sin conteo ficticio
- **AND** no aparecen análisis/Findings globales ni riesgo inventado

### Requirement: AL-14 Iconografía y acciones con affordance visible

**Historia:** Como usuario quiero reconocer navegación y acciones sin depender del hover. **Prioridad:** Alta. **Dependencias:** AL-07/09/12/13.

La UI SHALL reutilizar un único sistema SVG lineal existente, ampliando solo iconos necesarios para folder/analysis/report/settings/plus/open/trash/estado/riesgo. SHALL NOT añadir librerías múltiples, emojis decorativos ni recursos externos. Acciones principales/secundarias/destructivas SHALL tener icono+label, área rectangular y borde/fondo distinguibles en reposo: primary teal, secondary superficie/borde visible y destructive con semántica danger discreta y confirmación inequívoca. Abrir SHALL conservar semántica de enlace y Eliminar de botón; un enlace subrayado aislado SHALL NOT satisfacer el diseño de Abrir.

Controles SHALL tener min-height44px, icono18–20px y gap8px, estados hover/focus/disabled/loading explícitos y sin cambios de layout. Disabled SHALL bloquear de verdad; loading SHALL conservar label ocupado y prevención de duplicados. Foco SHALL ser visible y no tapado; iconos decorativos SHALL ser inertes para tecnología asistiva y botones repetidos SHALL identificar el proyecto. Ninguna acción SHALL depender de fin de animación.

#### Scenario: Acciones de fila y envío
- **WHEN** se abre una colección, se navega por teclado y se confirma un borrado
- **THEN** Abrir y Eliminar son reconocibles antes del hover, el foco es visible y el envío muestra estado ocupado sin segunda petición
- **AND** los controles preservan el contrato de navegación y delete existente

### Requirement: AL-15 Superficies y densidad de producto

**Historia:** Como operador quiero distinguir grupos y aprovechar el espacio de escritorio. **Prioridad:** Alta. **Dependencias:** AL-01/02/05/08/11/13.

La UI SHALL delimitar paneles de resumen Projects, registro Projects, Risk Overview, bandas OWASP, lista Findings y Finding Intelligence Panel mediante surface/borde/radio/encabezado consistentes. Surface-raised SHALL diferenciar encabezados/metadatos agrupados; evidencia/decisión/recomendación SHALL seguir secciones de un único panel con icono/label/divisor, no cuatro cards. SHALL NOT reducir todos los grupos a texto/divisores sobre background ni encajonar cada dato. RUNNING SHALL agrupar rails/inspector conservando actividad real. Scan COMPLETED SHALL aprovechar dos columnas de bandas+Findings e inspección cuando el ancho lo permita, haciendo reflow si1024px compromete lectura.

Desktop SHALL usar contenido fluido de max-width1440px dentro del área principal, gutters32px en1440/1280 y24px en1024, gaps16–24px y padding de panel20–24px. Títulos de página SHALL tener referencia32–36px (28–32px a1024), sección20–22px, proyecto18–20px, KPI Projects28–32px, score48–56px, descripción14–16px y metadata/referencia13–14px. Valores SHALL ajustarse por accesibilidad/contenido sin esconder datos. Registros SHALL ocupar el ancho útil; no espaciadores hero ni filas estiradas para llenar el viewport. Pocos registros SHALL dejar espacio natural sin métricas falsas.

#### Scenario: Densidad desktop con contenido normal
- **WHEN** se revisan a1440/1280/1024px Projects con tres filas normales y Scan con Finding seleccionado
- **THEN** marca/navegación/paneles/acciones tienen presencia y el contenido aprovecha el ancho útil con las proporciones indicadas
- **AND** descripciones largas o zoom aumentan altura/reflow en vez de recortar contenido

### Requirement: AL-16 Aceptación visual regional contra mínimos insuficientes

**Historia:** Como mantenedor quiero comprobar el acabado y no solo el render funcional. **Prioridad:** Alta. **Dependencias:** AL-12 a AL-15 y AL-09/10.

La reimplementación SHALL registrar evidencia por región y viewport1440/1280/1024, estados loaded/loading/empty/error y fixtures sintéticos normales/largos, manteniendo trabajo previo y regresiones. SHALL comprobar marca en dos niveles, navegación contextual real, versión, iconos, botones en reposo/hover/focus/busy, único conteo honesto de Projects, panel de registro con columnas, superficies/jerarquía Scan, alineación y reduced motion. A altura de referencia900px, Projects con tres filas normales SHALL mostrar título/CTA/resumen/encabezado/filas sin hero ni scroll inicial innecesario; esta referencia SHALL NOT recortar contenido largo ni impedir zoom. Scan SHALL mostrar overview y comienzo de bandas/Findings/inspección en el primer viewport equivalente.

Capturas de referencia SHALL usarse como dirección visual, no prueba de datos/código ni autoridad para copiar rutas, v1.0.0, Target URL histórica no disponible o Finding derivado de INCONCLUSIVE. La revisión SHALL declarar que las imágenes suministradas son vistas Projects/Scan elaboradas y que no se identificó una captura austera separada; las observaciones del actual se basan en el código y relato. PASS de tests DOM o dark mode SHALL NOT bastar para aceptar acabado. Si falta autorización/evidencia visual efectiva SHALL registrarse pendiente, sin afirmar PASS ni completar tareas de validación.

#### Scenario: Gate de acabado
- **WHEN** se solicita cerrar la corrección visual
- **THEN** se presenta checklist regional con medidas/capturas de la UI realmente implementada y regresiones ejecutadas/pendientes
- **AND** sidebar sin contexto/versionado, acciones como enlaces planos o registro sin panel impiden aceptar aunque los datos rendericen
