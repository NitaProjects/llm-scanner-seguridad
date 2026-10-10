# ADR-002: autenticación contra el target y gestión de credenciales

## Estado

Aceptada para el alcance funcional del MVP.

## Contexto

Algunas aplicaciones LLM objetivo no requieren autenticación, mientras que otras utilizan tokens Bearer o API keys en headers. El scanner necesita cubrir estos casos sin ampliar inicialmente el alcance a flujos empresariales interactivos o multi-step.

## Decisión funcional

El MVP soportará únicamente:

- `NONE`.
- `BEARER_TOKEN`.
- `API_KEY_HEADER`.

Quedan fuera del MVP:

- OAuth2.
- mTLS.
- Basic Auth avanzada.
- Autenticación basada en cookies o sesión.
- Flujos interactivos.
- Autenticación multi-step.

La configuración conceptual del target incluirá:

```text
authType
authConfig
```

Para `BEARER_TOKEN`:

```text
Authorization: Bearer <TOKEN>
```

Para `API_KEY_HEADER`:

```text
headerName
secretValue
```

Ejemplo:

```text
X-API-Key: <SECRET>
```

## Reglas de gestión de secretos

- Las credenciales nunca deben aparecer en logs.
- Nunca deben incluirse en informes.
- Nunca deben almacenarse en texto plano.
- No deben exponerse al frontend después de ser configuradas.
- Los errores no deben devolver secretos.
- Los payloads persistidos deben sanitizar headers sensibles.
- El secreto solo está en memoria durante recepción/cifrado y durante su uso por el adaptador de invocación autorizado. GET y conservación en PUT no lo descifran.

Flujo conceptual:

```text
Frontend envía token/API key
    -> backend cifra el secreto
    -> PostgreSQL almacena el valor cifrado
    -> backend descifra en memoria durante la ejecución
    -> se utiliza en la request al target
    -> no se devuelve ni se registra
```

## Fuera del MVP / Evolución futura

Quedan fuera del MVP OAuth2, mTLS, Basic Auth avanzada, cookies o sesiones, flujos interactivos y autenticación multi-step.

En fases posteriores podrán incorporarse rotación avanzada, gestión multi-entorno y proveedores externos de secretos.

## Gestión futura en AWS

Para fases posteriores y AWS se considerará la utilización conjunta de AWS Secrets Manager y AWS KMS: Secrets Manager para almacenar y gestionar secretos, y KMS para gestionar las claves de cifrado. Esto no fija todavía una tecnología concreta ni forma parte de la implementación del MVP.

## D-CRYPTO: decisión aprobada para el MVP local

El change `target-configuration` concreta AES-256-GCM mediante JCA de Java 21, sin dependencias adicionales. La clave externa tiene exactamente 256 bits, codificada en Base64 estándar y suministrada por `TARGET_CREDENTIAL_ENCRYPTION_KEY`. No hay valor por defecto ni generación automática. Debe conservarse de forma segura fuera de PostgreSQL y del repositorio para recuperar credenciales tras reinicio.

El envelope binario v1 se almacena en BYTEA: un byte de versión (1), 12 bytes de nonce aleatorio generado con SecureRandom y el ciphertext seguido de 16 bytes de tag GCM. Se utiliza AES/GCM/NoPadding y la versión como AAD. Cada cifrado genera un nonce nuevo. La clave no forma parte del envelope. El parser rechaza versiones desconocidas y material truncado; la verificación GCM con clave incorrecta o material alterado falla sin producir plaintext utilizable.

El recurso de configuración consulta la clave únicamente al cifrar. Su ausencia, formato inválido o tamaño distinto de 32 bytes provoca un error interno sanitizado y ninguna escritura, sin degradar a NONE. GET y PUT que conservan credencial funcionan sin consultar la clave.

security-test-execution añade CredentialDecryptionPort separado del contrato de configuración. El mismo adaptador AES reutiliza provisión de clave/envelope/JCA y descifra solo después de autorizar y validar destino, inmediatamente antes del request. NONE no invoca crypto. Un recurso efímero limpia los buffers propios; no se promete borrado garantizado de Strings del runtime. Error de clave/envelope/tag/secreto utilizable produce CREDENTIAL_UNAVAILABLE sin causa, envío ni degradación. El adaptador HTTP redacta coincidencias exactas conocidas en cadenas/claves JSON decodificadas y texto extraído, marca flags y no retorna plaintext, blobs o headers. El snapshot conserva el cifrado original solo en memoria durante el Scan. Las pruebas preparadas usan claves y certificados exclusivamente sintéticos.

La actualización conserva el blob únicamente con el mismo origen URL, tipo de autenticación y headerName exacto, con authConfig presente y secretValue omitido. Un nuevo secreto reemplaza el anterior; NONE elimina el blob en la misma transacción.

## Decisiones pendientes fuera de este change

- Provisión operativa y custodia de la clave en PRE/PRO y AWS.
- Uso de un gestor de secretos en PRE/PRO y AWS.
- Rotación, revocación y ciclo de vida de credenciales.
- Retención/borrado de evidencia y redacción avanzada de secretos arbitrarios; la redacción exacta de credenciales conocidas queda cerrada por security-test-execution.

## Consecuencias

El MVP cubre la demo sin autenticación y dos patrones habituales de APIs protegidas, manteniendo fuera los flujos complejos. La gestión de secretos se convierte en una preocupación transversal de seguridad y deberá resolverse antes de utilizar targets reales o desplegar en AWS.
