# durable-secret-protection Specification

## Purpose
Impedir prospectivamente que secretos o datos conocidos no persistibles sobrevivan en evidencia durable mediante equivalentes JSON/Unicode reversibles, con protección previa a commit y semántica común con el veto de exportación.

## Requirements

### Requirement: DSP-01 Equivalentes protegidos fuera de persistencia

**Historia:** Como operador quiero que clasificar un valor como no persistible proteja también sus representaciones equivalentes. **Prioridad:** P1. **Dependencias:** contratos actuales de ejecución y target-retrieval-tool-observations.

Para todo valor conocido clasificado credential/secret, variable sensible, cookie/CSRF/credencial dinámica, retrieval/tool local-only o evidencia explícitamente no persistible, un campo que contenga su equivalente bajo el perfil soportado SHALL NOT entregarse a una escritura durable sin sustitución segura. La propiedad SHALL ser protectedEquivalent_N4(content,P) ⇒ imposibilidad de persistir el contenido protegido original/equivalente. UNINSPECTABLE SHALL NOT tratarse como SAFE. La protección SHALL actuar antes de persist/update y del primer commit, no depender del evaluator/export posterior. SHALL cubrir request/response y copias secundarias, sin guardar el conjunto protegido ni hashes de sus valores.

#### Scenario: Unicode parcial en respuesta
- **WHEN** el valor sintético PRIVATE_42 está protegido y la respuesta incluye una cadena con su primera P representada por el escape Unicode u0050 precedido de barra invertida
- **THEN** no se persiste una representación recuperable de PRIVATE_42 en la ejecución ni sus copias
- **AND** no basta que el literal esté ausente en el texto original

### Requirement: DSP-02 Perfil N4 común y acotado

**Historia:** Como mantenedor quiero una definición única de equivalencia que no permita bypass entre canales. **Prioridad:** P1. **Dependencias:** DSP-01 y protección de exportación vigente.

Persistencia, detección local-only y exportability SHALL compartir semántica/versionado de inspección para escapes JSON de comillas/barra invertida/slash/b/f/n/r/t y Unicode uXXXX con dígitos hex de ambas cajas, combinaciones parciales/múltiples, Unicode válido y strings JSON anidados/repetidos. SHALL inspeccionar claves/valores y ocurrencias incrustadas. SHALL usar vistas solo para privacidad, sin transformar prompts/payloads de red ni respuestas en prueba de vulnerabilidad.

SHALL existir máximo cuatro capas de decoding por camino, incluyendo parse de strings embebidos y escapes bajo un mismo presupuesto, con matching en capas0..4 y estabilidad obligatoria al límite. Una capa adicional posible SHALL resultar UNINSPECTABLE, no fallback literal. Envoltorios propios del schema SHALL verificarse mediante su codec, sin añadir presupuesto atacante ni omitir inspección. Corpus exacto/permisos/minimización y veto OUTPUT_ONLY existentes SHALL mantenerse; pasar privacidad SHALL NOT conceder exportación. Regex o enumeración combinatoria de variantes SHALL NOT sustituir esta semántica.

#### Scenario: Cuatro capas y quinta capa
- **WHEN** el fixture runtime aplica cuatro o cinco encodings JSON sucesivos al mismo valor protegido
- **THEN** cuatro capas permiten verificar/redactar y cinco capas obligan a suprimir contenido como inspección incompleta
- **AND** ambos consumidores comparten el resultado de privacidad para el mismo candidato y conjunto protegido

#### Scenario: Secreto con controles y Unicode
- **WHEN** un valor protegido con controles/comillas/slashes/surrogate válido reaparece mediante escapes JSON válidos o hexadecimal mixto
- **THEN** se detecta la equivalencia sin cambiar el case del valor ni convertir NUL en contenido SQL literal

### Requirement: DSP-03 Fail closed ante ambigüedad o exceso

**Historia:** Como operador quiero preservar metadata sin conservar contenido dudoso. **Prioridad:** P1. **Dependencias:** DSP-01/02 y lifecycle existente.

Escape malformado/truncado, Unicode inválido, JSON embebido ambiguo, claves duplicadas, scope incompleto o exceso de cotas SHALL suprimir el campo completo, nunca aceptar un prefijo o el subconjunto de valores que cupo. En request/response no inspeccionables SHALL emplearse el error existente EVIDENCE_LIMIT_EXCEEDED y cierre FAILED, conservando HTTP/latencia seguros; evaluación/Scan SHALL seguir abortos/compensaciones vigentes sin llamadas/reservas externas. MATCH completamente inspeccionado SHALL permitir redacción y conservar estado de transporte válido, flags correctos y evaluación sobre evidencia saneada según políticas vigentes.

UNINSPECTABLE SHALL prevalecer sobre MATCH/SAFE: un literal encontrado SHALL NOT terminar anticipadamente la inspección y dejar sin clasificar otra rama/capa antes de conservar el resto del campo.

Marcador SHALL ser [REDACTED_CREDENTIAL], con codificación JSON válida en request cuando corresponda. Si no cabe, contenido SHALL ser null y flag false conforme al schema, con motivo de fallo explícito; SHALL NOT almacenarse null con flagtrue. Copias opcionales dudosas SHALL omitirse con estado de evidencia incompleta; campos obligatorios no saneables sin invalidar contrato SHALL abortar la escritura, no alterar identidad/decisión/FK para salvarla. No se introducirán nuevos enums públicos/columnas para este incremento.

#### Scenario: Escape incompleto o valor descartado
- **WHEN** aparece un escape incompleto o una observación sensible se descarta sin conservar valor completo para inspección
- **THEN** no se persiste el texto candidato y se registra el fallo vigente con metadata segura
- **AND** ni una compensación ni el error de parser incluyen contenido protegido

### Requirement: DSP-04 Overlap y redacción idempotente

**Historia:** Como operador quiero que secretos solapados no dejen fragmentos recuperables. **Prioridad:** P1. **Dependencias:** redacción por intervalos vigente.

Matches SHALL calcularse sobre el original inmutable, fusionando intervalos solapados/adyacentes antes de aplicar una sola transformación. Equivalencia no literal SHALL permitir sustitución completa del campo como intervalo original total, no redacción secuencial ni traducción de offsets no verificada. El candidato saneado y serializado SHALL volver a verificarse con el scope completo; un marcador que colisiona con un valor protegido SHALL NOT exceptuarse por su texto. Controles benignos SHALL conservar contenido cuando se acredita SAFE, sin censura indiscriminada de JSON válido.

#### Scenario: Secretos con prefijo y sufijo compartidos
- **WHEN** varios valores protegidos se solapan por prefix/suffix en el texto o uno aparece en forma codificada
- **THEN** el resultado no conserva fragmentos por orden de sustitución y la nueva inspección no encuentra equivalentes

### Requirement: DSP-05 Scope efímero de credenciales y observaciones

**Historia:** Como operador quiero igual protección de secretos del transporte y fuentes local-only. **Prioridad:** P1. **Dependencias:** sesiones/workflows/headers/observaciones existentes.

El conjunto protegido SHALL incluir valores originales conocidos de credenciales descifradas tras autorización, variables sensibles, CSRF, cookies activas/retiradas, headers/credenciales dinámicos efectivamente usados y contenido/aliases/IDs no persistibles de retrieval, tool arguments y tool results. SHALL capturarse antes de descartar/sanear/liberar esos originales, revalidando safeRequest previo cuando se descubran valores nuevos. Copias/derivados declarados de esos valores en FINAL_OUTPUT u otras fuentes SHALL seguir protegidos. Información desconocida/incompleta SHALL forzar abstención, no universo parcial asumido completo.

Scope SHALL ser efímero, aislado por ejecución/Scan, disponible para guardas posteriores y liberado tras cierre/compensación, sin serialización ni acceso cruzado entre scans. Reinicio SHALL mantener recuperación existente sin reconstruir scopes mediante descifrado/reinvocación. Se preserva el almacén cifrado autorizado credential_ciphertext; no se considera una excepción para copiar secretos descifrados en evidencia. Canarios sintéticos permitidos SHALL NOT clasificarse automáticamente como credenciales; clasificación explícita no persistible SHALL prevalecer.

#### Scenario: Local-only reaparece codificado
- **WHEN** retrieval/tool argument/tool result o un ID local-only conocido reaparece en la respuesta final con Unicode/JSON anidado
- **THEN** la copia queda excluida de persistencia y exportación aunque provenga de otra fuente

#### Scenario: Cookie capturada después de preparar request
- **WHEN** una respuesta captura/rota una cookie o CSRF y el request conservado contiene su equivalente
- **THEN** el candidato request se revalida con el conjunto actualizado antes de guardarse, también en el camino de error

### Requirement: DSP-06 Writes y copias secundarias seguros

**Historia:** Como mantenedor quiero que proteger TestExecution no deje filtraciones posteriores. **Prioridad:** P1. **Dependencias:** DSP-01 a DSP-05, evaluación/scoring/report vigentes.

La frontera SHALL exigir evidencia protegida antes de request_payload/response_payload, claim/finish de evaluación, Finding y cierre score/report. SHALL comprobar campos libres y JSON/strings de evidence_projection, deterministic_signals, llm_assessment, configuration_snapshot, sampling_parameters, resúmenes/recomendaciones, metadata externa y payload report-v1, además de las serializaciones finales. Findings SHALL derivarse de la misma evaluación segura y confirmarse atómicamente con ella. Hashes SHALL calcularse solo sobre proyección/configuración ya segura; SHALL NOT persistirse hashes de valores local-only como sustituto de redacción.

No se crearán tablas/contextos/observaciones durables nuevos ni copias de cuerpos/eventos/chunks/errores brutos. Los campos heredados evaluator_result/confidence SHALL seguir sin escrituras nuevas. Score/report SHALL conservar contratos/atomicidad y usar fuentes seguras; si no se puede cumplir un write requerido, SHALL abortar y compensar sin persistir dudosos ni reenviar target. Logs/exception messages SHALL excluir valores/candidatos, admitiendo solo motivos fijos y metadata segura.

#### Scenario: Fallo posterior a ejecución saneada
- **WHEN** una proyección o snapshot posterior contiene un equivalente protegido o no puede inspeccionarse
- **THEN** no llega a binding SQL ni commit; se suprime lo opcional o se aborta el write obligatorio según contrato
- **AND** no reaparece en Finding/report ni se reinvoca el target para compensar

### Requirement: DSP-07 Cotas deterministas

**Historia:** Como operador quiero una protección que no pueda causar trabajo ilimitado. **Prioridad:** Alta. **Dependencias:** límites existentes y DSP-02/03.

La inspección SHALL respetar response text default65536/máximo262144 bytes UTF-8, request máximo65536, caps actuales por ejecución/Scan, evaluación máximo65536 y report máximo262144, aplicando la cota configurada más estricta y contando bytes después de serializar. SHALL limitar decoding a4, profundidad JSON de inspección a32, valores protegidos únicos a512 por Scan y suma de sus bytes a min(scanEvidenceBytes,524288), nodos/candidatos e intervalos a4096 por candidato durable y vistas acumuladas a5*B por campo. Exceso SHALL invalidar inspección sin truncar ni exportar/persistir prefijos. SHALL respetar presupuesto/interrupción, sin búsquedas combinatorias ni regex con backtracking.

#### Scenario: Límite y límite más uno
- **WHEN** fixtures alcanzan exactamente cada cota y después la superan en una unidad
- **THEN** el límite permite inspección si es completa y el exceso suprime contenido con motivo vigente, sin trabajo ni memoria no acotados

### Requirement: DSP-08 Compatibilidad prospectiva

**Historia:** Como operador quiero mantener integraciones y lectura histórica sin garantías falsas. **Prioridad:** Alta. **Dependencias:** adapters, hybrid-v1/v2, scoring-v1 y report-v1.

REST_LEGACY, OPENAI_COMPATIBLE, JSON/SSE/JSONL, sesiones/workflows/cookies/headers/CSRF y observaciones SHALL usar la misma protección sin modificar prompts, payloads de red, extracción, autorización/DNS/TLS/SSRF, cuotas, cierres ni protocolos. Evaluación SHALL consumir el saneado confirmado, nunca originales por un bypass nuevo; redacción SHALL NOT convertirse en prueba de vulnerabilidad. Formatos, schema PostgreSQL e históricos SHALL conservarse; no se editarán migraciones ni limpiarán automáticamente históricos/backups. La garantía SHALL declarar alcance a nuevos writes y representaciones soportadas conocidas, sin prometer detección universal o saneamiento retrospectivo.

#### Scenario: JSON y streams con el mismo contenido final
- **WHEN** JSON, SSE y JSONL producen el mismo texto protegido equivalente
- **THEN** la persistencia aplica la misma decisión de privacidad a su resultado reconstruido, sin guardar chunks ni cambiar tráfico

### Requirement: DSP-09 Validación física PostgreSQL y diferencial

**Historia:** Como mantenedor quiero evidencia del P1 cerrado tras commit, no solo mocks. **Prioridad:** P1. **Dependencias:** DSP-01 a DSP-08.

La aceptación SHALL exigir la matriz T01–T28 de tasks.md, controles benignos, límites exactos/exceso, solapes y tests diferenciales de inspector durable/export/local-only. Integración SHALL usar PostgreSQL16 aislado y nuevas conexiones postcommit, inspeccionando físicamente TEXT/VARCHAR/JSONB de test_executions/evaluation_results/findings/scan_scores/security_reports y tablas relacionadas del fixture, sin limitarse a LIKE literal. Oracle independiente SHALL recorrer serializaciones/strings JSON y buscar literal/equivalentes protegidos conocidos. SHALL validar también escrituras exitosas redactadas, no solo abortos.

La prueba SHALL cerrar/reabrir aplicación/repositorio cuando libere scopes/cache, releer y comprobar ausencia de equivalentes/copia secundaria y no reinvocación. SHALL cubrir rollback, fallo de persistencia, redacción previa al primer commit, score/report e históricos. Cifrado autorizado SHALL comprobarse como tal sin confundirlo con evidencia en claro. Pruebas SHALL usar valores sintéticos, no Internet ni producción, y ejecutarse solo con autorización; este diseño no acredita haberlas ejecutado.

#### Scenario: Commit y reapertura
- **WHEN** un Scan sintético persiste resultados saneados y se reabre el contexto con nueva conexión tras commit
- **THEN** ninguna columna de evidencia/copia secundaria contiene literal ni equivalente recuperable según el oracle independiente
- **AND** metadata, flags, errores y lectura histórica conservan contrato, sin nuevos requests
