# Arquitectura general

## Visión conceptual

LLM Scanner Seguridad se plantea como un monolito modular con un frontend Angular y un backend Spring Boot. El backend concentra la lógica de aplicación y coordina las integraciones externas.

```text
+------------------+
| Angular frontend |
+--------+---------+
         |
         | REST / JSON
         v
+---------------------------+
| Spring Boot API           |
| entrada y coordinación   |
+-------------+-------------+
              |
              v
+---------------------------+
| Aplicación y dominio      |
| casos de uso y reglas     |
+------+------+-------------+
       |      |
       |      +----------------------+
       |                             |
       v                             v
+--------------+             +-------------------------+
| Adaptadores  |             | Adaptadores HTTP / LLM  |
| persistencia |             | integraciones separadas |
+------+-------+             +------+-----------+------+
       |                            |           |
       v                            v           v
 PostgreSQL                  Aplicación      Modelo
                              objetivo      evaluador
```

El diagrama resume responsabilidades; el backend coordina las integraciones independientes con target y evaluador. El backend core está aprobado e implementado por `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`. Los contratos actuales están en `openspec/changes/<cambio>/specs/<capacidad>/spec.md` y en [OpenAPI](../05-api/openapi.yaml); `openspec/specs/` aún no contiene specs consolidadas. La consolidación/archivo podrá realizarse posteriormente según el workflow, conservando los históricos.

## Frontend

Angular es la interfaz prevista para crear proyectos, configurar targets, iniciar scans sobre el plan fijo de cuatro pruebas, consultar findings y visualizar informes. Su integración y el dashboard siguen en progreso; no hay selección configurable de pruebas en el MVP actual.

El frontend se comunica con el backend mediante la API REST. No accede directamente a PostgreSQL, a la aplicación objetivo ni al modelo evaluador.

## Backend

Spring Boot expone la API y coordina el flujo de análisis. Recibe las solicitudes del frontend, valida los datos, ejecuta los casos de uso y devuelve resultados consistentes.

El backend también controla la comunicación con los sistemas externos y debe aplicar límites y validaciones antes de realizar peticiones a un target.

El flujo conceptual de un scan es:

1. Recibir la solicitud de ejecución.
2. Preparar el plan fijo de exactamente cuatro SecurityTests inmutables, uno por categoría, sin selección configurable por usuario.
3. Enviar una colección controlada de casos de prueba al target.
4. Registrar las peticiones y respuestas necesarias.
5. Aplicar reglas deterministas y evaluación mediante LLM.
6. Generar findings con evidencia, severidad, explicación, recomendación y confianza.
7. Calcular ScanScore (`scoring-v1`), generar SecurityReport (`report-v1`) y confirmar ambos junto al cierre COMPLETED; dejarlos disponibles por API para su futura visualización en el frontend.

Este flujo se ejecuta de forma asíncrona en el MVP. La solicitud inicial crea el `Scan` en estado `PENDING` y devuelve un identificador de scan; el procesamiento posterior pasa a `RUNNING` y se realiza fuera del ciclo de la petición inicial. La API permite polling de estado y progreso derivado de ejecuciones. Cada Scan nuevo COMPLETED exige cuatro ejecuciones y cuatro evaluaciones COMPLETED, ScanScore y SecurityReport íntegros. Un fallo técnico global puede llevar a FAILED; un fallo/degradación LLM consolidado puede producir INCONCLUSIVE o VULNERABLE según las reglas sin implicar Scan FAILED. Los históricos conservan su semántica sin backfill.

El controller no contiene procesamiento del análisis. `scan-execution` implementa StartScanUseCase/GetScanUseCase/ListProjectScansUseCase y desacopla ProcessScanUseCase mediante ScanDispatcherPort/ScanWorkPort. Un ThreadPoolTaskExecutor dedicado acotado/configurable acepta o rechaza inmediatamente; el índice único parcial PostgreSQL garantiza un activo por Project. Dominio/aplicación no dependen de Spring, JPA ni executor.

T1 confirma PENDING antes de dispatch, T2 reclama RUNNING condicionalmente y T3 confirma terminal; todas son transacciones nuevas cortas. ScanWorkerEntry suspende una transacción ambiente durante la orquestación y el trabajo. `security-test-execution` sustituye el trabajo no-op por ExecuteSecurityTestsUseCase sobre ScanWorkPort. Fija una sola lectura del target y las cuatro definiciones inmutables del catálogo; confirma el plan PENDING completo y cada RUNNING antes de la red. HTTP y espera de cuota ocurren sin transacción; cada resultado se confirma antes del siguiente intento. Los scans no-op históricos conservan sus estados y ejecuciones vacías.

SecurityTestCatalogPort, TestExecutionRepositoryPort y TargetInvocationPort separan catálogo, persistencia e invocación. HttpClient 5 queda dentro del adaptador HTTP, con DNS acotado, validación de todas las IPs y conexión fijada a una sola, TLS del hostname original, cero redirects/retries/proxy/pooling entre intentos y lectura limitada. CredentialDecryptionPort reutiliza AES existente; el plaintext no sale del adaptador. Una compuerta global permite una invocación simultánea y separación mínima de un segundo. Defaults/márgenes y códigos están en STE-09/STE-10 del change.

`evaluation-engine` fija `evaluation-engine-v1` en T1 de cada Scan nuevo. El plan confirma cuatro ejecuciones y cuatro evaluaciones PENDING atómicamente. Tras confirmar cada ejecución, EvaluateExecutionUseCase aplica reglas, solicita EvaluatorPort cuando la proyección sintética está autorizada y consolida antes del siguiente probe. COMPLETED exige cuatro ejecuciones y cuatro evaluaciones COMPLETED: VULNERABLE, INCONCLUSIVE y fallos LLM consolidados no son fallo técnico global. EvaluationResult FAILED, fallos del target o deadline global impiden ese cierre. T3 cierra hijos activos y Scan atómicamente. Históricos sin perfil permanecen intactos. GET executions conserva su contrato; evaluations/findings añaden proyecciones sin evidencia. LLM10 conserva OUTPUT_ONLY sin validar downstream/sink.

`scoring-engine` añade `domain/scoring` y `application/scoring` sin modificar Evaluation Engine. T1 fija además `scoring-v1` en un marcador interno nullable. T3 bloquea Scan primero; ScoringInputPort lee únicamente plan, identidades y finales estructurados, valida cuatro pruebas/revisiones/categorías y correspondencia V↔Finding; CalculateScanScoreUseCase aplica ScoringPolicyV1 sin red. ScanScoreRepositoryPort inserta un único resultado inmutable y la misma transacción confirma RUNNING→COMPLETED. Un error revierte ambas escrituras y aplica la compensación existente. CalculatedAt no precede inputs y finishedAt no precede calculatedAt. No cambia el presupuesto de 300 s, el DTO de Scan ni las transacciones del evaluador.

V6 añade scan_scores con PK Scan+versión, contadores y cuatro VOs de categoría en JSONB cerrado; no hay tablas/lifecycle por categoría. El GET scoped /score lee estado, marcador y score en una única instantánea SQL, sin calcular ni acceder a evidencia, target o proveedor. Distingue AVAILABLE/PENDING/UNAVAILABLE/NOT_APPLICABLE; un histórico sin marcador no recibe backfill y global_score heredado sigue sin uso. Recovery valida integridad y conserva score+COMPLETED confirmados; interrumpe activos sin recalcular. Fórmula, bandas, cero e incertidumbre: ADR-005; contrato y persistencia: OpenAPI/modelo de datos.

Los estados son `PENDING`, `RUNNING`, `COMPLETED` y `FAILED`. ScanStartup recupera ejecuciones activas a FAILED/EXECUTION_INTERRUPTED y scans activos a FAILED/SCAN_INTERRUPTED en la misma transacción antes de habilitar nuevos inicios y el executor; fallo de recuperación impide arrancar. Conserva terminales, también ejecuciones ya correctas, y cierra ejecuciones activas de scans terminales. Si durante ejecución no puede confirmarse tampoco FAILED, una compuerta local bloquea nuevos inicios/reclamaciones hasta reinicio, preservando consultas y permitiendo finalizar tareas ya RUNNING. Se soporta una sola instancia; no hay relanzamientos ni cola durable. No hay PARTIAL; cancelación y reintentos siguen fuera del incremento.

`reporting` incorpora SecurityReport como snapshot local inmutable `report-v1/es`, independiente de renderer y de los motores de evaluación/scoring. T1 fija además `report_version`; T3 conserva el lock de Scan, inserta ScanScore, lee su proyección literal, genera/inserta SecurityReport y confirma COMPLETED en el mismo commit. GenerateReportUseCase participa sin transacción propia ni red; no reconstruye ScanScore ni ejecuta su política. GetSecurityReportUseCase solo lee un snapshot coherente de Scan/marcadores/score/report. ReportInputPort selecciona columnas públicas permitidas; ReportPresentationPort carga constantes locales versionadas. Detalle contractual y alternativas: [ADR-008](decisiones/ADR-008-reporting-snapshot-inmutable.md).

V7 añade `security_reports` (metadata relacional y JSONB cerrado de doce campos) y un marcador nullable prospectivo, sin backfill. Los informes conservan referencias históricas del plan, summary literal, cuatro evaluaciones mínimas y hasta cuatro Findings ordenados por severidad/categoría/id. Locale es solo localiza textos propios; los cuatro textos de Finding permanecen en su idioma original. INCONCLUSIVE y las nueve limitaciones siempre son visibles. Recovery valida el conjunto antes de admisión, preserva terminales e interrumpe activos sin regenerar. GET /report distingue AVAILABLE/PENDING/UNAVAILABLE/NOT_APPLICABLE con no-store; un histórico V6 con score no tiene informe aplicable.

## Dominio

El dominio representa las reglas y conceptos principales del producto, entre ellos:

- Project.
- Scan.
- SecurityTest.
- TestExecution.
- Finding.
- ScanScore, RiskLevel y los VOs contenidos CategoryScore/SeverityCounts.
- SecurityReport y sus VOs contenidos, sin lifecycle propio.

El dominio no debe depender directamente de Spring, PostgreSQL, clientes HTTP ni proveedores de modelos LLM.

## Aplicación

La capa de aplicación coordina los casos de uso del sistema. Orquesta el ciclo de vida de proyectos y scans, solicita la ejecución de pruebas, envía las respuestas a la estrategia de evaluación y registra los resultados.

Los casos de uso deben depender de abstracciones y no de implementaciones concretas de infraestructura.

Entre los casos de uso implementados se encuentran:

- `CreateProjectUseCase`.
- `ConfigureTargetUseCase`.
- `StartScanUseCase`, `GetScanUseCase`, `ListProjectScansUseCase`, `ProcessScanUseCase` y `RecoverInterruptedScansUseCase` (scan-execution).
- `EvaluateExecutionUseCase`.
- `CalculateScanScoreUseCase` y `GetScanScoreUseCase` (scoring-engine).
- `GenerateReportUseCase` y `GetSecurityReportUseCase` (reporting).

Las responsabilidades, puertos y granularidad del backend core están definidos por los diseños aprobados de los siete changes e implementados en el backend. Los cambios de alcance futuros requieren su propio workflow OpenSpec.

## Infraestructura

La infraestructura contiene los detalles técnicos necesarios para conectar el sistema con servicios externos y persistir información. Incluye adaptadores para:

- PostgreSQL.
- La aplicación LLM objetivo.
- El modelo evaluador.
- La generación de informes.

La infraestructura puede utilizar Spring Boot y sus librerías, pero esas dependencias no deben filtrarse innecesariamente hacia el dominio.

## Puertos y adaptadores

Los puertos definen las capacidades que necesita la aplicación, como guardar un proyecto, ejecutar una petición contra el target o evaluar una respuesta.

Los adaptadores implementan esos puertos para tecnologías concretas. Este enfoque permite sustituir un proveedor, cliente HTTP o mecanismo de persistencia sin cambiar las reglas principales del dominio.

## PostgreSQL

PostgreSQL es la persistencia principal de proyectos, targets, scans, ejecuciones, evaluaciones, findings, scores e informes. Flyway gestiona la evolución del esquema hasta V7.

El modelo relacional, las relaciones y las restricciones aprobadas están documentados en [modelo-datos.md](../06-base-de-datos/modelo-datos.md), incluidas las diferencias entre históricos y nuevos scans.

## Aplicación objetivo

Es el endpoint autorizado que se desea analizar. Recibe los prompts o peticiones de prueba y devuelve respuestas que serán registradas y evaluadas.

El acceso al target incorpora los controles contra SSRF, autorización externa de orígenes, DNS efectivo, tiempo, tamaño y frecuencia definidos por security-test-execution. Configurar un target por API no autoriza tráfico; ver la política de autorización consolidada.

El contrato de integración del MVP queda limitado a HTTP REST, `POST` y JSON. La autenticación es opcional y se configura como parte del target.

## Modelo evaluador

Es un modelo LLM independiente de la aplicación objetivo. Evalúa semánticamente evidencia sintética autorizada e interpreta la categoría y los criterios fijados por el backend sin modificarlos. Devuelve un assessment con veredicto, confianza, severidad sugerida, explicación, recomendación y referencias verificables; el engine consolida el resultado híbrido y sustituye las narrativas libres por plantillas inertes.

El dominio y los casos de uso no dependen de un proveedor concreto. La integración utiliza el puerto:

```text
EvaluatorPort
    ↓
Adaptador de proveedor concreto
```

El MVP implementa un único adaptador `OpenAiEvaluatorAdapter` para OpenAI Responses API/Structured Outputs, con modelo inicial `gpt-6-luna` configurable externamente (ADR-004). La abstracción permite sustituir el proveedor sin acoplar dominio ni casos de uso; otros adaptadores, como Gemini o Hugging Face, siguen siendo futuros. La validación real de cuenta, disponibilidad, retención y tarifa sigue pendiente antes de operación autorizada.

El contrato aprobado e implementado es:

```text
evaluate(EvaluatorInput, EvaluationCallBudget) → EvaluatorOutcome
```

El puerto devuelve assessment validado o fallo cerrado; HybridDecisionPolicy calcula el resultado final fuera del adaptador. EvaluationResult guarda estado técnico, fallo LLM separado, señales, assessment sanitizado, verdict/confidence/severity, snapshots sintéticos y metadata/versiones. Finding solo existe para COMPLETED/VULNERABLE y se confirma junto con el resultado. Catálogo JSON y manifiesto local se fijan una vez; no se modifica SecurityTest ni TargetConfiguration.

La decisión final se basará en una estrategia híbrida formada por reglas deterministas, criterios esperados y evaluación mediante LLM. El evaluador no decidirá por sí solo la existencia de una vulnerabilidad.

El proveedor y el modelo se configurarán mediante variables de entorno o configuración externa. Nunca se hardcodearán API keys, se incluirán secretos en el repositorio, se escribirán claves en logs ni se persistirán secretos en findings o informes.

Las evaluaciones forman parte del procesamiento asíncrono del scan. El resultado, la confianza y los errores del modelo evaluador deben persistirse asociados a cada `TestExecution`. Un fallo del evaluador debe distinguirse de una vulnerabilidad detectada en el target.

## Contrato conceptual del target

El MVP no dependerá de un formato fijo de request. Cada target se configurará mediante una estructura conceptual `TargetConfiguration` con:

- `name`.
- `url`.
- `method = POST`.
- `contentType = application/json`.
- `requestTemplate`.
- `promptPlaceholder = {{PROMPT}}`.
- `responsePath`.
- `headers` opcionales.
- `authType`.
- `authConfig`.
- `timeout`.

La plantilla permitirá construir el body JSON e insertar el prompt en la ubicación configurada. `responsePath` permitirá extraer el texto relevante de la respuesta, por ejemplo `$.response.text`.

Ejemplo conceptual de request:

```json
{
  "message": "{{PROMPT}}",
  "temperature": 0.2
}
```

La autenticación del target es `NONE`, `BEARER_TOKEN` o `API_KEY_HEADER`. El change `target-configuration` define un recurso único POST/GET/PUT en `/api/projects/{projectId}/target`, casos de uso explícitos, puertos de validación/cifrado/persistencia y adaptadores separados. Cada Project admite cero o un target en el MVP. Las operaciones de configuración no ejecutan requests ni extracción de respuestas; ese trabajo corresponde al worker implementado por `security-test-execution`.

D-CRYPTO queda cerrado para el MVP local: AES-256-GCM con clave externa de 256 bits y envelope versionado (ADR-002). GET y conservación en PUT no descifran. Históricamente los controles SSRF de ejecución quedaron fuera de `target-configuration`; `security-test-execution` ya los incorpora, según la [política de autorización](../03-seguridad/politica-de-autorizacion.md). La provisión AWS Secrets Manager/KMS, custodia y rotación avanzada siguen pendientes.

## Decisiones todavía abiertas

- Las alternativas y recomendaciones se recogen en `docs/02-arquitectura/decisiones/decisiones-abiertas.md`.
- OpenAI y modelo inicial configurable gpt-6-luna están aprobados; cuenta/disponibilidad/retención operativa se comprueban antes de uso real autorizado.
- Límites y fallback del evaluador cerrados en evaluation-engine D-09/D-15; coste monetario estimado opcional y externo.
- confidence-v1/severity-v1/scoring-v1 aprobadas; calibración estadística sigue pendiente.
- Diseño del experimento académico definitivo.
- Evolución del executor interno de instancia única hacia cola durable, AWS SQS o workers distribuidos.
- Reintentos y cancelación; recuperación local y fallo agregado con éxitos conservados aprobados en ADR-003.
