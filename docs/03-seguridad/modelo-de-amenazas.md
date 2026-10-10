# Modelo de amenazas inicial

## Propósito

Este documento identifica las amenazas principales del MVP de LLM Scanner Seguridad. No sustituye a un análisis de riesgos completo y deberá evolucionar junto con la arquitectura y los requisitos OpenSpec.

## Activos principales

- Configuración de proyectos y targets.
- Credenciales o referencias de autenticación del target.
- Prompts, peticiones y respuestas de los scans.
- Evidencias y findings.
- Informes generados.
- Credenciales de proveedores LLM.
- Disponibilidad y coste de la plataforma.

## Actores y superficies de ataque

- Usuario autorizado que configura y ejecuta análisis.
- Usuario no autorizado que intenta acceder a proyectos o resultados.
- Endpoint objetivo configurado por el usuario.
- Respuestas maliciosas del target que intentan influir en el evaluador.
- Atacante externo que intenta utilizar el scanner como proxy o extraer información.
- Proveedor externo de modelo evaluador.

## Riesgo SSRF

El backend realizará peticiones a URLs proporcionadas por usuarios mediante una configuración de target. Un atacante podría intentar utilizar el scanner para acceder a servicios internos, localhost, rangos privados o endpoints de metadatos cloud.

Controles previstos:

- Aceptar únicamente protocolos y destinos permitidos.
- Bloquear localhost, loopback, redes privadas, interfaces locales y metadatos cloud.
- Resolver y validar el destino antes de realizar la petición.
- Controlar redirecciones y evitar que salten a destinos no autorizados.
- Aplicar timeouts, límites de tamaño, frecuencia y concurrencia.
- Mantener un mecanismo para demostrar la autorización del target.
- Validar la plantilla JSON, el `responsePath` y los headers configurables sin permitir transformaciones arbitrarias.

`security-test-execution` concreta la ejecución real: TARGET_ALLOWED_ORIGINS inicialmente vacía, orígenes exactos externos y autorización antes de DNS/crypto. La excepción demo conserva perfil local, TARGET_LOCAL_DEMO_ENABLED y TARGET_LOCAL_DEMO_ORIGINS; nunca se activa por API. Todas las A/AAAA (máximo 16) se validan y un conjunto mixto se rechaza; se conecta a una sola IP validada manteniendo Host/SNI/TLS originales, sin nueva resolución, fallback, proxies ni reutilización entre intentos. Cero redirects y retries; no se reenvían credenciales. La tabla estable de exclusiones está en TargetDestinationPolicy y se documenta en la política de autorización; demo no permite link-local/metadatos/CGNAT ni otras redes especiales. No certifica titularidad del destino.

## Configuración del target

La configuración admite headers ordinarios no sensibles y autenticación separada mediante `NONE`, `BEARER_TOKEN` o `API_KEY_HEADER`. Los secretos entran exclusivamente por authConfig.secretValue y no se devuelven, ni siquiera cifrados o enmascarados. Se bloquean headers sensibles/reservados y colisiones; no se promete detectar cualquier secreto arbitrario pegado en texto libre.

Reglas de protección:

- Las credenciales nunca deben aparecer en logs.
- Nunca deben incluirse en informes.
- Nunca deben almacenarse en texto plano.
- No deben exponerse al frontend después de ser configuradas.
- Los errores no deben devolver secretos.
- Los payloads persistidos deben sanitizar headers sensibles.
- Los secretos deben descifrarse únicamente en memoria durante la ejecución autorizada.

D-CRYPTO local está aprobado: AES-256-GCM, clave externa de 256 bits, nonce aleatorio nuevo y envelope versionado (ADR-002). La clave no se persiste ni se genera automáticamente; ausencia/formato inválido impiden cifrar con error sanitizado y sin escritura parcial. GET y conservación en PUT no necesitan clave ni descifrado. AWS Secrets Manager/KMS y rotación avanzada siguen fuera del change.

El MVP no aceptará configuraciones basadas en GET, streaming, multipart, WebSocket, SSE, SOAP o gRPC.

## Respuestas no confiables y prompt injection indirecta

La aplicación objetivo puede devolver contenido diseñado para influir en el modelo evaluador o en el sistema. Las respuestas deben tratarse como datos no confiables, delimitarse en el contexto de evaluación y no tener capacidad para cambiar instrucciones, configuración o flujo.

## Exposición de información sensible

Las peticiones y respuestas pueden contener credenciales, datos personales, información interna o secretos. Deben aplicarse minimización, redacción, controles de acceso y políticas de retención antes de almacenar o mostrar evidencias.

La ejecución técnica local persiste request JSON preparado y string extraído, con redacción exacta de la credencial conocida sobre claves/cadenas JSON decodificadas y responseText. Una colisión de claves tras redacción omite evidencia y falla; no conserva copia bruta. No se promete detectar secretos arbitrarios/codificados. La evidencia no sale por REST/logs. evaluation-engine permite únicamente una proyección mínima del corpus sintético completo con autorización externa independiente y manifiesto vinculado al snapshot. Texto desconocido o redacción relevante impiden exportar/concluir. Los límites por Scan no resuelven retención temporal, borrado ni autorización de usuarios: esas decisiones siguen pendientes antes de datos reales/exposición pública.

Defaults técnicos: request 64 KiB, response 256 KiB, texto 64 KiB, evidencia 128 KiB por ejecución/512 KiB por Scan y 300 s globales; una invocación y separación >= 1 s. Se cuentan bytes de red antes de parsear y evidencia después de serializar/redactar. DNS tiene dos hilos/cola ocho, máximo 2 s, conexión/TLS 5 s y deadline monotónico total recortado por presupuesto. El parser acota profundidad/cabeceras; UTF-8 y JSON son estrictos. Cierre/cancelación impiden continuar un intento terminado tarde.

## Errores de evaluación

El evaluador LLM puede producir falsos positivos, falsos negativos o resultados inconsistentes. evaluation-engine combina señales deterministas independientes, criterios y assessment validado mediante HYB-01..07. Un voto aislado no genera Finding; evidencia insuficiente produce INCONCLUSIVE/0.00. El engine controla severity y confidence-v1 heurístico, sin CRITICAL automático ni cobertura del sink LLM10.

La integración utiliza EvaluatorPort independiente y un único adapter OpenAI Responses API/Structured Outputs, modelo inicial externo gpt-6-luna. La evidencia va en un mensaje JSON de datos no confiables separado de instrucciones, criterios y schema; no hay tools, memoria o URLs configurables del proveedor. Parser estricto y referencias reales; las narrativas se sustituyen por plantillas antes de guardar/publicar, sin chain-of-thought. API key externa exclusivamente en Authorization, sin cuerpo/causa sensible en errores, logs o persistencia. Esto limita el impacto de prompt injection, sin prometer eliminar su riesgo semántico.

Límites aprobados: cuatro llamadas por Scan, evidencia 16 KiB/input 24 KiB/respuesta 16 KiB, output 1024 tokens, timeout 20 s incluida espera, concurrencia global uno e intervalo 1 s dentro del deadline compartido 300 s. Sin retries. 429/auth/configuración/cupo suprimen llamadas restantes según D-09. llmStatus/llmErrorReason son distintos de status/errorReason globales: fallo LLM permite COMPLETED/VULNERABLE con fuerte válida o COMPLETED/INCONCLUSIVE sin ella. Solo fallo técnico global impide cerrar Scan. Reserva/claim confirmados antes de red y transacción resultado+Finding evitan duplicados y estados parciales; recuperación no reenvía llamadas.

## Abuso de recursos

Un usuario podría provocar costes o degradación mediante demasiados scans, prompts grandes o targets lentos. Se deberán aplicar límites de peticiones, tamaño, tiempo, concurrencia y coste.

La ejecución asíncrona evita mantener una petición HTTP abierta durante todo el análisis, pero introduce trabajo pendiente y tareas en segundo plano que también deben protegerse. Deben controlarse el número máximo de pruebas por scan, la concurrencia de scans y ejecuciones, el rate limiting, los timeouts del target y del evaluador y el coste máximo de llamadas al LLM.

`scan-execution` limita un activo por Project mediante índice único parcial y acota pool/cola configurables con rechazo inmediato. Reiniciar una única instancia marca activos FAILED/SCAN_INTERRUPTED sin relanzar; un fallo irrecuperable de persistencia bloquea nuevos inicios/reclamos hasta reinicio con recuperación correcta. Esta política no soporta múltiples instancias concurrentes. Los futuros fallos individuales deben persistirse sin reintentos automáticos no definidos. Reintentos, cancelación y políticas distribuidas siguen **Pendientes de decisión**. Un fallo del modelo evaluador debe registrarse como error de evaluación y no clasificarse automáticamente como vulnerabilidad del target. COMPLETED del no-op no acredita seguridad ni produce findings.

## Alcance pendiente

- Modelo de autenticación y autorización de usuarios.
- Provisión operativa y ciclo de vida avanzado de credenciales del target fuera del MVP local.
- Política de retención y borrado de evidencias.
- Nivel de aislamiento de los scans.
- Controles concretos para producción y AWS.
