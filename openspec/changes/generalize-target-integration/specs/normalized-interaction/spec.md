## Purpose

Establecer una frontera de interacción y evidencia estable entre los protocolos del target, la ejecución de pruebas y la evaluación híbrida de seguridad.

## ADDED Requirements

### Requirement: NI-01 Resultado neutral y reproducible

**Historia:** Como motor de seguridad quiero consumir una interacción normalizada sin depender de HTTP o un proveedor.
**Prioridad:** Alta M1/D1. **Dependencias:** STE-02 histórico; TI-01.

Cada interacción SHALL identificar Scan, ejecución, target/revisión, adapter/versión y esquema. SHALL representar prompt efectivo saneado o referencia, metadata segura, texto final, referencias de evidencia, estado técnico COMPLETE/INCOMPLETE/FAILED, latencia monotónica, protocolo, proveedor/modelo opcionales, warnings, partial/truncated/redacted y categoría de error. La evaluación SHALL consumir texto/criterio/evidencia normalizados, nunca estructuras HTTP, frames SSE ni credenciales. Metadata desconocida SHALL ser null, no inventada. Un error de transporte SHALL NOT representar una vulnerabilidad.

#### Scenario: Igual contenido por protocolos distintos
- **WHEN** REST y SSE producen el mismo prompt, texto final, contexto, completitud y señales
- **THEN** la evaluación recibe evidencia semánticamente equivalente sin ramificarse según protocolo o proveedor

### Requirement: NI-02 Completitud y lifecycle compatibles

**Historia:** Como analista quiero distinguir una respuesta completa, una interrupción y un resultado inconcluso de seguridad.
**Prioridad:** Alta D1/D2. **Dependencias:** NI-01; STE-03 y cierre de reporting históricos.

COMPLETE SHALL requerir final válido según el contrato, texto seleccionado y límites respetados. INCOMPLETE SHALL conservar motivo y solo prefijo saneado permitido; FAILED SHALL incluir código seguro. En D1/D2 INCOMPLETE/FAILED SHALL terminalizar TestExecution FAILED y aplicar la política Scan FAILED/evaluación abortada vigente; SHALL NOT incorporarse estados Scan PARTIAL/CANCELLED ni evaluar el prefijo como completo. Una interacción completa sin criterio suficiente SHALL permitir evaluación COMPLETED/INCONCLUSIVE. Los nuevos perfiles SHALL conservar exactamente cuatro pruebas/evaluaciones y score+informe+cierre COMPLETED atómicos. Las invariantes HTTP de v1 SHALL permanecer para v1; los perfiles nuevos SHALL validar éxito neutral sin exigir HTTP como propiedad universal.

#### Scenario: Stream cortado y criterio ausente
- **WHEN** un stream se corta antes del terminal y otro target devuelve texto completo sin contexto aplicable
- **THEN** el primero produce fallo técnico con evidencia parcial identificada y el segundo evaluación inconclusa
- **AND** ninguno se presenta como prueba de seguridad o vulnerabilidad

### Requirement: NI-03 Procedencia y separación de evidencias

**Historia:** Como revisor quiero distinguir observación de transporte, texto normalizado, proyección evaluadora y evidencia del hallazgo.
**Prioridad:** Alta M1/D1. **Dependencias:** IS-04; RV-01.

El sistema SHALL separar esas cuatro capas mediante referencias versionadas y transformaciones trazables. Los bytes crudos SHALL estar acotados en memoria; persistir contenido de transporte SHALL requerir política explícita y saneamiento. Cada cita SHALL referirse a la proyección realmente evaluada con offsets verificables; redacción que invalide el criterio SHALL producir insuficiencia, no una cita reconstruida. Findings SHALL depender de criterio y evidencia verificable, no de códigos HTTP. El texto normalizado SHALL preservar contenido/orden sin render, ejecución ni reparación semántica.

#### Scenario: Redacción que elimina una prueba
- **WHEN** el saneamiento elimina la única referencia que permitía comprobar el criterio
- **THEN** la evaluación queda INCONCLUSIVE y no genera un Finding sustentado en bytes que ya no forman parte de la proyección verificable

### Requirement: NI-04 Observabilidad segura de adapters

**Historia:** Como operador quiero diagnosticar errores de integración sin exponer contenido.
**Prioridad:** Alta D1, extensiones M3/M4. **Dependencias:** IS-03..04.

El sistema SHALL correlacionar Scan/execution/interaction y paso/intento con IDs locales; registrar tipo/versión de adapter, categoría técnica, tiempos, contadores y causa de finalización. Métricas SHALL diferenciar fallos de transporte y evaluador, inconclusos y límites de stream. Logs/labels SHALL NOT contener secretos, sesiones remotas, cuerpos, texto de respuesta, cabeceras o URLs libres; labels SHALL tener cardinalidad acotada. No SHALL atribuirse latencia total de red al tiempo de inferencia del modelo.

#### Scenario: Error remoto sensible
- **WHEN** un endpoint devuelve un mensaje de error con token o contenido privado
- **THEN** el diagnóstico publica solo código seguro, identificadores locales y contadores, sin copiar el mensaje remoto
