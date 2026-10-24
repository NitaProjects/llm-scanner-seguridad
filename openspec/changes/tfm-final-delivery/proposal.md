## Why

El MVP y el despliegue académico están operativos, pero la entrega todavía carece de enlaces GitHub/slides/vídeo completos, entrega privada de credenciales acreditada y documentación final coherente. Este cambio prepara un cierre académico verificable, sin ampliar el producto ni condicionar la entrega al restore aislado expresamente pospuesto.

## What Changes

- Definir una matriz de requisitos oficiales, evidencia existente y pendientes, conservando la diferencia entre evidencia local, producción y comprobaciones administrativas.
- Planificar A–H: auditoría documental, README final, revisión GitHub/public-safe, acceso al deployment, slides, preparación oral fuera del repositorio, grabación/publicación y checklist/formulario final. El cierre técnico de 2026-10-24 congela código/docs/slides; no se crea guion versionado.
- Especificar la aceptación del paquete de entrega: documentación autosuficiente, repositorio accesible, URL real, presentación válida y vídeo con explicación propia y pantalla.
- Preparar una revisión pública de contenido, historial y material audiovisual sin leer ni mostrar secretos; antes de H se documentará únicamente «las credenciales se facilitarán por canal privado», con preparación técnica en D y entrega efectiva junto al formulario en H. Los secretos reales son bloqueantes; los identificadores operativos se minimizan cuando no aportan valor y solo bloquean ante un riesgo concreto documentado.
- Separar obligaciones de mejoras opcionales y pendientes técnicos no bloqueantes. No se declara completada ninguna ejecución futura.

## Capabilities

### New Capabilities

- `academic-delivery-acceptance`: contrato de aceptación del paquete académico solicitado expresamente para este change. Sujeto: documentación, evidencias y actuación del autor/reviewer; no es una capability del scanner ni un requisito de su dominio, API o interfaz.

La spec permite revisar esta entrega en el workflow solicitado. No convierte slides, vídeo o formulario en funcionalidades de producto, ni propone sincronizarlos como capacidades funcionales consolidadas. Se conserva la separación indicada en `docs/08-entrega-tfm/requisitos-oficiales.md`.

### Modified Capabilities

Ninguna. No se modifican contratos funcionales existentes.

## Impact

Ahora: exclusivamente los artefactos de `openspec/changes/tfm-final-delivery/`, incluido el scaffold del CLI.

Tras aprobación: `Readme.md` y los documentos directamente afectados de `docs/08-entrega-tfm/`; slides o enlace y evidencia mínima no secreta de aceptación. El diseño delimita cada documento y el orden de trabajo. No se cambia la transcripción de obligaciones oficiales para acomodar el producto. Después del cierre técnico solo se esperan la URL final del vídeo público/no listado, estados/checklist/tasks/evidence y formulario/entrega privada; 3.3 conserva su dependencia externa. Preparación y grabación fuera del repositorio, sin guion versionado.

Sin cambios en código, API, dependencias, configuración, infraestructura, dominio, puertos o adaptadores. Sin AWS, despliegue, builds, nuevas pruebas del evaluator, Scans, commit/push ni publicación durante esta planificación. Una incidencia que exija código o infraestructura deberá justificarse y autorizarse por separado.

Fuera de alcance: refactors, features, restore 6.3, PDF reporting, autenticación multiusuario, nuevas categorías OWASP, cambios visuales salvo defecto grave sujeto a revisión separada, benchmark académico formal, sincronización/archivo general de OpenSpec y renovación operativa del despliegue.
