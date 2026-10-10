## Context

Motivación y alcance: [proposal.md](proposal.md). Contrato: [FE-01..FE-24](specs/frontend-mvp/spec.md). Estado: **READY FOR IMPLEMENTATION**; no se ha implementado código.

### Baseline observado y fuentes

| Fuente inspeccionada | Hecho relevante para este diseño |
| --- | --- |
| `AGENTS.md`, `agent-roles/README.md`, `agent-roles/architecture-agent.md`, `frontend/AGENTS.md`, `openspec/config.yaml` | Diseño antes de código, castellano, standalone, contratos como autoridad, sin dependencias nuevas, red real ni comprobaciones frontend no solicitadas. |
| `frontend/package.json`, fragmentos de `package-lock.json`, `angular.json`, `tsconfig.json` | Core/router 21.2.22 resueltos; CLI/build 21.2.23; TS 5.9 estricto, RxJS 7.8, Vitest 4/jsdom ya existentes; builder `@angular/build:unit-test`. |
| `frontend/src/main.ts`, `index.html`, `app/app.ts`, `app.config.ts`, `app.routes.ts`, `app.html`, `app.css`, `app.spec.ts`, `styles.css`, README frontend | Bootstrap standalone y RouterOutlet; rutas vacías; bienvenida CLI con estilos embebidos; `lang=en`; prueba de bienvenida. No producto, environments, HttpClient ni design system/Gencat. Se reemplaza la bienvenida, no el scaffolding. |
| `openspec/changes/project-management/specs/project-management/spec.md` PM-01..07 | Cinco campos Project, nombre120/descripción2000 Unicode, listado completo ordenado; no edición ni borrado. |
| `openspec/changes/target-configuration/specs/target-configuration/spec.md` TC-01..12 | Target único, POST/GET/PUT, constantes, gramática y límites, hasCredentials, conservación de secreto condicionada a origen/tipo/header exacto. |
| `openspec/changes/scan-execution/specs/scan-execution/spec.md` SE-01..10 | POST sin body, siete campos, cuatro estados, Location de algunos errores, un activo por proyecto; no-op histórico, no idempotencia de inicio. |
| `openspec/changes/security-test-execution/specs/security-test-execution/spec.md` STE-01..14 | Cuatro pruebas fijas, metadatos sin evidencia, contadores derivados, autorización externa/SSRF y límites en backend. |
| `openspec/changes/evaluation-engine/specs/evaluation-engine/spec.md` EE-01..14 | EvaluationListing con perfil nullable, Findings públicos, tres verdicts y fallo LLM separado; FAILED técnico no equivale a incertidumbre semántica. |
| `openspec/changes/scoring-engine/specs/scoring-engine/spec.md` SC-01..13 | Fuente única de riesgo, cero real, RiskLevel separado, availability y categorías inmutables; no cálculo en cliente. |
| `openspec/changes/reporting/specs/reporting/spec.md` RP-01..14 | Snapshot report-v1/es, doce campos, summary literal, diecinueve campos de evaluación, quince de Finding, nueve limitaciones; orden propio de Findings. |
| `docs/05-api/openapi.yaml` (paths y schemas) y `target-contract.md` | Endpoints, DTOs exactos, nulabilidad, enums y errores. Scan no incluye porcentaje/fase ni marcadores de versiones. |
| `docs/00-contexto/alcance.md`, `docs/02-arquitectura/arquitectura-general.md`, `decisiones/decisiones-abiertas.md`, `decisiones/ADR-008-reporting-snapshot-inmutable.md` | Backend core actual aprobado; histórico de cada incremento respetado. Sin URL histórica recuperable del report; frontend es consumidor. |
| `docs/04-ia/estrategia-evaluacion.md`, `docs/06-base-de-datos/modelo-datos.md` | Confidence heurística, contexto sintético, OUTPUT_ONLY; datos internos no publicables; V6/V7 pueden diferir en aplicabilidad. |
| `docs/08-entrega-tfm/checklist-entrega.md`, `requisitos-oficiales.md` | Interfaz ayuda a demostración; vídeo/slides/despliegue/experimento siguen pendientes y no son funcionalidades. |

Los siete contratos se leen con sus extensiones posteriores: el no-op SE-10 es histórico; STE/EE/SC/RP amplían prospectivamente el procesamiento hasta score+report+COMPLETED atómicos. No se ha identificado contradicción contractual que requiera cambiar backend. Que textos históricos de aprobación digan «implementación futura» no invalida los registros posteriores ni el estado actual documentado.

## Goals / Non-Goals

**Goals:** separar presentación, orquestación de lecturas y adaptación HTTP; hacer explícita la propiedad del estado y las fuentes de cada sección; minimizar secretos, duplicaciones y mantenimiento, con componentes fáciles de evaluar y probar.

**Non-Goals:** construir un dominio frontend paralelo, replicar Clean Architecture backend en capas ceremoniales, adoptar un store global, diseñar autorización productiva o resolver decisiones operativas existentes. Las exclusiones funcionales de proposal se mantienen. La aprobación se registra aquí; esta actualización no crea una ADR adicional ni duplica decisiones.

## Decisions

Las treinta decisiones **D-01..D-30 están aprobadas explícitamente por el usuario el 2026-10-10**. Se mantiene el resto del diseño y se incorporan cuatro aclaraciones obligatorias, trazadas a continuación. FE-01..FE-24 y las tareas concretan esa aprobación sin necesitar contexto del chat. Cada bloque registra problema, decisión, alternativas, motivo, consecuencias e invariantes. READY FOR IMPLEMENTATION acredita planificación aprobada; esta petición no solicita implementar.

| Aclaración aprobada | Decisiones afectadas | Requisitos / tareas |
| --- | --- | --- |
| Pausa tras tres errores y recuperación explícita Reintentar | D-06 | FE-08; 6.2/6.3 |
| COMPLETED detiene polling y carga evaluations/findings/score/report sin espera adicional | D-06/D-20 | FE-10; 6.5/7.6 |
| Transiciones de autenticación y credenciales exactamente según TC-10 | D-09 | FE-06; 5.3/5.4 |
| Informe documental sin duplicar Findings/evaluaciones | D-14/D-16/D-20 | FE-14/FE-16; 7.3/7.5 |

Estado final: 30/30 decisiones aprobadas; ninguna decisión funcional pendiente ni contradicción backend identificada.

### D-01 Routing

- **Problema:** recuperar contexto mediante enlaces sin multiplicar pantallas.
- **Decisión:** `/projects`, `/projects/:projectId`, `/projects/:projectId/target`, `/projects/:projectId/scans/:scanId`; raíz redirigida y wildcard de no encontrada. Páginas con `loadComponent`, parámetros observables y enlaces atrás/breadcrumbs simples.
- **Alternativas/motivo:** ruta `/scans/:id` pierde el scope Project; todo en una ruta dificulta refresh; rutas de cada resultado no aportan valor para cuatro pruebas.
- **Consecuencias:** resolver recursos a partir de parámetros; no recibir target/Scan por navigation state obligatorio.
- **Invariantes:** UUID y pertenencia los valida backend; limpiar al cambiar params. Fallback SPA del host es precondición de publicación, no cambio de infraestructura aquí.

### D-02 Páginas principales

- **Problema:** sostener el flujo completo con navegación breve.
- **Decisión:** ProjectsPage, ProjectPage, TargetPage, ScanPage y vista técnica NotFoundPage; App conserva el shell simple de marca, enlace Proyectos y área principal.
- **Alternativas/motivo:** pantalla propia de creación obliga a más navegación; target dentro de ProjectPage mezcla formulario complejo y consultas; ReportPage duplica resultados.
- **Consecuencias:** cuatro páginas de negocio; Findings e informe son secciones de ScanPage.
- **Invariantes:** ninguna página extra de login, catálogo de tests, dashboard o exportación.

### D-03 Estructura y componentes

- **Problema:** evitar tanto componentes gigantes como fragmentación cosmética.
- **Decisión:** organización por features y pocos componentes de responsabilidad completa; árbol y contratos en la sección de estructura.
- **Alternativas/motivo:** directorios técnicos globales diluyen negocio; un componente por `div` añade coordinación sin reutilización.
- **Consecuencias:** páginas cargan/orquestan; componentes presentan datos mediante inputs y outputs tipados, sin HTTP propio.
- **Invariantes:** el frontend no replica puertos de persistencia, evaluadores o políticas backend.

### D-04 Propiedad del estado

- **Problema:** evitar estado persistente obsoleto y filtración de credenciales.
- **Decisión:** ProjectsPage/ProjectPage poseen sus lecturas; TargetPage posee formulario y metadatos públicos originales; ScanDetailState provisto por ScanPage posee un único estado de lecturas, polling y resultados. Solo transportes HTTP/config son singleton.
- **Alternativas/motivo:** NgRx/store global/caché compartida no se justifican por cuatro páginas; estado solo en navegación rompe deep links.
- **Consecuencias:** repetir GET al regresar es intencional; se pierden únicamente inputs no guardados tras aviso de salida.
- **Invariantes:** no storage; no secreto en ScanDetailState ni servicios singleton; ruta+backend recuperan estado crítico.

### D-05 RxJS y signals

- **Problema:** coordinar HTTP cancelable y una vista sencilla.
- **Decisión:** RxJS para rutas, solicitudes, secuencias, tiempos/cancelación; signals para estado de vista y `computed` para etiquetas/selecciones puramente visuales. `takeUntilDestroyed` y `switchMap` al cambiar params; una suscripción propietaria del seguimiento.
- **Alternativas/motivo:** solo signals complica cancelación/tiempo; RxJS en todos los detalles visuales añade ruido; librería de estado es innecesaria.
- **Consecuencias:** `LoadState<T>` distingue loading/loaded/error y datos previos desactualizados; availability sigue siendo dato de API distinto de ese estado local.
- **Invariantes:** no múltiples suscripciones a HTTP frío desde template; no efectos que recalculen score/verdict/severity.

### D-06 Polling

- **Problema:** asincronía sin eventos push y red variable.
- **Decisión:** GET inmediato de Scan; después ciclos separados tres segundos desde el fin del anterior, timeout HTTP de diez segundos. En activo, consultar executions/evaluations; terminal incluido en la secuencia detiene inmediatamente la repetición antes de una carga final. Tres errores consecutivos del GET de Scan detienen polling automático y muestran error de consulta no terminal con Reintentar; éxito reinicia contador. Si falla la primera lectura, aplicar la misma política mostrando estado desconocido, sin inferir PENDING/RUNNING.
- **Alternativas/motivo:** `interval` con suscripciones anidadas solapa; `switchMap` en ticks puede cancelar siempre una lectura lenta; SSE/WebSocket y backoff complejo exceden MVP.
- **Consecuencias:** ciclo tipo `expand`/timer encadenado, con outcomes de error tipados; cada petición secundaria captura error propio, para que no corte el estado principal. Un error de bloque no impide seguir leyendo Scan. Reintentar hace GET inmediato, bloquea dobles clicks durante esa lectura y, solo si tiene éxito, resetea el contador: PENDING/RUNNING reactiva un único ciclo; COMPLETED/FAILED inicia carga terminal sin timer posterior. Si falla, sigue pausado con la acción disponible.
- **Invariantes:** un ciclo por ScanPage, fin en COMPLETED/FAILED, cancelación al navegar/destruir/cambiar params, sin retry de POST ni retries automáticos infinitos. Sin timeout visual que convierta RUNNING en FAILED por transcurso local. No existe polling independiente de score/report.

### D-07 Project UX

- **Problema:** crear rápido y elegir siguiente acción.
- **Decisión:** lista con nombre/descripción/fecha y enlace; formulario integrado desplegable para Nuevo proyecto. ProjectPage con cabecera, card de target y tabla/lista de histórico completo, sin score por fila y con Actualizar.
- **Alternativas/motivo:** modal requiere foco/trap innecesarios; página dedicada ralentiza demo; cards con todas las métricas requieren consultas masivas.
- **Consecuencias:** al crear 201, navegar directamente al nuevo proyecto; contenido inicial vacío explica el siguiente paso.
- **Invariantes:** respetar orden backend, nombre repetido válido, no editar/borrar Project ni crear métricas globales; errors no son arrays vacíos.

### D-08 Target UX y validación

- **Problema:** formulario técnico útil, acotado y coherente.
- **Decisión:** página dedicada con bloques Conexión, Petición, Respuesta, Autenticación y headers opcionales. Typed reactive forms/FormArray de headers; constantes de solo lectura; ejemplo de plantilla como ayuda, sin resultados demo. Tiempo en ms sin conversiones ocultas.
- **Alternativas/motivo:** editor JSON completo y parser de gramática avanzada son demasiado tooling; textarea y validaciones básicas bastan.
- **Consecuencias:** validar lo enumerado en la matriz de formularios; autorizar origen/SSRF/JSONPath completo corresponde al backend. Guardado muestra éxito y enlace Volver al proyecto; se queda en edición con el DTO seguro retornado.
- **Invariantes:** distinguir POST de PUT desde GET; PUT es reemplazo de campos ordinarios; no test de conexión ni acceso directo a target. Un CanDeactivate limitado a TargetPage con formulario sucio evita pérdida accidental, sin persistir borrador ni añadir guards de autenticación. Confirmar salida no imprime formulario ni secreto.

### D-09 Secrets UX

- **Problema:** hasCredentials no permite leer/rellenar una credencial.
- **Decisión:** indicador público, input password vacío y selector Conservar/Reemplazar en edición. Conservación explícita con authConfig sin secretValue; alta/cambio sensible autenticado exige nuevo. Comparar `URL.origin` normalizado para ayuda (esquema/host/puerto efectivo) y headerName exacto; backend decide diferencias que su normalización detecte.
- **Alternativas/motivo:** máscara fija simula conocer longitud; siempre exigir secreto empeora edición; enviar vacío/null rompe TC-10.
- **Consecuencias:** al iniciar envío se crea solo el request necesario y se vacía el control sin guardar otra copia duradera; el request permanece únicamente mientras HTTP lo necesita. Error pide reintroducción. Cambio de proyecto/tipo, cancelar o abandonar limpia; reemplazo nuevo nunca hace trim. La matriz de nueve transiciones bajo formularios es normativa: NONE elimina solo mediante PUT válido; cambiar entre mecanismos autenticados exige secreto nuevo; invalidación/fallo preserva estado persistido previo y respuesta perdida exige recarga pública para confirmar.
- **Invariantes:** no credentials en DTO de respuesta/storage/logs/URL/Authorization del scanner; no promesa de borrado garantizado de Strings. `autocomplete` debe desalentar autofill del secreto; no botón para recuperar el valor anterior.

### D-10 Scan UX e inicio

- **Problema:** iniciar trabajo real y resolver admisión sin duplicados.
- **Decisión:** CTA en ProjectPage; si target falta, Configurar target; si activo conocido, Ver análisis en curso. Tras POST 202 usar body.id para ruta propia y GET inmediato. Aviso de uso autorizado visible en proyecto/target, sin login o flujo de aceptación legal ficticio.
- **Alternativas/motivo:** wizard de pruebas o confirmaciones repetidas añade alcance; doble POST ante fallo de red no es seguro por falta de idempotencia.
- **Consecuencias:** enviar HTTP sin body usando request explícita, nunca `{}`/`null` serializado. Bloquear botón durante submit. Conflicto recarga histórico; pérdida de respuesta informa de ambigüedad y permite buscar intento, sin afirmar cuál creó esa llamada.
- **Invariantes:** 202 no significa completado; Location de error solo se transforma en ruta frontend si coincide con base/ruta y projectId esperados, extrayendo UUID. Sin navegación/fetch de Location externo; GET confirma el estado.

### D-11 Scan progress

- **Problema:** API sin fase precisa ni porcentaje.
- **Decisión:** estado general más filas/counters confirmados de ejecuciones y evaluaciones. Mostrar «X de Y pruebas finalizadas técnicamente» con Y recibido, y contador de evaluaciones independiente; fallos no cuentan como éxitos.
- **Alternativas/motivo:** stepper preparando/evaluando/calculando/report puede mentir porque ejecución/evaluación se intercalan; porcentaje sobre cuatro probes omite evaluación/T3.
- **Consecuencias:** indicador de actividad discreto sin valor `progress`; vacío significa plan aún no disponible, no cero satisfechas.
- **Invariantes:** ninguna etiqueta de fase interna exacta, predicción o pausa artificial; timestamps de última actualización son del navegador y se etiquetan como tal.

### D-12 Score presentation y composición

- **Problema:** destacar el resultado y evitar falsa seguridad.
- **Decisión:** cabecera compacta con estado/fecha; primer bloque con score grande N/100, RiskLevel, hallazgos e inconclusos. Advertencia de cero/no calibración siempre ligada a su significado; aviso inconcluso visible cuando count>0. Severidades y cuatro cards de categoría después; secciones verticales con enlaces Hallazgos/Evaluaciones/Informe.
- **Alternativas/motivo:** gauge porcentual sugiere probabilidad; tabs esconden hallazgos o incertidumbre; media/suma de categorías sería lógica de negocio indebida.
- **Consecuencias:** `/score` alimenta bloque global y categorías, sin fórmulas/umbrales. Datos de report.summary se cotejan por igualdad si ambas fuentes están disponibles, no se calculan. Si `/score` falla, su card conserva error; el resumen propio del informe sigue accesible y claramente rotulado en la sección Informe.
- **Invariantes:** cero AVAILABLE se muestra; ausencia es null, no cero; duration solo finishedAt-startedAt conocido, rotulado «Duración de ejecución», sin falsa precisión de microsegundos (navegador muestra ms/segundos).

### D-13 RiskLevel presentation

- **Problema:** diferenciar riesgo agregado e impacto individual.
- **Decisión:** badges con texto Bajo/Medio/Alto/Crítico/Sin hallazgos detectados para riesgo y Baja/Media/Alta/Crítica para severity; tonos neutro/azul/ámbar/naranja/rojo, icono auxiliar opcional.
- **Alternativas/motivo:** color solo excluye usuarios; usar un enum común oculta NO_FINDINGS y diferencias semánticas.
- **Consecuencias:** mappings exhaustivos de tipos separados, sin clasificar por valor numérico en cliente.
- **Invariantes:** NO_FINDINGS no es «Seguro»; CRITICAL se soporta sin asignarlo el frontend.

### D-14 Findings UX y confidence

- **Problema:** revisar todo con prioridad clara y detalle legible.
- **Decisión:** filas/cards con severity, categoría, título y confianza complementaria; `<details>/<summary>` permite abrir descripción, evidenceSummary y recomendación, sin endpoint individual. La lista principal procede de GET findings y se ordena sobre copia según RP-04; el snapshot report no se muta ni crea una segunda lista.
- **Alternativas/motivo:** una tabla con cuatro textos largos satura; una ruta por Finding sin endpoint no mejora trazabilidad; ordenar por confidence desvirtúa impacto.
- **Consecuencias:** «Confianza heurística: 85 %» es conversión visual de 0.85; ayuda permanente/accesible «Soporte de la conclusión; no es una probabilidad calibrada ni una medida de impacto». Sin reponderar, filtrar o agrupar hallazgos por texto.
- **Invariantes:** conservar literalmente title/description/evidenceSummary/recommendation y recomendaciones repetidas; no traducir fuentes ni mostrar raw evidence o reasoning del evaluador.

### D-15 INCONCLUSIVE UX

- **Problema:** una abstención puede parecer resultado negativo o fallo técnico.
- **Decisión:** contador superior y filas «Resultado inconcluso» con motivo público; mapping NOT_VULNERABLE «No vulnerable en esta prueba». Confidence 0 de I se acompaña de explicación de soporte insuficiente. Badge de degradación separado si corresponde.
- **Alternativas/motivo:** agrupar I con tests aprobados o con fallos globales contradice EE/SC; mostrar 0 % sin contexto induce error.
- **Consecuencias:** usar finalVerdict solo en evaluaciones COMPLETED; status FAILED prevalece como error técnico y no aporta un contador semántico nuevo.
- **Invariantes:** backend determina todos los verdicts, contadores y severity. Un LLM fallido puede coexistir con VULNERABLE y Scan COMPLETED; nunca reinterpretarlo.

### D-16 Reporting UX

- **Problema:** presentar el snapshot sin inventar un segundo reporte.
- **Decisión:** ReportSections prioriza scope, summary, cuatro interpretation y nueve limitations, con metadata pública aprobada: reportId/scanId/projectId/generatedAt/reportVersion/locale y referencias/versiones contractuales en detalles secundarios cerrados por defecto. Findings/evaluaciones principales proceden de sus endpoints. Sus proyecciones del report solo aportan referencias de trazabilidad a esos bloques cuando sean útiles, sin segundas listas ni narrativas duplicadas y sin sustituir fuentes.
- **Alternativas/motivo:** ReportPage/DTO completo como JSON duplica navegación o expone información irrelevante; renderer HTML/Markdown añade riesgo y alcance.
- **Consecuencias:** report-v1/es se soporta explícitamente. No añadir executiveSummaryText. Si score carga, summary referencia ese bloque tras comprobar igualdad; si falla, se muestra summary del report rotulado como tal. Scope muestra ids/versiones históricas, no URL actual.
- **Invariantes:** cuatro textos de interpretación y nueve limitaciones literales completas siempre visibles en la sección de informe; labels propios en castellano, Finding en idioma original. No contenidos internos/proveedor/modelo/uso en report.

### D-17 Histórico NOT_APPLICABLE

- **Problema:** migraciones prospectivas permiten score sin report e históricos sin evaluación.
- **Decisión:** estado por recurso, con «Este análisis fue creado antes de esta funcionalidad»; las consultas públicas de Findings/evaluaciones son independientes de availability del report. EvaluationListing.evaluationProfile null se respeta; arrays vacíos no producen pruebas inventadas.
- **Alternativas/motivo:** inferir antigüedad por fecha o score0 da resultados falsos; backfill/frontend-report cambia historia.
- **Consecuencias:** cubrir combinaciones de disponibilidad, también FAILED histórico; renderizar solo categorías que realmente existen en score/report y ejecuciones registradas.
- **Invariantes:** estado Scan no revela marcadores internos; NOT_APPLICABLE no es error, UNAVAILABLE no es histórico.

### D-18 FAILED UX

- **Problema:** fallo puede conservar hallazgos útiles sin resumen completo.
- **Decisión:** encabezado Fallido, motivo público traducido y resultados confirmados bajo aviso de procesamiento incompleto; sin fabricar puntuación/report. Volver al proyecto permite un nuevo Scan explícito.
- **Alternativas/motivo:** página de error global escondería Findings; presentar un informe parcial implicaría un contrato inexistente.
- **Consecuencias:** leer executions/evaluations/findings y disponibilidad de ambos resultados incluso si falla antes del plan.
- **Invariantes:** FAILED consultable con 200; no «vulnerable por fallo», ni «seguro sin hallazgos», ni reabrir trabajo.

### D-19 Error handling

- **Problema:** distinguir errores públicos, conectividad e incompatibilidad sin filtrar contenido.
- **Decisión:** adaptador `ApiErrorMapper` puro y `ApiClientError` local; validar forma básica de error `{code,message,fieldErrors}`, seleccionar códigos/campos conocidos y representar mensajes como texto. Red/timeout/body no contractual/5xx inesperado tienen mensaje local seguro. Tabla de acciones más abajo.
- **Alternativas/motivo:** interceptor que reintenta toda petición puede duplicar writes; imprimir HttpErrorResponse expone formularios/respuestas.
- **Consecuencias:** error por bloque, reintento explícito de GET, errores de validación junto a campos y foco al primero; sin interceptores de logs ni credenciales.
- **Invariantes:** nunca leer body libre como texto de error; ni valores rechazados, excepciones o SQL. 500 de report/score no equivale a ausencia contractual; no convertir status 0 en fallo del Scan.

### D-20 API services y orquestación

- **Problema:** muchas fuentes pueden duplicar peticiones y resultados.
- **Decisión:** transportes ProjectsApi/TargetApi/ScansApi sin estado de negocio; ScanDetailState orquesta según matriz de carga. Scan determina ciclo; evaluations/findings sus listas principales; score card y categorías; report contenido documental. COMPLETED detiene inmediatamente polling y dispara una lectura final de evaluations/findings/score/report, más executions para cerrar progreso técnico.
- **Alternativas/motivo:** `forkJoin` sin captura individual oculta éxitos al fallar un GET. Usar report como sustituto de endpoints de listas ahorra dos lecturas, pero contradice la carga final expresamente aprobada y cambia fuentes según availability; se descarta. Descargar resultados en cada ciclo activo sí añade llamadas innecesarias.
- **Consecuencias:** capturar errores por recurso, sin caches globales; una sola carga terminal por entrada/transición, también en FAILED para conservar datos permitidos. Reintentar report solo actualiza ese bloque, sin recargar ni sustituir listas principales. El backend confirma ScanScore+SecurityReport+COMPLETED atómicamente para nuevos scans: no temporizador de espera ni polling independiente de resultados; NOT_APPLICABLE histórico es normal.
- **Invariantes:** no POST de generación/reevaluación, no resultados ficticios, no target actual para completar históricos; no reglas de riesgo/evaluación en servicios.

### D-21 TypeScript DTOs

- **Problema:** fidelidad contractual con volumen pequeño y tipos distintos de escritura/lectura.
- **Decisión:** interfaces/unions manuales readonly de los schemas usados; modelos separados por feature y shared types solo para enums realmente compartidos. Conservar UTC strings y null; disponibilidad como unión discriminada AVAILABLE/objeto versus otras/null. Response auth nunca usa tipo con secretValue.
- **Alternativas/motivo:** generación automática añade CLI/tooling de mantenimiento para pocos endpoints; `any` o tipos de entidad backend pierden nulls/proyección.
- **Consecuencias:** `HttpClient<unknown>` en frontera con narrow/decoders pequeños para campos usados, enums, forma de wrappers, ids/versiones y colecciones; tipado genérico solo no valida JSON. No duplicar validación relacional/fórmulas backend. Campos desconocidos no se propagan al modelo de vista ni se recorren genéricamente.
- **Invariantes:** no coerciones, null→0, enums desconocidos→LOW; error local seguro de contrato sin inventar códigos backend. ReportEvaluation y EvaluationMetadata no son intercambiables.

### D-22 API base URL/config

- **Problema:** scaffold no contiene environments/proxy y localhost en servicios impediría publicación.
- **Decisión:** `src/environments/environment.ts` producción y `environment.development.ts` mediante fileReplacement de la configuración development; ambos `apiBaseUrl: '/api'`. Token API_BASE_URL inyectado una vez. Proxy de desarrollo solo `/api/**` hacia backend local, con destino no sensible centralizado, verificando puerto del entorno al implementarlo. Sin runtime config remoto ni selección por query.
- **Alternativas/motivo:** base absoluta cross-origin exige CORS/exposición de Location no garantizados; URL por servicio diverge; runtime endpoint introduce carga adicional.
- **Consecuencias:** añadir únicamente fileReplacement y proxyConfig al frontend en futura implementación, sin tocar budgets/builders/scripts/lockfile ni despliegue. Producción requiere reverse proxy mismo origen y fallback SPA proporcionados por hosting posterior. No cambiar CORS/backend aquí.
- **Invariantes:** configuración pública sin tokens; los servicios añaden `/projects...` sin duplicar `/api`. Proxy de desarrollo no habilita orígenes de target ni es configuración de despliegue.

### D-23 UI library

- **Problema:** elegir componentes sin inventar un sistema visual existente.
- **Decisión:** HTML semántico y CSS propio; reutilizar dependencias Angular/forms/router ya disponibles. No se observó Gencat/core, Material, PrimeNG, Tailwind o Bootstrap.
- **Alternativas/motivo:** nueva biblioteca aporta componentes avanzados que el MVP no necesita y exige aprobación/dependencias.
- **Consecuencias:** inputs, botones, cards, badges y details nativos con clases pequeñas.
- **Invariantes:** no instalar/actualizar librerías ni fuentes/iconos remotos; no asumir componentes inexistentes.

### D-24 Styles/layout

- **Problema:** resultado profesional con esfuerzo razonable.
- **Decisión:** fondo claro neutro, texto azul grisáceo oscuro, acento azul sobrio, tarjetas blancas con bordes discretos, escala consistente de espaciado/tipografía y máximo aproximado 1200 px de contenido. CSS variables para superficies, texto, foco y estados; system font stack. App.css para shell, estilos de feature encapsulados; styles.css solo tokens/base.
- **Alternativas/motivo:** tema hacker o gráfico decorativo distrae; design system completo excede alcance.
- **Consecuencias:** jerarquía visual fuerte del bloque score y aviso ámbar de incertidumbre; listados legibles, sin duplicar cabecera por feature.
- **Invariantes:** significado no depende del color; no CDN, animación de progreso ficticia, nuevas assets de marca ni renderizadores externos.

### D-25 Responsive

- **Problema:** demo en portátil y consulta en tablet sin layout roto.
- **Decisión:** grid flexible con tarjetas en cuatro/dos/una columnas según espacio, formularios que apilan campos y tabla de scans adaptable a lista o scroll contenido local. Referencias QA 1280/1024/768/360 px.
- **Alternativas/motivo:** desktop fijo falla en tablet; app móvil diferenciada duplica trabajo.
- **Consecuencias:** `min-width:0`, wrap de URLs/ids/textos y acciones que saltan de línea; sin alturas rígidas para Findings.
- **Invariantes:** no overflow horizontal del documento; títulos, labels, acciones y avisos siguen accesibles en móvil básico.

### D-26 Accessibility

- **Problema:** teclado, lector y contraste básicos sin auditoría completa.
- **Decisión:** landmarks, h1 único/h2 por bloque, labels/aria-describedby, foco visible, links/botones reales, details nativo. Mensajes de estado `aria-live=polite` limitados a cambios relevantes; errores de submit anunciados con foco al primer campo/error. Foco al título tras navegación y restauración de creación inline.
- **Alternativas/motivo:** click handlers en div o color solo no son accesibles; modales propios requieren gestión extra.
- **Consecuencias:** QA manual teclado/contraste y assertions DOM simples; texto normal 4.5:1, grande y controles 3:1.
- **Invariantes:** no anunciar cada tick ni bloquear tabulación; score/inconclusos/estado tienen etiquetas visibles; no afirmar auditoría WCAG certificada.

### D-27 Security/XSS

- **Problema:** campos pueden derivar de outputs adversariales incluso si backend sanitiza.
- **Decisión:** interpolación/bindings de texto, nunca innerHTML, Markdown, bypassSecurityTrust ni HTML dinámico. Enlaces de navegación solo construidos por aplicación; URL de target se edita/muestra como texto. Inputs/API strings no se usan como instrucciones ni templates.
- **Alternativas/motivo:** renderer rich text aporta riesgo innecesario; sanitización backend no autoriza ejecutar contenido.
- **Consecuencias:** no storage/telemetría/service worker/console de formularios, bodies o errores completos; se revisa también el manejador de bootstrap y global listeners existentes para no volcar objetos HTTP sensibles añadidos por este flujo. No se pretende controlar devtools o plugins del navegador.
- **Invariantes:** secret solo en request de configuración necesario; toda red de app a API del scanner, ninguna a target/OpenAI/PostgreSQL; sin prometer redacción universal de secretos en texto libre.

### D-28 Testing

- **Problema:** validar flujo/asincronía/semántica con herramientas existentes.
- **Decisión:** Vitest + Angular TestBed, HttpTestingController y RouterTestingHarness; timers virtuales para seguimiento, sin framework E2E nuevo. Matriz detallada más abajo. Tests de estado y flujos, no un test por div.
- **Alternativas/motivo:** E2E real exige backend/demo/operación fuera del change; proveedor externo haría tests costosos e irreproducibles.
- **Consecuencias:** reemplazar expectativa de bienvenida y preparar tests de pages/componentes/servicios/estado/seguridad; comandos pendientes hasta petición explícita.
- **Invariantes:** no ejecutar aquí npm/ng/tests/builds; strict OpenSpec solicitado valida artefactos, no prueba frontend.

### D-29 Fixtures/mocking

- **Problema:** cobertura de casos críticos sin datos falsos en producto.
- **Decisión:** factories/fixtures sintéticas tipadas junto a tests o `frontend/src/testing/`, excluidas del runtime; HTTP testing en todos los flujos. No interceptores mock en producción ni bandera implícita de demo.
- **Alternativas/motivo:** JSON hardcodeado en services escondería fallos reales; backend real por defecto acopla entornos.
- **Consecuencias:** fixtures de V/N/I, degrado, cero, histórico V6, no-op, fallos y CRITICAL sintético; ninguna credencial real. Valores coherentes con constraints/relaciones, salvo casos deliberados de contrato inválido.
- **Invariantes:** `HttpTestingController.verify()` sin peticiones inesperadas; no integrar MSW/dependencia nueva ni ground truth académico en UI.

### D-30 Demo flow

- **Problema:** demostrar el producto con narrativa visual breve.
- **Decisión:** listado→proyecto→target guardado→volver→Ejecutar análisis→progreso real→resumen→abrir Finding→evaluación/inconclusos→scope/interpretaciones/limitaciones. Score visible arriba desde el resultado, sin pestañas que oculten el mensaje principal.
- **Alternativas/motivo:** métricas globales y exportación desplazan foco; retrasos simulados falsean la demo.
- **Consecuencias:** un Scan rápido puede pasar directo a terminal; otra consulta activa existente demuestra seguimiento sin pausas artificiales. La preparación operativa de target/escenario y permiso evaluador debe haberse hecho fuera del frontend.
- **Invariantes:** no fabricar Findings si falta contexto/manifiesto; modificar target cambia updatedAt y puede invalidar el escenario sintético. Mostrar inconcluso real y no prometer hallazgo. Demo-vulnerable, vídeo, benchmark y uso real OpenAI requieren bloques/autorizaciones separados.

### Estructura propuesta y ownership

```text
frontend/src/
  environments/                 environment.ts, environment.development.ts
  app/
    app.*                       shell de aplicación y RouterOutlet
    app.config.ts               provideHttpClient + provideRouter + API_BASE_URL
    app.routes.ts               cuatro rutas lazy y NotFoundPage
    core/api/                   api-config, api-error, api-error-mapper
    shared/models/              category, severity, risk-level, verdict, lifecycle
    shared/presentation/        mappings castellanos/orden Finding/formatos
    shared/ui/                  StatusBadge, FeedbackPanel, NotFoundPage
    features/projects/
      models/                   ProjectDto, CreateProjectRequest
      data-access/              ProjectsApi
      pages/                    ProjectsPage, ProjectPage
      components/               ProjectCreateForm, ProjectScanList
    features/target/
      models/                   TargetConfigurationDto, inputs auth discriminados
      data-access/              TargetApi
      pages/                    TargetPage
      components/               TargetForm (incluye FormArray headers/auth)
      forms/                    validators básicos, mapper de entrada
      guards/                   unsaved-target (CanDeactivate limitado)
    features/scans/
      models/                   ScanDto, executions, evaluations, findings, score, report
      data-access/              ScansApi
      state/                    ScanDetailState (provider de ScanPage)
      pages/                    ScanPage
      components/               ScanProgress, RiskSummary, CategorySummary,
                                FindingList, EvaluationList, ReportSections
  testing/                      fixtures compartidas solo de tests, si se necesitan
```

No crear directorios vacíos preventivos. Nombres de archivo siguiendo el scaffold Angular (`*.ts/html/css/spec.ts`, sin imponer sufijo component). TargetForm es un bloque de edición completo; solo separar un subcomponente de headers/auth si el tamaño real lo justifica durante implementación, manteniendo propiedad del formulario y secreto local. FindingList contiene details por fila; CategorySummary contiene cuatro cards, sin un componente por cifra.

| Propietario | Estado propio | Datos de entrada / salida |
| --- | --- | --- |
| App | navegación/foco global mínimo | RouterOutlet; no datos de negocio |
| ProjectsPage | lista, formulario abierto, submit/error | ProjectsApi; ProjectCreateForm emite input ordinario válido |
| ProjectPage | project, target público, lista scans, start pending | ProjectsApi/TargetApi/ScansApi; botones emiten acciones |
| TargetPage/TargetForm | DTO público original, formulario, modo conservar/reemplazar, campo efímero | TargetApi; secreto no sale hacia store/telemetría |
| ScanPage/ScanDetailState | route key, lectura principal, secundarios, última actualización, fallo consecutivo/pausa, carga terminal | ScansApi; components reciben proyecciones públicas sin HTTP |
| Shared UI | presentación, expansión/foco local cuando procede | inputs tipados; no subscriptions de datos |

### Wireflow textual

**ProjectsPage:** cabecera Proyectos + Nuevo proyecto → formulario inline al abrir → nombre/desc opcional + Crear/Cancelar → lista de proyectos o estado vacío con CTA. Carga con texto; error con Reintentar. Tras crear abrir ProjectPage, sin otra confirmación.

**ProjectPage:** breadcrumb Proyectos → nombre/descripción → card Target configurado/no configurado/error, con nombre/URL actual no enlazada y tipo de auth, nunca credenciales → CTA Configurar/Editar target y Ejecutar análisis/Ver análisis en curso → lista Histórico (fecha, estado, Abrir, Actualizar). Aviso breve de sistemas propios/autorizados. No score por scan de lista.

**TargetPage:** breadcrumb Proyecto → Configuración del target → conexión (nombre opcional/URL/timeout) → petición (POST y JSON fijos, textarea template con ayuda placeholder, headers no sensibles añadibles) → respuesta (responsePath + ejemplos) → autenticación (tipo, headerName cuando aplica, credencial configurada/mode/password) → resumen de errores + Guardar/Cancelar. Cancelar vuelve al proyecto; salir con cambios avisa y limpia al aceptar. Guardar muestra éxito, limpia secreto, actualiza original seguro y ofrece Volver al proyecto.

**ScanPage activo:** breadcrumb Proyecto → fecha + badge Pendiente/Análisis en curso → aviso de conexión si desactualizado → estado de ejecuciones/evaluaciones y contadores técnicos → texto «Los resultados consolidados se mostrarán al terminar». Sin card de score numérica, etapas ficticias ni Findings provisionales que parezcan informe final.

**ScanPage terminal:** cabecera estado/fecha/duración conocida → RiskSummary (N/100, texto de riesgo, hallazgos, inconclusos, severidades) → aviso visible de incertidumbre/cero → CategorySummary (cuatro categorías, datos literales, LLM10 solo salida) → FindingList con detalles → EvaluationList (tres verdicts/motivo/degradación) → ReportSections (resumen como referencia, scope, interpretación, nueve limitaciones y trazabilidad secundaria). Nav local de anclas; sin tabs. Si no hay report, mostrar su disponibilidad/error conservando los recursos independientes del Scan. Informe no repite Findings/evaluaciones.

### Orquestación de consultas

| Momento | Solicitudes | Fuente de cada bloque / política |
| --- | --- | --- |
| Entrada en ProjectsPage | GET projects | Lista completa en orden; sin consulta target por proyecto. |
| Entrada/actualización ProjectPage | GET project, y luego GET target + GET scans independientes | Primero confirma existencia; secundarios con error propio. Sin polling; Actualizar lee histórico y, si procede tras guardado/conflicto, target. |
| Entrada TargetPage | GET project + GET target, con errores independientes tras existencia | 404 TARGET_NOT_CONFIGURED abre alta; no usar datos de ProjectPage como requisito. |
| Entrada ScanPage | GET scan scoped inmediato | Confirma ruta y estado durable, no requiere GET target. Nombre del proyecto puede leerse una vez opcionalmente; el enlace atrás funciona por id si ese nombre falla. |
| Ciclo activo | GET scan; si activo, GET executions + GET evaluations en paralelo con captura individual | Únicamente progreso técnico. No findings/score/report. Esperar fin del ciclo y tres segundos antes de siguiente GET scan. |
| Primer terminal observado | Parar polling inmediatamente; GET evaluations + GET findings + GET score + GET report; GET executions final, cada uno con captura individual | Una carga terminal por entrada/transición, incluso si primer GET ya es terminal; no reutilizar datos activos como completos ni repetir la carga por cada render. |
| report AVAILABLE | Renderizar scope/summary/interpretation/limitations y metadata pública | Listas principales siguen en evaluations/findings; las proyecciones del report solo permiten referencias útiles de trazabilidad, sin duplicación visual ni cambio de fuente. |
| report no AVAILABLE o lectura fallida | No iniciar otras peticiones por ese resultado | Listas independientes ya solicitadas, también FAILED/históricos. No reconstruir report ni nueve limitaciones locales atribuidas a snapshot inexistente. Sin polling independiente ni espera de score/report. |
| Reintento manual de bloque terminal | Solo GET fallido o relectura explícita de respuesta incoherente | Actualizar el bloque, sin recargar listas por éxito de report ni reabrir polling terminal. |
| Reintentar seguimiento pausado | GET scan inmediato, nunca POST | Éxito resetea contador: activo reactiva ciclo único; terminal carga recursos una vez. Error mantiene pausa/acción, sin intentos automáticos. |
| Salida/cambio params | Cancelar stream, timers y HTTP; limpiar datos antes de nueva ruta | Sin respuestas atrasadas aplicadas ni caché global. |

Comparar igualdad de summary/score cuando ambos se reciben es detección de incompatibilidad, no fórmula. No calcular RiskLevel desde números ni sumas de categorías. Si difieren, marcar conflicto de datos y ofrecer relectura explícita, conservando procedencia; no escoger silenciosamente. PENDING de score/report tras un GET terminal se trata como respuesta incoherente que requiere relectura, sin dar por terminado un bloque todavía pendiente ni empezar otra espera automática. No inferir marcador faltante. Un backend sano moderno da AVAILABLE para ambos en COMPLETED por confirmación atómica; las combinaciones históricas se basan exclusivamente en availability, incluido NOT_APPLICABLE como estado normal.

### Servicios y tipos contractuales

| Servicio | Operaciones / endpoints |
| --- | --- |
| ProjectsApi | list GET `/projects`; get GET `/projects/{id}`; create POST `/projects` |
| TargetApi | get/create/update GET/POST/PUT `/projects/{projectId}/target` |
| ScansApi | start POST sin body `/projects/{projectId}/scans`; list GET misma ruta; get GET `/{scanId}`; executions/evaluations/findings/score/report GET subrutas scoped |

Base `/api` procede de API_BASE_URL. No endpoint individual Finding, catálogo, evidencia, reevaluación, generación/exportación o cancelación. Las lecturas respetan no-store backend; no añadir cache/shareReplay duradero. Compartir dentro de una carga local solo si evita duplicación con refCount y fin conocido. Cliente usa transporte HttpClient normal sin interceptor que copie auth del target. El inicio observa respuesta/cabeceras para 202/Location en error; Location se valida, no se sigue como URL arbitraria. Sin retries de escritura ni auth de usuario ficticia.

| Tipo frontend | Schema OpenAPI / precisión necesaria |
| --- | --- |
| ProjectDto / CreateProjectRequest | Project cinco campos y entrada de solo dos |
| TargetConfigurationDto / CreateTargetRequest / UpdateTargetRequest | Dieciséis campos públicos; entrada distinta con authConfig de escritura. NONE/null, Bearer secretValue sin headerName, API key headerName/secretValue. Secret requerido para creación y condicionado por modo de edición. |
| ScanDto / ScanStatus / ScanErrorReason | Exactamente siete campos, cuatro estados y tres motivos cerrados |
| TestExecutionMetadataDto / ExecutionErrorReason | Catorce campos, sin evidence/prompt/url/target refs en esta proyección |
| EvaluationListingDto / EvaluationMetadataDto | Perfil nullable + items; veintiséis campos, nullable finales, status técnico y llmStatus separados |
| FindingDto | Quince campos; verdict VULNERABLE, cuatro textos originales |
| ScoreResponse / ScanScoreDto / CategoryScoreDto / SeverityCounts / RiskLevel | Unión discriminada availability/score; once campos de score, cuatro entradas de ocho campos; severity y riesgo separados |
| ReportResponse / SecurityReportDto / ReportScopeDto / ReportEvaluationDto / ReportVersionsDto / ReportInterpretationDto / ReportLimitationDto | Wrapper propio y snapshot de doce campos, evaluación mínima de diecinueve, siete identificadores versions, cuatro interpretaciones y nueve limitaciones |
| ApiErrorDto / FieldErrorDto / ApiClientError | Forma pública conocida y fallo local de red/timeout/contrato; nunca se serializa HttpErrorResponse para UI |

Mantener DTOs y proyecciones de vista separados cuando sea necesario para excluir evaluatorProvider/evaluatorModel/decisionCode/reasoningSummary/recommendation del listado de evaluación. Que EE-13 publique esos datos no exige enseñarlos: EvaluationList usa solo categoría/test, status/finalVerdict, confidence/severity si aplican, outcomeReason, degraded/llmStatus/llmErrorReason/errorReason. Versions/ids públicos del report son trazabilidad secundaria, no contenido del prompt/schema. Ningún mapper construye una evaluación mínima del report desde el DTO amplio para simular report.

### Matriz de formularios y secretos

| Campo | UX básica | Autoridad avanzada |
| --- | --- | --- |
| Project name/description | Required/trim exterior para comprobación, Unicode120/2000, U+0000; descripción vacía→null | Backend confirma normalización/tipos/validez |
| Target name/url | Nombre opcional120, URL obligatoria hasta2048, HTTP/HTTPS absoluta, sin userinfo/fragmento/controles | Backend normaliza host/puerto, bloquea destinos locales/privados y exige allowlist operativa al ejecutar; UI no intenta comprobarla |
| requestTemplate | Texto sin trim automático, JSON.parse objeto, recorrer claves y strings decodificados para token único/posición; hasta32768 bytes UTF-8 | JSON.parse no detecta claves duplicadas: lo valida backend; no introducir parser nuevo ni afirmar que se validó exhaustivamente |
| responsePath | Required, límite1024 puntos de código, ayuda `$.response.text`, `$.choices[0].message.content`, `$["response-text"]` | Backend verifica32 segmentos, índices y gramática exacta; no ejecutar ruta en navegador |
| timeoutMs | Required, entero1000..60000; default10000 en alta, dato original en edición; enviar explícito | Backend confirma rango y tipo |
| headers | FormArray pares, <=20, token ASCII128, valores sin controles<=4096 bytes, suma<=16384 bytes, duplicados case-insensitive, nombres reservados/lista TC-08 y colisión con auth header | Backend confirma reglas completas. Mapeo a objeto solo después de detectar duplicados para no sobrescribirlos silenciosamente |
| authType/headerName | Enum tres tipos; API key requiere nombre token128 y evita reservados/colisión; comparación exacta para conservación | Backend decide validez y conservación TC-09/10 |
| secretValue nuevo | Required según modo; no blanco/control/surrogate aislado, <=8192 bytes; Bearer sin espacios; no trim; solo password efímero | Backend valida antes de cifrar y escribir; no reflejar contenido en error ni debug |

Para contar UTF-8, TextEncoder después de rechazar Unicode inválido donde exige el contrato; para longitud de puntos de código, iteración Unicode, no `string.length`/maxlength UTF-16 como único validador. No reproducir tablas SSRF/DNS ni toda la gramática JSONPath. En PUT se envían name, url, headers, template, responsePath, timeoutMs, authType y authConfig de forma explícita; constantes pueden omitirse para defaults fijos. No trasladar campos id/projectId/hasCredentials/fechas al request. Mostrar mensaje bajo secreto: «No se recupera la credencial guardada. Si reemplazas, introdúcela de nuevo después de un error».

#### Transiciones normativas TC-10

La referencia para editar es el DTO público original del último guardado confirmado, nunca un secreto recuperado ni un cambio intermedio del formulario. En alta autenticada siempre se exige secreto nuevo. En edición:

| Tipo persistido → tipo seleccionado | Entrada authConfig / UX | Efecto de PUT válido |
| --- | --- | --- |
| NONE → NONE | null, sin campo secreto | Sin credencial |
| NONE → BEARER_TOKEN | `{secretValue: nuevo}`, sin headerName | Crea credencial Bearer |
| NONE → API_KEY_HEADER | `{headerName, secretValue: nuevo}` | Crea credencial API key |
| BEARER_TOKEN → NONE | null, limpiar input; avisar eliminación al guardar | Elimina credencial; hasCredentials false |
| BEARER_TOKEN → BEARER_TOKEN | Conservar: `{}` solo con hasCredentials true y mismo origen; Reemplazar: `{secretValue: nuevo}` | Conserva o reemplaza atómicamente |
| BEARER_TOKEN → API_KEY_HEADER | `{headerName, secretValue: nuevo}`; Conservar no disponible | Reemplaza por credencial del mecanismo de destino |
| API_KEY_HEADER → NONE | null, limpiar input; avisar eliminación al guardar | Elimina credencial; hasCredentials false |
| API_KEY_HEADER → BEARER_TOKEN | `{secretValue: nuevo}`, sin headerName; Conservar no disponible | Reemplaza por credencial del mecanismo de destino |
| API_KEY_HEADER → API_KEY_HEADER | Conservar: `{headerName}` solo con hasCredentials true, mismo origen y headerName exacto; Reemplazar: `{headerName, secretValue: nuevo}` | Conserva o reemplaza atómicamente |

Mismo origen significa esquema, host normalizado y puerto efectivo; cambio de ruta con mismo origen permite conservar. Si cambia origen o headerName exacto, o falta credencial persistida, el destino autenticado exige nuevo secreto. Backend decide la normalización final. Seleccionar NONE o cambiar mecanismo solo limpia el secreto nuevo del formulario; no envía PUT ni elimina la credencial persistida anticipadamente. Volver al tipo original sin guardar reevalúa Conservar contra el DTO original. Null/vacío de secretValue nunca significan conservar/eliminar. Cancelar no modifica persistencia; PUT inválido/fallido conserva íntegra configuración/credencial anterior según TC-03/TC-10/TC-11. Una respuesta perdida deja resultado desconocido: consultar metadatos públicos antes de confirmar cambio o nuevo envío. Solo la respuesta válida actualiza el original y el indicador hasCredentials. No GET de secreto ni máscara de valor existente.

### Errores y mensajes públicos

| Código/situación | Acción de interfaz |
| --- | --- |
| VALIDATION_ERROR | Vincular fieldErrors conocidos; foco al primer campo inválido. Field desconocido→resumen seguro, no clave arbitraria. |
| INVALID_REQUEST | Error de envío/contrato; no reformatear/reintentar write automáticamente. |
| INVALID_PROJECT_ID / INVALID_SCAN_ID | Ruta inválida, retorno a proyectos/proyecto; parar polling. |
| PROJECT_NOT_FOUND / SCAN_NOT_FOUND | Recurso no encontrado, sin datos de otro scope; parar polling. |
| TARGET_NOT_CONFIGURED | Estado de alta en GET target; precondición no cumplida en inicio, enlace configurar. |
| TARGET_ALREADY_CONFIGURED | Conflicto, ofrecer cargar existente; no upsert silencioso. |
| SCAN_ALREADY_ACTIVE | Actualizar histórico y ofrecer abrir PENDING/RUNNING confirmado; si ya terminal, mostrar lista actual y dejar nueva acción manual. |
| SCAN_DISPATCH_FAILED | Explicar rechazo de inicio, usar Location validada para consulta del intento si disponible. |
| SCAN_EXECUTION_UNAVAILABLE | Infraestructura temporalmente no disponible; no crear ni repetir intento; nueva acción humana posterior. |
| INTERNAL_ERROR / HTTP5xx | Error de bloque; si es GET estado entra en política de tres fallos; no convertir en ausencia. INTERNAL_ERROR de inicio con Location permite consultar intento. |
| Red/status0/timeout/error no DTO | Mensaje local seguro, sin body de proxy/stack; reconciliar writes ambiguos mediante GET y limpiar secreto. |
| Enum/versión/forma inesperada | Error local de compatibilidad; detener polling si afecta Scan; ningún default de riesgo/verdict. |

Motivos públicos INCONCLUSIVE: CONTEXT_UNAVAILABLE→«No hay contexto de evaluación disponible»; CRITERION_NOT_APPLICABLE→«El criterio no es aplicable»; EXPORT_NOT_AUTHORIZED→«Envío al evaluador no autorizado»; EVIDENCE_NOT_EXPORTABLE→«Evidencia no habilitada para el evaluador»; EVIDENCE_TOO_LARGE→«Evidencia demasiado extensa»; EVIDENCE_INCOMPLETE→«Evidencia incompleta»; INSUFFICIENT_EVIDENCE→«Evidencia insuficiente para concluir»; SIGNAL_CONFLICT→«Señales contradictorias»; LLM_COMPONENT_UNAVAILABLE→«Componente evaluador no disponible». Motivo null no se inventa. Códigos de errores LLM/ejecución/evaluación públicos también tienen mapping seguro; no traducirlo a conclusiones nuevas. Unknown/null usa explicación genérica sin imprimir contenido libre.

### Matriz de tests propuesta

| Área | Casos y evidencia verificable |
| --- | --- |
| Mappings/presentación | Severity CRITICAL/HIGH/MEDIUM/LOW, RiskLevel con NO_FINDINGS, N/I separados; confidence0/0.85/1 como presentación; no umbrales/formulas; orden por severity/category/id con empate; literales Finding conservados. |
| Formularios | Project whitespace/Unicode120/121/2000/2001/U+0000; Target JSON/token/bytes/rango timeout/headers; errores backend fieldErrors; POST vs PUT; optional fields enviados explícitos. |
| Secrets | Matriz completa de nueve transiciones TC-10; GET sin campo secreto; conservar Bearer `{}`/API header exacto sin secretValue; cambio origen/tipo/case header exige nuevo; mismo origen otra ruta conserva; NONE elimina solo con PUT válido; cancelar/fallo preserva persistencia; respuesta perdida requiere consulta pública; volver al tipo original usa DTO original; trim prohibido; clear al enviar/error/cancelar/cambio params; no storage/log ni auth ajena. |
| Services/DTO | Todos métodos/rutas scoped, start sin body (inspección request), wrappers AVAILABLE/objeto vs restantes/null, timestamps/null intactos, enum/versión desconocida falla seguro; Location válida/externa; status0/HTML/5xx/errores públicos. |
| Polling | GET inmediato,3s tras ciclo, una solicitud pendiente/no solapamiento, terminal rápido, éxito resetea errores, tres fallos detienen automático con error no terminal; Reintentar GET inmediato: éxito activo resetea/reactiva, éxito terminal carga sin polling, fallo conserva pausa; error secundario no bloquea terminal, timeout10s, params/destroy cancelan HTTP/timer; nada se emite para scan previo. |
| Orquestación | Activo sin findings/score/report; terminal detiene primero y carga evaluations/findings/score/report más executions una vez con errores independientes, para cualquier availability del informe; no polling de score/report ni espera artificial; reintento solo de bloque sin sustituir listas; fuentes discordantes sin reparación; summary visible como report si score falla; Informe documental sin segundas listas/narrativas. |
| Resultados | 0+cuatro N versus0+cuatro I, mezcla V/N/I, V degradado no FAILED, evaluación FAILED no contador I inventado; cuatro availability por recurso y combinaciones V6/no-op/FAILED; findings completos y report nueve limitaciones inalteradas. |
| Flujos de componente | RouterTestingHarness Projects→create201→Project; Target sin configurar→POST→PUT conservación→error; Scan POST202→GET activo→terminal→Finding/report,409/503/ambigüedad. Refresh/deep link sin navigation state. |
| XSS/privacidad | Fixture adversarial de texto en Project/Finding/report se escapa y no crea nodos/script/enlaces externos; spies storage/console verifican ausencia de payload/secret; ningún request inesperado con HttpTestingController. |
| QA manual posterior | Teclado/foco/details/error announcements, contraste y breakpoints1280/1024/768/360, textos largos; recorrido demo con entorno autorizado. No prueba E2E automática completa aquí. |

Comandos que podrá solicitar el usuario al implementar: desde `frontend/`, `npm test -- --watch=false` y `npm run build`; mantener scripts/tooling existentes. No ejecutados durante planificación. No añadir tests de implementación sin comportamiento observable ni librerías de pruebas. Mock timers se restauran al acabar cada test para evitar contaminación.

## Risks / Trade-offs

- **[Frontend básico propio]** → cuatro pantallas y piezas nativas limitan esfuerzo; no crear design system entero. Revisión visual posterior centrada en ScanPage.
- **[Polling no es instantáneo]** → tres segundos entre ciclos, un stream, indicación de dato desactualizado y pausa tras fallos; no temporizadores ilimitados ni fases supuestas. Diez segundos de timeout es de consulta HTTP, no tiempo permitido al análisis.
- **[No hay instantánea global entre todos los GET]** → durante activo contadores son observaciones independientes, no fase exacta; terminal inmutable y report aporta snapshot consolidado. Detectar conflictos sin reparar.
- **[Validación parcial JSON]** → indicar autoridad backend; JSON.parse pierde claves duplicadas y no justifica afirmar validez total. Evitar parser/dependencia nueva.
- **[Escrituras no idempotentes/última edición gana]** → sin reintentos automáticos, reconciliar listado/target tras respuesta perdida; no simular versionado optimista inexistente. Un PUT ambiguo requiere recargar DTO público antes de nueva edición.
- **[Secretos transitorios existen en memoria/red necesaria]** → aislamiento de formulario, limpiar referencias controladas y no persistir; TLS del acceso al scanner será responsabilidad operativa, no prometer borrado garantizado del navegador.
- **[Target configurado no significa destino habilitado]** → aviso de autorización; no añadir bypass local a UI. Un cambio de target.updatedAt puede provocar CONTEXT_UNAVAILABLE hasta preparar escenario externo, que frontend no puede configurar.
- **[Histórico sin URL recuperable]** → mostrar referencias del plan y limitación original; URL actual solo en Project/Target, nunca inferida como histórica.
- **[Sin autenticación de usuarios]** → mantener alcance local autorizado documentado; no presentar scope Project como permiso multiusuario ni publicar el sistema como productivo.
- **[Publicación necesita `/api` y deep links]** → diseño mismo origen elimina necesidad de tocar CORS aquí. Configurar hosting/reverse proxy/fallback corresponde a despliegue futuro autorizado; no dar por publicada la aplicación.
- **[Planificación extensa no acredita producto completo]** → marcar únicamente la aprobación documental 1.1; las tareas futuras y checklist TFM siguen sin evidencia de implementación. No ejecutar demo real/OpenAI ni registrar resultados académicos en esta tarea.

## Migration Plan

No hay migración de datos ni cambio backend. Tras aprobación y solicitud de implementación, sustituir bienvenida manteniendo bootstrap/builders/versiones; añadir servicios/types/config frontend mínima; construir flujos por dependencias; preparar tests y actualizar documentación directamente afectada. Ejecutar comprobaciones frontend solo con autorización explícita. Reversión técnica de una futura entrega frontend no altera datos/API, pero cualquier operación Git o despliegue requiere la autorización correspondiente. Este plan no autoriza ejecutar esos pasos.

## Open Questions

No quedan decisiones funcionales pendientes: D-01..D-30 están aprobadas con las cuatro aclaraciones incorporadas y FE-01..FE-24 alineados. Estado **READY FOR IMPLEMENTATION**. No se cierra ni reabre ninguna decisión backend. Implementar requiere una petición posterior; no forma parte de esta actualización.

Datos operativos diferibles que no cambian requisitos/arquitectura: puerto local concreto del backend para proxy de desarrollo (comprobar configuración no sensible del entorno al implementarlo) y valores finales de tokens CSS dentro de contraste/layout ya definidos. El hosting mismo origen/fallback y el escenario sintético real de la demo son precondiciones de integración/despliegue posteriores, no tareas encubiertas de este change.

## Fuera del MVP / Evolución futura

E2E con demo-vulnerable/backend reales, generación OpenAPI de clientes si crece la API, biblioteca UI si se justifica después, más idiomas, autenticación/autorización de usuario, cache/offline, eventos push, cancelación, múltiples tests por categoría, gráficos históricos, exportación y publicación AWS. Requieren trabajo/aprobación separados, sin alterar silenciosamente scoring-v1, report-v1 o históricos. Una ADR frontend estable puede registrarse posteriormente si aporta valor, evitando duplicar este registro aprobado.
