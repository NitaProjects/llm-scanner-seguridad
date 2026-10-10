# Documentación del proyecto

## Separación de responsabilidades

- `openspec/specs/`: destino de las especificaciones consolidadas; actualmente aún no contiene specs consolidadas.
- `openspec/changes/`: contratos aprobados disponibles, propuestas, diseños, especificaciones delta, tareas y registros de implementación. Las extensiones posteriores deben leerse junto con el alcance histórico de cada incremento.
- `docs/`: memoria del TFM, investigación, diagramas, metodología y documentación técnica consolidada.
- `docs/08-entrega-tfm/`: requisitos académicos oficiales, checklist, trazabilidad y enlaces de entrega del TFM.

El backend core está aprobado e implementado por `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`. Sus contratos están en `openspec/changes/<cambio>/specs/<capacidad>/spec.md`. La sincronización a `openspec/specs/` y el archivo de changes podrán realizarse posteriormente según el workflow OpenSpec, sin reescribir los artefactos históricos. Esto no completa frontend, demo vulnerable, operación real, experimento ni entrega del TFM.

## Regla de no duplicación

La documentación de `docs/` no debe repetir íntegramente las especificaciones de OpenSpec. Cuando sea necesario, debe explicar, justificar o resumir una decisión y referenciar la especificación relacionada.

El backlog de `docs/00-contexto/backlog.md` se mantiene como roadmap de alto nivel. Las tareas concretas de implementación se mantienen en `tasks.md` dentro de cada cambio OpenSpec.
