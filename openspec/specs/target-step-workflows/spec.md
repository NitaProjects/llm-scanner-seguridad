# target-step-workflows Specification

## Purpose
Componer un flujo HTTP autorizado mediante pasos declarativos acotados que entregan una respuesta final a la evaluación.

## Requirements

### Requirement: WF-01 Plan secuencial versionado

La definición JSON SHALL declarar versión, pasos GET/POST, rutas relativas y dependencias anteriores. Los pasos intermedios SHALL usar intercambios HTTP JSON con body JSON cuando proceda y extraer variables únicamente de documentos de respuesta JSON completos y válidos. Un paso intermedio SSE/JSONL SHALL rechazarse antes de red. Las guardas SHALL limitarse a exists/equals de valores no sensibles. SHALL validar antes de red, rechazar ciclos y referencias futuras y exigir como último paso un POST JSON incondicional que inserte el prompt una vez. La respuesta terminal SHALL admitir JSON mediante selector o SSE/JSONL mediante el colector del change 1 y producir la única NormalizedInteraction final. Una variable obligatoria ausente por un paso omitido SHALL abortar antes de su consumidor.

#### Scenario: Flujo chat y sesión
- **WHEN** la fixture crea chat y sesión, extrae chatId y sessionId de sus documentos JSON completos y envía el prompt a /answer con sessionId
- **THEN** se recorren los pasos declarados y se obtiene una única respuesta final de stream para el motor actual

#### Scenario: Stream intermedio
- **WHEN** un plan declara un paso intermedio SSE o JSONL para extraer una variable
- **THEN** la validación rechaza el plan antes de red

### Requirement: WF-02 Presupuesto y evidencia por paso

Cada prueba SHALL compartir un deadline global, límites acumulados y por exchange, cuota y máximo de pasos acotado. SHALL reautorizar destino, DNS/IP y TLS en cada exchange y mantener cero redirects y retries. Fallo requerido o cancelación SHALL abortar y liberar el contexto. La evidencia técnica por paso SHALL limitarse a IDs locales, status, tiempos, contadores y códigos. El resultado durable SHALL usar el intercambio terminal real y preservar la proyección actual.

#### Scenario: Fallo de paso requerido
- **WHEN** el segundo paso falla, o una guarda deja un dato requerido sin valor
- **THEN** no se envía el paso dependiente, el diagnóstico identifica el fallo sin contenido sensible y no se reanuda al reiniciar

### Requirement: WF-03 Origen fijo antes de red y credenciales

Cada paso SHALL conservar el esquema, host normalizado y puerto efectivo del origen base del binding. La validación del plan SHALL rechazar rutas con esquema o autoridad, incluidas referencias `//host/ruta`. La URI final resuelta tras aplicar variables SHALL comprobarse antes de red del paso y antes de materializar Authorization, cookies, secretos o headers sensibles. Si el origen difiere, el workflow SHALL abortar sin solicitudes ni propagación de credenciales al destino rechazado, incluso si figura en `TARGET_ALLOWED_ORIGINS`. El diagnóstico SHALL conservar solo evidencia segura del rechazo.

#### Scenario: Dos orígenes permitidos en la allowlist
- **WHEN** la base es `https://target-a.example`, el paso declara `//target-b.example/path` y ambos orígenes están autorizados
- **THEN** el workflow se bloquea antes de red, target-b recibe cero requests, el Bearer del paso no se materializa ni se envía y se registra un diagnóstico sin URI ni credenciales
