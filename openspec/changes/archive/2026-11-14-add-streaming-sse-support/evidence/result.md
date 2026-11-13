# add-streaming-sse-support — validación

2026-11-14. **6/6 verificadas. READY FOR FINAL REVIEW.** Cierre persistido con PostgreSQL **PASS**.

| Ejecución | Total | Pasados | Fallos | Errores | Omitidos |
| --- | ---: | ---: | ---: | ---: | ---: |
| Suite backend antes del ajuste final de delimitador JSONL | 682 | 590 | 0 | 0 | 92 |
| Regresión dirigida final de transporte, wiring y ejecución | 20 | 20 | 0 | 0 | 0 |
| StreamingTargetTest, incluida en la dirigida final | 10 | 10 | 0 | 0 | 0 |
| StreamingPostgresIntegrationTest, incluida en la suite offline | 1 | 0 | 0 | 0 | 1 |
| StreamingPostgresIntegrationTest, ejecución manual con PostgreSQL | 1 | 1 | 0 | 0 | 0 |

Las ejecuciones no se suman. Las omisiones corresponden a pruebas condicionadas
por el entorno y no cuentan como PASS. Suite offline con regresiones REST_LEGACY,
OPENAI_COMPATIBLE, hybrid-v1/v2, scoring-v1 y report-v1 en PASS para los tests
ejecutados. Solo se usaron fixtures locales y datos sintéticos.

- SSE reconstruye deltas con BOM, CRLF/LF/CR, eventos ignorados, comentarios,
  `data` multilínea, UTF-8 fragmentado y terminal explícito aun con el servidor
  abierto. Se redacta la credencial sintética partida entre deltas.
- JSONL admite ambos media types declarados, líneas fragmentadas, snapshots y
  terminal por discriminador o por objeto literal completo. JSONL malformado,
  media type distinto, error remoto, terminal sin contenido y EOF prematuro
  devuelven `RESPONSE_FORMAT_ERROR` sin texto parcial.
- La negociación exige un único `Accept` del binding aunque el target tenga
  configurado `application/json`. Las colisiones `aCcEpT` con credencial o
  nombre reservado se rechazan con cero DNS, descifrados y peticiones.
- Los topes de entidad, frame de 32 KiB y 1 024 unidades lógicas acotan la
  lectura. Los heartbeats consumen cuota. Los excesos devuelven
  `RESPONSE_TOO_LARGE`. Timeout e interrupción cierran la lectura pendiente
  y descartan el texto parcial. Se aplican las cotas vigentes de texto/evidencia.
- El resultado técnico conserva HTTP status, petición saneada y un único texto
  final. El diagnóstico DEBUG solo incluye unidades, bytes, duración, terminal
  y error. No se escriben frames crudos en el pipeline persistido.

El operador confirmó **BUILD SUCCESS** en **9.159 s** con PostgreSQL de test.
La fixture acredita que el binding permanece fijo tras cambiar el fichero
durante el Scan y que este cierra en `COMPLETED` con cuatro TestExecutions,
cuatro evaluaciones, `scoring-v1` y `report-v1` en el esquema vigente.
La ejecución manual cierra la tarea 2.2. Comando de la integración:

```powershell
.\mvnw.cmd -o '-Dtest=StreamingPostgresIntegrationTest' test
```

OpenSpec strict **PASS**.

### P2.1 y P2.2 — líneas JSONL y cuota SSE

JSONL omite únicamente líneas de longitud cero una vez consumido el delimitador
LF o CRLF. Cualquier otra línea pasa al parser estricto. Las fixtures aceptan
líneas vacías y objetos rodeados de whitespace JSON. Tras un delta válido,
U+2003, U+3000 y una línea de espacios/tabuladores devuelven
`RESPONSE_FORMAT_ERROR` sin texto evaluable, aunque el terminal posterior sea válido.

Cada línea SSE consume una unidad antes de decidir su significado. Se incluyen
los campos ignorados, comentarios y delimitadores de eventos. La fixture completa
con 1 024 líneas totales y terminal. Con 1 025 devuelve `RESPONSE_TOO_LARGE`, tanto
con un flood de `id:` antes del contenido como tras acumular un delta. En SSE y
JSONL un servidor de prueba que mantiene abierto el stream observa el cierre de
la conexión tras el fallo. El resultado conserva el error y descarta el prefijo.

| Validación | Total | Pasados | Fallos | Errores | Omitidos |
| --- | ---: | ---: | ---: | ---: | ---: |
| StreamingTargetTest | 18 | 18 | 0 | 0 | 0 |
| Dirigida con wiring y ejecución | 28 | 28 | 0 | 0 | 0 |
| Suite backend offline | 691 | 599 | 0 | 0 | 92 |

La suite offline mantiene en PASS las regresiones REST_LEGACY, OPENAI_COMPATIBLE,
hybrid-v1/v2, scoring-v1 y report-v1 ejecutadas. Los omitidos no cuentan como PASS.
El ajuste se limita al colector y conserva el contrato posterior de persistencia.
Reejecución PostgreSQL no requerida. OpenSpec strict **PASS**. **6/6 tareas.**
