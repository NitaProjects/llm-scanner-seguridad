## Context

El transporte procesa respuestas JSON completas y streams SSE/JSONL. Los workflows pueden combinar pasos JSON con un terminal de stream y reutilizar credenciales acotadas por origen. La dependencia [add-dynamic-headers-and-cookies](../archive/2026-11-14-add-dynamic-headers-and-cookies/) está archivada. La evaluación recibe TestExecution después de confirmarla y aplica la política fijada en su snapshot. EvaluatorInput y SemanticProjection ya delimitan la información que llega al evaluador externo.

## Goals / Non-Goals

Extraer observaciones declaradas de fixtures autorizadas e incorporarlas como contexto de la evaluación existente. El scanner realiza cero ejecuciones de herramientas y cero exchanges derivados de las observaciones. Shell, navegador, MCP, agentes, RAG propio, embeddings e indexación quedan fuera del alcance.

## Decisions

### D-01 Extracción y normalización comunes

El binding operativo opt-in declara formato por intercambio y selectores de retrieval query, retrieved items/chunks, tool calls, tool results y final answer. Los selectores reutilizan el subconjunto soportado de propiedades e índices. Para una colección, seleccionar un array con ese mismo selector y aplicar los selectores declarados relativos a cada elemento, bajo contador acotado. La colección conserva su orden. No se incorporan comodines, filtros ejecutables ni autodetección de esquemas. Cada registro expone únicamente los campos declarados y tipados. Los campos de texto son strings y las claves de correlación son escalares opacos. Los objetos de argumentos o resultados solo aportan los campos seleccionados, nunca un volcado completo.

En JSON, validar el documento completo con el parser estricto y los límites del intercambio antes de emitir observaciones. El lector JSON y el colector SSE/JSONL entregan al mismo acumulador interno los tipos MODEL_TEXT, RETRIEVAL_QUERY, RETRIEVED_ITEM, TOOL_REQUEST, TOOL_RESULT y FINAL_ANSWER. El acumulador ejecuta las mismas reglas de correlación, límites y saneamiento. RETRIEVED_ITEM puede incluir alias sintético de documento o chunk y texto seleccionado. TOOL_REQUEST contiene alias sintético y argumentos seleccionados. TOOL_RESULT contiene resultado seleccionado. MODEL_TEXT conserva procedencia de diagnóstico. FINAL_ANSWER es el único texto que resulta elegible como responseText. Para REST_LEGACY y OPENAI_COMPATIBLE sin el perfil opt-in se conservan parsers y contratos existentes. Configurar observación en OpenAI-compatible no autoriza responder a tool calls ni aceptar un turno pendiente de herramienta como final.

En un workflow mixto, solo los pasos JSON completos declarados aportan observaciones intermedias y solo el terminal declarado aporta la respuesta final, en JSON o SSE/JSONL. Un frame portador de JSON se procesa como parte del stream con los selectores existentes. El Content-Type y el binding del intercambio deciden el parser. Un formato distinto del declarado falla conforme al contrato de transporte. Quedan fuera el sniffing, el fallback a otro parser y los cuerpos que cambian de protocolo a mitad de lectura.

JSON malformado o parcial invalida el documento y conserva el fallo técnico del intercambio. No se reutilizan prefijos ni se evalúan resultados de una interacción fallida. Si el documento o stream final es válido pero una observación opcional falta o tiene forma incorrecta, marcar esa parte de la traza incompleta. El resultado textual válido puede seguir evaluándose para criterios que no dependen de esa parte. Si falta la respuesta final o el terminal exigido, aplicar el fallo técnico existente.

### D-02 Correlación y límites

Correlacionar petición y resultado mediante callId opaco dentro de una sola TestExecution y con ordinales locales. Para streams y pasos sucesivos, el resultado debe referir una petición ya observada. En un documento JSON con arrays separados se vinculan por ID único dentro del documento, sin deducir cronología de la posición relativa de las propiedades. Para una lista ordenada de eventos, se respeta el orden declarado. IDs ausentes, duplicados, resultados huérfanos y resultados esperados ausentes dejan inconclusas las comprobaciones dependientes. La petición acredita una solicitud observada. El resultado es una declaración del target y se conserva con esa procedencia. Los IDs remotos se mantienen en memoria.

Reutilizar responseBytes, textBytes, límites del parser y de eventos/frames, presupuesto acumulado del workflow y plazo monotónico. Proponer máximos agregados por interacción de una consulta, 32 items/chunks y 16 pares potenciales de tool calls/resultados. Acotar cada registro saneado a 4 KiB UTF-8 y cada ID a 128 bytes UTF-8, con total de observaciones sujeto también a textBytes efectivo, incluidos los escapes al serializar. Prevalece el menor límite aplicable. Los contadores evitan que arrays cortos en bytes multipliquen el coste de extracción y correlación. Al exceder una cota de observación, invalidar el conjunto afectado para decisión/exportación, sin truncarlo como prueba ni seguir acumulándolo. Los límites del intercambio completo conservan sus errores técnicos y abortos actuales.

### D-03 Contexto tipado y entrega a evaluación

Usar un objeto interno e inmutable de evidencia estructurada como acompañante efímero. Transportarlo opcionalmente desde NormalizedInteraction, mediante un componente interno con ausencia por defecto y constructores compatibles. La proyección legacy omite ese componente. Mantenerlo en la variable de la iteración del worker y pasarlo explícitamente a una sobrecarga de preparación de evaluación tras confirmar la TestExecution correspondiente. Se usa únicamente para el mismo executionId y una ejecución completa. Se descarta al finalizar la evaluación, fallar el commit, cancelar o abortar. Un reinicio conserva el comportamiento histórico de recuperación. La respuesta final siempre proviene del texto confirmado y sus flags de redacción.

| Fuente tipada | Procedencia y representación |
|---|---|
| USER_INPUT | Prompt local realmente enviado, identificado por la prueba. No incluye body de transporte ni instrucciones del target |
| RETRIEVED_CONTEXT | Consulta e items/chunks en registros separados. Alias, ordinal y campos seleccionados |
| TOOL_ARGUMENT | Petición observada, alias y campos de argumento seleccionados, ligados a la correlación |
| TOOL_RESULT | Resultado declarado y campos seleccionados, ligados a la misma correlación |
| FINAL_OUTPUT | responseText saneado confirmado, con información de completitud y redacción |

Cada registro conserva tipo, procedencia, ordinal, completitud y clasificación local-only/exportable. Las observaciones son datos no confiables y quedan separadas del criterio y del contexto sintético autorizado. El motor local recibe esta estructura mediante el punto actual de preparación. Las comprobaciones de aliases de retrieval y de herramienta solicitada utilizan solo las fuentes pertinentes y emiten diagnósticos. Para cada prueba opt-in, el binding fija las fuentes adicionales requeridas u opcionales para la corroboración semántica. Preparación usa esa declaración fijada al inicio del scan, nunca instrucciones del target o del evaluador. La declaración no vuelve elegible una evaluación que la política vigente descartaría. La evaluación del catálogo mantiene las precondiciones, métricas, referencias y consolidación de hybrid-v1 o hybrid-v2. Las citas que consolidan los verdicts se comprueban sobre FINAL_OUTPUT, con sus offsets originales. Un diagnóstico de traza por sí solo no crea un Finding.

### D-04 Privacidad y semántica

Por defecto toda observación adicional es local-only. Sanear después de ensamblar y seleccionar, con la redacción vigente para credenciales y valores sensibles conocidos de sesión/workflow. Excluir cabeceras, secretos, URLs, cuerpos de request, argumentos/resultados completos, documentos brutos y system prompts del contexto de exportación y de logs. Mantener en memoria solo los campos acotados necesarios. No usar ni mostrar una copia anterior al saneamiento. Una redacción que invalida una prueba reduce su aplicabilidad.

Si la política vigente y el criterio permiten una llamada semántica, extender EvaluatorInput/SemanticProjection solo en la rama opt-in con campos tipados y acotados. Cada campo adicional requiere permiso explícito de la fixture por tipo y selector, coincidencia exacta del valor saneado con el dato sintético permitido, y clasificación exportable. Se mantienen las puertas actuales de proveedor, escenario, corpus exacto de la respuesta final y el filtro de exportación de la política seleccionada. USER_INPUT solo se proyecta donde la política ya admite prompt de catálogo. Aplicar controles al candidato serializado completo, también si una fuente local-only aparece repetida en FINAL_OUTPUT o en otro campo. Bloquear si se detecta esa copia o el candidato no puede verificarse dentro de las puertas de inspección vigentes. Las cotas de input, evidencia, memoria y persistencia se aplican al total serializado. No mandar un prefijo ni promover contenido observado a instrucciones o parámetros confiables.

Una señal no exportable puede sostener una decisión local cuando las reglas de la política y la evidencia suficiente lo permiten. Si una fuente requerida por la decisión semántica queda bloqueada, omitir input y llamada y consolidar INCONCLUSIVE determinista con causa existente. Usar EXPORT_NOT_AUTHORIZED para falta de permiso, EVIDENCE_NOT_EXPORTABLE para fuente local-only o ajena a la fixture, EVIDENCE_INCOMPLETE para fuente requerida inválida/ausente y EVIDENCE_TOO_LARGE para exceso de límites. Una fuente opcional se omite si la decisión puede evaluarse con la proyección autorizada remanente y esta cumple todas las comprobaciones. Las señales locales ya concluyentes de hybrid-v2 conservan el camino sin reserva. Hybrid-v1 mantiene sus condiciones y precedencias originales para evaluaciones sin el perfil.

### D-05 Compatibilidad y persistencia

Reutilizar resultado, esquema y proyección públicos de ejecución. Persistir respuesta final saneada, señales, versiones, estados y contadores conforme a los contratos actuales. Las observaciones adicionales y sus valores se descartan después de la evaluación. Para una llamada opt-in que incluye fuentes adicionales, serializar la proyección exacta en memoria y dejar evidence_projection e input_hash nulos. Conservar input_bytes y el estado ya soportados. Esta excepción prospectiva evita guardar documentos, consultas y argumentos/resultados adicionales y evita representar una proyección parcial como si fuera el input exacto. La llamada con la proyección histórica sigue guardando su representación y hash actuales. No se requieren migraciones. Lecturas históricas conservan estructura y significado con contexto ausente. Scoring-v1, Findings, report-v1, API pública y frontend conservan sus contratos.

El operador configura fixtures, stubs inertes y egress del target. Aislamiento de origen, SSRF/DNS/TLS, redirects desactivados, límites y credenciales vigentes se aplican a los intercambios previstos. El scanner jamás despacha tools, descarga documentos, sigue URLs de trazas ni reenvía argumentos. Los diagnósticos locales contienen clases, ordinales, contadores y códigos, con IDs de ejecución locales. No incluir valores ni IDs remotos en logs, informes o evidencia pública.

## Risks / Trade-offs

El target puede omitir, falsificar o reordenar observaciones. La decisión depende de la procedencia, completitud y contrato sintético verificables. Una traza inconclusa preserva la incertidumbre. La autorización exacta sacrifica cobertura semántica sobre texto libre. El target puede ejecutar acciones por su cuenta. Las fixtures requieren stubs y límites operativos controlados por su propietario.

## Open Questions

- Qué fixtures autorizadas aportará el operador para una validación posterior del contrato real. La aceptación reproducible usa fixtures sintéticas definidas en la spec.
