## Context

Estado propuesto: **READY FOR REVIEW**, pendiente de aprobación e implementación. Motivación en `proposal.md`. `generalize-target-integration` es únicamente roadmap: este documento no adopta su modelo normalizado ni sus requisitos.

Fuentes revisadas: `AGENTS.md`, perfil architecture-agent, `openspec/config.yaml`, arquitectura general y ADR-001/002; contratos históricos de ejecución/evaluación/scoring/reporting; `InvocationResult`, `TargetInvocationPort`, `ExecuteSecurityTestsUseCase`, `TestExecution`, `ExecutionError`, `ScanConfiguration`, `SecurityTestExecutionConfiguration`, adapter HTTP y tests dirigidos. No se identifica una divergencia de comportamiento que deba corregirse en D0.

El use case hace prepare → start → invoke → finish → evaluación por cada test. El control de presupuesto previo evita invocar cuando está agotado. Captura RuntimeException de invocación como EXECUTION_INTERNAL_ERROR; finish queda fuera del catch y un fallo de escritura aborta sin repetir red. `errorReason.global()` determina si continúa tras un error individual. Al finalizar exige todas las ejecuciones/evaluaciones completas, presupuesto e hilo no interrumpido.

`TestExecution` v1 exige para COMPLETED httpStatus 2xx, latencyMs, requestBody y responseText no nulos, sin error. Los flags no pueden ser true si falta su texto. Estas validaciones y las de persistencia continúan exactamente en sus lugares actuales. `InvocationResult` es un record sin validación propia y no contiene un body bruto de respuesta: `responseText` es el texto extraído por responsePath y saneado. No inventar un octavo campo responseBody.

## Goals / Non-Goals

**Goals:** introducir una única frontera efectivamente usada, convertir sin pérdida al resultado legacy y conservar todas las decisiones, datos y efectos observables v1. Tipos nuevos solo de aplicación/adapters; dominio y persistencia no cambian.

**Non-Goals:** los de proposal.md; no añadir capacidades futuras, otro estado de Scan/TestExecution, status enum duplicado, cambios de payload/extracción/sanitización, validaciones nuevas, I/O extra o controles de red duplicados.

## Decisions

### D0-01 Contrato exacto del puerto

Contrato propuesto en `application/execution/TargetAdapter.java` (firma de diseño, no código implementado):

```java
public interface TargetAdapter {
    NormalizedInteraction interact(TargetConfiguration target, String prompt, ScanBudget budget);
}
```

Se conservan los tres argumentos existentes para no inventar otro request/contexto/budget. El mismo target snapshot, el mismo prompt y la misma instancia mutable de presupuesto atraviesan la llamada; no se copian ni reinician. Una llamada representa un intento existente, sin retry, prepare, collect, cleanup público, describe, capabilities o registro de plugins.

Alternativas: renombrar simplemente TargetInvocationPort no deja la frontera/proyección solicitada; añadir un framework de adapters excede D0. Mantener ambos puertos con responsabilidades claras: TargetAdapter es la frontera consumida por el caso de uso; TargetInvocationPort sigue siendo el contrato del transporte legacy detrás del bridge.

### D0-02 NormalizedInteraction mínimo

Modelo propuesto en `application/execution/NormalizedInteraction.java`, no entidad de dominio persistido:

```java
public record NormalizedInteraction(
    String responseText,
    Long latencyMs,
    ExecutionError errorReason,
    boolean requestRedacted,
    boolean responseRedacted,
    LegacyCompatibility legacy
) {
    public record LegacyCompatibility(Integer httpStatus, String requestBody) { }
}
```

El resultado técnico está representado por el `ExecutionError` actual: null conserva la ausencia de error, otro valor conserva exactamente ese error y su alcance `global()`. No se deriva éxito de HTTP ni se añade un enum redundante. `Long`/`Integer` y Strings mantienen su nulabilidad. `responseText` no se trimmea, decodifica de nuevo, normaliza Unicode ni sanitiza otra vez. latencyMs conserva la medición original, no una nueva duración del bridge.

`LegacyCompatibility` es únicamente el par de datos en memoria que no puede eliminarse sin perder v1; no contiene TargetConfiguration, secreto, proveedor, IDs/revisiones, protocolo, evidencia futura o estado de stream. Con los cinco campos del resultado y este par se conserva exactamente la información original. No se guarda `InvocationResult` entero como objeto opaco ni se duplica el texto en dos sitios.

Ambos records deberán sobrescribir el toString generado: solo nombre del tipo/código error seguro, nunca textos, requestBody o contenido de legacy. Sin logs ni serialización del modelo. D0 no endurece constructores ni introduce defaults: las validaciones actuales siguen siendo autoridad. Los cinco campos públicos neutrales no son un modelo universal v2; el par legacy es una deuda explícita y acotada.

### D0-03 Siete campos y proyección reversible

`application/execution/LegacyInvocationResultProjection` será una utilidad pura, sin estado, beans, reloj, I/O ni validación de negocio:

```text
fromLegacy(InvocationResult) -> NormalizedInteraction
toLegacy(NormalizedInteraction) -> InvocationResult
```

| Campo exacto de InvocationResult | Tipo real | Destino normalizado | Retorno legacy |
|---|---|---|---|
| httpStatus | Integer | legacy.httpStatus | httpStatus idéntico, incluido null |
| latencyMs | Long | latencyMs | latencyMs idéntico, incluido null/0 |
| errorReason | ExecutionError | errorReason | misma constante, sin reclasificar |
| requestBody | String | legacy.requestBody | mismo texto saneado, incluido null/vacío |
| responseText | String | responseText | mismo texto extraído/saneado, incluido null/vacío |
| requestRedacted | boolean | requestRedacted | mismo boolean |
| responseRedacted | boolean | responseRedacted | mismo boolean |

Invariante: `toLegacy(fromLegacy(r)).equals(r)` para todo InvocationResult no nulo representable, incluidos errores con metadata disponible. fromLegacy crea siempre el contenedor legacy con sus dos valores, aunque ambos sean null. No reconstruye un failure desde errorReason descartando otros campos. Para preservar también el comportamiento de un delegado anómalo que retorne null, `fromLegacy(null)` y `toLegacy(null)` devuelven null; no se introduce clasificación o validación antes de finish. Un contenedor legacy ausente en una instancia construida manualmente no forma parte del resultado válido del bridge; no se rellena con HTTP 200 o `{}`. No cambiar InvocationResult, sus factories o toString.

Alternativa descartada: devolver solo texto/error y fabricar requestBody o httpStatus al persistir. Rompe tanto evidencia como invariantes de TestExecution. Aplanar los siete campos sería reversible pero oculta cuáles siguen siendo deuda de transporte; la agrupación explícita preserva la distinción sin infraestructura nueva.

### D0-04 Bridge REST legacy y ruta real

`adapters/out/execution/securitytest/LegacyRestTargetAdapter` implementa TargetAdapter y recibe un único TargetInvocationPort por constructor. Su operación equivale estrictamente a `fromLegacy(invocation.invoke(target, prompt, budget))`. No captura/reclasifica excepciones, no mide tiempo ni realiza red propia. Las RuntimeExceptions llegan al catch existente del use case; los Errors no se interceptan, igual que hoy.

```text
ExecuteSecurityTestsUseCase
  -> TargetAdapter.interact(target, prompt, sameBudget)
     -> LegacyRestTargetAdapter
        -> TargetInvocationPort.invoke(target, prompt, sameBudget)
           -> HttpTargetInvocationAdapter existente
        <- InvocationResult original
     <- NormalizedInteraction (copia reversible de valores)
  -> LegacyInvocationResultProjection.toLegacy(...)
  -> executions.finish(running, InvocationResult proyectado)
  -> evaluación y comprobaciones existentes
```

Cambiar los dos constructores del use case para recibir TargetAdapter; no mantener un constructor alternativo que invoque TargetInvocationPort directamente. El caso de uso no construye el bridge ni conserva una segunda dependencia de transporte. La proyección ocurre en el lugar de la invocación, dentro del try actual. La rama de presupuesto agotado permanece `InvocationResult.failure(SCAN_BUDGET_EXCEEDED)` sin llamar a TargetAdapter. No mover prepare/start/finish/evaluación, ni el catch, ni el control de errores después de finish. Finish sigue fuera del catch para no convertir un fallo de persistencia en otro resultado técnico.

### D0-05 Wiring y propiedad de recursos

`SecurityTestExecutionConfiguration` mantiene el bean `targetInvocationPort` actual y su destroyMethod=close, resolver y controles. Añade un único bean TargetAdapter construido con el **tipo interfaz TargetInvocationPort** inyectado, no buscando HttpTargetInvocationAdapter por clase. Así los fakes @Primary existentes siguen siendo efectivos en integración. El bridge no tiene destroyMethod ni vuelve a cerrar el delegado compartido.

`ScanConfiguration.scanWorkPort` pasa a recibir TargetAdapter y lo entrega al use case; las restantes dependencias y clocks/tickers no cambian. Sin selección por perfil/adapterType, registry, bean duplicado de HTTP ni nueva configuración de entorno.

El adapter HTTP continúa como único propietario de SSRF/allowlist, DNS acotado/IP fijada, TLS, autorización previa a descifrado, cifrado existente, quota/gate, timeout, tamaño, ausencia de redirects/retries y limpieza de buffers/conexiones. Su código y sus parámetros no se modifican para este refactor. El bridge recibe solo el resultado ya saneado; no descifra ni redacciona otra vez.

### D0-06 Matriz de equivalencia y pruebas

Las pruebas se diseñan aquí; no se ejecutan en esta tarea. Toda ejecución posterior de suites, uso de PostgreSQL dedicado o fixture HTTP local requiere petición explícita; nunca se usan endpoints externos reales.

| ID | Prueba discriminante | Evidencia esperada |
|---|---|---|
| T1 | Unit del use case con TargetAdapter fake/spy | prepare/start anteriores a interact, finish posterior con los siete valores; cero llamadas si falla plan o presupuesto ya agotado; dependencia antigua ausente |
| T2 | fromLegacy→toLegacy parametrizado | Igualdad íntegra para éxito, String vacío, Unicode/NUL/escapes, latencia 0/null, todos los ExecutionError, errores con metadata, combinaciones de ambos flags y null anómalo; no tests que solo cuenten campos |
| T3 | Bridge real sobre TargetInvocationPort fake | Una invocación con `same(target)`/mismo prompt/`same(budget)` y misma excepción/flag de interrupción; ninguna copia de presupuesto ni reintento |
| T4 | Use case con bridge real y fixtures deterministas | Cuatro prompts originales en el mismo orden y cuatro finish equivalentes; se conserva fallo individual→continuación, global→aborto, RuntimeException→error interno, finish fallido→cero siguientes llamadas |
| T5 | Wiring Spring real con InvocationPort @Primary fake | ScanWorkPort usa TargetAdapter real que delega al fake seleccionado; no se alcanza HTTP de producción ni se duplica/cierra el bean delegado |
| T6 | Regresiones de transporte existentes a través del bridge | Misma request JSON/cabeceras/extracción responsePath/redacción, mismos límites, TLS, SSRF/DNS y errores. Fixture HTTP/HTTPS solo local controlada cuando sea necesaria; no conexión a Internet |
| T7 | Pipeline con evaluación determinista/evaluador fake | Cuatro evaluaciones y mismos verdicts/Findings, severity/confidence, score y report-v1; incluir inconclusos y caso con Finding. Ground truth/contexto/corpus de fixtures existentes, sin LLM real |
| T8 | Ejecución autorizada de suites existentes | Conservar expectativas actuales y registrar resultados reales; integraciones deshabilitadas por entorno se informan como no ejecutadas, no como aprobadas |

Reutilización dirigida: `ExecuteSecurityTestsUseCaseTest` conserva sus expectativas, adaptando inyección; `HttpTargetFailurePrecedenceTest` adapta únicamente construcción del worker y mantiene precedencia de errores. Las suites HTTP bajo `adapters/out/http/target` conservan sus verificaciones de seguridad. `SecurityTestExecutionPostgresIntegrationTest` ya comprueba cuatro filas antes de red, fuera de transacción, Unicode y redacción. `EvaluationEnginePostgresIntegrationTest`, `ScoringEnginePostgresIntegrationTest` y `ReportingPostgresIntegrationTest` ya incluyen pipeline, compensaciones y fakes TargetInvocationPort; extender solo lo necesario para demostrar que pasan por el bridge. No cambiar algoritmos de evaluación/scoring/report ni rebajar aserciones para hacer pasar suites.

Para comparar derivados, reutilizar fixtures de mismo catálogo/contexto/evidencia. Comparar exactamente campos semánticos, versiones y textos; usar clocks/IDs fijos donde estén inyectables. UUID/timestamps generados legítimamente en ejecuciones independientes se comparan por sus relaciones/invariantes, no se cambian algoritmos productivos para hacerlos iguales. No agregar una dependencia nueva solo para congelarlos.

La medición de latencia sigue siendo la del adapter existente: en tests HTTP se conserva su semántica/rango, sin exigir igualdad de wall-clock entre dos peticiones. Para conversión y pipeline fake se verifica igualdad exacta del valor provisto. Misma petición significa contenido enviado idéntico, no exigir IDs de conexión o duración física idénticos.

### D0-07 Alcance de archivos

| Afectados en implementación futura | Cambio acotado |
|---|---|
| application/execution/TargetAdapter.java | Puerto de una operación |
| application/execution/NormalizedInteraction.java | Resultado mínimo y LegacyCompatibility anidado |
| application/execution/LegacyInvocationResultProjection.java | Conversión pura reversible |
| adapters/out/execution/securitytest/LegacyRestTargetAdapter.java | Delegación al puerto legacy |
| application/execution/ExecuteSecurityTestsUseCase.java | Dependencia y llamada efectiva + proyección |
| config/ScanConfiguration.java | Inyección del nuevo puerto |
| config/SecurityTestExecutionConfiguration.java | Bean bridge único |
| Tests dirigidos | Nuevos contratos y adaptación mínima de construcción/wiring |

Explícitamente intactos: frontend, REST controllers/DTOs/OpenAPI, JPA/repositorios/migraciones/esquema, TargetSettings/TestExecution/ExecutionError, catálogo/prompts, adapter HTTP/JSON/red/crypto y sus límites, evaluation/scoring/reporting productivos, datos y perfiles v1, demo, AWS, versiones/dependencias/lockfiles. Si aparece necesidad de modificarlos para implementar el puente, detener esa ampliación e informar. Documentación técnica existente: solo actualizar la descripción de la cadena de invocación en arquitectura-general si queda desactualizada tras implementar; no crear ADR o documentación de v2 extensa por este refactor.

## Risks / Trade-offs

- [Abstracción decorativa] → tests del worker y wiring real; el use case solo recibe TargetAdapter, sin ruta directa alternativa.
- [Pérdida de campos al reducir el modelo] → LegacyCompatibility explícito y round-trip completo, incluidos errores con metadata y flags independientes.
- [Duplicación de estado/error] → usar ExecutionError existente; no nuevo status ni clasificación por HTTP.
- [Cambio de precedencia/interrupción] → no capturas nuevas en bridge ni cambios de posición del catch/finish; mantener flag del hilo y políticas global/local.
- [Controles duplicados o cierre doble] → un bean HTTP, una llamada y propietario de recursos intacto; bridge sin lifecycle.
- [Fakes existentes dejan de interceptar red] → inyectar TargetInvocationPort por interfaz respetando @Primary; prueba de contexto y contadores.
- [Nuevos records imprimen evidencia] → toString seguros en resultado y contenedor, sin logging adicional.
- [Afirmar regresión completa con tests omitidos] → separar unit/integración habilitada/omitida y no declarar suites aprobadas sin ejecución real.
- [Coste de una capa adicional sin funcionalidad] → aceptar únicamente puerto, valor mínimo, proyección y bridge; no construir extensibilidad especulativa.

## Migration Plan

No hay migración de datos, formato persistido ni API. Tras aprobar, implementar tipos/bridge, sustituir inyección del worker y wiring juntos, adaptar tests y comprobar equivalencia con autorización. Rollback de código posterior consiste en volver a la inyección anterior; no exige tocar filas, perfiles ni credenciales. No se ejecuta ninguna operación Git/despliegue en esta tarea.

## Open Questions

No existen decisiones técnicas bloqueantes para este alcance propuesto. Falta revisión/aprobación del change antes de implementar y autorización de ejecución de tests cuando corresponda; ninguna equivale a permiso de usar targets reales.

### Fuera del MVP / Evolución futura

El modelo universal de interacción, eliminación futura del par legacy y nuevos protocolos pertenecen a otros changes independientes. D0 no promete que este contrato mínimo sea suficiente para SSE, sesiones u OpenAI-compatible y no los prepara con campos vacíos.
