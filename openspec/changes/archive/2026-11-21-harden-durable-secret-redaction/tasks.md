## 1. Inspector común y resultado de protección

- [x] 1.1 Extraer semántica N4 compartida del decoder de EvaluationEvidenceProjector y endurecer Unicode/errores/nested JSON con resultados SAFE/MATCH/UNINSPECTABLE, sin dependencias de infraestructura en dominio; verificar casos benignos, escapes parciales, cuatro capas, quinta capa y precedencia de inspección incompleta sobre match temprano. [DSP-01/02/03]
- [x] 1.2 Aplicar cotas de bytes/profundidad32/capas4/patrones512/candidatos4096/intervalos4096 y vistas5*B con presupuesto/interrupción; verificar límites y límite+1, memoria/trabajo acotados y ningún fallback a prefijo/subconjunto, sin regex combinatoria. [DSP-07]
- [x] 1.3 Reutilizar fusión de intervalos originales y sustitución única, con campo completo para equivalencia solo decodificada y verificación de salida serializada; verificar overlap, prefix/suffix, idempotencia, marker collision y schema de request/response. [DSP-04]

## 2. Scope efímero y frontera de ejecución

- [x] 2.1 Añadir scope/handle opaco por Scan/ejecución y alimentar originales protegidos en descifrado autorizado/capturas/cookies rotadas/headers/CSRF/observaciones antes de descartar o cerrar; verificar cobertura de localValues/IDs/aliases, aislamiento entre scans, exceso/incompleto y liberación en commit/fallo/interrupción, sin persistir registry ni logs sensibles. [DSP-05]
- [x] 2.2 Introducir DurableEvidenceProtectionPort y candidato seguro exigido entre TargetAdapter/proyección legacy y executions.finish; verificar que ningún bypass puede escribir resultado raw mediante flags y que el guard actúa antes de entity.replace/persist/binding, conservando fallo de commit sin reinvocación. [DSP-01/06]
- [x] 2.3 Reusar inspector/redactor en TargetJsonProcessor/InteractionSession y guard final de HttpTargetInvocationAdapter, incluidos safeRequest en catches y valores capturados después; verificar REST/chat/JSON/SSE/JSONL/workflows, mismos bytes enviados/extracción/SSRF/DNS/TLS y recursos cerrados. [DSP-05/08]
- [x] 2.4 Implementar fail closed con marcador/flags o null/false si no cabe, EVIDENCE_LIMIT_EXCEEDED bajo incertidumbre y compensación actual; verificar checks V4, metadata segura, estado FAILED/evaluación abortada y cero llamadas/reservas externas, sin nuevas columnas/enums ni mensajes libres. [DSP-03]

## 3. Exportación y copias secundarias

- [x] 3.1 Sustituir definición duplicada de equivalencia en projector/local-only por inspector común, conservando autorización/corpus exacto/veto LLM10 y política híbrida; verificar diferencial del mismo candidato/P y que una normalización de privacidad no autoriza exportación ni modifica evidencia de vulnerabilidad. [DSP-02/08]
- [x] 3.2 Proteger candidatos serializados de prepare/claim/finish de evaluación y metadata externa; verificar proyecciones/signals/assessment/snapshots/resúmenes seguros, hashes sobre saneado, omisión opcional/incompletitud y aborto de campo obligatorio sin corromper identidad/contexto/decisión ni schema. [DSP-06]
- [x] 3.3 Mantener Finding derivado de la misma evaluación ya segura y commit conjunto; añadir guard de candidato final score/report usando scope vigente, sin nueva evidencia libre; verificar rollback atómico, proyecciones report-v1 válidas y ausencia de copias en payload/campos de Finding, sin modificar fórmula/scoring ni snapshot histórico. [DSP-06/08]

## 4. Matriz mínima de fixtures

- [x] 4.1 Cubrir T01 secret literal; T02 JSON escaped secret con comillas/backslash/slash y b/f/n/r/t; T03 Unicode first char; T04 múltiples Unicode; T05 hex lowercase/uppercase. Verificar ausencia de equivalente en salida con oráculo de fixture independiente y controles sin secreto. [DSP-01/02]
- [x] 4.2 Cubrir T06 nested JSON string; T07 double encoded JSON; T08 límite exacto encode^4; T09 encode^5 fail closed; T10 malformed escape fail closed. Verificar entradas runtime (no escapes consumidos por compilador), wrapper DB separado y ninguna rama ignorada tras match literal. [DSP-02/03]
- [x] 4.3 Cubrir T11 overlapping secrets y T12 prefix/suffix overlap; verificar todos los órdenes de inserción de valores y transformación única sin filtrado parcial ni nuevos matches del marcador. [DSP-04]
- [x] 4.4 Cubrir T13 local-only retrieval encoded; T14 tool argument encoded; T15 tool result encoded; T16 sensitive session variable encoded; T17 CSRF encoded; T18 cookie encoded. Verificar originales/IDs/aliases y copias cruzadas en FINAL_OUTPUT/request/error, incluidos campos descartados/capturas rotadas. [DSP-05]
- [x] 4.5 Cubrir T19 JSON response; T20 SSE reconstructed; T21 JSONL reconstructed; T22 request body; T23 response body; T24 error path. Verificar política de privacidad equivalente, safeRequest revalidado, cuerpo no truncado y requests/conexiones iguales salvo contenido de evidencia omitido. [DSP-03/08]
- [x] 4.6 Cubrir T25 evidence persistence (projection/signals/assessment/snapshot/Findings/report); T26 evaluator projection consistency; T27 report/scoring regression; T28 historical regression. Verificar que no se introducen copias reversibles en ningún write ni nuevas llamadas externas, y que casos benignos conservan resultados/formatos. [DSP-06/08/09]
- [x] 4.7 Añadir controles de Unicode surrogate/NUL válidos, surrogate inválido, claves duplicadas, secreto numérico en key/value, límite de cada cota nueva, marker collision, scans concurrentes y fallo de persistencia; verificar motivos/flags y terminación acotada, con sintéticos no operativos. [DSP-03/04/05/07]

## 5. PostgreSQL obligatorio y reapertura

- [x] 5.1 Añadir integración con ScanTestDatabase/PostgreSQL16 dedicado para éxitos redactados y fallos por incertidumbre en REST/chat/stream/session/workflow/observaciones; verificar commit real antes de inspección, restricciones V4/V5/V7 sin migración y nada dudoso en parámetros SQL/logs aun si transacción revierte. No ejecutar DB ni migraciones sin petición explícita. [DSP-03/06/09]
- [x] 5.2 Implementar auditoría JDBC postcommit con conexión independiente de todas las columnas textuales/JSONB del fixture: test_executions (incluido legacy), evaluation_results, findings, scan_scores, security_reports y tablas relacionadas; verificar sentinels literales/variantes y decoding independiente recursivo de JSON/strings, no solo LIKE ni reutilizar únicamente el inspector de producción. Comprobar ciphertext autorizado sin confundirlo con evidencia ni exponerlo. [DSP-09]
- [x] 5.3 Cerrar/reabrir contexto con mismos datos, sin scope/cache, releer repositorios/DTO/report y repetir auditoría física; verificar ausencia recuperable tras serialización/lectura, sin reinvocación, recuperación vigente de activos y formatos históricos sin backfill. Probar fallo de write secundario/score-report y aislamiento de otros scans. [DSP-08/09]

## 6. Regresión y documentación

- [x] 6.1 Mantener suites actuales de ejecución/sesión/workflows/stream/observaciones/evaluación/hybrid-v1/v2/scoring/report; verificar tests unitarios y PostgreSQL obligatorios solo cuando se soliciten expresamente, sin Internet/proveedor real ni dependencias nuevas. Registrar qué pasó y qué quedó pendiente, sin cerrar P1 por mocks solamente. [DSP-08/09]
- [x] 6.2 Actualizar al implementar docs/03-seguridad/modelo-de-amenazas.md, docs/04-ia/estrategia-evaluacion.md y docs/06-base-de-datos/modelo-datos.md en la frontera/cotas afectadas; verificar coherencia con N4, prospectividad, errores, límites/hashes/cifrado y exclusión de secretos, sin modificar documentación histórica ni prometer anonimización universal. [DSP-01/07/08]
- [x] 6.3 Revisar diff y evidencia para confirmar sin API/UI/schema/migraciones/deployment/AWS, sin cambios de políticas ni secretos reales; verificar requisitos DSP-01..09 y ejecutar openspec validate harden-durable-secret-redaction --strict con autorización vigente. Mantener P1 pendiente de implementación y validación física hasta disponer de evidencia. [DSP-09]
