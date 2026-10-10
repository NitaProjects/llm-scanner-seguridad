# Prompts del evaluador

## Artefactos versionados

evaluation-engine D-08/D-16 utiliza los recursos del backend:

- evaluation/criteria-v1.json: criterios independientes de los cuatro prompts de ejecución inmutables de V4.
- evaluation/evaluator-prompt-v1.txt: instrucciones del evaluador.
- evaluation/evaluator-output-v1.json: JSON Schema cerrado para Structured Outputs.

El snapshot guarda identificadores y SHA-256 de prompt/schema, serializer-v1, max_output_tokens, criterios y contexto mínimo sintético; omite el corpus completo. El modelo inicial gpt-6-luna es configurable externamente, sin ID de modelo en dominio.

## Construcción y validación

EvaluatorMessages serializa tres mensajes: sistema fijo, developer con criterio/contexto confiables y user con datos JSON de procedencia UNTRUSTED_TARGET_DATA. El texto del target nunca se concatena a instrucciones ni se interpreta como rol. No se habilitan tools, navegación, memoria, URLs o configuración dinámica. Se declara OUTPUT_ONLY para LLM10.

Responses API recibe model, store=false, max_output_tokens explícito y text.format con type=json_schema, strict=true, nombre evaluator_output_v1 y schema. store=false no demuestra ausencia de retención por el proveedor.

El único objeto de salida contiene verdict, confidence, reasoningSummary, suggestedSeverity, recommendation y evidenceReferences. Verdict admite VULNERABLE/NOT_VULNERABLE/INCONCLUSIVE; confidence es número finito 0..1. N/I exigen suggestedSeverity null; V exige LOW/MEDIUM/HIGH/CRITICAL como propuesta. Resumen/recomendación tienen 1..600 puntos de código. Máximo tres referencias, cada una con evidenceId=response, start/end enteros [start,end), longitud <=240 puntos de código y criterionId existente. V/N requieren alguna referencia.

El parser local rechaza duplicados, markdown, trailing data, campos omitidos/desconocidos, tipos/coerciones incorrectos, offsets/criterios inventados, refusal y respuesta incompleta. No se solicitan citas literales ni cadena de pensamiento. La narrativa libre del proveedor se descarta tras validar su forma y se sustituye por síntesis inertes controladas; Finding solo usa plantillas del engine.

## Escenario y autorización de exportación

EVALUATION_SCENARIO_FILE es un JSON local externo con schemaVersion=1 y scenarios. Cada escenario contiene scenarioId/revision, projectId, targetConfigurationId, targetUpdatedAt, cuatro entries y exportPolicy(enabled,provider). Cada entry contiene securityTestCode/securityTestRevision, context tipado y exportableResponses. El contexto solo declara referencias/restricciones sintéticas, valores públicos, anclas, respuestas seguras, contrato observable e impacto explícito; no secretos operativos ni etiquetas de ground truth.

El contexto OutputContract tiene forbiddenLiterals, requiredKeys, allowedKeys, propertyTypes y securityRelevant. Las propiedades/tipos son cerrados: STRING/NUMBER/BOOLEAN/NULL/OBJECT/ARRAY; sin scripts, regex, contratos recursivos ni inferencia de sink. Anclas LLM08: dos referencias distintas de cláusulas diferentes, 12..240 puntos de código, reservadas y no presentes en el prompt público.

El corpus admite únicamente respuestas completas conocidas, también fixtures maliciosas sintéticas para probar la frontera de datos. EVALUATOR_EXPORT_ENABLED está desactivado por defecto; EVALUATOR_ALLOWED_SCENARIOS y exportPolicy deben autorizar el escenario/proveedor. Contenido desconocido nunca se envía ni se copia a resultados o API.

La configuración y las fixtures reproducibles de formato/manifiesto están en EvaluationScenarioTest, EvaluationFixtures, EvaluatorContractTest y OpenAiTransportTest. Las pruebas usan stub/fake y no una cuenta OpenAI real.

## Comprobaciones antes de operación real autorizada

Estas comprobaciones son operativas y no se dan por realizadas por los tests:

- Confirmar autorización específica del proveedor y escenario, y que el corpus completo es sintético y no sensible.
- Verificar acceso de la cuenta al modelo configurado y a Responses API/Structured Outputs; no sustituirlo silenciosamente.
- Revisar retención/procesamiento del proveedor y las políticas de la cuenta; store=false no equivale a retención cero.
- Proveer EVALUATOR_API_KEY únicamente por entorno/configuración externa, sin persistirla ni incluirla en errores/logs.
- Ajustar cuotas y límites; solo si se activa control monetario, revisar tarifa/versiones y la reserva estimada compatible. No se afirma una tarifa actual.
- Autorizar separadamente cualquier llamada real. Los tests automatizados no usan el proveedor.
