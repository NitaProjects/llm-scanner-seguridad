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
- El secreto solo debe estar disponible en memoria durante la ejecución autorizada del scan.

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

## Decisiones pendientes

- Algoritmo y mecanismo concreto de cifrado.
- Ubicación definitiva de la clave maestra.
- Uso de un gestor de secretos en local, preproducción y producción.
- Rotación, revocación y ciclo de vida de credenciales.
- Política de redacción y retención de payloads persistidos.

## Consecuencias

El MVP cubre la demo sin autenticación y dos patrones habituales de APIs protegidas, manteniendo fuera los flujos complejos. La gestión de secretos se convierte en una preocupación transversal de seguridad y deberá resolverse antes de utilizar targets reales o desplegar en AWS.
