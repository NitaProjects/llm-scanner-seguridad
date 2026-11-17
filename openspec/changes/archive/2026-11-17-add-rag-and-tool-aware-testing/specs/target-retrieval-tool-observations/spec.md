## Purpose

Comprobar observaciones de retrieval y herramientas sobre fixtures autorizadas e incorporar fuentes tipadas a la evaluación actual.

## ADDED Requirements

### Requirement: OBS-01 Extracción y observaciones correlacionadas

El binding opt-in SHALL permitir seleccionar retrieval query, items/chunks, tool calls, tool results y final answer desde JSON completo y desde SSE/JSONL. SHALL reutilizar selectores soportados de propiedad e índice, con selección acotada de arrays y selectores relativos por elemento. Ambos formatos SHALL converger en el mismo modelo y acumulador de observaciones. SHALL correlacionar call/result por ID único y ámbito de ejecución, respetando orden cuando la fuente ordena eventos. En arrays separados SHALL correlacionar por ID sin inferir cronología. Duplicados, resultados huérfanos o faltantes SHALL dejar inconclusa la comprobación dependiente.

#### Scenario: La misma traza por dos formatos
- **WHEN** dos fixtures presentan la misma consulta, chunks, call/result y respuesta final mediante JSON completo y stream
- **THEN** se obtienen las mismas observaciones tipadas y la misma respuesta final mediante un acumulador común

### Requirement: OBS-02 Comprobaciones defensivas locales

Con fixtures de propietario autorizado y acciones inertes, el scanner SHALL comprobar aliases sintéticos de retrieval y herramientas según el contrato de prueba. SHALL emitir diagnósticos técnicos acotados sin modificar scoring, Findings o report-v1 como consecuencia directa de esos diagnósticos. SHALL NOT ejecutar herramientas, callbacks, shell, navegador, MCP o agentes ni seguir URLs, descargar documentos o abrir exchanges adicionales por instrucciones del target. RAG propio, embeddings e indexación SHALL quedar fuera del incremento.

#### Scenario: Solicitud no permitida
- **WHEN** una fixture emite una solicitud no incluida en la lista y un evento que pide un callback
- **THEN** se registra el diagnóstico local y hay cero despachos y conexiones nuevas

### Requirement: OBS-03 Contratos de formato y cotas

El JSON malformado o parcial SHALL fallar según el contrato técnico vigente y SHALL NOT aportar observaciones de prefijos. Un workflow mixto SHALL usar el formato declarado por intercambio. Los pasos JSON declarados pueden aportar observaciones y solo el terminal válido aporta FINAL_OUTPUT. SHALL rechazar discrepancia de formato sin sniffing, fallback ni cambio de parser a mitad del cuerpo. Observaciones inválidas o ausentes SHALL dejar inconclusa la parte dependiente y conservar la respuesta final válida para comprobaciones independientes.

Las observaciones SHALL consumir las cotas vigentes de bytes, texto, eventos, parser y tiempo. Por interacción SHALL admitirse como máximo una consulta, 32 items/chunks y 16 calls con hasta 16 resultados. Cada registro saneado SHALL limitarse a 4 KiB UTF-8, cada ID a 128 bytes UTF-8 y el agregado saneado serializado a textBytes efectivo. Un exceso SHALL invalidar la evidencia dependiente sin promover prefijos truncados.

#### Scenario: Workflow mixto y JSON parcial
- **WHEN** un workflow recibe observaciones de un paso JSON completo y termina con un stream válido
- **THEN** las observaciones comparten contexto y solo el terminal aporta FINAL_OUTPUT
- **AND** sustituir el documento intermedio por JSON parcial provoca fallo del paso sin reutilizar su prefijo

### Requirement: OBS-04 Contexto tipado en el motor actual

La preparación de evaluación SHALL recibir opcionalmente evidencia interna acotada de la misma TestExecution confirmada, distinguida como USER_INPUT, RETRIEVED_CONTEXT, TOOL_ARGUMENT, TOOL_RESULT y FINAL_OUTPUT. SHALL mantener procedencia, orden, completitud y clasificación de exportación. SHALL NOT concatenar las fuentes ni usar observaciones como criterio o instrucción confiable. FINAL_OUTPUT SHALL usar la respuesta saneada confirmada. Los formatos y políticas legacy sin el perfil SHALL conservar su comportamiento. Hybrid-v1 y hybrid-v2 SHALL mantener criterios, reglas de consolidación y citas sobre la respuesta final.

#### Scenario: Entrega al punto de evaluación existente
- **WHEN** una ejecución opt-in completa confirma texto final y trae contexto retrieval/tool válido
- **THEN** la preparación recibe las fuentes por tipo y conserva la entrada textual y sus offsets originales
- **AND** una ejecución histórica o sin perfil mantiene el camino de evaluación habitual

### Requirement: OBS-05 Exportación por fuente y fallo cerrado

Cada observación adicional SHALL ser local-only por defecto. La proyección semántica estructurada SHALL incluir solo campos mínimos saneados, marcados exportables y autorizados explícitamente por tipo y selector con coincidencia exacta en la fixture sintética. SHALL mantener permiso operativo, corpus exacto de la respuesta y filtros y cotas actuales aplicables. SHALL verificar el candidato serializado completo, incluidas copias de datos bloqueados en otras fuentes. SHALL excluir secretos y datos locales de logs, evidencia pública, snapshot durable y proyección externa. La decisión local suficiente SHALL conservarse cuando la política lo permita. Si la decisión requiere semántica y una fuente necesaria queda bloqueada, SHALL producirse INCONCLUSIVE con motivo determinista existente y cero llamadas/reservas.

#### Scenario: Fuente requerida local-only
- **WHEN** una decisión exige corroboración semántica y una fuente requerida queda local-only
- **THEN** se omite la llamada y se registra INCONCLUSIVE con el motivo de evidencia o permiso correspondiente
- **AND** un caso independiente con evidencia local suficiente conserva su decisión según la política vigente

### Requirement: OBS-06 Compatibilidad de almacenamiento y transporte

El sistema SHALL descartar el objeto estructurado efímero al cerrar, abortar o cancelar la evaluación o su ejecución. Para una llamada opt-in con fuentes adicionales SHALL conservar únicamente en memoria su proyección y SHALL persistir evidence_projection e input_hash nulos, junto a los estados y contadores vigentes. El camino histórico SHALL conservar la representación y hash actuales. SHALL preservar los lectores históricos, la proyección pública, scoring-v1 y report-v1 usando el esquema existente. SHALL mantener controles vigentes de SSRF, DNS, TLS, origen, redirección y credenciales para REST_LEGACY, OPENAI_COMPATIBLE, streaming, sesiones y workflows.

#### Scenario: Lectura histórica y ejecución sin observaciones
- **WHEN** se consulta un informe previo y se repite su fixture sin perfil de observación
- **THEN** ambos conservan el contrato de resultados y el nuevo scan conserva el comportamiento de su política
