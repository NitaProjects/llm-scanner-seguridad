## Purpose

Permitir describir sesiones y obtención diferida de respuestas mediante pasos declarativos seguros y acotados, sin código específico para cada aplicación objetivo.

## ADDED Requirements

### Requirement: TW-01 Workflow declarativo validable

**Historia:** Como usuario quiero crear sesión, enviar prompt y obtener resultado sin programar.
**Prioridad:** Media M4, fuera D1/D2. **Dependencias:** TI-04; TS-01..03; IS-01..04.

El workflow SHALL tener esquema versionado, orígenes fijos, pasos ordenados HTTP/poll/stream, headers/body, extracción tipada JSONPath, guards restringidos y selector final único. SHALL admitir GET/POST y cleanup DELETE explícito, no métodos/expresiones arbitrarios. SHALL validarse localmente sin ejecución, detectar referencias inexistentes, dependencias futuras, tipos erróneos y ciclos. Condiciones SHALL limitarse a exists/equals/in y status declarado; polling SHALL ser la única repetición. No SHALL permitir scripts, shell, motor de expresiones, imports remotos ni goto. La representación interna SHALL ser JSON estricto coherente con el editor visual, sin exigir YAML.

#### Scenario: Sesión, envío y resultado
- **WHEN** se declara crear sesión, extraer sessionId, enviar prompt, esperar por polling y seleccionar el último mensaje
- **THEN** el resultado se normaliza por la misma frontera que REST/SSE, sin lógica específica de la aplicación en el dominio

### Requirement: TW-02 Variables y aislamiento

**Historia:** Como operador quiero usar IDs de pasos anteriores sin que una respuesta pueda alterar el destino o la política.
**Prioridad:** Alta M4. **Dependencias:** TW-01; IS-01..02.

Las variables SHALL ser tipadas, acotadas, de ámbito por ejecución y sensibles por defecto. sessionId/conversationId/messageId SHALL ser datos no confiables. SHALL insertarse como valores JSON o segmentos/query codificados, nunca como origen, nombre de cabecera, permiso o referencia de secreto. Inputs reservados SHALL ser inmutables; variables ausentes/incorrectas SHALL fallar antes del envío dependiente. Cada TestExecution SHALL crear contexto de sesión independiente y eliminar referencias locales al finalizar; SHALL NOT reutilizar sesión entre categorías.

#### Scenario: Variable intenta cambiar destino
- **WHEN** un sessionId extraído contiene URL, separadores de ruta o traversal
- **THEN** se rechaza la interpolación insegura y no se realiza el envío dependiente ni se transmite una credencial a otro origen

### Requirement: TW-03 Polling, limpieza y límites compuestos

**Historia:** Como operador quiero limitar el coste de un workflow y conocer fallos de limpieza.
**Prioridad:** Alta M4. **Dependencias:** TW-01..02; IS-03.

El workflow SHALL fijar máximo de pasos/variables/requests, intervalos y número de polls, timeout por operación y deadline absoluto compartido con Scan. Poll SHALL distinguir éxito, fallo terminal y agotamiento; SHALL NOT reenviar el POST como polling. Retry y cleanup SHALL consumir el mismo presupuesto. Cleanup remoto SHALL ser opcional, autorizado y limitado a recursos creados por la ejecución; su error SHALL quedar visible sin ocultar el principal. Tras caída/reinicio SHALL NOT reanudarse ni repetirse automáticamente una sesión.

#### Scenario: Poll agotado y cleanup fallido
- **WHEN** el resultado nunca alcanza la condición final y la limpieza también falla dentro de su límite
- **THEN** el workflow termina fallido con causa original y warning de cleanup, sin bucle infinito ni garantía ficticia de borrado remoto
