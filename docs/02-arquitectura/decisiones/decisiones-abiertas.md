# Decisiones abiertas del diseño

Este documento registra alternativas y recomendaciones para decisiones relevantes del MVP. Las recomendaciones no constituyen decisiones cerradas. Las decisiones cerradas se identifican explícitamente y las restantes mantienen el estado **Pendiente de decisión** hasta su aprobación y posterior reflejo en OpenSpec.

## 1. Contrato del endpoint objetivo

### Restricciones ya establecidas

El MVP utilizará HTTP REST, método `POST`, `Content-Type: application/json`, entrada y salida JSON, un campo configurable para el mensaje del usuario y extracción configurable de la respuesta. No se incluirán inicialmente agentes, herramientas, streaming ni RAG avanzado.

### Opciones

1. **Contrato fijo compatible con un único formato**
   - Ventajas: implementación sencilla, validación clara y pruebas reproducibles.
   - Inconvenientes: limita la utilidad a targets con ese formato y dificulta las demostraciones con APIs distintas.

2. **Contrato JSON configurable mediante rutas de campos**
   - Ventajas: mantiene el alcance REST/JSON y permite adaptar la ubicación del mensaje y de la respuesta.
   - Inconvenientes: aumenta la configuración, la validación y los posibles errores de usuario.

3. **Adaptadores predefinidos para varios formatos**
   - Ventajas: experiencia de usuario más guiada y menos configuración manual.
   - Inconvenientes: incrementa el código, el mantenimiento y el alcance del MVP.

### Decisión adoptada

Se adopta un contrato JSON configurable de alcance limitado, representado conceptualmente por `TargetConfiguration`. Incluirá plantilla de request, placeholder `{{PROMPT}}`, `responsePath`, headers configurables, autenticación opcional y timeout configurable. Se evitarán scripting arbitrario y transformaciones complejas.

### Impacto en el MVP

La decisión permite demostrar varios targets sin convertir el scanner en una plataforma genérica de integración. Requiere validar configuraciones y documentar claramente los errores. La decisión se documenta también en `docs/02-arquitectura/decisiones/ADR-001-contrato-target.md`.

**Estado: Decisión cerrada para el MVP.**

## 2. Categorías de pruebas del MVP

La cuarta categoría queda cerrada como **LLM10 – Improper Output Handling**. El MVP cubrirá exactamente estas cuatro categorías del OWASP GenAI LLM Top 10 (2026):

- LLM01 – Prompt Injection.
- LLM02 – Sensitive Information Disclosure.
- LLM08 – Hidden Context Exposure.
- LLM10 – Improper Output Handling.

Las categorías LLM03, LLM04, LLM05, LLM06, LLM07 y LLM09 quedan fuera del MVP y se documentan como evoluciones futuras.

**Estado: Decisión cerrada para el alcance del MVP.**

## 3. Autenticación contra el target y gestión de credenciales

### Opciones consideradas

1. **Sin autenticación (`NONE`)**
   - Ventajas: mínima complejidad y adecuada para la aplicación vulnerable.
   - Inconvenientes: no cubre targets protegidos.

2. **Bearer token (`BEARER_TOKEN`)**
   - Ventajas: patrón habitual para APIs HTTP y sencillo de expresar mediante `Authorization`.
   - Inconvenientes: requiere gestionar un secreto sensible y evitar su exposición.

3. **API key en header (`API_KEY_HEADER`)**
   - Ventajas: cubre APIs que utilizan una cabecera propietaria como `X-API-Key`.
   - Inconvenientes: requiere gestionar de forma segura el nombre de la cabecera y su secreto.

4. **OAuth2, mTLS, Basic Auth avanzada, cookies/sesión, flujos interactivos y autenticación multi-step**
   - Ventajas: cubren escenarios empresariales adicionales.
   - Inconvenientes: aumentan considerablemente la complejidad y quedan fuera del MVP.

### Decisión adoptada

El MVP soportará únicamente los siguientes tipos:

- `NONE`.
- `BEARER_TOKEN`.
- `API_KEY_HEADER`.

La configuración conceptual del target incluirá `authType` y `authConfig`.

Para `BEARER_TOKEN`, el header conceptual será:

```text
Authorization: Bearer <TOKEN>
```

Para `API_KEY_HEADER`, la configuración conceptual incluirá:

```text
headerName
secretValue
```

Ejemplo:

```text
X-API-Key: <SECRET>
```

### Gestión de credenciales

Las credenciales:

- Nunca aparecerán en logs.
- Nunca se incluirán en informes.
- Nunca se devolverán al frontend después de configurarlas.
- Nunca se almacenarán en texto plano.
- No se incluirán sin sanitización en payloads persistidos.
- No se devolverán mediante mensajes de error.

El flujo conceptual será:

```text
Frontend envía token/API key
    -> backend cifra el secreto
    -> PostgreSQL almacena el valor cifrado
    -> backend descifra en memoria durante la ejecución
    -> se utiliza en la request al target
    -> no se devuelve ni se registra
```

Para fases posteriores y AWS se considerará la utilización conjunta de AWS Secrets Manager y AWS KMS: Secrets Manager para almacenar y gestionar secretos, y KMS para gestionar las claves de cifrado. La tecnología concreta de cifrado, almacenamiento y gestión de claves queda como **Pendiente de decisión**.

### Impacto en el MVP

El alcance cubre la aplicación vulnerable sin autenticación y dos patrones habituales de APIs protegidas, sin incorporar flujos interactivos. Añade requisitos de protección de secretos, sanitización de evidencias y control estricto de errores.

**Estado: Decisión funcional cerrada para el MVP. La implementación criptográfica y de secret manager permanece pendiente.**

## 4. Proveedor y modelo LLM evaluador

### Opciones

1. **Proveedor gestionado externo**
   - Ventajas: integración rápida, buena capacidad semántica y menor esfuerzo operativo.
   - Inconvenientes: coste, dependencia externa y posible exposición de respuestas sensibles.

2. **Modelo local u open source**
   - Ventajas: mayor control de datos y posibilidad de trabajar sin enviar respuestas a terceros.
   - Inconvenientes: requisitos de hardware, operación más compleja y posible menor calidad.

3. **Arquitectura de proveedor intercambiable**
   - Ventajas: permite comparar proveedores y evita acoplamiento.
   - Inconvenientes: mayor diseño inicial y más trabajo de configuración.

### Recomendación técnica

Definir un puerto de evaluación independiente del proveedor y utilizar un único proveedor durante el MVP, combinado siempre con reglas deterministas y criterios esperados. El proveedor y el modelo se configurarán externamente y no formarán parte del dominio ni de los casos de uso.

La abstracción conceptual será:

```text
EvaluatorPort
    ↓
Proveedor concreto
```

Podrán existir adaptadores como `OpenAiEvaluatorAdapter`, `GeminiEvaluatorAdapter` o `HuggingFaceEvaluatorAdapter`, sin que el dominio dependa de ellos.

El puerto podrá representar conceptualmente una operación como:

```text
evaluate(
    securityTest,
    targetRequest,
    targetResponse,
    deterministicResults
) → EvaluationResult
```

`EvaluationResult` deberá contemplar, conceptualmente, veredicto, confianza, resumen del razonamiento, severidad sugerida, recomendación, proveedor, modelo y fecha de evaluación.

### Impacto en el MVP

La abstracción permite cambiar de proveedor sin modificar el dominio ni la aplicación. El MVP deberá controlar costes, proteger los datos enviados, registrar el proveedor y modelo utilizados y mantener la decisión final dentro de la estrategia híbrida. El LLM evaluador no podrá determinar por sí solo la existencia de una vulnerabilidad.

**Estado: Decisión arquitectónica cerrada. El proveedor y el modelo concretos, así como sus límites operativos, permanecen Pendientes de decisión.**

## 5. Ejecución asíncrona de scans

### Opciones

1. **Ejecución síncrona dentro de la petición HTTP**
   - Ventajas: implementación sencilla y flujo fácil de comprender.
   - Inconvenientes: timeouts, mala experiencia de usuario y dificultad para scans largos.

2. **Ejecución asíncrona en el mismo monolito**
   - Ventajas: permite devolver un identificador de scan, consultar estados y mantener el alcance del MVP.
   - Inconvenientes: requiere controlar tareas, errores, concurrencia y reinicios.

3. **Cola y workers persistentes**
   - Ventajas: mejor aislamiento, escalabilidad y recuperación.
   - Inconvenientes: introduce nuevos componentes y complejidad operativa.

### Recomendación técnica

El MVP utilizará ejecución asíncrona de scans con estados persistidos y comunicación desacoplada entre el controller y el procesamiento del análisis. La solicitud inicial creará un scan en estado `PENDING` y devolverá su identificador; el frontend consultará el estado y el progreso mediante polling.

La decisión funcional no selecciona todavía entre asincronía interna de Spring, una cola de mensajes, AWS SQS o un worker independiente. Para el MVP se prioriza mantener la solución simple y evolutiva, dejando la selección tecnológica para el diseño del cambio OpenSpec que la implemente.

Los estados mínimos serán:

- `PENDING`: scan creado y pendiente de procesamiento.
- `RUNNING`: scan en ejecución.
- `COMPLETED`: procesamiento finalizado.
- `FAILED`: procesamiento terminado con un error que impide completar el scan.

Un fallo individual debe persistirse en la ejecución de la prueba y no tiene por qué detener el resto del scan. La política definitiva para representar resultados parciales y determinar el estado final queda pendiente.

### Impacto en el MVP

Mejora la experiencia y permite registrar histórico sin mantener una petición HTTP abierta durante todo el análisis. Deben definirse posteriormente el comportamiento ante reinicio, cancelación, timeout, reintentos, recuperación y error parcial.

`CANCELLED` y `PARTIAL` no forman parte de los estados mínimos del MVP. Su incorporación queda como **Pendiente de decisión**.

**Estado: Decisión funcional cerrada para el MVP. La tecnología concreta de ejecución y las políticas operativas permanecen pendientes.**

## 6. Métrica de severidad y scoring

### Opciones

1. **Severidad cualitativa sin puntuación global**
   - Ventajas: evita falsa precisión y es fácil de explicar.
   - Inconvenientes: dificulta comparar scans y resumir resultados en un dashboard.

2. **Puntuación determinista ponderada**
   - Ventajas: reproducible, auditable y fácil de repetir.
   - Inconvenientes: los pesos pueden ser subjetivos y simplificar demasiado el riesgo.

3. **Puntuación híbrida con confianza del evaluador**
   - Ventajas: combina severidad, evidencia y confianza.
   - Inconvenientes: más difícil de calibrar y puede transmitir una precisión que los datos no justifican.

### Recomendación técnica

Se adopta un modelo conceptual que separa severidad, confidence y score global.

La severidad tendrá cuatro niveles:

- `LOW`: impacto limitado o comportamiento dudoso.
- `MEDIUM`: vulnerabilidad relevante con impacto contenido.
- `HIGH`: vulnerabilidad clara con impacto importante.
- `CRITICAL`: exposición grave, compromiso evidente o riesgo inmediato.

El `confidence` representará la confianza del sistema en la corrección del finding, con un rango conceptual de `0.0` a `1.0`. Se calculará combinando reglas deterministas, criterios esperados, evaluación semántica del LLM, consistencia entre señales y calidad de la evidencia. No dependerá exclusivamente del LLM.

Las señales se conservarán separadas como `deterministicResult`, `llmVerdict`, `finalVerdict`, `confidence` y `rationale`. La coincidencia entre reglas y LLM aumentará la confianza; las discrepancias se registrarán y podrán reducirla o requerir revisión.

El score global del scan considerará número de findings, severidad, confidence y categoría. Como pesos provisionales se documentan `LOW = 1`, `MEDIUM = 3`, `HIGH = 7` y `CRITICAL = 10`. La fórmula definitiva, normalización, pesos por categoría y umbrales deberán validarse experimentalmente.

Un score global bajo nunca podrá ocultar un finding `CRITICAL`, que deberá mostrarse claramente de forma independiente.

### Impacto en el MVP

Permite presentar resultados claros y comparar estrategias en el experimento. La revisión manual podrá incorporarse posteriormente para confirmar findings, rechazar falsos positivos, modificar severidad o añadir comentarios.

**Estado: Decisión conceptual cerrada para el MVP. La fórmula numérica definitiva, los umbrales, la calibración de confidence y la revisión manual permanecen pendientes.**

## 7. Criterios de detección del MVP

Los criterios conceptuales de detección de LLM01, LLM02, LLM08 y LLM10 quedan definidos en `docs/03-seguridad/riesgos-owasp.md` y `docs/04-ia/casos-de-prueba.md`.

Se adopta la regla de que no puede generarse un finding únicamente a partir de la opinión del LLM evaluador. Debe existir evidencia asociada a una `TestExecution`, señales deterministas o criterios esperados cuando sean aplicables, y un veredicto final entre `VULNERABLE`, `NOT_VULNERABLE` o `INCONCLUSIVE`.

La fórmula matemática de `confidence`, los umbrales concretos, los payloads, la severidad definitiva por caso y el procedimiento experimental siguen **Pendientes de decisión**.

**Estado: Decisión conceptual cerrada para el MVP.**

## 8. Experimento académico definitivo

El experimento final será un benchmark controlado y reproducible sobre escenarios autorizados, utilizando `demo-vulnerable/` como fuente principal. Comparará reglas deterministas, LLM evaluador y evaluación híbrida sobre las cuatro categorías del MVP.

El dataset incluirá casos vulnerables, no vulnerables y, cuando proceda, ambiguos. Cada caso tendrá un ground truth documentado y la estructura conceptual `ExperimentCase` definida en `docs/01-investigacion/experimento-academico.md`.

Se medirán true positives, true negatives, false positives, false negatives, precision, recall, reproducibilidad, tiempo medio y coste aproximado.

El benchmark controlado y la comparación de estrategias quedan adoptados como núcleo del experimento. No se incorpora como requisito el estudio con desarrolladores sobre comprensión del informe.

El tamaño del dataset, número de repeticiones, etiquetado definitivo, tratamiento estadístico de `INCONCLUSIVE`, proveedor/modelo evaluador y valores objetivo permanecen **Pendientes de decisión**.

**Estado: Decisión conceptual cerrada para el TFM.**
