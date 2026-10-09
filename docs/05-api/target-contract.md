# Contrato del target MVP

El change `target-configuration` (TC-01 a TC-12) configura un único target por Project sin ejecutar HTTP, resolver DNS, construir payloads ni extraer respuestas. El contrato interno completo se documenta en [OpenAPI](openapi.yaml).

## Recurso interno

POST, GET y PUT `/api/projects/{projectId}/target`. POST devuelve 201 y Location relativo tras commit; GET/PUT devuelven 200. Project inexistente produce PROJECT_NOT_FOUND, target ausente TARGET_NOT_CONFIGURED y segunda creación TARGET_ALREADY_CONFIGURED (409), también en concurrencia. PUT no hace upsert.

La entrada exige url, requestTemplate, responsePath y authType. name es opcional (120 puntos de código tras trim; blanco/null/omisión → null); headers omitido → {}; timeoutMs omitido → 10000 en POST y PUT, con rango 1000..60000 y null inválido. method=POST, contentType=application/json y promptPlaceholder={{PROMPT}} son constantes públicas, opcionales en entrada y no persistidas.

Las fechas UTC tienen precisión de microsegundos y son iguales al crear. PUT conserva id/projectId/createdAt y no hace retroceder updatedAt. Nunca cambia Project.

## Validaciones locales

- URL absoluta HTTP/HTTPS, hasta 2048 puntos de código tras trim, sin userinfo, fragmento, espacios interiores ni controles, con puerto válido. Se bloquean localhost, loopback y literales privados por defecto.
- Excepción demo: solo perfil local más TARGET_LOCAL_DEMO_ENABLED=true y orígenes declarados en TARGET_LOCAL_DEMO_ORIGINS (lista separada por comas), comparados por esquema/host/puerto efectivo. No admite metadatos/link-local ni redes completas; no se activa desde la API.
- requestTemplate es cadena JSON objeto, hasta 32768 bytes UTF-8, sin claves duplicadas y exactamente un token {{PROMPT}} decodificado como valor completo de cadena, nunca clave/fragmento. No se aplica trim ni sustitución.
- responsePath tiene trim, hasta 1024 puntos de código y 32 segmentos: raíz $, .identifier ASCII, propiedades ["cadena JSON no vacía"] e índices [0]..[2147483647], sin ceros iniciales, filtros, scripts, comodines, uniones, slices ni descenso recursivo.
- Headers: hasta 20; nombres HTTP token ASCII de hasta 128 caracteres; valores hasta 4096 bytes UTF-8 y total nombres/valores hasta 16384 bytes. Sin controles U+0000..U+001F/U+007F, duplicados por caso ni colisiones con autenticación. Se prohíben Authorization, Proxy-Authorization, Cookie, Set-Cookie, X-API-Key, API-Key, Host, Content-Length, Content-Type, Connection, Transfer-Encoding, TE, Trailer, Upgrade, Keep-Alive, Proxy-Connection, Forwarded y prefijo X-Forwarded-.
- U+0000 se rechaza antes de normalizar, también en claves/cadenas JSON decodificadas y rutas. No se elimina ni sustituye. JSON estricto sin coerción, claves duplicadas ni campos desconocidos/generados.

La futura ejecución debe resolver DNS, validar direcciones efectivas, autorización, redirecciones y límites. Guardar la configuración no acredita seguridad ni autorización. El futuro prompt se insertará como cadena JSON escapada; extracción debe producir un único texto y fallar ante ausencia o tipo distinto, sin usar todo el documento.

## Autenticación y secretos

NONE exige authConfig omitido/null. BEARER_TOKEN requiere authConfig.secretValue y prohíbe headerName. API_KEY_HEADER exige headerName y secretValue; X-API-Key/API-Key son válidos aquí, pero se prohíben nombres sensibles de sesión/Authorization y de transporte/proxy.

secretValue es write-only: obligatorio al crear con autenticación, hasta 8192 bytes UTF-8, no vacío/blanco ni controles; se conserva exactamente sin trim. Bearer no admite espacios. Se rechazan surrogates UTF-16 altos o bajos aislados antes de cifrar o escribir: 400 VALIDATION_ERROR e INVALID_CHARACTER para authConfig.secretValue, sin reflejar el valor. Los pares válidos se aceptan sin alterar el carácter suplementario. Un PUT inválido conserva configuración y credencial anteriores. No se admiten credenciales por URL, plantilla o headers ordinarios; no se promete detectar automáticamente cualquier secreto arbitrario en texto libre.

GET nunca descifra ni consulta la clave. Solo se devuelve authType, hasCredentials y authConfig con headerName para API_KEY_HEADER (null para otros tipos). Nunca secreto, máscara, ciphertext, clave, nonce ni metadatos criptográficos.

PUT reemplaza campos ordinarios; omitir secretValue conserva el blob solo manteniendo origen URL normalizado, authType y headerName exacto, con authConfig presente ({} para Bearer). Cambiar cualquiera de esos elementos exige secreto nuevo. null/vacío no conserva. NONE elimina la credencial en la misma transacción.

AES-256-GCM y el envelope versionado se describen en [ADR-002](../02-arquitectura/decisiones/ADR-002-autenticacion-target-credenciales.md). La clave externa Base64 de 256 bits se suministra mediante TARGET_CREDENTIAL_ENCRYPTION_KEY, sin valor por defecto. Ausencia/formato inválido impide cifrar con 500 INTERNAL_ERROR y sin cambios parciales. Consulta/conservación no necesitan clave. No se registran entradas, secretos ni causas internas; no habilitar logs de SQL/bindings/cuerpos para este flujo.

## Errores

Forma {code,message,fieldErrors}, mensajes en español sin valores rechazados. 400: INVALID_REQUEST (estructura, claves duplicadas/desconocidas), INVALID_PROJECT_ID o VALIDATION_ERROR. 404: PROJECT_NOT_FOUND/TARGET_NOT_CONFIGURED. 409: TARGET_ALREADY_CONFIGURED. 500: INTERNAL_ERROR, sin SQL, conexión ni detalles criptográficos. fieldErrors solo se usa en validación, con rutas contractuales y REQUIRED, INVALID_TYPE, TOO_LONG, INVALID_FORMAT, INVALID_CHARACTER, OUT_OF_RANGE, NOT_ALLOWED o CONFLICT.

## Fuera del MVP / Evolución futura

Ejecución HTTP, extracción real, múltiples targets, historial, borrado, protocolos distintos de REST/POST/JSON, OAuth2, mTLS, cookies/sesión, AWS y rotación avanzada requieren otros changes. La concurrencia de PUT aplica última escritura sin detección de cambios perdidos.
