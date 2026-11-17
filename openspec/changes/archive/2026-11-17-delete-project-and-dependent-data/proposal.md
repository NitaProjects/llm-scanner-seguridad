## Why

Los usuarios pueden crear proyectos y acumular análisis, pero carecen de una operación para eliminar el proyecto y la evidencia asociada en PostgreSQL.

## What Changes

- Añadir `DELETE /api/projects/{projectId}` con borrado físico de los datos propios del proyecto en una transacción y respuesta 204 tras confirmar.
- Rechazar la operación si existe un scan PENDING o RUNNING. Serializar el borrado con la admisión de scans y las escrituras del target mediante la fila del proyecto.
- Eliminar dependencias en orden explícito y conservar el catálogo compartido de pruebas.
- Añadir una acción en el listado de proyectos con confirmación modal, estado de espera y resultado.

## Capabilities

### New Capabilities

- `project-deletion` define borrado, concurrencia, contrato HTTP y confirmación de usuario.

### Modified Capabilities

Ninguna spec consolidada requiere modificación. El cambio amplía prospectivamente project-management y la coordinación de admisión. Los contratos existentes de lectura, creación y evaluación se conservan.

## Impact

API de proyectos, caso de uso, adaptador PostgreSQL y coordinación de escrituras de Scan y TargetConfiguration. El listado Angular incorpora una acción y un diálogo. Las FK vigentes permiten la operación sin migración.

Fuera de alcance quedan restore, papelera, soft-delete, archivado, retención, auditoría avanzada, permisos nuevos, borrado masivo, AWS y rediseño. El borrado abarca los datos de la aplicación en la base activa. Las copias, manifiestos administrados por el operador y sistemas externos siguen su propio ciclo de vida.
