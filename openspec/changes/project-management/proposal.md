## Why

El flujo de LLM Scanner Seguridad necesita un contenedor persistente de proyectos antes de incorporar la configuración del target y las ejecuciones de análisis. Este primer cambio define una base pequeña y verificable para crear y consultar proyectos, respetando la arquitectura existente.

## What Changes

- Definir la creación, consulta por identificador y listado de proyectos de análisis.
- Especificar los datos básicos de `Project`, su validación, sus respuestas y los errores funcionales necesarios.
- Proponer `CreateProjectUseCase`, `GetProjectUseCase` y `ListProjectsUseCase`, separados de HTTP y persistencia.
- Definir exclusivamente la persistencia de `Project` en PostgreSQL mediante un puerto de aplicación y un adaptador de salida.
- Alinear, durante la implementación posterior, la definición existente de `projects` en `V1__crear_tablas_iniciales.sql` con el modelo aprobado. Se autoriza excepcionalmente modificar V1 porque aún no constituye una migración consolidada; no se creará una V2 correctiva para este ajuste inicial.
- Proponer el contrato REST mínimo, sin diseñar pantallas Angular.

## Non-goals

Quedan fuera de este cambio la edición y eliminación de proyectos, búsqueda y filtros avanzados, `TargetConfiguration`, scans, `SecurityTest`/`TestCase`, `TestExecution`, findings, reports, integración LLM, OWASP, scoring, confidence, usuarios, autenticación, autorización, AWS y `demo-vulnerable`.

Esta entrega solo actualiza artefactos OpenSpec. No modifica código, configuración de ejecución ni dependencias; tampoco crea o ejecuta migraciones. Las tareas describen una implementación futura que requiere una petición explícita de implementación.

## Capabilities

### New Capabilities

- `project-management`: creación, consulta individual y listado de proyectos, con datos básicos, validación, errores y persistencia.

### Modified Capabilities

Ninguna. `openspec/specs/` todavía no contiene especificaciones funcionales.

## Impact

- La implementación futura afectará al dominio, aplicación y adaptadores REST y PostgreSQL del backend. El dominio seguirá independiente de Spring, JPA, HTTP y PostgreSQL.
- Se proponen `POST /api/projects`, `GET /api/projects/{id}` y `GET /api/projects`.
- El ajuste de almacenamiento se limita a `Project`. Se conserva la FK existente `scans.project_id → projects.id` y su índice, sin rediseñar `scans` ni otras tablas futuras de V1.
- `projects` contendrá únicamente `id`, `name`, `description`, `created_at` y `updated_at`. Se retirarán `target_url` y `app_type` de esa tabla; la configuración del target corresponde a una capability posterior `TargetConfiguration`, que no se inicia en este change.
- Según el contexto confirmado por el usuario, V1 no se ha desplegado en PRE/PRO y no hay base compartida ni datos productivos que preservar. Una vez utilizada como baseline real, V1 será inmutable y la evolución del esquema utilizará nuevas migraciones V2, V3, etc.
- La documentación existente prevé identificador, nombre, descripción y fecha de creación. Se ha aprobado incorporar `updatedAt`, inicialmente igual a `createdAt`, sin habilitar modificaciones.
- Decisiones aprobadas por el usuario: UUID como identidad, nombres repetidos, máximo de 120 caracteres para `name` y 2000 para `description`, trim de ambos textos y rechazo del nombre vacío tras el trim.
- Para el MVP se ha aprobado un listado completo sin paginación. La paginación queda documentada como posible evolución futura.
- Tras aprobar el cambio, su implementación deberá reflejar únicamente los contratos y el modelo afectados en `docs/05-api/openapi.yaml` y `docs/06-base-de-datos/modelo-datos.md`. En esta entrega ambos permanecen intactos.
- Referencias: `docs/00-contexto/alcance.md`, `docs/00-contexto/backlog.md`, `docs/02-arquitectura/arquitectura-general.md`, `docs/06-base-de-datos/modelo-datos.md` y `backend/AGENTS.md`.
- No se proponen cambios de versiones, dependencias, despliegue o decisiones pendientes ajenas al cambio. Las decisiones anteriores están aprobadas; los artefactos quedan preparados para la implementación posterior, sin autorización para ejecutar tareas en esta entrega.
