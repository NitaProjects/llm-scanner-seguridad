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
+--------------+             +----------------+
| Adaptadores  |             | Adaptadores    |
| persistencia |             | HTTP / LLM     |
+------+-------+             +--------+-------+
       |                               |
       v                               v
 PostgreSQL                 Aplicación objetivo
                                      |
                                      v
                              Modelo evaluador
```

El diagrama representa responsabilidades conceptuales. No define todavía clases, endpoints ni contratos definitivos.

## Frontend

Angular proporciona la interfaz web para crear proyectos, configurar targets, seleccionar pruebas, ejecutar scans, consultar findings y visualizar informes.

El frontend se comunica con el backend mediante la API REST. No accede directamente a PostgreSQL, a la aplicación objetivo ni al modelo evaluador.

## Backend

Spring Boot expone la API y coordina el flujo de análisis. Recibe las solicitudes del frontend, valida los datos, ejecuta los casos de uso y devuelve resultados consistentes.

El backend también controla la comunicación con los sistemas externos y debe aplicar límites y validaciones antes de realizar peticiones a un target.

El flujo conceptual de un scan es:

1. Recibir la solicitud de ejecución.
2. Seleccionar los Security Tests configurados.
3. Enviar una colección controlada de casos de prueba al target.
4. Registrar las peticiones y respuestas necesarias.
5. Aplicar reglas deterministas y evaluación mediante LLM.
6. Generar findings con evidencia, severidad, explicación, recomendación y confianza.
7. Calcular el resultado del scan y dejarlo disponible para el dashboard y el informe.

Este flujo se ejecutará de forma asíncrona en el MVP. La solicitud inicial crea el `Scan` en estado `PENDING` y devuelve un identificador de scan; el procesamiento posterior pasa a `RUNNING` y se realiza fuera del ciclo de la petición inicial. El frontend consultará el estado y el progreso mediante polling hasta que el scan termine.

El controller no contiene procesamiento del análisis. `scan-execution` implementa StartScanUseCase/GetScanUseCase/ListProjectScansUseCase y desacopla ProcessScanUseCase mediante ScanDispatcherPort/ScanWorkPort. Un ThreadPoolTaskExecutor dedicado acotado/configurable acepta o rechaza inmediatamente; el índice único parcial PostgreSQL garantiza un activo por Project. Dominio/aplicación no dependen de Spring, JPA ni executor.

T1 confirma PENDING antes de dispatch, T2 reclama RUNNING condicionalmente y T3 confirma terminal; todas son transacciones nuevas cortas. ScanWorkerEntry suspende una transacción ambiente durante la orquestación y el trabajo. `security-test-execution` sustituye el trabajo no-op por ExecuteSecurityTestsUseCase sobre ScanWorkPort. Fija una sola lectura del target y las cuatro definiciones inmutables del catálogo; confirma el plan PENDING completo y cada RUNNING antes de la red. HTTP y espera de cuota ocurren sin transacción; cada resultado se confirma antes del siguiente intento. Los scans no-op históricos conservan sus estados y ejecuciones vacías.

SecurityTestCatalogPort, TestExecutionRepositoryPort y TargetInvocationPort separan catálogo, persistencia e invocación. HttpClient 5 queda dentro del adaptador HTTP, con DNS acotado, validación de todas las IPs y conexión fijada a una sola, TLS del hostname original, cero redirects/retries/proxy/pooling entre intentos y lectura limitada. CredentialDecryptionPort reutiliza AES existente; el plaintext no sale del adaptador. Una compuerta global permite una invocación simultánea y separación mínima de un segundo. Defaults/márgenes y códigos están en STE-09/STE-10 del change.

COMPLETED exige cuatro ejecuciones técnicamente COMPLETED, sin juicio de vulnerabilidad. Un fallo individual permite continuar pero produce Scan FAILED; uno global aborta pendientes. T3 confirma el fallo de Scan y el cierre de ejecuciones activas en una sola transacción. El listado GET executions solo devuelve metadatos; la evidencia JSON/texto redactada queda disponible internamente. No hay evaluador, Findings ni scoring en este incremento. LLM10 observa únicamente output y no valida downstream/sink.

Los estados son `PENDING`, `RUNNING`, `COMPLETED` y `FAILED`. ScanStartup recupera ejecuciones activas a FAILED/EXECUTION_INTERRUPTED y scans activos a FAILED/SCAN_INTERRUPTED en la misma transacción antes de habilitar nuevos inicios y el executor; fallo de recuperación impide arrancar. Conserva terminales, también ejecuciones ya correctas, y cierra ejecuciones activas de scans terminales. Si durante ejecución no puede confirmarse tampoco FAILED, una compuerta local bloquea nuevos inicios/reclamaciones hasta reinicio, preservando consultas y permitiendo finalizar tareas ya RUNNING. Se soporta una sola instancia; no hay relanzamientos ni cola durable. No hay PARTIAL; cancelación y reintentos siguen fuera del incremento.

## Dominio

El dominio representa las reglas y conceptos principales del producto, entre ellos:

- Project.
- Scan.
- SecurityTest.
- TestExecution.
- Finding.
- Report.

El dominio no debe depender directamente de Spring, PostgreSQL, clientes HTTP ni proveedores de modelos LLM.

## Aplicación

La capa de aplicación coordina los casos de uso del sistema. Orquesta el ciclo de vida de proyectos y scans, solicita la ejecución de pruebas, envía las respuestas a la estrategia de evaluación y registra los resultados.

Los casos de uso deben depender de abstracciones y no de implementaciones concretas de infraestructura.

Como orientación de diseño, se prefieren casos de uso explícitos:

- `CreateProjectUseCase`.
- `ConfigureTargetUseCase`.
- `StartScanUseCase`, `GetScanUseCase`, `ListProjectScansUseCase`, `ProcessScanUseCase` y `RecoverInterruptedScansUseCase` (scan-execution).
- `EvaluateExecutionUseCase`.
- `GenerateReportUseCase`.

La división definitiva de responsabilidades y el nivel de granularidad quedan como **Pendiente de decisión** hasta definir cada cambio OpenSpec.

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

PostgreSQL será la persistencia principal de proyectos, scans, ejecuciones, findings e informes. La evolución del esquema se gestionará mediante Flyway.

El modelo relacional detallado, las relaciones y las restricciones se documentarán en `docs/06-base-de-datos/` cuando se definan.

## Aplicación objetivo

Es el endpoint autorizado que se desea analizar. Recibe los prompts o peticiones de prueba y devuelve respuestas que serán registradas y evaluadas.

El acceso al target incorpora los controles contra SSRF, autorización externa de orígenes, DNS efectivo, tiempo, tamaño y frecuencia definidos por security-test-execution. Configurar un target por API no autoriza tráfico; ver la política de autorización consolidada.

El contrato de integración del MVP queda limitado a HTTP REST, `POST` y JSON. La autenticación es opcional y se configura como parte del target.

## Modelo evaluador

Es un modelo LLM independiente de la aplicación objetivo. Analiza las respuestas obtenidas y ayuda a determinar si existe un comportamiento de riesgo. Podrá evaluar semánticamente respuestas, detectar comportamientos que no puedan resolverse solo con reglas, proponer una categoría de riesgo, generar una explicación técnica, proponer una recomendación y devolver un nivel de confianza.

El backend no dependerá directamente de OpenAI, Gemini, Hugging Face ni de ningún proveedor concreto. La integración se realizará mediante una abstracción conceptual:

```text
EvaluatorPort
    ↓
Adaptador de proveedor concreto
```

Durante el MVP se utilizará un único proveedor, configurable externamente. La arquitectura permitirá sustituirlo sin modificar el dominio ni los casos de uso. Los adaptadores concretos podrán incluir, en el futuro, `OpenAiEvaluatorAdapter`, `GeminiEvaluatorAdapter` o `HuggingFaceEvaluatorAdapter`.

El puerto podrá representar conceptualmente una operación como:

```text
evaluate(
    securityTest,
    targetRequest,
    targetResponse,
    deterministicResults
) → EvaluationResult
```

`EvaluationResult` podrá incluir `verdict`, `confidence`, `reasoningSummary`, `suggestedSeverity`, `recommendation`, `evaluatorProvider`, `evaluatorModel` y `evaluatedAt`. No se definen todavía clases Java concretas.

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

La autenticación del target es `NONE`, `BEARER_TOKEN` o `API_KEY_HEADER`. El change `target-configuration` define un recurso único POST/GET/PUT en `/api/projects/{projectId}/target`, casos de uso explícitos, puertos de validación/cifrado/persistencia y adaptadores separados. Cada Project admite cero o un target en el MVP; no se ejecutan requests ni extracción de respuestas.

D-CRYPTO queda cerrado para el MVP local: AES-256-GCM con clave externa de 256 bits y envelope versionado (ADR-002). GET y conservación en PUT no descifran. La provisión AWS Secrets Manager/KMS y los controles SSRF completos de ejecución quedan fuera de este change.

## Decisiones todavía abiertas

- Las alternativas y recomendaciones se recogen en `docs/02-arquitectura/decisiones/decisiones-abiertas.md`.
- Proveedor y modelo concretos del evaluador.
- Valores de coste, número máximo de evaluaciones, timeout, rate limits, longitud máxima y fallback del evaluador.
- Fórmula numérica definitiva, normalización y calibración de severidad, confidence y scoring.
- Diseño del experimento académico definitivo.
- Evolución del executor interno de instancia única hacia cola durable, AWS SQS o workers distribuidos.
- Reintentos y cancelación; recuperación local y fallo agregado con éxitos conservados aprobados en ADR-003.
