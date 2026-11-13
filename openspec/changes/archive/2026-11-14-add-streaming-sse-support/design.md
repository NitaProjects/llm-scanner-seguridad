## Context

HttpTargetInvocationAdapter lee JSON completo antes de aplicar el extractor. Ya gestiona cuota, deadlines, DNS/IP fijada, TLS, credencial y cierre. El resultado HTTP existente se proyecta a NormalizedInteraction y TestExecution.

## Goals / Non-Goals

Resolver una sola petición POST JSON con respuesta SSE o JSONL bajo la frontera existente. El flujo de sesión queda en su change.

## Decisions

Configurar por manifiesto operativo el binding del target REST_LEGACY, media type admitido, discriminador de evento, selector restringido del texto, acumulación delta o snapshot y terminal explícito. Mantener plantilla de envío, auth y timeout del target. SSE usa text/event-stream y JSONL application/x-ndjson o application/jsonl declarados. Un formato incorrecto falla con RESPONSE_FORMAT_ERROR. El adaptador se elige al preparar el plan y el flujo normal sin binding conserva su parser.

El intercambio activado por el binding envía exactamente un header `Accept` con `text/event-stream` para SSE o el media type JSONL concreto declarado. Ese valor reemplaza cualquier `Accept` ordinario configurado, incluido el default `application/json`, solo para ese intercambio. La validación del binding compara nombres de header sin distinguir mayúsculas y minúsculas. Si `authHeaderName` o una asignación sensible o reservada reclama `Accept`, rechazar antes de DNS, descifrado y envío. No materializar el valor ni reflejarlo en el diagnóstico. REST_LEGACY y OPENAI_COMPATIBLE sin binding mantienen su `Accept` configurado o el default actual.

La fixture de negociación devuelve SSE válido únicamente si recibe `Accept: text/event-stream`. Configurar además un `Accept: application/json` ordinario y comprobar que el intercambio SSE recibe un único header y reconstruye el texto exacto. Parametrizar la negociación JSONL con ambos media types permitidos. Una segunda fixture intenta usar `aCcEpT` como nombre de credencial o header sensible/reservado y comprueba rechazo antes de DNS, cero descifrados, cero requests y diagnóstico seguro.

Extraer la lectura incremental dentro del intercambio seguro común. UTF-8 estricto incremental, CRLF/LF/CR para SSE, BOM inicial opcional, campos data/event y unión de líneas data según SSE. Procesar frames delimitados por línea vacía y JSONL por línea completa. Un fragmento TCP nunca determina un frame. Usar selectores cerrados sobre JSON estricto o un terminal literal declarado. Los eventos desconocidos y heartbeats se descartan y cuentan. El terminal explícito cierra la lectura y el socket. EOF anterior al terminal, error remoto, delta inválido o terminal sin contenido válido deja la interacción fallida. El texto de snapshots sustituye al acumulado, los deltas se agregan en orden.

InteractionEvent es interno y acotado. Usar responseBytes, textBytes, evidenceBytes y presupuesto global existentes. Proponer 32 KiB por frame/línea y 1 024 unidades lógicas por petición, reducidos por el límite efectivo de bytes. Los comentarios y las líneas descartadas también consumen cuota, al menos una unidad por heartbeat o registro ignorado. Los topes acotan memoria del parser y trabajo por respuesta aunque los frames sean diminutos. Medir bytes de la entidad antes de parsear y texto saneado después de ensamblar. Deshabilitar compresión y reintento de stream. Deadline absoluto igual al menor de timeout del target y presupuesto restante, incluida espera de cuota dentro del scan. El cierre cancela I/O bloqueado y preserva interrupciones. Error de deadline usa TARGET_TIMEOUT o SCAN_BUDGET_EXCEEDED según origen. Exceso de entidad/frame/eventos usa RESPONSE_TOO_LARGE. Texto o evidencia excedida usa EVIDENCE_LIMIT_EXCEEDED. Fallo parcial usa RESPONSE_FORMAT_ERROR con código de diagnóstico cerrado, sin entregar prefijo a evaluación.

El body JSON real saneado y HTTP status quedan en la proyección existente. Redactar el texto final después de ensamblar, también cuando una credencial se divide entre eventos. La evidencia conserva solo el resultado final y metadata segura. Sin cambios de DB/API/UI. Scoring y evaluación consumen el texto final como en REST. Fuente de seguridad y rollback de bindings en el [roadmap](../generalize-target-integration/roadmap.md).

## Risks / Trade-offs

Un stream infinito retiene recursos. El deadline absoluto, los límites y el cierre acotado aplican aunque sigan llegando heartbeats. Terminar la lectura reduce el uso local, pero el coste remoto depende del proveedor. La extracción incremental comparte las defensas de DNS, TLS y origen sin abrir redirects, cookies ni credenciales dinámicas.
