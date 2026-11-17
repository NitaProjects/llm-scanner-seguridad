## Purpose

Eliminar físicamente un proyecto y sus datos dependientes mediante una petición explícita, con aislamiento entre proyectos y confirmación tras el commit.

## ADDED Requirements

### Requirement: PD-01 Eliminación completa y aislada

**Historia:** Como operador quiero retirar un proyecto y su evidencia para que dejen de estar disponibles en la aplicación. **Prioridad:** Alta. **Dependencias:** project-management, V1–V8.

La operación SHALL borrar físicamente `projects`, `target_configurations`, `scans`, `test_executions`, `evaluation_results`, `findings`, `scan_scores` y `security_reports` del proyecto seleccionado, incluidas las credenciales cifradas y la evidencia que contienen. SHALL conservar `test_cases`, los datos de otros proyectos y las FK vigentes. SHALL aplicar una sola transacción a todo el agregado y confirmar éxito después del commit. SHALL admitir proyectos sin hijos y scans terminales históricos o fallidos con resultados ausentes. Los archivos de operador, copias de base, logs y target remoto quedan fuera de la operación.

#### Scenario: Proyecto con historial completo
- **WHEN** se elimina un proyecto con target y scans terminales con ejecuciones, evaluaciones, hallazgos, score e informe
- **THEN** desaparecen todas sus filas y la evidencia contenida sin dejar huérfanos
- **AND** otro proyecto y el catálogo compartido conservan sus datos

#### Scenario: Fallo intermedio o referencia cruzada
- **WHEN** falla una sentencia después de borrar algunas filas o una FK de otro proyecto impide terminar
- **THEN** la transacción revierte y las filas de ambos proyectos conservan sus valores originales

### Requirement: PD-02 Exclusión de actividad y concurrencia

**Historia:** Como operador quiero un resultado predecible si coincide un borrado con un análisis o una edición del target. **Prioridad:** Alta. **Dependencias:** scan-execution, target-configuration.

DELETE SHALL comprobar PENDING/RUNNING después de obtener el bloqueo de Project y SHALL devolver 409 sin escribir si existe actividad. SHALL serializarse por Project con otros DELETE, la creación de scans y los guardados de target. Si el borrado gana y confirma, esas escrituras SHALL devolver PROJECT_NOT_FOUND y la admisión SHALL NOT despachar. Si la admisión gana y su scan sigue activo, el borrado SHALL devolver 409. El estado terminal SHALL ser visible solo después del cierre correspondiente. Una entrega de trabajo de un scan ausente SHALL quedar sin trabajo. DELETE SHALL NOT cancelar scans. El estado efímero de los descendientes eliminados SHALL liberarse por los caminos vigentes y, si queda una entrada del corpus en memoria, tras el commit del borrado. Las lecturas nuevas tras el 204 SHALL ver la ausencia y las concurrentes pueden observar la versión previa.

#### Scenario: Scan pendiente o en ejecución
- **WHEN** un DELETE encuentra un scan PENDING o RUNNING, incluso si está confirmando evaluación y Finding
- **THEN** devuelve 409 sin eliminar filas ni alterar el ciclo de vida
- **AND** una petición posterior a un cierre terminal confirmado puede borrar el proyecto

#### Scenario: Dos peticiones de borrado
- **WHEN** dos DELETE concurren para el mismo proyecto existente sin actividad y el primero confirma
- **THEN** uno devuelve 204 y el otro devuelve 404 sin repetir un borrado parcial

#### Scenario: DELETE confirma antes de admitir un scan
- **WHEN** la admisión se bloquea mientras el DELETE tiene el bloqueo de Project y este confirma
- **THEN** la admisión devuelve PROJECT_NOT_FOUND y no crea ni despacha el scan

### Requirement: PD-03 Contrato REST y acceso existente

**Historia:** Como consumidor quiero distinguir éxito, ausencia, actividad y error sin que se expongan datos internos. **Prioridad:** Alta. **Dependencias:** PM-07.

`DELETE /api/projects/{projectId}` SHALL aceptar la misma sintaxis UUID que GET y funcionar sin cuerpo. SHALL devolver 204 sin contenido tras commit, 400 INVALID_PROJECT_ID para UUID inválido, 404 PROJECT_NOT_FOUND para ausente o repetición, 409 SCAN_ALREADY_ACTIVE para PENDING/RUNNING, 409 PROJECT_DELETE_CONFLICT para FK que impide el borrado y 500 INTERNAL_ERROR para otros errores internos. Los errores de aplicación SHALL tener code, message en español y fieldErrors vacío. Las respuestas del endpoint SHALL usar no-store y SHALL excluir información sensible. El acceso SHALL respetar el perímetro de operador existente, incluido su rechazo de credenciales inválidas cuando esté configurado. Sin identidad de usuario ni ACL por Project en el backend actual, el change no introduce ni promete separación por propietario.

#### Scenario: Repetición y entradas incorrectas
- **WHEN** se borra un proyecto vacío y se repite el DELETE con el mismo UUID, o se envía un ID inválido
- **THEN** el primer DELETE devuelve 204, la repetición devuelve 404 y el ID inválido devuelve 400 con el código correspondiente

### Requirement: PD-04 Confirmación y resultado en el listado

**Historia:** Como usuario quiero confirmar sobre qué proyecto actúo y recibir el resultado en el listado. **Prioridad:** Alta. **Dependencias:** PD-03 y frontend actual.

El listado SHALL permitir eliminar un proyecto mediante diálogo accesible que muestre el nombre, los datos asociados y la irreversibilidad. SHALL presentar Cancelar y una acción destructiva explícita sin exigir escribir el nombre. Durante el envío SHALL impedir confirmaciones duplicadas. Tras 204 SHALL cerrar, retirar la fila y anunciar éxito. Si el proyecto tiene actividad SHALL conservar la fila y explicar el conflicto. Ante ausencia SHALL reconciliar el listado y ante respuesta ambigua SHALL consultar el estado antes de un nuevo intento, sin repetir DELETE automáticamente.

#### Scenario: Confirmación o cancelación desde el listado
- **WHEN** el usuario abre el diálogo y cancela o confirma
- **THEN** cancelar no envía DELETE y confirmar envía una sola petición para ese proyecto
- **AND** un 204 retira el elemento y un conflicto lo conserva con feedback
