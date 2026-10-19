## Why

El E2E offline está cerrado, pero el adaptador OpenAI nunca se ha validado contra el proveedor real. Se necesita una validación operativa local acotada que compruebe la integración y conserve sus resultados reales, sin exigir cuatro detecciones ni confundir un Finding determinista con éxito del proveedor.

## What Changes

- Definir preflight, autorización temporal de exportación y un único Scan inicial contra demo-vulnerable VULNERABLE, con el evaluador real existente.
- Contrastar cuatro EvaluationResults, Findings persistidos, scoring-v1, report-v1 y presentación Angular; comparar externamente con el ground truth.
- Registrar modelo, uso disponible, errores y degradaciones sin secretos ni resultados prefijados.
- Condicionar el cierre futuro de frontend-mvp 9.4 a su recorrido original completo y apertura de un Finding persistido real.
- Definir un cierre sencillo que deshabilite exportación y retire la clave del entorno de la sesión.
- Diseñar la corrección mínima del schema local evaluation-scenario-v1: enabled booleano, compatible con offline=false y operación real autorizada=true; conservar plantillas false y todos los gates del scanner. La implementación posterior se limita al schema, tests de contrato y documentación directamente afectada.
- Corregir por autorización posterior expresa la inicialización HTTPS de OpenAiEvaluatorAdapter para HttpClient 5.6.4, con registro TLS válido, resolver acotado intacto, regresión offline del manager real y diagnóstico saneado, sin Scan ni llamadas al proveedor (REV-12/D-09).

**Fuera de alcance:** refactor y cambios de runtime ajenos a la reparación HTTPS expresamente autorizada REV-12, servicios de producto o llamadas reales en esta corrección, AWS, despliegues, SAFE adicional, múltiples modelos, benchmark formal, precision/recall, tuning, nuevas categorías, cambios visuales y exportaciones de informes. Se conservan los registros de planificación/operación previos; esta reparación no autoriza otro Scan.

## Capabilities

### New Capabilities

- `real-evaluator-validation`: procedimiento y criterios de aceptación de una validación local controlada del evaluador real, con evidencia no secreta y cierre condicionado de 9.4.

### Modified Capabilities

No se altera el contrato runtime de evaluation-engine, scoring-engine, reporting ni frontend-mvp. REV-11 concreta en esta capacidad la corrección del schema local auxiliar de demo-vulnerable, preservando DV-10 y sus plantillas offline. La operación externa es un incremento separado del E2E offline; no reescribe sus evidencias ni su prohibición histórica de exportar.

## Impact

Solo artefactos de este change durante la planificación. La ejecución futura requiere aprobación y autorización operativa independiente de cuenta/modelo/coste, arranque local y lecturas necesarias. No altera arquitectura, APIs, esquema, dependencias ni configuración versionada. Reutiliza ADR-004, los manifests y contratos existentes. La disponibilidad real de `gpt-6-luna` y la tarifa de la cuenta siguen sin comprobarse; este change no los inventa ni autoriza sustituir el modelo automáticamente.

Estado de la reparación HTTPS: **READY FOR REVIEW**; evidencia offline y alcance en tasks.md. El registro histórico conserva la corrección de schema y el Scan observado posteriormente, sin atribuir éxito del proveedor a Findings deterministas. La reparación no acredita acceso real a OpenAI ni al modelo y no modifica frontend-mvp 9.4.

Validación documental ejecutada el 2026-10-19: `openspec validate real-evaluator-validation --strict` → `Change 'real-evaluator-validation' is valid`. Los cuatro artefactos de planificación están completos; esto no acredita ejecución operativa ni aprobación del change.
