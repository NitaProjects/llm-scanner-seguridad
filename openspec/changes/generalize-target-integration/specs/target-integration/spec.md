## Purpose

Permitir configurar integraciones versionadas con aplicaciones LLM autorizadas, manteniendo compatibilidad histórica y una experiencia progresiva según el protocolo soportado.

## ADDED Requirements

### Requirement: TI-01 Configuración versionada y compatibilidad

**Historia:** Como operador quiero elegir el contrato de un target sin perder la configuración o los resultados v1.
**Prioridad:** Alta, M1/D1. **Dependencias:** TC-01..TC-11 históricos; NI-01; IS-01.

El sistema SHALL mantener un target por Project y permitir revisiones inmutables de integración con tipo de adapter y versión de esquema. Ausencia de revisión v2 SHALL significar configuración legacy. Crear/editar/validar configuración SHALL ser local, sin DNS, HTTP ni descifrado de credenciales conservadas. La configuración fijada al preparar el plan SHALL permanecer estable durante el Scan. Las APIs v1 SHALL conservar sus contratos sobre legacy; un cliente v1 SHALL recibir incompatibilidad explícita al intentar editar un target v2 o leer resultados de perfil no soportado, sin conversión destructiva. Actualizaciones v2 SHALL detectar una revisión obsoleta con conflicto y sin sobrescribir.

#### Scenario: Legacy y actualización concurrente
- **WHEN** un target legacy se ejecuta y después se convierte explícitamente a v2 mientras otro cliente guarda una revisión anterior
- **THEN** el Scan conserva su snapshot original y la escritura obsoleta se rechaza sin perder la revisión vigente ni credenciales
- **AND** los resultados históricos conservan versiones y significado v1

### Requirement: TI-02 REST JSON con selectores restringidos

**Historia:** Como analista quiero insertar un prompt y extraer texto de JSON anidado sin programar un adapter.
**Prioridad:** Alta, M1/D1. **Dependencias:** TI-01; IS-01..IS-03.

El sistema SHALL soportar POST JSON, headers validados, NONE/Bearer/API key, timeout y límites. La entrada SHALL seleccionar exactamente un nodo existente y sustituirlo por el prompt String mediante serialización JSON segura. La salida SHALL seleccionar exactamente un String. El subconjunto JSONPath SHALL soportar raíz, propiedades, corchetes, arrays e índices no negativos, y `[-1]` solo en lectura sobre array no vacío; SHALL rechazar scripts, filtros, recursión, comodines y resultados múltiples. Valor ausente, tipo incorrecto o JSON inválido SHALL producir error tipado, nunca conversión silenciosa a texto. El contrato legacy de `{{PROMPT}}` SHALL seguir disponible con su validación original.

#### Scenario: Prompt con caracteres especiales y array anidado
- **WHEN** el prompt contiene comillas/saltos y se inserta en `$.messages[0].content`, extrayendo `$.result.messages[-1].text`
- **THEN** se envía un JSON válido con un único valor sustituido y se devuelve el String seleccionado intacto

#### Scenario: Selector peligroso o resultado no textual
- **WHEN** se configura un filtro ejecutable o la respuesta seleccionada es un objeto/array vacío para índice final
- **THEN** se rechaza la configuración o se registra error de extracción, sin evaluar un texto inventado

### Requirement: TI-03 Chat compatible independiente del proveedor

**Historia:** Como operador quiero usar APIs chat compatibles sin lógica comercial en el motor de seguridad.
**Prioridad:** Alta, M2/D1. **Dependencias:** TI-01; NI-01; RV-01.

La integración SHALL aceptar endpoint completo, modelo, system opcional declarado, prompt de prueba como mensaje user y autenticación protegida, enviando `stream=false` y una elección. SHALL extraer `choices[0].message.content` cuando sea String y la respuesta esté completa según el contrato. Final por longitud, tool calls, contenido multimodal/null o error remoto SHALL conservar diagnóstico de insuficiencia/no soporte, sin ejecutar herramientas ni inferir éxito por HTTP 200. El dominio y la consolidación SHALL NOT seleccionar comportamiento por Groq, OpenRouter, Ollama u otro nombre comercial.

#### Scenario: Dos proveedores con el mismo contrato
- **WHEN** dos endpoints autorizados devuelven el mismo contenido y final compatible bajo el mismo criterio
- **THEN** ambos producen entrada normalizada equivalente y utilizan el mismo motor de seguridad

#### Scenario: Respuesta limitada o de herramienta
- **WHEN** la elección termina por límite de tokens o contiene tool calls en lugar de respuesta textual completa
- **THEN** no se ejecuta ninguna herramienta ni se registra una respuesta completa ficticia

### Requirement: TI-04 Configuración progresiva y validación sin tráfico

**Historia:** Como usuario quiero configurar únicamente los campos del tipo de target elegido.
**Prioridad:** Alta D1/D2; workflow M4. **Dependencias:** TI-01..03; TS-01; TW-01.

La UI SHALL ofrecer REST JSON, OpenAI-compatible, Streaming y Workflow avanzado solo cuando estén disponibles. SHALL mostrar los campos aplicables, previsualización local segura y errores por campo/paso. Workflow SHALL usar tarjetas ordenadas y selectores, sin exigir JSON/YAML crudo. Guardar o abrir la pantalla SHALL NOT contactar con el target. Credenciales SHALL ser de escritura, con conservar/reemplazar/eliminar y limpieza transitoria; cambiar origen SHALL NOT transferir el secreto automáticamente. Diagnósticos técnicos, veredicto y cobertura SHALL presentarse separados y como texto inerte.

#### Scenario: Cambio de tipo y guardado
- **WHEN** el usuario cambia de REST a chat compatible y guarda
- **THEN** ve los campos de chat y la revisión segura resultante, sin tráfico remoto, secretos recuperados ni campos incompatibles aplicados silenciosamente
