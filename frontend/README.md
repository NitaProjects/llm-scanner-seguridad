# Frontend

Interfaz Angular 21 standalone para LLM Scanner Seguridad. Implementa [frontend-mvp](../openspec/changes/frontend-mvp/) y la presentación de [apply-risk-intelligence-visual-identity](../openspec/changes/apply-risk-intelligence-visual-identity/), sobre la modernización anterior, con RxJS, formularios reactivos tipados, HttpClient, Vitest y CSS propio. El backend determina estados, hallazgos, veredictos, score e informe.

## Navegación y componentes

| Ruta | Página | Responsabilidad |
| --- | --- | --- |
| `/projects` | ProjectsPage | Listado y creación inline con ProjectCreateForm. |
| `/projects/:projectId` | ProjectPage | Target actual, admisión de análisis e histórico con ProjectScanList. |
| `/projects/:projectId/target` | TargetPage | Alta/edición mediante TargetForm y protección de salida con cambios. |
| `/projects/:projectId/scans/:scanId` | ScanPage | Seguimiento, RiskSummary, CategorySummary, FindingList, EvaluationList, ScanProgress y ReportSections. |

La raíz redirige a proyectos. Las rutas desconocidas muestran NotFoundPage. Las cuatro páginas se cargan mediante `loadComponent`; los parámetros y nuevas lecturas permiten abrir enlaces directos o refrescar, sin navigation state obligatorio. El shell contiene marca, navegación, salto al contenido y gestión del foco.

Los enlaces a secciones y referencias del informe usan RouterLink con fragmentos y conservan la ruta actual con `<base href="/">`. Angular Router desplaza la vista al destino. El salto al contenido también enfoca el `<main tabindex="-1">`; cambiar solo el fragmento no vuelve a enfocar el título de página.

`src/app/core/api/` contiene configuración, transporte, decoders y errores seguros. `shared/` contiene tipos comunes, etiquetas y UI reutilizable. `features/projects`, `features/target` y `features/scans` separan modelos, servicios, páginas y componentes. Los componentes presentan inputs y emiten acciones; no llaman a HTTP. Las páginas poseen su estado local, y ScanDetailState se proporciona exclusivamente en ScanPage. No hay store, caché global ni persistencia del navegador.

## API y ejecución local

Los entornos `environment.ts` y `environment.development.ts` usan `apiBaseUrl: '/api'`, inyectado por API_BASE_URL. ProjectsApi, TargetApi y ScansApi consumen los endpoints de [OpenAPI](../docs/05-api/openapi.yaml); los DTOs manuales preservan enums y nulls, y los decoders rechazan formas/versiones incompatibles y descartan campos desconocidos. Las lecturas tienen timeout de 10 segundos.

Con las dependencias existentes y el backend preparado por separado:

```powershell
npm start
```

El proxy de desarrollo dirige `/api/**` a `http://localhost:8080`, puerto por defecto confirmado en `backend/src/main/resources/application.yml`. Si el backend local usa otro puerto, ajusta solo `proxy.conf.json`. Los servicios no contienen localhost. No hay llamadas del navegador al target ni al evaluador.

Producción usa `/api` en el mismo origen. El servidor de publicación debe enrutar `/api` al backend y devolver `index.html` para deep links del frontend. Este change no configura ni acredita hosting, CORS o despliegue.

## Datos, seguimiento y errores

ProjectPage confirma primero el proyecto y carga target/histórico independientemente. Un target ausente se distingue de un error de lectura. Solo se habilita ejecutar con configuración e histórico confirmados; un activo ofrece su enlace. POST Scan no tiene cuerpo y requiere 202. Los conflictos recargan histórico y una Location de error solo permite construir un enlace interno del mismo proyecto. Nunca se sigue una URL externa ni se reintenta automáticamente una escritura. Una respuesta perdida requiere consultar el listado o la configuración pública antes de otro envío.

En ProjectsPage, una creación ambigua bloquea nuevos POST hasta una lectura exitosa iniciada después de entrar en ese estado. Una respuesta tardía de una consulta anterior puede actualizar el listado, pero mantiene el bloqueo. La reconciliación nunca repite el POST automáticamente.

ScanDetailState realiza GET inmediato y, mientras está activo, consulta ejecuciones/evaluaciones en paralelo. Espera tres segundos desde el final del ciclo antes del siguiente, sin solapamiento. Tres errores consecutivos de estado pausan el seguimiento; Reintentar realiza GET inmediato, reinicia el contador solo tras éxito y reanuda únicamente si el estado sigue activo. Un error manual mantiene la pausa. 400/404 o incompatibilidad detienen automáticamente. Navegación, cambio de parámetros y destrucción cancelan HTTP y timers.

COMPLETED/FAILED detienen la repetición antes de una carga final única de ejecuciones, evaluaciones, hallazgos, score e informe. Cada recurso tiene loading/datos/error propios y relectura de bloque; score/report no tienen polling independiente. Los nuevos COMPLETED están confirmados atómicamente en backend. PENDING de un resultado terminal o discrepancia score-summary se señalan sin reparar datos ni volver a esperar automáticamente.

Los errores usan mensajes locales cerrados y fieldErrors contractuales asociados a campos. No se muestran cuerpos libres, valores rechazados, SQL ni stack traces. FAILED conserva motivo público y datos consultables, con aviso de análisis incompleto. NOT_APPLICABLE de cada bloque es un histórico normal; ausencia y fallo de lectura nunca se convierten en cero.

## Target y credenciales

El formulario valida límites Unicode/UTF-8, URL, objeto JSON y placeholder, timeout y cabeceras ordinarias antes de construir su objeto. La validación avanzada, claves JSON duplicadas, JSONPath, SSRF y autorización operativa siguen en backend. POST, application/json y el placeholder son constantes informativas. Guardar no prueba conexión ni autoriza tráfico.

El campo password se carga vacío. hasCredentials solo indica existencia. Conservar omite secretValue y requiere mismo tipo/origen y nombre exacto de cabecera cuando aplica; cambios sensibles o alta autenticada requieren reemplazo. Las nueve transiciones siguen TC-10 respecto del último DTO público confirmado. NONE envía authConfig null y solo un PUT válido confirma eliminación. No se recorta una credencial nueva. El secreto se limpia al enviar, ante error, al cancelar, cambiar tipo/proyecto o salir, incluso si se cancela el aviso de navegación. Una escritura ambigua bloquea otro envío hasta recargar metadatos públicos. La salida con cambios sin guardar se confirma mediante un modal integrado: Seguir editando (también Escape) o Salir y descartar. El guard espera la decisión, evita duplicar el diálogo y limpia el secreto incluso cuando se cancela la salida; el resto del borrador se conserva. No se usa storage, logs, URL, servicios globales ni Authorization del scanner para guardar/copiar secretos.

## Interpretación de resultados

El resumen prioriza puntuación, texto de riesgo, hallazgos e inconclusos, después categorías, detalle y contenido documental. RiskLevel y Severity tienen etiquetas castellanas distintas y no dependen solo del color. Cero significa que no se detectaron Findings en las pruebas completadas; no demuestra seguridad. La puntuación es heurística y no se calcula en el cliente. INCONCLUSIVE mantiene su contador y motivo público; no equivale a no vulnerable ni a fallo técnico global. La confianza se etiqueta «Confianza heurística», separada de severidad y probabilidad.

Los hallazgos se ordenan sobre copia por severidad, categoría e id y conservan sus textos, incluyendo recomendaciones repetidas. Details nativos abren descripción, resumen de evidencia y recomendación; no hay evidencia bruta. EvaluationList proyecta únicamente datos públicos necesarios y distingue fallo/degradación LLM del veredicto consolidado.

ReportSections consume exclusivamente report-v1/es: resumen propio o referencia al score coincidente, alcance histórico, cuatro interpretaciones, nueve limitaciones literales y trazabilidad secundaria. No repite narrativas/listas principales, no atribuye URL actual al pasado ni reconstruye un informe ausente. Todos los textos se interpolan como texto inerte; no hay innerHTML, Markdown ni bypass de sanitización.

## Sistema visual

El [diseño aprobado](../openspec/changes/apply-risk-intelligence-visual-identity/design.md) prioriza riesgo, hallazgos y evidencia, con profundidad técnica accesible y fuentes del sistema, sin framework CSS adicional. Risk Intelligence designa la dirección visual, no un texto de marca visible; el descriptor es «Análisis de seguridad LLM». [styles.css](src/styles.css) centraliza tokens semánticos, spacing, radios y movimiento. Conserva los alias `--text`/`--muted`/`--accent`; los textos sobre botones rellenos usan `--on-primary`/`--on-danger`.

Fondo carbón `#14191C`, superficies `#1D2529`/`#273237`, texto `#F0EFE8`/`#BAC4C2` y acento teal `#79CBC4`. Rojo, ámbar y verde quedan reservados para estados semánticos. Riesgo y severidad mantienen etiquetas diferenciadas; el score cero es neutral y COMPLETED no certifica seguridad. Controles con radio 6px y paneles 8px, bordes definidos y sin glow ni gradientes decorativos.

La sidebar mide 240px, o 224px entre 1024 y 1199px; por debajo se utiliza navegación en flujo, sin funcionalidades ocultas. Contexto y versión frontend proceden de recursos ya cargados y `package.json`. El contenido tiene máximo 1440px, lectura 72ch y gutters 24/32px en escritorio. Proyectos usa un único conteo del listado y un registro enmarcado con Abrir/Eliminar; no consulta agregaciones. El formulario del target agrupa conexión, contrato y autenticación y ocupa el mismo ancho útil que el aviso de autorización. En COMPLETED, Risk Overview precede a las bandas OWASP, Findings y su inspector; cuando cabe, bandas/listado e inspector ocupan dos columnas. RUNNING separa ejecución, evaluación y resultado sin score provisional. El informe conserva su snapshot.

`PageHeader`, `EmptyState`, `LoadingState` y `StatCard` son componentes de presentación sin HTTP. Reutilizan `Icon`, `StatusBadge` y `FeedbackPanel`; loading tiene etiqueta requerida y región ocupada, y los errores permanecen disponibles. El diálogo nativo de eliminación conserva Cancelar como foco inicial, Escape, bloqueo del envío y reconciliación de respuestas ambiguas.

[Icon](src/app/shared/ui/icon.ts) centraliza doce SVG propios, incluidos Abrir y Eliminar, en un vocabulario TypeScript cerrado mediante el input requerido `name`. Usa `currentColor` y tamaños según su función; se integra junto a texto, siempre decorativo con `aria-hidden="true"` y `focusable="false"`. No carga librerías ni recursos externos.

Botones, enlaces y controles mantienen foco visible, targets de 44px y errores asociados. Microinteracciones de 140ms, transiciones normales de 200ms y modal de 180ms. Solo estados activos recibidos y seguimiento vigente permiten actividad; cesa ante error, pausa o estado terminal. Selección y actividad son independientes. No hay porcentaje ficticio ni animación de score. `prefers-reduced-motion: reduce` elimina movimiento no esencial y smooth scroll, conservando feedback textual.

## Validación

```powershell
npm test -- --watch=false
npm run build
```

No existe script de lint. Vitest/TestBed, HttpTestingController y RouterTestingHarness verifican servicios/contratos, formularios, secretos, polling con timers virtuales, rutas y flujos, resultados, históricos, errores y XSS. `src/testing/fixtures.ts` contiene únicamente datos sintéticos; no se importa desde producción. No se requieren backend, OpenAI ni target reales. No se añaden dependencias ni un framework E2E.

La [evidencia actual](../openspec/changes/apply-risk-intelligence-visual-identity/evidence/result.md) identifica la revisión R2, los 232 tests sin omitidos, el build y las comprobaciones reales de navegador, separándolos de las capturas intermedias. La aceptación manual aún pendiente queda indicada en sus tareas; no se acredita una auditoría WCAG completa ni un despliegue. Para repetir QA, recorrer proyectos, creación, target REST/chat, estados del Scan, hallazgos, informe y diálogo en 1440/1280/1024px, con reflow inferior, teclado, contraste, zoom 200% y reduce/no-preference. Se utilizan exclusivamente fixtures sintéticas sin backend, targets ni proveedores reales.

El [recorrido local con la demo](../openspec/changes/e2e-integration/evidence/2026-10-16-005150/result.md) acredita integración de API e interfaz. El [recorrido posterior con hallazgos](../openspec/changes/real-evaluator-validation/evidence/2026-10-19-ui-real/result.md) conserva la apertura de un Finding y la consulta del informe. Estas evidencias se refieren a las revisiones probadas; no sustituyen la revisión formal de los cambios históricos ni la validación de versiones posteriores.
