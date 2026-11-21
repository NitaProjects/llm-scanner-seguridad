## Context

Ver proposal.md. La divergencia P1 está comprobada por inspección, no por ejecución: TargetJsonProcessor.redactText sustituye literal; InteractionSession.addSecret añade literal y serialización JSON y redactMatches fusiona intervalos originales. No enumeran codificaciones parciales Unicode. EvaluationEvidenceProjector.containsBlocked/blocksSpanExport recorre strings/JSON y decodifica hasta MAX_EMBEDDED_JSON_LAYERS=4, pero ExecuteSecurityTestsUseCase llama executions.finish antes de EvaluateExecutionUseCase. El veto de exportación no puede deshacer ese commit.

HttpTargetInvocationAdapter sanea safeRequest antes de red, vuelve a sanear el resultado y, en observaciones terminales, usa trace.localValues(); inspectionIncomplete ya puede sustituir request por {} y response por marcador. La cobertura de variantes sigue siendo insuficiente y los catches pueden devolver el safeRequest calculado antes de capturar nuevos secretos. PostgresTestExecutionRepositoryAdapter.finish comprueba tamaños, no equivalencia. Es una divergencia respecto a la exclusión durable de target-retrieval-tool-observations, no una excepción autorizada.

Fuentes: docs/03-seguridad/modelo-de-amenazas.md, docs/04-ia/estrategia-evaluacion.md, docs/06-base-de-datos/modelo-datos.md; specs target-retrieval-tool-observations, target-event-streaming, target-step-workflows; migraciones V1–V8 y adaptadores de persistencia. No se leen secretos, .env ni bases reales.

## Goals / Non-Goals

**Goals:** impedir copias recuperables de valores conocidos no persistibles en las representaciones soportadas, antes del primer commit y en copias secundarias; equivalencia común, límites explícitos y fail closed compatible con schema.

**Non-Goals:** detectar secretos desconocidos o toda transformación reversible imaginable; nuevos decodificadores base64/percent/HTML/compresión; NFC/NFKC, transliteración o cambios de mayúsculas del valor; censurar automáticamente canarios sintéticos autorizados para persistencia; cambiar protocolos, reglas híbridas, scoring/report-v1 o API pública; limpiar datos históricos/backups/WAL.

## Decisions

### 1. Frontera durable real y copias

| Tabla/componente | Campos reales / exposición | Protección |
| --- | --- | --- |
| test_executions | request_payload TEXT; response_payload TEXT contiene JSON String serializado, no respuesta bruta; request_redacted/response_redacted; error_reason enum | Guardar solo resultado certificado; inspección también del contenido lógico y representación que encodeResponse entrega a JPA |
| test_executions legacy | evaluator_result/confidence conservados sin mapeo ni nuevas escrituras | Mantener null en nuevas filas; auditar en PostgreSQL sin modificar históricos |
| evaluation_results | evidence_projection TEXT; deterministic_signals, llm_assessment, configuration_snapshot, sampling_parameters JSONB; reasoning_summary, recommendation, evidence_summary; metadata textual provider/model/requestId e input_hash/configuration_hash | Guardar solo campos/proyecciones permitidos; revalidar strings/keys/citas y serialización antes de claim/finish; hashes solo de contenido ya seguro |
| findings | title, explanation, evidence, recommendation; copia desde evaluación | Construir desde la misma evaluación saneada y confirmar juntos; no divergencia de contenido por redacción posterior independiente |
| scan_scores | categories/severity_counts JSONB y números/enums; no evidencia libre | Mantener modelo; comprobar que no se incorpora contenido de transporte/observaciones ni nuevos strings libres |
| security_reports | payload JSONB report-v1 con copias de Findings, referencias, interpretación y limitaciones | Validar candidato final antes de commit conjunto score/report/Scan; no sustituir JSONB entero por marcador inválido |
| target_configurations | credential_ciphertext BYTEA es el almacén cifrado autorizado; request_template/system_prompt/headers son configuración pública bajo reglas actuales | No copiar credencial descifrada en evidencia. No cambiar cifrado ni tratar ciphertext como texto equivalente. No convertir este change en saneamiento general de inputs del operador |
| projects/scans/test_cases | Metadata, estado, catálogo/prompts sintéticos; no repositorio de observaciones remotas | No introducir copias nuevas. Revisar todas las columnas en tests, incluidas heredadas; no redactar PK/FK/enums para forzar que una transacción sea válida |
| StructuredInteractionEvidence/Context, InteractionSession, exportCorpus | Efímeros, sin tabla de observaciones/contexto | No crear nueva tabla ni serializar accidentalmente toString/observations; proteger copias en FINAL_OUTPUT, aliases y correlation IDs |

T1 prepare y start persisten identidad/configuración; finish de ejecución usa REQUIRES_NEW y flush/commit; claim/finish de evaluación son transacciones posteriores (con Finding); cierre confirma score/report/Scan juntos. La validación debe preceder a persist/update/JDBC binding, no solo a flush ni a un interceptor postcommit. Evaluación se alimenta de la ejecución durable confirmada y saneada como hoy.

SQL/logs/errores nunca reciben el cuerpo descartado. No registrar el mensaje de parser/exception, tokens problemáticos, path/alias remoto o valores protegidos. Logs admiten IDs internos del scanner, contadores y motivos fijos; no hashes del secreto. Las serializaciones propias del schema (JSON String de response_payload, JSONB del informe) se decodifican en pruebas con el codec correspondiente, no se confunden con cifrado.

### 2. Conjunto protegido y ciclo de vida

Introducir un scope efímero de protección por Scan con aislamiento por ejecución y unión conservadora para el cierre. No es un vault persistido ni se obtiene del prompt. Añadir, antes de producir un candidato durable: credencial descifrada después de autorización vigente; variables sensitive de sesión, cookies activas/retiradas, CSRF/headers dinámicos efectivamente materializados, localValues y IDs/aliases local-only de observaciones. La clausura incluye bytes conocidos de valores derivados declarados/reenviados, no inferencia de derivados arbitrarios.

No basta conservar solo valores ya redactados: registrar originales en el scope seguro antes de que ObservationAccumulator descarte/altere una observación o InteractionSession.close libere su estado. Si no se pudo capturar un campo/ID completo, marcar scope incompleto y no aceptar su prefijo como universo completo. Captura posterior debe revalidar también safeRequest de éxito/error, porque puede contener valores recién clasificados. Pasos intermedios no adquieren persistencia nueva.

El scope permanece accesible mediante handle opaco al guard de ejecución/evaluación/cierre; no viajar en InvocationResult, snapshots o DTOs. Se libera en todos los cierres/compensaciones/interrupciones y tras commit final, sin reintentar HTTP. Separación estricta entre scans concurrentes; no cache global de credenciales. En reinicio, scopes no se reconstruyen descifrando secretos para recuperar: conservar recuperación FAILED/INTERRUPTED y no crear nuevas proyecciones desde datos no certificados.

La garantía se aplica a valores conocidos/clasificados al escribir y a sus copias en el pipeline. No afirma reconocimiento de futuros secretos aún desconocidos ni limpieza retroactiva. La configuración de escenario es sintética y permitida según contrato; reservedValue no implica por sí solo credencial operacional/local-only. Si un valor sí está clasificado no persistible, la precedencia de privacidad impide copiarlo aunque se pretenda incluir en snapshot de escenario.

### 3. Semántica compartida de equivalencia

Extraer ProtectedContentInspector (nombre orientativo), junto a utilidades de privacidad fuera del dominio; puerto de aplicación para protección de escritura, sin dependencia del dominio en Jackson/HTTP/Spring. Reusar el decoder del projector como punto de partida, endureciendo errores y cotas; no mantener clones. Los tres consumidores —durable, local-only y semantic exportability— comparten versión, límites, candidatos y resultado SAFE/MATCH/UNINSPECTABLE. Políticas distintas: durable redacta/abstiene; exportación veta; ninguna otorga permiso por pasar el inspector.

Contrato lógico para un scope P y perfil N4:

- MATCH si una vista soportada de un campo/string/key contiene un valor protegido conocido, incluidos matches incrustados en texto.
- SAFE solo tras inspección completa y estable de todas las vistas/campos necesarios y sin coincidencias.
- UNINSPECTABLE si falta dato protegido necesario, hay formato/Unicode/escape ambiguo, exceso o no se alcanza estabilidad.
- protectedEquivalent_N4(x,P) implica que el campo original no puede formar parte de un write durable; puede confirmarse únicamente una sustitución segura. UNINSPECTABLE tampoco autoriza conservarlo.

UNINSPECTABLE tiene precedencia sobre MATCH y SAFE: encontrar un literal no permite terminar antes de inspeccionar las demás ramas ni certificar un documento con quinta capa o escape malformado. No se conserva «el resto» de un contenido cuya inspección quedó incompleta.

Valores protegidos se usan como literales case-sensitive, sin ejecutar su contenido ni tratarlos como regex. Se inspeccionan además sus vistas de equivalencia conocidas de forma acotada: un valor protegido que ya contiene escapes no abre un bypass por comparar solamente su forma raw. Si la propia clasificación es ambigua, scope incompleto y fail closed.

### 4. N4: algoritmo y capas exactas

1. Validar bytes/Unicode y cotas antes de materializar una copia grande. En documentos propios JSON, parser estricto sin claves duplicadas, trailing tokens ni Unicode inválido; recorrer claves, strings y representaciones escalares con pila/nodos acotados. Texto de target no tiene por qué ser un documento JSON completo.
2. Conservar el texto original inmutable. Capa0 es contenido lógico del campo después de extracción de protocolo; cada candidato conserva región/origen, profundidad y resultado de inspección. Parsear envoltorios propios del almacenamiento no consume capa atacante ni relaja el límite.
3. En cada rama, comprobar literal y producir a lo sumo una siguiente vista por pasada de decoding JSON: comillas, barra invertida, slash, b/f/n/r/t y u seguido de cuatro dígitos hex. Aceptar A–F/a–f; soportar mezclas parciales como u0050 seguido de RIVATE_42, escapes múltiples y pares surrogate válidos. Mantener case del valor y validar pares/límites; NUL se representa seguro, no se introduce literal en PostgreSQL.
4. Strings con JSON anidado se recorren mediante el mismo algoritmo. Decodificar un token string/escape del contenido atacante consume una capa; recorrer contenedor sin decoding no añade capas. No sumar dos presupuestos independientes de cuatro para parse y escapes: máximo cuatro transformaciones en cualquier camino. Representaciones internas equivalentes se memoizan; no enumerar combinaciones de qué caracteres escapar.
5. Inspeccionar capas0..4. En capa4 permitir comprobación de estabilidad sin construir ni usar una quinta vista: si existe otra transformación válida capaz de cambiar texto relevante, UNINSPECTABLE aunque todavía no se vea el secreto. Un parse/decoding nuevo dentro de string cuenta en ese mismo camino. Batería de fixtures define encode^4/encode^5 sobre el mismo contenido lógico, sin contar el único wrapper response_payload que el scanner añade después.
6. Escape desconocido/truncado, surrogate aislado, JSON embebido reconocible pero inválido, claves duplicadas o ambigüedad producen UNINSPECTABLE. Prosa ordinaria sin candidato de escape/JSON sigue siendo texto válido; no exige parsear todo como JSON. Una secuencia sospechosa en texto libre no se ignora como «solo prosa». No normalizar ni devolver las vistas como respuesta del target; sirven para búsqueda de privacidad, no evidencia de vulnerabilidad.

Una implementación puede usar tokenización/procedencia para evitar parsear como JSON el resultado de desescapar estructura completa. Es obligatorio superar controles benignos de JSON válido con comillas escapadas y nested strings; no es aceptable resolverlo censurando todos los JSON. Canonicalización no se usa para autorizar nuevos corpus ni para relajar el matching exacto de respuestas sintéticas exportables.

### 5. Redacción y solapes sin source-map arriesgado

- Para coincidencias literales sobre original, calcular todos los intervalos antes de modificar, fusionar solapados/adyacentes y sustituir una vez. Conservar y probar el comportamiento de InteractionSession.redactMatches para secretos prefix/suffix/overlap.
- Si MATCH solo aparece en una vista decodificada, sustituir el campo completo. Es una decisión deliberada: no se introduce reconstrucción compleja de offsets para obtener un excerpt más bonito. Este reemplazo equivale al intervalo original [0,length); se fusiona con cualquier otro antes de una sola aplicación. No volver a redactar secuencialmente marcadores insertados.
- Para response usar el marcador existente [REDACTED_CREDENTIAL]; request completo se representa como JSON String de ese marcador, válido para request_payload TEXT. En campos JSON internos usar string marcador donde el schema lo permita, manteniendo estructura. No convertir todo report-v1/configuration_snapshot en un string que viole su contrato.
- Reinspeccionar el candidato saneado y su serialización con la misma política y scope: no deben quedar equivalentes. Marcador es una constante del scanner, no evidencia del target. Si un valor protegido coincide con el marcador o no puede garantizarse salida segura, omitir contenido/error según la sección6; nunca exceptuar cualquier aparición del marcador aportada por target.

### 6. Fail-closed y error semantics sin migración

| Situación | Resultado |
| --- | --- |
| SAFE | Mantener texto/estado técnico y flags previos; ninguna canonicalización del payload enviado |
| MATCH con inspección completa | Redactar según sección5, flags correspondientes true; conservar HTTP/latencia/error de transporte y ejecución COMPLETED si cumple lifecycle. Evaluador recibe texto ya saneado; reglas no confunden el marcador con señal positiva |
| UNINSPECTABLE en request/response | Suprimir campo completo; ExecutionError.EVIDENCE_LIMIT_EXCEEDED existente, status FAILED. Mantener HTTP/latencia seguros y demás metadata. Cuerpo marcador+flag true si cabe; si no cabe, null+flag false conforme a checks, con causa explícita. Evaluación sigue EVALUATION_ABORTED/FAILED por flujo vigente, sin llamadas/reservas externas |
| Transporte falla con safeRequest antiguo | Revalidar safeRequest con scope actualizado; conservar error original cuando el contenido se puede suprimir con seguridad; si inspección incompleta fue causa adicional usar EVIDENCE_LIMIT_EXCEEDED de manera determinista, sin mensaje libre |
| Copia secundaria no inspeccionable | No entregar el campo dudoso al repositorio. Suprimir proyección opcional con evidence.complete=false/redacted=true; si snapshot/campo obligatorio no puede mantenerse válido sin alterar decisión/identidad, abortar transacción y usar compensación existente. Nunca reparar FK, criterio, veredicto o score para hacer pasar schema |

Estado interno de inspección (COMPLETE/REDACTED/INCOMPLETE y motivo fijo) viaja solo en el resultado seguro/handle; no añade columnas ni enum REST. Incompletitud durable observable usa error_reason existente en TestExecution y evidence_complete/error/outcome existentes donde aplique. Redaction flags no son por sí solos certificación de inspección completa. V4 impone cuerpo no null si su flag es true; no diseñar null+flagtrue. Límite extremo que no admite ni marcador usa null/false y motivo de fallo, no contenido parcial.

Las reglas hybrid-v1/v2, scoring-v1 y report-v1 no cambian. Más redacción puede reducir evidencia o causar FAILED bajo incertidumbre: ese cambio de resultado de privacidad es explícito, no una regresión que justifique guardar datos dudosos. Si una señal independiente intacta sigue siendo suficiente, la política vigente conserva su decisión local. No introducir nuevo camino semántico sobre originales ocultos. MATCH no concede exportación: autorización/corpus/minimización siguen obligatorios.

### 7. Integración de puertos y writes

Scope empieza al preparar el trabajo y se completa en los puntos autorizados de descifrado/captura; nunca descifrar antes de autorización para facilitar la redacción. Un DurableEvidenceProtectionPort de aplicación produce un resultado interno certificado con campos seguros/flags/estado. ExecuteSecurityTestsUseCase lo exige entre TargetAdapter/LegacyInvocationResultProjection y executions.finish; el adaptador de persistencia solo acepta el candidato protegido (tipo interno o contrato equivalente explícito, sin escape por flags sueltos).

Reusar el mismo guard temprano dentro del transporte/InteractionSession para capturas y catches, y después aplicar guard final tras reunir local-only; no duplicar algoritmos. Idempotencia de la protección permite esa defensa en profundidad. El guard del repositorio/candidato serializado funciona antes de entity.replace/persist o JDBC binding, no reabre red ni vuelve a descifrar. Failed persistence conserva aborto inmediato/no reinvocación actual.

Evaluación: preparar snapshot/configuración únicamente con datos sintéticos permitidos; no mezclar registry. Revalidar claim/finish y salida sanitizada del evaluador, metadatos incluidos; snapshots/hash se producen después de la validación. Si un campo obligatorio clasificado no persistible no puede omitirse sin invalidar contrato/hash, rechazar, no cambiar silenciosamente el contexto de decisión. Finding se deriva de evaluación ya segura y se confirma en su misma transacción. Score/report usan únicamente esas fuentes y candidato final validado; scope agregado sigue disponible hasta su commit/compensación. Tras recuperación sin scope no se certifica una escritura de evidencia nueva desde un raw cache ausente.

Request/response cubren REST_LEGACY, OPENAI_COMPATIBLE, JSON, salida reconstruida SSE/JSONL y resultado final de workflows; no cambian prompts, bytes de red, extracción, auth/DNS/TLS/SSRF, límites de transporte ni cierre de recursos. También se revisan safeRequest previo, fallos de bootstrap/pasos, capturas nuevas/rotadas, bodies de error (no se introducen ahora), aliases, IDs y logs de observaciones. No persistir chunks ni raw eventos para «facilitar» normalización.

### 8. Límites y rendimiento

Reusar cotas existentes sin ampliarlas:

| Cota | Decisión |
| --- | --- |
| Contenido response a inspeccionar | ExecutionLimits.textBytes: default65536, máximo262144 bytes UTF-8; nunca inspeccionar solo prefijo |
| Request | ExecutionLimits.requestBytes, máximo65536; cap combinado evidenceBytes default131072 y scanEvidenceBytes default524288 después de serializar |
| Evaluación | EvaluatorSettings.recordBytes máximo65536; evidenceBytes máximo16384 e inputBytes máximo24576 permanecen |
| Informe | ReportJson.MAX_BYTES y check DB262144 bytes, wrapper propio separado512 |
| Capas | 4, extraído de MAX_EMBEDDED_JSON_LAYERS del projector; capa5 detectada como exceso, no fallback literal |
| Profundidad de JSON del inspector | 32, reusa EvaluationJson; privacidad puede suprimir evidencia más profunda sin cambiar parser/protocolo que hoy admite64/1000 |

Nuevas cotas explícitas para trabajo que antes no tenía contrato transversal: máximo512 valores únicos protegidos por Scan, bytes de valores sumados <=min(scanEvidenceBytes,524288), máximo4096 nodos/candidatos y4096 intervalos por candidato durable. Se justifican como topes de memoria/trabajo conservadores para cuatro tests y hasta seis pasos, no como cobertura universal de todos los registros posibles; exceso nunca trunca el conjunto, invalida inspección. Campos que superen esas cotas se suprimen incluso si su tamaño de red era válido. Añadir pruebas de límite/límite+1 para cada cota nueva.

Decoding una sola vez por candidato/capa, sin combinatoria2^n de Unicode parcial. Bytes acumulados de vistas por campo <=5*B (incluye original); presupuesto equivalente para conjunto protegido. Deduplicar; procesamiento de documentos suma B, no B por cada contenedor ancestro. Matching literal puede conservar búsqueda determinista por valor con complejidad O(P*(D+1)*B+matches), P<=512 y D=4, y fusión lineal/ordenada de intervalos. Memoria acotada por B+bytes(P)+4096 metadatos, con vistas transitorias; no retain de todas las ramas. Comprobar ScanBudget/interrupción durante recorrido y entre patrones; vencimiento usa motivos actuales y omite contenido. No regex con backtracking, normalización recursiva ilimitada ni llamadas externas para clasificar.

### 9. Plan de pruebas y PostgreSQL

Matriz T01–T28 de tasks.md cubre el mínimo solicitado. Añadir controles negativos para prosa/JSON benignos, Unicode surrogate válido/NUL, exactCase del valor, marker collision, branches duplicadas, mismo secreto en key/value/número, límites bytes/candidatos/patrones/matches y scopes concurrentes. Generar entradas como strings runtime/fixtures para no confundir escapes de Java con los bytes que recibe el redactor. La expectativa es inspección equivalente, no solo comprobar que contiene un marcador.

Differential tests: durable SAFE implica que la primitiva común no encuentra protegido ni ambigüedad; export no puede aceptar MATCH/UNINSPECTABLE del mismo candidato/P. Un export veto puede ser más estricto por permisos/corpus/spanOUTPUT_ONLY. Mantener veto LLM10 y coincidencia exacta de corpus; compartir decoder no convierte privacidad en regla de vulnerabilidad.

Integración obligatoria sobre PostgreSQL16 aislado con ScanTestDatabase y variables/guardas de suites existentes, sin H2, sin proveedor real. No ejecutar ahora. Fixtures REST/chat, SSE y JSONL reconstruidos, sesión/cookie/CSRF/headers/workflow y observaciones JSON/stream; incluir éxito redactado y abortos/límites, no solo fallos que evitan toda persistencia. Confirmar transacciones reales y después leer con conexión JDBC nueva, fuera de transacción de test y sin caché EntityManager.

Inspeccionar físicamente todas las columnas TEXT/VARCHAR/JSONB de test_executions, evaluation_results, findings, scan_scores, security_reports y las filas del fixture en scans/projects/test_cases/target_configurations; ciphertext se valida como cifrado/no copia en claro, no se decodifica como evidencia. Para cada sentinel único, buscar literal, variantes conocidas y equivalencia mediante oracle de test independiente (parser/decoder de fixture), incluida una decodificación propia de response_payload y recorrido de JSONB/strings anidados. Un SQL LIKE literal solo no acredita ausencia de equivalentes.

Reabrir application context con mismos datos tras commit y scope liberado; leer repositorios/DTO/report e inspeccionar JDBC otra vez, sin volver a invocar target/evaluador. Verificar cero copias recuperables, flags/errores correctos, ausencia de scopes reconstruidos y lecturas históricas intactas. Probar rollback de escritura secundaria/score-report y error de persistencia: nada dudoso llega a binding SQL ni a commit previo, sin reenvíos. Nuevos datos saneados permanecen seguros tras lectura/re-serialización; fixtures históricas previas siguen formato/lectura sin afirmar limpieza de su contenido.

## Risks / Trade-offs

- P1 registro protegido incompleto o liberado antes de un write → captura temprana, señal incompleta, handle efímero y liberación tras commit/compensación.
- P1 serialización crea copia no inspeccionada o falso SAFE por D+1 → guard en representación final, oracle independiente y pruebas físicas tras commit.
- P2 falsos positivos de texto con escapes/supresión completa → conservar metadata, controles benignos y límites documentados; privacidad prevalece sobre riqueza de evidencia.
- P2 redacción borra evidencia útil → reglas usan solo saneado; no evaluar originales locales por un bypass nuevo; preservar señales independientes según política vigente.
- P2 schema/hash de snapshot no admite marcador → no corromper contrato; abortar write y compensar, sin migración implícita.
- P3 duplicación futura de equivalencia → versión común y differential tests; nuevos formatos requieren cambio explícito.

## Migration Plan

NOT REQUIRED. Reutilizar flags/motivos/campos y codecs actuales, sin editar Flyway ni backfill. Implementar prospectivamente por capas internas, con guard activo antes del primer nuevo write; no ventana donde el projector ya cambió pero durable sigue débil. Documentar metodología de redacción, frontera en modelo de datos y seguridad de observaciones al implementar. No modificar changes archivados ni datos históricos. Restaurar una versión anterior con la vulnerabilidad no es rollback de privacidad aceptable; ante fallo deshabilitar nuevas ejecuciones por mecanismo operativo existente, sin diseñar despliegue en este change.

## Open Questions

Ninguna decisión material necesaria para redactar/implementar este alcance. Las cotas nuevas y la abstención técnica bajo UNINSPECTABLE quedan propuestas explícitas para revisión en este change; no son constantes ya implementadas. Limpieza de históricos/backups y formatos reversibles distintos de JSON requieren autorización/cambio separado, no bloquean la protección prospectiva.
