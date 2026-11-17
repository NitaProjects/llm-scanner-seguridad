# Validación de add-rag-and-tool-aware-testing

## Observación

- Las fixtures HTTP locales verificaron paridad de consulta, dos chunks,
  petición/resultado y respuesta final mediante JSON REST, JSON chat,
  SSE y JSONL. Los frames se fragmentaron en escrituras independientes.
- La correlación usa IDs efímeros y ordinales locales. Las pruebas cubrieron
  arrays separados, varias herramientas, resultados adelantados y huérfanos,
  IDs duplicados y resultado ausente. El acumulador limita registros y
  bytes UTF-8. JSON parcial, formato incompatible y EOF prematuro no
  entregaron observaciones al motor.
- La fixture adversaria declaró `shell` y pidió un callback. Se capturó
  la declaración como dato local, con una sola petición HTTP y una sola
  consulta DNS de la interacción prevista. El archivo marcador no apareció.

## Evaluación y almacenamiento

- La evidencia viaja en memoria hasta la confirmación de TestExecution.
  El worker mantiene separadas las cinco fuentes, y las citas conservan
  el texto confirmado y sus offsets. El fallo de escritura impide entregar
  las observaciones al evaluador.
- Las pruebas con evaluador simulado verificaron ambos perfiles híbridos.
  Una fuente autorizada se serializó en el mensaje de datos, con proyección
  y hash nulos en el resultado. La corroboración que dependía de una fuente
  local quedó inconclusa y no reservó cuota. Una decisión local v2 suficiente
  conservó su veredicto. Una copia de evidencia local en la respuesta final
  vetó la exportación.
- El operador ejecutó `WorkflowPostgresIntegrationTest` en PostgreSQL
  dedicado. Resultado 1 aprobado, 0 fallos, 0 errores, 0 omitidos y
  `BUILD SUCCESS`, 20,02 s. La fixture acreditó workflow JSON → SSE con
  cookies y CSRF, cierre del Scan con cuatro ejecuciones y cuatro
  evaluaciones, scoring-v1, report-v1, lectura histórica y ausencia de
  valores observados y correlación efímera en las tablas comprobadas.
  La corrida PostgreSQL cubrió la rama local. La rama de proyección
  semántica ampliada se ejercitó offline con evaluador y repositorio fakes.

## Revisión P2

La evidencia terminal se sanea antes de convertirla al resultado que se
persiste. La redacción conjunta incluye valores locales observados e IDs de
correlación. Una respuesta redactada mantiene el veto semántico del perfil.
Las fixtures de JSON, SSE y JSONL repiten un chunk local y un ID en la
respuesta final y comprueban que la proyección legacy contiene solo los
marcadores de redacción.

La fixture PostgreSQL repite ambos valores en la respuesta terminal,
comprueba la redacción tras reabrir la aplicación y busca los valores
originales en ejecuciones, evaluaciones, hallazgos, score e informe.
El operador confirmó la corrida posterior a la corrección de
`WorkflowPostgresIntegrationTest` en PostgreSQL dedicado. Maven registró
1 aprobado, 0 fallos, 0 errores, 0 omitidos y `BUILD SUCCESS` (20,04 s).

La revisión posterior amplió el fallo cerrado a los campos descartados
o alterados antes de inspeccionar la evidencia terminal. Si la inspección
queda incompleta, se sustituyen petición y respuesta antes de la escritura.
Las pruebas dirigidas cubren un ID de 129 bytes repetido y un chunk local
que contiene una credencial estática, ambos también reflejados en la
petición de la fixture. Las 62 pruebas dirigidas pasaron sin omisiones.

El operador confirmó la fixture PostgreSQL ampliada con el ID de 129 bytes
y un chunk solapado con CSRF. La fixture confirma el cierre del Scan y
comprueba tras reabrir la aplicación que se sustituyeron ambas evidencias.
También comprueba la ausencia durable del ID y de los fragmentos del chunk.
Maven registró 1 aprobado, 0 fallos, 0 errores, 0 omitidos y
`BUILD SUCCESS` (20,08 s).

## Regresión

- Suite backend offline con Java 21 y `.\mvnw.cmd -o test`. Maven registró
  804 pruebas, 710 aprobadas, 0 fallos, 0 errores y 94 omitidas. Incluye
  25 pruebas de observaciones, 27 de evaluación híbrida, 30 de workflow y
  33 de credenciales, todas aprobadas y sin omisiones.
- Pasaron las regresiones REST_LEGACY, OPENAI_COMPATIBLE, SSE, JSONL,
  sesión, credenciales, origen, API, hybrid-v1/v2, scoring-v1 y report-v1.
- `openspec validate add-rag-and-tool-aware-testing --strict` pasó.
  Tareas completas 7/7.
