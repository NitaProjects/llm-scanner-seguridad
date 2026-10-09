## Why

Un proyecto ya puede crearse y consultarse, pero aún no dispone de una configuración separada para describir su aplicación LLM objetivo. Este cambio define esa configuración persistente y sus garantías de seguridad sin ejecutar peticiones ni iniciar scans.

## What Changes

- Crear, consultar y actualizar una única `TargetConfiguration` por proyecto existente, respetando la cardinalidad MVP aprobada `Project → 0..1 TargetConfiguration`.
- Mantener el contrato aprobado REST/POST/JSON, con URL, plantilla, `{{PROMPT}}`, headers, timeout y extracción mediante JSONPath restringido a propiedades e índices.
- Definir `NONE`, `BEARER_TOKEN` y `API_KEY_HEADER`, separando headers no sensibles y credenciales de entrada que nunca regresan al cliente.
- Persistir credenciales cifradas mediante AES-256-GCM, con clave externa de 256 bits, nonce aleatorio nuevo y material cifrado versionado; D-CRYPTO queda cerrado para el MVP local.
- Aplicar timeout por defecto de 10000 ms si se omite, dentro del rango aprobado 1000..60000 ms.
- Bloquear por defecto localhost, loopback y redes privadas, con excepción explícita y controlada para local/demo; sin implementar ejecución HTTP ni protección SSRF completa aquí.
- No persistir method, contentType ni promptPlaceholder: son constantes del MVP y se mantienen en el contrato público.
- Distinguir validación de configuración sin acceso a red y controles SSRF obligatorios durante la futura ejecución.
- Definir contrato REST, errores sanitizados, persistencia conceptual y tareas trazables, sin alterar `Project` ni sus requisitos aprobados.

## Non-Goals

No se incluye ejecución HTTP, comprobación de conectividad, resolución DNS, implementación del control de red efectivo, scans, casos de prueba de seguridad, ejecuciones, findings, scoring, evaluador LLM, payloads OWASP, informes, frontend completo, AWS, usuarios ni autenticación de usuarios. No se incluyen múltiples targets, selección de target activo, borrado, historial de versiones ni clonación de credenciales.

Quedan fuera los protocolos y autenticaciones excluidos por ADR-001/ADR-002: GET contra el target, streaming, multipart, WebSocket, SSE, SOAP, gRPC, OAuth2, mTLS, cookies/sesión, flujos interactivos, agentes complejos y herramientas externas. El `GET` de la API interna para consultar configuración no es una petición al target.

Esta entrega únicamente crea artefactos OpenSpec. No implementa código, SQL ni migraciones, no instala dependencias y no modifica documentación consolidada ni `project-management`.

## Capabilities

### New Capabilities

- `target-configuration`: configuración de un target asociado a un proyecto, con validación local, autenticación segura, consulta sin secretos, actualización y persistencia.

### Modified Capabilities

Ninguna. Se utiliza la gestión de proyectos existente sin cambiar su contrato. `openspec/specs/` está vacío; se ha revisado el change implementado `project-management` como referencia funcional y técnica.

## Impact

- La implementación futura incorporará dominio/aplicación/adaptadores de target y composición siguiendo las convenciones de proyectos.
- Se proponen `POST`, `GET` y `PUT /api/projects/{projectId}/target`, sin llamadas al target ni cambios en la representación de `Project`.
- Se propone una tabla separada `target_configurations`, FK hacia `projects`, unicidad de `project_id` y credencial cifrada separada de los headers ordinarios. V1 no vuelve a modificarse; se utilizará la siguiente migración disponible.
- La aprobación del usuario concreta para el MVP el modelo conceptual 1→N de `docs/06-base-de-datos/modelo-datos.md` en 0..1, manteniendo múltiples configuraciones como evolución futura. Su actualización documental se planifica dentro de la implementación posterior.
- Los contratos/documentos afectados que deberán actualizarse durante la implementación son `docs/05-api/openapi.yaml`, `docs/05-api/target-contract.md`, la sección de target del modelo de datos, ADR-001/modelo de amenazas por la excepción local/demo y ADR-002/decisiones abiertas por el cierre criptográfico aprobado. Esta entrega modifica únicamente OpenSpec; los textos consolidados anteriores no reabren las decisiones del usuario.
- Referencias revisadas: `AGENTS.md`, perfiles de arquitectura, `openspec/config.yaml`, change `project-management`, alcance/backlog, arquitectura general, decisiones abiertas, ADR-001/ADR-002, modelo de amenazas, contrato del target, OpenAPI y modelo de datos. `docs/03-seguridad/politica-de-autorizacion.md` está vacío; no se deduce de él una política de autorización aprobada.
- Las propuestas quedan aprobadas con las concreciones del usuario. No hay decisiones pendientes que bloqueen implementar este change para el MVP local. AWS y la protección SSRF completa en ejecución se resolverán en cambios posteriores.
