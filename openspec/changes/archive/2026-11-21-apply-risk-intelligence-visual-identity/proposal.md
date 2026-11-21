## Why

La interfaz debe facilitar primero la lectura del riesgo, los problemas encontrados y sus evidencias. Risk Intelligence es la capa visual principal, conservando de AppSec Lab la trazabilidad test → evidencia → decisión → recomendación. La primera materialización resulta demasiado austera: esta revisión concreta shell, iconografía, acciones, superficies y densidad para alcanzar acabado de producto sin inventar navegación ni métricas.

## What Changes

- Prioridad desktop: composición verificable a 1440, 1280 y 1024px, con degradación responsive básica por debajo, sin experiencia móvil dedicada.
- Scan COMPLETED prioriza Risk Overview: score heurístico 0–100, riesgo, estado, duración/versión disponibles y conteos reales, con límites interpretativos siempre visibles.
- OWASP Risk Bands comparables para LLM01/02/08/10 con scores/conteos existentes y outcome procedente de evaluación, nunca inferido desde score.
- Findings protagonistas mediante índice por severidad y Finding Intelligence Panel: EVIDENCIA → DECISIÓN → RECOMENDACIÓN → REFERENCIAS, conservando test/revisión y resumen de evidencia explícito.
- RUNNING conserva cabecera compacta, Technical Test Rail e inspector con ejecución/evaluación separadas, sin riesgo ni score provisionales. Al cerrar cambia la jerarquía visual sin perder identidad, selección ni referencias.
- Projects como registro compacto con nombre, descripción, fecha y una única acción Abrir; conservación íntegra de Eliminar y creación.
- Dark carbón/grafito, texto cálido, acento teal/petróleo, tipografía editorial y paneles selectivos; no consola ni colección indiscriminada de cards. Adaptación mínima del resto de vistas consumidoras de tokens, sin rediseño funcional.
- Rework/reutilización selectiva de componentes actuales, manteniendo teclado, foco, contraste, reduced motion, modales, fuentes de datos y navegación.
- Shell completo de presentación: marca en dos niveles, sidebar de 224–240px según viewport, iconos, navegación contextual sobre rutas existentes y versión frontend real al pie, sin copiar v1.0.0 de la referencia ni crear ajustes globales.
- Projects incorpora un resumen visual del número real del listado, panel único de registro con iconos/columnas y botones Abrir/Eliminar visibles; no incorpora totales globales de scans/Findings.
- Sistema verificable de botones, paneles selectivos, tipografía y densidad; aceptación con medidas y checklist por región, no solo «tema oscuro y datos renderizados».

Fuera de alcance: código en esta fase, nuevos mockups, backend/API/BD, evaluación/scoring/report-v1, transporte/protocolos/workflows/RAG/tools, polling, validaciones, cambios de delete, tendencias/comparaciones históricas, nuevas métricas/datos, dependencias, gráficos temporales, diseño móvil específico, AWS/despliegue.

## Capabilities

### New Capabilities

- `appsec-lab-identity`: identificador conservado del contrato de presentación, cuyo contenido final es Risk Intelligence con trazabilidad técnica AppSec Lab. Incluye Risk Overview, OWASP Risk Bands, Finding Intelligence Panel y Technical Test Rail.

### Modified Capabilities

Ninguna capability consolidada se modifica. `frontend-visual-experience` aún está definida en `modernize-frontend-experience`, no en `openspec/specs/`; este incremento sustituye sus decisiones visuales en design.md y AL-01. Esta revisión sustituye también la primacía de AppSec Lab de la versión anterior de este mismo plan. Los contratos funcionales de frontend-mvp y project-deletion siguen vigentes.

## Impact

Solo frontend de presentación: styles.css, shell, ProjectsPage, ScanPage, componentes presentacionales de scans y shared/ui, sus pruebas y documentación vigente directamente afectada. Se conservan rutas/fragmentos, servicios, DTOs/decoders, ScanDetailState, cadencia de lecturas, controladores de escrituras y política de privacidad. Proyecciones de vista puras unen únicamente datos ya cargados, sin nuevas solicitudes ni almacenamiento de navegador.

El change se denomina `apply-risk-intelligence-visual-identity` tras el rename autorizado por el usuario para reflejar la dirección final. Se conservan la ruta de spec `appsec-lab-identity` y los IDs AL como identificadores documentales; no implican estética de consola. Es el mismo change, sin duplicarlo ni alterar su alcance. No se requiere otra ronda de mockups. La implementación y sus comprobaciones requieren una petición posterior; este trabajo solo actualiza planificación.

Las dos capturas aportadas muestran Projects y Scan con acabado elaborado; no permiten identificar inequívocamente una captura separada de la versión austera. El GAP de estado actual se fundamenta en el código inspeccionado y el problema descrito por el usuario; las imágenes orientan composición/acabado, nunca datos, nuevas rutas ni lógica. Existen cambios de implementación en el árbol de trabajo: se conservan intactos. Esta revisión solo modifica estos cuatro artefactos y añade tareas pendientes de corrección visual.
