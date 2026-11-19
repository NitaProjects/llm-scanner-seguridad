## Why

La interfaz funcional actual utiliza un tema claro, una cabecera horizontal y superficies poco diferenciadas. Modernizar su presentación permitirá distinguir mejor acciones, seguimiento técnico y resultados de seguridad sin cambiar los contratos ni los datos del producto.

## What Changes

- Tema oscuro principal azul-negro con tokens CSS semánticos, acentos violeta/azul, superficies diferenciadas y jerarquía tipográfica consistente.
- Shell con sidebar compacta en escritorio y navegación adaptada a pantallas pequeñas, conservando rutas y enlaces directos.
- Modernización de proyectos, creación inline, target REST/chat, histórico, lanzamiento/seguimiento de scans, score, categorías, Findings, evaluaciones e informe.
- Estados de carga, vacío, error, éxito y confirmación de eliminación coherentes; progreso indeterminado honesto y microinteracciones con reduced motion.
- Accesibilidad WCAG 2.2 AA aplicable y revisión responsive, manteniendo iconos SVG propios y CSS sin dependencias nuevas.
- Auditoría y decisiones implementables registradas en design.md; aceptación mediante pruebas Angular existentes y revisión visual posterior.

Fuera de alcance: backend/API/BD/AWS, scoring/evaluación/report-v1, dashboard global, métricas no disponibles, nuevas rutas funcionales, edición de proyectos inexistente, editores de bindings operativos, permisos, selector claro/oscuro, bibliotecas UI/animación, charts y visualizaciones ficticias.

## Capabilities

### New Capabilities

- `frontend-visual-experience`: sistema visual oscuro, navegación, presentación de estados/resultados, movimiento accesible y adaptación responsive sobre la funcionalidad vigente.

### Modified Capabilities

Ninguna. Se añaden requisitos de presentación compatibles con frontend-mvp y project-deletion; no se alteran sus contratos funcionales.

## Impact

Solo presentación Angular: shell, styles.css, shared/ui y componentes de projects/target/scans; pruebas de presentación y documentación frontend directamente afectada durante implementación. Se mantienen HttpClient, decoders, DTOs, rutas, validadores, guard, estado/polling y reconciliación de escrituras. Sin nuevas consultas para decoración ni cambios en dependencias/lockfiles.

El change tiene implementación local en el working tree y [evidencia](evidence/result.md) histórica y final separadas. El operador ha aceptado manualmente la versión FINAL, incluidos el modal de salida y la navegación/foco; la inspección estática cierra la revisión de foco sin cambios de contrato. Cierre documental: 18/18, listo para archivar cuando se solicite. Los 185 tests, build y contraste son históricos; las nuevas pruebas del modal no se han ejecutado ni se presentan como PASS. No se acredita despliegue ni archivado.
