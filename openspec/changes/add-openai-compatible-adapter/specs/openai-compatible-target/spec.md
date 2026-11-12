## Purpose

Permitir configurar desde la aplicación targets de chat compatibles con OpenAI y ejecutar las cuatro pruebas actuales mediante un contrato textual no streaming, conservando la integración REST y la evaluación existentes.

## ADDED Requirements

### Requirement: OAT-01 Configuración tipada y persistente

**Historia:** Como usuario quiero guardar endpoint/modelo/Bearer/system opcional/timeout sin editar JSON de transporte.
**Prioridad:** Alta. **Dependencias:** configuración v1 y frontera D0 implementadas.

El sistema SHALL distinguir REST_LEGACY y OPENAI_COMPATIBLE persistentemente, manteniendo un target por Project. La rama chat SHALL admitir endpoint completo, model requerido, auth BEARER_TOKEN, systemPrompt opcional, name opcional y timeout 1000..60000/default10000. Model SHALL tener 1..200 puntos de código tras trim, sin controles; system presente SHALL ser no blanco, <=16 KiB y preservarse; documento base generado solo en memoria SHALL respetar la cota de 32 KiB existente y body efectivo los límites vigentes. Model/system SHALL rechazar el token reservado `{{PROMPT}}`. No SHALL admitir plantilla/ruta de extracción manuales ni parámetros adicionales en chat. Guardar/leer SHALL ser local, sin DNS/peticiones al proveedor. El tipo SHALL conservarse tras reinicio sin inferirse de URL/plantilla; legacy existente SHALL permanecer legacy.

TargetConfiguration SHALL persistir adapter_type, model y system_prompt explícitamente. En OPENAI_COMPATIBLE model SHALL ser no nulo/no blanco, system_prompt nullable y request_template/response_path NULL; la request SHALL generarse en memoria desde campos tipados. En REST_LEGACY request_template/response_path SHALL conservar obligatoriedad y fuente de verdad actuales, con model/system_prompt NULL. Una única migración incremental SHALL establecer default REST_LEGACY, nulabilidad física compatible y checks condicionales con IS NULL/IS NOT NULL explícitos para no aceptar UNKNOWN indebidamente en PostgreSQL. Filas legacy existentes SHALL conservar datos, sin backfill de modelos/systems, extracción de templates, nuevas tablas, JSON genérico o revisiones. El codec/generador SHALL NOT utilizarse como almacenamiento persistente de configuración chat.

#### Scenario: Configuración chat y lectura posterior
- **WHEN** se guarda chat con endpoint/model/system/Bearer válidos y se vuelve a consultar tras reinicio
- **THEN** se recuperan los mismos campos seguros y tipo, hasCredentials sin recuperar token, sin requests remotos ni campos manuales de plantilla/ruta

#### Scenario: Nulabilidad según rama y legacy existente
- **WHEN** se migra una fixture legacy y se guardan configuraciones chat válidas o con combinaciones cruzadas de campos
- **THEN** la legacy conserva plantilla/ruta y model/system NULL, chat válido persiste sus columnas con plantilla/ruta NULL, y la BD rechaza chat sin model o legacy con model/system incluso cuando haya otros NULLs

### Requirement: OAT-02 API compatible y UX progresiva

**Historia:** Como usuario quiero seleccionar el tipo y ver solo los campos necesarios manteniendo el formulario REST.
**Prioridad:** Alta. **Dependencias:** OAT-01.

POST/GET/PUT de target SHALL soportar las dos ramas. Crear sin adapterType SHALL conservar el comportamiento REST legacy y su representación histórica. PUT sobre chat sin adapterType SHALL devolver 409 TARGET_TYPE_REQUIRED sin modificar datos; cambios de tipo SHALL ser explícitos. La UI SHALL ofrecer REST JSON/OpenAI-compatible y mostrar endpoint/model/Bearer/system/timeout en chat, sin requestTemplate/responsePath/headers genéricos ni extras. Datos ocultos de otro tipo SHALL NOT enviarse. Credencial SHALL mantenerse transitoria con conservar/reemplazar y limpieza como v1; cambio de origen SHALL requerir secreto nuevo según reglas vigentes. La aplicación SHALL explicar la limitación sintética de evaluación v1 sin cambiar report-v1. Guardar/cambiar tipo SHALL NOT ejecutar Scan ni comprobar credenciales por red.

La protección frente a clientes antiguos SHALL ser atómica en la escritura, no solo en una lectura/validación previa. Reemplazo SHALL condicionar la actualización por id/projectId y adapter_type esperado: REST_LEGACY para PUT sin discriminador, y tipo leído para PUT explícito. Configuración completa de destino, NULLs de campos no aplicables, credencial y updatedAt SHALL confirmarse juntos; ninguna ruta JPA/SQL incondicional SHALL eludir esa protección. Si una conversión concurrente impide coincidencia, el PUT sin tipo SHALL devolver 409 TARGET_TYPE_REQUIRED y el explícito 409 TARGET_TYPE_CHANGED, sin relectura/retry que sobrescriba el tipo nuevo ni escritura parcial. No SHALL añadirse revisions, @Version o versionado general para resolver esta carrera.

#### Scenario: Cliente viejo y cambio de tipo
- **WHEN** un cliente sin discriminador intenta sobrescribir un target chat y otro usuario cambia explícitamente a REST
- **THEN** la primera escritura se rechaza sin pérdida y la segunda exige contrato REST completo válido y confirmación en UI

#### Scenario: Conversión concurrente antes del UPDATE legacy
- **WHEN** T1 lee legacy para un PUT sin discriminador y se pausa antes de escribir, T2 convierte y confirma chat, y después T1 intenta su escritura condicional
- **THEN** T1 afecta cero filas y devuelve 409 TARGET_TYPE_REQUIRED sin retry; los campos chat, URL, credencial y updatedAt confirmados por T2 permanecen intactos
- **AND** la prueba coordina ambas transacciones con barreras deterministas, sin sleeps ni llamadas al target

### Requirement: OAT-03 Request chat exacta y selección simple

**Historia:** Como operador quiero que el motor envíe prompts de prueba a APIs compatibles sin conocer proveedores comerciales.
**Prioridad:** Alta. **Dependencias:** OAT-01; OAT-05.

Cada SecurityTest SHALL generar un único POST JSON al endpoint exacto con model, messages=[system opcional,user] y stream=false; user SHALL contener el prompt original sin alteración ni contexto adicional. No SHALL enviar tools, temperature, response_format, reasoning u opciones por proveedor. Las cuatro pruebas SHALL ser interacciones independientes. La selección SHALL depender solo del tipo explícito, sin registry/reflection, inferencia por hostname o fallback a legacy ante tipo desconocido. El endpoint/modelo comercial SHALL NOT introducir reglas específicas en dominio/evaluación.

#### Scenario: Request con y sin system
- **WHEN** se ejecutan fixtures de chat con system configurado y omitido
- **THEN** el servidor controlado observa respectivamente dos mensajes y un mensaje user, model correcto y stream=false, con prompt serializado de forma segura e idéntico al original

### Requirement: OAT-04 Normalización estricta y errores existentes

**Historia:** Como motor quiero recibir texto final completo o un error técnico existente sin ampliar el resultado normalizado.
**Prioridad:** Alta. **Dependencias:** OAT-03; contrato D0.

La respuesta SHALL ser JSON estricto y contener una única elección con message.content String y finish_reason=stop; role/index presentes SHALL ser assistant/0. Textos SHALL conservarse exactamente tras el saneamiento vigente. Tools/function calls no vacíos, finales truncados/ausentes/desconocidos, múltiples elecciones o forma no soportada SHALL producir RESPONSE_FORMAT_ERROR. Cuerpo vacío/JSON malformado SHALL producir RESPONSE_INVALID_JSON; ruta ausente/choices vacío SHALL producir RESPONSE_PATH_NOT_FOUND; content null/no textual SHALL producir RESPONSE_NOT_TEXT. content vacío o whitespace con terminal válido SHALL permanecer éxito técnico sin certificar seguridad. No SHALL ejecutarse herramientas ni evaluarse prefijos incompletos.

401/403/404/429/5xx SHALL conservar HTTP_STATUS_ERROR y httpStatus, sin copiar cuerpo libre remoto ni retries. Timeout y tamaños SHALL conservar los códigos y precedencia existentes. La clasificación individual/global y continuación/aborto del motor SHALL permanecer como v1. NormalizedInteraction y su proyección SHALL conservar exactamente los siete valores actuales y no añadir metadata de proveedor/modelo/protocolo.

#### Scenario: Errores HTTP y texto incompleto
- **WHEN** el servidor devuelve 429 o HTTP200 con content válido pero finish_reason=length
- **THEN** se registran respectivamente HTTP_STATUS_ERROR/429 y RESPONSE_FORMAT_ERROR/200, sin reenvío ni éxito de evaluación sobre texto truncado

#### Scenario: Diferencias entre ausencia, null y vacío
- **WHEN** se reciben content ausente, null y String vacío con stop
- **THEN** se distinguen RESPONSE_PATH_NOT_FOUND, RESPONSE_NOT_TEXT y éxito técnico con texto vacío, sin inventar contenido

### Requirement: OAT-05 Transporte y credenciales protegidos

**Historia:** Como responsable quiero que chat conserve todos los controles de red y secretos actuales.
**Prioridad:** Crítica. **Dependencias:** política de autorización/ADR-002 y transporte v1.

La invocación SHALL reutilizar controles comunes de allowlist exacta externa, SSRF/DNS efectivo/IP fijada, TLS del hostname, bloqueo privado/loopback/metadata, timeout/tamaños/cuota y cierre de recursos. SHALL mantener cero redirects/retries/cookies automáticos. Tipo/model/endpoint configurados SHALL NOT conceder autorización ni habilitar redes privadas. Bearer SHALL almacenarse cifrado, usarse solo tras autorizar/validar destino y omitirse de logs/errores/Finding/evidencia pública/GET/report; si se refleja en salida SHALL aplicarse redacción/flags vigentes. La validación de respuesta SHALL operar dentro del transporte acotado sin recibir secreto ni exportar cuerpo crudo. REST legacy SHALL conservar sus controles/resultados.

#### Scenario: Destino no autorizado y token reflejado
- **WHEN** el destino está bloqueado o una fixture permitida devuelve la credencial conocida en content
- **THEN** el primer caso no descifra/envía y el segundo devuelve texto redactado con flags, sin token en errores/logs/proyecciones públicas

### Requirement: OAT-06 Pipeline y alcance de evaluación preservados

**Historia:** Como analista quiero usar el mismo Scan engine y entender qué se ha validado realmente.
**Prioridad:** Alta. **Dependencias:** OAT-03..05; evaluación/scoring/reporting v1.

La nueva integración SHALL atravesar TargetAdapter→NormalizedInteraction→proyección legacy→TestExecution y el pipeline actual, con cuatro pruebas/evaluaciones y cierre atómico de score/report-v1/Scan. SHALL NOT modificar criterios, corpus, prompts, Findings o fórmulas/report para obtener detecciones comerciales. Respuestas sin contexto/corpus autorizado SHALL seguir INCONCLUSIVE/supresión LLM según v1, sin exportación automática al evaluador. Transporte real SHALL NOT presentarse como validación de cobertura general ni habilitación de datos sensibles.

#### Scenario: Proveedor válido sin contexto de evaluación
- **WHEN** las cuatro respuestas chat son técnicamente válidas pero no hay contexto sintético autorizado aplicable
- **THEN** el motor puede completar cuatro ejecuciones/evaluaciones inconclusas, score0 y report-v1 íntegro sin llamar al evaluador ni afirmar seguridad

### Requirement: OAT-07 Tests aislados y aceptación real autorizada

**Historia:** Como revisor quiero pruebas reproducibles antes de usar una cuenta real.
**Prioridad:** Alta. **Dependencias:** OAT-01..06.

La validación automatizada SHALL usar mocks/servidores locales controlados y fixtures deterministas para request/Bearer/system/model/stream=false, extracción, errores, timeout, seguridad y ausencia de secretos, más regresión REST y resultados derivados. No SHALL depender de Internet/proveedor LLM. La aceptación externa SHALL realizarse después con autorización específica, clave local no versionada y datos no sensibles, inicialmente Groq u OpenRouter; SHALL ejecutar el mismo Scan de cuatro tests sin código comercial en dominio y registrar solo evidencia segura/versiones/resultados. Modelo no disponible, falta de permiso o fallo técnico SHALL dejar la aceptación pendiente, no sustituirla por una fixture. Ninguna validación real SHALL ejecutarse durante el diseño.

#### Scenario: Aceptación aislada y externa separadas
- **WHEN** pasan las pruebas locales pero no hay autorización o modelo real confirmado
- **THEN** se informa aceptación aislada y validación real pendiente, sin afirmar llamada real, métricas de detección ni calidad de un proveedor
