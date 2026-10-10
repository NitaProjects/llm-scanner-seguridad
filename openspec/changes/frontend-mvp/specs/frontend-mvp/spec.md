## Purpose

Permitir utilizar visualmente el flujo defensivo completo del backend desde proyectos y targets hasta análisis, hallazgos, score e informe, preservando los contratos, la incertidumbre y la privacidad de sus resultados.

## Review Status

**READY FOR IMPLEMENTATION.** FE-01..FE-24, prioridad MUST/MVP, concretan D-01..D-30 aprobadas explícitamente por el usuario el 2026-10-10 con las cuatro aclaraciones incorporadas en FE-06, FE-08, FE-10, FE-14 y FE-16. Cada requisito incluye historia y dependencias. Esta petición es exclusivamente de planificación: no solicita implementar ni modifica los siete contratos backend aprobados.

## ADDED Requirements

### Requirement: FE-01 Navegación recuperable y alcance

**Historia:** Como usuario, quiero recorrer y recuperar el flujo del análisis desde una URL para poder continuar una demostración o consulta.
**Dependencias:** PM-04, SE-06; D-01/D-02.

La interfaz SHALL ofrecer `/projects`, `/projects/:projectId`, `/projects/:projectId/target` y `/projects/:projectId/scans/:scanId`. La raíz SHALL redirigir a `/projects`; una ruta desconocida SHALL mostrar una página comprensible con retorno a proyectos. Los identificadores y el backend SHALL permitir reconstruir cada vista tras refrescar o abrir un enlace directo, sin depender de navegación anterior. SHALL existir retorno del Scan al proyecto y de este al listado. No SHALL incorporar login, dashboard global, elección de pruebas ni operaciones backend nuevas. La publicación SHALL requerir soporte del servidor para fallback SPA y rutas API, sin modificar despliegue en este change.

#### Scenario: Enlace directo a un análisis
- **WHEN** se abre o refresca una URL de Scan sin estado previo en memoria
- **THEN** se recupera el recurso bajo el proyecto indicado y se muestra su estado y los bloques disponibles
- **AND** un Scan ajeno, inexistente o con identificadores inválidos produce el error público correspondiente, sin mostrar datos anteriores

#### Scenario: Ruta desconocida
- **WHEN** se abre una ruta no reconocida
- **THEN** se muestra «Página no encontrada» y un enlace a proyectos

### Requirement: FE-02 Listado y creación de proyectos

**Historia:** Como usuario, quiero ver, crear y abrir proyectos para organizar análisis autorizados.
**Dependencias:** PM-01..PM-07; D-07.

La interfaz SHALL consumir GET `/api/projects`, respetar su orden y mostrar nombre, descripción si existe y fecha útil, con acción abrir. SHALL distinguir carga, lista vacía y error; un error no SHALL aparentar lista vacía. «Nuevo proyecto» SHALL desplegar un formulario integrado en la misma página, con nombre y descripción opcional, acciones crear/cancelar y sin modal ni nueva ruta. SHALL comprobar contenido obligatorio, límites de 120/2000 puntos de código Unicode tras espacios exteriores y U+0000, sin alterar espacios interiores. SHALL enviar únicamente `name` y `description` a POST `/api/projects`. Tras 201 SHALL abrir el proyecto devuelto. Durante el envío SHALL impedir dobles envíos; un fallo ambiguo no SHALL desencadenar reintento automático de POST.

#### Scenario: Primera creación
- **WHEN** GET devuelve `[]` y el usuario crea un proyecto válido
- **THEN** el estado vacío ofrece la creación y, tras 201, se abre el proyecto con su identificador real

#### Scenario: Nombre con Unicode y errores
- **WHEN** se introduce un nombre en blanco, de 121 puntos de código o con U+0000
- **THEN** se informa junto al campo y no se envía
- **AND** un error de validación backend se asocia al campo sin exponer la entrada rechazada

#### Scenario: Creación con respuesta perdida
- **WHEN** falla la conexión sin poder confirmar el resultado de POST
- **THEN** se informa de que la creación no pudo confirmarse y se ofrece consultar el listado antes de volver a crear
- **AND** no se afirma que no existe ni se repite automáticamente la operación

### Requirement: FE-03 Centro del proyecto e histórico

**Historia:** Como usuario, quiero reconocer la preparación y los análisis de un proyecto para ejecutar o consultar el siguiente paso.
**Dependencias:** PM-04, TC-02, SE-02/SE-04/SE-06; D-07/D-10.

La página SHALL consultar Project, su target y su histórico de scans, mostrando nombre/descripción, estado de configuración y lista de scans en el orden backend. Cada Scan SHALL mostrar fecha, estado y enlace, sin obtener score/report de todos los históricos. Un 404 TARGET_NOT_CONFIGURED SHALL ser estado sin configuración; otros errores no SHALL convertirse en ausencia. La acción principal SHALL ser «Configurar target» cuando falta y «Ejecutar análisis» cuando está configurado y no se conoce un Scan activo. Si se conoce PENDING/RUNNING SHALL ofrecer «Ver análisis en curso». SHALL permitir actualizar el histórico manualmente, sin mantener polling de fondo fuera de ScanPage ni impedir consultar históricos por fallo del target actual.

#### Scenario: Proyecto preparado o sin target
- **WHEN** el proyecto existe con o sin target
- **THEN** se muestra su histórico y la acción principal apropiada, distinguiendo ausencia válida de carga/error

#### Scenario: Histórico vacío y error parcial
- **WHEN** no existen scans o falla únicamente la consulta del target
- **THEN** se muestra respectivamente el estado vacío o un error en ese bloque, conservando el proyecto y el histórico ya recuperados
- **AND** no se habilita ejecutar con preparación desconocida

### Requirement: FE-04 Formulario contractual del target

**Historia:** Como usuario, quiero configurar el endpoint que analizará el backend sin aprender una integración distinta.
**Dependencias:** TC-01..TC-04/TC-07..TC-10; D-08.

La configuración SHALL permitir consultar, crear y actualizar el único target mediante GET/POST/PUT `/api/projects/{projectId}/target`. SHALL admitir nombre opcional, URL, requestTemplate como texto JSON, headers ordinarios como pares nombre/valor, authType NONE/BEARER_TOKEN/API_KEY_HEADER, headerName cuando procede, credencial de escritura, responsePath y timeoutMs. POST, application/json y `{{PROMPT}}` SHALL mostrarse como constantes informativas no editables. El timeout inicial SHALL ser 10000 ms, con rango 1000..60000 y envío explícito de entero, preservando en edición el valor consultado. PUT SHALL enviar todos los campos configurables ordinarios; no SHALL actuar como PATCH ni upsert. Guardar SHALL comunicar configuración guardada, sin afirmar conectividad, autorización de red o evaluación exitosa.

#### Scenario: Alta y actualización
- **WHEN** GET indica TARGET_NOT_CONFIGURED o devuelve una configuración existente
- **THEN** se ofrece respectivamente creación mediante POST o edición mediante PUT con datos públicos precargados
- **AND** las constantes no pueden cambiarse y los valores opcionales no se pierden por omisión accidental

#### Scenario: Creación concurrente
- **WHEN** POST devuelve TARGET_ALREADY_CONFIGURED
- **THEN** se informa del conflicto y se ofrece cargar la configuración existente sin reenviar automáticamente como PUT

### Requirement: FE-05 Validación UX del target sin duplicar autoridad

**Historia:** Como usuario, quiero corregir errores frecuentes antes de enviar y comprender los rechazos del backend.
**Dependencias:** TC-05..TC-09/TC-12; D-08/D-19.

La interfaz SHALL comprobar presencia y límites básicos, URL absoluta HTTP/HTTPS sin userinfo/fragmento, JSON objeto, exactamente una aparición de `{{PROMPT}}` decodificada como valor completo y nunca como clave o fragmento, timeout entero, cabeceras con nombres válidos/no duplicados por caso, controles y separación de autenticación. SHALL contar los límites en puntos de código o bytes UTF-8 según el contrato. SHALL avisar de que headers, URL y plantilla no son lugares para secretos. responsePath SHALL ser obligatorio y mostrar ejemplos del subconjunto aprobado; el backend SHALL validar su gramática completa, claves JSON duplicadas, SSRF, orígenes autorizados y demás reglas avanzadas. La validación UX no SHALL contactar directamente al target ni resolver DNS, probar conexión, ofrecer bypass SSRF o sustituir errores backend.

#### Scenario: Plantilla inválida
- **WHEN** la plantilla no es un objeto JSON, contiene cero/dos tokens o lo sitúa como clave o dentro de otra cadena
- **THEN** se muestra el error del campo sin ejecutar la plantilla ni iniciar solicitudes

#### Scenario: Regla avanzada rechazada
- **WHEN** una entrada que supera la validación básica es rechazada por el backend por responsePath, URL, claves duplicadas o headers
- **THEN** se muestran los errores públicos asociados a campos y la configuración no se presenta como guardada

### Requirement: FE-06 Credenciales efímeras y conservación explícita

**Historia:** Como usuario, quiero conservar o reemplazar la credencial sin recuperarla ni divulgarla.
**Dependencias:** TC-09/TC-10, ADR-002; D-09/D-27.

La interfaz SHALL usar hasCredentials para mostrar «Credencial configurada» y un campo secreto siempre vacío al cargar, sin máscara que finja conocer el valor anterior. En edición autenticada SHALL distinguir «Conservar credencial» de «Reemplazar credencial». Conservar SHALL omitir secretValue, enviando authConfig `{}` para Bearer o `{headerName}` para API key, únicamente bajo las condiciones TC-10. Crear autenticado, cambiar authType, headerName exacto u origen normalizado de URL SHALL requerir credencial nueva; el backend SHALL seguir siendo autoridad de esa comparación. Un campo vacío no SHALL enviarse como null ni cadena vacía para conservar. Una credencial nueva SHALL enviarse exactamente, sin trim. NONE SHALL enviar authConfig null, avisar de que guardar elimina la credencial previa y limpiar cualquier entrada secreta.

El secreto SHALL permanecer solo transitoriamente en formulario/petición necesaria; SHALL limpiarse al enviar, también ante error, y al cambiar proyecto/tipo, cancelar o abandonar la vista. Un error SHALL requerir reintroducir una credencial nueva, conservando solo los datos ordinarios necesarios. No SHALL almacenarse en storage, URL, servicios globales, cachés, logs ni fixtures reales. No se promete borrado garantizado de memoria del navegador ni acceso a credenciales ya guardadas.

Las nueve transiciones entre NONE, BEARER_TOKEN y API_KEY_HEADER SHALL seguir TC-10: NONE→NONE sin secreto; NONE→tipo autenticado con secreto nuevo; tipo autenticado→NONE con authConfig null, sin secretValue y eliminación únicamente al confirmar PUT válido; Bearer↔API key con secreto nuevo y authConfig del destino. Permanecer en el mismo tipo autenticado SHALL permitir conservar solo si existe credencial y se mantienen origen y headerName exacto cuando aplica; en otro caso SHALL exigir reemplazo. La UI SHALL enviar authConfig null para NONE. Cambiar el selector o cancelar no SHALL modificar la credencial persistida. PUT inválido/fallido SHALL conservar configuración y credencial previas; ante respuesta perdida SHALL consultarse el DTO público antes de afirmar qué quedó guardado. Volver a elegir el tipo original antes de guardar SHALL evaluar conservación contra el DTO público original, nunca contra estados intermedios del formulario. Null/vacío explícitos de secretValue no SHALL significar conservar ni eliminar.

#### Scenario: Modificar plantilla conservando Bearer
- **WHEN** se edita un target Bearer conservando origen y tipo y se elige conservar
- **THEN** se envía authConfig vacío sin secretValue y la interfaz nunca solicita ni muestra el token existente

#### Scenario: Cambio sensible y eliminación
- **WHEN** se cambia origen, authType o nombre exacto de cabecera autenticada
- **THEN** se requiere secreto nuevo para el tipo autenticado
- **AND** elegir NONE informa de eliminación al guardar y no conserva el secreto introducido

#### Scenario: Error al guardar un secreto nuevo
- **WHEN** el envío termina con error de validación, red o backend
- **THEN** el campo secreto queda vacío, se informa de la necesidad de reintroducirlo y no se reenvía automáticamente

#### Scenario: Transiciones entre mecanismos autenticados
- **WHEN** un target Bearer cambia a API_KEY_HEADER o un target API key cambia a BEARER_TOKEN
- **THEN** no se ofrece conservar la credencial anterior y se exige secreto nuevo con authConfig del mecanismo de destino
- **AND** una omisión rechazada por backend no cambia configuración ni credencial persistidas

#### Scenario: Retirada de autenticación confirmada o cancelada
- **WHEN** un target autenticado se edita a NONE
- **THEN** se limpia el secreto introducido, se avisa de eliminación al guardar y se envía authConfig null sin secretValue
- **AND** solo PUT válido confirma hasCredentials false; cancelar o PUT fallido conserva el estado persistido anterior, y una respuesta perdida requiere consulta pública

### Requirement: FE-07 Inicio de Scan y reconciliación de admisión

**Historia:** Como usuario, quiero ejecutar el plan autorizado y acceder al análisis real creado.
**Dependencias:** SE-02..SE-04/SE-07/SE-09; D-10/D-19.

«Ejecutar análisis» SHALL enviar POST `/api/projects/{projectId}/scans` sin cuerpo: no SHALL enviar `{}`, `null` ni espacios. SHALL bloquear envíos duplicados locales mientras se resuelve. Tras 202 SHALL usar el id devuelto, abrir su ruta y consultar el estado durable inmediatamente; no SHALL prolongar PENDING artificialmente. SHALL recordar que solo deben analizarse sistemas propios/autorizados y que configurar no autoriza orígenes operativos. SCAN_ALREADY_ACTIVE SHALL ofrecer recuperar el histórico y abrir el activo confirmado. SCAN_DISPATCH_FAILED o INTERNAL_ERROR con Location contractual SHALL permitir consultar ese intento, sin afirmar su estado hasta GET. Ante respuesta perdida SHALL reconciliar el histórico y ofrecer consulta, sin repetir POST ni garantizar idempotencia.

#### Scenario: Finalización rápida
- **WHEN** POST devuelve PENDING pero el primer GET ya devuelve COMPLETED
- **THEN** se muestra el terminal y se cargan resultados sin animaciones o esperas que aparenten trabajo

#### Scenario: Rechazo con intento persistido
- **WHEN** POST devuelve SCAN_DISPATCH_FAILED con Location válida del mismo proyecto
- **THEN** se informa del rechazo y se ofrece abrir ese intento mediante la ruta propia y GET
- **AND** no se navega a un origen externo ni se relanza el análisis

### Requirement: FE-08 Seguimiento único y con limpieza

**Historia:** Como usuario, quiero observar actualizaciones sin solicitudes duplicadas ni errores interpretados como resultado.
**Dependencias:** SE-05/SE-06, STE-13, EE-13; D-04..D-06/D-20.

Al entrar en un Scan, la interfaz SHALL consultar su estado inmediatamente. Mientras el último estado confirmado sea PENDING/RUNNING SHALL consultar de nuevo tres segundos después de finalizar el ciclo anterior, sin solapar ciclos ni duplicar seguimiento por componente. Las lecturas de la API SHALL tener timeout de diez segundos. Observar COMPLETED/FAILED SHALL detener el seguimiento periódico y dar paso a una carga terminal única. Navegar, cambiar parámetros o destruir la vista SHALL cancelar solicitudes/timers anteriores e impedir aplicar respuestas tardías a otro Scan.

Un fallo de GET de estado por red, timeout o 5xx SHALL mantener el último dato marcado como desactualizado, con aviso de actualización fallida, y permitir hasta tres fallos consecutivos antes de pausar; un éxito SHALL reiniciar ese contador. Tras el tercer fallo SHALL detenerse el polling automático, mostrarse un error de consulta no terminal y ofrecerse «Reintentar». Esta acción SHALL realizar un GET inmediato de Scan, nunca POST: si tiene éxito SHALL resetear el contador y reactivar polling solo para PENDING/RUNNING; si devuelve COMPLETED/FAILED SHALL respetar ese terminal y cargar sus recursos sin polling posterior. Si el reintento manual falla SHALL mantenerse la pausa y la acción, sin retries automáticos infinitos ni reiniciar el contador como si hubiera éxito. Errores definitivos de ruta/recurso o respuesta incompatible SHALL detener la consulta y mostrar error. Fallos de bloques secundarios SHALL mostrarse en su bloque y no impedir leer el terminal de Scan.

Si falla el primer GET sin datos previos, SHALL aplicarse el mismo máximo de tres intentos separados por tres segundos, mostrando estado aún desconocido/error de consulta, sin asumir PENDING/RUNNING a partir de la ruta ni de un inicio anterior.

#### Scenario: Paso a terminal y abandono
- **WHEN** se observa COMPLETED/FAILED o se abandona/cambia la ruta
- **THEN** no se inicia otro ciclo periódico para ese Scan
- **AND** el terminal permite una carga final de datos, mientras la navegación cancela también esa carga

#### Scenario: Error temporal y pausa
- **WHEN** GET de estado falla una vez y después responde, o falla tres veces consecutivas
- **THEN** el primer caso conserva el estado, advierte y continúa; el segundo detiene el polling y muestra error no terminal con Reintentar
- **AND** ningún fallo de lectura modifica Scan a FAILED ni provoca POST

#### Scenario: Primera lectura sin respuesta
- **WHEN** se abre un enlace directo y el primer GET falla temporalmente
- **THEN** se muestra que el estado todavía no se ha podido consultar y se aplica el límite de tres fallos antes de pausar
- **AND** no se atribuye al Scan un estado técnico inventado

#### Scenario: Recuperación manual de un Scan activo
- **WHEN** se pulsa Reintentar después de la pausa y el GET inmediato devuelve PENDING/RUNNING
- **THEN** se reinicia el contador de errores y se reactiva un único polling con el intervalo acordado

#### Scenario: Recuperación manual de un Scan ya terminal
- **WHEN** Reintentar obtiene COMPLETED/FAILED
- **THEN** se muestra el terminal y se cargan sus recursos una vez sin reactivar polling ni enviar POST

#### Scenario: Reintento manual fallido
- **WHEN** falla también el GET inmediato de Reintentar
- **THEN** se mantiene el error no terminal, la pausa y la acción explícita, sin nuevos intentos automáticos

### Requirement: FE-09 Progreso honesto y técnico

**Historia:** Como usuario, quiero saber qué se ha confirmado sin confundir actividad con detección o porcentaje.
**Dependencias:** SE-01/SE-05, STE-03/STE-13, EE-10/EE-13; D-11.

La vista SHALL traducir PENDING como «Pendiente», RUNNING como «Análisis en curso», COMPLETED como «Completado» y FAILED como «Fallido». Durante actividad SHALL mostrar metadatos de ejecuciones y estados técnicos de evaluaciones disponibles, en orden contractual. SHALL poder mostrar «X de Y pruebas finalizadas técnicamente» usando filas terminales observadas y tamaño del plan recibido, y contador separado de evaluaciones finalizadas técnicamente. Ningún contador SHALL significar pruebas satisfactorias, vulnerabilidades, cobertura o porcentaje. Sin plan SHALL indicar que aún no hay plan disponible. SHALL NOT afirmar fases de cálculo/generación, estimaciones de tiempo restante ni avance basado en cuatro pruebas esperadas en vez de datos recibidos.

#### Scenario: Cuatro ejecuciones y evaluación pendiente
- **WHEN** las ejecuciones están COMPLETED y Scan sigue RUNNING con alguna evaluación no terminal
- **THEN** se muestran ambos conjuntos de datos confirmados y «Análisis en curso»
- **AND** no se declara el análisis completado ni una fase interna exacta

#### Scenario: Plan vacío
- **WHEN** executions devuelve `[]`
- **THEN** se muestra ausencia de plan disponible, sin interpretar cero pruebas satisfactorias ni ausencia de riesgo

### Requirement: FE-10 Carga de resultados con fuentes diferenciadas

**Historia:** Como usuario, quiero resultados consistentes y consultas necesarias sin mezclar informe, estado y datos provisionales.
**Dependencias:** EE-13, SC-12/SC-13, RP-02/RP-13; D-20.

Durante PENDING/RUNNING SHALL consultarse solo Scan, executions y evaluations para seguimiento; Findings completos, score y report SHALL cargarse al observar terminal. Cuando GET Scan devuelve COMPLETED SHALL detenerse inmediatamente su polling antes de cargar una vez `/evaluations`, `/findings`, `/score` y `/report`, con errores independientes; executions SHALL releerse una vez para cerrar la vista técnica. Esta misma carga única SHALL permitir consultar recursos conservados en FAILED. Las listas principales SHALL proceder de evaluations/findings, el riesgo y las categorías de score, y el contenido documental de report-v1. No SHALL condicionarse la lectura final de evaluations/findings a la disponibilidad del informe ni reemplazar silenciosamente sus fuentes al reintentar report. `/score` SHALL ser fuente del bloque de score; report.summary SHALL mantenerse como resumen del propio informe, sin recalcularlo ni sustituir silenciosamente un error de `/score`.

Para scans nuevos, backend confirma ScanScore, SecurityReport y Scan COMPLETED de forma atómica. La interfaz SHALL interpretar availability conforme al contrato y no SHALL iniciar polling independiente de score/report ni una espera artificial después de COMPLETED. NOT_APPLICABLE histórico SHALL ser comportamiento normal e independiente para cada bloque.

El frontend SHALL distinguir carga/error/ausencia contractual por bloque y permitir reintentar solo lecturas fallidas. No SHALL crear un informe alternativo, duplicar listas de Findings entre secciones ni combinar datos de distintos scans. Desacuerdo entre summary y score recibidos o PENDING de resultados después de un terminal SHALL mostrarse como respuesta incoherente que necesita revisión/relectura explícita, sin repararla ni reactivar polling ilimitado.

#### Scenario: Terminal moderno con informe
- **WHEN** Scan es COMPLETED y score/report están AVAILABLE
- **THEN** se detiene inmediatamente polling de Scan y se hace una carga final de evaluations/findings/score/report y executions
- **AND** el riesgo procede de score, las listas principales de sus endpoints y el contenido documental de report, sin polling adicional ni peticiones finales duplicadas

#### Scenario: Disponibilidad final sin espera artificial
- **WHEN** después de COMPLETED un resultado devuelve NOT_APPLICABLE histórico o un PENDING incompatible con el terminal
- **THEN** el primero se presenta como ausencia histórica normal y el segundo como incoherencia contractual con relectura explícita
- **AND** ninguno activa polling de score/report ni altera Scan

#### Scenario: Informe fallido de lectura
- **WHEN** Scan es terminal y falla GET report mientras score/findings/evaluations pueden consultarse
- **THEN** se muestran los datos recuperados como recursos del Scan y un error de carga del informe
- **AND** no se etiqueta ese error como UNAVAILABLE o NOT_APPLICABLE ni se genera un report local

### Requirement: FE-11 Composición principal de resultados

**Historia:** Como analista, quiero identificar riesgo, detecciones e incertidumbre al abrir el resultado.
**Dependencias:** SC-02/SC-05/SC-06, RP-04..RP-07; D-10/D-12.

El detalle terminal SHALL presentar arriba estado y fechas junto al score/riskLevel, findingCount e inconclusiveCount disponibles. SHALL mostrar severidades y categorías después, seguidas de Findings detallados, evaluaciones, alcance, interpretaciones y limitaciones del report. SHALL usar secciones verticales con enlaces internos; no SHALL ocultar el resumen o los inconclusos detrás de tabs. La duración de ejecución SHALL derivarse solo de startedAt y finishedAt cuando ambos existan; su ausencia SHALL mostrarse como desconocida/no iniciado, sin inventar duración cero. La configuración actual del target no SHALL presentarse como la que se analizó históricamente.

#### Scenario: Resultado con riesgo e inconclusos
- **WHEN** hay un score alto, Findings y evaluaciones INCONCLUSIVE
- **THEN** riesgo, cantidad de hallazgos e incertidumbre son visibles antes de abrir detalles

### Requirement: FE-12 Disponibilidad de score y cero real

**Historia:** Como usuario, quiero interpretar la puntuación sin confundir ausencia, seguridad y riesgo detectado.
**Dependencias:** SC-02/SC-05/SC-12; D-12/D-17.

La interfaz SHALL consumir `{availability,score}` de `/score`. AVAILABLE SHALL mostrar el entero como `N/100`, versión y contexto heurístico, sin símbolo porcentual de riesgo ni derivar riskLevel por umbrales. PENDING SHALL indicar pendiente, UNAVAILABLE ausencia por fallo del análisis y NOT_APPLICABLE funcionalidad no aplicable a ese histórico; todos SHALL tener score null. No SHALL convertir ausencia/error en cero ni usar comprobación truthy para globalScore. Para AVAILABLE con cero SHALL mostrar «No se detectaron hallazgos en las pruebas completadas. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades». SHALL mantener visible inconclusiveCount y advertencia cuando sea mayor que cero, incluso con score 0.

#### Scenario: Cero con cuatro inconclusos
- **WHEN** score está AVAILABLE con globalScore 0 e inconclusiveCount 4
- **THEN** aparece `0/100`, «Sin hallazgos detectados» y advertencia de cuatro resultados inconclusos
- **AND** no se afirma «seguro», «0 % riesgo» ni NOT_VULNERABLE para esos resultados

#### Scenario: Score ausente o fallo de lectura
- **WHEN** availability no es AVAILABLE o GET falla
- **THEN** se muestra respectivamente la ausencia contractual o un error de carga, sin puntuación numérica inventada

### Requirement: FE-13 Riesgo y severidad accesibles

**Historia:** Como usuario, quiero distinguir el nivel agregado de la gravedad de cada hallazgo sin depender del color.
**Dependencias:** SC-02, EE-04; D-13/D-24/D-26.

RiskLevel SHALL tener etiquetas NO_FINDINGS «Sin hallazgos detectados», LOW «Bajo», MEDIUM «Medio», HIGH «Alto» y CRITICAL «Crítico». Finding Severity SHALL tener LOW «Baja», MEDIUM «Media», HIGH «Alta», CRITICAL «Crítica». Cada representación SHALL incluir texto visible y estilo distintivo con contraste razonable. Ambos tipos SHALL permanecer separados; no SHALL inferirse uno del otro ni modificar enums del payload.

#### Scenario: Nivel y severidad altos
- **WHEN** riskLevel y una severity valen HIGH
- **THEN** se muestran «Riesgo alto» y «Severidad alta» con texto además de color
- **AND** CRITICAL sintético o histórico sigue siendo representable sin habilitar su asignación automática

### Requirement: FE-14 Findings completos y confianza complementaria

**Historia:** Como analista, quiero revisar todos los hallazgos sustentados y sus recomendaciones.
**Dependencias:** EE-04/EE-08/EE-13, RP-03/RP-04; D-14.

La interfaz SHALL mostrar todos los Findings de GET findings sin filtrar por confidence ni recortar colecciones. SHALL ordenar una copia para presentación por CRITICAL>HIGH>MEDIUM>LOW, categoría LLM01<LLM02<LLM08<LLM10 e id canónico minúsculo ascendente, sin mutar el snapshot report ni crear otra lista desde sus proyecciones. Cada hallazgo SHALL mostrar categoría, severity, title y «Confianza heurística: N %», con aclaración accesible de soporte de la conclusión, no probabilidad calibrada ni impacto. Description, evidenceSummary y recommendation SHALL estar accesibles en el detalle expandible del mismo hallazgo, sin llamada individual inexistente ni traducción o reescritura de esos textos. No SHALL exponer evidencia bruta ni internals del evaluator. Un listado vacío SHALL indicar ausencia de hallazgos registrados, sin concluir seguridad.

#### Scenario: Gravedad y confidence distintas
- **WHEN** hay un HIGH con confidence menor que un MEDIUM
- **THEN** HIGH aparece primero y ambos mantienen sus valores originales

#### Scenario: Detalle e idioma original
- **WHEN** se abre un Finding con textos en inglés y recomendación igual a la de otro Finding
- **THEN** se muestran literalmente sus cuatro textos, también su propia recomendación, sin traducción, deduplicación ni solicitud de evidencia

### Requirement: FE-15 Resultados inconclusos y degradación

**Historia:** Como usuario, quiero comprender las abstenciones sin interpretarlas como ausencia de vulnerabilidad.
**Dependencias:** EE-03/EE-04/EE-07/EE-13, SC-05, RP-06; D-15.

INCONCLUSIVE SHALL mostrarse como «Resultado inconcluso» diferenciado de «Vulnerable» y «No vulnerable en esta prueba». SHALL mantenerse en contador visible y fila de evaluación con motivo público cuando exista. Confidence 0 de INCONCLUSIVE SHALL explicarse como falta de soporte para concluir, sin insinuar que la incertidumbre es improbable. Un resultado con degraded o fallo LLM SHALL mostrar una indicación complementaria y código/motivo público traducido, manteniendo finalVerdict. Solo una evaluación técnicamente COMPLETED SHALL presentarse como conclusión semántica; FAILED SHALL mostrarse como fallo técnico, aunque su DTO incluya INCONCLUSIVE técnico. No SHALL reclasificar Scan a FAILED por un fallo LLM consolidado.

#### Scenario: Falta de contexto
- **WHEN** una evaluación COMPLETED tiene INCONCLUSIVE y CONTEXT_UNAVAILABLE
- **THEN** se muestra «Resultado inconcluso: no hay contexto de evaluación disponible» y no se mezcla con resultados no vulnerables

#### Scenario: Detección degradada y fallo global
- **WHEN** se recibe VULNERABLE con fallo LLM consolidado o una evaluación FAILED con verdict técnico INCONCLUSIVE
- **THEN** el primero conserva su hallazgo con aviso de degradación y el segundo se presenta como fallo técnico sin una conclusión semántica nueva

### Requirement: FE-16 Informe estructurado fiel

**Historia:** Como lector, quiero revisar el informe persistido y sus límites sin una reconstrucción del frontend.
**Dependencias:** RP-01..RP-07/RP-10/RP-13/RP-14, ADR-008; D-16.

La interfaz SHALL consumir `/report` y renderizar únicamente report-v1/es conforme a su contrato. La sección Informe SHALL priorizar scope, summary, interpretation, limitations y metadata pública aprobada (reportId, scanId, projectId, generatedAt, reportVersion, locale y referencias/versiones públicas contractuales como trazabilidad secundaria). Summary SHALL usar los valores del snapshot; scope SHALL mostrar scanCreatedAt/scanStartedAt y referencia targetConfigurationId/targetUpdatedAt, sin completar URL histórica desde target actual. Las proyecciones findings/evaluations del report no SHALL generar segundas listas ni detalles duplicados: SHALL poder aportar referencias de trazabilidad a las secciones principales sin cambiar su fuente o repetir narrativas. Si summary coincide con score SHALL referenciar el bloque existente; si score no carga SHALL poder mostrar el summary rotulado como resumen del informe, sin ocultar el error del score. Interpretation SHALL presentar sus cuatro textos y limitations SHALL mostrar siempre sus nueve entradas literales y ordenadas cuando existe informe, incluso sin Findings o inconclusos. No SHALL generar executiveSummaryText libre, traducir Findings, recalcular, regenerar, exportar ni introducir campos internos. Una versión incompatible SHALL mostrar error de compatibilidad sin usar el renderer v1 a ciegas.

#### Scenario: Informe sin Findings
- **WHEN** report-v1 está AVAILABLE con Findings vacío
- **THEN** se presenta su resumen y las nueve limitaciones completas, sin concluir seguridad

#### Scenario: Cambio del target después del análisis
- **WHEN** se abre un report cuyo target fue modificado posteriormente
- **THEN** se mantienen las referencias históricas originales y no se atribuye al análisis la URL actual

#### Scenario: Informe sin duplicación visual
- **WHEN** el Scan dispone de listas principales y report-v1 contiene proyecciones de los mismos Findings/evaluaciones
- **THEN** Informe presenta contenido documental y referencias a los bloques existentes sin una segunda lista ni narrativas repetidas
- **AND** se conserva intacto el contrato report-v1 y no se sustituye la fuente de las listas principales

### Requirement: FE-17 Históricos y disponibilidades independientes

**Historia:** Como usuario, quiero consultar análisis previos sin errores ficticios ni resultados retrospectivos.
**Dependencias:** SC-12, RP-13; D-17.

Score y report SHALL tratar su availability de forma independiente. NOT_APPLICABLE SHALL mostrar «Este análisis fue creado antes de esta funcionalidad» referido al bloque correspondiente. Un histórico con score AVAILABLE y report NOT_APPLICABLE SHALL mostrar el score y los recursos públicos restantes sin construir informe. Un COMPLETED no-op con ejecuciones vacías SHALL indicar que no hay pruebas registradas; no SHALL tratarse como cuatro pruebas satisfactorias. La UI no SHALL inferir antigüedad o marcadores internos por fecha, arrays vacíos ni estado FAILED, ni rellenar categorías/evaluaciones ausentes.

#### Scenario: Histórico V6 con score
- **WHEN** score está AVAILABLE y report NOT_APPLICABLE
- **THEN** se muestra la puntuación real y la explicación histórica solo para el informe

#### Scenario: Histórico no-op y FAILED previo
- **WHEN** executions es vacío y ambas disponibilidades son NOT_APPLICABLE, con Scan COMPLETED o FAILED
- **THEN** no se inventan pruebas, hallazgos, score cero o informe ni se presenta NOT_APPLICABLE como error

### Requirement: FE-18 Scan FAILED con datos conservados

**Historia:** Como usuario, quiero comprender un fallo técnico y revisar resultados confirmados anteriores al fallo.
**Dependencias:** SE-07/SE-08, STE-10/STE-13, EE-10/EE-13; D-18.

Un Scan FAILED consultado con 200 SHALL tener vista útil con estado, fechas, motivo cerrado traducido y datos técnicos/evaluaciones/Findings disponibles. SCAN_DISPATCH_FAILED SHALL indicar que no se pudo iniciar procesamiento; SCAN_PROCESSING_FAILED, que no se pudo completar; SCAN_INTERRUPTED, interrupción tras reinicio. No SHALL exponer excepciones ni fabricar score/informe parcial. SHALL aclarar que esos datos no constituyen un informe de análisis completado. SHALL permitir volver al proyecto para iniciar otro análisis mediante acción explícita, sin reapertura ni reintento automático del Scan fallido.

#### Scenario: Fallo después de un Finding
- **WHEN** FAILED conserva un Finding y otras pruebas fallidas o abortadas
- **THEN** el hallazgo se mantiene accesible y se advierte que el análisis no pudo completarse
- **AND** score/report ausentes no se sustituyen por cero ni informe local

### Requirement: FE-19 Estados de interfaz y errores seguros

**Historia:** Como usuario, quiero una respuesta comprensible ante carga, ausencia y fallos.
**Dependencias:** PM-07, TC-12, SE-09, RP-13; D-19.

Cada vista/bloque relevante SHALL distinguir loading, empty legítimo, éxito, validación, error backend y error inesperado, sin pantalla en blanco. SHALL consumir `{code,message,fieldErrors}` sanitizado y asociar solo campos contractuales. SHALL tratar ausencia válida de target y availabilities como estados de producto. Identificadores inválidos/recursos ausentes SHALL ofrecer retorno, conflictos SHALL ofrecer reconciliación y 503 SHALL informar de indisponibilidad sin relanzamiento. Red/status 0, timeout, HTML de proxy, códigos desconocidos o payload incompatible SHALL usar mensaje local seguro. No SHALL mostrar stack traces, SQL, cuerpos libres, HttpErrorResponse completo ni datos rechazados. Reintentar SHALL repetir solo GET salvo nueva acción explícita del usuario.

#### Scenario: Error no contractual
- **WHEN** una respuesta de error contiene HTML, SQL o una excepción en vez del DTO esperado
- **THEN** se muestra un mensaje genérico seguro y no se imprime ese contenido ni en UI ni en logs

#### Scenario: Error de un bloque
- **WHEN** falla score o histórico pero otros recursos se cargan correctamente
- **THEN** el bloque fallido ofrece reintentar la lectura y los demás permanecen visibles

### Requirement: FE-20 API configurable y consumo limitado

**Historia:** Como responsable del producto, quiero conectar la interfaz a la API del entorno sin cambiar contratos o divulgar datos.
**Dependencias:** OpenAPI vigente, FE-07/FE-10; D-20..D-23.

Las llamadas SHALL usar una base API centralizada por entorno, sin localhost distribuido en servicios ni elección de host mediante URL del usuario. Desarrollo SHALL permitir proxy local de `/api`; producción SHALL usar `/api` relativo como baseline de mismo origen. La configuración pública no SHALL contener secretos ni habilitar acceso directo a target, evaluador o base de datos. DTOs SHALL conservar nombres, nulabilidad, enums y distinción entre entrada de autenticación y respuesta segura. Respuestas no compatibles SHALL producir error seguro y no defaults de negocio. No SHALL añadirse endpoints ni modificar contratos o despliegue para facilitar el consumo.

#### Scenario: Entornos y escritura segura
- **WHEN** se cambia entre configuración de desarrollo y producción
- **THEN** todos los servicios usan la base centralizada y las credenciales del target solo se envían en su POST/PUT de configuración
- **AND** nunca se copian a Authorization de las consultas del scanner

### Requirement: FE-21 Idioma, jerarquía visual y adaptación

**Historia:** Como usuario, quiero una interfaz sobria y legible durante una demo y en pantallas habituales.
**Dependencias:** FE-11/FE-13; D-23..D-25.

La UI propia SHALL usar castellano y mappings explícitos de enums conservados en inglés. SHALL mantener jerarquía de títulos, cards de resumen, badges textuales y listas/tablas legibles con aspecto técnico sobrio. SHALL evitar terminales falsas, animaciones gratuitas o información de implementación en el flujo del usuario. SHALL funcionar a 1280, 1024 y 768 píxeles de ancho y conservar uso básico a 360, sin desbordamiento global, con campos/textos largos que se ajusten y acciones accesibles. No SHALL requerir nueva librería visual, tipografía remota ni i18n completo.

#### Scenario: Tablet y textos largos
- **WHEN** se abre un resultado o target con URL/textos extensos a 768 o 360 píxeles
- **THEN** cards/formulario se adaptan, texto se ajusta y los controles siguen siendo utilizables sin desplazamiento horizontal de toda la página

### Requirement: FE-22 Accesibilidad mínima

**Historia:** Como usuario de teclado o lector de pantalla, quiero completar el flujo y reconocer los estados.
**Dependencias:** FE-13/FE-21; D-26.

La interfaz SHALL usar labels asociados, nombres claros de botones, headings jerárquicos, foco visible, navegación con teclado y enlaces semánticos. SHALL asociar ayudas/errores al campo y anunciar validación, guardado y cambios relevantes de estado sin anunciar cada tick de polling. Al cambiar de página SHALL identificar el título y mover el foco de forma coherente; al desplegar creación SHALL enfocar el primer campo y al cancelar SHALL devolver foco al control de apertura. Riesgo, error e inconcluso SHALL incluir texto además de color. El contraste de texto normal SHALL alcanzar 4.5:1 y el de texto grande/controles 3:1 en los elementos diseñados, sin afirmar auditoría WCAG completa.

#### Scenario: Flujo con teclado
- **WHEN** se recorre listado, creación, target y Finding mediante teclado
- **THEN** se alcanzan acciones/campos, se identifican errores y se pueden abrir/cerrar detalles sin depender del ratón

### Requirement: FE-23 Texto no confiable y privacidad del navegador

**Historia:** Como responsable de seguridad, quiero que respuestas y hallazgos adversariales permanezcan como datos inertes.
**Dependencias:** TC-09..TC-12, EE-06, RP-14; D-09/D-27.

Todos los textos procedentes de API, target indirecto, nombres, descripciones, resúmenes y recomendaciones SHALL renderizarse como texto escapado. No SHALL ejecutarse HTML/Markdown, scripts, URLs, plantillas, herramientas ni instrucciones contenidas en ellos; no SHALL usarse bypass de sanitización o inserción de HTML no confiable. URL de target SHALL mostrarse como dato editable, sin preview automático ni petición desde el navegador. No SHALL guardarse credenciales/configuraciones/cuerpos/resultados en localStorage, sessionStorage, IndexedDB, caches propias o telemetría. Logs SHALL excluir cuerpos, formularios, textos y secretos; no SHALL añadirse service worker ni analítica externa en este MVP.

#### Scenario: Fixture adversarial
- **WHEN** description o evidenceSummary contiene marcado, URL javascript o instrucciones para cambiar el scanner
- **THEN** se representa literalmente como texto sin ejecución, navegación automática, modificación de estado o llamada externa

### Requirement: FE-24 Verificación aislada y demostración trazable

**Historia:** Como mantenedor, quiero verificar el flujo y sus casos críticos sin depender de servicios reales ni resultados ficticios de producción.
**Dependencias:** FE-01..FE-23; D-28..D-30.

La implementación SHALL disponer de tests de presentación, formularios/secretos, estados, servicios/DTOs, orquestación y seguimiento temporal con fixtures sintéticas y HTTP simulado. SHALL cubrir cero con N/I, cuatro availabilities independientes, FAILED/históricos, severidad/confidence, XSS, errores, inicio sin body, polling que actualiza/termina/limpia, rutas directas y flujo Project→Target→Scan→resultados. Tests no SHALL depender por defecto de backend, target, OpenAI ni red real. Fixtures SHALL existir solo en tests, sin fallback ficticio de producción. El recorrido manual de demo SHALL documentarse dentro del change como comprobación posterior; E2E real, grabación/vídeo y demo-vulnerable SHALL quedar fuera. La planificación no SHALL ejecutar compilaciones/tests frontend; su ejecución posterior requiere solicitud expresa.

#### Scenario: Flujos con HTTP simulado
- **WHEN** se ejecuten posteriormente los tests autorizados de los tres flujos
- **THEN** se verifican endpoints, estados y resultados con fixtures reproducibles y se rechazan solicitudes inesperadas
- **AND** no se necesita backend real ni se activan datos falsos en producción

#### Scenario: Demo completa posterior
- **WHEN** se recorra el frontend implementado en un entorno previamente autorizado
- **THEN** se podrá abrir proyecto, consultar target, ejecutar Scan, observar estado real y revisar Finding/score/inconclusos/limitaciones
- **AND** el recorrido no exige login ficticio, exportación, fase simulada o nueva funcionalidad backend
