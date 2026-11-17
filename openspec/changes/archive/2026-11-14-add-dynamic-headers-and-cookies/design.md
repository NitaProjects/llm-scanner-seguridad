## Context

Las sesiones capturan handles sensibles. Los workflows proporcionan los destinos declarados que pueden consumirlos.

## Goals / Non-Goals

Usar cookies de sesión y tokens CSRF en bindings explícitos por paso y mismo origen. Depende de sesiones y workflows.

## Decisions

CredentialBinding declara fuente, paso consumidor, nombre de cookie o header permitido y origen exacto del exchange de captura. Ámbito de una TestExecution. Verificar igualdad de scheme/host/puerto normalizados, path y expiración para cookies. Aplicar Secure y expiración Max-Age/Expires con precedencia estándar. Para el primer incremento admitir cookies host-only sin atributo Domain, rechazar expiradas/ambiguas e invalidar las eliminadas por el servidor. Mantener los controles de path sin semántica de navegador. HttpOnly no habilita exponer el valor a UI/JSON, SameSite nunca amplía el ámbito same-origin. Solo cookies declaradas, sin jar automático ni forwarding por redirects. No derivar Authorization, Host, Cookie libre, headers hop-by-hop, Content-Length ni nombres dinámicos. El emisor de cookies es el único que compone Cookie desde bindings válidos.

Permitir headers como X-CSRF-Token mediante lista explícita de nombres admitidos por configuración de operador. Validar nombre y valor, sin CR/LF, caracteres de control, duplicados ni overrides de auth estática. Cotas de variables y headers previas. Una credencial requerida expirada, ausente o de origen/path distinto aborta antes de enviar. Resolución de handle solo después de autorización, DNS pinning y configuración TLS del destino. Cero redirects incluso same-origin. Presupuestos, timeout y cuota del paso siguen aplicándose.

Proteger cada valor en memoria y redactar coincidencias conocidas en texto ensamblado y evidencia. Nunca incluir valores/headers/cookies en logs, snapshots, URL, contexto de evaluación o storage de frontend. Borrar referencias al finalizar el scope. Captura y envío no añaden exchanges. Sin columnas, API pública o UI adicionales. La activación es por manifiesto y puede retirarse para scans futuros según el [roadmap](../generalize-target-integration/roadmap.md).

## Risks / Trade-offs

Same-origin es necesario pero insuficiente para una cookie. Path, Secure, expiración y bindings explícitos cierran la propagación accidental. El límite host-only deja fuera los targets que dependen de cookies de dominio compartido.
