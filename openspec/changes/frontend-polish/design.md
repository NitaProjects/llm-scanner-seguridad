## Context

Motivación y límites: [proposal.md](proposal.md). Contrato adicional: [FP-01..FP-16](specs/frontend-polish/spec.md). Estado: **READY FOR IMPLEMENTATION**, sin código implementado por este change.

La inspección es estática de CSS/templates y documentación, sin ejecutar navegador, servidor, tests ni compilación. La percepción visual aportada por el usuario orienta la propuesta; todavía no existe evidencia renderizada de este acabado.

| Fuente observada | Base real y consecuencia |
| --- | --- |
| `frontend/src/styles.css` | Tokens `surface/background/text/muted/accent/border/focus`, sistema tipográfico nativo, h1 hasta 2.3rem, controles de 44px y scroll suave condicionado a movimiento. Evolucionar esta base, sin reset/framework nuevo. |
| `app/app.css`, `app.html` | Contenedor de 1200px, padding principal 2rem/1rem/3rem, min-height 75vh, header flex con padding 1.2rem, marca y enlace Proyectos sin estilo activo. Conservar estructura y destinos; compactar altura y alineación. |
| `shared/ui/page.css` | Card genérica con márgenes 1.2rem y padding hasta 1.6rem, grid de dos columnas, reglas genéricas de listas y fieldsets. Evitar suma de márgenes de card/lista/títulos. |
| `shared/ui/status-badge.ts`, `feedback-panel.ts` | Badges textuales con tonos locales hardcoded; FeedbackPanel solo tiene inputs info/error y roles existentes. Unificar colores con variables sin ampliar sus APIs. |
| ProjectsPage, ProjectPage, ProjectCreateForm, ProjectScanList | Templates inline, creación integrada con foco inicial y retorno a `#new-project`; listado de cards y target/histórico separados. Mantener acciones, bindings y estados. |
| TargetPage, `target-form.html` y metadatos de TargetForm | Cuatro fieldsets, cabeceras en grid, autenticación condicional y acciones finales. Agrupar con CSS y wrappers que preserven el árbol de formularios. |
| `scan-page.html` y RiskSummary, CategorySummary, FindingList, ScanProgress, EvaluationList, ReportSections | Resumen ya existe (score hasta 4.5rem, dos contadores); categorías 4/2/1 columnas; Finding con card dentro de sección card; detalles nativos; informe sin segunda lista. Refinar jerarquía, no sustituir fuentes o interacción. |
| NotFoundPage | Título, párrafo y enlace sin tratamiento visual común; aplicar la misma composición de estados. |
| `frontend/README.md`, FE-01..FE-24 y diseño de `frontend-mvp`; sección frontend de arquitectura general, ADR-005 y ADR-008 | Preservar resumen prioritario, advertencias, resultados heurísticos, disponibilidades independientes y nueve limitaciones. No hay contradicción funcional identificada que deba resolverse aquí. |

`frontend-mvp` conserva textos históricos de planificación; el código, README y arquitectura documentan su implementación posterior. No reescribir esos históricos ni volver a aprobar su arquitectura. No se necesita inspeccionar backend, secretos o OpenAPI para este incremento de presentación.

## Goals / Non-Goals

**Goals:** aumentar reconocimiento y densidad útil con pocos tokens y patrones compartidos; diseñar las seis superficies solicitadas; hacer explícitas las 26 decisiones y su trazabilidad; mantener el MVP fácil de revisar y demostrar.

**Non-Goals:** nueva arquitectura frontend, clases base, componentes por cada icono/card, variantes sin consumidor, cambios de formularios/estado o soluciones funcionales encubiertas en templates. Sin nueva ADR: las decisiones son locales de presentación y se registran aquí.

## Decisions

**Registro de aprobación:** D-01..D-26 aprobadas explícitamente por el usuario el 2026-10-11, con las tres aclaraciones siguientes incorporadas. Los bloques rotulados «Propuesta» conservan la formulación original ahora aprobada; el resto de decisiones permanece sin cambios. No hay decisiones visuales pendientes. Esta petición actualiza planificación y no solicita implementar.

| Aclaración aprobada | Decisiones afectadas | Requisitos / tareas |
| --- | --- | --- |
| SVG inline propios sin duplicación masiva; patrón reutilizable ligero, sin librería externa | D-24/D-25 | FP-05; 2.4/6.2/7.11 |
| LOW azul diferenciado de primary/CTA y sin apariencia de acción | D-10/D-11 | FP-06; 2.4/7.6/7.11 |
| Validar las 50 combinaciones; conservar capturas representativas, no 50 capturas | D-26 | FP-16; 7.1..7.11 y Evidence Record |

### D-01 Dirección visual

- **Propuesta:** SaaS técnico claro: fondo gris azulado, superficies blancas, acento azul actual, bordes sutiles y tipografía sobria. Seguridad se comunica con contenido e iconos funcionales, sin rebranding.
- **Alternativa/motivo:** un tema oscuro o apariencia terminal cambia la identidad y el alcance; mantener todo igual no corrige la jerarquía. La evolución azul/gris minimiza cambios y conserva reconocimiento.
- **Impacto:** FP-01/FP-02; sin marca nueva, assets externos, gradientes ni ornamentación.

### D-02 Layout global

- **Propuesta:** shell, main y footer comparten contenedor; gutter interior 16px bajo 768px, 24px entre 768 y 1199px y 32px desde 1200px. Padding vertical principal 24px móvil y 32px desde tablet; secciones separadas 24/32px, cabecera a contenido 24px. Sin alturas fijas ni centrado vertical de estados vacíos.
- **Alternativa/motivo:** full width dificulta lectura; padding grande en todos los tamaños desperdicia móvil. Retirar la dependencia de `min-height:75vh` permite ritmo determinado por contenido; el footer sigue en flujo normal.
- **Impacto:** FP-03/FP-14; ordenar en DOM, sin reposicionamiento CSS que altere lectura o foco.

### D-03 Ancho máximo

- **Propuesta:** `--content-max:1200px` conserva el valor inspeccionado; es el ancho exterior máximo del contenedor, incluido gutter por border-box. Párrafos documentales pueden limitarse a `72ch`; TargetForm a `960px` alineado al inicio, sin estrechar las demás páginas.
- **Alternativa/motivo:** 1180px ofrece diferencia mínima frente a 1200px y no responde a un defecto observado. Reducir todos los contenidos limita categorías y formularios. La densidad se corrige con agrupación y márgenes.
- **Impacto:** FP-03/FP-14; header usará contenedor interior alineado al main, sin duplicar fórmulas de ancho y padding.

### D-04 Escala de espaciado

- **Propuesta:** `--space-1:4px`, `--space-2:8px`, `--space-3:12px`, `--space-4:16px`, `--space-5:24px`, `--space-6:32px`, `--space-7:48px`. 48px solo para separación excepcional de grandes bloques, no para rellenar estados vacíos. Gap de título/ayuda 8px; filas 12/16px; padding de panel 20px desktop y 16px móvil (20px = 16+4); secciones 24/32px.
- **Alternativa/motivo:** una escala extensa introduce opciones innecesarias; reutilizar márgenes actuales de todos los elementos genera acumulaciones. El contenedor organiza el gap; cards no añaden margen exterior por defecto.
- **Impacto:** FP-02/FP-03; evitar resets de margen agresivos fuera del área diseñada.

### D-05 Tipografía

- **Propuesta:** conservar `system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif`. Base 16px/1.5; h1 `clamp(1.5rem,2vw,1.875rem)`/1.2 y peso 700; h2 1.25rem/1.3 y 650; h3 1rem/1.4 y 650; metadatos/ayudas 0.875rem/1.5; badge 0.8125rem/1.3 y 600; CTA 0.9375rem y 600. Número de score `clamp(2.5rem,5vw,3.5rem)`/1.05 y 750; `/100` 1rem. Números con `font-variant-numeric:tabular-nums`.
- **Alternativa/motivo:** títulos enormes y score actual de hasta 4.5rem compiten con contenido; bajar todo a 14px perjudica lectura. Mono del sistema solo para JSON/identificadores, sin presentar una falsa terminal.
- **Impacto:** FP-02/FP-10; H1/H2/H3 conservan su orden semántico, y textos de API no se reescriben.

### D-06 Tokens de color centrales

Valores propuestos para `styles.css` (definición única, no implementación en esta fase):

| Token | Valor | Uso |
| --- | --- | --- |
| `--background` | `#f4f6fa` | Fondo existente |
| `--surface` | `#ffffff` | Paneles y campos |
| `--surface-subtle` | `#eef2f7` | Cabeceras de bloque y hover neutro |
| `--text-primary` | `#172b45` | Texto/títulos |
| `--text-secondary` | `#4f6075` | Ayudas y metadatos |
| `--border` | `#c7d1df` | Separación no interactiva |
| `--control-border` | `#71839a` | Límites de inputs/controles |
| `--accent` | `#1857a1` | Primary/enlaces |
| `--accent-hover` | `#12457f` | Hover primary |
| `--accent-soft` | `#eff6ff` | Activo/estado informativo |
| `--focus` | `#17499a` | Foco visible |
| `--success` / `--success-soft` | `#166534` / `#f0fdf4` | Confirmación técnica |
| `--warning` / `--warning-soft` | `#754500` / `#fff8e6` | Advertencia e incertidumbre |
| `--danger` / `--danger-soft` | `#991b1b` / `#fff1f2` | Error y riesgo crítico |

- **Propuesta:** mantener `--text:var(--text-primary)` y `--muted:var(--text-secondary)` como alias para consumidores existentes. Añadir los pares de riesgo de D-11; no generar una escala completa de tonos.
- **Alternativa/motivo:** renombrar en masa todas las clases aumenta diff; dejar colores locales dispersos perpetúa inconsistencias. Los alias permiten migración visual acotada.
- **Impacto:** FP-02/FP-06/FP-15; valores de partida sujetos a comprobación de contraste en QA, sin declarar auditoría realizada. Border sutil no basta como único límite de control: usar control-border.

### D-07 Superficies

- **Propuesta:** fondo neutro + una superficie blanca por grupo que necesite delimitación; secciones de categorías/histórico/informe pueden usar título y separadores sin caja exterior. Reservar fondos suaves de estado a badges/avisos pequeños, no al panel entero de resultados.
- **Alternativa/motivo:** todas las secciones en cards produce capas repetidas; todo plano pierde agrupación. Agrupar donde hay unidad de lectura o interacción.
- **Impacto:** FP-02/FP-03/FP-11/FP-12; los estados no ocupan todo el viewport.

### D-08 Bordes, radio y sombra

- **Propuesta:** bordes 1px; `--radius-control:6px`, `--radius-panel:10px`, `--radius-badge:999px`; `--shadow-panel:0 1px 2px rgb(23 43 69 / 4%)`, solo para resumen principal/formulario cuando se necesite separación. Listas/filas sin sombra. Acento de Finding 3px como máximo, no dependencia exclusiva del color.
- **Alternativa/motivo:** radios grandes y sombras profundas parecen ornamentales; ausencia absoluta de bordes confunde superficies. No usar sombras de elevación en hover de filas que no sean controles.
- **Impacto:** FP-02/FP-03/FP-11; no `!important` ni estilos inline de elementos.

### D-09 Botones

- **Propuesta:** primary azul/blanco; secondary blanco/texto azul/borde control; text/link sin superficie fuerte, enlace subrayado en contexto textual. Altura mínima y objetivo clicable de 44px, padding 10px 14px, icono 18px y gap 8px. Una acción contextual domina cada grupo. Hover cambia fondo/borde; focus outline 3px con offset 3px; disabled explícito con tonos opacos legibles y sin hover activador, evitando opacity global que degrade texto.
- **Alternativa/motivo:** hacer todo primary elimina jerarquía; botones gigantes desperdician espacio. Danger queda definido semánticamente para acciones destructivas existentes; no se añade eliminar proyecto, Scan ni credenciales.
- **Impacto:** FP-05/FP-15; clases actuales `.button` y `.secondary` siguen siendo base. Guardar NONE continúa usando su confirmación/advertencia existente, no una operación nueva.

### D-10 Badges

- **Propuesta:** altura visual aproximada 26–28px por padding 4px 8px, borde 1px, texto 13px y símbolo opcional 14px. No son controles, por lo que no necesitan 44px. Los badges de riesgo son estáticos: sin hover de acción, cursor pointer, tabindex de control ni semántica de botón/enlace. `StatusBadge` conserva sus inputs y mapping; la availability usa texto/wrapper estilizado en las ramas actuales, sin ampliar el tipo Badge ni añadir estados a modelos.
- **Alternativa/motivo:** un componente nuevo por enum fragmenta presentación; unificar enums para compartir color altera semántica. Estilos comunes y tonos por atributos de presentación existentes son suficientes.
- **Impacto:** FP-06; texto siempre visible, sin tooltips imprescindibles. No introducir `role=status` en cada badge ni anuncios por tick.

### D-11 Paleta de riesgo y severidad

| Significado | Texto / fondo | Indicador propio con texto |
| --- | --- | --- |
| NO_FINDINGS | `#334155` / `#f1f5f9` | Círculo neutro; Sin hallazgos detectados |
| LOW | `#365b7a` / `#edf2f7` | Escudo simple; Riesgo bajo / Severidad baja |
| MEDIUM | `#754500` / `#fff8e6` | Aviso simple; Riesgo medio / Severidad media |
| HIGH | `#8a330b` / `#fff3ed` | Aviso; Riesgo alto / Severidad alta |
| CRITICAL | `#991b1b` / `#fff1f2` | Aviso marcado; Riesgo crítico / Severidad crítica |
| INCONCLUSIVE | `#754500` / `#fff8e6` | Círculo con interrogación; Resultado inconcluso |

- **Propuesta:** tokens pares `--risk-none[-soft]`, `--risk-low[-soft]`, `--risk-medium[-soft]`, `--risk-high[-soft]`, `--risk-critical[-soft]`; alias medium/critical a warning/danger cuando coincidan. LOW mantiene azul desaturado `#365b7a` sobre `#edf2f7`, distinto del primary saturado `#1857a1` con texto blanco; no reutiliza accent/accent-soft ni estilos de acción. INCONCLUSIVE usa warning con icono/etiqueta diferentes; nunca es valor de RiskLevel. No se calculan tonos desde números de score.
- **Alternativa/motivo:** verde para NO_FINDINGS puede sugerir garantía; rojo para cualquier inconcluso confunde incertidumbre con fallo. Texto distingue niveles incluso donde forma/tono coinciden. CRITICAL se diseña para fixtures o históricos, sin habilitar asignación automática.
- **Impacto:** FP-06/FP-10; color localizado, sin cards enteras rojas/naranjas.

### D-12 Shell/header e identidad ligera

- **Propuesta:** header blanco, borde inferior, altura natural aproximada 64–72px desktop, contenido en contenedor compartido; marca 18px/700 con pequeño escudo propio (sin lock que prometa seguridad), enlace Proyectos con icono de carpeta y activo azul suave/subrayado. En móvil puede envolver en dos líneas con gaps 8/12px, sin menú hamburguesa ni posición fija. Footer discreto conserva aviso actual.
- **Alternativa/motivo:** sidebar o navegación nueva ocupa espacio y cambia alcance. `RouterLinkActive` de Angular existente permite una clase y `aria-current="location"` en Proyectos para rutas descendientes; no cambia rutas ni servicios de navegación. Para wildcard no debe activarse.
- **Impacto:** FP-04/FP-05; import exclusivamente de presentación si hace falta; skip-link y foco actuales intactos. El símbolo es decorativo junto al nombre completo.

### D-13 ProjectsPage

- **Propuesta:** encabezado compacto con H1/descrición a izquierda y Nuevo proyecto con plus a derecha; formulario inline debajo. Listado semántico en una columna de filas/superficies compactas: nombre/enlace primero, descripción completa debajo, fecha secundaria; sin grid de tarjetas altas. Sin datos, usar D-14.
- **Alternativa/motivo:** grid desperdicia espacio con pocos proyectos y textos variables; truncar descripciones cambia accesibilidad del contenido. Una columna aprovecha el ancho con jerarquía clara y crece con contenido real.
- **Impacto:** FP-07; conservar `id="new-project"`, apertura y retorno del foco. El CTA del empty state invoca `showForm.set(true)` como el existente y usa la misma condición disabled; no crea estado nuevo. Al abrir el formulario se retira visualmente el empty state redundante mediante la condición existente `showForm()`, manteniendo el opener original en DOM.

### D-14 Empty states

- **Propuesta:** panel compacto con borde neutro, símbolo 40px, título, párrafo de hasta 56ch y acción existente; padding 24px desktop/16px móvil, sin min-height. Projects: «Todavía no hay proyectos», explicación breve del primer paso y Nuevo proyecto. Otras ausencias usan la acción contextual existente si existe; nunca se inventa un botón.
- **Alternativa/motivo:** una ilustración hero o una card vacía enorme no ayuda al flujo. SVG propio aporta reconocimiento sin recurso externo. No mostrar empty ante loading/error.
- **Impacto:** FP-07/FP-13; NotFoundPage usa símbolo de documento no encontrado, título/explicación actuales y Volver a proyectos como enlace destacado. Vacío de Findings conserva su advertencia de que no demuestra seguridad.

### D-15 ProjectPage

- **Propuesta:** nombre/descripción y breadcrumb compacto; aviso de autorización local sin dominar la página; bloque de preparación con Target público y CTA existente próximo. Histórico como sección de filas con fecha/estado/enlace y Actualizar histórico secundario. URL completa ajustable y auth/hasCredentials en metadatos.
- **Alternativa/motivo:** un dashboard de métricas exigiría datos o llamadas nuevas; cards separadas gigantes fragmentan el siguiente paso. Mantener bloques independientes para errores parciales y agrupar solo lo visual.
- **Impacto:** FP-08; la CTA conserva exactamente las ramas Configurar/Ejecutar/Ver activo, reconciliación y disabled. No incorporar riesgo/score a filas del histórico.

### D-16 TargetPage

- **Propuesta:** página con breadcrumb, título y advertencia; formulario de hasta 960px, una sola superficie, fieldsets separados por borde y 24px. Conexión: nombre/timeout pueden compartir fila desde 768px, URL completa. Petición: constantes en línea informativa y JSON a ancho completo en fuente mono nativa. Respuesta: campo y ayuda próximos. Autenticación: estado público y controles condicionales con avisos completos. Cabeceras: pares nombre/valor con acción Quitar alineada y Añadir secundaria. Acciones al final.
- **Alternativa/motivo:** wizard/acordeones ocultan datos y cambian interacción; CSS visual en las secciones existentes consigue agrupación. En móvil todo a una columna. No mover controles fuera de su FormGroup/FormArray ni duplicarlos para responsive.
- **Impacto:** FP-09; labels, IDs, data-field, tabindex, aria-describedby, tipos, directivas y handlers intactos. No screenshots con credenciales; la presencia visual se revisa con campo vacío. FeedbackPanel conserva info/error; no ampliar su API para un success cosmético.

### D-17 ScanPage

- **Propuesta:** breadcrumb → título/estado/fechas → resumen disponible → aviso de discrepancia si existe → enlaces internos → categorías → Findings → estado técnico → evaluaciones → informe con limitaciones. Es el orden actual refinado, sin mover datos entre fuentes. Mientras activo, estado/progreso técnico son protagonistas y no aparece un resumen ficticio.
- **Alternativa/motivo:** tabs o paneles laterales ocultan incertidumbre y limitaciones. Mantener flujo vertical/anchors permite revisar todo, con estado técnico menos pesado tras el resultado consolidado.
- **Impacto:** FP-10; no modificar `terminal()`, carga de bloques, errores, relecturas, duración ni funciones de vista. Mantener IDs/fragments y no hacer header sticky.

### D-18 Resumen de Scan

- **Propuesta:** una superficie principal con tres zonas: score + `/100` + badge RiskLevel; Hallazgos; Resultados inconclusos. Desktop proporción aproximada 1.4/1/1; a menos de 768px se apilan en ese orden. Iconos 18/20px acompañan labels, sin gráficos. Versión/explicación heurística y severidades debajo; advertencias de cero e inconclusos siempre visibles dentro del bloque.
- **Alternativa/motivo:** gauge/donut sugiere medida calibrada y añade ruido; varias cards compiten. Un número con contexto conserva claridad y la semántica aprobada.
- **Impacto:** FP-06/FP-10; no convertir null/error/NOT_APPLICABLE en cero ni derivar riesgo del valor; un score cero mantiene peso tipográfico, sin check verde o mensaje «seguro».

### D-19 Findings

- **Propuesta:** sección sin card exterior anidada; cada article de Finding es una fila/panel con borde, summary flexible y badge, categoría/título, confianza secundaria y disclosure nativo. Detalle con tres subtítulos y párrafos ajustables; acento lateral de hasta 3px para severidad si ayuda. No alinear títulos extensos en una única línea.
- **Alternativa/motivo:** pintar todas las cards de rojo genera alarma constante; ocultar automáticamente los leves altera el listado. Details/summary ya resuelve interacción y teclado sin estado propio.
- **Impacto:** FP-11; conservar sort, IDs, textos y confianza, marker nativo y área clicable. Iconos decorativos no incorporan botones dentro de summary.

### D-20 Report presentation

- **Propuesta:** sección documental abierta con H3, lectura hasta 72ch para párrafos, dl ajustable y lista numerada de limitaciones. Summary continúa como referencia si coincide o resumen rotulado del informe según lógica actual. Trazabilidad mantiene details secundarios y referencias existentes; interpretaciones y nueve limitaciones no se colapsan.
- **Alternativa/motivo:** una segunda colección de Findings duplica contenido; un export/chart queda fuera de alcance. Separadores y tipografía diferencian documento de resumen operativo.
- **Impacto:** FP-12; todos los textos, orden, comparación y versiones quedan intactos. No añadir URL histórica ni reinterpretar errores.

### D-21 Loading, error, success y estados técnicos

| Estado | Presentación propuesta |
| --- | --- |
| Loading | Mensaje actual con indicador estático de actividad, sin skeleton/tiempo mínimo |
| Empty | Patrón D-14, tono neutro |
| Error de lectura/validación | Icono aviso y texto danger; borde/aviso local; reintento existente |
| Éxito confirmado | Icono check y label explícito ya existente; acento success local, sin afirmar conectividad |
| PENDING | Badge neutro y etiqueta Pendiente |
| RUNNING | Badge azul y etiqueta En ejecución; sin spinner ni porcentaje |
| COMPLETED | Badge verde y etiqueta Completado; solo estado técnico |
| FAILED | Badge/aviso danger, motivo público y aviso de datos incompletos conservados |
| AVAILABLE | Dato real presente; etiqueta de disponibilidad solo donde el contexto la necesite |
| PENDING de resultado | Nota pendiente y relectura manual existente; no nueva espera |
| UNAVAILABLE | Nota de indisponibilidad del bloque según mensaje actual; no inferir fallo de red |
| NOT_APPLICABLE | Nota histórica neutra del recurso correspondiente |
| INCONCLUSIVE / degradación LLM | Aviso warning con motivo público y semántica previa |

- **Propuesta:** reutilizar FeedbackPanel y mensajes; clases/estructura visual de cada rama aportan el tono donde el componente solo admite info/error. Ningún input/output nuevo. No reemplazar roles actuales ni convertir todos los contenedores en live regions.
- **Alternativa/motivo:** un error global elimina información útil; usar rojo para histórico comunica una avería inexistente.
- **Impacto:** FP-06/FP-13; el diseño no modifica triggers de anuncios, timers, requests ni ramas de renderizado contractuales.

### D-22 Responsive

| Ancho de QA | Composición |
| --- | --- |
| 1440/1280 | Main máximo 1200px, gutter 32px; resumen tres zonas, categorías cuatro columnas si caben |
| 1024 | Gutter 24px; resumen tres zonas; categorías dos columnas (continuidad del umbral actual 1050px) |
| 768 | Gutter 24px; categorías dos; pares de formulario solo donde labels/textos caben; header sin forzar una línea |
| 360 | Gutter 16px; todas las grids a una columna, resumen apilado, acciones apiladas si falta ancho y header envolvente |

- **Propuesta:** breakpoints locales orientativos 1050px (categorías), 768px (resumen/formulario/dl) y 550px (categorías una columna). `min-width:0`, `minmax(0,1fr)`, `overflow-wrap:anywhere` para datos largos. Wrapping de flex y acciones antes de provocar overflow. No `overflow-x:hidden` global como arreglo.
- **Alternativa/motivo:** solo escalar tamaños no resuelve composición; duplicar DOM móvil genera controles/foco inconsistentes. Mantener fuente 16px de inputs también en móvil.
- **Impacto:** FP-14; comprobar ambas direcciones de resize, texto ampliado al 200% y sin ocultar datos. Si 768px no admite pares de formulario, esos pares pasan a una columna mediante CSS, sin nuevo comportamiento.

### D-23 Accesibilidad

- **Propuesta:** mantener landmarks, un H1 por vista, labels/errores, skip-link, foco y anuncios actuales. Foco 3px visible; contraste normal 4.5:1 y grande/controles/indicadores esenciales 3:1; revisar también hover, focus, disabled y fondos de estado. Controles objetivo ≥44×44px, enlaces de navegación/acción con área adecuada sin inflar enlaces en párrafos. Nada comunica solo por color/icono.
- **Alternativa/motivo:** icon-only o texto tenue reduce comprensión; añadir aria indiscriminadamente duplica anuncios. No se declara auditoría WCAG completa; se verifica el alcance diseñado.
- **Impacto:** FP-05/FP-15; preservar el foco de apertura/cancelación y navegación, details nativos, orden DOM y mensajes. UI textual puede ampliarse sin recorte.

### D-24 Animación e iconografía

- **Propuesta de motion:** solo transiciones 120–160ms de color/background/border/box-shadow en hover/focus, sin transform, skeleton o loaders animados. `prefers-reduced-motion:reduce` las desactiva y elimina scroll suave; el scroll existente ya está condicionado a no-preference.
- **Propuesta de iconos:** SVG inline propios con viewBox `0 0 24 24`, `currentColor`, stroke 1.75, linecap/linejoin round y sin fill salvo indicador simple. Tamaños 18px en acciones/nav, 14px en badge y 40px en empty. Vocabulario corto: plus, carpeta, ajustes, play, escudo, aviso, interrogación, check, documento. Siempre texto acompañante y `aria-hidden="true" focusable="false"` decorativos. SVG estático de autoría del producto, nunca generado desde contenido API.
- **Reutilización aprobada:** un único componente standalone de presentación `Icon` en `shared/ui/icon.ts`, con nombre de icono cerrado y paths estáticos definidos una sola vez; los consumidores reutilizan su selector. Tamaño/trazo mediante CSS común, sin estado, HTTP, servicios, registro extensible, carga remota o librería de iconos. La selección del path es puramente visual; no aceptar HTML/SVG/URLs arbitrarios ni datos API como definiciones de icono. No crear un componente por icono.
- **Alternativa/motivo:** repetir SVG completo en cada template provoca divergencias; una librería o registro genérico añade complejidad innecesaria. El componente pequeño concentra las definiciones propias y conserva SVG inline en el DOM sin añadir arquitectura funcional; sin emoji ni terminal decorativa.
- **Impacto:** FP-05/FP-15; iconos en navegación, CTAs, riesgo, Findings, inconclusos/errores y vacíos refuerzan reconocimiento, no reemplazan contenido.

### D-25 Organización CSS y límite de edición

- **Propuesta:** `styles.css` contiene tokens y base semántica, tipografía, campos, botones y foco; `app.css` solo shell; `shared/ui/page.css` conserva patrones realmente comunes (heading/actions, panel, fields, grid, avisos, listas y dl). CSS específico de resumen, categorías, formulario y Findings queda scoped al componente actual: extraer sus styles de decorador a `.css` adyacente si se toca sustancialmente, sin extracción masiva ajena. Prohibidos selectores dependientes de profundidad/`nth-child`, `::ng-deep`, atributos style y `!important` salvo excepción documentada.
- **Alternativa/motivo:** todo global provoca fugas; repetir tokens/patrones por página produce divergencia. Evitar catálogo de utilidades o nombres de variante sin uso real.
- **Límite:** solo CSS/templates, metadatos de estilos e imports de presentación como RouterLinkActive/Icon, con la excepción acotada del componente de iconos puramente visual descrito en D-24. Si TS existente contiene template inline, editar ese template no autoriza cambiar la lógica de su clase. No modificar servicios, `*.models`, contracts, decoders, estado de negocio, validators, guards, `app.routes.ts`, API/config/entornos ni APIs públicas de los componentes existentes. `styles:` del decorador es CSS local existente, distinto de style inline en HTML.
- **Impacto:** FP-01/FP-02; revisar diff funcional y bindings antes de cerrar. Templates conservan condiciones, handlers y atributos contractuales salvo el caso de CTA vacío que reutiliza showForm según D-13.

### D-26 QA visual y verificación

- **Propuesta:** matriz de diez escenarios × cinco anchos, descrita en [tasks.md](tasks.md), con datos públicos sintéticos y revisión de teclado/contraste/longitudes. Validar y registrar las 50 combinaciones; conservar un conjunto representativo de capturas seguras de los estados/anchos principales, sin una captura obligatoria por combinación. Revisar antes/después del mismo caso/viewport para las seis superficies principales; conservar las comparativas representativas que permitan evaluar el acabado. Evidencia y resultados dentro del change, sin fixtures de producción ni nuevo harness/framework.
- **Alternativa/motivo:** screenshots de un único happy path no cubren score 0, INCONCLUSIVE, FAILED o históricos. Crear infraestructura E2E/visual regression excede el incremento; utilizar entorno previamente autorizado y mocks/fixtures de tests existentes. Una fixture puntual que falte solo se añade a pruebas existentes.
- **Impacto:** FP-16; todos los resultados inicialmente pendientes. No iniciar servidores, tests/build ni conectar servicios en esta planificación. Posteriormente ejecutar cada comprobación solo con solicitud expresa; strict de OpenSpec no sustituye QA visual ni suite funcional. Adaptar tests solo por markup/accesibilidad/estado visual y conservar cobertura funcional previa.

## Risks / Trade-offs

- [Densidad excesiva] → controles de 44px, base 16px, espacios por grupo y revisión a 360px/zoom 200%; compactar márgenes, no reducir legibilidad.
- [Color percibido como conclusión de seguridad] → NO_FINDINGS neutro, COMPLETED rotulado como estado técnico, advertencias literales y riesgo separado de incertidumbre.
- [Fugas de estilos al compartir page.css] → patrones mínimos, clases expresivas y layout específico scoped; inspeccionar las seis vistas tras tocar base compartida.
- [Cambios cosméticos alteran formularios/foco] → preservar IDs, estructura FormGroup/FormArray, directivas, bindings, handlers y orden DOM; revisar diff y recorrido de teclado.
- [Tokens iniciales no cubren todas las combinaciones reales] → revisar contraste por estado y corregir tonos dentro de la dirección aprobada; registrar cualquier cambio material de decisión para revisión.
- [QA requiere ejecución que aún no está autorizada] → tareas pendientes explícitas; no declarar acabado validado ni lanzar entorno sin petición.
- [Duplicación o abstracción excesiva de iconos] → componente visual único con vocabulario cerrado y paths propios centralizados, sin registro extensible ni librería externa.

## Migration Plan

No hay migración de datos, backend o despliegue. Cuando se solicite implementar, aplicar tokens/base e iconos reutilizables y shell, después proyectos/target y por último Scan/resultados; revisar el diff para preservar contratos; ejecutar únicamente comprobaciones solicitadas y registrar QA. Para corregir o retirar un acabado, preparar cambios inversos limitados a presentación propia y someterlos a revisión, conservando trabajo ajeno; no ejecutar resets, cambios de rama o despliegues como parte de este change.

## Open Questions

No hay decisiones visuales pendientes: D-01..D-26 y las tres aclaraciones están aprobadas. Los ajustes menores de wrapping/espaciado encontrados en QA pueden resolverse dentro de estas reglas; cualquier cambio de alcance o semántica requiere detener la modificación afectada y consultar. Implementación, suite y QA siguen pendientes de sus solicitudes de ejecución correspondientes.

## Out of MVP / Future Evolution

Dark mode, temas por usuario, catálogo de iconos/design system general, librerías de charts/UI, dashboard, nuevas rutas, exportaciones y automatización visual/E2E con dependencias nuevas quedan fuera. Este change tampoco cierra decisiones operativas, de autorización, de IA o de entrega TFM pendientes.
