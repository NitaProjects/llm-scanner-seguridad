## Context

Motivación y alcance funcional: [proposal.md](proposal.md). Contrato verificable: [specs/project-management/spec.md](specs/project-management/spec.md), requisitos PM-01 a PM-07.

Observaciones del repositorio:

- `docs/00-contexto/alcance.md` y `backlog.md` contemplan la gestión básica de proyectos como punto de partida.
- `docs/06-base-de-datos/modelo-datos.md` prevé identificador, nombre, descripción y fecha de creación. No impone campos adicionales al crear ni define aún tipos, índices o restricciones. Sus relaciones con targets y scans describen capacidades futuras; este cambio permite crear un contenedor vacío.
- `docs/02-arquitectura/arquitectura-general.md` establece monolito modular, dominio independiente y puertos/adaptadores. La granularidad de los casos de uso se concreta por cambio OpenSpec.
- `backend/AGENTS.md` orienta la organización hacia `domain`, `application`, `adapters/in`, `adapters/out` y `config`, y sitúa la API bajo `/api/`.
- `backend/pom.xml` ya declara Spring MVC, validación, Spring Data JPA, Flyway y el driver PostgreSQL. La inspección dirigida del código Java solo muestra el arranque, configuración web y una prueba de contexto; no hay gestión de proyectos implementada.
- `docs/05-api/openapi.yaml` está vacío. No existe un contrato de proyectos previo ni una spec funcional consolidada que deba modificarse.
- La revisión de `backend/src/main/resources/db/migration/V1__crear_tablas_iniciales.sql` detecta un diseño anterior: `projects.name` tiene longitud 150, `description` longitud 1000, `target_url` y `app_type` son obligatorios y falta `updated_at`. `projects.id` y `scans.project_id` son UUID y existe una FK válida entre ambos.
- El usuario confirma que V1 no se ha desplegado en PRE/PRO, no existe base de datos compartida ni datos productivos y autoriza expresamente su modificación como diseño inicial aún no consolidado. Esta excepción queda limitada al ajuste de `Project` de este change.

Estado: decisiones de identidad, fechas iniciales, límites, trim, nombres repetidos y listado completo sin paginación aprobadas explícitamente por el usuario. El resto del diseño conserva el alcance y las alternativas ya documentadas, sin sustituir ADRs aprobadas ni cerrar decisiones de otras capacidades. No hay bloqueo documental o técnico identificado para la implementación posterior; esta entrega no la autoriza.

## Goals / Non-Goals

**Goals:**

- Concretar un recorrido REST → aplicación → dominio/puertos → adaptador PostgreSQL para tres operaciones pequeñas.
- Mantener separado el modelo de dominio de DTOs HTTP y entidades JPA.
- Dar a la creación una confirmación transaccional y a las consultas respuestas previsibles.
- Limitar el diseño relacional a los cinco campos de `Project`.

**Non-Goals:**

- Diseñar pantallas, navegación, formularios o estado de Angular.
- Introducir componentes distribuidos, nuevas dependencias o cambios de configuración de despliegue.
- Crear abstracciones genéricas de CRUD, auditoría o repositorios para entidades futuras.
- Implementar cualquier elemento en esta entrega. El resto de exclusiones figura en `proposal.md`.

## Decisions

### 1. Mantener el modelo pequeño y conservar `updatedAt` con semántica limitada

Decisión aprobada: conservar `id`, `name`, `description`, `createdAt` y `updatedAt`. Los cuatro primeros conceptos coinciden con el modelo documental. La incorporación de `updatedAt` y su igualdad inicial con `createdAt` están aprobadas explícitamente. Durante este cambio ninguna lectura modifica las fechas.

Alternativa: omitir `updatedAt` hasta incorporar edición. Reduce un campo redundante, pero requeriría ampliar la representación cuando se decida editar proyectos. No se propone añadir `status`, propietario, configuración del target, contadores ni relaciones anticipadas.

Decisión aprobada: UUID para `id` y nombres repetidos permitidos; la identidad del proyecto la determina el UUID. Se mantienen cadenas para `name`/`description` e instantes UTC para las fechas. Generar UUID aleatorio en la aplicación evita depender de una secuencia PostgreSQL; un identificador numérico sería más compacto, pero vincularía más la creación a la persistencia.

### 2. Aplicar reglas simples y comunes de normalización

Decisiones aprobadas: máximo de 120 caracteres para `name`, máximo de 2000 para `description` y trim al principio y al final de ambos campos. Un nombre vacío después del trim es inválido. Se mantiene el cómputo en puntos de código Unicode definido en PM-02 y la descripción sin contenido se almacena como `null`. Los límites evitan campos ilimitados y mantienen margen para nombres descriptivos; exigir nombres únicos, un slug o restricciones ASCII no aporta valor al alcance solicitado.

El adaptador de entrada valida la estructura y tipos JSON y rechaza propiedades ajenas al contrato. Las invariantes de nombre y descripción se protegen también en el dominio, sin anotaciones Spring o Jakarta Validation. La validación HTTP no será la única barrera. No se interpretará el texto como HTML, instrucciones, URL o configuración de una integración.

Corrección aprobada del finding 1: rechazar U+0000 en ambos textos porque PostgreSQL no puede almacenarlo en `VARCHAR`. La regla se centraliza en `Project.validateCreation`, reutilizada por el constructor de dominio y por `CreateProjectCommand` antes de iniciar la operación transaccional o acceder al puerto. La respuesta es HTTP `400`, `VALIDATION_ERROR`, con `INVALID_CHARACTER` por cada campo afectado y mensajes en español sin valores rechazados. No se elimina ni sustituye el carácter. La exclusión se limita a U+0000; conserva trim, descripción opcional y límites por puntos de código. El mapeo REST existente propaga el código sin cambios adicionales de producción ni de esquema.

### 3. Casos de uso explícitos y un puerto de persistencia acotado

Flujo conceptual:

```text
Angular (consumidor futuro, sin diseño de interfaz en este change)
  → Adaptador REST
  → CreateProjectUseCase / GetProjectUseCase / ListProjectsUseCase
  → Project y ProjectRepositoryPort
  → Adaptador PostgreSQL mediante JPA
  → PostgreSQL
```

Responsabilidades propuestas:

| Elemento | Responsabilidad |
| --- | --- |
| `Project` en dominio | Representar datos válidos y proteger sus invariantes sin Spring, JPA, HTTP o SQL. |
| `CreateProjectUseCase` en aplicación | Validar el comando, asignar identidad y un mismo instante a ambas fechas, solicitar inserción y devolver el proyecto confirmado. |
| `GetProjectUseCase` en aplicación | Recuperar por identificador y distinguir ausencia de un fallo del almacenamiento. |
| `ListProjectsUseCase` en aplicación | Recuperar el listado con el orden contratado. |
| `ProjectRepositoryPort` en aplicación | Expresar inserción, búsqueda opcional por UUID y listado ordenado usando tipos propios, sin `JpaRepository`, `Pageable` ni entidades JPA en su firma. |
| Adaptador REST de entrada | Convertir JSON/DTOs, validar formato UUID, invocar casos de uso y traducir resultados/errores a HTTP. |
| Adaptador PostgreSQL de salida | Mapear entre dominio y persistencia, ejecutar las operaciones y traducir fallos técnicos. |
| Composición en `config` | Conectar dependencias y la frontera transaccional con Spring sin anotarlo en el dominio. |

La dirección de las dependencias de código apunta hacia dominio/aplicación; las flechas del flujo no autorizan dependencias del dominio hacia adaptadores. Un proveedor de reloj e identidad puede inyectarse mediante capacidades de Java, sin diseñar un subsistema adicional. Conviene obtener un solo instante con precisión de microsegundos para que coincida con la representación persistida.

Alternativa: un servicio CRUD con entidades JPA expuestas por REST. Reduce mapeos, pero incumple la independencia requerida y mezcla el contrato público con persistencia. Se propone JPA exclusivamente en el adaptador porque ya está declarado; JDBC también permitiría el aislamiento, pero no hace falta introducir una segunda estrategia para este cambio.

### 4. Contrato REST mínimo

Las rutas respetan `/api/` y los requisitos PM-01 a PM-07:

| Operación | Entrada | Resultado |
| --- | --- | --- |
| `POST /api/projects` | Objeto JSON con `name` y `description` opcional. | `201`, representación completa y `Location: /api/projects/{id}` después de confirmar la transacción. |
| `GET /api/projects/{id}` | UUID en la ruta. | `200` y representación; `400` para formato inválido; `404` para UUID inexistente. |
| `GET /api/projects` | Sin parámetros funcionales. | `200` y array completo, incluido `[]`, ordenado según PM-05. |

Ejemplo de representación:

```json
{
  "id": "b33bfe85-a166-4bef-bb36-184a1c27e322",
  "name": "Análisis inicial",
  "description": null,
  "createdAt": "2026-10-09T10:00:00Z",
  "updatedAt": "2026-10-09T10:00:00Z"
}
```

Decisión aprobada para el MVP: `ListProjects` devuelve el listado completo sin paginación. La alternativa paginada limita el consumo desde el inicio, pero añade parámetros, metadatos y casos de aceptación. Se documenta como posible evolución futura que requerirá revisar el contrato mediante otro cambio, antes de utilizarlo con grandes colecciones. No hay silenciamiento de resultados ni límite oculto.

### 5. Persistir únicamente `Project`

Propuesta conceptual de una tabla `projects`:

| Campo de dominio | Columna | Tipo PostgreSQL propuesto | Restricción |
| --- | --- | --- | --- |
| `id` | `id` | `uuid` | Clave primaria, no nulo, asignado por la aplicación. |
| `name` | `name` | `varchar(120)` | No nulo, no vacío; sin unicidad por nombre. |
| `description` | `description` | `varchar(2000)` | Nulo permitido. |
| `createdAt` | `created_at` | `timestamp(6) with time zone` | No nulo, asignado por la aplicación. |
| `updatedAt` | `updated_at` | `timestamp(6) with time zone` | No nulo y no anterior a `created_at`. |

La normalización y los límites de texto pertenecen al dominio; la persistencia añade restricciones defensivas de nulabilidad, longitud, nombre no vacío y consistencia temporal. El adaptador usa un modelo JPA separado y convierte las fechas a instantes UTC. No se añaden claves foráneas desde `projects`, tablas de otras capacidades, índice único de nombre ni índices adicionales anticipados; la clave primaria cubre la consulta individual. Se conserva la FK entrante existente desde `scans`, como se detalla a continuación.

### 5.1. Alinear V1 antes de consolidarla como baseline

Decisión aprobada: modificar la definición inicial de `projects` en V1 durante la implementación posterior, en lugar de añadir una V2 correctiva. La regla general de no modificar migraciones consolidadas se mantiene; la excepción se justifica únicamente por el estado inicial confirmado por el usuario. Esta entrega solo registra el diseño y no modifica SQL.

Ajustes previstos y revisión de compatibilidad:

- Conservar `id UUID PRIMARY KEY`. El `DEFAULT gen_random_uuid()` existente es compatible con que la aplicación proporcione explícitamente el UUID; no requiere cambiar identificadores de otras tablas.
- Cambiar `name` a `VARCHAR(120) NOT NULL`, sin unicidad, y `description` a `VARCHAR(2000)` nullable, según PM-01 y PM-02.
- Eliminar `target_url` y `app_type` de `projects`. No trasladarlos a otra tabla ni crear `TargetConfiguration` aquí: la capability posterior definirá su modelo y contrato, sin asumir que estos dos campos agoten su diseño.
- Mantener `created_at` y añadir `updated_at`, ambos `TIMESTAMPTZ NOT NULL`, equivalente a `timestamp with time zone`, con precisión de microsegundos compatible con el diseño. La aplicación suministra un mismo instante para ambas columnas al crear; las lecturas no lo actualizan. El default existente de `created_at` no contradice esa asignación explícita.
- Incorporar únicamente las restricciones defensivas ya previstas para `projects`: nombre no vacío y `updated_at >= created_at`, además de clave primaria, nulabilidad y límites. No introducir triggers de actualización ni restricciones de unicidad sobre el nombre.
- Conservar exactamente la relación `scans.project_id UUID NOT NULL REFERENCES projects(id)` y el índice `idx_scans_project_id`. Representa correctamente la pertenencia conceptual de un scan a un proyecto, no obliga a crear scans al crear proyectos y no implica introducir scans en el dominio o la API de este change. No cambiar acciones referenciales ni el resto del diseño de `scans`.
- Los nombres `id`, `project_id` y `created_at` y los tipos UUID existentes son compatibles. Los timestamps y constraints restantes de `scans` no impiden la gestión básica de proyectos y permanecen intactos.
- Conservar las restantes tablas, índices y la declaración existente de `pgcrypto` de V1: no se revisan ni rediseñan capacidades futuras que no afectan a `Project`.

Alternativa descartada en este contexto: V2 correctiva sobre el esquema antiguo. Preservaría un historial consolidado, pero ese historial no existe todavía según el contexto confirmado, y perpetuaría innecesariamente el diseño inicial obsoleto. Desde el momento en que V1 se utilice como baseline real, quedará congelada: toda modificación posterior del esquema, incluida la futura `TargetConfiguration`, utilizará nuevas migraciones Flyway V2, V3, etc. No se crea V2 en este change.

Una inserción se confirma atómicamente antes de producir el resultado HTTP exitoso. La frontera transaccional reside en infraestructura/composición alrededor de la operación de aplicación: ni el dominio ni los contratos del puerto conocen Spring. Si el commit falla no se emite `201`. No se introduce deduplicación automática de peticiones ni reintento implícito de creación.

La alternativa de almacenamiento en memoria no satisface la recuperación después de reiniciar. No se diseña aquí el modelo completo de la base de datos.

### 6. Separar errores funcionales y fallos técnicos

El adaptador REST transforma entrada estructural inválida, validación de campos, identificador inválido y proyecto ausente en los códigos de PM-07. Un resultado de búsqueda vacío significa ausencia; una excepción de persistencia no equivale a ausencia ni a listado vacío.

Los fallos técnicos se traducen a `INTERNAL_ERROR` con HTTP `500` y un mensaje genérico. No se envían excepciones, SQL, información de conexión ni valores rechazados. La respuesta de error pertenece al contrato HTTP, no al dominio. Las consultas de persistencia usan parámetros del mecanismo existente, sin construir SQL mediante concatenación de entradas.

Alternativa: utilizar directamente el formato predeterminado de errores del framework. Requiere menos adaptación, pero deja menos control sobre códigos estables, errores de campo y exposición de detalles; se propone el objeto pequeño de PM-07.

### Estado de las decisiones

El usuario ha aprobado UUID, `updatedAt` inicialmente igual a `createdAt`, máximos de 120/2000 caracteres, trim de ambos textos, rechazo del nombre vacío tras el trim, nombres repetidos y listado completo sin paginación para el MVP. Estas decisiones dejan de estar pendientes. La paginación permanece como posible evolución futura. El contrato REST, los errores, el orden del listado y el modelo de cinco campos conservan el diseño existente; esta actualización concreta únicamente su alineación con V1 y la política de migraciones.

También queda aprobada la excepción de V1 descrita en la sección 5.1: corregir su definición de `projects` antes de consolidar el baseline, conservar la FK de `scans` y utilizar nuevas migraciones para cambios posteriores. Se resuelve la divergencia entre la migración antigua y el diseño aprobado sin modificar requisitos funcionales.

La autenticación del usuario y las decisiones pendientes de targets, scans, evaluador y despliegue permanecen fuera del cambio. No bloquean su implementación ni se resuelven aquí. La ejecución de tareas necesitará una petición posterior que autorice implementar.

## Risks / Trade-offs

- Listado completo con volumen creciente → Mantener el alcance inicial controlado y revisar paginación en otro change antes de ampliar el volumen esperado.
- `updatedAt` es redundante sin edición → Documentar su igualdad inicial y comprobar que las lecturas no lo alteran; el futuro cambio de edición definirá cuándo se actualiza.
- Diferencias entre longitud UTF-16, Unicode y almacenamiento → Contar puntos de código en las reglas y probar límites con caracteres no ASCII, incluidos caracteres fuera del plano básico.
- Diferencias de precisión de las fechas → Generar un único instante compatible con microsegundos y verificar su conservación en el recorrido completo.
- Una respuesta perdida después de un commit puede inducir al consumidor a repetir la creación → No prometer idempotencia; cada petición válida independiente crea un nuevo proyecto. Diseñar idempotencia, si se necesita, en otro change.
- Textos libres pueden contener contenido no confiable → Tratarlos como texto y no incluirlos en errores o logs por defecto; el futuro frontend deberá mostrarlos con escape estándar.
- No se incluye autenticación → Este change define operaciones de proyectos, sin afirmar que la plataforma esté preparada para exposición pública o entrega final.
- Una V1 modificada puede discrepar del checksum de una base local donde ya se haya ejecutado → No ejecutar `repair`, `clean`, borrados ni recreaciones automáticamente; cualquier actuación sobre esa base local requiere una petición explícita y queda fuera de esta actualización documental. La excepción no permite alterar bases compartidas o un baseline ya consolidado.

## Migration Plan

Solo para una implementación posterior aprobada:

1. Actualizar el contrato OpenAPI afectado antes o junto con su implementación y reflejar el modelo aprobado de `Project` en la documentación de datos.
2. Modificar únicamente la definición de `projects` en la V1 existente conforme a la excepción aprobada de la sección 5.1, conservando la FK e índice de `scans` y el resto de elementos ajenos al cambio. No crear V2 ni ejecutar Flyway en esta entrega. Una vez utilizada V1 como baseline real, conservarla sin nuevas modificaciones y usar nuevas migraciones para cualquier evolución posterior.
3. Incorporar dominio, aplicación, adaptadores y composición; utilizar las dependencias existentes sin alterar versiones ni configuración de despliegue.
4. Verificar los escenarios de la spec cuando el usuario autorice ejecutar las comprobaciones correspondientes.

No se prescribe despliegue en esta tarea. Para una eventual reversión de aplicación, conservar la tabla y los proyectos y retirar únicamente la versión de aplicación por el procedimiento autorizado. No proponer borrar datos, editar migraciones aplicadas ni ejecutar operaciones de rollback destructivas.

## Fuera del MVP / Evolución futura

La paginación queda fuera del MVP de gestión de proyectos por decisión aprobada y se documenta como posible evolución futura. Edición y eliminación, búsqueda, filtros y reglas de unicidad adicionales quedan fuera de este primer change; no se decide aquí si todos formarán parte del MVP global. La vinculación con targets y scans llegará mediante sus cambios correspondientes. Capacidades avanzadas de usuarios, organizaciones y multi-tenancy siguen fuera del MVP según la documentación existente.
