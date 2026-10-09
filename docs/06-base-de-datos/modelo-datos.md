# Modelo de datos conceptual

## Entidades principales

### Project

Representa un proyecto de análisis con cero o una configuración de target en el MVP y sus scans.

Modelo aprobado e implementado por `project-management`, limitado a la tabla `projects`:

| Campo | Columna | Tipo PostgreSQL | Restricciones |
| --- | --- | --- | --- |
| `id` | `id` | `UUID` | Clave primaria; generado por la aplicación. Se conserva el default `gen_random_uuid()` de V1. |
| `name` | `name` | `VARCHAR(120)` | Obligatorio, no vacío ni solo espacios; nombres repetidos permitidos. |
| `description` | `description` | `VARCHAR(2000)` | Opcional; ausencia o texto vacío normalizado a `NULL`. |
| `createdAt` | `created_at` | `TIMESTAMPTZ(6)` | Obligatorio; instante generado por la aplicación. |
| `updatedAt` | `updated_at` | `TIMESTAMPTZ(6)` | Obligatorio; no anterior a `created_at`. |

El dominio retira los espacios en blanco iniciales y finales de ambos textos y cuenta los límites en puntos de código Unicode, sin alterar espacios interiores. Al crear, ambas fechas reciben el mismo instante UTC con precisión de microsegundos; las consultas no las modifican. La creación se confirma tras el commit de la transacción.

El proyecto puede crearse sin targets ni scans. `target_url` y `app_type` se retiran de `projects`; `TargetConfiguration` se almacena por separado en `target_configurations`. Se conservan la FK existente `scans.project_id → projects.id` y `idx_scans_project_id`, sin rediseñar `scans`.

Por la excepción inicial aprobada en `openspec/changes/project-management/design.md`, se ajusta directamente V1, sin V2 correctiva. Desde su uso como baseline real, V1 será inmutable y cualquier evolución requerirá una nueva migración Flyway. No se han ejecutado migraciones durante esta implementación.

### TargetConfiguration

Recurso único opcional por Project, aprobado en `target-configuration`. V2 crea la tabla sin modificar V1, projects ni scans. Múltiples targets quedan como evolución futura.

| Columna | Tipo PostgreSQL | Restricciones |
| --- | --- | --- |
| id | UUID | PK generada por aplicación |
| project_id | UUID | NOT NULL, FK projects(id), UNIQUE uq_target_configurations_project_id |
| name | VARCHAR(120) | Opcional |
| url | VARCHAR(2048) | NOT NULL |
| request_template | TEXT | NOT NULL; hasta 32768 bytes UTF-8 en aplicación |
| response_path | VARCHAR(1024) | NOT NULL |
| headers | JSONB | NOT NULL, objeto no sensible, default {} |
| auth_type | VARCHAR(20) | NONE, BEARER_TOKEN o API_KEY_HEADER |
| auth_header_name | VARCHAR(128) | Solo para API_KEY_HEADER |
| credential_ciphertext | BYTEA | Envelope AES-256-GCM v1; null para NONE |
| timeout_ms | INTEGER | NOT NULL, 1000..60000 |
| created_at | TIMESTAMPTZ(6) | NOT NULL |
| updated_at | TIMESTAMPTZ(6) | NOT NULL, >= created_at |

Checks: headers objeto; NONE sin header/blob, Bearer con blob y sin header, API key con ambos. UNIQUE garantiza cardinalidad incluso en creación concurrente. Sin cascada de borrado anticipada. method/contentType/promptPlaceholder son constantes derivadas y no se persisten, tampoco authConfig, secretValue ni hasCredentials.

El material cifrado separa la credencial de la configuración pública. Formato y provisión de clave externa (nunca en PostgreSQL) en ADR-002. Creación/actualización son atómicas, con commit antes del éxito; PUT preserva identidad/createdAt y no retrocede updatedAt. No hay versionado optimista ni detección de cambios perdidos.

### Scan

Representa una ejecución de análisis asociada a un proyecto.

Datos previstos:

- `id`.
- `projectId` o proyecto asociado.
- `status`, con valores mínimos `PENDING`, `RUNNING`, `COMPLETED` y `FAILED`.
- `createdAt`.
- `startedAt`.
- `finishedAt`.
- `errorReason` opcional.
- `totalTests`.
- `completedTests`.
- Score o resultado global del scan.

El scan se creará en `PENDING`, pasará a `RUNNING` durante el procesamiento asíncrono y terminará en `COMPLETED` o `FAILED`. `CANCELLED` y `PARTIAL` no forman parte de los estados mínimos del MVP; su incorporación queda como **Pendiente de decisión**.

### SecurityTest / TestCase

`SecurityTest` es el término conceptual preferido para representar un caso de prueba reutilizable. `TestCase` puede utilizarse como nombre técnico de persistencia o implementación si resulta más claro.

Datos previstos:

- Identificador.
- Categoría.
- Nombre.
- Descripción.
- Payload o prompt de prueba.
- Referencia OWASP.
- Criterios de detección.

La decisión definitiva sobre el nombre público y el nombre de persistencia queda como **Pendiente de decisión**.

### TestExecution

Representa la ejecución de un SecurityTest dentro de un Scan.

Datos previstos:

- Identificador.
- Scan asociado.
- SecurityTest asociado.
- Petición enviada.
- Respuesta recibida.
- Resultado del evaluador.
- Confianza.
- Fecha de ejecución.
- Estado de la ejecución.
- Error de la ejecución, cuando proceda.
- Resultado determinista.
- Veredicto del evaluador LLM.
- Veredicto final: `VULNERABLE`, `NOT_VULNERABLE` o `INCONCLUSIVE`.
- Rationale o resumen de la evaluación.

`TestExecution` debe representar una ejecución concreta de una prueba dentro de un scan.

### Finding

Representa un riesgo detectado durante un Scan.

Datos previstos:

- Identificador.
- Scan asociado.
- TestExecution asociada.
- Categoría.
- Nivel de riesgo o severidad: `LOW`, `MEDIUM`, `HIGH` o `CRITICAL`.
- `confidence`, conceptualmente entre `0.0` y `1.0`.
- Evidencia.
- Resultado determinista.
- Evaluación del LLM.
- Veredicto final.
- Explicación.
- Recomendación o remediación.
- Referencia a `testExecutionId`.
- Fecha de creación.

La severidad y el `confidence` son conceptos diferentes: la primera representa la importancia del riesgo y el segundo la confianza en el veredicto. El score global del scan es un resumen y no debe ocultar findings individuales, especialmente los `CRITICAL`.

### Report

Representa el informe generado para un Scan. El formato, almacenamiento y ciclo de vida del informe quedan como **Pendiente de decisión**.

## Relaciones conceptuales

```text
Project 1 ---- N Scan
Project 1 ---- 0..1 TargetConfiguration (MVP; N como evolución futura)
TargetConfiguration 1 ---- N Scan
Scan 1 ------- N TestExecution
SecurityTest 1 ---- N TestExecution
Scan 1 ------- N Finding
TestExecution 1 - N Finding
Scan 1 ------- N Report
```

## Consideraciones de seguridad

- No almacenar claves API en `Project`.
- Las credenciales o referencias de autenticación deben gestionarse de forma segura y solo durante la ejecución autorizada.
- Las peticiones, respuestas y evidencias pueden contener información sensible.
- Deben aplicarse minimización, redacción, control de acceso y retención limitada.
- Project y TargetConfiguration tienen esquema aprobado; los campos e índices de las demás entidades siguen pendientes.

## Persistencia

PostgreSQL será la base de datos principal y Flyway gestionará la evolución del esquema. Este documento describe el modelo conceptual y no constituye todavía el diseño relacional definitivo.
