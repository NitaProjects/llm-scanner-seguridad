## Purpose

Aplicar autorización, privacidad y presupuestos a cada operación de integración, incluyendo destinos derivados, streaming y workflows con múltiples peticiones.

## ADDED Requirements

### Requirement: IS-01 Autorización efectiva de cada conexión

**Historia:** Como responsable quiero evitar SSRF directo o indirecto en cualquier adapter.
**Prioridad:** Crítica M1 en adelante. **Dependencias:** política de autorización y STE-08 históricos.

Cada exchange/intento/poll/cleanup SHALL autorizar origen exacto desde política operativa externa, validar URL final, resolver DNS acotado, validar todas las direcciones y fijar la conexión a una IP autorizada manteniendo hostname TLS. SHALL bloquear loopback, privadas, interfaces locales, link-local y metadata por defecto, IPv4/IPv6 y representaciones equivalentes. Allowlist SHALL NOT autorizar por sí sola direcciones privadas. Variables SHALL NOT modificar scheme/host/port. D1..M4 SHALL usar cero redirects; SHALL NOT seguir Location ni URLs retornadas por el remoto. Guardar configuración o seleccionar un proveedor SHALL NOT conceder permiso de red.

#### Scenario: DNS público que devuelve dirección privada
- **WHEN** un origen autorizado resuelve una dirección pública y otra privada antes de un poll
- **THEN** se bloquea ese exchange sin descifrar ni enviar credenciales, sin elegir solo la dirección aparentemente segura

#### Scenario: Redirect a metadata
- **WHEN** el servidor responde con Location apuntando a metadata o a otro origen
- **THEN** no se sigue el redirect ni se reenvía Authorization

### Requirement: IS-02 Credenciales restringidas al destino

**Historia:** Como usuario quiero autenticar el target sin divulgar secretos en configuraciones o evidencias.
**Prioridad:** Crítica M1 en adelante. **Dependencias:** ADR-002; TI-01.

Los secretos SHALL almacenarse cifrados, solo descifrarse en memoria tras autorizar destino y permanecer ligados a origen/uso. Headers sensibles SHALL configurarse mediante referencias protegidas, no variables, literales públicos ni URLs. GET, logs, errores, informes y evidencia SHALL NOT exponer credenciales/ciphertext. Cambio de origen SHALL exigir credencial nueva o binding autorizado explícito. Sesiones remotas SHALL tratarse como valores sensibles; no SHALL habilitar cookie jar/OAuth/login implícito.

#### Scenario: Cambio de origen y error de autenticación
- **WHEN** cambia el endpoint o una respuesta de error refleja el token
- **THEN** no se arrastra automáticamente la credencial al nuevo origen ni se copia el error sensible a diagnóstico/evidencia

### Requirement: IS-03 Presupuestos y retries seguros

**Historia:** Como operador quiero evitar amplificación de tráfico y coste por retries o pasos.
**Prioridad:** Alta M1; retry configurable M1b; workflow M4. **Dependencias:** STE-09 histórico.

Toda interacción SHALL compartir límites de tiempo, bytes, frecuencia, concurrencia y solicitudes con Scan. Cada intento SHALL reservar presupuesto antes de red; streaming/polls/retries/cleanup SHALL consumirlo sin reiniciarlo. Retry SHALL ser cero por defecto; reenvío de POST SHALL requerir contrato explícito de idempotencia soportado por el remoto. Timeout ambiguo SHALL NOT provocar reenvío ciego. Backoff/Retry-After SHALL respetar deadline; auth/política/parsing SHALL NOT reintentarse. HTTP y esperas SHALL ejecutarse fuera de transacciones de BD.

#### Scenario: Timeout después de enviar prompt
- **WHEN** no se sabe si un POST fue procesado y no hay garantía de idempotencia
- **THEN** se registra fallo técnico sin repetir generación ni cambiar de proveedor

### Requirement: IS-04 Consentimiento, evidencia y retención

**Historia:** Como propietario quiero controlar qué respuestas se conservan o salen a otro modelo.
**Prioridad:** Crítica antes de aceptación real D1/D2. **Dependencias:** NI-03; EE-06 histórico; RV-01.

Autorizar un target SHALL NOT autorizar exportación al evaluador. El perfil v2 SHALL requerir política explícita vinculada a revisión, proveedor, finalidad, campos y límites; exportación SHALL estar deshabilitada por defecto. Redactar credenciales conocidas SHALL NOT certificar ausencia de otros secretos. La captura de contenido SHALL ser opt-in, saneada, cifrada cuando se persista y con caducidad/purga/control de lectura definidos, incluidos efectos en backups. Datos sensibles SHALL permanecer bloqueados hasta aprobar esa política operativa. D1/D2 SHALL aceptar inicialmente solo casos autorizados con datos sintéticos/no sensibles; el corpus v1 exacto SHALL conservar su contrato. Si el saneamiento elimina soporte, SHALL emitirse INCONCLUSIVE, nunca evidencia fabricada.

#### Scenario: Permiso de red sin permiso de exportación
- **WHEN** un endpoint autorizado devuelve contenido y no hay política de exportación válida para esa revisión
- **THEN** no se envía contenido al evaluador externo y se conserva el motivo de supresión sin interpretar ausencia de llamada como seguridad
