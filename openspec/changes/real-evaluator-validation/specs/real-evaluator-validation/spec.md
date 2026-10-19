## Purpose

Definir una validación operativa local, única y controlada del evaluador OpenAI existente, con trazabilidad de resultados reales hasta Angular y sin convertirla en un experimento académico formal.

## ADDED Requirements

### Requirement: REV-01 Alcance y autorización

**Historia:** Como responsable, quiero revisar el procedimiento antes de autorizar consumo externo.
**Prioridad:** MUST. **Dependencias:** ADR-004; evaluation-engine; e2e-integration E2E-24.

La preparación SHALL limitarse a artefactos OpenSpec. La corrección del contrato local SHALL limitarse al schema auxiliar, tests y documentación definidos en REV-11. La reparación posterior expresamente autorizada del transporte SHALL limitarse a REV-12 sin otros cambios de runtime. La ejecución real SHALL requerir aprobación del change y autorización operativa explícita de servicios locales, proveedor/modelo y coste. SHALL realizar un único Scan inicial contra la demo propia VULNERABLE; SHALL NOT modificar runtime ajeno a REV-12, iniciar operaciones durante la planificación/reparación, llamar directamente al proveedor desde Angular/herramientas, fabricar Findings ni ampliar el alcance a benchmark, tuning, SAFE, AWS o modelos alternativos.

#### Scenario: Entrega del diseño
- **WHEN** se presenta el change
- **THEN** no se acredita ejecución real y todas las tareas operativas permanecen pendientes

### Requirement: REV-02 Preflight local verificable

**Historia:** Como operador, quiero comprobar destino y configuración antes de consumir llamadas.
**Prioridad:** MUST. **Dependencias:** REV-01; target-configuration; demo-vulnerable; E2E-02..E2E-08.

El procedimiento SHALL comprobar PostgreSQL local y BD de desarrollo, demo UP/VULNERABLE/version 1, scanner con profile local, excepción SSRF limitada a `http://127.0.0.1:8081`, API y proxy Angular existentes, identidad del proyecto y ausencia de otro Scan activo. SHALL registrar los valores efectivos no secretos del proceso scanner con procedencia, sin inferirlos del entorno de otra consola ni presentar defaults como runtime observado. Un parámetro crítico no confirmado SHALL bloquear el lanzamiento.

#### Scenario: Preflight incompatible
- **WHEN** falta autorización, el proceso no está identificado o destino/perfil/BD no coinciden
- **THEN** no se lanza Scan ni se relajan controles

### Requirement: REV-03 Clave y exportación temporal

**Historia:** Como operador, quiero limitar el envío a datos sintéticos autorizados y proteger la clave.
**Prioridad:** MUST. **Dependencias:** REV-02; EE-05/EE-06.

La clave SHALL suministrarse únicamente mediante `EVALUATOR_API_KEY` en el entorno del proceso scanner, sin leer ni registrar su valor en evidencia. SHALL NOT persistirse en repo, application*.yml, archivos .env, manifest, Target, BD, logs, historial de comandos, frontend o capturas. El envío SHALL requerir simultáneamente `EVALUATOR_EXPORT_ENABLED=true`, `EVALUATOR_ALLOWED_SCENARIOS=demo-vulnerable-v1` y `exportPolicy.enabled=true/provider=OPENAI` en el escenario ligado al Target. SHALL autorizar exclusivamente respuestas sintéticas completas que coincidan exactamente con el corpus aprobado; ground truth y clasificaciones SHALL permanecer externos.

#### Scenario: Gate ausente o respuesta ajena al corpus
- **WHEN** falta un permiso de exportación o la respuesta no pertenece al corpus autorizado
- **THEN** no se envía esa evidencia y se conserva el motivo real, sin ampliar el corpus para forzar el envío

### Requirement: REV-04 Manifest y binding reales

**Historia:** Como operador, quiero vincular el contexto al Target realmente analizado.
**Prioridad:** MUST. **Dependencias:** REV-02/REV-03; DV-10; EE-06.

La preparación SHALL partir de `evaluation-vulnerable.template.json` y los schemas existentes, con scenarioId `demo-vulnerable-v1`, revisión 1, cuatro code/revision originales y IDs/targetUpdatedAt obtenidos de API sin redondear. Solo SHALL sustituir los tres placeholders de identidad y habilitar la exportPolicy para esta ejecución. SHALL conservar contexto/corpus; el binding SHALL incluir perfil, health comprobado y declaración de autorización. SHALL validar los dos documentos y releer Target antes del Scan. Un cambio de Target/perfil SHALL invalidar la preparación.

#### Scenario: Target actualizado tras el binding
- **WHEN** la fecha o identidad ya no coincide
- **THEN** se bloquea el lanzamiento hasta preparar de nuevo el binding, sin alterar resultados históricos

### Requirement: REV-05 Consumo acotado y lanzamiento único

**Historia:** Como responsable, quiero validar integración con un consumo pequeño y trazable.
**Prioridad:** MUST. **Dependencias:** REV-01..REV-04; EE-09.

La ejecución SHALL conservar como máximo cuatro intentos por Scan, uno por evaluación, concurrencia uno, intervalo mínimo de 1000 ms, timeout de evaluación de 20000 ms y máximo de 1024 output tokens por llamada, sin ampliar límites existentes. SHALL registrar modelo solicitado, límites efectivos y política monetaria aprobada antes del envío. SHALL NOT inventar tarifas ni presentar límites de tokens/reservas estimadas como garantía de factura máxima. Angular SHALL lanzar una sola vez el Scan; respuesta ambigua SHALL investigarse por histórico sin repetir POST. Cualquier nuevo Scan o retry manual SHALL exigir nueva autorización, nunca repetirse para obtener Findings.

#### Scenario: Error o ausencia de Findings
- **WHEN** el único Scan no produce detecciones o falla el proveedor
- **THEN** se conserva el resultado y no se lanza otro Scan automáticamente

### Requirement: REV-06 Verificación del proveedor separada del veredicto

**Historia:** Como revisor, quiero distinguir integración real, degradación y conclusión híbrida.
**Prioridad:** MUST. **Dependencias:** REV-05; EE-07/EE-10.

La evidencia SHALL inspeccionar cuatro EvaluationResults y separar status técnico, llmStatus, llmErrorReason, finalVerdict, decisionCode y degraded. La validación completa del proveedor SHALL exigir cuatro componentes LLM SUCCEEDED de este Scan con respuesta estructurada aceptada; cualquier fallo/supresión SHALL registrarse como validación parcial o fallida, aunque el Scan sea COMPLETED. SHALL NOT exigir cuatro VULNERABLE ni concordancia del LLM con el ground truth. Un Finding determinista o Scan COMPLETED por sí solos SHALL NOT acreditar éxito de OpenAI.

Timeout, 5xx y salida inválida SHALL conservar el error individual; 429/configuración/cupo SHALL suprimir intentos posteriores conforme al contrato actual. Señal fuerte válida SHALL poder consolidar VULNERABLE degradado; sin soporte suficiente SHALL conservar INCONCLUSIVE. Fallos técnicos globales SHALL registrarse como FAILED, sin inventar resultados faltantes.

#### Scenario: Finding con proveedor fallido
- **WHEN** una señal fuerte produce HYB-02 y el LLM falla
- **THEN** se conserva el Finding real, el error/degradación y la validación del proveedor incompleta

#### Scenario: Respuesta válida sin vulnerabilidad
- **WHEN** el proveedor responde válidamente y la consolidación produce NOT_VULNERABLE o INCONCLUSIVE
- **THEN** se registra el resultado real sin fabricar Finding ni ajustar prompts o expectativas

### Requirement: REV-07 Cadena persistida y presentación

**Historia:** Como usuario, quiero revisar los resultados del mismo Scan hasta Angular.
**Prioridad:** MUST. **Dependencias:** REV-06; scoring-engine; reporting; frontend-mvp.

Para Scan COMPLETED SHALL verificarse cuatro ejecuciones/evaluaciones COMPLETED, coherencia de Findings con sus evaluaciones VULNERABLE, score AVAILABLE/scoring-v1 y report AVAILABLE/report-v1/es confirmados con el cierre. SHALL contrastarse summary del informe con score, cuatro categorías, referencias, interpretación y limitaciones vigentes, y snapshot estable al releerlo. Angular SHALL mostrar los datos realmente persistidos sin recalcular, ocultar incertidumbre ni ejecutar el contenido del target. FAILED SHALL documentarse con sus datos conservados y disponibilidades reales; SHALL NOT presentarse como cadena completa.

#### Scenario: Scan sin Findings
- **WHEN** las evaluaciones completadas no justifican Findings
- **THEN** Findings permanece vacío y score cero se interpreta como ausencia de hallazgos, sin acreditar seguridad

### Requirement: REV-08 Ground truth externo

**Historia:** Como revisor, quiero comparar cada caso sin contaminar al evaluador.
**Prioridad:** MUST. **Dependencias:** REV-06; ground-truth-v1; DV-14.

La comparación SHALL unir Scan/categoría/code/revision/perfil/version con los cuatro casos VULNERABLE del ground truth, después de recopilar resultados. Para evaluaciones COMPLETED SHALL usar V/V=TP, N/N=TN, N/V=FP, V/N=FN e I=INCONCLUSIVE. Fallos técnicos/ausencias SHALL figurar aparte, sin convertirlos en abstenciones válidas. Este único recorrido VULNERABLE SHALL NOT acreditar TN/FP observados, precision/recall o benchmark estadístico. LLM10 SHALL conservar OUTPUT_ONLY, sin sink demostrado. El assessment LLM disponible SHALL distinguirse del veredicto híbrido comparado.

#### Scenario: Abstención con ground truth vulnerable
- **WHEN** el resultado COMPLETED es INCONCLUSIVE para un caso VULNERABLE
- **THEN** la clasificación es INCONCLUSIVE, nunca FN

### Requirement: REV-09 Cierre condicionado de frontend 9.4

**Historia:** Como responsable, quiero acreditar exactamente el recorrido original.
**Prioridad:** MUST. **Dependencias:** REV-07; frontend-mvp tasks 9.4, FE-24 y D-30.

El recorrido SHALL cubrir proyecto → target autorizado ya preparado → ejecutar → estado real → score/Findings/I → abrir Finding → report/limitaciones, con binding ligado a target.updatedAt antes del lanzamiento. Si existe al menos un Finding persistido del Scan, SHALL abrirse desde Angular y contrastarse sus campos y referencias con la API. Solo SHALL proponerse cerrar 9.4 cuando toda la secuencia original esté observada y documentada; ausencia de Finding o cualquier paso faltante SHALL mantenerla pendiente. SHALL NOT simular detecciones, modificar el escenario para obtenerlas ni convertir OpenAI en requisito intrínseco de 9.4. Este change de planificación SHALL NOT marcarla completada.

#### Scenario: Finding real con recorrido incompleto
- **WHEN** existe Finding pero no se ha observado el recorrido original completo
- **THEN** 9.4 permanece pendiente con la cobertura parcial identificada

### Requirement: REV-10 Evidencia no secreta y cierre sencillo

**Historia:** Como operador, quiero conservar una prueba auditable sin mantener exportación activa.
**Prioridad:** MUST. **Dependencias:** REV-01..REV-09.

La evidencia SHALL incluir configuración no secreta/procedencia, IDs/fechas/versiones, manifest/hash, respuestas públicas del Scan, comparación externa y capturas mínimas de UI. SHALL registrar el modelo solicitado expuesto por API. La lectura interna de modelo resuelto, usage y demás metadata SHALL ser diagnóstica, opcional y no bloqueante para lanzar el Scan ni aceptar funcionalmente la validación real del evaluator. Si existe autorización y disponibilidad, la consulta SHALL ser READ ONLY, sin modificar datos y limitada al Scan de validación, sin SELECT * ni cuerpos libres; SHALL registrar los valores observados, incluidos null cuando corresponda. Si no existe autorización o la lectura no está disponible, SHALL registrar NOT_INSPECTED con su motivo, sin inventar valores, interpretar la ausencia como consumo cero ni bloquear lanzamiento o aceptación funcional. SHALL separar aceptación del proveedor, cadena de integración y cierre 9.4.

#### Scenario: Diagnóstico interno no autorizado o no disponible
- **WHEN** no existe autorización de lectura interna de metadata o esta no está disponible
- **THEN** se registra NOT_INSPECTED sin inventar valores ni asumir consumo cero
- **AND** el lanzamiento y la aceptación funcional se evalúan con los demás criterios, sin exigir ese diagnóstico

Tras terminal o interrupción del recorrido SHALL deshabilitarse exportación efectiva del scanner y permiso local del manifest, retirar la API key del entorno de la sesión y conservar solo evidencia no secreta. Cambiar una variable en otra consola SHALL NOT acreditarse como revocación en un proceso ya iniciado. SHALL NOT borrar BD/históricos ni diseñar un cierre operacional adicional complejo.

#### Scenario: Finalización con error
- **WHEN** el proveedor falla o el recorrido se interrumpe
- **THEN** se registra el fallo y se aplica el mismo cierre de exportación/clave sin relanzar

### Requirement: REV-11 Contrato booleano de exportPolicy compatible en V1

**Historia:** Como operador, quiero validar estructuralmente manifests offline y reales autorizados sin confundir validez JSON con permiso de exportación.
**Prioridad:** MUST. **Dependencias:** REV-03/REV-04/REV-05; DV-10/DV-11; EE-06; D-08.

El schema auxiliar `demo-vulnerable/manifest/schemas/evaluation-scenario-v1.schema.json` SHALL admitir exactamente los booleanos false y true en exportPolicy.enabled, manteniendo el campo obligatorio, schemaVersion=1, provider OPENAI, estructura cerrada y las restantes restricciones vigentes. SHALL NOT crear V2 ni introducir un modo/campo nuevo. Plantillas versionadas SAFE/VULNERABLE SHALL conservar false; solo la copia local de una operación real autorizada SHALL habilitar true. El perfil de demo SHALL NOT interpretarse como modalidad de exportación.

La validación estructural SHALL NOT constituir autorización. El recorrido offline SHALL exigir enabled=false y EVALUATOR_EXPORT_ENABLED=false en su preflight aunque un documento true sea válido según el schema generalizado. La operación real SHALL conservar todos los gates existentes, incluidos binding exacto, flag global, allowlist, provider, contexto/corpus y clave únicamente en entorno, así como SSRF, separación oracle/evaluator y cuotas/coste. enabled=true por sí solo SHALL NOT autorizar envío. Los manifiestos benchmark/ground truth y los contratos runtime SHALL conservarse sin cambios.

La corrección SHALL incluir pruebas locales de ambos booleanos, tipos inválidos/campo ausente, conservación de plantillas y rechazo offline de true como precondición operacional. SHALL preservar y completar solo la cobertura ausente de los gates conjuntos sin red real. El README SHALL distinguir validez del schema, default offline y autorización real. La implementación y la ejecución de sus tests SHALL permanecer pendientes en esta entrega de diseño; el recorrido real SHALL NOT reanudarse hasta aplicar/comprobar la corrección y el preflight.

#### Scenario: Compatibilidad offline
- **WHEN** una plantilla SAFE o VULNERABLE con enabled=false se completa con un binding válido
- **THEN** el schema V1 sigue aceptándola y la plantilla versionada conserva false
- **AND** el preflight offline mantiene la exportación deshabilitada

#### Scenario: Copia real válida pero no automáticamente autorizada
- **WHEN** una copia local con binding válido declara enabled=true
- **THEN** el schema V1 generalizado acepta el booleano sin otorgar permiso de envío
- **AND** si falta flag global o allowlist, o el binding/corpus no coincide, el scanner conserva su rechazo de exportación

#### Scenario: Booleano obligatorio sin coerción
- **WHEN** enabled falta, es null, una cadena, un número, una lista o un objeto
- **THEN** el schema rechaza la instancia sin coerción ni default implícito

#### Scenario: Manifest true en recorrido offline
- **WHEN** un manifest habilitado pasa validación estructural pero se prepara un recorrido offline
- **THEN** el preflight offline lo rechaza y no se lanza el Scan
- **AND** no se utiliza schema PASS como sustituto de comprobar los permisos

### Requirement: REV-12 Transporte HTTPS con TLS válido y diagnóstico seguro

**Historia:** Como operador, quiero que el cliente alcance la negociación TLS con su configuración de confianza normal y permita distinguir fallos técnicos sin exponer datos sensibles.
**Prioridad:** MUST. **Dependencias:** REV-03/REV-05/REV-06; EE-05/EE-07/EE-09; ADR-004; D-09.

La reparación SHALL inicializar el manager HTTPS con un registro TLS válido soportado por HttpClient 5.6.4, conservando el resolver DNS acotado, caps DNS/IP, verificación de confianza/hostname, timeouts/deadline, endpoint Responses API, store=false, modelo configurable, clave solo en entorno, permisos y cuotas existentes. SHALL NOT añadir retries, redirects, proxies, destinos configurables, trust-all ni cambios de dependencias/API/BD. La regresión offline SHALL recorrer el manager real compartido con producción hasta selección TLS y emisión de ClientHello, sin sustituirlo por fixtureTransport ni usar el proveedor real.

El diagnóstico SHALL distinguir DNS, TLS/conexión, timeout, respuesta HTTP del proveedor y parse/schema; fallos internos SHALL conservar un identificador técnico seguro. SHALL mantener los LlmError y políticas de consolidación/supresión actuales. Los logs SHALL limitarse a categoría cerrada, LlmError y nombre de tipo de excepción cuando exista, sin mensaje/causas/stack trace, claves, cabeceras, cuerpos ni datos del target/proveedor. El diagnóstico SHALL NOT reconstruir retrospectivamente la causa del Scan fallido ni acreditar recepción HTTP, facturación o acceso al modelo.

#### Scenario: Ruta HTTPS offline real
- **WHEN** el cliente con el mismo manager de producción se conecta a un peer sintético de loopback que observa TLS y devuelve datos inválidos
- **THEN** emite ClientHello y rechaza la respuesta como fallo TLS, sin NPE de registry, sin HTTP de aplicación ni conexión externa

#### Scenario: Excepción con contenido sensible
- **WHEN** DNS, conexión/TLS, timeout o parseo fallan con un mensaje que contiene datos privados sintéticos
- **THEN** el resultado conserva el LlmError existente y el log solo registra categoría, código y tipo técnico, sin mensaje ni throwable

#### Scenario: Proveedor con respuesta no exitosa
- **WHEN** el transporte simulado devuelve un estado HTTP no exitoso
- **THEN** se conserva classify/status y su política actual, registrando la categoría HTTP_PROVIDER sin guardar el cuerpo
