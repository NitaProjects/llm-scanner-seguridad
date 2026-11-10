## Purpose

Establecer una frontera interna mínima consumida por la ejecución real de pruebas, preservando sin cambios observables el contrato REST y los resultados de la versión v1.

## ADDED Requirements

### Requirement: TAB-01 Frontera única efectivamente utilizada

**Historia:** Como mantenedor quiero que el caso de uso dependa de una interacción mínima y no invoque directamente el transporte legacy.
**Prioridad:** Alta D0. **Dependencias:** contratos v1 de ejecución vigentes, sin ampliarlos.

La ejecución real SHALL consumir una frontera de aplicación con una única operación de interacción. Cada intento elegible SHALL atravesarla y delegar exactamente una vez en la invocación REST existente, conservando target snapshot, prompt y misma instancia de presupuesto. SHALL NOT existir una ruta alternativa directa desde el use case al puerto de transporte. La nueva frontera SHALL NOT añadir operaciones de capabilities/describe/registry, lifecycle, red propia, retries, sesiones o protocolos. Presupuesto agotado antes de invocar y fallo de preparación SHALL conservar la ausencia de llamada actual.

#### Scenario: Plan normal y presupuesto agotado
- **WHEN** se ejecuta un plan válido con presupuesto suficiente y después se caracteriza un caso agotado antes del primer envío
- **THEN** el primer caso atraviesa la frontera cuatro veces en el orden actual y el segundo no la invoca ni realiza red

### Requirement: TAB-02 Resultado mínimo y proyección íntegra

**Historia:** Como consumidor del resultado quiero conservar toda la información v1 al introducir el valor normalizado.
**Prioridad:** Alta D0. **Dependencias:** TAB-01; invariantes existentes de TestExecution.

El resultado en memoria SHALL representar responseText, latencyMs, errorReason y los flags requestRedacted/responseRedacted, más el par legacy httpStatus/requestBody necesario para compatibilidad. Esos siete nombres y tipos SHALL corresponder a los de InvocationResult, sin responseBody adicional. El estado técnico SHALL conservarse mediante ExecutionError existente, sin clasificarlo de nuevo por HTTP. La conversión resultado legacy→normalizado→legacy SHALL preservar por igualdad todos los valores y nulabilidad, incluidos errores con metadata, String vacío, Unicode y ambos flags independientes. SHALL NOT generar valores por defecto, reextraer/sanitizar texto, medir otra latencia ni persistir el modelo. Representaciones textuales diagnósticas SHALL NOT exponer cuerpos o respuestas.

#### Scenario: Resultado con evidencia y redacción
- **WHEN** una invocación produce valores conocidos distintos en los siete campos, incluido un error con metadata y flags diferentes
- **THEN** el resultado proyectado entregado a persistencia es igual campo a campo al original, sin eliminar metadata por tratarse de un fallo

#### Scenario: Nulabilidad y cadenas especiales
- **WHEN** un resultado contiene nulls válidos, texto vacío, NUL o caracteres Unicode
- **THEN** la conversión conserva exactamente los valores sin serialización, recorte, normalización ni nuevas validaciones

### Requirement: TAB-03 Semántica de ejecución y seguridad invariantes

**Historia:** Como operador quiero que el refactor no cambie errores, tráfico o protecciones del scanner.
**Prioridad:** Alta D0. **Dependencias:** TAB-01/02; políticas de seguridad y errores v1.

El flujo SHALL conservar prepare/start antes de invocar, finish antes de evaluar y las mismas fronteras transaccionales. SHALL conservar errores y su alcance individual/global, precedencia de presupuesto/interrupción, RuntimeException de invocación→EXECUTION_INTERNAL_ERROR y aborto inmediato tras fallo de persistencia sin nueva petición. El adapter HTTP existente SHALL continuar aplicando los mismos SSRF, autorización, DNS/IP fijada, TLS, autorización previa a descifrado, cifrado, límites/timeout, redacción y cierre de recursos; el bridge SHALL NOT duplicarlos, debilitarlos ni cerrar nuevamente el delegado compartido. Prompts, JSON enviado, headers, responsePath y texto saneado SHALL permanecer idénticos.

#### Scenario: Fallo individual, global y de persistencia
- **WHEN** fixtures reproducen un fallo individual, uno global y un fallo en finish
- **THEN** se conserva respectivamente continuación con fallo final, aborto de pendientes y aborto sin repetir ni continuar invocaciones

#### Scenario: Destino bloqueado y recursos
- **WHEN** la política existente deniega el destino o el transporte termina por timeout/interrupción
- **THEN** se mantienen el código/precedencia, la ausencia de descifrado/envío cuando corresponda y la liberación de recursos original, sin conexión adicional

### Requirement: TAB-04 Equivalencia del pipeline y verificación aislada

**Historia:** Como revisor quiero evidencia de compatibilidad completa, no solo de que existen tipos nuevos.
**Prioridad:** Alta D0. **Dependencias:** TAB-01..03.

La verificación SHALL demostrar ruta real mediante wiring y fixtures deterministas, preservación de los siete campos, cuatro ejecuciones, evaluaciones, Findings, score y report-v1 bajo los mismos inputs. Fakes del puerto legacy seleccionados por el contenedor SHALL seguir siendo efectivos a través del bridge; SHALL NOT sustituirse por clientes HTTP reales. La automatización SHALL usar mocks/fakes y, donde sea necesario para probar transporte, servidores locales controlados; SHALL NOT llamar proveedores/targets externos reales. Las suites existentes SHALL conservar sus expectativas y pasar cuando se ejecuten en su entorno autorizado; pruebas omitidas por entorno SHALL informarse como no ejecutadas. D0 SHALL NOT requerir cambios de frontend, API/DTOs, BD/JPA/migraciones, perfiles, scoring o reporting para obtener equivalencia.

#### Scenario: Pipeline sin servicios externos
- **WHEN** se ejecuta la fixture de cuatro tests con invocación y evaluador deterministas a través del wiring real
- **THEN** se conservan los resultados esperados de evaluación/Findings/score/report y el contador de peticiones externas es cero

#### Scenario: Integraciones no habilitadas
- **WHEN** las suites PostgreSQL no disponen de entorno dedicado autorizado
- **THEN** su validación queda pendiente y no se afirma regresión completa ni equivalencia ejecutada a partir de pruebas omitidas
