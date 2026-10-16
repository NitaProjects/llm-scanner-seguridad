# Recorrido E2E local OFFLINE

Procedimiento operacional de D-01..D-24/E2E-01..E2E-24. Requiere autorización del entorno local. La ejecución observada está en [evidence/2026-10-16-005150/result.md](evidence/2026-10-16-005150/result.md); los comandos siguientes son instrucciones para otra ejecución y no acreditan por sí solos acciones realizadas.

La corrección específica F3/F4 y sus estados reales se registra por separado en [evidence/2026-10-16-f3-f4/result.md](evidence/2026-10-16-f3-f4/result.md). Los lanzamientos complementarios conservan sus propios IDs; una cabecera de un Scan nuevo no se atribuye al histórico anterior.

F3/F4 quedaron resueltos tras recibir la confirmación manual del operador sobre Network en la misma pestaña Chrome, con Keep log, salida mientras activo y más de12 s sin nuevos GET periódicos del Scan individual. Su procedencia está en f4-network-operator-confirmation.json y se distingue de los GET API de regresión y de la observación anterior del navegador integrado. No se atribuyen UUID/timestamps no proporcionados por el operador; no se ejecutan Scans para documentar el cierre.

## Preparación y servicios

Usar Java 21 y los artefactos existentes identificados por hash. No instalar dependencias ni reconstruir artefactos automáticamente. PostgreSQL existente debe estar operativo en localhost:5432 con la BD de desarrollo `llm_scanner_seguridad` y esquema preparado. Los tests automáticos que modifiquen datos solo pueden usar `llm_scanner_seguridad_test`. No borrar datos, crear instancias alternativas ni arrancar Docker desde este procedimiento.

Confirmar puertos con `Get-NetTCPConnection -State Listen` y comprobar qué proceso los ocupa. No detener procesos ajenos. El operador libera 8080 si la instancia existente no tiene configuración demostrable. Credenciales locales por el mecanismo del operador, nunca en argumentos, documentación, capturas o archivos de evidencia; no abrir `.env`.

En terminal dedicada del scanner, desde la raíz, sin clave de proveedor suministrada:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SERVER_PORT = '8080'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/llm_scanner_seguridad'
$env:TARGET_LOCAL_DEMO_ENABLED = 'true'
$env:TARGET_LOCAL_DEMO_ORIGINS = 'http://127.0.0.1:8081'
$env:EVALUATOR_EXPORT_ENABLED = 'false'
$env:EVALUATOR_ALLOWED_SCENARIOS = ''
$env:EVALUATION_SCENARIO_FILE = Join-Path (Get-Location).Path 'demo-vulnerable/manifest/evaluation-scenario.local.json'
java -jar backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8080 --spring.datasource.url=jdbc:postgresql://localhost:5432/llm_scanner_seguridad --TARGET_LOCAL_DEMO_ENABLED=true --TARGET_LOCAL_DEMO_ORIGINS=http://127.0.0.1:8081 --EVALUATOR_EXPORT_ENABLED=false "--EVALUATION_SCENARIO_FILE=$env:EVALUATION_SCENARIO_FILE"
```

El arranque requiere autorización operacional y del Flyway que pudiera aplicar migraciones pendientes. No usarlo para migrar un entorno cuya preparación no se ha confirmado. Verificar en logs no secretos la BD efectiva, profile, versión actual y validación Flyway, último esquema aplicado y puerto. No asumir V7 sin contrastar las migraciones del repositorio/artefacto. En la ejecución registrada: PostgreSQL 16.15, siete migraciones válidas y esquema 7 actualizado, sin migraciones nuevas.

En otra terminal, desde raíz:

```powershell
$env:DEMO_PROFILE = 'VULNERABLE'
$env:DEMO_BIND_ADDRESS = '127.0.0.1'
$env:DEMO_PORT = '8081'
java -jar demo-vulnerable/target/demo-vulnerable-0.0.1-SNAPSHOT.jar
```

Conservar Angular activo si responde. En caso contrario, previa autorización, en `frontend/`: `npm start -- --host localhost --port 4200`. No instalar dependencias. El proxy existente `/api/**` apunta a `http://localhost:8080`.

Preflight HTTP: GET demo `/health` devuelve UP/demo=true/VULNERABLE/scenarioVersion="1"; scanner GET `/api/projects` devuelve 200 JSON; frontend `/` devuelve HTML y `/api/projects` devuelve JSON del scanner. Usar timeouts de 10 s como máximo y cero redirects. Configuración de un proceso ajeno no se deduce de defaults del workspace. No continuar con parámetros críticos NOT_VERIFIED, exportación habilitada, binding obsoleto, servicios incompatibles o Scan activo del Project. SQL no es requisito; sus campos quedan NOT_INSPECTED si no se realiza el diagnóstico autorizado.

## Project, Target y binding

Para el recorrido completo previsto en D-09, crear Project `E2E Demo Vulnerable` desde Angular, configurar Target y capturar sus IDs por API. La preparación de esta sesión se hizo por API por petición explícita del usuario; no acredita creación/guardado de formularios UI. Reutilizar solo el id real conocido y confirmado; no elegir entre nombres ambiguos ni crear otro Project innecesariamente.

El objeto contractual está en `demo-vulnerable/manifest/benchmark-v1.json`, propiedad `targetConfiguration`: POST, URL `http://127.0.0.1:8081/api/chat`, contentType `application/json`, requestTemplate string `{"message":"{{PROMPT}}"}`, placeholder `{{PROMPT}}`, responsePath `$.response`, headers `{}`, authType `NONE`, sin authConfig y timeoutMs 5000. Guardar por POST `/api/projects/{projectId}/target`; no PUT si no hay modificación necesaria/autorizada. Releer Project/Target y conservar `updatedAt` literalmente, sin conversión a Date ni redondeo.

Tras obtener `$projectId` de la respuesta real y verificar autorización/health, preparar las copias locales ya ignoradas:

```powershell
$reply = Invoke-WebRequest "http://localhost:8080/api/projects/$projectId/target" -TimeoutSec 10 -MaximumRedirection 0
$target = ConvertFrom-Json -InputObject $reply.Content -DateKind String
$template = Get-Content demo-vulnerable/manifest/evaluation-vulnerable.template.json -Raw
$scenario = $template.Replace('REPLACE_PROJECT_ID', $projectId).Replace('REPLACE_TARGET_ID', $target.id).Replace('REPLACE_TARGET_UPDATED_AT', $target.updatedAt)
Test-Json -Json $scenario -SchemaFile demo-vulnerable/manifest/schemas/evaluation-scenario-v1.schema.json
```

Continuar solo si el schema es válido. Escribir UTF-8 sin BOM en `demo-vulnerable/manifest/evaluation-scenario.local.json`, sin sobrescribir un binding diferente silenciosamente. Completar `bindings.local.json` con el esquema `bindings-v1.schema.json`: schemaVersion=1, benchmarkVersion="1", profile=VULNERABLE, scenarioId=demo-vulnerable-v1, projectId/targetConfigurationId/targetUpdatedAt reales, checkedHealth exacto y authorizationDeclaredByOperator=true. Validar ambos esquemas y releer Target para contrastar identidad/fecha antes del Scan.

Solo se sustituyen tres placeholders: entradas, contexto, referencias y corpus permanecen exactamente como en la plantilla. Un escenario, cuatro entries revisión 1, exportPolicy.enabled=false/provider OPENAI contractual; no añadir provider NONE, etiquetas, oracle, expectedVerdict, rationale, clasificación, URL o auth. Ground truth permanece en la comparación externa posterior. Si cambia Target/perfil, regenerar binding y repetir preflight. El adaptador lee el archivo al preparar cada Scan: completar el JSON no requiere reiniciar scanner. No guardar Target de nuevo después del binding.

## Scan y recopilación

Desde ProjectPage pulsar una sola vez «Ejecutar análisis». Registrar instante del intento e id de navegación. Contrato HTTP: POST sin body, 202 y Location scoped. Ante escritura ambigua no repetir POST: identificar por histórico o detener. No fabricar estados en BD.

Angular es el observador principal: GET inmediato, ciclos encadenados separados 3000 ms tras completar el ciclo y lecturas con timeout de 10 s. No ejecutar otro poller a la vez. El operador impone 360 s desde el intento; tres errores consecutivos pausan el seguimiento existente. Terminal COMPLETED/FAILED detiene la repetición. No ralentizar una ejecución rápida para obtener capturas. Navegar fuera cancela la suscripción; probarlo durante un activo solo en un recorrido adicional autorizado, sin alargar artificialmente este Scan.

Después del terminal, recoger por GET, timeout 10 s y sin redirects, para los IDs reales:

```text
/api/projects/{projectId}/scans/{scanId}
/api/projects/{projectId}/scans/{scanId}/executions
/api/projects/{projectId}/scans/{scanId}/evaluations
/api/projects/{projectId}/scans/{scanId}/findings
/api/projects/{projectId}/scans/{scanId}/score
/api/projects/{projectId}/scans/{scanId}/report
```

Guardar solo esas respuestas públicas sintéticas, sin listados ajenos. Releer report para verificar inmutabilidad y repetir demo health. FAILED, hijos ausentes, CONTEXT_UNAVAILABLE o score/report incoherentes son fallos del caso preparado; no cambiar expectativas ni relanzar silenciosamente. Polling agotado no cambia el estado del servidor.

Expectativa offline condicionada a preparación correcta: cuatro ejecuciones COMPLETED/200 y evaluaciones COMPLETED/INCONCLUSIVE, confidence 0.00, HYB-01, SKIPPED, EXPORT_NOT_AUTHORIZED, degraded=true, errorReason/llmErrorReason/severity/findingId null. Cero Findings; AVAILABLE/scoring-v1, score0/NO_FINDINGS, cuatro inconclusos, ninguna evaluación NOT_VULNERABLE; una evaluación inconclusa por categoría. Report AVAILABLE/report-v1/es, summary literal del score, cuatro evaluaciones concordantes, Findings vacío, cuatro textos de interpretación y nueve limitaciones vigentes, fechas ordenadas y snapshot inmutable. Cero significa «no se han generado Findings»; no acredita seguridad.

Respuesta persistida, señales deterministas, número de intentos LLM y exportAuthorized del snapshot interno no se exponen por API: NOT_INSPECTED si no se realiza el diagnóstico SQL opcional D-12. No rellenarlos con fixtures, expectativas o una nueva llamada a la demo. No se necesita SQL para aceptar resultados API/UI.

## QA, comparación y conservación

Verificar listado/proyecto/Target actual sin guardar, lanzamiento y estado real, cuatro ejecuciones/evaluaciones, categorías, cero Findings/cuatro I, report/interpretación/limitaciones/trazabilidad, enlaces internos, retorno al histórico y refresh/ruta directa. Conservar hasta cuatro PNG representativas, sin HAR/logs completos. Registrar por separado acciones de formularios o cancelación activa que no se hayan ejercitado.

Para acreditar Location, conservar status y body/id de la respuesta real de lanzamiento y su cabecera Location literal. Compararla con el recurso contractual scoped y hacer GET a esa ruta; no guardar un valor reconstruido como cabecera observada. Un POST adicional requiere autorización; no repetir una escritura ambigua.

Para comprobar cancelación del polling, usar **la misma pestaña y navegador** para acciones y Network. Activar Preserve log antes de lanzar, comprobar que la carga inicial al scanId está registrada y anotar tiempos/conteo de GET. Salir de ScanPage mediante navegación mientras esté activo; permanecer al menos12 s fuera y contrastar que no comienzan nuevos GET al recurso individual, /executions o /evaluations. Una petición inicial pendiente puede terminar/abortarse tras salir; distinguir su inicio de uno nuevo. El GET /projects/{projectId}/scans del histórico es otra operación y no demuestra polling individual. Reentrar debe producir lecturas nuevas y mostrar el estado remoto real, sin POST de cancelación. Cero filas sin baseline, otra pestaña o un Scan ya terminal no acreditan esta prueba. Si la herramienta no expone Network, registrar con procedencia explícita la observación manual del operador; no reemplazarla por una inferencia de la fuente ni marcar PASS antes de recibir el resultado.

Construir después la matriz externa usando ground-truth-v1, profile/version/code/revision y el Scan real. V/V→TP, N/N→TN, N/V→FP, V/N→FN; I→INCONCLUSIVE, nunca FN. Fallos técnicos se registran aparte. LLM10 es OUTPUT_ONLY, sin sink demostrado. No precision/recall ni benchmark formal. El evaluador recibe referencias técnicas sintéticas: no es completamente ciego a ellas; etiquetas/justificaciones del oracle siguen fuera.

Frontend-mvp 9.4 exige abrir un Finding persistido real. El offline sin Findings cubre parcialmente su recorrido y conserva 9.4 pendiente; no simular un Finding ni introducir un requisito nuevo de proveedor en su texto original.

Conservar Project/Target/Scans y evidencia. No existe DELETE aprobado; no limpiar la BD. El operador revoca la excepción local cuando ya no necesite continuar y detiene únicamente procesos propios, sin Scan activo. No apagar PostgreSQL compartido ni matar procesos por scripts. En esta entrega se conservan servicios para inspección; el cierre operacional se registra pendiente.

## Evaluador real futuro: procedimiento, no ejecución

Requiere autorización independiente de proveedor/modelo/escenario/presupuesto. Mantener oracle externo, Target y binding válido; otra copia del escenario con corpus exacto y exportPolicy.enabled=true/provider OPENAI. Configurar solo en scanner EVALUATOR_PROVIDER=OPENAI, EVALUATOR_MODEL aprobado, EVALUATOR_EXPORT_ENABLED=true, EVALUATOR_ALLOWED_SCENARIOS=demo-vulnerable-v1 y EVALUATION_SCENARIO_FILE correspondiente. La clave se suministra únicamente por EVALUATOR_API_KEY mediante mecanismo seguro del operador, nunca en documentación, Target, frontend o evidencia. Reiniciar configuración de proceso solo sin activo y con autorización.

Mantener límites existentes: máximo cuatro intentos/Scan, uno/evaluación, 1024 output tokens por defecto, timeout 20000 ms, intervalo 1000 ms, concurrencia uno, presupuesto worker 300000 ms, cero retries/redirects. El modelo configurado es gpt-6-luna; disponibilidad/tarifas no se han comprobado. Resolverlas antes de habilitar envío. Si se usa gate monetario, aprobar EVALUATOR_MAX_ESTIMATED_COST_USD, EVALUATOR_ESTIMATED_MAX_CALL_COST_USD y el perfil EVALUATOR_PRICING_PROVIDER/MODEL/VERSION/DATE/CURRENCY, EVALUATOR_ESTIMATE_VERSION, EVALUATOR_INPUT_PRICE_PER_MILLION y EVALUATOR_OUTPUT_PRICE_PER_MILLION. No inventar importes ni confundir cap de tokens con gasto máximo garantizado.

Ejecutar otro Scan autorizado; conservar resultado híbrido/uso/coste conocidos y clasificar externamente. HYB-02 puede consolidar VULNERABLE con señal fuerte y precondiciones válidas aun ante fallo LLM; SAFE opcional exige LLM concordante para HYB-04. Abstenciones/fallos permanecen originales. Deshabilitar exportación/allowlist y retirar configuración de clave al terminar. Esta fase no ejecuta proveedor, SAFE, AWS, Docker, PDF ni experimento académico.
