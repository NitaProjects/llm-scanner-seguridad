# Reanudación operativa autorizada — 2026-10-19

Estado: **BLOCKED antes del Scan por detención de la automatización del navegador**. La corrección del schema está aprobada; no es el bloqueo actual. No se ha lanzado ningún Scan ni iniciado llamada al proveedor.

## Preflight observado

- PostgreSQL TCP127.0.0.1:5432 accesible, listeners PIDs31736/1116. BD efectiva declarada por argumento explícito del backend: llm_scanner_seguridad. Las consultas REST de Project/Target/históricos funcionaron. No SQL ni credenciales inspeccionadas.
- Demo iniciada desde su jar existente con Java21, VULNERABLE, bind127.0.0.1/8081, PID2940. GET /health HTTP200, UP/demo=true/profile=VULNERABLE/scenarioVersion1. No se enviaron probes.
- El operador arrancó el backend en su terminal preparada y confirmó el arranque. No se leyó la clave ni el entorno de ningún proceso. PID35120, java.exe, inicio2026-10-19T22:43:20.267866+02:00. Se extrajeron solo argumentos no secretos: profile local; puerto8080; URL PostgreSQL local/llm_scanner_seguridad; TARGET_LOCAL_DEMO_ENABLED=true; origen exacto http://127.0.0.1:8081; provider OPENAI; modelo gpt-6-luna; export global true; allowlist exclusivamente demo-vulnerable-v1; ruta absoluta al manifest local autorizado; maxCalls4/timeout20000/interval1000/outputTokens1024/scanBudget300000. Registro en preflight.json. La presencia de clave procede de la declaración del operador, sin leerla ni atribuir la comprobación a otra consola.
- Se mantiene la autorización expresa anterior para un único Scan sin gate monetario estimado interno, con el hard spend limit de OpenAI declarado por el operador. No se inventan tarifas, factura máxima ni acceso al modelo.
- Manifest local comparado estructuralmente con la plantilla VULNERABLE tras las tres sustituciones exactas de identidad/fecha; solo se habilitó exportPolicy.enabled. Contexto/corpus/cuatro entries intactos. Schema V1 corregido y schema del binding: PASS. Hash del manifest habilitado: 2219C27C4E866EDD79845A663DC88844956A383EB4FDB0B6943AC905DC22F75C.
- GET Project65f132db-20c5-40b8-a3ee-6ee7d2cb2d76 y Target3ae92e44-4e81-427f-93ce-021a6a751643 HTTP200. Target.updatedAt exacto2026-10-16T16:25:10.710151Z coincide con manifest/binding; contrato POST/api/chat, application/json, message/{{PROMPT}}, $.response, NONE/sin credenciales, headers vacíos y timeout5000 verificado. target-before.json conserva la respuesta pública. Cuatro Projects comprobados: ningún Scan PENDING/RUNNING.
- Angular iniciado con dependencias existentes, host127.0.0.1/puerto4200, PID35680, para el lanzamiento y recorrido UI exigidos por el change. En esta reanudación no se acredita respuesta HTTP/proxy/UI: el bloqueo ocurrió antes de observarlo.

## Bloqueo de navegador

El conector de navegador no tenía Chrome disponible. Se intentó seleccionar una ventana existente de Chrome mediante la skill computer-use para abrir una pestaña local nueva y observar el lanzamiento HTTP, sin seleccionar la ventana de credenciales. La herramienta detuvo Computer Use para este turno porque no pudo determinar la URL activa con suficiente confianza para aplicar su política. No devolvió estado verificable de la nueva pestaña. Se respetó la detención: sin acciones posteriores de navegador, sin reintento, sin vías alternativas de lanzamiento y sin pulsar ejecutar ni enviar POST de Scan.

No se considera que el preflight UI completo haya pasado. No se diagnostica error del backend, de OpenAI ni del modelo a partir de este bloqueo de herramienta. Hará falta recuperar una vía de navegador verificable para el recorrido aprobado antes de lanzar el único Scan; no se ha consumido esa autorización.

## Resultados

Modelo configurado: gpt-6-luna. Acceso: **BLOCKED/no comprobado por falta de ejecución**, sin modelo alternativo ni error del proveedor observado. ScanId: ninguno. Llamadas al evaluator iniciadas por esta sesión:0. LLM01/02/08/10 sin ejecuciones/evaluaciones nuevas, sin llmVerdict/confidence/consolidación. Findings/Score/Report no obtenidos. Matriz ground truth sin observaciones clasificables: no se inventan TP/FN/INCONCLUSIVE ni TN/FP. Frontend9.4 pendiente. Usage/model metadata/coste: **NOT_INSPECTED**, no Scan propio que consultar y ninguna consulta de facturación. No se afirma consumo cero de la cuenta a partir de la ausencia de llamadas de esta sesión.

## Cierre

Incluso sin Scan se aplicó el cierre autorizado. Manifest local restaurado enabled=false, schema PASS, SHA256 C603784A8599201D7C82914147382C0471676FE34E02F76CD5BEAD0DD32F8334. Sin modificación de plantilla/schema/binding en esta reanudación.

Comprobados los históricos de cuatro Projects antes de parar: cero Scans activos. Se verificaron nombres/fechas de inicio y se detuvieron scanner35120, demo2940 y Angular35680. Verificación posterior: sin listeners8080/8081/4200; PostgreSQL5432 conservado. closure.json registra el cierre22:47:16 Europe/Madrid. La exportación de esa instancia queda inoperativa por parada; el manifest queda deshabilitado. No se puede retirar la variable de la terminal padre del operador desde el ejecutor: pendiente que el operador ejecute `Remove-Item Env:EVALUATOR_API_KEY -ErrorAction SilentlyContinue` en su terminal preparada. No se lee ni imprime el valor.

Sin cambios de código, builds/tests adicionales, migraciones, limpieza BD, AWS, deploy, commit/push ni llamadas externas. Se conserva únicamente este resultado y evidencia no secreta del preflight/Target/cierre. Las tareas operativas no realizadas y frontend9.4 permanecen pendientes.

OpenSpec strict ejecutado al cerrar: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`, exit0. No acredita ejecución real ni cobertura UI.
