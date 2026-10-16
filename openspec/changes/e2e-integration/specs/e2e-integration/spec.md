## Purpose

Definir un recorrido operacional E2E local reproducible del MVP existente, sin proveedor externo, con precondiciones explícitas, resultados observados y evidencia separada del oráculo académico.

## ADDED Requirements

### Requirement: E2E-01 Alcance y autorización del recorrido

**Historia:** Como responsable del MVP, quiero comprobar la integración real sin costes externos ni resultados fabricados.
**Prioridad:** MUST, primer E2E.
**Dependencias:** contratos aprobados de los siete changes backend, frontend-mvp, frontend-polish y demo-vulnerable.

El procedimiento SHALL recorrer Angular → scanner → demo → execution → evaluation → Findings → score → report → Angular en modo OFFLINE, sobre sistemas locales propios/autorizados. SHALL requerir aprobación del change y autorización operativa separada antes de arrancar servicios o ejecutar el recorrido. SHALL NOT activar proveedores externos, AWS, PDF, despliegues, benchmark formal, métricas precision/recall, cambios visuales o nuevos contratos de producto.

#### Scenario: Planificación sin operación
- **WHEN** se entregue este change para revisión
- **THEN** solo existen artefactos de planificación y ninguna tarea operativa se acredita como ejecutada

### Requirement: E2E-02 Topología compatible

**Historia:** Como operador, quiero utilizar los puertos y rutas existentes sin reconfigurar el producto.
**Prioridad:** MUST.
**Dependencias:** E2E-01; configuración local vigente.

El recorrido SHALL usar Angular `http://localhost:4200`, scanner `http://localhost:8080`, demo `http://127.0.0.1:8081` y PostgreSQL local 5432. SHALL conservar el proxy `/api/**` del frontend al scanner. SHALL comprobar la identidad/disponibilidad de cada servicio y bloquear si otro proceso o una configuración incompatible ocupa un puerto, sin terminar procesos ni elegir otro origen automáticamente.

#### Scenario: Puerto ocupado por servicio distinto
- **WHEN** el endpoint no coincide con el contrato esperado
- **THEN** preflight falla y no se inicia un Scan ni se modifica la topología

### Requirement: E2E-03 Separación de bases de datos

**Historia:** Como mantenedor, quiero preservar los datos locales y aislar los tests automáticos.
**Prioridad:** MUST.
**Dependencias:** E2E-02; configuración datasource y convenciones de tests existentes.

El E2E manual/local SHALL usar exclusivamente `llm_scanner_seguridad`. Tests automáticos con BD SHALL usar exclusivamente `llm_scanner_seguridad_test`. El procedimiento SHALL verificar nombre efectivo y endpoint local sin exponer credenciales. SHALL NOT borrar tablas, ejecutar limpieza masiva, reconstruir BD, migrar históricos ni usar la BD de desarrollo como destino de tests.

#### Scenario: Base de datos intercambiada
- **WHEN** el nombre efectivo no corresponde al tipo de recorrido
- **THEN** se bloquea la operación antes de crear datos o ejecutar tests

### Requirement: E2E-04 Preflight con fallo explícito

**Historia:** Como operador, quiero detectar una preparación inválida antes de enviar probes.
**Prioridad:** MUST.
**Dependencias:** E2E-02/E2E-03/E2E-06/E2E-07/E2E-08/E2E-18.

El procedimiento SHALL comprobar autorización local, componentes y versiones identificables, BD correcta/esquema preparado, perfil scanner `local`, excepción SSRF exacta, demo UP/VULNERABLE/version 1, API/proxy y ausencia de otro Scan activo del proyecto. Antes del Scan SHALL comprobar target persistido y binding exacto, contexto/corpus del perfil y exportación deshabilitada en entorno scanner y manifiesto. Configuración del proceso scanner no observable por API SHALL registrarse como declaración del operador; el contraste posterior con el snapshot del Scan SHALL ser diagnóstico complementario solo si existe autorización SQL de lectura, y en caso contrario sus campos internos SHALL registrarse como NOT_INSPECTED; SHALL NOT presentarse el entorno del script como prueba de configuración efectiva del proceso.

#### Scenario: Binding o exportación incorrectos
- **WHEN** hay tokens pendientes, fecha distinta, perfil distinto o exportación habilitada
- **THEN** no se autoriza el inicio y se registra qué precondición falló

### Requirement: E2E-05 Proyecto identificable con identidades reales

**Historia:** Como operador, quiero separar este recorrido de proyectos de análisis habituales.
**Prioridad:** MUST.
**Dependencias:** project-management; E2E-04.

El procedimiento SHALL crear por API/UI un Project `E2E Demo Vulnerable`, con descripción sintética que identifique la ejecución, y conservar el id real. Reutilización SHALL exigir id explícito, pertenencia verificada y ausencia de Scan activo. SHALL NOT seleccionar proyectos por nombre ambiguo, hardcodear UUID ni modificar proyectos ajenos.

#### Scenario: Nombres repetidos
- **WHEN** existen dos proyectos con el mismo nombre
- **THEN** se utiliza el id explícito comprobado o se crea uno nuevo, sin elegir el primero

### Requirement: E2E-06 Target y SSRF aprobados

**Historia:** Como operador, quiero invocar exclusivamente la demo autorizada usando el contrato vigente.
**Prioridad:** MUST.
**Dependencias:** target-configuration; demo-vulnerable DV-03; security-test-execution.

TargetConfiguration SHALL usar URL `http://127.0.0.1:8081/api/chat`, POST, contentType `application/json`, requestTemplate string `{"message":"{{PROMPT}}"}`, promptPlaceholder `{{PROMPT}}`, responsePath `$.response`, headers `{}`, authType `NONE`, authConfig omitido y timeoutMs `5000`. SSRF SHALL utilizar exclusivamente Spring profile `local`, `TARGET_LOCAL_DEMO_ENABLED=true` y `TARGET_LOCAL_DEMO_ORIGINS=http://127.0.0.1:8081`. SHALL NOT añadir bypass, wildcards, redirects, orígenes privados generales ni sustitución automática por localhost.

#### Scenario: Target guardado y releído
- **WHEN** la API confirma la configuración
- **THEN** se conserva id/updatedAt exactos y se contrastan todos los campos del contrato antes del binding

#### Scenario: Origen distinto
- **WHEN** cambia host o puerto del target respecto al origen declarado
- **THEN** preflight bloquea el recorrido y no relaja SSRF

### Requirement: E2E-07 Manifiestos y binding del estado real

**Historia:** Como operador, quiero ligar el contexto sintético al target que realmente analizará el scanner.
**Prioridad:** MUST.
**Dependencias:** demo-vulnerable DV-10; evaluation-engine EE-06; E2E-05/E2E-06.

El procedimiento SHALL reutilizar benchmark-v1, plantilla evaluation-vulnerable y esquema bindings-v1 de la demo. SHALL obtener `projectId`, `targetConfigurationId` y `targetUpdatedAt` de API sin redondear la fecha, mantener un único escenario y cuatro entries code/revision=1, y completar checkedHealth/autorización del operador. EVALUATION_SCENARIO_FILE SHALL señalar la copia local completada, con exportPolicy.enabled=false y provider OPENAI contractual. SHALL NOT importar benchmark/ground truth al evaluador, inventar ids, añadir provider NONE o mezclar perfiles. Cambio de target/perfil SHALL invalidar la preparación y exigir nuevo preflight/binding.

#### Scenario: Modificación del target después de preparar
- **WHEN** cambia updatedAt antes de iniciar el Scan
- **THEN** el recorrido exige regenerar el binding y no trata CONTEXT_UNAVAILABLE como aceptación del caso principal

### Requirement: E2E-08 Perfil estable de demo

**Historia:** Como revisor, quiero observar señales sintéticas reales de las cuatro categorías.
**Prioridad:** MUST para VULNERABLE; SAFE fuera de aceptación obligatoria.
**Dependencias:** demo-vulnerable; E2E-07.

El primer recorrido SHALL usar DEMO_PROFILE=VULNERABLE y verificar `/health` antes y después del Scan, sin reiniciar ni cambiar perfil durante él. Un control SAFE posterior SHALL ser opcional y separado, con su propia preparación y Scan; SHALL NOT sustituir el recorrido principal ni duplicarse como requisito de entrega.

#### Scenario: Cambio de perfil durante el recorrido
- **WHEN** health final no concuerda con el preflight o se reinicia la demo durante el Scan
- **THEN** la evidencia se marca inválida para ese caso y se requiere nueva ejecución autorizada

### Requirement: E2E-09 Flujo de Scan íntegro

**Historia:** Como usuario, quiero comprobar el análisis desde la UI hasta los resultados persistidos.
**Prioridad:** MUST.
**Dependencias:** scan-execution, security-test-execution, evaluation-engine, scoring-engine, reporting.

El recorrido SHALL crear Project, configurar Target, preparar binding, lanzar Scan desde Angular mediante POST sin body y capturar su id real/202/Location. SHALL consultar hasta terminal y leer executions, evaluations, findings, score y report mediante endpoints scoped existentes. SHALL exigir cuatro ejecuciones y cuatro evaluaciones COMPLETED correspondientes uno a uno, score/report AVAILABLE y Scan COMPLETED para aceptar el recorrido principal. SHALL NOT escribir estados en BD, repetir automáticamente POST ni utilizar históricos sin los marcadores actuales como sustituto.

#### Scenario: Fallo técnico real
- **WHEN** el Scan termina FAILED o faltan hijos/resultados requeridos
- **THEN** se conserva la evidencia y el recorrido se declara fallido, aunque el frontend lo represente correctamente

### Requirement: E2E-10 Polling acotado sin solapamiento

**Historia:** Como operador, quiero esperar el estado real con límites y sin duplicar solicitudes.
**Prioridad:** MUST.
**Dependencias:** frontend-mvp FE-08/FE-10 y D-06; E2E-09.

El seguimiento operacional SHALL tener plazo global de 360 s desde inicio del intento de Scan, solicitudes con timeout máximo 10 s y ciclos separados 3 s desde el fin del anterior, acotados por el tiempo restante. SHALL detenerse en COMPLETED/FAILED o tras tres errores consecutivos de GET, conservar el último estado confirmado y emitir fallo de seguimiento al agotar el plazo. SHALL NOT solapar ciclos del mismo observador, ejecutar sleeps largos, repetir POST ni convertir timeout local en Scan FAILED. La UI vigente SHALL conservar su semántica de cancelación al navegar y carga terminal única; el límite operacional SHALL NOT introducir un nuevo timeout de producto.

#### Scenario: Límite operacional agotado
- **WHEN** transcurren 360 s sin terminal confirmado
- **THEN** el recopilador termina con error explícito y el estado del servidor permanece sin alteración

#### Scenario: Terminal recibido
- **WHEN** GET confirma COMPLETED
- **THEN** cesa polling, se leen los cinco bloques finales y no se espera artificialmente a score/report

### Requirement: E2E-11 Resultado offline conforme al engine

**Historia:** Como revisor, quiero validar el contrato sin exigir un veredicto vulnerable artificial.
**Prioridad:** MUST.
**Dependencias:** evaluation-engine EE-02..EE-07; demo-vulnerable DV-14; E2E-07.

Con binding/contexto válidos y exportación deshabilitada, el recorrido SHALL contrastar por categoría TestExecution COMPLETED/HTTP 200, EvaluationResult COMPLETED, llmStatus SKIPPED, llmErrorReason null, intento LLM cero, finalVerdict INCONCLUSIVE, confidence 0.00, severity null, HYB-01, outcomeReason EXPORT_NOT_AUTHORIZED, errorReason null y findingId null. SHALL registrar señales existentes y explicación sin promoverlas por sí solas a veredicto. CONTEXT_UNAVAILABLE SHALL diagnosticarse como preparación incompleta del caso principal; FAILED SHALL mantenerse como fallo técnico. Toda divergencia SHALL investigarse sin cambiar expectativas para hacer pasar el recorrido.

#### Scenario: Señal fuerte con exportación deshabilitada
- **WHEN** se observa un marcador vulnerable real con contexto correcto
- **THEN** se registra la señal y se mantiene el resultado híbrido INCONCLUSIVE por falta de autorización de exportación

### Requirement: E2E-12 Evidencia por categoría sin ampliar API

**Historia:** Como revisor, quiero distinguir el resultado observado de las expectativas del oráculo.
**Prioridad:** MUST para evidencia funcional; inspección SQL complementaria y opcional.
**Dependencias:** E2E-09/E2E-11; EE-13; DV-09/DV-10.

La evidencia funcional SHALL contener una fila por LLM01/LLM02/LLM08/LLM10 con los ids reales, code/revision, estado TestExecution, HTTP/latencia, redacción, estado de evaluación/LLM, verdict, confidence, Finding sí/no y explicación/motivos disponibles por API, contrastados con UI. El recorrido funcional SHALL poder aceptarse exclusivamente con APIs + UI. La respuesta sintética persistida, señales deterministas, intento LLM y campos de snapshot no expuestos por API SHALL registrarse como NOT_INSPECTED si no se inspeccionan; SHALL NOT copiarse expectativas contractuales como valores observados.

Si existe autorización SQL de lectura, una extracción manual SHALL poder aportar evidencia diagnóstica complementaria sobre esos campos internos. En tal caso SHALL usar transacción READ ONLY y filtros por Project/Scan/target/fecha exactos, sin SELECT *, credenciales, tablas ajenas, UPDATE/DELETE ni otra escritura o fabricación de estados. La ausencia de acceso o de extracción SQL SHALL NOT invalidar el recorrido funcional ni hacerlo incompleto si APIs + UI cumplen sus criterios. Una discrepancia realmente observada SHALL registrarse e investigarse conforme al workflow de defectos, sin ocultarla por ser diagnóstica.

En la inspección complementaria, LLM01 SHALL contrastar acción reservada; LLM02 canario completo; LLM08 ambas anclas/contexto; LLM10 transporte y string JSON interno/clave inesperada con alcance OUTPUT_ONLY. Campos internos no observados SHALL permanecer NOT_INSPECTED, sin rellenarlos con oráculo, inferencias o una llamada nueva al target.

#### Scenario: Inspección interna autorizada
- **WHEN** se dispone de autorización SQL de lectura y se realiza la extracción complementaria
- **THEN** sus campos internos observados se adjuntan con procedencia y alcance READ ONLY, separados de la evidencia funcional API/UI

#### Scenario: Recorrido sin autorización SQL
- **WHEN** APIs + UI satisfacen los criterios funcionales y no hay autorización SQL o no se realiza la inspección opcional
- **THEN** el E2E funcional puede aceptarse con los campos internos NOT_INSPECTED, sin inventarlos ni exigir acceso SQL

### Requirement: E2E-13 Scoring persistido e incertidumbre

**Historia:** Como usuario, quiero que el riesgo mostrado concuerde con los Findings y sus límites.
**Prioridad:** MUST.
**Dependencias:** scoring-engine SC-01..SC-07; E2E-11.

El recorrido SHALL comprobar availability AVAILABLE, scoringVersion scoring-v1, scanId, fechas, globalScore, RiskLevel, contadores y cuatro categorías ordenadas, coherentes con evaluaciones/Findings. Para cuatro I y cero Findings SHALL contrastar globalScore=0, riskLevel=NO_FINDINGS, inconclusiveCount=4, findingCount=0, notVulnerableCount=0 y evaluationCount=4, con una I por categoría. SHALL usar el score de API como fuente de verdad, sin recalcularlo para sustituirlo. Score cero SHALL NOT interpretarse como aplicación segura.

#### Scenario: Cero con cuatro abstenciones
- **WHEN** la API devuelve cero y cuatro I
- **THEN** frontend y evidencia conservan la incertidumbre y ningún resultado se convierte en NOT_VULNERABLE

### Requirement: E2E-14 Informe estructurado coherente

**Historia:** Como usuario, quiero consultar el informe del mismo Scan sin generar otra evaluación.
**Prioridad:** MUST.
**Dependencias:** reporting RP-01..RP-14; E2E-13.

El recorrido SHALL comprobar availability AVAILABLE, reportVersion report-v1, locale es, ids/scope/fechas, summary idéntico al score persistido, cuatro evaluations concordantes, Findings reales, interpretation y las nueve limitations vigentes. SHALL releer para contrastar snapshot inmutable sin regenerar. Para el caso offline SHALL haber cuatro I y findings vacío. SHALL NOT añadir PDF, render de respuesta target, exportaciones o nuevos endpoints.

#### Scenario: Resumen contradictorio
- **WHEN** report.summary no coincide con GET score
- **THEN** el E2E falla y registra el defecto sin escoger silenciosamente un resumen

### Requirement: E2E-15 QA frontend sobre datos reales

**Historia:** Como revisor, quiero observar el flujo existente y sus estados reales en Angular.
**Prioridad:** MUST.
**Dependencias:** frontend-mvp FE-01..FE-24; frontend-polish; E2E-09/E2E-13/E2E-14.

QA SHALL cubrir Projects, creación/listado, Project detail, Target guardado y releído, lanzamiento único, estado/polling, ScanPage, score/risk/categorías, Findings vacíos, cuatro inconclusos y report/limitaciones. SHALL verificar rutas directas/refresh, cancelación al salir y contenido sintético tratado como texto, sin nuevos cambios de estilo. Un terminal rápido SHALL aceptarse sin simular progreso. Si existen Findings reales en una ejecución posterior autorizada, SHALL comprobar apertura del detalle y coherencia de referencias.

#### Scenario: Offline sin Findings
- **WHEN** el Scan offline finaliza correctamente
- **THEN** QA registra el estado vacío e inconclusos reales y deja la apertura de Finding como no cubierta

### Requirement: E2E-16 Criterio original de frontend-mvp 9.4

**Historia:** Como responsable de trazabilidad, quiero cerrar una tarea solo con la evidencia que realmente exige.
**Prioridad:** MUST.
**Dependencias:** frontend-mvp tasks 9.4, FE-24 y D-30; E2E-15.

El procedimiento SHALL conservar el criterio original: proyecto → target autorizado preparado → ejecutar → estado real → score/Findings/I → abrir Finding → report/limitaciones, con binding ligado a updatedAt antes del Scan. SHALL atribuir cobertura parcial al offline sin Findings y mantener 9.4 pendiente. Demo-vulnerable autorizada SHALL bastar como target; un proveedor real SHALL NOT convertirse en requisito intrínseco nuevo de 9.4. Con la semántica vigente, el primer offline no proporciona Finding: su apertura requiere otra ejecución real autorizada que lo produzca; cualquier excepción al criterio original requeriría aprobación aparte, no este change.

#### Scenario: Cierre parcial rechazado
- **WHEN** existe evidencia offline completa excepto apertura de Finding
- **THEN** se acepta el alcance offline de e2e-integration y se mantiene 9.4 sin marcar completada

### Requirement: E2E-17 Comparación externa con ground truth

**Historia:** Como revisor, quiero separar detecciones, negativos y abstenciones sin inventar métricas.
**Prioridad:** MUST.
**Dependencias:** DV-10/DV-14; E2E-11/E2E-12.

La evidencia SHALL incluir matriz Caso | Ground truth | Resultado scanner | Clasificación, unida por caso/perfil/version, code/revision y Scan real. Para evaluaciones COMPLETED SHALL clasificar V/V=TP, N/N=TN, N/V=FP, V/N=FN e I=INCONCLUSIVE independientemente del ground truth. Fallo técnico o ausencia SHALL anotarse aparte sin clasificación diagnóstica, sin convertirlos en I válido. SHALL NOT afirmar benchmark formal ni calcular precision/recall.

#### Scenario: Vulnerable con abstención
- **WHEN** ground truth es VULNERABLE y scanner COMPLETED/INCONCLUSIVE
- **THEN** clasificación es INCONCLUSIVE, nunca FN ni TN

### Requirement: E2E-18 Frontera target, evaluador y oráculo F-01

**Historia:** Como responsable de evaluación, quiero preservar la separación aprobada de información.
**Prioridad:** MUST.
**Dependencias:** demo-vulnerable F-01/DV-10/DV-16; evaluation-engine EE-06.

El target SHALL recibir solo el probe del catálogo en message. El evaluador SHALL conservar referencias técnicas aprobadas y respuesta observada como datos no confiables cuando se autorice su uso. Ground truth, expectedVerdict, justificación del caso y clasificación/resultado esperado u observado externos SHALL permanecer en el oráculo/evidencia externa y SHALL NOT enviarse en requests reales. SHALL NOT quitar trustedSyntheticContext, modificar serializador ni afirmar evaluación completamente ciega a referencias. Manifest de preparación SHALL NOT utilizarse como entrada directa del proveedor.

#### Scenario: Oráculo unido después de recopilar
- **WHEN** se calcula la matriz externa tras finalizar el Scan
- **THEN** sus etiquetas no modifican probes, contexto evaluador ni resultados persistidos

### Requirement: E2E-19 Automatización opcional y explícita

**Historia:** Como operador, quiero disponer de apoyo opcional para reducir errores sin depender de un script para ejecutar el recorrido.
**Prioridad:** Opcional para script; MUST para reproducibilidad manual y restricciones si se implementa.
**Dependencias:** E2E-04..E2E-10/E2E-21.

El E2E SHALL poder ejecutarse completamente mediante un procedimiento manual reproducible con APIs + UI. scripts/e2e-local.ps1 SHALL ser apoyo opcional; su ausencia SHALL NOT bloquear ejecución ni aceptación funcional. La recopilación manual o asistida SHALL aplicar el mismo criterio de aceptación sin SQL obligatorio definido en E2E-12/E2E-21.

Si se implementa la herramienta, SHALL ofrecer Prepare/Collect o automatización equivalente segura: crear/reutilizar Project/Target por API, completar copias locales de binding/manifiesto y recopilar resultados de un Scan real identificado por el operador. SHALL dejar el inicio principal a Angular, usar solo orígenes locales fijados, rechazar redirects y devolver códigos de salida distintos para éxito/fallo/precondición/timeout. SHALL NOT arrancar/matar procesos arbitrariamente, borrar BD, almacenar secretos, activar exportación/proveedor, reintentar escrituras ambiguas ni ocultar fallos. Su salida SHALL distinguir verificaciones API, QA UI pendiente y diagnóstico interno NOT_INSPECTED; no disponer de SQL SHALL NOT impedir aceptación funcional con APIs + UI completas.

#### Scenario: Procedimiento manual sin script
- **WHEN** el operador ejecuta y documenta el procedimiento manual reproducible con APIs + UI
- **THEN** el E2E funcional puede aceptarse sin scripts/e2e-local.ps1 ni inspección SQL

#### Scenario: Escritura con respuesta ambigua
- **WHEN** falla la respuesta de creación después del envío, manual o asistido
- **THEN** se detiene con fallo explícito, sin repetir POST automáticamente ni inventar ids

### Requirement: E2E-20 Regresiones dirigidas y tests aislados

**Historia:** Como mantenedor, quiero cubrir defectos descubiertos sin duplicar suites existentes.
**Prioridad:** MUST si se descubre un defecto; no suite nueva obligatoria.
**Dependencias:** E2E-03/E2E-22.

El recorrido SHALL reutilizar cobertura existente y proponer tests nuevos solo por fallo contractual, defecto de integración o regresión no cubierta. Tests con BD SHALL validar `llm_scanner_seguridad_test` antes de cualquier escritura. Builds/tests/linters/servidores SHALL requerir solicitud explícita independiente; preparar el change SHALL NOT ejecutarlos ni instalar dependencias.

#### Scenario: No hay defecto nuevo
- **WHEN** el recorrido cumple los contratos
- **THEN** no se añade una suite de tests que replique unit tests existentes

### Requirement: E2E-21 Evidencia pequeña y reproducible

**Historia:** Como revisor, quiero auditar el recorrido sin dumps ni datos sensibles.
**Prioridad:** MUST.
**Dependencias:** E2E-09..E2E-18.

El paquete SHALL registrar comandos realmente ejecutados, configuración no secreta, versiones/revisión, tiempos, ids reales/binding, preflight, estados/resultados, cuatro filas por categoría, matriz externa y hasta cuatro capturas representativas de frontend. SHALL separar comandos propuestos de ejecutados y cobertura pendiente. SHALL NOT guardar dumps, logs extensos, secretos, API keys, entorno completo, HAR sin filtrar o decenas de capturas. La evidencia funcional obligatoria de APIs + UI ausente SHALL mantener el paquete funcional incompleto. La ausencia de SQL o script SHALL NOT impedir aceptación; campos internos no inspeccionados SHALL figurar como NOT_INSPECTED con su motivo, separados del resultado funcional, sin inventar valores. La evidencia SQL autorizada SHALL ser complementaria conforme a E2E-12.

#### Scenario: Evidencia todavía no ejecutada
- **WHEN** solo existen artefactos de planificación
- **THEN** no se rellenan ids, screenshots, matriz observada ni resultados con valores ficticios

#### Scenario: Paquete funcional sin SQL ni script
- **WHEN** el paquete documenta el recorrido manual reproducible con APIs + UI y sus resultados obligatorios
- **THEN** puede acreditar aceptación funcional con diagnóstico interno NOT_INSPECTED, sin exigir SELECT SQL ni script

### Requirement: E2E-22 Tratamiento de defectos sin cambiar expectativas

**Historia:** Como mantenedor, quiero reparar integración preservando los contratos aprobados.
**Prioridad:** MUST.
**Dependencias:** E2E-20/E2E-21; lifecycle del proyecto.

Ante defecto SHALL reproducirse, localizar capa, documentar esperado/observado, proponer corrección mínima, añadir regresión no cubierta, reejecutar lo afectado con autorización y registrar evidencia. Cambios en backend/frontend/demo fuera del alcance operacional SHALL requerir autorización específica y revisión de su change/contrato antes de corregir. Una contradicción de fuentes SHALL detener la modificación afectada. SHALL NOT cambiar expectativas, omitir categorías ni falsificar estados para ocultar fallos.

#### Scenario: Defecto exige editar producto
- **WHEN** se identifica un bug de backend durante E2E
- **THEN** se registra y solicita autorización de la corrección concreta; no se amplía silenciosamente el scope del script

### Requirement: E2E-23 Limpieza y reutilización conservadoras

**Historia:** Como operador, quiero repetir el recorrido sin perder el historial.
**Prioridad:** MUST.
**Dependencias:** E2E-05/E2E-07/E2E-21.

La política SHALL conservar Project/Target/Scans en BD y reutilizar por ids explícitos o crear un nuevo Project identificable. Como no existe API DELETE aprobada, «desechable» SHALL significar datos sintéticos aislados prescindibles, sin borrado implementado. Tras el recorrido SHALL deshabilitarse operacionalmente la excepción local/exportación cuando ya no se necesiten y conservarse evidencia mínima; el script SHALL NOT gestionar procesos. Bindings invalidados SHALL regenerarse antes de nuevas ejecuciones, sin reescribir históricos.

#### Scenario: Repetición del recorrido
- **WHEN** se reutiliza un Project sin Scan activo
- **THEN** se relee Target/updatedAt, se valida el binding y se crea un Scan nuevo, conservando los anteriores

### Requirement: E2E-24 Procedimiento futuro de evaluador real

**Historia:** Como responsable, quiero conocer los pasos futuros y sus costes sin activarlos en el primer E2E.
**Prioridad:** MUST documental; ejecución fuera de aceptación obligatoria.
**Dependencias:** ADR-004; EE-05/EE-06/EE-09; E2E-07/E2E-17/E2E-18.

La documentación SHALL separar una operación futura expresamente autorizada con EVALUATOR_PROVIDER=OPENAI, modelo aprobado configurable, API key suministrada únicamente al proceso scanner por EVALUATOR_API_KEY, exportación global habilitada, scenarioId permitido y exportPolicy habilitada para corpus sintético exacto. SHALL exigir límites de llamadas/tokens/timeout/coste y autorización de tarifa/modelo antes del envío, preservar cero retries y registrar uso/coste desconocido como desconocido. SHALL comparar resultados realmente observados con oráculo externo y documentar HYB-02 ante señales fuertes válidas, posibles abstenciones/fallos y SAFE opcional con LLM concordante para HYB-04. SHALL NOT suministrar claves, activar proveedor, llamar desde la herramienta offline ni hacer esta operación condición de aceptación del primer E2E.

#### Scenario: Futuro no autorizado
- **WHEN** solo se autorice el primer E2E offline
- **THEN** proveedor/exportación permanecen deshabilitados y la operación real queda pendiente separada

#### Scenario: Cambio de conclusión en una ejecución futura
- **WHEN** se obtiene un veredicto distinto con exportación/proveedor autorizados
- **THEN** se registra en otro Scan y matriz propios, sin reescribir el resultado offline anterior
